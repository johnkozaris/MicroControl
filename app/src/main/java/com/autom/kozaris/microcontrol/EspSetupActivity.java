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

/**
 * H Δραστηριότητα δείνει στον χρήστη ενα έυκολο περιβάλλον ρύθμισης των συσκευων Esp8266 που βρίσκονται σε κατάσταση Ρύθμισης
 * Αναζητά διαθέσιμους μικροελγκτές Esp8266 εντός εμβέλειας Wifi της συσκευής.
 * Συνδέετε μαζί τους και πλοηγήτε στην κατάλληλη διευθυνση Ip  οπου μπορει να γίνει αλλαγή ρυθμίσεων των συσκευων Esp8266
 */
public class EspSetupActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener,WifiListFragment.OnWifiSelectedListener{

    WebView mWebView;
    //Διευθυνση που προβάλεται απο τους Esp8266 η Ιστοσελίδα με HTML-Javascript
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
        //Αρχικοποίηση ενος Browser που θα πλοηγηθεί στις σελίδες του Esp8266
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
        // Πλήκτρο πίσω πλοήγησης Browser
        ImageButton buttonBack =findViewById(R.id.imageButtonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWebView.canGoBack()){
                    mWebView.goBack();
                }
            }
        });
        //Πλήκτρο εμφάνησης διαθέσιμων μικροελεγκτών Esp8266
        ImageButton buttonNetworkList =findViewById(R.id.imageButtonWifi);
        buttonNetworkList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiManager.disconnect();
                //Εκτέλεσε το παράθυρο  WifiListFragment που θα αναλάβει την αναζήτηση μικροελεγκτών
                DialogFragment wifiListFragment= WifiListFragment.newInstance();
                wifiListFragment.show(getSupportFragmentManager(),"wifiscan");
            }
        });
    }

    /**
     * Κατα την εκτέλεση της δραστηριότητας αν η συσκευή είναι συνδεδεμένη σε ένα δίκτυο
     * που περιέχει στο όνομα του του λέξη ESP τοτε θα προσπαθήσει να  πλοηγηθεί στην
     * διεύθυνση ρυθμίσεων του μικροελεγκτή
     */
    @Override
    protected void onStart() {
        super.onStart();
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setWebViewClient(new WebViewClient());
         cm = (ConnectivityManager)this.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = cm != null ? cm.getActiveNetworkInfo() : null;
        //Αν το WiFi ειναι συνδεδεμένο σε ένα δίκτυο
        if (wifiNetwork != null && wifiNetwork.isConnected()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            //Και αν το δίκτυο περίέχει στο όνομα του την λέξη ESP
            if (ssid.contains("ESP")) {
                //Τοτε θα πλοηγηθεί στης διευθυνση WebServerIPAddress
                mWebView.loadUrl("http://"+WebServerIPAddress);
                spinnerHtml.setSelection(spinAdapter.getPosition("Main Page"));
            } else {
                //Αλλιως βρισκομαστε σε λάθος δίκτυο και θα γίνει αποσύνδεση και θα ξεκινήσει
                //το παράθυρο διαλόγο WifiListManager για να γίνει η ανιχνευση σωστού δικτύου
                Toast.makeText(this,"Choose an ESP network",Toast.LENGTH_LONG).show();
                DialogFragment wifiListFragment= WifiListFragment.newInstance();
                wifiManager.disconnect();
                wifiListFragment.show(getSupportFragmentManager(),"wifiscan");
            }
        }
    }

    /**
     * Μέθοδος Callback που καλέιτε απο την κλάση {@link WifiListFragment} οταν επιλεχθεί ενα
     * δίκτυο WiFi ενος Esp8266.
     * Εδώ γίνεται η σύνδεση της συσκευής στο δίκτυο του Esp8266
     * @param ssid Ονομα SSID του Esp8266 που επιλέχθηκε
     */
    @Override
    public void onWifiSelected(String ssid) {
        //Ελεγχος του ονοματος ssid που επιλέχθηκε
        WifiConfiguration wifiConf = new WifiConfiguration();
        wifiConf.SSID="\"" + ssid + "\"";
        //Δεν υπάρχει κωδικός δικτυου τα δικτυα Esp8266 είναι ανοιχτά
        wifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        wifiManager.addNetwork(wifiConf);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        //Βρες το δίκτυο που επιλέξε ο χρήστης στην λίστα διαθεσιμων δικτύων και συνδεσου σε αυτο μολις βρεθεί
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();
                break;
            }
        }
    }

    /**
     * Σελίδες ρυθμίσεων του Esp8266
     */
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
    public void onNothingSelected(AdapterView<?> parent) {}

    /**
     * Γίνεται ερώτηση στον χρήστη οταν πατήσει το πίσω πλήκτρο, αν θέλει πραγματικα
     * να τερματίσει την δραστηριότητα {@link EspSetupActivity} και να ξαναγυρίσει στην δραστηριότητα
     * {@link LoginActivity}
     */
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(EspSetupActivity.this)
                .setTitle("Exit")
                .setMessage("You will now exit the Esp Setup")
                .setNegativeButton(android.R.string.cancel, null)
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


