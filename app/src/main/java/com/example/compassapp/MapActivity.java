package com.example.compassapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.widget.Button;
import android.widget.Toast;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLocation;
    private LatLng currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Get current location from intent
        Intent intent = getIntent();
        double currentLat = intent.getDoubleExtra("current_latitude", 0);
        double currentLon = intent.getDoubleExtra("current_longitude", 0);
        currentLocation = new LatLng(currentLat, currentLon);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Button confirmButton = findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(v -> {
            if (selectedLocation != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", selectedLocation.latitude);
                resultIntent.putExtra("longitude", selectedLocation.longitude);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please select a destination", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        // Center map on current location if valid
        if (currentLocation.latitude != 0 && currentLocation.longitude != 0) {
            mMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
        } else {
            LatLng defaultLocation = new LatLng(47.4979, 19.0402);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f));
            Toast.makeText(this, "Current location unavailable", Toast.LENGTH_SHORT).show();
        }

        // Handle map clicks to select destination
        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            if (currentLocation.latitude != 0 && currentLocation.longitude != 0) {
                mMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));
            }
            mMap.addMarker(new MarkerOptions().position(latLng).title("Destination"));
            selectedLocation = latLng;
        });
    }
}