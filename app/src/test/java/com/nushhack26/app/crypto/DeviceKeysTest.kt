package com.nushhack26.app.crypto

import android.app.Application
import android.app.KeyguardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DeviceKeysTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun noScreenLock() {
        shadowOf(context.getSystemService(KeyguardManager::class.java)).setIsDeviceSecure(false)
    }

    @Test fun softwareKeyPersistsSignsAndRotates() {
        val keys = deviceKeys(context)
        assertTrue(keys is SoftwareDeviceKeys)
        val created = keys.createKey()
        assertEquals("software", created.security)
        assertEquals(Signatures.keyFp(created.publicKey), created.keyFp)
        val loaded = deviceKeys(context)
        assertEquals(created, loaded.keyInfo())
        val signature = loaded.sign("CS1 offline handover")
        assertTrue(Signatures.verify(created.publicKey, "CS1 offline handover", signature))
        assertFalse(Signatures.verify(created.publicKey, "changed handover", signature))
        val replacement = keys.createKey()
        assertNotEquals(created.publicKey, replacement.publicKey)
        assertFalse(Signatures.verify(replacement.publicKey, "CS1 offline handover", signature))
    }

    @Test fun aSecurePhoneNeverUsesTheSoftwareFallback() {
        shadowOf(context.getSystemService(KeyguardManager::class.java)).setIsDeviceSecure(true)
        assertTrue(deviceKeys(context) is KeystoreDeviceKeys)
        assertThrows(IllegalStateException::class.java) { SoftwareDeviceKeys(context).createKey() }
        assertThrows(IllegalStateException::class.java) { SoftwareDeviceKeys(context).sign("not allowed") }
    }

    @Test fun keystoreRefusesToCreateWithoutASecureLock() {
        assertThrows(NoSecureLockException::class.java) { KeystoreDeviceKeys(context).createKey() }
    }
}
