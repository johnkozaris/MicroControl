package com.autom.kozaris.microcontrol.Receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ConnectionStatusReceiver extends BroadcastReceiver {
    public interface ACTIONS{
        String CONNECTED="mqttconnected";
        String FAILED_TO_CONNECT="mqttfailedto";
    }
    public interface MqttConnectionListener{
        void onConnectSuccess();
        void onConnectFail();
    }
    public ConnectionStatusReceiver() {
    }

    public ConnectionStatusReceiver(Context context) {
        new ConnectionStatusReceiver();
        Activity a = null;
        if (context instanceof Activity) {
            a = (Activity) context;

        }
        // Verify that the host activity implements the callback interface
        ActivityCont = a;
        try {
            // Instantiate the MqttConnectionListener so we can send events to the host
            mListener = (MqttConnectionListener) a;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(a.toString()
                    + " must implement MqttConnectionListener");
        }
    }
    MqttConnectionListener mListener;
    Context ActivityCont = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()){
            case ACTIONS.CONNECTED:
                mListener.onConnectSuccess();
                break;
            case ACTIONS.FAILED_TO_CONNECT:
                mListener.onConnectFail();
                break;
        }

    }
}
