package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ShopDao
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.DailyPriceEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.ScrapCategoryEntity
import com.example.data.local.entity.SettlementEntity
import com.example.data.local.entity.ShopProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ShopProfileEntity::class,
        ScrapCategoryEntity::class,
        DailyPriceEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        CustomerEntity::class,
        SettlementEntity::class,
        ExpenseEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shopDao(): ShopDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "abotalal_scrap_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
