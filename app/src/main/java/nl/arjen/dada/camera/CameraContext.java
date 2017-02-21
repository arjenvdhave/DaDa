package nl.arjen.dada.camera;

import android.app.Activity;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Size;
import android.view.TextureView;

import java.io.File;

/**
 * Created by arjen on 2/21/17.
 */

public class CameraContext {

    private TextureView previewView;
    private Activity context;
    private File photoFile;

    private Handler backgroundHandler;
    private ImageReader imageReader;
    private Size previewSize;

    private String cameraId;
    private String frontCameraId;
    private String rearCameraId;
    private boolean frontCameraAvailable = true;

    private CameraDevice cameraDevice;
    private CaptureRequest captureRequest;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder previewBuilder;

    public CameraContext() {
    }

    public CameraContext(Activity context, TextureView previewView, File photoFile) {
        this.context = context;
        this.previewView = previewView;
        this.photoFile = photoFile;
    }

    public Handler getBackgroundHandler() {
        return backgroundHandler;
    }

    public void setBackgroundHandler(Handler backgroundHandler) {
        this.backgroundHandler = backgroundHandler;
    }

    public File getPhotoFile() {
        return photoFile;
    }

    public void setPhotoFile(File photoFile) {
        this.photoFile = photoFile;
    }

    public ImageReader getImageReader() {
        return imageReader;
    }

    public void setImageReader(ImageReader imageReader) {
        this.imageReader = imageReader;
    }

    public TextureView getPreviewView() {
        return previewView;
    }

    public void setPreviewView(TextureView previewView) {
        this.previewView = previewView;
    }

    public Size getPreviewSize() {
        return previewSize;
    }

    public void setPreviewSize(Size previewSize) {
        this.previewSize = previewSize;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public Activity getContext() {
        return context;
    }

    public void setContext(Activity context) {
        this.context = context;
    }

    public String getFrontCameraId() {
        return frontCameraId;
    }

    public void setFrontCameraId(String frontCameraId) {
        this.frontCameraId = frontCameraId;
    }

    public String getRearCameraId() {
        return rearCameraId;
    }

    public void setRearCameraId(String rearCameraId) {
        this.rearCameraId = rearCameraId;
    }

    public boolean isFrontCameraAvailable() {
        return frontCameraAvailable;
    }

    public void setFrontCameraAvailable(boolean frontCameraAvailable) {
        this.frontCameraAvailable = frontCameraAvailable;
    }

    public CameraDevice getCameraDevice() {
        return cameraDevice;
    }

    public void setCameraDevice(CameraDevice cameraDevice) {
        this.cameraDevice = cameraDevice;
    }

    public CaptureRequest getCaptureRequest() {
        return captureRequest;
    }

    public void setCaptureRequest(CaptureRequest captureRequest) {
        this.captureRequest = captureRequest;
    }

    public CameraCaptureSession getCameraCaptureSession() {
        return cameraCaptureSession;
    }

    public void setCameraCaptureSession(CameraCaptureSession cameraCaptureSession) {
        this.cameraCaptureSession = cameraCaptureSession;
    }

    public CaptureRequest.Builder getPreviewBuilder() {
        return previewBuilder;
    }

    public void setPreviewBuilder(CaptureRequest.Builder previewBuilder) {
        this.previewBuilder = previewBuilder;
    }
}
