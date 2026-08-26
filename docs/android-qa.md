# Android QA Workflows

## Stateless workflow

`connectedDebugAndroidTest` runs the instrumented suite through the Gradle
connected-device workflow. In the validated OPPO environment it removes the
target package when the task finishes, which removes the target app data as
well. Do not use it after creating a session or pairing state that must survive.

## Stateful workflow

Use `scripts/run-stateful-android-tests.ps1` to build the APKs, install the
target, install only the androidTest APK with `adb install -r`, and run the
suite directly with `adb shell am instrument`. The script verifies that the
target package remains installed after instrumentation.

```powershell
.\scripts\run-stateful-android-tests.ps1
.\scripts\run-stateful-android-tests.ps1 -Serial <serial>
.\scripts\run-stateful-android-tests.ps1 -Serial <serial> -VerifyDataPreservation
.\scripts\run-stateful-android-tests.ps1 -Serial <serial> -VerifyDataPreservation -CleanupTestPackage
```

The script never runs `connectedDebugAndroidTest`, `pm clear`, or target
uninstall. The optional sentinel is an isolated file under the target sandbox;
it does not use DataStore, SecureTokenStore, Room, auth, or personal data.
