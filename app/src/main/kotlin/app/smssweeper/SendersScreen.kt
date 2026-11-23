package app.smssweeper

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

class SenderStat(val address: String, val msgs: List<Sms>) {
    val count get() = msgs.size
    val unread get() = msgs.count { !it.read }
    val last get() = msgs.first().date // msgs are newest first
}

fun senderStats(all: List<Sms>): List<SenderStat> =
    all.groupBy { it.address }.map { SenderStat(it.key, it.value) }.sortedByDescending { it.count }

private const val MAX_ROWS = 100

@Composable
fun SendersScreen(vm: AppVm, isDefault: Boolean) {
    val all = vm.all
    if (all == null) {
        Panel { Text("پیامک‌ها هنوز خوانده نشده‌اند.") }
        return
    }
    var q by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<SenderStat?>(null) }
    val stats = remember(all) { senderStats(all) }
    val shown = remember(stats, q) {
        if (q.isBlank()) stats else stats.filter { it.address.contains(q.trim(), true) }
    }

    OutlinedTextField(
        q, { q = it }, Modifier.fillMaxWidth(),
        label = { Text("جستجوی فرستنده") }, singleLine = true,
    )
    Hint("${stats.size} فرستنده · مرتب‌شده بر اساس تعداد پیامک")

    shown.take(MAX_ROWS).forEach { s ->
        Panel {
            Text(s.address.ifBlank { "(بدون شماره)" }, fontWeight = FontWeight.Bold)
            Hint("${s.count} پیامک · ${s.unread} خوانده‌نشده · آخرین: ${jalali(s.last)}")
            val l = vm.lists
            val protectedNow = l.isProtected(s.address)
            Row {
                TextButton(onClick = { confirm = s }, enabled = isDefault && !vm.busy && !protectedNow) {
                    Text("حذف همه", color = MaterialTheme.colorScheme.error)
                }
                if (s.address.isNotBlank()) {
                    TextButton({
                        vm.updateLists(
                            if (protectedNow) l.copy(protectedSenders = l.protectedSenders.filter { !s.address.contains(it, true) })
                            else l.copy(protectedSenders = (l.protectedSenders + s.address).distinct())
                        )
                    }) { Text(if (protectedNow) "برداشتن محافظت" else "محافظت") }
                    if (!l.isBlocked(s.address)) {
                        TextButton({ vm.updateLists(l.copy(blockedSenders = (l.blockedSenders + s.address).distinct())) }) { Text("بلاک") }
                    }
                }
            }
        }
    }
    if (shown.size > MAX_ROWS) Hint("فقط ${MAX_ROWS} فرستنده‌ی اول نمایش داده می‌شود؛ برای دیدن بقیه جستجو کن.")
    if (!isDefault) Hint("برای حذف، اپ باید پیش‌فرض پیامک باشد.")
    vm.message?.let { Panel { Text(it, fontWeight = FontWeight.Medium) } }

    confirm?.let { s ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("حذف پیامک‌های یک فرستنده") },
            text = { Text("همه‌ی ${s.count} پیامک «${s.address}» برای همیشه حذف می‌شود. ادامه می‌دهی؟") },
            confirmButton = {
                TextButton({ confirm = null; vm.delete(s.msgs) }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton({ confirm = null }) { Text("انصراف") } },
        )
    }
}
