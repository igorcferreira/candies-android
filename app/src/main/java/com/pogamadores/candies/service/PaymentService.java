package com.pogamadores.candies.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.pogamadores.candies.application.CandiesApplication;
import com.pogamadores.candies.domain.NewTransaction;
import com.pogamadores.candies.domain.Token;
import com.pogamadores.candies.util.Util;
import com.pogamadores.candies.util.WebServerHelper;

public class PaymentService extends Service {

    private LocalBinder mBinder = new LocalBinder();

    public PaymentService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getApplicationContext(), "Pagamento", Toast.LENGTH_SHORT).show();
        Token token = CandiesApplication.getDatasource().getToken();
        if(token != null) {
            WebServerHelper.performNewPayment(
                    token.getToken(),
                    Util.PRODUCT_DEFAULT_VALUE,
                    new Response.Listener<NewTransaction>() {
                        @Override
                        public void onResponse(NewTransaction response) {
                            if (response.isSuccessfull()) {
                                Toast.makeText(getApplicationContext(), "Sucesso", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), response.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(CandiesApplication.class.getSimpleName(), Log.getStackTraceString(error));
                            Toast.makeText(getApplicationContext(), "Error on payment", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
            return START_STICKY;
        } else
            return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public PaymentService getService() {
            return PaymentService.this;
        }
    }
}
