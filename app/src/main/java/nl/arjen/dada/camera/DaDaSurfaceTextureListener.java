package nl.arjen.dada.camera;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

/**
 * Created by arjen on 2/16/17.
 */

public class DaDaSurfaceTextureListener implements TextureView.SurfaceTextureListener {

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

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
}
