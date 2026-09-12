# Editions

Premium English ships in two editions, built from the same repository.

|  | Free | Pro |
| --- | --- | --- |
| Refined tier | ✓ | ✓ |
| Courtly tier — *thou*, *thee*, *-est*, *-eth* | — | ✓ |
| Sovereign tier — dropped auxiliaries, pomp | — | ✓ |
| Lexicon | ~170 entries | ~730 entries |
| Ceremonial flourishes | Refined only | all tiers |
| Ye olde spellings | — | ✓ |
| The keyboard itself — ✦ key, sizes, number row, long-press, emoji, key preview, auto-capitals, double-space full stop | ✓ | ✓ |
| Application id | `com.premiumenglish.keyboard` | `com.premiumenglish.keyboard.pro` |

The two application ids differ, so both can be installed side by side — useful
when you want to check what the free edition actually does.

## How the split works

The free edition is not the Pro edition with its features switched off. The
Courtly and Sovereign vocabulary is a separate Gradle source set, `app/src/pro`,
which is **not in this repository**. The free build has no premium tables in it
at all, and its `Edition.MAX_TIER` is Refined:

```
app/src/main/…         the engine, the keyboard, the Refined tier  (public)
app/src/free/…         Edition.kt — no premium tables, MAX_TIER = 1 (public)
app/src/pro/…          Edition.kt + PremiumTables.kt                (private)
app/src/test/…         tests that run in both editions              (public)
app/src/testFree/…     what the free edition may not do             (public)
app/src/testPro/…      the Courtly and Sovereign tests              (private)
```

`PremiumEnglish.translate` clamps the requested tier to `Edition.MAX_TIER`, so
there is no flag to flip: asking the free build for Sovereign gets you Refined,
because there is nothing else in the build to give.

You can check this on a built APK:

```sh
unzip -p app/build/outputs/apk/free/debug/app-free-debug.apk classes.dex \
  | grep -a -c prithee     # 0
unzip -p app/build/outputs/apk/pro/debug/app-pro-debug.apk classes.dex \
  | grep -a -c prithee     # more than 0
```

## Building

The free edition builds from a clean clone, with no extra steps:

```sh
./gradlew assembleFreeDebug
./gradlew testFreeDebugUnitTest
```

The Pro edition needs the private sources. Unpack them into the root of the
repository so that `app/src/pro/` and `app/src/testPro/` exist, then:

```sh
./gradlew assembleProDebug
./gradlew testProDebugUnitTest
```

Both source sets are listed in `.gitignore`, so they cannot be committed here by
accident. Keep your copy somewhere safe — it is the part that is sold, and it is
not backed up by this repository.

To build everything at once: `./gradlew assemble` and `./gradlew test`.

## Adding to the premium vocabulary

`app/src/pro/java/com/premiumenglish/keyboard/PremiumTables.kt` holds the
Courtly and Sovereign tables in the same `key = value` text format as the public
`Lexicon.kt`. A translation at tier N applies every table up to N, so a Courtly
entry can refine a Refined one (`house` → `residence` → `abode`).

Two rules worth keeping to, both learned the hard way:

- Leave out words whose sense depends on their grammar — `will`, `may`, `can`,
  `just`, `like`, `so`, `well`, `mean`, `back`, `right`. They mean two things at
  once and replacing them turns sentences to nonsense.
- The engine looks words up by their stem, so one entry covers the plural, the
  past tense and the -ing form. Add `dog`, not `dogs`.
