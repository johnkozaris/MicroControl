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

public class MqttClientServiceReceiver extends BroadcastReceiver {

    public interface BroadcastListener {
        void onModuleCreateNew(MicroModule newModule);

        void onModuleOutputData(int moduleId, String data);

        void onModuleDisconected(int moduleId,String Type);

    }

    BroadcastListener mListener;
    Context ActivityCont = null;

    public MqttClientServiceReceiver() {

    }

    public MqttClientServiceReceiver(Context context) {
        new MqttClientServiceReceiver();
        Activity a = null;
        if (context instanceof Activity) {
            a = (Activity) context;

        }
        // Verify that the host activity implements the callback interface
        ActivityCont = a;
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (BroadcastListener) a;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(a.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        @SuppressLint("HardwareIds") String androidID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        String topic = intent.getStringExtra(ConstantStrings.EXTRAS._INCOMING_TOPIC);
        String payload = intent.getStringExtra(ConstantStrings.EXTRAS._INCOMING_PAYLOAD);
        if(!topic.startsWith("/")){return;}
        String TopicLevels[] = topic.split("/");
        if (Arrays.asList(TopicLevels).size()!=4 ){return;}
        String TypeTopicLevel= TopicLevels[1];
        String IdTopicLevel=TopicLevels[2];
        String CommandTopicLevel=TopicLevels[3];
        switch (TypeTopicLevel){

            case "android":
                if (!IdTopicLevel.equals(androidID)){return;}
                if (!payload.contains("&")){return;}
                String PayloadForNewModule[]=payload.split("&");
                if (Arrays.asList(PayloadForNewModule).size()!=4){return;}
                MicroModule newModule= new MicroModule(Integer.parseInt(PayloadForNewModule[0]),PayloadForNewModule[1],PayloadForNewModule[2],PayloadForNewModule[3]);
                mListener.onModuleCreateNew(newModule);
                break;

            case MicroModule.IConstants.ModuleTypes.INPUT:
                if (!CommandTopicLevel.equals(MicroModule.IConstants.Topics._LASTWIL)){return;}
                mListener.onModuleDisconected(Integer.parseInt(IdTopicLevel),TopicLevels[1]);
                break;

            case MicroModule.IConstants.ModuleTypes.OUTPUT:
                switch (CommandTopicLevel){
                    case MicroModule.IConstants.Topics._DATA:
                        mListener.onModuleOutputData(Integer.parseInt(IdTopicLevel),payload);
                        break;
                    case MicroModule.IConstants.Topics._LASTWIL:
                        mListener.onModuleDisconected(Integer.parseInt(IdTopicLevel),TopicLevels[1]);
                        break;
                }
                break;
        }
    }

}
