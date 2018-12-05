package com.balance.pranacahya.fp_komber;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLConnection;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private static Socket s;
    private static ServerSocket ss;
    private static InputStreamReader isr;
    private static BufferedReader br;
    private static PrintWriter printWriter;
    private ImageView img;
    double kecepatan=0;
    String Alamat;
    private String IP;
    private int PORT ;

    TextView response,SpeedText;
    EditText editTextAddress, editTextPort;
    Button buttonConnect, buttonClear;
    DownloadManager downloadManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Socket s = null;
        editTextAddress = (EditText) findViewById(R.id.addressEditText);
        editTextPort = (EditText) findViewById(R.id.portEditText);
        buttonConnect = (Button) findViewById(R.id.connectButton);
        buttonClear = (Button) findViewById(R.id.clearButton);
        SpeedText = (TextView) findViewById(R.id.speed);
        img = (ImageView) findViewById(R.id.imageView);

        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                TestSpeed();
                send_request();
            }

        });

        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url2 = "http://"+IP+"/"+Alamat;
                new DownloadImage(img).execute(url2);
            }
        });
    }

    private void send_request() {
        Request req = new Request();
        req.execute();
    }

    private void close_socket() {
        Toast.makeText(this, "Socket Closed", Toast.LENGTH_SHORT).show();
    }

    private void TestSpeed(){
        InternetSpeedTest is = new InternetSpeedTest();
        is.execute();
    }

    private class DownloadImage extends AsyncTask<String,Void,Bitmap>
    {
        private final static String TAG = "AsyncTaskLoadImage";
        ImageView bmImage;
        public DownloadImage(ImageView bmImage)
        {
            this.bmImage = bmImage;
        }
        @Override
        protected Bitmap doInBackground(String... urls) {
            Bitmap bitmap = null;
            try {
                URL url = new URL(urls[0]);
                bitmap = BitmapFactory.decodeStream((InputStream)url.getContent());
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            bmImage.setImageBitmap(bitmap);
        }
    }

    class Request extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                IP = editTextAddress.getText().toString();
                PORT = Integer.parseInt(editTextPort.getText().toString());
                s = new Socket(IP, PORT );

                BufferedReader sin = new BufferedReader(new InputStreamReader(s.getInputStream()));
                PrintStream sout = new PrintStream(s.getOutputStream());
                BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));


                sout.println(kecepatan);
                Alamat = sin.readLine();
//                String url_image = "http://192.168.1.101/"+Alamat;
//                new DownloadImage(img).execute(url_image);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    class InternetSpeedTest extends AsyncTask<String, Void, String> {

        long startTime;
        long endTime;
        private long takenTime;
        @Override
        protected String doInBackground(String... paramVarArgs) {

            startTime = System.currentTimeMillis();

            Bitmap bmp = null;
            try {
                URL ulrn = new URL("http://www.daycomsolutions.com/Support/BatchImage/HPIM0050w800.JPG");
                HttpURLConnection con = (HttpURLConnection) ulrn.openConnection();
                InputStream is = con.getInputStream();
                bmp = BitmapFactory.decodeStream(is);

                Bitmap bitmap = bmp;
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 99, stream);
                byte[] imageInByte = stream.toByteArray();
                long lengthbmp = imageInByte.length;

                if (null != bmp) {
                    endTime = System.currentTimeMillis();
                    return lengthbmp + "";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        }

        protected void onPostExecute(String result) {
            if (result != null) {
                long dataSize = Integer.parseInt(result) / 1024;
                takenTime = endTime - startTime;
                double s = (double) takenTime / 1000;
                double speed = dataSize / s;
                kecepatan = speed;
                SpeedText.setText(String.valueOf(speed)+" kb/s");
                Toast.makeText(MainActivity.this, String.valueOf(speed)+"kb/second", Toast.LENGTH_SHORT).show();
//                Log.d(TAG, "onPostExecute: " + "" + new DecimalFormat("##.##").format(speed) + "kb/second");
            }
        }


    }

    static class DownloadTask extends  AsyncTask<String,Void,Void>
    {
        ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            //progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setTitle("Download in progress....");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(100);
            progressDialog.setProgress(0);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {
            int file_Length = 0;
            String path = params[0];
            try {
                URL url = new URL(path);
                URLConnection urlConnection = url.openConnection();
                urlConnection.connect();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

}