# 능력 구현

능력은 `@AbilityInfo`와 `GodAbility`로 구현합니다. 외부 애드온은 Bukkit 플러그인으로 만들고 `NewGodWarApi`로 등록합니다. 설치와 빌드 예제는 [애드온 개발](https://github.com/minjae9010/NewGodWar/wiki/addon-development)을 참고하세요.

## 기본 구조

아래는 플레이어에게 주는 공격 피해를 2배로 만드는 패시브 능력 예제입니다. `SampleAbility.java`로 저장하고 애드온에서 등록하세요.

```java
import kr.newgodwar.ability.api.AbilityDamageContext;
import kr.newgodwar.ability.api.AbilityInfo;
import kr.newgodwar.ability.api.GodAbility;

@AbilityInfo(
    id = "sample",
    name = "샘플",
    description = "공격 피해량을 2배로 올립니다.",
    passiveSkill = "공격 피해량을 2배로 올립니다.",
    author = "minjae9010"
)
public final class SampleAbility implements GodAbility {

    @Override
    public void onDamage(AbilityDamageContext context) {
        context.damage(context.damage() * 2.0D);
    }
}
```

`@AbilityInfo`는 도감에 표시할 설명과 기본 수치를 정의합니다. 일반/고급 설명을 적는 것만으로 스킬이 구현되지는 않습니다. 블레이즈 막대 좌클릭/우클릭 능력은 공개된 `kr.newgodwar.ability.builtin.BaseAbility`를 상속하고 `onStaffLeft`/`onStaffRight`를 구현할 수 있습니다. `useNormal`, `useAdvanced`는 비용과 쿨타임을 처리하고, `scheduleLater`, `scheduleRepeating`은 능력 해제 시 취소되는 작업을 등록합니다.

## 외부 애드온 등록

`plugin.yml`에 `depend: [NewGodWar]`를 넣고 애드온의 `onEnable()`에서 등록합니다.

```java
NewGodWarApi api = getServer().getServicesManager().load(NewGodWarApi.class);
if (api == null) {
    throw new IllegalStateException("NewGodWar API is unavailable");
}
api.registerAbility(this, SampleAbility.class);
```

등록한 능력은 기존 능력 도감, 수동 지정, 랜덤 배정, 블랙리스트에서 사용할 수 있습니다. 등록한 애드온이 비활성화되면 능력 등록과 배정도 해제됩니다.

`AbilityRegistrar`와 `META-INF/services`는 NewGodWar 자체 클래스 로더에서 발견할 수 있는 내장 모듈용으로 유지됩니다. 별도 JAR를 `plugins` 폴더에 넣는 것만으로 그 서비스 파일이 발견되지는 않습니다. 외부 애드온에는 위 API를 사용하세요.

## 어노테이션

`@AbilityInfo`는 능력의 기본 정보를 정의합니다.

```java
@AbilityInfo(
    id = "sample",
    name = "샘플",
    description = "능력 설명",
    normalSkill = "일반 능력 설명",
    normalStoneCost = 10,
    normalCooldownSeconds = 30,
    advancedSkill = "고급 능력 설명",
    advancedStoneCost = 20,
    advancedCooldownSeconds = 90,
    passiveSkill = "패시브 능력 설명",
    grade = AbilityGrade.B,
    author = "작성자",
    enabledByDefault = true
)
```

- `id`: 설정과 명령어에서 사용하는 고유 ID
- `name`: 게임 안에 표시되는 이름
- `description`: 능력 설명
- `normalSkill`: 일반 능력 설명. 보통 좌클릭 능력을 적습니다.
- `normalStoneCost`: 일반 능력 돌 소모량
- `normalCooldownSeconds`: 일반 능력 쿨타임. `0`은 쿨타임 없음, 음수는 제한/특수 처리 용도로 사용할 수 있습니다.
- `advancedSkill`: 고급 능력 설명. 보통 우클릭 능력을 적습니다.
- `advancedStoneCost`: 고급 능력 돌 소모량
- `advancedCooldownSeconds`: 고급 능력 쿨타임. `0`은 쿨타임 없음, 음수는 제한/특수 처리 용도로 사용할 수 있습니다.
- `passiveSkill`: 패시브 능력 설명
- `grade`: 밸런스를 기준으로 정한 능력 등급. `S`, `A`, `B`, `C`, `D`, `UNRATED` 중 하나입니다.
- `author`: 능력 작성자
- `enabledByDefault`: config에 값이 없을 때 기본 활성화 여부

## 이벤트 메서드

`GodAbility`는 필요한 메서드만 override해서 사용합니다.

```java
public interface GodAbility {
    default void saveSession(org.bukkit.configuration.ConfigurationSection data) {}
    default void loadSession(org.bukkit.configuration.ConfigurationSection data) {}
    default void onAssign(AbilityPlayerContext context) {}
    default void onPrepare(AbilityPlayerContext context) {}
    default void onRemove(AbilityPlayerContext context) {}
    default void cancelScheduledTasks() {}
    default void onDamage(AbilityDamageContext context) {}
    default void onTick(AbilityPlayerContext context) {}
    default void onKill(AbilityKillContext context) {}
    default void onDeath(AbilityPlayerContext context, PlayerDeathEvent event) {}
    default void onInteract(AbilityPlayerContext context, PlayerInteractEvent event) {}
    default void onGenericDamage(AbilityPlayerContext context, EntityDamageEvent event) {}
    default void onDamageByEntity(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player opponent, boolean attacker) {}
    default void onProjectileHit(AbilityPlayerContext context, EntityDamageByEntityEvent event, Player victim) {}
    default void onProjectileLaunch(AbilityPlayerContext context, ProjectileLaunchEvent event) {}
    default void onBlockBreak(AbilityPlayerContext context, BlockBreakEvent event) {}
    default void onBlockPlace(AbilityPlayerContext context, BlockPlaceEvent event) {}
    default void onBlockExplode(BlockExplodeEvent event) {}
    default void onSignChange(AbilityPlayerContext context, SignChangeEvent event) {}
    default void onFoodLevelChange(AbilityPlayerContext context, FoodLevelChangeEvent event) {}
    default void onItemConsume(AbilityPlayerContext context, PlayerItemConsumeEvent event) {}
    default void onRegainHealth(AbilityPlayerContext context, EntityRegainHealthEvent event) {}
    default void onRespawn(AbilityPlayerContext context, PlayerRespawnEvent event) {}
    default void onMove(AbilityPlayerContext context, PlayerMoveEvent event) {}
    default void onChat(AbilityPlayerContext context, AsyncPlayerChatEvent event) {}
    default void onChatMessage(AbilityPlayerContext context, String message) {}
    default void onFish(AbilityPlayerContext context, PlayerFishEvent event) {}
    default void setTarget(AbilityPlayerContext context, CommandSender sender, String targetName) {}
    default boolean requiresTarget() { return false; }
    default void onCountdownTick(AbilityPlayerContext context) {}
    default long cooldownRemainingMillis(int slot) { return 0L; }
    default void clearCooldowns() {}
    default List<String> activeTimerLines() { return Collections.emptyList(); }
    default boolean supports(Player player) { return true; }
}
```

`onAssign`은 능력 배정과 재접속·봉인 해제 때 지속 효과를 복구하는 용도입니다. 시작 장비처럼 한 번만 지급해야 하는 아이템은 `onPrepare`에서 지급해야 하며, 이 메서드는 게임 시작 준비 또는 진행 중 능력 변경 시에만 호출됩니다.

`onDeath`는 누군가 사망할 때 활성 능력들에 전달됩니다. 능력 소유자 자신의 사망만 처리하려면 `event.getEntity().equals(context.player())`를 먼저 검사하세요. `onItemConsume`은 음식·물약 등 아이템 섭취 이벤트를 받습니다.

채팅 능력은 `onChatMessage`를 구현하세요. 기본 게임 리스너는 채팅 문자열을 복사해 서버 메인 스레드에서 이 콜백을 호출합니다. `onChat`은 인터페이스에 남아 있지만 현재 기본 게임 경로에서는 호출하지 않습니다. `onChatMessage`에서는 원본 채팅 이벤트를 취소하거나 수정할 수 없습니다. 애드온이 직접 비동기 Bukkit 리스너를 등록한다면 월드·인벤토리 변경은 메인 스레드로 예약해야 합니다.

### 작업 정리와 게임 복구

`cancelScheduledTasks()`는 오프라인 플레이어의 능력을 해제할 때도 호출됩니다. `onRemove`만으로 작업을 정리하지 마세요. `BaseAbility`를 상속해 이 메서드를 재정의하면 `super.cancelScheduledTasks()`도 호출해 기본 예약 작업을 취소해야 합니다.

기본 게임과 능력 테스트의 서버 재시작 복구에 필요한 추가 값은 `saveSession`과 `loadSession`으로 저장·복원합니다. `BaseAbility`는 타깃 이름과 남은 쿨타임을 저장하므로, 이 메서드들을 재정의할 때는 각각 `super.saveSession(data)`와 `super.loadSession(data)`도 호출하세요. 직접 `GodAbility`를 구현하면 필요한 쿨타임 저장도 직접 처리해야 합니다.

복구는 새로운 능력 인스턴스에 저장 데이터를 읽으며 `onPrepare`를 다시 호출하지 않습니다. `loadSession`은 플레이어가 오프라인인 상태에서도 실행되므로 플레이어 객체에 의존하지 말고, 접속 후 지속 효과 적용은 `onAssign`에서 처리하세요. 실행 중이던 임시 효과나 예약 작업은 자동으로 재생되지 않습니다.

## 타깃형 능력

타깃형 능력은 `/x <player>` 또는 `/gw target <player>`로 대상 이름을 지정받습니다. `setTarget`을 override해 대상 검증과 저장을 처리하고, 실제 발동 시 온라인 여부, 같은 월드 여부, 팀 관계, 거리 등을 다시 확인하세요.

## 설정 사용

능력 설정은 `abilities.<id>.<key>` 형태로 읽습니다.

```java
double bonus = context.plugin()
    .getConfig()
    .getDouble(context.configPath("damage-bonus"), 1.25D);
```

위 코드는 `abilities.sample.damage-bonus` 값을 읽습니다.

## 쿨타임과 우르프

능력 쿨타임은 `AbilityManager`를 통해 우르프 배율이 적용됩니다. 직접 시간을 계산하는 능력도 `game.urf.enabled`와 `game.urf.cooldown-multiplier`의 영향을 받을지 결정해야 합니다. 내장 능력처럼 쿨타임을 노출하면 `/a` GUI와 스코어보드에서 남은 시간을 표시할 수 있습니다.

## 등록 확인

서버에서 다음 명령으로 등록 여부를 확인합니다.

```text
/gw abilities sample
```

무작위 배정에서 제외하려면 다음 명령을 사용할 수 있습니다.

```text
/gw blacklist add sample
```

## 구현 체크리스트

- `id`는 소문자 영문, 숫자, `-`, `_` 위주로 정합니다.
- 능력 비용은 기본적으로 조약돌 기준으로 설명합니다.
- 타깃형 능력은 자기 자신, 오프라인 플레이어, 다른 월드 플레이어를 처리합니다.
- 블록을 바꾸는 능력은 다이아 심장을 훼손하지 않도록 주의합니다.
- 타이머나 쿨타임이 있는 능력은 제거 시 상태가 남지 않도록 정리합니다.
- 설정값은 기본값을 함께 제공해 누락된 config에서도 동작하게 합니다.
