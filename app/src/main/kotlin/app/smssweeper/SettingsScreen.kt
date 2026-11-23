package app.smssweeper

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportTo(uri)
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFrom(uri)
    }
    Panel {
        Text("پشتیبان‌گیری", fontWeight = FontWeight.Bold)
        SwitchRow("پشتیبان خودکار قبل از هر حذف", vm.backupOn) { vm.setBackup(it) }
        Hint("اگر روشن باشد، هر حذف قبل از اجرا در حافظه‌ی خود اپ ذخیره می‌شود و در صورت پشیمانی قابل بازگردانی است. اگر پشتیبان‌گیری شکست بخورد، چیزی حذف نمی‌شود.")
        OutlinedButton({ export.launch("sms-backup-${iso(System.currentTimeMillis())}.json") }, Modifier.fillMaxWidth()) {
            Text("پشتیبان‌گیری از همه‌ی پیامک‌ها (فایل)")
        }
        OutlinedButton({ import.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth()) { Text("بازیابی از فایل پشتیبان") }
        Hint("بازیابی نیاز دارد اپ پیش‌فرض پیامک باشد؛ پیامک‌های تکراری دوباره اضافه نمی‌شوند.")
    }

    val l = vm.lists
    ListEditor(
        "فرستنده‌های محافظت‌شده",
        "پیامک این فرستنده‌ها با هیچ قانونی حذف نمی‌شود (مثلاً بانک یا خانواده). نام یا بخشی از شماره کافی است.",
        l.protectedSenders,
    ) { vm.updateLists(l.copy(protectedSenders = it)) }
    ListEditor(
        "فرستنده‌های بلاک (همیشه اسپم)",
        "پیامک دریافتی این فرستنده‌ها در فیلتر «فقط اسپم» همیشه شامل می‌شود.",
        l.blockedSenders,
    ) { vm.updateLists(l.copy(blockedSenders = it)) }
    ListEditor(
        "کلمات اسپم دلخواه",
        "علاوه بر کلمات پیش‌فرض، پیامک‌های دریافتی حاوی این کلمات اسپم حساب می‌شوند.",
        l.spamWords,
    ) { vm.updateLists(l.copy(spamWords = it)) }
}

@Composable
fun ListEditor(title: String, hint: String, items: List<String>, onChange: (List<String>) -> Unit) {
    var text by remember { mutableStateOf("") }
    Panel {
        Text(title, fontWeight = FontWeight.Bold)
        Hint(hint)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, Modifier.weight(1f), label = { Text("افزودن") }, singleLine = true)
            Button({
                val v = text.trim()
                if (v.isNotEmpty() && v !in items) onChange(items + v)
                text = ""
            }) { Text("+") }
        }
        items.forEach {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(it)
                TextButton({ onChange(items - it) }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
