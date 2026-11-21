package com.example.feelingsapp.data

import androidx.compose.ui.graphics.Color

data class CoreEmotion(
    val name: String,
    val color: String,
    val sub1: Map<String, List<String>>
)

fun parseColorSafe(colorString: String, fallback: Color = Color(0xFF5F27CD)): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (_: Exception) {
        fallback
    }
}

object FeelingsData {
    val emotions = mapOf(
        "Sad" to CoreEmotion(
            name = "Sad",
            color = "#7da5d9",
            sub1 = mapOf(
                "Lonely" to listOf("Isolated", "Abandoned"),
                "Vulnerable" to listOf("Victimised", "Fragile"),
                "Despair" to listOf("Grief", "Powerless"),
                "Guilty" to listOf("Ashamed", "Remorseful"),
                "Depressed" to listOf("Empty", "Inferior"),
                "Hurt" to listOf("Disappointed", "Embarrassed")
            )
        ),
        "Disgusted" to CoreEmotion(
            name = "Disgusted",
            color = "#a48dbd",
            sub1 = mapOf(
                "Repelled" to listOf("Horrified", "Hesitant"),
                "Awful" to listOf("Nauseated", "Detestable"),
                "Disapproving" to listOf("Judgmental", "Embarrassed"),
                "Disappointed" to listOf("Appalled", "Revolted")
            )
        ),
        "Angry" to CoreEmotion(
            name = "Angry",
            color = "#d96259",
            sub1 = mapOf(
                "Let down" to listOf("Betrayed", "Resentful"),
                "Humiliated" to listOf("Disrespected", "Ridiculed"),
                "Bitter" to listOf("Indignant", "Violated"),
                "Mad" to listOf("Furious", "Jealous"),
                "Aggressive" to listOf("Provoked", "Hostile"),
                "Frustrated" to listOf("Infuriated", "Annoyed"),
                "Distant" to listOf("Withdrawn", "Numb"),
                "Critical" to listOf("Sceptical", "Dismissive")
            )
        ),
        "Fearful" to CoreEmotion(
            name = "Fearful",
            color = "#62c4b8",
            sub1 = mapOf(
                "Scared" to listOf("Helpless", "Frightened"),
                "Anxious" to listOf("Overwhelmed", "Worried"),
                "Insecure" to listOf("Inadequate", "Inferior"),
                "Weak" to listOf("Worthless", "Insignificant"),
                "Rejected" to listOf("Excluded", "Persecuted"),
                "Threatened" to listOf("Nervous", "Exposed")
            )
        ),
        "Bad" to CoreEmotion(
            name = "Bad",
            color = "#70c292",
            sub1 = mapOf(
                "Bored" to listOf("Indifferent", "Apathetic"),
                "Busy" to listOf("Pressured", "Rushed"),
                "Stressed" to listOf("Overwhelmed", "Out of control"),
                "Tired" to listOf("Sleepy", "Unfocused")
            )
        ),
        "Surprised" to CoreEmotion(
            name = "Surprised",
            color = "#e6af73",
            sub1 = mapOf(
                "Startled" to listOf("Shocked", "Dismayed"),
                "Confused" to listOf("Disillusioned", "Perplexed"),
                "Amazed" to listOf("Astonished", "Awe"),
                "Excited" to listOf("Eager", "Energetic")
            )
        ),
        "Happy" to CoreEmotion(
            name = "Happy",
            color = "#f2a380",
            sub1 = mapOf(
                "Playful" to listOf("Aroused", "Cheeky"),
                "Content" to listOf("Free", "Joyful"),
                "Interested" to listOf("Curious", "Inquisitive"),
                "Proud" to listOf("Successful", "Confident"),
                "Accepted" to listOf("Respected", "Valued"),
                "Powerful" to listOf("Courageous", "Creative"),
                "Peaceful" to listOf("Loving", "Thankful"),
                "Trusting" to listOf("Sensitive", "Intimate"),
                "Optimistic" to listOf("Hopeful", "Inspired")
            )
        )
    )
}
