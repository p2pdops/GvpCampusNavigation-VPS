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
package com.beyondar.example;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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
import com.beyondar.android.world.World;

public class StaticViewGeoObjectActivity extends FragmentActivity implements OnClickBeyondarObjectListener, LocationListener {

    private static final String TAG = "StaticViewGeoObjectActi";
    private static final String TMP_IMAGE_PREFIX = "viewImage_";

    private BeyondarFragmentSupport mBeyondarFragment;
    private World mWorld;
    private LocationManager lm;

    /**
     * Called when the activity is first created.
     */
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

        lm = getSystemService(LocationManager.class);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA}, 1);
            return;
        }

        // check and enable GPS
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }

        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 10.0F, (LocationListener) this);

        mBeyondarFragment = (BeyondarFragmentSupport) getSupportFragmentManager().findFragmentById(R.id.beyondarFragment);

        mBeyondarFragment.setMaxDistanceToRender(1000);

        mBeyondarFragment.setOnClickBeyondarObjectListener(this);

        // We create the world and fill it ...
        mWorld = CustomWorldHelper.fromGvpGate(this);

        Location lastKnownLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        mWorld.setLocation(lastKnownLocation);
        setLocationUI(lastKnownLocation);

        // .. and send it to the fragment
        mBeyondarFragment.setWorld(mWorld);

        // We also can see the Frames per seconds
        mBeyondarFragment.showFPS(true);

        // This method will replace all GeoObjects the images with a simple
        // static view
        replaceImagesByStaticViews(mWorld);
    }

    @Override
    protected void onPause() {
        super.onPause();
        lm.removeUpdates(this);
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
    public void onClickBeyondarObject(ArrayList<BeyondarObject> beyondarObjects) {
        if (beyondarObjects.size() > 0) {
            Toast.makeText(this, "Clicked on: " + beyondarObjects.get(0).getName(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d(TAG, "onLocationChanged: " + location);
        mWorld.setGeoPosition(location.getLatitude(), location.getLongitude());
        setLocationUI(location);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Intent intent = new Intent(this, StaticViewGeoObjectActivity.class);
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
