package com.pogamadores.candies.util;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.pogamadores.candies.R;
import com.pogamadores.candies.broadcast.PaymentOrderReceiver;

import org.altbeacon.beacon.Beacon;

import java.util.Calendar;

public class Util
{
    public static final int NOTIFICATION_ID = 123456;
    public static final double PRODUCT_DEFAULT_VALUE = 10.5f;

    public static void dispatchNotification(Context context, String uuid, String major, String minor, int productImage)
    {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), productImage);
        dispatchNotification(context, uuid, major, minor, bitmap);
        bitmap.recycle();
    }

    public static void dispatchNotification(Context context, String uuid, String major, String minor, Bitmap productImage)
    {
        Bundle infoBundle = new Bundle();
        infoBundle.putString(IntentParameters.UUID,uuid);
        infoBundle.putString(IntentParameters.MAJOR,major);
        infoBundle.putString(IntentParameters.MINOR,minor);

        Intent purchaseIntent = new Intent(context, PaymentOrderReceiver.class);
        purchaseIntent.putExtras(infoBundle);

        PendingIntent purchasePendingIntent = PendingIntent.getBroadcast(
                context,
                RequestCode.PURCHASE,
                purchaseIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLocalOnly(true)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_PROMO)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(context.getString(R.string.notification_generic_title))
                .setContentText(context.getString(R.string.notification_generic_message))
                .addAction(R.drawable.ic_launcher, context.getString(R.string.action_purchase), purchasePendingIntent);

        Notification notification = builder.build();
        notification.defaults |= Notification.DEFAULT_ALL;

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.notify(NOTIFICATION_ID,notification);
    }

    public static boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void sendMessage(GoogleApiClient client, String path, String message) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
        putDataMapRequest.getDataMap().putLong("DataStamp", System.currentTimeMillis());
        putDataMapRequest.getDataMap().putString("content", message);

        Wearable.DataApi.putDataItem(client, putDataMapRequest.asPutDataRequest());
    }

    public static void informNewBeacon(final GoogleApiClient client, final String path, String uuid, String major, String minor) {
        final Uri.Builder builder = new Uri.Builder()
                .scheme("candie")
                .authority("beacon")
                .appendQueryParameter("uuid",uuid)
                .appendQueryParameter("major",major)
                .appendQueryParameter("minor",minor);
        sendMessage(client, path, builder.toString());
    }

    public static void informNewBeacon(GoogleApiClient client, String path, Beacon targetBeacon) {
        informNewBeacon(client, path, targetBeacon.getId1().toString(), targetBeacon.getId2().toString(), targetBeacon.getId3().toString());
    }

    public static void scheduleReceiver(Context context, Class receiver) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 5);

        AlarmManager alarmManager = (AlarmManager)context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        Intent receiverIntent = new Intent(context.getApplicationContext(), receiver);
        PendingIntent receiverPendent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, receiverIntent, 0);
        alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                receiverPendent
        );
    }

    /**
     * Check if the beacon belongs to the list of Beacons that my App recognizes.
     * The list is defined at @see Constants.myBeacons
     * @param beacon
     * @return true if the beacon belongs to my app
     */
    public static boolean isMyBeacon(Beacon beacon)       {
        boolean retorno = false;

        if (beacon!= null)    {

            String beaconId = beacon.getId1() + "-" + beacon.getId2() + "-" + beacon.getId3();

            for (String myBeaconId: Constants.myBeacons)    {
                if (myBeaconId.equals(beaconId))    {
                    retorno = true;
                    break;
                }
            }
        }

        return retorno;
    }
}
