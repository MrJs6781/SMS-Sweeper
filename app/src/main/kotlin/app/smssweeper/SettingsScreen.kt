package app.smssweeper

import androidx.compose.foundation.layout.Arrangement
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
