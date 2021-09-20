package com.example.kyrylo.firehub;
//delivery version 1
/*
 * Authors: Group 4, CCPS406 - Friday, Ryerson University, Fall 2018
 */

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    //this is where the application starts
    private static final int SPLASH_DELAY = 3000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //delay the screen
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, TweetListActivity.class);
            MainActivity.this.startActivity(intent);
            MainActivity.this.finish();
        }, SPLASH_DELAY);

    }


}
