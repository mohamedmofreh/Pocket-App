package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconKey: String, // e.g. "shopping_cart", "restaurant", "directions_car", etc.
    val colorHex: String, // e.g. "#10B981"
    val isDefault: Boolean = false
)
