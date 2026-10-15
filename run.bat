@echo off
title Simulador Time-Sharing
cd /d "%~dp0"
if not exist out mkdir out
echo Compilando...
javac -encoding UTF-8 -d out src\*.java
if %errorlevel% neq 0 (
    echo [ERROR] Hubo un problema al compilar.
    pause
    exit /b 1
)
echo Ejecutando...
java -cp out SimuladorGUI
