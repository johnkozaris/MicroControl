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
 * This is an Android Service Created for Connecting to Mqtt Server  with Minimum battery consumption
 * The Push connection is kept alive even if the phone goes to sleep
 * Created by Ioannis Kozaris on 23/6/2016.
 */
public class MicroMqttService extends Service implements MqttCallback,IMqttActionListener {

    //region STATIC DECLARATION STRINGS
    String TAG="MicroMqttService";
    public static final String _APPLICATION_ID ="com.autom.kozaris.microcontrol";
    public static String DefaultBrokerAddress = "tcp://178.23.76.11:1883";
    public static String DefaultUsername = "androiduse";
    public static String DefaultPassword = "and3125";
    public static boolean DefaultCleanSession = true;
    private String BrokerAddress;
    private String Username;
    private static String Password;
    private  boolean CleanSession;
    //Broadcast Receivers
    private NetworkChangedReceiver netChangeReceiver;
    private PingBroadcast pingBroadcaster;
    private PubSubRequestReceiver publishReceiver;

    private Hashtable<String, String> msgCache = new Hashtable<>();
    //endregion


    //region Internal Class Variables
    private enum ConnectionStatus {
        INITIALIZING,
        CONNECTING,
        CONNECTED,
        DISCONNECTED_NO_INTERNET,
        DISCONNECTED_MANUALLY,
        DISCONNECTED_DATA_DISABLED,
    }

    private ConnectionStatus conStatus = ConnectionStatus.INITIALIZING;
    MqttClient mqttclient=null;
    private String newModulesTopic =null;


    //endregion

    //region ANDROID SERVICE SPECIFIC OVERRIDES
    @Override
    public void onCreate() {
        super.onCreate();
        getPreferenceConnectionSettings();

        @SuppressLint("HardwareIds") String androidID = android.provider.Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        newModulesTopic = "/android/"+androidID+"/newmodules";
        conStatus= ConnectionStatus.INITIALIZING;
        updateServicePreferences(true);
        inUndiliveredList=new ArrayList<>();
        ClientInitialization(BrokerAddress);
    }

    private void getPreferenceConnectionSettings() {
        SharedPreferences Pref = getApplicationContext().getSharedPreferences(ConstantStrings.STORAGE.STORAGE_KEY_CON_SETTINGS_PREF,Context.MODE_PRIVATE);
        BrokerAddress = Pref.getString(ConstantStrings.STORAGE.PREFERENCE_BROKER_ADDR,DefaultBrokerAddress);
        if (BrokerAddress.isEmpty()){BrokerAddress=DefaultBrokerAddress;}
        Username = Pref.getString(ConstantStrings.STORAGE.PREFERENCE_BROKER_USERNAME,DefaultUsername);
        Password = Pref.getString(ConstantStrings.STORAGE.PREFERENCE_BROKER_PASSWORD,DefaultPassword);
        CleanSession = Pref.getBoolean(ConstantStrings.STORAGE.PREFERENCE_BROKER_CLEANSESSION,DefaultCleanSession);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        //android.os.Debug.waitForDebugger();
        new Thread() {
            @Override
            public void run() {
              new   ExecuteService().execute();
            }
        }.start();
        return START_STICKY;
    }

    synchronized void handleStart() {
        if (mqttclient==null) {stopSelf();return;}

        ConnectivityManager connmanager=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        if (connmanager.getActiveNetworkInfo()==null)
        {
            conStatus= ConnectionStatus.DISCONNECTED_DATA_DISABLED;
            broadcastStatus("Unable to Connect. No Network");
            return;
        }
        repeatBroadcastStatus();
        repeatIncomingMessagesBroadcast();
        //Make A permanent Notification
        if (!isAlreadyConnected()) {
            conStatus = ConnectionStatus.CONNECTING;
                UpdateStickyNotification("Δημιουργία Σύνδεσης");
            if (IsOnline() && ConnectToBroker()) {
                    LockConnection.unlock();
                    SubscribeToTopic(newModulesTopic);
            }
            else {
                conStatus = ConnectionStatus.DISCONNECTED_NO_INTERNET;
                broadcastStatus("Unable to Connect. No Internet");
            }
        }

        if (publishReceiver==null)
        {
            publishReceiver= new PubSubRequestReceiver();
            registerReceiver(publishReceiver,new IntentFilter(ConstantStrings.ACTIONS._PUBLISH));
            registerReceiver(publishReceiver,new IntentFilter(ConstantStrings.ACTIONS._RECEIVED_REPEAT));
            registerReceiver(publishReceiver,new IntentFilter(ConstantStrings.ACTIONS._SUBSCRIBE));
            registerReceiver(publishReceiver,new IntentFilter(ConstantStrings.ACTIONS._SERVICE_TERMINATE));
        }
        if (netChangeReceiver== null)
        {
            netChangeReceiver= new NetworkChangedReceiver();
            registerReceiver(netChangeReceiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
        if(pingBroadcaster==null)
        {
            pingBroadcaster= new PingBroadcast();
            registerReceiver(pingBroadcaster,new IntentFilter(ConstantStrings.ACTIONS._PING));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectFromBroker();
        updateServicePreferences(false,false);
        broadcastStatus("Disconnected");
        if (netChangeReceiver!=null)
        {
            unregisterReceiver(netChangeReceiver);
            netChangeReceiver= null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    //endregion

    //region MQTT CLIENT ACTION LISTENERS
    @Override
    public void connectionLost(Throwable cause) {
        //android.os.Debug.waitForDebugger();
        PowerManager pm =(PowerManager)getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wlock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,TAG);
        wlock.acquire();
        updateServicePreferences(true,false);
        if (!IsOnline())
        {
            conStatus = ConnectionStatus.DISCONNECTED_NO_INTERNET;
            broadcastStatus("Disconnected. No Internet Connection");
            UpdateStickyNotification("Μη Συνδεδεμένο. Δεν υπάρχει Internet");
            // the netwokr receiver will be called an we will reEstablish there when internet is available
        }
        else {
            //The phone is online
            conStatus= ConnectionStatus.DISCONNECTED_DATA_DISABLED;
            broadcastStatus("Disconnected. Reason Unknown. Reconnecting");
            if (ConnectToBroker()){
                SubscribeToTopic(newModulesTopic);
            }
        }
        wlock.release();
    }

    List<String> inUndiliveredList;

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        PowerManager pm =(PowerManager)getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wlock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,TAG);
        wlock.acquire();
        String messageString= new String(message.getPayload());
        if (saveIncomingMessage(topic,messageString))
        {
            SharedPreferences activityinfo= getSharedPreferences(ConstantStrings.SETTINGS._ACTIVITY,MODE_PRIVATE);
            boolean ActivityRunning= activityinfo.getBoolean(ConstantStrings.SETTINGS._QUERY_ACTIVE,false);
            if (ActivityRunning)
            {
                    broadcastIncomingMessage(topic,messageString);
            }
            else {
                //save for repeat
                inUndiliveredList.add(topic+'$'+messageString);
                Log.d("ok","ok");
                SaveMessagesToSd(this,"undeliveredincoming",inUndiliveredList);
            }
            if (messageString.contains("critical"))
            {//TODO Put /output/id/critical, payload data%critmessage
               // UpdateSecondaryNotification("Μύνημα MQTT",topic+": "+messageString,"Κλήση Ανάγκης",true,true);
            }
        }
        ScheduleNextKeepAlive();
        wlock.release();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
    //endregion


    //region MQTT CONNECTION CALLBACK LISTENERS (FOR MESSAGE AND CONNECTION DELIVERIES)
    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        inconnecting=false;
        PublishToTopic("hi","android");
        Log.d(TAG, "onSuccess");
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        inconnecting=false;
        Log.d(TAG, "onFailure");
    }
    //endregion


    //region HELPER METHODS

    //region BROADCASTS & UPDATE NOTIFICATION
    private  void UpdateStickyNotification(String ContentMessage){

                //Creating a notification Icon so the user knows the service is running
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//                Intent notificationIntent;
//                if(isAlreadyConnected()){
//                    notificationIntent= new Intent(this, MainActivity.class);
//                }else {
//                    notificationIntent= new Intent(this, LoginActivity.class);
//                }

                //PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();

        inboxStyle.setBigContentTitle("Mikro Mqtt");
        inboxStyle.addLine(ContentMessage);

        Intent terminateIntent= new Intent();
                terminateIntent.setAction(ConstantStrings.ACTIONS._SERVICE_TERMINATE);
                PendingIntent actionIntent= PendingIntent.getBroadcast(this,1,terminateIntent,PendingIntent.FLAG_CANCEL_CURRENT);
                Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_mikro_cloud);
                Notification notification = new NotificationCompat.Builder(this,"1")
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setSmallIcon(R.mipmap.ic_mikro_cloud)
                        .setContentTitle("Mikro MQTT")
                        .setContentText(ContentMessage)
                        .setStyle(inboxStyle)
                        .setLargeIcon(bm)
                        .setWhen(System.currentTimeMillis())
                        .addAction(android.R.drawable.ic_delete,"Τερματισμός Υπηρεσίας",actionIntent)
                        .build();
                notification.flags |= Notification.FLAG_ONGOING_EVENT;
                notification.flags |= Notification.FLAG_NO_CLEAR;
                nm.notify(0, notification);

    }

    private void broadcastStatus(String status){

        //informs the app about the activity status
        Intent statusIntent = new Intent();
        statusIntent.setAction(ConstantStrings.ACTIONS._SERVICE_SEND_STATUS);
        statusIntent.putExtra(ConstantStrings.EXTRAS._STATUS_INFO,status);
        sendBroadcast(statusIntent);
    }

    private void broadcastIncomingMessage(String topic,String message) {
        Intent IncomingMessageIntent = new Intent();
        IncomingMessageIntent.setAction(ConstantStrings.ACTIONS._RECEIVED_MESSAGE);
        IncomingMessageIntent.putExtra(ConstantStrings.EXTRAS._INCOMING_TOPIC,topic);
        IncomingMessageIntent.putExtra(ConstantStrings.EXTRAS._INCOMING_PAYLOAD,message);
        sendBroadcast(IncomingMessageIntent);
    }

//    private void UpdateSecondaryNotification(String title,String body,String ActionTitle,boolean vibrate, boolean Danger) {
//        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//
//        Intent notificationIntent = new Intent(this, MainActivity.class);
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//        Notification notification;
//        if (Danger) {
//            Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", "1000", null));
//            PendingIntent pendingcallIntent = PendingIntent.getActivity(this, 0, callIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//            NotificationCompat.Action mAction = new NotificationCompat.Action.Builder(android.R.drawable.ic_menu_call, ActionTitle, pendingcallIntent).build();
//            notification = new NotificationCompat.Builder(this)
//                    .setContentText(body)
//                    .setContentTitle(title)
//                    .setContentIntent(contentIntent)
//                    .setSmallIcon(android.R.drawable.ic_dialog_info)
//                    .setWhen(System.currentTimeMillis())
//                    .addAction(mAction)
//                    .build();
//        }
//        else {
//             notification = new NotificationCompat.Builder(this)
//                    .setContentText(body)
//                    .setContentTitle(title)
//                    .setContentIntent(contentIntent)
//                    .setSmallIcon(android.R.drawable.ic_dialog_info)
//                    .setWhen(System.currentTimeMillis())
//                    .build();
//        }
//        notification.flags |= Notification.FLAG_AUTO_CANCEL;
//        nm.notify(1, notification);
//    }

    private void updateServicePreferences(boolean running , boolean connected){
        SharedPreferences sp = getSharedPreferences(ConstantStrings.SETTINGS._ACTIVITY, MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(ConstantStrings.SETTINGS._BOOL_SERVICE_RUNNING,running);
        ed.putBoolean(ConstantStrings.SETTINGS._BOOL_SERVICE_CONNECTED,connected);
        ed.apply();
    }
    private void updateServicePreferences(boolean running ){
        SharedPreferences sp = getSharedPreferences(ConstantStrings.SETTINGS._ACTIVITY, MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(ConstantStrings.SETTINGS._BOOL_SERVICE_RUNNING,running);
        ed.apply();
    }
    //endregion

    //region PUBLICLY ACCESSIBLE METHODS
    public void repeatBroadcastStatus() {
        String status = "";
        switch (conStatus)
        {
            case INITIALIZING:
                status="Intializing..";
                break;
            case CONNECTING:
                status="Connecting...";
                break;
            case CONNECTED:
                status="You are Connected";
                break;
            case DISCONNECTED_NO_INTERNET:
                status="Disconnected. Check Internet Connection";
                break;
            case DISCONNECTED_MANUALLY:
                status="Disconnected. Manually";
                break;
        }
        broadcastStatus(status);
    }

    public void disconnect(){
        disconnectFromBroker();
        conStatus= ConnectionStatus.DISCONNECTED_MANUALLY;
        broadcastStatus("Disconnected");

       updateServicePreferences(false,false);

        this.stopSelf();
    }

    //endregion


    public static <E> void SaveMessagesToSd(Context mContext, String filename, List<E> list) {
        //android.os.Debug.waitForDebugger();
        try {
            //File outFile= new File(mContext.get,filename+".dat");
            FileOutputStream fos = mContext.openFileOutput(filename + ".dat", MODE_PRIVATE);
            ObjectOutput oos = new ObjectOutputStream(fos);
            oos.writeObject(list);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void SubscribeToTopic(String topic) {
        boolean subscribed = false;
        if (isAlreadyConnected())
        {
            try{
                String[] topics = {topic};
                int[] qos={1};
                mqttclient.subscribe(topics,qos);
                subscribed=true;
            }
            catch (MqttException e)
            {
                Log.e(TAG,"Cannot Subscribe "+e.getMessage());
            }
            catch (IllegalArgumentException e)
            {
               Log.e(TAG,e.getMessage());
            }
        }
        else{
            updateServicePreferences(true,false);
           Log.e(TAG,"Not connected to subscribe");
        }
        if (!subscribed)
        {
            broadcastStatus("Subscription Failed");
          //  notifyUI("Subscription Failed","Subscription Failed");
        }
    }

    private boolean PublishToTopic(String topic,String payload) {
        //android.os.Debug.waitForDebugger();
        if (!isAlreadyConnected())return false;
        else {
            try{
                mqttclient.publish(topic,new MqttMessage(payload.getBytes()));
                return true;
            }catch (MqttException e){
                updateServicePreferences(true,false);
                Log.e(TAG,e.getMessage());
                return false;
            }
        }
    }

    private boolean IsOnline() {
        ConnectivityManager cm =(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo()!=null&& cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected());
    }

    private boolean isAlreadyConnected() {
        return this.mqttclient != null && this.mqttclient.isConnected();
    }


    private void repeatIncomingMessagesBroadcast() {
     //   android.os.Debug.waitForDebugger();
        Enumeration<String> enRetreiver= msgCache.keys();
        while (enRetreiver.hasMoreElements())
        {
            String nextTopic= enRetreiver.nextElement();
            String nextMessage=msgCache.get(nextTopic);
            broadcastIncomingMessage(nextTopic,nextMessage);
        }
    }

    private synchronized boolean saveIncomingMessage(String topic, String messageString) {


        if (messageString.isEmpty())
        {
            msgCache.remove(topic);
            return false;
        }
        else {
            msgCache.put(topic,messageString);
            return true;
        }

        //return ((lastMessage==null)||lastMessage.equalsIgnoreCase(messageString));
    }

    ReentrantLock LockInit= new ReentrantLock();
    private void ClientInitialization(String Brokeraddr) {
        LockInit.lock();
        try {
            MqttClientPersistence persistence = new MqttDefaultFilePersistence(this.getApplicationInfo().dataDir);
            @SuppressLint("HardwareIds") String androidID = android.provider.Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            mqttclient = new MqttClient(Brokeraddr,androidID,persistence);
            mqttclient.setCallback(this);
        }catch (MqttException e)
        {
            Log.d(TAG,e.getMessage());
            mqttclient=null;
        }
        LockInit.unlock();
    }

    ReentrantLock LockConnection= new ReentrantLock();
    boolean inconnecting=false;

    private  boolean ConnectToBroker() {
        LockConnection.lock();
        //android.os.Debug.waitForDebugger();
        if (isAlreadyConnected() || inconnecting) {return true;}
            inconnecting=true;
            conStatus = ConnectionStatus.CONNECTING;
            try {
                MqttConnectOptions options1 = new MqttConnectOptions();
                options1.setKeepAliveInterval(1000);
                options1.setCleanSession(CleanSession);
                options1.setUserName(Username);
                options1.setPassword(Password.toCharArray());
                options1.setConnectionTimeout(10);
                mqttclient.connect(options1);
                broadcastStatus("Connected");
                conStatus = ConnectionStatus.CONNECTED;
                ScheduleNextKeepAlive();
                UpdateStickyNotification("Συνδεδεμένο");
                updateServicePreferences(true,true);
                inconnecting=false;
                sendBroadcast(new Intent(ConnectionStatusReceiver.ACTIONS.CONNECTED));
                return true;
            } catch (MqttException e) {
                inconnecting=false;
                conStatus = ConnectionStatus.DISCONNECTED_MANUALLY;
                broadcastStatus("Connection Failed");
                UpdateStickyNotification("Αποτυχία Σύνδεσης.O Μεσίτης δεν Ανταποκρίνεται");
                sendBroadcast(new Intent(ConnectionStatusReceiver.ACTIONS.FAILED_TO_CONNECT));
                ScheduleNextKeepAlive();
               LockConnection.unlock();
                return false;
            }

        }


    private void disconnectFromBroker() {
        try {
            if (netChangeReceiver!=null)
            {
                unregisterReceiver(netChangeReceiver);
                netChangeReceiver=null;
            }
            if (pingBroadcaster!=null)
            {
                unregisterReceiver(pingBroadcaster);
                pingBroadcaster=null;
            }
            if (publishReceiver!=null){
                unregisterReceiver(publishReceiver);
                publishReceiver=null;
            }
        }
        catch (Exception e)
        {
            Log.e(TAG,e.getMessage());
        }
        try {
            if (mqttclient!= null)
            {
                mqttclient.disconnect();
            }
        }
        catch (MqttException e)
        {
            Log.e(TAG,"Disconnection Failure");
        }
        finally {
            mqttclient=null;
        }
        //Remove Notification Icon
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();
    }

    private void ScheduleNextKeepAlive() {

        PendingIntent pIntent= PendingIntent.getBroadcast(this,0,new Intent(ConstantStrings.ACTIONS._PING),PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar stopSleeping = Calendar.getInstance();
        short keepAliveInterval = 950;
        stopSleeping.add(Calendar.SECOND, keepAliveInterval);
        AlarmManager alarm= (AlarmManager)getSystemService(ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP,stopSleeping.getTimeInMillis(),pIntent);
    }

    //endregion


    //region HELPER CLASSES

    public class PingBroadcast extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {
         //   android.os.Debug.waitForDebugger();
            if (mqttclient != null) {

                try {
                    byte[] b = {0x01};
                    mqttclient.publish("/ping", b, 0, false);
                } catch (MqttException e) {
                    Log.e(TAG, "Cannot Ping");
                    try {
                        mqttclient.disconnect();
                    }catch (MqttException e1)
                    {
                        Log.e(TAG,"Cannot Disconnect");
                    }
                    if (ConnectToBroker()){
                        SubscribeToTopic(newModulesTopic);
                    }
                }
                ScheduleNextKeepAlive();
            }
            else {
                if (ConnectToBroker()){
                    SubscribeToTopic(newModulesTopic);
                }
                ScheduleNextKeepAlive();
            }
        }

    }

    public class NetworkChangedReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            PowerManager pm =(PowerManager)getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wlock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,TAG);
            wlock.acquire();
            if (IsOnline()) {
                if (ConnectToBroker()) {
                    SubscribeToTopic(newModulesTopic);
                    updateServicePreferences(true, true);
                } else {
                    updateServicePreferences(true, false);
                }
            }else {updateServicePreferences(true,false);
            UpdateStickyNotification("Μη Συνδεδεμένο. Δεν υπάρχει Internet");}
            wlock.release();
        }
    }

    public class PubSubRequestReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case ConstantStrings.ACTIONS._PUBLISH:
                    if (!intent.hasExtra(ConstantStrings.EXTRAS._PUB_TOPIC)&&intent.hasExtra(ConstantStrings.EXTRAS._PUB_PAYLOAD)){return;}
                    PublishToTopic(intent.getStringExtra(ConstantStrings.EXTRAS._PUB_TOPIC) , intent.getStringExtra(ConstantStrings.EXTRAS._PUB_PAYLOAD));
                    break;
                case ConstantStrings.ACTIONS._SUBSCRIBE:
                    if (!intent.hasExtra(ConstantStrings.EXTRAS._SUB_TOPIC)) {return;}
                    SubscribeToTopic(intent.getStringExtra(ConstantStrings.EXTRAS._SUB_TOPIC));
                    if (IsOnline() && ConnectToBroker() && mqttclient!=null){return;}
                    ClientInitialization(BrokerAddress);
                    break;
                case ConstantStrings.ACTIONS._SERVICE_TERMINATE:
                    disconnect();
                    break;
            }
        }
    }

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
