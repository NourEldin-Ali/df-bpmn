@echo off
setlocal enabledelayedexpansion

REM Set the directory containing JAR files
set JAR_DIR=.

REM Set Maven coordinates
set GROUP_ID=com.discovery
set ARTIFACT_ID=discovery
set VERSION=1.0.0

REM Loop through all JAR files in the directory
for %%f in ("%JAR_DIR%\*.jar") do (
    REM Extract the base name of the file (without extension)
    set "BASE_NAME=%%~nf"
    
    REM Install the JAR file using Maven Install Plugin
    mvn install:install-file ^
        -Dfile="%%f" ^
        -DgroupId=%GROUP_ID% ^
        -DartifactId=%ARTIFACT_ID%-%%~nf ^
        -Dversion=%VERSION% ^
        -Dpackaging=jar
)

endlocal
