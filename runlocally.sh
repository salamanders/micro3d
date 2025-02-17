#!/bin/bash

PROJECT_DIR="/home/pi/my-kotlin-app"
CURRENT_COMMIT=$(git -C "$PROJECT_DIR" rev-parse HEAD) # Get current commit hash

cd "$PROJECT_DIR" || exit
git pull origin main  # Or your branch name

NEW_COMMIT=$(git -C "$PROJECT_DIR" rev-parse HEAD)  # Get new commit hash

if [ "$CURRENT_COMMIT" != "$NEW_COMMIT" ]; then # Compare commit hashes
  echo "New commit found. Deploying..."

  # Stop the current application (replace with your actual stop command)
  pkill -f "java -jar my-kotlin-app.jar" # Example

  # Build the project
  ./gradlew build

  # Run the application
  java -jar "$PROJECT_DIR/build/libs/my-kotlin-app-*.jar" &

else
  echo "No new commit found. Skipping deployment."
fi
