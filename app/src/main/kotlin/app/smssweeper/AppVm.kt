package app.smssweeper

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Holds the loaded messages and shared UI state so every tab sees the same data. */
class AppVm(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()

    val form = Form()

    var lists by mutableStateOf(Prefs.lists(ctx))
        private set

    fun updateLists(l: Lists) {
        lists = l
        Prefs.saveLists(ctx, l)
    }

    // ---- in-app trash (backup batches) ----
    var batches by mutableStateOf<List<Batch>>(emptyList())
        private set

    /** Backup made by the most recent delete; lets the UI offer "undo". */
    var lastBatch by mutableStateOf<Batch?>(null)
        private set

    var retention by mutableIntStateOf(Prefs.retention(ctx))
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            Backup.purge(ctx, retention)
            batches = Backup.batches(ctx)
        }
    }

    fun reloadBatches() {
        batches = Backup.batches(ctx)
    }

    fun setRetention(days: Int) {
        retention = days
        Prefs.setRetention(ctx, days)
        viewModelScope.launch(Dispatchers.IO) {
            Backup.purge(ctx, days)
            batches = Backup.batches(ctx)
        }
    }

    fun restoreBatch(b: Batch) {
        viewModelScope.launch {
            message = runCatching {
                withContext(Dispatchers.IO) {
                    val msgs = b.file.inputStream().use { Backup.read(it) }
                    val n = Backup.restore(ctx.contentResolver, msgs)
                    b.file.delete() // only reached if the restore didn't throw
                    n
                }
            }.fold({ "$it پیامک بازیابی شد." }, { "بازیابی ناموفق بود (آیا اپ پیش‌فرض پیامک است؟): ${it.message}" })
            if (lastBatch?.file == b.file) lastBatch = null
            reloadBatches()
            loading = false
            refresh()
        }
    }

    fun deleteBatch(b: Batch) {
        b.file.delete()
        if (lastBatch?.file == b.file) lastBatch = null
        reloadBatches()
    }

    fun emptyTrash() {
        batches.forEach { it.file.delete() }
        lastBatch = null
        reloadBatches()
    }

    var backupOn by mutableStateOf(Prefs.backupOn(ctx))
        private set

    fun setBackup(v: Boolean) {
        backupOn = v
        Prefs.setBackupOn(ctx, v)
    }

    /** Writes every message to [uri] (user-chosen file). */
    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            message = runCatching {
                withContext(Dispatchers.IO) {
                    val msgs = SmsRepo.load(ctx.contentResolver)
                    ctx.contentResolver.openOutputStream(uri)!!.use { Backup.write(it, msgs, "manual") }
                    msgs.size
                }
            }.fold({ "$it پیامک در فایل پشتیبان ذخیره شد." }, { "خطا در پشتیبان‌گیری: ${it.message}" })
        }
    }

    /** Restores messages from a backup file; needs the default-SMS role. */
    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            message = runCatching {
                withContext(Dispatchers.IO) {
                    val msgs = ctx.contentResolver.openInputStream(uri)!!.use { Backup.read(it) }
                    Backup.restore(ctx.contentResolver, msgs)
                }
            }.fold({ "$it پیامک بازیابی شد." }, { "خطا در بازیابی: ${it.message}" })
            loading = false
            refresh()
        }
    }

    var presets by mutableStateOf(Prefs.presets(ctx))
        private set

    /** Saves under [name], replacing a preset with the same name. */
    fun savePreset(name: String, c: Criteria) {
        presets = presets.filter { it.name != name } + Preset(name, c)
        Prefs.savePresets(ctx, presets)
    }

    fun deletePreset(name: String) {
        presets = presets.filter { it.name != name }
        Prefs.savePresets(ctx, presets)
    }

    /** Every message, newest first; null until loaded. */
    var all by mutableStateOf<List<Sms>?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var progress by mutableIntStateOf(0)
        private set
    var message by mutableStateOf<String?>(null)

    fun refresh() {
        if (loading) return
        viewModelScope.launch {
            loading = true
            try {
                all = withContext(Dispatchers.IO) { SmsRepo.load(ctx.contentResolver) }
            } catch (e: SecurityException) {
                message = "دسترسی خواندن پیامک داده نشده است."
            }
            loading = false
        }
    }

    /** Deletes [msgs] and reloads the list. */
    fun delete(msgs: List<Sms>) {
        if (busy || msgs.isEmpty()) return
        viewModelScope.launch {
            busy = true
            progress = 0
            message = null
            lastBatch = null
            val res = runCatching {
                withContext(Dispatchers.IO) {
                    // If the backup fails this throws and nothing is deleted.
                    if (backupOn) Backup.saveBatch(ctx, msgs)
                    SmsRepo.delete(ctx.contentResolver, msgs.map { it.id }) { progress = it }
                }
            }
            message = res.fold(
                { if (it == 0) "هیچ پیامکی حذف نشد؛ مطمئن شو این برنامه پیش‌فرض پیامک است." else "$it پیامک حذف شد." },
                { "خطا: ${it.message}" },
            )
            if (backupOn && res.getOrDefault(0) > 0) lastBatch = Backup.batches(ctx).firstOrNull()
            reloadBatches()
            busy = false
            loading = false
            refresh()
        }
    }
}
