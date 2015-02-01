package com.pogamadores.candies.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.pogamadores.candies.application.CandiesApplication;
import com.pogamadores.candies.domain.NewTransaction;
import com.pogamadores.candies.domain.Token;
import com.pogamadores.candies.util.Util;
import com.pogamadores.candies.util.WebServerHelper;

import java.util.Calendar;


public class PaymentService extends Service {

    private LocalBinder mBinder = new LocalBinder();
    private PaymentStepsListener paymentStepsListener;
    private Token token = null;


    public interface PaymentStepsListener {
        public void onPaymentStarted(Token token, double value);
        public void onPaymentFinished(Token token, double value, boolean successful, String message);
    }


    public PaymentService() {
        token = CandiesApplication.getDatasource().getToken();
    }

    private String getStringToken()    {
        return token.getToken();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final Token token = CandiesApplication.getDatasource().getToken();

        if(token != null) {
            NotificationManagerCompat.from(getApplicationContext()).cancel(Util.NOTIFICATION_ID);
            Util.sendMessage(
                    CandiesApplication.getGoogleClient(),
                    "/candies/payment",
                    "purchasing"
            );
            WebServerHelper.performNewPayment (
                    this.getStringToken(),
                    Util.PRODUCT_DEFAULT_VALUE,
                    this.getResponsePaymentListener(),
                    this.getErrorPaymentListener()
            );

            if(paymentStepsListener != null) {
                paymentStepsListener.onPaymentStarted(token, Util.PRODUCT_DEFAULT_VALUE);
            }

            return START_STICKY;
        } else {
            return START_NOT_STICKY;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setPaymentStepsListener(PaymentStepsListener paymentStepsListener) {
        this.paymentStepsListener = paymentStepsListener;
    }


    public class LocalBinder extends Binder {
        public PaymentService getService() {
            return PaymentService.this;
        }
    }


    /**
     * Return specific Response Payment listener to payment webservice call
     */
    public Response.Listener<NewTransaction> getResponsePaymentListener() {

        return new Response.Listener<NewTransaction>() {

            public void onResponse (NewTransaction response)    {
                if (response.isSuccessfull()) {

                    WebServerHelper.sendMachineOrder(
                            token,
                            new Response.Listener<NewTransaction>() {

                                @Override
                                public void onResponse(NewTransaction response) {
                                    if(response.isSuccessfull()) {
                                        Util.sendMessage(
                                                CandiesApplication.getGoogleClient(),
                                                "/candies/payment",
                                                "success"
                                        );
                                        finishService("Doce sendo liberado...", true);
                                    } else {
                                        finishService("Erro no comando à maquina.", false);
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.e(PaymentService.class.getSimpleName(), "Erro no envio à máquina", error);
                                    finishService(Log.getStackTraceString(error), false);
                                }
                            }
                    );
                } else {
                    finishService(response.getMessage(), false);
                }
            }

        };
    }

    private void finishService(String message, boolean successful) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

        Util.sendMessage(
                CandiesApplication.getGoogleClient(),
                "/candies/payment",
                (successful?"success":"fail")
        );

        if (paymentStepsListener != null)
            paymentStepsListener.onPaymentFinished(token, Util.PRODUCT_DEFAULT_VALUE, successful, message);
        stopSelf();
    }

    /**
     * Specific error listener for payment webservice call
     * @return Error Listener for payment webservice call
     */
    public Response.ErrorListener getErrorPaymentListener() {
        return new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                Log.e(CandiesApplication.class.getSimpleName(), Log.getStackTraceString(error));
                Toast.makeText(getApplicationContext(), "Erro no pagamento: " + error.getLocalizedMessage() + " | Tempo final: " + (Calendar.getInstance().getTimeInMillis()), Toast.LENGTH_SHORT).show();
                if (paymentStepsListener != null)
                    paymentStepsListener.onPaymentFinished(token, Util.PRODUCT_DEFAULT_VALUE, false, error.getMessage());
                stopSelf();
            }
        };
    }
}
