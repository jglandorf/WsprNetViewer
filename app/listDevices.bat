@echo off
setlocal
set adbPath=C:\Program Files (x86)\Android\android-studio\sdk\platform-tools
"%adbPath%\adb.exe" devices -l
endlocal
pause