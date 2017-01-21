package nl.arjen.dada;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraTestActivity extends Activity {

    private SurfaceTexture mPreviewSurfaceTexture;
    private TextureView previewView;
    private Size previewSize;
    private String cameraId;

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(previewView.isAvailable()){

        }else{
            previewView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    mPreviewSurfaceTexture = surface;
                    setupCamera(width, height);
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

    private void setupCamera(int width, int height){
        try {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            //check if there is a front facing camera, if not default back to the first available camera
            for(String id : cameraManager.getCameraIdList()){
                if(cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT){
                    cameraId = id;
                    break;
                }
            }
            if(cameraId == null)
                cameraId = cameraManager.getCameraIdList()[0];

            CameraCharacteristics cc = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap streamConfigs = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            previewSize = getPreferredSizePreviewSize(streamConfigs.getOutputSizes(SurfaceTexture.class), width, height);

        }catch (Exception e){
            Log.d("DADA", e.getMessage());
        }
    }

    /**
     * Search for the camera size that best matches the size of the preview window
     * @param cameraSizes list of available sizes of the camera
     * @param width width of the preview
     * @param height height of the preview
     * @return best matching size
     */
    private Size getPreferredSizePreviewSize(Size[] cameraSizes, int width, int height){
        List<Size> sizes = new ArrayList<>();
        for(Size option : cameraSizes){
            if(width > height){//landscape
                if(option.getWidth() > width && option.getHeight() > height)
                    sizes.add(option);
            }else{ //portrait
                if(option.getWidth() > height && option.getHeight() > width)
                    sizes.add(option);
            }
        }

        if(sizes.isEmpty())
            return cameraSizes[0];

        return Collections.min(sizes, new Comparator<Size>() {
            @Override
            public int compare(Size lhs, Size rhs) {
                return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
            }
        });
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
}
