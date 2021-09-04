package com.avrapps.pdfviewer.results_fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.avrapps.pdfviewer.R;

import java.util.ArrayList;

public class ThumbnailSelectionActivity extends AppCompatActivity {

    PdfThumbnailAdapter adapter;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home || id == R.id.done) {
            Intent returnIntent = new Intent();
            ArrayList<Integer> pageNumbers = (adapter != null) ? adapter.getPageNumbers() : new ArrayList<>();
            returnIntent.putExtra("result",pageNumbers);
            returnIntent.putExtra("pageNumbers",adapter.getItemCount());
            setResult(Activity.RESULT_OK,returnIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_thumbnails, menu);
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_selection_fragment);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        toolbar.setTitle(R.string.select_pages);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayUseLogoEnabled(true);
        }
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String filePath = bundle.getString("filePath");
            RecyclerView recyclerView = findViewById(R.id.recycler_view);
            final int numberOfColumns = getResources().getInteger(R.integer.grid_columns);
            recyclerView.setLayoutManager(new GridLayoutManager(this, numberOfColumns));
            try {
                 adapter = new PdfThumbnailAdapter(filePath, this);
                recyclerView.setAdapter(adapter);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}