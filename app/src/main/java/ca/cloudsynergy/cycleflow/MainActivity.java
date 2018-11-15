package ca.cloudsynergy.cycleflow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import ca.cloudsynergy.cycleflow.location.GpsCoordinates;
import ca.cloudsynergy.cycleflow.station.Entrance;
import ca.cloudsynergy.cycleflow.station.StationInfo;
import ca.cloudsynergy.cycleflow.synergy.Synergy;

/*
 * Main class for CycleFlow Android Application
 *
 * Some code based on DeviceControlActivity.java from Google Samples on Github:
 * https://github.com/googlesamples/android-BluetoothLeGatt/
 *
 * @author Mitchell Kovacs
 * @author Noah Kruiper
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    // Permission Requests
    private final int PR_FINE_LOCATION = 0;
    private static final int PR_ENABLE_BT = 2;

    // Bluetooth Scanning
    private BluetoothAdapter mBluetoothAdapter;
    BluetoothLeScanner bluetoothLeScanner;
    private boolean mScanning; // currently scanning
    private Handler mHandler = new Handler(); // handler for scanning

    // Station Info
    Map<String, StationInfo> stations;
    StationInfo approachStation; // Selected approach station
    StationInfo passedStation; // Previously-crossed intersection
    ArrayList<StationInfo> currentScanList = new ArrayList<>(); // Current list being populated/updated by scanner
    ArrayList<StationInfo> lastScanList = new ArrayList<>(); // List from previous scan

    // GPS Location information
    private String lastUpdateTime;
    private Boolean requestingLocationUpdates;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Synergy Module - contains everything needed for calculations. Current Location, current station
    private Synergy synergy;

    // UI Widgets
    private CheckBox useSimulationBox;

    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView currentSpeedTextView;
    private TextView currentSpeedAccuracyTextView;
    private TextView bearingTextView;
    private TextView bearingAccuracyTextView;
    private TextView lastUpdateTimeTextView;
    private TextView distanceToIntersectionTextView;
    private TextView stationSelectedTextView;
    private TextView stationCountTextView;
    private TextView stationSelectedEntranceTextView;
    private TextView stationCurrentLightTextView;
    private TextView stationTimeToLightChangeTextView;
    private TextView approachStationRawDataTextView;
    private TextView approachStationNameTextView;
    private TextView approachStationLatTextView;
    private TextView approachStationLongTextView;
    private TextView approachStationRssiTextView;
    private TextView approachStationNumEntrancesTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ca.cloudsynergy.cycleflow.R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        lastUpdateTime = "";

        // Link view elements to objects by label
        assignWidgets();

        // TODO: Maybe change this to follow the use of buttons or something.
        requestingLocationUpdates = false; // Wait until permissions have been set.

        stations = new HashMap<>();
        synergy = new Synergy();

        createLocationCallback();
        createLocationRequest();
    }

    private void assignWidgets() {
        useSimulationBox = findViewById(R.id.sim_checkbox);

        latitudeTextView = findViewById(R.id.latitude_data);
        longitudeTextView = findViewById(R.id.longitude_data);
        currentSpeedTextView = findViewById(R.id.current_speed_data);
        currentSpeedAccuracyTextView = findViewById(R.id.current_speed_accuracy_data);
        bearingTextView = findViewById(R.id.bearing_data);
        bearingAccuracyTextView = findViewById(R.id.bearing_accuracy_data);
        lastUpdateTimeTextView = findViewById(R.id.last_update_time_data);
        distanceToIntersectionTextView = findViewById(R.id.distance_to_intersection_data);
        stationSelectedTextView = findViewById(R.id.selected_station_data);
        stationCountTextView = findViewById(R.id.station_count_data);
        stationSelectedEntranceTextView = findViewById(R.id.selected_entrance_data);
        stationCurrentLightTextView = findViewById(R.id.entrance_light_data);
        stationTimeToLightChangeTextView = findViewById(R.id.light_time_to_change_data);
        approachStationRawDataTextView = findViewById(R.id.approach_station_raw_data);
        approachStationNameTextView = findViewById(R.id.approach_station_name_data);
        approachStationLatTextView = findViewById(R.id.approach_station_lat_data);
        approachStationLongTextView = findViewById(R.id.approach_station_long_data);
        approachStationRssiTextView = findViewById(R.id.approach_station_rssi_data);
        approachStationNumEntrancesTextView = findViewById(R.id.approach_station_num_entrances_data);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Handle Bluetooth adapter/scanner
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is unavailable", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // Request location permissions and get the last known location if permissions are granted.
        if (!checkPermissions()) {
            requestPermissions();
        } else {
            requestingLocationUpdates = true;
            getLastLocation();
        }

        // Request bluetooth to be enabled if it is not already
        if (!mBluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Bluetooth not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, PR_ENABLE_BT);
        }

        // Scan for LE Devices
        scanLeDevice(true);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

                synergy.setCurrentLocation(locationResult.getLastLocation());
                lastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                updateLocationUi();
            }
        };
    }

    private void updateLocationUi() {
        Location currentLocation;
        if (useSimulationBox.isChecked()) {
            currentLocation = null;
        } else {
            currentLocation = synergy.getCurrentLocation();
        }

        if (currentLocation != null) {
            // TODO: Increase API level for finding accuracy?
            latitudeTextView.setText(String.format(Locale.ENGLISH, "%f", currentLocation.getLatitude()));
            longitudeTextView.setText(String.format(Locale.ENGLISH, "%f", currentLocation.getLongitude()));
            currentSpeedTextView.setText(String.format(Locale.ENGLISH, "%f m/s", currentLocation.getSpeed()));
            currentSpeedAccuracyTextView.setText(String.format(Locale.ENGLISH, "NEED MIN API OF 26", currentLocation));
            bearingTextView.setText(String.format(Locale.ENGLISH, "%f degrees", currentLocation.getBearing()));
            bearingAccuracyTextView.setText(String.format(Locale.ENGLISH, "NEED MIN API OF 26"));
            lastUpdateTimeTextView.setText(String.format(Locale.ENGLISH, "%s", lastUpdateTime));
        }

        // Determine which station is being approached with updated information
        determineStation();
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
                            synergy.setCurrentLocation(task.getResult());
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
                    // User interaction interrupted, arrays will be empty
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
            case PR_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (grantResults[0] == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "Error enabling Bluetooth");
                    Toast.makeText(this, "Error enabling Bluetooth", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "Unhandled requestCode.");
        }
    }

    //-------------------------
    // BLE

    /**
     * Scan and display available BLE devices.
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // stops scanning after 10 seconds
            /*
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothLeScanner.stopScan(mLeScanCallback);
                }
            }, 20000);
            */

            // Using highest-power mode for scanning
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            // Not using filters due to many reported bugs in Android
            // Supposed to filter manually
            ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();

            mScanning = true;
            bluetoothLeScanner.startScan(filters, scanSettings, mLeScanCallback);
        } else {
            mScanning = false;
            bluetoothLeScanner.stopScan(mLeScanCallback);
        }
    }

    /**
     * Callback for BLE scanner
     */
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());

            // Get transmitted bytes from the broadcast data from both packets
            byte [] cfData = result.getScanRecord().getBytes();

            // Only if the right bytes match should we attempt to create StationInfo
            if (cfData != null && cfData[1] == (byte)0xFF && cfData[2] == (byte)0xFF && cfData[3] == (byte)0xFF && cfData[10] == (byte)0xCF) {

                // Generate String version of received data
                String text = "";
                for(int i = 0; i < cfData.length; i++){
                    text = text + cfData[i];
                    if(i == cfData.length/2 - 1){
                        text = text + "\n";
                    }
                }
                Log.i("ScanResult", "Scan result data: " + text);

                // Try to get info from the station
                StationInfo info = null;
                try {
                    info = StationInfo.StationInfoBuilder(cfData, result.getRssi());
                    info.time = result.getTimestampNanos();
                } catch (Exception e){
                    Log.e("ScanResult", "Error creating Station Info.", e);
                }
                // If the info already exists, update it to the latest-received data
                if(info != null){
                    for(int i = 0; i < currentScanList.size(); i++){
                        if(currentScanList.get(i).id == info.id){
                            currentScanList.remove(i);
                            currentScanList.add(info);
                            Log.i("ScanResult","Updated station info for " + info.name + " in the currentScanList");
                        }
                    }
                    // If the candidate is more appropriate, update approach station
                    if(approachStation != null && approachStation.rssi > info.rssi){
                        approachStation = info;
                        Log.i("ScanResult","Updated approach station to " + info.name);
                    }
                    // Display data
                    approachStationRawDataTextView.setText(text);
                    approachStationNameTextView.setText(info.name);
                    approachStationLatTextView.setText(String.valueOf(info.coordinates.getLatitude()));
                    approachStationLongTextView.setText(String.valueOf(info.coordinates.getLongitude()));
                    approachStationRssiTextView.setText(String.valueOf(info.rssi));
                    approachStationNumEntrancesTextView.setText(String.valueOf(info.numEntrances));
                }

                // Create station map key based on longitude and latitude
                String key = String.valueOf(info.coordinates.getLatitude()) + String.valueOf(info.coordinates.getLongitude());
                stations.put(key, info);

            } else {
                Log.i("DATA", "no match");
            }

            super.onScanResult(callbackType, result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    //-------------------------
    // Station Determination

    private void determineStation() {
        // TODO: Clear out intersections after they haven't been updated in x time.
        Iterator<Map.Entry<String, StationInfo>> it = stations.entrySet().iterator();
//        while(it.hasNext()) {
//        }

        // Assumptions on size:
        //      0 - gg no re
        //      1 - Just display this data
        //      2 - tbd, most likely if distance < something display other station
        switch (stations.size()) {
            case 0:
                synergy.setCurrentStation(null);
                break;
            case 1:
                synergy.setCurrentStation(stations.entrySet().iterator().next().getValue());
                break;
            case 2:
                break;
            default:
                Log.e("determineStation", "Too man stations discovered: "
                        + stations.size());
        }

        // Use the users location relative to the station to determine which entrance they are using
        if (synergy.getCurrentLocation() != null && synergy.getCurrentStation() != null) {
            Double bearingToIntersection = GpsCoordinates.calculateBearing(
                    synergy.getCurrentCoordinates(),
                    synergy.getCurrentStation().coordinates);

            // Find the entrance with the closest approach angle matching bearing to intersection
            Entrance closestEntrance = null;
            double bearingDiff = -1;
            for (Entrance entrance : synergy.getCurrentStation().entrances) {
                if (closestEntrance == null) {
                    closestEntrance = entrance;
                    bearingDiff = GpsCoordinates.calculateBearingDiff(bearingToIntersection,
                            entrance.getBearing());
                } else {
                    double entranceBearingDiff = GpsCoordinates.calculateBearingDiff(bearingToIntersection,
                                    entrance.getBearing());
                    if (entranceBearingDiff < bearingDiff) {
                        closestEntrance = entrance;
                        bearingDiff = entranceBearingDiff;
                    }
                }
            }
            synergy.setDesiredEntrance(closestEntrance);
            Log.i("desiredEntrance", "Desired entrance: " + bearingToIntersection.toString());
        }

        // Update UI
        if (synergy.getCurrentStation() != null) {
            stationCountTextView.setText(String.valueOf(stations.size()));
            stationSelectedTextView.setText(synergy.getCurrentStation().name);
            distanceToIntersectionTextView.setText(String.valueOf(synergy.getDistanceToStation()));
        }

        if (synergy.getDesiredEntrance() != null) {
            stationSelectedEntranceTextView.setText(synergy.getDesiredEntrance().getBearing().toString());
            stationCurrentLightTextView.setText(synergy.getDesiredEntrance().getCurrentState().toString());
            stationTimeToLightChangeTextView.setText(
                    String.valueOf(synergy.getDesiredEntrance().getTimeToNextLight()));
        }
    }
}
