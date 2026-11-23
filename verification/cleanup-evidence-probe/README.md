# Isolated Android evidence-store verification

This standalone app compiles the production `CleanupEvidenceRecord`, `CleanupEvidenceSession`,
and `CleanupEvidenceStore` directly. Package `com.photoplace.evidenceprobe` has no media
permissions or launcher. It writes synthetic URI strings only to its own private SQLite database.
It never opens the installed `com.photoplace.app` database or photo library.

From repository root, set `ANDROID_HOME` to the local SDK and use:

```powershell
.\gradlew.bat -p verification/cleanup-evidence-probe assembleDebug --console=plain
adb install -r verification/cleanup-evidence-probe/build/outputs/apk/debug/cleanup-evidence-probe-debug.apk
adb shell am instrument -w com.photoplace.evidenceprobe/com.example.gallerysorter.CleanupEvidenceProbe
adb shell am instrument -w -e mode crash com.photoplace.evidenceprobe/com.example.gallerysorter.CleanupEvidenceProbe
adb shell am instrument -w -e mode audit com.photoplace.evidenceprobe/com.example.gallerysorter.CleanupEvidenceProbe
adb uninstall com.photoplace.evidenceprobe
```

`crash` intentionally kills only the probe process with an open transaction and buffered tail;
its instrumentation error is expected. `audit` must then pass with exactly 50 committed records.
The main run checks transaction rollback, replay, bounded pagination, close/reopen, write rejection
and a 3,000-observation checkpoint timing sample. That sample measures ledger work, not real
photo copy/search performance. Actual copy integration and throughput still need user-device testing.
