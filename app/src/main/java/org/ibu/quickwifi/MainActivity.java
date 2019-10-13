package org.ibu.quickwifi;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 10;
    private static final int GPS_REQUEST_CODE = 11;
    private WifiManager mWifiManager;

    private ListView mEnabledDeviceListView;
    private LinearLayout mEnabledDeviceBlock;
    private Switch mWifiSwitch;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout wifiSwitchBlock = findViewById(R.id.wifi_switch_block);
        WifiSwitchListener wifiClickListener = new WifiSwitchListener();
        wifiSwitchBlock.setOnClickListener(wifiClickListener);
        // add switch click listener
        mWifiSwitch = findViewById(R.id.wifi_switch);

        mEnabledDeviceBlock = findViewById(R.id.enabled_device_block);
        // get wifiManager
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mEnabledDeviceListView = findViewById(R.id.enabled_device);

        LocationManager mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.d(TAG, "gps enabled status:" + gpsEnabled);
        if(gpsEnabled){
            requestLocationPermission();
        }else{// setting location to opened status
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, GPS_REQUEST_CODE);
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
            if(!mWifiManager.isWifiEnabled()){
                mWifiManager.setWifiEnabled(true);
            }
            mEnabledDeviceBlock.setVisibility(View.VISIBLE);
            initEnabledDeviceList();
        }
        private void toUnCheckedStatus(){
            mEnabledDeviceBlock.setVisibility(View.GONE);
        }
    }
    private void requestLocationPermission(){
        // getScanResults() need Permission ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "no Permission ACCESS_FINE_LOCATION");
            //申请ACCESS_FINE_LOCATION权限
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_REQUEST_CODE);
        }else{
            initEnabledDeviceList();
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
                requestLocationPermission();
                break;
            default:
                break;
        }
    }

    private void initEnabledDeviceList(){
        mWifiSwitch.setChecked(mWifiManager.isWifiEnabled());
        if(mWifiManager.isWifiEnabled()){
            mEnabledDeviceBlock.setVisibility(View.VISIBLE);
        }else{
            mEnabledDeviceBlock.setVisibility(View.GONE);
        }
        // start scan AP
        mWifiManager.startScan();
        Log.d(TAG, "isWifiEnabled");
        // get scan AP result
        List<ScanResult> scanResultList = mWifiManager.getScanResults();
        List<ScanResult> enabledScanResultList = new ArrayList<>();
        for (ScanResult scanResult: scanResultList) {
            if(!scanResult.SSID.equals("")){
                enabledScanResultList.add(scanResult);
            }
        }
        Log.d(TAG, scanResultList.size()+"");
        WifiEnabledDeviceAdapter wifiEnabledDeviceAdapter
                = new WifiEnabledDeviceAdapter(MainActivity.this, enabledScanResultList);
        mEnabledDeviceListView.setAdapter(wifiEnabledDeviceAdapter);
    }

    class WifiEnabledDeviceAdapter extends ArrayAdapter<ScanResult> {
        private List<ScanResult> mmEnabledDeviceList;
        WifiEnabledDeviceAdapter(Context context, List<ScanResult> list){
            super(context,0, list);
            mmEnabledDeviceList = list;
        }
        
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.enabled_device_list, null);
            ImageView imageView = convertView.findViewById(R.id.enabled_device_icon);
            TextView textView = convertView.findViewById(R.id.enabled_device_name);

            ScanResult scanResult = mmEnabledDeviceList.get(position);
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

            return convertView;
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        initEnabledDeviceList();
    }
}
