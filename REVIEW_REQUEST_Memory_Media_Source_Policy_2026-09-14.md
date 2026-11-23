# PhotoPlace Memory Media Source Policy Review

## Review mode

Review the current working-tree diff read-only. Do not edit code, access or mutate device media, commit, or push. Verify behavior against the implementation and surrounding `OrganizationLink`/Memory code rather than treating this document as proof.

## Slice boundary

This is only pure-Java policy groundwork for the future shared Memory media resolver. It does not query MediaStore, convert Gallery rows, build date sections, connect a viewer/UI, or mutate links or user media. Do not assess it as the completed resolver or release-ready viewer.

## Expected policy

- A valid exact `MEMORY` link in `SUCCESS` or `PARTIAL` status plus a confirmed Gallery output chooses Gallery output as the sole source, even when live Discovery refs exist.
- A confirmed-missing linked output falls back to live Discovery refs, or `EMPTY` when none exist.
- A failed/unknown Gallery lookup must not be confused with confirmed absence; with a usable exact link it should not silently select Discovery.
- Without a usable exact Memory link, use live Discovery refs if present, otherwise `EMPTY`.
- Collection links, invalid links, and `MISSING` links are not usable exact Memory links for this policy.
- Gallery and Discovery sources must not be unconditionally unioned.

## Review questions

1. Does `MemoryMediaSourcePolicy` implement the policy above without an unsafe fallback or accidental union?
2. Is treating `PARTIAL` as usable appropriate given the existing `MemoryOrganizationLinkStore` contract? Does the policy expose any omission risk that must be addressed before wiring the real resolver?
3. Is `FAILED -> UNAVAILABLE` the right conservative behavior, and is the result name/caller contract clear enough to prevent callers from falling back anyway?
4. Are invalid/non-Memory/MISSING link cases adequately guarded and tested?
5. Are tests sufficient, focused, and independent of Android framework/MediaStore behavior?
6. Identify any release blocker or follow-up needed before the next resolver slice.

## Verification

`testDebugUnitTest`, `assembleDebug`, and `git diff --check` all PASS. No device install or media operation was performed because this is pure policy/model/test groundwork with no UI/runtime integration.

## Expected output

Findings first, ordered by severity, with exact file and line references. Separate confirmed defects from design questions and follow-up recommendations. No code changes.
