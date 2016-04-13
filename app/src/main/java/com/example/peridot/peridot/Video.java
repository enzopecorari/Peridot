package com.example.peridot.peridot;

import android.util.Log;

import java.io.File;

/**
 * Created by Enzo on 2/4/2016.
 */
public class Video {
    protected String link;
    protected String volume;
    protected String duration;
    protected String resolution_x;
    protected String resolution_y;
    protected long size;
    protected String videoLocalURI;
    protected boolean available = false;
    protected String videoName;
    protected boolean verified = false;
    private final String LOG_TAG = "VIDEO";

    public Video(String link, String volume)
    {
        this.link = link;
        this.volume = volume;
    }

    public Video(String link, String volume, String duration, String resolution_x, String resolution_y, long size, String videoLocalURI, String videoName)
    {
        this.link = link;
        this.volume = volume;
        this.duration = duration;
        this.resolution_x = resolution_x;
        this.resolution_y = resolution_y;
        this.size = size;
        this.videoLocalURI = videoLocalURI;
        this.videoName = videoName;
    }

    @Override
    public String toString(){
        return link + " - " + volume + " - " + duration + " - " + resolution_x + " - " + resolution_y + " - " + size + " - " + videoName;
    }

    public boolean isAvailable(){
        return available;
    }

    public boolean checkAvailability(){
        File file = new File(videoLocalURI);
        Log.i(LOG_TAG, "exist: " + file.length() + "server: " + size);
        if(file.exists()){
            if(size == file.length()){
                available = true;
            }else{
                file.delete();
                available = false;
            }

        }else{
            available = false;
        }
        return available;
    }

    public boolean delete(){
        File file = new File(videoLocalURI);
        return file.delete();
    }

}
