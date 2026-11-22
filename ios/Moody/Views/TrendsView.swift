import Charts
import SwiftUI

private struct IndexedChartPoint: Identifiable {
    let index: Int
    let log: LogEntry
    let dateLabel: String

    var id: String { log.id }
}

struct TrendsView: View {
    @Environment(MoodyStore.self) private var store

    @State private var selectedDayOffset: Int = {
        let weekday = Calendar.current.component(.weekday, from: Date())
        return (weekday - 1 + 7) % 7
    }()
    @State private var weekOffset: Int = 0
    @State private var selectedChartIndex: Int?

    private let dayNames = ["S", "M", "T", "W", "Th", "F", "S"]

    private var weekDates: [Date] {
        computeWeekDates(weekOffset: weekOffset)
    }

    private var selectedDate: Date {
        guard weekDates.indices.contains(selectedDayOffset) else {
            return weekDates.first ?? Date()
        }
        return weekDates[selectedDayOffset]
    }

    private var selectedDayLogs: [LogEntry] {
        filterLogs(store.logs, forDay: selectedDate)
    }

    private var chartPoints: [IndexedChartPoint] {
        let recent = store.logs
            .sorted {
                (IsoTimestamp.parseUtc($0.timestamp) ?? .distantPast)
                    < (IsoTimestamp.parseUtc($1.timestamp) ?? .distantPast)
            }
            .suffix(7)

        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US")
        formatter.dateFormat = "M/d"

        return Array(recent.enumerated()).map { idx, entry in
            let parsed = IsoTimestamp.parseUtc(entry.timestamp)
            let label = parsed.map { formatter.string(from: $0) } ?? ""
            return IndexedChartPoint(index: idx, log: entry, dateLabel: label)
        }
    }

    private var selectedChartPoint: IndexedChartPoint? {
        guard let idx = selectedChartIndex else { return nil }
        return chartPoints.first { $0.index == idx }
    }

    private var displayDateString: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US")
        formatter.dateFormat = "EEEE, MMM d"
        return formatter.string(from: selectedDate)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Trends")
                        .font(.system(size: 24, weight: .bold))
                    Text("Observe the patterns in your emotions over time.")
                        .font(.system(size: 14))
                        .foregroundStyle(.secondary)
                }

                intensityChartCard
                weekCalendarCard
                dailyLogsSection
            }
            .padding(24)
        }
    }

    private var intensityChartCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Intensity Trend")
                    .font(.system(size: 15, weight: .semibold))
                Spacer()
                Text("Last 7 checks")
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
            }

            if chartPoints.count < 2 {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color.secondary.opacity(0.25), lineWidth: 1)
                    .frame(height: 150)
                    .overlay {
                        Text("Log feelings multiple times to view trends graph.")
                            .font(.system(size: 13))
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(16)
                    }
            } else {
                Chart(chartPoints) { pt in
                    AreaMark(
                        x: .value("Check", pt.index),
                        y: .value("Intensity", pt.log.intensity)
                    )
                    .foregroundStyle(
                        LinearGradient(
                            colors: [Color.accentColor.opacity(0.22), .clear],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )

                    LineMark(
                        x: .value("Check", pt.index),
                        y: .value("Intensity", pt.log.intensity)
                    )
                    .lineStyle(StrokeStyle(lineWidth: 2.5, lineCap: .round, join: .round))
                    .foregroundStyle(Color.accentColor)

                    PointMark(
                        x: .value("Check", pt.index),
                        y: .value("Intensity", pt.log.intensity)
                    )
                    .symbolSize(selectedChartIndex == pt.index ? 110 : 65)
                    .foregroundStyle(Color(hex: pt.log.color))
                }
                .chartYScale(domain: 1...10)
                .chartXScale(domain: 0...max(chartPoints.count - 1, 1))
                .chartYAxis {
                    AxisMarks(values: [1, 5, 10]) { value in
                        AxisGridLine(stroke: StrokeStyle(lineWidth: 0.8, dash: [3, 3]))
                        AxisValueLabel()
                    }
                }
                .chartXAxis {
                    AxisMarks(values: chartPoints.map(\.index)) { value in
                        if let idx = value.as(Int.self), chartPoints.indices.contains(idx) {
                            AxisValueLabel {
                                Text(chartPoints[idx].dateLabel)
                                    .font(.system(size: 10, weight: .medium))
                            }
                        }
                    }
                }
                .chartXSelection(value: $selectedChartIndex)
                .frame(height: 165)

                if let active = selectedChartPoint {
                    let parsed = IsoTimestamp.parseUtc(active.log.timestamp)
                    let shortDate = parsed.map {
                        let f = DateFormatter()
                        f.locale = Locale(identifier: "en_US")
                        f.dateFormat = "MMM d"
                        return f.string(from: $0)
                    } ?? ""

                    HStack {
                        HStack(spacing: 8) {
                            Circle()
                                .fill(Color(hex: active.log.color))
                                .frame(width: 8, height: 8)
                            Text(active.log.primaryLabel)
                                .font(.system(size: 13, weight: .semibold))
                            Text("(\(active.log.core))")
                                .font(.system(size: 12))
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Text("\(active.log.intensity)/10 · \(shortDate)")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(Color.accentColor)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Color(.systemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
            }
        }
        .padding(16)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private var weekCalendarCard: some View {
        let today = Date()
        let cal = Calendar.current

        return VStack(spacing: 12) {
            HStack {
                Button {
                    weekOffset -= 1
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(.secondary)
                        .frame(width: 32, height: 32)
                }

                Spacer()

                Text("Daily Summary")
                    .font(.system(size: 15, weight: .semibold))

                Spacer()

                Button {
                    weekOffset += 1
                } label: {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(.secondary)
                        .frame(width: 32, height: 32)
                }
            }

            HStack(spacing: 0) {
                ForEach(0..<7, id: \.self) { index in
                    let dayDate = weekDates[index]
                    let isSelected = index == selectedDayOffset
                    let isToday = cal.isDate(dayDate, inSameDayAs: today)
                    let dominant = findDominantEmotion(in: store.logs, forDay: dayDate)

                    Button {
                        selectedDayOffset = index
                    } label: {
                        VStack(spacing: 6) {
                            Text(dayNames[index])
                                .font(.system(size: 11, weight: .medium))
                                .foregroundStyle(isToday ? Color.accentColor : .secondary)

                            ZStack {
                                Circle()
                                    .fill(isSelected ? Color.accentColor : .clear)
                                    .frame(width: 34, height: 34)
                                Text("\(cal.component(.day, from: dayDate))")
                                    .font(.system(size: 13, weight: isSelected ? .bold : .regular))
                                    .foregroundStyle(isSelected ? .white : .primary)
                            }

                            Circle()
                                .fill(dominant.map { Color(hex: $0.color) } ?? .clear)
                                .frame(width: 6, height: 6)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                        .background(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .fill(isSelected ? Color.accentColor.opacity(0.1) : .clear)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(16)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private var dailyLogsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(displayDateString)
                .font(.system(size: 16, weight: .semibold))

            if selectedDayLogs.isEmpty {
                Text("No logs recorded for this day.")
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(24)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            } else {
                ForEach(selectedDayLogs) { log in
                    DailyLogCard(log: log)
                }
            }
        }
    }

    private func computeWeekDates(weekOffset: Int) -> [Date] {
        let cal = Calendar.current
        let today = cal.startOfDay(for: Date())
        let weekday = cal.component(.weekday, from: today) // 1 = Sunday
        let daysSinceSunday = (weekday - 1 + 7) % 7
        guard let sunday = cal.date(byAdding: .day, value: -daysSinceSunday + weekOffset * 7, to: today) else {
            return [today]
        }
        return (0..<7).compactMap { cal.date(byAdding: .day, value: $0, to: sunday) }
    }

    private func filterLogs(_ logs: [LogEntry], forDay date: Date) -> [LogEntry] {
        let cal = Calendar.current
        return logs
            .filter { entry in
                guard let parsed = IsoTimestamp.parseUtc(entry.timestamp) else { return false }
                return cal.isDate(parsed, inSameDayAs: date)
            }
            .sorted {
                (IsoTimestamp.parseUtc($0.timestamp) ?? .distantPast)
                    > (IsoTimestamp.parseUtc($1.timestamp) ?? .distantPast)
            }
    }

    private func findDominantEmotion(in logs: [LogEntry], forDay date: Date) -> LogEntry? {
        let dayLogs = filterLogs(logs, forDay: date)
        guard !dayLogs.isEmpty else { return nil }

        var counts: [String: Int] = [:]
        for entry in dayLogs {
            counts[entry.core, default: 0] += 1
        }

        var bestCore = dayLogs[0].core
        var bestCount = 0
        for entry in dayLogs {
            let c = counts[entry.core, default: 0]
            if c > bestCount {
                bestCount = c
                bestCore = entry.core
            }
        }
        return dayLogs.first { $0.core == bestCore } ?? dayLogs.first
    }
}

private struct DailyLogCard: View {
    let log: LogEntry

    private var timeString: String {
        guard let parsed = IsoTimestamp.parseUtc(log.timestamp) else { return "" }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US")
        formatter.dateFormat = "h:mm a"
        return formatter.string(from: parsed)
    }

    var body: some View {
        HStack(spacing: 0) {
            Rectangle()
                .fill(Color(hex: log.color))
                .frame(width: 6)

            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(timeString)
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text("Intensity: \(log.intensity)")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Color.accentColor)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Color.accentColor.opacity(0.14))
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                }

                HStack(alignment: .firstTextBaseline, spacing: 5) {
                    Text(log.primaryLabel)
                        .font(.system(size: 15, weight: .bold))
                    Text("(\(log.secondaryLabel))")
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                }

                if !log.note.isEmpty {
                    Text("\"\(log.note)\"")
                        .font(.system(size: 13))
                        .italic()
                        .foregroundStyle(.secondary)
                        .padding(8)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .overlay(
                            RoundedRectangle(cornerRadius: 6, style: .continuous)
                                .stroke(Color.secondary.opacity(0.25), lineWidth: 1)
                        )
                }
            }
            .padding(14)
        }
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}
