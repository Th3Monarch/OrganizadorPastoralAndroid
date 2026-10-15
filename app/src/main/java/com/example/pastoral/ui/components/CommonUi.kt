package com.example.pastoral.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun CategoryBadge(category: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (category.lowercase()) {
        "misa" -> LiturgicalPurpleContainer to LiturgicalPurple
        "retiro" -> GoldContainer to Color(0xFFB45309)
        "reunión", "reunion" -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
        "campaña", "campana" -> CrimsonContainer to CrimsonRed
        "formación", "formacion" -> Color(0xFFDBEAFE) to NavyPrimary
        "convivencia" -> SageContainer to SageGreen
        else -> Color(0xFFF1F5F9) to TextSecondaryLight
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = category,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PriorityBadge(priority: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (priority.lowercase()) {
        "alta" -> Triple(CrimsonContainer, CrimsonRed, "Alta")
        "media" -> Triple(GoldContainer, Color(0xFFB45309), "Media")
        else -> Triple(Color(0xFFE2E8F0), Color(0xFF475569), "Baja")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun RoleBadge(role: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFFE0F2FE),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = role,
            color = Color(0xFF0369A1),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
