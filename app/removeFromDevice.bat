@echo off
rem *********************************************************
rem *
rem * Uninstall build from device connected to USB port.
rem *
rem *********************************************************

setlocal
set apkPath=com.glandorf1.joe.wsprnetviewer.app
set adbpath=C:\Program Files (x86)\Android\android-studio\sdk\platform-tools\
rem New path as of Android Studio 0.8.14:
if not exist "%adbpath%adb.exe" set adbpath=C:\Program Files (x86)\Android\sdk\platform-tools\

rem uninstall
set cmd="%adbpath%adb.exe" -d uninstall "%apkPath%"
echo %cmd%
%cmd%
set installCode=%errorlevel%
echo Install return code=%installCode%
pause
endlocal