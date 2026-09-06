package dev.mitin.internal

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keystore contains the AES key, NOT the refresh string. Ciphertext is no-backup. */
class KeystoreRefreshStore(context: Context, namespace: String = "auth") : RefreshStore {
    init { require(Regex("[a-z0-9_]{1,60}").matches(namespace)) }
    private val file = AtomicFile(File(context.noBackupFilesDir, "$namespace-session-v1.bin"))
    private val alias = "${context.packageName}.$namespace.aes.v1"
    private val aad = "${context.packageName}|auth-v1".toByteArray(Charsets.UTF_8)
    private fun keystore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun key(create: Boolean): SecretKey {
        val existing = keystore().getKey(alias, null)
        if (existing is SecretKey) return existing
        if (!create) throw IllegalStateException("Secure session unavailable")
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    private fun wipe() { file.delete(); keystore().deleteEntry(alias) }
    private fun write(record: JsonObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(true)) // provider creates a fresh random IV every write
        cipher.updateAAD(aad)
        val encrypted = cipher.doFinal(record.toString().toByteArray(Charsets.UTF_8))
        val blob = buildJsonObject {
            put("v", 1); put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            put("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        }.toString().toByteArray(Charsets.UTF_8)
        val output = file.startWrite()
        try {
            output.write(blob); file.finishWrite(output)
            // AtomicFile can log a failed rename without throwing on some API levels.
            // Confirm the committed ciphertext before making the new pair usable.
            if (!file.readFully().contentEquals(blob)) throw AuthFailure()
        }
        catch (_: Exception) { file.failWrite(output); wipe(); throw AuthFailure() }
    }
    override suspend fun read(): StoredSession? = withContext(Dispatchers.IO) {
        if (!file.baseFile.exists()) return@withContext null
        try {
            if (file.baseFile.length() > 4096) throw AuthFailure()
            val blob = Json.parseToJsonElement(file.readFully().toString(Charsets.UTF_8)).jsonObject
            if (blob.getValue("v").jsonPrimitive.int != 1) throw AuthFailure()
            val iv = Base64.decode(blob.getValue("iv").jsonPrimitive.content, Base64.NO_WRAP)
            if (iv.size != 12) throw AuthFailure()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(false), GCMParameterSpec(128, iv)); cipher.updateAAD(aad)
            val clear = cipher.doFinal(Base64.decode(blob.getValue("ciphertext").jsonPrimitive.content, Base64.NO_WRAP))
            val record = Json.parseToJsonElement(clear.toString(Charsets.UTF_8)).jsonObject
            clear.fill(0)
            if (record.getValue("state").jsonPrimitive.content != "ready") {
                wipe(); StoredSession(null, exchanging = true)
            } else {
                val token = record.getValue("refresh").jsonPrimitive.content
                if (!Regex("mdr1_[A-Za-z0-9_-]{43}").matches(token)) throw AuthFailure()
                StoredSession(Secret(token))
            }
        } catch (_: Exception) { wipe(); StoredSession(null, exchanging = true) }
    }
    override suspend fun save(refresh: Secret) = withContext(Dispatchers.IO) {
        try { write(buildJsonObject { put("state", "ready"); put("refresh", refresh.value) }) }
        catch (_: Exception) { wipe(); throw AuthFailure() }
    }
    override suspend fun beginExchange() = withContext(Dispatchers.IO) {
        try { write(buildJsonObject { put("state", "exchanging") }) }
        catch (_: Exception) { wipe(); throw AuthFailure() }
    }
    override suspend fun clear() = withContext(Dispatchers.IO) { wipe() }
}
