package com.example.feelingsapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feelingsapp.data.IsoTimestamp
import com.example.feelingsapp.data.LogEntry
import com.example.feelingsapp.data.parseColorSafe
import java.text.SimpleDateFormat
import java.util.*

private fun computeWeekDates(weekOffset: Int): List<Date> {
    val cal = Calendar.getInstance()
    val daysSinceSunday = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
    cal.add(Calendar.DATE, -daysSinceSunday + weekOffset * 7)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val list = mutableListOf<Date>()
    for (i in 0..6) {
        list.add(cal.time)
        cal.add(Calendar.DATE, 1)
    }
    return list
}

private fun filterLogsForDay(logs: List<LogEntry>, date: Date): List<LogEntry> {
    val matchCal = Calendar.getInstance().apply { time = date }
    return logs.filter { log ->
        val parsed = IsoTimestamp.parseUtc(log.timestamp) ?: return@filter false
        val logCal = Calendar.getInstance().apply { time = parsed }
        logCal.get(Calendar.DATE) == matchCal.get(Calendar.DATE) &&
            logCal.get(Calendar.MONTH) == matchCal.get(Calendar.MONTH) &&
            logCal.get(Calendar.YEAR) == matchCal.get(Calendar.YEAR)
    }.sortedByDescending { IsoTimestamp.parseUtc(it.timestamp)?.time ?: 0L }
}

private fun findDominantEmotionForDay(logs: List<LogEntry>, date: Date): LogEntry? {
    val dayLogs = filterLogsForDay(logs, date) // sorted newest-first
    if (dayLogs.isEmpty()) return null

    val counts = mutableMapOf<String, Int>()
    for (log in dayLogs) {
        counts[log.core] = (counts[log.core] ?: 0) + 1
    }

    var bestCore = dayLogs.first().core
    var bestCount = 0
    for (log in dayLogs) {
        val count = counts[log.core] ?: 0
        if (count > bestCount) {
            bestCount = count
            bestCore = log.core
        }
    }

    return dayLogs.firstOrNull { it.core == bestCore } ?: dayLogs.first()
}

@Composable
fun TrendsScreen(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    val todayDayOffset = remember {
        (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
    }
    var selectedDayOffset by rememberSaveable { mutableStateOf(todayDayOffset) }
    var weekOffset by rememberSaveable { mutableStateOf(0) }

    val weekDates = remember(weekOffset) { computeWeekDates(weekOffset) }
    val dayNames = listOf("S", "M", "T", "W", "Th", "F", "S")

    val selectedDate = weekDates.getOrElse(selectedDayOffset) { weekDates.first() }
    val selectedDayLogs = remember(logs, selectedDate) {
        filterLogsForDay(logs, selectedDate)
    }

    val sortedGraphLogs = remember(logs) {
        logs.sortedBy { IsoTimestamp.parseUtc(it.timestamp)?.time ?: 0L }.takeLast(7)
    }

    val todayCal = Calendar.getInstance()
    val displayDateString = remember(selectedDate) {
        SimpleDateFormat("EEEE, MMM d", Locale.US).format(selectedDate)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text("Trends", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Observe the patterns in your emotions over time.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Section 1: Line Graph
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Intensity Trend", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Last 7 checks", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (sortedGraphLogs.size < 2) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        ) {
                            Text(
                                "Log feelings multiple times to view trends graph.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        val accentColor = MaterialTheme.colorScheme.primary

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            val hPad = 14.dp.toPx()
                            val vPad = 14.dp.toPx()
                            val graphWidth = (size.width - hPad * 2).coerceAtLeast(1f)
                            val graphHeight = (size.height - vPad * 2).coerceAtLeast(1f)

                            val getX = { index: Int ->
                                hPad + (index * graphWidth / (sortedGraphLogs.size - 1))
                            }
                            val getY = { intensity: Int ->
                                val clamped = intensity.coerceIn(1, 10)
                                size.height - vPad - ((clamped - 1) * graphHeight / 9f)
                            }

                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                start = Offset(hPad, vPad),
                                end = Offset(size.width - hPad, vPad),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                start = Offset(hPad, size.height / 2),
                                end = Offset(size.width - hPad, size.height / 2),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                start = Offset(hPad, size.height - vPad),
                                end = Offset(size.width - hPad, size.height - vPad),
                                strokeWidth = 1.5.dp.toPx()
                            )

                            val linePath = Path()
                            val fillPath = Path()

                            val firstX = getX(0)
                            val firstY = getY(sortedGraphLogs[0].intensity)

                            linePath.moveTo(firstX, firstY)
                            fillPath.moveTo(firstX, firstY)

                            for (i in 1 until sortedGraphLogs.size) {
                                val x = getX(i)
                                val y = getY(sortedGraphLogs[i].intensity)
                                linePath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }

                            fillPath.lineTo(getX(sortedGraphLogs.size - 1), size.height - vPad)
                            fillPath.lineTo(firstX, size.height - vPad)
                            fillPath.close()

                            drawPath(
                                path = fillPath,
                                brush = Brush.linearGradient(
                                    colors = listOf(accentColor.copy(alpha = 0.2f), Color.Transparent),
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height)
                                )
                            )

                            drawPath(
                                path = linePath,
                                color = accentColor,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            sortedGraphLogs.forEachIndexed { index, log ->
                                val x = getX(index)
                                val y = getY(log.intensity)
                                val color = parseColorSafe(log.color, fallback = accentColor)

                                drawCircle(
                                    color = color,
                                    radius = 5.dp.toPx(),
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.5.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            sortedGraphLogs.forEach { log ->
                                val parsed = IsoTimestamp.parseUtc(log.timestamp)
                                val label = if (parsed != null) {
                                    SimpleDateFormat("M/d", Locale.US).format(parsed)
                                } else {
                                    ""
                                }
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Week Calendar
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("By Day", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (weekOffset != 0) {
                                TextButton(
                                    onClick = {
                                        weekOffset = 0
                                        selectedDayOffset = todayDayOffset
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Today", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            OutlinedButton(
                                onClick = { weekOffset -= 1 },
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("<", fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = { if (weekOffset < 0) weekOffset += 1 },
                                enabled = weekOffset < 0,
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text(">", fontSize = 13.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekDates.forEachIndexed { index, date ->
                            val isSelected = selectedDayOffset == index
                            val dominantLog = findDominantEmotionForDay(logs, date)

                            val cal = Calendar.getInstance().apply { time = date }
                            val dayNum = cal.get(Calendar.DATE)
                            val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                                cal.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH) &&
                                cal.get(Calendar.DATE) == todayCal.get(Calendar.DATE)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .then(
                                        if (isToday) {
                                            Modifier.border(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(12.dp)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { selectedDayOffset = index }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = dayNames[index],
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 14.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (dominantLog != null) parseColorSafe(dominantLog.color)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Selected day header
        item {
            Text(
                text = displayDateString.uppercase(Locale.US),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (selectedDayLogs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text("No entries for this day.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(selectedDayLogs, key = { it.id }) { log ->
                val timeString = remember(log.timestamp) {
                    val parsed = IsoTimestamp.parseUtc(log.timestamp) ?: Date()
                    SimpleDateFormat("hh:mm a", Locale.US).format(parsed)
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight()
                                .background(parseColorSafe(log.color))
                        )

                        Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(timeString, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Intensity: ${log.intensity}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            val primaryLabel = log.sub2.ifEmpty { log.sub1 }
                            val secondaryLabel = if (log.sub2.isNotEmpty()) log.sub1 else log.core
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(primaryLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("($secondaryLabel)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (log.note.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "\"${log.note}\"",
                                    fontSize = 13.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
