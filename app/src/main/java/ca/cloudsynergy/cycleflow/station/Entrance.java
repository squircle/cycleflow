package ca.cloudsynergy.cycleflow.station;

public class Entrance {

    private LightType type;
    private LightState currentState;
    private float bearing;
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
            bearing = -1;
        } else {
            omnidirectional = false;
            bearing = (int)nHeading * 1.5f;
        }

        // State change time
        if (type != LightType.DEMAND) {
            timeToNextLight = (int) stateChangeTime;
        }


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
