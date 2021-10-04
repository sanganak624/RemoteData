package com.example.remotedata;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";

    private ProgressBar progressBar;
    private TextView textArea;
    private URL searchURL = null;
    private String result;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textArea = findViewById(R.id.textArea);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        Button downloadBtn = findViewById(R.id.downloadBtn);

        Uri.Builder url = Uri.parse("https://192.168.1.23:8000/testwebservice/rest").buildUpon();
        url.appendQueryParameter("method","thedata.getit");
        url.appendQueryParameter("api_key","01189998819991197253");
        url.appendQueryParameter("format","json");
        String urlString = url.build().toString();
        try
        {
            searchURL = new URL(urlString);
        }
        catch (Exception e)
        {

        }
        Log.d("Hello","URL:"+urlString);

        downloadBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ServerQuery query = new ServerQuery();
                query.execute(searchURL);
            }
        });
    }

    public class ServerQuery extends AsyncTask<URL, Integer,String> {
        @Override
        protected String doInBackground(URL... urls) {
            textArea.setText("");
            int count = urls.length;
            String result = "";
            for (int i=0; i<count; i++)
            {
                HttpsURLConnection connection = (HttpsURLConnection) openConnection(urls[i]);
                try {
                    new DownloadUtils().addCertificate(MainActivity.this,connection);
                }
                catch (Exception e)
                {

                }
                if(checkConnection(connection))
                {
                    result = returnQuery(connection);
                }
            }
            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(progress[0]);
        }



        @Override
        protected void onPostExecute(String s) {
            progressBar.setVisibility(View.INVISIBLE);
            String row = JSONToString(s);
            textArea.setText(row);
        }

        private String JSONToString(String data)
        {
            String row = "";
            try {
                JSONObject jBase = new JSONObject(data);
                JSONArray jFactions = jBase.getJSONArray("factions");
                for(int i=0; i<jFactions.length(); i++)
                {
                    JSONObject jFactionsItem = jFactions.getJSONObject(i);
                    String name = jFactionsItem.getString("name");
                    String strength = jFactionsItem.getString("strength");
                    String relationship = jFactionsItem.getString("relationship");
                    row = name + ": strength=" + strength + ", " + relationship +"\n" + row;
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
           return row;
        }

        private String returnQuery(HttpsURLConnection conn)
        {
            progressBar.setMin(0);
            progressBar.setMax(conn.getContentLength());
            String data = null;
            try {
                InputStream inputStream = conn.getInputStream();
                byte[] byteData = getByteArrayFromInputStream(inputStream);
                data = new String(byteData, StandardCharsets.UTF_8);
            }
            catch (IOException e){
                e.printStackTrace();
            }
            conn.disconnect();
            return data;
        }

        private byte[] getByteArrayFromInputStream(InputStream inputStream) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[4096];
            int progress = 0;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
                progress = progress+nRead;
                publishProgress(progress);
            }
            return buffer.toByteArray();
        }

        private HttpsURLConnection openConnection(URL url)  {

            HttpsURLConnection conn = null;
            try {
                conn = (HttpsURLConnection) url.openConnection();
            } catch (MalformedURLException e){
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return conn;
        }

        private boolean checkConnection (HttpsURLConnection connection)
        {
            boolean goodConnection = false;
            if(connection == null){
                Log.d("Hello", "Check Internet");

            }
            else if (isConnectionOkay(connection) == false){
                Log.d("Hello", "Problem with downloading");

            }
            else{
                goodConnection = true;
                Log.d("Hello", "Connected");
            }
            return goodConnection;
        }

        private boolean isConnectionOkay(HttpsURLConnection conn){
            try {
                if(conn.getResponseCode()==HttpsURLConnection.HTTP_OK){
                    Log.d("Hello", "Problem with downloading"+conn.getResponseCode());
                    return true;
                }
            } catch (IOException e) {
                Log.d("Hello", "Problem with downloading"+e);
                e.printStackTrace();
            }
            return false;
        }
    }


}

