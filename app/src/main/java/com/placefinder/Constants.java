package com.placefinder;

public class Constants {
    public static class URL{
        private static final String HOST = "http://192.168.0.15:8080/";

        public static final String GET_PLACE_BY_ID(long id){
            return HOST + "places/" + id;
        }
        public static final String GET_ALL_PLACES = HOST + "places";
        public static final String GET_ALL_PLACES_AROUND(double latitude, double longitude){
            return HOST + "places/allAround?lat=" + String.valueOf(latitude) + "&lng=" + String.valueOf(longitude);
        }

        public static final String DELETE_PLACE(long id){
            return HOST + "places/" + id;
        }

        public static final String ADD_OR_UPDATE_PLACE = HOST + "places";
    }
}
