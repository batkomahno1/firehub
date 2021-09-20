package com.example.kyrylo.firehub;
//delivery version 1

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;

import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

public class TweetListActivity extends AppCompatActivity {
    //this activity pulls the tweets from HFD and maps them to buttons
    //ToDo: IMPORTANT: hide the tokens better
    public static final String LOCATION_MESSAGE = R.string.app_web_path+".LOCATION_MESSAGE ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tweet_list);
        new setCallSelectionButtons().execute();
    }

    public void chooseLocation(View view){
        //method gets called when button is pressed
        //should inform the user of bad address
        Intent intent = new Intent(this,LocationListActivity.class);
        String location = new LocationExtractor(((Button)view).getText().toString()).getLocation();
        //check if address is valid and display next activity
        if (location != null) {
            intent.putExtra(LOCATION_MESSAGE, location);
            startActivity(intent);
        }else{
            //tell the user to choose a correct address
            Toast.makeText(this, getString(R.string.twitter_address_error),Toast.LENGTH_SHORT).show();
        }

    }

    private class setCallSelectionButtons extends AsyncTask<String, Void, String> {
        //a background task to map tweets to buttons
        @Override
        protected String doInBackground(String... params) {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(getString(R.string.twitter_consumer_key));
            builder.setOAuthConsumerSecret(getString(R.string.twitter_consumer_secret));
            AccessToken token = new AccessToken(getString(R.string.twitter_access_token), getString(R.string.twitter_access_token_secret));
            Twitter twitter = new TwitterFactory(builder.build()).getInstance(token);
            try {
                ResponseList<twitter4j.Status> tweets;
                tweets = new TweetParser(twitter.getUserTimeline(getString(R.string.twitter_handle))).getParsedTimeline();
                runOnUiThread(() -> {
                    ViewGroup viewGroup = (ViewGroup) getWindow().getDecorView();
                    ArrayList<View> buttons = viewGroup.getTouchables();
                    Iterator<twitter4j.Status> iter = tweets.iterator();
                    for (View view : buttons) {
                        if(view instanceof Button)((Button) view).setText(iter.next().getText());//Todo: make dynamic button size
                    }
                });
            } catch (Exception e) {
                //Todo: improve this error handling, carefull concurrency
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(TweetListActivity.this,"No network!",Toast.LENGTH_LONG).show());
                TweetListActivity.this.finish();
            }
            return null;
        }
    }
}