package br.com.novatrix.candies.ui.fragment;


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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import br.com.novatrix.candies.R;
import br.com.novatrix.candies.application.CandiesApplication;
import br.com.novatrix.candies.domain.Token;
import br.com.novatrix.candies.service.BeaconDiscoverService;
import br.com.novatrix.candies.service.PaymentService;
import br.com.novatrix.candies.ui.activity.PermissionActivity;
import br.com.novatrix.candies.util.IntentParameters;
import br.com.novatrix.candies.util.Util;

import org.altbeacon.beacon.Beacon;

/**
 * A simple {@link android.app.Fragment} subclass.
 */
public class MainFragment extends Fragment {

    private static final String IS_PURCHASING = "IS_PURCHASING";
    private TextView mTvInformation;
    private Button mBtPurchase;
    private BeaconDiscoverService beaconService;
    private PaymentService paymentService;
    private GoogleApiClient mGoogleClient;
    private View mSearchingContainer;
    private ProgressBar mProgress;
    private View mFinalMessage;
    private boolean purchasing = false;

    public MainFragment() {}

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_PURCHASING, purchasing);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        purchasing = false;
        if(savedInstanceState != null)
            purchasing = savedInstanceState.getBoolean(IS_PURCHASING, false);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mSearchingContainer = rootView.findViewById(R.id.searchContainer);
        mTvInformation = ((TextView) rootView.findViewById(R.id.tvInformation));
        mBtPurchase = ((Button) rootView.findViewById(R.id.btPurchase));
        mProgress = ((ProgressBar) rootView.findViewById(R.id.progress));
        mFinalMessage = rootView.findViewById(R.id.finalMessage);
        mFinalMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        mFinalMessage.bringToFront();
        mFinalMessage.setVisibility(View.GONE);
        mBtPurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle infoBundle = null;
                purchasing = true;

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

        setUpGoogleClientIfNeeded();
        setBeaconNotFoundScreen();

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

    @Override
    public void onResume() {
        super.onResume();
        CandiesApplication.get().setFromUnbind(false);
        if(CandiesApplication.get().getBeacon() == null) {
            searchBeacon();
        } else {
            setBeaconFoundScreen();
        }
    }

    private void setBeaconNotFoundScreen() {
        mTvInformation.setText(getString(R.string.message_not_close_machine));
        mBtPurchase.setVisibility(View.GONE);
        mSearchingContainer.setVisibility(View.VISIBLE);
        mProgress.setVisibility(View.GONE);

        CandiesApplication.get().setLastNotificationDate(null);
        CandiesApplication.get().setBeacon(null);
        Intent service = new Intent(getActivity().getApplicationContext(), BeaconDiscoverService.class);
        if (getActivity().getIntent() != null && getActivity().getIntent().getExtras() != null)
            service.putExtras(getActivity().getIntent().getExtras());
        getActivity().getApplicationContext().startService(service);
    }

    private void setBeaconFoundScreen() {
        mTvInformation.setText(getString(R.string.message_machine_close));
        mSearchingContainer.setVisibility(View.GONE);
        configView();
    }

    private void searchBeacon() {
        if(!Util.isServiceRunning(BeaconDiscoverService.class, getActivity().getApplicationContext())) {
            Intent service = new Intent(getActivity().getApplicationContext(), BeaconDiscoverService.class);
            if (getActivity().getIntent() != null && getActivity().getIntent().getExtras() != null)
                service.putExtras(getActivity().getIntent().getExtras());
            getActivity().getApplicationContext().startService(service);
        }
    }

    private void configView() {
        if(purchasing) {
            mProgress.setVisibility(View.VISIBLE);
            mTvInformation.setVisibility(View.GONE);
            mBtPurchase.setVisibility(View.GONE);
        } else {
            mProgress.setVisibility(View.GONE);
            mTvInformation.setVisibility(View.VISIBLE);
            mBtPurchase.setVisibility(View.VISIBLE);
        }
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
            purchasing = true;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    configView();
                }
            });
            setUpGoogleClientIfNeeded();
            Util.sendMessage(mGoogleClient, "/candies/payment/started", "New payment");
        }

        @Override
        public void onPaymentFinished(Token token, double value, boolean successful, String message) {
            purchasing = false;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgress.setVisibility(View.GONE);
                    mBtPurchase.setVisibility(View.GONE);
                    mFinalMessage.setVisibility(View.VISIBLE);
                }
            });

            setUpGoogleClientIfNeeded();
            Util.sendMessage(mGoogleClient, "/candies/payment/finished",(successful?"true":"false"));
        }
    };

    private BeaconDiscoverService.DiscoverListener discoverListener = new BeaconDiscoverService.DiscoverListener() {
        @Override
        public void didDiscoverBeacon(Beacon beacon) {
            if(getActivity() != null) {
                Util.cancelNotification(getActivity().getApplicationContext());
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setBeaconFoundScreen();
                    }
                });
                if(beaconService != null)
                    beaconService.removeDiscoverListener(this);
            }
        }
    };
}
