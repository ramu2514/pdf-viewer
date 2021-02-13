package com.avrapps.pdfviewer;

import android.content.Context;

import androidx.multidex.MultiDex;

import com.orm.SugarApp;
import com.orm.SugarContext;

public class MultidexApplication extends SugarApp {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
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