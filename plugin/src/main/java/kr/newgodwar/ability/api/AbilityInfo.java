package kr.newgodwar.ability.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface AbilityInfo {

    String id();

    String name();

    String description();

    String normalSkill() default "";

    int normalStoneCost() default 0;

    int normalCooldownSeconds() default 0;

    String advancedSkill() default "";

    int advancedStoneCost() default 0;

    int advancedCooldownSeconds() default 0;

    String passiveSkill() default "";

    AbilityGrade grade() default AbilityGrade.UNRATED;

    String author() default "NewGodWar";

    boolean enabledByDefault() default true;
}
