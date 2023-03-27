package com.gvpit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.beyondar.android.fragment.BeyondarFragmentSupport;
import com.beyondar.android.util.ImageUtils;
import com.beyondar.android.view.OnClickBeyondarObjectListener;
import com.beyondar.android.world.BeyondarObject;
import com.beyondar.android.world.GeoObject;
import com.beyondar.android.world.World;
import com.google.android.gms.maps.model.LatLng;
import com.gvpit.R;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DirectionsActivity extends FragmentActivity implements OnClickBeyondarObjectListener, LocationListener {

    private static final String TAG = "StaticViewGeoObjectActi";
    private static final String TMP_IMAGE_PREFIX = "viewImage_";

    private BeyondarFragmentSupport mBeyondarFragment;
    private World mWorld;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cleanTempFolder();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.simple_camera);
        mBeyondarFragment = (BeyondarFragmentSupport) getSupportFragmentManager().findFragmentById(R.id.beyondarFragment);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mWorld = new World(this);
        mWorld.setDefaultImage(R.drawable.beyondar_default_unknow_icon);
        mWorld.setGeoPosition(17.820033, 83.343591);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA}, 1);
            return;
        }

        LocationRequest request = new LocationRequest.Builder(1000).setPriority(Priority.PRIORITY_HIGH_ACCURACY).setDurationMillis(5000).build();

        fusedLocationClient.requestLocationUpdates(request, this, Looper.getMainLooper());

        CurrentLocationRequest clr = new CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY).build();

        fusedLocationClient.getCurrentLocation(clr, null).addOnSuccessListener(this, (Location location) -> {
            if (location != null) {
                Log.d(TAG, "onSuccess: " + location.getLatitude() + ", " + location.getLongitude());
                mWorld.setLocation(location);
                setLocationUI(location);
            }
        });

//        // TODO: Remove
//        onLocationChanged(new Location("test"));


        mBeyondarFragment.setSensorDelay(300);
        mBeyondarFragment.setPushAwayDistance(50);
        mBeyondarFragment.setWorld(mWorld);
        mBeyondarFragment.setMaxDistanceToRender(100);
        mBeyondarFragment.setDistanceFactor(4);
        mBeyondarFragment.showFPS(true);


        try {

            LatLng start = new Gson().fromJson(getIntent().getStringExtra("start"), LatLng.class);
            LatLng end = new Gson().fromJson(getIntent().getStringExtra("end"), LatLng.class);
            String endName = getIntent().getStringExtra("endName");
            GeoObject endObject = new GeoObject(1000);
            endObject.setGeoPosition(end.latitude, end.longitude);
            endObject.setName(endName);

            String json = getIntent().getStringExtra("directions");
            JSONObject jsonObject = new JSONObject(json);
            Log.d(TAG, "onResume: " + jsonObject);

            DirectionsObject directionsObject = DirectionsObject.from(start, endObject, jsonObject);

            for (int i = 0; i < directionsObject.directionDotsGeoObjects.length; i++) {
                GeoObject geoObject = directionsObject.directionDotsGeoObjects[i];
                Log.d(TAG, "onResume: " + i + " : " + geoObject.getLatitude() + ", " + geoObject.getLongitude());
                mWorld.addBeyondarObject(geoObject);
            }

            mWorld.addBeyondarObject(directionsObject.end);

            updateEndObject(directionsObject.end);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(this);
    }

    private void updateEndObject(GeoObject endGeoObject) {
        String path = getTmpPath();

        View view = getLayoutInflater().inflate(R.layout.static_beyondar_object_view, null);
        TextView textView = (TextView) view.findViewById(R.id.geoObjectName);
        textView.setText(endGeoObject.getName());
        try {
            String imageName = TMP_IMAGE_PREFIX + endGeoObject.getName() + ".png";
            ImageUtils.storeView(view, path, imageName);
            endGeoObject.setImageUri(path + imageName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the path to store temporally the images. Remember that you need to
     * set WRITE_EXTERNAL_STORAGE permission in your manifest in order to
     * write/read the storage
     */
    private String getTmpPath() {
        return getExternalFilesDir(null).getAbsoluteFile() + "/tmp/";
    }

    /**
     * Clean all the generated files
     */
    private void cleanTempFolder() {
        File tmpFolder = new File(getTmpPath());
        if (tmpFolder.isDirectory()) {
            String[] children = tmpFolder.list();
            for (int i = 0; i < children.length; i++) {
                if (children[i].startsWith(TMP_IMAGE_PREFIX)) {
                    new File(tmpFolder, children[i]).delete();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        onResume();
    }


    @Override
    public void onClickBeyondarObject(ArrayList<BeyondarObject> beyondarObjects) {
        if (beyondarObjects.size() > 0) {
            Toast.makeText(this, "Clicked on: " + beyondarObjects.get(0).getName(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d(TAG, "onLocationChanged: " + location);
//        // TODO: Remove
//        Location location1 = new Location("gps");
//        location1.setLatitude(17.820954);
//        location1.setLongitude(83.341744);
//        location = location1;
        mWorld.setLocation(location);
        setLocationUI(location);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Intent intent = new Intent(this, DirectionsActivity.class);
        finish();
        startActivity(intent);
    }

    private void setLocationUI(Location location) {
        if (location == null) return;
        runOnUiThread(() -> {
            TextView tv = findViewById(R.id.textView);
            tv.setText("Lat: " + location.getLatitude() + " Long: " + location.getLongitude());
            Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show();
        });
    }


}
