package br.com.novatrix.candies.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import br.com.novatrix.candies.util.Util;

/**
 * @author Igor Castañeda Ferreira - github.com/igorcferreira - @igorcferreira
 */
public class CancelNotificationReceiver extends BroadcastReceiver {


    private GoogleApiClient mGoogleClient;

    public CancelNotificationReceiver() {}

    private void setUpGoogleClientIfNeeded(Context context) {
        if (mGoogleClient == null) {

            mGoogleClient = new GoogleApiClient.Builder(context.getApplicationContext())
                    .addApi(Wearable.API)
                    .build();

            mGoogleClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {}
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
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        setUpGoogleClientIfNeeded(context);
        Util.sendMessage(mGoogleClient, "/candies/notification", "cancel");
    }
}
