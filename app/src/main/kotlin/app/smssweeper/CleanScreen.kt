@file:OptIn(ExperimentalMaterial3Api::class)

package app.smssweeper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun CleanScreen(vm: AppVm, isDefault: Boolean) {
    val form = vm.form
    val criteria = form.criteria
    val all = vm.all
    val lists = vm.lists

    // Live matches; reset to null first so stale results can never be deleted.
    val hits by produceState<List<Sms>?>(null, all, criteria, lists) {
        value = null
        if (all != null && criteria != null) {
            delay(200)
            value = withContext(Dispatchers.Default) { SmsRepo.select(all, criteria, lists) }
        }
    }
    var confirm by remember { mutableStateOf<List<Sms>?>(null) }
    var naming by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var presetToDelete by remember { mutableStateOf<Preset?>(null) }

    ScreenHeader(Icons.Outlined.Delete, "پاکسازی", "پیامک‌ها را با شرط‌های دلخواه پیدا کن و یک‌جا حذف کن.")

    if (vm.presets.isNotEmpty()) {
        SectionLabel("قانون‌های من")
        ChipRow {
            vm.presets.forEach { p ->
                InputChip(
                    selected = false,
                    onClick = { form.load(p.criteria) },
                    label = { Text(p.name) },
                    trailingIcon = {
                        Icon(Icons.Outlined.Close, "حذف قانون", Modifier.size(18.dp).clickable { presetToDelete = p })
                    },
                )
            }
        }
    }

    FilterForm(form)

    Panel {
        val h = hits
        when {
            all == null && vm.loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("در حال خواندن پیامک‌ها…")
            }
            all == null -> {
                Text("پیامک‌ها هنوز خوانده نشده‌اند. اگر مجوز داده‌ای، دوباره امتحان کن.")
                OutlinedButton({ vm.refresh() }) { Text("خواندن پیامک‌ها") }
            }
            criteria == null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Info, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Hint(form.problem ?: "")
            }
            h == null -> Text("در حال محاسبه…")
            else -> {
                Text(
                    if (h.isEmpty()) "پیامکی با این شرط پیدا نشد" else "${h.size} پیامک از ${all.size} پیامک مطابقت دارد",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                h.take(3).forEach {
                    Hint("${it.address} · ${jalali(it.date)}")
                    Text(it.body, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                }
                if (h.isNotEmpty()) {
                    Button(
                        onClick = { confirm = h },
                        enabled = isDefault && !vm.busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp))
                        Text("حذف ${h.size} پیامک", Modifier.padding(start = 8.dp))
                    }
                    if (!isDefault) Hint("برای حذف، اپ باید پیش‌فرض پیامک باشد.")
                }
            }
        }
        if (criteria != null) {
            TextButton({ naming = true }) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                Text("ذخیره به‌عنوان قانون", Modifier.padding(start = 6.dp))
            }
        }
    }

    if (vm.busy) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        if (vm.progress > 0) Hint("${vm.progress} پیامک حذف شد…")
    }

    confirm?.let { list ->
        TwoStepConfirm(
            title = "حذف ${list.size} پیامک؟",
            message = "پیامک‌های مطابق شرط‌هایی که تعیین کردی حذف می‌شوند.",
            finalMessage = "${list.size} پیامک حذف می‌شود.\n\n${backupNote(vm)}",
            confirmLabel = "حذف نهایی",
            onConfirm = { confirm = null; vm.delete(list) },
            onDismiss = { confirm = null },
        )
    }

    presetToDelete?.let { p ->
        TwoStepConfirm(
            title = "حذف قانون «${p.name}»؟",
            message = "این قانون از فهرست قانون‌های ذخیره‌شده پاک می‌شود.",
            finalMessage = "قانون «${p.name}» برای همیشه حذف می‌شود. اگر در پاکسازی خودکار استفاده شده باشد، دیگر اجرا نخواهد شد.",
            confirmLabel = "حذف قانون",
            onConfirm = { vm.deletePreset(p.name); presetToDelete = null },
            onDismiss = { presetToDelete = null },
        )
    }

    if (naming) {
        AlertDialog(
            onDismissRequest = { naming = false },
            title = { Text("نام قانون") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("مثلاً: اسپم قدیمی") }) },
            confirmButton = {
                TextButton({
                    val c = form.criteria
                    if (name.isNotBlank() && c != null) vm.savePreset(name.trim(), c)
                    naming = false
                    name = ""
                }) { Text("ذخیره") }
            },
            dismissButton = { TextButton({ naming = false }) { Text("انصراف") } },
        )
    }
}
