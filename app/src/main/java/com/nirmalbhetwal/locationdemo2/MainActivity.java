package com.nirmalbhetwal.locationdemo2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final int REQUEST_CODE = 1;
    private final int UPDATE_INTERVAL = 5000;
    private final int FASTEST_UPDATE_INTERVAL = 3000;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private List<String> permissionsToRequest;
    private List<String> permissions = new ArrayList<>();
    private List<String> permissionsRejected = new ArrayList<>();

    TextView tvLocationData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionsToRequest = permissionsToRequest(permissions);
        tvLocationData = (TextView) findViewById(R.id.locationData);
        if (permissionsToRequest.size() > 0) {
            requestPermissions(permissionsToRequest.toArray(
                    new String[permissionsToRequest.size()]
                    ),
                    REQUEST_CODE);
        }
    }

    private List<String> permissionsToRequest(List<String> permissions) {
        ArrayList<String> results = new ArrayList<>();

        for (String permission : permissions) {
            if (!isGranted(permission)) {
                results.add(permission);
            }
        }

        return results;
    }

    private boolean isGranted(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // TODO: 2022-06-01 implement me
        if (requestCode == REQUEST_CODE) {
            for (String permission : permissions) {
                if (!isGranted(permission)) {
                    permissionsRejected.add(permission);
                }
            }

            if (permissionsRejected.size() > 0 ) {
                for (String rejectedPermission : permissionsRejected) {
                    if (shouldShowRequestPermissionRationale(rejectedPermission)) {
                        new AlertDialog.Builder(this)
                                .setMessage("Location access is mandatory")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        requestPermissions(new String[]{rejectedPermission}, REQUEST_CODE);
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .create()
                                .show();
                    }
                }
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            findLocation();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            Dialog errorDialog = GoogleApiAvailability.getInstance().getErrorDialog(this, REQUEST_CODE, REQUEST_CODE, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    Toast.makeText(MainActivity.this, "The service is not available", Toast.LENGTH_LONG);
                }
            });

            errorDialog.show();
        } else {
            findLocation();
        }
    }

    private void findLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location == null) {
                    return;
                }

                Log.d(TAG, "on Success " + location.getLatitude() + " " + location.getLongitude());
            }
        });

        startUpdatingLocation();
    }

    private void startUpdatingLocation() {
        // location request
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Location location = locationResult.getLastLocation();
                String address = "";
                try {
                    Geocoder geo = new Geocoder(MainActivity.this.getApplicationContext(), Locale.getDefault());
                    List<Address> addresses = geo.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (!addresses.isEmpty()) {
                        if (addresses.size() > 0) {
                            address = addresses.get(0).getFeatureName() + ", " + addresses.get(0).getLocality() +", " + addresses.get(0).getAdminArea() + ", " + addresses.get(0).getCountryName();
                            //Toast.makeText(getApplicationContext(), "Address:- " + addresses.get(0).getFeatureName() + addresses.get(0).getAdminArea() + addresses.get(0).getLocality(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace(); // getFromLocation() may sometimes fail
                }
                tvLocationData.setText(
                        String.format(
                                "Latitude: %s\nLongitude: %s\nAltitude: %s\nAccuracy: %s\nAddress: %s\n",
                                location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getAccuracy(), address)
                );
                Log.d(TAG, "on Success + Loc " + location.getLatitude() + " long: " + location.getLongitude());
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
        );
    }
}