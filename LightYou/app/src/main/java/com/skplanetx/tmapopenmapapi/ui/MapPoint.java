package com.skplanetx.tmapopenmapapi.ui;

public class MapPoint {
    private String Name;
    private Double latitude;
    private Double longtitude;

    public MapPoint(){
        super();
    }

    public MapPoint(String Name, double latitude, double longtitude)
    {
        this.Name = Name;
        this.latitude = latitude;
        this.longtitude = longtitude;
    }

    public String getName(){
        return Name;
    }

    public void setName(String Name){
        this.Name = Name;
    }

    public double getLatitude(){
        return latitude;
    }

    public void setLatitude(){
        this.latitude = latitude;
    }

    public double getLongtitude(){
        return longtitude;
    }

    public void setLongtitude(){
        this.longtitude = longtitude;
    }
}