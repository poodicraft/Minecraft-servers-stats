# ServerScope

A free, native Android app that checks the status of any Minecraft server, Java or Bedrock.
Written in Kotlin with Jetpack Compose (Material 3). No accounts, no ads, no API keys, and the
only permission it asks for is **Internet**.

<p align="center"><img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="96" alt="ServerScope icon"></p>

## Download

- **Ready-made APK:** [`ServerScope-debug.apk`](ServerScope-debug.apk) in this folder, or the
  latest CI build on the [Releases page](../../releases) (look for the `debug-…` pre-release).
- Runs on Android 7.0 (API 24) and newer, phones and tablets.

## Features

- **Check any server.** Type an IP or domain (an optional `:port` is fine), choose Java or
  Bedrock, then tap **Check**. Typos get a friendly message before any network call.
- **Result screen:**
  - Online/offline badge, server icon, MOTD (clean text), version, protocol and software.
  - Players online/max with an XP-bar meter.
  - The player list with each player's head. Tap a player for their name, UUID (with a copy
    button) and a full skin render.
  - Plugins and mods, when the server reports them.
  - Hostname, resolved IP, port, SRV record and whether Mojang has EULA-blocked the server.
  - Pull to refresh, plus an optional auto-refresh every 30 seconds that pauses when the app
    is in the background.
- **Favorites.** Tap the heart to pin a server. Favorites appear on the home screen with an
  online/offline light and the player count, re-checked when you open the app (or pull down).
- **Recent searches.** The last 12 checks, one tap to repeat.
- **Error handling.** Invalid addresses, no internet, timeouts, rate limiting, offline servers
  and malformed responses each get a clear message. Nothing crashes the app; the parser treats
  every field as optional.
- **Dark Minecraft look.** Grass/dirt/stone colors, "Press Start 2P" pixel font for headings and
  the system font for body text. Layouts adapt to tablets: two columns on wide screens.

### What ServerScope deliberately doesn't show

- **Per-player stats** (playtime, kills, deaths, inventory). The public Minecraft status
  protocol doesn't include them. The player card only shows what's real (name, UUID, skin)
  and says: *"Detailed stats require the server to run a stats plugin."*
- **Hidden player lists.** Many servers hide or cap the list. When a server reports players
  online but no names, the app says **"Player list hidden by server"**. When it shares only a
  sample (vanilla caps it at 12), the app says how many of the total it is showing. Bedrock
  servers never share names, only the count. Text lines that some networks put in the sample
  (the all-zero UUID) appear as a server message, not as fake players.
- **Ping.** The mcstatus.io v2 API doesn't report in-game latency. The app shows the
  lookup time (how long the API took to answer) and labels it that way.

## Data sources (all free, no keys)

| What | Where |
| --- | --- |
| Server status | `GET https://api.mcstatus.io/v2/status/java/{address}` and `/v2/status/bedrock/{address}` (results are cached by mcstatus.io for about a minute) |
| Player heads | `https://mc-heads.net/avatar/{uuid}/128`, falling back to `https://minotar.net/avatar/{name}/128` |
| Skin render | `https://mc-heads.net/body/{uuid}/256`, falling back to `https://minotar.net/body/{name}/256` |

Libraries: Retrofit + OkHttp, kotlinx.serialization, Coil 3, Navigation Compose, DataStore.

## Install the APK on your phone

1. Copy `ServerScope-debug.apk` to your phone, or open the Releases page on the phone and
   download it there.
2. Allow installs from the app you'll open the APK with. This is a one-time setting:
   - **Android 8.0 and newer:** open the APK from your browser or Files app. When Android says
     installing unknown apps isn't allowed, tap **Settings** and turn on **Allow from this
     source**, then go back. The setting also lives at *Settings → Apps → Special app access →
     Install unknown apps → (your browser or Files app) → Allow from this source*. Menu names
     vary slightly between phone makers.
   - **Android 7.x:** *Settings → Security → Unknown sources → On*.
3. Tap **Install**. Google Play Protect may warn that the app is from an unknown developer.
   That's expected for apps not installed through the Play Store; choose **Install anyway**.
4. Optionally switch "Install unknown apps" back off for that app afterwards.

With a USB cable and [platform-tools](https://developer.android.com/tools/releases/platform-tools)
you can instead enable *Developer options → USB debugging* and run:

```bash
adb install -r ServerScope-debug.apk
```

## Build it yourself

### Requirements

- **JDK 17 or newer** (21 recommended), e.g. [Eclipse Temurin](https://adoptium.net).
  Check with `java -version`.
- **Android SDK** with the Android 17 (API 37) platform. Either install
  [Android Studio](https://developer.android.com/studio) and open this folder (it installs
  everything it needs), or use the command-line tools only:

```bash
# Linux/macOS. Download "Command line tools only" from
# https://developer.android.com/studio#command-line-tools-only first.
mkdir -p "$HOME/Android/Sdk/cmdline-tools"
unzip commandlinetools-*_latest.zip -d "$HOME/Android/Sdk/cmdline-tools"
mv "$HOME/Android/Sdk/cmdline-tools/cmdline-tools" "$HOME/Android/Sdk/cmdline-tools/latest"

export ANDROID_HOME="$HOME/Android/Sdk"   # add these two lines to ~/.bashrc or ~/.zshrc
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

sdkmanager --licenses                       # accept with "y"
sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
```

On Windows, unzip to `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest`, set the `ANDROID_HOME`
environment variable to `%LOCALAPPDATA%\Android\Sdk`, and run the same `sdkmanager` commands
from `cmdline-tools\latest\bin`. Instead of `ANDROID_HOME` you can also create
`local.properties` in the project root containing `sdk.dir=/path/to/Android/Sdk`.

Gradle itself doesn't need installing: the wrapper (`./gradlew`) downloads the right version
(9.7.1). The build uses Android Gradle Plugin 9.4, Kotlin 2.4 and Compose BOM 2026.09.

### Build the debug APK

```bash
./gradlew assembleDebug                 # Windows: gradlew.bat assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk ServerScope-debug.apk
```

### Run the unit tests

```bash
./gradlew testDebugUnitTest
```

The parser tests in `app/src/test` use real, unmodified mcstatus.io responses captured on
2026-10-01 (`app/src/test/resources/fixtures`): an online Java server with players, mods and
plugins; two offline Java servers (one that resolves, one that doesn't); three online Bedrock
servers; and an offline Bedrock server. Other tests cover missing and malformed fields,
address validation and error mapping.

## Signed release APK

A debug APK is signed with a throwaway debug key. For an APK you keep updating over time (or
share), sign a release build with your own key:

1. **Create a keystore once** and keep it safe. You need the same key for every future update.

   ```bash
   keytool -genkeypair -v -keystore serverscope-release.jks -alias serverscope \
     -keyalg RSA -keysize 4096 -validity 10000
   ```

2. **Create `keystore.properties`** in the project root. Both this file and `*.jks` are in
   `.gitignore`, so never commit them.

   ```properties
   storeFile=serverscope-release.jks
   storePassword=your-store-password
   keyAlias=serverscope
   keyPassword=your-key-password
   ```

3. **Build:**

   ```bash
   ./gradlew assembleRelease
   ```

   The signed, minified (R8) APK is `app/build/outputs/apk/release/app-release.apk`. Without
   `keystore.properties` the same command produces `app-release-unsigned.apk`, which Android
   won't install.

4. **Optionally verify the signature:**

   ```bash
   "$ANDROID_HOME/build-tools/37.0.0/apksigner" verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```

A phone can't update an app that was signed with a different key, so uninstall the debug
build before installing your release build.

## Continuous integration

`.github/workflows/android.yml` runs on every push: unit tests, debug and release builds, a
check of the APK's permissions and SDK levels, then it uploads the debug APK as a workflow
artifact and publishes it as a `debug-<branch>` pre-release.

## Project layout

```
app/src/main/java/io/github/poodicraft/serverscope/
├── data/
│   ├── remote/        Retrofit API + raw JSON DTOs (every field optional)
│   ├── local/         Favorites and history in Preferences DataStore
│   ├── model/         Clean domain models (ServerStatus, Player, ...)
│   ├── StatusParser   JSON → ServerStatus, defensive mapping
│   ├── ServerAddress  Input validation (host, IPv4, [IPv6], port)
│   ├── StatusError    Exceptions/HTTP codes → friendly messages
│   └── StatusRepository
└── ui/
    ├── home/          Search, favorites, recent searches
    ├── result/        Result screen, auto-refresh, player detail sheet
    ├── components/    Pixel-style cards, meters, badges, images
    └── theme/         Colors, typography, shapes
tools/make_icons.py    Regenerates the pixel-art launcher icon and drawables
```

## Credits

- Server status by [mcstatus.io](https://mcstatus.io).
- Player renders by [mc-heads.net](https://mc-heads.net) and [minotar.net](https://minotar.net).
- [Press Start 2P](https://fonts.google.com/specimen/Press+Start+2P) by CodeMan38, SIL Open
  Font License 1.1 (see `licenses/PressStart2P-OFL.txt`).

ServerScope isn't affiliated with Mojang or Microsoft. Minecraft is a trademark of Mojang AB.
