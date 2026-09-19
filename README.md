# NewGodWar

`NewGodWar`는 마인크래프트 팀전 룰과 랜덤 능력 전투를 지원하는 Bukkit/Spigot 플러그인입니다.

## 문서

- [Wiki 첫 페이지](docs/wiki/Home.md)
- [시작하기](docs/wiki/getting-started.md)
- [명령어](docs/wiki/commands.md)
- [설정](docs/wiki/configuration.md)
- [능력 목록](docs/wiki/abilities.md)
- [애드온 설치·개발](docs/wiki/addon-development.md)

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

`/gw <분류> <동작> [대상/값]` 구조이며, 각 단계에서 Tab 자동완성과 권한에 맞는 도움말을 제공합니다. `/gw help`는 클릭 가능한 기능별 목차를, 상세 도움말은 페이지당 5개 항목과 이전·다음 이동을 제공합니다. 기존 `/gw start`, `/gw clear`, `/t` 등의 명령도 계속 사용할 수 있습니다.

| 기능 | 단계형 명령 예시 | 간편 명령 |
| --- | --- | --- |
| 도움말 / 검색 | `/gw help`, `/gw help 쿨타임`, `/gw help 간편` | `/ghelp` |
| 시작 / 종료 | `/gw game start`, `/gw game stop` | `/gstart`, `/gstop` |
| 상태 / 선택 스킵 | `/gw game status`, `/gw game skip 5` | `/gstatus`, `/gskip 5` |
| 팀 배정 | `/gw team auto`, `/gw team join red Steve` | `/gautoteam`, `/gjoin red Steve` |
| 능력 확인 / 지정 | `/gw ability show`, `/gw ability set Steve zeus` | `/a`, `/a set Steve zeus` |
| 능력 확정 / 다시 뽑기 | `/gw ability confirm`, `/gw ability reroll` | `/gconfirm`, `/greroll` |
| 쿨타임 초기화 | `/gw ability cooldown reset Steve`, `/gw ability cooldown reset all` | `/gcd Steve`, `/gcd all` |
| 설정 / 기본 아이템 | `/gw settings open`, `/gw settings items` | `/gmenu`, `/gkit` |
| 맵 / 월드 | `/gw map`, `/gw world help` | `/gmap`, `/gworld help` |
| 위치 등록 | `/gw setup spawn red`, `/gw setup temple red`, `/gw setup lobby` | `/gw s red`, `/gw d red`, `/gw sl` |

한글 명령 예: `/gw 게임 시작`, `/gw 팀 자동`, `/gw 능력 쿨타임 초기화 전체`. 팀 채팅 `/tc`, 대상 지정 `/x`, 도박 `/도박`도 지원합니다.

설정 화면은 `/gw gui game`, `/gw gui team red`, `/gw gui protection`, `/gw gui display`처럼 바로 열 수 있습니다. `/gmenu <화면>`과 `/gw settings open <화면>`도 지원하며 `/gw gui help`에서 전체 화면을 찾을 수 있습니다.

쿨타임 초기화는 관리자 전용이며 본인·특정 플레이어·전체를 구분합니다. 콘솔은 `/gcd <player|all>`처럼 대상을 지정해야 합니다. 잘못된 플레이어 이름이 전체 초기화로 처리되지 않습니다.

전체 동작, 별칭과 호환 명령은 [명령어 문서](docs/wiki/commands.md)를 확인하세요.

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
- `game.eliminated-player-action`: 팀 탈락 시 플레이어 처리 방식. `spectator`, `kick`, `midjoin`, `none`
- `game.clear-inventory`, `game.clear-inventory-on-stop`, `game.give-skyblock-items`, `game.skyblock-items`, `game.remove-entities`: 시작/종료 시 인벤토리, 기본 아이템, 엔티티 정리
- `game.ignore-bed`: 리스폰 시 침대 대신 팀 스폰 사용
- `game.fast-start`, `game.ready-countdown-seconds`: 시작 준비 카운트다운
- `game.select-right`: 능력 재추첨 기회 사용 여부
- `game.reveal-abilities-on-end`: 게임 종료 시 플레이어별 능력 공개 여부
- `game.ability-roll-message`: 능력 배정 타이틀 표시 여부
- `game.ability-tick-interval-seconds`: 능력 반복 처리 주기
- `game.killtime-bossbar`: 게임 시작 후 킬타임 상단 보스바 표시 여부
- `game.killtime-seconds`: 게임 시작 후 킬타임 보호 시간
- `game.killtime-mode`: `player-combat` 유저 간 피해 차단 또는 `core-only` 코어 파괴 차단
- `game.urf`: 우르프 모드 사용 여부와 능력 쿨타임 배율
- `abilities.voodoo.damage`, `abilities.voodoo.hit-interval-millis`: 부두술사 연결 피해량과 피해 간격
- `abilities.darkness.incoming-damage-multiplier`: 다크니스가 받는 피해 배율
- `world`: 시작 시 autosave, 동물/몬스터 스폰, 난이도, 시작 시간을 적용하고 종료 시 복구
- `maps.<world>`: `/gw map <world>`로 선택하는 맵별 팀 스폰과 심장 위치
- `lobby`: 접속 위치와 게임 종료 후 복귀 위치
- `compatibility.clear-teleport-invulnerability`: 다른 플러그인의 텔레포트 보호로 생기는 전투 중 무적 틱 정리
- `updates`: GitHub Release 최신 버전 확인, 선택적 자동 다운로드, 다음 재시작 적용 안내
- `core`: 코어 폭파 보호와 시간 해제, 맨손 파괴 제한, 곡괭이 코어 파괴 시간
- `gambling.enabled`: 도박 GUI 사용 여부
- `gambling.rewards`: 도박 당첨 아이템, 수량, 메시지, 확률 가중치
- `scoreboard.enabled`: 우측 스코어보드에 내 팀, 능력, 킬 상태 표시
- `scoreboard.team-prefixes`: 탭 목록과 머리 위 이름에 팀 Prefix 표시
- `blacklist.abilities`: 랜덤 배정에서 제외할 능력 ID 목록
- `tips`: `/gw tips`, 게임 시작 준비, 게임 진행 중 시간차로 표시할 서버 플레이 팁
- `gamerules`: 게임 시작 시 모든 월드에 적용할 마인크래프트 게임룰과 종료 시 복구 여부

## 지원 버전

| 마인크래프트 버전 | 권장 플러그인 버전 |
| --- | --- |
| MC 26.1.1 ~ 26.3 | 최신 버전 |
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

빌드 결과물은 `build/libs/NewGodWar-0.3.2.jar`에 복사되며, 모듈 산출물은 `plugin/build/libs/NewGodWar-0.3.2.jar`에서도 확인할 수 있습니다.

## 라이선스

Copyright (c) 2026 minjae9010

이 프로젝트는 MIT License를 따릅니다. 자세한 내용은 [LICENSE](LICENSE)를 확인하세요.


## 코어 회귀 테스트

`build`는 배포 플러그인과 별도로 `plugin/build/core-regression/CoreRegressionProbe.jar`를 생성합니다. 이 파일은 테스트 전용이며 운영 서버에 설치하면 안 됩니다.

```powershell
./scripts/Test-PaperMatrix.ps1 -SkipBuild -LatestVersion -PluginJar build/libs/NewGodWar-0.3.2.jar -ProbeJar plugin/build/core-regression/CoreRegressionProbe.jar -WorkDir .paper-smoke/core-regression
```

격리된 Paper 서버에서 비참가자·관전자 등의 심장 파괴 차단, 정상 파괴, 중복 위치, 동시 폭발의 일괄 탈락, 종료 후 이벤트 차단, 준비 인원 재검사와 타이머 정리를 검사합니다. 일반 게임과 능력 테스트에서 설치·저장한 상자가 종료 후 제거되고 다음 게임에 남지 않는지도 검사합니다. 릴리즈는 이 검사를 통과해야 게시됩니다.

## 명령어 회귀 테스트

`build`는 `plugin/build/command-regression/CommandRegressionProbe.jar`도 생성합니다. 테스트 전용으로 격리된 Paper 서버에서만 사용합니다.

```powershell
./scripts/Test-PaperMatrix.ps1 -SkipBuild -LatestVersion -PluginJar build/libs/NewGodWar-0.3.2.jar -ProbeJar plugin/build/command-regression/CommandRegressionProbe.jar -ProbeSuccessMarker "COMMAND REGRESSION PASS" -WorkDir .paper-smoke/command-regression
```

단계형·한글·간편 명령의 권한, 자동완성, 실제 등록 상태와 쿨타임 초기화 범위를 확인합니다. 없는 대상·콘솔 대상 생략·추가 인수·능력 조회가 전체 초기화로 이어지지 않는지도 검사합니다.
