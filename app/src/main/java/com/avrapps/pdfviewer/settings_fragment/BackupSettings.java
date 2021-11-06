package com.avrapps.pdfviewer.settings_fragment;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.others_fragments.OthersFragment;
import com.avrapps.pdfviewer.utils.MiscUtils;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;

public class BackupSettings extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MiscUtils.setTheme(this);
        setContentView(R.layout.layout_backup_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setTitle("Backup Settings");
        SplitInstallManager splitInstallManager = SplitInstallManagerFactory.create(this);
        if (!splitInstallManager.getInstalledModules().contains("DocumentScannerAndSync")) {
            openFragment(new OthersFragment());
        } else {
            openScannerFragment();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void openFragment(Fragment fragment) {
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit();
    }

    private void openScannerFragment() {
        Fragment fragment;
        try {
            fragment = (Fragment) Class.forName("com.avrapps.documentscanner.ScanSettingsFragment").newInstance();
        } catch (Exception e) {
            Toast.makeText(this, R.string.cant_start_feature, Toast.LENGTH_SHORT).show();
            return;
        }
        openFragment(fragment);
    }

}
