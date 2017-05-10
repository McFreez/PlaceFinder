package com.placefinder.xslt;


public class GeocodingResult {

    private String Adress;
    private double Latitude;
    private double Longitude;

    public GeocodingResult(String adress, double latitude, double longitude){
        Adress = adress;
        Latitude = latitude;
        Longitude = longitude;
    }

    public String getAdress(){
        return Adress;
    }

    public void setAdress(String adress){
        Adress = adress;
    }

    public double getLatitude(){
        return Latitude;
    }

    public void setLatitude(double latitude){
        Latitude = latitude;
    }

    public double getLongitude(){
        return Longitude;
    }

    public void setLongitude(double longitude){
        Longitude = longitude;
    }
}
