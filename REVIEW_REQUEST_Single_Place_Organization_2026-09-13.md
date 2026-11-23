# PhotoPlace Single-Place Organization Review Request

## Scope

Review the single-place `Discovery Memory -> Gallery location album` implementation added after the OrganizationLink lifecycle and exact-link repository work, including the Memory media-source lifecycle and the current completion UX.

No commit, push, or Play release has been performed for this slice. The user reports that a single-place organization run succeeded on the connected device after confirming 39 new items and 0 existing duplicates. Original-photo trash was not invoked. Post-organization Memory photo/note persistence has not yet been explicitly verified on-device.

The user clarified that the first shared mockup is the intended final-screen direction; the second mockup is the simpler screen they actually saw. Repeated manual tests have already created multiple albums, so do not trigger another organization run during review. The latest first-mockup-inspired UI revision has only been built locally and has not been installed or visually checked on-device.

## Implementation Summary

- Adds `이 장소만 위치 앨범으로 만들기` to eligible Discovery Memory detail screens.
- Reloads the repository and live-filters the selected Memory immediately before preflight; it prepares only that Memory through the existing `DiscoveryAlbumOrganizer` and `MediaStoreAlbumLookup`.
- Reuses the existing sort input / Worker / result path, adding an `OrganizationRequest` with the Memory stable ID and the exact target album path.
- Saves an OrganizationLink only from the confirmed Worker result; exact path and Memory identity are passed through persisted request/result data.
- Keeps the request identity through the video permission prompt. Denial or permission-launch failure clears pending state and returns to the Memory detail without starting the sort.
- Confirmation counts reflect the video-move setting and disclose inaccessible media and videos excluded by the current setting.
- A dedicated completion screen inspired by the first shared mockup is now implemented in `MemoryOrganizationCompletionRenderer`, with a reduced success area, album name/count, a created-album row and thumbnail, and Gallery navigation. Device visual/navigation verification is pending.
- `위치 앨범 보기` is primary for this explicit organize action; `기억으로 돌아가기` is secondary. The renderer distinguishes success, no copied items, cancellation, partial failure, and OrganizationLink persistence failure.
- During a single-Memory Worker run, the app now stays on the Memory detail rather than briefly routing through the generic bulk-result screen. Bulk organization progress routing is unchanged.
- Single-Memory results do not enqueue copied source URIs for original-photo trashing. The dedicated result screen has no trash action. Memory detail currently builds photo sections from Discovery refs only; the exact OrganizationLink is not yet used as a detail-media source.

## Product / Completion UI Proposal

The user considers Gallery album creation optional: Memory browsing is the main value, while some users still prefer real Gallery folders. Keep single-place album creation available, but do not make the completion screen feel like the app's primary destination.

The user shared a completion-screen mockup with a large `정리 앨범 생성 완료` hero, item count, `위치 앨범 보기` button, an original-trash warning/action, and a newly created album row. Review this direction with these constraints:

- The success hero currently feels oversized; prefer a compact confirmation showing the selected place and actual item count.
- Use singular wording for a single-place operation (for example, `성남 앨범을 만들었어요 · 사진 39장`) and plural wording only for bulk organization.
- The user expects the first mockup's Gallery album summary as the final screen. The revised UI makes `위치 앨범 보기` primary and keeps returning to the same Memory as a secondary path.
- Do not expose an enabled original-trash action until Memory detail can resolve the exact linked Gallery output and safely fall back to Discovery refs. A warning alone is not sufficient protection.
- Review whether the reduced first-mockup-inspired layout is compact and truthful across success/partial/failure states, and whether the Location Albums primary action and Memory return are clear.

## Review Focus

1. Does the live preflight guarantee that only the selected place's current media can enter the Worker input?
2. Are duplicate refs and videos excluded consistently between confirmation counts, `PhotoItem` adaptation, and the existing Worker behavior?
3. Can permission acceptance, cancellation, launch failure, or activity recreation lose the pending `OrganizationRequest` or leave the app in a false progress state?
4. Is the OrganizationLink created only for confirmed successful output, with the selected stable Memory ID and target path?
5. Can partial failure, retry, or replay produce a misleading organized state or duplicate album-history/link records?
6. Are Memory identity, date grouping, and date notes preserved after the Gallery output is created? What device checks are still needed?
7. The current Memory detail appears to build sections from Discovery refs while the result flow can offer original-photo trashing. Is the Memory media resolver a release blocker before allowing that cleanup? Verify exact-link source precedence, fallback, partial-link behavior, persistence-failure behavior, and duplicate avoidance.
8. Should original-photo trashing be gated on a verified complete OrganizationLink and resolver availability?
9. Does keeping the Memory detail visible during one-place work, followed by the dedicated completion screen, avoid confusing intermediate navigation? Confirm bulk progress routing remains unchanged.
10. Review the updated completion screen: is the reduced success area balanced, is the album name/count and thumbnail row clear, and is `위치 앨범 보기` an appropriate primary action for this explicit organize flow?
11. What is the minimum safe handling for single-place results when OrganizationLink persistence fails or the result is partial, especially regarding the original-trash action?
12. Identify missing unit tests or code-level blockers before release.

## Verification

- `testDebugUnitTest`: PASS.
- `assembleDebug`: PASS.
- `git diff --check`: PASS.
- Device smoke: user-reported successful single-place organization; 39 new / 0 duplicate items shown; original trash not used.
- Device checks pending: Memory detail photo/date-note preservation after organization, same-place retry/idempotency, exact Gallery entry, permission-denied path, and UX return flow.

Please review the code and reported device observation separately. Do not infer that unlisted UI/device lifecycle cases have passed. Do not recommend trashing originals until the Memory media-source gap is resolved or a safe source policy is implemented.
