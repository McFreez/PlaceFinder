package com.placefinder.network;

public class Constants {
    public static class URL{
        //private static final String HOST = "http://192.168.0.15:8085/"; // HOME
        private static final String HOST = "http://172.16.20.93:8085/"; // PWR

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

        public static final String GET_ALL_PLACE_PHOTOS(long id){
            return HOST + "photos?placeId=" + String.valueOf(id);
        }
        public static final String SAVE_PHOTO(long id){
            return HOST + "photos?placeId=" + String.valueOf(id);
        }
        public static final String DELETE_PHOTO(long id){
            return HOST + "photos/" + id;
        }

        public static final String GET_ALL_PLACE_COMMENTS(long id){
            return HOST + "comments?placeId=" + String.valueOf(id);
        }
        public static final String SAVE_COMMENT(long id){
            return HOST + "comments?placeId=" + String.valueOf(id);
        }
        public static final String DELETE_COMMENT(long id){
            return HOST + "comments/" + id;
        }
    }
}
