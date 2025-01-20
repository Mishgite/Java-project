package com.example.java_project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_IMAGE_REQUEST = 1;

    private ImageView imageView;
    private TextView textViewResults;
    private Button buttonSelectImage, buttonRecognizeText, buttonDetectObjects;
    private Bitmap selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        textViewResults = findViewById(R.id.textViewResults);
        buttonSelectImage = findViewById(R.id.buttonSelectImage);
        buttonRecognizeText = findViewById(R.id.buttonRecognizeText);
        buttonDetectObjects = findViewById(R.id.buttonDetectObjects);

        buttonSelectImage.setOnClickListener(v -> selectImage());

        buttonRecognizeText.setOnClickListener(v -> recognizeText());

        buttonDetectObjects.setOnClickListener(v -> detectObjects());
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
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(this::displayRecognizedText)
                .addOnFailureListener(e -> textViewResults.setText("Ошибка распознавания: " + e.getMessage()));
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
                .addOnSuccessListener(this::displayDetectedObjects)
                .addOnFailureListener(e -> textViewResults.setText("Ошибка распознавания объектов: " + e.getMessage()));
    }

    private void displayRecognizedText(Text result) {
        StringBuilder recognizedText = new StringBuilder();

        for (Text.TextBlock block : result.getTextBlocks()) {
            recognizedText.append(block.getText()).append("\n");
        }

        if (recognizedText.length() > 0) {
            textViewResults.setText(recognizedText.toString());
        } else {
            textViewResults.setText("Текст не распознан.");
        }
    }

    private void displayDetectedObjects(List<DetectedObject> objects) {
        StringBuilder detectedObjects = new StringBuilder();

        for (DetectedObject object : objects) {
            detectedObjects.append("Объект ID: ").append(object.getTrackingId()).append("\n");
            for (DetectedObject.Label label : object.getLabels()) {
                detectedObjects.append(" - ")
                        .append(label.getText())
                        .append(" (").append(label.getConfidence()).append(")\n");
            }
            detectedObjects.append("\n");
        }

        if (detectedObjects.length() > 0) {
            textViewResults.setText(detectedObjects.toString());
        } else {
            textViewResults.setText("Объекты не распознаны.");
        }
    }
}
