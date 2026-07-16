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
import com.example.feelingsapp.data.LogEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrendsScreen(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    var selectedDayOffset by remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1) } // 0-6 (Sun-Sat)

    // Get current week dates
    val weekDates = remember {
        val current = Calendar.getInstance()
        current.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val list = mutableListOf<Date>()
        for (i in 0..6) {
            list.add(current.time)
            current.add(Calendar.DATE, 1)
        }
        list
    }

    val dayNames = listOf("S", "M", "T", "W", "Th", "F", "S")

    // Filter logs for selected day
    val getLogsForDay = { date: Date ->
        logs.filter { log ->
            val logCal = Calendar.getInstance().apply { time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(log.timestamp) ?: Date() }
            val matchCal = Calendar.getInstance().apply { time = date }
            logCal.get(Calendar.DATE) == matchCal.get(Calendar.DATE) &&
            logCal.get(Calendar.MONTH) == matchCal.get(Calendar.MONTH) &&
            logCal.get(Calendar.YEAR) == matchCal.get(Calendar.YEAR)
        }
    }

    val selectedDate = weekDates[selectedDayOffset]
    val selectedDayLogs = getLogsForDay(selectedDate)

    // Prepare graph logs (chronological last 7 logs)
    val sortedGraphLogs = remember(logs) {
        logs.sortedBy { it.timestamp }.takeLast(7)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Trends", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Observe the patterns in your emotions over time.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Section 1: Line Graph
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
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
                            "Log feelings on multiple days to view trends graph.",
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
                        val padding = 40f
                        val graphWidth = size.width - padding * 2
                        val graphHeight = size.height - padding * 2

                        // Scale math
                        val getX = { index: Int ->
                            padding + (index * graphWidth / (sortedGraphLogs.size - 1))
                        }
                        val getY = { intensity: Int ->
                            size.height - padding - ((intensity - 1) * graphHeight / 9f)
                        }

                        // Grid lines
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(padding, padding),
                            end = Offset(size.width - padding, padding),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(padding, size.height / 2),
                            end = Offset(size.width - padding, size.height / 2),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(padding, size.height - padding),
                            end = Offset(size.width - padding, size.height - padding),
                            strokeWidth = 3f
                        )

                        // Build line path & fill path
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

                        fillPath.lineTo(getX(sortedGraphLogs.size - 1), size.height - padding)
                        fillPath.lineTo(firstX, size.height - padding)
                        fillPath.close()

                        // Draw fill gradient
                        drawPath(
                            path = fillPath,
                            brush = Brush.linearGradient(
                                colors = listOf(accentColor.copy(alpha = 0.2f), Color.Transparent),
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height)
                            )
                        )

                        // Draw path line
                        drawPath(
                            path = linePath,
                            color = accentColor,
                            style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Draw dots & dates text
                        sortedGraphLogs.forEachIndexed { index, log ->
                            val x = getX(index)
                            val y = getY(log.intensity)
                            val color = Color(android.graphics.Color.parseColor(log.color))

                            drawCircle(
                                color = color,
                                radius = 10f,
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 5f,
                                center = Offset(x, y)
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Week Calendar
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("By Day", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDates.forEachIndexed { index, date ->
                        val isSelected = selectedDayOffset == index
                        val dominantLog = getLogsForDay(date).lastOrNull()

                        val cal = Calendar.getInstance().apply { time = date }
                        val dayNum = cal.get(Calendar.DATE)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .clickable { selectedDayOffset = index }
                                .padding(vertical = 8.dp, horizontal = 6.dp)
                                .weight(1f)
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
                                        if (dominantLog != null) Color(android.graphics.Color.parseColor(dominantLog.color))
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Section 3: History list for selected day
        val displayDateString = remember(selectedDayOffset) {
            SimpleDateFormat("EEEE, MMM d", Locale.US).format(selectedDate)
        }
        Text(
            text = displayDateString,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (selectedDayLogs.isEmpty()) {
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
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(selectedDayLogs) { log ->
                    val timeString = remember(log.timestamp) {
                        val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(log.timestamp) ?: Date()
                        SimpleDateFormat("hh:mm a", Locale.US).format(parsed)
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Color stripe indicator
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .fillMaxHeight()
                                    .align(Alignment.CenterVertically)
                                    .background(Color(android.graphics.Color.parseColor(log.color)))
                            )

                            Column(modifier = Modifier.padding(16.dp)) {
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

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(log.sub2, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("(${log.sub1})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}
