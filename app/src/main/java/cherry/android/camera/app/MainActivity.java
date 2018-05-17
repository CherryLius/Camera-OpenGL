package cherry.android.camera.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import cherry.android.camera.ImageManager;
import cherry.android.camera.body.CaptureBody;
import cherry.android.camera.renderer.CaptureRenderer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    GLSurfaceView glSurfaceView;
    CaptureRenderer captureRenderer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        glSurfaceView = findViewById(R.id.gl_surface);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initCapture();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }
        findViewById(R.id.btn_capture).setOnClickListener(this);
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
            initCapture();
        }
    }

    void initCapture() {
        captureRenderer = new CaptureRenderer(glSurfaceView);
        captureRenderer.getCameraCompat().setCallback(new CaptureCallback() {
            @Override
            public void onCaptured(@NonNull CaptureBody captureBody) {
                ImageManager.instance().saveImage(MainActivity.this, captureBody, captureRenderer.getCameraCompat().getCamera());
            }

            @Override
            public void onBurstComplete() {

            }

            @Override
            public void onPictureSaved(@NonNull CaptureBody captureBody) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_capture:
                //captureRenderer.getCameraCompat().capture();
                startActivity(new Intent(this, ShapeActivity.class));
                break;
        }
    }
}
