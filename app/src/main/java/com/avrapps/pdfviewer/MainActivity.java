package com.avrapps.pdfviewer;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.avrapps.pdfviewer.constants.AppConstants;
import com.avrapps.pdfviewer.fragments.OthersFragment;
import com.avrapps.pdfviewer.fragments.SettingsFragment;
import com.avrapps.pdfviewer.library_fragment.LibraryFragment;
import com.avrapps.pdfviewer.utils.AppRater;
import com.avrapps.pdfviewer.utils.BillinUtils;
import com.avrapps.pdfviewer.utils.MessagingUtility;
import com.avrapps.pdfviewer.utils.MiscUtils;
import com.avrapps.pdfviewer.utils.PermissionUtils;
import com.avrapps.pdfviewer.utils.PreferenceUtil;
import com.avrapps.pdfviewer.utils.SharingUtils;
import com.avrapps.pdfviewer.viewer_fragment.DocumentFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    PermissionUtils permissionUtils;
    String fragmentOpen = "";
    DocumentFragment documentFragment;
    boolean shouldExitOnBackButton = false;
    LibraryFragment libraryFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MiscUtils.setTheme(this);
        setContentView(R.layout.main_activity);
        permissionUtils = new PermissionUtils();
        if (permissionUtils.checkRunTimePermission(this)) {
            restoreOpenFragmentOnReopen(savedInstanceState);
        }
        AppRater.app_launched(this);
        setSupportActionBar(findViewById(R.id.my_toolbar));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!BillinUtils.isApplicationBought(this)) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.pro_option) {
            MessagingUtility.showBuyApplicationDailog(this);
        }
        return true;
    }

    private void restoreOpenFragmentOnReopen(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            String message = savedInstanceState.getString("fragmentOpen");
            if (message != null && message.equals("SettingsFragment")) {
                openSettingsFragment(null);
                return;
            }
        }
        continueOperations();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionUtils.validatePermissions(this, requestCode, permissions, grantResults);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("fragmentOpen", fragmentOpen);
        super.onSaveInstanceState(outState);
    }

    public void openSettingsFragment(View view) {
        if (fragmentOpen.equals("SettingsFragment")) return;
        fragmentOpen = "SettingsFragment";
        openFragment(new SettingsFragment());
    }

    public void continueOperations() {
        openLibraryFragment(null);
        Uri uri = getIntent().getData();
        if (uri != null) {
            shouldExitOnBackButton = true;
            MiscUtils.openDoc(uri, this, new ArrayList<>());
        }
    }

    public void openLibraryFragment(View view) {
        if (fragmentOpen.equals("LibraryFragment")) return;
        fragmentOpen = "LibraryFragment";
        libraryFragment = new LibraryFragment();
        openFragment(libraryFragment);
    }

    public void openOthersFragment(View view) {
        if (fragmentOpen.equals("OthersFragment")) return;
        fragmentOpen = "OthersFragment";
        openFragment(new OthersFragment());
    }

    private void openFragment(Fragment f) {
        Log.d("fragmentOpen", fragmentOpen);
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.frame_layout, f)
                .commit();
    }

    public void openDocumentFragment(String path, ArrayList<String> selectedFiles) {
        fragmentOpen = "DocumentFragment";
        documentFragment = new DocumentFragment();
        Bundle bundle = new Bundle();
        bundle.putString("path", path);
        bundle.putStringArrayList("additionalFiles", selectedFiles);
        bundle.putBoolean("shouldQuit", shouldExitOnBackButton);
        documentFragment.setArguments(bundle);
        //DocFragment f = new DocFragment();f.setArguments(bundle);openFragment(f);
        openFragment(documentFragment);
    }


    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
        if (fragment instanceof LibraryFragment && libraryFragment != null) {
            libraryFragment.onBackPressed();
        } else if (fragment instanceof DocumentFragment) {
            if (documentFragment != null && documentFragment.onBackPressed()) {
                if (shouldExitOnBackButton) {
                    finish();
                } else {
                    openLibraryFragment(null);
                    findViewById(R.id.my_toolbar).setVisibility(View.VISIBLE);
                    findViewById(R.id.buttonPanel).setVisibility(View.VISIBLE);
                }
            }
        } else {
            this.finish();
        }
    }

    public void performAction(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.share_app:
                SharingUtils.shareApp(this);
                break;
            case R.id.more_apps:
                SharingUtils.openDeveloperPage(this);
                break;
            case R.id.rate_app:
                SharingUtils.rateApp(this, this.getPackageName());
                break;
            case R.id.install_doc_scanner:
                SharingUtils.rateApp(this, "com.avrapps.documentscanner");
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        Log.e("Main", "Response Received on Activity" + resultCode);
        if (requestCode == 4010) {
            if (resultCode == RESULT_OK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Uri treeUri = resultData.getData();
                DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
                Log.e("Main", "Doc Id : " + pickedDir.getUri());
                Set<String> removableStorages = PreferenceUtil.getStringSet(this, AppConstants.REMOVABLE_STORAGES);
                String sdcard = "";
                for (String storage : removableStorages) {
                    sdcard = new File(storage).getName();
                    if (new File(storage).getName().equals(pickedDir.getName())) {
                        Log.e("Main", "Doc Id matched: " + pickedDir.getUri());
                        grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
                        if (fragment instanceof DocumentFragment) {
                            documentFragment.checkSDCardPermission();
                        }
                        return;
                    }
                }
                MessagingUtility.showSdcardAccessDialog(this, "Incorrect path Chosen",
                        "Please select root folder of SDCARD " + sdcard + ". " +
                                "\nWith out this permission you can't add edit PDF & annotations/form filling will not work",
                        "Grant Permission", (String returnValue) -> PermissionUtils.takeCardUriPermission(this), false);
            }
        }
    }
}
