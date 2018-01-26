package ca.cloudsynergy.cycleflow.trafficsim;

import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import ca.cloudsynergy.cycleflow.MainActivity;

/**
 * Created by Mitchell Kovacs on 2018-01-19.
 */

public class Intersection extends TimerTask {
    private static final String TAG = TrafficSim.class.getSimpleName();

    private List<TrafficLight> trafficLights;
    private GpsCoordinates location;
    private boolean running;
    private Timer timer;

    private volatile int currentTime = 0;

    public Intersection(GpsCoordinates location) {
        this.trafficLights = new LinkedList<>();
        this.location = location;
        this.running = false;
    }

    public void addLight(TrafficLight trafficLight) {
        trafficLights.add(trafficLight);
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "Location: %s. Roads: %s", location, trafficLights);
    }


    public void startIntersection() {
        if (running) {
            Log.i(TAG, "Light is already running...");
        } else {
            timer = new Timer();
            timer.schedule(this, 0, 1000);
        }
    }

    public void stopIntersection() {
        this.cancel();
    }

    @Override
    public void run() {
        // TODO: This should eventually be replaced to interact with the BlueTooth(/whatever) interface instead
        // TODO: Currently assuming all trafficLights have the same total duration, make this smarter instead

        Queue<LightInfo> intersectionInfo = new LinkedList<>();

        for (TrafficLight trafficLight : trafficLights) {
            Log.i(TAG, "Current time: " + currentTime);
            Log.i(TAG, "LIGHT:\n" + trafficLight.toString());
            LightInfo lightInfo = trafficLight.getState(currentTime);
            Log.i(TAG, lightInfo.toString());
            Log.i(TAG, "___");
            intersectionInfo.add(lightInfo);
        }
        Log.i(TAG, "=======");

        // Basic imitation of "sending" data to the app that will be heavily changed.
        MainActivity.ReceiveInformation(new IntersectionInfo(location, intersectionInfo));

        currentTime++;
    }
}
