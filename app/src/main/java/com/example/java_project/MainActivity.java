package com.example.java_project;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_IMAGE_REQUEST = 1;

    private PreviewView cameraPreview;
    private ImageView imageView;
    private TextView textViewLiveResults, textViewResults;
    private Button buttonSelectImage, buttonRecognizeText, buttonDetectObjects, buttonCopyResult;

    private Bitmap selectedImage;
    private ExecutorService cameraExecutor;
    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraPreview = findViewById(R.id.cameraPreview);
        imageView = findViewById(R.id.imageView);
        textViewLiveResults = findViewById(R.id.textViewLiveResults);
        textViewResults = findViewById(R.id.textViewResults);
        buttonSelectImage = findViewById(R.id.buttonSelectImage);
        buttonRecognizeText = findViewById(R.id.buttonRecognizeText);
        buttonDetectObjects = findViewById(R.id.buttonDetectObjects);
        buttonCopyResult = findViewById(R.id.buttonCopyResult);

        cameraExecutor = Executors.newSingleThreadExecutor();

        textRecognizer = TextRecognition.getClient(new TextRecognizerOptions.Builder().build());

        startCamera();

        buttonSelectImage.setOnClickListener(v -> selectImage());
        buttonRecognizeText.setOnClickListener(v -> recognizeText());
        buttonDetectObjects.setOnClickListener(v -> detectObjects());
        buttonCopyResult.setOnClickListener(v -> copyResultToClipboard());
    }


    @OptIn(markerClass = ExperimentalGetImage.class)
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    InputImage image = InputImage.fromMediaImage(
                            imageProxy.getImage(),
                            imageProxy.getImageInfo().getRotationDegrees()
                    );

                    textRecognizer.process(image)
                            .addOnSuccessListener(text -> runOnUiThread(() -> textViewLiveResults.setText(text.getText())))
                            .addOnFailureListener(e -> Log.e("MainActivity", "Ошибка анализа", e))
                            .addOnCompleteListener(task -> imageProxy.close());
                });

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Toast.makeText(this, "Ошибка запуска камеры: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, SELECT_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                selectedImage = BitmapFactory.decodeStream(imageStream);
                imageView.setImageBitmap(selectedImage);

                buttonRecognizeText.setEnabled(true);
                buttonDetectObjects.setEnabled(true);
                buttonCopyResult.setEnabled(false);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void recognizeText() {
        if (selectedImage == null) {
            textViewResults.setText("Пожалуйста, выберите изображение.");
            return;
        }

        InputImage image = InputImage.fromBitmap(selectedImage, 0);
        textRecognizer.process(image)
                .addOnSuccessListener(result -> {
                    textViewResults.setText(result.getText());
                    buttonCopyResult.setEnabled(true);
                })
                .addOnFailureListener(e -> textViewResults.setText("Ошибка распознавания текста."));
    }

    private void detectObjects() {
        if (selectedImage == null) {
            textViewResults.setText("Пожалуйста, выберите изображение.");
            return;
        }

        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build();

        ObjectDetector detector = ObjectDetection.getClient(options);

        InputImage image = InputImage.fromBitmap(selectedImage, 0);
        detector.process(image)
                .addOnSuccessListener(objects -> {
                    StringBuilder results = new StringBuilder();
                    for (DetectedObject object : objects) {
                        for (DetectedObject.Label label : object.getLabels()) {
                            results.append(label.getText()).append("\n");
                        }
                    }
                    textViewResults.setText(results.toString());
                    buttonCopyResult.setEnabled(true);
                })
                .addOnFailureListener(e -> textViewResults.setText("Ошибка распознавания объектов."));
    }

    private void copyResultToClipboard() {
        String text = textViewResults.getText().toString();
        if (!text.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Результаты", text));
            Toast.makeText(this, "Текст скопирован", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Нет текста для копирования", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        textRecognizer.close();
    }
}
