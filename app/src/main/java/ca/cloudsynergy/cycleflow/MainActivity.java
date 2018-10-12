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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
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

    // Wireless Information
    private static Queue<IntersectionInfo> recievedData;

    // Bluetooth Scanning
    private BluetoothAdapter mBluetoothAdapter;
    BluetoothLeScanner bluetoothLeScanner;
    private boolean mScanning; // currently scanning
    private Handler mHandler = new Handler(); // handler for scanning

    // BLE Service
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;
    private boolean mConnected = false;

    // Valid Master UUID info
    String validUUIDStart = "CC9D";
    String validUUIDEnd = "-55D1-4F78-89F4-4D0EAAB24FED";

    // GPS Location information
    protected Location currentLocation;
    private String lastUpdateTime;
    private Boolean requestingLocationUpdates;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

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

        // Link view elements to objects by label
        assignWidgets();

        // Setup BLE service
        setupService();

        // TODO: Maybe change this to follow the use of buttons or something.
        requestingLocationUpdates = false; // Wait until permissions have been set.

        TrafficSim.StartSim();

        createLocationCallback();
        createLocationRequest();

    }

    private void assignWidgets() {
        latitudeTextView = findViewById(R.id.latitude_data);
        longitudeTextView = findViewById(R.id.longitude_data);
        currentSpeedTextView = findViewById(R.id.current_speed_data);
        currentSpeedAccuracyTextView = findViewById(R.id.current_speed_accuracy_data);
        bearingTextView = findViewById(R.id.bearing_data);
        bearingAccuracyTextView = findViewById(R.id.bearing_accuracy_data);
        lastUpdateTimeTextView = findViewById(R.id.last_update_time_data);
        distanceToIntersectionTextView = findViewById(R.id.distance_to_intersection_data);
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
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null && mDeviceAddress != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
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
            latitudeTextView.setText(String.format(Locale.ENGLISH, "%f", currentLocation.getLatitude()));
            longitudeTextView.setText(String.format(Locale.ENGLISH, "%f", currentLocation.getLongitude()));
            currentSpeedTextView.setText(String.format(Locale.ENGLISH, "%f m/s", currentLocation.getSpeed()));
            currentSpeedAccuracyTextView.setText(String.format(Locale.ENGLISH, "NEED MIN API OF 26", currentLocation));
            bearingTextView.setText(String.format(Locale.ENGLISH, "%f degrees", currentLocation.getBearing()));
            bearingAccuracyTextView.setText(String.format(Locale.ENGLISH, "NEED MIN API OF 26"));
            lastUpdateTimeTextView.setText(String.format(Locale.ENGLISH, "%s", lastUpdateTime));

            //TODO Move this next section to be called from somewhere else, like handle the information as you get it instead.
            while(!recievedData.isEmpty()) {
                IntersectionInfo intersectionInfo = recievedData.remove();
                // TODO: At this time only using one intersection, eventually will be mapped to an object or something. Once mapped the bellow needs to be changed to prevent constant overwrites
                GpsCoordinates location = new GpsCoordinates(currentLocation.getLatitude(), currentLocation.getLongitude());
                distanceToIntersectionTextView.setText(String.format(Locale.ENGLISH, "%f metres",
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


    // Very basic example method for receiving information. Will be heavily changed/removed
    // when an actual protocol is used.
    public static void ReceiveInformation(IntersectionInfo intersectionInfo) {
        recievedData.add(intersectionInfo);
    }


    //-------------------------
    // BLE

    /**
     * Scan and display available BLE devices.
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after 10 seconds.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothLeScanner.stopScan(mLeScanCallback);
                }
            }, 10000);

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
            String uuid = Utility.getUuidFromResult(result);
            Log.i("UUID", uuid);

            String uStart = "";
            String uEnd = "";
            try{
                uStart = uuid.substring(0, Math.min(uuid.length(), 5));
                uEnd = uuid.substring(uuid.length() - 29);
            }catch (Exception e){
                Log.d("VerifyUUID", "Error verifying UUID.");
            }

            if(!uuid.equals("") && !uStart.equals("") && !uEnd.equals("")){
                Log.i("Split UUID Start", uStart);
                Log.i("Split UUID End", uEnd);
                // verify if the UUID is a match
                if(uStart.equals(validUUIDStart) && uEnd.equals(validUUIDEnd)){
                    // valid match - end the scanning process
                    // TODO - maybe you can do both at once?
                    // TODO ensure this method call does not cause problems since it comes back here
                    scanLeDevice(false);

                    // TODO
                    mDeviceAddress = "";

                    // here would be where we activate the connection
                    mBluetoothLeService.connect(mDeviceAddress);

                }
            }
            super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
                Log.i("UUID", Utility.getUuidFromResult(sr));
            }
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };


    public void setupService(){
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                // TODO
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                // TODO
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // TODO

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // TODO
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


}
