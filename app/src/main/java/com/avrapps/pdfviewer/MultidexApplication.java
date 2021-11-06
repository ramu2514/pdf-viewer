package com.avrapps.pdfviewer;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.google.android.play.core.splitcompat.SplitCompat;
import com.orm.SugarContext;

public class MultidexApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
        SplitCompat.install(this);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        SugarContext.init(this);
    }

    @Override
    public void onTerminate() {
        SugarContext.terminate();
        super.onTerminate();
    }
}