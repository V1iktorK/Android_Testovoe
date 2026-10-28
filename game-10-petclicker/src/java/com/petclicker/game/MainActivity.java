package com.petclicker.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private PetView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        view = new PetView(this);
        setContentView(view);
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.saveNow();
    }
}
