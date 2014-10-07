@echo off
rem *********************************************************
rem *
rem * Install release build to device connected to USB port.
rem *
rem *********************************************************

setlocal
set apkPath=%~dp0
set adbpath=C:\Program Files (x86)\Android\android-studio\sdk\platform-tools\

rem install
set cmd="%adbpath%adb.exe" -d install   "%apkPath%app-release.apk"
echo %cmd%
%cmd%
set installCode=%errorlevel%
echo Install return code=%installCode%
pause
endlocal