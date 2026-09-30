package com.github.touhoumaidaffection.client.screen.component;

/** Header coordinates come from the page frame, never the ability-list viewport. */
public record BondHeaderLayout(Rect title, Rect settings) {
    public static BondHeaderLayout fromPage(int pageX, int pageY, int pageWidth) {
        int gearX = pageX + pageWidth - 6 - 12;
        return new BondHeaderLayout(
                new Rect(pageX + 6, pageY + 4, gearX - 4 - (pageX + 6), 16),
                new Rect(gearX, pageY + 6, 12, 12));
    }

    public record Rect(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
