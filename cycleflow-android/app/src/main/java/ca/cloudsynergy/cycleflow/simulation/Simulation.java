package ca.cloudsynergy.cycleflow.simulation;

import android.location.Location;
import android.util.Log;

import ca.cloudsynergy.cycleflow.location.Direction;

/**
 * Control simulated location data.
 *
 * @author Mitchell Kovacs
 */
public class Simulation {
    private int distance; // meters
    private int resetDistance;
    private int movement; // how many meters it moves with each update
    private float bearing;
    private long lastUpdateTime;
    private Direction direction;
    // TODO: Maybe update these so they aren't hard coded.
    // Point of interest (intersection)
    private double latitude = 51.041045;
    private double longitude = -114.071501;

    // Earth's radius, sphere
    private static final double R = 6378137;

    public Simulation(Direction direction) {
        movement = 7;
        distance = 285;
        resetDistance = distance;
        this.direction = direction;

        switch (direction) {
            case SOUTH:
                bearing = 180F;
                break;
            case WEST:
                bearing = 270F;
                break;
            default:
                Log.e("simulation", "Unknown simulation direction");
        }

        lastUpdateTime = System.currentTimeMillis();
    }

    public Location getNewLocation() {
        Location location = new Location("simulated location");
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        // Calculate offset in radians
        switch (direction) {
            case SOUTH:
                double dLat = distance / R;
                // for south to north this would be "-" instead, similar for west to east but below calc instead.
                double newLat = latitude + dLat * 180 / Math.PI;
                location.setLatitude(newLat);
                break;
            case WEST:
                double dLon = distance / (R * Math.cos(Math.PI * latitude / 180));
                double newLon = longitude + dLon * 180 / Math.PI;
                location.setLongitude(newLon);
                break;
        }


        // Change distance for next update
        updateDistance();

        long timeTravelled = (System.currentTimeMillis() - lastUpdateTime) / 1000;
        //location.setSpeed(((float)movement) / timeTravelled);
        location.setSpeed((float) 7.0);
        location.setBearing(bearing);

        return location;
    }

    private void updateDistance() {
        distance = distance - movement;
        if (distance < 0) {
            distance = resetDistance;
        }
    }
}
