package com.android.phasebeam;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.Matrix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class PhaseBeamRenderer implements GLSurfaceView.Renderer
{
    //region Data
        private final Context context;
        private final ParticleManager particleManager = new ParticleManager();
        private int densityDPI;
        private long startTime;
    //endregion

    //region OpenGL ES2.0 Data
        // Shader program ID
        private int backgroundProgramId;
        private int particleProgramId;
        private int beamProgramId;

        // Vertex Buffer Object (VBO)
        private int vboId;
        private int dotVboId;
        private int beamVboId;

        // Texture id for particles
        private int dotTextureId;
        private int beamTextureId;

        // Projection matrix
        private final float[] mvpMatrix = new float[16];

        // Scaling factor
        private float scaleSize;
    //endregion

    public PhaseBeamRenderer(Context context)
    {
        this.context = context;
    }

    //region Surface handling
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config)
        {
            // Set the clear color
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

            // Enable blending for transparency
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);

            try {
                backgroundProgramId = setupProgram(R.raw.bg_vs, R.raw.bg_fs);

                // Create VBO and upload vertex data
                int[] buffers = new int[3];
                GLES20.glGenBuffers(3, buffers, 0);
                vboId = buffers[0];

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, BackgroundManager.vertexData.length * 4,
                        FloatBuffer.wrap(BackgroundManager.vertexData), GLES20.GL_STATIC_DRAW);

                particleProgramId = setupProgram(R.raw.dot_vs, R.raw.dot_fs);

                // Load particle texture
                dotTextureId = loadTexture(R.drawable.dot);
                beamTextureId = loadTexture(R.drawable.beam);

                // Create particle VBO
                dotVboId = buffers[1];
                beamVboId = buffers[2];

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, dotVboId);
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, particleManager.getParticleArrayDataLength(),
                        FloatBuffer.wrap(particleManager.getParticleData()), GLES20.GL_DYNAMIC_DRAW);

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, beamVboId);
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, particleManager.getParticleArrayDataLength(),
                        FloatBuffer.wrap(particleManager.getBeamData()), GLES20.GL_DYNAMIC_DRAW);
            }
            catch (Exception ignored) {}
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height)
        {
            GLES20.glViewport(0, 0, width, height);
            setupProjectionMatrix(width, height);
            scaleSize = densityDPI / 240.0f;
        }
    //endregion

    //region Setters
        public void setDensityDPI(int densityDPI)
        {
            this.densityDPI = densityDPI;
        }

        public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels)
        {
            particleManager.setXOffset(xOffset);
        }
    //endregion

    //region Draw handling
        @Override
        public void onDrawFrame(GL10 gl)
        {
            // Some older Android images don't limit to 60FPS themselves
            long endTime = System.currentTimeMillis();
            long dt = endTime - startTime;
            if (dt < 33 && dt > 0)
            {
                try {
                    Thread.sleep(33 - dt);
                }
                catch (Exception ignored) {}
            }
            startTime = System.currentTimeMillis();

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            try {
                particleManager.tickXOffset();
                drawBackground();

                // Update and draw dots
                particleManager.updateParticles();
                drawParticle(particleManager.getParticleData(), dotVboId, dotTextureId);
                drawParticle(particleManager.getBeamData(), beamVboId, beamTextureId);

                particleManager.finishXOffseTick();
            }
            catch (Exception ignored) {}
        }
    //endregion

    //region Drawing functions
        private void drawBackground()
        {
            GLES20.glUseProgram(backgroundProgramId);

            // Bind VBO and enable vertex attributes
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);

            // position
            GLES20.glEnableVertexAttribArray(0);
            GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 20, 0);

            // color
            GLES20.glEnableVertexAttribArray(1);
            GLES20.glVertexAttribPointer(1, 3, GLES20.GL_FLOAT, false, 20, 8);

            // xOffset
            GLES20.glUniform1f(0, particleManager.backgroundXOffset);

            // Draw the vertices
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, BackgroundManager.vertexCount);
        }

        private void drawParticle(float[] particleData, int vboId, int textureId) {
            // Render particles
            GLES20.glUseProgram(particleProgramId);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, particleManager.getParticleArrayDataLength(),
                                        FloatBuffer.wrap(particleData));

            // Pass float x, y and z
            GLES20.glEnableVertexAttribArray(0);
            GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 12, 0);

            // Pass view matrix
            GLES20.glUniformMatrix4fv(0, 1, false, mvpMatrix, 0);

            // Pass scale size
            GLES20.glUniform1f(1, scaleSize);

            // Pass x offset
            GLES20.glUniform1f(2, particleManager.particleXOffset);

            // Bind particle texture
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(3, 0);

            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, particleManager.getParticleCount());
        }
    //endregion

    //region OpenGL helper functions
        private int setupProgram(int vertexShaderResourceId, int fragmentShaderResourceId1)
        {
            String vertexShaderSource = loadShaderSource(context.getResources(), vertexShaderResourceId);
            String fragmentShaderSource = loadShaderSource(context.getResources(), fragmentShaderResourceId1);

            int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource);
            int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource);

            int tempStore = GLES20.glCreateProgram();
            GLES20.glAttachShader(tempStore, vertexShader);
            GLES20.glAttachShader(tempStore, fragmentShader);
            GLES20.glLinkProgram(tempStore);

            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(tempStore, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] == 0)
            {
                String error = GLES20.glGetProgramInfoLog(tempStore);
                throw new RuntimeException("Program linking failed: " + error);
            }

            return tempStore;
        }

        private int compileShader(int type, String source)
        {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);

            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0)
            {
                String error = GLES20.glGetShaderInfoLog(shader);
                GLES20.glDeleteShader(shader);
                throw new RuntimeException("Shader compilation failed: " + error);
            }

            return shader;
        }

        private String loadShaderSource(Resources resources, int resourceId)
        {
            InputStream inputStream = resources.openRawResource(resourceId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder shaderSource = new StringBuilder();

            try
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    shaderSource.append(line).append("\n");
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to read shader source: " + e.getMessage());
            }
            finally
            {
                try
                {
                    inputStream.close();
                }
                catch (IOException ignored) {}
            }

            return shaderSource.toString();
        }

        private int loadTexture(int resourceId)
        {
            // Generate a texture ID
            final int[] textureHandle = new int[1];
            GLES20.glGenTextures(1, textureHandle, 0);

            if (textureHandle[0] != 0)
            {
                // Load the texture resource as a Bitmap
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false; // No pre-scaling
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

                // Bind to the texture ID
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

                // Set texture parameters
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                // Load the bitmap into the bound texture
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

                // Recycle the bitmap, as it's no longer needed
                bitmap.recycle();
            }
            else
            {
                throw new RuntimeException("Error generating texture handle.");
            }

            return textureHandle[0];
        }

        private void setupProjectionMatrix(int width, int height)
        {
            float aspectRatio;
            if (width > height)
            {
                aspectRatio = (float) height / width;
                Matrix.frustumM(mvpMatrix, 0, -aspectRatio, aspectRatio, -1.0f, 1.0f, 1.0f, 100.0f);
            }
            else
            {
                aspectRatio = (float) width / height;
                Matrix.frustumM(mvpMatrix, 0, -1.0f, 1.0f, -aspectRatio, aspectRatio, 1.0f, 100.0f);
            }

            // Apply additional transformations like the original code
            Matrix.rotateM(mvpMatrix, 0, 180.0f, 0.0f, 1.0f, 0.0f);
            Matrix.scaleM(mvpMatrix, 0, -1.0f, 1.0f, 1.0f);
            Matrix.translateM(mvpMatrix, 0, 0.0f, 0.0f, 1.0f);
        }
    //endregion
}
