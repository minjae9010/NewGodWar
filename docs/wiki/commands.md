# 명령어

기본 안내 명령어는 `/gw`입니다. 긴 명령어 이름과 `/godswar`, `/신들의전쟁`도 alias로 사용할 수 있으며, `/t`는 Themachy 스타일 호환 명령어로 별도 유지됩니다. 별도 단축 명령으로 `/a`, `/x`, `/teamchat`, `/도박`도 제공됩니다.

## 권한

| 권한 | 기본값 | 설명 |
| --- | --- | --- |
| `newgodwar.play` | 모든 플레이어 | 기본 플레이 명령 |
| `newgodwar.admin` | OP | 게임 운영, 설정, 강제 지정 명령 |

팀 참가, 팀 해제, 중간 참여, 능력, 관전 상태를 바꾸는 운영 명령은 `newgodwar.admin` 권한이 필요합니다.

## 플레이어 명령어

| 명령어 | 설명 |
| --- | --- |
| `/gw help [페이지|섹션]` | 도움말을 봅니다. |
| `/gw status` | 현재 게임 상태와 주요 설정을 확인합니다. |
| `/gw tips` | 서버 플레이 팁을 확인합니다. |
| `/gw info [team]` | 팀원 목록을 확인합니다. |
| `/gw ability [player]` 또는 `/gw a [player]` | 본인 또는 같은 팀 플레이어의 현재 능력을 확인합니다. 관리자는 모든 플레이어를 볼 수 있습니다. |
| `/gw abilities [검색어]` | 등록된 능력 도감 GUI를 엽니다. |
| `/gw target <player>` | 타깃형 능력의 대상을 지정합니다. |
| `/gw yes` | 배정된 능력을 확정합니다. |
| `/gw no` | 남은 재추첨 횟수를 사용해 능력을 다시 뽑습니다. |
| `/gw gamble` 또는 `/도박` | 도박 GUI를 엽니다. |
| `/a` | 내 능력 GUI를 빠르게 엽니다. |
| `/x <player>` | 타깃형 능력 대상을 빠르게 지정합니다. |
| `/teamchat <message>` 또는 `/tc <message>` | 같은 팀에게만 메시지를 보냅니다. |
| `/teamchat` 또는 `/tc` | 팀 채팅 모드를 전환합니다. 켜진 동안 일반 채팅이 팀챗으로 전송됩니다. |

## 관리자 명령어

| 명령어 | 설명 |
| --- | --- |
| `/gw gui` 또는 `/gw settings` | 관리자 설정 GUI를 엽니다. |
| `/gw autoteam` | 온라인 플레이어를 활성 팀에 자동 배정합니다. |
| `/gw join <team> <player>` | 플레이어를 팀에 배정합니다. 진행 중에는 중간 참여로 처리될 수 있습니다. |
| `/gw changeteam <player> <team>` 또는 `/gw changeteam <team> <player>` | 진행 중 플레이어의 능력과 인벤토리를 유지한 채 팀만 변경합니다. |
| `/gw midjoin <player> [team|auto]` | 진행 중인 게임에 플레이어를 중간 참여시킵니다. |
| `/gw leave <player>` | 플레이어의 팀 배정을 해제합니다. |
| `/gw setspawn <team>` | 현재 위치를 팀 스폰으로 저장합니다. |
| `/gw setlobby` | 현재 위치를 접속/게임 종료 후 이동할 로비 위치로 저장합니다. |
| `/gw settemple <team>` | 바라보는 다이아몬드 블록을 팀 심장으로 저장합니다. |
| `/gw map [world|clear]` | 게임에 사용할 맵을 확인, 선택, 해제합니다. 월드 폴더만 있으면 자동 로드 후 선택합니다. |
| `/gw world help` 또는 `/gw help world` | 월드 생성, 로드, 이동, 백업, GUI 설정 도움말을 봅니다. |
| `/gw world gui` | 월드 전용 설정 GUI를 엽니다. |
| `/gw world list` | 현재 로드된 월드 목록을 확인합니다. |
| `/gw world game <world|clear>` | 게임 월드를 지정하거나 해제합니다. 지정된 월드는 시작 시 백업되고 종료 시 초기화됩니다. |
| `/gw world create <world> [normal|flat|void]` | 일반, 평지, 공허 월드를 생성하고 로드합니다. |
| `/gw world load <world> [normal|flat|void]` | 서버 폴더에 있는 월드를 로드하고 자동 로드 목록에 등록합니다. |
| `/gw world copy <sourceWorld> <newWorld> [normal|flat|void]` | 특정 월드를 기준으로 새 월드를 복사하고 로드합니다. |
| `/gw world tp <world> [player]` | 자신 또는 지정한 플레이어를 해당 월드 스폰으로 이동시킵니다. |
| `/gw world lobby [player]` | 자신 또는 지정한 플레이어를 저장된 로비 위치로 이동시킵니다. |
| `/gw world unload <world> [save]` | 플레이어가 없는 월드를 언로드합니다. 기본값은 저장입니다. |
| `/gw world delete <world> confirm` | 플레이어가 없는 월드를 언로드하고 월드 폴더를 삭제합니다. |
| `/gw world backup <create|list|load> [이름]` | 월드 백업 생성, 목록 확인, 새 월드로 로드를 처리합니다. |
| `/gw start` | 게임 시작 준비를 시작하고 능력을 배정합니다. |
| `/gw test [ability]` | 혼자 능력 테스트를 시작합니다. |
| `/gw stop` | 게임을 종료하고 적용된 게임룰/월드 설정을 복구합니다. |
| `/gw a set <player> <ability>` | 플레이어에게 특정 능력을 지정합니다. |
| `/gw a <ability> <player>` | 호환 순서로 플레이어에게 특정 능력을 지정합니다. |
| `/gw a list [검색어]` | 플레이어별 배정 능력을 확인합니다. |
| `/gw a catalog [검색어]` | 등록된 능력 도감을 능력 그룹에서 검색합니다. |
| `/gw a random [player]` | 특정 플레이어 또는 참가자 전체에게 랜덤 능력을 배정합니다. |
| `/gw a remove <player>` | 플레이어의 능력을 삭제합니다. |
| `/gw a reset [player]` | 특정 플레이어 또는 전체 능력 배정을 초기화합니다. |
| `/gw a skip [초]` | 능력 확정 대기를 종료하고 시작 카운트다운을 지정합니다. |
| `/gw a cutin <player> [team|auto]` | 진행 중 중간 참여를 능력 그룹에서 처리합니다. |
| `/gw participants [검색어|팀]` | 참가자, 팀, 능력, 킬, 관전 상태를 확인합니다. |
| `/gw clear [player]` | 능력 쿨타임을 초기화합니다. |
| `/gw rerolls <횟수>` | 능력 재추첨 가능 횟수를 설정합니다. |
| `/gw skip [초]` | 능력 확정 대기를 종료하고 시작 카운트다운을 지정합니다. |
| `/gw skipseconds <초>` | 자동/관리자 skip 기본 초를 설정합니다. |
| `/gw pickaxe [status]` | 현재 진행 시간과 곡괭이별 코어 파괴 허용 상태를 확인합니다. |
| `/gw pickaxe <wooden|stone|iron|diamond|all> <open|off|분>` | 곡괭이별 코어 파괴 허용 시간을 조정합니다. 금 곡괭이는 제외됩니다. `open`은 즉시 허용, `off`는 자동 해제 안 함입니다. |
| `/gw urf <on|off|toggle|퍼센트>` | 우르프 모드와 쿨타임 감소율을 설정합니다. |
| `/gw blacklist <list|add|remove|toggle> [ability]` | 랜덤 배정 제외 능력을 관리합니다. |
| `/gw gamerule <apply|restore>` | 설정된 게임룰을 수동 적용하거나 복구합니다. |
| `/gw spectate <player>` | 플레이어를 관전 상태로 전환합니다. |
| `/gw unspectate <player>` | 관전 상태를 해제합니다. |
| `/gw observer [list]` | 자신을 옵저버로 전환하거나 옵저버 목록을 봅니다. |
| `/gw gamblereward <normal> <번호|add> hand|message|<material> [값]` | 도박 보상 아이템 또는 멘트를 변경합니다. |
| `/gw defaultitems [gui|list|add|set|remove|clear|reset]` | 게임 시작 시 지급할 기본 아이템 창고를 열거나 목록을 관리합니다. |
| `/gw reload` | `config.yml`을 다시 불러오고 팀/스폰/심장 설정을 갱신합니다. |
| `/gw update [check|download]` | 최신 릴리즈를 확인하고 다음 서버 재시작 때 적용할 업데이트 jar를 다운로드합니다. |

## Themachy 호환 명령어

`/t`는 기존 Themachy 스타일 명령을 일부 지원합니다. 오른쪽의 `/gw` 명령이 원본입니다.

| 명령어 | `/gw` 원본 |
| --- | --- |
| `/t <team> <player>` | `/gw join <team> <player>` |
| `/t spawn <team>` 또는 `/t s <team>` | `/gw setspawn <team>` |
| `/t dia <team>` 또는 `/t d <team>` | `/gw settemple <team>` |
| `/t set` | `/gw settings` |
| `/t info [team]` | `/gw info [team]` |
| `/t a list [검색어]` | `/gw a list [검색어]` |
| `/t a <ability> <player>` | `/gw a set <player> <ability>` |
| `/t a random [player]` | `/gw a random [player]` |
| `/t a remove <player>` | `/gw a remove <player>` |
| `/t a reset [player]` | `/gw a reset [player]` |
| `/t a skip [초]` | `/gw a skip [초]` |
| `/t a cutin <player> [team|auto]` | `/gw a cutin <player> [team|auto]` |
| `/t observer [list]` | `/gw observer [list]` |
| `/t con` | `/gw gamble` |

## 값 입력 팁

- `<team>`은 `red`, `blue`, `green` 또는 설정된 표시 이름을 사용할 수 있습니다.
- `<ability>`는 능력 ID, 능력 이름, 또는 능력 목록 순번을 사용할 수 있습니다.
- 우르프 퍼센트는 `/gw urf 80%`처럼 입력할 수 있습니다. 이 값은 쿨타임 감소율이며, 내부 설정은 `cooldown-multiplier`로 저장됩니다.
- 기본 지급 아이템은 `/gw defaultitems`로 가상 창고를 열어 넣고 빼면 저장됩니다. 콘솔에서는 `/gw defaultitems list`, `/gw defaultitems set 1 LAVA_BUCKET 2`처럼 수정할 수 있습니다.
- 맵 선택은 `/gw map <world>`로 합니다. 선택된 맵의 팀 스폰과 심장은 맵별로 저장되므로, 새 맵을 선택한 뒤 `/gw setspawn <team>`, `/gw settemple <team>`로 한 번씩 설정하세요.
- 월드 설정은 `/gw world gui` 또는 `/gw gui`의 `월드` 메뉴에서 변경할 수 있습니다. 현재 월드를 게임 월드로 지정하거나 자동 초기화, 시작 난이도, 시작 시간을 조정할 수 있습니다.
- 월드 백업 로드는 안전을 위해 기존 월드 폴더에 바로 덮어쓰지 않습니다. 예를 들어 `/gw world backup load arena-1 reset-arena`는 백업을 `reset-arena` 월드로 새로 로드합니다.
