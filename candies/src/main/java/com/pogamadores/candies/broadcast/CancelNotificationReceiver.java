package com.pogamadores.candies.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.pogamadores.candies.util.Util;

import java.util.concurrent.TimeUnit;

public class CancelNotificationReceiver extends BroadcastReceiver {


    private GoogleApiClient mGoogleClient;

    public CancelNotificationReceiver() {}

    private void setUpGoogleClientIfNeeded(Context context) {
        if (mGoogleClient == null) {

            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context.getApplicationContext())
                    .addApi(Wearable.API)
                    .build();

            ConnectionResult connectionResult =
                    googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                Log.e(CancelNotificationReceiver.class.getSimpleName(), "Failed to connect to GoogleApiClient.");
            } else {
                mGoogleClient = googleApiClient;
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        setUpGoogleClientIfNeeded(context);
        Util.sendMessage(mGoogleClient, "/candies/notification", "cancel");
    }
}
