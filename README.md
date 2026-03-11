# 13SSH

Android tablet SSH client with a tabbed workspace, reusable machine profiles, SSH key management, and startup groups that can open several sessions at launch.

## Build

This repository is configured to build with repo-local tooling under `.tools/`.

### One-time local toolchain setup

Install a local JDK 17, Gradle bootstrap, and the Android SDK pieces required by the app:

```bash
mkdir -p .tools
curl -L -o .tools/temurin17.tar.gz "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.14%2B7/OpenJDK17U-jdk_x64_linux_hotspot_17.0.14_7.tar.gz"
tar -xzf .tools/temurin17.tar.gz -C .tools

curl -L -o .tools/gradle-8.9-bin.zip "https://services.gradle.org/distributions/gradle-8.9-bin.zip"
unzip -q .tools/gradle-8.9-bin.zip -d .tools

JAVA_HOME="$(pwd)/.tools/jdk-17.0.14+7" PATH="$(pwd)/.tools/jdk-17.0.14+7/bin:$(pwd)/.tools/gradle-8.9/bin:$PATH" .tools/gradle-8.9/bin/gradle wrapper

curl -L -o .tools/commandlinetools-linux.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
mkdir -p .tools/android-sdk/cmdline-tools
unzip -q .tools/commandlinetools-linux.zip -d .tools/android-sdk/cmdline-tools
mv .tools/android-sdk/cmdline-tools/cmdline-tools .tools/android-sdk/cmdline-tools/latest

yes | JAVA_HOME="$(pwd)/.tools/jdk-17.0.14+7" PATH="$(pwd)/.tools/jdk-17.0.14+7/bin:$PATH" .tools/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root="$(pwd)/.tools/android-sdk" "platform-tools" "platforms;android-34" "platforms;android-35" "build-tools;34.0.0" "build-tools;35.0.0"

printf "sdk.dir=%s/.tools/android-sdk\n" "$(pwd)" > local.properties
```

### Build the debug APK

```bash
JAVA_HOME="$(pwd)/.tools/jdk-17.0.14+7" \
ANDROID_HOME="$(pwd)/.tools/android-sdk" \
ANDROID_SDK_ROOT="$(pwd)/.tools/android-sdk" \
PATH="$(pwd)/.tools/jdk-17.0.14+7/bin:$(pwd)/.tools/android-sdk/platform-tools:$PATH" \
./gradlew assembleDebug
```

APK output:

```bash
app/build/outputs/apk/debug/app-debug.apk
```

### Useful commands

```bash
JAVA_HOME="$(pwd)/.tools/jdk-17.0.14+7" ANDROID_HOME="$(pwd)/.tools/android-sdk" ANDROID_SDK_ROOT="$(pwd)/.tools/android-sdk" PATH="$(pwd)/.tools/jdk-17.0.14+7/bin:$(pwd)/.tools/android-sdk/platform-tools:$PATH" ./gradlew test

JAVA_HOME="$(pwd)/.tools/jdk-17.0.14+7" ANDROID_HOME="$(pwd)/.tools/android-sdk" ANDROID_SDK_ROOT="$(pwd)/.tools/android-sdk" PATH="$(pwd)/.tools/jdk-17.0.14+7/bin:$(pwd)/.tools/android-sdk/platform-tools:$PATH" ./gradlew lint
```

## APK Download Service

The repo includes `scripts/serve-debug-apk.sh` and a user service at `~/.config/systemd/user/13ssh-apk.service`.

Manage it with:

```bash
systemctl --user enable --now 13ssh-apk.service
systemctl --user status 13ssh-apk.service
systemctl --user restart 13ssh-apk.service
systemctl --user stop 13ssh-apk.service
```

Default download URL on the local network:

```bash
http://<your-host-ip>:8181/app-debug.apk
```
