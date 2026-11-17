package com.tagtrack.nfc.nfc

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.Charset
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

    suspend fun readTag(tag: Tag): Result<Int> = withContext(Dispatchers.IO) {
        // 1. Try reading via Ndef
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
                if (message != null && message.records.isNotEmpty()) {
                    val record = message.records[0]
                    val payload = record.payload
                    if (payload != null && payload.isNotEmpty()) {
                        val text = parseTextRecord(record) ?: String(payload, Charsets.UTF_8)
                        val code = text.trim().toIntOrNull()
                        if (code != null && code > 0) {
                            return@withContext Result.success(code)
                        }
                    }
                }
            } catch (_: Exception) {
                // Fall through to NfcA / MifareUltralight
            } finally {
                try { ndef.close() } catch (_: Exception) {}
            }
        }

        // 2. Try reading via MifareUltralight (NFC-A)
        val mifare = MifareUltralight.get(tag)
        if (mifare != null) {
            try {
                mifare.connect()
                // MIFARE Ultralight user memory starts at page 4 (4 pages x 4 bytes = 16 bytes)
                val payloadBytes = mifare.readPages(4)
                if (payloadBytes != null && payloadBytes.isNotEmpty()) {
                    val text = String(payloadBytes, Charsets.UTF_8).trim('\u0000', ' ')
                    val code = text.toIntOrNull()
                    if (code != null && code > 0) {
                        return@withContext Result.success(code)
                    }
                }
            } catch (_: Exception) {
                // Fall through
            } finally {
                try { mifare.close() } catch (_: Exception) {}
            }
        }

        // 3. Try reading via NfcA basic tech or check if tag is NFC-A
        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            try {
                nfcA.connect()
                return@withContext Result.failure(IOException("NFC-A / MIFARE tag is blank or unformatted. Write an item code to it first."))
            } catch (e: TagLostException) {
                return@withContext Result.failure(IOException("Tag was lost. Hold it still against the back of the phone and try again."))
            } catch (e: IOException) {
                return@withContext Result.failure(IOException("Tag I/O error occurred. Try again."))
            } catch (e: Exception) {
                return@withContext Result.failure(IOException("Tag is blank or unformatted."))
            } finally {
                try { nfcA.close() } catch (_: Exception) {}
            }
        }

        return@withContext Result.failure(IOException("This tag is blank, unformatted, or unsupported. Write an item code to it first."))
    }

    suspend fun writeTag(
        tag: Tag,
        nfcCode: Int,
        onConfirmOverwrite: ((existingCode: Int) -> Boolean)? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val text = nfcCode.toString()
        val langBytes = "en".toByteArray(Charsets.US_ASCII)
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val status = langBytes.size.toByte()
        val data = ByteArray(1 + langBytes.size + textBytes.size)
        data[0] = status
        System.arraycopy(langBytes, 0, data, 1, langBytes.size)
        System.arraycopy(textBytes, 0, data, 1 + langBytes.size, textBytes.size)
        val record = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), data)
        val ndefMessage = NdefMessage(arrayOf(record))
        val messageSize = ndefMessage.toByteArray().size

        // 1. Try Ndef writing if supported and writable
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                if (!ndef.isWritable) {
                    return@withContext Result.failure(IOException("This tag is read-only. Nothing was written."))
                }
                if (ndef.maxSize < messageSize) {
                    return@withContext Result.failure(IOException("This tag is too small to hold the code."))
                }

                // Check existing message for overwrite confirmation
                val existingMessage = ndef.ndefMessage ?: ndef.cachedNdefMessage
                if (existingMessage != null && existingMessage.records.isNotEmpty()) {
                    val existingText = parseTextRecord(existingMessage.records[0])
                    val existingCode = existingText?.trim()?.toIntOrNull()
                    if (existingCode != null && existingCode > 0) {
                        if (onConfirmOverwrite != null && !onConfirmOverwrite(existingCode)) {
                            return@withContext Result.failure(IOException("Overwrite cancelled."))
                        }
                    }
                }

                ndef.writeNdefMessage(ndefMessage)

                // Read-back verification
                val readMessage = ndef.ndefMessage ?: ndef.cachedNdefMessage
                if (readMessage == null || readMessage.records.isEmpty()) {
                    return@withContext Result.failure(IOException("Verification failed. Try again."))
                }
                val readCode = parseTextRecord(readMessage.records[0])?.trim()?.toIntOrNull()
                if (readCode != nfcCode) {
                    return@withContext Result.failure(IOException("Verification failed. Try again."))
                }
                return@withContext Result.success(Unit)
            } catch (e: TagLostException) {
                return@withContext Result.failure(IOException("Tag was lost. Hold it still against the back of the phone and try again."))
            } catch (e: IOException) {
                // Fall through to formatable / NfcA / MifareUltralight
            } catch (e: Exception) {
                return@withContext Result.failure(IOException(e.localizedMessage ?: "Verification failed. Try again."))
            } finally {
                try { ndef.close() } catch (_: Exception) {}
            }
        }

        // 2. Try NdefFormatable if tag is unformatted
        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            try {
                formatable.connect()
                formatable.format(ndefMessage)
                return@withContext Result.success(Unit)
            } catch (e: TagLostException) {
                return@withContext Result.failure(IOException("Tag was lost. Hold it still against the back of the phone and try again."))
            } catch (e: IOException) {
                // Fall through to MifareUltralight / NfcA
            } catch (e: Exception) {
                return@withContext Result.failure(IOException(e.localizedMessage ?: "Formatting failed."))
            } finally {
                try { formatable.close() } catch (_: Exception) {}
            }
        }

        // 3. Try MifareUltralight (NFC-A) direct page writing
        val mifare = MifareUltralight.get(tag)
        if (mifare != null) {
            try {
                mifare.connect()
                val textBytesUsAscii = text.toByteArray(Charset.forName("US-ASCII"))
                // Write across pages 4, 5, 6, 7 (each page is 4 bytes)
                val padded = ByteArray(16) { 0 }
                System.arraycopy(textBytesUsAscii, 0, padded, 0, minOf(textBytesUsAscii.size, 16))
                for (i in 0 until 4) {
                    val pageData = padded.copyOfRange(i * 4, (i + 1) * 4)
                    mifare.writePage(4 + i, pageData)
                }
                return@withContext Result.success(Unit)
            } catch (e: TagLostException) {
                return@withContext Result.failure(IOException("Tag was lost. Hold it still against the back of the phone and try again."))
            } catch (e: IOException) {
                return@withContext Result.failure(IOException("MIFARE Ultralight write error. Tag may be locked or read-only."))
            } catch (e: Exception) {
                return@withContext Result.failure(IOException(e.localizedMessage ?: "MIFARE write failed."))
            } finally {
                try { mifare.close() } catch (_: Exception) {}
            }
        }

        // 4. Try NfcA generic
        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            try {
                nfcA.connect()
                return@withContext Result.failure(IOException("NFC-A tag connected but does not support NDEF formatting or writing."))
            } catch (e: Exception) {
                return@withContext Result.failure(IOException("Failed to write NFC-A tag."))
            } finally {
                try { nfcA.close() } catch (_: Exception) {}
            }
        }

        return@withContext Result.failure(IOException("This tag does not support NDEF, NFC-A, or writing."))
    }

    suspend fun eraseTag(tag: Tag): Result<Unit> = withContext(Dispatchers.IO) {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                if (!ndef.isWritable) {
                    return@withContext Result.failure(IOException("This tag is read-only. Cannot erase."))
                }
                val emptyMessage = NdefMessage(arrayOf(NdefRecord(NdefRecord.TNF_EMPTY, ByteArray(0), ByteArray(0), ByteArray(0))))
                ndef.writeNdefMessage(emptyMessage)
                return@withContext Result.success(Unit)
            } catch (e: TagLostException) {
                return@withContext Result.failure(IOException("Tag was lost. Hold it still against the back of the phone and try again."))
            } catch (e: IOException) {
                // Fall through
            } catch (e: Exception) {
                return@withContext Result.failure(IOException(e.localizedMessage ?: "Failed to erase tag."))
            } finally {
                try { ndef.close() } catch (_: Exception) {}
            }
        }

        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            try {
                formatable.connect()
                val emptyMessage = NdefMessage(arrayOf(NdefRecord(NdefRecord.TNF_EMPTY, ByteArray(0), ByteArray(0), ByteArray(0))))
                formatable.format(emptyMessage)
                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                // Fall through
            } finally {
                try { formatable.close() } catch (_: Exception) {}
            }
        }

        val mifare = MifareUltralight.get(tag)
        if (mifare != null) {
            try {
                mifare.connect()
                val zeros = ByteArray(4) { 0 }
                for (i in 0 until 4) {
                    mifare.writePage(4 + i, zeros)
                }
                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                return@withContext Result.failure(IOException("Failed to clear MIFARE tag memory."))
            } finally {
                try { mifare.close() } catch (_: Exception) {}
            }
        }

        return@withContext Result.failure(IOException("This tag does not support erasing or formatting."))
    }

    private fun parseTextRecord(record: NdefRecord): String? {
        if (record.tnf != NdefRecord.TNF_WELL_KNOWN || !record.type.contentEquals(NdefRecord.RTD_TEXT)) {
            try {
                return String(record.payload, Charsets.UTF_8)
            } catch (_: Exception) {
                return null
            }
        }
        val payload = record.payload ?: return null
        if (payload.isEmpty()) return null
        val status = payload[0].toInt()
        val textEncoding = if ((status and 128) == 0) Charsets.UTF_8 else Charsets.UTF_16
        val languageCodeLength = status and 63
        if (1 + languageCodeLength > payload.size) return null
        return String(payload, 1 + languageCodeLength, payload.size - 1 - languageCodeLength, textEncoding)
    }
}
