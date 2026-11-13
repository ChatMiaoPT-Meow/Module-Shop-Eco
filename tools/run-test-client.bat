@echo off
title ModShopEco-TestClient
cd /d "%~dp0.."
call gradlew.bat runClient --console=plain
echo.
echo === client exited (code %errorlevel%) ===
pause
