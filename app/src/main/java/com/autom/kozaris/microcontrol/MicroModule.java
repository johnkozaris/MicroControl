package com.autom.kozaris.microcontrol;

/**
 * MicroModule
 *
 * Αναππαριστά μια συσκευή που είναι συνδεδεμένη στον μεσίτη MQTT
 *
 * Αρχικοποίηση μέσω: {@link #MicroModule(int, String, String, String)}
 * @author Ioannis Kozaris
 */
public class MicroModule {
    //region PROPERTIES

    /**
     * To Interface {@link IConstants} χρησημοποιέιται για την αποθήκευση
     * σταθερών String.
     */
    public interface IConstants {
         interface Payloads {
            String COMMAND_RESET_ROM = "reset&rom";
        }

         interface Topics {
             String _LASTWIL="lastwill";
             String _DATA = "data";
             String SYNCHRONIZE="/android/synchronize";
        }

         interface ModuleTypes {
            String INPUT = "input";
            String OUTPUT = "output";
        }

         interface ControlTypes {
            String SWITCH = "switch";
            String ANALOG_DISPLAY = "analog";
            String MOTOR_CONRTOL = "motorcontrol";
            String LCD_TEXT = "lcdtext";
            String DIGITAL_DISPLAY = "digital";
        }
    }

    /**
     * @return Ονομα Συσκευής ως String
     */
    public String getName() {
        return Name;
    }
    private final String Name;

    /**
     * Το αναγνωρηστικό @moduleID εξασφαλίζει την μοναδικότητα κάθε συσκευής
     * @return Αναγνωρηστικό Συσκευής ως int
     */
    int getModuleID() {
        return moduleID;
    }
    private final int moduleID;

    /**
     * Enum moduleType αναπαριστά το είδος ελέγχου των συσκευών
     * Μια συσκευή είναι είτε ΕΙΣΟΔΟΣ είτε ΕΞΟΔΟΣ
     * @return Είδος ελεγχου ως INPUT ή OUTPUT
     */
    Type getModuleType() {
        return moduleType;
    }
    private final  Type moduleType;
    public enum Type{
        INPUT,
        OUTPUT;
        @Override
        public String toString() {
            switch (this)
            {
                case INPUT:return "input";
                case OUTPUT:return "output";
                default:return null;
            }
        }
    }

    /**
     * Enum ControlType αναπαριστά τον τρόπο ελεγχου των συσκευών.
     * Ο τρόπος ελέγχου εξαρτάται απο τα ηλεκτρονικά χαρακτηρηστικα
     * της συσκευής, αν ειναι διακόπτης,αναλογικός αισθητήρας, κτλπ.
     * Η μεταβλητή αυτή εκφράζει τον τρόπο που θα αναπαρασταθούν
     * τα εργαλεία ελεγχου της συσκευής
     * @return Τρόπος Ελεγχου ως SWITCH,LCD_TEXT,ANALOG,DIGITAL,MOTOR_CONTROL
     */
    ControlType getControlType() {
        return moduleControl;
    }
    private final ControlType moduleControl;
    public enum ControlType{
        SWITCH,
        LCD_TEXT,
        ANALOG_DISPLAY,
        DIGITAL_DISPLAY,
        MOTOR_CONRTOL
    }

    /** Αν η συσκευή είναι διακόπτης , η μεταβλητη switchActive
     * κρατάει την κατάσταση του διακόπτη
     * True= Κλειστός
     * False=Ανοιχτός
     */
    private boolean switchActive;
    boolean isSwitchActive() {
        return switchActive;
    }
    void setSwitchActive(boolean switchActive) {
        this.switchActive = switchActive;
    }

    /** Αν η συσκευή είναι οποιαδήποτε Εξοδος , η μεταβλητη Data
     * κρατάει τα δεδομένα της συσκευής εξόδου
     */
    private String Data;
    public String getData() {
        return Data;
    }
    public void setData(String data) {
        Data = data;
    }
    //endregion


    /**
     * Αρχικοποίηση μιας συσκευής.
     * @param stmoduleID Μοναδικό αναγνωρηστικό συσκευής
     * @param stmoduleType Τύπος συσκευής Είσοδος η έξοδος (INPUT,OUTPUT)
     * @param controlType Τρόπος Ελεγχου Συσκευής {@link #getControlType()}
     * @param stmoduleName Ονομα συσκευής
     */
    public MicroModule(int stmoduleID, String stmoduleType,String controlType, String stmoduleName ) {
        this.moduleID = stmoduleID;
        this.moduleType = ParseStrToType(stmoduleType);
        this.moduleControl= ParseStrToControlType(controlType);
        this.Name=stmoduleName;
    }

    // Οι παρακάτω μέθοδοι είναι βοηθητικοι και χρησιμοποιούντε για την μετατροπή
    //Strings σε Enums που χρησιμοποιούντε απο την κλάση MicroModule
    private Type ParseStrToType(String stype){
        switch (stype)
        {
            case IConstants.ModuleTypes.INPUT:return   Type.INPUT;
            case IConstants.ModuleTypes.OUTPUT:return Type.OUTPUT;
            default:return null;
        }
    }
    private ControlType ParseStrToControlType(String ctype){
        switch (ctype){
            case IConstants.ControlTypes.ANALOG_DISPLAY:return ControlType.ANALOG_DISPLAY;
            case IConstants.ControlTypes.DIGITAL_DISPLAY:return ControlType.DIGITAL_DISPLAY;
            case IConstants.ControlTypes.MOTOR_CONRTOL:return ControlType.MOTOR_CONRTOL;
            case IConstants.ControlTypes.LCD_TEXT:return ControlType.LCD_TEXT;
            case IConstants.ControlTypes.SWITCH:return ControlType.SWITCH;
            default:return null;
        }
    }

    /**
     * @return Επιστρέφει το Θέμα ρυθμίσεων (SettingsTopic) της συσκευής
     */
    String getSettingsTopic(){
        return ("/"+this.moduleType.toString()+"/"+this.moduleID+"/settings");
    }
    /**
     * @return Επιστρέφει το Θέμα δεδομένων (DataTopic) της συσκευής
     */
    String getDataTopic(){
        return ("/"+this.moduleType.toString()+"/"+this.moduleID+"/data");
    }
    /**
     * @return Επιστρέφει το Θέμα LastWill της συσκευής
     */
    String getWillTopic(){
        return ("/"+this.moduleType.toString()+"/"+this.moduleID+"/lastwill");
    }


}

