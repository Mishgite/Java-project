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
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_IMAGE_REQUEST = 1;

    private ImageView imageView;
    private TextView textViewResults;
    private Button buttonSelectImage, buttonRecognizeText;
    private Bitmap selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        textViewResults = findViewById(R.id.textViewResults);
        buttonSelectImage = findViewById(R.id.buttonSelectImage);
        buttonRecognizeText = findViewById(R.id.buttonRecognizeText);

        // Обработчик кнопки выбора изображения
        buttonSelectImage.setOnClickListener(v -> selectImage());

        // Обработчик кнопки распознавания текста
        buttonRecognizeText.setOnClickListener(v -> recognizeText());
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

                // Включаем кнопку распознавания текста после того как изображение выбрано
                buttonRecognizeText.setEnabled(true);
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

        // Преобразуем Bitmap в InputImage
        InputImage image = InputImage.fromBitmap(selectedImage, 0);

        // Создаем распознаватель текста с настройками по умолчанию
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Обрабатываем изображение
        recognizer.process(image)
                .addOnSuccessListener(this::displayRecognizedText)
                .addOnFailureListener(e -> textViewResults.setText("Ошибка распознавания: " + e.getMessage()));
    }


    private void displayRecognizedText(Text result) {
        StringBuilder recognizedText = new StringBuilder();

        // Проходим по всем строкам текста
        for (Text.TextBlock block : result.getTextBlocks()) {
            recognizedText.append(block.getText()).append("\n");
        }

        // Отображаем результат в TextView
        if (recognizedText.length() > 0) {
            textViewResults.setText(recognizedText.toString());
        } else {
            textViewResults.setText("Текст не распознан.");
        }
    }
}
