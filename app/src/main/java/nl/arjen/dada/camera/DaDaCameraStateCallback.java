package nl.arjen.dada.camera;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.util.Arrays;

/**
 * Created by arjen on 2/16/17.
 */

public class DaDaCameraStateCallback extends CameraDevice.StateCallback {

    private DaDaCameraCaptureCallback daDaCameraCaptureCallback;

    private CameraDevice cameraDevice;
    private CaptureRequest captureRequest;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder previewBuilder;

    private Size previewSize;
    private TextureView previewView;
    private Handler backgroundHandler;
    private ImageReader imageReader;

    private String cameraId;
    private Activity context;


    public DaDaCameraStateCallback(Size previewSize, TextureView previewView, Handler backgroundHandler, ImageReader imageReader, String cameraId, Activity context) {
        this.previewSize = previewSize;
        this.previewView = previewView;
        this.backgroundHandler = backgroundHandler;
        this.imageReader = imageReader;
        this.cameraId = cameraId;
        this.context = context;
    }

    public void takePhoto() {
        daDaCameraCaptureCallback.lockFocus();
    }

    public void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }


    }

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

                        daDaCameraCaptureCallback = new DaDaCameraCaptureCallback(cameraId,
                                context,
                                cameraDevice,
                                imageReader,
                                cameraCaptureSession,
                                previewBuilder,
                                backgroundHandler);

                        cameraCaptureSession.setRepeatingRequest(captureRequest,
                                daDaCameraCaptureCallback,
                                backgroundHandler);
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
}
