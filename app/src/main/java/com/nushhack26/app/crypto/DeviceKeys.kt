package com.nushhack26.app.crypto

import android.content.Context
import android.app.KeyguardManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo as AndroidKeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.AtomicFile
import com.nushhack26.app.BuildConfig
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

// This phone's signing key (docs/sync.md A.1). Card 01 fills in the bodies; the
// signatures here are what the other cards call.

/** The public half of this phone's key, as it goes on the roster. */
data class KeyInfo(
    /** X.509 SubjectPublicKeyInfo, base64url without padding. */
    val publicKey: String,
    /** For example 3F:A9:12:7C. */
    val keyFp: String,
    /** strongbox, tee or software. */
    val security: String,
)

interface DeviceKeys {
    /** The key, or null if this phone hasn't made one yet. */
    fun keyInfo(): KeyInfo?

    /**
     * Creates the key, replacing any old one. Release builds throw [NoSecureLockException]
     * on a phone without a fingerprint or PIN.
     */
    fun createKey(): KeyInfo

    /**
     * Signs [text] (its UTF-8 bytes) with SHA256withECDSA and returns base64url DER.
     * Call it within 10 seconds of a successful SignPrompt, or it throws.
     */
    fun sign(text: String): String
}

class NoSecureLockException : Exception("Set a fingerprint or PIN on this phone to sign handovers.")

/** The Keystore key, or in debug builds on a phone with no screen lock, a software key. */
fun deviceKeys(context: Context): DeviceKeys =
    if (BuildConfig.DEBUG && !context.hasSecureLock()) SoftwareDeviceKeys(context.applicationContext)
    else KeystoreDeviceKeys(context.applicationContext)

private fun Context.hasSecureLock(): Boolean = getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

class KeystoreDeviceKeys(private val context: Context) : DeviceKeys {
    private fun store(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    override fun keyInfo(): KeyInfo? {
        val store = store()
        val publicKey = store.getCertificate(ALIAS)?.publicKey ?: return null
        val privateKey = store.getKey(ALIAS, null) ?: return null
        val info = KeyFactory.getInstance("EC", "AndroidKeyStore").getKeySpec(privateKey, AndroidKeyInfo::class.java)
        val security = if (Build.VERSION.SDK_INT >= 31) {
            when (info.securityLevel) {
                KeyProperties.SECURITY_LEVEL_STRONGBOX -> "strongbox"
                KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> "tee"
                else -> "software"
            }
        } else {
            @Suppress("DEPRECATION")
            if (info.isInsideSecureHardware) "tee" else "software"
        }
        val encoded = encode(publicKey.encoded)
        return KeyInfo(encoded, Signatures.keyFp(encoded), security)
    }

    override fun createKey(): KeyInfo {
        if (!context.hasSecureLock()) throw NoSecureLockException()
        store().deleteEntry(ALIAS)
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                generate(strongBox = true)
            } catch (_: StrongBoxUnavailableException) {
                generate(strongBox = false)
            }
        } else {
            generate(strongBox = false)
        }
        return checkNotNull(keyInfo())
    }

    private fun generate(strongBox: Boolean) {
        val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(true)
        if (Build.VERSION.SDK_INT >= 30) {
            spec.setUserAuthenticationParameters(10, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
        } else {
            @Suppress("DEPRECATION")
            spec.setUserAuthenticationValidityDurationSeconds(10)
        }
        if (Build.VERSION.SDK_INT >= 28) spec.setIsStrongBoxBacked(strongBox)
        KeyPairGenerator.getInstance("EC", "AndroidKeyStore").apply { initialize(spec.build()) }.generateKeyPair()
    }

    override fun sign(text: String): String = Signature.getInstance("SHA256withECDSA").run {
        val privateKey = store().getKey(ALIAS, null) as? java.security.PrivateKey
            ?: error("Create this phone's signing key first")
        initSign(privateKey)
        update(text.toByteArray(Charsets.UTF_8))
        encode(sign())
    }

    private companion object {
        const val ALIAS = "countersign-device-key"
    }
}

/** Only for debug phones with no lock; excluded from Android backup. */
class SoftwareDeviceKeys(private val context: Context) : DeviceKeys {
    private val file get() = AtomicFile(File(context.noBackupFilesDir, "countersign-test-key"))

    private fun requireTestPhone() {
        check(BuildConfig.DEBUG && !context.hasSecureLock()) { "Software keys are only for debug phones without a secure lock" }
    }

    private fun read(): KeyPair? {
        requireTestPhone()
        if (!file.baseFile.exists()) return null
        return DataInputStream(file.openRead()).use { input ->
            fun bytes(): ByteArray {
                val size = input.readInt()
                require(size in 1..4096) { "Invalid test key" }
                return ByteArray(size).also { input.readFully(it) }
            }
            val factory = KeyFactory.getInstance("EC")
            val publicKey = factory.generatePublic(X509EncodedKeySpec(bytes()))
            val privateKey = factory.generatePrivate(PKCS8EncodedKeySpec(bytes()))
            KeyPair(publicKey, privateKey)
        }
    }

    override fun keyInfo(): KeyInfo? = read()?.let { pair ->
        val publicKey = encode(pair.public.encoded)
        KeyInfo(publicKey, Signatures.keyFp(publicKey), "software")
    }

    override fun createKey(): KeyInfo {
        requireTestPhone()
        val pair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val atomicFile = file
        // On Windows (the build laptop's unit tests) a rename can't replace an existing file,
        // so the old key would survive. Replacing the key means deleting the old one first.
        atomicFile.delete()
        val output = atomicFile.startWrite()
        try {
            val data = DataOutputStream(output)
            for (bytes in listOf(pair.public.encoded, pair.private.encoded)) {
                data.writeInt(bytes.size)
                data.write(bytes)
            }
            data.flush()
            atomicFile.finishWrite(output)
        } catch (e: Exception) {
            atomicFile.failWrite(output)
            throw e
        }
        return checkNotNull(keyInfo())
    }

    override fun sign(text: String): String = Signature.getInstance("SHA256withECDSA").run {
        initSign(checkNotNull(read()) { "Create this phone's signing key first" }.private)
        update(text.toByteArray(Charsets.UTF_8))
        encode(sign())
    }
}
