# 문제 해결

운영 중 자주 막히는 지점을 증상별로 정리했습니다.

## 플러그인이 로드되지 않음

확인할 것:

- 서버가 Paper/Spigot 계열인지 확인합니다.
- 서버 버전에 맞는 Java로 실행 중인지 확인합니다.
- `plugins/NewGodWar-<version>.jar`가 릴리즈 Assets 또는 `build/libs/`에서 가져온 배포용 파일인지 확인합니다. `*-plain.jar`와 회귀 테스트용 Probe JAR를 설치하지 마세요.
- 콘솔에 `UnsupportedClassVersionError`, `NoClassDefFoundError`, `ClassNotFoundException`이 있는지 확인합니다.
- 서버 로그에 `NewGodWar enabled`가 출력되는지 확인합니다.

직접 빌드한 JAR는 아래의 **호환성 스모크 테스트** 절차로 확인할 수 있습니다.

## `/gw start`가 실패함

대부분 시작 설정 누락입니다.

- 활성 팀마다 `/gw setspawn <team>`이 필요합니다.
- 활성 팀마다 `/gw settemple <team>`이 필요합니다.
- 심장으로 등록한 블록은 현재도 다이아몬드 블록이어야 합니다.
- 팀 배정 인원이 `game.min-players` 이상이어야 합니다.
- 게임이 이미 준비중 또는 진행중이면 다시 시작할 수 없습니다.
- `game.mode`가 애드온 모드 ID이면 해당 애드온이 설치·활성화되어 있어야 합니다. 기본 코어전은 `default`를 사용합니다.
- 이전 게임 월드 복원이 끝나지 않았다는 안내가 나오면 남은 플레이어를 다른 월드로 이동시키고 `/gw stop`을 다시 실행합니다.
- `Game recovery failed` 로그가 나오면 `game-session.yml`과 월드 스냅샷을 보존하고, 로그에 나온 누락 월드·능력·팀 또는 손상 데이터를 해결한 뒤 재시작합니다.

상태 확인은 `/gw status`와 `/gw participants`를 사용합니다.

## 팀 배정이나 중간 참여가 안 됨

- 비활성 팀에는 배정할 수 없습니다.
- 진행 중 중간 참여는 `game.allow-mid-join`이 켜져 있어야 합니다.
- 팀 배정, 팀 해제, 중간 참여는 관리자가 대상 플레이어를 지정해야 합니다.
- 이미 살아있는 팀에서 능력을 가진 플레이어는 중간 참여 대상이 아닙니다.
- 탈락한 팀에는 중간 참여할 수 없습니다.
- 운영 명령을 사용하려면 `newgodwar.admin` 권한이 필요합니다.

## 능력이 발동하지 않음

- 대부분의 일반/고급 능력은 블레이즈 막대를 들고 좌클릭/우클릭으로 사용합니다.
- 조약돌 비용이 부족하면 발동하지 않습니다.
- 쿨타임이 남아 있으면 발동하지 않습니다.
- 기본 설정에서는 게임 시작 후 300초 동안 유저 간 피해를 차단합니다. 피해형 능력은 `game.killtime-mode`와 남은 킬타임을 확인하세요. `/gw test`에도 적용됩니다.
- 타깃형 능력은 먼저 `/x <player>`로 대상을 지정해야 합니다.
- 일부 능력은 채팅, 팻말, 활, 낚시, 피격, 이동 같은 별도 조건으로 발동됩니다.
- `/a`로 현재 능력 설명과 쿨타임을 확인합니다.

### 부두술사 팻말에 이름을 적어도 연결되지 않음

- 자작나무를 포함한 팻말의 첫 줄에 접속 중인 적의 정확한 플레이어 이름을 적습니다. 이름 앞뒤 공백은 무시합니다.
- 대상은 같은 월드에서 게임에 참가 중인 적이어야 합니다. 자신, 아군, 관전자, 탈락자는 연결할 수 없습니다.
- `game.killtime-mode: player-combat`의 킬타임에는 연결할 수 없습니다. `/gw test`에도 킬타임 설정이 적용됩니다.
- 실패 시 대상 이름, 킬타임, 조약돌 부족 등 이유를 안내합니다. `abilities.messages.enabled`와 `abilities.messages.failure`를 끄면 실패 안내도 표시되지 않습니다.
- 연결 성공 후 7초 동안 팻말을 좌클릭합니다. 타격으로 팻말이 파괴되지 않으며 종료 시 자동 제거됩니다.

## 심장이 파괴되지 않음

설정에 따라 정상 동작일 수 있습니다.

- 자기 팀 심장은 파괴할 수 없습니다.
- 참가 중인 생존 팀 플레이어만 파괴할 수 있습니다. 관전자·비참가자·탈락자는 파괴할 수 없습니다.
- `game.killtime-mode: core-only`의 킬타임에는 직접 파괴와 폭발 파괴가 모두 차단됩니다.
- `core.require-empty-hand: true`이면 시간 해제된 곡괭이를 제외하고 손에 아이템이 없어야 합니다.
- `core.pickaxe-unlock.*-seconds`가 `0` 이상인 곡괭이는 지정 시간이 지나면 맨손 제한의 예외로 사용할 수 있습니다.
- 금·네더라이트 곡괭이는 시간 해제 항목이 없으며 맨손 제한이 켜져 있으면 사용할 수 없습니다. 맨손 제한을 끄면 사용할 수 있습니다.
- `core.forbid-diamond-pickaxe: true`이면 다이아몬드 곡괭이는 금지됩니다. 다이아 곡괭이 시간 해제가 끝난 뒤에는 허용됩니다.
- 등록되지 않은 다이아몬드 블록은 팀 탈락을 발생시키지 않습니다.
- 폭발로 심장이 제거되지 않는 것은 `core.protect-diamond-from-explosion` 설정 때문입니다. `core.explosion-unlock-seconds` 시간이 지난 뒤에는 폭발 파괴가 허용될 수 있습니다.

## 도박 GUI가 열리지 않거나 뽑기가 안 됨

- `gambling.enabled`가 `true`인지 확인합니다.
- GUI는 비용이 없어도 열립니다. 보상 뽑기를 누를 때 조약돌 비용을 검사하며 기본값은 32개입니다.
- `/도박` 또는 `/gw gamble`을 사용합니다.
- 보상 설정을 직접 수정했다면 YAML 들여쓰기와 material 이름을 확인합니다.

## config를 바꿨는데 반영되지 않음

`/gw reload`를 실행하거나 서버를 재시작하세요. 팀 설정, 스폰, 심장 정보는 reload 시 다시 읽습니다.

게임이 진행 중일 때 일부 값은 즉시 바뀌어도 이미 배정된 능력이나 진행 중인 타이머에는 영향을 주지 않을 수 있습니다. 확실히 반영하려면 게임 종료 후 수정하는 편이 좋습니다.

`game.recovery-save-interval-seconds`는 서버 시작 시 반복 작업에 적용되므로 변경 후 서버를 재시작해야 합니다. 애드온 JAR 교체도 서버 재시작이 필요하며 `/gw reload`로 다시 로드되지 않습니다.

## 호환성 스모크 테스트

빌드는 JDK 21로 실행하고 서버 테스트에는 대상 버전에 맞는 Java를 지정하세요. `-JavaExecutable`은 테스트 서버용이며 Gradle 빌드용 Java를 바꾸지 않습니다. 다음은 최신 지원 서버(Java 25) 예제입니다. `$serverJava`는 실제 설치 경로로 바꾸세요.

```powershell
$serverJava = "C:\Java\jdk-25\bin\java.exe"
.\gradlew.bat build
.\scripts\Test-PaperMatrix.ps1 -SkipBuild -LatestVersion -JavaExecutable $serverJava
```

구버전은 `-Versions 1.12.2`처럼 버전을 선택하고 해당 버전에 맞는 Java를 지정합니다. 인수를 생략하면 대표 버전들을, `-AllSupportedVersions`를 지정하면 지원 목록 전체를 선택합니다.

```powershell
.\scripts\Test-PaperMatrix.ps1 -SkipBuild -Versions 1.12.2 -JavaExecutable "C:\Java\jdk-8\bin\java.exe"
```

한 번의 실행에서는 모든 서버에 같은 Java 실행 파일을 사용하므로, 여러 Java 버전이 필요한 검사는 버전별로 나누거나 저장소의 `Compatibility` GitHub Actions 매트릭스를 사용하세요.

이미 빌드된 jar 확인 (`-PluginJar`는 실제 빌드한 파일 경로로 지정):

```powershell
.\scripts\Test-PaperMatrix.ps1 -SkipBuild -PluginJar .\build\libs\NewGodWar-0.3.3.jar -LatestVersion -JavaExecutable $serverJava
```

테스트 서버 파일은 `.paper-smoke/` 아래에 생성됩니다.

부두술사의 팻말 종류별 연결·타격 및 공통 타깃 검증:

```powershell
.\gradlew.bat build
.\scripts\Test-PaperMatrix.ps1 -SkipBuild -Versions 26.3 -JavaExecutable $serverJava -ProbeJar .\plugin\build\ability-regression\AbilityRegressionProbe.jar -ProbeSuccessMarker "ABILITY REGRESSION PASS"
```

회귀 테스트 플러그인은 임시 테스트 서버 전용입니다.
