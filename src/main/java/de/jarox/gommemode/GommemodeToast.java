package de.jarox.gommemode;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

public class GommemodeToast implements Toast {

    public static final Text ACTIVE = Text.literal("Gommemode ")
            .append(Text.translatable("gommemode.started")
                    .withColor(Colors.GREEN));

    public static final Text INACTIVE = Text.literal("Gommemode ")
            .append(Text.translatable("gommemode.stopped")
                    .withColor(Colors.RED));

    public final static int DURATION_MS = 3 * 1000;
    private final static Identifier TEXTURE = Identifier.ofVanilla("toast/advancement");
    private final boolean active;

    public GommemodeToast(boolean active) {
        this.active = active;
    }

    @Override
    public Toast.Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        if (startTime > DURATION_MS) return Visibility.HIDE;

        context.drawGuiTexture(TEXTURE, 0, 0, this.getWidth(), this.getHeight());

        context.drawCenteredTextWithShadow(manager.getClient().textRenderer,
                active ? ACTIVE : INACTIVE,
                this.getWidth() / 2, this.getHeight() / 2 - manager.getClient().textRenderer.fontHeight / 2,
                Colors.WHITE);

        return Visibility.SHOW;
    }
}
