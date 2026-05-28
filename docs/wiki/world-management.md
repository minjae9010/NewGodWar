# 월드 관리

NewGodWar는 로비 월드와 게임 월드를 분리해서 운영할 수 있습니다. 게임 월드를 지정하면 게임 시작 직전에 스냅샷을 만들고, 게임 종료 후 로비로 플레이어를 돌린 다음 게임 월드를 복원합니다.

## 빠른 사용법

| 목적 | 방법 |
| --- | --- |
| 월드 도움말 확인 | `/godwar world help` 또는 `/godwar help world` |
| 월드 목록 확인 | `/godwar world list` |
| 현재 위치를 로비로 저장 | `/godwar setlobby` |
| 게임 맵 선택 | `/godwar map <world>` |
| 게임 맵 선택 해제 | `/godwar map clear` |
| 월드 설정 GUI 열기 | `/godwar world gui` |
| 전체 설정 GUI 열기 | `/godwar gui` 또는 `/godwar settings` |

`/godwar map <world>`는 로드된 월드를 선택합니다. 월드 폴더만 있고 아직 로드되지 않은 경우에는 자동으로 로드한 뒤 선택합니다. 선택된 맵의 팀 스폰과 심장은 `maps.<world>` 아래에 별도로 저장되므로, 새 맵을 만들면 해당 맵에서 `/godwar setspawn <team>`과 `/godwar settemple <team>`을 다시 지정하세요.

설정 GUI에서는 `월드` 메뉴를 열어 자동 저장, 동물/몬스터 스폰, 시작 시간, 시작 난이도, 게임 월드 자동 초기화, 현재 월드를 게임 월드로 지정/해제를 조정할 수 있습니다.

## GUI 설정

1. 관리자로 접속하고 `/godwar world gui`를 실행합니다.
2. 필요한 항목을 클릭해 값을 바꿉니다.
3. 게임 월드로 쓸 월드에 서서 `현재 월드를 게임 월드로 지정`을 클릭합니다.
4. 자동 초기화를 끄고 싶으면 `게임 월드 자동 초기화`를 꺼둡니다.

전체 설정 GUI에서 들어가려면 `/godwar gui`를 실행한 뒤 `월드` 메뉴를 클릭합니다.

`게임 시작 월드 시간` 항목은 클릭하면 현재 월드 시간을 저장합니다. 좌우의 시간 버튼은 1000틱 단위, Shift 클릭은 6000틱 단위로 조정합니다. 난이도는 `PEACEFUL`, `EASY`, `NORMAL`, `HARD` 순서로 순환합니다.

## 월드 생성과 로드

| 명령어 | 설명 |
| --- | --- |
| `/godwar world create <world> [normal|flat|void]` | 새 월드를 생성하고 로드합니다. |
| `/godwar world load <world> [normal|flat|void]` | 서버 폴더에 있는 월드를 로드하고 자동 로드 목록에 등록합니다. |
| `/godwar world copy <sourceWorld> <newWorld> [normal|flat|void]` | 기존 월드 폴더를 복사해 새 월드로 로드합니다. |

`void` 타입은 공허 월드 생성용입니다. `normal`, `flat`, `void`를 생략하면 일반 월드로 처리되거나 저장된 관리 월드 타입을 사용합니다.

## 이동과 정리

| 명령어 | 설명 |
| --- | --- |
| `/godwar world tp <world> [player]` | 자신 또는 지정 플레이어를 해당 월드 스폰으로 이동합니다. |
| `/godwar world lobby [player]` | 저장된 로비 위치로 이동합니다. |
| `/godwar world unload <world> [save]` | 플레이어가 없는 월드를 언로드합니다. 기본값은 저장입니다. |
| `/godwar world delete <world> confirm` | 플레이어가 없는 월드를 언로드하고 월드 폴더를 삭제합니다. |

기본 월드와 로비 월드는 보호 대상이라 언로드/삭제할 수 없습니다. 삭제는 되돌릴 수 없으므로 먼저 백업을 만드는 것을 권장합니다.

## 백업과 복원

| 명령어 | 설명 |
| --- | --- |
| `/godwar world backup create [이름]` | 현재 로드된 모든 월드를 `plugins/NewGodWar/world-backups/`에 백업합니다. |
| `/godwar world backup list` | 저장된 백업 목록을 확인합니다. |
| `/godwar world backup load <백업이름> [로드월드이름]` | 백업을 새 월드 폴더로 복사해 로드합니다. |

백업 로드는 기존 월드에 직접 덮어쓰지 않습니다. 예를 들어 `/godwar world backup load arena-1 reset-arena`는 `arena-1` 백업을 `reset-arena` 월드로 새로 로드합니다.

## 운영 주의사항

- 로비 월드는 게임 월드로 지정할 수 없습니다.
- 맵마다 팀 스폰과 심장을 따로 설정해야 합니다.
- 게임 월드 자동 초기화는 로비 위치가 있어야 안전하게 동작합니다.
- 게임 종료 시 게임 월드에 플레이어가 남아 있으면 안전을 위해 복원을 건너뛸 수 있습니다.
- 월드를 삭제하거나 언로드하기 전에는 `/godwar world lobby [player]` 또는 `/godwar world tp <world> [player]`로 플레이어를 다른 월드로 이동하세요.
