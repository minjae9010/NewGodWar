# NewGodWar

`NewGodWar`는 마인크래프트 팀전 룰과 랜덤 능력 전투를 지원하는 Bukkit/Spigot 플러그인입니다.

## 핵심 기능

- 빨강/파랑/초록 3팀 자동 배정
- 팀 색상 스코어보드, 팀킬 방지
- 팀별 다이아 심장 등록 및 파괴 시 팀 탈락
- 랜덤 능력 배정
- 킬 수 집계
- 팀 채팅
- 관전 모드 전환
- NMS 리플렉션 어댑터 기반 액션바/타이틀 전송

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
- `/godwar blacklist <list|add|remove|toggle> [ability]` 랜덤 배정에서 제외할 능력 관리
- `/godwar gamerule <apply|restore>` 설정된 게임룰 수동 적용 또는 복구
- `/godwar target <player>` 또는 `/x <player>` 타깃형 능력 대상 지정
- `/godwar setability <player> <ability>` 능력 수동 지정
- `/godwar spectate <player>` 관전 모드
- `/godwar unspectate <player>` 관전 해제
- `/teamchat <message>` 팀 채팅

## 내장 능력

해당 능력들은 기존 Septagrame 및 Flair-Delta 님이 만드신 능력을 포함하였습니다.

내장 능력 목록은 [능력 목록](docs/wiki/abilities.md)을 확인하세요.

## 주요 설정

- `game.min-players`: 게임 시작 최소 팀 배정 인원
- `game.friendly-fire`: 같은 팀 공격 허용 여부
- `game.auto-balance-teams`: 팀이 비어 있을 때 시작 시 자동 팀 배정
- `game.ability-roll-message`: 능력 배정 타이틀 표시 여부
- `game.urf`: 우르프 모드 설정, 능력 쿨타임과 일부 능력 보정에 적용
- `blacklist.abilities`: 랜덤 배정에서 제외할 능력 ID 목록
- `gamerules`: 게임 시작 시 모든 월드에 적용할 마인크래프트 게임룰과 종료 시 복구 여부

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
