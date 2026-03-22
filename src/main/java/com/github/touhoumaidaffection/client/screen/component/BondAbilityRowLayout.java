package com.github.touhoumaidaffection.client.screen.component;

public record BondAbilityRowLayout(
        int rowLeft,
        int rowTop,
        int rowRight,
        int rowBottom,
        int textLeft,
        int textRight,
        int mainButtonX,
        int secondaryButtonX,
        int buttonY,
        boolean hasSecondaryButton
) {
    public static BondAbilityRowLayout create(int rowLeft,
                                              int rowTop,
                                              int rowRight,
                                              int rowHeight,
                                              int mainButtonWidth,
                                              int secondaryButtonWidth,
                                              int buttonHeight,
                                              int buttonGap,
                                              boolean hasSecondaryButton) {
        int rowBottom = rowTop + rowHeight - 1;
        int mainButtonX = rowRight - mainButtonWidth - 4;
        int secondaryButtonX = hasSecondaryButton ? mainButtonX - buttonGap - secondaryButtonWidth : mainButtonX;
        int textLeft = rowLeft + 4;
        int textRight = hasSecondaryButton ? secondaryButtonX - 6 : mainButtonX - 6;
        int buttonY = rowTop + Math.max(3, (rowHeight - buttonHeight) / 2);
        return new BondAbilityRowLayout(
                rowLeft,
                rowTop,
                rowRight,
                rowBottom,
                textLeft,
                Math.max(textLeft, textRight),
                mainButtonX,
                secondaryButtonX,
                buttonY,
                hasSecondaryButton
        );
    }

    public boolean containsMainButton(double mouseX, double mouseY, int mainButtonWidth, int buttonHeight) {
        return mouseX >= mainButtonX
                && mouseX < mainButtonX + mainButtonWidth
                && mouseY >= buttonY
                && mouseY < buttonY + buttonHeight;
    }

    public boolean containsSecondaryButton(double mouseX, double mouseY, int secondaryButtonWidth, int buttonHeight) {
        return hasSecondaryButton
                && mouseX >= secondaryButtonX
                && mouseX < secondaryButtonX + secondaryButtonWidth
                && mouseY >= buttonY
                && mouseY < buttonY + buttonHeight;
    }
}
