package com.pogamadores.candies.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;

import com.pogamadores.candies.application.CandiesApplication;
import com.pogamadores.candies.database.CandieSQLiteDataSource;
import com.pogamadores.candies.domain.Token;
import com.pogamadores.candies.service.PaymentService;
import com.pogamadores.candies.ui.activity.PermissionActivity;
import com.pogamadores.candies.util.IntentParameters;
import com.pogamadores.candies.util.Util;

/**
 * This class are responsible to perform the payment for the item
 */
public class PaymentOrderReceiver extends BroadcastReceiver {

    public PaymentOrderReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {

        NotificationManagerCompat.from(context).cancel(Util.NOTIFICATION_ID);

        if(intent != null && intent.hasExtra(IntentParameters.UUID)) {
            CandieSQLiteDataSource dataSource = CandiesApplication.getDatasource();

            if(dataSource != null) {
                Token token = dataSource.getToken();
                if(token != null) {

                    Util.sendMessage(
                            CandiesApplication.getGoogleClient(),
                            "/candies/payment",
                            "start"
                    );

                    Intent serviceIntent = new Intent(context, PaymentService.class);
                    serviceIntent.putExtras(intent.getExtras());
                    serviceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startService(serviceIntent);
                    return;
                }
            }

            Util.sendMessage(
                    CandiesApplication.getGoogleClient(),
                    "/candies/payment",
                    "token"
            );

            Intent webIntent = new Intent(context, PermissionActivity.class);
            webIntent.putExtras(intent.getExtras());
            webIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(webIntent);
        }
    }
}
