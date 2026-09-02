# Premium English Keyboard

An Android keyboard that quietly upgrades your English as you type it.

You type `hello, how are you?` and the field fills in with
**`Hail, how dost thou fare?`** — no button to press, no second app, no
copy-and-paste. It is an ordinary soft keyboard everywhere it appears, and the
translation happens between your thumb and the text field.

```
┌────────────────────────────────────────────────────────┐
│ ◆  “i think you are nice”        ↺  Courtly  🌐  ⚙     │
├────────────────────────────────────────────────────────┤
│  q¹  w²  e³  r⁴  t⁵  y⁶  u⁷  i⁸  o⁹  p⁰                │
│    a@  s#  d$  f_  g&  h-  j+  k(  l)                  │
│  ⇧    z*  x"  c'  v:  b;  n!  m?      ⌫                │
│ ?123  ☺   ,        space        .        ↵             │
└────────────────────────────────────────────────────────┘
```

The status bar shows what you actually typed, so you can see both halves at
once, and **↺** puts your own words back if the ceremony is unwelcome.

## Service tiers

The keyboard is sold, as all premium things are, in tiers. Tap the tier chip to
move between them.

| Modern | Refined | Courtly | Sovereign |
| --- | --- | --- | --- |
| hey, what's up? | I say, how goes it? | Hark, how fareth it with thee? | Hark, what tidings, gentle soul? |
| i think you look great today | I think you look splendid today | Methinks thou lookest most excellent this day | Methinks thou lookest passing great this day |
| can you help me? i don't know what to do | Can you assist me? I do not know what to do | Canst thou aid me? I do not know what to do | Canst thou aid me? I know not what to do |
| she has a big house and two dogs | She has a considerable residence and two dogs | She hath a most excellent abode and two dogs | She hath a passing great abode and two dogs |
| my brother works at the bar and he loves it | My brother works at the bar and he loves it | My brother worketh at the alehouse and he loveth it | My brother worketh at the alehouse and he loveth it |

**Refined** is a lift in register and nothing more — no *thou*, no *-eth*, no
costume. **Courtly** brings in *thou* and *thee*, the archaic endings, and the
full wardrobe. **Sovereign** adds dropped auxiliaries and a good deal of pomp.

Two extras are off by default, in Settings:

- **Ceremonial flourishes** decorate a sentence once you finish it:
  `i don't know what you want.` → *By my troth, I know not what thou covetest, upon mine honour.*
- **Ye olde spellings** respell at the Sovereign tier: `the old book` → *ye auncient tome*.

## Living with it

It is meant to be usable as your only keyboard, so it does the things a
keyboard is expected to do:

- **Size** — five steps from Compact to Huge, on a slider, with a working
  keyboard underneath it in Settings so you can judge the size by typing on it.
- **Hold a letter** for the digit or symbol printed on it (`q`→`1`, `a`→`@`),
  or turn on a permanent **number row**.
- **A bubble above the key** you just pressed, so your thumb does not hide it.
- **Sentences capitalise themselves**, and **two spaces make a full stop**.
- **Emoji**, symbols across two pages, and **🌐** to hop back to your usual
  keyboard.
- **↺** restores exactly what you typed, and **◆** suspends translation
  altogether, leaving an ordinary — regrettably modern — keyboard.

Each of those can be turned off in Settings.

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
2. **Phrases** match longest-first and win over the words inside them, so
   `thank you very much` is not four separate substitutions.
3. **Pronouns** — `you` becomes *thou* as a subject and *thee* as an object,
   read from the words on either side of it. A finite auxiliary after it means
   it heads a clause (`i can't believe you did that` → *thou didst*), and a
   coordination is looked through, so `give it to me and you` finds its way to
   *and thee*.
4. **Words** substitute, following the chain through the tiers: `house` →
   `residence` → `abode`.
5. **Do-support** drops at the Sovereign tier: `I do not know` → *I know not*,
   `do you know` → *knowest thou*.
6. **Agreement** adds the archaic endings with the ordinary English spelling
   rules (`try` → *triest*, `run` → *runneth*, `teach` → *teacheth*). `-eth`
   needs a subject it believes in — a pronoun, a proper noun, or a determiner
   and a noun (`my mother works` → *worketh*) — which keeps it away from plural
   nouns that merely end in *s*. In an inversion the auxiliary carries the
   marking instead: `can you help` is *canst thou aid*, not *can thou aidest*.
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
./gradlew testDebugUnitTest      # 54 tests
./gradlew installDebug           # to a connected device
```

Then, on the device:

1. Open **Premium English** from the app drawer.
2. **Enable the keyboard** — takes you to Android's input method settings.
3. **Select it as your input method** — opens the keyboard picker.

## Where it does not interfere

Translation switches itself off in password, email, URL and filter fields, and
the ◆ key suspends it anywhere else.

## Layout

```
app/src/main/java/com/premiumenglish/keyboard/
    PremiumEnglish.kt      the translation engine (no Android imports)
    Lexicon.kt             vocabulary and grammar tables, by tier
    SegmentBuffer.kt       the typing loop: what is typed, what is shown
    PremiumEnglishIME.kt   the input method service
    KeyboardPanel.kt       the keys, the status bar, the key preview
    SettingsActivity.kt    setup, preferences, and a live keyboard
    Prefs.kt               stored settings
app/src/test/java/...      54 unit tests
```

The engine and the typing loop have no Android imports and run on a plain JVM.
The keyboard view and the settings screen are covered with Robolectric, which
is how the tests can press keys and read what comes out.

## Honest limitations

It is a lexicon and a set of spelling rules, not a parser, so it infers grammar
from neighbouring words. Where the words on both sides are ambiguous it guesses,
and a word the lexicon has never heard of is passed through untouched. Some
substitutions are right for one sense of a word and wrong for another — a shop
that *opens* and a jar that *opens* are the same word to it. It aims to be funny
and mostly right rather than philologically defensible: real Old English is a
different language altogether, and this is closer to Early Modern English with
delusions of grandeur.

## Licence

MIT — see [LICENSE](LICENSE).
