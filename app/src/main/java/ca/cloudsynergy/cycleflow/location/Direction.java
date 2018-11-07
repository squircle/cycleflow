package ca.cloudsynergy.cycleflow.location;

public enum Direction {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    private Direction opposite;

    static {
        NORTH.opposite = SOUTH;
        EAST.opposite = WEST;
        SOUTH.opposite = NORTH;
        WEST.opposite = EAST;
    }

    public Direction getOppositeDirection() {
        return  opposite;
    }

    public static Direction directionFromBearing(long bearing) {

        return NORTH;
    }
}
