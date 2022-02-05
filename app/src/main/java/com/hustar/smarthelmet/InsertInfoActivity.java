package com.hustar.smarthelmet;

import static android.view.View.GONE;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.hustar.smarthelmet.DB.Api;
import com.hustar.smarthelmet.handler.RequestHandler;
import com.hustar.smarthelmet.model.Worker;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class InsertInfoActivity extends AppCompatActivity {

    private static final int CODE_GET_REQUEST = 1024;
    private static final int CODE_POST_REQUEST = 1025;

    EditText etName, etPhone;
    Spinner etGroup;
    Button btInsert;
    ProgressBar progressBar;

    String name, phone, group;
    boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_info);

        getHashKey();

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etGroup = findViewById(R.id.etGroup);
        btInsert = findViewById(R.id.btInsert);
        progressBar = findViewById(R.id.progressBar);

        btInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isUpdating) {
                    //calling the method update hero
                    //method is commented becuase it is not yet created
                    //updateHero();
                } else {
                    //if it is not updating
                    //that means it is creating
                    //so calling the method create hero
                    insertWorker();
                }
            }
        });
    }

    private void insertWorker() {
        name = etName.getText().toString();
        phone = etPhone.getText().toString();
        group = etGroup.getSelectedItem().toString();

        //validating the inputs
        if (TextUtils.isEmpty(name)) {
            etName.setError("이를을 입력해주세요.");
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("전화번호를 입력해주세요");
            etPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(group)) {
            etPhone.setError("소속을 선택해주세요");
            etPhone.requestFocus();
            return;
        }

        System.out.println("name : "+name);
        System.out.println("phone : "+phone);
        System.out.println("group : "+group);

        //if validation passes

//        HashMap<String, String> params = new HashMap<>();
//        params.put("workerName", name);
//        params.put("workerPhoneNum", phone);
//        params.put("workerGroup", group);
//
//        PerformNetworkRequest request = new PerformNetworkRequest(Api.URL_INSERT_WORKER, params, CODE_POST_REQUEST);
//        request.execute();

        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        intent.putExtra("workerName", name);
        intent.putExtra("workerPhoneNum", phone);
        intent.putExtra("workerGroup", group);

        startActivity(intent);
        finish();
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
            progressBar.setVisibility(View.VISIBLE);
        }


        //this method will give the response from the request
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressBar.setVisibility(GONE);
            try {
                JSONObject object = new JSONObject(s);
                if (!object.getBoolean("error")) {
                    Toast.makeText(getApplicationContext(), object.getString("message"), Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
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

    private void getHashKey(){
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo == null)
            Log.e("KeyHash", "KeyHash:null");

        for (Signature signature : packageInfo.signatures) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            } catch (NoSuchAlgorithmException e) {
                Log.e("KeyHash", "Unable to get MessageDigest. signature=" + signature, e);
            }
        }
    }

}