package com.example.finalytics.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investment_balances")
data class InvestmentBalance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "asset_name")
    val assetName: String,
    
    @ColumnInfo(name = "asset_type")
    val assetType: String,
    
    val quantity: Double,
    
    @ColumnInfo(name = "current_value")
    val currentValue: Double,
    
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long
)
