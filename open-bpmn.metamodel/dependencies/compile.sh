#!/bin/bash

# Directory containing JAR files
JAR_DIR="."

# Group ID, Artifact ID, and Version
GROUP_ID="com.discovery"
ARTIFACT_ID="discovery-bonita"
VERSION="1.0.0"

# Loop through all JAR files in the directory
for JAR_FILE in "$JAR_DIR"/*.jar; 
do
  # Extract the base name of the file (without extension)
  BASE_NAME=$(basename "$JAR_FILE" .jar)
  
  # Install the JAR file using Maven Install Plugin
  mvn install:install-file \
    -Dfile="$JAR_FILE" \
    -DgroupId="$GROUP_ID" \
    -DartifactId="$ARTIFACT_ID-$BASE_NAME" \
    -Dversion="$VERSION" \
    -Dpackaging=jar
done
