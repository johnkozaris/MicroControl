package com.autom.kozaris.microcontrol;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import com.autom.kozaris.microcontrol.Fragments.WifiListFragment;

import java.util.List;

public class EspSetupActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener,WifiListFragment.OnWifiSelectedListener{

    WebView mWebView;
    String WebServerIPAddress= "192.168.4.22";
    Spinner spinnerHtml;
    ArrayAdapter<CharSequence> spinAdapter;
    ConnectivityManager cm;
    WifiManager wifiManager;
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.browser_view);
        mWebView=findViewById(R.id.EspBrowser);
        spinnerHtml=findViewById(R.id.spinnerEspHTMLS);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
        spinAdapter = ArrayAdapter.createFromResource(this,
                R.array.string_array_esp_html, android.R.layout.simple_spinner_item);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHtml.setAdapter(spinAdapter);
        spinnerHtml.setOnItemSelectedListener(this);
        ImageButton buttonRefresh =findViewById(R.id.imageButton_manual_html_values);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.reload();
            }
        });

        ImageButton buttonBack =findViewById(R.id.imageButtonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWebView.canGoBack()){
                    mWebView.goBack();
                }
            }
        });
        ImageButton buttonNetworkList =findViewById(R.id.imageButtonWifi);
        buttonNetworkList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiManager.disconnect();
                DialogFragment wifiListFragment= WifiListFragment.newInstance();
                wifiListFragment.show(getSupportFragmentManager(),"wifiscan");
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setWebViewClient(new WebViewClient());
         cm = (ConnectivityManager)this.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = cm.getActiveNetworkInfo();
        if (wifiNetwork != null && wifiNetwork.isConnected()) {

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            if (ssid.contains("ESP")) {
                mWebView.loadUrl("http://"+WebServerIPAddress);
                spinnerHtml.setSelection(spinAdapter.getPosition("Main Page"));
            } else {
                Toast.makeText(this,"Choose an ESP network",Toast.LENGTH_LONG).show();
                DialogFragment wifiListFragment= WifiListFragment.newInstance();
                wifiManager.disconnect();
                wifiListFragment.show(getSupportFragmentManager(),"wifiscan");
            }
        }
    }

    @Override
    public void onWifiSelected(String ssid) {
        WifiConfiguration wifiConf = new WifiConfiguration();
        wifiConf.SSID="\"" + ssid + "\"";
        wifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiManager.addNetwork(wifiConf);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();
                break;
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch (parent.getItemAtPosition(position).toString())
            {
                case "Main Page":
                    mWebView.loadUrl("http://"+WebServerIPAddress);
                    break;
                case "Options":
                    mWebView.loadUrl("http://"+WebServerIPAddress+"/options");
                    break;
                case "WiFi Chooser":
                    mWebView.loadUrl("http://"+WebServerIPAddress+"/wifi");
                    break;
            }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(EspSetupActivity.this)
                .setTitle("Exit")
                .setMessage("You will now exit the Esp Setup")
                .setNegativeButton(android.R.string.cancel, null) // dismisses by default
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create()
                .show();
    }


}


