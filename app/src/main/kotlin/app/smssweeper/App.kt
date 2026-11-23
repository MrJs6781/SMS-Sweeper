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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect

enum class Tab(val label: String, val icon: String) {
    CLEAN("پاکسازی", "🧹"),
    SENDERS("فرستنده‌ها", "👥"),
    STATS("آمار", "📊"),
    TRASH("سطل", "♻️"),
    SETTINGS("تنظیمات", "⚙️"),
}

@Composable
fun App(vm: AppVm) {
    val ctx = LocalContext.current

    // ---- default-SMS-app / permission state ----
    fun checkDefault() = Telephony.Sms.getDefaultSmsPackage(ctx) == ctx.packageName
    fun checkRead() = ctx.checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    var isDefault by remember { mutableStateOf(checkDefault()) }
    var hasRead by remember { mutableStateOf(checkRead()) }
    LifecycleResumeEffect(Unit) {
        isDefault = checkDefault()
        hasRead = checkRead()
        onPauseOrDispose { }
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = checkDefault()
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasRead = it }
    LaunchedEffect(isDefault) {
        if (isDefault && !checkRead()) permLauncher.launch(Manifest.permission.READ_SMS)
    }
    LaunchedEffect(hasRead) { if (hasRead && vm.all == null) vm.refresh() }

    var tab by rememberSaveable { mutableStateOf(Tab.CLEAN) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("SMS Sweeper", fontWeight = FontWeight.Bold) }) },
        bottomBar = {
            if (Tab.entries.size > 1) NavigationBar {
                Tab.entries.forEach {
                    NavigationBarItem(tab == it, { tab = it; vm.message = null },{ Text(it.icon) }, label = { Text(it.label) })
                }
            }
        },
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
            when (tab) {
                Tab.CLEAN -> CleanScreen(vm, isDefault)
                Tab.SENDERS -> SendersScreen(vm, isDefault)
                Tab.STATS -> StatsScreen(vm)
                Tab.TRASH -> TrashScreen(vm, isDefault)
                Tab.SETTINGS -> SettingsScreen(vm)
            }
            vm.message?.let { msg ->
                Panel {
                    Text(msg, fontWeight = FontWeight.Medium)
                    vm.lastBatch?.let { b ->
                        OutlinedButton({ vm.restoreBatch(b) }, enabled = isDefault && !vm.busy) { Text("بازگردانی همین حذف") }
                    }
                }
            }
        }
    }
}
