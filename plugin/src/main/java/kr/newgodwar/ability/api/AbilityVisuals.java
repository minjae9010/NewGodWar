package kr.newgodwar.ability.api;

import kr.newgodwar.ability.feedback.AbilityStyle;

/** Visual policy supplied by the ability itself. No central registration is required. */
public interface AbilityVisuals {
    default AbilityStyle style() { return AbilityStyle.DEFAULT; }
}
