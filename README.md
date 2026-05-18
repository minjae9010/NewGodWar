# NewGodWar

`NewGodWar`는 마인크래프트 팀전 룰과 랜덤 능력 전투를 지원하는 Bukkit/Spigot 플러그인입니다.

## 문서

- [Wiki 첫 페이지](docs/wiki/Home.md)
- [시작하기](docs/wiki/getting-started.md)
- [명령어](docs/wiki/commands.md)
- [설정](docs/wiki/configuration.md)
- [능력 목록](docs/wiki/abilities.md)

`docs/wiki` 문서는 `Wiki` GitHub Actions 워크플로를 통해 GitHub Wiki에 자동 반영됩니다. GitHub Wiki의 첫 화면은 `Home.md`입니다.

Wiki 배포 전 저장소 Settings > Features에서 Wiki 기능을 켜야 합니다. 그리고 Wiki 탭에서 첫 페이지를 한 번 저장해 `<owner>/<repo>.wiki.git` 저장소를 초기화해야 합니다. 이후부터 워크플로가 `docs/wiki` 내용을 자동 동기화합니다.

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
- `/gw` 또는 `/godwar` 기본 명령어, `/t` Themachy 스타일 호환 명령어 도움말
- `/a` 자신의 현재 능력 빠른 확인
- `/godwar a set <player> <ability>` 또는 `/t a <ability> <player>` 능력 수동 지정
- `/godwar a list [검색어]` 또는 `/t a list [검색어]` 플레이어별 배정 능력 확인
- `/godwar a skip [초]` 또는 `/t a skip [초]` 능력 확정 대기 스킵
- `/godwar a random|reset|remove` 원본과 `/t a random|reset|remove` 호환 명령
- `/godwar a cutin` 원본과 `/t a cutin` 진행 중 중간 참여 보조 명령
- `/t yes` 또는 `/t no` 능력 재추첨 확정 / 다시 뽑기
- `/godwar autoteam` 온라인 플레이어 자동 팀 배정
- `/godwar join <red|blue|green> [player]` 팀 수동 배정
- `/godwar midjoin [red|blue|green]` 진행 중인 게임에 중간 참여. 탈락 팀 플레이어도 살아있는 팀으로 재참여할 수 있습니다.
- `/godwar leave [player]` 팀 제거
- `/godwar setspawn <red|blue|green>` 현재 위치를 팀 스폰으로 등록
- `/godwar settemple <red|blue|green>` 바라보는 다이아 블록을 팀 심장으로 등록
- `/t info [team]` 팀원 목록 확인
- `/t observer [list]` 옵저버 전환 또는 목록 확인
- `/t clear [player]` 능력 쿨타임 초기화
- `/도박` 또는 `/t con` 카지노 GUI 열기
- `/godwar start` 게임 시작 및 능력 배정
- `/godwar test [ability]` 혼자 능력 테스트 시작
- `/godwar stop` 게임 종료
- `/godwar status` 현재 게임 상태
- `/godwar update [check|download]` 최신 릴리즈 확인 및 서버 실행 중 업데이트 적용
- `/godwar tips` 서버 플레이 팁 확인
- `/godwar gui` 또는 `/godwar settings` 관리자용 상자 GUI 설정 열기
- `/godwar gamblereward <normal|tajja> <번호|add> hand|message|<material> [값]` 도박 당첨 아이템 변경/추가 및 멘트 수정
- `/godwar ability [player]` 현재 능력만 확인
- `/godwar abilities` 등록된 능력 도감 GUI 열기
- `/godwar blacklist <list|add|remove|toggle> [ability]` 랜덤 배정에서 제외할 능력 관리
- `/godwar gamerule <apply|restore>` 설정된 게임룰 수동 적용 또는 복구
- `/godwar target <player>` 또는 `/x <player>` 타깃형 능력 대상 지정
- `/godwar a set <player> <ability>` 능력 수동 지정
- `/godwar spectate <player>` 관전 모드
- `/godwar unspectate <player>` 관전 해제
- `/teamchat <message>` 팀 채팅, `/tc` 팀 채팅 모드 전환

## 내장 능력

해당 능력들은 기존 Septagrame 및 Flair-Delta 님이 만드신 능력을 포함하였습니다.

내장 능력 목록은 [능력 목록](docs/wiki/abilities.md)을 확인하세요.

## 추가 조합법

- 막대기 3개를 세로, 가로, 또는 대각선으로 배치하면 블레이즈 막대기 1개를 제작할 수 있습니다.

## 주요 설정

- `game.min-players`: 게임 시작 최소 팀 배정 인원
- `game.friendly-fire`: 같은 팀 공격 허용 여부
- `game.auto-balance-teams`: 팀이 비어 있을 때 시작 시 자동 팀 배정
- `game.allow-mid-join`: 진행 중인 게임의 중간 참여 허용 여부
- `game.eliminated-player-action`: 팀 탈락 시 플레이어 처리 방식. `spectator`, `kick`, `midjoin`
- `game.clear-inventory`, `game.give-skyblock-items`, `game.remove-entities`: 시작 시 인벤토리/기본 아이템/엔티티 정리
- `game.ignore-bed`: 리스폰 시 침대 대신 팀 스폰 사용
- `game.fast-start`, `game.ready-countdown-seconds`: 시작 준비 카운트다운
- `game.select-right`: 능력 재추첨 기회 사용 여부
- `game.reveal-abilities-on-end`: 게임 종료 시 플레이어별 능력 공개 여부
- `game.ability-roll-message`: 능력 배정 타이틀 표시 여부
- `game.ability-tick-interval-seconds`: 능력 반복 처리 주기
- `game.killtime-bossbar`: 게임 시작 후 킬타임 상단 보스바 표시 여부
- `game.killtime-seconds`: 게임 시작 후 공격 금지 안내 킬타임 초
- `game.urf`: 우르프 모드 사용 여부와 능력 쿨타임 배율
- `abilities.voodoo.damage`, `abilities.voodoo.hit-interval-millis`: 부두술사 연결 피해량과 피해 간격
- `abilities.darkness.incoming-damage-multiplier`: 다크니스가 받는 피해 배율
- `world`: 시작 시 autosave, 동물/몬스터 스폰, 난이도, 시작 시간을 적용하고 종료 시 복구
- `updates`: GitHub Release 최신 버전 확인, 자동 다운로드, 관리자 접속 알림
- `core`: 코어 폭파 보호, 맨손 파괴 제한, 다이아 곡괭이 파괴 금지
- `gambling.enabled`: 도박 GUI 사용 여부
- `gambling.rewards`: 일반/타짜 도박 당첨 아이템, 수량, 메시지, 확률 가중치
- `scoreboard.enabled`: 우측 스코어보드에 내 팀, 능력, 킬 상태 표시
- `scoreboard.team-prefixes`: 탭 목록과 머리 위 이름에 팀 Prefix 표시
- `blacklist.abilities`: 랜덤 배정에서 제외할 능력 ID 목록
- `tips`: `/godwar tips`, 게임 시작 준비, 게임 진행 중 시간차로 표시할 서버 플레이 팁
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
| MC 1.11.x 이하 Legacy | 지원 X |
| Pre-release / RC | 지원 X |

## 빌드

```bash
./gradlew clean build
```

Windows PowerShell에서는 다음처럼 실행할 수 있습니다.

```powershell
.\gradlew.bat clean build
```

빌드 결과물은 `build/libs/NewGodWar-0.1.6.jar`에 복사되며, 모듈 산출물은 `plugin/build/libs/NewGodWar-0.1.6.jar`에서도 확인할 수 있습니다.

## 라이선스

Copyright (c) 2026 minjae9010

이 프로젝트는 MIT License를 따릅니다. 자세한 내용은 [LICENSE](LICENSE)를 확인하세요.
