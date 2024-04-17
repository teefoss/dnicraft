package net.teefoss.dnicraft;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.api.example.ExampleGuiPortalRendering;
import qouteall.imm_ptl.core.chunk_loading.ChunkLoader;
import qouteall.imm_ptl.core.chunk_loading.DimensionalChunkPos;
import qouteall.imm_ptl.core.render.GuiPortalRendering;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.q_misc_util.my_util.DQuaternion;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class DniBookScreen {
    @Environment(EnvType.CLIENT)
    private static Framebuffer frameBuffer;

    private static final WeakHashMap<ServerPlayerEntity, ChunkLoader> chunkLoaderMap = new WeakHashMap<>();

    private static void removeChunkLoaderFor(ServerPlayerEntity player) {
        ChunkLoader chunkLoader = chunkLoaderMap.remove(player);
        if (chunkLoader != null) {
            PortalAPI.removeChunkLoaderForPlayer(player, chunkLoader);
        }
    }

    public static void onCommandExecuted(
        ServerPlayerEntity player,
        ServerWorld world,
        Vec3d pos,
        ItemStack stack) {
        removeChunkLoaderFor(player);

        ChunkLoader chunkLoader = new ChunkLoader(
            new DimensionalChunkPos(
                world.getRegistryKey(), new ChunkPos(BlockPos.ofFloored(pos))
            ),
            8
        );

        // Add the per-player additional chunk loader
        PortalAPI.addChunkLoaderForPlayer(player, chunkLoader);
        chunkLoaderMap.put(player, chunkLoader);

        // Tell the client to open the screen
        McRemoteProcedureCall.tellClientToInvoke(
            player,
            "net.teefoss.dnicraft.DniBookScreen.RemoteCallables.clientActivateGuiPortal",
            world.getRegistryKey(),
            pos,
            stack
        );
    }

    public static class RemoteCallables {
        @Environment(EnvType.CLIENT)
        public static void clientActivateGuiPortal(
            RegistryKey<World> dimension,
            Vec3d position,
            ItemStack stack
        ) {
            if (frameBuffer == null) {
                // the framebuffer size doesn't matter here
                // because it will be automatically resized when rendering
                frameBuffer = new SimpleFramebuffer(2, 2, true, true);
            }

            MinecraftClient.getInstance().setScreen(new DniBookScreen.GuiPortalScreen(dimension, position, stack));
        }

        public static void serverRemoveChunkLoader(ServerPlayerEntity player) {
            removeChunkLoaderFor(player);
        }
    }

    @Environment(EnvType.CLIENT)
    public static class GuiPortalScreen extends Screen {
        private static final Identifier FONT_ID = new Identifier(DniCraft.MOD_ID, "ascii-dni");
        private static final int CHAR_WIDTH = 4;
        private static final int CHAR_HEIGHT = 4;
        private static final int CHARS_PER_LINE = 24;
        private static final int LINES_PER_PAGE = 40; // PAGE == two-up
        private static final Identifier BOOK_TEXTURE = new Identifier(DniCraft.MOD_ID, "textures/gui/book.png");
        private static final int BOOK_WIDTH = 256;
        private static final int BOOK_HEIGHT = 192;

        protected static final int LINK_PANEL_X = 139; // relative to book image origin
        protected static final int LINK_PANEL_Y = 34;
        private static final int LINK_PANEL_WIDTH = 95;
        private static final int LINK_PANEL_HEIGHT = 66;

        private final RegistryKey<World> viewingDimension;
        private final Vec3d viewingPosition;
        private ItemStack stack;

        private int page = -1;
        private int numPages;
        private int cx = 0;
        private int cy = 0;
        private int ticks = 0;
        private List<String> pages = new ArrayList<>();
        private String text = "";

        private DniPageTurnWidget nextPageButton;
        private DniPageTurnWidget endButton;
        private DniPageTurnWidget previousPageButton;
        private DniPageTurnWidget beginningButton;
        private ButtonWidget linkPanel;

        public GuiPortalScreen(RegistryKey<World> viewingDimension, Vec3d viewingPosition, ItemStack stack) {
            super(Text.literal("GUI Portal Example"));
            this.viewingDimension = viewingDimension;
            this.viewingPosition = viewingPosition;
            this.stack = stack;

            NbtCompound tag = stack.getNbt();
            if (tag != null) {
                numPages = tag.getInt("numPages");
            } else {
                numPages = 1;
            }
        }

        @Override
        protected void init() {
            super.init();

            int x = getBookX() + LINK_PANEL_X;
            int y = getBookY() + LINK_PANEL_Y;

            linkPanel = ButtonWidget.builder(Text.literal("Gateway Image"), button -> {
                    McRemoteProcedureCall.tellServerToInvoke(
                        "net.teefoss.dnicraft.Link.RemoteCallables.transportPlayer"
                    );
                })
                .dimensions(x, y, LINK_PANEL_WIDTH, LINK_PANEL_HEIGHT)
                .build();

            addSelectableChild(linkPanel);

            int center = (this.width - 192) / 2;
            int buttonY = BOOK_HEIGHT + 2;
            int buttonWidth = 23;

            this.previousPageButton = (DniPageTurnWidget) this.addDrawableChild(
                new DniPageTurnWidget(
                    getBookX(),
                    buttonY,
                    false,
                    true,
                    (button) -> {
                        turnPage(-1, false);
                    })
            );

            this.beginningButton = (DniPageTurnWidget) this.addDrawableChild(
                new DniPageTurnWidget(
                    previousPageButton.getX() + buttonWidth + 2,
                    buttonY,
                    false,
                    false,
                    (button) -> {
                        turnPage(-1, true);
                    })
            );

            this.nextPageButton = (DniPageTurnWidget) this.addDrawableChild(
                new DniPageTurnWidget(
                    getBookX() + BOOK_HEIGHT - 23 * 2,
                    buttonY,
                    true,
                    true,
                    (button) -> {
                        turnPage(+1, false);
                    })
            );

            this.endButton = (DniPageTurnWidget) this.addDrawableChild(
                new DniPageTurnWidget(
                    nextPageButton.getX() - (buttonWidth + 2),
                    buttonY,
                    true,
                    false,
                    (button) -> {
                        turnPage(+1, true);
                    })
            );

            updateButtons();
        }

        public void tick() {
            super.tick();
            ticks++;
        }

        private void updateButtons() {
            nextPageButton.visible = page != numPages;
            endButton.visible = page != numPages;
            previousPageButton.visible = page != -1;
            beginningButton.visible = page != -1;
        }

        private void saveCurrentPageToNBT() {
            if (page >= 0) {
                NbtCompound tag = stack.getOrCreateNbt();
                tag.putInt("numPages", numPages);
                tag.putString("page" + page, text);
                stack.setNbt(tag);
            }
        }

        private void turnPage(int step, boolean allTheWay) {
            saveCurrentPageToNBT();

            // Change page.
            if (step == 1) {
                page = allTheWay ? numPages - 1 : page + 1;
            } else if (step == -1) {
                page = allTheWay ? -1 : page - 1;
            }

            // Load text for new page from NBT, if present.
            if (page >= 0) {
                NbtCompound tag = stack.getNbt();
                if (tag != null) {
                    text = tag.getString("page" + page);
                } else {
                    text = "";
                }
            }

            updateButtons();
        }

        @Override
        public void close() {
            super.close();

            // Tell the server to remove the additional chunk loader
            McRemoteProcedureCall.tellServerToInvoke(
                "net.teefoss.dnicraft.DniBookScreen.RemoteCallables.serverRemoveChunkLoader"
            );
        }

        private int getBookX() {
            return (width - BOOK_WIDTH) / 2;
        }

        private int getBookY() {
            return 2;
        }

        @Override
        public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
            super.renderBackground(context, mouseX, mouseY, delta);

            context.drawTexture(BOOK_TEXTURE, getBookX(), getBookY(), 0, 0, BOOK_WIDTH, BOOK_HEIGHT);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);

            if (page == -1) {
                if (viewingDimension != null) {
                    renderGatewayImage();
                }
            } else {
                // Render page content.
                renderCursor(context);
            }
        }

        private void renderCursor(DrawContext context) {
            if ((ticks / 6) % 2 == 0) {
                int drawX = getBookX() + 16 + cx * CHAR_WIDTH;
                int drawY = (cy * 8) + getBookY() + 16;

                if (cy >= LINES_PER_PAGE / 2) { // On left side of book?
                    drawX += BOOK_WIDTH / 2;
                    drawY -= LINES_PER_PAGE * CHAR_HEIGHT;
                }

                context.drawText(
                    textRenderer,
                    Text.literal("_").fillStyle(Style.EMPTY.withFont(FONT_ID)),
                    drawX,
                    drawY,
                    0,
                    false
                );
            }
        }

        private void renderGatewayImage() {
            double t1 = CHelper.getSmoothCycles(160);
            // double t2 = CHelper.getSmoothCycles(400);

            // Determine the camera transformation
            Matrix4f cameraTransformation = new Matrix4f();
            cameraTransformation.identity();
            cameraTransformation.mul(
                DQuaternion.rotationByDegrees(
                    new Vec3d(0, -1.0f, 0).normalize(),
                    t1 * 360
                ).toMatrix()
            );

            // Determine the camera position
//            Vec3d cameraPosition = this.viewingPosition.add(
//                new Vec3d(Math.cos(t2 * 2 * Math.PI), 0, Math.sin(t2 * 2 * Math.PI)).multiply(30)
//            );

            // Create the world render info
            WorldRenderInfo worldRenderInfo = new WorldRenderInfo.Builder()
                .setWorld(ClientWorldLoader.getWorld(viewingDimension))
                .setCameraPos(this.viewingPosition)
                .setCameraTransformation(cameraTransformation)
                .setOverwriteCameraTransformation(true) // do not apply camera transformation to existing player camera transformation
                .setDescription(null)
                .setRenderDistance(client.options.getClampedViewDistance())
                .setDoRenderHand(false)
                .setEnableViewBobbing(false)
                .setDoRenderSky(false)
                .setHasFog(true)
                .build();

            // Ask it to render the world into the framebuffer the next frame
            GuiPortalRendering.submitNextFrameRendering(worldRenderInfo, frameBuffer);

            double guiScale = client.getWindow().getScaleFactor();
            float xMin = (getBookX() + LINK_PANEL_X) * (float) guiScale;
            double xMax = xMin + LINK_PANEL_WIDTH * guiScale;
            float yMin = getBookY() + LINK_PANEL_Y * (float) guiScale;
            double yMax = yMin + LINK_PANEL_HEIGHT * guiScale;

            MyRenderHelper.drawFramebuffer(frameBuffer, true, false, xMin, xMax, yMin, yMax);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }

        public boolean charTyped(char ch, int modifiers) {
            if (super.charTyped(ch, modifiers)) {
                return true;
            }

            // String pageText = pages.get(page);
            return false;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (super.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }

            switch (keyCode) {
                case 262: // Right
                    if (cx == CHARS_PER_LINE - 1) {
                        cx = 0;
                        if (cy != LINES_PER_PAGE - 1) {
                            cy++;
                        }
                    } else {
                        cx++;
                    }
                    return true;
                case 263: // Left
                    cx--;
                    if (cx < 0) {
                        if (cy > 0) {
                            cx = CHARS_PER_LINE - 1;
                            cy--;
                        } else {
                            cx = 0;
                        }
                    }
                    return true;
                case 264: // Down?
                    if (cy < LINES_PER_PAGE - 1) {
                        cy++;
                    }
                    return true;
                case 265: // Up?
                    if (cy > 0) {
                        cy--;
                    }
                    return true;
                default:
                    return false;
            }
        }
    }
}