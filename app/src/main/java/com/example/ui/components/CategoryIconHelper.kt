package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {

    fun getIcon(iconKey: String): ImageVector {
        return when (iconKey.lowercase()) {
            "shopping_cart" -> Icons.Default.ShoppingCart
            "restaurant" -> Icons.Default.Restaurant
            "directions_car" -> Icons.Default.DirectionsCar
            "shopping_bag" -> Icons.Default.ShoppingBag
            "receipt_long" -> Icons.AutoMirrored.Filled.ReceiptLong
            "movie" -> Icons.Default.Movie
            "medical_services" -> Icons.Default.MedicalServices
            "payments" -> Icons.Default.Payments
            "attach_money" -> Icons.Default.AttachMoney
            "flight" -> Icons.Default.Flight
            "school" -> Icons.Default.School
            "fitness_center" -> Icons.Default.FitnessCenter
            else -> Icons.Default.Category
        }
    }

    val AVAILABLE_ICONS = listOf(
        "shopping_cart" to "Groceries",
        "restaurant" to "Dining",
        "directions_car" to "Transport",
        "shopping_bag" to "Shopping",
        "receipt_long" to "Bills",
        "movie" to "Entertainment",
        "medical_services" to "Health",
        "payments" to "Income",
        "attach_money" to "Finance",
        "flight" to "Travel",
        "fitness_center" to "Fitness",
        "school" to "Education",
        "category" to "Other"
    )

    val PRESET_COLORS = listOf(
        "#10B981", // Emerald
        "#3B82F6", // Blue
        "#F97316", // Orange
        "#8B5CF6", // Purple
        "#EAB308", // Yellow
        "#EC4899", // Pink
        "#EF4444", // Red
        "#0D9488", // Teal
        "#06B6D4", // Cyan
        "#6366F1", // Indigo
        "#64748B"  // Slate
    )

    fun parseColor(hex: String, default: Color = Color(0xFF64748B)): Color {
        return try {
            val clean = hex.removePrefix("#")
            if (clean.length == 6) {
                Color(android.graphics.Color.parseColor("#$clean"))
            } else if (clean.length == 8) {
                Color(android.graphics.Color.parseColor("#$clean"))
            } else {
                default
            }
        } catch (e: Exception) {
            default
        }
    }
}
