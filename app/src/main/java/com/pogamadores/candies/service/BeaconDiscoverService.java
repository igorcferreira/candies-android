package com.pogamadores.candies.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.pogamadores.candies.R;
import com.pogamadores.candies.application.CandiesApplication;
import com.pogamadores.candies.broadcast.StartServiceReceiver;
import com.pogamadores.candies.util.Util;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The {@link BeaconDiscoverService} will be responsible to look for new beacons and create the
 * notification flow
 */
public class BeaconDiscoverService extends Service implements BeaconConsumer {

    private static final String TAG = BeaconDiscoverService.class.getSimpleName();
    private GoogleApiClient mGoogleClient;

    private BeaconDiscoverBinder mBinder = new BeaconDiscoverBinder();

    private Region region;

    private boolean onIteration = false;
    private List<DiscoverListener> listenerList = new ArrayList<>();
    private List<DiscoverListener> listenerToRemove;
    private BeaconManager beaconManager;
    private CandiesApplication application;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(region == null) {
            region = new Region("regionid", null, null, null);
            beaconManager = CandiesApplication.get().getBeaconManager();
            beaconManager.getBeaconParsers().add(new BeaconParser().
                    setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
            try {
                beaconManager.bind(this);
            } catch (Exception error) {
                Log.e(TAG, "Beacon manager bind error", error);
            }
            if(application == null)
                application = CandiesApplication.get();
            application.setBeacon(null);
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void setUpGoogleClientIfNeeded() {
        if(mGoogleClient == null) {
            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();

            ConnectionResult connectionResult =
                    googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                Log.e(TAG, "Failed to connect to GoogleApiClient.");
                return;
            }
            mGoogleClient = googleApiClient;
        }
    }

    public void addDiscoverListener(DiscoverListener listener) {
        if(listener != null) {
            if(listenerList == null)
                listenerList = new ArrayList<>();
            if (listenerList.indexOf(listener) < 0)
                listenerList.add(listener);
            if(CandiesApplication.get().getBeacon() != null)
                listener.didDiscoverBeacon(CandiesApplication.get().getBeacon());
        }
    }

    public void removeDiscoverListener(DiscoverListener listener) {
        if(listener != null) {
            if (onIteration) {
                if (listenerToRemove == null)
                    listenerToRemove = new ArrayList<>();
                if (listenerToRemove.indexOf(listener) < 0)
                    listenerToRemove.add(listener);
            } else if (listenerList != null)
                listenerList.remove(listener);
        }
    }

    public Beacon getBeacon()
    {
        return CandiesApplication.get().getBeacon();
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                for (Beacon rangedBeacon : beacons) {
                    if (application.getBeacon() == null) {
                        if (rangedBeacon.getDistance() > 2.f)
                            continue;
                        if(!Util.isMyBeacon(rangedBeacon))
                            continue;
                        application.setBeacon(rangedBeacon);
                        onIteration = true;
                        if (listenerList != null) {
                            for (DiscoverListener listener : listenerList)
                                listener.didDiscoverBeacon(rangedBeacon);
                            onIteration = false;
                            if (listenerToRemove != null) {
                                for (DiscoverListener listener : listenerToRemove) {
                                    listenerList.remove(listener);
                                }
                                listenerList.clear();
                                listenerList = null;
                            }
                        } else
                            onIteration = false;
                        if (!CandiesApplication.get().isFromUnbind()) {
                            setUpGoogleClientIfNeeded();
                            Util.informNewBeacon(mGoogleClient, "/new/candies/beacon", rangedBeacon);
                            Util.dispatchNotification(
                                    getApplicationContext(),
                                    rangedBeacon.getId1().toString(),
                                    rangedBeacon.getId2().toString(),
                                    rangedBeacon.getId3().toString(),
                                    R.drawable.ic_launcher
                            );
                        }
                        CandiesApplication.get().setFromUnbind(false);
                        finishService();
                        return;
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (Exception ignored) {}
    }

    private void finishService() {
        try {
            beaconManager.stopMonitoringBeaconsInRegion(region);
            beaconManager.unbind(this);
        } catch (Exception ignored) {}
        region = null;
        Util.scheduleReceiver(getApplicationContext(), StartServiceReceiver.class);
        stopSelf();
    }
    //endregion

    //region Helper classes
    public class BeaconDiscoverBinder extends Binder {
        public BeaconDiscoverService getService() {
            return BeaconDiscoverService.this;
        }
    }

    public interface DiscoverListener
    {
        public void didDiscoverBeacon(Beacon beacon);
    }
    //endregion

}
