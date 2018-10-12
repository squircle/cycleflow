package ca.cloudsynergy.cycleflow;

import android.bluetooth.le.ScanResult;
import android.util.Log;

public class Utility {

    public static String getUuidFromResult(ScanResult result){
        String UUIDx = "";
        try{
            UUIDx = result.getScanRecord().getServiceUuids().toString();
        } catch (Exception e){
            Log.e("getUuidFromResult", "Null result.");
        }
        Log.i("getUuidFromResult", " as String ->>" + UUIDx);
        return UUIDx;
    }
}
