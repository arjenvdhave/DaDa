package nl.arjen.dada.camera;

import android.media.ImageReader;
import android.os.Handler;

import java.io.File;

/**
 * Created by arjen on 2/16/17.
 */

public class DaDaImageAvailableListener implements ImageReader.OnImageAvailableListener {

    private Handler backgroundHandler;
    private File file;

    public DaDaImageAvailableListener(Handler backgroundHandler, File file) {
        this.backgroundHandler = backgroundHandler;
        this.file = file;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        //todo file aanmaken ergens
        backgroundHandler.post(new DaDaImageSaver(reader.acquireNextImage(), file));
    }
}
