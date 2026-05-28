# 설정

설정 파일은 서버 시작 후 `plugins/NewGodWar/config.yml`에 생성됩니다. 파일을 직접 수정한 뒤에는 `/godwar reload`를 실행하거나 서버를 재시작하세요.

## game

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `game.min-players` | `2` | 시작에 필요한 최소 팀 배정 인원 |
| `game.friendly-fire` | `false` | 같은 팀 공격 허용 여부 |
| `game.auto-balance-teams` | `true` | 팀 배정이 비어 있을 때 시작 시 자동 배정 |
| `game.allow-mid-join` | `true` | 진행 중 중간 참여 허용 여부 |
| `game.eliminated-player-action` | `spectator` | 탈락 팀 플레이어 처리 방식: `spectator`, `kick`, `midjoin`, `none` |
| `game.clear-inventory` | `true` | 시작 시 인벤토리와 방어구 초기화 |
| `game.clear-inventory-on-stop` | `true` | 게임 종료 시 참가자/옵저버 인벤토리와 방어구 초기화 |
| `game.give-skyblock-items` | `true` | 시작 시 기본 스카이블럭 아이템 지급 |
| `game.skyblock-items` | 용암 양동이, 얼음, 묘목, 뼛가루, 상자, 익힌 소고기 | 시작 시 지급할 기본 아이템 목록 |
| `game.remove-entities` | `true` | 시작 시 아이템, 몬스터, 동물 엔티티 제거 |
| `game.ignore-bed` | `true` | 리스폰 시 침대 대신 팀 스폰 사용 |
| `game.fast-start` | `true` | 빠른 시작 카운트다운 사용 |
| `game.ready-countdown-seconds` | `40` | 일반 준비 카운트다운 초 |
| `game.fast-ready-countdown-seconds` | `5` | 빠른 준비 카운트다운 초 |
| `game.skip-ready-countdown-seconds` | `5` | 능력 확정 자동 skip 및 관리자 skip 기본 초 |
| `game.select-right` | `true` | 능력 재추첨 선택 사용 여부 |
| `game.ability-reroll-count` | `1` | 플레이어별 재추첨 가능 횟수 |
| `game.reveal-abilities-on-end` | `true` | 게임 종료 시 플레이어별 능력 공개 여부 |
| `game.announce-radius` | `0` | 공지 반경용 예약 설정 |
| `game.ability-roll-message` | `true` | 능력 배정 타이틀 표시 여부 |
| `game.ability-tick-interval-seconds` | `1` | 능력 반복 처리 주기 |
| `game.killtime-bossbar` | `false` | 게임 시작 후 킬타임을 상단 보스바로 표시할지 여부 |
| `game.killtime-seconds` | `300` | 게임 시작 후 공격 금지 안내 킬타임 초 |
| `game.urf.enabled` | `false` | 우르프 모드 사용 여부 |
| `game.urf.cooldown-multiplier` | `0.2` | 우르프 모드 쿨타임 배율 |

우르프 명령에서 `80%`를 입력하면 쿨타임을 80% 줄인다는 의미입니다. 이때 저장되는 배율은 `0.2`입니다.

기본 지급 아이템은 `game.give-skyblock-items`가 켜져 있을 때만 지급됩니다. 기본값은 용암 양동이 2개, 얼음 2개, 참나무 묘목 1개, 뼛가루 4개, 상자 1개, 익힌 소고기 64개입니다. 인게임에서는 `/godwar defaultitems`로 가상 창고를 열어 넣고 빼면 닫을 때 저장됩니다.

## world

게임 시작 시 모든 월드에 적용하고, 게임 종료 시 기존 값을 복구합니다.

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `world.autosave` | `true` | 월드 자동 저장 여부 |
| `world.spawn-animals` | `false` | 동물 스폰 여부 |
| `world.spawn-monsters` | `false` | 몬스터 스폰 여부 |
| `world.difficulty` | `EASY` | 시작 후 난이도 |
| `world.start-time` | `6000` | 시작 후 월드 시간 |
| `world.game-world` | `""` | 자동 초기화할 게임 월드 이름. 비어 있으면 자동 초기화 기능은 작동하지 않음 |
| `world.reset-game-world-on-stop` | `true` | 게임 월드가 지정되어 있을 때 시작 시 백업하고 종료 시 해당 월드 복원 |
| `world.managed-worlds` | `[]` | `/godwar world create`, `load`, `copy`, 백업 로드로 등록된 월드 목록. 서버 시작 시 자동 로드 |
| `maps.<world>.spawns` | 자동 생성 | 맵별 팀 스폰 위치. `/godwar map <world>` 선택 후 `/godwar setspawn <team>`로 저장 |
| `maps.<world>.temples` | 자동 생성 | 맵별 팀 심장 위치. `/godwar map <world>` 선택 후 `/godwar settemple <team>`로 저장 |

게임 맵은 `/godwar map <world>`로 선택하고 `/godwar map clear`로 해제합니다. 기존 `/godwar world game <world>`도 같은 설정을 사용합니다. 로비 월드는 게임 맵으로 지정할 수 없습니다. 게임 월드가 지정되어 있으면 게임 시작 직전에 해당 월드를 스냅샷으로 저장하고, 게임 종료 시 참가자를 로비로 이동시킨 뒤 월드를 언로드, 복원, 재로드합니다. 로비가 설정되어 있지 않거나 플레이어가 게임 월드에 남아 있으면 안전을 위해 월드 초기화를 건너뜁니다.

인게임에서는 `/godwar world gui` 또는 `/godwar settings`의 `월드` 메뉴에서 `world.autosave`, `world.spawn-animals`, `world.spawn-monsters`, `world.difficulty`, `world.start-time`, `world.game-world`, `world.reset-game-world-on-stop`을 조정할 수 있습니다. 자세한 운영 흐름은 [월드 관리](world-management)를 참고하세요.

## lobby

접속 위치와 게임 종료 후 복귀 위치를 관리합니다. 위치는 `/godwar setlobby`로 저장합니다.

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `lobby.teleport-on-join` | `true` | 게임 진행 중이 아닐 때 접속한 플레이어를 로비 위치로 이동 |
| `lobby.teleport-on-game-stop` | `true` | 게임 종료 시 참가자/옵저버를 로비 위치로 이동 |
| `lobby.location` | `""` | 저장된 로비 위치. 비어 있거나 월드가 없으면 이동 처리 없이 기존 흐름대로 진행 |

게임 종료 시 참가자 팀 배정, 옵저버 상태, 팀 채팅 모드, 킬 기록도 함께 초기화됩니다.

로비 월드와 게임 월드를 분리해서 운영할 수 있습니다. 로비 월드에서 `/godwar setlobby`를 실행하면 종료 후 참가자는 해당 월드로 돌아갑니다. 운영자는 `/godwar world help`, `/godwar world list`, `/godwar world game <world|clear>`, `/godwar world create <world> [normal|flat|void]`, `/godwar world load <world>`, `/godwar world copy <sourceWorld> <newWorld>`, `/godwar world tp <world> [player]`, `/godwar world lobby [player]`, `/godwar world unload <world>`, `/godwar world delete <world> confirm`으로 월드를 지정, 생성, 복사, 이동, 로드, 언로드, 삭제할 수 있습니다.

## updates

서버가 켜져 있는 동안 GitHub Release를 확인하고, 새 버전이 있으면 관리자에게 알립니다. `updates.auto-download`를 켜거나 `/godwar update download`를 실행하면 업데이트 jar를 `plugins/update/`에 준비하고, 서버 재시작 후 새 버전이 적용됩니다.

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `updates.enabled` | `true` | 자동 업데이트 확인 사용 여부 |
| `updates.auto-download` | `false` | 새 릴리즈 발견 시 jar 자동 다운로드 여부 |
| `updates.apply-without-restart` | `false` | 더 이상 사용하지 않는 이전 설정. 최신 Paper에서는 플러그인 업데이트 적용에 서버 재시작이 필요합니다. |
| `updates.apply-delay-seconds` | `10` | 더 이상 사용하지 않는 이전 설정 |
| `updates.notify-admins` | `true` | 관리자에게 구버전 및 재시작 필요 알림 표시 여부 |
| `updates.initial-delay-seconds` | `10` | 서버 시작 후 첫 업데이트 확인 지연 시간 |
| `updates.check-interval-minutes` | `60` | 반복 확인 간격. `0` 이하이면 시작 시 한 번만 확인 |
| `updates.github.owner` | `minjae9010` | GitHub 릴리즈 저장소 소유자 |
| `updates.github.repo` | `NewGodWar` | GitHub 릴리즈 저장소 이름 |

관리자는 `/godwar update check`로 즉시 확인하고, `/godwar update download`로 서버 실행 중 최신 jar를 내려받을 수 있습니다.

## compatibility

다른 플러그인과 함께 사용할 때 게임 중 전투에 영향을 줄 수 있는 보호 효과를 보정합니다.

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `compatibility.clear-teleport-invulnerability.enabled` | `true` | 게임 참가자가 텔레포트한 뒤 추가 무적 틱을 정리할지 여부 |
| `compatibility.clear-teleport-invulnerability.delay-ticks` | `1` | 텔레포트 후 첫 정리를 실행할 tick 지연 |
| `compatibility.clear-teleport-invulnerability.repeat-ticks` | `3` | 외부 플러그인의 지연 적용까지 잡기 위해 추가 정리할 tick 범위 |
| `compatibility.clear-teleport-invulnerability.allowed-no-damage-ticks` | `0` | 텔레포트 후 허용할 최대 무적 tick |
| `compatibility.clear-teleport-invulnerability.clear-entity-invulnerable` | `true` | 서버 버전이 지원하면 엔티티 invulnerable 플래그도 해제 |

이 기능은 NewGodWar의 능력 무적 판정에는 영향을 주지 않고, Bukkit 플레이어의 텔레포트 후 피해 무시 틱과 엔티티 무적 플래그만 정리합니다.

## core

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `core.protect-diamond-from-explosion` | `true` | 등록된 다이아 심장 폭발 보호 |
| `core.require-empty-hand` | `true` | 심장을 맨손으로만 파괴 가능 |
| `core.forbid-diamond-pickaxe` | `true` | 다이아몬드 곡괭이 심장 파괴 금지 |
| `core.pickaxe-unlock.wooden-seconds` | `-1` | 게임 시작 후 나무 곡괭이 코어 파괴 허용 시간. `-1`이면 자동 해제 안 함 |
| `core.pickaxe-unlock.stone-seconds` | `-1` | 게임 시작 후 돌 곡괭이 코어 파괴 허용 시간. `-1`이면 자동 해제 안 함 |
| `core.pickaxe-unlock.iron-seconds` | `-1` | 게임 시작 후 철 곡괭이 코어 파괴 허용 시간. `-1`이면 자동 해제 안 함 |
| `core.pickaxe-unlock.gold-seconds` | `-1` | 게임 시작 후 금 곡괭이 코어 파괴 허용 시간. `-1`이면 자동 해제 안 함 |
| `core.pickaxe-unlock.diamond-seconds` | `-1` | 게임 시작 후 다이아 곡괭이 코어 파괴 허용 시간. `-1`이면 자동 해제 안 함 |

곡괭이 시간 해제 값이 `0` 이상이면 `core.require-empty-hand`가 켜져 있어도 해당 시간이 지난 뒤 그 곡괭이로 코어를 파괴할 수 있습니다. 월드 / 코어 설정 GUI에서 각 곡괭이를 좌클릭/우클릭해 1분 단위로, 쉬프트 좌클릭/쉬프트 우클릭해 5분 단위로 조정할 수 있습니다. 명령어로는 `/godwar pickaxe status`로 진행 시간과 해제 상태를 확인하고, `/godwar pickaxe <종류|all> <open|off|분>`으로 바로 조정할 수 있습니다.

## abilities

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `abilities.messages.enabled` | `true` | 능력 메시지 전체 사용 여부 |
| `abilities.messages.success` | `true` | 능력 성공 메시지 |
| `abilities.messages.failure` | `true` | 능력 실패 메시지 |
| `abilities.messages.timer` | `true` | 쿨타임/타이머 안내 메시지 |
| `abilities.nasdaq.iron-success-percent` | `75` | 나스닥 철괴 복사 성공률 |
| `abilities.nasdaq.diamond-success-percent` | `25` | 나스닥 다이아몬드 복사 성공률 |
| `abilities.voodoo.damage` | `0.5` | 부두술사 연결 피해량 |
| `abilities.voodoo.hit-interval-millis` | `1000` | 부두술사 연결 피해 간격 |
| `abilities.darkness.incoming-damage-multiplier` | `0.25` | 다크니스가 받는 피해 배율 |

능력별 추가 설정은 `abilities.<ability-id>.<key>` 형태로 확장할 수 있습니다.

## gambling

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `gambling.enabled` | `true` | 도박 GUI 사용 여부 |
| `gambling.cost.cobblestone` | `32` | 1회 도박에 필요한 조약돌 수 |
| `gambling.rewards.normal` | 목록 | 도박 보상 목록 |

보상 항목은 `chance`, `material`, `amount`, `message` 또는 `messages`를 가집니다. 구버전 재료 호환이 필요하면 `legacy-material`을 함께 둘 수 있습니다.

인게임에서는 `/godwar gamblereward <normal> <번호|add> hand|message|<material> [값]`로 보상 아이템과 멘트를 바꿀 수 있습니다.

## scoreboard

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `scoreboard.enabled` | `true` | 우측 스코어보드 표시 |
| `scoreboard.team-prefixes` | `true` | 탭 목록과 머리 위 이름에 팀 prefix 표시 |

## blacklist

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `blacklist.abilities` | `[]` | 랜덤 배정에서 제외할 능력 ID 목록 |

인게임에서는 `/godwar blacklist list`, `/godwar blacklist add <ability>`, `/godwar blacklist remove <ability>`, `/godwar blacklist toggle <ability>`을 사용할 수 있습니다.

## tips

`/godwar tips`와 게임 시작 준비, 게임 진행 중 시간차 안내에 사용할 팁 문구입니다. 색상 코드는 `&` 문법을 사용할 수 있습니다.

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `tips.enabled` | `true` | 팁 기능 사용 여부 |
| `tips.show-on-start` | `true` | 시작 준비 시 첫 번째 팁 표시 |
| `tips.timed.enabled` | `true` | 게임 진행 중 시간차 팁 표시 |
| `tips.timed.initial-delay-seconds` | `60` | 게임 시작 후 첫 시간차 팁까지 대기할 초 |
| `tips.timed.interval-seconds` | `180` | 다음 팁을 표시할 간격(초) |
| `tips.timed.repeat` | `true` | 모든 팁을 보여준 뒤 처음부터 반복할지 여부 |
| `tips.lines` | 목록 | 표시할 팁 문구 |

## gamerules

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `gamerules.enabled` | `true` | 게임 시작 시 게임룰 적용 |
| `gamerules.restore-on-stop` | `true` | 종료 시 이전 게임룰 복구 |
| `gamerules.rules` | 목록 | 적용할 Minecraft GameRule |

기본값은 `keepInventory: false`, `doImmediateRespawn: true`, `doDaylightCycle: false`, `doWeatherCycle: false`, `doFireTick: false`, `fire_spread_radius_around_player: 0`, `naturalRegeneration: true`, `locatorBar: false`입니다. `doFireTick: false`는 구버전 불 틱을 끄고, `fire_spread_radius_around_player: 0`은 상위 버전에서 플레이어 주변 불 번짐 반경을 0으로 설정합니다. `locatorBar`와 `fire_spread_radius_around_player`는 해당 게임룰이 있는 서버 버전에서만 적용됩니다.

## teams

기본 팀은 `red`, `blue`, `green`입니다.

```yaml
teams:
  red:
    enabled: true
    display-name: "빨강"
    color: RED
```

`enabled`가 꺼진 팀은 자동 배정, 중간 참여, 시작 설정 검사에서 제외됩니다. `display-name`은 명령어 팀 입력 alias로도 사용할 수 있습니다.

## spawns, temples, messages

`spawns`와 `temples`는 `/godwar setspawn`, `/godwar settemple` 명령 또는 설정 GUI가 저장합니다. 직접 수정할 수 있지만 월드 이름과 좌표가 정확해야 합니다.

월드 백업은 `/godwar world backup create [이름]`으로 생성하고 `plugins/NewGodWar/world-backups/`에 저장됩니다. `/godwar world backup load <백업이름> [로드월드이름]`은 기존 월드를 덮어쓰지 않고 새 월드 폴더로 복사한 뒤 서버에 로드합니다.

`messages`에서는 prefix와 주요 게임 메시지를 바꿀 수 있습니다.

| 키 | 설명 |
| --- | --- |
| `messages.prefix` | 플러그인 메시지 앞에 붙는 prefix |
| `messages.game-start` | 게임 시작 방송 |
| `messages.game-stop` | 게임 종료 방송 |
| `messages.team-eliminated` | 팀 탈락 방송 |
| `messages.team-eliminated-kick` | 탈락 팀 kick 메시지 |
| `messages.winner` | 승리 방송 |
