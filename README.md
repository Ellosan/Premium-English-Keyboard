# Premium English Keyboard

An Android keyboard that quietly upgrades your English as you type it.

You type your message the ordinary way, tap **✦**, and
`hello, how are you?` becomes **`Hail, how dost thou fare?`** — in place, in
whatever app you are in, with no second app and no copy-and-paste. While you
type, the status bar shows what the key is about to give you.

```
┌────────────────────────────────────────────────────────┐
│ ◆  “Methinks thou art pleasant”  ↺  Courtly  🌐  ⚙     │
├────────────────────────────────────────────────────────┤
│  q¹  w²  e³  r⁴  t⁵  y⁶  u⁷  i⁸  o⁹  p⁰                │
│    a@  s#  d$  f_  g&  h-  j+  k(  l)                  │
│  ⇧    z*  x"  c'  v:  b;  n!  m?      ⌫                │
│ ?123 ☺  ,      space      .    ✦     ↵                 │
└────────────────────────────────────────────────────────┘
```

The bar always shows the half the field is not showing: the translation while
you are still typing plainly, and your own plain words afterwards. **↺** takes
a translation back, and takes it back again if you press **✦** by mistake.

## Editions

The keyboard is sold, as all premium things are, in tiers — and the upper two
are the paid ones.

- **Free** — the Refined tier: your writing, lifted a register. The whole
  keyboard, and this whole repository.
- **Pro** — adds Courtly and Sovereign: *thou* and *thee*, the *-est* and *-eth*
  endings, several hundred more words of vocabulary, ceremonial flourishes and
  ye olde spellings. On [itch.io](https://ellosan.itch.io/premium-english-keyboard).

The free edition is not the Pro edition with its features switched off: the
premium vocabulary is a separate source set that is not published here, so the
free build has none of it in it. See [docs/EDITIONS.md](docs/EDITIONS.md) for
how that is arranged and how to build each one, and
[docs/RELEASING.md](docs/RELEASING.md) for putting them on itch.io.

## Service tiers

Tap the tier chip to move between the tiers your edition has.

| What you type | Refined | Courtly | Sovereign |
| --- | --- | --- | --- |
| hey, what's up? | I say, how goes it? | Hark, how fareth it with thee? | Hark, what tidings, gentle soul? |
| my brother works at the bar and he loves it | My brother labours at the bar and he adores it | My brother toileth at the alehouse and he cherisheth it | My brother toileth at the alehouse and he cherisheth it |
| the soldiers walked to the old castle | The soldiers walked to the old castle | The men-at-arms perambulated to the ancient castle | The men-at-arms perambulated to the ancient castle |
| she has a big house and two dogs | She has a considerable residence and two dogs | She hath a most excellent abode and two hounds | She hath a passing great abode and two faithful hounds |
| why are you so angry? i bought you a gift | Why are you so displeased? I procured you a gift | Wherefore art thou so wroth? I procured thee a boon | Wherefore art thou so wroth? I procured thee a boon |

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

- **✦ translates** what you have typed since you last pressed it, so you can
  write and edit in plain English and elevate it when the message is finished.
  Prefer the older behaviour, where the words rearrange themselves under your
  thumb as you type? Turn on *Translate as I type* in Settings.
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

The keyboard keeps what you have typed in a buffer of plain modern English, and
knows which characters in the field are its own. Pressing ✦ swaps exactly those
characters for the translation — so you can type a whole message, edit it, and
elevate it in one go. Press it twice and nothing happens the second time;
translated text is never put through again.

Keeping the typed original is what makes the rest work: ↺ can put it back,
backspace rewinds your own words, and a translation can depend on a later word
— `i` is nothing on its own, but `i think` is *methinks*.

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
plain text tables — the Refined tier here, the Courtly and Sovereign tiers in
the Pro source set, around 730 entries between them. A translation at tier N
applies every table up to N. The engine looks words up by their stem and puts
the ending back on the replacement, so one entry for `dog` also covers `dogs`,
and one for `walk` covers `walked` and `walking`; irregular pasts are listed, so
`bought` reaches `buy` and comes back as `procured` rather than `buyed`.

Words whose meaning depends on their grammar are deliberately left out — `will`,
`may`, `can`, `just`, `like`, `so`, `well`, `mean`, `back` and `right` each mean
two things at once, and replacing them turns sentences to nonsense. Plenty of
other words are missing because they need no help: `time`, `hand`, `water`,
`night`, `heart` and `stand` were Shakespeare's words already.

## Building

Requires JDK 17+ and an Android SDK with platform 34.

```sh
./gradlew assembleFreeDebug      # app/build/outputs/apk/free/debug/
./gradlew testFreeDebugUnitTest  # 49 tests
./gradlew installFreeDebug       # to a connected device
```

The Pro edition needs the private sources in `app/src/pro` (see
[docs/EDITIONS.md](docs/EDITIONS.md)); with them in place, `assembleProDebug`
and `testProDebugUnitTest` build and test 65.

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
    Lexicon.kt             the Refined vocabulary, and the grammar tables
    LexiconText.kt         reads the plain-text tables
    SegmentBuffer.kt       the typing loop: what is typed, what ✦ replaces
    PremiumEnglishIME.kt   the input method service
    KeyboardPanel.kt       the keys, the status bar, the key preview
    SettingsActivity.kt    setup, preferences, and a live keyboard
    Prefs.kt               stored settings
app/src/free/java/...      Edition.kt — what the free build can do
app/src/test/java/...      tests that run in both editions
app/src/testFree/java/...  what the free edition may not do
```

`app/src/pro` and `app/src/testPro` hold the Pro edition and are not in this
repository.

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
