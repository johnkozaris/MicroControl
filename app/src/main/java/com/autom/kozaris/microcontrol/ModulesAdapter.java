package com.autom.kozaris.microcontrol;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.autom.kozaris.microcontrol.Fragments.MotorController;

import java.util.List;

public class ModulesAdapter extends RecyclerView.Adapter<ModulesViewHolder> {

    private List<MicroModule> AdapterModulelist;

    private Context mContext;
    public ModulesAdapter(Context context, List<MicroModule> moduleList) {
        this.AdapterModulelist = moduleList;
        mContext=context;
    }

    @Override
    public ModulesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemview = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview, parent, false);
        return new ModulesViewHolder(itemview,mContext);

    }

    @Override
    public void onBindViewHolder(final ModulesViewHolder holder, int position) {
        final MicroModule mmd = AdapterModulelist.get(position);
        holder.vId.setText(mContext.getString(R.string.cardview_title_id_prop, mmd.getModuleID()));
        ConfigureFab(holder,mmd);
        if (mmd.getName() != null) {
            holder.vName.setText(mmd.getName());
            //   holder.vStatus.setLcdText("Συνδεδεμενο");
            //  holder.vStatus.setTextColor(Color.GREEN);
        } else {
            holder.vName.setText("Αγνωστο");
            //  holder.vStatus.setLcdText("Συνδεδεμενο");
            //   holder.vStatus.setTextColor(Color.GREEN);
        }

        switch (mmd.getControlType()) {
            case SWITCH:
                holder.vSwitch1.setVisibility(View.VISIBLE);
                holder.vSwitch1.setEnabled(true);
                holder.vSwitch1.setClickable(false);
                holder.vSwitch1.setChecked(mmd.isSwitchActive());
                holder.vSwitch1.setClickable(true);
                break;
            case MOTOR_CONRTOL:
                holder.vControlMotorBtn.setVisibility(View.VISIBLE);
                holder.vControlMotorBtn.setEnabled(true);
                holder.vControlMotorBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        android.support.v4.app.DialogFragment ctrFrag = new MotorController();
                        Bundle args = new Bundle();
                        args.putString(MotorController.ID_FOR_CONTROLL, String.valueOf(mmd.getModuleID()));
                        ctrFrag.setArguments(args);
                        if (mContext instanceof MainActivity) {
                            ctrFrag.show(((MainActivity) mContext).getSupportFragmentManager(), "control");
                        }
                    }
                });
                break;
            case ANALOG_DISPLAY:
                EnableOutputControls(holder);
                if (mmd.getData() == null || mmd.getData().isEmpty()) {
                    holder.vTextData.setText("Χωρις Δεδομένα");
                    break;
                }
                holder.vTextData.setText(mmd.getData());
                holder.vProgressBarOutput.setMax(1024);
                try {
                    holder.vProgressBarOutput.setProgress(Integer.parseInt(mmd.getData()));
                    if (Integer.parseInt(mmd.getData()) >= 500) {

                        holder.vProgressBarOutput.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                    } else {
                        holder.vProgressBarOutput.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                    }
                } catch (NumberFormatException e) {
                    Log.e("Number Format", "onBindViewHolder: ");
                }
                break;
            case DIGITAL_DISPLAY:
                EnableOutputControls(holder);
                holder.vProgressBarOutput.setMax(100);
                holder.vProgressBarOutput.setProgress(100);
                try {
                    if (Integer.parseInt(mmd.getData()) == 1) {
                        holder.vProgressBarOutput.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                        holder.vTextData.setText("Ενεργό");
                        holder.vTextData.setTextColor(Color.RED);
                    } else {
                        holder.vProgressBarOutput.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                        holder.vTextData.setText("Ανενεργό");
                        holder.vTextData.setTextColor(Color.GREEN);
                    }
                } catch (NumberFormatException e) {
                    Log.e("Number Format", "onBindViewHolder: ");
                }
                break;
            case LCD_TEXT:
                holder.lcdEditText.setVisibility(View.VISIBLE);
                holder.lcdButton.setVisibility(View.VISIBLE);
                holder.lcdButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if ( holder.lcdEditText.getText()== null || holder.lcdEditText.getText().toString().isEmpty()){return;}
                        String lcdPayload= holder.lcdEditText.getText().toString();
                        if (lcdPayload.contains("\"") || lcdPayload.contains("\\")){ holder.lcdEditText.setError("Cannot contain special characters"); return;}
                        Intent send = new Intent();
                        send.setAction(ConstantStrings.ACTIONS._PUBLISH);
                        send.putExtra(ConstantStrings.EXTRAS._PUB_PAYLOAD, lcdPayload);
                        send.putExtra(ConstantStrings.EXTRAS._PUB_TOPIC, mmd.getDataTopic());
                        holder.lcdEditText.setText("");
                        holder.lcdEditText.setEnabled(false);
                        holder.lcdEditText.setEnabled(true);
                        (mContext).sendBroadcast(send);
                    }
                });
                break;
        }
    }
    @Override
    public int getItemCount() {
        return AdapterModulelist.size();
    }

    private void EnableOutputControls(ModulesViewHolder mholder){
        mholder.vTextLabelData.setVisibility(View.VISIBLE);
        mholder.vTextLabelData.setEnabled(true);
        mholder.vTextData.setEnabled(true);
        mholder.vTextData.setVisibility(View.VISIBLE);
        mholder.vProgressBarOutput.setEnabled(true);
        mholder.vProgressBarOutput.setVisibility(View.VISIBLE);
    }

    private void ConfigureFab(ModulesViewHolder mholder,final MicroModule module){
        if (mholder.fabSettings != null) {
            mholder.fabSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
                    alertDialog.setTitle(mContext.getText(R.string.fab_sett_not_dialog_title));
                    alertDialog.setMessage(mContext.getText(R.string.fab_sett_not_dialog_message));
                    alertDialog.setPositiveButton("OK",

                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent send = new Intent();
                                    send.setAction(ConstantStrings.ACTIONS._PUBLISH);
                                    send.putExtra(ConstantStrings.EXTRAS._PUB_PAYLOAD, MicroModule.IConstants.Payloads.COMMAND_RESET_ROM);
                                    send.putExtra(ConstantStrings.EXTRAS._PUB_TOPIC, module.getSettingsTopic());
                                    (mContext).sendBroadcast(send);
                                    switch (module.getModuleType()){
                                        case INPUT:
                                            ((MainActivity) mContext).inputsFragment.InputModuleList.remove(module);
                                            break;
                                        case OUTPUT:
                                            ((MainActivity) mContext).outputsfragment.OutputModuleList.remove(module);
                                            break;
                                    }
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.setNegativeButton("Ακυρο", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.dismiss();
                        }
                    });
                    alertDialog.show();
                }
            });
        }
    }
}