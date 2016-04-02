package com.example.peridot.peridot;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenVideoActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private final String LOG_TAG = FetchVideoTask.class.getSimpleName();
    private static final boolean AUTO_HIDE = true;
    private final int TIMEOUT_CONNECTION = 5000;//5sec
    private final int TIMEOUT_SOCKET = 30000;//30sec
    private int position = 1;
    private MediaController mMediaController;
    private VideoView videoHolder;
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };
    private boolean mVisible;






    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaController = new MediaController(this);
        setContentView(R.layout.activity_fullscreen_video);
        mVisible = true;
        mContentView = findViewById(R.id.my_video_view);

        if (savedInstanceState != null)
        {
            position = savedInstanceState.getInt("position");
        }
        videoHolder = (VideoView)findViewById(R.id.my_video_view);
        videoHolder.setMediaController(mMediaController);

        videoHolder.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {
                if (videoHolder.isPlaying()) {
                    videoHolder.pause();
                    hide();
                    mMediaController.show(0);
                    position = videoHolder.getCurrentPosition();
                    return false;
                } else {

                    mMediaController.show(0);
                    videoHolder.seekTo(position);
                    videoHolder.start();
                    hide();
                    return false;
                }
            }
        });

        videoHolder.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                videoHolder.seekTo(1);
            }
        });

        videoHolder.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mediaPlayer,  int what, int extra) {
                Log.i(LOG_TAG, "on info");
                return true;
            }
        });

        videoHolder.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer,  int what, int extra) {
                Log.i(LOG_TAG, "on error");
                return true;
            }
        });

        if (position != 1)
        {
            videoHolder.seekTo(position);
            videoHolder.start();
        }
        else
        {
            //from beginning
            videoHolder.seekTo(1);
        }


        FetchVideoTask videoTask = new FetchVideoTask();
        videoTask.execute("video1.mp4");

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);

        if (videoHolder != null)
        {
            savedInstanceState.putInt("position", videoHolder.getCurrentPosition());
        }

        videoHolder.pause();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {

    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public class FetchVideoTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchVideoTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {

            // If there's no video url, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }
            String videoName = params[0];
            String videoLocalURI = getFilesDir().toString() + "/" + videoName;
            String[] resultStrs = new String[1];
            resultStrs[0] = videoLocalURI;
            File file = new File(videoLocalURI);
            if(file.exists()){
                Log.i(LOG_TAG, "Video found");

                return resultStrs;
            }
            else{
                Log.i(LOG_TAG, "Video not found");
                String videoURL = "http://www.tablero.hostingbahia.com.ar/Peridot/" + videoName;
                URL url = null;
                try {
                    url = new URL(videoURL);
                    long startTime = System.currentTimeMillis();
                    Log.i(LOG_TAG, "video download beginning: "+videoURL);

                    //Open a connection to that URL.
                    URLConnection ucon = url.openConnection();

                    //this timeout affects how long it takes for the app to realize there's a connection problem
                    ucon.setReadTimeout(TIMEOUT_CONNECTION);
                    ucon.setConnectTimeout(TIMEOUT_SOCKET);

                    //Define InputStreams to read from the URLConnection.
                    // uses 3KB download buffer  ¿3KB?
                    InputStream is = ucon.getInputStream();
                    BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
                    FileOutputStream outStream = openFileOutput(videoName, MODE_PRIVATE);
                    byte[] buff = new byte[5 * 1024];

                    //Read bytes (and store them) until there is nothing more to read(-1)
                    int len;
                    while ((len = inStream.read(buff)) != -1)
                    {
                        outStream.write(buff, 0, len);
                    }

                    //clean up
                    outStream.flush();
                    outStream.close();
                    inStream.close();
                    Log.i(LOG_TAG, "download completed in "
                            + ((System.currentTimeMillis() - startTime) / 1000)
                            + " sec");
                    return resultStrs;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                videoHolder.setVideoURI(Uri.parse(result[0]));
                videoHolder.requestFocus();
                videoHolder.start();

                //hide UI
                hide();
                mMediaController.show(0);
            }
        }
    }
}
