# Releasing to itch.io

Two APKs go up: the free edition as a free download, the Pro edition behind the
price. Nothing in the app talks to itch.io — a purchase is simply a different
APK — so there is no billing code, no licence check and nothing to go wrong
offline.

## 1. A signing key

Android will not install an unsigned release build, and every update must be
signed with the *same* key. Make one once and keep it safe; losing it means
never being able to update the app under the same identity.

```sh
keytool -genkey -v -keystore premium-english.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias premium-english
```

Then create `keystore.properties` in the repository root:

```properties
storeFile=premium-english.jks
storePassword=…
keyAlias=premium-english
keyPassword=…
```

Both the keystore and that file are in `.gitignore`. If the file is missing the
release build still runs, it is just left unsigned.

## 2. Bump the version

In `app/build.gradle.kts`, raise `versionCode` by one for every upload and set
`versionName` to something people can read:

```kotlin
versionCode = 2
versionName = "1.1"
```

## 3. Build

```sh
./gradlew clean
./gradlew testFreeDebugUnitTest testProDebugUnitTest    # both suites first
./gradlew assembleFreeRelease assembleProRelease
```

Which leaves:

```
app/build/outputs/apk/free/release/app-free-release.apk
app/build/outputs/apk/pro/release/app-pro-release.apk
```

Check both are signed before uploading anything:

```sh
$ANDROID_HOME/build-tools/34.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/pro/release/app-pro-release.apk
```

## 4. The itch.io page

One project page, priced, with the free edition attached as a free download:

1. Create the project, set **Kind of project** to *Downloadable*.
2. Set **Pricing** to *Paid* with your price (or *Pay what you want* with a
   minimum). This is what the Pro APK sits behind.
3. Upload both APKs. On the free one, tick the box that makes a file free to
   download — on itch.io this is the "demo / free download" flag on the upload
   row. Leave the Pro APK without it, so it requires a purchase.
4. Set the platform on both uploads to **Android**.
5. Under *Metadata*, the release status should be *Released*.

Check the page while logged out: the free APK should be downloadable and the Pro
one should show the buy button.

## 5. Uploading with butler

[butler](https://itch.io/docs/butler/) makes the upload repeatable. Log in once
with `butler login`, then:

```sh
butler push app/build/outputs/apk/free/release/app-free-release.apk \
  ellosan/premium-english-keyboard:android-free --userversion 1.1

butler push app/build/outputs/apk/pro/release/app-pro-release.apk \
  ellosan/premium-english-keyboard:android-pro --userversion 1.1
```

Channel names are yours to choose, but keep them stable — itch.io treats a
channel as the thing being updated. Whether a channel is free or paid is still
set on the web page, not by butler, so check that after the first push of a new
channel.

## Store page copy

Something to start from:

> **Premium English Keyboard**
>
> An Android keyboard that translates what you type into Shakespearean English.
> Type your message the ordinary way, tap ✦, and *hello, how are you?* becomes
> **Hail, how dost thou fare?** — in place, in any app.
>
> It is a real keyboard as well as a joke: sizes from compact to huge, a number
> row, digits and symbols on long press, emoji, self-capitalising sentences and
> a double-space full stop. Translation stays out of password, email and URL
> fields, and one key suspends it entirely.
>
> **Free edition** — the Refined tier: your writing, in a better register.
>
> **Pro** — the Courtly and Sovereign tiers: *thou* and *thee*, the *-est* and
> *-eth* endings, several hundred more words of Shakespearean vocabulary,
> ceremonial flourishes and ye olde spellings.
>
> No ads, no accounts, no network access, nothing collected.

The last line is worth keeping accurate: the app requests no permissions and has
no network code. Do not add any without changing that sentence.
