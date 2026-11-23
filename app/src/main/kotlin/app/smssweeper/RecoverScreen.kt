package app.smssweeper

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RecoverScreen(vm: AppVm, isDefault: Boolean) {
    LaunchedEffect(Unit) { vm.reloadBatches() }
    var one by remember { mutableStateOf<Batch?>(null) }
    var emptyAll by remember { mutableStateOf(false) }

    ScreenHeader(Icons.Outlined.Refresh, "بازیابی", "پیامک‌های حذف‌شده تا مدتی اینجا می‌مانند و می‌توانی برشان گردانی.")

    Panel {
        SectionLabel("مدت نگه‌داری")
        ChipRow {
            listOf(7, 30, 90).forEach { FilterChip(vm.retention == it, { vm.changeRetention(it) }, { Text("$it روز") }) }
        }
        Hint("فقط وقتی «پشتیبان قبل از حذف» در تنظیمات روشن باشد، حذف‌ها اینجا ذخیره می‌شوند.")
    }

    ListPanel {
        if (vm.batches.isEmpty()) Text("چیزی برای بازیابی نیست.", Modifier.padding(16.dp))
        vm.batches.forEachIndexed { i, b ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${b.count} پیامک", fontWeight = FontWeight.Bold)
                    Hint("حذف‌شده در ${jalali(b.createdAt)}")
                }
                IconButton({ vm.restoreBatch(b) }, enabled = isDefault && !vm.busy) { Icon(Icons.Outlined.Refresh, "بازیابی") }
                IconButton({ one = b }) { Icon(Icons.Outlined.Close, "حذف دائمی", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (vm.batches.isNotEmpty()) {
        TextButton({ emptyAll = true }) { Text("خالی کردن همه", color = MaterialTheme.colorScheme.error) }
        if (!isDefault) Hint("برای بازیابی، اپ باید پیش‌فرض پیامک باشد.")
    }

    one?.let { b ->
        TwoStepConfirm(
            title = "حذف دائمی ${b.count} پیامک؟",
            message = "این پیامک‌ها از بخش بازیابی پاک می‌شوند.",
            finalMessage = "${b.count} پیامک برای همیشه پاک می‌شود و دیگر قابل بازیابی نیست.",
            confirmLabel = "حذف دائمی",
            onConfirm = { vm.deleteBatch(b); one = null },
            onDismiss = { one = null },
        )
    }
    if (emptyAll) {
        TwoStepConfirm(
            title = "خالی کردن بخش بازیابی؟",
            message = "همه‌ی حذف‌های ذخیره‌شده پاک می‌شوند.",
            finalMessage = "${vm.batches.sumOf { it.count }} پیامک برای همیشه پاک می‌شود و دیگر قابل بازیابی نیست.",
            confirmLabel = "پاک کن",
            onConfirm = { vm.emptyTrash(); emptyAll = false },
            onDismiss = { emptyAll = false },
        )
    }
}
