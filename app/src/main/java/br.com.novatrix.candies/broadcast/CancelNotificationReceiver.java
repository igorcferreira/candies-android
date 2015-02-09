package br.com.novatrix.candies.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import br.com.novatrix.candies.application.CandiesApplication;
import br.com.novatrix.candies.util.Util;

/**
 * @author Igor Castañeda Ferreira - github.com/igorcferreira - @igorcferreira
 */
public class CancelNotificationReceiver extends BroadcastReceiver {
    public CancelNotificationReceiver(){}
    @Override
    public void onReceive(Context context, Intent intent) {
        Util.sendMessage(CandiesApplication.getGoogleClient(),"/candies/notification","cancel");
    }
}
