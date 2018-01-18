package com.autom.kozaris.microcontrol.Receivers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings.Secure;


import com.autom.kozaris.microcontrol.ConstantStrings;
import com.autom.kozaris.microcontrol.MicroModule;

import java.util.Arrays;

/**
 * ConnectionStatusReceiver
 *
 * Διαμεσολαβήτης ανάμεσα στην υπηρεσία MicroMqttService και τις δραστηριότητες
 * του προγράμματος. Λαμβάνει μυνήματα μέσω της υπηρεσίας και τα δρομολογεί κατάλληλα
 * στις δραστηριότητες που είναι εγγεγραμένες στον ConnectionStatusReceiver
 *
 * Οι Δραστηριότητες πρέπει να υλοποιούν τις μεθόδους:
 * onModuleCreateNew:Καλείται όταν το μύνημα αναγνωριστεί ως καινούργια συσκευή
 * onModuleOutputData:Καλείται όταν το μύνημα αναγνωριστεί ως δεδομένα αισθητήρα
 * onModuleDisconected:Καλείται όταν το μύνημα αναγνωριστεί ως αποσύνδεση μιας συσκευής
 * @author Ioannis Kozaris
 */
public class MqttClientServiceReceiver extends BroadcastReceiver {

    public interface BroadcastListener {
        void onModuleCreateNew(MicroModule newModule);
        void onModuleOutputData(int moduleId, String data);
        void onModuleDisconected(int moduleId,String Type);
    }

    BroadcastListener mListener;
    Context ActivityCont = null;

    //Δεν πρέπει να χρησιμοποιείται η κενή αρχικοποίηση
    public MqttClientServiceReceiver() {}

    /**
     * Αρχικοποίηση του BroadcastReceiver.
     * Η αρχικοποίηση αυτή αναγκάζει την κλάση να χρησιμοποιήσει τις μεθόδους
     * onModuleOutputData και onModuleCreateNew και onModuleDisconected.
     * @param context Το Context της δραστηριότητας που καλεί τον BroadcastReceiver
     */
    public MqttClientServiceReceiver(Context context) {
        new MqttClientServiceReceiver();
        Activity a = null;
        if (context instanceof Activity) {
            a = (Activity) context;
        }
        // Σιγουρέψου οτι η δραστηριότητα χρησιμοποιει το callback interface
        ActivityCont = a;
        try {
            mListener = (BroadcastListener) a;
        } catch (ClassCastException e) {
            // Αν δεν χρησιμοποιεί το interface, τότε παρήγαγε ένα exception
            throw new ClassCastException(a.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    /**
     * Καλέιτε οταν λυφθέι ένα μήνυμα απο την υπηρεσία MicroMqttService
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        @SuppressLint("HardwareIds") String androidID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        //Λήψη και κανονικοποίηση δεδομένων που στάλθηκαν απο την υπηρεσία.
        String topic = intent.getStringExtra(ConstantStrings.EXTRAS._INCOMING_TOPIC);
        String payload = intent.getStringExtra(ConstantStrings.EXTRAS._INCOMING_PAYLOAD);

        //Ελεγχος σωστής μορφοποίησης του θέματος.
        if(!topic.startsWith("/")){return;}
        //Διαχωρισμός των επιπέδων του θέματος.
        String TopicLevels[] = topic.split("/");
        if (Arrays.asList(TopicLevels).size()!=4 ){return;}
        String TypeTopicLevel= TopicLevels[1];
        String IdTopicLevel=TopicLevels[2];
        String CommandTopicLevel=TopicLevels[3];

        //Ελεγχος 1ου επιπέδου θέματος.
        switch (TypeTopicLevel){

            //Αν το πρώτο επίπεδο είναι /android.Τότε έχουμε ενα μήνυμα δημιουργίας καινούργιας συσκευής.
            case "android":
                //Ελεγχος οτι το μήνυμα αναφέρεται στο androidID αυτης της συσκευής Andoroid.
                if (!IdTopicLevel.equals(androidID)){return;}
                //Ελεγχος σωστής μορφοποίησης του μηνύματος.
                if (!payload.contains("&")){return;}
                String PayloadForNewModule[]=payload.split("&");
                if (Arrays.asList(PayloadForNewModule).size()!=4){return;}
                //Δημιουργίας  μια οντότητας MicroModule που αναπαριστά την καινουργια συσκευή .
                MicroModule newModule= new MicroModule(Integer.parseInt(PayloadForNewModule[0]),PayloadForNewModule[1],PayloadForNewModule[2],PayloadForNewModule[3]);
                //Ειδοποίηση της δραστηριότητα MainActivity  και αποστολή του MicroModule.
                mListener.onModuleCreateNew(newModule);
                break;

            //Αν το πρώτο επίπεδο είναι /input. Τοτε μπορεί να έχουμε μονο ένα μήνυμα αποσύνδεσης.
            case MicroModule.IConstants.ModuleTypes.INPUT:
                if (!CommandTopicLevel.equals(MicroModule.IConstants.Topics._LASTWIL)){return;}
                //Ειδοποίηση της δραστηριότητα MainActivity  και αποστολή του  αποσυνδεδεμένου Id.
                mListener.onModuleDisconected(Integer.parseInt(IdTopicLevel),TopicLevels[1]);
                break;

            //Αν το πρώτο επίπεδο είναι /output.Τοτε έχουμε είτε μήνυμα αποσύνδεσης είτε μήνυμα δεδομένων.
            case MicroModule.IConstants.ModuleTypes.OUTPUT:
                switch (CommandTopicLevel){
                    case MicroModule.IConstants.Topics._DATA:
                        //Ειδοποίηση της δραστηριότητα MainActivity  και αποστολή δεδομένων και id της συσκευής.
                        mListener.onModuleOutputData(Integer.parseInt(IdTopicLevel),payload);
                        break;
                    case MicroModule.IConstants.Topics._LASTWIL:
                        //Ειδοποίηση της δραστηριότητα MainActivity  και αποστολή του  αποσυνδεδεμένου Id.
                        mListener.onModuleDisconected(Integer.parseInt(IdTopicLevel),TopicLevels[1]);
                        break;
                }
                break;
        }
    }
}
