package com.example.efs_read_write_file;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final ThreatEventsReceiver threatEventsReceiver = new ThreatEventsReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startThread(null);
        startSecMitm(null);
        startInsecMitm(null);
        onResume();

    }

    public void startInsecMitm(View view){
        MitmInsecThread thread = new MitmInsecThread();
        thread.start();
    }
    public void startSecMitm(View view){
        MitmSecThread thread = new MitmSecThread();
        thread.start();
    }

    public void startThread(View view){
        FileThread thread = new FileThread("");
        thread.start();
    }
    public void stopThread(View view) {
        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        try {
            // image naming and path  to include sd card  appending name you choose for file
            String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";

            // create bitmap screen capture
            View v1 = getWindow().getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

            openScreenshot(imageFile);
        } catch (Throwable e) {
            // Several error may come out with file handling or DOM
            e.printStackTrace();
        }
    }
    private void openScreenshot(File imageFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(imageFile);
        intent.setDataAndType(uri, "image/*");
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "register for RootedDevice ThreatEvent");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("RootedDevice");
        intentFilter.addAction("DeveloperOptionsEnabled");
        intentFilter.addAction("ClickBotDetected");
        intentFilter.addAction("ActiveADBDetected");
        intentFilter.addAction("BannedManufacturer");
        intentFilter.addAction("BlockedClipboardEvent");
        intentFilter.addAction("ClickBotDetectedByPermissions");
        intentFilter.addAction("BlockedScreenCaptureEvent");
        intentFilter.addAction("DebuggerThreatDetected");
        intentFilter.addAction("EmulatorFound");
        intentFilter.addAction("AppIsDebuggable");
        intentFilter.addAction("AppIntegrityError");
        intentFilter.addAction("MagiskManagerDetected");
        intentFilter.addAction("FridaDetected");
        intentFilter.addAction("RootedDevice");
        intentFilter.addAction("SslCertificateValidationFailed");
        intentFilter.addAction("SslIncompatibleVersion");
        intentFilter.addAction("UrlWhitelistFailed");
        intentFilter.addAction("NetworkProxyConfigured");
        registerReceiver(threatEventsReceiver, intentFilter,  getPackageName() + ".deveventspermission", null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "unregister for RootedDevice ThreatEvent");
        unregisterReceiver(threatEventsReceiver);
    }

    class FileThread extends Thread{
        String file_path;

        FileThread(String file_path){
            this.file_path = file_path;
        }

        @Override
        public void run() {
            String filename = "file.txt";
            AssetManager assetManager = getAssets();
            long start1 = System.nanoTime();
            String str = read_file_from_asset(assetManager, filename);
            String file = "encrypt.txt";
            Context context = getBaseContext();
            long start_efs_write = System.nanoTime();
            writeFileOnInternalStorage(context, file, str);
            long end_efs_write = System.nanoTime();
            double in_seconds = (double)(end_efs_write-start_efs_write) / 1_000_000_000;
            Log.i(TAG, "Automation EFS Write in seconds: "+ in_seconds);
            str = "";
            context = getApplicationContext();
            long start_efs_read = System.nanoTime();
            str = read_file_from_internal(context, file);
            long end_efs_read = System.nanoTime();
            long end1 = System.nanoTime();
            in_seconds = (double)(end_efs_read-start_efs_read) / 1_000_000_000;
            Log.i(TAG, "Automation EFS Read in seconds: "+ in_seconds);
            in_seconds = (double)(end1-start1) / 1_000_000_000;
            Log.i(TAG, "Automation EFS R/W in seconds: "+ in_seconds);
        }
        public String read_file_from_asset(AssetManager assetManager, String filename) {
            try {
                InputStream fis = assetManager.open(filename);
                InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            } catch (IOException e) {
                return "";
            }
        }
        public void writeFileOnInternalStorage(Context mcoContext, String sFileName, String sBody){
            File srcdir = new File(mcoContext.getFilesDir(), "");
            File destdir = Environment.getExternalStorageDirectory();
            String fname = "test.txt";
            File destpath = new File(destdir, fname);


            if(!srcdir.exists()){
                srcdir.mkdir();
            }

            try {
                File gpxfile = new File(srcdir, sFileName);
                FileWriter writer = new FileWriter(gpxfile);
                writer.append(sBody);
                writer.flush();
                writer.close();
                //copy(gpxfile,destpath);
                process_cmd();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void process_cmd(){
            StringBuilder output = new StringBuilder();
            try{
                Process p = Runtime.getRuntime().exec("cat /data/user/0/com.example.efs_read_write_file/files/encrypt.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    output.append(line + "\n");
                    //p.waitFor();
                }
                p.waitFor();
            }
            catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String response = output.toString();
            Log.i("levyTest", response);
        }

        public void copy(File src, File dst) throws IOException {
            InputStream in = new FileInputStream(src);
            try {
                OutputStream out = new FileOutputStream(dst);
                try {
                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        }

        public String read_file_from_internal(Context context, String filename) {
            try {
                FileInputStream fis = context.openFileInput(filename);
                InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            } catch (FileNotFoundException e) {
                return "";
            } catch (UnsupportedEncodingException e) {
                return "";
            } catch (IOException e) {
                return "";
            }
        }
    }

    class MitmInsecThread extends  Thread{
        String insec_url;
        MitmInsecThread(){
            this.insec_url = "http://api.openweathermap.org/data/2.5/forecast?id=524901&appid=e53301e27efa0b66d05045d91b2742d3";
        }
        public void run(){
            this.getWeatherDetails_Insec(null);
        }
        public void getWeatherDetails_Insec(View view){
            StringRequest stringRequest = new StringRequest(Request.Method.GET, this.insec_url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d("Automation INSECURITY", response);
                    String output = "";
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String response_code = jsonResponse.getString("cod");
                        Toast.makeText(getApplicationContext(), "http returned: "+ response_code, Toast.LENGTH_SHORT).show();
                    } catch (JSONException e){
                        e.printStackTrace();
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
                }
            });
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            requestQueue.add(stringRequest);
        }
    }

    class MitmSecThread extends  Thread{
        String sec_url;
        MitmSecThread(){
            this.sec_url = "https://api.openweathermap.org/data/2.5/forecast?id=524901&appid=e53301e27efa0b66d05045d91b2742d3";
        }
        public void run(){
            this.getWeatherDetails_Sec(null);
        }
        public void getWeatherDetails_Sec(View view){
//            StringRequest stringRequest = new StringRequest(Request.Method.GET, this.sec_url, new Response.Listener<String>() {
//                @Override
//                public void onResponse(String response) {
//                    Log.d("Automation SECURITY", response);
//                    String output = "";
//                    try {
//                        JSONObject jsonResponse = new JSONObject(response);
//                        String response_code = jsonResponse.getString("cod");
//                        Toast.makeText(getApplicationContext(), "https returned: "+ response_code, Toast.LENGTH_SHORT).show();
//                    } catch (JSONException e){
//                        e.printStackTrace();
//                    }
//
//                }
//            }, new Response.ErrorListener() {
//                @Override
//                public void onErrorResponse(VolleyError error) {
//                    Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
//                }
//            });
//            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
//            requestQueue.add(stringRequest);
            HttpURLConnection http = null;
            try {
                URL url = new URL("https://www.google.com/humans.txt");
                http = (HttpURLConnection)url.openConnection();
                int statusCode = http.getResponseCode();
                String responsecod = String.valueOf(statusCode);
                Log.d("Automation SECURITY", responsecod);
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
    }
}