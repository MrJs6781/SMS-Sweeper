@file:OptIn(ExperimentalMaterial3Api::class)

package app.smssweeper

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Mode(val label: String) {
    BEFORE("قبل از تاریخ"), RANGE("بازه تاریخی"), COUNT("تعداد"), SPAM("اسپم"), MATCH("فرستنده / متن")
}

private class Preview(val box: Box, val rule: Rule, val total: Int, val hits: List<Sms>)

@Composable
fun Home() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // ---- default-SMS-app state ----
    fun checkDefault() = Telephony.Sms.getDefaultSmsPackage(ctx) == ctx.packageName
    var isDefault by remember { mutableStateOf(checkDefault()) }
    LifecycleResumeEffect(Unit) {
        isDefault = checkDefault()
        onPauseOrDispose { }
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = checkDefault()
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(isDefault) {
        if (isDefault && ctx.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    // ---- inputs ----
    var box by rememberSaveable { mutableStateOf(Box.ALL) }
    var mode by rememberSaveable { mutableStateOf(Mode.BEFORE) }
    var d1 by rememberSaveable { mutableStateOf<Long?>(null) }
    var d2 by rememberSaveable { mutableStateOf<Long?>(null) }
    var keepMode by rememberSaveable { mutableStateOf(false) }
    var nText by rememberSaveable { mutableStateOf("") }
    var sender by rememberSaveable { mutableStateOf("") }
    var keyword by rememberSaveable { mutableStateOf("") }

    // Character.digit also understands Persian/Arabic digits
    val n = nText.mapNotNull { Character.digit(it, 10).takeIf { d -> d >= 0 } }.joinToString("").toIntOrNull()
    val a = d1
    val b = d2
    val rule: Rule? = when (mode) {
        Mode.BEFORE -> a?.let { Rule.Before(it) }
        Mode.RANGE -> if (a != null && b != null && b >= a) Rule.Range(a, dayAfter(b)) else null
        Mode.COUNT -> n?.takeIf { it > 0 }?.let { if (keepMode) Rule.KeepLatestN(it) else Rule.OldestN(it) }
        Mode.SPAM -> Rule.Spam
        Mode.MATCH -> if (sender.isBlank() && keyword.isBlank()) null else Rule.Match(sender, keyword)
    }

    // ---- results ----
    var preview by remember { mutableStateOf<Preview?>(null) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }
    val shown = preview?.takeIf { it.box == box && it.rule == rule }

    fun runPreview(r: Rule) = scope.launch {
        busy = true
        message = null
        try {
            preview = withContext(Dispatchers.IO) {
                val all = SmsRepo.load(ctx.contentResolver, box)
                Preview(box, r, all.size, SmsRepo.select(all, r))
            }
        } catch (e: SecurityException) {
            message = "دسترسی خواندن پیامک داده نشده است."
        }
        busy = false
    }

    fun runDelete(p: Preview) = scope.launch {
        busy = true
        progress = 0
        val res = runCatching {
            withContext(Dispatchers.IO) {
                SmsRepo.delete(ctx.contentResolver, p.hits.map { it.id }) { progress = it }
            }
        }
        busy = false
        preview = null
        message = res.fold(
            { if (it == 0) "هیچ پیامکی حذف نشد؛ مطمئن شو این برنامه پیش‌فرض پیامک است." else "$it پیامک حذف شد." },
            { "خطا: ${it.message}" },
        )
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("SMS Sweeper", fontWeight = FontWeight.Bold) }) },
    ) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Panel {
                if (!isDefault) {
                    Text("اندروید فقط به «برنامه پیش‌فرض پیامک» اجازه‌ی حذف پیامک می‌دهد. SMS Sweeper را موقتاً پیش‌فرض کن و بعد از پاکسازی، برنامه‌ی قبلی‌ات را برگردان.")
                    Button(onClick = {
                        val rm = ctx.getSystemService(RoleManager::class.java)
                        roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_SMS))
                    }) { Text("تنظیم به‌عنوان پیش‌فرض") }
                } else {
                    Text("SMS Sweeper الان برنامه‌ی پیش‌فرض پیامک است. بعد از پاکسازی، برنامه‌ی پیامک قبلی‌ات را پیش‌فرض کن تا پیام‌های جدید (به‌خصوص MMS) از دست نروند.")
                    OutlinedButton(onClick = {
                        ctx.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                    }) { Text("تغییر برنامه‌ی پیش‌فرض") }
                }
            }

            Text("کدام پیامک‌ها؟", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box.entries.forEach { FilterChip(box == it, { box = it }, { Text(it.label) }) }
            }

            Text("روش حذف", fontWeight = FontWeight.Bold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mode.entries.forEach { FilterChip(mode == it, { mode = it }, { Text(it.label) }) }
            }

            Panel {
                when (mode) {
                    Mode.BEFORE -> {
                        DateField("انتخاب تاریخ", d1) { d1 = it }
                        Text("همه‌ی پیامک‌های قبل از این روز حذف می‌شوند.", style = MaterialTheme.typography.bodySmall)
                    }
                    Mode.RANGE -> {
                        DateField("از تاریخ", d1) { d1 = it }
                        DateField("تا تاریخ (شامل همین روز)", d2) { d2 = it }
                        if (a != null && b != null && b < a) {
                            Text("تاریخ پایان باید بعد از شروع باشد.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Mode.COUNT -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(!keepMode, { keepMode = false }, { Text("حذف قدیمی‌ترین‌ها") })
                            FilterChip(keepMode, { keepMode = true }, { Text("نگه داشتن آخرین‌ها") })
                        }
                        OutlinedTextField(
                            nText, { nText = it }, Modifier.fillMaxWidth(),
                            label = { Text("تعداد") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                    Mode.SPAM -> Text(
                        "پیامک‌های دریافتیِ تبلیغاتی (لغو ۱۱، تخفیف، جایزه، لینک و …) شناسایی می‌شوند. پیامک‌های کد تأیید و رمز هرگز اسپم حساب نمی‌شوند. حتماً قبل از حذف، پیش‌نمایش را ببین؛ تشخیص بر پایه‌ی کلمات کلیدی است.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Mode.MATCH -> {
                        OutlinedTextField(
                            sender, { sender = it }, Modifier.fillMaxWidth(),
                            label = { Text("شماره یا نام فرستنده") }, singleLine = true,
                        )
                        OutlinedTextField(
                            keyword, { keyword = it }, Modifier.fillMaxWidth(),
                            label = { Text("متن پیام شامل") }, singleLine = true,
                        )
                    }
                }
            }

            Button(
                onClick = { rule?.let(::runPreview) },
                enabled = isDefault && rule != null && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("پیش‌نمایش") }

            if (busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                if (progress > 0) Text("$progress پیامک حذف شد…", style = MaterialTheme.typography.bodySmall)
            }

            shown?.let { p ->
                Panel {
                    Text(
                        if (p.hits.isEmpty()) "پیامکی با این شرط پیدا نشد."
                        else "${p.hits.size} پیامک از ${p.total} پیامک مطابقت دارد.",
                        fontWeight = FontWeight.Bold,
                    )
                    p.hits.take(5).forEach {
                        Text(
                            "${it.address} · ${jalali(it.date)}\n${it.body.take(80)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (p.hits.isNotEmpty()) {
                        Button(
                            onClick = { confirm = true },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("حذف ${p.hits.size} پیامک") }
                    }
                }
            }

            message?.let { Panel { Text(it, fontWeight = FontWeight.Medium) } }
        }
    }

    if (confirm && shown != null) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("حذف دسته‌ای") },
            text = { Text("${shown.hits.size} پیامک برای همیشه حذف می‌شود و قابل بازگشت نیست. ادامه می‌دهی؟") },
            confirmButton = {
                TextButton({ confirm = false; runDelete(shown) }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton({ confirm = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) = Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
private fun DateField(label: String, value: Long?, onPick: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Text(if (value == null) label else "${jalali(value)}  (${iso(value)})")
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value?.let(::toPicker))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { onPick(fromPicker(it)) }
                    open = false
                }) { Text("تأیید") }
            },
            dismissButton = { TextButton({ open = false }) { Text("انصراف") } },
        ) { DatePicker(state) }
    }
}
