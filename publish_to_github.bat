@echo off
title Publish NoRisk Cosmetics Unlocker to GitHub
cd /d "%~dp0"

echo ===================================================
echo   NoRisk Cosmetics Unlocker - GitHub Release Publisher
echo   (Kein Source-Code Leak - Nur Readme, Icon ^& JAR)
echo ===================================================
echo.

git --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Git ist nicht installiert oder nicht im PATH!
    pause
    exit /b
)

if not exist .git (
    echo [*] Initialisiere Git Repository...
    git init
    git branch -M main
)

git config user.name "mulareal"
git config user.email "mulareal@users.noreply.github.com"

echo [*] Fuege Release-Dateien hinzu...
git add README.md LICENSE icon.png .gitignore releases/

echo [*] Erstelle Commit...
git commit -m "Release v1.0.3 - NoRisk Cosmetics Unlocker"

echo.
set /p REPO_URL="Gib deine GitHub Repo URL ein (z.B. https://github.com/DEIN_NAME/norisk-cosmetics-unlocker.git): "

if "%REPO_URL%"=="" (
    set REPO_URL=https://github.com/mulareal/norisk-cosmetics-unlocker.git
)

git remote remove origin >nul 2>&1
git remote add origin %REPO_URL%

echo [*] Pushe zu GitHub...
git push -u origin main --force

echo.
echo ===================================================
echo   ERFOLGREICH ZU GITHUB HOCHGELADEN!
echo ===================================================
pause
