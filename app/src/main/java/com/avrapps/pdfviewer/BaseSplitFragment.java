package com.avrapps.pdfviewer;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.play.core.splitcompat.SplitCompat;

public class BaseSplitFragment extends Fragment {
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        SplitCompat.install(context);
    }
}
