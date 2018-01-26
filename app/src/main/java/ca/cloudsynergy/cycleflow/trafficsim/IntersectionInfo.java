package ca.cloudsynergy.cycleflow.trafficsim;

import java.util.Queue;

/**
 * Created by Mitchell Kovacs on 2018-01-26.
 */

public class IntersectionInfo {
    private GpsCoordinates gpsCoordinates;
    private Queue<LightInfo> lights;

    public IntersectionInfo (GpsCoordinates gpsCoordinates, Queue<LightInfo> lights) {
        this.gpsCoordinates = gpsCoordinates;
        this.lights = lights;
    }

    public Queue<LightInfo> getLights() {
        return lights;
    }

    public GpsCoordinates getGpsCoordinates() {
        return gpsCoordinates;
    }

    @Override
    public String toString() {
        return "Intersection location: " + gpsCoordinates;
    }
}
