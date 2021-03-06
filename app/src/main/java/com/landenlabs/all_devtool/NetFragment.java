package com.landenlabs.all_devtool;

/*
 * Copyright (c) 2016 Dennis Lang (LanDen Labs) landenlabs@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @author Dennis Lang  (3/21/2015)
 * @see http://LanDenLabs.com/
 *
 */


import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.landenlabs.all_devtool.util.LLog;
import com.landenlabs.all_devtool.util.Ui;
import com.landenlabs.all_devtool.util.Utils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.telephony.TelephonyManager.NETWORK_TYPE_1xRTT;
import static android.telephony.TelephonyManager.NETWORK_TYPE_CDMA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EDGE;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EHRPD;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_0;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_A;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_B;
import static android.telephony.TelephonyManager.NETWORK_TYPE_GPRS;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSPA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP;
import static android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_IDEN;
import static android.telephony.TelephonyManager.NETWORK_TYPE_LTE;
import static android.telephony.TelephonyManager.NETWORK_TYPE_UMTS;


/**
 * Display "Network" system information.
 *
 * @author Dennis Lang
 */
public class NetFragment extends DevFragment {
    // Logger - set to LLog.DBG to only log in Debug build, use LLog.On for always log.
    private final LLog m_log = LLog.DBG;

    final ArrayList<BuildInfo> m_list = new ArrayList<BuildInfo>();
    ExpandableListView m_listView;
    TextView m_titleTime;
    BuildArrayAdapter m_adapter;

    SubMenu m_menu;

    public static String s_name = "Network";
    private static final int MB = 1 << 20;
    private static int m_rowColor1 = 0;
    private static int m_rowColor2 = 0x80d0ffe0;
    private static SimpleDateFormat m_timeFormat = new SimpleDateFormat("HH:mm:ss zz");
    private static IntentFilter INTENT_FILTER_SCAN_AVAILABLE = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);


    class SystemBroadcastReceiver extends BroadcastReceiver {
        final WifiManager mWifiMgr;

        public SystemBroadcastReceiver(WifiManager wifiMgr) {
            mWifiMgr = wifiMgr;
        }
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                // results = wifi.getScanResults();
                m_listView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mWifiMgr.getScanResults() != null &&
                                mWifiMgr.getScanResults().size() > 1) {
                            mLastScanSize = mWifiMgr.getScanResults().size();
                            // updateList();
                        }
                    }
                });
            }
        }
    }

    SystemBroadcastReceiver mSystemBroadcastReceiver;

    // ---------------------------------------------------------------------------------------------
    String getNetworkTypeName(int type) {

        switch (type) {
            case NETWORK_TYPE_GPRS:
                return "GPRS";
            case NETWORK_TYPE_EDGE:
                return "EDGE";
            case NETWORK_TYPE_UMTS:
                return "UMTS";
            case NETWORK_TYPE_HSDPA:
                return "HSDPA";
            case NETWORK_TYPE_HSUPA:
                return "HSUPA";
            case NETWORK_TYPE_HSPA:
                return "HSPA";
            case NETWORK_TYPE_CDMA:
                return "CDMA";
            case NETWORK_TYPE_EVDO_0:
                return "CDMA - EvDo rev. 0";
            case NETWORK_TYPE_EVDO_A:
                return "CDMA - EvDo rev. A";
            case NETWORK_TYPE_EVDO_B:
                return "CDMA - EvDo rev. B";
            case NETWORK_TYPE_1xRTT:
                return "CDMA - 1xRTT";
            case NETWORK_TYPE_LTE:
                return "LTE";
            case NETWORK_TYPE_EHRPD:
                return "CDMA - eHRPD";
            case NETWORK_TYPE_IDEN:
                return "iDEN";
            case NETWORK_TYPE_HSPAP:
                return "HSPA+";
            default:
                return "UNKNOWN";
        }
    }


    public NetFragment() {
    }

    public static DevFragment create() {
        return new NetFragment();
    }

    // ============================================================================================
    // DevFragment methods

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public List<Bitmap> getBitmaps(int maxHeight) {
        return Utils.getListViewAsBitmaps(m_listView, maxHeight);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.build_tab, container, false);

        Ui.viewById(rootView, R.id.buildListTitle).setVisibility(View.GONE);
        m_titleTime = Ui.viewById(rootView, R.id.buildListTime);
        m_titleTime.setVisibility(View.VISIBLE);
        m_titleTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateList();
                m_listView.invalidateViews();
            }
        });

        m_listView = Ui.viewById(rootView, R.id.buildListView);

        m_adapter = new BuildArrayAdapter(this.getActivity());
        m_listView.setAdapter(m_adapter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != m_listView) {
            updateList();
            m_listView.invalidateViews();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int pos = -1;
        int id = item.getItemId();
        switch (id) {
            case R.id.sys_clean_networks:
                clean_networks();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        m_menu = menu.addSubMenu("Sys Options");
        inflater.inflate(R.menu.sys_menu, m_menu);
        // m_menu.findItem(m_sortBy).setChecked(true);
    }

    private static int mLastScanSize = 0;

    public void updateList() {
        // Time today = new Time(Time.getCurrentTimezone());
        // today.setToNow();
        // today.format(" %H:%M:%S")
        Date dt = new Date();
        m_titleTime.setText(m_timeFormat.format(dt));

        boolean expandAll = m_list.isEmpty();
        m_list.clear();

        // Swap colors
        int color = m_rowColor1;
        m_rowColor1 = m_rowColor2;
        m_rowColor2 = color;

        ActivityManager actMgr = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);

        try {
            String androidIDStr = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            addBuild("Android ID", androidIDStr);

            try {
                AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(getContext());
                final String adIdStr = adInfo.getId();
                final boolean isLAT = adInfo.isLimitAdTrackingEnabled();
                addBuild("Ad ID", adIdStr);
            } catch (IOException e) {
                // Unrecoverable error connecting to Google Play services (e.g.,
                // the old version of the service doesn't support getting AdvertisingId).
            } catch (GooglePlayServicesNotAvailableException e) {
                // Google Play services is not available entirely.
            }

            /*
            try {
                InstanceID instanceID = InstanceID.getInstance(getContext());
                if (instanceID != null) {
                    // Requires a Google Developer project ID.
                    String authorizedEntity = "<need to make this on google developer site>";
                    instanceID.getToken(authorizedEntity, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    addBuild("Instance ID", instanceID.getId());
                }
            } catch (Exception ex) {
            }
            */

            ConfigurationInfo info = actMgr.getDeviceConfigurationInfo();
            addBuild("OpenGL", info.getGlEsVersion());
        } catch (Exception ex) {
            m_log.e(ex.getMessage());
        }


        // --------------- Connection Services -------------
        try {
            ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
            if (netInfo != null) {
                Map<String, String> netListStr = new LinkedHashMap<String, String>();

                putIf(netListStr, "Available", "Yes", netInfo.isAvailable());
                putIf(netListStr, "Connected", "Yes", netInfo.isConnected());
                putIf(netListStr, "Connecting", "Yes", !netInfo.isConnected() && netInfo.isConnectedOrConnecting());
                putIf(netListStr, "Roaming", "Yes", netInfo.isRoaming());
                putIf(netListStr, "Extra", netInfo.getExtraInfo(), !TextUtils.isEmpty(netInfo.getExtraInfo()));
                putIf(netListStr, "WhyFailed", netInfo.getReason(), !TextUtils.isEmpty(netInfo.getReason()));
                if (Build.VERSION.SDK_INT >= 16) {
                    putIf(netListStr, "Metered", "Avoid heavy use", connMgr.isActiveNetworkMetered());
                }

                netListStr.put("NetworkType", netInfo.getTypeName());
                if (connMgr.getAllNetworkInfo().length > 1) {
                    netListStr.put("Available Networks:", " ");
                    for (NetworkInfo netI : connMgr.getAllNetworkInfo()) {
                        if (netI.isAvailable()) {
                            netListStr.put(" " + netI.getTypeName(), netI.isAvailable() ? "Yes" : "No");
                        }
                    }
                }

                if (netInfo.isConnected()) {
                    try {
                        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                            NetworkInterface intf = en.nextElement();
                            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                                InetAddress inetAddress = enumIpAddr.nextElement();
                                if (!inetAddress.isLoopbackAddress()) {
                                    if (inetAddress.getHostAddress() != null) {
                                        String ipType = (inetAddress instanceof Inet4Address) ? "IPv4" : "IPv6";
                                        netListStr.put(intf.getName() + " " + ipType, inetAddress.getHostAddress());
                                    }
                                    // if (!TextUtils.isEmpty(inetAddress.getHostName()))
                                    //     listStr.put( "HostName", inetAddress.getHostName());
                                }
                            }
                        }
                    } catch (Exception ex) {
                        m_log.e("Network %s", ex.getMessage());
                    }
                }

                addBuild("Network...", netListStr);
            }
        } catch (Exception ex) {
            m_log.e("Network %s", ex.getMessage());
        }

        // --------------- Telephony Services -------------
        TelephonyManager telephonyManager =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            Map<String, String> cellListStr = new LinkedHashMap<String, String>();
            try {
                cellListStr.put("Version", telephonyManager.getDeviceSoftwareVersion());
                cellListStr.put("Number", telephonyManager.getLine1Number());
                cellListStr.put("Service", telephonyManager.getNetworkOperatorName());
                cellListStr.put("Roaming", telephonyManager.isNetworkRoaming() ? "Yes" : "No");
                cellListStr.put("Type", getNetworkTypeName(telephonyManager.getNetworkType()));

                if (Build.VERSION.SDK_INT >= 17) {
                    if (telephonyManager.getAllCellInfo() != null) {
                        for (CellInfo cellInfo : telephonyManager.getAllCellInfo()) {
                            String cellName = cellInfo.getClass().getSimpleName();
                            int level = 0;
                            if (cellInfo instanceof CellInfoCdma) {
                                level = ((CellInfoCdma) cellInfo).getCellSignalStrength().getLevel();
                            } else if (cellInfo instanceof CellInfoGsm) {
                                level = ((CellInfoGsm) cellInfo).getCellSignalStrength().getLevel();
                            } else if (cellInfo instanceof CellInfoLte) {
                                level = ((CellInfoLte) cellInfo).getCellSignalStrength().getLevel();
                            } else if (cellInfo instanceof CellInfoWcdma) {
                                if (Build.VERSION.SDK_INT >= 18) {
                                    level = ((CellInfoWcdma) cellInfo).getCellSignalStrength().getLevel();
                                }
                            }
                            cellListStr.put(cellName, "Level% " + String.valueOf(100 * level / 4));
                        }
                    }
                }

                for (NeighboringCellInfo cellInfo : telephonyManager.getNeighboringCellInfo()) {
                    int level = cellInfo.getRssi();
                    cellListStr.put("Cell level%", String.valueOf(100 * level / 31));
                }

            } catch (Exception ex) {
                m_log.e("Cell %s", ex.getMessage());
            }

            if (!cellListStr.isEmpty()) {
                addBuild("Cell...", cellListStr);
            }
        }

        // --------------- Bluetooth Services (API18) -------------
        if (Build.VERSION.SDK_INT >= 18) {
            try {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter != null) {

                    Map<String, String> btListStr = new LinkedHashMap<String, String>();

                    btListStr.put("Enabled", bluetoothAdapter.isEnabled() ? "yes" : "no");
                    btListStr.put("Name", bluetoothAdapter.getName());
                    btListStr.put("ScanMode", String.valueOf(bluetoothAdapter.getScanMode()));
                    btListStr.put("State", String.valueOf(bluetoothAdapter.getState()));
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    // If there are paired devices
                    if (pairedDevices.size() > 0) {
                        // Loop through paired devices
                        for (BluetoothDevice device : pairedDevices) {
                            // Add the name and address to an array adapter to show in a ListView
                            btListStr.put("Paired:" + device.getName(), device.getAddress());
                        }
                    }

                    BluetoothManager btMgr = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
                    if (btMgr != null) {
                        // btMgr.getAdapter().
                    }
                    addBuild("Bluetooth", btListStr);
                }

            } catch (Exception ex) {

            }
        }

        // --------------- Wifi Services -------------
    final WifiManager wifiMgr = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiMgr != null && wifiMgr.isWifiEnabled() && wifiMgr.getDhcpInfo() != null) {

            if (mSystemBroadcastReceiver == null) {
                mSystemBroadcastReceiver = new SystemBroadcastReceiver(wifiMgr);
                getActivity().registerReceiver(mSystemBroadcastReceiver, INTENT_FILTER_SCAN_AVAILABLE);
            }

            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                if (wifiMgr.getScanResults() == null ||
                        wifiMgr.getScanResults().size() != mLastScanSize) {
                    mLastScanSize = wifiMgr.getScanResults().size();
                    wifiMgr.startScan();
                }
            }

            Map<String, String> wifiListStr = new LinkedHashMap<String, String>();
            try {
                DhcpInfo dhcpInfo = wifiMgr.getDhcpInfo();

                wifiListStr.put("DNS1", Formatter.formatIpAddress(dhcpInfo.dns1));
                wifiListStr.put("DNS2", Formatter.formatIpAddress(dhcpInfo.dns2));
                wifiListStr.put("Default Gateway", Formatter.formatIpAddress(dhcpInfo.gateway));
                wifiListStr.put("IP Address", Formatter.formatIpAddress(dhcpInfo.ipAddress));
                wifiListStr.put("Subnet Mask", Formatter.formatIpAddress(dhcpInfo.netmask));
                wifiListStr.put("Server IP", Formatter.formatIpAddress(dhcpInfo.serverAddress));
                wifiListStr.put("Lease Time(sec)", String.valueOf(dhcpInfo.leaseDuration));

                WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                if (wifiInfo != null) {
                    wifiListStr.put("LinkSpeed Mbps", String.valueOf(wifiInfo.getLinkSpeed()));
                    int numberOfLevels = 10;
                    int level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels + 1);
                    wifiListStr.put("Signal%", String.valueOf(100 * level / numberOfLevels));
                    if (Build.VERSION.SDK_INT >= 23) {
                        wifiListStr.put("MAC", getMacAddr());
                    } else {
                        wifiListStr.put("MAC", wifiInfo.getMacAddress());
                    }
                }

            } catch (Exception ex) {
                m_log.e("Wifi %s", ex.getMessage());
            }

            if (!wifiListStr.isEmpty()) {
                addBuild("WiFi...", wifiListStr);
            }

            try {
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    List<ScanResult> listWifi = wifiMgr.getScanResults();
                    if (listWifi != null && !listWifi.isEmpty()) {
                        int idx = 0;

                        for (ScanResult scanResult : listWifi) {
                            Map<String, String> wifiScanListStr = new LinkedHashMap<String, String>();
                            wifiScanListStr.put("SSID", scanResult.SSID);
                            if (Build.VERSION.SDK_INT >= 23) {
                                wifiScanListStr.put("  Name", scanResult.operatorFriendlyName.toString());
                                wifiScanListStr.put("  Venue", scanResult.venueName.toString());
                            }

                            //        wifiScanListStr.put("  BSSID ",scanResult.BSSID);
                            wifiScanListStr.put("  Capabilities", scanResult.capabilities);
                            //       wifiScanListStr.put("  Center Freq", String.valueOf(scanResult.centerFreq0));
                            //       wifiScanListStr.put("  Freq width", String.valueOf(scanResult.channelWidth));
                            wifiScanListStr.put("  Level, Freq", String.format("%d, %d", scanResult.level, scanResult.frequency));
                            if (Build.VERSION.SDK_INT >= 17) {
                                Date wifiTime = new Date(scanResult.timestamp);
                                wifiScanListStr.put("  Time", wifiTime.toLocaleString());
                            }
                            addBuild(String.format("WiFiScan #%d", ++idx), wifiScanListStr);
                        }
                    }
                }
            } catch (Exception ex) {
                m_log.e("WifiList %s", ex.getMessage());
            }

            try {
                List<WifiConfiguration> listWifiCfg = wifiMgr.getConfiguredNetworks();

                for (WifiConfiguration wifiCfg : listWifiCfg) {
                    Map<String, String> wifiCfgListStr = new LinkedHashMap<String, String>();
                    if (Build.VERSION.SDK_INT >= 23) {
                        wifiCfgListStr.put("Name", wifiCfg.providerFriendlyName);
                    }
                    wifiCfgListStr.put("SSID", wifiCfg.SSID);
                    String netStatus = "";
                    switch (wifiCfg.status) {
                        case WifiConfiguration.Status.CURRENT:
                            netStatus = "Connected"; break;
                        case WifiConfiguration.Status.DISABLED:
                            netStatus = "Disabled"; break;
                        case WifiConfiguration.Status.ENABLED:
                            netStatus = "Enabled"; break;
                    }
                    wifiCfgListStr.put(" Status", netStatus);
                    wifiCfgListStr.put(" Priority", String.valueOf(wifiCfg.priority));
                    if (null != wifiCfg.wepKeys) {
         //               wifiCfgListStr.put(" wepKeys", TextUtils.join(",", wifiCfg.wepKeys));
                    }
                    String protocols = "";
                    if (wifiCfg.allowedProtocols.get(WifiConfiguration.Protocol.RSN))
                        protocols = "RSN ";
                    if (wifiCfg.allowedProtocols.get(WifiConfiguration.Protocol.WPA))
                        protocols = protocols + "WPA ";
                    wifiCfgListStr.put(" Protocols", protocols);

                    String keyProt = "";
                    if (wifiCfg.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE))
                        keyProt = "none";
                    if (wifiCfg.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP))
                        keyProt = "WPA+EAP ";
                    if (wifiCfg.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK))
                        keyProt = "WPA+PSK ";
                    wifiCfgListStr.put(" Keys", keyProt);

                    if (wifiCfg.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
                        // Remove network connections with no Password.
                        // wifiMgr.removeNetwork(wifiCfg.networkId);
                    }

                    addBuild("WiFiCfg #" + wifiCfg.networkId, wifiCfgListStr);
                }

            } catch (Exception ex) {
                m_log.e("Wifi Cfg List %s", ex.getMessage());
            }
        }


        if (expandAll) {
            // updateList();
            int count = m_list.size();
            for (int position = 0; position < count; position++)
                m_listView.expandGroup(position);
        }

        m_adapter.notifyDataSetChanged();
    }

    void addBuildIf(String name, String value, boolean ifValue) {
        if (ifValue)
            m_list.add(new BuildInfo(name, value));
    }

    void addBuild(String name, String value) {
        addBuildIf(name, value, !TextUtils.isEmpty(value));
    }

    void addBuild(String name, Map<String, String> value) {
        if (!value.isEmpty())
            m_list.add(new BuildInfo(name, value));
    }

    private void clean_networks() {
        StringBuilder sb = new StringBuilder();
        final WifiManager wifiMgr = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr != null && wifiMgr.isWifiEnabled() && wifiMgr.getDhcpInfo() != null) {
            try {
                List<WifiConfiguration> listWifiCfg = wifiMgr.getConfiguredNetworks();
                for (WifiConfiguration wifiCfg : listWifiCfg) {

                    if (wifiCfg.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {

                        // Remove network connections with no Password.
                        if (wifiMgr.removeNetwork(wifiCfg.networkId)) {
                            sb.append(wifiCfg.SSID);
                            sb.append("\n");
                        }
                    }
                }
            } catch (Exception ex) {

            }
        }

        if (sb.length() != 0) {
            Toast.makeText(this.getContext(), "Removed Networks: " + sb.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF)).append(":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }

    // ============================================================================================
    // DevFragment

    @Override
    public void onStop() {
        if (mSystemBroadcastReceiver != null) {
            getActivity().unregisterReceiver(mSystemBroadcastReceiver);
            mSystemBroadcastReceiver = null;
        }
        super.onStop();
    }

    // ============================================================================================
    // Internal methods

    // Put values in List ifValue true.
    private static <M extends Map<E, E>, E> void putIf(M listObj, E v1, E v2, boolean ifValue) {
        if (ifValue) {
            listObj.put(v1, v2);
        }
    }

    class BuildInfo {
        final String m_fieldStr;
        final String m_valueStr;
        final Map<String, String> m_valueList;

        BuildInfo() {
            m_fieldStr = m_valueStr = null;
            m_valueList = null;
        }

        BuildInfo(String str1, String str2) {
            m_fieldStr = str1;
            m_valueStr = str2;
            m_valueList = null;
        }

        BuildInfo(String str1, Map<String, String> list2) {
            m_fieldStr = str1;
            m_valueStr = null;
            m_valueList = list2;
        }

        public String toString() {
            return m_fieldStr;
        }

        public String fieldStr() {
            return m_fieldStr;
        }

        public String valueStr() {
            return m_valueStr;
        }

        public Map<String, String> valueListStr() {
            return m_valueList;
        }

        public int getCount() {
            return (m_valueList == null) ? 0 : m_valueList.size();
        }
    }

    final static int EXPANDED_LAYOUT = R.layout.build_list_row;
    final static int SUMMARY_LAYOUT = R.layout.build_list_row;

    /**
     * ExpandableLis UI 'data model' class
     */
    private class BuildArrayAdapter extends BaseExpandableListAdapter {
        private final LayoutInflater m_inflater;

        public BuildArrayAdapter(Context context) {
            m_inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        /**
         * Generated expanded detail view object.
         */
        @Override
        public View getChildView(final int groupPosition,
                                 final int childPosition, boolean isLastChild, View convertView,
                                 ViewGroup parent) {

            BuildInfo buildInfo = m_list.get(groupPosition);

            View expandView = convertView;
            if (null == expandView) {
                expandView = m_inflater.inflate(SUMMARY_LAYOUT, parent, false);
            }

            if (childPosition < buildInfo.valueListStr().keySet().size()) {
                String key = (String) buildInfo.valueListStr().keySet().toArray()[childPosition];
                String val = buildInfo.valueListStr().get(key);

                TextView textView = Ui.viewById(expandView, R.id.buildField);
                textView.setText(key);
                textView.setPadding(40, 0, 0, 0);

                textView = Ui.viewById(expandView, R.id.buildValue);
                textView.setText(val);

                if ((groupPosition & 1) == 1)
                    expandView.setBackgroundColor(m_rowColor1);
                else
                    expandView.setBackgroundColor(m_rowColor2);
            }

            return expandView;
        }

        @Override
        public int getGroupCount() {
            return m_list.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return m_list.get(groupPosition).getCount();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return null;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return 0;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        /**
         * Generate summary (row) presentation view object.
         */
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {

            BuildInfo buildInfo = m_list.get(groupPosition);

            View summaryView = convertView;
            if (null == summaryView) {
                summaryView = m_inflater.inflate(SUMMARY_LAYOUT, parent, false);
            }

            TextView textView;
            textView = Ui.viewById(summaryView, R.id.buildField);
            textView.setText(buildInfo.fieldStr());
            textView.setPadding(10, 0, 0, 0);

            textView = Ui.viewById(summaryView, R.id.buildValue);
            textView.setText(buildInfo.valueStr());

            if ((groupPosition & 1) == 1)
                summaryView.setBackgroundColor(m_rowColor1);
            else
                summaryView.setBackgroundColor(m_rowColor2);

            return summaryView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

    }

}