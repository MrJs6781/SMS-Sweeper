package app.smssweeper

import android.icu.util.Calendar
import android.icu.util.ULocale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val JALALI_MONTHS = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
)

/** A fresh ICU calendar using the Persian (Solar Hijri) system, in the default time zone. */
fun persianCal(): Calendar = Calendar.getInstance(ULocale("fa_IR@calendar=persian"))

private fun cal(y: Int, m: Int, d: Int) = persianCal().apply { clear(); set(y, m - 1, d) }

/** (year, month 1..12, day) of [ms] in the Persian calendar. */
fun jalaliParts(ms: Long): Triple<Int, Int, Int> {
    val c = persianCal().apply { timeInMillis = ms }
    return Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

/** Local midnight of the given Persian date, as epoch millis. */
fun fromJalali(y: Int, m: Int, d: Int): Long = cal(y, m, d).timeInMillis

private fun daysIn(y: Int, m: Int) = when {
    m <= 6 -> 31
    m <= 11 -> 30
    else -> cal(y, 12, 1).getActualMaximum(Calendar.DAY_OF_MONTH)
}

@Composable
fun JalaliPickerDialog(initial: Long?, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val start = remember { jalaliParts(initial ?: System.currentTimeMillis()) }
    var y by remember { mutableIntStateOf(start.first) }
    var m by remember { mutableIntStateOf(start.second) }
    var d by remember { mutableIntStateOf(start.third) }
    val dim = daysIn(y, m)
    val day = d.coerceAtMost(dim)
    // Persian weeks start on Saturday
    val offset = (cal(y, m, 1).get(Calendar.DAY_OF_WEEK) - Calendar.SATURDAY + 7) % 7

    fun shiftMonth(delta: Int) {
        val idx = y * 12 + (m - 1) + delta
        y = idx / 12
        m = idx % 12 + 1
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب تاریخ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton({ y-- }) { Text("‹") }
                    Text("$y", fontWeight = FontWeight.Bold)
                    TextButton({ y++ }) { Text("›") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton({ shiftMonth(-1) }) { Text("‹") }
                    Text(JALALI_MONTHS[m - 1], fontWeight = FontWeight.Bold)
                    TextButton({ shiftMonth(1) }) { Text("›") }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach {
                        Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val cells = List(offset) { 0 } + (1..dim).toList()
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { n ->
                            if (n == 0) Spacer(Modifier.weight(1f))
                            else {
                                val sel = n == day
                                Box(
                                    Modifier.weight(1f).padding(2.dp).aspectRatio(1f).clip(CircleShape)
                                        .background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { d = n },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("$n", color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = { TextButton({ onPick(fromJalali(y, m, day)) }) { Text("تأیید") } },
        dismissButton = { TextButton(onDismiss) { Text("انصراف") } },
    )
}
