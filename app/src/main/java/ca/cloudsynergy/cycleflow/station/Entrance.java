package ca.cloudsynergy.cycleflow.station;

import ca.cloudsynergy.cycleflow.location.Direction;

public class Entrance {

    private LightType type;
    private LightState currentState;
    private Double bearing;
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
            bearing = (nHeading & 0xFF) * 1.5;
        }

        // State change time
        if (type != LightType.DEMAND) {
            timeToNextLight = (int) stateChangeTime;
        }


    }

    public LightState getCurrentState() {
        return currentState;
    }

    public int getTimeToNextLight() {
        return timeToNextLight;
    }

    public Double getBearing() {
        return bearing;
    }


    public void updateLightTimer(){
        if(timeToNextLight > 0){
            timeToNextLight--;
        }
    }

    public void flipLightTimer(int time){
        timeToNextLight = time;
        switch (currentState){
            case GREEN:
                currentState = LightState.RED;
                break;
            case RED:
                currentState = LightState.GREEN;
                break;
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
