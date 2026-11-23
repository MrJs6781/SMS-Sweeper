package app.smssweeper

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight

@Composable
fun TrashScreen(vm: AppVm, isDefault: Boolean) {
    LaunchedEffect(Unit) { vm.reloadBatches() }
    var confirmEmpty by remember { mutableStateOf(false) }

    Panel {
        Text("سطل بازیافت", fontWeight = FontWeight.Bold)
        Hint("پیامک‌های حذف‌شده (وقتی «پشتیبان قبل از حذف» روشن است) این مدت اینجا می‌مانند و بعد برای همیشه پاک می‌شوند.")
        ChipRow {
            listOf(7, 30, 90).forEach { FilterChip(vm.retention == it, { vm.setRetention(it) }, { Text("$it روز") }) }
        }
    }

    if (vm.batches.isEmpty()) Panel { Text("سطل خالی است.") }
    vm.batches.forEach { b ->
        Panel {
            Text("${jalali(b.createdAt)} · ${b.count} پیامک", fontWeight = FontWeight.Bold)
            Row {
                TextButton({ vm.restoreBatch(b) }, enabled = isDefault && !vm.busy) { Text("بازیابی") }
                TextButton({ vm.deleteBatch(b) }) { Text("حذف دائمی", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (vm.batches.isNotEmpty()) {
        OutlinedButton({ confirmEmpty = true }) { Text("خالی کردن سطل") }
        if (!isDefault) Hint("برای بازیابی، اپ باید پیش‌فرض پیامک باشد.")
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text("خالی کردن سطل") },
            text = { Text("همه‌ی پیامک‌های داخل سطل برای همیشه پاک می‌شوند. ادامه می‌دهی؟") },
            confirmButton = {
                TextButton({ confirmEmpty = false; vm.emptyTrash() }) { Text("پاک کن", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ confirmEmpty = false }) { Text("انصراف") } },
        )
    }
}
