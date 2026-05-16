# 능력 구현

능력은 `@AbilityInfo`와 `GodAbility`로 구현합니다.

## 기본 구조

```java
@AbilityInfo(
    id = "sample",
    name = "샘플",
    description = "공격 피해량을 2배로 올립니다.",
    normalSkill = "없음",
    normalStoneCost = 0,
    advancedSkill = "없음",
    advancedStoneCost = 0,
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

## 어노테이션

`@AbilityInfo`는 능력의 기본 정보를 정의합니다.

```java
@AbilityInfo(
    id = "sample",
    name = "샘플",
    description = "능력 설명",
    normalSkill = "일반 능력 설명",
    normalStoneCost = 10,
    advancedSkill = "고급 능력 설명",
    advancedStoneCost = 20,
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
- `advancedSkill`: 고급 능력 설명. 보통 우클릭 능력을 적습니다.
- `advancedStoneCost`: 고급 능력 돌 소모량
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
}
```

## 설정 사용

능력 설정은 `abilities.<id>.<key>` 형태로 읽습니다.

```java
double bonus = context.plugin()
    .getConfig()
    .getDouble(context.configPath("damage-bonus"), 1.25D);
```

위 코드는 `abilities.sample.damage-bonus` 값을 읽습니다.
