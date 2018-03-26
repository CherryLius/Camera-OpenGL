package cherry.android.camera.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLUtils;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cherry.android.camera.util.CameraLog;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glTexParameterf;

/**
 * Created by Administrator on 2017/4/6.
 */

public class OpenGLUtils {

    private static final String TAG = "OpenGLUtils";

    public static final int NO_TEXTURE = -1;

    /**
     * 创建可执行的OpenGL ES program
     *
     * @param strVSource 顶点着色器的glsl
     * @param strFSource 片段着色器的glsl
     * @return
     */
    public static int loadProgram(final String strVSource, final String strFSource) {
        int iVShader;
        int iFShader;
        int iProgramId;
        int[] link = new int[1];
        iVShader = loadShader(strVSource, GL_VERTEX_SHADER);
        if (iVShader == 0)
            return 0;
        iFShader = loadShader(strFSource, GL_FRAGMENT_SHADER);
        if (iFShader == 0)
            return 0;
        iProgramId = glCreateProgram();
        glAttachShader(iProgramId, iVShader);
        glAttachShader(iProgramId, iFShader);

        glLinkProgram(iProgramId);
        glGetProgramiv(iProgramId, GL_LINK_STATUS, link, 0);
        if (link[0] <= 0) {
            CameraLog.e(TAG, "Link Program Failed.");
            return 0;
        }

        glDeleteShader(iVShader);
        glDeleteShader(iFShader);
        return iProgramId;
    }

    /**
     * 创建着色器
     *
     * @param strSource
     * @param iType
     * @return
     */
    private static int loadShader(@NonNull final String strSource, final int iType) {
        int[] compiled = new int[1];
        int iShader = glCreateShader(iType);
        glShaderSource(iShader, strSource);
        glCompileShader(iShader);
        glGetShaderiv(iShader, GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            CameraLog.e(TAG, " Failed : Compilation\n" + glGetShaderInfoLog(iShader));
            return 0;
        }
        return iShader;
    }

    public static String readShaderFromRaw(@NonNull final Context context, @RawRes final int resourceId) {
        InputStream is = context.getResources().openRawResource(resourceId);
        InputStreamReader isReader = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isReader);

        String buffer;
        StringBuilder builder = new StringBuilder();
        try {
            while ((buffer = br.readLine()) != null) {
                builder.append(buffer);
                builder.append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return builder.toString();
    }

    public static int getExternalOESTextureId() {
        int[] texture = new int[1];
        glGenTextures(1, texture, 0);
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);

        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        return texture[0];
    }

    public static int loadTexture(final Bitmap bitmap, final int textureId) {
        return loadTexture(bitmap, textureId, false);
    }

    public static int loadTexture(final Bitmap bitmap, final int textureId, boolean recycle) {
        if (bitmap == null) return NO_TEXTURE;
        int[] textures = new int[1];
        if (textureId == NO_TEXTURE) {
            glGenTextures(1, textures, 0);
            glBindTexture(GL_TEXTURE_2D, textures[0]);

            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);
        } else {
            glBindTexture(GL_TEXTURE_2D, textureId);
            GLUtils.texSubImage2D(GL_TEXTURE_2D, 0, 0, 0, bitmap);
            textures[0] = textureId;
        }
        if (recycle) {
            bitmap.recycle();
        }
        return textures[0];
    }
}
