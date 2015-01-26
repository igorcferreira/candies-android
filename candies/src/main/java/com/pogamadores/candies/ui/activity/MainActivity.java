package com.pogamadores.candies.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;
import com.pogamadores.candies.R;
import com.pogamadores.candies.util.IntentParameters;
import com.pogamadores.candies.util.RequestCode;
import com.pogamadores.candies.util.Util;

public class MainActivity extends Activity implements DataApi.DataListener {

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
                    if(intent != null && intent.getExtras() != null && intent.hasExtra(IntentParameters.REQUEST_CODE)) {
                        Util.requestPurchase(mGoogleClient,"/purchase/candies", intent.getExtras());
                    }
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
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Wearable.DataApi.removeListener(mGoogleClient, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Wearable.DataApi.addListener(mGoogleClient,this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleClient, this);
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
                mTextView.setText("Aproxime-se de uma máquina");
                setUpGoogleClientIfNeeded(mTextView, getIntent());
                Util.sendMessage(
                        mGoogleClient,
                        "/candies/beacon",
                        "search"
                );
            }
        });
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent event : dataEvents) {

            final String newMessage = Util.extractMessage(event, "/new/candies/beacon");
            if(newMessage != null) {
                mTextView.setText("Novo beacon");
                mTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Uri uri = Uri.parse(newMessage);

                        Bundle infoBundle = new Bundle();
                        infoBundle.putString(IntentParameters.UUID, uri.getQueryParameter("uuid"));
                        infoBundle.putString(IntentParameters.MAJOR, uri.getQueryParameter("major"));
                        infoBundle.putString(IntentParameters.MINOR, uri.getQueryParameter("minor"));
                        infoBundle.putInt(IntentParameters.REQUEST_CODE, RequestCode.PURCHASE);

                        Util.requestPurchase(mGoogleClient,"/purchase/candies", infoBundle);
                        mTextView.setOnClickListener(null);
                    }
                });
                return;
            }

            String message = Util.extractMessage(event, "/candies/payment");
            if(message != null) {
                mTextView.setText(message);
                return;
            }
        }
    }
}
