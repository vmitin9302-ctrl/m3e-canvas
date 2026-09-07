package dev.mitin.internal

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Anonymous session and unacknowledged command are encrypted before transmission. */
class BriefStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "mobile-brief-v1.bin"))
    private val alias = "${context.packageName}.brief.v1"
    private val aad = alias.toByteArray()
    private fun key(create: Boolean): SecretKey {
        val existing = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.getKey(alias, null)
        if(existing is SecretKey) return existing
        check(create)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    suspend fun read(): BriefRecord? = withContext(Dispatchers.IO) {
        if(!file.baseFile.exists()) return@withContext null
        check(file.baseFile.length() in 29..65536)
        val bytes = file.readFully()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(false), GCMParameterSpec(128, bytes.copyOfRange(0,12))); cipher.updateAAD(aad)
        val clear = cipher.doFinal(bytes.copyOfRange(12, bytes.size))
        try { Json.decodeFromString<BriefRecord>(clear.toString(Charsets.UTF_8)) } finally { clear.fill(0) }
    }
    suspend fun save(record: BriefRecord) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(true)); cipher.updateAAD(aad)
        val clear = Json.encodeToString(record).toByteArray(Charsets.UTF_8)
        val bytes = try { cipher.iv + cipher.doFinal(clear) } finally { clear.fill(0) }
        val out = file.startWrite()
        try { out.write(bytes); file.finishWrite(out); check(file.readFully().contentEquals(bytes)) }
        catch(e: Exception) { file.failWrite(out); throw e }
    }
    suspend fun clear() = withContext(Dispatchers.IO) { file.delete() }
}
