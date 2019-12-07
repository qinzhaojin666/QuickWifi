package org.ibu.quickwifi;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;


public class CWifiManager {

    private static final String TAG = "CWifiManager";

    /* scan result listener start */

    public interface OnScanAPListener{
        void onScanFinish(List<ScanResult> scanResultList);
    }

    public void setScanAPListener(OnScanAPListener l){
        mOnScanAPListener = l;
    }

    private OnScanAPListener mOnScanAPListener;

    private class ScanResultBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                if (mOnScanAPListener != null) {
                    List<ScanResult> enabledScanResultList = new ArrayList<>();
                    List<ScanResult> scanResultList = mWifiManager.getScanResults();
                    for (ScanResult scanResult : scanResultList) {
                        if (!scanResult.SSID.equals("")) {
                            enabledScanResultList.add(scanResult);
                        }
                    }
                    mOnScanAPListener.onScanFinish(enabledScanResultList);
                }
            }
        }
    }
    /* scan result listener end */

    private Context mContext;

    private static CWifiManager mInstance;

    private WifiManager mWifiManager;

    /**
     * enabledWifi() status
     */
    public static final int CUSTOMER_NO_ACCESS_FINE_LOCATION_PERMISSION = 0;
    public static final int CUSTOMER_NO_LOCATION = 1;
    public static final int CUSTOMER_ENABLED_WIFI_SUCCESS = 2;
    public static final int CUSTOMER_ENABLED_WIFI_FAIL = 3;

    private CWifiManager(Context context){
        this.mContext = context;
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // register a broadcast for scan result
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mContext.registerReceiver(new ScanResultBroadcastReceiver(),
                filter);
    }

    public static CWifiManager getInstance(Context context){
        if(null == mInstance){
            mInstance = new CWifiManager(context);
        }
        return mInstance;
    }
    /**
     * 1.open and enable wifi
     */
    public int enabledWifi(){
        // get gps location status
        LocationManager mLocationManager = (LocationManager) mContext.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.d(TAG, "enabledWifi()::gps enabled status:" + gpsEnabled);
        if(gpsEnabled){
            // getScanResults() need Permission ACCESS_FINE_LOCATION
            if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "enabledWifi()::no Permission ACCESS_FINE_LOCATION");
                return CUSTOMER_NO_ACCESS_FINE_LOCATION_PERMISSION;
            }else{
                if(mWifiManager.setWifiEnabled(true)){
                    return CUSTOMER_ENABLED_WIFI_SUCCESS;
                }else {
                    return CUSTOMER_ENABLED_WIFI_FAIL;
                }
            }
        }else{
            return CUSTOMER_NO_LOCATION;
        }
    }

    /**
     * 2.disable wifi
     */
    public void disabledWifi(){
        Log.d(TAG, "disabledWifi()");
        mWifiManager.setWifiEnabled(false);
    }
    public boolean isWifiEnabled(){
        return mWifiManager.isWifiEnabled();
    }
    /**
     * 3.scan AP
     *
     */
    public void scanAP(){
        Log.d(TAG, "scanAP()");
        // start scan AP
        mWifiManager.startScan();
    }

    private static final String DEFAULT_KEY = "12345678";

    /**
     * key auth status
     */
    public static final int KEY_MANAGEMENT_NONE = 0;
    public static final int KEY_MANAGEMENT_WEP = 1;
    public static final int KEY_MANAGEMENT_PSK = 2;
    public static final int KEY_MANAGEMENT_EAP = 3;

    /**
     * check auth for AP
     */
    public int checkAPKeyMgmt(ScanResult scanResult){
        Log.d(TAG, "checkAPKeyMgmt()");
        // AP auth
        String capabilities = scanResult.capabilities;
        if(capabilities.contains("WEP")){
            return KEY_MANAGEMENT_WEP;
        }else if(capabilities.contains("PSK")){
            return KEY_MANAGEMENT_PSK;
        }else if(capabilities.contains("EAP")){
            return KEY_MANAGEMENT_EAP;
        }else {
            return KEY_MANAGEMENT_NONE;
        }
    }
    /**
     * 4.connect AP
     */
    public boolean connectAP(int keyMgmt, ScanResult toConnectAP, String key){
        Log.d(TAG, "connectAP("+"keyMgmt:"+keyMgmt+", toConnectAP:"+toConnectAP.toString()+", key:"+keyMgmt);
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = toConnectAP.SSID;
        config.BSSID = toConnectAP.BSSID;

        switch (keyMgmt){
            case KEY_MANAGEMENT_WEP:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                config.wepKeys[0] =
                        (key == null && key.length() == 0) ? DEFAULT_KEY : key;
                break;
            case KEY_MANAGEMENT_PSK:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                config.preSharedKey =
                        (key == null && key.length() == 0) ? DEFAULT_KEY : key;
                break;
            case KEY_MANAGEMENT_EAP:
                break;
            case KEY_MANAGEMENT_NONE:
                break;
        }
        boolean existed = false;
        List<WifiConfiguration> wifiConfigurations = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration wifiConfig: wifiConfigurations) {
            if(config.SSID.equals(wifiConfig.SSID)){
                existed = true;
            }
        }
        if(existed) {
            Log.d(TAG, "config is existed in getConfiguredNetworks() result");
            int netWorkId = mWifiManager.updateNetwork(config);
            return mWifiManager.enableNetwork(netWorkId, true);
        }else{
            Log.d(TAG, "config is not existed in getConfiguredNetworks() result");
            int netWorkId = mWifiManager.addNetwork(config);
            return mWifiManager.enableNetwork(netWorkId, true);
        }
        /**
         * networkId
         * SSID
         * BSSID
         * priority
         * allowedProtocols
         * allowedKeyManagement
         * allowedAuthAlgorithms
         * allowedPairwiseCiphers
         * allowedGroupCiphers
         */
    }
    /**
     * 5.auto connect AP
     */
    public void autoConnectAP(ScanResult toConnectAP){

    }
    /**
     * 6.disconnect AP
     */
    public void disconnectAP(ScanResult toConnectAP){

    }
    public WifiInfo getConnectionInfo(){
        return mWifiManager.getConnectionInfo();
    }
}

