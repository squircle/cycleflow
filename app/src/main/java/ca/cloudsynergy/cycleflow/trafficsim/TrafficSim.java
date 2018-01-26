package ca.cloudsynergy.cycleflow.trafficsim;

/**
 * Created by Mitchell Kovacs on 2018-01-19.
 */

public class TrafficSim {
    private static final String TAG = TrafficSim.class.getSimpleName();

    public static void StartSim() {
        Intersection intersection1 = new Intersection(new GpsCoordinates(45, -75));
        // North
        intersection1.addLight(new TrafficLight(0, 10, 10, true));
        // South
//        intersection1.addLight(new TrafficLight(180, 10, 10, true));
        // East
        intersection1.addLight(new TrafficLight(90, 10, 10, false));
        // West
//        intersection1.addLight(new TrafficLight(270, 10, 10, false));

        intersection1.startIntersection();

    }
}
