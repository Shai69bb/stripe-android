#!/usr/bin/env bash

# Uninstall existing app on cached AVD.
adb uninstall com.stripe.android.financialconnections.example || true

# Create folder for reports
mkdir -p financial-connections-example/build/reports

# create log file and allow writes.
rm financial-connections-example/build/reports/emulator.log
touch financial-connections-example/build/reports/emulator.log
chmod 777 financial-connections-example/build/reports/emulator.log

# Compile and install APK.
./gradlew -PSTRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL=$STRIPE_FINANCIAL_CONNECTIONS_EXAMPLE_BACKEND_URL :financial-connections-example:installDebug

# Clear logs
adb logcat -c

# Start screen record
adb shell screenrecord --bit-rate '100000' /sdcard/emulator-screenrecording.mp4 &
childpid=$!

# Start collecting logs
adb logcat >> financial-connections-example/build/reports/emulator.log &
maestro test --format junit maestro/financial-connections
result=$?

# Stop video recording and pull record.
kill "$childpid"
wait "$childpid"
cd financial-connections-example/build/reports/ && adb pull /sdcard/emulator-screenrecording.mp4 || true

#echo "TEST $?"
exit "$result"



