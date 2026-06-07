package com.example.mapapplication;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class GoogleMapActivity extends AppCompatActivity {

    private MapView map;
    private RequestQueue requestQueue;
    private final String showUrl = "http://10.0.2.2/map_project/getPosition.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialisation OSMDroid obligatoire avant setContentView
        Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE)
        );

        setContentView(R.layout.activity_google_map);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
        map.getController().setCenter(new GeoPoint(33.5892, -7.6031)); // Casablanca par défaut

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        loadPositions();
    }

    private void loadPositions() {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                showUrl,
                null,
                response -> {
                    try {
                        JSONArray positions = response.getJSONArray("positions");

                        for (int i = 0; i < positions.length(); i++) {
                            JSONObject pos = positions.getJSONObject(i);
                            double lat = pos.getDouble("latitude");
                            double lon = pos.getDouble("longitude");
                            String date = pos.getString("date");

                            Marker marker = new Marker(map);
                            marker.setPosition(new GeoPoint(lat, lon));
                            marker.setTitle("Position " + (i + 1));
                            marker.setSnippet(date);
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            map.getOverlays().add(marker);
                        }

                        map.invalidate();

                        Toast.makeText(getApplicationContext(),
                                positions.length() + " position(s) chargée(s)",
                                Toast.LENGTH_SHORT).show();

                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(),
                                "Erreur parsing JSON : " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getApplicationContext(),
                        "Erreur réseau : " + error.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        map.onDetach();
    }
}