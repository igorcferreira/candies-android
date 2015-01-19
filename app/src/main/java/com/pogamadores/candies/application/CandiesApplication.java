package com.pogamadores.candies.application;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.pogamadores.candies.database.CandieSQLiteDataSource;
import com.pogamadores.candies.service.BeaconDiscoverService;
import com.pogamadores.candies.util.OkHttpStack;
import com.pogamadores.candies.util.Util;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


public class CandiesApplication extends Application {

    private static final String UNBIND_FLAG = "UNBIND_FLAG";
    private CandieSQLiteDataSource dataSource;
    private static CandiesApplication app;

    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private BackgroundPowerSaver backgroundPowerSaver;
    private BeaconManager beaconManager;
    private RequestQueue queue;
    private Beacon beacon;
    private MqttClient mqttClient;


    public Beacon getBeacon() {
        return beacon;
    }


    public void setBeacon(Beacon beacon) {
        this.beacon = beacon;
    }

    @Override
    public void onCreate() {

        super.onCreate();
        app = this;
        backgroundPowerSaver = new BackgroundPowerSaver(app);
        beaconManager = BeaconManager.getInstanceForApplication(app);
        dataSource = new CandieSQLiteDataSource(app);

        if (!Util.isServiceRunning(BeaconDiscoverService.class, getApplicationContext())) {
            startService(new Intent(getApplicationContext(), BeaconDiscoverService.class));
        }

        try {
            mqttClient = new MqttClient("tcp://iot.eclipse.org:1883", Util.getPhoneIMEI(app.getApplicationContext()), new MemoryPersistence());
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e("MQTT", e.getMessage(), e);
        }
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

    public BeaconManager getBeaconManager() {
        return beaconManager;
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


    public MqttClient getMqttClient() {
        if (mqttClient == null)     {
            try {
                mqttClient = new MqttClient("tcp://iot.eclipse.org:1883", Util.getPhoneIMEI(app.getApplicationContext()), new MemoryPersistence());
            } catch (MqttException e) {
                e.printStackTrace();
                Log.e("MQTT", e.getMessage(), e);
            }
        }
        return mqttClient;
    }

    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }
}
