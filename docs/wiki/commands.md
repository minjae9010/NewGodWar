# 명령어

`/gw`만 입력하면 권한에 맞는 기본 명령어와 설명이 바로 나옵니다. `/gw yes`, `/gw start`처럼 짧게 입력하거나 `/gw <분류> <동작> [대상/값]` 형식으로 사용할 수 있습니다. 각 단계에서 Tab으로 다음 동작과 대상을 찾을 수 있습니다. `/godwar`, `/godswar`, `/신들의전쟁`도 같은 명령이며, `/t`는 Themachy 호환 입력을 함께 지원합니다.

## 도움말과 검색

- `/gw`, `/gw help` 또는 `/ghelp`: 자주 쓰는 명령어를 바로 보여줍니다. 일반 유저는 **내 능력**과 **팀·게임 정보**, 관리자는 **기본 플레이**와 **관리자 전용 게임 운영** 구역으로 나뉩니다.
- 일반 유저 첫 화면: `/a`, `/gw yes`, `/gw no`, `/gw abilities`, `/x`, `/tc`, `/gw info`, `/gw status`, `/gw tips`, `/gw gamble`.
- 관리자 첫 화면: `/a`, `/gw yes`, `/gw no`, `/tc`와 함께 `/gw gui`, `/gw autoteam`, `/gw start`, `/gw skip`, `/gw stop`, `/gw participants`, `/gw dummy`.
- `/gw help player [페이지]` 또는 `/gw help 유저 [페이지]`: 관리자도 유저 기본 명령만 따로 볼 수 있습니다.
- `/gw help all [페이지]`: 전체 명령 목록. 기존 `/gw help 2`도 전체 목록의 2페이지를 엽니다.
- `/gw help team`, `/gw help ability 2`: 해당 분류만 봅니다. `general`, `game`, `team`, `ability`, `settings`, `admin` 분류를 지원합니다.
- `/gw help 쿨타임`, `/gw help gkit`: 명령어 이름, 별칭, 설명으로 검색합니다.
- `/gw help world backup`, `/gw help ability cooldown`: 여러 단어로 검색하거나 하위 분류를 바로 봅니다.
- `/gw help 간편`: 간편 명령 목록. `/gw help 간편 2`로 페이지를 이동합니다.
- `/gw game help`, `/gw team help`, `/a help`, `/gw settings help`, `/gw setup help`, `/gw server help`: 각 분류의 하위 동작을 확인합니다.
- `/gw ability cooldown` 또는 `/gw ability cooldown help`: 쿨타임 관리의 다음 단계를 안내합니다.
- `/gw world help`, `/gw map help`: 월드·맵 상세 도움말. 기존 `/gw help world`, `/gw help map`도 지원합니다.
- `/gw gui help` 또는 `/gw help gui`: 설정 화면 바로가기 목록. `/gw gui help 2`로 다음 화면 목록을 봅니다.

상세 도움말은 페이지당 5개 항목으로 표시하며, **유저 명령**과 **관리자 전용**을 제목과 색으로 구분합니다. 첫 화면 아래 **더 보기**의 분류 버튼과 이전·다음·기본 명령 버튼은 클릭으로 이동하며, 명령어는 클릭하면 입력창에 들어갑니다. 마우스를 올리면 다른 입력 형식이나 간편 명령과 별칭을 볼 수 있습니다. 명령 클릭만으로 게임을 시작하거나 설정을 변경하지 않습니다. 콘솔에서는 이동 명령이 텍스트로 표시됩니다.

명령어를 잘못 입력하면 유사 명령과 용도를 한 줄씩 안내하고, 추천이 없으면 권한에 맞는 기본 명령을 보여줍니다. `/gw `에서 Tab을 누르면 기본 명령과 분류를 우선 표시하며, 글자를 입력하면 기존 별칭과 나머지 명령도 찾을 수 있습니다. 도움말과 자동완성에는 사용할 수 있는 권한의 명령만 표시합니다. `<값>`은 필수 인수, `[값]`은 선택 인수입니다.

## 설정 GUI 바로가기

`/gw gui <화면>`, `/gmenu <화면>`, `/gw settings open <화면>`은 같은 화면을 엽니다. 모든 화면은 관리자 플레이어 전용이며, 화면 이름과 팀 이름을 Tab으로 완성할 수 있습니다. 각 GUI 상단 안내 아이템에도 바로가기 명령이 표시됩니다.

| 화면 | 바로가기 | 한글 또는 짧은 별칭 |
| --- | --- | --- |
| 설정 메인 | `/gw gui main` 또는 `/gmenu` | `메인`, `home` |
| 게임 진행 | `/gw gui game` | `게임`, `진행` |
| 팀 목록 | `/gw gui team` | `팀`, `teams` |
| 특정 팀 상세 | `/gw gui team red` | `/gmenu team red`, `/gw 설정 열기 팀 red` |
| 월드 | `/gw gui world` | `월드`, `맵` |
| 코어 / 게임룰 | `/gw gui core` | `코어`, `게임룰`, `gamerule` |
| 코어 보호 / 곡괭이 시간 | `/gw gui protection` | `보호`, `곡괭이`, `pickaxe` |
| 표시 / 우르프 | `/gw gui display` | `표시`, `우르프`, `urf` |
| 도박 | `/gw gui gambling` | `도박`, `gamble` |
| 도박 확률 | `/gw gui rewards` | `보상`, `확률`, `chance` |
| 기본 지급 아이템 창고 | `/gw gui items` | `기본템`, `시작템`, `kit` |

예: `/gmenu protection`, `/gw settings open world`, `/gw gui team blue`. 팀 상세에서 뒤로 가면 팀 목록으로, 코어 보호에서 뒤로 가면 코어 설정으로 돌아갑니다.

`/gw settings urf 80%` 같은 기존 설정 값 변경 명령은 유지됩니다. 해당 GUI를 열려면 `/gw settings open urf` 또는 `/gmenu urf`를 쓰세요. 잘못된 화면·팀 이름은 안내만 표시하고 다른 화면을 열지 않습니다.

## 권한

| 권한 | 기본값 | 설명 |
| --- | --- | --- |
| `newgodwar.play` | 모든 플레이어 | 기본 플레이 명령 |
| `newgodwar.admin` | OP | 게임 운영, 설정, 강제 지정 명령 |

아래 표에서 **관리자**로 표시한 동작은 간편 명령이나 한글 별칭으로 실행해도 `newgodwar.admin` 권한이 필요합니다. 같은 팀의 능력만 볼 수 있는 제한도 모든 진입점에서 동일합니다.

## 게임 진행

| 단계형 명령어 | 간편 / 기존 명령 | 권한과 동작 |
| --- | --- | --- |
| `/gw game status` | `/gstatus`, `/gw st`, `/gw status` | 현재 상태 확인 |
| `/gw game start` | `/gstart`, `/gw go`, `/gw start` | 관리자: 게임 시작 및 능력 배정 |
| `/gw game stop` | `/gstop`, `/gw end`, `/gw stop` | 관리자: 게임 종료 |
| `/gw game test [ability]` | `/gw test [ability]` | 관리자: 혼자 능력 테스트 |
| `/gw game dummy [spawn\|remove]` | `/gw dummy`, `/gw 더미` | 관리자: 플레이어형 타깃 더미 소환 / 제거 |
| `/gw game skip [초]` | `/gskip [초]`, `/gw skip [초]` | 관리자: 능력 선택 대기 종료 및 시작 카운트다운 조정 |
| `/gw game tips` | `/gw tips`, `/gw 팁` | 서버 플레이 팁 |

한글 예: `/gw 게임 시작`, `/gw 게임 상태`, `/gw 게임 스킵 5`.

### 플레이어형 타깃 더미

1. `/gw test morpious`처럼 테스트할 능력을 선택합니다.
2. `/gw dummy`로 앞쪽 1~3블록의 빈 바닥 위에 더미를 세웁니다. `/gw dummy spawn`도 같습니다.
3. 채팅에 표시된 `/x GW_D_...`를 클릭해 입력하거나 Tab으로 더미 이름을 골라 타깃을 지정합니다.
4. 능력을 사용하거나 더미를 공격합니다. 시선·범위 탐색에도 적 플레이어로 인식됩니다.
5. `/gw dummy remove`로 본인의 더미를 제거합니다. `/gw 더미 제거`도 같습니다.

관리자마다 하나씩 소환하며 다시 소환하면 기존 더미를 교체합니다. 체력은 매 틱 회복되고 죽을 정도의 피해는 생존 가능한 양으로 제한됩니다. 더미는 일반 접속자 수, 자동 팀 배정, 참가 인원, 킬 점수에 포함되지 않습니다. 더미에 대한 공격은 킬타임의 유저 간 전투 제한을 받지 않으며, 실제 유저 간 판정은 그대로 유지됩니다. 소환한 관리자가 나가거나 다른 월드로 이동할 때, 게임 종료·플러그인 종료 시에도 제거됩니다. 능력의 비용·쿨타임은 유지되므로 필요하면 `/gw ability cooldown reset self`로 초기화하세요.

## 팀과 참가자

| 단계형 명령어 | 간편 / 기존 명령 | 권한과 동작 |
| --- | --- | --- |
| `/gw team info [team]` | `/gw i [team]`, `/gw info [team]` | 본인 또는 지정 팀의 팀원 확인 |
| `/gw team auto` | `/gautoteam`, `/gw at`, `/gw autoteam` | 관리자: 자동 팀 배정 |
| `/gw team join <team> <player>` | `/gjoin <team> <player>`, `/gw j <team> <player>` | 관리자: 팀 배정. 게임 진행 중에는 중간 참여로 처리 |
| `/gw team change <player> <team>` | `/gw ct <player> <team>`, `/gw changeteam ...` | 관리자: 능력·인벤토리를 유지한 팀 변경. 팀과 플레이어 순서 교환 가능 |
| `/gw team midjoin <player> [team\|auto]` | `/gw mj ...`, `/gw midjoin ...` | 관리자: 진행 중 중간 참여 |
| `/gw team leave <player>` | `/gw out <player>`, `/gw leave <player>` | 관리자: 팀 배정 해제 |
| `/gw team list [검색어\|팀]` | `/gplayers`, `/gw p`, `/gw participants` | 관리자: 참가자의 팀·능력·킬·관전 현황 |
| `/gw team spectate <player>` | `/gw spec <player>`, `/gw spectate <player>` | 관리자: 관전 전환 |
| `/gw team unspectate <player>` | `/gw unspec <player>` | 관리자: 관전 해제 |
| `/gw team observer [list]` | `/gw obs [list]`, `/gw observer [list]` | 관리자: 내 옵저버 모드 전환 / 목록 |
| `/tc [message]` | `/teamchat`, `/팀채팅` | 메시지 전송, 생략하면 팀 채팅 모드 전환 |

한글 예: `/gw 팀 자동`, `/gw 팀 배정 red Steve`, `/gw 팀 변경 Steve blue`.

## 능력

`/a`는 기존처럼 내 능력 GUI를 엽니다. 이제 `/a help`, `/a set ...`처럼 모든 능력 하위 명령도 사용할 수 있습니다. `/ability`, `/능력`도 같은 단축 명령입니다.

| 단계형 명령어 | 간편 / 기존 명령 | 권한과 동작 |
| --- | --- | --- |
| `/gw ability show [player]` | `/a [player]`, `/gw a [player]` | 본인/같은 팀 능력 확인. 관리자는 모든 플레이어 조회 |
| `/gw ability catalog [검색어]` | `/a catalog`, `/gw book`, `/gw abilities` | 능력 도감 검색 |
| `/gw ability confirm` | `/gconfirm`, `/gw y`, `/gw yes`, `/gw 확정` | 내 능력 확정 |
| `/gw ability reroll` | `/greroll`, `/gw rr`, `/gw n`, `/gw no` | 남은 횟수로 내 능력 다시 뽑기 |
| `/gw ability set <player> <ability>` | `/a set ...`, `/gw sa ...`, `/gw setability ...` | 관리자: 능력 수동 지정 |
| `/gw ability list [검색어]` | `/a list [검색어]`, `/gw assigned [검색어]` | 관리자: 배정 능력 목록 |
| `/gw ability random [player]` | `/a random [player]` | 관리자: 특정 플레이어에게 랜덤 능력 배정. 생략하면 참가자 전체 |
| `/gw ability remove <player>` | `/a remove <player>` | 관리자: 능력 삭제 |
| `/gw ability reset [player]` | `/a reset [player]` | 관리자: 능력 배정 초기화. 생략하면 전체 |
| `/gw ability skip [초]` | `/a skip [초]` | 관리자: 능력 선택 대기 종료 |
| `/gw ability cutin <player> [team\|auto]` | `/a cutin ...` | 관리자: 중간 참여 |
| `/gw ability target <player>` | `/gw target <player>`, `/x <player>`, `/gw 대상 <player>` | 타깃형 능력 대상 지정 |
| `/gw gamble` | `/도박`, `/gamble`, `/gw con` | 도박 GUI 열기 |

한글 예: `/gw 능력 지정 Steve zeus`, `/gw 능력 확정`, `/gw 능력 다시뽑기`.

### 쿨타임 초기화

쿨타임은 **능력 → 쿨타임 → 초기화 → 대상** 순서로 관리합니다. 능력 배정 자체를 지우는 `/a reset`과 구분됩니다. 모든 쿨타임 초기화는 관리자 권한이 필요합니다.

| 대상 | 단계형 명령 | 간편 명령 |
| --- | --- | --- |
| 본인 | `/gw ability cooldown reset` 또는 `/gw ability cooldown reset self` | `/gcd`, `/gw cd`, `/gw clear` |
| 특정 플레이어 | `/gw ability cooldown reset Steve` | `/gcd Steve`, `/gw clear Steve` |
| 전체 | `/gw ability cooldown reset all` | `/gcd all`, `/gw clear all` |

`/a cd reset all`, `/gw cooldown reset all`, `/gw 능력 쿨타임 초기화 전체`도 같습니다. 대상은 `self`/`본인`, 정확한 온라인 플레이어 이름, `all`/`전체`/`*`를 사용합니다. `all`, `self`는 대상 선택에 쓰는 예약어입니다.

**콘솔은 대상을 반드시 지정해야 합니다.** 기존 `/gw clear`의 콘솔 전체 초기화는 `/gw clear all`로 바꿔 입력하세요. 대상 이름 오타, 없는 플레이어, 대상 뒤 추가 인수가 있으면 아무 쿨타임도 초기화하지 않습니다.

## 설정과 서버 관리

| 단계형 명령어 | 간편 / 기존 명령 | 동작 (모두 관리자) |
| --- | --- | --- |
| `/gw settings open [화면] [team]` | `/gmenu [화면] [team]`, `/gw gui [화면] [team]` | 원하는 설정 GUI로 바로 이동 |
| `/gw settings items [gui\|list\|add\|set\|remove\|clear\|reset]` | `/gkit`, `/gw kit`, `/gw defaultitems` | 기본 지급 아이템 창고 / 목록 관리 |
| `/gw settings rerolls <횟수>` | `/gw rerolls`, `/gw reroll`, `/gw 재추첨` | 능력 재추첨 가능 횟수 설정 |
| `/gw settings skipseconds <초>` | `/gw skipseconds` | 기본 시작 카운트다운 설정 |
| `/gw settings pickaxe [status]` | `/gw pickaxe` | 곡괭이별 코어 파괴 허용 상태 확인 |
| `/gw settings pickaxe <wooden\|stone\|iron\|diamond\|all> <open\|off\|분>` | `/gw pickaxe ...` | 코어 파괴 시간 해제 설정. 금·네더라이트 곡괭이는 시간 설정 대상 아님 |
| `/gw settings urf <on\|off\|toggle\|퍼센트>` | `/gw urf ...` | 우르프 모드 / 쿨타임 감소율 |
| `/gw settings blacklist <list\|add\|remove\|toggle> [ability]` | `/gw bl ...`, `/gw blacklist ...` | 랜덤 배정 제외 능력 관리 |
| `/gw settings gamerule <apply\|restore>` | `/gw gamerule ...` | 게임룰 수동 적용 / 복구 |
| `/gw settings rewards <normal> <번호\|add> <hand\|message\|material> [값]` | `/gw gamblereward ...` | 도박 보상 변경 |
| `/gw server reload` | `/gw rl`, `/gw reload` | 설정 다시 불러오기 |
| `/gw server update [check\|download]` | `/gw update ...` | 최신 릴리즈 확인 / 다음 재시작용 다운로드 |

예: `/gw settings items set 1 LAVA_BUCKET 2`, `/gw 설정 기본템`, `/gw 서버 리로드`.

`/gw reroll <횟수>`는 기존대로 **관리자의 횟수 설정**입니다. 플레이어가 다시 뽑으려면 `/greroll`, `/gw rr`, `/a reroll`을 사용하세요.

## 맵과 월드

아래 명령은 모두 관리자용입니다. `/gw world`는 `/gworld`, `/gw w`, `/gw 월드`로 줄일 수 있습니다.

| 명령어 | 동작 |
| --- | --- |
| `/gw setup spawn <team>` | 현재 위치를 팀 스폰으로 저장. 기존 `/gw s <team>`, `/gw setspawn <team>` 지원 |
| `/gw setup temple <team>` | 바라보는 다이아 블록을 심장으로 저장. 기존 `/gw d <team>`, `/gw settemple <team>` 지원 |
| `/gw setup lobby` | 현재 위치를 로비로 저장. 기존 `/gw sl`, `/gw setlobby`, `/gw lobby` 지원 |
| `/gw map [world\|clear\|help]` 또는 `/gmap ...` | 게임 맵 목록, 선택, 해제, 도움말 |
| `/gw world gui` | 월드 전용 설정 GUI |
| `/gw world list` | 로드된 월드 목록 |
| `/gw world game <world\|clear>` | 게임 월드 지정 / 해제 |
| `/gw world create <world> [normal\|flat\|void]` | 새 월드 생성 |
| `/gw world load <world> [normal\|flat\|void]` | 기존 월드 로드 |
| `/gw world copy <sourceWorld> <newWorld> [normal\|flat\|void]` | 월드 복사 |
| `/gw world tp <world> [player]` | 본인 또는 지정 플레이어 이동 |
| `/gw world lobby [player]` | 저장된 로비로 이동 |
| `/gw world unload <world> [save]` | 플레이어가 없는 월드 언로드 (기본 저장) |
| `/gw world delete <world> confirm` | 플레이어가 없는 월드를 언로드하고 폴더 삭제 |
| `/gw world backup <create\|list\|load> [이름]` | 백업 생성, 목록, 새 월드로 로드 |

`/gw lobby`는 **현재 위치를 로비로 등록**하는 기존 명령입니다. 로비로 이동하려면 `/gw world lobby`를 사용하세요.

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
| `/t a cutin <player> [team\|auto]` | `/gw a cutin <player> [team\|auto]` |
| `/t observer [list]` | `/gw observer [list]` |
| `/t con` | `/gw gamble` |

## 값 입력 팁

- `<team>`은 `red`, `blue`, `green` 또는 설정된 표시 이름을 사용할 수 있습니다.
- `<ability>`는 능력 ID, 능력 이름, 또는 능력 목록 순번을 사용할 수 있습니다.
- 우르프 퍼센트는 `/gw urf 80%`처럼 입력할 수 있습니다. 이 값은 쿨타임 감소율이며, 내부 설정은 `cooldown-multiplier`로 저장됩니다.
- 기본 지급 아이템은 `/gw defaultitems`로 가상 창고를 열어 넣고 빼면 저장됩니다. 콘솔에서는 `/gw defaultitems list`, `/gw defaultitems set 1 LAVA_BUCKET 2`처럼 수정할 수 있습니다.
- 맵 선택은 `/gw map <world>`로 합니다. 선택된 맵의 팀 스폰과 심장은 맵별로 저장되므로, 새 맵을 선택한 뒤 `/gw setspawn <team>`, `/gw settemple <team>`로 한 번씩 설정하세요.
- 월드 설정은 `/gw world gui` 또는 `/gw gui`의 `월드` 메뉴에서 변경할 수 있습니다. 현재 월드를 게임 월드로 지정하거나 자동 초기화, 시작 난이도, 시작 시간을 조정할 수 있습니다.
- 월드 백업 로드는 기존 월드 폴더에 바로 덮어쓰지 않습니다. `/gw world backup load arena-1 reset-arena`는 백업에 월드가 하나면 `reset-arena`로, 여러 개면 `reset-arena-<원본월드명>`으로 모두 로드합니다. 이름을 생략하면 `ngw-<백업이름>`을 기준으로 같은 규칙을 적용합니다. 생성된 이름은 `/gw world list`에서 확인하세요.
