package com.hustar.smarthelmet.service;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.hustar.smarthelmet.DB.Api;
import com.hustar.smarthelmet.GPS.GpsTracker;
import com.hustar.smarthelmet.MainActivity;
import com.hustar.smarthelmet.R;
import com.hustar.smarthelmet.handler.RequestHandler;

import java.util.HashMap;

/**
 * 앱 강제종료 핸들링
 */
public class UnCatchTaskService extends Service {
    private static final int CODE_GET_REQUEST = 1024;
    private static final int CODE_POST_REQUEST = 1025;

    private GpsTracker gpsTracker;
    private String deviceId;
    private String workerName;
    private String workerPhoneNum;
    private String workerGroup;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        deviceId = intent.getStringExtra("deviceId");
        workerName = intent.getStringExtra("workerName");
        workerPhoneNum = intent.getStringExtra("workerPhoneNum");
        workerGroup = intent.getStringExtra("workerGroup");

        return super.onStartCommand(intent, flags, startId);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) { // 핸들링 하는 부분
        Log.e("Error","onTaskRemoved - " + rootIntent + "/ : " + rootIntent.getStringExtra("message"));

        gpsTracker = new GpsTracker(this);

        String latitude = Double.toString(gpsTracker.getLatitude());
        String longitude = Double.toString(gpsTracker.getLongitude());

        System.out.println("=======================어플 종료============================");
        System.out.println("latitude : " + latitude);
        System.out.println("longitude : " + longitude);
        System.out.println("deviceId : " + deviceId);
        System.out.println("workerName : " + workerName);
        System.out.println("workerPhoneNum : " + workerPhoneNum);
        System.out.println("workerGroup : " + workerGroup);

        HashMap<String, String> params = new HashMap<>();
        params.put("deviceId", deviceId);
        params.put("workerName", workerName);
        params.put("workerPhoneNum", workerPhoneNum);
        params.put("workerGroup", workerGroup);
        params.put("reason", "어플강제종료");
        params.put("status", "경고");
        params.put("latitude", latitude);
        params.put("longitude", longitude);

        try {
            UnCatchTaskService.PerformNetworkRequest request = new UnCatchTaskService.PerformNetworkRequest(Api.URL_UPDATE_WORKER_STATUS, params, CODE_POST_REQUEST);
            request.execute();
            Thread.sleep(1000);
            UnCatchTaskService.PerformNetworkRequest request1 = new UnCatchTaskService.PerformNetworkRequest(Api.URL_INSERT_WORKER_Warning, params, CODE_POST_REQUEST);
            request1.execute();
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stopSelf(); //서비스도 같이 종료
    }

    //inner class to perform network request extending an AsyncTask
    private class PerformNetworkRequest extends AsyncTask<Void, Void, String> {

        //the url where we need to send the request
        String url;

        //the parameters
        HashMap<String, String> params;

        //the request code to define whether it is a GET or POST
        int requestCode;

        //constructor to initialize values
        PerformNetworkRequest(String url, HashMap<String, String> params, int requestCode) {
            this.url = url;
            this.params = params;
            this.requestCode = requestCode;
        }

        //when the task started displaying a progressbar
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        //this method will give the response from the request
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //the network operation will be performed in background
        @Override
        protected String doInBackground(Void... voids) {
            RequestHandler requestHandler = new RequestHandler();

            if (requestCode == CODE_POST_REQUEST)
                return requestHandler.sendPostRequest(url, params);


            if (requestCode == CODE_GET_REQUEST)
                return requestHandler.sendGetRequest(url);

            return null;
        }
    }
}