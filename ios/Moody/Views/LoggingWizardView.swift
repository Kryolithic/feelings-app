import SwiftUI

struct LoggingWizardView: View {
    @Environment(MoodyStore.self) private var store

    @State private var step = 0
    @State private var selectedCore = ""
    @State private var selectedSub1 = ""
    @State private var selectedSub2 = ""
    @State private var intensity: Double = 5
    @State private var note = ""
    @State private var isSubmitting = false

    private var coreEmotion: CoreEmotion? {
        FeelingsData.emotion(named: selectedCore)
    }

    private var coreColor: Color {
        if let hex = coreEmotion?.colorHex {
            return Color(hex: hex)
        }
        return Color.accentColor
    }

    var body: some View {
        VStack(spacing: 0) {
            if (1...4).contains(step) {
                wizardTopBar
            }

            Group {
                switch step {
                case 0:
                    StartBreathingStep {
                        selectedCore = ""
                        selectedSub1 = ""
                        selectedSub2 = ""
                        intensity = 5
                        note = ""
                        step = 1
                    }
                case 1:
                    CoreSelectStep { core in
                        selectedCore = core
                        selectedSub1 = ""
                        selectedSub2 = ""
                        step = 2
                    }
                case 2:
                    if let coreEmotion {
                        Sub1SelectStep(coreEmotion: coreEmotion) { sub1 in
                            selectedSub1 = sub1
                            selectedSub2 = ""
                            step = 3
                        }
                    }
                case 3:
                    if let coreEmotion {
                        Sub2SelectStep(
                            coreEmotion: coreEmotion,
                            sub1: selectedSub1,
                            onSelect: { sub2 in
                                selectedSub2 = sub2
                                step = 4
                            },
                            onSkip: {
                                selectedSub2 = ""
                                step = 4
                            }
                        )
                    }
                case 4:
                    IntensityReflectionStep(
                        coreColor: coreColor,
                        selectedCore: selectedCore,
                        selectedSub1: selectedSub1,
                        selectedSub2: selectedSub2,
                        intensity: $intensity,
                        note: $note,
                        isSubmitting: isSubmitting,
                        onSubmit: submitLog
                    )
                case 5:
                    WizardSuccessStep(
                        coreColor: coreColor,
                        selectedCore: selectedCore,
                        selectedSub1: selectedSub1,
                        selectedSub2: selectedSub2,
                        intensity: Int(intensity.rounded())
                    ) {
                        step = 0
                    }
                default:
                    EmptyView()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private var wizardTopBar: some View {
        HStack {
            Button {
                if step > 0 { step -= 1 }
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(.secondary)
                    .frame(width: 36, height: 36)
            }
            .accessibilityLabel("Back")

            Spacer()

            HStack(spacing: 8) {
                ForEach(1...4, id: \.self) { index in
                    Circle()
                        .fill(
                            index == step ? coreColor :
                            index < step ? coreColor.opacity(0.45) :
                            Color.secondary.opacity(0.28)
                        )
                        .frame(width: 8, height: 8)
                }
            }

            Spacer()

            Color.clear.frame(width: 36, height: 36)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    private func submitLog() {
        guard !isSubmitting else { return }
        isSubmitting = true
        let newLog = LogEntry(
            id: UUID().uuidString.lowercased(),
            timestamp: IsoTimestamp.formatUtc(Date()),
            core: selectedCore,
            sub1: selectedSub1,
            sub2: selectedSub2,
            intensity: Int(intensity.rounded()),
            note: note.trimmingCharacters(in: .whitespacesAndNewlines),
            color: FeelingsData.colorHex(for: selectedCore)
        )
        Task {
            await store.saveLog(newLog)
            await MainActor.run {
                isSubmitting = false
                step = 5
            }
        }
    }
}

private struct StartBreathingStep: View {
    let onStart: () -> Void
    @State private var isInhaling = true

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            Text("How are you feeling today?")
                .font(.system(size: 20, weight: .medium))
                .padding(.horizontal, 24)
                .padding(.vertical, 16)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                .padding(.bottom, 56)

            Button(action: onStart) {
                ZStack {
                    Circle()
                        .fill(Color.accentColor.opacity(0.18))
                        .frame(width: 176, height: 176)
                        .scaleEffect(isInhaling ? 1.07 : 0.94)
                        .animation(.easeInOut(duration: 4.0), value: isInhaling)

                    VStack(spacing: 4) {
                        Text(isInhaling ? "Inhale" : "Exhale")
                            .font(.system(size: 18, weight: .medium))
                            .foregroundStyle(Color.accentColor)
                        Text("Tap to log")
                            .font(.system(size: 12))
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .buttonStyle(.plain)

            Spacer()
        }
        .padding(24)
        .task {
            isInhaling = true
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(4))
                if Task.isCancelled { break }
                isInhaling.toggle()
            }
        }
    }
}

private struct CoreSelectStep: View {
    let onSelect: (String) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("Select your main feeling")
                    .font(.system(size: 24, weight: .bold))
                Text("Choose the core emotion that matches your current state.")
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)
                    .padding(.bottom, 12)

                ForEach(FeelingsData.emotions) { core in
                    Button {
                        onSelect(core.name)
                    } label: {
                        HStack {
                            Text(core.name)
                                .font(.system(size: 18, weight: .medium))
                                .foregroundStyle(.primary)
                            Spacer()
                            Circle()
                                .fill(core.swiftUIColor)
                                .frame(width: 12, height: 12)
                        }
                        .padding(18)
                        .background(Color(.secondarySystemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(24)
        }
    }
}

private struct Sub1SelectStep: View {
    let coreEmotion: CoreEmotion
    let onSelect: (String) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                Text("Let's get more specific")
                    .font(.system(size: 24, weight: .bold))

                (
                    Text("Under ")
                        .foregroundColor(.secondary)
                    + Text(coreEmotion.name)
                        .foregroundColor(coreEmotion.swiftUIColor)
                        .bold()
                    + Text(", which path fits best?")
                        .foregroundColor(.secondary)
                )
                .font(.system(size: 14))
                .padding(.bottom, 14)

                ForEach(coreEmotion.sub1Groups) { group in
                    Button {
                        onSelect(group.name)
                    } label: {
                        HStack {
                            Text(group.name)
                                .font(.system(size: 16, weight: .medium))
                                .foregroundStyle(.primary)
                            Spacer()
                            Text("→")
                                .font(.system(size: 16))
                                .foregroundStyle(.secondary)
                        }
                        .padding(16)
                        .background(Color(.secondarySystemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(24)
        }
    }
}

private struct Sub2SelectStep: View {
    let coreEmotion: CoreEmotion
    let sub1: String
    let onSelect: (String) -> Void
    let onSkip: () -> Void

    private var options: [String] {
        coreEmotion.sub2Options(for: sub1)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                Text("Do any of these options describe how you are feeling?")
                    .font(.system(size: 22, weight: .bold))
                Text("Refining \(coreEmotion.name) → \(sub1)")
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)
                    .padding(.bottom, 14)

                ForEach(options, id: \.self) { option in
                    Button {
                        onSelect(option)
                    } label: {
                        HStack {
                            Text(option)
                                .font(.system(size: 16, weight: .medium))
                                .foregroundStyle(.primary)
                            Spacer()
                            Image(systemName: "checkmark")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(coreEmotion.swiftUIColor)
                        }
                        .padding(16)
                        .background(Color(.secondarySystemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }

                Button(action: onSkip) {
                    HStack {
                        Text("None of these apply — Skip")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(.secondary)
                        Spacer()
                        Text("→")
                            .font(.system(size: 16))
                            .foregroundStyle(.secondary)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 14)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .strokeBorder(
                                Color.secondary.opacity(0.35),
                                style: StrokeStyle(lineWidth: 1, dash: [6, 4])
                            )
                    )
                }
                .buttonStyle(.plain)
                .padding(.top, 6)
            }
            .padding(24)
        }
    }
}

private struct IntensityReflectionStep: View {
    let coreColor: Color
    let selectedCore: String
    let selectedSub1: String
    let selectedSub2: String
    @Binding var intensity: Double
    @Binding var note: String
    let isSubmitting: Bool
    let onSubmit: () -> Void

    private var breadcrumb: String {
        if selectedSub2.isEmpty {
            return "\(selectedCore) → \(selectedSub1)"
        }
        return "\(selectedCore) → \(selectedSub1) → \(selectedSub2)"
    }

    private var roundedIntensity: Int {
        Int(intensity.rounded())
    }

    private var intensityLabel: String {
        switch roundedIntensity {
        case 1...3: return "Mild"
        case 4...6: return "Moderate"
        case 7...8: return "Intense"
        default: return "Overwhelming"
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("How intense is this emotion?")
                        .font(.system(size: 24, weight: .bold))

                    Text(breadcrumb)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(coreColor)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(coreColor.opacity(0.14))
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                }

                VStack(spacing: 12) {
                    Text("\(roundedIntensity)")
                        .font(.system(size: 56, weight: .bold))
                        .foregroundStyle(coreColor)

                    Text(intensityLabel)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(.secondary)

                    Slider(value: $intensity, in: 1...10, step: 1)
                        .tint(coreColor)
                        .padding(.top, 8)

                    HStack {
                        Text("1 (Subtle)")
                        Spacer()
                        Text("10 (Peak)")
                    }
                    .font(.system(size: 11))
                    .foregroundStyle(.secondary)
                }
                .padding(20)
                .background(Color(.secondarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))

                VStack(alignment: .leading, spacing: 8) {
                    Text("Journal reflection (Optional)")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(.secondary)

                    ZStack(alignment: .topLeading) {
                        TextEditor(text: $note)
                            .scrollContentBackground(.hidden)
                            .frame(minHeight: 110)
                            .padding(10)

                        if note.isEmpty {
                            Text("What is causing this feeling? Writing it down can bring clarity...")
                                .font(.system(size: 14))
                                .foregroundStyle(.secondary.opacity(0.65))
                                .padding(.horizontal, 15)
                                .padding(.vertical, 18)
                                .allowsHitTesting(false)
                        }
                    }
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .stroke(Color.secondary.opacity(0.25), lineWidth: 1)
                    )
                }

                Button(action: onSubmit) {
                    Text(isSubmitting ? "Saving..." : "Log Reflection")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(coreColor)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .disabled(isSubmitting)
                .padding(.top, 8)
            }
            .padding(24)
        }
        .scrollDismissesKeyboard(.interactively)
    }
}

private struct WizardSuccessStep: View {
    let coreColor: Color
    let selectedCore: String
    let selectedSub1: String
    let selectedSub2: String
    let intensity: Int
    let onFinish: () -> Void

    private var primaryLabel: String {
        selectedSub2.isEmpty ? selectedSub1 : selectedSub2
    }

    private var secondaryLabel: String {
        selectedSub2.isEmpty ? selectedCore : selectedSub1
    }

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            ZStack {
                Circle()
                    .fill(coreColor.opacity(0.15))
                    .frame(width: 80, height: 80)
                Image(systemName: "checkmark")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundStyle(coreColor)
            }

            VStack(spacing: 8) {
                Text("Reflection Logged")
                    .font(.system(size: 24, weight: .bold))
                Text("Your feelings have been recorded. Acknowledge this moment and take a deep breath.")
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 16)
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("LOGGED EMOTION")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(.secondary)

                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Text(primaryLabel)
                        .font(.system(size: 18, weight: .bold))
                    Text("(\(secondaryLabel))")
                        .font(.system(size: 13))
                        .foregroundStyle(.secondary)
                }

                Text("Intensity: \(intensity)")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Color(.tertiarySystemFill))
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color.secondary.opacity(0.22), lineWidth: 1)
            )
            .padding(.horizontal, 16)

            Button(action: onFinish) {
                Text("Back to Home")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.primary)
                    .padding(.horizontal, 28)
                    .padding(.vertical, 14)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .padding(.top, 8)

            Spacer()
        }
        .padding(24)
    }
}
