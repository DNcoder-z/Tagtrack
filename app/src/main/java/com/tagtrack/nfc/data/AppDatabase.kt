package com.tagtrack.nfc.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Item::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tagtrack.db"
                )
                    .fallbackToDestructiveMigration(true)
//                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

//        private class DatabaseCallback : RoomDatabase.Callback() {
//            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
//                super.onCreate(db)
//                INSTANCE?.let { database ->
//                    CoroutineScope(Dispatchers.IO).launch {
//                        val dao = database.itemDao()
//                        dao.insertItems(SeedData.getSampleItems())
//                    }
//                }
//            }
//        }
    }
}
