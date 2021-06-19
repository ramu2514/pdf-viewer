package com.avrapps.pdfviewer;

import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.avrapps.pdfviewer.utils.MiscUtils;
import com.avrapps.pdfviewer.utils.PathUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TextViewerActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        MiscUtils.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_viewer);
        String text = readFileAsString();
        displayContent(text);
    }

    private String readFileAsString() {
        Uri uri = getIntent().getData();
        if (uri != null) {
            try {
                File path = PathUtils.getPathNew(this, uri, new StringBuilder());
                Toolbar toolbar = findViewById(R.id.my_toolbar);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    toolbar.setTitle(path.getName());
                }
                InputStream is = new FileInputStream(path);
                BufferedReader buf = new BufferedReader(new InputStreamReader(is));

                String line = buf.readLine();
                StringBuilder sb = new StringBuilder();

                while (line != null) {
                    sb.append(line).append("\n");
                    line = buf.readLine();
                }
                return sb.toString();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return getString(R.string.cant_open_file);
    }

    private void displayContent(String text) {
        TextView editor = findViewById(R.id.editor);
        editor.setText(text);
    }

}
