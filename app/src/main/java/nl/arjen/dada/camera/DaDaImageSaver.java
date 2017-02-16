package nl.arjen.dada.camera;

import android.media.Image;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by arjen on 2/16/17.
 */

public class DaDaImageSaver implements Runnable {

    private final Image image;
    private File imageFile;

    public DaDaImageSaver(Image image, File imageFile) {
        this.image = image;
        this.imageFile = imageFile;
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
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
