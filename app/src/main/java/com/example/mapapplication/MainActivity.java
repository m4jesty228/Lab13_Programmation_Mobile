package com.example.mapapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private double latitude, longitude, altitude;
    private float accuracy;

    private RequestQueue requestQueue;
    private LocationManager locationManager;

    private final String insertUrl = "http://10.0.2.2/map_project/createPosition.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Button btnMap = findViewById(R.id.btnMap);
        btnMap.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, GoogleMapActivity.class))
        );

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE
                    }, PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                60000,
                150,
                new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        latitude  = location.getLatitude();
                        longitude = location.getLongitude();
                        altitude  = location.getAltitude();
                        accuracy  = location.getAccuracy();

                        String msg = String.format(
                                getResources().getString(R.string.new_location),
                                latitude, longitude, altitude, accuracy
                        );

                        addPosition(latitude, longitude);
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {
                        Toast.makeText(getApplicationContext(),
                                String.format(getString(R.string.provider_enabled), provider),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                        Toast.makeText(getApplicationContext(),
                                String.format(getString(R.string.provider_disabled), provider),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}
                }
        );
    }

    private void addPosition(final double lat, final double lon) {
        StringRequest request = new StringRequest(
                Request.Method.POST,
                insertUrl,
                response -> {},
                error -> Toast.makeText(getApplicationContext(),
                        "Erreur réseau : " + error.getMessage(),
                        Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                params.put("latitude",  String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("date",      sdf.format(new Date()));
                params.put("imei",      Settings.Secure.getString(
                        getContentResolver(), Settings.Secure.ANDROID_ID));
                return params;
            }
        };
        requestQueue.add(request);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this,
                        "Permission refusée. L'application ne peut pas fonctionner.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(v -> {});
    }
}