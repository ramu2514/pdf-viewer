package com.avrapps.pdfviewer.others_fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;

public class OthersFragment extends Fragment {

    MainActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.others_fragment, container, false);
        activity = (MainActivity) getActivity();
        setTitle();
        return view;
    }
    private void setTitle() {
        try{
            Toolbar toolbar=activity.findViewById(R.id.my_toolbar);
            activity.setSupportActionBar(toolbar);
            toolbar.setTitle(R.string.support_us);
        }catch (Exception ignored){}
    }
}
