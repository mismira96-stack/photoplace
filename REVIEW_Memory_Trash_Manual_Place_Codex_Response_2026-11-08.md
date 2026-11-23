# 안티 설계 리뷰에 대한 Codex 반영 기록

- 일자: 2026-11-08
- 원문: REVIEW_Memory_Trash_Manual_Place_Antigravity_2026-11-08.md
- 설계 변경: V1.1 → V1.2, 12절에 B1~B5와 API·직렬화·LABEL_ONLY 계약 명시.

| 지적 | 반영 및 현재 상태 |
| --- | --- |
| B1 이중 커밋/토큰 | outbox 의존 제거, 수동 DB canonical alias와 JSON 읽기 합성, 충돌 거부·revision 통합 기반 구현. 장소 생성 쓰기 API는 후속. |
| B2 동명 파일/출처 | 설계 계약 구체화. resolver/MemoryPhotoItem 변경은 아직 미구현. |
| B3 raw 무결성/receipt | 검증 후 수동 투영 적용 계약. 현재 assignments 존재 시 cleanup 임시 차단. receipt 승계는 미구현. |
| B4 raw 미해결 소스/동영상 | 기존 scanner에서 raw 미해결 항목 보존 기반 구현·통합 검증. 동영상 단독 앨범 생성은 미구현. |
| B5 노트만 남은 기록 | 카드·빈 날짜 섹션·페이지 분할까지 계약에 포함. 코드 구현은 후속. |
| MainActivity/생명주기 | 위임 계약 유지. 이번 Phase A에서 MainActivity 전체 파일 해시 무변경. |
| API29/30 | 공식 API 문서에서 createTrashRequest 및 IS_TRASHED의 API30 도입 확인. 새 일반 휴지통 실행은 아직 추가하지 않음. |
| 영속 락 | 단순 디스크 조회만으로 상호 배제가 된다고 주장하지 않음. 예약 획득과 실제 활성 작업 재조정 계약 추가, 구현은 후속. |
| LABEL_ONLY/단계 | 동명 폴더 자동 연결 금지 계약. B-1 이름 입력/기존 기록, B-2 검색/지도로 분리 유지. |

Merge-on-Read 자체가 여러 저장소의 일관성을 100% 보장하거나, journal 조회만으로 동시 실행이 완전히 차단된다는 절대적 주장은 채택하지 않았다. 충돌과 revision 변경을 실제로 검사하고 다음 실행 계층에서 영속 획득 계약을 구현해야 한다.

구현/검증 상세 및 남은 위험: REVIEW_REQUEST_Memory_Place_PhaseA_2026-11-08.md. 현재 판정은 **Phase A 리뷰 요청 준비**, 전체 기능 승인 아님.
