package cherry.android.camera.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import cherry.android.camera.renderer.CaptureRenderer;

public class MainActivity extends AppCompatActivity {

    GLSurfaceView glSurfaceView;
    CaptureRenderer captureRenderer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        glSurfaceView = findViewById(R.id.gl_surface);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            captureRenderer = new CaptureRenderer(glSurfaceView);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (captureRenderer != null) {
            captureRenderer.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (captureRenderer != null) {
            captureRenderer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (captureRenderer != null) {
            captureRenderer.destroy();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            captureRenderer = new CaptureRenderer(glSurfaceView);
        }
    }
}
