package com.github.touhoumaidaffection.client.screen.component;

public record BondSplitPanelPage(
        BondModalPage modal,
        int leftPanelLeft,
        int leftPanelTop,
        int leftPanelWidth,
        int leftPanelHeight,
        int rightPanelLeft,
        int rightPanelTop,
        int rightPanelWidth,
        int rightPanelHeight
) {
    public static BondSplitPanelPage create(BondModalPage modal, int topOffset, int height, int gap, int leftWidth) {
        int contentLeft = modal.contentLeft();
        int contentRight = modal.contentRight();
        int totalWidth = contentRight - contentLeft;
        int rightWidth = totalWidth - leftWidth - gap;
        return new BondSplitPanelPage(
                modal,
                contentLeft,
                modal.top() + topOffset,
                leftWidth,
                height,
                contentLeft + leftWidth + gap,
                modal.top() + topOffset,
                rightWidth,
                height
        );
    }
}
