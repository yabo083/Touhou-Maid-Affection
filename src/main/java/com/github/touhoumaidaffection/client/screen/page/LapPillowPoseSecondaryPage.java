package com.github.touhoumaidaffection.client.screen.page;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.lap.LapPillowMode;
import com.github.touhoumaidaffection.bond.lap.LapPillowPoseSnapshot;
import com.github.touhoumaidaffection.client.BondClientStateCache;
import com.github.touhoumaidaffection.client.YsmModelActionIndex;
import com.github.touhoumaidaffection.client.screen.component.BondButtonRow;
import com.github.touhoumaidaffection.client.screen.component.BondDropdown;
import com.github.touhoumaidaffection.client.screen.component.BondGuiTokens;
import com.github.touhoumaidaffection.client.screen.component.BondModalPage;
import com.github.touhoumaidaffection.network.LapPillowPoseConfigPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class LapPillowPoseSecondaryPage implements BondSecondaryPage {
    private static final int MODAL_WIDTH = BondGuiTokens.SECONDARY_MODAL_WIDTH;
    private static final int MODAL_HEIGHT = BondGuiTokens.SECONDARY_MODAL_HEIGHT;
    private static final int GRID_SIZE = 64;
    private static final int LEFT_PANEL_WIDTH = 72;
    private static final int LEFT_PANEL_HEIGHT = 82;
    private static final int BUTTON_HEIGHT = BondGuiTokens.CONTROL_HEIGHT;
    private static final int BUTTON_GAP = BondGuiTokens.SPACING_MD;
    private static final int DROPDOWN_WIDTH = 72;
    private static final int DROPDOWN_HEIGHT = BondGuiTokens.CONTROL_HEIGHT;
    private static final int DROPDOWN_ROW_HEIGHT = BondGuiTokens.DROPDOWN_ROW_HEIGHT;
    private static final int DROPDOWN_VISIBLE_ROWS = 4;
    private static final int COORD_CARD_WIDTH = 72;
    private static final int COORD_CARD_HEIGHT = 16;
    private static final double STEP_Y = 0.05D;
    private static final int POINT_HITBOX = 7;

    private static final List<ActionOption> PLAYER_ACTION_OPTIONS = List.of(
            new ActionOption("builtin:sit", Component.translatable("bond.lap_pillow.action.sit")),
            new ActionOption("builtin:lie", Component.translatable("bond.lap_pillow.action.lie"))
    );

    private final BondSecondaryPageHost host;
    private final UUID maidUuid;
    private final BondDropdown<ActionOption> maidActionDropdown;
    private final BondDropdown<ActionOption> playerActionDropdown;
    private final List<ActionOption> maidActionOptions;
    private LapPillowPoseSnapshot workingPose;
    private EditablePoint activePoint = EditablePoint.PLAYER;
    private boolean draggingGrid;

    public LapPillowPoseSecondaryPage(BondSecondaryPageHost host) {
        this.host = host;
        this.maidUuid = host.getMaid() == null ? new UUID(0L, 0L) : host.getMaid().getUUID();
        this.workingPose = sanitizePose(BondClientStateCache.getLapPillowPose(maidUuid).clamp());
        BondModalPage modal = modal();
        int rightLeft = rightPanelLeft(modal);
        int rightTop = rightPanelTop(modal);
        this.maidActionDropdown = new BondDropdown<>(rightLeft, rightTop + 15, DROPDOWN_WIDTH, DROPDOWN_HEIGHT, DROPDOWN_ROW_HEIGHT, DROPDOWN_VISIBLE_ROWS);
        this.playerActionDropdown = new BondDropdown<>(rightLeft, rightTop + 43, DROPDOWN_WIDTH, DROPDOWN_HEIGHT, DROPDOWN_ROW_HEIGHT, DROPDOWN_VISIBLE_ROWS);
        this.maidActionOptions = buildMaidActionOptions();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = host.getFont();
        BondModalPage modal = modal();
        modal.renderChrome(graphics, font);

        boolean dropdownExpanded = maidActionDropdown.isExpanded() || playerActionDropdown.isExpanded();
        int contentMouseX = dropdownExpanded ? Integer.MIN_VALUE : mouseX;
        int contentMouseY = dropdownExpanded ? Integer.MIN_VALUE : mouseY;

        drawGridPanel(graphics, font, modal);
        drawRightPanel(graphics, font, modal, contentMouseX, contentMouseY);
        BondButtonRow.render(graphics, font, modal.left(), buttons(modal), contentMouseX, contentMouseY);

        maidActionDropdown.renderOverlay(graphics, font, maidActionOptions, selectedActionIndex(maidActionOptions, workingPose.maidActionId()), mouseX, mouseY, this::renderMaidActionOption);
        playerActionDropdown.renderOverlay(graphics, font, PLAYER_ACTION_OPTIONS, selectedActionIndex(PLAYER_ACTION_OPTIONS, workingPose.playerActionId()), mouseX, mouseY, this::renderPlayerActionOption);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return true;
        }
        BondModalPage modal = modal();
        if (!modal.contains(mouseX, mouseY)) {
            host.closeSecondaryPage();
            return true;
        }

        BondDropdown.ClickResult maidActionClick = maidActionDropdown.mouseClicked(mouseX, mouseY, maidActionOptions.size());
        if (maidActionClick.handled()) {
            playerActionDropdown.collapse();
            if (maidActionClick.selectedIndex() >= 0 && maidActionClick.selectedIndex() < maidActionOptions.size()) {
                workingPose = withSelectedAction(maidActionOptions.get(maidActionClick.selectedIndex()).actionId(), true);
            }
            return true;
        }

        BondDropdown.ClickResult playerActionClick = playerActionDropdown.mouseClicked(mouseX, mouseY, PLAYER_ACTION_OPTIONS.size());
        if (playerActionClick.handled()) {
            maidActionDropdown.collapse();
            if (playerActionClick.selectedIndex() >= 0 && playerActionClick.selectedIndex() < PLAYER_ACTION_OPTIONS.size()) {
                workingPose = withSelectedAction(PLAYER_ACTION_OPTIONS.get(playerActionClick.selectedIndex()).actionId(), false);
            }
            return true;
        }

        String buttonId = BondButtonRow.click(buttons(modal), modal.left(), mouseX, mouseY);
        if (!buttonId.isEmpty()) {
            switch (buttonId) {
                case "save" -> saveAndClose();
                case "reset" -> {
                    workingPose = sanitizePose(LapPillowPoseSnapshot.maidSitPlayerLieDefault());
                    activePoint = EditablePoint.PLAYER;
                }
                case "cancel" -> host.closeSecondaryPage();
                default -> {
                }
            }
            return true;
        }

        PointChoice choice = detectPointChoice(mouseX, mouseY, modal);
        if (choice != PointChoice.NONE) {
            activePoint = choice == PointChoice.MAID ? EditablePoint.MAID : EditablePoint.PLAYER;
            draggingGrid = true;
            updateActivePoint(mouseX, mouseY, modal);
            return true;
        }

        if (containsGrid(mouseX, mouseY, modal)) {
            draggingGrid = true;
            updateActivePoint(mouseX, mouseY, modal);
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingGrid = false;
        return button == 0;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!draggingGrid || button != 0) {
            return false;
        }
        updateActivePoint(mouseX, mouseY, modal());
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (maidActionDropdown.mouseScrolled(mouseX, mouseY, scrollY, maidActionOptions.size())
                || playerActionDropdown.mouseScrolled(mouseX, mouseY, scrollY, PLAYER_ACTION_OPTIONS.size())) {
            return true;
        }
        BondModalPage modal = modal();
        if (containsGrid(mouseX, mouseY, modal)) {
            if (activePoint == EditablePoint.PLAYER) {
                workingPose = sanitizePose(workingPose.withPlayerOffset(
                        workingPose.playerOffsetX(),
                        clampOffset(workingPose.playerOffsetY() + scrollY * STEP_Y),
                        workingPose.playerOffsetZ()
                ));
            } else {
                workingPose = sanitizePose(workingPose.withMaidOffset(
                        workingPose.maidOffsetX(),
                        clampOffset(workingPose.maidOffsetY() + scrollY * STEP_Y),
                        workingPose.maidOffsetZ()
                ));
            }
            return true;
        }
        return maidActionDropdown.contains(mouseX, mouseY, maidActionOptions.size())
                || playerActionDropdown.contains(mouseX, mouseY, PLAYER_ACTION_OPTIONS.size());
    }

    @Override
    public boolean onEscapePressed() {
        boolean collapsed = false;
        if (maidActionDropdown.isExpanded()) {
            maidActionDropdown.collapse();
            collapsed = true;
        }
        if (playerActionDropdown.isExpanded()) {
            playerActionDropdown.collapse();
            collapsed = true;
        }
        return collapsed;
    }

    @Override
    public List<Component> getTooltip(int mouseX, int mouseY) {
        if (maidActionDropdown.isExpanded() || playerActionDropdown.isExpanded()) {
            return List.of();
        }
        BondModalPage modal = modal();
        if (containsGrid(mouseX, mouseY, modal)) {
            return List.of(
                    Component.translatable("bond.lap_pillow.grid.tip.drag"),
                    Component.translatable("bond.lap_pillow.grid.tip.scroll")
            );
        }
        return List.of();
    }

    private void drawGridPanel(GuiGraphics graphics, Font font, BondModalPage modal) {
        int panelLeft = leftPanelLeft(modal);
        int panelTop = leftPanelTop(modal);
        int panelRight = panelLeft + LEFT_PANEL_WIDTH;
        int panelBottom = panelTop + LEFT_PANEL_HEIGHT;
        BondGuiTokens.drawFramedPanel(graphics, panelLeft, panelTop, panelRight, panelBottom, BondGuiTokens.COLOR_BG_ELEMENT);

        graphics.drawString(font, Component.translatable("bond.lap_pillow.relative_position"), panelLeft + 4, panelTop + 4, BondGuiTokens.COLOR_TEXT_BODY, false);

        int gridLeft = panelLeft + 4;
        int gridTop = panelTop + 14;
        int gridRight = gridLeft + GRID_SIZE;
        int gridBottom = gridTop + GRID_SIZE;
        BondGuiTokens.drawFramedPanel(graphics, gridLeft, gridTop, gridRight, gridBottom, BondGuiTokens.STATE_DEFAULT_BG);
        for (int step = 1; step < 4; step++) {
            int offset = step * (GRID_SIZE / 4);
            graphics.hLine(gridLeft, gridRight - 1, gridTop + offset, 0x334A4A4A);
            graphics.vLine(gridLeft + offset, gridTop, gridBottom - 1, 0x334A4A4A);
        }
        int centerX = gridLeft + GRID_SIZE / 2;
        int centerY = gridTop + GRID_SIZE / 2;
        graphics.hLine(gridLeft, gridRight - 1, centerY, 0x66FFFFFF);
        graphics.vLine(centerX, gridTop, gridBottom - 1, 0x66FFFFFF);
        drawPoint(graphics, centerX, centerY, workingPose.maidOffsetX(), workingPose.maidOffsetZ(), BondGuiTokens.COLOR_ACCENT, activePoint == EditablePoint.MAID);
        drawPoint(graphics, centerX, centerY, workingPose.playerOffsetX(), workingPose.playerOffsetZ(), BondGuiTokens.COLOR_TEXT_BODY, activePoint == EditablePoint.PLAYER);
    }

    private void drawRightPanel(GuiGraphics graphics, Font font, BondModalPage modal, int mouseX, int mouseY) {
        int left = rightPanelLeft(modal);
        int top = rightPanelTop(modal);

        graphics.drawString(font, Component.translatable("bond.lap_pillow.maid_action"), left, top + 6, BondGuiTokens.COLOR_TEXT_BODY, false);
        maidActionDropdown.renderBase(graphics, font, maidActionOptions, selectedActionIndex(maidActionOptions, workingPose.maidActionId()), mouseX, mouseY, this::renderMaidActionOption);

        graphics.drawString(font, Component.translatable("bond.lap_pillow.player_action"), left, top + 34, BondGuiTokens.COLOR_TEXT_BODY, false);
        playerActionDropdown.renderBase(graphics, font, PLAYER_ACTION_OPTIONS, selectedActionIndex(PLAYER_ACTION_OPTIONS, workingPose.playerActionId()), mouseX, mouseY, this::renderPlayerActionOption);

        drawCoordCard(graphics, font, left, top + 58, COORD_CARD_WIDTH, Component.translatable("bond.lap_pillow.subject.player"), workingPose.playerOffsetX(), workingPose.playerOffsetY(), workingPose.playerOffsetZ(), activePoint == EditablePoint.PLAYER);
        drawCoordCard(graphics, font, left, top + 78, COORD_CARD_WIDTH, Component.translatable("bond.lap_pillow.subject.maid"), workingPose.maidOffsetX(), workingPose.maidOffsetY(), workingPose.maidOffsetZ(), activePoint == EditablePoint.MAID);
    }

    private void renderMaidActionOption(GuiGraphics graphics, Font font, ActionOption option, int index, int left, int top, int right, int height, boolean hovered, boolean selectedHeader) {
        renderActionOption(graphics, font, option, index, left, top, right, height, hovered, selectedHeader, option.actionId().equals(workingPose.maidActionId()));
    }

    private void renderPlayerActionOption(GuiGraphics graphics, Font font, ActionOption option, int index, int left, int top, int right, int height, boolean hovered, boolean selectedHeader) {
        renderActionOption(graphics, font, option, index, left, top, right, height, hovered, selectedHeader, option.actionId().equals(workingPose.playerActionId()));
    }

    private void renderActionOption(GuiGraphics graphics, Font font, ActionOption option, int index, int left, int top, int right, int height, boolean hovered, boolean selectedHeader, boolean selected) {
        if (!selectedHeader) {
            BondGuiTokens.drawSelectableRow(graphics, left - 4, top - 2, right, top + height + 2, selected, hovered);
        }
        graphics.drawString(font, option.label(), left, top + 1, selected ? BondGuiTokens.COLOR_TEXT_SELECTED : BondGuiTokens.COLOR_TEXT_BODY, false);
    }

    private void drawCoordCard(GuiGraphics graphics, Font font, int left, int top, int width, Component title, double x, double y, double z, boolean active) {
        int innerBorder = active ? BondGuiTokens.STATE_HOVER_BORDER : BondGuiTokens.STATE_DEFAULT_BORDER;
        int bodyColor = active ? BondGuiTokens.STATE_SELECTED_BG : BondGuiTokens.COLOR_BG_ELEMENT;
        BondGuiTokens.drawFramedPanelWithInnerBorder(graphics, left, top, left + width, top + COORD_CARD_HEIGHT, bodyColor, innerBorder);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(left + 3.0F, top + 3.0F, 0.0F);
            graphics.pose().scale(0.70F, 0.70F, 1.0F);
            graphics.drawString(font, Component.translatable("bond.lap_pillow.coords.compact", title.getString(), formatValue(x), formatValue(y), formatValue(z)), 0, 0, BondGuiTokens.COLOR_TEXT_BODY, false);
        } finally {
            graphics.pose().popPose();
        }
    }

    private void drawPoint(GuiGraphics graphics, int centerX, int centerY, double offsetX, double offsetZ, int color, boolean active) {
        double halfRange = GRID_SIZE / 2.0D - 6.0D;
        int pointX = centerX + (int) Math.round((offsetX / LapPillowPoseSnapshot.MAX_OFFSET) * halfRange);
        int pointY = centerY - (int) Math.round((offsetZ / LapPillowPoseSnapshot.MAX_OFFSET) * halfRange);
        int size = active ? 4 : 3;
        graphics.fill(pointX - size, pointY - size, pointX + size + 1, pointY + size + 1, color);
    }

    private PointChoice detectPointChoice(double mouseX, double mouseY, BondModalPage modal) {
        if (!containsGrid(mouseX, mouseY, modal)) {
            return PointChoice.NONE;
        }
        int gridLeft = gridLeft(modal);
        int gridTop = gridTop(modal);
        int centerX = gridLeft + GRID_SIZE / 2;
        int centerY = gridTop + GRID_SIZE / 2;
        if (isNearPoint(mouseX, mouseY, centerX, centerY, workingPose.playerOffsetX(), workingPose.playerOffsetZ())) {
            return PointChoice.PLAYER;
        }
        if (isNearPoint(mouseX, mouseY, centerX, centerY, workingPose.maidOffsetX(), workingPose.maidOffsetZ())) {
            return PointChoice.MAID;
        }
        return PointChoice.NONE;
    }

    private boolean isNearPoint(double mouseX, double mouseY, int centerX, int centerY, double offsetX, double offsetZ) {
        double halfRange = GRID_SIZE / 2.0D - 6.0D;
        int pointX = centerX + (int) Math.round((offsetX / LapPillowPoseSnapshot.MAX_OFFSET) * halfRange);
        int pointY = centerY - (int) Math.round((offsetZ / LapPillowPoseSnapshot.MAX_OFFSET) * halfRange);
        return Math.abs(mouseX - pointX) <= POINT_HITBOX && Math.abs(mouseY - pointY) <= POINT_HITBOX;
    }

    private void saveAndClose() {
        LapPillowPoseSnapshot pose = sanitizePose(workingPose.clamp());
        BondClientStateCache.updateLapPillowPose(maidUuid, pose);
        TouhouMaidAffection.CHANNEL.sendToServer(new LapPillowPoseConfigPayload(
                maidUuid,
                pose.mode().serializedName(),
                pose.maidOffsetX(),
                pose.maidOffsetY(),
                pose.maidOffsetZ(),
                pose.playerOffsetX(),
                pose.playerOffsetY(),
                pose.playerOffsetZ(),
                pose.maidActionId(),
                pose.playerActionId()
        ));
        host.closeSecondaryPage();
    }

    private void updateActivePoint(double mouseX, double mouseY, BondModalPage modal) {
        int gridLeft = gridLeft(modal);
        int gridTop = gridTop(modal);
        int centerX = gridLeft + GRID_SIZE / 2;
        int centerY = gridTop + GRID_SIZE / 2;
        double halfRange = GRID_SIZE / 2.0D - 6.0D;
        double offsetX = clampOffset(((mouseX - centerX) / halfRange) * LapPillowPoseSnapshot.MAX_OFFSET);
        double offsetZ = clampOffset(((centerY - mouseY) / halfRange) * LapPillowPoseSnapshot.MAX_OFFSET);
        if (activePoint == EditablePoint.PLAYER) {
            workingPose = sanitizePose(workingPose.withPlayerOffset(offsetX, workingPose.playerOffsetY(), offsetZ));
        } else {
            workingPose = sanitizePose(workingPose.withMaidOffset(offsetX, workingPose.maidOffsetY(), offsetZ));
        }
    }

    private boolean containsGrid(double mouseX, double mouseY, BondModalPage modal) {
        int gridLeft = gridLeft(modal);
        int gridTop = gridTop(modal);
        return mouseX >= gridLeft && mouseX < gridLeft + GRID_SIZE
                && mouseY >= gridTop && mouseY < gridTop + GRID_SIZE;
    }

    private int selectedActionIndex(List<ActionOption> options, String actionId) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).actionId().equals(actionId)) {
                return i;
            }
        }
        return 0;
    }

    private List<ActionOption> buildMaidActionOptions() {
        List<ActionOption> options = new ArrayList<>();
        options.add(new ActionOption("builtin:sit", Component.translatable("bond.lap_pillow.action.sit")));
        options.add(new ActionOption("builtin:lie", Component.translatable("bond.lap_pillow.action.lie")));
        if (host.getMaid() != null && host.getMaid().isYsmModel() && host.getMaid().getYsmModelId() != null) {
            for (YsmModelActionIndex.DetectedYsmAction action : YsmModelActionIndex.getActions(host.getMaid().getYsmModelId(), host.getMaid().getYsmModelTexture())) {
                options.add(new ActionOption(action.actionId(), Component.literal(action.displayName())));
            }
        }
        return List.copyOf(options);
    }

    private LapPillowPoseSnapshot withSelectedAction(String actionId, boolean maidSide) {
        LapPillowPoseSnapshot next = maidSide
                ? new LapPillowPoseSnapshot(
                workingPose.mode(),
                workingPose.maidOffsetX(),
                workingPose.maidOffsetY(),
                workingPose.maidOffsetZ(),
                workingPose.playerOffsetX(),
                workingPose.playerOffsetY(),
                workingPose.playerOffsetZ(),
                actionId,
                workingPose.playerActionId()
        )
                : new LapPillowPoseSnapshot(
                workingPose.mode(),
                workingPose.maidOffsetX(),
                workingPose.maidOffsetY(),
                workingPose.maidOffsetZ(),
                workingPose.playerOffsetX(),
                workingPose.playerOffsetY(),
                workingPose.playerOffsetZ(),
                workingPose.maidActionId(),
                actionId
        );
        return sanitizePose(next);
    }

    private LapPillowPoseSnapshot sanitizePose(LapPillowPoseSnapshot pose) {
        String maidAction = pose.maidActionId().isBlank() ? "builtin:sit" : pose.maidActionId();
        String playerAction = pose.playerActionId().isBlank() ? "builtin:lie" : pose.playerActionId();

        boolean maidLying = false;
        boolean playerLying = true;
        if ("builtin:sit".equals(maidAction)) {
            maidLying = false;
        } else if ("builtin:lie".equals(maidAction)) {
            maidLying = true;
        }
        if ("builtin:sit".equals(playerAction)) {
            playerLying = false;
        } else if ("builtin:lie".equals(playerAction)) {
            playerLying = true;
        }

        return new LapPillowPoseSnapshot(
                deriveMode(maidLying, playerLying),
                pose.maidOffsetX(),
                pose.maidOffsetY(),
                pose.maidOffsetZ(),
                pose.playerOffsetX(),
                pose.playerOffsetY(),
                pose.playerOffsetZ(),
                maidAction,
                playerAction
        ).clamp();
    }

    private LapPillowMode deriveMode(boolean maidLying, boolean playerLying) {
        if (maidLying) {
            return playerLying ? LapPillowMode.MAID_LIE_PLAYER_LIE : LapPillowMode.MAID_LIE_PLAYER_SIT;
        }
        return playerLying ? LapPillowMode.MAID_SIT_PLAYER_LIE : LapPillowMode.MAID_SIT_PLAYER_SIT;
    }

    private List<BondButtonRow.ButtonSpec> buttons(BondModalPage modal) {
        return BondButtonRow.createCenteredUniform(host.getFont(), modal.width(), modal.footerButtonY(BUTTON_HEIGHT), BUTTON_HEIGHT, BUTTON_GAP, BondGuiTokens.BUTTON_HORIZONTAL_PADDING,
                new BondButtonRow.ButtonSpec(0, 0, BondGuiTokens.BUTTON_MIN_WIDTH, BUTTON_HEIGHT, Component.translatable("bond.lap_pillow.save"), "save", true, true),
                new BondButtonRow.ButtonSpec(0, 0, BondGuiTokens.BUTTON_MIN_WIDTH, BUTTON_HEIGHT, Component.translatable("bond.lap_pillow.reset"), "reset", true),
                new BondButtonRow.ButtonSpec(0, 0, BondGuiTokens.BUTTON_MIN_WIDTH, BUTTON_HEIGHT, Component.translatable("gui.cancel"), "cancel", true)
        );
    }

    private BondModalPage modal() {
        return host.createModal(MODAL_WIDTH, MODAL_HEIGHT, Component.translatable("bond.lap_pillow.config.title"));
    }

    private int leftPanelLeft(BondModalPage modal) {
        return modal.contentLeft();
    }

    private int leftPanelTop(BondModalPage modal) {
        return modal.contentTop();
    }

    private int rightPanelLeft(BondModalPage modal) {
        return leftPanelLeft(modal) + LEFT_PANEL_WIDTH + BondGuiTokens.SPACING_MD;
    }

    private int rightPanelTop(BondModalPage modal) {
        return modal.contentTop();
    }

    private int gridLeft(BondModalPage modal) {
        return leftPanelLeft(modal) + 4;
    }

    private int gridTop(BondModalPage modal) {
        return leftPanelTop(modal) + 14;
    }

    private static String formatValue(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static double clampOffset(double value) {
        return Math.max(LapPillowPoseSnapshot.MIN_OFFSET, Math.min(LapPillowPoseSnapshot.MAX_OFFSET, value));
    }

    private enum EditablePoint {
        PLAYER,
        MAID
    }

    private enum PointChoice {
        NONE,
        PLAYER,
        MAID
    }

    private record ActionOption(String actionId, Component label) {
    }
}
