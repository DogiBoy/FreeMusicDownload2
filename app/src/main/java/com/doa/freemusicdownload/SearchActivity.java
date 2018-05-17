package com.doa.freemusicdownload;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.startapp.android.publish.adsCommon.Ad;
import com.startapp.android.publish.adsCommon.StartAppAd;
import com.startapp.android.publish.adsCommon.adListeners.AdEventListener;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class SearchActivity extends AppCompatActivity {

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return GET(urls[0]);
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            getJamendoResponse(result);
        }
    }

    private class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread
         * Show Progress Bar Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(0);
        }

        /**
         * Downloading file in background thread
         */
        @Override
        protected String doInBackground(String[] params) {
            int count;
            try {
                URL url = new URL(params[0]);
                URLConnection conection = url.openConnection();
                conection.connect();
                // getting file length
                int lenghtOfFile = conection.getContentLength();

                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);


                // Output stream to write file
                File folder = new File(Environment.getExternalStorageDirectory() + "/MP3");
                if (!folder.exists()) {
                    folder.mkdir();
                }

                OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory() + "/MP3/" + sFileName + ".mp3");

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error1: ", e.getMessage());
            }

            return null;
        }

        /**
         * Updating progress bar
         */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            pdDownload.setProgress(Integer.parseInt(progress[0]));
        }

        /**
         * After completing background task
         * Dismiss the progress dialog
         **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            dismissDialog(0);

            startAppAd.loadAd(new AdEventListener() {
                @Override
                public void onReceiveAd(Ad ad) {
                    startAppAd.showAd();
                }

                @Override
                public void onFailedToReceiveAd(Ad ad) {
                }
            });
        }

    }

    private EditText edSearchMusic;
    private LinearLayout llSearchResults;
    private ProgressDialog pdDownload, pdSearch;
    private StartAppAd startAppAd = new StartAppAd(this);
    private String sFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.doa.freemusicdownload.R.layout.activity_search);

        edSearchMusic = (EditText) findViewById(com.doa.freemusicdownload.R.id.edSearchMusic);
        llSearchResults = (LinearLayout) findViewById(com.doa.freemusicdownload.R.id.llSearchResults);

        startAppAd.loadAd(new AdEventListener() {
            @Override
            public void onReceiveAd(Ad ad) {
                startAppAd.showAd();
            }

            @Override
            public void onFailedToReceiveAd(Ad ad) {
            }
        });
    }

    @Override
    public void onBackPressed() {
        StartAppAd.onBackPressed(this);
        super.onBackPressed();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 0:
                pdDownload = new ProgressDialog(this);
                pdDownload.setMessage("Downloading...");
                pdDownload.setIndeterminate(false);
                pdDownload.setMax(100);
                pdDownload.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pdDownload.setCancelable(true);
                pdDownload.show();
                return pdDownload;
            case 1:
                pdSearch = new ProgressDialog(this);
                pdSearch.setMessage("Searching...");
                pdSearch.setIndeterminate(true);
                pdSearch.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                pdSearch.setCancelable(true);
                pdSearch.show();
                return pdSearch;
            default:
                return null;
        }
    }

    public String GET(String url) {
        InputStream inputStream = null;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if (inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "-1";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    // convert inputstream to String
    private String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while ((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;
    }

    public void getJamendoResponse(String result) {

        dismissDialog(1);

        try {
            JSONObject json = new JSONObject(result);
            JSONArray jarray = new JSONArray(json.getString("results"));

            for (int i = 0; i < jarray.length(); i++) {

                JSONObject jmusic = jarray.getJSONObject(i);
                final String id = jmusic.getString("id");
                final String title = jmusic.getString("name");
                final String image = jmusic.getString("image");
                final String sduration = jmusic.getString("duration");
                int dura = Integer.parseInt(sduration);
                int min = dura / 60;
                int sec = dura % 60;
                final String duration = min + ":" + sec;

                LinearLayout item_music = (LinearLayout) getLayoutInflater().inflate(com.doa.freemusicdownload.R.layout.item_searchresult, null);
                ImageView ivimage = (ImageView) item_music.getChildAt(0);
                LinearLayout ll1 = (LinearLayout) item_music.getChildAt(1);
                TextView tvname = (TextView) ll1.getChildAt(0);
                TextView tvduration = (TextView) ll1.getChildAt(1);

                ImageLoader.getInstance().displayImage(image, ivimage);

                item_music.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestDownload(id, title);
                    }
                });

                ivimage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestDownload(id, title);
                    }
                });

                tvname.setText(title);
                tvname.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestDownload(id, title);
                    }
                });

                tvduration.setText(duration);
                tvduration.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestDownload(id, title);
                    }
                });

                llSearchResults.addView(item_music);

            }

        } catch (Exception e) {

        }
    }

    public void requestDownload(final String id, final String title) {
        if (title.length() > 20)
            sFileName = title.substring(0, 20);
        else
            sFileName = title;

        new DownloadFileFromURL().execute("https://api.jamendo.com/v3.0/tracks/file/?client_id=d50b1a96&id=" + id);

        startAppAd.loadAd(new AdEventListener() {
            @Override
            public void onReceiveAd(Ad ad) {
                startAppAd.showAd();
            }

            @Override
            public void onFailedToReceiveAd(Ad ad) {
            }
        });
    }

    public void searchMusic(View v) {
        final String music = edSearchMusic.getText().toString().trim();

        if (music.length() == 0)
            return;

        llSearchResults.removeAllViews();
        showDialog(1);

        new HttpAsyncTask().execute("https://api.jamendo.com/v3.0/tracks/?client_id=d50b1a96&format=jsonpretty&imagesize=75&limit=50&boost=buzzrate&search=" + music);

        startAppAd.loadAd(new AdEventListener() {
            @Override
            public void onReceiveAd(Ad ad) {
                startAppAd.showAd();
            }

            @Override
            public void onFailedToReceiveAd(Ad ad) {
            }
        });
    }
}
