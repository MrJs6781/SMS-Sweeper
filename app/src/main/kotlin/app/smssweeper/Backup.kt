package app.smssweeper

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.provider.Telephony
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/** One saved set of messages (what a delete removed). */
class Batch(val file: File, val createdAt: Long, val count: Int)

/**
 * JSON backups. Files in [dir] are the in-app "trash": one file per delete,
 * named `b_<createdAtMillis>_<count>.json`.
 */
object Backup {
    private fun dir(ctx: Context) = File(ctx.filesDir, "backups").apply { mkdirs() }
    private val name = Regex("""b_(\d+)_(\d+)\.json""")

    fun write(out: OutputStream, msgs: List<Sms>, reason: String) {
        val arr = JSONArray()
        for (m in msgs) {
            arr.put(JSONObject().put("a", m.address).put("b", m.body).put("d", m.date).put("t", m.type).put("r", if (m.read) 1 else 0))
        }
        val root = JSONObject().put("version", 1).put("app", "SMS Sweeper")
            .put("createdAt", System.currentTimeMillis()).put("reason", reason).put("messages", arr)
        out.write(root.toString().toByteArray(Charsets.UTF_8))
    }

    /** Throws if the file is not a valid backup. */
    fun read(input: InputStream): List<Sms> {
        val arr = JSONObject(input.readBytes().toString(Charsets.UTF_8)).getJSONArray("messages")
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Sms(0, o.getString("a"), o.getString("b"), o.getLong("d"), o.getInt("t"), o.optInt("r", 1) != 0)
        }
    }

    fun saveBatch(ctx: Context, msgs: List<Sms>): File {
        val f = File(dir(ctx), "b_${System.currentTimeMillis()}_${msgs.size}.json")
        f.outputStream().use { write(it, msgs, "delete") }
        return f
    }

    /** Newest first. */
    fun batches(ctx: Context): List<Batch> =
        dir(ctx).listFiles().orEmpty().mapNotNull { f ->
            name.matchEntire(f.name)?.let { Batch(f, it.groupValues[1].toLong(), it.groupValues[2].toInt()) }
        }.sortedByDescending { it.createdAt }

    /** Drops batches older than [days]. */
    fun purge(ctx: Context, days: Int, now: Long = System.currentTimeMillis()) {
        batches(ctx).filter { it.createdAt < now - days * 86_400_000L }.forEach { it.file.delete() }
    }

    private fun key(m: Sms) = "${m.date}|${m.address}|${m.body}"

    /** Inserts messages that aren't already present; needs default-SMS role. Returns inserted count. */
    fun restore(cr: ContentResolver, msgs: List<Sms>): Int {
        val have = SmsRepo.load(cr).mapTo(HashSet()) { key(it) }
        var n = 0
        for (m in msgs) {
            if (!have.add(key(m))) continue
            val v = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, m.address)
                put(Telephony.Sms.BODY, m.body)
                put(Telephony.Sms.DATE, m.date)
                put(Telephony.Sms.TYPE, m.type)
                put(Telephony.Sms.READ, if (m.read) 1 else 0)
                put(Telephony.Sms.SEEN, 1)
            }
            if (cr.insert(Telephony.Sms.CONTENT_URI, v) != null) n++
        }
        return n
    }
}
