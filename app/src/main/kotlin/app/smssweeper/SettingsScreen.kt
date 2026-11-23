package app.smssweeper

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

@Composable
fun SettingsScreen(vm: AppVm) {
    ScreenHeader(Icons.Outlined.Settings, "تنظیمات", "پاکسازی خودکار، پشتیبان‌گیری و فهرست‌های محافظت و بلاک.")

    var askAutoOn by remember { mutableStateOf(false) }
    var askRunNow by remember { mutableStateOf(false) }

    // ---- appearance ----
    Panel {
        Text("ظاهر برنامه", fontWeight = FontWeight.Bold)
        ChipRow {
            listOf(0 to "مثل سیستم", 1 to "روشن", 2 to "تیره").forEach { (m, label) ->
                FilterChip(vm.themeMode == m, { vm.changeTheme(m) }, { Text(label) })
            }
        }
    }

    // ---- auto-clean ----
    Panel {
        Text("پاکسازی خودکار", fontWeight = FontWeight.Bold)
        Hint("قانون‌های ذخیره‌شده‌ات را به‌صورت دوره‌ای اجرا می‌کند. فقط وقتی کار می‌کند که اپ همان لحظه پیش‌فرض پیامک باشد.")
        SwitchRow("فعال", vm.autoOn) { if (it) askAutoOn = true else vm.setAuto(on = false) }
        SectionLabel("هر چند وقت")
        ChipRow {
            listOf(1 to "روزانه", 7 to "هفتگی").forEach { (d, label) ->
                FilterChip(vm.autoEvery == d, { vm.setAuto(everyDays = d) }, { Text(label) })
            }
        }
        SectionLabel("قانون‌هایی که اجرا شوند")
        if (vm.presets.isEmpty()) {
            Hint("ابتدا در بخش «پاکسازی» یک قانون ذخیره کن.")
        } else {
            ChipRow {
                vm.presets.forEach { p ->
                    val on = p.name in vm.autoPresets
                    FilterChip(on, { vm.setAuto(names = if (on) vm.autoPresets - p.name else vm.autoPresets + p.name) }, { Text(p.name) })
                }
            }
        }
        OutlinedButton({ askRunNow = true }, Modifier.fillMaxWidth(), enabled = !vm.busy) { Text("اجرای همین حالا") }
        vm.autoLog?.let { (t, msg) -> Hint("آخرین اجرا (${jalali(t)}): $msg") }
    }

    // ---- backup ----
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportTo(uri)
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFrom(uri)
    }
    Panel {
        Text("پشتیبان‌گیری", fontWeight = FontWeight.Bold)
        SwitchRow("پشتیبان قبل از هر حذف", vm.backupOn) { vm.setBackup(it) }
        Hint("حذف‌ها در بخش «بازیابی» ذخیره می‌شوند. اگر پشتیبان‌گیری شکست بخورد، چیزی حذف نمی‌شود.")
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        OutlinedButton({ export.launch("sms-backup-${iso(System.currentTimeMillis())}.json") }, Modifier.fillMaxWidth()) {
            Text("ذخیره‌ی همه‌ی پیامک‌ها در فایل")
        }
        OutlinedButton({ import.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth()) { Text("بازیابی از فایل") }
        Hint("بازیابی نیاز دارد اپ پیش‌فرض پیامک باشد؛ پیامک‌های تکراری دوباره اضافه نمی‌شوند.")
    }

    // ---- lists ----
    val l = vm.lists
    ListEditor(
        "فرستنده‌های محافظت‌شده",
        "پیامک این فرستنده‌ها با هیچ قانونی حذف نمی‌شود (مثلاً بانک یا خانواده). نام یا بخشی از شماره کافی است.",
        l.protectedSenders,
    ) { vm.updateLists(l.copy(protectedSenders = it)) }
    ListEditor(
        "فرستنده‌های بلاک",
        "پیامک دریافتی این فرستنده‌ها در فیلتر «فقط اسپم» همیشه شامل می‌شود.",
        l.blockedSenders,
    ) { vm.updateLists(l.copy(blockedSenders = it)) }
    ListEditor(
        "کلمات اسپم دلخواه",
        "علاوه بر کلمات پیش‌فرض، پیامک‌های دریافتی حاوی این کلمات اسپم حساب می‌شوند.",
        l.spamWords,
    ) { vm.updateLists(l.copy(spamWords = it)) }

    if (askAutoOn) {
        TwoStepConfirm(
            title = "فعال‌سازی پاکسازی خودکار؟",
            message = "پیامک‌های مطابق قانون‌های انتخاب‌شده به‌صورت دوره‌ای و بدون پرسش حذف می‌شوند.",
            finalMessage = "پاکسازی خودکار ${if (vm.autoEvery == 1) "هر روز" else "هر هفته"} اجرا می‌شود. ${backupNote(vm)}",
            confirmLabel = "فعال شود",
            onConfirm = { vm.setAuto(on = true); askAutoOn = false },
            onDismiss = { askAutoOn = false },
        )
    }
    if (askRunNow) {
        TwoStepConfirm(
            title = "اجرای قانون‌های خودکار؟",
            message = "قانون‌های انتخاب‌شده همین حالا اجرا و پیامک‌های مطابق حذف می‌شوند.",
            finalMessage = "پاکسازی همین حالا اجرا می‌شود. ${backupNote(vm)}",
            confirmLabel = "اجرا و حذف",
            onConfirm = { vm.runAutoNow(); askRunNow = false },
            onDismiss = { askRunNow = false },
        )
    }
}

@Composable
fun ListEditor(title: String, hint: String, items: List<String>, onChange: (List<String>) -> Unit) {
    var text by remember { mutableStateOf("") }
    var removing by remember { mutableStateOf<String?>(null) }
    Panel {
        Text(title, fontWeight = FontWeight.Bold)
        Hint(hint)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, Modifier.weight(1f), label = { Text("افزودن") }, singleLine = true)
            FilledIconButton({
                val v = text.trim()
                if (v.isNotEmpty() && v !in items) onChange(items + v)
                text = ""
            }) { Icon(Icons.Outlined.Add, "افزودن") }
        }
        items.forEach {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(it)
                IconButton({ removing = it }) { Icon(Icons.Outlined.Close, "حذف", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
    removing?.let { item ->
        TwoStepConfirm(
            title = "حذف «$item» از فهرست؟",
            message = "این مورد از فهرست «$title» برداشته می‌شود.",
            finalMessage = "«$item» از فهرست برداشته می‌شود و دیگر اثری نخواهد داشت.",
            confirmLabel = "حذف از فهرست",
            onConfirm = { onChange(items - item); removing = null },
            onDismiss = { removing = null },
        )
    }
}
