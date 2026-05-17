# 설정

설정 파일은 서버 시작 후 `plugins/NewGodWar/config.yml`에 생성됩니다. 파일을 직접 수정한 뒤에는 `/godwar reload`를 실행하거나 서버를 재시작하세요.

## game

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `game.min-players` | `2` | 시작에 필요한 최소 팀 배정 인원 |
| `game.friendly-fire` | `false` | 같은 팀 공격 허용 여부 |
| `game.auto-balance-teams` | `true` | 팀 배정이 비어 있을 때 시작 시 자동 배정 |
| `game.allow-mid-join` | `true` | 진행 중 중간 참여 허용 여부 |
| `game.eliminated-player-action` | `spectator` | 탈락 팀 플레이어 처리 방식: `spectator`, `kick`, `midjoin` |
| `game.clear-inventory` | `true` | 시작 시 인벤토리와 방어구 초기화 |
| `game.give-skyblock-items` | `true` | 시작 시 기본 스카이블럭 아이템 지급 |
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
| `game.urf.enabled` | `false` | 우르프 모드 사용 여부 |
| `game.urf.cooldown-multiplier` | `0.2` | 우르프 모드 쿨타임 배율 |

우르프 명령에서 `80%`를 입력하면 쿨타임을 80% 줄인다는 의미입니다. 이때 저장되는 배율은 `0.2`입니다.

## world

게임 시작 시 모든 월드에 적용하고, 게임 종료 시 기존 값을 복구합니다.

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `world.autosave` | `true` | 월드 자동 저장 여부 |
| `world.spawn-animals` | `false` | 동물 스폰 여부 |
| `world.spawn-monsters` | `false` | 몬스터 스폰 여부 |
| `world.difficulty` | `EASY` | 시작 후 난이도 |
| `world.start-time` | `6000` | 시작 후 월드 시간 |

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

곡괭이 시간 해제 값이 `0` 이상이면 `core.require-empty-hand`가 켜져 있어도 해당 시간이 지난 뒤 그 곡괭이로 코어를 파괴할 수 있습니다. 월드 / 코어 설정 GUI에서 각 곡괭이를 좌클릭/우클릭해 1분 단위로, 쉬프트 좌클릭/쉬프트 우클릭해 5분 단위로 조정할 수 있습니다.

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
| `gambling.rewards.normal` | 목록 | 일반 도박 보상 목록 |
| `gambling.rewards.tajja` | 목록 | 타짜 능력 보상 목록 |

보상 항목은 `chance`, `material`, `amount`, `message` 또는 `messages`를 가집니다. 구버전 재료 호환이 필요하면 `legacy-material`을 함께 둘 수 있습니다.

인게임에서는 `/godwar gamblereward <normal|tajja> <번호|add> hand|message|<material> [값]`로 보상 아이템과 멘트를 바꿀 수 있습니다.

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

기본값은 `keepInventory: false`, `doImmediateRespawn: true`, `doDaylightCycle: false`, `doWeatherCycle: false`, `naturalRegeneration: true`입니다.

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

`messages`에서는 prefix와 주요 게임 메시지를 바꿀 수 있습니다.

| 키 | 설명 |
| --- | --- |
| `messages.prefix` | 플러그인 메시지 앞에 붙는 prefix |
| `messages.game-start` | 게임 시작 방송 |
| `messages.game-stop` | 게임 종료 방송 |
| `messages.team-eliminated` | 팀 탈락 방송 |
| `messages.team-eliminated-kick` | 탈락 팀 kick 메시지 |
| `messages.winner` | 승리 방송 |
