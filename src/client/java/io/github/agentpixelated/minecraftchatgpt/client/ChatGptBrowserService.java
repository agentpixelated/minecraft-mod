package io.github.agentpixelated.minecraftchatgpt.client;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class ChatGptBrowserService {
    private static final String CHATGPT_HOME = "https://chatgpt.com/";
    private static final String RESPONSE_PREFIX = "MCGPT_RESPONSE:";
    private static final String STATUS_PREFIX = "MCGPT_STATUS:";
    private static final String ERROR_PREFIX = "MCGPT_ERROR:";

    private static MCEFBrowser browser;
    private static boolean handlersInstalled;
    private static String pendingPrompt;

    private ChatGptBrowserService() {
    }

    public static synchronized boolean ensureBrowser() {
        try {
            if (browser != null) {
                return true;
            }

            if (!MCEF.isInitialized() && !MCEF.initialize()) {
                return false;
            }

            installHandlers();
            browser = MCEF.createBrowser(CHATGPT_HOME, true, 1280, 720);
            browser.useBrowserControls(true);
            browser.setFocus(true);
            return true;
        } catch (Throwable throwable) {
            chat(Component.literal("[GPT] Browser init failed: " + throwable.getMessage()));
            return false;
        }
    }

    private static void installHandlers() {
        if (handlersInstalled) {
            return;
        }
        handlersInstalled = true;

        MCEF.getClient().addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public boolean onConsoleMessage(CefBrowser cefBrowser, CefSettings.LogSeverity severity, String message, String source, int line) {
                if (!isOurBrowser(cefBrowser) || message == null) {
                    return false;
                }

                if (message.startsWith(RESPONSE_PREFIX)) {
                    String payload = message.substring(RESPONSE_PREFIX.length());
                    String response = decode(payload);
                    Minecraft.getInstance().execute(() -> showResponse(response));
                    return true;
                }

                if (message.startsWith(STATUS_PREFIX)) {
                    String status = decode(message.substring(STATUS_PREFIX.length()));
                    Minecraft.getInstance().execute(() -> chat(Component.literal("[GPT] " + status)));
                    return true;
                }

                if (message.startsWith(ERROR_PREFIX)) {
                    String error = decode(message.substring(ERROR_PREFIX.length()));
                    Minecraft.getInstance().execute(() -> chat(Component.literal("[GPT] Error: " + error)));
                    return true;
                }

                return false;
            }
        });

        MCEF.getClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                if (!isOurBrowser(cefBrowser) || frame == null || !frame.isMain()) {
                    return;
                }

                String prompt;
                synchronized (ChatGptBrowserService.class) {
                    prompt = pendingPrompt;
                    pendingPrompt = null;
                }

                if (prompt != null && !prompt.isBlank()) {
                    injectPrompt(prompt);
                }
            }
        });
    }

    private static boolean isOurBrowser(CefBrowser candidate) {
        return browser != null && candidate != null && browser.getIdentifier() == candidate.getIdentifier();
    }

    public static void ask(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            chat(Component.literal("[GPT] Usage: /gpt <prompt>"));
            return;
        }

        if (!ensureBrowser()) {
            chat(Component.literal("[GPT] Chromium is unavailable. Use /gpt open and check the browser."));
            return;
        }

        chat(Component.literal("[GPT] Sending…"));

        String currentUrl = browser.getURL();
        if (currentUrl == null || !currentUrl.startsWith("https://chatgpt.com")) {
            synchronized (ChatGptBrowserService.class) {
                pendingPrompt = prompt;
            }
            browser.loadURL(CHATGPT_HOME);
            return;
        }

        injectPrompt(prompt);
    }

    public static void newChat() {
        if (!ensureBrowser()) {
            chat(Component.literal("[GPT] Chromium is unavailable."));
            return;
        }
        pendingPrompt = null;
        browser.loadURL(CHATGPT_HOME);
        chat(Component.literal("[GPT] New ChatGPT page opened."));
    }

    public static void showStatus() {
        if (!ensureBrowser()) {
            chat(Component.literal("[GPT] Browser: unavailable"));
            return;
        }
        chat(Component.literal("[GPT] Browser: ready | " + browser.getURL()));
    }

    public static MCEFBrowser browser() {
        return ensureBrowser() ? browser : null;
    }

    public static void resize(int width, int height) {
        MCEFBrowser active = browser();
        if (active != null) {
            active.resize(Math.max(1, width), Math.max(1, height));
        }
    }

    private static void injectPrompt(String prompt) {
        MCEFBrowser active = browser;
        if (active == null) {
            return;
        }

        String promptB64 = Base64.getEncoder().encodeToString(prompt.getBytes(StandardCharsets.UTF_8));
        String script = DOM_AUTOMATION.replace("__PROMPT_B64__", promptB64);
        active.executeJavaScript(script, active.getURL(), 0);
    }

    private static String decode(String payload) {
        try {
            return new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return payload;
        }
    }

    private static void showResponse(String response) {
        if (response == null || response.isBlank()) {
            chat(Component.literal("[GPT] Empty response."));
            return;
        }

        chat(Component.literal("[GPT] ─────────────────"));
        for (String line : response.split("\\R", -1)) {
            if (line.isEmpty()) {
                chat(Component.literal(" "));
                continue;
            }

            int offset = 0;
            while (offset < line.length()) {
                int end = Math.min(line.length(), offset + 300);
                chat(Component.literal((offset == 0 ? "[GPT] " : "      ") + line.substring(offset, end)));
                offset = end;
            }
        }
    }

    public static void chat(Component message) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.displayClientMessage(message, false);
            }
        });
    }

    private static final String DOM_AUTOMATION = """
            (() => {
              const RESPONSE = 'MCGPT_RESPONSE:';
              const STATUS = 'MCGPT_STATUS:';
              const ERROR = 'MCGPT_ERROR:';
              const promptB64 = '__PROMPT_B64__';

              const b64ToUtf8 = (s) => decodeURIComponent(Array.prototype.map.call(atob(s), c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''));
              const utf8ToB64 = (s) => btoa(unescape(encodeURIComponent(s)));
              const emit = (prefix, value) => console.log(prefix + utf8ToB64(String(value ?? '')));
              const prompt = b64ToUtf8(promptB64);

              const findComposer = () =>
                document.querySelector('#prompt-textarea') ||
                document.querySelector('[data-testid="prompt-textarea"]') ||
                document.querySelector('[role="textbox"][contenteditable="true"]') ||
                document.querySelector('textarea');

              const assistantMessages = () => Array.from(document.querySelectorAll('[data-message-author-role="assistant"]'));

              const writePrompt = (el) => {
                el.focus();
                if (el instanceof HTMLTextAreaElement || el instanceof HTMLInputElement) {
                  const proto = el instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
                  const setter = Object.getOwnPropertyDescriptor(proto, 'value')?.set;
                  if (setter) setter.call(el, prompt); else el.value = prompt;
                  el.dispatchEvent(new Event('input', { bubbles: true }));
                  el.dispatchEvent(new Event('change', { bubbles: true }));
                  return;
                }

                try {
                  document.execCommand('selectAll', false, null);
                  document.execCommand('insertText', false, prompt);
                } catch (_) {
                  el.textContent = prompt;
                }
                el.dispatchEvent(new InputEvent('input', { bubbles: true, inputType: 'insertText', data: prompt }));
              };

              const findSendButton = () => {
                const direct = document.querySelector('button[data-testid="send-button"]');
                if (direct) return direct;
                return Array.from(document.querySelectorAll('button')).find((b) => {
                  const label = `${b.getAttribute('aria-label') || ''} ${b.title || ''}`.toLowerCase();
                  return label.includes('send') || label.includes('submit');
                });
              };

              const isGenerating = () => {
                if (document.querySelector('button[data-testid="stop-button"]')) return true;
                return Array.from(document.querySelectorAll('button')).some((b) => {
                  const label = `${b.getAttribute('aria-label') || ''} ${b.title || ''}`.toLowerCase();
                  return label.includes('stop generating') || label === 'stop';
                });
              };

              const monitor = (beforeCount, beforeText) => {
                let lastText = '';
                let stableTicks = 0;
                const started = Date.now();

                const timer = setInterval(() => {
                  const messages = assistantMessages();
                  const latest = messages[messages.length - 1];
                  const text = latest?.innerText?.trim() || '';
                  const hasNew = messages.length > beforeCount || (text && text !== beforeText);

                  if (hasNew && text && text === lastText && !isGenerating()) {
                    stableTicks++;
                  } else {
                    stableTicks = 0;
                  }
                  lastText = text;

                  if (hasNew && text && stableTicks >= 2) {
                    clearInterval(timer);
                    emit(RESPONSE, text);
                    return;
                  }

                  if (Date.now() - started > 180000) {
                    clearInterval(timer);
                    emit(ERROR, 'Timed out waiting for ChatGPT response. Open /gpt open to inspect the page.');
                  }
                }, 750);
              };

              let attempts = 0;
              const trySend = () => {
                attempts++;
                const composer = findComposer();
                if (!composer) {
                  if (attempts < 120) {
                    setTimeout(trySend, 250);
                  } else {
                    emit(ERROR, 'Prompt box was not found. You may need to log in with /gpt open, or the ChatGPT web UI changed.');
                  }
                  return;
                }

                const before = assistantMessages();
                const beforeCount = before.length;
                const beforeText = before[before.length - 1]?.innerText?.trim() || '';

                writePrompt(composer);
                setTimeout(() => {
                  const send = findSendButton();
                  if (send && !send.disabled) {
                    send.click();
                  } else {
                    composer.dispatchEvent(new KeyboardEvent('keydown', {
                      key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true, cancelable: true
                    }));
                    composer.dispatchEvent(new KeyboardEvent('keyup', {
                      key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true, cancelable: true
                    }));
                  }
                  emit(STATUS, 'Prompt submitted through chatgpt.com');
                  monitor(beforeCount, beforeText);
                }, 150);
              };

              trySend();
            })();
            """;
}
