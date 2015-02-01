package com.pogamadores.candies.util;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.pogamadores.candies.R;
import com.pogamadores.candies.broadcast.CancelNotificationReceiver;
import com.pogamadores.candies.ui.activity.MainActivity;

import java.util.List;

public class Util {
    public static final int NOTIFICATION_ID = 23156;

    public static void cancelNotification(Context context)
    {
        NotificationManagerCompat
                .from(context.getApplicationContext())
                .cancel(NOTIFICATION_ID);
    }

    public static void dispatchNotification(Context context, Uri uri) {

        Bundle infoBundle = new Bundle();
        infoBundle.putString(IntentParameters.UUID, uri.getQueryParameter("uuid"));
        infoBundle.putString(IntentParameters.MAJOR, uri.getQueryParameter("major"));
        infoBundle.putString(IntentParameters.MINOR, uri.getQueryParameter("minor"));
        infoBundle.putInt(IntentParameters.REQUEST_CODE, RequestCode.PURCHASE);

        Intent cancelIntent = new Intent(context.getApplicationContext(), CancelNotificationReceiver.class);
        cancelIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        cancelIntent.putExtras(infoBundle);

        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(
                context.getApplicationContext(),
                0,
                cancelIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtras(infoBundle);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context.getApplicationContext(),
                RequestCode.PURCHASE,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context.getApplicationContext())
                .setSmallIcon(R.drawable.ic_launcher)
                .setLocalOnly(true)
                .setAutoCancel(true)
                .setDeleteIntent(cancelPendingIntent)
                .setCategory(NotificationCompat.CATEGORY_PROMO)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentText(context.getString(R.string.notification_generic_message))
                .setContentTitle(context.getString(R.string.notification_generic_title))
                .addAction(R.drawable.ic_logo_paypal, context.getString(R.string.action_purchase), pendingIntent);

        Notification notification = new NotificationCompat.WearableExtender()
                .setBackground(BitmapFactory.decodeResource(context.getApplicationContext().getResources(), R.drawable.ic_launcher))
                .extend(builder)
                .build();

         NotificationManagerCompat.from(context.getApplicationContext())
                .notify(NOTIFICATION_ID, notification);
    }

    public static void sendMessage(GoogleApiClient client, String path, String message) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
        putDataMapRequest.getDataMap().putLong("DataStamp", System.currentTimeMillis());
        putDataMapRequest.getDataMap().putString("content", message);

        Wearable.DataApi.putDataItem(client, putDataMapRequest.asPutDataRequest());
    }

    public static void requestPurchase(GoogleApiClient client, String path, Bundle extras) {
        if(extras != null) {
            Uri.Builder builder = new Uri.Builder()
                    .scheme("candie")
                    .authority("beacon")
                    .appendQueryParameter("uuid", extras.getString(IntentParameters.UUID))
                    .appendQueryParameter("major", extras.getString(IntentParameters.MAJOR))
                    .appendQueryParameter("minor", extras.getString(IntentParameters.MINOR));
            sendMessage(client, path, builder.toString());
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

    public static boolean isForeground(Context context, String myPackage){
        ActivityManager manager = (ActivityManager) context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List< ActivityManager.RunningTaskInfo > runningTaskInfo = manager.getRunningTasks(Integer.MAX_VALUE);
        ComponentName componentInfo = runningTaskInfo.get(0).topActivity;
        return componentInfo.getPackageName().equals(myPackage);
    }

    public static String extractMessage(DataEvent event, String path) {
        String message = null;
        DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
        if (event.getDataItem().getUri().getPath().equalsIgnoreCase(path)) {
            message = dataMapItem.getDataMap().getString("content");
        }
        return message;
    }
}
