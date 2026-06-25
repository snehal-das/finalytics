package com.example.finalytics.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val timestamp: Long,
    
    val sender: String,
    
    val amount: Double,
    
    val category: String,
    
    @ColumnInfo(name = "raw_text")
    val rawText: String,
    
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,
    
    val note: String = ""
)
