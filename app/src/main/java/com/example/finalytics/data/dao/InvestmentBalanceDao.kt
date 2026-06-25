package com.example.finalytics.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.finalytics.data.entity.InvestmentBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentBalanceDao {
    @Query("SELECT * FROM investment_balances ORDER BY asset_name ASC")
    fun getAllBalances(): Flow<List<InvestmentBalance>>

    @Query("SELECT * FROM investment_balances WHERE id = :id")
    suspend fun getBalanceById(id: Long): InvestmentBalance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalance(balance: InvestmentBalance): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(balances: List<InvestmentBalance>)

    @Update
    suspend fun updateBalance(balance: InvestmentBalance)

    @Delete
    suspend fun deleteBalance(balance: InvestmentBalance)

    @Query("DELETE FROM investment_balances")
    suspend fun deleteAllBalances()
}
