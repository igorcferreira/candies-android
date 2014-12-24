package com.pogamadores.candies.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class PaymentService extends Service {

    private LocalBinder mBinder = new LocalBinder();

    public PaymentService() {}

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
