# 명령어

기본 명령어는 `/godwar`이며 alias로 `/gw`, `/godswar`, `/신들의전쟁`을 사용할 수 있습니다. `/t`는 Themachy 스타일 호환 명령어로 별도 제공되며, `/t`에 있는 기능은 `/gw` 원본 명령으로도 사용할 수 있습니다. 별도 단축 명령으로 `/a`, `/x`, `/teamchat`, `/도박`도 제공됩니다.

## 권한

| 권한 | 기본값 | 설명 |
| --- | --- | --- |
| `newgodwar.play` | 모든 플레이어 | 기본 플레이 명령 |
| `newgodwar.admin` | OP | 게임 운영, 설정, 강제 지정 명령 |

팀 참가, 팀 해제, 중간 참여, 능력, 관전 상태를 바꾸는 운영 명령은 `newgodwar.admin` 권한이 필요합니다.

## 플레이어 명령어

| 명령어 | 설명 |
| --- | --- |
| `/godwar help [페이지|섹션]` | 도움말을 봅니다. |
| `/godwar status` | 현재 게임 상태와 주요 설정을 확인합니다. |
| `/godwar tips` | 서버 플레이 팁을 확인합니다. |
| `/godwar info [team]` | 팀원 목록을 확인합니다. |
| `/godwar ability [player]` 또는 `/godwar a [player]` | 본인 또는 같은 팀 플레이어의 현재 능력을 확인합니다. 관리자는 모든 플레이어를 볼 수 있습니다. |
| `/godwar abilities [검색어]` | 등록된 능력 도감 GUI를 엽니다. |
| `/godwar target <player>` | 타깃형 능력의 대상을 지정합니다. |
| `/godwar yes` 또는 `/t yes` | 배정된 능력을 확정합니다. |
| `/godwar no` 또는 `/t no` | 남은 재추첨 횟수를 사용해 능력을 다시 뽑습니다. |
| `/godwar gamble` 또는 `/도박` | 도박 GUI를 엽니다. |
| `/a` | 내 능력 GUI를 빠르게 엽니다. |
| `/x <player>` | 타깃형 능력 대상을 빠르게 지정합니다. |
| `/teamchat <message>` 또는 `/tc <message>` | 같은 팀에게만 메시지를 보냅니다. |
| `/teamchat` 또는 `/tc` | 팀 채팅 모드를 전환합니다. 켜진 동안 일반 채팅이 팀챗으로 전송됩니다. |

## 관리자 명령어

| 명령어 | 설명 |
| --- | --- |
| `/godwar gui` 또는 `/godwar settings` | 관리자 설정 GUI를 엽니다. |
| `/godwar autoteam` | 온라인 플레이어를 활성 팀에 자동 배정합니다. |
| `/godwar join <team> <player>` | 플레이어를 팀에 배정합니다. 진행 중에는 중간 참여로 처리될 수 있습니다. |
| `/godwar midjoin <player> [team|auto]` | 진행 중인 게임에 플레이어를 중간 참여시킵니다. |
| `/godwar leave <player>` | 플레이어의 팀 배정을 해제합니다. |
| `/godwar setspawn <team>` | 현재 위치를 팀 스폰으로 저장합니다. |
| `/godwar settemple <team>` | 바라보는 다이아몬드 블록을 팀 심장으로 저장합니다. |
| `/godwar start` | 게임 시작 준비를 시작하고 능력을 배정합니다. |
| `/godwar test [ability]` | 혼자 능력 테스트를 시작합니다. |
| `/godwar stop` | 게임을 종료하고 적용된 게임룰/월드 설정을 복구합니다. |
| `/godwar a set <player> <ability>` | 플레이어에게 특정 능력을 지정합니다. |
| `/godwar a list [검색어]` | 플레이어별 배정 능력을 확인합니다. |
| `/godwar a catalog [검색어]` | 등록된 능력 도감을 능력 그룹에서 검색합니다. |
| `/godwar a random [player]` | 특정 플레이어 또는 참가자 전체에게 랜덤 능력을 배정합니다. |
| `/godwar a remove <player>` | 플레이어의 능력을 삭제합니다. |
| `/godwar a reset [player]` | 특정 플레이어 또는 전체 능력 배정을 초기화합니다. |
| `/godwar a skip [초]` | 능력 확정 대기를 종료하고 시작 카운트다운을 지정합니다. |
| `/godwar a cutin <player> [team|auto]` | 진행 중 중간 참여를 능력 그룹에서 처리합니다. |
| `/godwar participants [검색어|팀]` | 참가자, 팀, 능력, 킬, 관전 상태를 확인합니다. |
| `/godwar clear [player]` | 능력 쿨타임을 초기화합니다. |
| `/godwar rerolls <횟수>` | 능력 재추첨 가능 횟수를 설정합니다. |
| `/godwar skip [초]` | 능력 확정 대기를 종료하고 시작 카운트다운을 지정합니다. |
| `/godwar skipseconds <초>` | 자동/관리자 skip 기본 초를 설정합니다. |
| `/godwar urf <on|off|toggle|퍼센트>` | 우르프 모드와 쿨타임 감소율을 설정합니다. |
| `/godwar blacklist <list|add|remove|toggle> [ability]` | 랜덤 배정 제외 능력을 관리합니다. |
| `/godwar gamerule <apply|restore>` | 설정된 게임룰을 수동 적용하거나 복구합니다. |
| `/godwar spectate <player>` | 플레이어를 관전 상태로 전환합니다. |
| `/godwar unspectate <player>` | 관전 상태를 해제합니다. |
| `/godwar observer [list]` | 자신을 옵저버로 전환하거나 옵저버 목록을 봅니다. |
| `/godwar gamblereward <normal|tajja> <번호|add> hand|message|<material> [값]` | 도박 보상 아이템 또는 멘트를 변경합니다. |
| `/godwar defaultitems [gui|list|add|set|remove|clear|reset]` | 게임 시작 시 지급할 기본 아이템 창고를 열거나 목록을 관리합니다. |
| `/godwar reload` | `config.yml`을 다시 불러오고 팀/스폰/심장 설정을 갱신합니다. |
| `/godwar update [check|download]` | 최신 릴리즈를 확인하고 다음 서버 재시작 때 적용할 업데이트 jar를 다운로드합니다. |

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
- 우르프 퍼센트는 `/godwar urf 80%`처럼 입력할 수 있습니다. 이 값은 쿨타임 감소율이며, 내부 설정은 `cooldown-multiplier`로 저장됩니다.
- 기본 지급 아이템은 `/godwar defaultitems`로 가상 창고를 열어 넣고 빼면 저장됩니다. 콘솔에서는 `/godwar defaultitems list`, `/godwar defaultitems set 1 LAVA_BUCKET 2`처럼 수정할 수 있습니다.
