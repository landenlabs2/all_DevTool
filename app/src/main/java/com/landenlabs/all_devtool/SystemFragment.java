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
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.ProcessErrorStateInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
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
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

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
 * Display system information.
 *
 * @author Dennis Lang
 */
public class SystemFragment extends DevFragment {
    // Logger - set to LLog.DBG to only log in Debug build, use LLog.On for always log.
    private final LLog m_log = LLog.DBG;

    final ArrayList<BuildInfo> m_list = new ArrayList<BuildInfo>();
    ExpandableListView m_listView;
    TextView m_titleTime;
    BuildArrayAdapter m_adapter;

    SubMenu m_menu;

    public static String s_name = "System";
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


    public SystemFragment() {
    }

    public static DevFragment create() {
        return new SystemFragment();
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

        try {
            long heapSize = Debug.getNativeHeapSize();
            // long maxHeap = Runtime.getRuntime().maxMemory();

            // ConfigurationInfo cfgInfo = actMgr.getDeviceConfigurationInfo();
            int largHeapMb = actMgr.getLargeMemoryClass();
            int heapMb = actMgr.getMemoryClass();

            MemoryInfo memInfo = new MemoryInfo();
            actMgr.getMemoryInfo(memInfo);

            final String sFmtMB = "%.2f MB";
            Map<String, String> listStr = new TreeMap<String, String>();
            listStr.put("Mem Available (now)", String.format(sFmtMB, (double) memInfo.availMem / MB));
            listStr.put("Mem LowWhenOnlyAvail", String.format(sFmtMB, (double) memInfo.threshold / MB));
            if (Build.VERSION.SDK_INT >= 16) {
                listStr.put("Mem Installed", String.format(sFmtMB, (double) memInfo.totalMem / MB));
            }
            listStr.put("Heap (this app)", String.format(sFmtMB, (double) heapSize / MB));
            listStr.put("HeapMax (default)", String.format(sFmtMB, (double) heapMb));
            listStr.put("HeapMax (large)", String.format(sFmtMB, (double) largHeapMb));
            addBuild("Memory...", listStr);
        } catch (Exception ex) {
            m_log.e(ex.getMessage());
        }

        try {
            List<ProcessErrorStateInfo> procErrList = actMgr.getProcessesInErrorState();
            int errCnt = (procErrList == null ? 0 : procErrList.size());
            procErrList = null;

            // List<RunningAppProcessInfo> procList = actMgr.getRunningAppProcesses();
            int procCnt = actMgr.getRunningAppProcesses().size();
            int srvCnt = actMgr.getRunningServices(100).size();

            Map<String, String> listStr = new TreeMap<String, String>();
            listStr.put("#Processes", String.valueOf(procCnt));
            listStr.put("#Proc With Err", String.valueOf(errCnt));
            listStr.put("#Services", String.valueOf(srvCnt));
            // Requires special permission
            //	int taskCnt = actMgr.getRunningTasks(100).size();
            //	listStr.put("#Tasks",  String.valueOf(taskCnt));
            addBuild("Processes...", listStr);
        } catch (Exception ex) {
            m_log.e("System-Processes %s", ex.getMessage());
        }

        try {
            Map<String, String> listStr = new LinkedHashMap<String, String>();
            listStr.put("LargeIconDensity", String.valueOf(actMgr.getLauncherLargeIconDensity()));
            listStr.put("LargeIconSize", String.valueOf(actMgr.getLauncherLargeIconSize()));
            putIf(listStr, "isRunningInTestHarness", "Yes", ActivityManager.isRunningInTestHarness());
            putIf(listStr, "isUserAMonkey", "Yes", ActivityManager.isUserAMonkey());
            addBuild("Misc...", listStr);
        } catch (Exception ex) {
            m_log.e("System-Misc %s", ex.getMessage());
        }

        // --------------- Locale / Timezone -------------
        try {
            Locale ourLocale = Locale.getDefault();
            Date m_date = new Date();
            TimeZone tz = TimeZone.getDefault();

            Map<String, String> localeListStr = new LinkedHashMap<String, String>();

            localeListStr.put("Locale Name", ourLocale.getDisplayName());
            localeListStr.put(" Variant", ourLocale.getVariant());
            localeListStr.put(" Country", ourLocale.getCountry());
            localeListStr.put(" Country ISO", ourLocale.getISO3Country());
            localeListStr.put(" Language", ourLocale.getLanguage());
            localeListStr.put(" Language ISO", ourLocale.getISO3Language());
            localeListStr.put(" Language Dsp", ourLocale.getDisplayLanguage());

            localeListStr.put("TimeZoneID", tz.getID());
            localeListStr.put(" DayLightSavings", tz.useDaylightTime() ? "Yes" : "No");
            localeListStr.put(" In DLS", tz.inDaylightTime(m_date) ? "Yes" : "No");
            localeListStr.put(" Short Name", tz.getDisplayName(false, TimeZone.SHORT, ourLocale));
            localeListStr.put(" Long Name", tz.getDisplayName(false, TimeZone.LONG, ourLocale));

            addBuild("Locale TZ...", localeListStr);
        } catch (Exception ex) {
            m_log.e("Locale/TZ %s", ex.getMessage());
        }

        // --------------- Location Services -------------
        try {
            Map<String, String> listStr = new LinkedHashMap<String, String>();

            final LocationManager locMgr = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

            GpsStatus gpsStatus = locMgr.getGpsStatus(null);
            if (gpsStatus != null) {
                listStr.put("Sec ToGetGPS", String.valueOf(gpsStatus.getTimeToFirstFix()));

                Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();
                Iterator<GpsSatellite> sat = satellites.iterator();
                while (sat.hasNext()) {
                    GpsSatellite satellite = sat.next();

                    putIf(listStr,
                            String.format("Azm:%.0f, Elev:%.0f", satellite.getAzimuth(), satellite.getElevation()),
                            String.format("%.2f Snr", satellite.getSnr()),
                            satellite.usedInFix());
                }
            }

            Location location = null;
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                location = locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (null == location)
                    location = locMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (null == location)
                    location = locMgr.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }

            if (null != location) {
                listStr.put(location.getProvider() + " lat,lng", String.format("%.3f, %.3f", location.getLatitude(), location.getLongitude()));
            }
            if (listStr.size() != 0) {
                List<String> gpsProviders = locMgr.getAllProviders();
                int idx = 1;
                for (String providerName : gpsProviders) {
                    LocationProvider provider = locMgr.getProvider(providerName);
                    if (null != provider) {
                        listStr.put(providerName, (locMgr.isProviderEnabled(providerName) ? "On " : "Off ") +
                                String.format("Accuracy:%d Pwr:%d", provider.getAccuracy(), provider.getPowerRequirement()));
                    }
                }
                addBuild("GPS...", listStr);
            } else
                addBuild("GPS", "Off");
        } catch (Exception ex) {
            m_log.e(ex.getMessage());
        }

        // --------------- Application Info -------------
        ApplicationInfo appInfo = getActivity().getApplicationInfo();
        if (null != appInfo) {
            Map<String, String> appList = new LinkedHashMap<String, String>();
            try {
                appList.put("ProcName", appInfo.processName);
                appList.put("PkgName", appInfo.packageName);
                appList.put("DataDir", appInfo.dataDir);
                appList.put("SrcDir", appInfo.sourceDir);
            //    appList.put("PkgResDir", getActivity().getPackageResourcePath());
           //     appList.put("PkgCodeDir", getActivity().getPackageCodePath());
                String[] dbList = getActivity().databaseList();
                if (dbList != null && dbList.length != 0)
                    appList.put("DataBase", dbList[0]);
                // getActivity().getComponentName().

            } catch (Exception ex) {
            }
            addBuild("AppInfo...", appList);
        }

        // --------------- Account Services -------------
        final AccountManager accMgr = (AccountManager) getActivity().getSystemService(Context.ACCOUNT_SERVICE);
        if (null != accMgr) {
            Map<String, String> strList = new LinkedHashMap<String, String>();
            try  {
                for (Account account : accMgr.getAccounts()) {
                    strList.put(account.name, account.type);
                }
            } catch (Exception ex) {
                m_log.e(ex.getMessage());
            }
            addBuild("Accounts...", strList);
        }

        // --------------- Package Features -------------
        PackageManager pm = getActivity().getPackageManager();
        FeatureInfo[] features = pm.getSystemAvailableFeatures();
        if (features != null) {
            Map<String, String> strList = new LinkedHashMap<String, String>();
            for (FeatureInfo featureInfo : features) {
                strList.put(featureInfo.name, "");
            }
            addBuild("Features...", strList);
        }

        // --------------- Sensor Services -------------
        final SensorManager senMgr = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        if (null != senMgr) {
            Map<String, String> strList = new LinkedHashMap<String, String>();
            // Sensor accelerometer = senMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            // senMgr.registerListener(foo, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            List<Sensor> listSensor = senMgr.getSensorList(Sensor.TYPE_ALL);
            try {
                for (Sensor sensor : listSensor) {
                      strList.put(sensor.getName(), sensor.getVendor());
                }
            } catch (Exception ex) {
                m_log.e(ex.getMessage());
            }
            addBuild("Sensors...", strList);
        }

        try {
            if (Build.VERSION.SDK_INT >= 17) {
                final UserManager userMgr = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
                if (null != userMgr) {
                    try {
                        addBuild("UserName", userMgr.getUserName());
                    } catch (Exception ex) {
                        m_log.e(ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
        }

        try {
            Map<String, String> strList = new LinkedHashMap<String, String>();
            int screenTimeout = Settings.System.getInt(getActivity().getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
            strList.put("ScreenTimeOut", String.valueOf(screenTimeout / 1000));
            int rotate = Settings.System.getInt(getActivity().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
            strList.put("RotateEnabled", String.valueOf(rotate));
            if (Build.VERSION.SDK_INT >= 17) {
                // Global added in API 17
                int adb = Settings.Global.getInt(getActivity().getContentResolver(), Settings.Global.ADB_ENABLED);
                strList.put("AdbEnabled", String.valueOf(adb));
            }
            addBuild("Settings...", strList);
        } catch (Exception ex) {
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