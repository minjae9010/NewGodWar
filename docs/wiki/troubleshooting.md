# 문제 해결

운영 중 자주 막히는 지점을 증상별로 정리했습니다.

## 플러그인이 로드되지 않음

확인할 것:

- 서버가 Paper/Spigot 계열인지 확인합니다.
- 서버 버전에 맞는 Java로 실행 중인지 확인합니다.
- `plugins/NewGodWar.jar`가 실제 플러그인 jar인지 확인합니다.
- 콘솔에 `UnsupportedClassVersionError`, `NoClassDefFoundError`, `ClassNotFoundException`이 있는지 확인합니다.
- 서버 로그에 `NewGodWar enabled`가 출력되는지 확인합니다.

직접 빌드한 jar를 테스트하려면 다음을 실행합니다.

```powershell
.\scripts\Test-PaperMatrix.ps1 -LatestVersion
```

## `/godwar start`가 실패함

대부분 시작 설정 누락입니다.

- 활성 팀마다 `/godwar setspawn <team>`이 필요합니다.
- 활성 팀마다 `/godwar settemple <team>`이 필요합니다.
- 심장으로 등록한 블록은 현재도 다이아몬드 블록이어야 합니다.
- 팀 배정 인원이 `game.min-players` 이상이어야 합니다.
- 게임이 이미 준비중 또는 진행중이면 다시 시작할 수 없습니다.

상태 확인은 `/godwar status`와 `/godwar participants`를 사용합니다.

## 팀 배정이나 중간 참여가 안 됨

- 비활성 팀에는 배정할 수 없습니다.
- 진행 중 중간 참여는 `game.allow-mid-join`이 켜져 있어야 합니다.
- 이미 살아있는 팀에서 능력을 가진 플레이어는 중간 참여 대상이 아닙니다.
- 탈락한 팀에는 중간 참여할 수 없습니다.
- 다른 플레이어를 배정하려면 `newgodwar.admin` 권한이 필요합니다.

## 능력이 발동하지 않음

- 대부분의 일반/고급 능력은 블레이즈 막대를 들고 좌클릭/우클릭으로 사용합니다.
- 조약돌 비용이 부족하면 발동하지 않습니다.
- 쿨타임이 남아 있으면 발동하지 않습니다.
- 타깃형 능력은 먼저 `/x <player>`로 대상을 지정해야 합니다.
- 일부 능력은 채팅, 팻말, 활, 낚시, 피격, 이동 같은 별도 조건으로 발동됩니다.
- `/a`로 현재 능력 설명과 쿨타임을 확인합니다.

## 심장이 파괴되지 않음

설정에 따라 정상 동작일 수 있습니다.

- 자기 팀 심장은 파괴할 수 없습니다.
- `core.require-empty-hand: true`이면 손에 아이템이 없어야 합니다.
- `core.pickaxe-unlock.*-seconds`가 설정된 곡괭이는 게임 시작 후 해당 시간이 지나야 코어 파괴에 사용할 수 있습니다.
- `core.forbid-diamond-pickaxe: true`이면 다이아몬드 곡괭이는 금지됩니다. 다이아 곡괭이 시간 해제가 끝난 뒤에는 허용됩니다.
- 등록되지 않은 다이아몬드 블록은 팀 탈락을 발생시키지 않습니다.
- 폭발로 심장이 제거되지 않는 것은 `core.protect-diamond-from-explosion` 설정 때문입니다.

## 도박 GUI가 열리지 않음

- `gambling.enabled`가 `true`인지 확인합니다.
- 플레이어가 조약돌 비용을 가지고 있는지 확인합니다.
- `/도박`, `/godwar gamble`, `/t con` 중 하나를 사용합니다.
- 보상 설정을 직접 수정했다면 YAML 들여쓰기와 material 이름을 확인합니다.

## config를 바꿨는데 반영되지 않음

`/godwar reload`를 실행하거나 서버를 재시작하세요. 팀 설정, 스폰, 심장 정보는 reload 시 다시 읽습니다.

게임이 진행 중일 때 일부 값은 즉시 바뀌어도 이미 배정된 능력이나 진행 중인 타이머에는 영향을 주지 않을 수 있습니다. 확실히 반영하려면 게임 종료 후 수정하는 편이 좋습니다.

## 호환성 스모크 테스트

기본 대표 버전만 확인:

```powershell
.\scripts\Test-PaperMatrix.ps1
```

지원 목록 전체 확인:

```powershell
.\scripts\Test-PaperMatrix.ps1 -AllSupportedVersions
```

이미 빌드된 jar 확인:

```powershell
.\scripts\Test-PaperMatrix.ps1 -SkipBuild -PluginJar .\build\libs\NewGodWar-0.1.4.jar -LatestVersion
```

테스트 서버 파일은 `.paper-smoke/` 아래에 생성됩니다.
