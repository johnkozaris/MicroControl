package com.autom.kozaris.microcontrol;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.autom.kozaris.microcontrol.Receivers.ConnectionStatusReceiver;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.io.FileOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Αυτή η κλάση είναι ενα Android Service που δημιουργεί σύνδεση με έναν Mqtt Μεσίτη,με την ελάχιστη κατανάλωση μπαταρίας
 * Η σύνδεση Push διατηρείται ενεργή ακόμα και αν το κινητό τεθεί σε κατάσταση αδράνειας
 * Created by Ioannis Kozaris on 23/6/2016.
 */
public class MicroMqttService extends Service implements MqttCallback, IMqttActionListener {

    //region STATIC DECLARATION STRINGS
    String TAG = "MicroMqttService";
    public static String DefaultBrokerAddress = "tcp://178.23.76.11:1883";//Προεπιλεγμένη διεύθυνση μεσίτη
    public static String DefaultUsername = "androiduse";//Προεπιλεγμένο όνομα χρήστη
    public static String DefaultPassword = "and3125";//Προεπιλεγμένος κωδικός πρόσβασης
    public static boolean DefaultCleanSession = true;
    private String BrokerAddress;
    private String Username;
    private static String Password;
    private boolean CleanSession;
    //Αρχικοποίηση Broadcast Receivers
    private NetworkChangedReceiver netChangeReceiver;
    private PingBroadcast pingBroadcaster;
    private PubSubRequestReceiver publishReceiver;
    private Hashtable<String, String> msgCache = new Hashtable<>();//Cache εισερχόμενων μνημάτων
    //endregion


    //region Internal Class Variables
    private enum ConnectionStatus {//Κατάσταση Υπηρεσίας
        INITIALIZING,//Γίνετέ έναρξη υπηρεσίας
        CONNECTING,//Γίνετε σύνδεση σε μεσίτη
        CONNECTED,//Υπάρχει σύνδεση σε μεσίτη
        DISCONNECTED_NO_INTERNET,//Αποσυνδέθηκε γιατι δεν υπάρχει σύνδεση στο διαδίκτυο
        DISCONNECTED_MANUALLY,//Αποσυνδέθηκε από τον χρήστη
        DISCONNECTED_DATA_DISABLED,//Αποσυνδέθηκε γιατι απενεργοποιήθηκαν τα δεδομένα(Wi-Fi η 3G)
    }

    private ConnectionStatus conStatus = ConnectionStatus.INITIALIZING;//Αρχικοποίηση κατάστασης υπηρεσίας
    MqttClient mqttclient = null;//Αντικείμενο MQTT πελάτη
    private String newModulesTopic = null;// Θέμα εισερχόμενων συσκευών
    //endregion

    //region ANDROID SERVICE SPECIFIC OVERRIDES

    /**
     * Μέθοδος που καλείται κατά την δημιουργία της Υπηρεσίας
     */
    @Override
    public void onCreate() {
        super.onCreate();
        getPreferenceConnectionSettings();
        //Διάβασμα μοναδικού αναγνωριστικού συσκευής Android
        @SuppressLint("HardwareIds") String androidID = android.provider.Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        newModulesTopic = "/android/" + androidID + "/newmodules";//Κατασκευή θέματος εισερχομένων συσκευών
        conStatus = ConnectionStatus.INITIALIZING;
        updateServicePreferences();
        inUndiliveredList = new ArrayList<>();//Αρχικοποίηση λίστας μη παραδεδομένων μηνυμάτων
        ClientInitialization(BrokerAddress);//Αρχικοποίηση πελάτη MQTT
    }

    /**
     * Λήψη παραμέτρων σύνδεσής από τις μεταβλητές προγράμματος.
     * Οι μεταβλητές αυτές καταχωρήθηκαν κατά την έναρξή της εφαρμογής
     * στην δραστηριότητα {@link LoginActivity}
     */
    private void getPreferenceConnectionSettings() {
        SharedPreferences Pref = getApplicationContext().getSharedPreferences(ConstantStrings.STORAGE.STORAGE_KEY_CON_SETTINGS_PREF, Context.MODE_PRIVATE);
        BrokerAddress = Pref.getString(ConstantStrings.STORAGE.PREFERENCE_BROKER_ADDR, DefaultBrokerAddress);
        if (BrokerAddress.isEmpty()) {
            BrokerAddress = DefaultBrokerAddress;
        }
        Username = Pref.getString(ConstantStrings.STORAGE.PREFERENCE_BROKER_USERNAME, DefaultUsername);
        Password = Pref.getString(ConstantStrings.STORAGE.PREFERENCE_BROKER_PASSWORD, DefaultPassword);
        CleanSession = Pref.getBoolean(ConstantStrings.STORAGE.PREFERENCE_BROKER_CLEANSESSION, DefaultCleanSession);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        /*
        *Εκτέλεση AsyncTask(Ασύγχρονης διεργασίας) σε ένα καινούργιο thread
        * η διεργασία αυτή {MicorMqttService.ExecuteService} αναλαμβάνει την δημιουργία σύνδεσης και την διατήρηση της.
        * Εξαιτίας του multithreading , η σύνδεση στον μεσίτη πρέπει να κλειδώνεται
        * στο πρώτο thread που την δημιουργεί
         */
        new Thread() {
            @Override
            public void run() {
                new ExecuteService().execute();
            }
        }.start();
        return START_STICKY;
    }

    /**
     * Έναρξη διαδικασίας σύνδεσης. Έλεγχος δικτύου και αρχικοποίηση Intents
     */
    synchronized void handleStart() {
        if (mqttclient == null) {
            stopSelf();
            return;
        } // Αν δεν πρόλαβε να αρχικοποιηθεί ο πελάτης MQTT  τερμάτισε την υπηρεσία
        ConnectivityManager connmanager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        assert connmanager != null;
        if (connmanager.getActiveNetworkInfo() == null) {
            conStatus = ConnectionStatus.DISCONNECTED_DATA_DISABLED;
            broadcastStatus("Unable to Connect. No Network");
            return;//Τα δεδομένα είναι απενεργοποιημένα
        }
        repeatBroadcastStatus();//Αποστολή της κατάστασης σύνδεσης σε όποια διεργασία την χρειάζεται
        repeatIncomingMessagesBroadcast();
        if (!isAlreadyConnected()) {
            conStatus = ConnectionStatus.CONNECTING;
            //Δημιουργία μόνιμης ειδοποίησης στην μπάρα ειδοποιήσεων της συσκευής
            UpdateStickyNotification("Δημιουργία Σύνδεσης");
            if (IsOnline() && ConnectToBroker()) {  //Προσπάθεια σύνδεσης:(ConnectoToBroker())
                //H σύνδεση πέτυχε
                LockConnection.unlock();//ξεκλείδωμα thread αυτού του τμήματος κώδικα
                SubscribeToTopic(newModulesTopic);//Εγγραφή στο θέμα εισερχομένων συσκευών
            } else {
                //Η σύνδεση απέτυχε, δεν υπάρχει σύνδεση στο διαδίκτυο
                conStatus = ConnectionStatus.DISCONNECTED_NO_INTERNET;
                broadcastStatus("Unable to Connect. No Internet");
            }
        }
        if (publishReceiver == null)//Receiver μήνυμάτων προς Δημοσίευση η Εγγραφη
        {
            publishReceiver = new PubSubRequestReceiver();
            registerReceiver(publishReceiver, new IntentFilter(ConstantStrings.ACTIONS._PUBLISH));//Ενέργεια δημοσίευσης
            registerReceiver(publishReceiver, new IntentFilter(ConstantStrings.ACTIONS._RECEIVED_REPEAT));//Επανάληψη Intent μηνύματος που λήφθηκε
            registerReceiver(publishReceiver, new IntentFilter(ConstantStrings.ACTIONS._SUBSCRIBE));//Ενέργεια εγγραφής σε θέμα
            registerReceiver(publishReceiver, new IntentFilter(ConstantStrings.ACTIONS._SERVICE_TERMINATE));//Ενεργεια τερματισμού υπηρεσίας
        }
        if (netChangeReceiver == null)//Receiver μυνημάτων αλλαγής κατάστασης δικτύου
        {
            netChangeReceiver = new NetworkChangedReceiver();
            registerReceiver(netChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));//Ενημέρωση κατάστασης
        }
        if (pingBroadcaster == null)//Receiver εντολής: αποστολή ping στον μεσίτη
        {
            pingBroadcaster = new PingBroadcast();
            registerReceiver(pingBroadcaster, new IntentFilter(ConstantStrings.ACTIONS._PING));
        }
    }

    /**
     * Μέθοδος που εκτελείτε κατά τον τερματισμό της υπηρεσίας
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectFromBroker();//Αποσύνδεση απο τον μεσίτη
        updateServicePreferences(false, false);//Ειδοποίηση οτι η υπηρεσία δεν εκτελέιται
        broadcastStatus("Disconnected");//Αλλαγή κατάστασης υπηρεσίας
        if (netChangeReceiver != null)//Αποσύνδεση απο εξωτερικο receiver
        {
            unregisterReceiver(netChangeReceiver);
            netChangeReceiver = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    //endregion

    //region MQTT CLIENT ACTION LISTENERS

    /**
     * Μέθοδος που καλείτε όταν η σύνδεση με τον μεσίτη χαθεί
     *
     * @param cause Λόγος αποσύνδεσης
     */
    @Override
    public void connectionLost(Throwable cause) {
        //Δημιουργία wakelock σε περίπτωση που η συσκευή είναι σε κατάσταση αδράνειας
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        assert pm != null;
        PowerManager.WakeLock wlock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wlock.acquire(5 * 60 * 1000L /*5 λεπτά Timeout*/);
        updateServicePreferences(true, false);
        if (!IsOnline())//Αν δεν υπάρχει σύνδεση στο ίντερνετ
        {
            conStatus = ConnectionStatus.DISCONNECTED_NO_INTERNET;
            broadcastStatus("Disconnected. No Internet Connection");
            UpdateStickyNotification("Μη Συνδεδεμένο. Δεν υπάρχει Internet");//Δημιουργία ειδοποίησης
        } else {//Υπάρχει σύνδεση στο διαδίκτυο
            conStatus = ConnectionStatus.DISCONNECTED_DATA_DISABLED;
            broadcastStatus("Disconnected. Reason Unknown. Reconnecting");
            if (ConnectToBroker()) {//Προσπάθεια επανασύνδεσης
                SubscribeToTopic(newModulesTopic);//Επανεγγραφή στο θέμα νέων συσκευών
            }
        }
        wlock.release();//Απελευθέρωση wakelock
    }

    List<String> inUndiliveredList;// λίστα μην παραδομένων εισερχόμενων μηνυμάτων

    /**
     * Μέθοδος που καλείται όταν ο πελάτης λάβει μήνυμα από τον μεσίτη
     *
     * @param topic   Θέμα εισερχόμενου μηνύματος
     * @param message περιεχόμενο εισερχόμενου μηνύματος
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        assert pm != null;
        PowerManager.WakeLock wlock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wlock.acquire(5 * 60 * 1000L /*10 λεπτά*/);
        String messageString = new String(message.getPayload());//Μετατροπή byte σε string
        if (saveIncomingMessage(topic, messageString))//Αποθήκευση μηνύατος σε περίπτωση που ο κώδικας διακοπεί
        {
            SharedPreferences activityinfo = getSharedPreferences(ConstantStrings.SETTINGS._ACTIVITY, MODE_PRIVATE);
            boolean ActivityRunning = activityinfo.getBoolean(ConstantStrings.SETTINGS._QUERY_ACTIVE, false);
            if (ActivityRunning)//Αν η δραστηριότητα mainActivity εκτελείτε
            {
                //αποστολή μηνύματος στον MqttClientServiceReceiver ώστε να το προωθήσει στην δραστηριότητα
                broadcastIncomingMessage(topic, messageString);
            } else {//Αν δεν εκτελείται
                //Αποθήκευση του μηνύματος για αποστολή αργότερα
                inUndiliveredList.add(topic + '$' + messageString);
                Log.d("ok", "ok");
                SaveMessagesToSd(this, inUndiliveredList);//Αποθήκευση
            }
        }
        ScheduleNextKeepAlive();
        wlock.release();
    }

    /**
     * Η αποστολή ενός μηνύματος ολοκληρώθηκε
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
    //endregion


    //region MQTT CONNECTION CALLBACK LISTENERS (FOR MESSAGE AND CONNECTION DELIVERIES)

    /**
     * Μέθοδος που καλείται κατά την επιτυχή σύνδεση με έναν μεσίτη
     */
    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        inconnecting = false;//Ολοκληρώθηκε μια προσπάθεια σύνεσης
        PublishToTopic("hi", "android");//Δοκιμαστική δημοσίευση
        Log.d(TAG, "onSuccess");
    }

    /**
     * Μέθοδος που καλείται όταν αποτύχει η σύνδεση με έναν μεσίτη
     */
    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        inconnecting = false;
        Log.d(TAG, "onFailure");
    }
    //endregion


    //region HELPER METHODS

    //region BROADCASTS & UPDATE NOTIFICATION

    /**
     * Δημιουργία- Ενημέρωση ειδοποίηση στην μπάρα ειδοποιήσεων
     *
     * @param ContentMessage Μήνυμα που εμφανίζεται
     */
    private void UpdateStickyNotification(String ContentMessage) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();

        inboxStyle.setBigContentTitle("Mikro Mqtt");
        inboxStyle.addLine(ContentMessage);

        Intent terminateIntent = new Intent();
        terminateIntent.setAction(ConstantStrings.ACTIONS._SERVICE_TERMINATE);
        PendingIntent actionIntent = PendingIntent.getBroadcast(this, 1, terminateIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_mikro_cloud);
        Notification notification = new NotificationCompat.Builder(this, "1")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.mipmap.ic_mikro_cloud)
                .setContentTitle("Mikro MQTT")
                .setContentText(ContentMessage)//Καταχώρηση μηνύματος
                .setStyle(inboxStyle)
                .setLargeIcon(bm)
                .setWhen(System.currentTimeMillis())
                .addAction(android.R.drawable.ic_delete, "Τερματισμός Υπηρεσίας", actionIntent)//Υποχρεωτικό πλήκτρο τερματισμού υπηρεσίας
                .build();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;//Μόνιμη ειδοποίηση καθως εκτελείτε η υπηρεσία
        notification.flags |= Notification.FLAG_NO_CLEAR;//Ο χρήστης δεν μπορει να απορρίψει την ειδοποίηση
        assert nm != null;
        nm.notify(0, notification);

    }

    /**
     * Αποστολή Intent με την κατάσταση της υπηρεσίας
     *
     * @param status Κατάσταση υπηρεσίας
     */
    private void broadcastStatus(String status) {
        Intent statusIntent = new Intent();
        statusIntent.setAction(ConstantStrings.ACTIONS._SERVICE_SEND_STATUS);
        statusIntent.putExtra(ConstantStrings.EXTRAS._STATUS_INFO, status);
        sendBroadcast(statusIntent);
    }

    /**
     * Επαναδημιουργία κύκλου μηνύματος μέσα στην εφαρμογή
     * Το μήνυμα θα αποσταλεί ξανά στον {MqttClientServiceReceiver} ώστε να το προωθήσει στην δραστηριότητα MainActivity
     *
     * @param topic   Θεμα μηνύματος
     * @param message περιεχόμενο μηνύματος
     */
    private void broadcastIncomingMessage(String topic, String message) {
        Intent IncomingMessageIntent = new Intent();
        IncomingMessageIntent.setAction(ConstantStrings.ACTIONS._RECEIVED_MESSAGE);
        IncomingMessageIntent.putExtra(ConstantStrings.EXTRAS._INCOMING_TOPIC, topic);
        IncomingMessageIntent.putExtra(ConstantStrings.EXTRAS._INCOMING_PAYLOAD, message);
        sendBroadcast(IncomingMessageIntent);
    }

    /**
     * Ενημερωση Μεταβλητών προγράμματος που κρατάνε την κατάσταση της υπηρεσίας
     *
     * @param running   Η υπηρεσία εκτελείτε
     * @param connected Η υπηρεσία εχει συνδεθεί σε μεσίτη
     */
    private void updateServicePreferences(boolean running, boolean connected) {
        SharedPreferences sp = getSharedPreferences(ConstantStrings.SETTINGS._ACTIVITY, MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(ConstantStrings.SETTINGS._BOOL_SERVICE_RUNNING, running);
        ed.putBoolean(ConstantStrings.SETTINGS._BOOL_SERVICE_CONNECTED, connected);
        ed.apply();
    }

    private void updateServicePreferences() {
        SharedPreferences sp = getSharedPreferences(ConstantStrings.SETTINGS._ACTIVITY, MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(ConstantStrings.SETTINGS._BOOL_SERVICE_RUNNING, true);
        ed.apply();
    }
    //endregion

    //region PUBLICLY ACCESSIBLE METHODS

    /**
     * Κανονικοποίηση σε String και επαναποστολή Intent κατάστασης υπηρεσίας
     */
    public void repeatBroadcastStatus() {
        String status = "";
        switch (conStatus) {
            case INITIALIZING:
                status = "Intializing..";
                break;
            case CONNECTING:
                status = "Connecting...";
                break;
            case CONNECTED:
                status = "You are Connected";
                break;
            case DISCONNECTED_NO_INTERNET:
                status = "Disconnected. Check Internet Connection";
                break;
            case DISCONNECTED_MANUALLY:
                status = "Disconnected. Manually";
                break;
        }
        broadcastStatus(status);
    }

    /**
     * Αποσύνδεση υπηρεσίας από την εφαρμογή android
     */
    public void disconnect() {
        disconnectFromBroker();//Αποσύνδεση από τον μεσίτη
        conStatus = ConnectionStatus.DISCONNECTED_MANUALLY;
        broadcastStatus("Disconnected");
        updateServicePreferences(false, false);
        this.stopSelf();//Τερματισμός υπηρεσίας
    }

    //endregion

    /**
     * Αποθήκευση μηνύματος στην κάρτα SD η στον τοπικό χώρο αποθήκευσης
     *
     * @param <E>      Generic Αντικείμενο για το OutputStream
     * @param mContext Context υπηρεσίας
     * @param list     Λίστα μηνυμάτων
     */
    public static <E> void SaveMessagesToSd(Context mContext, List<E> list) {
        try {
            FileOutputStream fos = mContext.openFileOutput("undeliveredincoming" + ".dat", MODE_PRIVATE);//Δημιουργία αρχείου dat
            ObjectOutput oos = new ObjectOutputStream(fos);
            oos.writeObject(list);//Εγγραφή στην κάρτα SD
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Δημιουργία εγγραφής σε ένα θέμα
     *
     * @param topic θέμα Εγγραφής
     */
    private void SubscribeToTopic(String topic) {
        boolean subscribed = false;
        if (isAlreadyConnected())//Έλεγχος σύνδεσης
        {
            try {
                String[] topics = {topic};
                int[] qos = {1};
                mqttclient.subscribe(topics, qos);//Εγγραφή πελάτης σε θέμα
                subscribed = true;
            } catch (MqttException e)//Η εγγραφή απέτυχε
            {
                Log.e(TAG, "Cannot Subscribe " + e.getMessage());
            } catch (IllegalArgumentException e)//Λάθος μορφή μεταβλητής κειμένου
            {
                Log.e(TAG, e.getMessage());
            }
        } else {
            updateServicePreferences(true, false);
            Log.e(TAG, "Not connected to subscribe");
        }
        if (!subscribed) {
            broadcastStatus("Subscription Failed");//Ενημέρωση με Intent ότι απέτυχε μια εγγραφή
        }
    }

    private void PublishToTopic(String topic, String payload) {
        if (isAlreadyConnected()) {//Έλεγχος κατάστασης σύνδεσης
            try {
                mqttclient.publish(topic, new MqttMessage(payload.getBytes()));//Δημοσίευση μηνύματος
            } catch (MqttException e) {//Η δημοσίευση απέτυχε
                updateServicePreferences(true, false);
                Log.e(TAG, e.getMessage());
            }
        }
    }

    /**
     * Η μέθοδος ελέγχει αν υπάρχει σύνδεση το Ίντερνετ, μέσω του Connectivity Service της συσκευής
     *
     * @return True: Υπάρχει σύνδεση
     */
    private boolean IsOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        assert cm != null;
        return (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected());
    }

    /**
     * Η μέθοδος ελέγχει αν η υπηρεσία ειναι συνδεδεμένη με έναν μεσίτη
     *
     * @return True: Υπάρχει σύνδεση
     */
    private boolean isAlreadyConnected() {
        return this.mqttclient != null && this.mqttclient.isConnected();
    }

    /**
     * Επανάληψη κύκλου μηνυμάτων που βρίσκονται στo Cache
     */
    private void repeatIncomingMessagesBroadcast() {
        Enumeration<String> enRetreiver = msgCache.keys();
        while (enRetreiver.hasMoreElements())//Για κάθε μήνυμα στο Cache
        {
            String nextTopic = enRetreiver.nextElement();
            String nextMessage = msgCache.get(nextTopic);
            broadcastIncomingMessage(nextTopic, nextMessage);//Επανάληψη αποστολής στον MqttClientServiceReceiver
        }
    }

    /**
     * Αποθήκευση μηνύματος στο Cache
     *
     * @param topic         Θέμα μηνύματος
     * @param messageString Περιεχόμενο μηνύματος
     * @return True: Επιτυχής αποθήκευση
     */
    private synchronized boolean saveIncomingMessage(String topic, String messageString) {
        if (messageString.isEmpty())//Αν το μήνυμα είναι κενό δεν θα αποθηκευτεί
        {
            msgCache.remove(topic);
            return false;
        } else {
            msgCache.put(topic, messageString);
            return true;
        }
    }

    ReentrantLock LockInit = new ReentrantLock();//Κλειδαριά Thread Για την διαδικασία αρχικοποίησης του πελάτη MQTT

    private void ClientInitialization(String Brokeraddr) {
        LockInit.lock();//Κλείδωμα εκτέλεσης από αλλα thread
        try {
            //Δημιουργία Θέσης αποθήκευσης μηνυμάτων, που βρίσκονται ακόμα στο socket και δεν παραλήφθηκαν(MQTT Persistence)
            MqttClientPersistence persistence = new MqttDefaultFilePersistence(this.getApplicationInfo().dataDir);
            @SuppressLint("HardwareIds") String androidID = android.provider.Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            mqttclient = new MqttClient(Brokeraddr, androidID, persistence);//Αρχικοποίηση MQTT πελάτη
            mqttclient.setCallback(this);//Κλάση που ακούει στα γεγονότα του πελάτη MQTT(Λήψη μηνυμάτων κλπ.)
        } catch (MqttException e)//Απέτυχε η αρχικοποίηση
        {
            Log.d(TAG, e.getMessage());
            mqttclient = null;
        }
        LockInit.unlock();//Ξεκλείδωμα μεθόδου
    }

    ReentrantLock LockConnection = new ReentrantLock();//Κλειδαριά Thread Για την διαδικασία σύνδεσης του πελάτη MQTT
    boolean inconnecting = false;//Εκτελείτε η διαδικασία σύνδεσης

    private boolean ConnectToBroker() {
        LockConnection.lock();//Κλείδωμα εκτέλεσης από αλλα thread
        if (isAlreadyConnected() || inconnecting) {
            return true;
        }
        inconnecting = true;
        conStatus = ConnectionStatus.CONNECTING;
        try {
            MqttConnectOptions options1 = new MqttConnectOptions();
            options1.setKeepAliveInterval(1000);//Mqtt Keep Alive
            options1.setCleanSession(CleanSession);//Mqtt Clean session
            options1.setUserName(Username);//Ονομα χρήστη μεσίτη
            options1.setPassword(Password.toCharArray());//κωδικός πρόσβασης μεσίτη
            options1.setConnectionTimeout(10);//Χρόνος timeout προσπάθειας σύνδεσης
            mqttclient.connect(options1);//Σύνδεση
            broadcastStatus("Connected");
            conStatus = ConnectionStatus.CONNECTED;
            ScheduleNextKeepAlive();//Προγραμματισμός αυτόματου Keep Alive Ping προς τον μεσίτη
            UpdateStickyNotification("Συνδεδεμένο");
            updateServicePreferences(true, true);
            inconnecting = false;
            sendBroadcast(new Intent(ConnectionStatusReceiver.ACTIONS.CONNECTED));//Ειδοποίηση τοι η σύνδεση πέτυχε
            return true;
        } catch (MqttException e) {//Η σύνδεση απέτυχε
            inconnecting = false;
            conStatus = ConnectionStatus.DISCONNECTED_MANUALLY;
            broadcastStatus("Connection Failed");
            UpdateStickyNotification("Αποτυχία Σύνδεσης. O Μεσίτης δεν Ανταποκρίνεται");
            sendBroadcast(new Intent(ConnectionStatusReceiver.ACTIONS.FAILED_TO_CONNECT));
            ScheduleNextKeepAlive();
            LockConnection.unlock();
            return false;
        }

    }

    /**
     * Αποσύνδεση από τον μεσίτη και κατάργηση Receivers
     */
    private void disconnectFromBroker() {
        try {
            if (netChangeReceiver != null) {
                unregisterReceiver(netChangeReceiver);
                netChangeReceiver = null;
            }
            if (pingBroadcaster != null) {
                unregisterReceiver(pingBroadcaster);
                pingBroadcaster = null;
            }
            if (publishReceiver != null) {
                unregisterReceiver(publishReceiver);
                publishReceiver = null;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        try {
            if (mqttclient != null) {
                mqttclient.disconnect();
            }
        } catch (MqttException e) {
            Log.e(TAG, "Disconnection Failure");
        } finally {
            mqttclient = null;
        }
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);//Διαγραφή μηνύματος ειδοποίησης από την μπάρα ειδοποιήσεων
        assert nm != null;
        nm.cancelAll();
    }

    /**
     * Προγραμματισμός Ping προς τον μεσίτη MQTT ακόμα και αν η συσκευή βρίσκεται σε αδράνεια
     * Ετσι κρατείτε ενεργό το socket σύνδεσης
     */
    private void ScheduleNextKeepAlive() {
        //Δημιουργία Intent για την αποστολή p;ing
        //To ping εκτελείτε από τον BroadCastReceiver PingBroadcast
        PendingIntent pIntent = PendingIntent.getBroadcast(this, 0, new Intent(ConstantStrings.ACTIONS._PING), PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar stopSleeping = Calendar.getInstance();
        short keepAliveInterval = 950;
        stopSleeping.add(Calendar.SECOND, keepAliveInterval);//χρονολογία αφύπνισης 950 δευτερόλεπτα απο τώρα
        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);//Δημιουργία ξυπνητηριού στην χρονολογία αφύπνισης
        assert alarm != null;
        alarm.set(AlarmManager.RTC_WAKEUP, stopSleeping.getTimeInMillis(), pIntent);//Καταχώρηση αφύπνισης CPU κατά την οποία θα εκτελεστεί το ping Ιntent
    }

    //endregion


    //region HELPER CLASSES

    /**
     * Εσωτερικός BroadcastReceiver που αναλαμβάνει την αποστολή ping στον μεσίτη, με σκοπό της διατήρησης
     * του μεταξύ τους socket
     */
    public class PingBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mqttclient != null) {
                try {
                    byte[] b = {0x01};//Μύνημα 1 byte
                    mqttclient.publish("/ping", b, 0, false);//Δημοσίευση μηνύματος
                } catch (MqttException e) {//Η αποστολή απέτυχε
                    Log.e(TAG, "Cannot Ping");
                    try {//Προσπάθεια επανασύνδεσης στον μεσίτη
                        mqttclient.disconnect();
                    } catch (MqttException e1) {
                        Log.e(TAG, "Cannot Disconnect");
                    }
                    if (ConnectToBroker()) {
                        SubscribeToTopic(newModulesTopic);
                    }
                }
                ScheduleNextKeepAlive();//Προγραμματισμός επόμενου PING
            } else {
                if (ConnectToBroker()) {
                    SubscribeToTopic(newModulesTopic);
                }
                ScheduleNextKeepAlive();
            }
        }

    }

    /**
     * Εσωτερικός BroadcastReceiver που αναλαμβάνει να ενημερώσει την υπηρεσία σε περίπτωση
     * αλλαγής της κατάστασης του δικτύου
     */
    public class NetworkChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            assert pm != null;
            PowerManager.WakeLock wlock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);//Δημιουργία WakeLock
            wlock.acquire(5 * 60 * 1000L /*5 λεπτά*/);
            if (IsOnline()) {
                if (ConnectToBroker()) {//Υπάρχει σύνδεση
                    updateServicePreferences(true, true);//Ενημέρωση κατάστασης υπηρεσίας
                } else {//Δεν υπάρχει σύνδεση
                    updateServicePreferences(true, false);//Ενημέρωση κατάστασης υπηρεσίας
                }
            } else {
                updateServicePreferences(true, false);
                UpdateStickyNotification("Μη Συνδεδεμένο. Δεν υπάρχει Internet");
            }//Ενημέρωση ειδοποίησης
            wlock.release();
        }
    }

    /**
     * Εσωτερικός BroadcastReceiver που αναλαμβάνει να  λαμβάνει τις αιτήσεις δημοσίευσης και εγγραφής
     * που κάνουν οι υπόλοιπες κλάσεις της εφαρμογής
     */
    public class PubSubRequestReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            switch (action) {
                case ConstantStrings.ACTIONS._PUBLISH://Αιτήθηκε δημοσίευση μηνύματος
                    if (!intent.hasExtra(ConstantStrings.EXTRAS._PUB_TOPIC) && intent.hasExtra(ConstantStrings.EXTRAS._PUB_PAYLOAD)) {
                        return;
                    }
                    PublishToTopic(intent.getStringExtra(ConstantStrings.EXTRAS._PUB_TOPIC), intent.getStringExtra(ConstantStrings.EXTRAS._PUB_PAYLOAD));
                    break;
                case ConstantStrings.ACTIONS._SUBSCRIBE://Αιτήθηκε εγγραφή σε θέμα
                    if (!intent.hasExtra(ConstantStrings.EXTRAS._SUB_TOPIC)) {
                        return;
                    }
                    SubscribeToTopic(intent.getStringExtra(ConstantStrings.EXTRAS._SUB_TOPIC));
                    if (IsOnline() && ConnectToBroker() && mqttclient != null) {
                        return;
                    }
                    ClientInitialization(BrokerAddress);
                    break;
                case ConstantStrings.ACTIONS._SERVICE_TERMINATE://Αιτήθηκε τερματισμός της υπηρεσίας
                    disconnect();
                    break;
            }
        }
    }

    /**
     * AsyncTask που αναλαμβάνει να τρέχει την αρχικοποίηση και την σύνδεση του πελάτη σε ξεχωριστό thread
     */
    @SuppressLint("StaticFieldLeak")
    private class ExecuteService extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            handleStart();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}
