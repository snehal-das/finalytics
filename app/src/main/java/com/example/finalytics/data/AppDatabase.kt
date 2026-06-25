package com.example.finalytics.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.finalytics.data.dao.CategoryDao
import com.example.finalytics.data.dao.InvestmentBalanceDao
import com.example.finalytics.data.dao.TransactionDao
import com.example.finalytics.data.entity.Category
import com.example.finalytics.data.entity.InvestmentBalance
import com.example.finalytics.data.entity.Transaction

@Database(
    entities = [Transaction::class, InvestmentBalance::class, Category::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun transactionDao(): TransactionDao
    abstract fun investmentBalanceDao(): InvestmentBalanceDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finalytics_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
