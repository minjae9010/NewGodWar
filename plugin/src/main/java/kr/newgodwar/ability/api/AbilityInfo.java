package kr.newgodwar.ability.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AbilityInfo {

    String id();

    String name();

    String description();

    String normalSkill() default "";

    int normalStoneCost() default 0;

    String advancedSkill() default "";

    int advancedStoneCost() default 0;

    String passiveSkill() default "";

    String author() default "NewGodWar";

    boolean enabledByDefault() default true;
}
