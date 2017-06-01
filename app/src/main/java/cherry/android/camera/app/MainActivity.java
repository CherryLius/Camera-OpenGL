package cherry.android.camera.app;

import android.opengl.GLSurfaceView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import cherry.android.camera.renderer.CaptureRenderer;

public class MainActivity extends AppCompatActivity {

    GLSurfaceView glSurfaceView;
    CaptureRenderer captureRenderer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        glSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface);
        captureRenderer = new CaptureRenderer(this, glSurfaceView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        captureRenderer.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        captureRenderer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureRenderer.destroy();
    }
}
