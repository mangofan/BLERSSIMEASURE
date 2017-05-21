package com.example.fanwe.blerssimeasure;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import utlis.MyUtlis;

public class MainActivity extends AppCompatActivity {

    TextView text1,text2;

    int RSSI_LIMIT = 5, BLE_CHOOSED_NUM = 4;
    private static final int ENABLE_BLUETOOTH = 1;
    Map<String, double[]> bleNodeLoc = new HashMap<>();    //固定节点的位置Map
    Map<String, ArrayList<Double>> mAllRssi = new HashMap<>();    //储存RSSI的MAP
    Map<String, Double> mRssiFilterd = new HashMap<>();     //过滤后的RSSI的Map
    private static final String TAG = "hh";
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        text1 = (TextView)findViewById(R.id.textView1);
        text2 = (TextView)findViewById(R.id.textView2);
        initBleMap();
        initBluetooth();
    }


    // TODO: 2017/5/18 测试老师的方法
    //蓝牙广播监听器
    public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        String dFinished = BluetoothAdapter.ACTION_DISCOVERY_FINISHED;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (dFinished.equals(intent.getAction())) {
                bluetoothAdapter.startDiscovery();
                Log.d("hh","finish");
            } else {
                BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                final Short rssi;
                if (remoteDevice != null) {    //判断接受到的信息中设备是否为空
                    String remoteMAC = remoteDevice.getAddress();
//                    Log.d("hh", remoteMAC);
                    if (bleNodeLoc.containsKey(remoteMAC)) {   //判断接受到的蓝牙节点是否在已知的蓝牙节点map中
                        rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
                        String need1 = remoteMAC + " " + rssi + "\n";;
                        if (mAllRssi.containsKey(remoteMAC)) {    //判断之前是否接收过相同蓝牙节点的广播，分别处理
                            ArrayList<Double> list1 = mAllRssi.get(remoteMAC);
                            list1.add(0, (double) rssi);   //因为是引用，所以直接修改的是原对象本身
                        } else {
                            ArrayList<Double> list = new ArrayList<>();
                            list.add((double) rssi);   //如果这个MAC地址没有出现过，建立list存储历次rssi
                            mAllRssi.put(remoteMAC, list);
                        }
                        double getAvgOfFilterdRssiValueList = MyUtlis.LogarNormalDistribution(mAllRssi.get(remoteMAC), RSSI_LIMIT);  //获取滤波后的信号强度表和强度平均值
                        mRssiFilterd.put(remoteMAC, getAvgOfFilterdRssiValueList);   //更新MAC地址对应信号强度的map
                        if (mRssiFilterd.size() > 2) {
                            SparseArray<ArrayList<String>> SortedNodeMacAndRssi = MyUtlis.sortNodeBasedOnRssi(mRssiFilterd, BLE_CHOOSED_NUM);     //得到按距离排序的蓝牙节点的列表
                            for (int i = 0; i < SortedNodeMacAndRssi.get(1).size(); i++) {
                                need1 += SortedNodeMacAndRssi.get(1).get(i) + " " + SortedNodeMacAndRssi.get(2).get(i) + "\n";
                            }
                            text1.setText(need1);
                            String nearestNode = "19:18:FC:01:F0:FD";
                            if(mAllRssi.containsKey(nearestNode)) {
                                ArrayList<Double> nearestNodeList = mAllRssi.get(nearestNode);
                                String need2 = nearestNode + "\n";
                                for (int i = 0; i < nearestNodeList.size(); i++) {
                                    need2 += nearestNodeList.get(i) + "\n";
                                }
                                text2.setText(need2);
                            }
                        }
                    }
//                    else{
//                        Log.d(TAG, "mac dont want");
//                    }
                }
//                else{
//                    Log.d(TAG, "device null");
//                }
                Log.d(TAG,"receive");
            }
        }
    };


    //提示用户开启手机蓝牙
    public void initBluetooth() {
        if (android.os.Build.VERSION.SDK_INT >= 23){
            ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION},2);
//            LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
//            boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//            if(!gps){
//                Settings.Secure.setLocationProviderEnabled( getContentResolver(), LocationManager.GPS_PROVIDER, true);
//            }
        }
        if (!bluetoothAdapter.isEnabled()) {
            //蓝牙未打开，提醒用户打开
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, ENABLE_BLUETOOTH);
        }
        startDiscovery();
    }

    public void startDiscovery() {
        registerReceiver(mReceiver, new IntentFilter((BluetoothDevice.ACTION_FOUND)));
        registerReceiver(mReceiver, new IntentFilter((BluetoothAdapter.ACTION_DISCOVERY_FINISHED)));
        bluetoothAdapter.startDiscovery();
    }

    //初始化存储节点位置和MAC地址的Map
    private void initBleMap(){
        double[] location21 = {11.5, 0.7};
        double[] location22 = {15.8, 0.7};
        double[] location23 = {7.8, 4.7};
        double[] location24 = {11.8, 4.7};
        double[] location25 = {15.8, 4.7};
        double[] location26 = {19.8, 4.7};
        double[] location27 = {7.8, 8.7};
        double[] location28 = {11.8, 8.7};
        double[] location29 = {15.8, 8.7};
        double[] location30 = {19.8, 8.7};

        bleNodeLoc.put("19:18:FC:01:F1:0E", location21);
        bleNodeLoc.put("19:18:FC:01:F1:0F", location22);
        bleNodeLoc.put("19:18:FC:01:F0:F8", location23);
        bleNodeLoc.put("19:18:FC:01:F0:F9", location24);
        bleNodeLoc.put("19:18:FC:01:F0:FA", location25);
        bleNodeLoc.put("19:18:FC:01:F0:FB", location26);
        bleNodeLoc.put("19:18:FC:01:F0:FC", location27);
        bleNodeLoc.put("19:18:FC:01:F0:FD", location28);
        bleNodeLoc.put("19:18:FC:01:F0:FE", location29);
        bleNodeLoc.put("19:18:FC:01:F0:FF", location30);
    }

    @Override
    protected void onPause() {
        bluetoothAdapter.cancelDiscovery();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bluetoothAdapter.startDiscovery();
    }
}
