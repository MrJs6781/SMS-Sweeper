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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class Preview(val c: Criteria, val total: Int, val hits: List<Sms>)

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

    // ---- filter + results ----
    val form = remember { Form() }
    val criteria = form.criteria
    var preview by remember { mutableStateOf<Preview?>(null) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }
    val shown = preview?.takeIf { it.c == criteria }

    fun runPreview(c: Criteria) = scope.launch {
        busy = true
        message = null
        try {
            preview = withContext(Dispatchers.IO) {
                val all = SmsRepo.load(ctx.contentResolver)
                Preview(c, all.size, SmsRepo.select(all, c))
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

            FilterForm(form)
            form.problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = { criteria?.let(::runPreview) },
                enabled = isDefault && criteria != null && !busy,
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
                        Hint("${it.address} · ${jalali(it.date)}\n${it.body.take(80)}")
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
