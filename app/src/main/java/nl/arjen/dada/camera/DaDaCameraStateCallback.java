package nl.arjen.dada.camera;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.view.Surface;

import java.util.Arrays;

/**
 * Created by arjen on 2/16/17.
 */

public class DaDaCameraStateCallback extends CameraDevice.StateCallback {

    private DaDaCameraCaptureCallback daDaCameraCaptureCallback;
    private CameraContext cameraContext;


    public DaDaCameraStateCallback(CameraContext cameraContext) {
        this.cameraContext = cameraContext;
    }

    public void takePhoto() {
        daDaCameraCaptureCallback.lockFocus();
    }

    public void closeCamera() {
        if (cameraContext.getCameraCaptureSession() != null) {
            cameraContext.getCameraCaptureSession().close();
            cameraContext.setCameraCaptureSession(null);
        }

        if (cameraContext.getCameraDevice() != null) {
            cameraContext.getCameraDevice().close();
            cameraContext.setCameraDevice(null);
        }
    }

    @Override
    public void onOpened(CameraDevice camera) {
        cameraContext.setCameraDevice(camera);
        createCameraPreviewSession();
    }

    @Override
    public void onDisconnected(CameraDevice camera) {
        camera.close();
        cameraContext.setCameraDevice(null);
    }

    @Override
    public void onError(CameraDevice camera, int error) {
        camera.close();
        cameraContext.setCameraDevice(null);
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = cameraContext.getPreviewView().getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(cameraContext.getPreviewSize().getWidth(),
                                                cameraContext.getPreviewSize().getHeight());

            Surface previewSurface = new Surface(surfaceTexture);

            cameraContext.setPreviewBuilder(cameraContext.getCameraDevice()
                                                         .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW));
            cameraContext.getPreviewBuilder().addTarget(previewSurface);
            cameraContext.getCameraDevice().createCaptureSession(Arrays.asList(previewSurface,
                                                                               cameraContext
                                                                                       .getImageReader()
                                                                                       .getSurface()),
                                                                 getStateCallback(),
                                                                 cameraContext
                                                                         .getBackgroundHandler());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.StateCallback getStateCallback() {
        return new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                if (cameraContext.getCameraDevice() == null)
                    return;

                try {
                    cameraContext.setCaptureRequest(cameraContext.getPreviewBuilder().build());
                    cameraContext.setCameraCaptureSession(session);

                    daDaCameraCaptureCallback = new DaDaCameraCaptureCallback(cameraContext);

                    cameraContext
                            .getCameraCaptureSession()
                            .setRepeatingRequest(cameraContext.getCaptureRequest(),
                                                 daDaCameraCaptureCallback,
                                                 cameraContext.getBackgroundHandler());
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {

            }
        };
    }
}
