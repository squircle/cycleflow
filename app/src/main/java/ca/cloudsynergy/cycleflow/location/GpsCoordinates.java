package ca.cloudsynergy.cycleflow.location;

/**
 * Created by Mitchell Kovacs on 2018-01-19.
 */

public class GpsCoordinates {
    private double latitude;
    private double longitude;

    public GpsCoordinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "Latitude: " + latitude + ", Longitude: " + longitude;
    }

    public boolean equals(GpsCoordinates gpsCoordinates) {
        return (this.latitude == gpsCoordinates.getLatitude() && this.longitude == gpsCoordinates.getLongitude());
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    // Returns the distance in meters.
    public static double calculateDistance(GpsCoordinates point1, GpsCoordinates point2) {
        double lat1 = point1.getLatitude();
        double lon1 = point1.getLongitude();
        double lat2 = point2.getLatitude();
        double lon2 = point2.getLongitude();

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = 0.0;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }
}
