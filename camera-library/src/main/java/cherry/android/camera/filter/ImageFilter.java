package cherry.android.camera.filter;

import android.content.Context;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import cherry.android.camera.R;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static cherry.android.camera.opengl.GLRotation.CUBE;
import static cherry.android.camera.opengl.GLRotation.TEXTURE_ROTATION_0;
import static cherry.android.camera.opengl.OpenGLUtils.NO_TEXTURE;
import static cherry.android.camera.opengl.OpenGLUtils.loadProgram;
import static cherry.android.camera.opengl.OpenGLUtils.readShaderFromRaw;

/**
 * Created by Administrator on 2017/4/11.
 */

public class ImageFilter implements RendererFilter {
    private static final String A_POSITION = "a_Position";
    private static final String A_TEXTURE_COORDINATE = "a_TextureCoordinate";
    private static final String U_IMAGE_TEXTURE = "u_ImageTexture";
    private final String mVertexShader;
    private final String mFragmentShader;
    private FloatBuffer mCubeBuffer;
    private FloatBuffer mTextureBuffer;
    private int mGLProgramId;
    private int mGLAttrPosition;
    private int mGLAttrTextureCoords;
    private int mGLUniformTexture;

    public ImageFilter(Context context) {
        mVertexShader = readShaderFromRaw(context, R.raw.texture_vertex_shader);
        mFragmentShader = readShaderFromRaw(context, R.raw.texture_fragment_shader);

        mCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mCubeBuffer.put(CUBE).position(0);

        mTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_ROTATION_0.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureBuffer.put(TEXTURE_ROTATION_0).position(0);
    }

    @Override
    public void setup() {
        mGLProgramId = loadProgram(mVertexShader, mFragmentShader);
        mGLAttrPosition = glGetAttribLocation(mGLProgramId, A_POSITION);
        mGLAttrTextureCoords = glGetAttribLocation(mGLProgramId, A_TEXTURE_COORDINATE);
        mGLUniformTexture = glGetUniformLocation(mGLProgramId, U_IMAGE_TEXTURE);
    }

    @Override
    public void destroy() {
        glDeleteProgram(mGLProgramId);
    }

    @Override
    public void render(int textureId, float[] mtx) {
        if (textureId == NO_TEXTURE)
            return;
        glUseProgram(mGLProgramId);

        mCubeBuffer.position(0);
        glEnableVertexAttribArray(mGLAttrPosition);
        glVertexAttribPointer(mGLAttrPosition, 2, GL_FLOAT, false, 0, mCubeBuffer);

        mTextureBuffer.position(0);
        glEnableVertexAttribArray(mGLAttrTextureCoords);
        glVertexAttribPointer(mGLAttrTextureCoords, 2, GL_FLOAT, false, 0, mTextureBuffer);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glUniform1i(mGLUniformTexture, 0);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glDisableVertexAttribArray(mGLAttrPosition);
        glDisableVertexAttribArray(mGLAttrTextureCoords);
        glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    @Override
    public void updateTexture(float[] mtx) {

    }
}
