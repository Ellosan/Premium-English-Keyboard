package com.premiumenglish.keyboard

/**
 * Settings that shape a translation.
 *
 * @param tier          1 Refined, 2 Courtly, 3 Sovereign.
 * @param flourish      add ceremonial openers and closers to finished sentences.
 * @param oldeSpelling  apply faux-antique respellings (Sovereign tier only).
 */
data class PremiumOptions(
    val tier: Int = PremiumEnglish.TIER_COURTLY,
    val flourish: Boolean = false,
    val oldeSpelling: Boolean = false
)

/**
 * Translates modern English into Premium English.
 *
 * Deliberately free of Android imports so it can be unit-tested on a plain JVM.
 *
 * The text runs through a series of passes over a token list. Each pass either
 * rewrites a token in place or splices a replacement over a span of tokens,
 * and whitespace is carried through untouched so that the caller gets back
 * something with the same shape as what it handed in — which matters a great
 * deal to the keyboard, since it re-translates on every keystroke.
 */
object PremiumEnglish {

    const val TIER_REFINED = 1
    const val TIER_COURTLY = 2
    const val TIER_SOVEREIGN = 3

    private const val WORD = 0
    private const val SPACE = 1
    private const val PUNCT = 2

    /** Longest phrase in the lexicon, in words. */
    private const val MAX_PHRASE = 5

    private val TOKEN_RE = Regex("[A-Za-z]+(?:'[A-Za-z]+)*|\\s+|[^\\sA-Za-z]")

    private val VOWELS = "aeiou"

    /** Auxiliaries that already carry the person marking, e.g. "dost thou know". */
    private val MARKED_AUXILIARIES = setOf(
        "dost", "doth", "didst", "art", "wast", "wert", "hast", "hadst",
        "wilt", "shalt", "canst", "couldst", "wouldst", "shouldst", "mayst",
        "mightst", "must", "beest"
    )

    /** Multi-syllable verbs that double the final consonant anyway. */
    private val DOUBLERS = setOf(
        "begin", "forget", "admit", "permit", "prefer", "refer", "occur",
        "regret", "submit", "commit", "control", "compel", "expel", "rebel",
        "upset", "forbid", "omit", "outwit"
    )

    private class Tok(val kind: Int, var text: String, var caps: Int = 0, var locked: Boolean = false)

    /**
     * Verbs the lexicon itself introduces. "walk" becomes "perambulate", and
     * "thou perambulate" wants the same -est ending that "thou walk" would
     * have taken, so anything a known verb can turn into counts as a verb too.
     */
    private val DERIVED_VERBS: Set<String> by lazy {
        val out = HashSet<String>()
        for (tier in TIER_REFINED..TIER_SOVEREIGN) {
            for ((from, to) in Lexicon.WORDS[tier].orEmpty()) {
                if (from.substringBefore(' ') !in Lexicon.VERBS && from !in out) continue
                val head = to.substringBefore(' ')
                // "please" becomes "if you please", and "if" is emphatically not
                // a verb. Only take a head word that could be one.
                if (head in FUNCTION_WORDS) continue
                out.add(head)
            }
        }
        out
    }

    /** Words that can never head a verb phrase. */
    private val FUNCTION_WORDS: Set<String> by lazy {
        Lexicon.DETERMINERS + Lexicon.PREPOSITIONS + Lexicon.INTERPOSED_ADVERBS +
            setOf("if", "when", "while", "whilst", "that", "so", "thus", "there", "here")
    }

    private fun isVerb(word: String): Boolean =
        word in Lexicon.VERBS || word in DERIVED_VERBS || word in Lexicon.PAST_VERBS

    private val resolvedWordCache = HashMap<Int, Map<String, String>>()
    private val resolvedPhraseCache = HashMap<Int, Map<String, String>>()

    // ------------------------------------------------------------------ public API

    /**
     * Translates [input] in full.
     *
     * @param finished the text is a completed thought (the user typed `.`, `!`
     *                 or `?`), which is the only point at which ceremonial
     *                 flourishes are added — a flourish that appeared and
     *                 vanished while you were still typing would be maddening.
     */
    @JvmOverloads
    fun translate(input: String, options: PremiumOptions = PremiumOptions(), finished: Boolean = false): String {
        if (input.isBlank()) return input
        val tier = options.tier.coerceIn(TIER_REFINED, TIER_SOVEREIGN)

        val toks = tokenize(input)
        expandContractions(toks)
        applyPhrases(toks, tier)
        applyPronouns(toks, tier)
        applyWords(toks, tier)
        applyDoSupport(toks, tier)
        applyAgreement(toks, tier)
        if (finished && options.flourish) applyFlourish(toks, tier)
        if (tier >= TIER_SOVEREIGN && options.oldeSpelling) applyOldeSpelling(toks)
        applyPhonetics(toks, tier)
        return render(toks)
    }

    /**
     * Translates text that is still being typed: everything up to the last word
     * boundary is translated, and the word currently under the user's thumb is
     * left exactly as they typed it. Without this, half-finished words would be
     * translated into nonsense on every keystroke.
     */
    fun translateLive(raw: String, options: PremiumOptions): String {
        if (raw.isEmpty()) return raw
        val last = raw[raw.length - 1]
        if (!isWordChar(last)) {
            // A sentence is finished even when a space has been typed after it.
            val terminal = raw.trimEnd().lastOrNull()
            return translate(
                raw, options,
                finished = terminal == '.' || terminal == '!' || terminal == '?'
            )
        }
        var cut = raw.length
        while (cut > 0 && isWordChar(raw[cut - 1])) cut--
        val head = raw.substring(0, cut)
        val tail = raw.substring(cut)
        return (if (head.isBlank()) head else translate(head, options)) + tail
    }

    private fun isWordChar(c: Char) = c.isLetter() || c == '\''

    // ------------------------------------------------------------------ tokenising

    private fun tokenize(input: String): MutableList<Tok> {
        val normalised = input.replace('’', '\'')
        val out = ArrayList<Tok>()
        for (m in TOKEN_RE.findAll(normalised)) {
            val s = m.value
            val c = s[0]
            when {
                c.isLetter() -> out.add(Tok(WORD, s.lowercase(), capsOf(s)))
                c.isWhitespace() -> out.add(Tok(SPACE, s))
                else -> out.add(Tok(PUNCT, s))
            }
        }
        return out
    }

    private fun capsOf(s: String): Int {
        if (!s[0].isUpperCase()) return 0
        // "I" counts as title case, not shouting
        if (s.length > 1 && s == s.uppercase()) return 2
        return 1
    }

    private fun applyCaps(s: String, caps: Int): String = when (caps) {
        2 -> s.uppercase()
        1 -> if (s.isEmpty()) s else s[0].uppercase() + s.substring(1)
        else -> s
    }

    private fun render(toks: List<Tok>): String {
        val sb = StringBuilder()
        var sentenceStart = true
        for (t in toks) {
            when (t.kind) {
                SPACE -> sb.append(t.text)
                PUNCT -> {
                    sb.append(t.text)
                    if (t.text == "." || t.text == "!" || t.text == "?") sentenceStart = true
                }
                else -> {
                    var s = t.text
                    if (s == "i") s = "I"
                    val caps = if (sentenceStart && t.caps == 0) 1 else t.caps
                    sb.append(applyCaps(s, caps))
                    sentenceStart = false
                }
            }
        }
        return sb.toString()
    }

    // ------------------------------------------------------------------ splicing

    private fun wordIndices(toks: List<Tok>): List<Int> {
        val out = ArrayList<Int>()
        for (i in toks.indices) if (toks[i].kind == WORD) out.add(i)
        return out
    }

    private fun hasPunctBetween(toks: List<Tok>, from: Int, to: Int): Boolean {
        for (i in (from + 1) until to) if (toks[i].kind == PUNCT) return true
        return false
    }

    /**
     * Replaces tokens [from]..[to] with [replacement], carrying the original
     * capitalisation onto the first word of the replacement.
     *
     * @return the index just past the inserted tokens.
     */
    private fun replaceSpan(
        toks: MutableList<Tok>,
        from: Int,
        to: Int,
        replacement: String,
        lock: Boolean
    ): Int {
        val caps = toks[from].caps
        val fresh = tokenize(replacement)
        if (fresh.isEmpty()) return from
        var first = true
        for (t in fresh) {
            if (t.kind == WORD) {
                // Shouting carries across the whole replacement; a single
                // capital belongs only to the word that starts it.
                if (caps == 2) t.caps = 2
                else if (first) t.caps = caps
                first = false
            }
            t.locked = lock
        }
        for (i in to downTo from) toks.removeAt(i)
        toks.addAll(from, fresh)
        return from + fresh.size
    }

    // ------------------------------------------------------------------ pass 1: contractions

    private fun expandContractions(toks: MutableList<Tok>) {
        var i = 0
        while (i < toks.size) {
            val t = toks[i]
            if (t.kind == WORD) {
                val expansion = Lexicon.CONTRACTIONS[t.text]
                if (expansion != null) {
                    i = replaceSpan(toks, i, i, expansion, lock = false)
                    continue
                }
            }
            i++
        }
    }

    // ------------------------------------------------------------------ pass 2: phrases

    private fun phrasesFor(tier: Int): Map<String, String> = resolvedPhraseCache.getOrPut(tier) {
        val merged = LinkedHashMap<String, String>()
        for (t in 1..tier) merged.putAll(Lexicon.PHRASES[t].orEmpty())
        merged
    }

    private fun applyPhrases(toks: MutableList<Tok>, tier: Int) {
        val table = phrasesFor(tier)
        var i = 0
        while (i < toks.size) {
            if (toks[i].kind != WORD || toks[i].locked) {
                i++
                continue
            }
            // Collect the run of words reachable from here without crossing punctuation.
            val run = ArrayList<Int>()
            var j = i
            while (j < toks.size && run.size < MAX_PHRASE) {
                val t = toks[j]
                if (t.kind == WORD) {
                    if (t.locked) break
                    run.add(j)
                } else if (t.kind != SPACE) break
                j++
            }
            var matched = false
            for (n in run.size downTo 2) {
                val key = StringBuilder()
                for (k in 0 until n) {
                    if (k > 0) key.append(' ')
                    key.append(toks[run[k]].text)
                }
                val value = table[key.toString()]
                if (value != null) {
                    i = replaceSpan(toks, i, run[n - 1], value, lock = true)
                    matched = true
                    break
                }
            }
            if (!matched) i++
        }
    }

    // ------------------------------------------------------------------ pass 3: pronouns

    private fun applyPronouns(toks: MutableList<Tok>, tier: Int) {
        if (tier < TIER_COURTLY) return
        val idx = wordIndices(toks)
        for (p in idx.indices) {
            val t = toks[idx[p]]
            if (t.locked) continue
            val next = if (p + 1 < idx.size) toks[idx[p + 1]].text else null
            when (t.text) {
                "you" -> t.text = if (isObjectPosition(toks, idx, p, next)) "thee" else "thou"
                // thy/thine is decided later, once we know what word follows.
                "your" -> t.text = "thy"
                "yours" -> t.text = "thine"
                "yourself" -> t.text = "thyself"
            }
        }
    }

    /**
     * Decides whether "you" is a subject (thou) or an object (thee) from the
     * words on either side of it.
     */
    private fun isObjectPosition(toks: List<Tok>, idx: List<Int>, p: Int, next: String?): Boolean {
        // A finite auxiliary after it means "you" is the subject of a clause:
        // "I can't believe you did that", "I think you should go".
        if (next != null && next in Lexicon.AUXILIARIES) return false

        val prev = governingWord(toks, idx, p) ?: return false
        if (prev in Lexicon.PREPOSITIONS) return true
        // "do you know" is a question, and "you" is still its subject.
        if (prev in Lexicon.AUXILIARIES) return false
        if (isVerb(prev)) return true
        // "and you know the rest" — a bare verb after it still makes it a subject.
        return false
    }

    /**
     * The word that governs the pronoun at [p], looking past a coordination:
     * in "give it to me and you", the pronoun is governed by "to", not "and".
     */
    private fun governingWord(toks: List<Tok>, idx: List<Int>, p: Int): String? {
        var q = p - 1
        while (q >= 0 && toks[idx[q]].text in CONJUNCTIONS) {
            // Step over the conjunction and the element it coordinates with.
            q -= 2
        }
        return if (q >= 0) toks[idx[q]].text else null
    }

    private val CONJUNCTIONS = setOf("and", "or", "nor")

    private fun startsWithVowelSound(next: String?): Boolean {
        if (next.isNullOrEmpty()) return false
        val c = next[0]
        if (c in VOWELS) return true
        // "thine hour", "mine honour" — h is silent in the words that matter here
        return next == "hour" || next == "honour" || next == "honor" || next == "heir"
    }

    // ------------------------------------------------------------------ pass 4: words

    /**
     * Merges the word tables up to [tier] and follows each chain to its end, so
     * that "house" resolves straight to "abode" rather than stopping at the
     * tier-1 "residence". Chains are followed with a visited set, so a pair of
     * tables that point at each other cannot loop.
     */
    private fun wordsFor(tier: Int): Map<String, String> = resolvedWordCache.getOrPut(tier) {
        val merged = LinkedHashMap<String, String>()
        for (t in 1..tier) merged.putAll(Lexicon.WORDS[t].orEmpty())
        val out = HashMap<String, String>()
        for (key in merged.keys) {
            var current = key
            val seen = HashSet<String>()
            seen.add(key)
            var depth = 0
            while (depth < 6) {
                val next = merged[current] ?: break
                if (!seen.add(next)) break
                current = next
                depth++
            }
            if (current != key) out[key] = current
        }
        out
    }

    private fun applyWords(toks: MutableList<Tok>, tier: Int) {
        val table = wordsFor(tier)
        var i = 0
        while (i < toks.size) {
            val t = toks[i]
            if (t.kind == WORD && !t.locked) {
                val value = table[t.text]
                if (value != null) {
                    i = replaceSpan(toks, i, i, value, lock = false)
                    continue
                }
            }
            i++
        }
    }

    // ------------------------------------------------------------------ pass 5: do-support

    /**
     * Sovereign tier drops the propped-up auxiliary, the way English did before
     * it acquired the habit: "I do not know" becomes "I know not", and
     * "do you know" becomes "knowest thou".
     */
    private fun applyDoSupport(toks: MutableList<Tok>, tier: Int) {
        if (tier < TIER_SOVEREIGN) return
        var idx = wordIndices(toks)
        var p = 0
        while (p < idx.size) {
            val aux = toks[idx[p]].text
            if ((aux == "do" || aux == "does") && p + 2 < idx.size) {
                val bIdx = idx[p + 1]
                val cIdx = idx[p + 2]
                val b = toks[bIdx].text
                val c = toks[cIdx].text
                val subject = if (p > 0) toks[idx[p - 1]].text else null
                val clean = !hasPunctBetween(toks, idx[p], cIdx)

                val negation = b == "not" && isVerb(c)
                val question = startsSentence(toks, idx[p]) && b in SUBJECT_PRONOUNS && isVerb(c)

                if (clean && (negation || question)) {
                    val governing = if (question) b else subject
                    val conjugated = when {
                        governing == "thou" -> secondPerson(c) ?: c
                        aux == "does" || governing in Lexicon.THIRD_SINGULAR_SUBJECTS -> thirdPerson(c)
                        else -> c
                    }
                    toks[idx[p]].text = conjugated
                    // Drop the displaced verb and the space in front of it.
                    toks.removeAt(cIdx)
                    if (cIdx - 1 > bIdx && toks[cIdx - 1].kind == SPACE) toks.removeAt(cIdx - 1)
                    idx = wordIndices(toks)
                }
            }
            p++
        }
    }

    private val SUBJECT_PRONOUNS = setOf("thou", "ye", "i", "we", "they", "he", "she", "it", "you")

    /** True when no word precedes [at] in its sentence. */
    private fun startsSentence(toks: List<Tok>, at: Int): Boolean {
        for (i in at - 1 downTo 0) {
            val t = toks[i]
            if (t.kind == WORD) return false
            if (t.kind == PUNCT && (t.text == "." || t.text == "!" || t.text == "?")) return true
        }
        return true
    }

    // ------------------------------------------------------------------ pass 6: agreement

    private fun applyAgreement(toks: MutableList<Tok>, tier: Int) {
        if (tier < TIER_COURTLY) return
        val idx = wordIndices(toks)

        // "thou" takes the -est ending on the verb it governs.
        for (p in idx.indices) {
            if (toks[idx[p]].text != "thou") continue
            // In an inversion the auxiliary carries the marking, not the verb:
            // "can you help" becomes "canst thou aid", not "can thou aidest".
            val beforeIdx = if (p > 0) idx[p - 1] else -1
            if (beforeIdx >= 0 && !hasPunctBetween(toks, beforeIdx, idx[p])) {
                val before = toks[beforeIdx].text
                if (before in MARKED_AUXILIARIES) continue
                if (before in Lexicon.AUXILIARIES) {
                    Lexicon.IRREGULAR_2SG[before]?.let { toks[beforeIdx].text = it }
                    continue
                }
            }

            var q = p + 1
            while (q < idx.size && toks[idx[q]].text in Lexicon.INTERPOSED_ADVERBS) q++
            if (q >= idx.size) continue
            if (hasPunctBetween(toks, idx[p], idx[q])) continue

            val target = toks[idx[q]]
            val conjugated = secondPerson(target.text)
            if (conjugated != null) target.text = conjugated
        }

        // Forms that are third-person singular wherever they appear.
        for (i in idx) {
            val t = toks[i]
            val irregular = Lexicon.IRREGULAR_3SG[t.text]
            if (irregular != null) t.text = irregular
        }

        // Regular third-person singular: "he runs" becomes "he runneth".
        for (p in idx.indices) {
            if (p == 0) continue
            val t = toks[idx[p]]
            val w = t.text
            if (w.length < 3 || !w.endsWith("s")) continue
            if (w in Lexicon.NEVER_ETH) continue

            val prevTok = toks[idx[p - 1]]
            val prev = prevTok.text
            if (prev in Lexicon.DETERMINERS || prev in Lexicon.PLURAL_SUBJECTS) continue
            if (hasPunctBetween(toks, idx[p - 1], idx[p])) continue

            val base = baseOfThirdPerson(w) ?: continue
            val plausibleSubject = prev in Lexicon.THIRD_SINGULAR_SUBJECTS ||
                prevTok.caps == 1 ||
                (!isVerb(prev) && prev !in Lexicon.PREPOSITIONS &&
                    prev !in Lexicon.AUXILIARIES && prev !in Lexicon.INTERPOSED_ADVERBS)
            if (!plausibleSubject) continue

            t.text = thirdPerson(base)
        }
    }

    /**
     * True when the word in front of a verb is plainly a third-person singular
     * subject: a pronoun like "he", a proper noun, or a determiner and a noun
     * ("my mother works", "the dog runs").
     */
    private fun subjectIsThirdSingular(toks: List<Tok>, idx: List<Int>, p: Int): Boolean {
        val prevTok = toks[idx[p - 1]]
        if (prevTok.text in Lexicon.THIRD_SINGULAR_SUBJECTS) return true
        if (p - 1 > 0 && prevTok.caps == 1) return true
        if (p >= 2 && toks[idx[p - 2]].text in Lexicon.DETERMINERS && !isVerb(prevTok.text)) return true
        return false
    }

    /** Finds the base verb behind an -s form, or null if there isn't a known one. */
    private fun baseOfThirdPerson(w: String): String? {
        val candidates = ArrayList<String>(3)
        if (w.endsWith("ies") && w.length > 4) candidates.add(w.dropLast(3) + "y")
        if (w.endsWith("es") && w.length > 3) candidates.add(w.dropLast(2))
        candidates.add(w.dropLast(1))
        return candidates.firstOrNull { isVerb(it) }
    }

    /** The form a verb takes after "thou". */
    private fun secondPerson(word: String): String? {
        Lexicon.IRREGULAR_2SG[word]?.let { return it }
        if (word.endsWith("ed") && word.length > 3 &&
            (isVerb(word.dropLast(2)) || isVerb(word.dropLast(1)))
        ) return word + "st"
        if (isVerb(word)) return inflect(word, "st", "est")
        if (word.endsWith("s")) {
            val base = baseOfThirdPerson(word) ?: return null
            return secondPerson(base)
        }
        return null
    }

    /** The form a verb takes after "he", "she" or "it". */
    private fun thirdPerson(base: String): String {
        Lexicon.IRREGULAR_3SG[base]?.let { return it }
        return inflect(base, "th", "eth")
    }

    /**
     * Attaches an archaic ending, applying the ordinary English spelling rules:
     * a final "e" is kept ("loveth"), "y" after a consonant becomes "i"
     * ("trieth"), and a short verb doubles its last consonant ("runneth").
     */
    private fun inflect(base: String, afterE: String, otherwise: String): String {
        if (base.isEmpty()) return base
        val last = base[base.length - 1]
        if (last == 'e') return base + afterE
        if (last == 'y' && base.length > 1 && base[base.length - 2] !in VOWELS) {
            return base.dropLast(1) + "i" + otherwise
        }
        if (base.endsWith("s") || base.endsWith("sh") || base.endsWith("ch") ||
            base.endsWith("x") || base.endsWith("z")
        ) return base + otherwise
        if (shouldDouble(base)) return base + last + otherwise
        return base + otherwise
    }

    private fun shouldDouble(base: String): Boolean {
        if (base in DOUBLERS) return true
        val n = base.length
        if (n < 3) return false
        val last = base[n - 1]
        val vowel = base[n - 2]
        val before = base[n - 3]
        if (last in VOWELS || last == 'w' || last == 'x' || last == 'y') return false
        if (vowel !in VOWELS) return false
        if (before in VOWELS) return false
        return vowelGroups(base) == 1
    }

    private fun vowelGroups(s: String): Int {
        var groups = 0
        var inGroup = false
        for (c in s) {
            val isVowel = c in VOWELS
            if (isVowel && !inGroup) groups++
            inGroup = isVowel
        }
        return groups
    }

    // ------------------------------------------------------------------ pass 7: ceremony

    private fun applyFlourish(toks: MutableList<Tok>, tier: Int) {
        val openers = Lexicon.OPENERS[tier].orEmpty()
        val closers = Lexicon.CLOSERS[tier].orEmpty()
        if (openers.isEmpty() && closers.isEmpty()) return

        val openerChance = when (tier) {
            TIER_REFINED -> 25
            TIER_COURTLY -> 50
            else -> 100
        }
        val closerChance = when (tier) {
            TIER_REFINED -> 0
            TIER_COURTLY -> 25
            else -> 50
        }

        // Walk the sentences back to front so that inserting never disturbs an
        // index we have not visited yet.
        for (span in sentenceSpans(toks).asReversed()) {
            val (start, end) = span
            val words = (start..end).filter { toks[it].kind == WORD }
            if (words.isEmpty()) continue
            val seed = words.joinToString(" ") { toks[it].text }.hashCode()

            if (closers.isNotEmpty() && pick(seed * 31, 100) < closerChance) {
                var at = end
                while (at >= start && toks[at].kind != WORD) at--
                if (at >= start) {
                    val closer = closers[pick(seed * 17, closers.size)]
                    toks.addAll(at + 1, tokenize(closer))
                }
            }
            if (openers.isNotEmpty() && pick(seed, 100) < openerChance) {
                val opener = openers[pick(seed * 7, openers.size)]
                val fresh = tokenize(opener)
                fresh.add(Tok(SPACE, " "))
                // The opener now leads the sentence, so the old first word
                // gives up its capital letter unless it earned it.
                val firstWord = (start..end).firstOrNull { toks[it].kind == WORD }
                if (firstWord != null && toks[firstWord].caps == 1 && toks[firstWord].text != "i") {
                    toks[firstWord].caps = 0
                }
                toks.addAll(start, fresh)
            }
        }
    }

    /** Inclusive token ranges, one per sentence. */
    private fun sentenceSpans(toks: List<Tok>): List<Pair<Int, Int>> {
        val spans = ArrayList<Pair<Int, Int>>()
        var start = 0
        for (i in toks.indices) {
            val t = toks[i]
            if (t.kind == PUNCT && (t.text == "." || t.text == "!" || t.text == "?")) {
                spans.add(start to i)
                start = i + 1
            }
        }
        if (start < toks.size) spans.add(start to toks.size - 1)
        return spans
    }

    /** A stable choice: the same sentence always draws the same flourish. */
    private fun pick(seed: Int, bound: Int): Int {
        var h = seed
        h = h xor (h ushr 16)
        h *= 0x7feb352d.toInt()
        h = h xor (h ushr 15)
        h *= 0x846ca68bL.toInt()
        h = h xor (h ushr 16)
        return ((h.toLong() and 0xffffffffL) % bound).toInt()
    }

    // ------------------------------------------------------------------ pass 8: phonetics

    /**
     * Substitution changes the sound a word begins with, and three words in
     * front of it care: "a beer" becomes "an ale", "your house" becomes
     * "thine abode". None of that can be settled until the substitutions are
     * done, so it is settled here, last.
     */
    private fun applyPhonetics(toks: MutableList<Tok>, tier: Int) {
        val idx = wordIndices(toks)
        for (p in 0 until idx.size - 1) {
            val t = toks[idx[p]]
            if (hasPunctBetween(toks, idx[p], idx[p + 1])) continue
            val vowel = startsWithVowelSound(toks[idx[p + 1]].text)
            when (t.text) {
                "a", "an" -> t.text = if (vowel) "an" else "a"
                "thy", "thine" -> if (tier >= TIER_COURTLY) t.text = if (vowel) "thine" else "thy"
                "my", "mine" -> if (tier >= TIER_COURTLY) t.text = if (vowel) "mine" else "my"
            }
        }
    }

    // ------------------------------------------------------------------ pass 9: spelling

    private fun applyOldeSpelling(toks: MutableList<Tok>) {
        val idx = wordIndices(toks)
        for (p in idx.indices) {
            val t = toks[idx[p]]
            // "the olde shoppe" earns its "ye"
            if (t.text == "the" && p + 1 < idx.size && toks[idx[p + 1]].text in Lexicon.OLDE_SPELLINGS) {
                t.text = "ye"
                continue
            }
            Lexicon.OLDE_SPELLINGS[t.text]?.let { t.text = it }
        }
    }
}
