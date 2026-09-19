# 애드온 설치·개발

능력과 게임 기능을 별도 Bukkit 플러그인으로 추가할 수 있습니다. API 버전은 `NewGodWarApi.VERSION = 1`입니다. 이 API를 포함한 NewGodWar 빌드가 필요합니다.

## 서버에 설치

1. 서버를 종료합니다.
2. 본체 JAR는 `plugins/`에, 애드온 JAR는 **`plugins/NewGodWar/addon/`**에 넣습니다. 본체가 폴더를 자동 생성합니다.
3. 서버를 시작하고 애드온 활성화 로그를 확인합니다.
4. `/gw abilities`에서 추가 능력을 확인합니다. 예제는 `/gw test example_windrunner`로 테스트할 수 있습니다.

본체가 시작 후 `addon/`의 JAR를 Bukkit 플러그인 로더로 읽고 활성화합니다. `plugin.yml`과 `depend: [NewGodWar]`가 필요합니다. 애드온끼리의 필수 의존성도 활성화 순서에 반영하며, 누락·순환 의존성이나 중복 플러그인 이름은 로그에 기록하고 건너뜁니다. 같은 애드온을 `plugins/`에도 중복 설치하지 마세요. 제거·업데이트는 서버를 종료한 뒤 JAR를 교체하세요. `/gw reload`는 설정만 갱신하며 JAR를 다시 로드하지 않습니다. 애드온은 서버에서 Java 코드를 실행하므로 신뢰하는 제작자의 파일을 설치하세요.

## 실행 가능한 예제

저장소의 `examples/addon`은 능력과 게임 기능을 모두 포함합니다. 아래 JAR 경로는 `0.3.3` 기준입니다. 다른 버전을 빌드하면 저장소 루트 `build.gradle`의 `version`에 맞춰 경로를 바꾸세요.

```powershell
.\gradlew.bat build :example-addon:build
```

- 본체: `build/libs/NewGodWar-0.3.3.jar`
- 예제: `examples/addon/build/libs/NewGodWar-ExampleAddon-0.3.3.jar`
- 능력: 바람 주자. 블레이즈 막대 좌클릭으로 조약돌 4개를 소모해 5초간 신속 II, 쿨타임 15초.
- 게임 기능: 실제 게임 시작 또는 테스트 시작 후 참가자에게 안내 메시지 표시.
- 교체 모드: `game.mode: example_timed`로 설정하면 `/gw start`가 코어·팀 준비 없이 60초 자유 게임을 시작합니다. `/gw stop`으로 조기 종료할 수도 있습니다. 이 예제는 승리 점수나 능력 자동 배정이 없는 최소 구현입니다.

예제는 본체 JAR 안에 포함되지 않습니다. 설치한 서버에서만 기능이 추가됩니다.

## 독립 프로젝트로 만들기

예제의 `src/`를 복사하고 Gradle Java 프로젝트에 다음 의존성을 설정하세요. 본체 JAR는 프로젝트의 `libs/`에 두세요.

```groovy
plugins { id 'java' }
repositories {
    mavenCentral()
    maven { url = uri('https://repo.papermc.io/repository/maven-public/') }
}
dependencies {
    compileOnly files('libs/NewGodWar-0.3.3.jar')
    compileOnly 'com.destroystokyo.paper:paper-api:1.12.2-R0.1-SNAPSHOT'
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }
```

독립 프로젝트에서는 예제 `plugin.yml`의 `${version}`을 실제 애드온 버전으로 바꾸세요. `name`, `main`도 자신의 고유 이름과 패키지로 변경합니다. `depend: [NewGodWar]`는 필수입니다. Bukkit API와 NewGodWar 클래스를 애드온 JAR에 복사하거나 shade하지 마세요.

## 능력 확장

`GodAbility`를 구현하거나 공개 `BaseAbility`를 상속하고 `@AbilityInfo`를 붙입니다. 인자 없는 생성자가 필요하며 능력 ID와 표시 이름은 중복될 수 없습니다. ID는 `myaddon_windrunner`처럼 애드온 접두사가 있는 소문자를 권장합니다.

```java
NewGodWarApi api = getServer().getServicesManager().load(NewGodWarApi.class);
if (api == null) throw new IllegalStateException("NewGodWar API is unavailable");
api.registerAbility(this, WindRunnerAbility.class);
```

팩터리가 필요한 경우 `registerAbility(this, AbilityDefinition)`을 사용합니다. 팩터리는 호출마다 독립된 능력 인스턴스를 반환해야 합니다. `supports(player)` 검사 중에도 생성되므로 생성자에서 이벤트나 반복 작업을 등록하지 마세요.

등록된 능력은 기존 GUI·명령어·랜덤 추첨에 반영됩니다. 설정은 `abilities.<id>.enabled`, 제외 목록은 `blacklist.abilities`를 사용합니다. `BaseAbility`의 `useNormal`/`useAdvanced`를 사용하면 기존 우르프 쿨타임 배율도 적용됩니다.

`onAssign`, `onPrepare`, `onRemove`, 전투·이동·상호작용 등의 콜백은 [능력 구현](https://github.com/minjae9010/NewGodWar/wiki/ability-development)을 참고하세요. 능력 내부 작업은 `BaseAbility.scheduleLater`/`scheduleRepeating`을 사용하거나 `cancelScheduledTasks()`에서 직접 취소해야 합니다. `onRemove`는 온라인 플레이어에게만 호출되므로 작업 정리를 이 메서드에만 의존하지 마세요.

애드온 비활성화 시 소유한 등록과 온라인·오프라인 배정을 해제하고 작업 취소를 호출합니다. 해제된 플레이어는 능력이 없는 상태가 됩니다. 필요하면 관리자가 다른 능력을 지정하세요. 직접 해제하려면 `api.unregisterAbilities(this)`를 호출합니다.

## 게임 기능 확장

애드온 자체 Bukkit 리스너·명령어·스케줄러를 사용할 수 있습니다. 리스너와 일반 게임 기능 작업의 소유 플러그인은 애드온 자신으로 지정하세요.

`GameStateChangeEvent`를 구독하면 `getPreviousState()`, `getNewState()`, `getGame()`을 사용할 수 있습니다.

| 상태 | 시점 |
| --- | --- |
| READY | 참가자 능력 배정과 준비 작업 설정 후 |
| RUNNING | 정식 시작 또는 `/gw test`의 동기 초기화 후 |
| ENDED | 종료 시 능력·참가자 정리 후. 이미 종료 상태면 재발행하지 않음 |

이벤트는 취소할 수 없는 메인 스레드 알림입니다. 월드 복구 같은 비동기 후속 작업의 완료까지 뜻하지는 않습니다. 종료 시 참가자 목록은 이미 비워져 있으므로 종료 보상 대상은 시작 이벤트 등에서 애드온이 보관하세요. 이벤트 핸들러에서 새 시작·종료를 실행하려면 다음 틱으로 예약해 중첩 호출을 피하세요.

`api.game()`은 현재 `GameManager`를 제공합니다. `participants()`는 온라인 참가자의 새 목록이며 `teamOf`, `state`, `isRunning` 등으로 게임 상황을 확인할 수 있습니다. 기존 `GameManager`의 상세 API는 본체 버전에 맞춰 컴파일하세요.

API 등록·해제와 게임 변경은 서버 메인 스레드에서 실행해야 합니다. 기본 게임의 채팅 능력은 메인 스레드에서 전달되는 `onChatMessage`를 사용합니다. `onChat`은 현재 기본 게임 경로에서 호출하지 않습니다. 애드온이 직접 비동기 Bukkit 리스너를 등록했다면 게임 변경을 메인 스레드로 예약하세요.

## 자기장 같은 기능 추가

`game.mode: default`를 유지하고 `GameStateChangeEvent`의 `RUNNING`에서 월드 보더를 줄이는 작업을 시작할 수 있습니다. 시작 시 대상 월드의 보더 중심·크기·피해 설정을 저장하고 `ENDED` 또는 애드온 `onDisable()`에서 복구하세요. 게임 종료 시 직접 등록한 반복 작업도 취소해야 합니다. 보더 크기와 축소 속도는 애드온의 자체 설정 파일로 제공하면 서버 운영자가 바꿀 수 있습니다.

## 게임 방식 전체 교체

`GameMode`를 구현해 애드온의 `onEnable()`에서 등록합니다.

```java
api.registerGameMode(this, "my_survival", new MySurvivalMode());
```

본체 `plugins/NewGodWar/config.yml`에서 선택합니다.

```yaml
game:
  mode: my_survival
```

설정 변경 후 `/gw reload`를 실행하면 다음 `/gw start`부터 적용됩니다. 진행 중인 모드는 종료까지 유지합니다. `default`는 기본 코어전이며 등록되지 않은 ID는 시작 오류로 표시됩니다.

- `onStart(GameManager)`: 참가자 선택, 팀 구성, 능력 배정, 시작 위치, 제한 시간 등을 직접 구현합니다. 기본 코어·팀 검증과 자동 배정은 실행하지 않습니다.
- `onStop(GameManager)`: 자신의 반복 작업, 보더·인벤토리·게임 모드 등 변경 상태를 복구합니다. 본체는 배정 능력과 게임 참가 정보만 정리합니다. 서버 종료 또는 모드 애드온 비활성화 때도 호출됩니다.
- 교체 모드 진행 중 본체 `GameListener`의 전투·코어·접속·리스폰·능력 콜백 처리는 실행하지 않습니다. 필요한 Bukkit 이벤트와 능력 콜백 전달은 모드에서 구현합니다. `canDamage`는 기존 능력 유틸리티가 문의하는 전투 허용 정책이며, Bukkit 피해 이벤트 자체도 모드 리스너에서 처리해야 합니다.
- 승리 조건도 모드에서 결정하고 `game.stop(true)`로 종료합니다. 기본 코어전 자동 승리 판정은 적용하지 않습니다.
- 모드는 매 게임마다 작업을 생성하고 종료할 때 취소하세요. 애드온을 켜 둔 채 여러 게임을 진행하므로 Bukkit의 플러그인 비활성화 정리만으로는 충분하지 않습니다.

따라서 자기장, 점수전, 시간 제한전, 개인 생존전 등의 Java 애드온을 만들 수 있습니다. 설정만으로 새로운 규칙이 자동 생성되는 것은 아니며, 전체 교체 모드의 참가·전투·승리 규칙은 제작자가 구현해야 합니다. 기존 명령어·능력 GUI 등 별도 기능은 유지되므로 새 모드에 맞게 관리자 사용 범위도 정하세요.
