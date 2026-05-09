package com.saynow.scenario.domain;

import lombok.Getter;

@Getter
public enum ScenarioDifficulty {
    EASY("쉬움"),
    NORMAL("보통"),
    HARD("어려움");

    private final String displayName;

    ScenarioDifficulty(String displayName) {
        this.displayName = displayName;
    }

}
