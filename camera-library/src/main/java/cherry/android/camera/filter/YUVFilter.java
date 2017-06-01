package cherry.android.camera.filter;

import android.content.Context;
import android.opengl.GLES11Ext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import cherry.android.camera.R;
import cherry.android.camera.opengl.OpenGLUtils;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE0;
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
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static cherry.android.camera.opengl.GLRotation.CUBE;
import static cherry.android.camera.opengl.GLRotation.TEXTURE_ROTATION_0;
import static cherry.android.camera.opengl.OpenGLUtils.NO_TEXTURE;

/**
 * Created by Administrator on 2017/4/6.
 */

public class YUVFilter implements RendererFilter {
    private static final String A_POSITION = "a_Position";
    private static final String A_TEXTURE_COORDINATE = "a_TextureCoordinate";
    private static final String U_TEXTURE_MATRIX = "u_TextureMatrix";
    private static final String U_IMAGE_TEXTURE = "u_ImageTexture";
    /**
     * 顶点坐标
     */
    final FloatBuffer mCubeBuffer;

    /**
     * 纹理坐标
     */
    final FloatBuffer mTextureBuffer;

    private final String mVertexShader;
    private final String mFragmentShader;
    private int mGLProgramId;
    private int mGLAttrPosition;
    private int mGLAttrTextureCoord;
    private int mGLTextureMatrix;
    private int mGLUniformTexture;


    public YUVFilter(Context context) {
        mVertexShader = OpenGLUtils.readShaderFromRaw(context, R.raw.yuv_vertex_shader);
        mFragmentShader = OpenGLUtils.readShaderFromRaw(context, R.raw.yuv_fragment_shader);

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
        mGLProgramId = OpenGLUtils.loadProgram(mVertexShader, mFragmentShader);
        //绑定变量的句柄
        mGLAttrPosition = glGetAttribLocation(mGLProgramId, A_POSITION);
        mGLAttrTextureCoord = glGetAttribLocation(mGLProgramId, A_TEXTURE_COORDINATE);
        mGLTextureMatrix = glGetUniformLocation(mGLProgramId, U_TEXTURE_MATRIX);
        mGLUniformTexture = glGetUniformLocation(mGLProgramId, U_IMAGE_TEXTURE);
    }

    @Override
    public void render(int textureId, float[] mtx) {
        if (mGLProgramId == 0)
            return;
        glUseProgram(mGLProgramId);

        mCubeBuffer.position(0);
        glVertexAttribPointer(mGLAttrPosition, 2, GL_FLOAT, false, 0, mCubeBuffer);
        glEnableVertexAttribArray(mGLAttrPosition);

        mTextureBuffer.position(0);
        glVertexAttribPointer(mGLAttrTextureCoord, 2, GL_FLOAT, false, 0, mTextureBuffer);
        glEnableVertexAttribArray(mGLAttrTextureCoord);

        glUniformMatrix4fv(mGLTextureMatrix, 1, false, mtx, 0);

        if (textureId != NO_TEXTURE) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            glUniform1i(mGLUniformTexture, 0);
        }

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glDisableVertexAttribArray(mGLAttrPosition);
        glDisableVertexAttribArray(mGLAttrTextureCoord);
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }

    @Override
    public void updateTexture(float[] mtx) {
        mTextureBuffer.clear();
        mTextureBuffer.put(mtx).position(0);
    }

    @Override
    public void destroy() {
        glDeleteProgram(mGLProgramId);
    }
}
