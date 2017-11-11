package com.autom.kozaris.microcontrol.Fragments;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.autom.kozaris.microcontrol.ConstantStrings;
import com.autom.kozaris.microcontrol.R;

public class MotorController extends DialogFragment{
    public static final String ID_FOR_CONTROLL="com.autom.kozaris.microcontrol.CTRL_MOTOR_ID";
    String MotorID;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args= getArguments();
        if (!args.isEmpty()) {
            MotorID = args.getString(ID_FOR_CONTROLL);
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.motor_control_dialog, container, false);
        TextView headline =view.findViewById(R.id.textViewControlID);
        ImageView ArrowRight=view.findViewById(R.id.imageViewButton_motor_right);
        ImageView ArrowLeft=view.findViewById(R.id.imageViewButton_motor_left);
        if (MotorID!=null) {
            headline.setText(getString(R.string.motordialog_title,String.valueOf(MotorID)));

            ArrowRight.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    Intent send = new Intent();
                    send.setAction(ConstantStrings.ACTIONS._PUBLISH);
                    switch (motionEvent.getAction())
                    {
                        case MotionEvent.ACTION_DOWN:
                            send.putExtra(ConstantStrings.EXTRAS._PUB_PAYLOAD, "1");
                            send.putExtra(ConstantStrings.EXTRAS._PUB_TOPIC, "/input/"+MotorID+"/data");
                            break;
                        case MotionEvent.ACTION_UP:

                            break;
                    }
                    getActivity().sendBroadcast(send);
                    return false;
                }
            });
            ArrowLeft.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    Intent send = new Intent();
                    send.setAction(ConstantStrings.ACTIONS._PUBLISH);
                    switch (motionEvent.getAction())
                    {
                        case MotionEvent.ACTION_DOWN:
                            send.putExtra(ConstantStrings.EXTRAS._PUB_PAYLOAD, "0");
                            send.putExtra(ConstantStrings.EXTRAS._PUB_TOPIC, "/input/"+MotorID+"/data");
                            break;
                        case MotionEvent.ACTION_UP:
                            break;
                    }
                    getActivity().sendBroadcast(send);
                    return false;
                }
            });
        }
        return view;

    }
}
