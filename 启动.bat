@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
title IMS v1.0.0

:: Find Java
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
echo Please install Java 17:
echo   https://adoptium.net/download/
echo.
pause
exit /b 1

:found_java

:: Check ims.jar
if not exist "%~dp0ims.jar" (
    echo [ERROR] ims.jar not found!
    pause
    exit /b 1
)

:: Check if already running
netstat -ano 2>nul | findstr ":8080 " | findstr "LISTENING" >nul
if %errorlevel% == 0 (
    echo Server is already running!
    echo Opening browser...
    start "" http://localhost:8080
    timeout /t 3 /nobreak >nul
    exit /b 0
)

echo ============================================
echo   IMS v1.0.0
echo ============================================
echo.
echo Java: !JAVA_PATH!
echo Starting server...

:: Use java (not javaw) so we can see startup in the window
:: Use start /min to minimize the server console
start "IMS Server" /min "!JAVA_PATH!" -jar "%~dp0ims.jar"

:: Wait for server to start (Spring Boot takes ~5 seconds)
echo Waiting for server to start...
timeout /t 6 /nobreak >nul

:: Check if server started successfully
netstat -ano 2>nul | findstr ":8080 " | findstr "LISTENING" >nul
if %errorlevel% == 0 (
    echo Server is ready!
    goto :open
)

:: Maybe needs more time
echo Still waiting...
timeout /t 5 /nobreak >nul
netstat -ano 2>nul | findstr ":8080 " | findstr "LISTENING" >nul
if %errorlevel% == 0 (
    echo Server is ready!
    goto :open
)

:: Failed
echo.
echo ============================================
echo   [ERROR] Server failed to start!
echo ============================================
echo.
echo Possible issues:
echo   1. Port 8080 is occupied
echo   2. Java version too old (need 17+)
echo   3. ims.jar file is corrupted
echo.
echo Check server window for details.
echo.
pause
exit /b 1

:open
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

:: Stop server
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING" 2^>nul') do (
    taskkill /f /pid %%a >nul 2>&1
)
echo Server stopped.
