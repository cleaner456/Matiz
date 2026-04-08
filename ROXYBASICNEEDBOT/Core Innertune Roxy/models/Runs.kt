package roxy.music.app.models

import kotlinx.serialization.Serializable

@Serializable
data class Run(
    val text: String,
    val navigationEndpoint: NavigationEndpoint? = null
)

@Serializable
data class Runs(
    val runs: List<Run>? = null
) {
    val firstText: String? get() = runs?.firstOrNull()?.text
    val joinedText: String? get() = runs?.joinToString("") { it.text }
}

// Extension functions used throughout pages/
fun List<Run>.oddElements() = filterIndexed { index, _ -> index % 2 == 0 }
fun List<Run>.splitBySeparator(): List<List<Run>> {
    val result = mutableListOf<List<Run>>()
    var current = mutableListOf<Run>()
    for (run in this) {
        if (run.text == " • " || run.text == "•") {
            result.add(current)
            current = mutableListOf()
        } else {
            current.add(run)
        }
    }
    if (current.isNotEmpty()) result.add(current)
    return result
}
fun List<Run>.clean() = filter { it.text.isNotBlank() && it.text != " • " && it.text != "•" }
