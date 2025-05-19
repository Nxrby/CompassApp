package com.example.compassapp;

import android.location.Location;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CompassViewModel extends ViewModel {
    private final MutableLiveData<Location> destinationLocation = new MutableLiveData<>();
    private final MutableLiveData<Float> bearingToDestination = new MutableLiveData<>();

    public LiveData<Location> getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(Location location) {
        destinationLocation.setValue(location);
    }

    public LiveData<Float> getBearingToDestination() {
        return bearingToDestination;
    }

    public void setBearingToDestination(float bearing) {
        bearingToDestination.setValue(bearing);
    }
}