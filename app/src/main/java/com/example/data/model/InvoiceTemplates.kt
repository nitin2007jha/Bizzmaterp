package com.example.data.model

import androidx.compose.ui.graphics.Color

data class InvoiceTemplateSpec(
    val id: String,
    val name: String,
    val layout: String,
    val primaryColor: Color,
    val accentColor: Color,
    val bgColor: Color = Color.White,
    val inkColor: Color = Color(0xFF0F172A),
    val softBg: Color = Color(0xFFF0FDFA),
    val isDark: Boolean = false
)

object InvoiceTemplates {
    val all: List<InvoiceTemplateSpec> = listOf(
        InvoiceTemplateSpec("modern", "Modern Teal", "band", Color(0xFF0B6377), Color(0xFF14B8A6), softBg = Color(0xFFF0FDFA)),
        InvoiceTemplateSpec("classic", "Classic Slate", "serif", Color(0xFF1E293B), Color(0xFF475569), softBg = Color(0xFFF8FAFC)),
        InvoiceTemplateSpec("minimal", "Minimal Clean", "minimal", Color(0xFF0F172A), Color(0xFF94A3B8), softBg = Color(0xFFF8FAFC)),
        InvoiceTemplateSpec("compact", "Compact Indigo", "band", Color(0xFF4F46E5), Color(0xFF818CF8), softBg = Color(0xFFEEF2FF)),
        InvoiceTemplateSpec("elegant", "Elegant Gold", "serif", Color(0xFF92400E), Color(0xFFD4B483), softBg = Color(0xFFFFFBF2)),
        InvoiceTemplateSpec("bold", "Bold Crimson", "band", Color(0xFF0F172A), Color(0xFFEF4444), softBg = Color(0xFFF1F5F9)),
        InvoiceTemplateSpec("nature", "Nature Emerald", "wave", Color(0xFF166534), Color(0xFF4ADE80), softBg = Color(0xFFF0FDF4)),
        InvoiceTemplateSpec("corporate", "Corporate Blue", "band", Color(0xFF1E3A8A), Color(0xFF3B82F6), softBg = Color(0xFFEFF6FF)),
        InvoiceTemplateSpec("ayurveda", "Ayurveda Herbal", "band", Color(0xFF073E4D), Color(0xFF34D399), softBg = Color(0xFFECFDF5)),
        InvoiceTemplateSpec("premium", "Premium Obsidian", "dark", Color(0xFF0B1220), Color(0xFFD4AF37), bgColor = Color(0xFF0B1220), inkColor = Color(0xFFE5E7EB), softBg = Color(0xFF121B2E), isDark = true),
        InvoiceTemplateSpec("nx-aurora", "Aurora Violet", "wave", Color(0xFF6D28D9), Color(0xFF22D3EE), softBg = Color(0xFFF5F3FF)),
        InvoiceTemplateSpec("nx-ocean", "Ocean Wave", "wave", Color(0xFF0369A1), Color(0xFF38BDF8), softBg = Color(0xFFF0F9FF)),
        InvoiceTemplateSpec("nx-sidebar", "Sidebar Mint", "side", Color(0xFF0F766E), Color(0xFF5EEAD4), softBg = Color(0xFFF0FDFA)),
        InvoiceTemplateSpec("nx-ribbon", "Ribbon Rose", "ribbon", Color(0xFFE11D48), Color(0xFFFB7185), softBg = Color(0xFFFFF1F2))
    )

    fun byId(id: String): InvoiceTemplateSpec = all.firstOrNull { it.id == id } ?: all.first()
}
