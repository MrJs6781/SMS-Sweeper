package app.smssweeper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    ScreenHeader(Icons.Outlined.Person, "فرستنده‌ها", "ببین چه کسانی بیشتر پیامک می‌فرستند؛ پیام‌هایشان را حذف یا محافظت کن.")

    val all = vm.all
    if (all == null) {
        Panel { Text("پیامک‌ها هنوز خوانده نشده‌اند.") }
        return
    }
    var q by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<SenderStat?>(null) }
    val stats = remember(all) { senderStats(all) }
    val shown = remember(stats, q) {
        if (q.isBlank()) stats else stats.filter { it.address.contains(q.trim(), true) }
    }
    val l = vm.lists

    OutlinedTextField(
        q, { q = it }, Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        label = { Text("جستجوی فرستنده") }, singleLine = true,
    )

    ListPanel {
        if (shown.isEmpty()) Text("فرستنده‌ای پیدا نشد.", Modifier.padding(16.dp))
        shown.take(MAX_ROWS).forEachIndexed { i, s ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val isProtected = l.isProtected(s.address)
            val isBlocked = l.isBlocked(s.address)
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.address.ifBlank { "(بدون شماره)" }, fontWeight = FontWeight.Bold)
                        if (isProtected) Icon(Icons.Outlined.Lock, "محافظت‌شده", Modifier.padding(start = 6.dp).size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        if (isBlocked) Icon(Icons.Outlined.Warning, "بلاک", Modifier.padding(start = 6.dp).size(14.dp), tint = MaterialTheme.colorScheme.error)
                    }
                    Hint("${s.count} پیامک · ${s.unread} خوانده‌نشده · آخرین: ${jalali(s.last)}")
                }
                Box {
                    IconButton({ menu = s.address }) { Icon(Icons.Outlined.MoreVert, "گزینه‌ها") }
                    DropdownMenu(expanded = menu == s.address, onDismissRequest = { menu = null }) {
                        if (s.address.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text(if (isProtected) "برداشتن محافظت" else "محافظت (هرگز حذف نشود)") },
                                leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                                onClick = {
                                    menu = null
                                    vm.updateLists(
                                        if (isProtected) l.copy(protectedSenders = l.protectedSenders.filter { !s.address.contains(it, true) })
                                        else l.copy(protectedSenders = (l.protectedSenders + s.address).distinct())
                                    )
                                },
                            )
                            if (!isBlocked) {
                                DropdownMenuItem(
                                    text = { Text("بلاک (همیشه اسپم)") },
                                    leadingIcon = { Icon(Icons.Outlined.Warning, null) },
                                    onClick = {
                                        menu = null
                                        vm.updateLists(l.copy(blockedSenders = (l.blockedSenders + s.address).distinct()))
                                    },
                                )
                            }
                        }
                        DropdownMenuItem(
                            text = { Text("حذف همه‌ی پیامک‌ها", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            enabled = isDefault && !vm.busy && !isProtected,
                            onClick = { menu = null; confirm = s },
                        )
                    }
                }
            }
        }
    }
    Hint(
        if (shown.size > MAX_ROWS) "$MAX_ROWS فرستنده‌ی اول از ${shown.size} نمایش داده می‌شود؛ برای دیدن بقیه جستجو کن."
        else "${shown.size} فرستنده · مرتب‌شده بر اساس تعداد پیامک"
    )
    if (!isDefault) Hint("برای حذف، اپ باید پیش‌فرض پیامک باشد.")

    confirm?.let { s ->
        TwoStepConfirm(
            title = "حذف پیامک‌های «${s.address}»؟",
            message = "همه‌ی ${s.count} پیامک این فرستنده حذف می‌شود.",
            finalMessage = "${s.count} پیامک از «${s.address}» حذف می‌شود.\n\n${backupNote(vm)}",
            confirmLabel = "حذف نهایی",
            onConfirm = { confirm = null; vm.delete(s.msgs) },
            onDismiss = { confirm = null },
        )
    }
}
