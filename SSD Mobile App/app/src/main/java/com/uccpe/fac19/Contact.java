package com.uccpe.fac19;

public class Contact {
    private Double Lat;
    private Double Lng;
    private String location;
    private String contactUsername;
    private String date;
    private String msTime;

    public Contact() {
        // Empty Constructor needed
    }


    public Contact(Double Lat, Double Lng, String contactUsername, String date, String location, String msTime) {
        this.msTime = msTime;
        this.Lat = Lat;
        this.Lng = Lng;
        this.date = date;
        this.location = location;
        this.contactUsername = contactUsername;
    }

    public Double getLat() {
        return Lat;
    }

    public String getContactUsername() {
        return contactUsername;
    }

    public String getLocation() {
        return location;
    }

    public String getMsTime() {
        return msTime;
    }

    public Double getLng() {
        return Lng;
    }

    public String getDate() {
        return date;
    }
}
