package com.example.task5;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private EditText journalIdEditText;
    private Button downloadButton, viewButton, deleteButton;
    private String filePath;

    private static final String PREFS_NAME = "AppPrefs";
    private static final String SHOW_POPUP_KEY = "showPopup";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        journalIdEditText = findViewById(R.id.journal_id);
        downloadButton = findViewById(R.id.download_button);
        viewButton = findViewById(R.id.view_button);
        deleteButton = findViewById(R.id.delete_button);

        // Проверка разрешений
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        // Загрузка файла
        downloadButton.setOnClickListener(view -> {
            String journalId = journalIdEditText.getText().toString().trim();
            if (!journalId.isEmpty()) {
                new DownloadFileTask().execute("https://ntv.ifmo.ru/file/journal/" + journalId + ".pdf");}
            else {
                Toast.makeText(this, "Введите ID журнала", Toast.LENGTH_SHORT).show();
            }
        });

        // Просмотр файла
        viewButton.setOnClickListener(view -> {
            if (filePath != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(filePath)), "application/pdf");
                startActivity(intent);
            } else {
                Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show();
            }
        });

        // Удаление файла
        deleteButton.setOnClickListener(view -> {
            File file = new File(filePath);
            if (file.exists() && file.delete()) {
                Toast.makeText(this, "Файл удален", Toast.LENGTH_SHORT).show();
                filePath = null;
                viewButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.GONE);
            } else {
                Toast.makeText(this, "Ошибка удаления файла", Toast.LENGTH_SHORT).show();
            }
        });

        // Проверка настроек для всплывающего окна
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean showPopup = settings.getBoolean(SHOW_POPUP_KEY, true);
        if (showPopup) {
            showWelcomePopup();
        }
    }

    private void showWelcomePopup() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_layout);

        CheckBox dontShowAgain = dialog.findViewById(R.id.dont_show_again);
        Button okButton = dialog.findViewById(R.id.ok_button);

        okButton.setOnClickListener(v -> {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(SHOW_POPUP_KEY, !dontShowAgain.isChecked()); // Исправлено: добавлены круглые скобки
            editor.apply();
            dialog.dismiss();
        });

        dialog.show();
    }

    private class DownloadFileTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(String... urls) {
            String fileURL = urls[0];
            try {
                URL url = new URL(fileURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                String contentType = connection.getContentType();
                if (contentType.equals("application/pdf")) {
                    InputStream input = new BufferedInputStream(connection.getInputStream());
                    File dir = new File(Environment.getExternalStorageDirectory(), "NtvJournal");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    filePath = dir.getPath() + "/" + connection.getURL().getFile().substring(connection.getURL().getFile().lastIndexOf('/') + 1);
                    FileOutputStream output = new FileOutputStream(filePath);

                    byte data[] = new byte[1024];
                    int count;
                    while ((count = input.read(data)) != -1) {
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    input.close();
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                viewButton.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "Файл загружен", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Файл не найден", Toast.LENGTH_SHORT).show();
            }
        }
    }
}