package com.camera.scanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    final int CAMERA_REQUEST_CODE = 100;
    Rect viewportRect;
    View viewport;
    AppCompatButton captureACB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        viewport = findViewById(R.id.viewport);
        captureACB = findViewById(R.id.captureACB);
        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();

        // check if the camera permission is granted or not
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[] {Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
        else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            viewportRect = new Rect(viewport.getLeft(), viewport.getTop(), viewport.getRight(), viewport.getBottom());
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        camera = Camera.open();
        Camera.Parameters parameters;
        parameters = camera.getParameters();

        camera.setDisplayOrientation(90);
        parameters.setPreviewFrameRate(30);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(parameters);
        try {
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        camera.startPreview();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        if (camera != null) {
            // Set the camera parameters for capturing the image
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPictureFormat(ImageFormat.JPEG);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            camera.setParameters(parameters);

            // Get the dimensions and coordinates of the viewport view
            int viewportWidth = viewport.getWidth();
            int viewportHeight = viewport.getHeight();
            int viewportLeft = viewport.getLeft();
            int viewportTop = viewport.getTop();

            // Implement the picture callback to capture the image
            Camera.PictureCallback pictureCallback = (data, camera) -> {
                // Call method to capture the image within the viewport
                captureImageInViewport(data, camera, viewportLeft, viewportTop, viewportWidth, viewportHeight);
                // Restart the camera preview
                camera.startPreview();
            };

            // Capture the image when the button is pressed
            captureACB.setOnClickListener(v -> {
                // Capture the image with the defined picture callback
                camera.takePicture(null, null, pictureCallback);
            });

            // If you want to click picture immediately after opening app through widget then uncomment below code
            /*if (getIntent() != null && getIntent().getAction() != null && getIntent().getAction().equals("android.appwidget.action.APPWIDGET_UPDATE")) {
                // Capture the image with the defined picture callback
                camera.takePicture(null, null, pictureCallback);
            }*/
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }

    private void captureImageInViewport(byte[] data, Camera camera, int viewportLeft, int viewportTop, int viewportWidth, int viewportHeight) {
        // Convert byte data to Bitmap
        Bitmap fullImage = BitmapFactory.decodeByteArray(data, 0, data.length);

        // Calculate cropping dimensions and coordinates
        int imageWidth = fullImage.getWidth();
        int imageHeight = fullImage.getHeight();

        int cropLeft = (imageWidth - viewportWidth) / 2;
        int cropTop = (imageHeight - viewportHeight) / 2;

        // Calculate actual cropping dimensions
        int cropWidth = Math.min(viewportWidth, imageWidth);
        int cropHeight = Math.min(viewportHeight, imageHeight);

        // Check if cropping dimensions are valid
        if (cropWidth > 0 && cropHeight > 0 && cropLeft >= 0 && cropTop >= 0) {
            // Crop the image
            Bitmap croppedImage = Bitmap.createBitmap(fullImage, cropLeft, cropTop, cropWidth, cropHeight);

            // Rotate the cropped image by 90 degrees
            Bitmap rotatedCroppedImage = rotateBitmap(croppedImage);
            String imageFilePath = saveImageToStorage(rotatedCroppedImage);

            Toast.makeText(this, "Image saved to: " + imageFilePath, Toast.LENGTH_SHORT).show();

            // After clicking image then go to preview activity to view cropped image
            if (imageFilePath != null) {
                Intent intent = new Intent(CameraActivity.this, ImageActivity.class);
                intent.putExtra("imageFilePath", imageFilePath);
                startActivity(intent);
            } else {
                // Display an error message if imageFilePath is null
                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Display an error message if cropping dimensions are invalid
            Toast.makeText(this, "Invalid cropping dimensions", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    // Save image to Android/media folder like WhatsApp
    private String saveImageToStorage(Bitmap bitmap) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "image_" + timeStamp + ".jpg";

        // Get the root directory of internal storage
        File rootDir = Environment.getExternalStorageDirectory();

        // Create a directory named "Scanner" inside the Android/media/... directory
        File scannerDir = new File(rootDir, "Android/media/" + getPackageName() + "/Scanner");
        if (!scannerDir.exists()) {
            if (!scannerDir.mkdirs()) {
                Log.e("SaveImage", "Failed to create directory");
                return null;
            }
        }

        File imageFile = new File(scannerDir, imageFileName);

        try {
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();

            MediaScannerConnection.scanFile(
                    this,
                    new String[]{imageFile.getAbsolutePath()},
                    new String[]{"image/jpeg"},
                    null
            );

            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("SaveImage", "Failed to save image: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Intent intent = new Intent(CameraActivity.this, CameraActivity.class);
                finish();
                startActivity(intent);
            } else {
                Toast.makeText(this, "Camera permission required.!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}