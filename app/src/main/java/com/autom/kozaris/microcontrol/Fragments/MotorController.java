package com.autom.kozaris.microcontrol.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.autom.kozaris.microcontrol.ConstantStrings;
import com.autom.kozaris.microcontrol.R;

/**
 * MotorController
 * <p>
 * Dialog το οποίο περιέχει αντικείμενα ελέγχου μιας συσκευή κινητήρα
 * καλείτε απο την κλάση {@link .ModulesAdapter} όταν πατηθεί ένα κουμπι ModulesViewHolder.vControlMotorBtn
 * Μεσω του παραθλυρου αυτου γίνεται δεξιοστροφη η αριστεροστροφη κίνηση ενος κινητήρα και μπορει
 * να ρυθμιστει το βήμα του κινητήρα
 * <p>
 * Αρχικοποίηση μέσω: {@link new MotorController().show();}
 *
 * @author Ioannis Kozaris
 */
public class MotorController extends DialogFragment {
    public static final String ID_FOR_CONTROLL = "com.autom.kozaris.microcontrol.CTRL_MOTOR_ID";
    String MotorID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (!args.isEmpty()) {
            MotorID = args.getString(ID_FOR_CONTROLL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.motor_control_dialog, container, false);
        TextView headline = view.findViewById(R.id.textViewControlID);
        ImageView ArrowRight = view.findViewById(R.id.imageViewButton_motor_right);
        ArrowRight.setClickable(true);
        ImageView ArrowLeft = view.findViewById(R.id.imageViewButton_motor_left);
        ArrowLeft.setClickable(true);
        if (MotorID != null) {
            headline.setText(getString(R.string.motordialog_title, String.valueOf(MotorID)));
            //Δεξιόστροφη κίνηση κινητήρα
            ArrowRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent send = new Intent();
                    send.setAction(ConstantStrings.ACTIONS._PUBLISH);
                    send.putExtra(ConstantStrings.EXTRAS._PUB_PAYLOAD, "1");
                    send.putExtra(ConstantStrings.EXTRAS._PUB_TOPIC, "/input/" + MotorID + "/data");
                    getActivity().sendBroadcast(send);
                }
            });
            //Αριστεροστροφη κίνηση κινητήρα
            ArrowLeft.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent send = new Intent();
                    send.setAction(ConstantStrings.ACTIONS._PUBLISH);
                    send.putExtra(ConstantStrings.EXTRAS._PUB_PAYLOAD, "0");
                    send.putExtra(ConstantStrings.EXTRAS._PUB_TOPIC, "/input/" + MotorID + "/data");
                    getActivity().sendBroadcast(send);
                }
            });
        }
        return view;

    }
}
