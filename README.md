# CycleFlow

CycleFlow is a project of Andrew Dam, Mitchell Kovacs, Noah Kruiper, and
Tyson Moore for the CEG 4912/4913 Capstone design project courses at the
University of Ottawa. 

CycleFlow encourages the efficient use of active transportation in cities by
transmitting traffic signal timing information to cyclists. This allows them to
conserve momentum and reduces effort to safely glide through urban corridors.

This repository consists of two components: a sample base station implementation
using Nordic Semiconductor's nRF52-DK development kit, and a sample client
implementation using Android. A description of the protocol can be found in
the integration guide (in the `doc/` directory).

More information on each project can be found in the `README.md` in the
respective directories.

## CycleFlow Android Client

The CycleFlow Android client (in `cycleflow-android/`) is a proof-of-concept
Android application implementing the CycleFlow protocol. It can use real or
simulated location data to decode and display information from CycleFlow base
stations. 

## CycleCaster

The CycleCaster is a CycleFlow base station using the nRF52 series Bluetooth
SoC. It can run internal timing demonstrations with static data, and is designed
to be integrated with traffic signal control systems.
