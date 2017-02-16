package nl.arjen.dada.camera;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Handler;
import android.util.SparseIntArray;
import android.view.Surface;

/**
 * Created by arjen on 2/16/17.
 */

public class DaDaCameraCaptureCallback extends CameraCaptureSession.CaptureCallback {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 270);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 90);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private static final int STATE_IMAGE_CAPTURED = 2;

    private int currentState;

    private String cameraId;
    private Activity context;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder previewBuilder;
    private Handler backgroundHandler;

    //TODO deze constructor zuigt natuurlijk kijken of dot anders kan
    public DaDaCameraCaptureCallback(String cameraId, Activity context, CameraDevice cameraDevice, ImageReader imageReader, CameraCaptureSession cameraCaptureSession, CaptureRequest.Builder previewBuilder, Handler backgroundHandler) {
        this.cameraId = cameraId;
        this.context = context;
        this.cameraDevice = cameraDevice;
        this.imageReader = imageReader;
        this.cameraCaptureSession = cameraCaptureSession;
        this.previewBuilder = previewBuilder;
        this.backgroundHandler = backgroundHandler;
    }

    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
        super.onCaptureCompleted(session, request, result);
        process(result);
    }

    @Override
    public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
        super.onCaptureFailed(session, request, failure);
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
                if (!hasAutoFocus())
                    captureStillImage();

                currentState = STATE_IMAGE_CAPTURED;
                break;
        }
    }

    private boolean hasAutoFocus() {
        if (cameraId == null)
            return false;

        Float minimumLens = null;
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics c = manager.getCameraCharacteristics(cameraId);
            minimumLens = c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        } catch (Exception e) {
        }
        if (minimumLens != null)
            return minimumLens > 0;
        return false;
    }

    private void captureStillImage() {
        try {
            CaptureRequest.Builder captureStillBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureStillBuilder.addTarget(imageReader.getSurface());

            int rotation = context.getWindowManager().getDefaultDisplay().getRotation();

            captureStillBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            CameraCaptureSession.CaptureCallback stillCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    unlockFocus();
                }
            };

            cameraCaptureSession.stopRepeating();
            cameraCaptureSession.capture(captureStillBuilder.build(), stillCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        currentState = STATE_PREVIEW;
        previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        try {
            cameraCaptureSession.setRepeatingRequest(previewBuilder.build(), this, backgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void lockFocus() {
        currentState = STATE_WAIT_LOCK;
        previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            cameraCaptureSession.capture(previewBuilder.build(), this, backgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
