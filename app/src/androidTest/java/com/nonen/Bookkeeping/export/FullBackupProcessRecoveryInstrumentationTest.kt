package com.nonen.Bookkeeping.export

import android.content.Context
import android.os.Process
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nonen.Bookkeeping.data.db.AppDatabase
import com.nonen.Bookkeeping.data.db.AssetEntity
import com.nonen.Bookkeeping.data.db.BookEntity
import com.nonen.Bookkeeping.data.db.TransactionEntity
import com.nonen.Bookkeeping.data.prefs.SettingsStore
import com.nonen.Bookkeeping.data.prefs.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class FullBackupProcessRecoveryInstrumentationTest {

    @Test
    fun interruptRestoreAtCheckpoint(): Unit = runBlocking {
        val checkpoint = requestedCheckpoint()
        val context = validationContext()
        val directory = workDirectory(context, checkpoint)
        if (directory.exists()) check(directory.deleteRecursively())
        check(directory.mkdirs())
        context.deleteDatabase(databaseName(checkpoint))
        val database = openDatabase(context, checkpoint)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val settings = settingsStore(context, directory, scope)
            val attachments = File(directory, FullBackupService.ATTACHMENTS_DIR)
            check(attachments.mkdirs())
            val receipt = File(attachments, "receipt.txt")

            insertFixture(database, restored = true)
            settings.setThemeMode(ThemeMode.DARK)
            settings.setHideAmount(true)
            receipt.writeText(RESTORED_ATTACHMENT)
            val backup = ByteArrayOutputStream()
            val service = FullBackupService(
                database = database,
                settingsStore = settings,
                appContext = context,
                filesDirProvider = { directory },
                interruptionHook = { reached ->
                    if (reached == checkpoint) {
                        val journal = File(directory, FullBackupService.RECOVERY_JOURNAL)
                        check(JSONObject(journal.readText()).getString("state") == checkpoint)
                        val marker = JSONObject().apply {
                            put("checkpoint", checkpoint)
                            put("pid", Process.myPid())
                            put("runId", InstrumentationRegistry.getArguments().getString("recoveryRunId"))
                        }
                        FileOutputStream(File(directory, "interrupted.json")).use { output ->
                            output.write(marker.toString().toByteArray(Charsets.UTF_8))
                            output.fd.sync()
                        }
                        Process.killProcess(Process.myPid())
                        error("进程终止后仍然继续执行")
                    }
                },
            )
            val export = service.exportBackup(backup)
            assertTrue(export.error, export.success)
            database.clearAllTables()
            insertFixture(database, restored = false)
            settings.setThemeMode(ThemeMode.LIGHT)
            settings.setHideAmount(false)
            receipt.writeText(ORIGINAL_ATTACHMENT)
            val result = service.restoreBackup(ByteArrayInputStream(backup.toByteArray()))
            error("未在 $checkpoint 终止进程：$result")
        } finally {
            scope.cancel()
            database.close()
        }
    }

    @Test
    fun verifyRecoveryAfterProcessRestart() = runBlocking {
        val checkpoint = requestedCheckpoint()
        val context = validationContext()
        val directory = workDirectory(context, checkpoint)
        val marker = JSONObject(File(directory, "interrupted.json").readText())
        assertEquals(checkpoint, marker.getString("checkpoint"))
        assertEquals(InstrumentationRegistry.getArguments().getString("recoveryRunId"), marker.getString("runId"))
        assertNotEquals("验证必须来自新进程", marker.getInt("pid"), Process.myPid())
        val journal = File(directory, FullBackupService.RECOVERY_JOURNAL)
        assertEquals(checkpoint, JSONObject(journal.readText()).getString("state"))
        val staging = File(directory, FullBackupService.ATTACHMENTS_STAGING)
        assertEquals(RESTORED_ATTACHMENT, File(staging, "receipt.txt").readText())
        val receipt = File(directory, "${FullBackupService.ATTACHMENTS_DIR}/receipt.txt")
        assertEquals(ORIGINAL_ATTACHMENT, receipt.readText())

        val database = openDatabase(context, checkpoint)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val settings = settingsStore(context, directory, scope)
            assertEquals(ThemeMode.LIGHT, settings.snapshot().themeMode)
            assertFalse(settings.snapshot().hideAmount)
            val committed = checkpoint == FullBackupService.JOURNAL_STAGE_DB_COMMITTED
            assertFixture(database, restored = committed)
            val service = FullBackupService(
                database = database,
                settingsStore = settings,
                appContext = context,
                filesDirProvider = { directory },
            )
            service.checkAndRepairPendingRecovery()
            assertFixture(database, restored = committed)
            assertFalse(journal.exists())
            assertFalse(staging.exists())
            assertFalse(File(directory, FullBackupService.ATTACHMENTS_BACKUP).exists())
            assertEquals(if (committed) RESTORED_ATTACHMENT else ORIGINAL_ATTACHMENT, receipt.readText())
            assertEquals(if (committed) ThemeMode.DARK else ThemeMode.LIGHT, settings.snapshot().themeMode)
            assertEquals(committed, settings.snapshot().hideAmount)
            service.checkAndRepairPendingRecovery()
            assertFixture(database, restored = committed)
            assertFalse(journal.exists())
        } finally {
            scope.cancel()
            database.close()
        }
    }

    private fun requestedCheckpoint(): String {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue(
            "真实进程中断测试需要显式参数 allowProcessTermination=true；本次跳过",
            arguments.getString("allowProcessTermination") == "true",
        )
        val checkpoint = requireNotNull(arguments.getString("recoveryCheckpoint"))
        require(checkpoint in setOf(
            FullBackupService.JOURNAL_STAGE_STAGED,
            FullBackupService.JOURNAL_STAGE_DB_COMMITTED,
        ))
        require(!arguments.getString("recoveryRunId").isNullOrBlank())
        return checkpoint
    }

    private fun validationContext(): Context = ApplicationProvider.getApplicationContext<Context>().also {
        check(it.packageName == "com.nonen.Bookkeeping.uivalidation")
    }

    private fun workDirectory(context: Context, checkpoint: String) =
        File(context.filesDir, "s10_backup_process_recovery/$checkpoint")

    private fun databaseName(checkpoint: String) = "s10_backup_process_$checkpoint.db"

    private fun openDatabase(context: Context, checkpoint: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, databaseName(checkpoint)).build()

    private fun settingsStore(context: Context, directory: File, scope: CoroutineScope) =
        SettingsStore(context, PreferenceDataStoreFactory.create(scope = scope) {
            File(directory, "settings.preferences_pb")
        })

    private suspend fun insertFixture(database: AppDatabase, restored: Boolean) {
        val id = if (restored) 2L else 1L
        database.bookDao().insert(BookEntity(bookId = id, bookName = "book-$id", createdAt = FIXTURE_TIME))
        database.assetDao().insert(AssetEntity(
            assetId = id, assetName = "asset-$id", assetNumber = id * 100.0,
            createdAt = FIXTURE_TIME, updatedAt = FIXTURE_TIME,
        ))
        database.transactionDao().insert(TransactionEntity(
            id = id, amount = -id * 10.0, category = "餐饮美食", note = "transaction-$id",
            timestamp = FIXTURE_TIME, source = "manual", assetId = id, bookId = id,
            hash = "s10-backup-process-$id", createdAt = FIXTURE_TIME, updatedAt = FIXTURE_TIME,
        ))
    }

    private suspend fun assertFixture(database: AppDatabase, restored: Boolean) {
        val id = if (restored) 2L else 1L
        val books = database.bookDao().getAll()
        val assets = database.assetDao().getAll()
        val transactions = database.transactionDao().getAll()
        assertEquals(1, books.size)
        assertEquals(1, assets.size)
        assertEquals(1, transactions.size)
        assertEquals(id, books.single().bookId)
        assertEquals("book-$id", books.single().bookName)
        assertEquals(id, assets.single().assetId)
        assertEquals(id * 100.0, assets.single().assetNumber, 0.0)
        assertEquals(id, transactions.single().id)
        assertEquals(id, transactions.single().bookId)
        assertEquals(id, transactions.single().assetId)
        assertEquals(-id * 10.0, transactions.single().amount, 0.0)
        assertEquals("transaction-$id", transactions.single().note)
        database.openHelper.readableDatabase.query("PRAGMA integrity_check").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("ok", cursor.getString(0))
            assertFalse(cursor.moveToNext())
        }
    }

    companion object {
        private const val FIXTURE_TIME = 1_792_310_400_000L
        private const val ORIGINAL_ATTACHMENT = "保留当前环境的附件"
        private const val RESTORED_ATTACHMENT = "来自备份的附件"
    }
}
