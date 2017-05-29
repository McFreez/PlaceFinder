package com.placefinder.placedetailspanel;

import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.placefinder.MapActivity;
import com.placefinder.R;

public class BottomPanelBehaviour extends BottomSheetBehaviorGoogleMapsLike.BottomSheetCallback implements View.OnClickListener {

    private MapActivity activity;
    private TextView bottomSheetPeekTitle;
    private TextView bottomSheetPeekDescription;
    private RelativeLayout bottomSheetPeek;
    private FloatingActionButton locationFAB;
    private BottomSheetBehaviorGoogleMapsLike currentBottomSheetBehaviour;
    private FloatingSearchView floatingSearchView;

    private boolean isFloatingControlsShown = true;

    public BottomPanelBehaviour(MapActivity mapActivity, TextView bSPT, TextView bSPD, RelativeLayout bSP, FloatingActionButton lFAB, BottomSheetBehaviorGoogleMapsLike bSB, FloatingSearchView flSV){
        activity = mapActivity;
        bottomSheetPeekTitle = bSPT;
        bottomSheetPeekDescription = bSPD;
        bottomSheetPeek = bSP;
        locationFAB = lFAB;
        currentBottomSheetBehaviour = bSB;
        floatingSearchView = flSV;
    }

    @Override
    public void onStateChanged(@NonNull View bottomSheet, int newState) {
        if(newState == BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED){
            bottomSheet.requestLayout();
            setBottomSheetAppearanceCollapsed();
            if(!isFloatingControlsShown)
                showFloatingControls();
        }

        if(newState == BottomSheetBehaviorGoogleMapsLike.STATE_DRAGGING && isFloatingControlsShown){
            setBottomSheetAppearanceAnchorPoint();
            hideFloatingControls();
        }
    }

    @Override
    public void onSlide(@NonNull View bottomSheet, float slideOffset) {

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
            case R.id.bottom_sheet_peek:{
                if(currentBottomSheetBehaviour.getState() == BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED){
                    currentBottomSheetBehaviour.setState(BottomSheetBehaviorGoogleMapsLike.STATE_ANCHOR_POINT);
                    setBottomSheetAppearanceAnchorPoint();
                }
                else if(currentBottomSheetBehaviour.getState() == BottomSheetBehaviorGoogleMapsLike.STATE_ANCHOR_POINT){
                    currentBottomSheetBehaviour.setState(BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED);
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
                activity.chooseImage();
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

    private void setBottomSheetAppearanceAnchorPoint(){
        bottomSheetPeek.setBackgroundColor(activity.getResources().getColor(R.color.colorPrimary));
        bottomSheetPeekTitle.setTextColor(activity.getResources().getColor(R.color.colorAccent));
        bottomSheetPeekDescription.setTextColor(activity.getResources().getColor(R.color.colorAccent));

        hideFloatingControls();
    }

    private void setBottomSheetAppearanceCollapsed(){
        bottomSheetPeek.setBackgroundColor(activity.getResources().getColor(R.color.colorAccent));
        bottomSheetPeekTitle.setTextColor(Color.BLACK);
        bottomSheetPeekDescription.setTextColor(Color.GRAY);
    }

    private void showFloatingControls(){
        floatingSearchView.animate().alpha(1).y(0).setDuration(300).start();
        locationFAB.animate().scaleX(1).scaleY(1).setDuration(300).start();
        isFloatingControlsShown = true;
    }

    private void hideFloatingControls(){
        floatingSearchView.animate().alpha(0).y(-50).setDuration(300).start();
        locationFAB.animate().scaleX(0).scaleY(0).setDuration(300).start();
        isFloatingControlsShown = false;
    }
}
