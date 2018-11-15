package ca.cloudsynergy.cycleflow.location;

public enum Direction {
    NORTH ("North"),
    EAST ("East"),
    SOUTH ("South"),
    WEST ("West");

    private Direction opposite;
    private String name;

    static {
        NORTH.opposite = SOUTH;
        EAST.opposite = WEST;
        SOUTH.opposite = NORTH;
        WEST.opposite = EAST;
    }

    Direction (String name) {
        this.name = name;
    }

    public Direction getOppositeDirection() {
        return  opposite;
    }

    @Override
    public String toString() {
        return name;
    }

    // Just keeping it simple for now with only 4 directions.
    public static Direction directionFromBearing(Double bearing) {
        if ((bearing > 315 && bearing <= 380) || (bearing >= 0 && bearing <= 45)) {
            return NORTH;
        } else if (bearing > 45 && bearing <= 135) {
            return EAST;
        } else if (bearing > 135 && bearing <= 225) {
            return SOUTH;
        } else if (bearing > 225 && bearing <= 315) {
            return WEST;
        } else {
            return null;
        }
    }
}
