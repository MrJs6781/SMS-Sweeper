package app.smssweeper

import android.icu.util.Calendar
import android.provider.Telephony
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MonthBar(val month: Int, val count: Int)

class Stats(
    val total: Int,
    val inbox: Int,
    val sent: Int,
    val unread: Int,
    val oldest: Long?,
    val kilobytes: Long,
    val months: List<MonthBar>,
    val first: String,
    val last: String,
    val top: List<SenderStat>,
)

/** Pure CPU work; call off the main thread. [all] is newest first. */
fun computeStats(all: List<Sms>, now: Long = System.currentTimeMillis()): Stats {
    val cal = persianCal().apply { timeInMillis = now }
    var y = cal.get(Calendar.YEAR)
    var m = cal.get(Calendar.MONTH)
    val keys = ArrayList<Pair<Int, Int>>() // (year, month0), oldest first
    repeat(12) {
        keys.add(0, y to m)
        if (--m < 0) { m = 11; y-- }
    }
    val perMonth = HashMap<Int, Int>()
    for (s in all) {
        cal.timeInMillis = s.date
        perMonth.merge(cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH), 1) { a, b -> a + b }
    }
    return Stats(
        total = all.size,
        inbox = all.count { it.type == Telephony.Sms.MESSAGE_TYPE_INBOX },
        sent = all.count { it.type == Telephony.Sms.MESSAGE_TYPE_SENT },
        unread = all.count { !it.read },
        oldest = all.lastOrNull()?.date,
        kilobytes = all.sumOf { it.body.length.toLong() } * 2 / 1024, // UTF-16 estimate
        months = keys.map { (yy, mm) -> MonthBar(mm + 1, perMonth[yy * 12 + mm] ?: 0) },
        first = "${JALALI_MONTHS[keys.first().second]} ${keys.first().first}",
        last = "${JALALI_MONTHS[keys.last().second]} ${keys.last().first}",
        top = senderStats(all).take(5),
    )
}

@Composable
private fun Stat(value: Int, label: String, modifier: Modifier) = Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text("$value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Hint(label)
}

@Composable
fun StatsScreen(vm: AppVm) {
    ScreenHeader(BarChartIcon, "آمار", "نگاهی کلی به صندوق پیامکت: حجم، ماه‌ها و فرستنده‌های پرپیام.")

    val all = vm.all
    if (all == null) {
        Panel { Text("پیامک‌ها هنوز خوانده نشده‌اند.") }
        return
    }
    val stats by produceState<Stats?>(null, all) { value = withContext(Dispatchers.Default) { computeStats(all) } }
    val s = stats
    if (s == null) {
        Panel { Text("در حال محاسبه…") }
        return
    }

    Panel {
        Row(Modifier.fillMaxWidth()) {
            Stat(s.total, "کل", Modifier.weight(1f))
            Stat(s.inbox, "دریافتی", Modifier.weight(1f))
            Stat(s.sent, "ارسالی", Modifier.weight(1f))
            Stat(s.unread, "نخوانده", Modifier.weight(1f))
        }
        s.oldest?.let { Hint("قدیمی‌ترین پیامک: ${jalali(it)} · حجم تقریبی متن: ${s.kilobytes} کیلوبایت") }
    }

    Panel {
        SectionLabel("پیامک در ۱۲ ماه اخیر (${s.first} تا ${s.last})")
        val max = maxOf(1, s.months.maxOf { it.count })
        Row(
            Modifier.fillMaxWidth().height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            s.months.forEach { b ->
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text("${b.count}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        Modifier.fillMaxWidth().height(maxOf(2f, 100f * b.count / max).dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
                    )
                    Text("${b.month}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Hint("عدد زیر هر ستون، شماره‌ی ماه شمسی است.")
    }

    Panel {
        SectionLabel("پرپیامک‌ترین فرستنده‌ها")
        s.top.forEach {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(it.address.ifBlank { "(بدون شماره)" })
                Text("${it.count}", fontWeight = FontWeight.Bold)
            }
        }
        Hint("برای حذف، به بخش «فرستنده‌ها» برو.")
    }
}
