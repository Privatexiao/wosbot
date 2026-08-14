@echo off
chcp 65001 > nul
title 无尽冬日 Whiteout Survival Bot 挂机脚本

echo ========================================================
echo     正在启动 Whiteout Survival Bot 2.1.0 自动化助手...
echo ========================================================

set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "E:\MeComputer\Desktop\wosbot\packaging\desktop\target\input"

java --enable-native-access=ALL-UNNAMED -cp "frostguard-desktop-2.1.0.jar;lib/*" dev.frostguard.app.bootstrap.Main

pause
