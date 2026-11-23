package app.smssweeper

import android.content.Context
import android.provider.Telephony
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Periodically applies the user's chosen presets (see Settings). */
object AutoClean {
    private const val WORK = "auto-clean"

    /** (Re)schedules or cancels the periodic job to match the saved settings. */
    fun schedule(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        if (!Prefs.autoOn(ctx)) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val req = PeriodicWorkRequestBuilder<AutoCleanWorker>(Prefs.autoEvery(ctx).toLong(), TimeUnit.DAYS).build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    /** Runs the selected presets once. Blocking; call off the main thread. Returns a log line. */
    fun run(ctx: Context): String {
        if (Telephony.Sms.getDefaultSmsPackage(ctx) != ctx.packageName) {
            return "اجرا نشد: SMS Sweeper در آن لحظه برنامه‌ی پیش‌فرض پیامک نبود."
        }
        val names = Prefs.autoPresets(ctx)
        val presets = Prefs.presets(ctx).filter { it.name in names }
        if (presets.isEmpty()) return "اجرا نشد: هیچ قانونی برای پاکسازی خودکار انتخاب نشده است."

        val cr = ctx.contentResolver
        val all = SmsRepo.load(cr)
        val lists = Prefs.lists(ctx)
        val victims = presets.flatMap { SmsRepo.select(all, it.criteria, lists) }.distinctBy { it.id }
        if (victims.isEmpty()) return "پیامکی برای حذف پیدا نشد."

        if (Prefs.backupOn(ctx)) Backup.saveBatch(ctx, victims) // throws -> nothing deleted
        val n = SmsRepo.delete(cr, victims.map { it.id }) { }
        Backup.purge(ctx, Prefs.retention(ctx))
        return "$n پیامک حذف شد."
    }
}

class AutoCleanWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val msg = runCatching { AutoClean.run(applicationContext) }.getOrElse { "خطا: ${it.message}" }
        Prefs.setAutoLog(applicationContext, System.currentTimeMillis(), msg)
        return Result.success()
    }
}
