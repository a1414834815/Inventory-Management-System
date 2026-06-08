@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
title IMS v1.0.0

:: ============================================
:: Find Java
:: ============================================
set JAVA_PATH=
set JAVAW_PATH=

:: Method 1: Check JAVA_HOME
if exist "%JAVA_HOME%\bin\javaw.exe" (
    set JAVAW_PATH=%JAVA_HOME%\bin\javaw.exe
    set JAVA_PATH=%JAVA_HOME%\bin\java.exe
    goto :found_java
)

:: Method 2: Check Eclipse Adoptium (auto-find latest)
for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk*") do (
    if exist "%%d\bin\javaw.exe" (
        set JAVAW_PATH=%%d\bin\javaw.exe
        set JAVA_PATH=%%d\bin\java.exe
        goto :found_java
    )
)

:: Method 3: Check standard Java dir
for /d %%d in ("C:\Program Files\Java\jdk*") do (
    if exist "%%d\bin\javaw.exe" (
        set JAVAW_PATH=%%d\bin\javaw.exe
        set JAVA_PATH=%%d\bin\java.exe
        goto :found_java
    )
)

:: Method 4: Check Program Files (x86)
for /d %%d in ("C:\Program Files (x86)\Eclipse Adoptium\jdk*") do (
    if exist "%%d\bin\javaw.exe" (
        set JAVAW_PATH=%%d\bin\javaw.exe
        set JAVA_PATH=%%d\bin\java.exe
        goto :found_java
    )
)

:: Method 5: Try PATH
where javaw >nul 2>&1
if %errorlevel% == 0 (
    for /f "delims=" %%f in ('where javaw 2^>nul') do set JAVAW_PATH=%%f
    for /f "delims=" %%f in ('where java 2^>nul') do set JAVA_PATH=%%f
    goto :found_java
)

:: Not found -> show diagnostic info
echo ============================================
echo   [ERROR] Java not found!
echo ============================================
echo.
echo Checking common locations:
echo.
if exist "C:\Program Files\Eclipse Adoptium" (
    echo Eclipse Adoptium folder found. Contents:
    dir /b "C:\Program Files\Eclipse Adoptium" 2>nul
) else (
    echo Eclipse Adoptium folder NOT found.
)
echo.
if exist "C:\Program Files\Java" (
    echo Java folder found. Contents:
    dir /b "C:\Program Files\Java" 2>nul
) else (
    echo Java folder NOT found.
)
echo.
echo Please install Java 17:
echo   https://adoptium.net/download/
echo.
echo After installing, restart your computer
echo and run this script again.
echo.
pause
exit /b 1

:found_java
echo ============================================
echo   IMS v1.0.0 - Starting...
echo ============================================
echo.
echo Java: !JAVA_PATH!

:: Check ims.jar
if not exist "%~dp0ims.jar" (
    echo [ERROR] ims.jar not found!
    pause
    exit /b 1
)

:: Check if already running
powershell -NoProfile -Command "try{$r=Invoke-WebRequest 'http://localhost:8080' -UseBasicParsing -TimeoutSec 2;exit 0}catch{exit 1}" >nul 2>&1
if !errorlevel! == 0 (
    echo.
    echo [OK] Server already running!
    echo Opening browser...
    start "" http://localhost:8080
    pause
    exit /b 0
)

:: Kill any stale process on port 8080
for /f "tokens=5" %%a in ('netstat -ano ^| findstr /c:":8080 " ^| findstr /c:"LISTENING" 2^>nul') do (
    echo Releasing port 8080 (PID: %%a)...
    taskkill /f /pid %%a >nul 2>&1
    ping -n 3 127.0.0.1 >nul
)

:: Start server
echo Starting server...
start /B "" "!JAVAW_PATH!" -jar "%~dp0ims.jar" > "%~dp0server.log" 2>&1

:: Wait for server to be ready
echo Waiting for server...
set /a N=0
:wait
ping -n 2 127.0.0.1 >nul
set /a N+=1

powershell -NoProfile -Command "try{$r=Invoke-WebRequest 'http://localhost:8080' -UseBasicParsing -TimeoutSec 2;exit 0}catch{exit 1}" >nul 2>&1
if !errorlevel! == 0 (
    echo [OK] Server ready!
    goto :open_browser
)

if !N! LSS 20 (
    echo   Waiting (!N!/20^)
    goto :wait
)

:: Failed to start
echo.
echo ============================================
echo   [ERROR] Server failed to start!
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

:: Stop server
for /f "tokens=5" %%a in ('netstat -ano ^| findstr /c:":8080 " ^| findstr /c:"LISTENING" 2^>nul') do (
    taskkill /f /pid %%a >nul 2>&1
)
echo Server stopped.
