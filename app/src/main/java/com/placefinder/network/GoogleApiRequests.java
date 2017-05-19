package com.placefinder.network;


import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import com.google.maps.errors.ApiException;
import com.google.maps.model.LatLng;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;
import com.placefinder.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GoogleApiRequests {

    public static class DirectionTask extends AsyncTask<com.google.android.gms.maps.model.LatLng, Void, DirectionsResult> {

        private MapActivity activity;
        private String apiKey;
        private GeoApiContext context;
        private static final String TAG = "DirectionTask";

        public DirectionTask(MapActivity mapActivity){
            activity = mapActivity;
            apiKey = activity.getString(R.string.google_maps_key);
            context = new GeoApiContext()
                    .setApiKey(apiKey)
                    .setQueryRateLimit(500, 0)
                    .setQueryRateLimit(3)
                    .setConnectTimeout(1, TimeUnit.SECONDS)
                    .setReadTimeout(1, TimeUnit.SECONDS)
                    .setWriteTimeout(1, TimeUnit.SECONDS);
        }

        @Override
        protected DirectionsResult doInBackground(com.google.android.gms.maps.model.LatLng... latLngs) {
            LatLng origin = new LatLng(latLngs[0].latitude, latLngs[0].longitude);
            LatLng destination = new LatLng(latLngs[1].latitude, latLngs[1].longitude);
            DirectionsResult result = null;
            try {
                result = DirectionsApi.newRequest(context)
                        .origin(origin)
                        .destination(destination)
                        .mode(TravelMode.DRIVING)
                        .await();
            } catch (ApiException|InterruptedException|IOException e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
            catch (Exception e){
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(DirectionsResult result) {
            //activity.onPlaceSaveFinished(place);
            activity.onRouteBuildingFinished(result);
        }
    }
}
