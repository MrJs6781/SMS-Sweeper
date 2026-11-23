package app.smssweeper

import android.content.ContentResolver
import android.provider.Telephony
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date

enum class Box(val label: String) { ALL("همه"), INBOX("دریافتی"), SENT("ارسالی") }

data class Sms(val id: Long, val address: String, val body: String, val date: Long, val type: Int)

/** What to delete. Date values are epoch millis in local time zone. */
sealed interface Rule {
    data class Before(val ts: Long) : Rule
    /** [from, to) */
    data class Range(val from: Long, val to: Long) : Rule
    data class OldestN(val n: Int) : Rule
    data class KeepLatestN(val n: Int) : Rule
    data object Spam : Rule
    data class Match(val sender: String, val keyword: String) : Rule
}

object SmsRepo {
    /** All messages of [box], newest first. */
    fun load(cr: ContentResolver, box: Box): List<Sms> {
        val where = when (box) {
            Box.ALL -> null
            Box.INBOX -> "type=${Telephony.Sms.MESSAGE_TYPE_INBOX}"
            Box.SENT -> "type=${Telephony.Sms.MESSAGE_TYPE_SENT}"
        }
        val out = ArrayList<Sms>()
        cr.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf("_id", "address", "body", "date", "type"),
            where, null, "date DESC"
        )?.use { c ->
            while (c.moveToNext()) {
                out += Sms(c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty(), c.getLong(3), c.getInt(4))
            }
        }
        return out
    }

    /** [all] must be newest first. */
    fun select(all: List<Sms>, rule: Rule): List<Sms> = when (rule) {
        is Rule.Before -> all.filter { it.date < rule.ts }
        is Rule.Range -> all.filter { it.date >= rule.from && it.date < rule.to }
        is Rule.OldestN -> all.takeLast(rule.n)
        is Rule.KeepLatestN -> all.drop(rule.n)
        Rule.Spam -> all.filter { isSpam(it) }
        is Rule.Match -> {
            val s = rule.sender.trim()
            val k = rule.keyword.trim()
            if (s.isEmpty() && k.isEmpty()) emptyList()
            else all.filter {
                (s.isEmpty() || it.address.contains(s, true)) && (k.isEmpty() || it.body.contains(k, true))
            }
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

    fun isSpam(m: Sms): Boolean {
        if (m.type != Telephony.Sms.MESSAGE_TYPE_INBOX) return false
        if (otp.containsMatchIn(m.body)) return false // never touch codes/passwords
        val b = m.body.lowercase()
        return adWords.any { b.contains(it) }
    }
}

// ---- dates ----
private val zone: ZoneId get() = ZoneId.systemDefault()

/** Material date picker returns UTC midnight; convert to local midnight. */
fun fromPicker(utc: Long): Long =
    Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

fun toPicker(local: Long): Long =
    Instant.ofEpochMilli(local).atZone(zone).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun dayAfter(local: Long): Long =
    Instant.ofEpochMilli(local).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

fun iso(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toString()

private val jalaliFmt by lazy {
    android.icu.text.SimpleDateFormat("d MMMM yyyy", android.icu.util.ULocale("fa_IR@calendar=persian"))
}

/** Main-thread only (SimpleDateFormat is not thread-safe). */
fun jalali(ms: Long): String = jalaliFmt.format(Date(ms))
