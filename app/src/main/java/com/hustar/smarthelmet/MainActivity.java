package com.hustar.smarthelmet;


import static android.view.View.GONE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hustar.smarthelmet.DB.Api;
import com.hustar.smarthelmet.GPS.GpsTracker;
import com.hustar.smarthelmet.handler.RequestHandler;
import com.hustar.smarthelmet.service.UnCatchTaskService;

import net.daum.mf.map.api.MapView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import model.Gyro;

public class MainActivity extends AppCompatActivity {
    private static final int CODE_GET_REQUEST = 1024;
    private static final int CODE_POST_REQUEST = 1025;

    TextView mTvBluetoothStatus;
    TextView status;
    CircleImageView btCon;
    TextView btConTxt;
    Switch swBT;
    TextView tvBTstat;
    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices;
    List<String> mListPairedDevices;

    Handler mBluetoothHandler;
    ConnectedBluetoothThread mThreadConnectedBluetooth;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String TAG = "phptest";
    private GpsTracker gpsTracker;
    private String deviceId;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private String workerName;
    private String workerPhoneNum;
    private String workerGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);

        workerName = intent.getStringExtra("workerName");
        workerPhoneNum = intent.getStringExtra("workerPhoneNum");
        workerGroup = intent.getStringExtra("workerGroup");

        MapView mapView = new MapView(this);

        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);

        if (chkPermission()){
            Toast.makeText(this, "위험 권한 승인함", Toast.LENGTH_SHORT).show();
        }

        swBT = findViewById(R.id.swBT);
        tvBTstat = findViewById(R.id.tvBTstat);

        btCon = findViewById(R.id.btCon);
        btConTxt = findViewById(R.id.btConTxt);

        status = (TextView)findViewById(R.id.status);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        deviceId = Settings.Secure.getString(this.getContentResolver(),Settings.Secure.ANDROID_ID);

        swBT.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    bluetoothOn();
                } else {
                    bluetoothOff();
                }
            }
        });

        status.addTextChangedListener(textWatcher);

        btCon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btConTxt.getText().equals("연결하기")) {
                    listPairedDevices();
                } else {
                    disconnectDevice();
                }
            }
        });

        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        }else {
            checkRunTimePermission();
        }

        if (mBluetoothAdapter.isEnabled()) {
            swBT.setChecked(true);
            tvBTstat.setText("블루투스 켜짐");
            tvBTstat.setTextColor(Color.BLUE);
        } else {
            swBT.setChecked(false);
            tvBTstat.setText("블루투스 꺼짐");
            tvBTstat.setTextColor(Color.RED);
        }

        mBluetoothHandler = new Handler()
        {
            @SuppressLint("HandlerLeak")
            public void handleMessage(android.os.Message msg){

                if(msg.what == BT_MESSAGE_READ) {
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                        System.out.println(readMessage);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    String arr[] = readMessage.split(":");

                    if(arr[1].contains("F"))
                    {
                        status.setText("낙상");
                        status.setTextColor(Color.BLUE);
                    }
                    else if(arr[0].contains("E"))
                    {
                        status.setText("착용");
                        status.setTextColor(Color.GREEN);
                    }
                    else if(arr[0].contains("N"))
                    {
                        status.setText("미착용");
                        status.setTextColor(Color.rgb(255,128,0));
                    }
                    else if(arr[0].contains("W"))
                    {
                        // 장시간 움직임 없음 경고
                        status.setText("움직임 미감지");
                        status.setTextColor(Color.RED);
                    }

//                    String arr[] = readMessage.split(":");
//                    if(arr[0].contains("h")) {
//                        if(arr[1].contains("acc")) {
//
//                        }
//                    }
//
//                    if(arr[9].contains("E"))
//                    {
//                        status.setText("착용");
//                        status.setTextColor(Color.GREEN);
//                    }
//                    else if(arr[9].contains("N"))
//                    {
//                        status.setText("미착용");
//                        status.setTextColor(Color.rgb(255,128,0));
//                    }
//                    else if(arr[9].contains("W"))
//                    {
//                        // 장시간 움직임 없음 경고
//                        status.setText("경고");
//                        status.setTextColor(Color.RED);
//                    }
//                    else if(arr[9].contains("F"))
//                    {
//                        // 낙상 경고
//                        status.setText("낙상");
//                        status.setTextColor(Color.RED);
//                    }
                }
            }
        };
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Intent intent1 = new Intent(context, UnCatchTaskService.class);
            intent1.putExtra("deviceId", deviceId);
            intent1.putExtra("workerName", workerName);
            intent1.putExtra("workerPhoneNum", workerPhoneNum);
            intent1.putExtra("workerGroup", workerGroup);

            switch (action){
                //블루투스 연결
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    startService(intent1);
                    break;
                // 블루투스 연결 끊김
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    if(status.getText().toString().equals("미착용") || status.getText().toString().equals("착용")) {
                        btConTxt.setText("연결해제");
                        status.setText("헬멧 연결 끊김");
                        status.setTextColor(Color.RED);

//                        HashMap<String, String> params = new HashMap<>();
//                        params.put("deviceId", deviceId);
//
//                        MainActivity.PerformNetworkRequest delete_request = new MainActivity.PerformNetworkRequest(Api.URL_DELETE_WORKER+deviceId, null, CODE_GET_REQUEST);
//
//                        delete_request.execute();
                    }
                    stopService(intent1);
                    break;
            }
        }
    };

    void bluetoothOn() {
        if(mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(getApplicationContext(), "블루투스가 활성화 되었습니다.", Toast.LENGTH_LONG).show();
            mBluetoothAdapter.enable();
            tvBTstat.setText("블루투스 켜짐");
            tvBTstat.setTextColor(Color.BLUE);
        }
    }

    void bluetoothOff() {
        mBluetoothAdapter.disable();
        Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되었습니다.", Toast.LENGTH_SHORT).show();
        tvBTstat.setText("블루투스 꺼짐");
        tvBTstat.setTextColor(Color.RED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BT_REQUEST_ENABLE:
                if (resultCode == RESULT_OK) { // 블루투스 활성화를 확인을 클릭하였다면
                    Toast.makeText(getApplicationContext(), "블루투스 활성화", Toast.LENGTH_LONG).show();
                    mTvBluetoothStatus.setText("활성화");
                } else if (resultCode == RESULT_CANCELED) { // 블루투스 활성화를 취소를 클릭하였다면
                    Toast.makeText(getApplicationContext(), "취소", Toast.LENGTH_LONG).show();
                    mTvBluetoothStatus.setText("비활성화");
                }
                break;

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void listPairedDevices() {
        if (mBluetoothAdapter.isEnabled()) {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();

            if (mPairedDevices.size() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("장치 선택");

                mListPairedDevices = new ArrayList<String>();
                for (BluetoothDevice device : mPairedDevices) {
                    mListPairedDevices.add(device.getName());
                }
                final CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        connectSelectedDevice(items[item].toString());
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    void connectSelectedDevice(String selectedDeviceName) {
        for(BluetoothDevice tempDevice : mPairedDevices) {
            if (selectedDeviceName.equals(tempDevice.getName())) {
                mBluetoothDevice = tempDevice;

                break;
            }
        }
        try {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            mBluetoothSocket.connect();
            mThreadConnectedBluetooth = new ConnectedBluetoothThread(mBluetoothSocket);
            mThreadConnectedBluetooth.start();
            mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
            status.setText("작업시작");
            status.setTextColor(Color.GREEN);
            btConTxt.setText("연결해제");
        } catch (IOException e) {
            status.setText("헬멧 전원을 확인해 주세요.");
            status.setTextColor(Color.RED);
            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    void disconnectDevice() {
        try {
            mThreadConnectedBluetooth.cancel();
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            mBluetoothSocket.close();
            // 정장적인 연결해제, worker삭제 workerLog테이블 endDate갱신
            HashMap<String, String> params = new HashMap<>();
            params.put("deviceId", deviceId);

            MainActivity.PerformNetworkRequest delete_request = new MainActivity.PerformNetworkRequest(Api.URL_DELETE_WORKER+deviceId, null, CODE_GET_REQUEST);

            delete_request.execute();

            btConTxt.setText("연결하기");
            status.setText("헬멧을 연결하여 주세요.");
            status.setTextColor(Color.parseColor("#6E6E6E"));
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "블루투스 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        public String t;
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            t = charSequence.toString();
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            gpsTracker = new GpsTracker(MainActivity.this);

            String latitude = Double.toString(gpsTracker.getLatitude());
            String longitude = Double.toString(gpsTracker.getLongitude());
            String text = status.getText().toString();

            System.out.println("BeforeText : " + t);
            System.out.println("OnChangedText : " + text);

            HashMap<String, String> params = new HashMap<>();
            params.put("deviceId", deviceId);
            params.put("latitude", latitude);
            params.put("longitude", longitude);
            params.put("workerName", workerName);
            params.put("workerPhoneNum", workerPhoneNum);
            params.put("workerGroup", workerGroup);

            if(!text.equals(t)) {
                if(text.equals("작업시작")) {
                    String buffer = null;
                    System.out.println("사용자 착용 인서트");
                    params.put("status", text);
                    MainActivity.PerformNetworkRequest request = new MainActivity.PerformNetworkRequest(Api.URL_INSERT_WORKER, params, CODE_POST_REQUEST);
                    request.execute();
                } else {
                    System.out.println("사용자 상태 업데이트");
                    params.put("status", text);
                    MainActivity.PerformNetworkRequest request1 = new MainActivity.PerformNetworkRequest(Api.URL_UPDATE_WORKER_STATUS, params, CODE_POST_REQUEST);
                    request1.execute();
                }

                if(text.equals("헬멧 연결 끊김") || text.equals("낙상") || text.equals("움직임 미감지")) {
                    System.out.println("경고");
                    params.put("status", "경고");
                    params.put("reason", text);
                    MainActivity.PerformNetworkRequest request2 = new MainActivity.PerformNetworkRequest(Api.URL_INSERT_WORKER_Warning, params, CODE_POST_REQUEST);
                    request2.execute();
                }
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

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

    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        if(mBluetoothSocket.isConnected()) {
                            System.out.println("getConnectionType : " +
                                    mBluetoothSocket.getConnectionType());
                        } else {
                            System.out.println("getConnectionType : " +
                                    mBluetoothSocket.getConnectionType());
                        }
                        SystemClock.sleep(100);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes);
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    // 헬멧 연결 끊기 버튼을 누르면 이쪽으로 오면서 브레이크 cancel()로 간다.
                    // 하지만 강제로 끊으면 루프가 멈춘다.
                    break;
                }
            }
        }
        public void write(String str) {
            byte[] bytes = str.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        super.onRequestPermissionsResult(permsRequestCode, permissions, grandResults);
        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면
            boolean check_result = true;

            // 모든 퍼미션을 허용했는지 체크합니다.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if (check_result) {

                //위치 값을 가져올 수 있음
                ;
            } else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();


                } else {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();

                }
            }
        }
    }

    void checkRunTimePermission(){

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)


            // 3.  위치 값을 가져올 수 있음



        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }

    public boolean chkPermission() {
        // 위험 권한을 모두 승인했는지 여부
        boolean mPermissionsGranted = false;
        String[] mRequiredPermissions = new String[1];
        // 사용자의 안드로이드 버전에 따라 권한을 다르게 요청합니다
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 11 이상인 경우
            mRequiredPermissions[0] = Manifest.permission.READ_PHONE_NUMBERS;

        }else{
            // 10 이하인 경우
            mRequiredPermissions[0] = Manifest.permission.READ_PRECISE_PHONE_STATE;
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 필수 권한을 가지고 있는지 확인한다.
            mPermissionsGranted = hasPermissions(mRequiredPermissions);

            // 필수 권한 중에 한 개라도 없는 경우
            if (!mPermissionsGranted) {
                // 권한을 요청한다.
                ActivityCompat.requestPermissions(MainActivity.this, mRequiredPermissions, PERMISSIONS_REQUEST_CODE);
            }
        } else {
            mPermissionsGranted = true;
        }

        return mPermissionsGranted;
    }

    public boolean hasPermissions(String[] permissions) {
        // 필수 권한을 가지고 있는지 확인한다.
        for (String permission : permissions) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}
