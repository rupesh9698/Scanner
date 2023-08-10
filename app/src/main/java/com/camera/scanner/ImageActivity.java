package com.camera.scanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

public class ImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        AppCompatImageView imageACIV = findViewById(R.id.imageACIV);

        // Get the image file path from the Intent
        String imageFilePath = getIntent().getStringExtra("imageFilePath");

        // Load and display the image using the image file path
        if (imageFilePath != null) {
            Bitmap imageBitmap = BitmapFactory.decodeFile(imageFilePath);
            imageACIV.setImageBitmap(imageBitmap);
        }
    }
}