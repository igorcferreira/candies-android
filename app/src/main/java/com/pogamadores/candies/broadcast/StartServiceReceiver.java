package com.pogamadores.candies.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.pogamadores.candies.service.BeaconDiscoverService;

/**
 * It will be responsible to start the {@link BeaconDiscoverService} when needed
 */
public class StartServiceReceiver extends BroadcastReceiver {

    public StartServiceReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {
            Intent service = new Intent(context, BeaconDiscoverService.class);
            if (intent != null && intent.getExtras() != null)
                service.putExtras(intent.getExtras());
            context.startService(service);
    }
}
