@echo off
title 仓库货物出入库管理系统 v1.0.0

echo ============================================
echo     仓库货物出入库管理系统 v1.0.0
echo ============================================
echo.

:: ============================================
:: 1. 查找 Java
:: ============================================
set JAVA_EXE=
set JAVAW_EXE=

:: 尝试从 JAVA_HOME 环境变量获取
if exist "%JAVA_HOME%\bin\javaw.exe" (
    set JAVAW_EXE=%JAVA_HOME%\bin\javaw.exe
    set JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

:: 尝试从 PATH 获取
if not defined JAVAW_EXE (
    for %%i in (javaw.exe) do set JAVAW_EXE=%%~$PATH:i
)
if not defined JAVA_EXE (
    for %%i in (java.exe) do set JAVA_EXE=%%~$PATH:i
)

:: 自动搜索常见安装位置
if not defined JAVAW_EXE (
    for /d %%d in ("C:\Program Files\Eclipse Adoptium\*") do (
        if exist "%%d\bin\javaw.exe" (
            set JAVAW_EXE=%%d\bin\javaw.exe
            set JAVA_EXE=%%d\bin\java.exe
        )
    )
)
if not defined JAVAW_EXE (
    for /d %%d in ("C:\Program Files\Java\*") do (
        if exist "%%d\bin\javaw.exe" (
            set JAVAW_EXE=%%d\bin\javaw.exe
            set JAVA_EXE=%%d\bin\java.exe
        )
    )
)

if not defined JAVAW_EXE (
    echo [错误] 未找到 Java 运行环境！
    echo.
    echo 请安装 Java 17 或更高版本：
    echo https://adoptium.net/download/
    echo.
    echo 安装后重新运行此脚本。
    echo.
    pause
    exit /b 1
)

echo [Java] %JAVA_EXE%

:: ============================================
:: 2. 检查 ims.jar 是否存在
:: ============================================
if not exist "%~dp0ims.jar" (
    echo [错误] 未找到 ims.jar 文件！
    echo 请确保 ims.jar 与本脚本在同一目录。
    pause
    exit /b 1
)

:: ============================================
:: 3. 检查服务是否已在运行
:: ============================================
powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:8080' -UseBasicParsing -TimeoutSec 2; exit 0 } catch { exit 1 }" >nul 2>&1
if %errorlevel% == 0 (
    echo [提示] 服务已在运行中，直接打开浏览器...
    start "" http://localhost:8080
    echo 浏览器已打开: http://localhost:8080
    pause
    exit /b 0
)

:: ============================================
:: 4. 释放已占用的端口
:: ============================================
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING" 2^>nul') do (
    echo [提示] 端口 8080 被占用 (PID: %%a)，正在释放...
    taskkill /f /pid %%a >nul 2>&1
    timeout /t 2 /nobreak >nul
)

:: ============================================
:: 5. 启动后端服务
:: ============================================
echo [启动] 正在启动后端服务...
start /B "" "%JAVAW_EXE%" -jar "%~dp0ims.jar" >nul 2>&1

:: ============================================
:: 6. 等待服务就绪
:: ============================================
echo [等待] 等待服务就绪...
set /a count=0
:wait_loop
timeout /t 1 /nobreak >nul
set /a count+=1

powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:8080' -UseBasicParsing -TimeoutSec 2; exit 0 } catch { exit 1 }" >nul 2>&1
if %errorlevel% == 0 goto ready
if %count% LSS 30 goto wait_loop

echo [错误] 服务启动超时 (已等待 30 秒)
echo 请确认 Java 版本为 17 或更高版本。
"%JAVA_EXE%" -version
pause
exit /b 1

:: ============================================
:: 7. 打开浏览器
:: ============================================
:ready
echo [完成] 正在打开浏览器...
start "" http://localhost:8080

echo.
echo ============================================
echo  系统已启动！访问地址: http://localhost:8080
echo  关闭此窗口将停止服务。
echo ============================================
echo.
pause >nul

:: ============================================
:: 8. 停止服务
:: ============================================
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING" 2^>nul') do (
    taskkill /f /pid %%a >nul 2>&1
)
echo 服务已停止。
