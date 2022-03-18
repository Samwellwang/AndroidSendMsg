package com.example.sendmsgbypost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class MyReceiver extends BroadcastReceiver {

    public static String webUrl="https://api.samwell.wang/接口地址";
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
//            SetShowContent("发送请求异常"+e);
        }
//        SetShowContent("收到了来自"+from+"的短信");
        Log.i("SendMsg","收到了来自"+from+"的短信"+body);
    }
}
