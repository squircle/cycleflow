#!/usr/bin/env sh

INSTALLER="nrfgostudio_win-64_1.21.2_installer.msi"

msiexec /i ./${INSTALLER}

echo "Found nRFgoStudio.exe at:"

find ~/.wine -name nRFgoStudio.exe
