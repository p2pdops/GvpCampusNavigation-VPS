package com.gvpit;

public class StepManeuver {
    public String type;
    public String modifier;
    public double lat;
    public double lng;
    public String duration;
    public String distance;

    public StepManeuver(String type, String modifier, double lat, double lng, String duration, String distance) {
        this.type = type;
        this.modifier = modifier;
        this.lat = lat;
        this.lng = lng;
        this.duration = duration;
        this.distance = distance;
    }

    @Override
    public String toString() {
        return "StepManeuver{" +
                "type='" + type + '\'' +
                ", modifier='" + modifier + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                ", duration='" + duration + '\'' +
                ", distance='" + distance + '\'' +
                '}';
    }
}