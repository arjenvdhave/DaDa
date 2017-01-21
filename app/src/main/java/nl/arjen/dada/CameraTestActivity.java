package nl.arjen.dada;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class CameraTestActivity extends Activity {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;

    private int currentState;
    private TextureView previewView;
    private Size previewSize;
    private String cameraId;
    private CameraDevice cameraDevice;
    private CaptureRequest captureRequest;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder previewBuilder;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private static File imageFile;
    private String mImageFileLocation = "";
    private File mGalleryFolder;
    private String GALLERY_LOCATION = "image gallery";

    private ImageReader imageReader;
    private final OnImageAvailableListener  imageAvailableListener=
        new OnImageAvailableListener(){

            @Override
            public void onImageAvailable(ImageReader reader) {
                backgroundHandler.post(new ImageSaver(reader.acquireNextImage() ));
            }
        };


    private static class ImageSaver implements Runnable{

        private final Image image;

        public ImageSaver(Image image) {
            this.image = image;
        }

        @Override
        public void run() {
            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);

            FileOutputStream fileOutputStream = null;

            try {
                fileOutputStream = new FileOutputStream(imageFile);
                fileOutputStream.write(bytes);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                image.close();
                if(fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private CameraCaptureSession.CaptureCallback captureCallback
            = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            process(result);
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.d("DaDa", "Focus faal");
        }

        private void process(TotalCaptureResult result) {
            switch (currentState) {
                case STATE_PREVIEW:
                    break;
                case STATE_WAIT_LOCK:
                    Integer autoFocusState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (hasAutoFocus() && autoFocusState == CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED) {
                        captureStillImage();
                    }
                    if(!hasAutoFocus())
                        captureStillImage();
                    break;
            }
        }
    };

    private boolean hasAutoFocus() {
        if (cameraId == null)
            return false;

        Float minimumLens = null;
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics c = manager.getCameraCharacteristics(cameraId);
            minimumLens = c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        } catch (Exception e) { }
        if (minimumLens != null)
            return minimumLens > 0;
        return false;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_test);

        if (!checkCameraHardware(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Camera")
                    .setMessage("Deze telefoon heeft geen camera")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            previewView = (TextureView) findViewById(R.id.tvCameraPreview);
        }

        createImageGallery();

        Button btnTakePhoto = (Button) findViewById(R.id.btnTakePhoto);
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    imageFile = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                lockFocus();
            }
        });
    }

    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMAGE_" + timeStamp + "_";

        File image = File.createTempFile(imageFileName,".jpg", mGalleryFolder);
        mImageFileLocation = image.getAbsolutePath();

        return image;

    }

    private void createImageGallery() {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        mGalleryFolder = new File(storageDirectory, GALLERY_LOCATION);
        if(!mGalleryFolder.exists()) {
            mGalleryFolder.mkdirs();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        openBackgroundThread();

        if (previewView.isAvailable()) {
            setupCamera(previewView.getWidth(), previewSize.getHeight());
            openCamera();
        } else {
            previewView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    setupCamera(width, height);
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            });
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        closeBackgroundThread();

        super.onPause();
    }

    private void setupCamera(int width, int height) {
        try {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            //check if there is a front facing camera, if not default back to the first available camera
            for (String id : cameraManager.getCameraIdList()) {
                if (cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraId = id;
                    break;
                }
            }
            if (cameraId == null)
                cameraId = cameraManager.getCameraIdList()[0];

            CameraCharacteristics cc = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap streamConfigs = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Size largestImageSize = Collections.max(Arrays.asList(streamConfigs.getOutputSizes(ImageFormat.JPEG)),
                    new Comparator<Size>() {
                        @Override
                        public int compare(Size lhs, Size rhs) {
                            return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                        }
                    });

            imageReader =  ImageReader.newInstance(largestImageSize.getWidth(), largestImageSize.getHeight(), ImageFormat.JPEG,1);
            imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);

            previewSize = getPreferredSizePreviewSize(streamConfigs.getOutputSizes(SurfaceTexture.class), width, height);

        } catch (Exception e) {
            Log.d("DADA", e.getMessage());
        }
    }

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

        return Collections.min(sizes, new Comparator<Size>() {
            @Override
            public int compare(Size lhs, Size rhs) {
                return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
            }
        });
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                }
            }, backgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        if(imageReader != null){
            imageReader.close();
            imageReader = null;
        }

    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = previewView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            Surface previewSurface = new Surface(surfaceTexture);

            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(previewSurface);
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    if (cameraDevice == null)
                        return;

                    try {
                        captureRequest = previewBuilder.build();
                        cameraCaptureSession = session;
                        cameraCaptureSession.setRepeatingRequest(captureRequest, captureCallback, backgroundHandler);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, backgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openBackgroundThread() {
        backgroundThread = new HandlerThread("DaDa camera background thread");
        backgroundThread.start();

        backgroundHandler = new Handler(backgroundThread.getLooper());

    }

    private void closeBackgroundThread() {
        backgroundThread.quitSafely();

        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void lockFocus() {
        currentState = STATE_WAIT_LOCK;
        previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            cameraCaptureSession.capture(previewBuilder.build(), captureCallback, backgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        currentState = STATE_PREVIEW;
        previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        try {
            cameraCaptureSession.capture(previewBuilder.build(), captureCallback, backgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void captureStillImage(){
        try {
            CaptureRequest.Builder captureStillBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureStillBuilder.addTarget(imageReader.getSurface());

            int rotation = getWindowManager().getDefaultDisplay().getRotation();

            captureStillBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            CameraCaptureSession.CaptureCallback stillCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

                    unlockFocus();
                }
            };

            cameraCaptureSession.capture(captureStillBuilder.build(),stillCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
