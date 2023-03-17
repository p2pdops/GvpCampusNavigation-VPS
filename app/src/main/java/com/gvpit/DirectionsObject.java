package com.gvpit;

import androidx.annotation.DrawableRes;

import com.beyondar.android.world.GeoObject;
import com.beyondar.example.R;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class DirectionsObject {


    public String distance;
    public String duration;

//    public GeoObject start;

    public GeoObject end;

    public StepManeuver[] steps;

    public DirectionsObject(String distance, String duration, GeoObject end, StepManeuver[] steps) {
        this.distance = distance;
        this.duration = duration;
//        this.start = start;
        this.end = end;
        this.steps = steps;
    }

    public static DirectionsObject from(GeoObject endGeoObject, JSONObject jsonObject) throws JSONException {
        JSONArray routes = jsonObject.getJSONArray("routes");
        JSONObject route = routes.getJSONObject(0);
        JSONArray legs = route.getJSONArray("legs");
        JSONObject leg = legs.getJSONObject(0);
        double distance = leg.getDouble("distance");
        double duration = leg.getDouble("duration");
        JSONArray steps = leg.getJSONArray("steps");
        StepManeuver[] stepManeuvers = new StepManeuver[steps.length()];
        for (int i = 0; i < steps.length(); i++) {
            JSONObject step = steps.getJSONObject(i);
            JSONObject stepManeuver = step.getJSONObject("maneuver");
            String type = stepManeuver.getString("type");
            String modifier = stepManeuver.getString("modifier");
            JSONArray location = stepManeuver.getJSONArray("location");
            double lat = location.getDouble(0);
            double lng = location.getDouble(1);
            String stepDuration = step.getString("duration");
            String stepDistance = step.getString("distance");
            stepManeuvers[i] = new StepManeuver(type, modifier, lat, lng, stepDuration, stepDistance);
        }
        return new DirectionsObject(String.valueOf(distance), String.valueOf(duration), endGeoObject, stepManeuvers);
    }

    public static GeoObject[] calculateDirectionsDots(LatLng current, StepManeuver[] steps) {
        ArrayList<GeoObject> directionDotsGeoObjects = new ArrayList<>();
        double currentLat = current.latitude;
        double currentLng = current.longitude;
        for (int i = 0; i < steps.length; i++) {
            StepManeuver maneuver = steps[i];
            if (i == 0) {
                // Depart
                directionDotsGeoObjects.add(geoFromLatLng(maneuver.lat, maneuver.lng, R.drawable.ic_direction_dots)); // start
                currentLat = maneuver.lat;
                currentLng = maneuver.lng;
            } else if (i == steps.length - 1) {
                // arrive
                directionDotsGeoObjects.add(geoFromLatLng(maneuver.lat, maneuver.lng, R.drawable.flag));
                currentLat = maneuver.lat;
                currentLng = maneuver.lng;
            } else {
                // turns
                double distance = LocationCalc.haversine(currentLat, currentLng, maneuver.lat, maneuver.lng);
                // b/w current & maneuver.location add dots at 10m intervals
                double latDiff = maneuver.lat - current.latitude;
                double lngDiff = maneuver.lng - current.longitude;
                double latInterval = latDiff / distance;
                double lngInterval = lngDiff / distance;
                double lat = current.latitude;
                double lng = current.longitude;
                while (distance > 5) {
                    lat += latInterval;
                    lng += lngInterval;
                    distance -= 5;
                    directionDotsGeoObjects.add(geoFromLatLng(lat, lng, R.drawable.ic_direction_dots));
                }
                currentLat = maneuver.lat;
                currentLng = maneuver.lng;
            }
        }
        return directionDotsGeoObjects.toArray(new GeoObject[0]);
    }

    static GeoObject geoFromLatLng(double lat, double lng, @DrawableRes int drawableRes) {
        GeoObject geoObject = new GeoObject();
        geoObject.setGeoPosition(lat, lng, -50.0);
        geoObject.setImageResource(drawableRes);
        return geoObject;
    }

}
