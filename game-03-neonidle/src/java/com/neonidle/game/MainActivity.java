package com.neonidle.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private NeonView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        view = new NeonView(this);
        setContentView(view);
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.save();
    }
}
