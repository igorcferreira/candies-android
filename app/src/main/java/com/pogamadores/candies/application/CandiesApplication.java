package com.pogamadores.candies.application;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.pogamadores.candies.database.CandieSQLiteDataSource;
import com.pogamadores.candies.service.BeaconDiscoverService;
import com.pogamadores.candies.util.Util;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

public class CandiesApplication extends Application {

    private static final String UNBIND_FLAG = "UNBIND_FLAG";
    private CandieSQLiteDataSource dataSource;
    private static CandiesApplication app;

    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    private BackgroundPowerSaver backgroundPowerSaver;
    private BeaconManager beaconManager;
    private RequestQueue queue;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        backgroundPowerSaver = new BackgroundPowerSaver(app);
        beaconManager = BeaconManager.getInstanceForApplication(app);
        dataSource = new CandieSQLiteDataSource(app);
        if(!Util.isServiceRunning(BeaconDiscoverService.class, getApplicationContext()))
            startService(new Intent(getApplicationContext(), BeaconDiscoverService.class));
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
            queue = Volley.newRequestQueue(app);
        }
        return queue;
    }

    /**
     * Add a simple Volley {@link com.android.volley.Request} to the application request queue
     * @param request   A valid Volley {@link com.android.volley.Request}
     */
    public void addRequestToQueue(Request<?> request) {
        getQueue().add(request);
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

    /**
     * Stop all the {@link com.android.volley.Request} associated to the specific tag
     * @param tag   {@link java.lang.String} tag of the requests that need to be stopped
     */
    public void stopRequest(String tag) {
        getQueue().cancelAll(tag);
    }
}
