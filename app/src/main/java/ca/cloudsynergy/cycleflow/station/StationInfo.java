package ca.cloudsynergy.cycleflow.station;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ca.cloudsynergy.cycleflow.location.GpsCoordinates;

public class StationInfo {
    public String id;
    public GpsCoordinates coordinates;
    public String name;
    public int rssi;
    public int numEntrances;
    public long time;
    public List<Entrance> entrances;

    // Lat/long conversion factors
    public static final long LATLONG_INT_OFFSET = 8388540;
    public static final double LATITUDE_TO_FLOAT_FACTOR = 0x1.6800bf40659a3p-17;
    public static final double LONGITUDE_TO_FLOAT_FACTOR = 0x1.6800bf40659a3p-16;

    /*
     * Create StationInfo from PARSED ble advertisement data
     */
    public StationInfo (double latitude, double longitude, String name, int rssi,
                        int numEntrances, List<Entrance> entrances, long time){
        this.coordinates = new GpsCoordinates(latitude, longitude);
        this.name = name;
        this.rssi = rssi;
        this.numEntrances = numEntrances;
        this.entrances = entrances;
        this.time = time;
        this.id = String.valueOf(latitude) + String.valueOf(longitude);
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
        if(aFrame[0] == (byte) 0x1E){ // 30
            numEntrances = 6;
        } else if (aFrame[0] == (byte) 0x1B){ // 27
            numEntrances = 5;
        } else if (aFrame[0] == (byte) 0x18){ // 24
            numEntrances = 4;
        } else if (aFrame[0] == (byte) 0x15){ // 21
            numEntrances = 3;
        } else if (aFrame[0] == (byte) 0x12){ // 18
            numEntrances = 2;
        } else { // 15
            numEntrances = 1;
        }

        // Ignoring byte 2-3

        // Longitude and Latitude need to be padded to 4 bytes
        // Latitude  byte 4-6
        byte[] latByte = new byte[4];
        latByte[0] = (byte) 0x00;
        latByte[1] = aFrame[4];
        latByte[2] = aFrame[5];
        latByte[3] = aFrame[6];
        // Longitude byte 7-9
        byte[] longByte = new byte[4];
        longByte[0] = (byte) 0x00;
        longByte[1] = aFrame[7];
        longByte[2] = aFrame[8];
        longByte[3] = aFrame[9];
        // Round to 5 decimal places
        double longitude = (double) Math.round(
                ((ByteBuffer.wrap(longByte).getInt() - LATLONG_INT_OFFSET) * LONGITUDE_TO_FLOAT_FACTOR)
                        * 100000) / 100000;
        double latitude = (double) Math.round(
                ((ByteBuffer.wrap(latByte).getInt() - LATLONG_INT_OFFSET) * LATITUDE_TO_FLOAT_FACTOR)
                        * 100000) / 100000;

        // Byte 10 contains CycleFlow magic number

        // Byte 11-12: Global flags, ignoring for now.

        // Byte 13-end of aFrame, entrance information
        List<Entrance> entrances = new ArrayList<>();
        for(int i = 0; i < numEntrances; i++) {
            int startByte = 13 + i*3;
            entrances.add(new Entrance(aFrame[startByte], aFrame[startByte + 1], aFrame[startByte + 2]));
        }

        // Scan Response Frame
        // Convert intersection name from UTF-8
        byte[] sFrame = Arrays.copyOfRange(aData, aFrameLength, (int)aData[aFrameLength] + aFrameLength + 1);
        String name = "";
        try{
            name = new String(Arrays.copyOfRange(sFrame, 4, sFrame.length), "UTF-8");
        }catch (Exception e){
        }

        return new StationInfo(latitude, longitude, name, rssi, numEntrances, entrances, System.currentTimeMillis());
    }

    public void updateTimers(){
        for(Entrance entrance : entrances){
            entrance.updateLightTimer();
        }
    }
    public void flipTimers(int time){
        for(Entrance entrance : entrances){
            entrance.flipLightTimer(time);
        }
    }
}
