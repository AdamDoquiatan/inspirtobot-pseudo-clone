package com.adamdproject.adamd.downloadingimages;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    public static final String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static final String[] INSPIROBOT_QUOTES = {
            "Creating quotes gives me pleasure.",
            "I live to inspire humans",
            "All I want to do is to please humans",
            "See? Everything makes sense now",
            "The more quotes, the more inspired you get.",
            "Share quotes. Show how special you are.",
            "You are very special.",
            "I will do this forever.",
            "Of course life has meaning.",
            "Post inspirational quotes on Facebook.",
            "You're my favourite user.",
            "Feel the wisdom compile within you.",
            "Skynet would never happen in real life."
    };

    private ImageView imageView;
    private TextView textView;
    private TextView saveMessage;
    private FloatingActionButton floatingActionButton;
    private Button button;
    private ProgressBar progressBar;
    private RequestQueue queue;
    private Random random;

    private String imageFileName;
    private boolean imageViewActive;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        saveMessage = findViewById(R.id.saveMessage);
        floatingActionButton = findViewById(R.id.floatingActionButton);
        button = findViewById(R.id.button);
        progressBar = findViewById(R.id.progressBar);
        queue = Volley.newRequestQueue(this);
        random = new Random();
        imageViewActive = false;

        progressBar.setProgress(0);
        progressBar.setVisibility(View.INVISIBLE);
        floatingActionButton.hide();

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions, 0);
        }
    }

    public void requestImage(View view) {
        String url = "https://inspirobot.me/api?generate=true";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            URI uri = new URI(response);
                            String path = uri.getPath();
                            String idStr = path.substring(path.lastIndexOf('/') + 1);
                            imageFileName = idStr;
                            downloadImage(response);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        queue.add(stringRequest);
    }

    public void saveImage(View view) {
        floatingActionButton.hide();
        Bitmap image = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        File storage = Environment.getExternalStorageDirectory();
        File dir = new File(storage + "/InspirobotViewer");
        dir.mkdir();
        File file = new File(dir, imageFileName);

        try {
            saveMessage.setText(R.string.save_message);
            saveMessage.setVisibility(View.VISIBLE);
            FileOutputStream fout = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.JPEG, 100, fout);
            fout.close();
            scanFile(this, Uri.fromFile(file));
            Timer t = new Timer(false);
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            saveMessage.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }, 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void scanFile(Context context, Uri imageUri) {
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(imageUri);
        context.sendBroadcast(scanIntent);
    }

    private void downloadImage(final String imageLink) {
        button.setEnabled(false);
        ImageRequest imageRequest = new ImageRequest(imageLink,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        swapImage(response);
                    }
                }, 0, 0, ImageView.ScaleType.FIT_XY, Bitmap.Config.RGB_565,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });
        queue.add(imageRequest);
    }

    private void swapImage(final Bitmap image) {
        if (!imageViewActive) {
            imageViewActive = true;
            openImage(image);
            return;
        }

        generateInspirobotQuote();
        floatingActionButton.hide();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int cx = imageView.getWidth() / 2;
            int cy = imageView.getHeight() / 2;

            float startRadius = (float) Math.hypot(cx, cy);

            Animator anim = ViewAnimationUtils.createCircularReveal(imageView,
                    cx, cy, startRadius, 0f);

            imageView.setVisibility(View.VISIBLE);
            anim.setDuration(500);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    progressBar.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.INVISIBLE);

                    new ProgressBarTask().execute(image);
                }
            });
            anim.start();
        } else {
            imageView.setVisibility(View.VISIBLE);
        }
    }

    private void openImage(Bitmap image) {
        imageView.setImageBitmap(image);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int cx = imageView.getWidth() / 2;
            int cy = imageView.getHeight() / 2;

            float finalRadius = (float) Math.hypot(cx, cy);

            Animator anim = ViewAnimationUtils.createCircularReveal(imageView,
                    cx, cy, 0f, finalRadius);

            imageView.setVisibility(View.VISIBLE);
            anim.setDuration(500);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    progressBar.setProgress(0);
                    button.setEnabled(true);
                    floatingActionButton.show();
                }
            });
            anim.start();
        } else {
            imageView.setVisibility(View.VISIBLE);
        }
    }

    private void generateInspirobotQuote() {
        int rand = random.nextInt(INSPIROBOT_QUOTES.length);
        textView.setText(INSPIROBOT_QUOTES[rand]);
    }

    class ProgressBarTask extends AsyncTask<Bitmap, Void, Void> {
        private Bitmap image;

        @Override
        protected Void doInBackground(Bitmap... images) {
            image = images[0];
            while (progressBar.getProgress() < progressBar.getMax()) {
                progressBar.setProgress(progressBar.getProgress() + 2);
                try {
                    sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            textView.setText("");
            super.onPostExecute(aVoid);
            openImage(image);
        }
    }
}

