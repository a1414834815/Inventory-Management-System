@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
title IMS v1.0.0

:: ============================================
:: Find Java
:: ============================================
set JAVA_PATH=
set JAVAW_PATH=

if exist "%JAVA_HOME%\bin\javaw.exe" (
    set JAVAW_PATH=%JAVA_HOME%\bin\javaw.exe
    set JAVA_PATH=%JAVA_HOME%\bin\java.exe
    goto :found_java
)

for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk*") do (
    if exist "%%d\bin\javaw.exe" (
        set JAVAW_PATH=%%d\bin\javaw.exe
        set JAVA_PATH=%%d\bin\java.exe
        goto :found_java
    )
)

for /d %%d in ("C:\Program Files\Java\jdk*") do (
    if exist "%%d\bin\javaw.exe" (
        set JAVAW_PATH=%%d\bin\javaw.exe
        set JAVA_PATH=%%d\bin\java.exe
        goto :found_java
    )
)

where javaw >nul 2>&1
if %errorlevel% == 0 (
    for /f "delims=" %%f in ('where javaw 2^>nul') do set JAVAW_PATH=%%f
    for /f "delims=" %%f in ('where java 2^>nul') do set JAVA_PATH=%%f
    goto :found_java
)

echo ============================================
echo   [ERROR] Java not found!
echo ============================================
echo.
echo Please install Java 17 or newer from:
echo   https://adoptium.net/download/
echo.
pause
exit /b 1

:found_java

:: ============================================
:: Check ims.jar
:: ============================================
if not exist "%~dp0ims.jar" (
    echo [ERROR] ims.jar not found!
    pause
    exit /b 1
)

:: ============================================
:: Check if already running
:: ============================================
netstat -ano 2>nul | findstr ":8080 " | findstr "LISTENING" >nul
if %errorlevel% == 0 (
    echo ============================================
    echo   Server is already running!
    echo   Opening browser...
    echo ============================================
    start "" http://localhost:8080
    timeout /t 5 /nobreak >nul
    exit /b 0
)

:: ============================================
:: Start server
:: ============================================
echo ============================================
echo   IMS v1.0.0 - Starting...
echo ============================================
echo.
echo Java: !JAVA_PATH!
echo Starting server, please wait...

:: javaw runs without console, returns immediately
"!JAVAW_PATH!" -jar "%~dp0ims.jar" > "%~dp0server.log" 2>&1

:: ============================================
:: Wait for port 8080 to be ready
:: ============================================
echo Waiting for server to be ready...
set /a COUNT=0

:wait_loop
ping -n 2 127.0.0.1 >nul
set /a COUNT+=1

netstat -ano 2>nul | findstr ":8080 " | findstr "LISTENING" >nul
if %errorlevel% == 0 goto :server_ready

if %COUNT% LSS 30 goto :wait_loop

:: ============================================
:: Timeout - show log
:: ============================================
echo.
echo ============================================
echo   [ERROR] Server failed to start!
echo ============================================
echo.
echo Server log (%~dp0server.log):
echo ----------------------------------------
type "%~dp0server.log" 2>nul
echo ----------------------------------------
echo.
pause
exit /b 1

:: ============================================
:: Success - open browser
:: ============================================
:server_ready
echo Server is ready!
echo Opening browser...
start "" http://localhost:8080

echo.
echo ============================================
echo   IMS is running!
echo   Address: http://localhost:8080
echo.
echo   Press any key to stop the server.
echo ============================================
echo.
pause >nul

:: ============================================
:: Stop server
:: ============================================
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING" 2^>nul') do (
    taskkill /f /pid %%a >nul 2>&1
)
echo Server stopped.
