package com.autom.kozaris.microcontrol.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.autom.kozaris.microcontrol.ConstantStrings;
import com.autom.kozaris.microcontrol.R;

/**
 * {@link Fragment} ConnectionDetailsFragment
 *
 * Δινει στον χρήστη την δυνατότητα να καταχωρήσει τις παραμέτρους
 * του μεσίτη στον οποίο θα γίνει η σύνδεση
 *
 * Activities που περιέχουν αυτο το Fragment πρέπει να υλοποιούν
 * {@link OnSettingsCompletedListener} interface
 * για να διαχειριζονται τα γεγονότα.
 *@author   Ioannis Kozaris
 */
public class ConnectionDetailsFragment extends DialogFragment {

    private OnSettingsCompletedListener mListener;

    public ConnectionDetailsFragment() {
    }

    public static ConnectionDetailsFragment newInstance() {
        return new ConnectionDetailsFragment();
    }
    //Αρχικοποίηση μεταβλητών για αποφυγή Null Reference Exception
    String defAddress="";
    String defUsername="";
    String defPassword="";
    boolean defCleanSession=true;
    EditText brokerAddress;
    EditText brokerUsername;
    EditText brokerPassword;
    Button buttonOK;
    Button buttonCancel;
    CheckBox checkboxCleanSession;
    CheckBox checkbocRemeber;
    String Address;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =inflater.inflate(R.layout.fragment_connection_details, container, false);
        //Ελεγχος για αποθηκευμένες μεταβλητές απο προηγούμενη καταχώρηση
        CheckPreferencesForData();
        Address=defAddress;
        Address=Address.replace(":1883","");
        brokerAddress =view.findViewById(R.id.editTextBrokerAddr);
        brokerAddress.setText(Address);
        brokerUsername =view.findViewById(R.id.editTextUsername);
        brokerUsername.setText(defUsername);
        brokerPassword =view.findViewById(R.id.editTextPassword);
        brokerPassword.setText(defPassword);
        checkboxCleanSession =view.findViewById(R.id.checkBoxCleanSession);
        checkboxCleanSession.setChecked(defCleanSession);
        checkbocRemeber=view.findViewById(R.id.checkBoxSaveSettings);
        buttonOK= view.findViewById(R.id.buttonConSettignsOK);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PassSettings();
            }
        });
        buttonCancel= view.findViewById(R.id.buttonConSettingsCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        return view;
    }

    /**
     * Ελεγχος Null και κανονικοποίηση παραμέτρων MQTT
     */
    private void PassSettings() {
        if (brokerAddress.getText()==null ||brokerAddress.getText().toString().isEmpty()){
            brokerAddress.setError("Field required");
            return;
        }
        if (brokerAddress.getText().toString().contains("tcp://")){
            Address=brokerAddress.getText().toString()+":1883";
        }else {
            Address="tcp://"+brokerAddress.getText().toString()+":1883";
        }
        //Αν ο χρήστης θέλει το πρόγραμμα να θυμάται τις παραμέτρους,
        //αποθήκευσε τις παραμέτρους στις ρυθμίσεις προγράμματος
        if (checkbocRemeber.isChecked()){
            defAddress=Address;
            defUsername=brokerUsername.getText().toString();
            defPassword=brokerPassword.getText().toString();
            defCleanSession=checkboxCleanSession.isChecked();
            SavePreferences();
        }
        //Ειδοποίησηε τον OnSettingsCompletedListener οτι η καταχώρηση παραμέτρων ολοκληρώθηκε.
        mListener.onSettingsCompleted(Address);
        dismiss();
    }

    /**
     * Φορτώνει τις παραμέτρους απο τις ρυθμίσεις προγράμματος
     * Αν δεν υπαρχουν αποθηκευμένες παράμετροι τότε φορτώνει κενά string
     * για αποφυγή NullReferenceExeption
     */
    void CheckPreferencesForData(){
        SharedPreferences Pref = getActivity().getSharedPreferences(ConstantStrings.STORAGE.STORAGE_KEY_CON_SETTINGS_PREF,Context.MODE_PRIVATE);
        defAddress = Pref.getString(ConstantStrings.STORAGE.PREFERENCE_BROKER_ADDR,"");
        defUsername = Pref.getString(ConstantStrings.STORAGE.PREFERENCE_BROKER_USERNAME,"");
        defPassword = Pref.getString(ConstantStrings.STORAGE.PREFERENCE_BROKER_PASSWORD,"");
        defCleanSession = Pref.getBoolean(ConstantStrings.STORAGE.PREFERENCE_BROKER_CLEANSESSION,true);
    }

    /**
     * Αποθήκευση των παραμέτρων στις ρυθμίσεις προγράμματος
     */
    void SavePreferences(){
        SharedPreferences sharedPref = getActivity().getSharedPreferences(ConstantStrings.STORAGE.STORAGE_KEY_CON_SETTINGS_PREF,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        //Οι κενες παράμετροι σύνδεσης δέν αποθηκευονται
        if (!Address.isEmpty()){ editor.putString(ConstantStrings.STORAGE.PREFERENCE_BROKER_ADDR,defAddress);}
        if (!defUsername.isEmpty()){ editor.putString(ConstantStrings.STORAGE.PREFERENCE_BROKER_USERNAME,defUsername);}
        if (!defPassword.isEmpty()){editor.putString(ConstantStrings.STORAGE.PREFERENCE_BROKER_PASSWORD,defPassword);}
        editor.putBoolean(ConstantStrings.STORAGE.PREFERENCE_BROKER_CLEANSESSION,true);
        editor.apply();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSettingsCompletedListener) {
            mListener = (OnSettingsCompletedListener) context;
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
     * Χρησημοποιείται για αποστολή δεδομένων πίσω στην δραστηριότητα LoginActivity
     * όταν ολοκληρωθεί η καταχώρηση των στοιχείων απο τον χρήστη
     */
    public interface OnSettingsCompletedListener {
        //Η δραστηριότητα LoginActivity κάνει Override την μέθοδο OnSettingsCompleted
        void onSettingsCompleted(String BrokerAddress);
    }
}
