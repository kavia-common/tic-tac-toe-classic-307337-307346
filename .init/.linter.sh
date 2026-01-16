#!/bin/bash
cd /home/kavia/workspace/code-generation/tic-tac-toe-classic-307337-307346/android_frontend
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

