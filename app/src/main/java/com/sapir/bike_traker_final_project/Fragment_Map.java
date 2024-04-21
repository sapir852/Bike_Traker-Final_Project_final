package com.sapir.bike_traker_final_project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;


public class Fragment_Map extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Polyline currentLocationPolyline;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private LocationCallback locationCallback;
    private boolean timerRunning = false; // Check if timer is running
    MainActivity mainActivity;
    private Handler handler;
    private Runnable updatePolylineRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.maps);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Properly initialize mainActivity
        mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            Log.d("sapirr", "mainActivity.isTimerOn " + mainActivity.isTimerOn());
            timerRunning = mainActivity.isTimerOn();
            Log.d("sapirr", "timerRun " + timerRunning);
            setTimerRunning(timerRunning);
        } else {
            Log.e("Fragment_Map", "MainActivity is null");
        }

        // Setup handler and runnable for updating polyline color
        handler = new Handler();
        updatePolylineRunnable = new Runnable() {
            @Override
            public void run() {
                // Check timer status and update polyline color
                if (mainActivity != null) {
                    timerRunning = mainActivity.isTimerOn();
                    setTimerRunning(timerRunning);
                    Log.d("sapirr", "Timer status checked. Timer running: " + timerRunning);
                }

                // Run this runnable again after 1 second
                handler.postDelayed(this, 1000);
            }
        };

        // Start the handler to run the runnable every second
        handler.postDelayed(updatePolylineRunnable, 1000);

        return view;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Enable location tracking on the map
        mMap.setMyLocationEnabled(true);

        // Create a polyline options with default settings
        PolylineOptions polylineOptions = new PolylineOptions()
                .width(10); // Set width of the polyline

        // Set polyline color based on timer status
        if (timerRunning) {
            polylineOptions.color(Color.RED); // Set color of the polyline when timer is running
        } else {
          //  currentLocationPolyline.remove();
          //  polylineOptions.color(Color.GRAY); // Set color of the polyline when timer is not running
        }

        // Create a list to store the polyline points
        List<LatLng> polylinePoints = new ArrayList<>();

        // Get the current device location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        // Create LatLng object for current location
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        // Add the current location as the first point in the polyline
                        polylinePoints.add(currentLatLng);

                        // Move camera to the current location
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));

                        // Add the polyline to the map
                        currentLocationPolyline = mMap.addPolyline(polylineOptions);
                        currentLocationPolyline.setPoints(polylinePoints);
                    }
                });

        // Create a location request to receive updates
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(5000) // 5 seconds
                .setFastestInterval(2000) // 2 seconds
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    // Update the polyline with the new location
                    LatLng newLatLng = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
                    polylinePoints.add(newLatLng);
                    currentLocationPolyline.setPoints(polylinePoints);
                }
            }
        };

        // Request location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, handle accordingly
                onMapReady(mMap);
            } else {
                // Permission denied
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop location updates when the fragment is destroyed
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // Stop the handler and remove the runnable
        if (handler != null && updatePolylineRunnable != null) {
            handler.removeCallbacks(updatePolylineRunnable);
        }
    }

    // Method to update polyline color based on timer status
    public void setTimerRunning(boolean timerRunning) {
        // Update the polyline color based on timer status
        if (currentLocationPolyline != null) {
            if (timerRunning) {
                currentLocationPolyline.setColor(Color.RED); // Set color of the polyline when timer is running
            } else {
                // currentLocationPolyline.setColor(Color.GRAY); // Set color of the polyline when timer is not running
            }
        }
    }
}