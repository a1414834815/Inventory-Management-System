@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
title IMS - Inventory Management System

:: Find Java
set JAVA_PATH=
set JAVAW_PATH=

:: Check JAVA_HOME first
if exist "%JAVA_HOME%\bin\javaw.exe" (
    set JAVAW_PATH=%JAVA_HOME%\bin\javaw.exe
    set JAVA_PATH=%JAVA_HOME%\bin\java.exe
)

:: Search common locations
if not defined JAVAW_PATH (
    for /f "delims=" %%f in ('dir /s /b "C:\Program Files\Eclipse Adoptium\jdk*" 2^>nul ^| findstr "\\bin\\javaw.exe$"') do (
        set JAVAW_PATH=%%f
        set JAVA_PATH=%%f
        set JAVA_PATH=!JAVA_PATH:javaw.exe=java.exe!
        goto :found_java
    )
)

if not defined JAVAW_PATH (
    for /f "delims=" %%f in ('dir /s /b "C:\Program Files\Java\jdk*" 2^>nul ^| findstr "\\bin\\javaw.exe$"') do (
        set JAVAW_PATH=%%f
        set JAVA_PATH=%%f
        set JAVA_PATH=!JAVA_PATH:javaw.exe=java.exe!
        goto :found_java
    )
)

:: Try PATH
if not defined JAVAW_PATH (
    where javaw >nul 2>&1
    if !errorlevel! == 0 (
        for /f "delims=" %%f in ('where javaw 2^>nul') do set JAVAW_PATH=%%f
        for /f "delims=" %%f in ('where java 2^>nul') do set JAVA_PATH=%%f
        goto :found_java
    )
)

:: Not found
echo ============================================
echo   ERROR: Java not found!
echo ============================================
echo.
echo Please install Java 17 or newer:
echo   https://adoptium.net/download/
echo.
echo After installation, restart your computer
echo and run this script again.
echo.
pause
exit /b 1

:found_java
echo ============================================
echo   IMS v1.0.0 - Starting...
echo ============================================
echo.
echo Java found: !JAVA_PATH!

:: Check jar exists
if not exist "%~dp0ims.jar" (
    echo ERROR: ims.jar not found in current folder!
    pause
    exit /b 1
)

:: Check if already running
powershell -NoProfile -Command "try{$r=Invoke-WebRequest 'http://localhost:8080' -UseBasicParsing -TimeoutSec 2;exit 0}catch{exit 1}" >nul 2>&1
if !errorlevel! == 0 (
    echo.
    echo Server is already running!
    echo Opening browser...
    start "" http://localhost:8080
    pause
    exit /b 0
)

:: Kill stale process on port 8080
for /f "tokens=5" %%a in ('netstat -ano ^| findstr /c:":8080 " ^| findstr /c:"LISTENING" 2^>nul') do (
    echo Releasing port 8080 (PID: %%a)...
    taskkill /f /pid %%a >nul 2>&1
    ping -n 3 127.0.0.1 >nul
)

:: Start server in background
echo Starting server...
start /B "" "!JAVAW_PATH!" -jar "%~dp0ims.jar" > "%~dp0server.log" 2>&1

:: Wait for server
echo Waiting for server to be ready...
set /a WAIT_COUNT=0
:wait
ping -n 2 127.0.0.1 >nul
set /a WAIT_COUNT+=1

powershell -NoProfile -Command "try{$r=Invoke-WebRequest 'http://localhost:8080' -UseBasicParsing -TimeoutSec 2;exit 0}catch{exit 1}" >nul 2>&1
if !errorlevel! == 0 (
    echo Server is ready!
    goto :open_browser
)

if !WAIT_COUNT! LSS 20 (
    echo   Waiting... (!WAIT_COUNT!/20^)
    goto :wait
)

:: Timeout
echo.
echo ============================================
echo   ERROR: Server failed to start!
echo ============================================
echo.
echo Server log:
type "%~dp0server.log" 2>nul
echo.
pause
exit /b 1

:open_browser
echo Opening browser: http://localhost:8080
start "" http://localhost:8080

echo.
echo ============================================
echo   IMS is running!
echo   Address: http://localhost:8080
echo   Close this window to stop the server.
echo ============================================
echo.
pause >nul

:: Cleanup
for /f "tokens=5" %%a in ('netstat -ano ^| findstr /c:":8080 " ^| findstr /c:"LISTENING" 2^>nul') do (
    taskkill /f /pid %%a >nul 2>&1
)
echo Server stopped.
