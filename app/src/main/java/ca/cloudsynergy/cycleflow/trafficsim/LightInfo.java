package ca.cloudsynergy.cycleflow.trafficsim;

import java.util.Locale;

/**
 * Created by Mitchell Kovacs on 2018-01-26.
 */

//TODO: Change time from int to something more meaningful when sim gets overhauled.
//TODO: Update from assumption that only states are green and red.
//TODO: Update to match protocol in docs. (Which will probably happen when this is overhauled to match the sim

public class LightInfo {
    private LightState currentState;
    private int timeRemaining;
    private int greenDuration;

    public LightState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(LightState currentState) {
        this.currentState = currentState;
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public void setTimeRemaining(int timeRemaining) {
        this.timeRemaining = timeRemaining;
    }

    public int getGreenDuration() {
        return greenDuration;
    }

    public void setGreenDuration(int greenDuration) {
        this.greenDuration = greenDuration;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "Current light: %s,\nTime remaining: %d\nGreen duration: %d",
                currentState, timeRemaining, greenDuration);
    }
}
