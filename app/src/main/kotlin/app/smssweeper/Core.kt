package app.smssweeper

import android.content.ContentResolver
import android.provider.Telephony
import java.time.Instant
import java.time.ZoneId
import java.util.Date

enum class Box(val label: String) { ALL("همه"), INBOX("دریافتی"), SENT("ارسالی") }

enum class ReadState(val label: String) { ANY("همه"), UNREAD("خوانده‌نشده"), READ("خوانده‌شده") }

data class Sms(
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int,
    val read: Boolean = true,
)

/** All set conditions must match (AND). Dates are epoch millis in local time. */
data class Criteria(
    val box: Box = Box.ALL,
    val before: Long? = null,
    val from: Long? = null,
    /** exclusive upper bound */
    val to: Long? = null,
    val olderThanDays: Int? = null,
    val sender: String = "",
    val keyword: String = "",
    val regex: Boolean = false,
    val read: ReadState = ReadState.ANY,
    val spam: Boolean = false,
    /** applied last: only the oldest N of what matched */
    val oldestN: Int? = null,
    /** applied last: everything that matched except the newest N */
    val keepLatestN: Int? = null,
) {
    val hasCondition: Boolean
        get() = box != Box.ALL || before != null || from != null || to != null || olderThanDays != null ||
            sender.isNotBlank() || keyword.isNotBlank() || read != ReadState.ANY || spam || oldestN != null || keepLatestN != null
}

/** User-managed lists. Senders match by "contains" (so a partial name or number prefix works). */
data class Lists(
    /** never deleted by any rule */
    val protectedSenders: List<String> = emptyList(),
    /** always counted as spam */
    val blockedSenders: List<String> = emptyList(),
    /** extra ad keywords on top of the built-in ones */
    val spamWords: List<String> = emptyList(),
) {
    fun isProtected(address: String) = protectedSenders.any { it.isNotBlank() && address.contains(it, true) }
    fun isBlocked(address: String) = blockedSenders.any { it.isNotBlank() && address.contains(it, true) }
}

private const val DAY = 86_400_000L

object SmsRepo {
    /** Every message, newest first. */
    fun load(cr: ContentResolver): List<Sms> {
        val out = ArrayList<Sms>()
        cr.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf("_id", "address", "body", "date", "type", "read"),
            null, null, "date DESC"
        )?.use { c ->
            while (c.moveToNext()) {
                out += Sms(
                    c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty(),
                    c.getLong(3), c.getInt(4), c.getInt(5) != 0,
                )
            }
        }
        return out
    }

    /** [all] must be newest first. */
    fun select(
        all: List<Sms>,
        c: Criteria,
        lists: Lists = Lists(),
        now: Long = System.currentTimeMillis(),
    ): List<Sms> {
        val s = matcher(c.sender, c.regex)
        val k = matcher(c.keyword, c.regex)
        val cutoff = c.olderThanDays?.let { now - it * DAY }
        val hits = all.filter { m ->
            val inBox = when (c.box) {
                Box.ALL -> true
                Box.INBOX -> m.type == Telephony.Sms.MESSAGE_TYPE_INBOX
                Box.SENT -> m.type == Telephony.Sms.MESSAGE_TYPE_SENT
            }
            inBox &&
                !lists.isProtected(m.address) &&
                (c.before == null || m.date < c.before) &&
                (c.from == null || m.date >= c.from) &&
                (c.to == null || m.date < c.to) &&
                (cutoff == null || m.date < cutoff) &&
                (s == null || s(m.address)) &&
                (k == null || k(m.body)) &&
                (c.read == ReadState.ANY || (c.read == ReadState.READ) == m.read) &&
                (!c.spam || isSpam(m, lists))
        }
        return when {
            c.oldestN != null -> hits.takeLast(c.oldestN)
            c.keepLatestN != null -> hits.drop(c.keepLatestN)
            else -> hits
        }
    }

    /** Deletes in batches; returns number actually deleted. */
    fun delete(cr: ContentResolver, ids: List<Long>, onProgress: (Int) -> Unit): Int {
        var n = 0
        for (chunk in ids.chunked(400)) {
            n += cr.delete(Telephony.Sms.CONTENT_URI, "_id IN (${chunk.joinToString(",")})", null)
            onProgress(n)
        }
        return n
    }

    // ponytail: keyword heuristic, upgrade to user-editable lists / on-device model if it misses too much
    private val adWords = listOf(
        "لغو11", "لغو 11", "لغو۱۱", "لغو ۱۱", "تخفیف", "جایزه", "برنده", "قرعه", "شرط بندی", "شرط‌بندی",
        "کازینو", "وام فوری", "فروش ویژه", "پیشنهاد ویژه", "هدیه", "رایگان", "http", "www.",
        "winner", "free", "casino", "promo", "click", "bet365"
    )
    private val otp = Regex("رمز|کد تایید|کد تأیید|کد ورود|کد فعال|otp|verification|verify", RegexOption.IGNORE_CASE)

    fun isSpam(m: Sms, lists: Lists = Lists()): Boolean {
        if (m.type != Telephony.Sms.MESSAGE_TYPE_INBOX) return false
        if (lists.isBlocked(m.address)) return true
        if (otp.containsMatchIn(m.body)) return false // never touch codes/passwords
        val b = m.body.lowercase()
        return adWords.any { b.contains(it) } || lists.spamWords.any { it.isNotBlank() && b.contains(it.lowercase()) }
    }
}

private fun matcher(pattern: String, regex: Boolean): ((String) -> Boolean)? {
    val t = pattern.trim()
    if (t.isEmpty()) return null
    if (!regex) return { s -> s.contains(t, true) }
    val r = runCatching { Regex(t, RegexOption.IGNORE_CASE) }.getOrNull()
    return { s -> r != null && r.containsMatchIn(s) }
}

fun validRegex(p: String) = p.isBlank() || runCatching { Regex(p) }.isSuccess

/** Reads digits in any script (Latin, Persian, Arabic-Indic). */
fun digits(s: String): Int? =
    s.mapNotNull { Character.digit(it, 10).takeIf { d -> d >= 0 } }.joinToString("").toIntOrNull()

// ---- dates ----
private val zone: ZoneId get() = ZoneId.systemDefault()

fun dayAfter(local: Long): Long =
    Instant.ofEpochMilli(local).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

fun iso(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toString()

private val jalaliFmt by lazy {
    android.icu.text.SimpleDateFormat("d MMMM yyyy", android.icu.util.ULocale("fa_IR@calendar=persian"))
}

/** Main-thread only (SimpleDateFormat is not thread-safe). */
fun jalali(ms: Long): String = jalaliFmt.format(Date(ms))
