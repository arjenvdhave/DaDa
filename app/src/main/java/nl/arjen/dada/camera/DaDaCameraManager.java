package nl.arjen.dada.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by arjen on 2/16/17.
 */

public class DaDaCameraManager {

    private DaDaCameraStateCallback daDaCameraStateCallback;
    private HandlerThread backgroundThread;
    private CameraContext cameraContext;

    public DaDaCameraManager(CameraContext cameraContext) {
        this.cameraContext = cameraContext;
    }

    public void takePhoto() {
        daDaCameraStateCallback.takePhoto();
    }

    public void closeCamera() {
        daDaCameraStateCallback.closeCamera();

        if (cameraContext.getImageReader() != null) {
            cameraContext.getImageReader().close();
            cameraContext.setImageReader(null);
        }

        backgroundThread.quitSafely();

        try {
            backgroundThread.join();
            backgroundThread = null;
            cameraContext.setBackgroundHandler(null);
        } catch (Exception e) {
            //TODO fix
            e.printStackTrace();
        }
    }


    // ===========  STEP 1: SETUP CAMERA IDS AND SURFACE LISTENER ================//

    public void init() {
        openBackgroundThread();
        setupCameraIds();

        if (cameraContext.isFrontCameraAvailable())
            cameraContext.setCameraId(cameraContext.getFrontCameraId());
        else
            cameraContext.setCameraId(cameraContext.getRearCameraId());

        if (cameraContext.getPreviewView().isAvailable()) {
            previewTextureReady(cameraContext.getPreviewView().getWidth(),
                                cameraContext.getPreviewView().getHeight());
        } else {
            cameraContext.getPreviewView()
                         .setSurfaceTextureListener(new DaDaSurfaceTextureListener() {
                             @Override
                             public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                                 previewTextureReady(width,
                                                     height);
                             }
                         });
        }
    }

    private void previewTextureReady(int width, int height) {
        setupCamera(width,
                    height);
        transformImage(width,
                       height);
        openCamera();
    }

    // ===========  STEP 2: GET FRONT AND REAR CAMERA ID ================//

    private void setupCameraIds() {
        try {
            CameraManager cameraManager = (CameraManager) cameraContext.getContext()
                                                                       .getSystemService(Context.CAMERA_SERVICE);
            //check if there is a front facing camera, if not default back to the first available camera
            for (String id : cameraManager.getCameraIdList()) {
                if (cameraManager.getCameraCharacteristics(id)
                                 .get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraContext.setFrontCameraId(id);
                    cameraContext.setFrontCameraAvailable(true);
                } else if (cameraManager.getCameraCharacteristics(id)
                                        .get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK) {
                    cameraContext.setRearCameraId(id);
                }
            }

            if (cameraContext.getFrontCameraId() == null && cameraContext
                    .getRearCameraId() == null) {
                //TODO iets van een melding dat er geen camera is
            }
        } catch (Exception e) {

        }
    }

    // ===========  STEP 3: SETUP IMAGE READER AND PREVIEW SIZE ================//

    /**
     * Setup imageReader en previewSize using the selected camera id
     *
     * @param width  of the preview texture
     * @param height of the preview texture
     */
    private void setupCamera(int width, int height) {
        try {
            CameraManager cameraManager = (CameraManager) cameraContext.getContext()
                                                                       .getSystemService(Context.CAMERA_SERVICE);


            CameraCharacteristics cc = cameraManager
                    .getCameraCharacteristics(cameraContext.getCameraId());
            StreamConfigurationMap streamConfigs = cc
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Size largestImageSize = Collections
                    .max(Arrays.asList(streamConfigs.getOutputSizes(ImageFormat.JPEG)),
                         new Comparator<Size>() {
                             @Override
                             public int compare(Size lhs, Size rhs) {
                                 return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs
                                         .getWidth() * rhs.getHeight());
                             }
                         });

            cameraContext.setImageReader(ImageReader
                                                 .newInstance(largestImageSize.getWidth(),
                                                              largestImageSize.getHeight(),
                                                              ImageFormat.JPEG,
                                                              1));

            cameraContext.getImageReader()
                         .setOnImageAvailableListener(new DaDaImageAvailableListener(cameraContext),
                                                      cameraContext.getBackgroundHandler());

            cameraContext.setPreviewSize(getPreferredSizePreviewSize(streamConfigs
                                                                             .getOutputSizes(SurfaceTexture.class),
                                                                     width,
                                                                     height));

        } catch (Exception e) {
            Log.d("DADA",
                  e.getMessage());
        }
    }

    // ===========  STEP 4: OPEN THE CAMERA AND CONNECT STATE CALLBACK ================//

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) cameraContext.getContext()
                                                                   .getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat
                    .checkSelfPermission(cameraContext.getContext(),
                                         Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            daDaCameraStateCallback = new DaDaCameraStateCallback(cameraContext);
            cameraManager.openCamera(cameraContext.getCameraId(),
                                     daDaCameraStateCallback,
                                     cameraContext.getBackgroundHandler());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ===========  HELPERS  ================//

    /**
     * Search for the camera size that best matches the size of the preview window
     *
     * @param cameraSizes list of available sizes of the camera
     * @param width       width of the preview
     * @param height      height of the preview
     * @return best matching size
     */

    private Size getPreferredSizePreviewSize(Size[] cameraSizes, int width, int height) {
        List<Size> sizes = new ArrayList<>();
        for (Size option : cameraSizes) {
            if (width > height) {//landscape
                if (option.getWidth() > width && option.getHeight() > height)
                    sizes.add(option);
            } else { //portrait
                if (option.getWidth() > height && option.getHeight() > width)
                    sizes.add(option);
            }
        }

        if (sizes.isEmpty())
            return cameraSizes[0];

        return Collections.min(sizes,
                               new Comparator<Size>() {
                                   @Override
                                   public int compare(Size lhs, Size rhs) {
                                       return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs
                                               .getWidth() * rhs
                                               .getHeight());
                                   }
                               });
    }

    /**
     * Magie om de het roteren van het scherm goed te laten verlopen om sommige toestellen (bijv. Samsung)
     *
     * @param width
     * @param height
     */
    private void transformImage(int width, int height) {
        if (cameraContext.getPreviewSize() == null || cameraContext.getPreviewView() == null)
            return;

        Matrix matrix = new Matrix();
        int rotation = cameraContext.getContext().getWindowManager()
                                    .getDefaultDisplay()
                                    .getRotation();
        RectF textureRectF = new RectF(0,
                                       0,
                                       width,
                                       height);
        RectF previewRectF = new RectF(0,
                                       0,
                                       cameraContext.getPreviewSize().getHeight(),
                                       cameraContext.getPreviewSize().getWidth());

        float centerX = textureRectF.centerX();
        float centerY = textureRectF.centerY();

        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            previewRectF.offset(centerX - previewRectF.centerX(),
                                centerY - previewRectF.centerY());
            matrix.setRectToRect(textureRectF,
                                 previewRectF,
                                 Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) width / cameraContext.getPreviewSize().getWidth(),
                                   (float) height / cameraContext.getPreviewSize().getHeight());
            matrix.postScale(scale,
                             scale,
                             centerX,
                             centerY);
            matrix.postRotate(90 * (rotation - 2),
                              centerX,
                              centerY);
        }
        cameraContext.getPreviewView().setTransform(matrix);
    }

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("DaDa camera background thread");
        backgroundThread.start();

        cameraContext.setBackgroundHandler(new Handler(backgroundThread.getLooper()));
    }


}
