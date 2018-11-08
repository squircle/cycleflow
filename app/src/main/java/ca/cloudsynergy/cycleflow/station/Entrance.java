package ca.cloudsynergy.cycleflow.station;

import ca.cloudsynergy.cycleflow.location.Direction;

public class Entrance {

    private LightType type;
    private LightState currentState;
    private Double bearing;
    private Direction approxDirection;
    private boolean omnidirectional;
    private int timeToNextLight;


    public Entrance(byte data, byte nHeading, byte stateChangeTime) {
        // Demand Trigger
        if ((data >> 1 & 1) == 1) {
            type = LightType.DEMAND;
        } else {
            type = LightType.TIME;
        }

        // Present State
        if ((data & 1) == 1) {
            currentState = LightState.GREEN;
        } else {
            currentState = LightState.RED;
        }

        // Approach bearing
        if (nHeading == 0xFF) {
            omnidirectional = true;
            bearing = 0.0;
        } else {
            omnidirectional = false;
            // Convert +- 180 bearing to 0-360
            bearing = (((int)nHeading * 1.5) + 450) % 360;
            approxDirection = Direction.directionFromBearing(bearing);
        }

        // State change time
        if (type != LightType.DEMAND) {
            timeToNextLight = (int) stateChangeTime;
        }


    }

    public Direction getApproxDirection() {
        return approxDirection;
    }

    public LightState getCurrentState() {
        return currentState;
    }

    public int getTimeToNextLight() {
        return timeToNextLight;
    }

    public enum LightType {
        TIME,
        DEMAND
    }

    public enum LightState {
        RED,
        GREEN
    }

}
