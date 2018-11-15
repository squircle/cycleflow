package ca.cloudsynergy.cycleflow.simulation;

import android.location.Location;

public class Simulation {
    private int distance; // meters
    private int movement; // how many meters it moves with each update
    // TODO: Maybe update these so they aren't hard coded.
    // Point of interest (intersection)
    private double latitude = 45.42025;
    private double longitude = -75.68502;

    // Earth's radius, sphere
    private static final double R = 6378137;

    public Simulation() {
        distance = 200;
        movement = 10;
    }

    public Location getNewLocation() {
        Location location = new Location("simulated location");
        // Calculate offset in radians
        double dLat = distance/R;
        // if ever doing longitude: dLon = de/(R*Cos(Pi*lat/180))

        // offset position, decimal degrees
        double newLat = latitude + dLat * 180/Math.PI;
        // lonO = lon + dLon * 180/Pi

        // Change distance for next update
        updateDistance();

        location.setLatitude(newLat);
        location.setLongitude(longitude);
        return location;
    }

    private void updateDistance() {
        distance = distance - movement;
        if (distance < 0) {
            distance = 200;
        }
    }
}
