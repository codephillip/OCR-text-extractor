package com.mytrendin.textrecognization;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SCANNER";
    Button button;
    ImageView imageView;
    private static final String LOG_TAG = "Barcode Scanner API";
    private static final int PHOTO_REQUEST = 10;
    private TextView scan;
    private TextRecognizer recognizer;
    private Uri imageuri;
    private static final int REQUEST_WRITE_PERMISSION = 20;
    private static final String SAVED_INSTANCE_URI = "uri";
    private static final String SAVED_INSTANCE_RESULT = "result";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.button);
        imageView = (ImageView) findViewById(R.id.imgview);
        scan = (TextView) findViewById(R.id.txtContent);
        if (savedInstanceState != null) {


            imageuri = Uri.parse(savedInstanceState.getString(SAVED_INSTANCE_URI));
            scan.setText(savedInstanceState.getString(SAVED_INSTANCE_RESULT));

        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
            }
        });

        recognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        Log.i("Hitesh", "" + recognizer);
        if (recognizer.isOperational()) {
            scan.setText("Could not set up the detector!");
        } else {
            scan.setText("Could not set up the detector!");
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {

            case REQUEST_WRITE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }


        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PHOTO_REQUEST && resultCode == RESULT_OK) {
            launchMediaScanIntent();
            try {
                CameraSource mCameraSource =
                        new CameraSource.Builder(getApplicationContext(), recognizer)
                                .setFacing(CameraSource.CAMERA_FACING_BACK)
                                .setRequestedPreviewSize(1280, 1024)
                                .setRequestedFps(3.0f)
                                .setAutoFocusEnabled(true)
                                //.setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                                // .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                                .build();
                Log.i("hiteshchauhan", "" + mCameraSource.getPreviewSize());

                Log.i("Hello", "" + imageuri);
                Scanner scanner = new Scanner();
                Bitmap bitmap = scanner.decodeBitmapUri(MainActivity.this, imageuri);
                Log.d(TAG, "onActivityResult: BRIGHTNESS: " + calculateBrightness(bitmap));

                // used in dim light
//                bitmap = changeBitmapContrastBrightness(bitmap, 2f, 1f);
                bitmap = changeBitmapContrastBrightness(bitmap, 2f, 1f);
                Log.d(TAG, "onActivityResult: BRIGHTNESS: " + calculateBrightness(bitmap));
                imageView.setImageBitmap(bitmap);

                if (recognizer.isOperational() && bitmap != null) {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<TextBlock> barcodes = recognizer.detect(frame);

                    Log.i("Hello", "" + barcodes.size());
                    for (int index = 0; index < barcodes.size(); index++) {
                        TextBlock code = barcodes.valueAt(index);

                        Log.d(TAG, "onActivityResult: " + code.getValue() + " ## " + code.getBoundingBox().centerY());
                        scan.setText(scan.getText() + code.getValue() + "#" + code.getBoundingBox().centerY() + "\n");

                        //Required only if you need to extract the type of barcode
                        //   int type = barcodes.valueAt(index).valueFormat;

                    }
                    if (barcodes.size() == 0) {
                        scan.setText("Scan Failed: Found nothing to scan");
                    }
                } else {
                    scan.setText("Could not set up the detector!");
                }
                Log.d(TAG, "onActivityResult: EXTRACTED: " + scan.getText());
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT)
                        .show();
                Log.e(LOG_TAG, e.toString());
            }
        }
    }

    public static Bitmap changeBitmapContrastBrightness(Bitmap bmp, float contrast, float brightness) {
        ColorMatrix cm = new ColorMatrix(new float[]{
                contrast, 0, 0, 0, brightness,
                0, contrast, 0, 0, brightness,
                0, 0, contrast, 0, brightness,
                0, 0, 0, 1, 0
        });

        Bitmap enchancedBitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
        Canvas canvas = new Canvas(enchancedBitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, 0, 0, paint);
        return enchancedBitmap;
    }

    public void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(), "picture.jpg");
        //  File photo = new File(getCacheDir(), "picture.jpg");
        imageuri = Uri.fromFile(photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageuri);
        startActivityForResult(intent, PHOTO_REQUEST);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (imageuri != null) {
            outState.putString(SAVED_INSTANCE_URI, imageuri.toString());
            outState.putString(SAVED_INSTANCE_RESULT, scan.getText().toString());
        }
        super.onSaveInstanceState(outState);
    }

    private void launchMediaScanIntent() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageuri);
        this.sendBroadcast(mediaScanIntent);
    }

    public int calculateBrightnessEstimate(android.graphics.Bitmap bitmap, int pixelSpacing) {
        int R = 0;
        int G = 0;
        int B = 0;
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        int n = 0;
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < pixels.length; i += pixelSpacing) {
            int color = pixels[i];
            R += Color.red(color);
            G += Color.green(color);
            B += Color.blue(color);
            n++;
        }
        return (R + B + G) / (n * 3);
    }

    public int calculateBrightness(android.graphics.Bitmap bitmap) {
        return calculateBrightnessEstimate(bitmap, 1);
    }
}
