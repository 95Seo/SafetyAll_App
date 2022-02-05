package com.hustar.smarthelmet;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hustar.smarthelmet.service.UnCatchTaskService;

public class SplashActivity extends AppCompatActivity {

    private ProgressBar splashProgress;
    private TextView tv_splash;
    private int percent = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        splashProgress = findViewById(R.id.splashProgress);
        tv_splash = findViewById(R.id.tv_splash);

        new DownloadTask().execute();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(),InsertInfoActivity.class);
                startActivity(intent);
                finish();
            }
        },5000);
    }

    class DownloadTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (int i = 0; i <= 100; i++){
                try{
                  Thread.sleep(100);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }

                percent = i;
                publishProgress(percent);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            tv_splash.setText(values[0]+"%");
            splashProgress.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}