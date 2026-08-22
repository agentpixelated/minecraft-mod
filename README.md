# Minecraft ChatGPT Web

A **Fabric client-side mod for Minecraft 26.1.2** that lets you use ChatGPT from Minecraft through the normal `chatgpt.com` website.

It intentionally does **not** use an OpenAI API key and does **not** extract OAuth/access tokens from your browser. Authentication happens manually inside the embedded Chromium browser provided by MCEF/Rinku, just like signing in on a normal website.

## Architecture

```text
Minecraft client
    |
    | /gpt <prompt>   (Fabric client command; Paper never receives it)
    v
Minecraft ChatGPT Web
    |
    | JavaScript DOM interaction
    v
Embedded Chromium (MCEF/Rinku)
    |
    v
https://chatgpt.com
    |
    | console bridge
    v
Minecraft chat
```

## Requirements

- Minecraft Java Edition **26.1.2**
- Fabric Loader **0.19.3+**
- Fabric API **0.155.2+26.1.2**
- Java **25**
- MCEF / Rinku (Keksuccino fork), compatible with Minecraft 26.1.x

This is entirely client-side. Your Paper server does not need this mod or MCEF installed.

## Commands

| Command | Action |
| --- | --- |
| `/gpt` | Open the ChatGPT browser |
| `/gpt open` | Open the ChatGPT browser |
| `/gpt login` | Open the browser so you can sign in manually |
| `/gpt <prompt>` | Send a prompt through the ChatGPT website |
| `/gpt new` | Return to the ChatGPT home/new-chat page |
| `/gpt status` | Show browser status/current URL |

Example:

```text
/gpt explain how an observer works in Minecraft redstone
```

The command is registered as a Fabric **client command**, so it is consumed locally and is not sent to Paper.

## First run

1. Install Fabric API and MCEF/Rinku alongside this mod.
2. Start Minecraft 26.1.2.
3. Join any single-player world or multiplayer server.
4. Run `/gpt login`.
5. Wait for Chromium to initialize/download its runtime if required.
6. Sign into `chatgpt.com` manually in the embedded browser.
7. Close the browser with Escape or the **Close** button.
8. Run `/gpt hello from Minecraft`.

The embedded browser keeps its own normal browser profile/session according to MCEF/Rinku's Chromium storage behavior.

## How the web bridge works

When `/gpt <prompt>` is executed, the mod:

1. Locates ChatGPT's web composer using semantic DOM selectors.
2. Inserts the prompt and dispatches normal input events.
3. Presses the website's send button (with keyboard fallback).
4. Watches the newest assistant message while ChatGPT is generating.
5. When the response is stable, JavaScript writes a Base64-encoded response to a specially prefixed browser console message.
6. MCEF's display handler receives that console event and the mod prints the decoded answer into Minecraft chat.

No OpenAI REST endpoint is called by this mod.

## Reliability / limitations

This is web UI automation. It can break if `chatgpt.com` changes its DOM structure, if Chromium is blocked by a login/security challenge, or if the site's behavior changes. The selectors are deliberately isolated in `ChatGptBrowserService.DOM_AUTOMATION` so they can be updated without redesigning the mod.

The project does not attempt to bypass CAPTCHA, account security, rate limits, or access controls.

## Building

GitHub Actions builds the project automatically with Java 25 and Gradle 9.5.1.

Locally, with Gradle 9.5.1 available:

```bash
gradle build
```

Output is placed in:

```text
build/libs/
```

## Credits

- Fabric / Fabric API
- MCEF / Rinku and JCEF/Chromium
- `dev-r-git/ChromiumBrowserMC` was used as an API/reference implementation for MCEF rendering and Minecraft 26.x input forwarding.

## License

MIT for this mod's source code. MCEF/Rinku and Chromium/JCEF remain under their respective licenses and are external dependencies.
