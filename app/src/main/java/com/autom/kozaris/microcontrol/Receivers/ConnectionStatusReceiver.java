package com.autom.kozaris.microcontrol.Receivers;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * MqttClientServiceReceiver
 *
 * Διαμεσολαβήτης ανάμεσα στην υπηρεσία MicroMqttService και τις δραστηριότητες
 * του προγράμματος. Λαμβάνει την κατάσταση σύνδεσης της υπηρεσίας και την δρομολογεί
 * κατάλληλα στις δραστηριότητες που είναι εγγεγραμένες στον ConnectionStatusReceiver
 *
 * Οι Δραστηριότητες πρέπει να υλοποιούν τις μεθόδους:
 * onConnectSuccess: Καλείται όταν η σύνδεση με τον μεσίτη είναι επιτυχής.
 * onConnectFail:Καλείται όταν η σύνδεση με τον μεσίτη αποτύχει.
 *
 * @author Ioannis Kozaris
 */
public class ConnectionStatusReceiver extends BroadcastReceiver {
    /** Οι σταθερες του interface ACTIONS  χρησιμοποιούντε
     * απο την υπηρεσία MicroMqttService για να ενημερώσουν τον BroadcastReceiver
     * για το αποτέλεσμα της προσπάθειας σύνδεσης.
     */
    public interface ACTIONS{
        String CONNECTED="mqttconnected";
        String FAILED_TO_CONNECT="mqttfailedto";
    }
    public interface MqttConnectionListener{
        void onConnectSuccess();
        void onConnectFail();
    }

    //Δεν πρέπει να χρησιμοποιείται η κενή αρχικοποίηση
    public ConnectionStatusReceiver() {
    }

    /**
     * Αρχικοποίηση του BroadcastReceiver.
     * Η αρχικοποίηση αυτή αναγκάζει την κλάση να χρησιμοποιήσει τις μεθόδους
     * onConnectSuccess και onConnectFail.
     * @param context Το Context της δραστηριότητας που καλεί τον BroadcastReceiver
     */
    public ConnectionStatusReceiver(Context context) {
        new ConnectionStatusReceiver();
        Activity a = null;
        if (context instanceof Activity) {
            a = (Activity) context;

        }
        // Σιγουρέψου οτι η δραστηριότητα χρησιμοποιει το callback interface
        ActivityCont = a;
        try {
            mListener = (MqttConnectionListener) a;
        } catch (ClassCastException e) {
            // Αν δεν χρησιμοποιεί το interface, τότε παρήγαγε ένα exception
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
