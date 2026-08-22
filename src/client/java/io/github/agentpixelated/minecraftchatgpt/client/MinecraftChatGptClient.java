package io.github.agentpixelated.minecraftchatgpt.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class MinecraftChatGptClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("gpt")
                        .executes(context -> {
                            openBrowser();
                            return 1;
                        })
                        .then(literal("open").executes(context -> {
                            openBrowser();
                            return 1;
                        }))
                        .then(literal("login").executes(context -> {
                            openBrowser();
                            return 1;
                        }))
                        .then(literal("new").executes(context -> {
                            ChatGptBrowserService.newChat();
                            return 1;
                        }))
                        .then(literal("status").executes(context -> {
                            ChatGptBrowserService.showStatus();
                            return 1;
                        }))
                        .then(argument("prompt", StringArgumentType.greedyString())
                                .executes(context -> {
                                    ChatGptBrowserService.ask(StringArgumentType.getString(context, "prompt"));
                                    return 1;
                                }))
        ));
    }

    private static void openBrowser() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (!ChatGptBrowserService.ensureBrowser()) {
                ChatGptBrowserService.chat(Component.literal("[GPT] Chromium could not be initialized."));
                return;
            }
            client.gui.setScreen(new ChatGptBrowserScreen());
        });
    }
}
