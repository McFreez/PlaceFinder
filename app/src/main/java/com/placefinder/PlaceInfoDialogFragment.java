package com.placefinder;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.placefinder.DTO.Place;

public class PlaceInfoDialogFragment extends DialogFragment {

    public static final String TAG = "PlaceInfo";

    public String fullAddress;
    public double latitude;
    public double longitude;
    public String creatorUid;

    private TextView placeNameTextView;
    private TextView placeDescriptionTextView;
    private TextView placeAddressTextView;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_place_info, null))
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                        MapActivity activity = (MapActivity) getActivity();

                        Place place = new Place();
                        place.setTitle(placeNameTextView.getText().toString());
                        place.setDescription(placeDescriptionTextView.getText().toString());
                        place.setLatitude(latitude);
                        place.setLongitude(longitude);
                        place.setOwnerGoogleId(creatorUid);

                        activity.addPlaceDialogResult(place);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        //final Dialog dialog = builder.create();
        //dialog.getWindow().setBackgroundDrawableResource(R.color.colorPrimary);

        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        placeNameTextView = (TextView) getDialog().findViewById(R.id.place_title);
        placeDescriptionTextView = (TextView) getDialog().findViewById(R.id.place_description);
        placeAddressTextView = (TextView) getDialog().findViewById(R.id.place_address);
        placeAddressTextView.setText(fullAddress);

        ((AlertDialog)getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorPrimary));
        ((AlertDialog)getDialog()).getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary));

    }
}
