@echo off
setlocal
chcp 65001 > nul
title 无尽冬日 Whiteout Survival Bot 挂机脚本

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

pushd "%APP_DIR%"
set "APP_JAR="
for %%F in (frostguard-desktop-*.jar) do set "APP_JAR=%%F"
if not defined APP_JAR (
    echo 未找到 frostguard-desktop JAR，请先完成项目构建。
    popd
    pause
    exit /b 1
)

java --enable-native-access=ALL-UNNAMED -cp "%APP_JAR%;lib/*" dev.frostguard.app.bootstrap.Main
set "EXIT_CODE=%ERRORLEVEL%"
popd

pause
exit /b %EXIT_CODE%
