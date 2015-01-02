package com.pogamadores.candies.ui.fragment;


import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.pogamadores.candies.R;
import com.pogamadores.candies.application.CandiesApplication;
import com.pogamadores.candies.domain.Token;
import com.pogamadores.candies.service.BeaconDiscoverService;
import com.pogamadores.candies.service.PaymentService;
import com.pogamadores.candies.ui.activity.PermissionActivity;
import com.pogamadores.candies.util.IntentParameters;

import org.altbeacon.beacon.Beacon;

/**
 * A simple {@link android.app.Fragment} subclass.
 */
public class MainFragment extends Fragment {

    private TextView mTvInformation;
    private Button mBtPurchase;
    private BeaconDiscoverService beaconService;
    private PaymentService paymentService;

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
                }else {
                    Intent purchaseIntent = new Intent(getActivity().getApplicationContext(), PaymentService.class);
                    if (infoBundle != null)
                        purchaseIntent.putExtras(infoBundle);
                    getActivity().startService(purchaseIntent);
                }
            }
        });

        mTvInformation.setText(getString(R.string.message_machine_close));
        mBtPurchase.setVisibility(View.VISIBLE);

        return rootView;
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

        }

        @Override
        public void onPaymentFinished(Token token, double value, boolean successful, String message) {

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
