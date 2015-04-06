package br.com.novatrix.candies.application;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import br.com.novatrix.candies.database.CandieSQLiteDataSource;
import br.com.novatrix.candies.service.BeaconDiscoverService;
import br.com.novatrix.candies.util.OkHttpStack;
import br.com.novatrix.candies.util.SimpleMqttClient;
import br.com.novatrix.candies.util.Util;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.Calendar;
import java.util.Date;

import io.fabric.sdk.android.Fabric;

/**
 * @author Igor Castañeda Ferreira - github.com/igorcferreira - @igorcferreira
 */
public class CandiesApplication extends Application {

    private static final String UNBIND_FLAG = "UNBIND_FLAG";
    private CandieSQLiteDataSource dataSource;
    private static CandiesApplication app;
    private static SimpleMqttClient mqttClient;

    private RequestQueue queue;
    private Beacon beacon;
    private Date lastNotificationDate;
    private static GoogleApiClient mGoogleClient;
    private BackgroundPowerSaver backgroundPowerSaver;


    @Override
    public void onCreate() {

        super.onCreate();

        Fabric.with(this, new Crashlytics());

        app = this;

        dataSource = new CandieSQLiteDataSource(app);

        if(!Util.isServiceRunning(BeaconDiscoverService.class, getApplicationContext())) {
            startService(new Intent(getApplicationContext(), BeaconDiscoverService.class));
        }

        mqttClient = new SimpleMqttClient();
    }


    /**
     * Get MQTT Client for Candies Systems
     * @return MQTT Client connected to Candies topic
     */
    public static SimpleMqttClient getMqttClient()   {
        return mqttClient;
    }

    public Beacon getBeacon() {
        return beacon;
    }

    public void setBeacon(Beacon beacon) {
        this.beacon = beacon;
    }


    private static void setUpGoogleClientIfNeeded() {
        if(mGoogleClient == null) {
            mGoogleClient = new GoogleApiClient.Builder(get().getApplicationContext())
                    .addApi(Wearable.API)
                    .build();

            mGoogleClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {}
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

    public static GoogleApiClient getGoogleClient() {
        setUpGoogleClientIfNeeded();
        return mGoogleClient;
    }

    public static CandiesApplication get() {
        return app;
    }

    public static CandieSQLiteDataSource getDatasource() {
        if(app == null)
            return null;
        else
            return app.dataSource;
    }

    public SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    public void setFromUnbind(boolean fromUnbind)
    {
        if(fromUnbind)
            getSharedPreferences().edit().putBoolean(UNBIND_FLAG, true).apply();
        else
            getSharedPreferences().edit().remove(UNBIND_FLAG).apply();
    }

    public boolean isFromUnbind()
    {
        return getSharedPreferences().getBoolean(UNBIND_FLAG, false);
    }

    /**
     * Get the application {@link com.android.volley.RequestQueue}. It holds all the application
     * internet request
     * @return  The application {@link com.android.volley.RequestQueue}
     */
    public RequestQueue getQueue() {
        if(queue == null) {
            queue = Volley.newRequestQueue(app, new OkHttpStack());
        }
        return queue;
    }

    /**
     * Add a simple Volley {@link com.android.volley.Request} to the application request queue
     * @param request   A valid Volley {@link com.android.volley.Request}
     */
    public void addRequestToQueue(Request<?> request) {
        addRequestToQueue(request, CandiesApplication.class.getSimpleName());
    }

    /**
     * <p>Add a Volley {@link com.android.volley.Request} to the application request queue.</p>
     * <p>But, first, associate a {@link java.lang.String} tag to the request. So, it can be
     * stopped latter</p>
     * @param request   A valid Volley {@link com.android.volley.Request}
     * @param tag       {@link java.lang.String} that will be associated to the specific request
     */
    public void addRequestToQueue(Request<?> request, String tag) {
        request.setTag(tag);
        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        getQueue().add(request);
    }

    public void stopAllRequests() {
        getQueue().cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    /**
     * Stop all the {@link com.android.volley.Request} associated to the specific tag
     * @param tag   {@link java.lang.String} tag of the requests that need to be stopped
     */
    public void stopRequest(String tag) {
        getQueue().cancelAll(tag);
    }

    public Date getLastNotificationDate() {
        return lastNotificationDate;
    }

    public void setLastNotificationDate(Date lastNotificationDate) {
        this.lastNotificationDate = lastNotificationDate;
    }

    public boolean shouldNotificate() {

        if(lastNotificationDate == null) return !CandiesApplication.get().isFromUnbind();

        Calendar calendar = Util.getDefaultIntervalCalendar(lastNotificationDate.getTime());
        return calendar.getTimeInMillis() >= System.currentTimeMillis() && !CandiesApplication.get().isFromUnbind();
    }

    @Override
    public void onTerminate() {
        mqttClient.disconnect();
        super.onTerminate();
    }
}
