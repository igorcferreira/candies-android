package com.pogamadores.candies.service;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.pogamadores.candies.application.CandiesApplication;
import com.pogamadores.candies.ui.activity.PermissionActivity;
import com.pogamadores.candies.util.IntentParameters;
import com.pogamadores.candies.util.Util;

import org.altbeacon.beacon.Beacon;

import java.util.concurrent.TimeUnit;

public class WearableListener extends WearableListenerService {

    private static final String TAG = WearableListener.class.getSimpleName();
    private GoogleApiClient mGoogleClient;

    public WearableListener() {}

    private void setUpGoogleClientIfNeeded() {
        if(mGoogleClient == null) {
            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();

            ConnectionResult connectionResult =
                    googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                Log.e(TAG, "Failed to connect to GoogleApiClient.");
                return;
            }
            mGoogleClient = googleApiClient;
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        //super.onDataChanged(dataEvents);
        setUpGoogleClientIfNeeded();

        for(DataEvent event : dataEvents) {
            if(event.getType() == DataEvent.TYPE_CHANGED) {
                if(event.getDataItem().getUri().getPath().equalsIgnoreCase("/purchase/candies")) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Uri message = Uri.parse(dataMapItem.getDataMap().getString("content"));

                    Bundle infoBundle = new Bundle();
                    infoBundle.putString(IntentParameters.UUID, message.getQueryParameter("uuid"));
                    infoBundle.putString(IntentParameters.MAJOR, message.getQueryParameter("major"));
                    infoBundle.putString(IntentParameters.MINOR, message.getQueryParameter("minor"));

                    if (CandiesApplication.getDatasource().getToken() == null) {
                        Util.sendMessage(
                                mGoogleClient,
                                "/candies/payment",
                                "token"
                        );

                        Intent permissionIntent = new Intent(getApplicationContext(), PermissionActivity.class);
                        permissionIntent.putExtras(infoBundle);
                        permissionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        getApplicationContext().startActivity(permissionIntent);
                    } else {
                        Util.sendMessage(
                                mGoogleClient,
                                "/candies/payment",
                                "start"
                        );

                        Intent purchaseIntent = new Intent(getApplicationContext(), PaymentService.class);
                        purchaseIntent.putExtras(infoBundle);
                        getApplicationContext().startService(purchaseIntent);
                    }
                } else if(event.getDataItem().getUri().getPath().equalsIgnoreCase("/candies/beacon")) {
                    Beacon beacon = CandiesApplication.get().getBeacon();
                    if(beacon == null) {
                        Intent service = new Intent(getApplicationContext(), BeaconDiscoverService.class);
                        getApplicationContext().startService(service);
                    } else {
                        Util.informNewBeacon(
                                mGoogleClient,
                                "/new/candies/beacon",
                                beacon
                        );
                    }
                } else if(event.getDataItem().getUri().getPath().equalsIgnoreCase("/candies/beacon")) {

                    if(CandiesApplication.get().getBeacon() != null) {
                        Util.informNewBeacon(
                                mGoogleClient,
                                "/new/candies/beacon",
                                CandiesApplication.get().getBeacon()
                        );
                    }

                    getApplicationContext().startService(new Intent(getApplicationContext(), BeaconDiscoverService.class));
                } else if(event.getDataItem().getUri().getPath().equalsIgnoreCase("/candies/notification")) {
                    Util.cancelNotification(getApplicationContext());
                }
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        //super.onMessageReceived(messageEvent);
        setUpGoogleClientIfNeeded();
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        //super.onPeerDisconnected(peer);
        setUpGoogleClientIfNeeded();
    }

    @Override
    public void onPeerConnected(Node peer) {
        //super.onPeerConnected(peer);
        setUpGoogleClientIfNeeded();
    }
}
