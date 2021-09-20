package com.example.kyrylo.firehub;
//delivery version 1
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class LocationExtractor {
    //this class extracts the exact location of the call from the tweet
    private String cleanLocation;
    LocationExtractor(String location){
        //ToDo:find/buy an address parsing service
        //parses location in the constructor
        this.cleanLocation=this.extractLocation(location);
    }
    private String extractLocation(String location){
        //Todo:create a fool-proof location extraction algorithm
        //ToDo:get rid of magic strings
        location = location.toLowerCase();
        //return on empty address
        if(location.contains("loc:") && location.contains("@")) {
            location = location.substring(location.indexOf("loc:"),location.indexOf('@'));
        }else return null;
        //replace redundant strings
        List<String> delStrs = Stream.of("block", "ham", "loc", ":","@").collect(Collectors.toList());
        for (String delStr : delStrs) location = location.replaceAll(delStr, "");
        //get rid of extra spaces
        location=location.replaceAll("  "," ");
        location=location.trim();

        return location.replaceAll(" ","").isEmpty() ? null:location+", hamilton, ontario, canada";//Todo: get rid of the magic string
    }
    String getLocation(){
        return this.cleanLocation;
    }
}
