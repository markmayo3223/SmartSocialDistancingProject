package com.uccpe.fac19;



public class Tracking {
    private Double Lat;
    private Double Lng;
    private String location;
    private String date;
    private String time;

    public Tracking() {
        // Empty constructor needed
    }

    public Tracking(Double Lat, Double Lng, String date, String location, String time) {
        this.time = time;
        this.Lat = Lat;
        this.location = location;
        this.Lng = Lng;
        this.date = date;
    }

    public Double getLat() {
        return Lat;
    }

    public Double getLng() {
        return Lng;
    }

    public String getLocation() {
        return location;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }
}
