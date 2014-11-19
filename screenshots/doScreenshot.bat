@echo off
setlocal
set fname=screen1.png
set adbPath=C:\Program Files (x86)\Android\android-studio\sdk\platform-tools
"%adbPath%\adb.exe" shell screencap -p
"%adbPath%\adb.exe" pull /sdcard/%fname%
endlocal
pause