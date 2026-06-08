@echo off
chcp 65001 >nul
title 仓库货物出入库管理系统

echo ============================================
echo     仓库货物出入库管理系统 v1.0.0
echo ============================================
echo.

:: 检查服务是否已在运行
curl -s -o NUL http://localhost:8080 2>nul
if %errorlevel% == 0 (
    echo [提示] 服务已在运行中，直接打开浏览器...
    start "" http://localhost:8080
    echo.
    echo 浏览器已打开: http://localhost:8080
    echo.
    pause
    exit /b 0
)

:: 如果端口被占用 (可能上次未正常退出)
netstat -ano 2>nul | findstr ":8080.*LISTENING" >nul
if %errorlevel% == 0 (
    echo [提示] 端口 8080 被占用，正在释放...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING"') do (
        taskkill /f /pid %%a >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
)

echo [1/3] 正在启动后端服务...

:: 后台启动 Java 服务
start /B "" javaw -jar "%~dp0ims.jar" >nul 2>&1

:: 等待服务就绪
echo [2/3] 等待服务就绪...
set /a count=0
:wait_loop
timeout /t 1 /nobreak >nul
set /a count+=1

curl -s -o NUL http://localhost:8080 2>nul
if %errorlevel% == 0 goto ready
if %count% LSS 30 goto wait_loop

echo [错误] 服务启动超时！
echo 请确认: 1^) 已安装 Java 17+  2^) ims.jar 文件存在
echo.
pause
exit /b 1

:ready
echo [3/3] 正在打开浏览器...
start "" http://localhost:8080

echo.
echo ============================================
echo  启动完成！浏览器已自动打开。
echo  访问地址: http://localhost:8080
echo ============================================
echo.
echo 按任意键停止服务并退出...
pause >nul

:: 停止后台 Java 进程
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING"') do (
    taskkill /f /pid %%a >nul 2>&1
)
echo 服务已停止。
