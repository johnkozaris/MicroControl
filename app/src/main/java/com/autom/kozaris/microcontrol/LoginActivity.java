package com.autom.kozaris.microcontrol;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.autom.kozaris.microcontrol.Fragments.ConnectionDetailsFragment;
import com.autom.kozaris.microcontrol.Receivers.ConnectionStatusReceiver;

import static com.autom.kozaris.microcontrol.ConstantStrings.SETTINGS._ACTIVITY;

/**
 * Login Activity
 *
 * Η πρώτη Δραστηριότητα της εφαρμογής.Παρουσιάζει το λογότυπο και δίνει 2 επιλογές στον χρήστη.
 * 1.Συνδεση σε εναν μεσίτη και έναρξη της υπηρεσίας {@link MicroMqttService}
 * 2. Αναζήτηση ενος Esp8266 WiFi Access Point
 */
public class LoginActivity extends AppCompatActivity implements ConnectionStatusReceiver.MqttConnectionListener,ConnectionDetailsFragment.OnSettingsCompletedListener {

    private ProgressBar mProgressView;
    private View mLoginFormView;
    ConnectionStatusReceiver mConnectionReceiver;

    /**
     * H μέθοδος αυτή καλείτε απο το {@link ConnectionDetailsFragment} οταν  συμπληρωθεί επιτυχώς
     * η φόρμα με τα στοιχεία του μεσίτη. Εδω γίνεται έναρξη της υπηρεσιας {@link MicroMqttService}
     * @param brokerAddress Η διευθυνση του μεσίτη στον οποιο θα γίνει σύνεση
     */
    @Override
    public void onSettingsCompleted(String brokerAddress) {
        if (brokerAddress!=null && !brokerAddress.isEmpty()){
            Toast.makeText(this,"Connecting to: "+brokerAddress,Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this,"Connecting to: Default Address",Toast.LENGTH_SHORT).show();
        }
        //Η υπηρεσία ξεκινάει σε ένα καινούργιο thread για να μήν παγώσει το UI και εξαρτάται απο το ApplicationContext
        new Thread() {
            public void run() {
                startService(new Intent(getApplicationContext(),MicroMqttService.class));
                showProgress(true);
            }
        }.start();
    }

    /**
     * H μέθοδος αυτή καλέιτε απο την κλάση {@link ConnectionStatusReceiver} οταν η υπηρεσια
     * MicroMqttService την ειδοποιήσει οτι η σύνδεση στον μεσίτη είναι επιτυχής.
     * Σε αυτήν την μέθοδο γίνεται η έναρξη της κεντρικής δραστηριότητας ελεγχου
     * μικροελεγκτών {@link MainActivity}
     */
    @Override
    public void onConnectSuccess() {
        //Εναρξη δραστηριότητας MainActivity
        startActivity(new Intent(this,MainActivity.class));
        Toast.makeText(this,"Connected to Broker",Toast.LENGTH_LONG).show();
        showProgress(false);
    }
    /**
     * H μέθοδος αυτή καλέιτε απο την κλάση {@link ConnectionStatusReceiver} οταν η υπηρεσια
     * MicroMqttService την ειδοποιήσει οτι η σύνδεση στον μεσίτη απέτυχε.
     */
    @Override
    public void onConnectFail() {
        stopService(new Intent(getApplicationContext(),MicroMqttService.class));
        Toast.makeText(this,"Failed To Connect to Broker",Toast.LENGTH_LONG).show();
        showProgress(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * H πρώτη μέθοδος η οποία θα καλεστεί κατα το άνοιγμα της εφαρμογής
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Η εφαρμογή δουλέυει μονο σε προσανατολισμο πορτρέτου
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        mProgressView=findViewById(R.id.login_progress);
        mLoginFormView=findViewById(R.id.login_form);
        Button ButtonConnect =findViewById(R.id.email_sign_in_button);
        RegisterReceivers();
        Button configureButton =findViewById(R.id.button_Setup_Esp);
        // Αποθηκευται στην μνημη ρυθμισεων η κατάσταση της δραστηριοτητας (Δραστηριοτητα εκτελείται)
        SharedPreferences sp = getSharedPreferences(ConstantStrings.SETTINGS._ACTIVITY, MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(ConstantStrings.SETTINGS._QUERY_ACTIVE, false);
        ed.apply();
        //Οταν πατηθέι το πλήκτρο σύνδεσης τερματίζετε η υπήρεσία MicroMqttService αν τρέχει και στην
        //συνέχει εκτελείτε το παράθυρο διαλόγου settingsFragment για να γίνει καταχώρηση των
        //παραμέτρων σύνδεσης του μεσίτη.
        ButtonConnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(getApplicationContext(),MicroMqttService.class));
                DialogFragment settingsFragment= ConnectionDetailsFragment.newInstance();
                settingsFragment.show(getSupportFragmentManager(),"settings");
                }

        });
        //Οταν πατηθεί το πλήκτρο ρυθμιση συσκευών θα ξεκινήσει η δραστηριότητα EspSetupActivity
        // που σκοπο εχει την ρυθμιση των συσκευων Esp που βρίσκονται εντος εμβέλειας WiFi
        configureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), EspSetupActivity.class));
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
        {
            NotificationManager nm =(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            assert nm != null;
            nm.cancel(2);
        }
    }

    /**
     * Εμφανίζει το εφέ φόρτωσης.
     */
    private void showProgress(final boolean show) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int shortAnimTime = 300;
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                        show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                    }
                });

                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                mProgressView.animate().setDuration(shortAnimTime).alpha(
                        show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mConnectionReceiver);
    }

    /**
     * Εγγραφή στις ενέργειες του Broadcast Receiver {@link ConnectionStatusReceiver}
     * για να λειτουργησει η μέθοδος Callback {nConnectSuccess} και onConnectFail
     */
    private void RegisterReceivers() {
        SharedPreferences sp = getSharedPreferences(_ACTIVITY, MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(ConstantStrings.SETTINGS._QUERY_ACTIVE, true);
        ed.apply();
        mConnectionReceiver = new ConnectionStatusReceiver(this);
        IntentFilter connectionActionFilters = new IntentFilter();
        connectionActionFilters.addAction(ConnectionStatusReceiver.ACTIONS.CONNECTED);
        connectionActionFilters.addAction(ConnectionStatusReceiver.ACTIONS.FAILED_TO_CONNECT);
        registerReceiver(mConnectionReceiver, new IntentFilter(connectionActionFilters));
    }


}


