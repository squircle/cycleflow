#!/usr/bin/env python3

import sys

print("Usage: latlong.py latitude longitude")

LATLONG_INT_OFFSET = 8388540

LATITUDE_TO_INT_FACTOR = 93206
LATITUDE_TO_FLOAT_FACTOR = float.fromhex("0x1.6800bf40659a3p-17")

LONGITUDE_TO_INT_FACTOR = 46603
LONGITUDE_TO_FLOAT_FACTOR = float.fromhex("0x1.6800bf40659a3p-16")

latitude = float(sys.argv[1])
longitude = float(sys.argv[2])

i_latitude = round(latitude * LATITUDE_TO_INT_FACTOR) + LATLONG_INT_OFFSET
i_longitude = round(longitude * LONGITUDE_TO_INT_FACTOR) + LATLONG_INT_OFFSET

print("Input: {}, {}".format(latitude, longitude))
print("Output: {} ({}), {} ({})".format(i_latitude, hex(i_latitude), i_longitude, hex(i_longitude)))
