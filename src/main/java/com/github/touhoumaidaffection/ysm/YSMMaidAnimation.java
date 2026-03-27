package com.github.touhoumaidaffection.ysm;

public enum YSMMaidAnimation {
    LAP_PILLOW("lap_pillow"),
    MORNING_KISS("morning_kiss"),
    RANDOM_GIFT("random_gift"),
    SPECIAL_EXPRESSION("special_expression");

    private final String animationName;

    YSMMaidAnimation(String animationName) {
        this.animationName = animationName;
    }

    public String animationName() {
        return animationName;
    }
}