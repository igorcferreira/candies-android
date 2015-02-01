package com.pogamadores.candies.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.pogamadores.candies.util.Util;

public class CancelNotificationReceiver extends BroadcastReceiver {


    private GoogleApiClient mGoogleClient;

    public CancelNotificationReceiver() {}

    private boolean setUpGoogleClientIfNeeded(Context context) {
        if (mGoogleClient == null) {

            mGoogleClient = new GoogleApiClient.Builder(context.getApplicationContext())
                    .addApi(Wearable.API)
                    .build();
            mGoogleClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                    Util.sendMessage(mGoogleClient, "/candies/notification", "cancel");
                }
                @Override
                public void onConnectionSuspended(int i) {
                    mGoogleClient = null;
                }
            });

            mGoogleClient.registerConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult connectionResult) {
                    mGoogleClient = null;
                }
            });
            mGoogleClient.connect();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(setUpGoogleClientIfNeeded(context))
            Util.sendMessage(mGoogleClient, "/candies/notification", "cancel");
    }
}
