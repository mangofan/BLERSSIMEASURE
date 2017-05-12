package com.example.fanwe.blerssimeasure;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int ENABLE_BLUETOOTH = 1;
    Map<String, double[]> bleNodeLoc = new HashMap<>();    //固定节点的位置Map
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initBluetooth();
    }

    //蓝牙广播监听器
    public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        String dFinished = BluetoothAdapter.ACTION_DISCOVERY_FINISHED;

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("hh", "receive");
            BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String remoteMAC;
            final Short rssi;
            if (remoteDevice != null) {    //判断接受到的信息中设备是否为空
                remoteMAC = remoteDevice.getAddress();
                if (bleNodeLoc.containsKey(remoteMAC)) {   //判断接受到的蓝牙节点是否在已知的蓝牙节点map中
                    rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);

                }
            }
        }
    };


    //提示用户开启手机蓝牙
    private void initBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            //蓝牙未打开，提醒用户打开
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, ENABLE_BLUETOOTH);
            startDiscovery();
        }
    }

    ScheduledFuture<?> future;
    public void startDiscovery() {

        // TODO: 2017/5/12 看看这个intentfilter应该怎么用比较好 
        registerReceiver(mReceiver, new IntentFilter((BluetoothDevice.ACTION_FOUND)));
        if (bluetoothAdapter.isEnabled() && !bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.startDiscovery();
        }
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        cancelTask();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        future = executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.startDiscovery();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }
    public void cancelTask(){
        if(future!=null) {
            future.cancel(false);
            future = null;
        }

    }
}
