package com.pogamadores.candies.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.pogamadores.candies.R;
import com.pogamadores.candies.application.CandiesApplication;
import com.pogamadores.candies.util.Util;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The {@link BeaconDiscoverService} will be responsible to look for new beacons and create the
 * notification flow
 */
public class BeaconDiscoverService extends Service implements BeaconConsumer {

    private BeaconDiscoverBinder mBinder = new BeaconDiscoverBinder();

    private Region region;

    private boolean onIteration = false;
    private List<DiscoverListener> listenerList = new ArrayList<>();
    private List<DiscoverListener> listenerToRemove;
    private BeaconManager beaconManager;
    private Beacon beacon = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(region == null) {
            region = new Region("regionid", null, null, null);
            beaconManager = CandiesApplication.get().getBeaconManager();
            beaconManager.getBeaconParsers().add(new BeaconParser().
                    setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
            beaconManager.bind(this);
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void addDiscoverListener(DiscoverListener listener) {
        if(listener != null) {
            if(listenerList == null)
                listenerList = new ArrayList<>();
            if (listenerList.indexOf(listener) < 0)
                listenerList.add(listener);
            if(beacon != null)
                listener.didDiscoverBeacon(beacon);
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
        return beacon;
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                for (Beacon rangedBeacon : beacons) {
                    if(beacon == null) {
                        if(rangedBeacon.getDistance() > 2.f)
                            continue;
                        beacon = rangedBeacon;
                        onIteration = true;
                        if(listenerList != null) {
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
                        if(!CandiesApplication.get().isFromUnbind()) {
                            Util.dispatchNotification(
                                    getApplicationContext(),
                                    rangedBeacon.getId1().toString(),
                                    rangedBeacon.getId2().toString(),
                                    rangedBeacon.getId3().toString(),
                                    R.drawable.ic_launcher
                            );
                        }
                        CandiesApplication.get().setFromUnbind(false);
                        break;
                    } else if(rangedBeacon.getId1().toString().equals(beacon.getId1().toString())) {
                        beacon = rangedBeacon;
                        break;
                    }
                }
            }
        });

        beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {}
            @Override
            public void didExitRegion(Region region) {
                if(region.getId1() != null && region.getId1().toString().equals(beacon.getId1().toString())) {
                    beacon = null;
                }
            }
            @Override
            public void didDetermineStateForRegion(int i, Region region) {}
        });
        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (Exception ignored) {}
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
