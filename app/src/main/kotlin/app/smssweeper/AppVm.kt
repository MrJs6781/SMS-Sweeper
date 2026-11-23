package app.smssweeper

import android.app.Application
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
            val res = runCatching {
                withContext(Dispatchers.IO) {
                    SmsRepo.delete(ctx.contentResolver, msgs.map { it.id }) { progress = it }
                }
            }
            message = res.fold(
                { if (it == 0) "هیچ پیامکی حذف نشد؛ مطمئن شو این برنامه پیش‌فرض پیامک است." else "$it پیامک حذف شد." },
                { "خطا: ${it.message}" },
            )
            busy = false
            loading = false
            refresh()
        }
    }
}
