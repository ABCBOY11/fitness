# Updating this app

- User preference: deliver the APK for the user to test. Do not connect to or operate a physical device, run device tests, install/update the app on a device, or capture screenshots unless the user explicitly requests it again. No screenshot evidence is needed.
- Default verification is local compilation, relevant unit tests and static checks (Lint). Clearly distinguish local checks from device testing in delivery notes.
- Preserve the user's installed `com.zzz.fitness` app and all its local data.
- Never uninstall or clear data for that package during builds, tests or updates. Never use `uninstallAll` on a user's device.
- Instrumentation tests must target the separate `com.zzz.fitness.uitest` build: `:app:connectedUiTestAndroidTest`. Keep `testBuildType "uiTest"` and its application ID suffix.
- Only if the user explicitly requests device work again: before device tests, back up the formal app using `node tools/safe_upgrade.mjs <adb.exe> --backup-only`.
- Only if the user explicitly requests device deployment again: deploy with `node tools/safe_upgrade.mjs <adb.exe> <apk>`. It backs up, uses `adb install -r`, and verifies durable data hashes before launch. If replacement fails, stop; never fall back to uninstall/reinstall.
- Keep the formal application ID, signing identity, database name and existing migration data. Increment versionCode for each delivered update.
- Completion toggles must update existing views without rebuilding the page. Cover repeated toggles, bulk completion, progress and scroll stability in the UI regression.
