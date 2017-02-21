package nl.arjen.dada.camera;

import android.media.ImageReader;

/**
 * Created by arjen on 2/16/17.
 */

public class DaDaImageAvailableListener implements ImageReader.OnImageAvailableListener {

    private CameraContext cameraContext;

    public DaDaImageAvailableListener(CameraContext cameraContext) {
        this.cameraContext = cameraContext;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        //todo file aanmaken ergens
        cameraContext.getBackgroundHandler().post(new DaDaImageSaver(reader.acquireNextImage(),
                                                                     cameraContext.getPhotoFile()));
    }
}
