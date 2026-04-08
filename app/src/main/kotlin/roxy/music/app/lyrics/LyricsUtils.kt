package roxy.music.app.lyrics

import android.text.format.DateUtils
import com.atilika.kuromoji.ipadic.Tokenizer
import com.anyascii.AnyAscii
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import roxy.music.app.betterlyrics.TTMLParser
import java.lang.Character.UnicodeScript

data class LyricsRomanizationPreferences(
    val romanizeJapanese: Boolean,
    val romanizeKorean: Boolean,
    val romanizeChinese: Boolean,
    val romanizeHindi: Boolean,
    val romanizeOther: Boolean,
) {
    val isEnabled: Boolean
        get() = romanizeJapanese || romanizeKorean || romanizeChinese || romanizeHindi || romanizeOther
}

@Suppress("RegExpRedundantEscape")
object LyricsUtils {
    val LINE_REGEX = "((\\[\\d\\d:\\d\\d\\.\\d{2,3}\\] ?)+)(.+)".toRegex()
    val TIME_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]".toRegex()
    private val WHITESPACE_REGEX = "\\s+".toRegex()
    private val OTHER_ROMANIZATION_EXCLUDED_SCRIPTS = setOf(
        UnicodeScript.LATIN,
        UnicodeScript.COMMON,
        UnicodeScript.INHERITED,
        UnicodeScript.HAN,
        UnicodeScript.HIRAGANA,
        UnicodeScript.KATAKANA,
        UnicodeScript.HANGUL,
        UnicodeScript.DEVANAGARI,
    )

    private val KANA_ROMAJI_MAP: Map<String, String> = mapOf(
        // Digraphs (YÅon - combinations like kya, sho)
        "ã‚­ãƒ£" to "kya", "ã‚­ãƒ¥" to "kyu", "ã‚­ãƒ§" to "kyo",
        "ã‚·ãƒ£" to "sha", "ã‚·ãƒ¥" to "shu", "ã‚·ãƒ§" to "sho",
        "ãƒãƒ£" to "cha", "ãƒãƒ¥" to "chu", "ãƒãƒ§" to "cho",
        "ãƒ‹ãƒ£" to "nya", "ãƒ‹ãƒ¥" to "nyu", "ãƒ‹ãƒ§" to "nyo",
        "ãƒ’ãƒ£" to "hya", "ãƒ’ãƒ¥" to "hyu", "ãƒ’ãƒ§" to "hyo",
        "ãƒŸãƒ£" to "mya", "ãƒŸãƒ¥" to "myu", "ãƒŸãƒ§" to "myo",
        "ãƒªãƒ£" to "rya", "ãƒªãƒ¥" to "ryu", "ãƒªãƒ§" to "ryo",
        "ã‚®ãƒ£" to "gya", "ã‚®ãƒ¥" to "gyu", "ã‚®ãƒ§" to "gyo",
        "ã‚¸ãƒ£" to "ja", "ã‚¸ãƒ¥" to "ju", "ã‚¸ãƒ§" to "jo",
        "ãƒ‚ãƒ£" to "ja", "ãƒ‚ãƒ¥" to "ju", "ãƒ‚ãƒ§" to "jo", // ãƒ‚ variants, also commonly 'ja', 'ju', 'jo'
        "ãƒ“ãƒ£" to "bya", "ãƒ“ãƒ¥" to "byu", "ãƒ“ãƒ§" to "byo",
        "ãƒ”ãƒ£" to "pya", "ãƒ”ãƒ¥" to "pyu", "ãƒ”ãƒ§" to "pyo",

        // Basic Katakana Characters
        "ã‚¢" to "a", "ã‚¤" to "i", "ã‚¦" to "u", "ã‚¨" to "e", "ã‚ª" to "o",
        "ã‚«" to "ka", "ã‚­" to "ki", "ã‚¯" to "ku", "ã‚±" to "ke", "ã‚³" to "ko",
        "ã‚µ" to "sa", "ã‚·" to "shi", "ã‚¹" to "su", "ã‚»" to "se", "ã‚½" to "so",
        "ã‚¿" to "ta", "ãƒ" to "chi", "ãƒ„" to "tsu", "ãƒ†" to "te", "ãƒˆ" to "to",
        "ãƒŠ" to "na", "ãƒ‹" to "ni", "ãƒŒ" to "nu", "ãƒ" to "ne", "ãƒŽ" to "no",
        "ãƒ" to "ha", "ãƒ’" to "hi", "ãƒ•" to "fu", "ãƒ˜" to "he", "ãƒ›" to "ho",
        "ãƒž" to "ma", "ãƒŸ" to "mi", "ãƒ " to "mu", "ãƒ¡" to "me", "ãƒ¢" to "mo",
        "ãƒ¤" to "ya", "ãƒ¦" to "yu", "ãƒ¨" to "yo",
        "ãƒ©" to "ra", "ãƒª" to "ri", "ãƒ«" to "ru", "ãƒ¬" to "re", "ãƒ­" to "ro",
        "ãƒ¯" to "wa", "ãƒ²" to "o", // ãƒ² is pronounced 'o'
        "ãƒ³" to "n",

        // Dakuten (voiced consonants)
        "ã‚¬" to "ga", "ã‚®" to "gi", "ã‚°" to "gu", "ã‚²" to "ge", "ã‚´" to "go",
        "ã‚¶" to "za", "ã‚¸" to "ji", "ã‚º" to "zu", "ã‚¼" to "ze", "ã‚¾" to "zo",
        "ãƒ€" to "da", "ãƒ‚" to "ji", "ãƒ…" to "zu", "ãƒ‡" to "de", "ãƒ‰" to "do", // ãƒ‚ and ãƒ… are often 'ji' and 'zu'

        // Handakuten (p-sounds for 'h' group) / Dakuten for 'h' group
        "ãƒ" to "ba", "ãƒ“" to "bi", "ãƒ–" to "bu", "ãƒ™" to "be", "ãƒœ" to "bo", // Dakuten for ãƒí–‰ (ha-row)
        "ãƒ‘" to "pa", "ãƒ”" to "pi", "ãƒ—" to "pu", "ãƒš" to "pe", "ãƒ" to "po", // Handakuten for ãƒí–‰ (ha-row)

        // ChÅonpu (long vowel mark) - removed as per original logic
        "ãƒ¼" to ""
    )

    private val HANGUL_ROMAJA_MAP: Map<String, Map<String, String>> = mapOf(
        "cho" to mapOf(
            "á„€" to "g",  "á„" to "kk", "á„‚" to "n",  "á„ƒ" to "d", 
            "á„„" to "tt", "á„…" to "r",  "á„†" to "m",  "á„‡" to "b",
            "á„ˆ" to "pp", "á„‰" to "s",  "á„Š" to "ss", "á„‹" to "",
            "á„Œ" to "j",  "á„" to "jj", "á„Ž" to "ch", "á„" to "k",
            "á„" to "t",  "á„‘" to "p",  "á„’" to "h"
        ),
        "jung" to mapOf(
            "á…¡" to "a",  "á…¢" to "ae", "á…£" to "ya",  "á…¤" to "yae", 
            "á…¥" to "eo", "á…¦" to "e",  "á…§" to "yeo", "á…¨" to "ye", 
            "á…©" to "o",  "á…ª" to "wa", "á…«" to "wae", "á…¬" to "oe",
            "á…­" to "yo", "á…®" to "u",  "á…¯" to "wo",  "á…°" to "we",
            "á…±" to "wi", "á…²" to "yu", "á…³" to "eu",  "á…´" to "eui",
            "á…µ" to "i"
        ),
        "jong" to mapOf(
            "á†¨" to "k",     "á†¨á„‹" to "g",   "á†¨á„‚" to "ngn", "á†¨á„…" to "ngn", "á†¨á„†" to "ngm", "á†¨á„’" to "kh",
            "á†©" to "kk",    "á†©á„‹" to "kg",  "á†©á„‚" to "ngn", "á†©á„…" to "ngn", "á†©á„†" to "ngm", "á†©á„’" to "kh",
            "á†ª" to "k",     "á†ªá„‹" to "ks",  "á†ªá„‚" to "ngn", "á†ªá„…" to "ngn", "á†ªá„†" to "ngm", "á†ªá„’" to "kch",
            "á†«" to "n",     "á†«á„…" to "ll",  "á†¬" to "n",     "á†¬á„‹" to "nj",  "á†¬á„‚" to "nn",  "á†¬á„…" to "nn",
            "á†¬á„†" to "nm",  "á†¬ã…Ž" to "nch", "á†­" to "n",     "á†­á„‹" to "nh",  "á†­á„…" to "nn",  "á†®" to "t",
            "á†®á„‹" to "d",   "á†®á„‚" to "nn",  "á†®á„…" to "nn",  "á†®á„†" to "nm",  "á†®á„’" to "th",  "á†¯" to "l",
            "á†¯á„‹" to "r",   "á†¯á„‚" to "ll",  "á†¯á„…" to "ll",  "á†°" to "k",     "á†°á„‹" to "lg",  "á†°á„‚" to "ngn",
            "á†°á„…" to "ngn", "á†°á„†" to "ngm", "á†°á„’" to "lkh", "á†±" to "m",     "á†±á„‹" to "lm",  "á†±á„‚" to "mn",
            "á†±á„…" to "mn",  "á†±á„†" to "mm",  "á†±á„’" to "lmh", "á†²" to "p",     "á†²á„‹" to "lb",  "á†²á„‚" to "mn",
            "á†²á„…" to "mn",  "á†²á„†" to "mm",  "á†²á„’" to "lph", "á†³" to "t",     "á†³á„‹" to "ls",  "á†³á„‚" to "nn",
            "á†³á„…" to "nn",  "á†³á„†" to "nm",  "á†³á„’" to "lsh", "á†´" to "t",     "á†´á„‹" to "lt",  "á†´á„‚" to "nn",
            "á†´á„…" to "nn",  "á†´á„†" to "nm",  "á†´á„’" to "lth", "á†µ" to "p",     "á†µá„‹" to "lp",  "á†µá„‚" to "mn",
            "á†µá„…" to "mn",  "á†µá„†" to "mm",  "á†µá„’" to "lph", "á†¶" to "l",     "á†¶á„‹" to "lh",  "á†¶á„‚" to "ll",
            "á†¶á„…" to "ll",  "á†¶á„†" to "lm",  "á†¶á„’" to "lh",  "á†·" to "m",     "á†·á„…" to "mn",  "á†¸" to "p",
            "á†¸á„‹" to "b",   "á†¸á„‚" to "mn",  "á†¸á„…" to "mn",  "á†¸á„†" to "mm",  "á†¸á„’" to "ph",  "á†¹" to "p",
            "á†¹á„‹" to "ps",  "á†¹á„‚" to "mn",  "á†¹á„…" to "mn",  "á†¹á„†" to "mm",  "á†¹á„’" to "psh", "á†º" to "t",
            "á†ºá„‹" to "s",   "á†ºá„‚" to "nn",  "á†ºá„…" to "nn",  "á†ºá„†" to "nm",  "á†ºá„’" to "sh",  "á†»" to "t",
            "á†»á„‹" to "ss",  "á†»á„‚" to "tn",  "á†»á„…" to "tn",  "á†»á„†" to "nm",  "á†»á„’" to "th",  "á†¼" to "ng",
            "á†½" to "t",     "á†½á„‹" to "j",   "á†½á„‚" to "nn",  "á†½á„…" to "nn",  "á†½á„†" to "nm",  "á†½á„’" to "ch",
            "á†¾" to "t",     "á†¾á„‹" to "ch",  "á†¾á„‚" to "nn",  "á†¾á„…" to "nn",  "á†¾á„†" to "nm",  "á†¾á„’" to "ch",
            "á†¿" to "k",     "á†¿á„‹" to "k",   "á†¿á„‚" to "ngn", "á†¿á„…" to "ngn", "á†¿á„†" to "ngm", "á†¿á„’" to "kh",
            "á‡€" to "t",     "á‡€á„‹" to "t",   "á‡€á„‚" to "nn",  "á‡€á„…" to "nn",  "á‡€á„†" to "nm",  "á‡€á„’" to "th",
            "á‡" to "p",     "á‡á„‹" to "p",   "á‡á„‚" to "mn",  "á‡á„…" to "mn",  "á‡á„†" to "mm",  "á‡á„’" to "ph",
            "á‡‚" to "t",     "á‡‚á„‹" to "h",   "á‡‚á„‚" to "nn",  "á‡‚á„…" to "nn",  "á‡‚á„†" to "mm",  "á‡‚á„’" to "t",
            "á‡‚á„€" to "k",
        )
    )

    // Lazy initialized Tokenizer
    private val kuromojiTokenizer: Tokenizer by lazy {
        Tokenizer()
    }

    fun isTtml(lyrics: String): Boolean {
        val trimmed = lyrics.trim()
        if (!trimmed.startsWith("<")) return false

        return trimmed.contains("<tt", ignoreCase = true) ||
                trimmed.contains("http://www.w3.org/ns/ttml", ignoreCase = true)
    }

    fun parseTtml(lyrics: String, durationSeconds: Int? = null): List<LyricsEntry> {
        val parsedLines = TTMLParser.parseTTML(lyrics)
        if (parsedLines.isEmpty()) return emptyList()
        val scale = 1.0

        return parsedLines.map { line ->
            val words =
                line.words
                    .filter { it.text.isNotEmpty() }
                    .map { word ->
                        WordTimestamp(
                            text = word.text,
                            startTime = word.startTime * scale,
                            endTime = word.endTime * scale,
                            isBackground = word.isBackground,
                        )
                    }.takeIf { it.isNotEmpty() }

            LyricsEntry(
                time = (line.startTime * scale * 1000.0).toLong(),
                text = line.text,
                words = words,
                agent = line.agent,
            )
        }.sorted()
    }

    fun parseLyrics(lyrics: String): List<LyricsEntry> {
        val lines = lyrics.lines()
        val result = mutableListOf<LyricsEntry>()

        for (line in lines) {
            val entries = parseLine(line)
            if (entries != null) {
                result.addAll(entries)
            }
        }
        return result.sorted()
    }

    private fun parseLine(line: String): List<LyricsEntry>? {
        if (line.isEmpty()) {
            return null
        }
        val matchResult = LINE_REGEX.matchEntire(line.trim()) ?: return null
        val times = matchResult.groupValues[1]
        val text = matchResult.groupValues[3]
        val timeMatchResults = TIME_REGEX.findAll(times)

        return timeMatchResults
            .map { timeMatchResult ->
                val min = timeMatchResult.groupValues[1].toLong()
                val sec = timeMatchResult.groupValues[2].toLong()
                val milString = timeMatchResult.groupValues[3]
                var mil = milString.toLong()
                if (milString.length == 2) {
                    mil *= 10
                }
                val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
                LyricsEntry(time, text)
            }.toList()
    }

    fun findCurrentLineIndex(
        lines: List<LyricsEntry>,
        position: Long,
        leadMs: Long = 300L,
    ): Int {
        if (lines.isEmpty()) return -1

        val target = position + leadMs
        var low = 0
        var high = lines.lastIndex

        while (low <= high) {
            val mid = (low + high).ushr(1)
            val midTime = lines[mid].time

            if (midTime < target) {
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return high.coerceIn(0, lines.lastIndex)
    }

    /**
     * Converts a Katakana string to Romaji.
     * This optimized version uses a pre-defined map and StringBuilder for better performance
     * compared to chained regex replacements.
     * Expected impact: Significant reduction in object creation (Regex, String) and faster execution.
     */
    fun katakanaToRomaji(katakana: String?): String {
        if (katakana.isNullOrEmpty()) return ""

        val romajiBuilder = StringBuilder(katakana.length) // Initial capacity
        var i = 0
        val n = katakana.length
        while (i < n) {
            var consumed = false
            // Prioritize 2-character sequences from the map (e.g., "ã‚­ãƒ£" before "ã‚­")
            if (i + 1 < n) {
                val twoCharCandidate = katakana.substring(i, i + 2)
                val mappedTwoChar = KANA_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    romajiBuilder.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }

            if (!consumed) {
                // If no 2-character sequence matched, try 1-character
                val oneCharCandidate = katakana[i].toString()
                val mappedOneChar = KANA_ROMAJI_MAP[oneCharCandidate]
                if (mappedOneChar != null) {
                    romajiBuilder.append(mappedOneChar)
                } else {
                    // If the character is not in Katakana map, append it as is.
                    romajiBuilder.append(oneCharCandidate)
                }
                i += 1
            }
        }
        return romajiBuilder.toString().lowercase()
    }

    /**
     * Romanizes Japanese text using Kuromoji Tokenizer and the optimized katakanaToRomaji function.
     * Runs on Dispatchers.Default for CPU-intensive work.
     * Expected impact: Faster tokenization due to reused Tokenizer instance and faster
     * per-token romanization.
     */
    suspend fun romanizeJapanese(text: String): String = withContext(Dispatchers.Default) {
        // Use the lazily initialized tokenizer
        val tokens = kuromojiTokenizer.tokenize(text)

        val romanizedTokens = tokens.mapIndexed { index, token ->
            val currentReading = if (token.reading.isNullOrEmpty() || token.reading == "*") {
                token.surface
            } else {
                token.reading
            }

            // Pass the next token's reading for sokuon handling if applicable
            val nextTokenReading = if (index + 1 < tokens.size) {
                tokens[index + 1].reading?.takeIf { it.isNotEmpty() && it != "*" } ?: tokens[index + 1].surface
            } else {
                null
            }
            katakanaToRomaji(currentReading, nextTokenReading)
        }
        romanizedTokens.joinToString(" ")
    }

    /**
     * Converts a Katakana string to Romaji.
     * This optimized version uses a pre-defined map and StringBuilder for better performance
     * compared to chained regex replacements.
     * Expected impact: Significant reduction in object creation (Regex, String) and faster execution.
     * @param katakana The Katakana string to convert.
     * @param nextKatakana Optional: The next Katakana string (from the next token) to help with sokuon (ãƒƒ) gemination.
     */
    fun katakanaToRomaji(katakana: String?, nextKatakana: String? = null): String {
        if (katakana.isNullOrEmpty()) return ""

        val romajiBuilder = StringBuilder(katakana.length) // Initial capacity
        var i = 0
        val n = katakana.length
        while (i < n) {
            var consumed = false
            // Prioritize 2-character sequences from the map (e.g., "ã‚­ãƒ£" before "ã‚­")
            if (i + 1 < n) {
                val twoCharCandidate = katakana.substring(i, i + 2)
                val mappedTwoChar = KANA_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    romajiBuilder.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }

            // Handle sokuon (ãƒƒ) - gemination
            if (!consumed && katakana[i] == 'ãƒƒ') {
                val nextCharToDouble = nextKatakana?.getOrNull(0)
                if (nextCharToDouble != null) {
                    val nextCharRomaji = KANA_ROMAJI_MAP[nextCharToDouble.toString()]?.getOrNull(0)?.toString()
                        ?: nextCharToDouble.toString()
                    romajiBuilder.append(nextCharRomaji.lowercase().trim())
                }
                // Sokuon itself doesn't have a direct romaji representation other than geminating the next consonant.
                // We just consume 'ãƒƒ' and let the next character (if any within the current token) be processed normally.
                i += 1 // Consume the 'ãƒƒ'
                consumed = true
            }

            if (!consumed) {
                // If no 2-character sequence matched, try 1-character
                val oneCharCandidate = katakana[i].toString()
                val mappedOneChar = KANA_ROMAJI_MAP[oneCharCandidate]
                if (mappedOneChar != null) {
                    romajiBuilder.append(mappedOneChar)
                } else {
                    // If the character is not in Katakana map, append it as is.
                    romajiBuilder.append(oneCharCandidate)
                }
                i += 1
            }
        }
        return romajiBuilder.toString().lowercase()
    }

    suspend fun romanizeKorean(text: String): String = withContext(Dispatchers.Default) {
        val romajaBuilder = StringBuilder()
        var prevFinal: String? = null

        for (i in text.indices) {
            val char = text[i]

            if (char in '\uAC00'..'\uD7A3') {
                val syllableIndex = char.code - 0xAC00
                
                val choIndex = syllableIndex / (21 * 28)
                val jungIndex = (syllableIndex % (21 * 28)) / 28
                val jongIndex = syllableIndex % 28

                val choChar = (0x1100 + choIndex).toChar().toString()
                val jungChar = (0x1161 + jungIndex).toChar().toString()
                val jongChar = if (jongIndex == 0) null else (0x11A7 + jongIndex).toChar().toString()

                if (prevFinal != null) {
                    val contextKey = prevFinal + choChar
                    val jong = HANGUL_ROMAJA_MAP["jong"]?.get(contextKey)
                        ?: HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal)
                        ?: prevFinal
                    romajaBuilder.append(jong)
                }

                val cho = HANGUL_ROMAJA_MAP["cho"]?.get(choChar) ?: choChar
                val jung = HANGUL_ROMAJA_MAP["jung"]?.get(jungChar) ?: jungChar
                romajaBuilder.append(cho).append(jung)

                prevFinal = jongChar
            } else {
                if (prevFinal != null) {
                    val jong = HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal) ?: prevFinal
                    romajaBuilder.append(jong)
                    prevFinal = null
                }
                romajaBuilder.append(char)
            }
        }

        if (prevFinal != null) {
            val jong = HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal) ?: prevFinal
            romajaBuilder.append(jong)
        }

        romajaBuilder.toString()
    }

    /**
     * Checks if the given text contains any Japanese characters (Hiragana, Katakana, or common Kanji).
     * This function is generally efficient due to '.any' and early exit.
     * No major performance bottlenecks expected here for typical inputs.
     */
    fun isJapanese(text: String): Boolean {
        return text.any { char ->
            (char in '\u3040'..'\u309F') || // Hiragana
            (char in '\u30A0'..'\u30FF') || // Katakana
            // CJK Unified Ideographs (covers most common Kanji)
            // Note: This range also includes many Chinese Hanzi.
            // Differentiating Japanese Kanji from Chinese Hanzi solely based on Unicode
            // ranges is challenging as they share many characters.
            // For more accurate Japanese detection, one might need to analyze
            // the presence of Hiragana/Katakana alongside Kanji.
            (char in '\u4E00'..'\u9FFF')
        }
    }

    /**
     * Checks if the given text contains any Korean characters (Hangul Syllables, Jamo, etc.).
     */
    fun isKorean(text: String): Boolean {
        return text.any { char ->
            (char in '\uAC00'..'\uD7A3') // Hangul Syllables
        }
    }
        
    /**
     * Checks if the given text contains any Chinese characters (common Hanzi).
     * This function is generally efficient due to '.any' and early exit.
     * To improve accuracy in distinguishing between Chinese and Japanese (which shares Kanji),
     * this function now checks if the text *predominantly* consists of CJK Unified Ideographs
     * and *lacks* significant amounts of Hiragana or Katakana.
     *
     * A simple threshold is used here. More sophisticated methods (e.g., frequency analysis,
     * dictionaries, or machine learning models) would be needed for higher accuracy.
     */
    fun isChinese(text: String): Boolean {
        if (text.isEmpty()) return false

        val hanCharCount = text.count { hasScript(it, UnicodeScript.HAN) }
        if (hanCharCount == 0) return false

        val japaneseKanaCount = text.count { hasScript(it, UnicodeScript.HIRAGANA) || hasScript(it, UnicodeScript.KATAKANA) }
        val hangulCount = text.count { hasScript(it, UnicodeScript.HANGUL) }

        return japaneseKanaCount == 0 && hangulCount == 0
    }

    fun isHindi(text: String): Boolean = text.any { hasScript(it, UnicodeScript.DEVANAGARI) }

    fun hasOtherRomanizableScript(text: String): Boolean {
        return text.any { char ->
            if (!char.isLetter()) return@any false
            val script = UnicodeScript.of(char.code)
            script !in OTHER_ROMANIZATION_EXCLUDED_SCRIPTS
        }
    }

    fun shouldRomanizeLyricsLine(
        text: String,
        preferences: LyricsRomanizationPreferences,
    ): Boolean {
        if (!preferences.isEnabled || text.isBlank()) return false

        return when {
            preferences.romanizeJapanese && looksJapanese(text) -> true
            preferences.romanizeKorean && isKorean(text) -> true
            preferences.romanizeHindi && isHindi(text) -> true
            preferences.romanizeChinese && isChinese(text) -> true
            preferences.romanizeOther && hasOtherRomanizableScript(text) -> true
            else -> false
        }
    }

    suspend fun romanizeLyricsLine(
        text: String,
        preferences: LyricsRomanizationPreferences,
    ): String? {
        if (!shouldRomanizeLyricsLine(text, preferences)) return null

        val romanized = when {
            preferences.romanizeJapanese && looksJapanese(text) -> romanizeJapanese(text)
            preferences.romanizeKorean && isKorean(text) -> romanizeKorean(text)
            preferences.romanizeHindi && isHindi(text) -> romanizeWithAnyAscii(text)
            preferences.romanizeChinese && isChinese(text) -> romanizeWithAnyAscii(text)
            preferences.romanizeOther && hasOtherRomanizableScript(text) -> romanizeWithAnyAscii(text)
            else -> null
        }

        return normalizeRomanizedText(text, romanized)
    }

    private suspend fun romanizeWithAnyAscii(text: String): String = withContext(Dispatchers.Default) {
        AnyAscii.transliterate(text)
    }

    private fun normalizeRomanizedText(original: String, romanized: String?): String? {
        val normalized = romanized
            ?.replace(WHITESPACE_REGEX, " ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        return normalized.takeUnless { it.equals(original.trim(), ignoreCase = true) }
    }

    private fun looksJapanese(text: String): Boolean {
        return text.any {
            hasScript(it, UnicodeScript.HIRAGANA) ||
                hasScript(it, UnicodeScript.KATAKANA) ||
                it == 'ã€…' ||
                it == 'ã€†' ||
                it == 'ãƒ¶'
        }
    }

    private fun hasScript(char: Char, script: UnicodeScript): Boolean {
        return char.isLetter() && UnicodeScript.of(char.code) == script
    }
}
