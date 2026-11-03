package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DailyThoughtCard
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PriorityHighRed
import com.example.ui.theme.PriorityLowGreen
import com.example.ui.theme.Secondary
import com.example.ui.theme.TertiaryOrange
import com.example.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun getTaskTimestamp(task: com.example.data.model.TaskEntity): Long {
    if (task.completedTimestamp != null && task.completedTimestamp > 0) {
        return task.completedTimestamp
    }
    if (task.date.isNotBlank()) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val d = sdf.parse(task.date)
            if (d != null) return d.time
        } catch (_: Exception) {}
    }
    return 0L
}

private data class PeriodBoundaries(
    val startMillis: Long,
    val endMillis: Long
)

private fun getPeriodBoundaries(period: String): PeriodBoundaries {
    val cal = Calendar.getInstance()
    val now = cal.timeInMillis

    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    return when (period) {
        "7 Days" -> {
            cal.add(Calendar.DAY_OF_YEAR, -6)
            PeriodBoundaries(startMillis = cal.timeInMillis, endMillis = now)
        }
        "30 Days" -> {
            cal.add(Calendar.DAY_OF_YEAR, -29)
            PeriodBoundaries(startMillis = cal.timeInMillis, endMillis = now)
        }
        "Year" -> {
            cal.set(Calendar.DAY_OF_YEAR, 1) // Jan 1 of current year
            PeriodBoundaries(startMillis = cal.timeInMillis, endMillis = now)
        }
        else -> PeriodBoundaries(0L, now)
    }
}

private data class BarChartData(
    val title: String,
    val subtitle: String,
    val labels: List<String>,
    val counts: IntArray,
    val peakLabel: String
)

@Composable
fun InsightsScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val focusSessions by viewModel.focusSessions.collectAsStateWithLifecycle()
    val streakDays by viewModel.streakDays.collectAsStateWithLifecycle()

    var selectedPeriodFilter by remember { mutableStateOf("30 Days") } // "7 Days", "30 Days", "Year"

    val boundaries = remember(selectedPeriodFilter) {
        getPeriodBoundaries(selectedPeriodFilter)
    }

    // Filter tasks and focus sessions according to selected period
    val periodTasks = remember(tasks, boundaries) {
        if (boundaries.startMillis <= 0L) tasks
        else tasks.filter { task ->
            val ts = getTaskTimestamp(task)
            ts >= boundaries.startMillis || (ts == 0L && task.isCompleted)
        }
    }

    val periodFocusSessions = remember(focusSessions, boundaries) {
        if (boundaries.startMillis <= 0L) focusSessions
        else focusSessions.filter { it.timestamp >= boundaries.startMillis }
    }

    val completedTasks = periodTasks.count { it.isCompleted }
    val totalTasks = periodTasks.size
    val completionPct = if (totalTasks > 0) ((completedTasks.toFloat() / totalTasks.toFloat()) * 100).toInt() else 0

    // Focus Time calculation for selected period
    val sessionFocusMinutes = periodFocusSessions.sumOf { it.durationMinutes }
    val taskFocusMinutes = periodTasks.filter { it.isCompleted }.sumOf { if (it.estimatedDurationMinutes > 0) it.estimatedDurationMinutes else 25 }
    val totalFocusMins = sessionFocusMinutes + taskFocusMinutes
    val focusHours = totalFocusMins / 60
    val focusMinsRem = totalFocusMins % 60
    val focusTimeText = if (focusHours > 0) "${focusHours}h ${focusMinsRem}m" else "${focusMinsRem}m"

    // Dynamic Bar Chart Data for selected period ("7 Days", "30 Days", "Year")
    val barChartData = remember(periodTasks, tasks, selectedPeriodFilter, boundaries) {
        when (selectedPeriodFilter) {
            "7 Days" -> {
                val labels = mutableListOf<String>()
                val counts = IntArray(7) { 0 }
                val dayCal = Calendar.getInstance()
                dayCal.timeInMillis = boundaries.startMillis
                val sdfDay = SimpleDateFormat("EEE", Locale.getDefault())

                for (i in 0 until 7) {
                    labels.add(sdfDay.format(dayCal.time))
                    val dayStart = dayCal.timeInMillis
                    dayCal.add(Calendar.DAY_OF_YEAR, 1)
                    val dayEnd = dayCal.timeInMillis

                    counts[i] = tasks.count { task ->
                        task.isCompleted && getTaskTimestamp(task) in dayStart until dayEnd
                    }
                }

                val maxVal = counts.maxOrNull() ?: 0
                val peakIdx = counts.indexOfFirst { it == maxVal }
                val peakDay = if (maxVal > 0 && peakIdx in labels.indices) labels[peakIdx] else labels.lastOrNull() ?: ""

                BarChartData(
                    title = "Weekly Activity Velocity",
                    subtitle = "Tasks completed per day (Last 7 Days)",
                    labels = labels,
                    counts = counts,
                    peakLabel = if (peakDay.isNotBlank()) "🔥 PEAK: $peakDay" else "🔥 ACTIVE"
                )
            }
            "30 Days" -> {
                val labels = listOf("Wk 1", "Wk 2", "Wk 3", "Wk 4", "Wk 5", "This Wk")
                val counts = IntArray(6) { 0 }
                val totalSpan = (boundaries.endMillis - boundaries.startMillis).coerceAtLeast(1L)
                val bucketSpan = totalSpan / 6.0

                tasks.filter { it.isCompleted }.forEach { task ->
                    val ts = getTaskTimestamp(task)
                    if (ts >= boundaries.startMillis && ts <= boundaries.endMillis) {
                        val b = ((ts - boundaries.startMillis) / bucketSpan).toInt().coerceIn(0, 5)
                        counts[b]++
                    }
                }

                val maxVal = counts.maxOrNull() ?: 0
                val peakIdx = counts.indexOfFirst { it == maxVal }
                val peakWk = if (maxVal > 0 && peakIdx in labels.indices) labels[peakIdx] else "This Wk"

                BarChartData(
                    title = "30-Day Completion Velocity",
                    subtitle = "Tasks completed per 5-day block",
                    labels = labels,
                    counts = counts,
                    peakLabel = "🔥 PEAK: $peakWk"
                )
            }
            else -> { // "Year"
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val counts = IntArray(12) { 0 }
                val calTask = Calendar.getInstance()

                tasks.filter { it.isCompleted }.forEach { task ->
                    val ts = getTaskTimestamp(task)
                    if (ts >= boundaries.startMillis && ts <= boundaries.endMillis) {
                        calTask.timeInMillis = ts
                        val m = calTask.get(Calendar.MONTH).coerceIn(0, 11)
                        counts[m]++
                    }
                }

                val maxVal = counts.maxOrNull() ?: 0
                val peakIdx = counts.indexOfFirst { it == maxVal }
                val peakMonth = if (maxVal > 0 && peakIdx in monthNames.indices) monthNames[peakIdx] else "This Month"

                BarChartData(
                    title = "Yearly Completion Velocity",
                    subtitle = "Tasks completed per month",
                    labels = monthNames,
                    counts = counts,
                    peakLabel = "🔥 PEAK: $peakMonth"
                )
            }
        }
    }

    // Period trend counts for bezier curve chart
    val trendCounts = remember(periodTasks, selectedPeriodFilter, boundaries) {
        val numBuckets = if (selectedPeriodFilter == "Year") 12 else 7
        val counts = IntArray(numBuckets) { 0 }
        val range = (boundaries.endMillis - boundaries.startMillis).coerceAtLeast(1L)
        val bucketSpan = range.toDouble() / numBuckets.toDouble()

        periodTasks.filter { it.isCompleted }.forEach { task ->
            val ts = getTaskTimestamp(task)
            if (ts >= boundaries.startMillis && ts <= boundaries.endMillis) {
                val bucket = ((ts - boundaries.startMillis) / bucketSpan).toInt().coerceIn(0, numBuckets - 1)
                counts[bucket]++
            } else if (ts > boundaries.endMillis) {
                counts[numBuckets - 1]++
            }
        }
        counts
    }

    // Time of Day distribution
    val timeOfDayMap = remember(periodTasks) {
        periodTasks.filter { it.isCompleted }.groupBy {
            when (it.timeOfDay.lowercase()) {
                "morning" -> "Morning"
                "afternoon" -> "Afternoon"
                "evening" -> "Evening"
                else -> "Anytime"
            }
        }.mapValues { it.value.size }
    }

    val morningCount = timeOfDayMap["Morning"] ?: 0
    val afternoonCount = timeOfDayMap["Afternoon"] ?: 0
    val eveningCount = timeOfDayMap["Evening"] ?: 0
    val anytimeCount = timeOfDayMap["Anytime"] ?: 0

    // Dynamic AI Recommendation Tip based on real peak energy time
    val timeOfDayCounts = listOf(
        "Morning" to morningCount,
        "Afternoon" to afternoonCount,
        "Evening" to eveningCount,
        "Anytime" to anytimeCount
    )
    val totalCompletedInPeriod = timeOfDayCounts.sumOf { it.second }
    val peakTime = timeOfDayCounts.maxByOrNull { it.second } ?: ("Morning" to 0)
    val peakPct = if (totalCompletedInPeriod > 0) ((peakTime.second.toFloat() / totalCompletedInPeriod.toFloat()) * 100).toInt() else 0

    val tipText = if (totalCompletedInPeriod > 0) {
        when (peakTime.first) {
            "Morning" -> "💡 Tip: You complete $peakPct% of your tasks during Morning hours. Schedule high-priority goals before 12 PM for maximum momentum!"
            "Afternoon" -> "💡 Tip: You complete $peakPct% of your tasks during Afternoon hours (12 PM - 6 PM). Keep up your post-lunch focus momentum!"
            "Evening" -> "💡 Tip: You complete $peakPct% of your tasks during Evening hours. High night-owl productivity detected!"
            else -> "💡 Tip: You complete $peakPct% of your tasks throughout the day. Consistent execution across all hours!"
        }
    } else {
        "💡 Tip: Complete tasks throughout the day to unlock personalized peak productivity energy insights!"
    }

    // Project breakdown calculation with Top 5 + "Other" bucket
    val rawProjectEntries = remember(periodTasks) {
        periodTasks.groupBy { it.project.ifBlank { "General Tasks" } }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .map { Pair(it.key, it.value) }
    }

    val displayProjectEntries = remember(rawProjectEntries) {
        if (rawProjectEntries.size <= 5) {
            rawProjectEntries
        } else {
            val top5 = rawProjectEntries.take(5)
            val otherSum = rawProjectEntries.drop(5).sumOf { it.second }
            top5 + Pair("Other", otherSum)
        }
    }

    val projectChartColors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFFF97316), // Orange
        Color(0xFF10B981), // Emerald
        Color(0xFFEC4899), // Pink
        Color(0xFF8B5CF6), // Purple
        Color(0xFF64748B)  // Slate for Other
    )

    val isDark = MaterialTheme.colorScheme.surface.red < 0.3f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Productivity Analytics",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "Real-time trends & focus metrics",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Filter Pills
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(3.dp)
                    ) {
                        listOf("7 Days", "30 Days", "Year").forEach { period ->
                            val isSelected = selectedPeriodFilter == period
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) PrimaryIndigo else Color.Transparent)
                                    .clickable { selectedPeriodFilter = period }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = period,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }

        // 365 Daily Thought Warm System Spotlight
        item {
            DailyThoughtCard(compactMode = true)
        }

        // Stats Grid Bento (Completion Rate, Focus Time, Streak, Level)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Completion Rate Large Card with Ring Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isDark) Color(0xFF1E2230)
                            else Color(0xFFF0F4FF)
                        )
                        .border(
                            1.2.dp,
                            PrimaryIndigo.copy(alpha = 0.3f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryIndigo.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "TARGET ACCURACY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigo,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$completionPct%",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryIndigo
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "🎯 $completedTasks of $totalTasks tasks finished",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Ring Chart
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { (completionPct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.size(72.dp),
                                color = PrimaryIndigo,
                                trackColor = PrimaryIndigo.copy(alpha = 0.15f),
                                strokeWidth = 8.dp
                            )
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Checked",
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Two side-by-side Bento Cards (Focus Time + Streak)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Focus Time Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isDark) Color(0xFF1A2624)
                                else Color(0xFFECFDF5)
                            )
                            .border(1.2.dp, Color(0xFF6EE7B7), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = "Focus",
                                    tint = PriorityLowGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Total Focus",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                            }
                            Text(
                                text = focusTimeText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Current Streak Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isDark) Color(0xFF332117)
                                else Color(0xFFFFF7ED)
                            )
                            .border(1.2.dp, Color(0xFFFDBA74), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = TertiaryOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Streak Record",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9A3412)
                                )
                            }
                            Text(
                                text = "$streakDays Days",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // CHART 1: Activity Velocity Bar Chart (Custom Canvas)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.BarChart,
                                    contentDescription = "Activity Chart",
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = barChartData.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = barChartData.subtitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEEF2FF))
                                .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = barChartData.peakLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryIndigo
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    WeeklyVelocityBarChart(
                        dayCounts = barChartData.counts,
                        daysLabels = barChartData.labels
                    )
                }
            }
        }

        // CHART 2: Project Breakdown Donut / Ring Chart
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.PieChart,
                            contentDescription = "Donut Chart",
                            tint = Color(0xFFEC4899),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Project Focus Distribution",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (displayProjectEntries.isEmpty()) {
                        Text(
                            text = "No project tasks logged yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Donut Canvas
                            DonutChartCanvas(
                                projectEntries = displayProjectEntries,
                                colors = projectChartColors,
                                totalTasks = totalTasks,
                                modifier = Modifier.size(140.dp)
                            )

                            // Legend breakdown list
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                displayProjectEntries.forEachIndexed { index, entry ->
                                    val color = projectChartColors.getOrElse(index) { projectChartColors[index % projectChartColors.size] }
                                    val pct = if (totalTasks > 0) ((entry.second.toFloat() / totalTasks.toFloat()) * 100).toInt() else 0

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = entry.first,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                        }

                                        Text(
                                            text = "$pct% (${entry.second})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CHART 3: Period Focus Velocity Curve (Bezier Line Chart)
        item {
            val trendTitle = when (selectedPeriodFilter) {
                "7 Days" -> "7-Day Velocity Trend"
                "Year" -> "Yearly Focus Trend"
                else -> "Monthly Focus Trend"
            }
            val trendBadge = when (selectedPeriodFilter) {
                "7 Days" -> "7-Day Curve"
                "Year" -> "Yearly Curve"
                else -> "30-Day Curve"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.ShowChart,
                                contentDescription = "Trend Line",
                                tint = Secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = trendTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = trendBadge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FocusTrendCurveChart(
                        dayCounts = trendCounts,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )
                }
            }
        }

        // CHART 4: Time of Day Energy Distribution
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = "Time of Day",
                            tint = TertiaryOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Time-of-Day Peak Energy",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val grandTotal = (morningCount + afternoonCount + eveningCount + anytimeCount).coerceAtLeast(1)

                    TimeOfDayBarItem(
                        label = "Morning (6 AM - 12 PM)",
                        icon = Icons.Filled.WbSunny,
                        count = morningCount,
                        total = grandTotal,
                        barColor = Color(0xFFD97706),
                        bgColor = Color(0xFFFFFBEB)
                    )

                    TimeOfDayBarItem(
                        label = "Afternoon (12 PM - 6 PM)",
                        icon = Icons.Filled.LightMode,
                        count = afternoonCount,
                        total = grandTotal,
                        barColor = Color(0xFFEA580C),
                        bgColor = Color(0xFFFFF7ED)
                    )

                    TimeOfDayBarItem(
                        label = "Evening (6 PM - 12 AM)",
                        icon = Icons.Filled.NightsStay,
                        count = eveningCount,
                        total = grandTotal,
                        barColor = Color(0xFF7C3AED),
                        bgColor = Color(0xFFF5F3FF)
                    )
                }
            }
        }

        // AI Recommendations & Level Badge Banner
        item {
            val badgeTitle = when {
                completedTasks >= 15 -> "Level 5 • Master Achiever"
                completedTasks >= 5 -> "Level 3 • Focus Practitioner"
                else -> "Level 1 • Task Explorer"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4F46E5),
                                Color(0xFF6366F1),
                                Color(0xFF4338CA)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "AI Level",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = badgeTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Personalized focus recommendation active",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Text(
                        text = tipText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.95f),
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ---------------------- CUSTOM CANVAS CHARTS ----------------------

@Composable
private fun WeeklyVelocityBarChart(
    dayCounts: IntArray,
    daysLabels: List<String>
) {
    val maxCount = maxOf(dayCounts.maxOrNull() ?: 1, 1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        dayCounts.forEachIndexed { idx, count ->
            val heightFrac = if (count > 0) (count.toFloat() / maxCount.toFloat()).coerceIn(0.2f, 1f) else 0.08f
            val isPeak = count > 0 && count == maxCount

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom
            ) {
                if (count > 0) {
                    Text(
                        text = "$count",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPeak) PrimaryIndigo else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .fillMaxHeight(heightFrac)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            if (isPeak) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        PrimaryIndigo,
                                        Color(0xFF818CF8)
                                    )
                                )
                            } else if (count > 0) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF6366F1).copy(alpha = 0.8f),
                                        Color(0xFFA5B4FC).copy(alpha = 0.6f)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = daysLabels.getOrElse(idx) { "" },
                    fontSize = 11.sp,
                    fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium,
                    color = if (isPeak) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DonutChartCanvas(
    projectEntries: List<Pair<String, Int>>,
    colors: List<Color>,
    totalTasks: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 24.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
        val arcSize = Size(diameter, diameter)

        var startAngle = -90f

        val safeTotal = if (totalTasks > 0) totalTasks.toFloat() else 1f

        projectEntries.forEachIndexed { index, entry ->
            val sweepAngle = (entry.second.toFloat() / safeTotal) * 360f
            val color = colors.getOrElse(index) { colors[index % colors.size] }

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = (sweepAngle - 2f).coerceAtLeast(0.1f), // gap between slices
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            startAngle += sweepAngle
        }
    }
}

@Composable
private fun FocusTrendCurveChart(
    dayCounts: IntArray,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val maxVal = maxOf(dayCounts.maxOrNull() ?: 1, 1).toFloat()

        val points = mutableListOf<Offset>()
        val stepX = w / (dayCounts.size - 1).coerceAtLeast(1).toFloat()

        dayCounts.forEachIndexed { idx, count ->
            val x = idx * stepX
            val y = h - ((count.toFloat() / maxVal) * (h * 0.75f)) - (h * 0.1f)
            points.add(Offset(x, y))
        }

        // Draw smooth path
        val path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points[0].x, points[0].y)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val cx1 = p1.x + (p2.x - p1.x) / 2
                    val cy1 = p1.y
                    val cx2 = p1.x + (p2.x - p1.x) / 2
                    val cy2 = p2.y
                    cubicTo(cx1, cy1, cx2, cy2, p2.x, p2.y)
                }
            }
        }

        // Gradient Fill Path
        val fillPath = Path().apply {
            addPath(path)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Secondary.copy(alpha = 0.35f),
                    Color.Transparent
                )
            )
        )

        // Draw Line Curve
        drawPath(
            path = path,
            color = Secondary,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Glowing Key Points
        points.forEach { pt ->
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Secondary,
                radius = 3.dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
private fun TimeOfDayBarItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    total: Int,
    barColor: Color,
    bgColor: Color
) {
    val pct = if (total > 0) ((count.toFloat() / total.toFloat()) * 100).toInt() else 0

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = barColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "$pct% ($count tasks)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }

        LinearProgressIndicator(
            progress = { (pct / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = bgColor
        )
    }
}
