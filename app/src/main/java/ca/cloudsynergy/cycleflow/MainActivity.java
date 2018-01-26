package ca.cloudsynergy.cycleflow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import ca.cloudsynergy.cycleflow.trafficsim.GpsCoordinates;
import ca.cloudsynergy.cycleflow.trafficsim.IntersectionInfo;
import ca.cloudsynergy.cycleflow.trafficsim.TrafficSim;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    // Permission Requests
    private final int PR_FINE_LOCATION = 0;

    // Wireless Information
    private static Queue<IntersectionInfo> recievedData;

    // GPS Location information
    protected Location currentLocation;
    private String lastUpdateTime;
    private Boolean requestingLocationUpdates;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Labels
    private String latitudeLabel;
    private String longitudeLabel;
    private String currentSpeedLabel;
    private String currentSpeedAccuracyLabel;
    private String bearingLabel;
    private String bearingAccuracyLabel;
    private String lastUpdateTimeLabel;
    private String distanceToInstersectionLabel;

    // UI Widgets
    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView currentSpeedTextView;
    private TextView currentSpeedAccuracyTextView;
    private TextView bearingTextView;
    private TextView bearingAccuracyTextView;
    private TextView lastUpdateTimeTextView;
    private TextView distanceToIntersectionTextView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ca.cloudsynergy.cycleflow.R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        lastUpdateTime = "";
        recievedData = new LinkedList<>();

        assignLabels();
        assignWidgets();

        // TODO: Maybe change this to follow the use of buttons or something.
        requestingLocationUpdates = false; // Wait until permissions have been set.

        TrafficSim.StartSim();

        createLocationCallback();
        createLocationRequest();

    }

    private void assignLabels() {
        latitudeLabel = getResources().getString(R.string.latitude_label);
        longitudeLabel = getResources().getString(R.string.longitude_label);
        currentSpeedLabel = getResources().getString(R.string.current_speed_label);
        currentSpeedAccuracyLabel = getResources().getString(R.string.current_speed_accuracy_label);
        bearingLabel = getResources().getString(R.string.bearing_label);
        bearingAccuracyLabel = getResources().getString(R.string.bearing_accuracy_label);
        lastUpdateTimeLabel = getResources().getString(R.string.last_update_time_label);
        distanceToInstersectionLabel = getResources().getString(R.string.distance_to_instersection_label);
    }

    private void assignWidgets() {
        latitudeTextView = findViewById(R.id.latitude_text);
        longitudeTextView = findViewById(R.id.longitude_text);
        currentSpeedTextView = findViewById(R.id.current_speed_text);
        currentSpeedAccuracyTextView = findViewById(R.id.current_speed_accuracy_text);
        bearingTextView = findViewById(R.id.bearing_text);
        bearingAccuracyTextView = findViewById(R.id.bearing_accuracy_text);
        lastUpdateTimeTextView = findViewById(R.id.last_update_time_text);
        distanceToIntersectionTextView = findViewById(R.id.distance_to_intersection_text);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Request permissions and get the last known location if permissions are granted.
        if (!checkPermissions()) {
            requestPermissions();
        } else {
            requestingLocationUpdates = true;
            getLastLocation();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currentLocation = locationResult.getLastLocation();
                lastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                updateLocationUi();
            }
        };
    }

    private void updateLocationUi() {
        if (currentLocation != null) {
            // TODO: Increase API level for finding accuracy?
            /*
            Log.d(TAG, "HAS SPEED?");
            Log.d(TAG, currentLocation.hasSpeed() + "");
            Log.d(TAG, "HAS BEARING: " + currentLocation.hasBearing());
            */
            latitudeTextView.setText(String.format(Locale.ENGLISH, "%s: %f", latitudeLabel,
                    currentLocation.getLatitude()));
            longitudeTextView.setText(String.format(Locale.ENGLISH, "%s: %f", longitudeLabel,
                    currentLocation.getLongitude()));
            currentSpeedTextView.setText(String.format(Locale.ENGLISH, "%s: %f m/s", currentSpeedLabel,
                    currentLocation.getSpeed()));
            currentSpeedAccuracyTextView.setText(String.format(Locale.ENGLISH, "%s: NEED MIN API OF 26", currentSpeedAccuracyLabel));
            bearingTextView.setText(String.format(Locale.ENGLISH, "%s: %f degrees", bearingLabel,
                    currentLocation.getBearing()));
            bearingAccuracyTextView.setText(String.format(Locale.ENGLISH, "%s: NEED MIN API OF 26", bearingAccuracyLabel));
            lastUpdateTimeTextView.setText(String.format(Locale.ENGLISH, "%s: %s", lastUpdateTimeLabel,
                    lastUpdateTime));

            //TODO Move this next section to be called from somewhere else, like handle the information as you get it instead.
            while(!recievedData.isEmpty()) {
                IntersectionInfo intersectionInfo = recievedData.remove();
                // TODO: At this time only using one intersection, eventually will be mapped to an object or something. Once mapped the bellow needs to be changed to prevent constant overwrites
                GpsCoordinates location = new GpsCoordinates(currentLocation.getLatitude(), currentLocation.getLongitude());
                distanceToIntersectionTextView.setText(String.format(Locale.ENGLISH, "%s: %f meters",
                        distanceToInstersectionLabel,
                        GpsCoordinates.calculateDistance(location, intersectionInfo.getGpsCoordinates())));
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.myLooper());
    }

    /**
     * Return the current state of the permissions needed.
     * @return If the current permissions are granted.
     */
    private boolean checkPermissions() {
        int permissionsState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionsState == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings("MissingPermission")
    private void getLastLocation() {
        fusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Log.i(TAG, task.getResult().toString());
                            currentLocation = task.getResult();
                            lastUpdateTime = DateFormat.getTimeInstance().format(new Date());

                            updateLocationUi();
                        } else {
                            Log.w(TAG, "getLastLocation:exception", task.getException());
                            // TODO: Show snackbar for no location detected.
                        }
                    }
                });
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            // TODO: Display rationale. Maybe use a Snackbar?
            // Example in this: https://github.com/googlesamples/android-play-location/blob/master/BasicLocationSample/app/src/main/java/com/google/android/gms/location/sample/basiclocationsample/MainActivity.java
        } else {
            Log.i(TAG, "Requesting permission.");
            startLocationPermissionRequest();
        }
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PR_FINE_LOCATION);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult:");
        switch (requestCode) {
            case (PR_FINE_LOCATION):
                if (grantResults.length <= 0) {
                    // User interaction interuppted, arrays will be empty
                    Log.i(TAG, "User interaction was cancelled.");
                } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission Granted.");
                    requestingLocationUpdates = true;
                    getLastLocation();
                }
                 else {
                    // Permission Denied.
                    Log.i(TAG, "Permission Denied.");
                    // TODO: Handle this.
                }
                break;
            default:
                Log.e(TAG, "Unhandled requestCode.");
        }
    }


    // Very basic example method for receiving information. Will be heavily changed/removed
    // when an actual protocol is used.
    public static void ReceiveInformation(IntersectionInfo intersectionInfo) {
        recievedData.add(intersectionInfo);
    }
}
