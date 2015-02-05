package com.pogamadores.candies.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.pogamadores.candies.R;
import com.pogamadores.candies.application.CandiesApplication;
import com.pogamadores.candies.service.BeaconDiscoverService;
import com.pogamadores.candies.ui.fragment.MainFragment;
import com.pogamadores.candies.util.Constants;
import com.pogamadores.candies.util.Util;

import org.altbeacon.beacon.Beacon;

public class MainActivity extends ActionBarActivity {

    private int settingsTouch = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container,new MainFragment())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        settingsTouch = 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(!Constants.LIVE_ENVIRONMENT) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            settingsTouch++;
            if (settingsTouch == 3) {
                CandiesApplication.get().setLastNotificationDate(null);
                if(CandiesApplication.get().getBeacon() != null) {
                    Beacon rangedBeacon = CandiesApplication.get().getBeacon();
                    Util.informNewBeacon(CandiesApplication.getGoogleClient(), "/new/candies/beacon", rangedBeacon);
                    Util.dispatchNotification(
                            getApplicationContext(),
                            rangedBeacon.getId1().toString(),
                            rangedBeacon.getId2().toString(),
                            rangedBeacon.getId3().toString(),
                            R.drawable.ic_launcher
                    );
                }
                CandiesApplication.get().setBeacon(null);
                Intent service = new Intent(getApplicationContext(), BeaconDiscoverService.class);
                if (getIntent() != null && getIntent().getExtras() != null)
                    service.putExtras(getIntent().getExtras());
                getApplicationContext().startService(service);
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
