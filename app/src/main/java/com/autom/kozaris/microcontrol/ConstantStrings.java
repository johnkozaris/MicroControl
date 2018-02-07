package com.autom.kozaris.microcontrol;

/**
 * Interface {@link ConstantStrings}
 * <p>
 * Περιέχει σταθερές που χρησιμοποιούνται ανάμεσα στις διαφορετικές κλάσεις του προγράμματος
 */
public interface ConstantStrings {
    //Σταθερες που λειτουργουν ως ετικέτες για την αποθήκευση δεδομένων στην συσκευη
    interface STORAGE {
        String STORAGE_KEY_CON_SETTINGS_PREF = "my_conn_setting_mqtt_br";
        //Ετικέτα Θέσης της μεταβλητής: Διευθυνση Μεσίτη
        String PREFERENCE_BROKER_ADDR = "pref_br_addr";
        //Ετικέτα Θέσης της μεταβλητής: Ονομα χρήστη Μεσίτη
        String PREFERENCE_BROKER_USERNAME = "pref_br_username";
        //Ετικέτα Θέσης της μεταβλητής: Κωδικός Προσβασης Μεσίτη
        String PREFERENCE_BROKER_PASSWORD = "pref_br_pass";
        //Ετικέτα Θέσης της μεταβλητής: Clean Session Μεσίτη
        String PREFERENCE_BROKER_CLEANSESSION = "pref_br_cleanses";
    }

    //Σταθερές που λειτουργουν ως ετικέτε Ρυθμίσεων της εφαρμογής
    interface SETTINGS {
        //Ετικέτα Θέσης των Ρυθμίσεων την Δραστηριότητας MainActivity
        String _ACTIVITY = "com.autom.kozaris.microcontrol.MICROCONTROLACTINFO";
        //Ετικέτα Θέσης της μεταβλητής: Κατάσταση MainActivity
        //True= Activity τρέχει, False= Activity εχει σταματήσει
        String _QUERY_ACTIVE = "com.autom.kozaris.microcontrol.ACTACTIVE";
        //Ετικέτα Θέσης της μεταβλητής: Κατάσταση MicroMqttService
        //True= Service τρέχει, False= Service εχει σταματήσει
        String _BOOL_SERVICE_RUNNING = "com.autom.kozaris.microcontrol.SRVRNBL";
        //Ετικέτα Θέσης της μεταβλητής: Κατάσταση σύνδεσης MicroMqttService με τον μεσίτη
        //True= Service έχει συνδεθεί, False= Service εχει αποσυνδεθεί
        String _BOOL_SERVICE_CONNECTED = "com.autom.kozaris.microcontrol.SRVCNCTBL";
    }

    //Ονόματα Intent.Extras που χρησιμοποιούντε για την αποστολή Intents
    interface EXTRAS {
        //Ετικέτα Μεταβλητής: Θέμα εγγραφής
        String _SUB_TOPIC = "com.autom.kozaris.microcontrol.SUB_EXT_TOPIC";
        //Ετικέτα Μεταβλητής: Θέμα δημοσίευσης
        String _PUB_TOPIC = "com.autom.kozaris.microcontrol.MSG_EXT_SEND_TOPIC";
        //Ετικέτα Μεταβλητής: Μήνυμα προς δημοσίευση
        String _PUB_PAYLOAD = "com.autom.kozaris.microcontrol.EXTRA_MESSAGE_SEND_PAYLOAD";
        //Ετικέτα Μεταβλητής: Θεμα μύνήματος που λήφθηκε
        String _INCOMING_TOPIC = "com.autom.kozaris.microcontrol.MSG_REC_EXT_TOPIC";
        //Ετικέτα Μεταβλητής: περιεχόμενο μηνύματος που λύφθηκε
        String _INCOMING_PAYLOAD = "com.autom.kozaris.microcontrol.MSG_REC_ECT_PAYLOAD";
        //Ετικέτα Μεταβλητής: Κατασταση σύνδεσης υπηρεσίας MicroMqttService
        String _STATUS_INFO = "com.autom.kozaris.microcontrol.SERV_EXT_INF";
    }

    //Ονόματα Intent.Actions που χρησιμοποιούντε για την αποστολή Intents
    interface ACTIONS {
        //Τερματισμος MicroMqttService
        String _SERVICE_TERMINATE = "com.autom.kozaris.microcontrol.TERMMQTTSERV";
        //Ping στον μεσίτη
        String _PING = "com.autom.kozaris.microcontrol.PING_BRK_ACT";
        //Αιτηση στην υπηρεσία MicroMqttService για δημοσιευση μηνύματος
        String _PUBLISH = "com.autom.kozaris.microcontrol.MSG_ACT_SEND";
        //Αιτηση στην υπηρεσία MicroMqttService για εγγαφή σε ένα θέμα
        String _SUBSCRIBE = "com.autom.kozaris.microcontrol.SUB_ACT_MAKE";
        //Ενημέρωση του MqttClientServiceReceiver οτι ενα μήνυμα λήφθηκε απο την υπηρεσια
        String _RECEIVED_MESSAGE = "com.autom.kozaris.microcontrol.MSG_REC_ACT";
        String _RECEIVED_REPEAT = "com.autom.kozaris.microcontrol.MSG_REP_REC_ACT";
        //Αποστολη κατάστασης της υπησρείσας (MicroMqttService.ConnectionStatus)
        String _SERVICE_SEND_STATUS = "com.autom.kozaris.microcontrol.SERV_STATUS";
    }
}
