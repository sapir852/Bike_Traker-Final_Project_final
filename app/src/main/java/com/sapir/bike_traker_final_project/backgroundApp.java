package com.sapir.bike_traker_final_project;

import android.app.Application;

public class backgroundApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Timer.initHelper();
    }
}
