import Foundation
import SwiftUI

struct SubEmotionGroup: Identifiable, Hashable, Sendable {
    let name: String
    let options: [String]

    var id: String { name }
}

struct CoreEmotion: Identifiable, Hashable, Sendable {
    let name: String
    let colorHex: String
    let sub1Groups: [SubEmotionGroup]

    var id: String { name }

    var swiftUIColor: Color {
        Color(hex: colorHex)
    }

    func sub2Options(for sub1: String) -> [String] {
        sub1Groups.first(where: { $0.name == sub1 })?.options ?? []
    }
}

enum FeelingsData {
    static let emotions: [CoreEmotion] = [
        CoreEmotion(
            name: "Sad",
            colorHex: "#7da5d9",
            sub1Groups: [
                SubEmotionGroup(name: "Lonely", options: ["Isolated", "Abandoned"]),
                SubEmotionGroup(name: "Vulnerable", options: ["Victimised", "Fragile"]),
                SubEmotionGroup(name: "Despair", options: ["Grief", "Powerless"]),
                SubEmotionGroup(name: "Guilty", options: ["Ashamed", "Remorseful"]),
                SubEmotionGroup(name: "Depressed", options: ["Empty", "Inferior"]),
                SubEmotionGroup(name: "Hurt", options: ["Disappointed", "Embarrassed"])
            ]
        ),
        CoreEmotion(
            name: "Disgusted",
            colorHex: "#a48dbd",
            sub1Groups: [
                SubEmotionGroup(name: "Repelled", options: ["Horrified", "Hesitant"]),
                SubEmotionGroup(name: "Awful", options: ["Nauseated", "Detestable"]),
                SubEmotionGroup(name: "Disapproving", options: ["Judgmental", "Embarrassed"]),
                SubEmotionGroup(name: "Disappointed", options: ["Appalled", "Revolted"])
            ]
        ),
        CoreEmotion(
            name: "Angry",
            colorHex: "#d96259",
            sub1Groups: [
                SubEmotionGroup(name: "Let down", options: ["Betrayed", "Resentful"]),
                SubEmotionGroup(name: "Humiliated", options: ["Disrespected", "Ridiculed"]),
                SubEmotionGroup(name: "Bitter", options: ["Indignant", "Violated"]),
                SubEmotionGroup(name: "Mad", options: ["Furious", "Jealous"]),
                SubEmotionGroup(name: "Aggressive", options: ["Provoked", "Hostile"]),
                SubEmotionGroup(name: "Frustrated", options: ["Infuriated", "Annoyed"]),
                SubEmotionGroup(name: "Distant", options: ["Withdrawn", "Numb"]),
                SubEmotionGroup(name: "Critical", options: ["Sceptical", "Dismissive"])
            ]
        ),
        CoreEmotion(
            name: "Fearful",
            colorHex: "#62c4b8",
            sub1Groups: [
                SubEmotionGroup(name: "Scared", options: ["Helpless", "Frightened"]),
                SubEmotionGroup(name: "Anxious", options: ["Overwhelmed", "Worried"]),
                SubEmotionGroup(name: "Insecure", options: ["Inadequate", "Inferior"]),
                SubEmotionGroup(name: "Weak", options: ["Worthless", "Insignificant"]),
                SubEmotionGroup(name: "Rejected", options: ["Excluded", "Persecuted"]),
                SubEmotionGroup(name: "Threatened", options: ["Nervous", "Exposed"])
            ]
        ),
        CoreEmotion(
            name: "Bad",
            colorHex: "#70c292",
            sub1Groups: [
                SubEmotionGroup(name: "Bored", options: ["Indifferent", "Apathetic"]),
                SubEmotionGroup(name: "Busy", options: ["Pressured", "Rushed"]),
                SubEmotionGroup(name: "Stressed", options: ["Overwhelmed", "Out of control"]),
                SubEmotionGroup(name: "Tired", options: ["Sleepy", "Unfocused"])
            ]
        ),
        CoreEmotion(
            name: "Surprised",
            colorHex: "#e6af73",
            sub1Groups: [
                SubEmotionGroup(name: "Startled", options: ["Shocked", "Dismayed"]),
                SubEmotionGroup(name: "Confused", options: ["Disillusioned", "Perplexed"]),
                SubEmotionGroup(name: "Amazed", options: ["Astonished", "Awe"]),
                SubEmotionGroup(name: "Excited", options: ["Eager", "Energetic"])
            ]
        ),
        CoreEmotion(
            name: "Happy",
            colorHex: "#f2a380",
            sub1Groups: [
                SubEmotionGroup(name: "Playful", options: ["Aroused", "Cheeky"]),
                SubEmotionGroup(name: "Content", options: ["Free", "Joyful"]),
                SubEmotionGroup(name: "Interested", options: ["Curious", "Inquisitive"]),
                SubEmotionGroup(name: "Proud", options: ["Successful", "Confident"]),
                SubEmotionGroup(name: "Accepted", options: ["Respected", "Valued"]),
                SubEmotionGroup(name: "Powerful", options: ["Courageous", "Creative"]),
                SubEmotionGroup(name: "Peaceful", options: ["Loving", "Thankful"]),
                SubEmotionGroup(name: "Trusting", options: ["Sensitive", "Intimate"]),
                SubEmotionGroup(name: "Optimistic", options: ["Hopeful", "Inspired"])
            ]
        )
    ]

    static func emotion(named name: String) -> CoreEmotion? {
        emotions.first { $0.name == name }
    }

    static func colorHex(for core: String) -> String {
        emotion(named: core)?.colorHex ?? "#5f27cd"
    }
}
