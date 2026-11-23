package app.smssweeper

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun CleanScreen(vm: AppVm, isDefault: Boolean) {
    val form = vm.form
    val criteria = form.criteria
    val all = vm.all

    // Live matches; reset to null first so stale results can never be deleted.
    val lists = vm.lists
    val hits by produceState<List<Sms>?>(null, all, criteria, lists) {
        value = null
        if (all != null && criteria != null) {
            delay(200)
            value = withContext(Dispatchers.Default) { SmsRepo.select(all, criteria, lists) }
        }
    }
    var confirm by remember { mutableStateOf<List<Sms>?>(null) }

    FilterForm(form)
    form.problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }

    Panel {
        val h = hits
        when {
            all == null && vm.loading -> Text("در حال خواندن پیامک‌ها…")
            all == null -> {
                Text("پیامک‌ها هنوز خوانده نشده‌اند. اگر مجوز داده‌ای، دوباره امتحان کن.")
                OutlinedButton({ vm.refresh() }) { Text("خواندن پیامک‌ها") }
            }
            criteria == null -> Hint("با تکمیل شرط‌ها، تعداد پیامک‌های مطابق اینجا نشان داده می‌شود.")
            h == null -> Text("در حال محاسبه…")
            else -> {
                Text(
                    if (h.isEmpty()) "پیامکی با این شرط پیدا نشد." else "${h.size} پیامک از ${all.size} پیامک مطابقت دارد.",
                    fontWeight = FontWeight.Bold,
                )
                h.take(5).forEach { Hint("${it.address} · ${jalali(it.date)}\n${it.body.take(80)}") }
                if (h.isNotEmpty()) {
                    Button(
                        onClick = { confirm = h },
                        enabled = isDefault && !vm.busy,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("حذف ${h.size} پیامک") }
                    if (!isDefault) Hint("برای حذف، اپ باید پیش‌فرض پیامک باشد.")
                }
            }
        }
    }

    if (vm.busy) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        if (vm.progress > 0) Hint("${vm.progress} پیامک حذف شد…")
    }
    vm.message?.let { Panel { Text(it, fontWeight = FontWeight.Medium) } }

    confirm?.let { list ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("حذف دسته‌ای") },
            text = { Text("${list.size} پیامک برای همیشه حذف می‌شود و قابل بازگشت نیست. ادامه می‌دهی؟") },
            confirmButton = {
                TextButton({ confirm = null; vm.delete(list) }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton({ confirm = null }) { Text("انصراف") } },
        )
    }
}
