package com.pogamadores.candies.util;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.pogamadores.candies.R;
import com.pogamadores.candies.ui.activity.MainActivity;

public class Util {
    public static final int NOTIFICATION_ID = 23156;

    public static void dispatchNotification(Context context, Uri uri) {

        Bundle infoBundle = new Bundle();
        infoBundle.putString(IntentParameters.UUID, uri.getQueryParameter("uuid"));
        infoBundle.putString(IntentParameters.MAJOR, uri.getQueryParameter("major"));
        infoBundle.putString(IntentParameters.MINOR, uri.getQueryParameter("minor"));
        infoBundle.putInt(IntentParameters.REQUEST_CODE, RequestCode.PURCHASE);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtras(infoBundle);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                RequestCode.PURCHASE,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLocalOnly(true)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_PROMO)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentText(context.getString(R.string.notification_generic_message))
                .setContentTitle(context.getString(R.string.notification_generic_title))
                .addAction(R.drawable.ic_launcher, context.getString(R.string.action_purchase), pendingIntent);

        NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID,builder.build());
    }

    public static void requestPurchase(GoogleApiClient client, String path, Bundle extras) {
        if(extras != null) {
            Uri.Builder builder = new Uri.Builder()
                    .scheme("candie")
                    .authority("beacon")
                    .appendQueryParameter("uuid", extras.getString(IntentParameters.UUID))
                    .appendQueryParameter("major", extras.getString(IntentParameters.MAJOR))
                    .appendQueryParameter("minor", extras.getString(IntentParameters.MINOR));

            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
            putDataMapRequest.getDataMap().putLong("DataStamp", System.currentTimeMillis());
            putDataMapRequest.getDataMap().putString("content", builder.toString());

            Wearable.DataApi.putDataItem(client, putDataMapRequest.asPutDataRequest());
        }
    }

    public static boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager)context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
