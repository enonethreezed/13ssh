#!/bin/sh
set -eu

REPO_DIR="/home/nth/Proyectos/13ssh"
APK_DIR="$REPO_DIR/app/build/outputs/apk/debug"
PORT="8181"

cd "$APK_DIR"
exec python3 -m http.server "$PORT" --bind 0.0.0.0
