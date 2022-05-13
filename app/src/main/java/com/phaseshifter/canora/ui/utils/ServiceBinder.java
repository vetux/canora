package com.phaseshifter.canora.ui.utils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;

/**
 * Helper class around service binding. Does not maintain state about the service connection or how the service reference is obtained. This has to be handled by the caller.
 */
public class ServiceBinder {
    private final String LOG_TAG = "ServiceBinder";

    private final Context host;
    private final Class<? extends Service> serviceClass;
    private final ServiceConnection serviceConnection;

    public ServiceBinder(Context host, Class<? extends Service> serviceClass, ServiceConnection connection) {
        this.host = host;
        this.serviceClass = serviceClass;
        this.serviceConnection = connection;
    }

    public void bindService() {
        Log.v(LOG_TAG, "BIND SERVICE ON CONTEXT: " + host.toString());
        Intent intent = new Intent(host, serviceClass);
        host.startService(intent);
        host.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbindService() {
        Log.v(LOG_TAG, "UNBIND SERVICE ON CONTEXT: " + host.toString());
        host.unbindService(serviceConnection);
    }
}