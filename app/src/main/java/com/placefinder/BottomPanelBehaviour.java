package com.placefinder;

import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class BottomPanelBehaviour extends BottomSheetBehavior.BottomSheetCallback implements View.OnClickListener {

    private MapActivity activity;
    private TextView bottomSheetPeekTitle;
    private TextView bottomSheetPeekDescription;
    private RelativeLayout bottomSheetPeek;
    private FloatingActionButton locationFAB;
    private BottomSheetBehavior currentBottomSheetBehaviour;

    private boolean changesApplied = true;
    private int lastState = BottomSheetBehavior.STATE_COLLAPSED;
    private boolean locationFABHidden = false;
    private boolean isSlidingTop = true;

    public BottomPanelBehaviour(MapActivity mapActivity, TextView bSPT, TextView bSPD, RelativeLayout bSP, FloatingActionButton lFAB, BottomSheetBehavior bSB){
        activity = mapActivity;
        bottomSheetPeekTitle = bSPT;
        bottomSheetPeekDescription = bSPD;
        bottomSheetPeek = bSP;
        locationFAB = lFAB;
        currentBottomSheetBehaviour = bSB;
    }

    @Override
    public void onStateChanged(@NonNull View bottomSheet, int newState) {
        if(locationFABHidden && (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN)){
            locationFAB.animate().scaleX(1).scaleY(1).setDuration(300).start();
            locationFABHidden = false;
        }

        if(newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_EXPANDED || newState == BottomSheetBehavior.STATE_HIDDEN){
            lastState = newState;
            if(newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN){
                setBottomSheetAppearanceCollapsed();
            }

        }
        else if(newState == BottomSheetBehavior.STATE_DRAGGING){
            changesApplied = false;
        }
    }

    @Override
    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        if(!isSlidingTop && slideOffset > 0)
            changesApplied = false;
        if(!changesApplied) {
            if(lastState ==  BottomSheetBehavior.STATE_COLLAPSED){
                if(slideOffset > 0) {
                    setBottomSheetAppearanceExpanded();
                }
                else
                    isSlidingTop = false;
                changesApplied = true;
            }
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
            case R.id.bottom_sheet_peek:{
                if(currentBottomSheetBehaviour.getState() == BottomSheetBehavior.STATE_COLLAPSED){
                    currentBottomSheetBehaviour.setState(BottomSheetBehavior.STATE_EXPANDED);
                    setBottomSheetAppearanceExpanded();
                }
                else if(currentBottomSheetBehaviour.getState() == BottomSheetBehavior.STATE_EXPANDED){
                    currentBottomSheetBehaviour.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    setBottomSheetAppearanceCollapsed();
                }
            } break;
            case R.id.button_delete_place:{
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.AlertDialogCustom));
                builder.setMessage("Are you sure?")
                        .setCancelable(true)
                        .setNegativeButton("No",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                })
                        .setPositiveButton("Yes",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int i) {
                                        dialog.cancel();
                                        activity.tryToRemovePlace();
                                    }
                                });
                AlertDialog alert = builder.create();
                alert.show();
            }
            break;
            case R.id.button_add_place_image:{

            }
            break;
            case R.id.button_build_route:{
                activity.buildRouteToCurrentPlace();
            }
            break;
            case R.id.button_clear_route:{
                activity.clearCurrentRoute();
            }
            break;
        }

    }

    private void setBottomSheetAppearanceExpanded(){
        bottomSheetPeek.setBackgroundColor(activity.getResources().getColor(R.color.colorPrimary));
        bottomSheetPeekTitle.setTextColor(activity.getResources().getColor(R.color.colorAccent));
        bottomSheetPeekDescription.setTextColor(activity.getResources().getColor(R.color.colorAccent));

        locationFAB.animate().scaleX(0).scaleY(0).setDuration(300).start();
        locationFABHidden = true;
        isSlidingTop = true;
    }

    private void setBottomSheetAppearanceCollapsed(){
        bottomSheetPeek.setBackgroundColor(activity.getResources().getColor(R.color.colorAccent));
        bottomSheetPeekTitle.setTextColor(Color.BLACK);
        bottomSheetPeekDescription.setTextColor(Color.GRAY);
    }
}
