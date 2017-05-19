package com.placefinder.network;


import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.placefinder.DTO.Place;
import com.placefinder.MapActivity;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class ServerRequests {

    public static class GetPlaceTask extends AsyncTask<Long, Void, ResponseEntity<Place>>{

        private MapActivity activity;
        private static final String TAG = "GetPlaceTask";

        public GetPlaceTask(MapActivity mapActivity){
            activity = mapActivity;
        }

        @Override
        protected ResponseEntity<Place> doInBackground(Long... longs) {
            RestTemplate template = new RestTemplate();
            template.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            ResponseEntity<Place> entity = null;
            try{
                entity = template.getForEntity(Constants.URL.GET_PLACE_BY_ID(longs[0]), Place.class);
            }
            catch (HttpClientErrorException|HttpServerErrorException e){
                Log.w(TAG, "No place with id " + longs[0]);
                entity = new ResponseEntity<>(e.getStatusCode());
            }
            return entity;
        }

        @Override
        protected void onPostExecute(ResponseEntity<Place> place) {
            activity.showToast(place);
        }
    }

    public static class PostPlaceTask extends AsyncTask<Place, Void, ResponseEntity<Place>>{

        private MapActivity activity;
        private static final String TAG = "PostPlaceTask";

        public PostPlaceTask(MapActivity mapActivity){
            activity = mapActivity;
        }

        @Override
        protected ResponseEntity<Place> doInBackground(Place... places) {
            RestTemplate template = new RestTemplate();
            template.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            ResponseEntity<Place> entity = null;
            try{
                entity = template.postForEntity(Constants.URL.ADD_OR_UPDATE_PLACE, places[0], Place.class);
            }
            catch (HttpClientErrorException|HttpServerErrorException e){
                Log.w(TAG, "Can`t add place " + places[0].getTitle());
                entity = new ResponseEntity<>(e.getStatusCode());
            }
            return entity;
        }

        @Override
        protected void onPostExecute(ResponseEntity<Place> place) {
            activity.onPlaceSaveFinished(place);
        }
    }

    public static class DeletePlaceTask extends AsyncTask<Long, Void, Boolean>{

        private static final String TAG = "DeletePlaceTask";
        private MapActivity activity;
        private long id;

        public DeletePlaceTask(MapActivity mapActivity){
            activity = mapActivity;
        }

        @Override
        protected Boolean doInBackground(Long... longs) {
            id = longs[0];
            RestTemplate template = new RestTemplate();
            template.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            //ResponseEntity<Place> entity = null;
            Boolean response;
            try{
                template.delete(Constants.URL.DELETE_PLACE(id));
                response = true;
            }
            catch (HttpClientErrorException|HttpServerErrorException e){
                Log.w(TAG, "Failed to delete place by id: " + id);
                response = false;
            }
            return response;
        }

        @Override
        protected void onPostExecute(Boolean response) {
            activity.onRemovePlaceFinished(response, id);
        }
    }

    public static class GetAllPlacesAroundTask extends AsyncTask<LatLng, Void, ResponseEntity<List<Place>>>{

        private static final String TAG = "GetAllPlacesAroundTask";
        private MapActivity activity;

        public GetAllPlacesAroundTask(MapActivity mapActivity){
            activity = mapActivity;
        }

        @Override
        protected ResponseEntity<List<Place>> doInBackground(LatLng... latLngs) {
            RestTemplate template = new RestTemplate();
            template.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
            ResponseEntity<List<Place>> entity = null;
            ResponseEntity<Place> entity1 = null;
            LatLng userLocation = latLngs[0];
            //Boolean response;
            try{

                ParameterizedTypeReference<List<Place>> parameterizedTypeReference = new ParameterizedTypeReference<List<Place>>(){};
                String url = Constants.URL.GET_ALL_PLACES_AROUND(userLocation.latitude, userLocation.longitude);
                //entity1 = template.getForEntity(url, Place.class);
                entity = template.exchange(Constants.URL.GET_ALL_PLACES_AROUND(userLocation.latitude, userLocation.longitude), HttpMethod.GET, null, parameterizedTypeReference);
                //response = true;
            }
            catch (HttpClientErrorException|HttpServerErrorException e){
                Log.w(TAG, "Failed to get places in location latitude: " + userLocation.latitude + " longitude: " + userLocation.longitude);
                entity = new ResponseEntity<>(e.getStatusCode());
                //response = false;
            }
            catch (Exception e){
                Log.w(TAG, "Failed to get places in location latitude: " + userLocation.latitude + " longitude: " + userLocation.longitude);
                entity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            return entity;
        }

        @Override
        protected void onPostExecute(ResponseEntity<List<Place>> responseEntity) {
            activity.onGetAllAroundPlacesFinished(responseEntity);
        }
    }
}
