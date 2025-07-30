@echo off
echo Building Simple AirFire APK...

cd airfire-android

REM Create a temporary simple project structure
echo Creating simple build...

REM Compile just the SimpleAirFireActivity
javac -cp "C:\Users\%USERNAME%\AppData\Local\Android\Sdk\platforms\android-34\android.jar" ^
      -d temp\classes ^
      app\src\main\java\com\airfire\SimpleAirFireActivity.java

REM Package it using Android SDK tools
echo Packaging APK...

REM For now, let's use Android Studio
echo.
echo ===================================================
echo The project has dependency issues with old code.
echo Please use Android Studio to build:
echo.
echo 1. Open Android Studio
echo 2. File -^> New -^> New Project
echo 3. Choose "Empty Activity" 
echo 4. Name: AirFireSimple
echo 5. Package: com.airfire
echo 6. Copy SimpleAirFireActivity.java to the new project
echo 7. Copy activity_simple_airfire.xml to res/layout
echo 8. Build and run
echo ===================================================
pause