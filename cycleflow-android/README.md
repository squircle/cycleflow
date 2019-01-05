# CycleFlow Android Client
***
```
                              \_____/
                             / .   . \
                           _|_________|_
                          | |         | |
                          | |         | |
      .-``'      _o       '-|_________|-'
    .`   .`   _ \<_           | | | | 
_.-'     '.__(_)/(_)__________'-' '-'
```

Client component for CEG 4912/4913 Capstone Project at the University of Ottawa.

Developed in 2018 alongside the `cyclecaster` base station component.

## Description
* Simple CycleFlow-compatible app to serve as a proof-of-concept and potential example for alternative clients
* Simulate location data
* Decode and display CycleFlow broadcasts

## Screenshot
![Screenshot from an LG V20](https://i.imgur.com/dHqd5GV.png)

## Requirements
* Requires Android 5.0 or higher
* Requires Google Play Services
* Requires Location and Bluetooth permissions
* Requires Google Maps API key (not provided)
* For full experience, deployment of CycleFlow-compatible base stations is required.  

## How to build
* Import project into Android Studio
* Any missing Android or Google dependencies can then be automatically installed
* Add your own Maps API Key to the AndroidManifest.xml (Google provides these, if you need one)
* Build project
* Deploy on physical Android device for best results (tested on LG V20, Samsung A5 2017, Moto G4)
