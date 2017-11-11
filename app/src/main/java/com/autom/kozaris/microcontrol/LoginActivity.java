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


public class LoginActivity extends AppCompatActivity implements ConnectionStatusReceiver.MqttConnectionListener,ConnectionDetailsFragment.OnSettingsCompletedListener {


    // UI references.
    private ProgressBar mProgressView;
    private View mLoginFormView;
    ConnectionStatusReceiver mConnectionReceiver;

    @Override
    public void onSettingsCompleted(String brokerAddress) {
        if (brokerAddress!=null && !brokerAddress.isEmpty()){
            Toast.makeText(this,"Connecting to: "+brokerAddress,Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this,"Connecting to: Default Address",Toast.LENGTH_SHORT).show();
        }
        new Thread() {
            public void run() {
                startService(new Intent(getApplicationContext(),MicroMqttService.class));
                showProgress(true);
            }
        }.start();
    }

    @Override
    public void onConnectSuccess() {
        Intent mainactIntent = new Intent(this,MainActivity.class);
        startActivity(mainactIntent);
        Toast.makeText(this,"Connected to Broker",Toast.LENGTH_LONG).show();
        showProgress(false);
    }

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        mProgressView=findViewById(R.id.login_progress);
        mLoginFormView=findViewById(R.id.login_form);
        Button ButtonConnect =findViewById(R.id.email_sign_in_button);
        RegisterReceivers();

        Button configureButton =findViewById(R.id.button_Setup_Esp);
        SharedPreferences initialValues = getSharedPreferences(MicroMqttService._APPLICATION_ID, 0);
        SharedPreferences.Editor initEdit = initialValues.edit();
        initEdit.putString("broker","127.0.0.1");
        initEdit.putString("topic","/login");
        SharedPreferences sp = getSharedPreferences(ConstantStrings.SETTINGS._ACTIVITY, MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(ConstantStrings.SETTINGS._QUERY_ACTIVE, false);
        initEdit.apply();
        ed.apply();

        ButtonConnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(getApplicationContext(),MicroMqttService.class));
                DialogFragment settingsFragment= ConnectionDetailsFragment.newInstance();
                settingsFragment.show(getSupportFragmentManager(),"settings");
                }

        });
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
            nm.cancel(2);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
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


