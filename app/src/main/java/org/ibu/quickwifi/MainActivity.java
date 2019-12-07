package org.ibu.quickwifi;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 10;
    private static final int GPS_REQUEST_CODE = 11;


    private CWifiManager mCWifiManager;

    private ListView mEnabledDeviceListView;
    private LinearLayout mEnabledDeviceBlock;
    private Switch mWifiSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        mCWifiManager = CWifiManager.getInstance(this);
        enabledWifi();
    }

    private void initView(){
        LinearLayout wifiSwitchBlock = findViewById(R.id.wifi_switch_block);
        WifiSwitchListener wifiClickListener = new WifiSwitchListener();
        wifiSwitchBlock.setOnClickListener(wifiClickListener);
        // add switch click listener
        mWifiSwitch = findViewById(R.id.wifi_switch);

        mEnabledDeviceBlock = findViewById(R.id.enabled_device_block);
        // get wifiManager
        mEnabledDeviceListView = findViewById(R.id.enabled_device);
    }

    private void enabledWifi(){
        int result = mCWifiManager.enabledWifi();
        switch (result){
            case CWifiManager.CUSTOMER_NO_LOCATION:
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(intent, GPS_REQUEST_CODE);
                break;
            case CWifiManager.CUSTOMER_NO_ACCESS_FINE_LOCATION_PERMISSION:
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        ACCESS_FINE_LOCATION_REQUEST_CODE);
                break;
            case CWifiManager.CUSTOMER_ENABLED_WIFI_SUCCESS:
                break;
            case CWifiManager.CUSTOMER_ENABLED_WIFI_FAIL:
                mCWifiManager.enabledWifi();
                break;
            default:
                break;
        }
    }

    class WifiSwitchListener implements View.OnClickListener{

        @Override
        public void onClick(View view) {
            mWifiSwitch.setChecked(!mWifiSwitch.isChecked());
            if(mWifiSwitch.isChecked()){
                toCheckedStatus();
            }else{
                toUnCheckedStatus();
            }
        }

        private void toCheckedStatus(){
            if(!mCWifiManager.isWifiEnabled()){
                mCWifiManager.enabledWifi();
            }
            mEnabledDeviceBlock.setVisibility(View.VISIBLE);
            initEnabledDeviceList();
        }
        private void toUnCheckedStatus(){
            mCWifiManager.disabledWifi();
            mEnabledDeviceBlock.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == ACCESS_FINE_LOCATION_REQUEST_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){// 授权成功
                initEnabledDeviceList();
            }else{// 授权失败
                Toast.makeText(MainActivity.this, "请允许访问位置", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case GPS_REQUEST_CODE:
                enabledWifi();
                break;
            default:
                break;
        }
    }

    private void initEnabledDeviceList(){
        mWifiSwitch.setChecked(mCWifiManager.isWifiEnabled());
        if(mCWifiManager.isWifiEnabled()){
            mEnabledDeviceBlock.setVisibility(View.VISIBLE);
        }else{
            mEnabledDeviceBlock.setVisibility(View.GONE);
        }

        // start scan AP
        mCWifiManager.scanAP();
        mCWifiManager.setScanAPListener(new CWifiManager.OnScanAPListener() {
            @Override
            public void onScanFinish(List<ScanResult> scanResultList) {
                WifiEnabledDeviceAdapter wifiEnabledDeviceAdapter
                        = new WifiEnabledDeviceAdapter(MainActivity.this, scanResultList);
                mEnabledDeviceListView.setAdapter(wifiEnabledDeviceAdapter);
            }
        });
    }

    class WifiEnabledDeviceAdapter extends ArrayAdapter<ScanResult> {
        private List<ScanResult> mmEnabledDeviceList;
        WifiEnabledDeviceAdapter(Context context, List<ScanResult> list){
            super(context,0, list);
            mmEnabledDeviceList = list;
        }

        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull final ViewGroup parent) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.enabled_device_list, null);
            ImageView imageView = convertView.findViewById(R.id.enabled_device_icon);
            TextView textView = convertView.findViewById(R.id.enabled_device_name);

            final ScanResult scanResult = mmEnabledDeviceList.get(position);
            Log.d(TAG, scanResult.toString());
            // signal level(calculate rssi)
            int level = WifiManager.calculateSignalLevel(scanResult.level, 5);
            int drawableSignalLevel = R.drawable.ic_wifi_signal0;
            switch (level){
                case 0:
                    break;
                case 1:
                    drawableSignalLevel = R.drawable.ic_wifi_signal1;
                    break;
                case 2:
                    drawableSignalLevel = R.drawable.ic_wifi_signal2;
                    break;
                case 3:
                    drawableSignalLevel = R.drawable.ic_wifi_signal3;
                    break;
                case 4:
                    drawableSignalLevel = R.drawable.ic_wifi_signal4;
                    break;
                default:
                    break;
            }

            Drawable drawable = getResources().getDrawable(drawableSignalLevel,null);
            imageView.setImageDrawable(drawable);
            // wifi name(ssid)
            textView.setText(scanResult.SSID);
            // set click event for each wifi list item
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final int keyMgmt = mCWifiManager.checkAPKeyMgmt(scanResult);

                    if(keyMgmt != CWifiManager.KEY_MANAGEMENT_NONE) {
                        final EditText passwordEdit = new EditText(MainActivity.this);
                        passwordEdit.setText("");
                        passwordEdit.setFocusable(true);
                        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("密码")
                                .setView(passwordEdit)
                                .setNegativeButton("取消", null);
                        builder.setPositiveButton("连接", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if(passwordEdit.getText().toString().length() < 8){
                                    Toast.makeText(MainActivity.this, "密码不得少于8位", Toast.LENGTH_SHORT).show();
                                }else {
                                    if (mCWifiManager.connectAP(keyMgmt, scanResult, passwordEdit.getText().toString())) {
                                        Toast.makeText(MainActivity.this, "连接成功。", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(MainActivity.this, "密码错误，连接失败。", Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        });
                        builder.show();
                    }else{
                        if(mCWifiManager.connectAP(keyMgmt, scanResult, null)){
                            Toast.makeText(MainActivity.this, "连接成功。", Toast.LENGTH_LONG).show();
                        }else{
                            Toast.makeText(MainActivity.this, "密码错误，连接失败。", Toast.LENGTH_LONG).show();
                        }
                    }

                }
            });
            return convertView;
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        initEnabledDeviceList();
        System.out.println(mCWifiManager.getConnectionInfo());
    }
}

