package br.com.novatrix.candies.service;

import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import br.com.novatrix.candies.util.Util;

import java.util.concurrent.TimeUnit;

/**
 * @author Igor Castañeda Ferreira - github.com/igorcferreira - @igorcferreira
 */
public class MessageService extends WearableListenerService {

    private static final String TAG = MessageService.class.getSimpleName();
    private GoogleApiClient mGoogleClient;

    public MessageService() {}

    private void setUpGoogleClientIfNeeded() {
        if(mGoogleClient == null) {
            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();

            ConnectionResult connectionResult =
                    googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                Log.e(TAG, "Failed to connect to GoogleApiClient.");
                Toast.makeText(getApplicationContext(), "Connection error", Toast.LENGTH_SHORT).show();
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
                Log.d("CandiesApplicationWatch", "Message arrived");
                String message = Util.extractMessage(event,"/new/candies/beacon");

                Log.d("CandiesApplicationWatch", "content of message: " + message);

                if(message != null && !Util.isForeground(getApplicationContext(), "br.com.novatrix.candies")) {
                    Uri uri = Uri.parse(message);
                    Util.dispatchNotification(getApplicationContext(), uri);
                    return;
                }

                message = Util.extractMessage(event, "/candies/notification");
                if(message != null && message.equalsIgnoreCase("cancel")) {
                    Util.cancelNotification(getApplicationContext());
                }
            }
        }

        dataEvents.close();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        //super.onMessageReceived(messageEvent);
        setUpGoogleClientIfNeeded();
    }

    @Override
    public void onPeerConnected(Node peer) {
        //super.onPeerConnected(peer);
        setUpGoogleClientIfNeeded();
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        //super.onPeerDisconnected(peer);
        setUpGoogleClientIfNeeded();
    }
}
