package com.tagtrack.nfc.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class NfcTagIO(private val activity: Activity) {
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private val isOperationInProgress = AtomicBoolean(false)

    fun checkNfcAvailability(): Result<Unit> {
        if (nfcAdapter == null) {
            return Result.failure(IllegalStateException("This phone has no NFC hardware."))
        }
        if (!nfcAdapter.isEnabled) {
            return Result.failure(IllegalStateException("NFC is turned off. Turn it on in phone settings."))
        }
        return Result.success(Unit)
    }

    fun enableReaderMode(
        onTagScanned: (Tag) -> Unit,
        onError: (String) -> Unit
    ) {
        val availability = checkNfcAvailability()
        if (availability.isFailure) {
            onError(availability.exceptionOrNull()?.message ?: "NFC unavailable")
            return
        }

        try {
            nfcAdapter?.enableReaderMode(
                activity,
                { tag ->
                    if (isOperationInProgress.compareAndSet(false, true)) {
                        try {
                            onTagScanned(tag)
                        } finally {
                            isOperationInProgress.set(false)
                        }
                    }
                },
                NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or
                        NfcAdapter.FLAG_READER_NFC_V or
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to enable reader mode")
        }
    }

    fun disableReaderMode() {
        try {
            nfcAdapter?.disableReaderMode(activity)
        } catch (e: Exception) {
            // ignore
        }
    }

    suspend fun readTagUid(tag: Tag): Result<String> = withContext(Dispatchers.IO) {
        val id = tag.id
        if (id == null || id.isEmpty()) {
            return@withContext Result.failure(IOException("Tag has no readable hardware UID."))
        }
        return@withContext Result.success(id.toHexString())
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02X".format(it) }
    }
}
