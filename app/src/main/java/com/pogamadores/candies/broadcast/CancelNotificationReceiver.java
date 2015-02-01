package com.pogamadores.candies.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.pogamadores.candies.application.CandiesApplication;
import com.pogamadores.candies.util.Util;

public class CancelNotificationReceiver extends BroadcastReceiver {
    public CancelNotificationReceiver(){}
    @Override
    public void onReceive(Context context, Intent intent) {
        Util.sendMessage(CandiesApplication.getGoogleClient(),"/candies/notification","cancel");
    }
}
