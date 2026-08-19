@echo off
setlocal
chcp 65001 > nul
title Whiteout Survival Bot 挂机脚本

echo ========================================================
echo     正在启动 Whiteout Survival Bot 自动化助手...
echo ========================================================

set "APP_DIR=%~dp0packaging\desktop\target\input"
if not exist "%APP_DIR%" (
    echo 未找到桌面构建目录：%APP_DIR%
    echo 请先完成项目构建。
    pause
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo Java not found. Install Java 21 or newer and add it to PATH.
    pause
    exit /b 1
)

set "JAVA_VERSION="
for /f "tokens=3" %%V in ('java -version 2^>^&1 ^| findstr /i "version"') do if not defined JAVA_VERSION set "JAVA_VERSION=%%~V"
for /f "tokens=1 delims=." %%M in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%M"
if not defined JAVA_MAJOR (
    echo Unable to determine the Java version. Java 21 or newer is required.
    pause
    exit /b 1
)
if %JAVA_MAJOR% LSS 21 (
    echo Java %JAVA_VERSION% is too old. Java 21 or newer is required.
    pause
    exit /b 1
)

pushd "%APP_DIR%"
set "APP_JAR="
for /f "delims=" %%F in ('dir /b /a-d /o-d "frostguard-desktop-*.jar" 2^>nul') do if not defined APP_JAR set "APP_JAR=%%F"
if not defined APP_JAR (
    echo 未找到 frostguard-desktop JAR，请先完成项目构建。
    popd
    pause
    exit /b 1
)

java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 --enable-native-access=ALL-UNNAMED -cp "%APP_JAR%;lib/*" dev.frostguard.app.bootstrap.Main
set "EXIT_CODE=%ERRORLEVEL%"
popd

pause
exit /b %EXIT_CODE%
