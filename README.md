# Premium English Keyboard

An Android keyboard that quietly upgrades your English as you type it.

You type `hello, how are you?` and the field fills in with
**`Hail, how dost thou fare?`** — no button to press, no second app, no
copy-and-paste. It is an ordinary soft keyboard everywhere it appears, and the
translation happens between your thumb and the text field.

```
┌──────────────────────────────────────────────────┐
│ ◆   Methinks thou art exceeding fair   Courtly ⚙ │
├──────────────────────────────────────────────────┤
│  q   w   e   r   t   y   u   i   o   p           │
│    a   s   d   f   g   h   j   k   l             │
│  ⇧    z   x   c   v   b   n   m      ⌫           │
│ ?123  ,          space          .        ↵       │
└──────────────────────────────────────────────────┘
```

## Service tiers

The keyboard is sold, as all premium things are, in tiers. Tap the tier chip on
the keyboard to move between them.

| Modern | Refined | Courtly | Sovereign |
| --- | --- | --- | --- |
| hey, what's up? | I say, how goes it? | Hark, how fareth it with thee? | Hark, what tidings, gentle soul? |
| i think you look great today | Methinks you look splendid today | Methinks thou lookest most excellent this day | Methinks thou lookest passing great this day |
| can you help me? i don't know what to do | Can you assist me? I do not know what to do | Canst thou aid me? I do not know what to do | Canst thou aid me? I know not what to do |
| she has a big house and two dogs | She has a considerable residence and two dogs | She hath a most excellent abode and two dogs | She hath a passing great abode and two dogs |
| thanks a lot, see you later | I thank you most heartily, until we meet again | I thank you most heartily, until we meet anon | I thank you most heartily, until the wheel of fortune turneth us together again |

**Refined** lifts the register and leaves your pronouns in peace. **Courtly**
brings in *thou* and *thee*, the *-est* and *-eth* endings, and the archaic
wardrobe. **Sovereign** adds everything above plus dropped auxiliaries and a
good deal of pomp.

Two extras are off by default, in Settings:

- **Ceremonial flourishes** decorate a sentence once you finish it:
  `i don't know what you want.` → *By my troth, I know not what thou covetest, upon mine honour.*
- **Ye olde spellings** respell at the Sovereign tier: `the light` → *ye lyght*.

## How the translation works

The keyboard keeps everything you have typed since the last full stop in a
buffer, in plain modern English. After each keystroke it translates that buffer
and writes the result back over itself. The field shows Premium English; the
buffer remembers what you actually typed.

Keeping the original around is what makes the rest work. Backspace rewinds your
words rather than the ornate ones on screen, and translations that depend on a
later word can still happen — `i` is nothing on its own, but `i think` is
*methinks*. The word currently under your thumb is never translated until you
finish it, so nothing rearranges itself mid-word.

The translation itself is a series of passes over a token list
([`PremiumEnglish.kt`](app/src/main/java/com/premiumenglish/keyboard/PremiumEnglish.kt)):

1. **Contractions** expand — `don't` → `do not`.
2. **Phrases** match longest-first, and win over the words inside them, so
   `thank you very much` does not become four separate substitutions.
3. **Pronouns** — `you` becomes *thou* as a subject and *thee* as an object,
   decided from the word in front of it.
4. **Words** substitute, following the chain through the tiers: `house` →
   `residence` → `abode`.
5. **Do-support** drops, at the Sovereign tier: `I do not know` → *I know not*,
   `do you know` → *knowest thou*.
6. **Agreement** adds the archaic endings: `thou` takes *-est* on its verb,
   `he`/`she`/`it` take *-eth*, with the ordinary English spelling rules
   (`try` → *triest*, `run` → *runneth*, `teach` → *teacheth*). In an
   inversion the auxiliary carries the marking instead, so `can you help` is
   *canst thou aid*, not *can thou aidest*.
7. **Flourishes**, on finished sentences only, chosen by a hash of the sentence
   so the same words always draw the same ornament.
8. **Phonetics** last, because substitution changes the sound a word starts
   with: `a beer` → *an ale*, `your house` → *thine abode*.

The vocabulary lives in
[`Lexicon.kt`](app/src/main/java/com/premiumenglish/keyboard/Lexicon.kt) as
plain text tables, one per tier, and a translation at tier N applies every table
up to N.

## Building

Requires JDK 17+ and an Android SDK with platform 34.

```sh
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # 22 tests over the translation engine
./gradlew installDebug           # to a connected device
```

Then, on the device:

1. Open **Premium English** from the app drawer.
2. **Enable the keyboard** — takes you to Android's input method settings.
3. **Select it as your input method** — opens the keyboard picker.

The settings screen also has a *Try it* box, so you can see what the tiers do
before handing your typing over to any of them.

## Where it does not interfere

Translation switches itself off in password, email, URL and filter fields. It
can also be turned off entirely with the ◆ key on the keyboard's status bar,
which leaves you with an ordinary, regrettably modern keyboard.

## Layout

```
app/src/main/java/com/premiumenglish/keyboard/
    PremiumEnglish.kt      the translation engine (no Android imports)
    Lexicon.kt             vocabulary and grammar tables, by tier
    PremiumEnglishIME.kt   the input method service and its buffer
    KeyboardPanel.kt       the keys and the status bar
    SettingsActivity.kt    setup, preferences, and a live preview
    Prefs.kt               stored settings
app/src/test/java/...      unit tests for the engine
```

The engine is deliberately free of Android imports, so it runs and is tested on
a plain JVM.

## Honest limitations

It is a lexicon and a set of spelling rules, not a parser, so it guesses at
grammar from the words either side. `you` defaults to the subject *thou* when
the surrounding words do not settle it; the *-eth* ending is withheld unless the
preceding word looks like a singular subject; and a word the lexicon has never
heard of is passed through untouched. It aims to be funny and mostly right
rather than philologically defensible — real Old English is a different language
altogether, and this is closer to Early Modern English with delusions of
grandeur.

## Licence

MIT — see [LICENSE](LICENSE).
