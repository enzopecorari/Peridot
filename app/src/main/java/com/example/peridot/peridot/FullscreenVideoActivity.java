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
import android.widget.ProgressBar;
import android.widget.TextView;
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
import java.util.LinkedList;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenVideoActivity extends AppCompatActivity {

    private final String LOG_TAG = FetchVideoTask.class.getSimpleName();
    private static final boolean AUTO_HIDE = true;
    private final int TIMEOUT_CONNECTION = 5000;//5sec
    private final int TIMEOUT_SOCKET = 30000;//30sec
    private VideoView videoHolder;
    private boolean showingProgress = false;


    private LinkedList<Video> currentVideos = new LinkedList<>();
    private int position = 1;
    private int currentVideo = 0;
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen_video);
        mContentView = findViewById(R.id.my_video_view);

        if (savedInstanceState != null)
        {
            position = savedInstanceState.getInt("position");
        }
        videoHolder = (VideoView)findViewById(R.id.my_video_view);

        videoHolder.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {
                if (videoHolder.isPlaying()) {
                    videoHolder.pause();
                    position = videoHolder.getCurrentPosition();
                    mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                    return false;
                } else {
                    videoHolder.seekTo(position);
                    videoHolder.start();
                    mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                    return false;
                }

            }
        });

        videoHolder.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                currentVideo++;
                if(currentVideo == currentVideos.size()){
                    currentVideo = 0;
                }
                videoListManager();
            }
        });

        videoHolder.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
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


        FetchVideoListTask videoListTask = new FetchVideoListTask();
        videoListTask.execute("123456"); //id app

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

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    public class FetchVideoTask extends AsyncTask<String, String, String[]> {

        private final String LOG_TAG = FetchVideoTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {

            // If there's no video url, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }
            String videoLocalURI = params[1];
            String[] resultStrs = new String[1];
            String videoName = params[3];
            long size = (Long.parseLong(params[2]));
            resultStrs[0] = videoLocalURI;
            File file = new File(videoLocalURI);
            if(file.exists()){
                Log.i(LOG_TAG, "Video found SHOULD NOT HAPPEN");
                return resultStrs;
            }
            else{
                Log.i(LOG_TAG, "Video not found");
                String videoURL = params[0];
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
                    InputStream is = ucon.getInputStream();
                    BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
                    FileOutputStream outStream = openFileOutput(videoName, MODE_PRIVATE);
                    byte[] buff = new byte[5 * 1024];

                    //Read bytes (and store them) until there is nothing more to read(-1)
                    int len;
                    long downloaded = 0;
                    while ((len = inStream.read(buff)) != -1)
                    {
                        outStream.write(buff, 0, len);
                        downloaded += len;
                        String t = "Downloading " + videoName + " " + (int)(((float)downloaded / (float)size) * 100 )  + "%";
                        publishProgress(t, String.valueOf(downloaded), String.valueOf(size));
                        Log.i(LOG_TAG, "downloaded: " + downloaded + "/" + size);
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

            return null;
        }

        protected void onProgressUpdate(String... progress) {
            ((TextView)findViewById(R.id.progressText)).setText(progress[0]);
            ((ProgressBar)findViewById(R.id.progressBar)).setMax(Integer.parseInt(progress[2]));
            ((ProgressBar)findViewById(R.id.progressBar)).setProgress(Integer.parseInt(progress[1]));
        }


    @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                if(showingProgress){
                    showingProgress = false;
                    (findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
                    (findViewById(R.id.progressText)).setVisibility(View.INVISIBLE);
                    videoListManager();
                }
            }
        }
    }

    public class FetchVideoListTask extends AsyncTask<String, Void, Video[]> {

        private final String LOG_TAG = FetchVideoListTask.class.getSimpleName();

        /**
         * Take the String representing the complete list of videos in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         */
        private Video[] getVideoListDataFromJson(String videoListJsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "video-list";
            final String OWM_LINK = "link";
            final String OWM_VOLUME = "volume";
            final String OWM_DURATION = "duration";
            final String OWM_RESOLUTION_X = "resolution_x";
            final String OWM_RESOLUTION_Y = "resolution_y";
            final String OWM_FILESIZE = "filesize";
            final String OWM_VIDEONAME = "video-name";


            JSONObject videoListJson = new JSONObject(videoListJsonStr);
            JSONArray videoArray = videoListJson.getJSONArray(OWM_LIST);
            Log.i(LOG_TAG, videoArray.length()+" tamaño del array");
            Video[] resultStrs = new Video[videoArray.length()];
            for(int i = 0; i < videoArray.length(); i++) {

                String link;
                String volume;
                String duration;
                long size;
                String resolution_x;
                String resolution_y;
                String videoName;

                // Get the JSON object representing the video
                JSONObject video = videoArray.getJSONObject(i);

                link = video.getString(OWM_LINK);
                volume = video.getString(OWM_VOLUME);
                duration = video.getString(OWM_DURATION);
                resolution_x = video.getString(OWM_RESOLUTION_X);
                resolution_y = video.getString(OWM_RESOLUTION_Y);
                size = video.getLong(OWM_FILESIZE);
                videoName = video.getString(OWM_VIDEONAME);

                resultStrs[i] = new Video(link, volume, duration, resolution_x, resolution_y, size, getFilesDir().toString() + "/" + videoName, videoName);
            }
            return resultStrs;

        }

        @Override
        protected Video[] doInBackground(String... params) {

            // If there's no id, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }
            String id = params[0];

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String videoListJsonStr = null;

            try {
                // Construct the URL for the API query
                final String API_BASE_URL =
                        "http://tableroweb.com.ar/Peridot/video-list.php?";
                final String ID_PARAM = "id";

                Uri builtUri = Uri.parse(API_BASE_URL).buildUpon()
                        .appendQueryParameter(ID_PARAM, id)
                        .build();

                URL url = new URL(builtUri.toString());

                // Create the request to API, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                videoListJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getVideoListDataFromJson(videoListJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the data.
            return null;
        }

        @Override
        protected void onPostExecute(Video[] result) {



            if (result != null) {
                for(Video video : result) {
                    Log.i(LOG_TAG, "Videos agrego " + video.videoName);
                    currentVideos.add(video);
                    if(!video.checkAvailability()){
                        new FetchVideoTask().execute(video.link, video.videoLocalURI, ((Long) video.size).toString(), video.videoName);
                    }else{
                        video.verified = true;
                    }
                    Log.i(LOG_TAG, video.toString());
                }

                //call video manager
                videoListManager();
            }
            localStorageCleaner(currentVideos);
        }
    }

    public void videoListManager()
    {
        boolean ready = false;
        for (Video video : currentVideos) {
            if(video.verified){
                ready = true;
            }else{
                if(video.checkAvailability()){
                    video.verified = true;
                    ready = true;
                }
            }
        }
        if(ready){
            Video next = currentVideos.get(currentVideo);
            if(next.verified){
                videoHolder.setVideoURI(Uri.parse(next.videoLocalURI));
                Log.i(LOG_TAG, "Reproduzco video de index " + currentVideo);
                videoHolder.requestFocus();
                videoHolder.start();
            }else{
                currentVideo++;
                videoListManager();
            }
        }else{
            showingProgress = true;
            (findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
            (findViewById(R.id.progressText)).setVisibility(View.VISIBLE);
        }
    }

    public void localStorageCleaner(LinkedList<Video> videosToShow){
        File filesDir = getFilesDir();
        String[] files = filesDir.list();

        for (String videoFile : files) {
            boolean needed = false;
            for (Video video : videosToShow) {
                Log.i(LOG_TAG, "name 1 " + videoFile);
                Log.i(LOG_TAG, "name 2: " + video.videoName);

                if(video.videoName.equals(videoFile)){
                    Log.i(LOG_TAG, "entreeee");
                    needed = true;
                }
            }
            if(!needed){
                Log.i(LOG_TAG, getFilesDir().toString() + "/" + videoFile);
                File file = new File(getFilesDir().toString() + "/" + videoFile);
                file.delete();
            }
        }
    }
}
