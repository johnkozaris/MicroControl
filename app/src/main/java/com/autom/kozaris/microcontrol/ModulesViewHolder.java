package com.autom.kozaris.microcontrol;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;


class ModulesViewHolder extends RecyclerView.ViewHolder implements CompoundButton.OnCheckedChangeListener {
    TextView vName;
     TextView vId;
    Switch vSwitch1;
     TextView vTextData;
     TextView vTextLabelData;
     Button vControlMotorBtn;
     ProgressBar vProgressBarOutput;
     FloatingActionButton fabSettings;
    EditText lcdEditText;
    Button lcdButton;

    private Context mContext;
    ModulesViewHolder(View itemView, Context baseContext) {
        super(itemView);
        vId =itemView.findViewById(R.id.textView);
        vName =itemView.findViewById(R.id.txtviewNameVar);
        vSwitch1 =itemView.findViewById(R.id.switch1);
        vTextData=itemView.findViewById(R.id.textViewOutputData);
        vTextLabelData=itemView.findViewById(R.id.textViewoutputlabel);
        vControlMotorBtn=itemView.findViewById(R.id.buttonControlMotor);
        vProgressBarOutput=itemView.findViewById(R.id.progressBarData);
        vSwitch1.setOnCheckedChangeListener(this);
        fabSettings=itemView.findViewById(R.id.floatingActionSetupSignal);
        lcdEditText = itemView.findViewById(R.id.editTextLcdText);
        lcdButton=itemView.findViewById(R.id.buttonSendtoLcd);
        mContext=baseContext;

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean ischecked) {

        Intent send = new Intent();
        send.setAction(ConstantStrings.ACTIONS._PUBLISH);
        if (compoundButton.getId() == vSwitch1.getId()) {
            if (compoundButton.isPressed()) {
                MicroModule mmp =((MainActivity)mContext).inputsFragment.InputModuleList.get(getAdapterPosition());
                if (compoundButton.isChecked()) {
                    mmp.setSwitchActive(true);
                    send.putExtra(ConstantStrings.EXTRAS._PUB_PAYLOAD,"1");
                    send.putExtra(ConstantStrings.EXTRAS._PUB_TOPIC,mmp.getDataTopic());

                } else {
                    mmp.setSwitchActive(false);
                    send.putExtra(ConstantStrings.EXTRAS._PUB_PAYLOAD,"0");
                    send.putExtra(ConstantStrings.EXTRAS._PUB_TOPIC,mmp.getDataTopic());
                }
                mContext.sendBroadcast(send);
            }
        }
    }
}

