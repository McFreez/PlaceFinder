package com.placefinder;

import android.os.AsyncTask;

import com.placefinder.DTO.Place;
import com.placefinder.xslt.NetworkUtils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class TestRequestToServer {

    public static class GetOnePlace extends AsyncTask<Integer, Void, String>{

        @Override
        protected String doInBackground(Integer... integers) {

            RestTemplate template = new RestTemplate();
            template.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

            try {
                ResponseEntity<Place> entity = template.getForEntity("http://192.168.0.15:8080/places/3", Place.class);

            }catch (HttpClientErrorException e){
                e.printStackTrace();
                HttpStatus status =  e.getStatusCode();
                int i = status.value();
            }
            String uri = "http://10.0.2.2:8080/places/3";
            URL url = null;
            try {
                url = new URL(uri);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            String result = null;

            try {
                //HttpGet
                result = NetworkUtils.getResponseFromHttpUrl(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }
}
