# NewGodWar

`NewGodWar`는 예전 마인크래프트 “신들의 전쟁” 서버팩의 팀전 룰과 AbilityWar 계열의 랜덤 능력 콘셉트를 참고해 만든 신규 Bukkit/Spigot 플러그인입니다.

## 핵심 기능

- 빨강/파랑/초록 3팀 자동 배정
- 팀 색상 스코어보드, 팀킬 방지
- 팀별 다이아 심장 등록 및 파괴 시 팀 탈락
- 랜덤 능력 배정
- 킬 수 집계
- 팀 채팅
- 관전 모드 전환
- NMS 리플렉션 어댑터 기반 액션바/타이틀 전송

## 참고한 기존 콘텐츠

- AbilityWar 계열: 게임 시작 시 플레이어에게 랜덤 능력을 부여하고 능력으로 대전하는 구조를 참고했습니다.
- 구 신들의 전쟁 서버팩: 팀 구별, 팀킬 방지, 다이아 블록 파괴 알림, 킬 순위, 팀채팅, 관전 모드, 팀 자동 세팅 같은 운영 흐름을 새 구조로 재해석했습니다.

## 명령어

- `/godwar help` 도움말
- `/godwar autoteam` 온라인 플레이어 자동 팀 배정
- `/godwar join <red|blue|green> [player]` 팀 수동 배정
- `/godwar leave [player]` 팀 제거
- `/godwar settemple <red|blue|green>` 바라보는 다이아 블록을 팀 심장으로 등록
- `/godwar start` 게임 시작 및 능력 배정
- `/godwar stop` 게임 종료
- `/godwar status` 현재 게임 상태
- `/godwar gui` 또는 `/godwar settings` 관리자용 상자 GUI 설정 열기
- `/godwar ability [player]` 능력 확인 GUI 열기
- `/godwar abilities` 등록된 능력 목록 GUI 열기
- `/godwar setability <player> <ability>` 능력 수동 지정
- `/godwar spectate <player>` 관전 모드
- `/godwar unspectate <player>` 관전 해제
- `/teamchat <message>` 팀 채팅

## 내장 능력

| ID | 이름 | 효과 |
| --- | --- | --- |
| `zeus` | 제우스 | 공격 시 일정 확률로 번개를 내리고 추가 피해를 줍니다. |
| `ares` | 아레스 | 근접 공격 피해량이 증가합니다. |
| `hermes` | 헤르메스 | 능력 배정 시 빠른 이동 효과를 받습니다. |
| `poseidon` | 포세이돈 | 물 안에 있을 때 설정된 간격마다 체력을 회복합니다. |

## 지원 버전

| 마인크래프트 버전 | 권장 플러그인 버전 |
| --- | --- |
| MC 26.1.1 ~ 26.1.2 | 최신 버전 |
| MC 1.21.x | 최신 버전 |
| MC 1.20.x | 최신 버전 |
| MC 1.19.x | 최신 버전 |
| MC 1.18.x | 최신 버전 |
| MC 1.17.x | 최신 버전 |
| MC 1.16.x | 최신 버전 |
| MC 1.15.x | 최신 버전 |
| MC 1.14.x | 최신 버전 |
| MC 1.13.x | 최신 버전 |
| MC 1.12.x | 최신 버전 |
| MC 1.11.x 이하 Legacy | 최신 버전 |
| Pre-release / RC | 지원 X |

## 빌드

```bash
./gradlew clean build
```

Windows PowerShell에서는 다음처럼 실행할 수 있습니다.

```powershell
.\gradlew.bat clean build
```

빌드 결과물은 `build/libs/NewGodWar-0.1.0-SNAPSHOT.jar`에 복사되며, 모듈 산출물은 `plugin/build/libs/NewGodWar-0.1.0-SNAPSHOT.jar`에서도 확인할 수 있습니다.

## 라이선스

Copyright (c) 2026 minjae9010

이 프로젝트는 MIT License를 따릅니다. 자세한 내용은 [LICENSE](LICENSE)를 확인하세요.
