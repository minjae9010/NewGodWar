# 능력 구현

능력은 `@AbilityInfo`와 `GodAbility`로 구현합니다. 내장 능력은 `kr.newgodwar.ability.builtin` 패키지에서 자동 스캔되고, 외부 addon jar는 `AbilityRegistrar` service 파일로 등록합니다.

## 기본 구조

```java
@AbilityInfo(
    id = "sample",
    name = "샘플",
    description = "공격 피해량을 2배로 올립니다.",
    normalSkill = "바라보는 적에게 피해를 줍니다.",
    normalStoneCost = 10,
    normalCooldownSeconds = 30,
    advancedSkill = "주변 적에게 피해를 줍니다.",
    advancedStoneCost = 25,
    advancedCooldownSeconds = 90,
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

블레이즈 막대 좌클릭/우클릭 능력을 만들 때는 내장 능력의 `BaseAbility`처럼 `onInteract`를 처리하면 됩니다. 외부 addon에서는 `GodAbility` 이벤트 메서드만 공개 API로 간주하고, 내장 전용 base class에 의존하지 않는 편이 안전합니다.

## 자동 등록

능력은 addon jar의 `AbilityRegistrar`를 통해 자동 등록합니다.

```java
public final class SampleAbilityRegistrar implements AbilityRegistrar {

    @Override
    public void registerAbilities(AbilityRegistry registry) {
        registry.register(SampleAbility.class);
    }
}
```

addon jar에 아래 service 파일을 추가하면 서버 시작 시 자동으로 로드됩니다.

```text
META-INF/services/kr.newgodwar.ability.api.AbilityRegistrar
```

파일 내용은 registrar 클래스의 전체 경로입니다.

```text
com.example.godwar.SampleAbilityRegistrar
```

플러그인은 Java `ServiceLoader` 방식으로 registrar를 찾습니다. addon jar가 서버의 `plugins` 폴더에 함께 들어 있으면 서버 시작 시 등록됩니다.

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
- `author`: 능력 작성자
- `enabledByDefault`: config에 값이 없을 때 기본 활성화 여부

## 이벤트 메서드

`GodAbility`는 필요한 메서드만 override해서 사용합니다.

```java
public interface GodAbility {
    default void onAssign(AbilityPlayerContext context) {}
    default void onRemove(AbilityPlayerContext context) {}
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

필요한 메서드만 override하면 됩니다. 이벤트 객체를 받는 메서드에서는 Bukkit 이벤트 취소 여부와 동기/비동기 실행 맥락을 고려해야 합니다. 채팅 이벤트에서 월드나 인벤토리를 직접 수정해야 한다면 서버 메인 스레드로 넘기는 방식이 안전합니다.

## 타깃형 능력

타깃형 능력은 `/x <player>` 또는 `/godwar target <player>`로 대상 이름을 지정받습니다. `setTarget`을 override해 대상 검증과 저장을 처리하고, 실제 발동 시 온라인 여부, 같은 월드 여부, 팀 관계, 거리 등을 다시 확인하세요.

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
/godwar abilities sample
```

무작위 배정에서 제외하려면 다음 명령을 사용할 수 있습니다.

```text
/godwar blacklist add sample
```

## 구현 체크리스트

- `id`는 소문자 영문, 숫자, `-`, `_` 위주로 정합니다.
- 능력 비용은 기본적으로 조약돌 기준으로 설명합니다.
- 타깃형 능력은 자기 자신, 오프라인 플레이어, 다른 월드 플레이어를 처리합니다.
- 블록을 바꾸는 능력은 다이아 심장을 훼손하지 않도록 주의합니다.
- 타이머나 쿨타임이 있는 능력은 제거 시 상태가 남지 않도록 정리합니다.
- 설정값은 기본값을 함께 제공해 누락된 config에서도 동작하게 합니다.
