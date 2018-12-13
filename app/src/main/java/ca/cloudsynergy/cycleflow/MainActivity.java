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
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import ca.cloudsynergy.cycleflow.location.Direction;
import ca.cloudsynergy.cycleflow.location.GpsCoordinates;
import ca.cloudsynergy.cycleflow.simulation.Simulation;
import ca.cloudsynergy.cycleflow.station.Entrance;
import ca.cloudsynergy.cycleflow.station.StationInfo;
import ca.cloudsynergy.cycleflow.synergy.Synergy;

/**
 * Main class for CycleFlow Android Application
 * <p>
 * Some code based on DeviceControlActivity.java from Google Samples on Github:
 * https://github.com/googlesamples/android-BluetoothLeGatt/
 *
 * @author Mitchell Kovacs
 * @author Noah Kruiper
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
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
    ArrayList<StationInfo> stationList; // Current list being populated/updated by scanner

    // Update UI, perform checks
    private CountDownTimer updater;

    // Map
    private GoogleMap mMap;

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

    // Simulation module - basic object used to mimic a user moving.
    private Simulation simulation;

    // UI Widgets
    private CheckBox useSimulationBox;
    private RadioGroup simRadioGroup;
    private RadioButton simNsRadioButton;
    private RadioButton simEwRadioButton;

    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView currentSpeedTextView;
    private TextView bearingTextView;
    private TextView lastUpdateTimeTextView;
    private TextView distanceToIntersectionTextView;
    private TextView stationSelectedTextView;
    private TextView stationCountTextView;
    private TextView stationSelectedEntranceTextView;
    private TextView stationCurrentLightTextView;
    private TextView stationTimeToLightChangeTextView;

    private TextView lightStatus;
    private TextView stationName;
    private TextView userSpeed;
    private TextView speedAdvice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ca.cloudsynergy.cycleflow.R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        lastUpdateTime = "";

        // Make the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        // Link view elements to objects by label
        assignWidgets();

        // This is dealt with using onStart(); - should be ok to leave this here
        requestingLocationUpdates = false; // Wait until permissions have been set.

        // List of found stations
        stationList = new ArrayList<>();
        // Link together modules
        synergy = new Synergy();

        // Create testing station
        ArrayList<Entrance> list = new ArrayList<>();
        Entrance e1 = new Entrance((byte) 0, (byte) (180 / 1.5), (byte) 30);
        list.add(e1);
        //StationInfo testStation = new StationInfo(45.422417, -75.683453, "Test Station", -50, 1, list, System.currentTimeMillis(), System.currentTimeMillis());
        //stationList.add(testStation);

        createLocationCallback();
        createLocationRequest();
    }

    private void assignWidgets() {
        useSimulationBox = findViewById(R.id.sim_checkbox);
        simRadioGroup = findViewById(R.id.sim_radio_group);
        simNsRadioButton = findViewById(R.id.sim_ns);
        simEwRadioButton = findViewById(R.id.sim_ew);
        latitudeTextView = findViewById(R.id.latitude_data);
        longitudeTextView = findViewById(R.id.longitude_data);
        currentSpeedTextView = findViewById(R.id.current_speed_data);
        bearingTextView = findViewById(R.id.bearing_data);
        lastUpdateTimeTextView = findViewById(R.id.last_update_time_data);
        distanceToIntersectionTextView = findViewById(R.id.distance_to_intersection_data);
        stationSelectedTextView = findViewById(R.id.selected_station_data);
        stationCountTextView = findViewById(R.id.station_count_data);
        stationSelectedEntranceTextView = findViewById(R.id.selected_entrance_data);
        stationCurrentLightTextView = findViewById(R.id.entrance_light_data);
        stationTimeToLightChangeTextView = findViewById(R.id.light_time_to_change_data);

        userSpeed = findViewById(R.id.user_speed_value);
        stationName = findViewById(R.id.station_name);
        lightStatus = findViewById(R.id.light_status);
        speedAdvice = findViewById(R.id.speed_advice);
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

        // Begin UI update process
        updater = new CountDownTimer(1000, 20) {

            @Override
            public void onTick(long millisUntilFinished) {
                // Do nothing every tick
            }

            @Override
            public void onFinish() {
                try {
                    update();
                } catch (Exception e) {
                    Log.e("Error", "Error: " + e.toString());
                }
            }
        }.start();
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

    private void update() {
        // Update time of test station so it doesn't expire, and flip light timer if needed
        for (StationInfo station : stationList) {
            if (station.name.equals("Test Station")) {
                station.time = System.currentTimeMillis() - 2000;
                if (station.entrances.get(0).getTimeToNextLight() == 0) {
                    station.flipTimers(30);
                }
            }
        }

        // Update station timers if they were last seen more than 1 second ago
        for (StationInfo station : stationList) {
            if (station.timerLastUpdated < System.currentTimeMillis() - 1000) {
                station.updateTimers();
            }
        }

        determineStation();

        updateLocationUi();

        // Restart timer
        updater.start();
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
            if (simulation == null) {
                Direction direction;
                if (simRadioGroup.getCheckedRadioButtonId() == simNsRadioButton.getId()) {
                    direction = Direction.SOUTH;
                } else {
                    direction = Direction.WEST;
                }
                simulation = new Simulation(direction);
            }
            currentLocation = simulation.getNewLocation();
            synergy.setCurrentLocation(currentLocation);
            Log.i("simlocation", "Setting the simulated location.");
        } else {
            currentLocation = synergy.getCurrentLocation();
        }

        if (currentLocation != null) {
            // TODO: Possible future addition could be to increase API level for finding accuracy
            latitudeTextView.setText(String.format(Locale.ENGLISH, "%f", currentLocation.getLatitude()));
            longitudeTextView.setText(String.format(Locale.ENGLISH, "%f", currentLocation.getLongitude()));
            currentSpeedTextView.setText(String.format(Locale.ENGLISH, "%f m/s", currentLocation.getSpeed()));
            bearingTextView.setText(String.format(Locale.ENGLISH, "%f degrees", currentLocation.getBearing()));
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
     *
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
                        } else {
                            Log.w(TAG, "getLastLocation:exception", task.getException());
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
                } else {
                    // Permission Denied.
                    Log.i(TAG, "Permission Denied.");
                    Toast.makeText(this, "FINE LOCATION PERMISSION REQUIRED", Toast.LENGTH_LONG).show();
                }
                break;
            case PR_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (grantResults[0] == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "Error enabling Bluetooth");
                    Toast.makeText(this, "Error enabling Bluetooth", Toast.LENGTH_LONG).show();
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
            // Using highest-power mode for scanning
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            // Not using filters due to many reported bugs in Android
            ArrayList<ScanFilter> filters = new ArrayList<>();
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
            byte[] cfData = result.getScanRecord().getBytes();

            // Only if the right bytes match should we attempt to create StationInfo
            if (cfData != null && cfData[1] == (byte) 0xFF && cfData[2] == (byte) 0xFF && cfData[3] == (byte) 0xFF && cfData[10] == (byte) 0xCF) {

                // Generate String version of received data
                String text = "";
                for (int i = 0; i < cfData.length; i++) {
                    text = text + cfData[i];
                    if (i == cfData.length / 2 - 1) {
                        text = text + "\n";
                    }
                }
                Log.i("ScanResult", "Scan result data: " + text);

                // Try to get info from the station
                StationInfo info = null;
                try {
                    info = StationInfo.StationInfoBuilder(cfData, result.getRssi());
                    info.time = System.currentTimeMillis();
                } catch (Exception e) {
                    Log.e("ScanResult", "Error creating Station Info.", e);
                }

                // If the station info exists, update it to the latest-received data
                if (info != null) {
                    boolean found = false;
                    for (int i = 0; i < stationList.size(); i++) {
                        if (stationList.get(i).id.equals(info.id)) {
                            // Update station timers if they have not changed and it has been more
                            if (info.time < System.currentTimeMillis() - 1000) {
                                info.updateTimers();
                            }

                            found = true;
                            stationList.remove(i);
                            stationList.add(i, info);
                            Log.i("ScanResult", "Updated station info for " + info.name + " in the stationList");
                        }
                    }

                    if (!found) {
                        stationList.add(info);
                    }
                }
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
        // A station is valid if the following conditions are met:
        //      it has been heard from in the last 15 seconds
        //      it is in front of the user
        //      it has an entrance whose angle is within range of current bearing

        // Removing old stations
        ArrayList<StationInfo> oldStations = new ArrayList<>();
        for (StationInfo station : stationList) {
            if (station.time < System.currentTimeMillis() - 15000) {
                oldStations.add(station);
            }
        }
        for (StationInfo station : oldStations) {
            stationList.remove(station);
        }

        StationInfo selectedStation = null;

        // find all stations within 90 degrees of user bearing
        ArrayList<StationInfo> validStations = new ArrayList<>();
        // Determine user direction
        Float userBearing = synergy.getCurrentLocation().getBearing();
        for (StationInfo station : stationList) {
            double bDif = GpsCoordinates.calculateBearingDiff(
                    userBearing,
                    GpsCoordinates.calculateBearing(synergy.getCurrentCoordinates(), station.coordinates));
            if (bDif <= 90) {
                validStations.add(station);
            }
        }

        // eliminate stations without an entrance with a valid angle
        // criteria: the difference between entrance bearing or user/user-station must be <=80
        ArrayList<StationInfo> nextValid = new ArrayList<>();
        for (StationInfo station : validStations) {
            for (Entrance entrance : station.entrances) {
                double entrance_user = GpsCoordinates.calculateBearingDiff(
                        synergy.getCurrentLocation().getBearing(), entrance.getBearing());
                double entrance_user_station = GpsCoordinates.calculateBearingDiff(
                        GpsCoordinates.calculateBearing(synergy.getCurrentCoordinates(), station.coordinates), entrance.getBearing());
                if ((entrance_user <= 80) || (entrance_user_station <= 80)) {
                    nextValid.add(station);
                    break;
                }
            }
        }

        // select the closest station
        double distance = 0;
        for (StationInfo station : nextValid) {
            if (selectedStation == null) {
                selectedStation = station;
                distance = GpsCoordinates.calculateDistance(synergy.getCurrentCoordinates(), selectedStation.coordinates);
            } else if (GpsCoordinates.calculateDistance(synergy.getCurrentCoordinates(), station.coordinates) < distance) {
                selectedStation = station;
            }
        }

        synergy.setCurrentStation(selectedStation);

        // Use the user's location relative to the station to determine which entrance they are using
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
            stationCountTextView.setText(String.valueOf(stationList.size()));
            stationSelectedTextView.setText(synergy.getCurrentStation().name);
            distanceToIntersectionTextView.setText(String.valueOf(synergy.getDistanceToStation()));

            stationSelectedEntranceTextView.setText(String.valueOf(synergy.getDesiredEntrance().getBearing()));
            stationCurrentLightTextView.setText(synergy.getDesiredEntrance().getCurrentState().toString());
            stationTimeToLightChangeTextView.setText(String.valueOf(synergy.getDesiredEntrance().getTimeToNextLight()));

            // Set end-user GUI elements

            if (synergy.getDesiredEntrance().getCurrentState() == Entrance.LightState.GREEN) {
                GradientDrawable drawable = (GradientDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.symbol_light, null);
                drawable.setColor(Color.parseColor("#03ae3c")); // green
                lightStatus.setBackground(drawable);
            } else if (synergy.getDesiredEntrance().getCurrentState() == Entrance.LightState.RED) {
                GradientDrawable drawable = (GradientDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.symbol_light, null);
                drawable.setColor(Color.parseColor("#d50000")); // red
                lightStatus.setBackground(drawable);
            } else {
                GradientDrawable drawable = (GradientDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.symbol_light, null);
                drawable.setColor(Color.parseColor("#dab000")); // gray
                lightStatus.setBackground(drawable);
            }
            lightStatus.setText(String.format(Locale.ENGLISH, "%d", synergy.getDesiredEntrance().getTimeToNextLight()));
            stationName.setText(synergy.getCurrentStation().name);

            // Determine if the user is going fast enough
            if (synergy.getDistanceToStation() > synergy.getDesiredEntrance().getTimeToNextLight() * synergy.getCurrentLocation().getSpeed() &
                    synergy.getDesiredEntrance().getCurrentState() == Entrance.LightState.GREEN) {
                speedAdvice.setText("Speed up!");
            } else if (synergy.getDesiredEntrance().getCurrentState() == Entrance.LightState.RED &&
                    synergy.getCurrentLocation().getSpeed() * synergy.getDesiredEntrance().getTimeToNextLight() > synergy.getDistanceToStation()) {
                speedAdvice.setText("Slow down!");
            } else {
                speedAdvice.setText("Speed OK");
            }

        } else {
            GradientDrawable drawable = (GradientDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.symbol_light, null);
            drawable.setColor(Color.parseColor("#dab000")); // gray
            lightStatus.setBackground(drawable);
            lightStatus.setText("?");
            stationName.setText("(No station)");
            speedAdvice.setText("");
        }
        int speed = (int) (synergy.getCurrentLocation().getSpeed() * 3.6);
        if (speed > 0 && speed < 100) {
            userSpeed.setText(String.valueOf(speed));
        } else {
            userSpeed.setText("0");
        }
        lightStatus.setGravity(Gravity.CENTER);

        // Clear and add user marker
        mMap.clear();
        LatLng user = new LatLng(synergy.getCurrentCoordinates().getLatitude(), synergy.getCurrentCoordinates().getLongitude());
        mMap.addMarker(new MarkerOptions().position(user).title("User").icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_bike_bordered_small)));

        if (synergy.getCurrentStation() != null) {
            // Add a marker for the station
            LatLng sta = new LatLng(synergy.getCurrentStation().coordinates.getLatitude(), synergy.getCurrentStation().coordinates.getLongitude());
            mMap.addMarker(new MarkerOptions().position(sta).title(synergy.getCurrentStation().name).icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_isometric_light_small)));
        }

        // Move map to user location
        mMap.moveCamera(CameraUpdateFactory.newLatLng(user));
        mMap.moveCamera(CameraUpdateFactory.zoomTo((float) 16.5));

    }

    // When the map is ready, display a certain area
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }


}
