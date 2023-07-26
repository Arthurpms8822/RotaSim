package com.example.rotasim;

import static java.lang.Math.round;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class LocationThread extends Thread {
    private final Context context;
    private final SpeedUpdateCallback speedUpdateCallback;
    private final FusedLocationProviderClient fusedLocationClient;
    private final LocationRequest locationRequest;
    private Location previousLocation;
    private final Vehicle vehicle;

    private boolean isTracking = false;

    public LocationThread(Context context, Vehicle vehicle, SpeedUpdateCallback speedUpdateCallback) {
        this.context = context;
        this.speedUpdateCallback = speedUpdateCallback;
        this.vehicle = vehicle;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        locationRequest = createLocationRequest();
    }

    /**
     * Interface de retorno de chamada para atualização de velocidade.
     */
    public interface SpeedUpdateCallback {
        void onSpeedUpdate(float speed, Location newLocation);
    }

    @Override
    public void run() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permissão não concedida
            return;
        }
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (isTracking && previousLocation != null) {
                        double distanceInMeters = location.distanceTo(previousLocation);
                        double speedT = location.getSpeed();
                        double currentSpeed = round(speedT);
                        float kmphSpeed = round((currentSpeed * 3.6));
                        vehicle.addSpeed(kmphSpeed);
                        vehicle.updateTotalDistance(distanceInMeters / 1000);
                        vehicle.setLocationAndSpeed(kmphSpeed, location);
                        speedUpdateCallback.onSpeedUpdate(kmphSpeed, location);

                        // Verifica se o veículo chegou ao destino
                        if (vehicle.hasReachedDestination(location)) {
                            stopTracking();
                            break;
                        }
                    }
                    previousLocation = location;
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /**
     * Inicia o rastreamento da localização.
     */
    public void startTracking() {
        isTracking = true;
        vehicle.startSimulation();
    }

    /**
     * Interrompe o rastreamento da localização.
     */
    public void stopTracking() {
        isTracking = false;
        if (vehicle != null) {
            vehicle.stopSimulation();
        }
    }

    /**
     * Cria uma solicitação de localização com configurações personalizadas.
     *
     * @return LocationRequest
     */
    protected LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }
}
