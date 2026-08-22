package io.github.agentpixelated.minecraftchatgpt.client;

import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ChatGptBrowserScreen extends Screen {
    private static final int TOOLBAR_HEIGHT = 24;
    private static final int BACKGROUND = 0xFF101114;
    private static final int TOOLBAR = 0xFF202124;
    private static final int TEXT = 0xFFE8EAED;

    public ChatGptBrowserScreen() {
        super(Component.literal("ChatGPT Web"));
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            MCEFBrowser browser = ChatGptBrowserService.browser();
            if (browser != null && browser.canGoBack()) browser.goBack();
        }).bounds(4, 3, 24, 18).build());

        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            MCEFBrowser browser = ChatGptBrowserService.browser();
            if (browser != null && browser.canGoForward()) browser.goForward();
        }).bounds(30, 3, 24, 18).build());

        addRenderableWidget(Button.builder(Component.literal("Reload"), button -> {
            MCEFBrowser browser = ChatGptBrowserService.browser();
            if (browser != null) browser.reload();
        }).bounds(56, 3, 52, 18).build());

        addRenderableWidget(Button.builder(Component.literal("New chat"), button -> ChatGptBrowserService.newChat())
                .bounds(110, 3, 66, 18).build());

        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(Math.max(178, width - 58), 3, 54, 18).build());

        resizeBrowser();
        MCEFBrowser browser = ChatGptBrowserService.browser();
        if (browser != null) browser.setFocus(true);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        resizeBrowser();
    }

    private void resizeBrowser() {
        int scale = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
        ChatGptBrowserService.resize(width * scale, Math.max(1, height - TOOLBAR_HEIGHT) * scale);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        graphics.fill(0, 0, width, height, BACKGROUND);
        graphics.fill(0, 0, width, TOOLBAR_HEIGHT, TOOLBAR);
        graphics.text(font, Component.literal("chatgpt.com — normal web session; no API key"), 184, 8, TEXT);

        MCEFBrowser browser = ChatGptBrowserService.browser();
        if (browser != null && browser.isTextureReady()) {
            Identifier texture = browser.getTextureIdentifier();
            if (texture != null) {
                RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
                graphics.blit(
                        pipeline,
                        texture,
                        0,
                        TOOLBAR_HEIGHT,
                        0.0F,
                        0.0F,
                        width,
                        Math.max(1, height - TOOLBAR_HEIGHT),
                        width,
                        Math.max(1, height - TOOLBAR_HEIGHT)
                );
            }
        } else {
            graphics.centeredText(font, Component.literal("Loading Chromium…"), width / 2, height / 2, TEXT);
        }

        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (!insideBrowser(event.x(), event.y())) return false;

        MCEFBrowser browser = ChatGptBrowserService.browser();
        if (browser == null) return false;

        browser.sendMousePress(browserX(event.x()), browserY(event.y()), event.button());
        browser.setFocus(true);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (super.mouseReleased(event)) return true;
        if (!insideBrowser(event.x(), event.y())) return false;

        MCEFBrowser browser = ChatGptBrowserService.browser();
        if (browser == null) return false;

        browser.sendMouseRelease(browserX(event.x()), browserY(event.y()), event.button());
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        MCEFBrowser browser = ChatGptBrowserService.browser();
        if (browser != null && insideBrowser(mouseX, mouseY)) {
            browser.sendMouseMove(browserX(mouseX), browserY(mouseY));
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        MCEFBrowser browser = ChatGptBrowserService.browser();
        if (browser != null && insideBrowser(event.x(), event.y())) {
            browser.sendMouseMove(browserX(event.x()), browserY(event.y()));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
        if (!insideBrowser(mouseX, mouseY)) return false;

        MCEFBrowser browser = ChatGptBrowserService.browser();
        if (browser == null) return false;

        browser.sendMouseWheel(
                browserX(mouseX),
                browserY(mouseY),
                (int) Math.round(verticalAmount),
                (int) Math.round(horizontalAmount)
        );
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (super.keyPressed(event)) return true;

        MCEFBrowser browser = ChatGptBrowserService.browser();
        if (browser == null) return false;
        browser.sendKeyPress(event.key(), event.scancode(), event.modifiers());
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (super.keyReleased(event)) return true;

        MCEFBrowser browser = ChatGptBrowserService.browser();
        if (browser == null) return false;
        browser.sendKeyRelease(event.key(), event.scancode(), event.modifiers());
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (super.charTyped(event)) return true;
        if (event.codepoint() == 0) return false;

        MCEFBrowser browser = ChatGptBrowserService.browser();
        if (browser == null) return false;
        browser.sendKeyTyped((char) event.codepoint(), 0);
        return true;
    }

    @Override
    public void onClose() {
        MCEFBrowser browser = ChatGptBrowserService.browser();
        if (browser != null) browser.setFocus(false);
        super.onClose();
    }

    private boolean insideBrowser(double x, double y) {
        return x >= 0 && x < width && y >= TOOLBAR_HEIGHT && y < height;
    }

    private int browserX(double x) {
        return (int) (x * guiScale());
    }

    private int browserY(double y) {
        return (int) ((y - TOOLBAR_HEIGHT) * guiScale());
    }

    private int guiScale() {
        return Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
    }
}
