/*
 * Copyright (C) 2014 BeyondAR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gvpit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import com.beyondar.android.world.BeyondarObjectList;
import com.beyondar.android.world.GeoObject;
import com.beyondar.android.world.World;
import com.beyondar.example.CustomWorldHelper;
import com.gvpit.R;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class MainActivity extends FragmentActivity implements OnClickBeyondarObjectListener, LocationListener {

    private static final String TAG = "StaticViewGeoObjectActi";
    private static final String TMP_IMAGE_PREFIX = "viewImage_";

    private BeyondarFragmentSupport mBeyondarFragment;
    private World mWorld;
    private FusedLocationProviderClient fusedLocationClient;

    private CustomWorldHelper.PlaceData[] mPlaces;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The first thing that we do is to remove all the generated temporal
        // images. Remember that the application needs external storage write
        // permission.
        cleanTempFolder();

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.simple_camera);
        mBeyondarFragment = (BeyondarFragmentSupport) getSupportFragmentManager().findFragmentById(R.id.beyondarFragment);


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mWorld = new World(this);

        mWorld = CustomWorldHelper.fromGvpGate(this);
        mPlaces = CustomWorldHelper.getPlaces(this);

    }

    void updateWordPlaceObjects(double lat, double lng) {
        ArrayList<CustomWorldHelper.PlaceData> eligiblePlaces = new ArrayList<>();

        new Thread(() -> {
            for (CustomWorldHelper.PlaceData place : mPlaces) {
                if (place.type.equals("block") ||
                        LocationCalc.haversine(place.latitude, place.longitude, lat, lng) * 1000 <= 50
                ) eligiblePlaces.add(place);
            }

            runOnUiThread(() -> {
                mWorld.clearWorld();
                for (CustomWorldHelper.PlaceData place : eligiblePlaces) {
                    GeoObject placeGO = new GeoObject(place.id);
                    placeGO.setGeoPosition(place.latitude, place.longitude);
                    placeGO.setImageResource(R.drawable.flag);
                    placeGO.setName(place.name);
                    mWorld.addBeyondarObject(placeGO);
                }
                replaceImagesByStaticViews(mWorld);
            });
        }).start();
    }


    @Override
    protected void onResume() {
        super.onResume();
//        mBeyondarFragment.onResume();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA}, 1);
            return;
        }

        // check and enable GPS
        //        if (!fusedLocationClient.getLocationAvailability().) {
        //            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        //            return;
        //        }

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

        mBeyondarFragment.setSensorDelay(300);
        mBeyondarFragment.setPushAwayDistance(50);

        mBeyondarFragment.setOnClickBeyondarObjectListener(this);


        // .. and send it to the fragment
        mBeyondarFragment.setWorld(mWorld);
        mBeyondarFragment.setMaxDistanceToRender(100);
        mBeyondarFragment.setDistanceFactor(4);

        // We also can see the Frames per seconds
        mBeyondarFragment.showFPS(true);
        updateWordPlaceObjects(0, 0);

//        // TODO: Remove
//        onLocationChanged(new Location("test"));
        // This method will replace all GeoObjects the images with a simple
        // static view
        replaceImagesByStaticViews(mWorld);

    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(this);
    }

    private void replaceImagesByStaticViews(World world) {
        String path = getTmpPath();

        for (BeyondarObjectList beyondarList : world.getBeyondarObjectLists()) {
            for (BeyondarObject beyondarObject : beyondarList) {
                // First let's get the view, inflate it and change some stuff
                View view = getLayoutInflater().inflate(R.layout.static_beyondar_object_view, null);
                TextView textView = (TextView) view.findViewById(R.id.geoObjectName);
                textView.setText(beyondarObject.getName());
                try {
                    // Now that we have it we need to store this view in the
                    // storage in order to allow the framework to load it when
                    // it will be need it
                    String imageName = TMP_IMAGE_PREFIX + beyondarObject.getName() + ".png";
                    ImageUtils.storeView(view, path, imageName);

                    // If there are no errors we can tell the object to use the
                    // view that we just stored
                    beyondarObject.setImageUri(path + imageName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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

    ProgressDialog progressDialog;

    @Override
    public void onClickBeyondarObject(ArrayList<BeyondarObject> beyondarObjects) {
        if (beyondarObjects.size() > 0) {
            GeoObject endBeyondarObject = (GeoObject) beyondarObjects.get(0);
            new AlertDialog.Builder(this, androidx.appcompat.R.style.AlertDialog_AppCompat_Light).setTitle("Navigate to " + endBeyondarObject.getName() + " ?").setMessage("Latitude: " + endBeyondarObject.getLatitude() + " Longitude: " + endBeyondarObject.getLongitude()).setPositiveButton("Yes", (dialog, a) -> {

                progressDialog = new ProgressDialog(MainActivity.this, androidx.appcompat.R.style.AlertDialog_AppCompat_Light);
                progressDialog.setTitle("Please wait while we're calculating the directions...");
                progressDialog.setMessage("Please wait while we calculate the directions to " + endBeyondarObject.getName());
                progressDialog.setCancelable(false);
                progressDialog.isIndeterminate();
                runOnUiThread(progressDialog::show);
                LatLng startLatLng = new LatLng(mWorld.getLatitude(), mWorld.getLongitude());
                Log.d(TAG, "onClickBeyondarObject: start: " + startLatLng);
                Log.d(TAG, "onClickBeyondarObject: end: " + endBeyondarObject.getLatitude() + ", " + endBeyondarObject.getLongitude());
                new Thread(() -> {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url("https://routing.openstreetmap.de/routed-bike/route/v1/driving/" + mWorld.getLongitude() + "," + mWorld.getLatitude() + ";" + endBeyondarObject.getLongitude() + "," + endBeyondarObject.getLatitude() + "?overview=false&alternatives=true&steps=true").build();
                    try {
                        String json = client.newCall(request).execute().body().string();
                        runOnUiThread(() -> {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                        });
                        Intent intent = new Intent(MainActivity.this, DirectionsActivity.class);
                        intent.putExtra("start", new Gson().toJson(startLatLng));
                        intent.putExtra("end", new Gson().toJson(new LatLng(endBeyondarObject.getLatitude(), endBeyondarObject.getLongitude())));
                        intent.putExtra("endName", endBeyondarObject.getName());
                        intent.putExtra("directions", json);
                        startActivity(intent);
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        throw new RuntimeException(e);
                    }
                }).start();
            }).setNegativeButton("No", (dialog, a) -> {
                dialog.dismiss();
            }).show();
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
        updateWordPlaceObjects(location.getLatitude(), location.getLongitude());
        setLocationUI(location);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Intent intent = new Intent(this, MainActivity.class);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
