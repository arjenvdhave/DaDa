package nl.arjen.dada.camera;

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
import android.util.SparseIntArray;
import android.view.Surface;

/**
 * Created by arjen on 2/16/17.
 */

public class DaDaCameraCaptureCallback extends CameraCaptureSession.CaptureCallback {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0,
                            270);
        ORIENTATIONS.append(Surface.ROTATION_90,
                            0);
        ORIENTATIONS.append(Surface.ROTATION_180,
                            90);
        ORIENTATIONS.append(Surface.ROTATION_270,
                            180);
    }


    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private static final int STATE_IMAGE_CAPTURED = 2;

    private int currentState;

    private CameraContext cameraContext;

    public DaDaCameraCaptureCallback(CameraContext cameraContext) {
        this.cameraContext = cameraContext;
    }

    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
        super.onCaptureCompleted(session,
                                 request,
                                 result);
        process(result);
    }

    @Override
    public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
        super.onCaptureFailed(session,
                              request,
                              failure);
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
        if (cameraContext.getCameraId() == null)
            return false;

        Float minimumLens = null;
        try {
            CameraManager cameraManager = (CameraManager) cameraContext.getContext()
                                                                       .getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics c = cameraManager.getCameraCharacteristics(cameraContext
                                                                                     .getCameraId());
            minimumLens = c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        } catch (Exception e) {
        }
        if (minimumLens != null)
            return minimumLens > 0;
        return false;
    }

    private void captureStillImage() {
        try {
            CaptureRequest.Builder captureStillBuilder = cameraContext.getCameraDevice()
                                                                      .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureStillBuilder.addTarget(cameraContext.getImageReader().getSurface());

            int rotation = cameraContext.getContext().getWindowManager()
                                        .getDefaultDisplay()
                                        .getRotation();

            captureStillBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                                    ORIENTATIONS.get(rotation));

            CameraCaptureSession.CaptureCallback stillCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session,
                                             request,
                                             result);
                    unlockFocus();
                }
            };

            cameraContext.getCameraCaptureSession().stopRepeating();
            cameraContext.getCameraCaptureSession().capture(captureStillBuilder.build(),
                                                            stillCallback,
                                                            null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        currentState = STATE_PREVIEW;
        cameraContext.getPreviewBuilder().set(CaptureRequest.CONTROL_AF_TRIGGER,
                                              CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        try {
            cameraContext.getCameraCaptureSession().setRepeatingRequest(cameraContext
                                                                                .getPreviewBuilder()
                                                                                .build(),
                                                                        this,
                                                                        cameraContext
                                                                                .getBackgroundHandler());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void lockFocus() {
        currentState = STATE_WAIT_LOCK;
        cameraContext.getPreviewBuilder().set(CaptureRequest.CONTROL_AF_TRIGGER,
                                              CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            cameraContext.getCameraCaptureSession().capture(cameraContext
                                                                    .getPreviewBuilder()
                                                                    .build(),
                                                            this,
                                                            cameraContext
                                                                    .getBackgroundHandler());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
