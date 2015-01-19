package com.pogamadores.candies.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.pogamadores.candies.R;
import com.pogamadores.candies.util.IntentParameters;
import com.pogamadores.candies.util.Util;

public class MainActivity extends Activity {

    private TextView mTextView;
    private GoogleApiClient mGoogleClient;

    private void setUpGoogleClientIfNeeded(final TextView messageField, final Intent intent) {
        if(mGoogleClient == null) {

            mGoogleClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();
            mGoogleClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                    messageField.setText("Connected");
                    if(intent != null && intent.getExtras() != null && intent.hasExtra(IntentParameters.REQUEST_CODE)) {
                        messageField.setText("Purchasing");
                        Util.requestPurchase(mGoogleClient,"/purchase/candies", intent.getExtras());
                    }
                }

                @Override
                public void onConnectionSuspended(int i) {
                    messageField.setText("Error");
                }
            });

            mGoogleClient.registerConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult connectionResult) {
                    messageField.setText("Error");
                }
            });
            messageField.setText("Connecting");
            mGoogleClient.connect();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationManagerCompat.from(getApplicationContext()).cancel(Util.NOTIFICATION_ID);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                setUpGoogleClientIfNeeded(mTextView, getIntent());
            }
        });
    }
}