package com.pogamadores.candies.ui.fragment;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationManagerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.pogamadores.candies.R;
import com.pogamadores.candies.application.CandiesApplication;
import com.pogamadores.candies.domain.Token;
import com.pogamadores.candies.service.BeaconDiscoverService;
import com.pogamadores.candies.service.PaymentService;
import com.pogamadores.candies.ui.activity.PermissionActivity;
import com.pogamadores.candies.util.IntentParameters;
import com.pogamadores.candies.util.Util;

import org.altbeacon.beacon.Beacon;

/**
 * A simple {@link android.app.Fragment} subclass.
 */
public class MainFragment extends Fragment {

    private TextView mTvInformation;
    private Button mBtPurchase;
    private BeaconDiscoverService beaconService;
    private PaymentService paymentService;
    private GoogleApiClient mGoogleClient;

    public MainFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mTvInformation = ((TextView) rootView.findViewById(R.id.tvInformation));
        mBtPurchase = ((Button) rootView.findViewById(R.id.btPurchase));
        mBtPurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle infoBundle = null;
                if(beaconService != null && beaconService.getBeacon() != null) {
                    Beacon beacon = beaconService.getBeacon();

                    infoBundle = new Bundle();
                    infoBundle.putString(IntentParameters.UUID, beacon.getId1().toString());
                    infoBundle.putString(IntentParameters.MAJOR, beacon.getId2().toString());
                    infoBundle.putString(IntentParameters.MINOR, beacon.getId3().toString());
                }
                if(CandiesApplication.getDatasource().getToken() == null) {
                    Intent permissionIntent = new Intent(getActivity(), PermissionActivity.class);
                    if(infoBundle != null)
                        permissionIntent.putExtras(infoBundle);
                    getActivity().startActivity(permissionIntent);
                    Util.sendMessage(
                            CandiesApplication.getGoogleClient(),
                            "/candies/payment",
                            "token"
                    );
                }else {
                    Intent purchaseIntent = new Intent(getActivity().getApplicationContext(), PaymentService.class);
                    if (infoBundle != null)
                        purchaseIntent.putExtras(infoBundle);
                    getActivity().startService(purchaseIntent);
                    Util.sendMessage(
                            CandiesApplication.getGoogleClient(),
                            "/candies/payment",
                            "start"
                    );
                }

                NotificationManagerCompat.from(getActivity().getApplicationContext()).cancel(Util.NOTIFICATION_ID);
            }
        });
        Button mBtNotificar = (Button)rootView.findViewById(R.id.btNotification);
        mBtNotificar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mGoogleClient != null) {
                    setUpGoogleClientIfNeeded();
                    Util.informNewBeacon(mGoogleClient, "/new/candies/beacon", "1", "1", "1");
                    Util.dispatchNotification(
                            getActivity().getApplicationContext(),
                            "1",
                            "1",
                            "1",
                            R.drawable.ic_launcher
                    );
                }
            }
        });

        mTvInformation.setText(getString(R.string.message_machine_close));
        mBtPurchase.setVisibility(View.VISIBLE);
        mBtNotificar.setVisibility(View.VISIBLE);

        setUpGoogleClientIfNeeded();

        if(!Util.isServiceRunning(BeaconDiscoverService.class, getActivity().getApplicationContext())) {
            Intent service = new Intent(getActivity().getApplicationContext(), BeaconDiscoverService.class);
            if (getActivity().getIntent() != null && getActivity().getIntent().getExtras() != null)
                service.putExtras(getActivity().getIntent().getExtras());
            getActivity().startService(service);
        }

        return rootView;
    }

    private void setUpGoogleClientIfNeeded() {
        if(mGoogleClient == null) {
            mGoogleClient = new GoogleApiClient.Builder(getActivity().getApplicationContext())
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
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().bindService(
                new Intent(getActivity().getApplicationContext(), BeaconDiscoverService.class),
                mBeaconConnection,
                Context.BIND_AUTO_CREATE
        );
        getActivity().bindService(
                new Intent(getActivity().getApplicationContext(), PaymentService.class),
                mPaymentConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    @Override
    public void onStop() {
        CandiesApplication.get().setFromUnbind(true);
        getActivity().unbindService(mBeaconConnection);
        getActivity().unbindService(mPaymentConnection);
        super.onStop();
    }


    private ServiceConnection mPaymentConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PaymentService.LocalBinder binder = (PaymentService.LocalBinder)service;
            paymentService = binder.getService();
            paymentService.setPaymentStepsListener(paymentStepsListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            paymentService = null;
        }
    };

    private ServiceConnection mBeaconConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            BeaconDiscoverService.BeaconDiscoverBinder beaconDiscoverBinder = (BeaconDiscoverService.BeaconDiscoverBinder)binder;
            beaconService = beaconDiscoverBinder.getService();
            beaconService.addDiscoverListener(discoverListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            beaconService = null;
            if(getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtPurchase.setVisibility(View.GONE);
                    }
                });
            }
        }
    };

    private PaymentService.PaymentStepsListener paymentStepsListener = new PaymentService.PaymentStepsListener() {
        @Override
        public void onPaymentStarted(Token token, double value) {
            setUpGoogleClientIfNeeded();
            Util.sendMessage(mGoogleClient, "/candies/payment/started", "New payment");
        }

        @Override
        public void onPaymentFinished(Token token, double value, boolean successful, String message) {
            Util.sendMessage(mGoogleClient, "/candies/payment/finished",(successful?"true":"false"));
        }
    };

    private BeaconDiscoverService.DiscoverListener discoverListener = new BeaconDiscoverService.DiscoverListener() {
        @Override
        public void didDiscoverBeacon(Beacon beacon) {
            if(getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTvInformation.setText(getString(R.string.message_machine_close));
                        mBtPurchase.setVisibility(View.VISIBLE);
                    }
                });
                if(beaconService != null)
                    beaconService.removeDiscoverListener(this);
            }
        }
    };
}
