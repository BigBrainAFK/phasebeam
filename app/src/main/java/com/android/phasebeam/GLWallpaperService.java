package com.android.phasebeam;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public abstract class GLWallpaperService extends WallpaperService
{
    public class GLEngine extends Engine
    {
        private WallpaperGLSurfaceView glSurfaceView;
        private boolean rendererHasBeenSet;
        PhaseBeamRenderer renderer;

        class WallpaperGLSurfaceView extends GLSurfaceView
        {

            WallpaperGLSurfaceView(Context context)
            {
                super(context);
            }

            public SurfaceHolder getHolder()
            {
                return getSurfaceHolder();
            }

            public void onDestroy()
            {
                super.onDetachedFromWindow();
            }
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder)
        {
            super.onCreate(surfaceHolder);

            glSurfaceView = new WallpaperGLSurfaceView(GLWallpaperService.this);
        }

        @Override
        public void onVisibilityChanged(boolean visible)
        {
            super.onVisibilityChanged(visible);

            if(rendererHasBeenSet)
            {
                if (visible)
                {
                    glSurfaceView.onResume();
                }
                else
                {
                    glSurfaceView.onPause();
                }
            }
        }

        @Override
        public void onDestroy()
        {
            super.onDestroy();
            glSurfaceView.onDestroy();
        }

        protected void setRenderer(GLSurfaceView.Renderer renderer)
        {
            this.renderer = (PhaseBeamRenderer)renderer;
            glSurfaceView.setRenderer(renderer);
            rendererHasBeenSet = true;
        }

        protected void setPreserveEGLContextOnPause(boolean preserve)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                glSurfaceView.setPreserveEGLContextOnPause(preserve);
            }
        }

        protected void setEGLContextClientVersion(int version)
        {
            glSurfaceView.setEGLContextClientVersion(version);
        }

        protected void setEGLConfigChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize)
        {
            glSurfaceView.setEGLConfigChooser(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize);
        }
    }
}
