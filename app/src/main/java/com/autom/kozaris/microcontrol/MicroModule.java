package com.autom.kozaris.microcontrol;

public class MicroModule {
    //region PROPERTIES
    public interface IConstants {
         interface Payloads {
            String COMMAND_RESET_MCU = "reset&mcu";
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
    private final String Name;
    public String getName() {
        return Name;
    }

    private final int moduleID;
    int getModuleID() {
        return moduleID;
    }

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

    private final  Type moduleType;
    public Type getModuleType() {
        return moduleType;
    }

    public enum ControlType{
        SWITCH,
        LCD_TEXT,
        ANALOG_DISPLAY,
        DIGITAL_DISPLAY,
        MOTOR_CONRTOL
    }

    private final ControlType moduleControl;
    ControlType getControlType() {
        return moduleControl;
    }


    private boolean switchActive;
    boolean isSwitchActive() {
        return switchActive;
    }
    void setSwitchActive(boolean switchActive) {
        this.switchActive = switchActive;
    }

    private String Data;
    public String getData() {
        return Data;
    }
    public void setData(String data) {
        Data = data;
    }
    //endregion


    public MicroModule(int stmoduleID, String stmoduleType,String controlType, String stmoduleName ) {
        this.moduleID = stmoduleID;
        this.moduleType = ParseStrToType(stmoduleType);
        this.moduleControl= ParseStrToControlType(controlType);
        this.Name=stmoduleName;
    }

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

    String getSettingsTopic(){
        return ("/"+this.moduleType.toString()+"/"+this.moduleID+"/settings");
    }

    String getDataTopic(){
        return ("/"+this.moduleType.toString()+"/"+this.moduleID+"/data");
    }

    String getWillTopic(){
        return ("/"+this.moduleType.toString()+"/"+this.moduleID+"/lastwill");
    }


}

