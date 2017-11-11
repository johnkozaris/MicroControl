package com.autom.kozaris.microcontrol;

public interface ConstantStrings {
    interface STORAGE{
        String STORAGE_KEY_CON_SETTINGS_PREF="my_conn_setting_mqtt_br";
        String PREFERENCE_BROKER_ADDR="pref_br_addr";
        String PREFERENCE_BROKER_USERNAME="pref_br_username";
        String PREFERENCE_BROKER_PASSWORD="pref_br_pass";
        String PREFERENCE_BROKER_CLEANSESSION="pref_br_cleanses";
    }
    interface SETTINGS{

        //SHARED PREFERENCES and SETTINGS
        String _ACTIVITY ="com.autom.kozaris.microcontrol.MICROCONTROLACTINFO";
        String _QUERY_ACTIVE ="com.autom.kozaris.microcontrol.ACTACTIVE";
        String _BOOL_SERVICE_RUNNING ="com.autom.kozaris.microcontrol.SRVRNBL";
        String _BOOL_SERVICE_CONNECTED="com.autom.kozaris.microcontrol.SRVCNCTBL";
    }
    interface EXTRAS{

        String _SUB_TOPIC ="com.autom.kozaris.microcontrol.SUB_EXT_TOPIC";
        String _PUB_TOPIC ="com.autom.kozaris.microcontrol.MSG_EXT_SEND_TOPIC";
        String _PUB_PAYLOAD ="com.autom.kozaris.microcontrol.EXTRA_MESSAGE_SEND_PAYLOAD";
        String _INCOMING_TOPIC ="com.autom.kozaris.microcontrol.MSG_REC_EXT_TOPIC";
        String _INCOMING_PAYLOAD ="com.autom.kozaris.microcontrol.MSG_REC_ECT_PAYLOAD";
        String _STATUS_INFO ="com.autom.kozaris.microcontrol.SERV_EXT_INF";
    }
    interface ACTIONS{

        //Intent ACTION strings FOR PubSubReceiver
        String _SERVICE_TERMINATE ="com.autom.kozaris.microcontrol.TERMMQTTSERV";
        String _PING="com.autom.kozaris.microcontrol.PING_BRK_ACT";
        String _PUBLISH="com.autom.kozaris.microcontrol.MSG_ACT_SEND";
        String _SUBSCRIBE="com.autom.kozaris.microcontrol.SUB_ACT_MAKE";
        //Intent ACTION and EXTRAS FOR ServiceReceiver
        String _RECEIVED_MESSAGE ="com.autom.kozaris.microcontrol.MSG_REC_ACT";
        String _RECEIVED_REPEAT ="com.autom.kozaris.microcontrol.MSG_REP_REC_ACT";
        //Information strings
        String _SERVICE_SEND_STATUS ="com.autom.kozaris.microcontrol.SERV_STATUS";
    }
}
