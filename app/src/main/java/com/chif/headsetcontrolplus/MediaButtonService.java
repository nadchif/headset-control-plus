package com.chif.headsetcontrolplus;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class MediaButtonService extends Service {

    //from MyService to MainActivity
    final static String KEY_INT_FROM_SERVICE = "KEY_INT_FROM_SERVICE";
    final static String KEY_STRING_FROM_SERVICE = "KEY_STRING_FROM_SERVICE";
    final static String ACTION_UPDATE_CNT = "UPDATE_CNT";
    final static String ACTION_UPDATE_MSG = "UPDATE_MSG";

    //from MainActivity to MyService
    final static String KEY_MSG_TO_SERVICE = "KEY_MSG_TO_SERVICE";
    final static String ACTION_MSG_TO_SERVICE = "MSG_TO_SERVICE";

    MyServiceReceiver myServiceReceiver;
    MyServiceThread myServiceThread;
    int cnt;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.e("CHIF", "started service");
        Toast.makeText(getApplicationContext(),
                "onCreate", Toast.LENGTH_LONG).show();
        myServiceReceiver = new MyServiceReceiver();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getApplicationContext(),
                "onStartCommand", Toast.LENGTH_LONG).show();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_MSG_TO_SERVICE);
        registerReceiver(myServiceReceiver, intentFilter);

        myServiceThread = new MyServiceThread();
        myServiceThread.start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(getApplicationContext(),
                "onDestroy", Toast.LENGTH_LONG).show();
        myServiceThread.setRunning(false);
        unregisterReceiver(myServiceReceiver);
        super.onDestroy();
    }

    public class MyServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if(action.equals(ACTION_MSG_TO_SERVICE)){
                String msg = intent.getStringExtra(KEY_MSG_TO_SERVICE);

                msg = new StringBuilder(msg).reverse().toString();

                //send back to MainActivity
                Intent i = new Intent();
                i.setAction(ACTION_UPDATE_MSG);
                i.putExtra(KEY_STRING_FROM_SERVICE, msg);
                sendBroadcast(i);
            }
        }
    }

    private class MyServiceThread extends Thread{

        private boolean running;

        public void setRunning(boolean running){
            this.running = running;
        }

        @Override
        public void run() {
            cnt = 0;
            running = true;
            while (running){
                try {
                    Thread.sleep(1000);

                    Intent intent = new Intent();
                    intent.setAction(ACTION_UPDATE_CNT);
                    intent.putExtra(KEY_INT_FROM_SERVICE, cnt);
                    sendBroadcast(intent);

                    cnt++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
