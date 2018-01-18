package com.autom.kozaris.microcontrol.Fragments;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.autom.kozaris.microcontrol.R;

import java.util.ArrayList;
import java.util.List;
/**
 * {@link Fragment} WifiListFragment
 *
 * Διάλογος που προβάλει τα κοντινα δίκτυα Wi-Fi των μικροελεγκτών ESP
 * που είναι διαθέσιμα.
 *
 * Activities που περιέχουν αυτο το Fragment πρέπει να υλοποιούν
 * {@link OnWifiSelectedListener} interface
 * για να διαχειριζονται τα γεγονότα.
 */
public class WifiListFragment extends DialogFragment {

    private OnWifiSelectedListener mListener;
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION=5;
    ListView list;
    WifiManager wifi;
    String wifis[];
    ArrayList<String> arrayList;
    WifiScanReceiver wifiReceiver;
    ArrayAdapter<String> arrayAdapter;
    ProgressBar scanBar;

    public static WifiListFragment newInstance() { return new WifiListFragment();}
    public WifiListFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        arrayList= new ArrayList<>();
        //Αρχικοποίηση της υπηρεσίας WiFi της συσκευής
        wifiReceiver = new WifiScanReceiver();
        wifi = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //Καταχώρηση φίλτρου για να λειφθούν απο τον BroadcastReceiver WifiScanReceiver τα διαθέσιμα δικτυα WiFi
        getActivity().registerReceiver(wifiReceiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

    }

    /**
     * Αποτέλεσμα της αιτηση άδειας απο τον χρήστη για να χρησιμοποιηθεί το WiFi της συσκευής
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //Αν δωθεί άδεια χρήσης του wifi τότε να ξεκίνήσει η αναζήτηση δικτύων
            showProgress(true);
            wifi.startScan();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =inflater.inflate(R.layout.fragment_wifi_list, container, false);
        list =view.findViewById(R.id.listview_esp_wifi);
        scanBar = view.findViewById(R.id.progressBarScanWifi);
        scanBar.setVisibility(View.VISIBLE);
        arrayAdapter = new ArrayAdapter<>(getActivity(),android.R.layout.simple_list_item_1,arrayList);
        list.setAdapter(arrayAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mListener.onWifiSelected(wifis[position]);
                dismiss();
            }
        });
        //Ενεργοποίηση το WiFi αν δεν είναι ενεργό
        if (!wifi.isWifiEnabled()){
            wifi.setWifiEnabled(true);
        }
        //Για καινούργια λογισμικά android κάνε αίτηση μιας άδειας απο τον χρήστη, για την χρήση του wifi
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
        }else{
            //Εναρξη αναζήτησης δικτύων
            showProgress(true);
            wifi.startScan();
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnWifiSelectedListener) {
            mListener = (OnWifiSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Πρεπει να υλοποιείτε απο τις δραστηριότητες που χρησιμοποιουν το WifiListFragment
     * ωστε να λαμβάνουν το επιλεγμένο ssid.
     */
    public interface OnWifiSelectedListener {
        void onWifiSelected(String ssid);
    }

    /**
     * Εφέ αναζήτηση δικτύου
     * @param show εντολη ενεργοποίησης απενεργοποίησης εφε
     */
    private void showProgress(final boolean show) {


                int shortAnimTime = 300;
                list.setVisibility(show ? View.GONE : View.VISIBLE);
                list.animate().setDuration(shortAnimTime).alpha(
                        show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        list.setVisibility(show ? View.GONE : View.VISIBLE);
                    }
                });

                scanBar.setVisibility(show ? View.VISIBLE : View.GONE);
                scanBar.animate().setDuration(shortAnimTime).alpha(
                        show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        scanBar.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });

    }

    /**
     * Λαμβάνει τα δίκτυα   Wifi που βρέθηκαν απο την αναζήτηση
     */
    private class WifiScanReceiver extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {
            List<ScanResult> wifiScanList = wifi.getScanResults();
            wifis = new String[wifiScanList.size()];
            arrayList.clear();
            arrayAdapter.clear();
            //Βάλε στην λίστα μόνο τα δίκτυα Wifi που περιέχουν την λέξη esp
            for (int i = 0; i < wifiScanList.size()-1; i++) {
                wifis[i] = wifiScanList.get(i).SSID;
                if (wifiScanList.get(i).SSID.contains("ESP")||wifiScanList.get(i).SSID.contains("esp")||wifiScanList.get(i).SSID.contains("Esp")) {
                    arrayAdapter.add(wifis[i]);
                }
            }
            showProgress(false);
            arrayAdapter.notifyDataSetChanged();
        }
    }
}
