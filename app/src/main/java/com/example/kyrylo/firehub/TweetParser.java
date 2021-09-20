package com.example.kyrylo.firehub;
//delivery version 1

import java.util.Iterator;

import twitter4j.ResponseList;
import twitter4j.Status;

class TweetParser {
    //this class cleans up the Twitter feed
    private ResponseList<twitter4j.Status> timeline;
    TweetParser(ResponseList<Status> timeline){
        this.timeline=timeline;
        //calss the cleaning method
        this.parseTweets();
    }
    private void parseTweets(){
        //ToDo: improve the parser algorithm
        //removes tweets containing the word update
        this.timeline.removeIf(temp -> temp.getText().toLowerCase().contains("update"));
    }
    ResponseList<twitter4j.Status> getParsedTimeline() {
        //a public getter method
        return this.timeline;
    }
}