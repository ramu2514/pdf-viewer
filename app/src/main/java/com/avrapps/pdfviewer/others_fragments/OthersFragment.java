package com.avrapps.pdfviewer.others_fragments;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.play.core.splitinstall.SplitInstallException;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;

import java.util.concurrent.atomic.AtomicInteger;

public class OthersFragment extends Fragment {

    MainActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.others_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = (MainActivity) getActivity();
        TextView textView = view.findViewById(R.id.text_view);
        Button install = view.findViewById(R.id.install_button);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(Html.fromHtml(getResources().getString(R.string.install_module)));
        setTitle();
        install.setOnClickListener(v -> checkAndInstallModule());

    }

    private void setTitle() {
        try {
            Toolbar toolbar = activity.findViewById(R.id.my_toolbar);
            activity.setSupportActionBar(toolbar);
            toolbar.setTitle(R.string.doc_scanner);
        } catch (Exception ignored) {
        }
    }

    private void checkAndInstallModule() {
        activity.findViewById(R.id.text_view).setVisibility(View.GONE);
        activity.findViewById(R.id.install_button).setVisibility(View.GONE);
        AtomicInteger mySessionId = new AtomicInteger();
        SplitInstallManager splitInstallManager = SplitInstallManagerFactory.create(activity);

        SplitInstallRequest request = SplitInstallRequest.newBuilder()
                .addModule("DocumentScannerAndSync")
                .build();
        CircularProgressIndicator progressIndicator = activity.findViewById(R.id.progressIndicator);
        TextView progressView = activity.findViewById(R.id.progressText);
        SplitInstallStateUpdatedListener listener = state -> {
            if (state.status() == SplitInstallSessionStatus.FAILED) {
                handleError(state.errorCode());
                return;
            }
            if (state.sessionId() == mySessionId.get()) {
                switch (state.status()) {
                    case SplitInstallSessionStatus.DOWNLOADING:
                        progressIndicator.setVisibility(View.VISIBLE);
                        int progress = (int) (state.bytesDownloaded() * 100 / state.totalBytesToDownload());
                        progressIndicator.setProgressCompat(progress, true);
                        progressView.setText(getString(R.string.module_download_progress, "" + progress));
                        break;

                    case SplitInstallSessionStatus.INSTALLED:
                        progressIndicator.setVisibility(View.GONE);
                        progressView.setVisibility(View.GONE);
                        activity.openScannerFragment();
                        //Module is downloaded successfully
                        break;
                }
            }
        };
        splitInstallManager.registerListener(listener);
        splitInstallManager.startInstall(request)
                .addOnSuccessListener(mySessionId::set)
                .addOnFailureListener(exception -> {
                    int errorCode = ((SplitInstallException) exception).getErrorCode();
                    handleError(errorCode);
                });
    }

    private void handleError(int errorCode) {
        switch (errorCode) {
            case SplitInstallErrorCode.NETWORK_ERROR:
                Toast.makeText(activity, R.string.module_failed_no_network, Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(activity, getString(R.string.module_failed, "" + errorCode), Toast.LENGTH_LONG).show();
        }
    }
}
