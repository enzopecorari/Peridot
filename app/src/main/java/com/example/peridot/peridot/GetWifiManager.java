package com.example.peridot.peridot;

import android.content.Context;
import android.net.wifi.WifiManager;

/**
 * Created by Enzo on 06/03/2016.
 */
public class GetWifiManager {
    Context mContext;
    public GetWifiManager(Context mContext) {
        this.mContext = mContext;
    }

    public WifiManager getWifiManager() {
        WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        return wifiManager;
    }
}
