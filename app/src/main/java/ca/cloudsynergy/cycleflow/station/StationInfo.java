package ca.cloudsynergy.cycleflow.station;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import ca.cloudsynergy.cycleflow.location.GpsCoordinates;

public class StationInfo {
    public String id;
    public GpsCoordinates coordinates;
    public String name;
    public int rssi;
    public int numEntrances;
    public long time;

    // Lat/long conversion factors
    public static final long LATLONG_INT_OFFSET = 8388540;
    public static final double LATITUDE_TO_FLOAT_FACTOR = 0x1.6800bf40659a3p-17;
    public static final double LONGITUDE_TO_FLOAT_FACTOR = 0x1.6800bf40659a3p-16;

    /*
     * Create StationInfo from PARSED ble advertisement data
     */
    public StationInfo (double latitude, double longitude, String name, int rssi, int numEntrances){
        this.coordinates = new GpsCoordinates(latitude, longitude);
        this.numEntrances = numEntrances;
        this.name = name;
        this.rssi = rssi;
    }

    /*
     * Create StationInfo from RAW ble advertisement data
     */
    public static StationInfo StationInfoBuilder (byte[] aData, int rssi){
        Objects.requireNonNull(aData);

        // Advertising Frame
        int aFrameLength = (int)aData[0] + 1;
        byte[] aFrame = Arrays.copyOfRange(aData, 0, aFrameLength);

        // Byte 0: Length of packet
        // Determine number of entrances based on reported length of packet
        int numEntrances;
        if(aData[0] == (byte) 0x1E){ // 30
            numEntrances = 6;
        } else if (aData[0] == (byte) 0x1B){ // 27
            numEntrances = 5;
        } else if (aData[0] == (byte) 0x18){ // 24
            numEntrances = 4;
        } else if (aData[0] == (byte) 0x15){ // 21
            numEntrances = 3;
        } else if (aData[0] == (byte) 0x12){ // 18
            numEntrances = 2;
        } else { // 15
            numEntrances = 1;
        }

        // Longitude and Latitude need to be padded to 4 bytes
        byte[] latByte = new byte[4];
        latByte[0] = (byte) 0x00;
        latByte[1] = aData[4];
        latByte[2] = aData[5];
        latByte[3] = aData[6];
        byte[] longByte = new byte[4];
        longByte[0] = (byte) 0x00;
        longByte[1] = aData[7];
        longByte[2] = aData[8];
        longByte[3] = aData[9];
        // Round to 5 decimal places
        double longitude = (double) Math.round(
                ((ByteBuffer.wrap(longByte).getInt() - LATLONG_INT_OFFSET) * LONGITUDE_TO_FLOAT_FACTOR)
                        * 100000) / 100000;
        double latitude = (double) Math.round(
                ((ByteBuffer.wrap(latByte).getInt() - LATLONG_INT_OFFSET) * LATITUDE_TO_FLOAT_FACTOR)
                        * 100000) / 100000;

        // Scan Response Frame
        // Convert intersection name from UTF-8
        byte[] sFrame = Arrays.copyOfRange(aData, aFrameLength, (int)aData[aFrameLength] + aFrameLength + 1);
        String name = "";
        try{
            name = new String(Arrays.copyOfRange(sFrame, 4, sFrame.length), "UTF-8");
        }catch (Exception e){
        }

        return new StationInfo(latitude, longitude, name, rssi, numEntrances);
    }
}
