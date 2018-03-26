package cherry.android.camera.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cherry.android.camera.annotations.CameraId;
import cherry.android.camera.camera.CameraCompact;
import cherry.android.camera.filter.ImageFilter;
import cherry.android.camera.filter.RendererFilter;
import cherry.android.camera.filter.YUVFilter;
import cherry.android.camera.opengl.GLRotation;
import cherry.android.camera.opengl.OpenGLUtils;
import cherry.android.camera.opengl.Rotation;
import cherry.android.camera.util.CameraLog;

/**
 * Created by Administrator on 2017/4/6.
 */

public class CaptureRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "CaptureRenderer";

    private Context mContext;
    private GLSurfaceView mGLSurfaceView;

    private RendererFilter mFilter;
    private SurfaceTexture mSurfaceTexture;
    private int mTextureId = OpenGLUtils.NO_TEXTURE;
    private CameraCompact mCameraCompact;

    private ArrayMap<Integer, RendererFilter> mFilterMap;
    private Bitmap mBitmap;

    public static final int STATE_CAPTURE = 0;
    public static final int STATE_PICTURE = 1;
    private int mState;

    private OnFilterChangeListener mFilterChangeListener;

    public CaptureRenderer(@NonNull GLSurfaceView glSurfaceView) {
        mGLSurfaceView = glSurfaceView;
        mContext = glSurfaceView.getContext();
        mState = STATE_CAPTURE;

        mFilterMap = new ArrayMap<>(2);

        filterFromState(mState);

        //Camera preview textureId
        mTextureId = OpenGLUtils.getExternalOESTextureId();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        mCameraCompact = new CameraCompact(mContext, mSurfaceTexture);

        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(this);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        CameraLog.i(TAG, "onSurfaceCreated");
        GLES20.glDisable(GLES20.GL_DITHER);
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        mFilter.setup();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        CameraLog.v(TAG, "[onSurfaceChanged] width=" + width + "x" + height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (mState == STATE_PICTURE) {
            int textureId = OpenGLUtils.loadTexture(mBitmap, OpenGLUtils.NO_TEXTURE);
            CameraLog.e(TAG, "draw picture : " + textureId + ", bm=" + mBitmap);
            mFilter.render(textureId, null);
        } else if (mState == STATE_CAPTURE) {
            if (mSurfaceTexture != null) {
                mSurfaceTexture.updateTexImage();
                float[] mtx = new float[16];
                mSurfaceTexture.getTransformMatrix(mtx);
                mFilter.render(mTextureId, mtx);
            }
        }
    }

    public void resume() {
        mGLSurfaceView.onResume();
        mCameraCompact.start(CameraId.CAMERA_BACK);
        boolean flipHorizontal = mCameraCompact.isFrontCamera();
        CameraLog.d(TAG, "orientation=" + mCameraCompact.getOrientation());
        adjustPosition(mCameraCompact.getOrientation(), flipHorizontal, !flipHorizontal);
    }

    public void pause() {
        mCameraCompact.stop();
        mGLSurfaceView.onPause();
    }

    public void destroy() {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                for (RendererFilter f : mFilterMap.values()) {
                    if (f != null)
                        f.destroy();
                }
            }
        });
        recycle();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (mState == STATE_CAPTURE) {
            mGLSurfaceView.requestRender();
        }
    }

    public CameraCompact getCameraCompat() {
        return mCameraCompact;
    }

    public void setFilter(final int state) {
        CameraLog.i(TAG, "setFilter");
        if (mFilterChangeListener != null && mState != state)
            mFilterChangeListener.onFilterChanged(state);

        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mState == STATE_PICTURE && mState != state) {
                    recycle();
                }
                mState = state;
                if (mFilter != null)
                    mFilter.destroy();
                filterFromState(state);
                mFilter.setup();
            }
        });
        mGLSurfaceView.requestRender();
    }

    public void setBitmap(final Bitmap bm) {
        if (bm != null && mBitmap != bm) {
            recycle();
            mBitmap = bm;
        }
    }

    public boolean isInCapture() {
        return mState == STATE_CAPTURE;
    }

    public void setFilterChangeListener(OnFilterChangeListener l) {
        mFilterChangeListener = l;
    }

    private void adjustPosition(int orientation, boolean flipHorizontal, boolean flipVertical) {
        Rotation rotation = Rotation.valueOf(orientation);
        CameraLog.i(TAG, "[adjustPosition] orientation=" + orientation + ",rotation=" + rotation);
        float[] textureCords = GLRotation.getRotation(rotation, flipHorizontal, flipVertical);
        mFilter.updateTexture(textureCords);
    }

    private void filterFromState(final int state) {
        if (mFilterMap.containsKey(state)) {
            mFilter = mFilterMap.get(state);
        } else {
            if (state == STATE_CAPTURE) {
                mFilter = new YUVFilter(mContext);
            } else if (state == STATE_PICTURE) {
                mFilter = new ImageFilter(mContext);
            }
            if (mFilter != null)
                mFilterMap.put(state, mFilter);
        }
    }

    private void recycle() {
        CameraLog.i(TAG, "recycle bitmap");
        if (mBitmap != null) {
            if (!mBitmap.isRecycled())
                mBitmap.recycle();
            mBitmap = null;
        }
    }

    public interface OnFilterChangeListener {
        void onFilterChanged(int filter);
    }
}
