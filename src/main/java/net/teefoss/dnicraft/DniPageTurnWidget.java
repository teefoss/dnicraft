package net.teefoss.dnicraft;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.nio.file.Path;

public class DniPageTurnWidget extends ButtonWidget {
    private static final Identifier PAGE_FORWARD_HIGHLIGHTED_TEXTURE = new Identifier("widget/page_forward_highlighted");
    private static final Identifier PAGE_FORWARD_TEXTURE = new Identifier("widget/page_forward");
    private static final Identifier PAGE_BACKWARD_HIGHLIGHTED_TEXTURE = new Identifier("widget/page_backward_highlighted");
    private static final Identifier PAGE_BACKWARD_TEXTURE = new Identifier("widget/page_backward");
    private static final Identifier PAGE_END_HIGHLIGHTED_TEXTURE = new Identifier("dnicraft", "page_end_highlighted");
    private static final Identifier PAGE_END_TEXTURE = new Identifier("dnicraft", "page_end");
    private static final Identifier PAGE_BEGINNING_HIGHLIGHTED_TEXTURE = new Identifier("dnicraft", "page_beginning_highlighted");
    private static final Identifier PAGE_BEGINNING_TEXTURE = new Identifier("dnicraft", "page_beginning");

    private final boolean isNextPageButton;
    private final boolean isSingle;

    public DniPageTurnWidget(int x, int y, boolean isNextPageButton, boolean isSingle, ButtonWidget.PressAction action) {
        super(x, y, 23, 13, ScreenTexts.EMPTY, action, DEFAULT_NARRATION_SUPPLIER);
        this.isNextPageButton = isNextPageButton;
        this.isSingle = isSingle;
    }

    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        Identifier identifier;
        if (this.isNextPageButton) {
            if ( isSingle ) {
                identifier = this.isSelected() ? PAGE_FORWARD_HIGHLIGHTED_TEXTURE : PAGE_FORWARD_TEXTURE;
            } else {
                identifier = this.isSelected() ? PAGE_END_HIGHLIGHTED_TEXTURE : PAGE_END_TEXTURE;
            }
        } else {
            if ( isSingle ) {
                identifier = this.isSelected() ? PAGE_BACKWARD_HIGHLIGHTED_TEXTURE : PAGE_BACKWARD_TEXTURE;
            } else {
                identifier = this.isSelected() ? PAGE_BEGINNING_HIGHLIGHTED_TEXTURE : PAGE_BEGINNING_TEXTURE;
            }
        }

        context.drawGuiTexture(identifier, this.getX(), this.getY(), 23, 13);
    }

    public void playDownSound(SoundManager soundManager) {
        // TODO: Myst page turn sound.
        soundManager.play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F));
    }
}
