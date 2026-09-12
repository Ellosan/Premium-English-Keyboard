package com.premiumenglish.keyboard

/**
 * Reads the lexicon's plain-text tables.
 *
 * Kept apart from [Lexicon] so that the premium tables, which live in their own
 * source set, can be written in the same format without the two objects having
 * to initialise each other.
 */
internal object LexiconText {

    /** Parses "key = value" lines, ignoring blanks and # comments. */
    fun pairs(spec: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (raw in spec.trimIndent().lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val i = line.indexOf('=')
            if (i <= 0) continue
            out[line.substring(0, i).trim()] = line.substring(i + 1).trim()
        }
        return out
    }

    /** Parses a whitespace- or comma-separated word list. */
    fun words(spec: String): Set<String> =
        spec.trim().split(Regex("[\\s,]+")).filter { it.isNotEmpty() }.toHashSet()
}
