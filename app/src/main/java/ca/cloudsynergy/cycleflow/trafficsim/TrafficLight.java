package ca.cloudsynergy.cycleflow.trafficsim;

import java.util.Locale;

/**
 * Created by Mitchell Kovacs on 2018-01-19.
 */

public class TrafficLight {
    private float bearing;
    private int redLightDuration;
    private int greenLightDuration;
    private boolean startRed;
    private int totalDuration;

    public TrafficLight(float bearing, int redLightDuration, int greenLightDuration, boolean startRed) {
        this.bearing = bearing;
        this.redLightDuration = redLightDuration;
        this.greenLightDuration = greenLightDuration;
        this.startRed = startRed;
        this.totalDuration = redLightDuration + greenLightDuration;
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "Bearing: %f\nRed duration: %d, Green duration: %d\nStart Red: %b",
                bearing, redLightDuration, greenLightDuration, startRed);
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    /**
     * Used by the simulation to determine the amount of time left in the current light.
     *
     * @return The current state of the light in a LightInfo object.
     */
    public LightInfo getState(int currentTime) {
        // Loop current time by the max amount of green red cycle. Should be changed when sim is improved.
        if (currentTime > totalDuration) {
            currentTime = currentTime % totalDuration;
        }

        LightInfo state = new LightInfo();
        state.setGreenDuration(greenLightDuration);

        LightState firstLightCol;
        LightState secondLightCol;
        int firstLightDur;
        int secondLightDur;
        if (startRed) {
            firstLightCol = LightState.RED;
            secondLightCol = LightState.GREEN;
            firstLightDur = redLightDuration;
            secondLightDur = greenLightDuration;
        } else {
            firstLightCol = LightState.GREEN;
            secondLightCol = LightState.RED;
            firstLightDur = greenLightDuration;
            secondLightDur = redLightDuration;
        }


        if (currentTime >= firstLightDur) {
            state.setCurrentState(secondLightCol);
            state.setTimeRemaining(secondLightDur - (currentTime - firstLightDur));
        } else {
            state.setCurrentState(firstLightCol);
            state.setTimeRemaining(firstLightDur - currentTime);
        }

        return state;
    }
}
