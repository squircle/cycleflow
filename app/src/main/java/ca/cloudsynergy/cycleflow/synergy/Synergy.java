package ca.cloudsynergy.cycleflow.synergy;

import android.location.Location;

import ca.cloudsynergy.cycleflow.location.Direction;
import ca.cloudsynergy.cycleflow.location.GpsCoordinates;
import ca.cloudsynergy.cycleflow.station.StationInfo;

public class Synergy {

    private Location currentLocation;
    private GpsCoordinates currentCoordinates;
    private Long bearing;
    private Direction direction;

    private StationInfo currentStation;

    public double getDistanceToStation() {
        if (currentCoordinates == null || currentStation == null) {
            return 0;
        }
        return GpsCoordinates.calculateDistance(currentCoordinates, currentStation.coordinates);
    }


    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
        if (currentStation != null) {
            currentCoordinates = new GpsCoordinates(currentLocation.getLatitude(),
                    currentLocation.getLongitude());
        }
    }

    public Long getBearing() {
        return bearing;
    }

    public void setBearing(Long bearing) {
        this.bearing = bearing;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public StationInfo getCurrentStation() {
        return currentStation;
    }

    public void setCurrentStation(StationInfo currentStation) {
        this.currentStation = currentStation;
    }
}
