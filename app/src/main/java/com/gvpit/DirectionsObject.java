package com.gvpit;

import android.util.Log;

import androidx.annotation.DrawableRes;

import com.beyondar.android.world.GeoObject;
import com.google.android.gms.maps.model.LatLng;
import com.gvpit.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class DirectionsObject {
    private static final String TAG = "DirectionsObject";

    public double distance;
    public double duration;

    public GeoObject start;

    public GeoObject end;

    public StepManeuver[] steps;

    public GeoObject[] directionDotsGeoObjects;

    public DirectionsObject(double distance, double duration, GeoObject end, StepManeuver[] steps, GeoObject[] directionDotsGeoObjects) {
        this.distance = distance;
        this.duration = duration;
        this.end = end;
        this.steps = steps;
        this.directionDotsGeoObjects = directionDotsGeoObjects;
    }

    public static DirectionsObject from(LatLng startLatLng, GeoObject endGeoObject, JSONObject jsonObject) throws JSONException {
        JSONArray routes = jsonObject.getJSONArray("routes");
        JSONObject route = routes.getJSONObject(0);
        JSONArray legs = route.getJSONArray("legs");
        JSONObject leg = legs.getJSONObject(0);
        double distance = route.getDouble("distance");
        double duration = route.getDouble("duration");
        JSONArray steps = leg.getJSONArray("steps");
        StepManeuver[] stepManeuvers = new StepManeuver[steps.length()];
        for (int i = 0; i < steps.length(); i++) {
            JSONObject step = steps.getJSONObject(i);
            JSONObject stepManeuver = step.getJSONObject("maneuver");
            String type = stepManeuver.getString("type");
            String modifier = stepManeuver.optString("modifier", "");
            JSONArray location = stepManeuver.getJSONArray("location");
            double lat = location.getDouble(1);
            double lng = location.getDouble(0);
            String stepDuration = step.getString("duration");
            String stepDistance = step.getString("distance");
            stepManeuvers[i] = new StepManeuver(type, modifier, lat, lng, stepDuration, stepDistance);
        }
        GeoObject[] directionDotsGeoObjects = calculateDirectionsDots(startLatLng.latitude, startLatLng.longitude, stepManeuvers);
        return new DirectionsObject(distance, duration, endGeoObject, stepManeuvers, directionDotsGeoObjects);
    }

    public static GeoObject[] calculateDirectionsDots(double currLat, double currLng, StepManeuver[] steps) {
        ArrayList<GeoObject> directionDotsGeoObjects = new ArrayList<>();
        for (int i = 0; i < steps.length; i++) {
            StepManeuver maneuver = steps[i];
            if (i == 0) {
                // Depart
                directionDotsGeoObjects.add(geoFromLatLng(maneuver.lat, maneuver.lng, R.drawable.ic_direction_dots)); // start
                currLat = maneuver.lat;
                currLng = maneuver.lng;
                Log.d(TAG, "calculateDirectionsDots: depart: " + maneuver.lat + ", " + maneuver.lng);
            } else if (i == steps.length - 1) {
                // arrive

                Log.d(TAG, "calculateDirectionsDots: " + maneuver.type + " " + maneuver.modifier + ": " + maneuver.lat + ", " + maneuver.lng);
                double distance = LocationCalc.haversine(currLat, currLng, maneuver.lat, maneuver.lng) * 1000;
                // b/w current & maneuver.location add dots at 2.5m intervals
                double latDiff = maneuver.lat - currLat;
                double lngDiff = maneuver.lng - currLng;
                double latStep = latDiff / distance * 2.5;
                double lngStep = lngDiff / distance * 2.5;
                double lat = currLat;
                double lng = currLng;

                while (distance > 2.5) {
                    Log.d(TAG, "calculateDirectionsDots: > >" + lat + ", " + lng + " distance: " + distance + "m");
                    directionDotsGeoObjects.add(geoFromLatLng(lat, lng, R.drawable.ic_direction_dots));
                    lat += latStep;
                    lng += lngStep;
                    distance -= 2.5;
                }

                directionDotsGeoObjects.add(geoFromLatLng(maneuver.lat, maneuver.lng, R.drawable.flag));
                break;
            } else {
                // turns
                Log.d(TAG, "calculateDirectionsDots: " + maneuver.type + " " + maneuver.modifier + ": " + maneuver.lat + ", " + maneuver.lng);
                double distance = LocationCalc.haversine(currLat, currLng, maneuver.lat, maneuver.lng) * 1000;
                // b/w current & maneuver.location add dots at 2.5m intervals
                double latDiff = maneuver.lat - currLat;
                double lngDiff = maneuver.lng - currLng;
                double latStep = latDiff / distance * 2.5;
                double lngStep = lngDiff / distance * 2.5;
                double lat = currLat;
                double lng = currLng;

                while (distance > 2.5) {
                    Log.d(TAG, "calculateDirectionsDots: > >" + lat + ", " + lng + " distance: " + distance + "m");
                    directionDotsGeoObjects.add(geoFromLatLng(lat, lng, R.drawable.ic_direction_dots));
                    lat += latStep;
                    lng += lngStep;
                    distance -= 2.5;
                }

                currLat = maneuver.lat;
                currLng = maneuver.lng;
            }
        }
        return directionDotsGeoObjects.toArray(new GeoObject[0]);
    }

    static GeoObject geoFromLatLng(
            double lat, double lng,
            @DrawableRes int drawableRes) {
        GeoObject geoObject = new GeoObject();
        geoObject.setGeoPosition(lat, lng, 0.0);
        geoObject.setImageResource(drawableRes);
        return geoObject;
    }

    @Override
    public String toString() {
        return "DirectionsObject{" +
                "distance=" + distance +
                ", duration=" + duration +
                ", start=" + start +
                ", end=" + end +
                ", steps=" + Arrays.toString(steps) +
                ", directionDotsGeoObjects=" + Arrays.toString(directionDotsGeoObjects) +
                '}';
    }
}
