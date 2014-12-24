package com.pogamadores.candies.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.pogamadores.candies.R;
import com.pogamadores.candies.util.IntentParameters;

/**
 * This class are responsible to perform the payment for the item
 */
public class PaymentOrderReceiver extends BroadcastReceiver {

    public PaymentOrderReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null && intent.hasExtra(IntentParameters.UUID)) {
            //TODO: Perform the payment action
            Toast.makeText(context, R.string.action_purchase, Toast.LENGTH_SHORT).show();
        }
    }
}
