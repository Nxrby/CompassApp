package com.example.compassapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.text.MessageFormat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private float[] gravity;
    private float[] geomagnetic;
    private ImageView compassImageView;
    private ImageView destinationArrow;
    private TextView directionTV;
    private TextView latitudeTV, longitudeTV;
    private TextView headingTV, distanceTV;
    private float currentDegree = 0f;
    private ActivityResultLauncher<Intent> mapActivityResultLauncher;
    private CompassViewModel viewModel;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        compassImageView = findViewById(R.id.compassImageView);
        destinationArrow = findViewById(R.id.destinationArrow);
        directionTV = findViewById(R.id.directionTV);
        latitudeTV = findViewById(R.id.latitudeTV);
        longitudeTV = findViewById(R.id.longitudeTV);
        headingTV = findViewById(R.id.headingTV);
        distanceTV = findViewById(R.id.distanceTV);

        viewModel = new ViewModelProvider(this).get(CompassViewModel.class);

        mapActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        double latitude = data.getDoubleExtra("latitude", 0);
                        double longitude = data.getDoubleExtra("longitude", 0);
                        Location destination = new Location("destination");
                        destination.setLatitude(latitude);
                        destination.setLongitude(longitude);
                        viewModel.setDestinationLocation(destination);
                        updateDistanceAndBearing();
                    }
                }
        );

        Button selectDestinationButton = findViewById(R.id.selectDestinationButton);
        selectDestinationButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            if (currentLocation != null) {
                intent.putExtra("current_latitude", currentLocation.getLatitude());
                intent.putExtra("current_longitude", currentLocation.getLongitude());
            }
            mapActivityResultLauncher.launch(intent);
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                currentLocation = locationResult.getLastLocation();
                updateUI();
                updateDistanceAndBearing();
            }
        };

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            getLocation();
        }
    }

    private void getLocation() {
        LocationRequest locationRequest = new LocationRequest.Builder(10000).build();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void updateUI() {
        if (currentLocation != null) {
            latitudeTV.setText(MessageFormat.format("Lat: {0}", currentLocation.getLatitude()));
            longitudeTV.setText(MessageFormat.format("Long: {0}", currentLocation.getLongitude()));
        }
    }

    private void updateDistanceAndBearing() {
        if (currentLocation != null && viewModel.getDestinationLocation().getValue() != null) {
            float bearing = currentLocation.bearingTo(viewModel.getDestinationLocation().getValue());
            viewModel.setBearingToDestination(bearing);
            float distance = currentLocation.distanceTo(viewModel.getDestinationLocation().getValue());
            distanceTV.setText(MessageFormat.format("Distance: {0} meters", distance));
        } else {
            distanceTV.setText("Distance: --");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values;
        }

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuthInRadians = orientation[0];
                float azimuthInDegrees = (float) Math.toDegrees(azimuthInRadians);
                float degree = (azimuthInDegrees + 360) % 360;

                RotateAnimation ra = new RotateAnimation(currentDegree, -degree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                ra.setDuration(210);
                ra.setFillAfter(true);
                compassImageView.startAnimation(ra);
                currentDegree = -degree;

                headingTV.setText(MessageFormat.format("{0}°", Math.round(degree)));
                directionTV.setText(getDirection(Math.round(degree)));

                if (viewModel.getDestinationLocation().getValue() != null) {
                    float bearing = viewModel.getBearingToDestination().getValue();
                    float relativeAngle = (bearing - degree + 360) % 360;
                    destinationArrow.setRotation(relativeAngle);
                    destinationArrow.setVisibility(View.VISIBLE);
                } else {
                    destinationArrow.setVisibility(View.GONE);
                }
            }
        }
    }

    private String getDirection(int degree) {
        if (degree >= 337.5 || degree < 22.5) return "N";
        if (degree >= 22.5 && degree < 67.5) return "NE";
        if (degree >= 67.5 && degree < 112.5) return "E";
        if (degree >= 112.5 && degree < 157.5) return "SE";
        if (degree >= 157.5 && degree < 202.5) return "S";
        if (degree >= 202.5 && degree < 247.5) return "SW";
        if (degree >= 247.5 && degree < 292.5) return "W";
        if (degree >= 292.5 && degree < 337.5) return "NW";
        return "";
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        getLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}