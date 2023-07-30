package com.example.rotasim;

import com.google.android.gms.maps.model.LatLng;

public class CrossDockingVehicle {
    public String vehicleId;
    public double distanceToArive;
    public LatLng location;
    public LatLng endPoint;

    public CrossDockingVehicle(String vehicleId, double distanceToArive, LatLng location, LatLng endPoint){
        this.vehicleId = vehicleId;
        this.distanceToArive = distanceToArive;
        this.location = location;
        this.endPoint = endPoint;
    }
}
