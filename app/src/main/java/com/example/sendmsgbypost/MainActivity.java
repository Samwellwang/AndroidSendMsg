package com.example.sendmsgbypost;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;


public class MainActivity extends AppCompatActivity {

    //自己的服务器 可不用
    public static String webUrl="https://api.samwell.wang/msg/sendMsg";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //注册电量变化广播
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBatInfoReveiver,filter);
        //注册短信广播
        MyReceiver yBroadCastReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(yBroadCastReceiver,intentFilter,
                "android.permission.RECEIVE_SMS", null);
        //检查读取短信权限
        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_SMS)!= PackageManager.PERMISSION_GRANTED
                ||ActivityCompat.checkSelfPermission(MainActivity.this,Manifest.permission.RECEIVE_SMS)!=PackageManager.PERMISSION_GRANTED
                ||ActivityCompat.checkSelfPermission(MainActivity.this
        ,Manifest.permission.INTERNET)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_SMS,Manifest.permission.RECEIVE_SMS,Manifest.permission.INTERNET},1);
        }
    }
    public static int nowPercent=0;

    public  void SetShowContent(String content){
        TextView t1 = (TextView)findViewById(R.id.text_view);
        t1.setMovementMethod(new ScrollingMovementMethod());
        SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();// 获取当前时间
        //将日志输出到app主页
        t1.append(sdf.format(date) +"   "+content+"\n");
    }
    private BroadcastReceiver mBatInfoReveiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            //电池变化处理
            int status=intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
            int current = intent.getExtras().getInt("level");// 获得当前电量
            int total = intent.getExtras().getInt("scale");// 获得总电量
            int percent = current * 100 / total;
            if(nowPercent!=percent){
                //只有电池变化时候,才输出日志
                Log.i("SendMsg","当前电量"+percent);
                SetShowContent("电池变化了,当前电量"+percent);
                nowPercent=percent;
            }
            //小于20%通知server酱
            if(percent<20&&status!=BatteryManager.BATTERY_STATUS_CHARGING){
                Map<String, String> params = new HashMap<String, String>();
                params.put("para","电量小于20%啦,快充电!当前电量:"+current+",总电量:"+total+",百分比:"+percent);
                try{
                    Thread thread = new MainActivity.MyThread1(webUrl,params);
                    thread.start();
                }
                catch (Exception e){
                    Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
                    System.out.println(e);
                }
            }
        }
    };
    public class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            //获得短信
            Object[] objs = (Object[]) arg1.getExtras().get("pdus");
            StringBuilder sb=new StringBuilder();
            String from="";
            for(Object obj : objs)
            {
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) obj);
                from = smsMessage.getOriginatingAddress();
                sb.append(smsMessage.getMessageBody());
            }
            String body=sb.toString();
            //服务器备份
            try{
                Map<String, String> selfParams = new HashMap<String, String>();
                selfParams.put("content", body);
                selfParams.put("phone", from);
                Thread thread = new com.example.sendmsgbypost.MainActivity.MyThread1(webUrl,selfParams);
                thread.start();
            }
            catch (Exception e){
                Toast.makeText(arg0, e.toString(), Toast.LENGTH_LONG).show();
                Log.i("SendMsg","发送请求异常"+e);
                SetShowContent("发送请求异常"+e);
            }
            SetShowContent("收到了来自"+from+"的短信");
            Log.i("SendMsg","收到了来自"+from+"的短信"+body);
        }
    }

    public static class MyThread1 extends Thread
    {
        private Map<String, String> params;
        private String url;
        public MyThread1(String url,Map<String, String> params)
        {
            //线程参数
            this.params = params;
            this.url=url;
        }
        public void run()
        {
            try {
                //发送post通知
                boolean result = sendPostRequest(url, params, "UTF-8");
            }
            catch (Exception e){
                //通知失败
                Log.i("SendMsg","post失败了"+e);
            }
        }
    }
    public static boolean sendPostRequest(String path,
                                          Map<String, String> params, String encoding) throws Exception {
        StringBuilder data = new StringBuilder();
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                data.append(entry.getKey()).append("=");
                data.append(URLEncoder.encode(entry.getValue(), encoding));// 编码
                data.append('&');
            }
            data.deleteCharAt(data.length() - 1);
        }
        byte[] entity = data.toString().getBytes(); // 得到实体数据
        FakeX509TrustManager.allowAllSSL();
        HttpsURLConnection connection = (HttpsURLConnection) new URL(path)
                .openConnection();
        connection.setConnectTimeout(5000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length",
                String.valueOf(entity.length));

        connection.setDoOutput(true);// 允许对外输出数据

//        connection.setSSLSocketFactory(context.getSocketFactory());
        connection.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(entity);

        if (connection.getResponseCode() == 200) {
            return true;
        }
        return false;
    }
}

