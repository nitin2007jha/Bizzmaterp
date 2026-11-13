package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.FormatUtils
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TealPrimary
import kotlinx.coroutines.delay

@Composable
fun AnimatedRupeeText(
    amount: Double,
    masked: Boolean = false,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "rupee_counter"
    )
    Text(
        text = FormatUtils.inr(animated.toDouble(), masked = masked),
        style = style,
        color = color,
        modifier = modifier
    )
}

@Composable
fun StaggeredEntrance(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index * 40L).coerceAtMost(240L))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(
            initialOffsetY = { it / 6 },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        )
    ) {
        content()
    }
}

@Composable
fun TactileBounceBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tactile_bounce"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .pointerInput(onClick) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                )
            }
    ) {
        content()
    }
}

@Composable
fun UpiPaymentQrCard(
    upiId: String,
    payeeName: String,
    amount: Double,
    note: String,
    onResultMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val uri = remember(upiId, payeeName, amount, note) {
        FormatUtils.buildUpiUri(upiId, payeeName, amount, note)
    }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp
            ) {
                UpiQrMatrixCanvas(
                    payload = uri,
                    modifier = Modifier.size(92.dp).padding(8.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Instant UPI Collect", style = MaterialTheme.typography.titleSmall, color = TealPrimary)
                Text(
                    text = upiId.ifBlank { "Set UPI ID in Settings" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (amount > 0.0) {
                    Text(
                        text = "Pay ${FormatUtils.inr(amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldAccent
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            FormatUtils.launchUpiPayment(context, upiId, payeeName, amount, note)
                                .onFailure { onResultMessage(it.localizedMessage ?: "No UPI app installed") }
                        },
                        enabled = upiId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pay App")
                    }
                    OutlinedButton(
                        onClick = {
                            FormatUtils.shareText(context, "UPI Payment Link", "Pay ${FormatUtils.inr(amount)} to $payeeName:\n$uri")
                        },
                        enabled = upiId.isNotBlank()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share UPI Link", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun UpiQrMatrixCanvas(payload: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val grid = 21
        val cell = size.width / grid
        fun drawFinder(startCol: Int, startRow: Int) {
            drawRect(Color.Black, Offset(startCol * cell, startRow * cell), Size(cell * 7, cell * 7))
            drawRect(Color.White, Offset((startCol + 1) * cell, (startRow + 1) * cell), Size(cell * 5, cell * 5))
            drawRect(TealPrimary, Offset((startCol + 2) * cell, (startRow + 2) * cell), Size(cell * 3, cell * 3))
        }
        drawFinder(0, 0)
        drawFinder(grid - 7, 0)
        drawFinder(0, grid - 7)

        var seed = payload.hashCode()
        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val inTopLeft = r < 8 && c < 8
                val inTopRight = r < 8 && c >= grid - 8
                val inBotLeft = r >= grid - 8 && c < 8
                if (!inTopLeft && !inTopRight && !inBotLeft) {
                    seed = seed * 1664525 + 1013904223 + r * 31 + c * 17
                    if ((seed ushr 30) and 1 == 1) {
                        drawRect(Color.Black, Offset(c * cell, r * cell), Size(cell * 0.92f, cell * 0.92f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCalculatorSheet(
    onCopyResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    var display by remember { mutableStateOf("0") }
    var pendingOp by remember { mutableStateOf<Char?>(null) }
    var lhs by remember { mutableStateOf<Double?>(null) }
    var resetNext by remember { mutableStateOf(false) }

    fun currentVal() = display.toDoubleOrNull() ?: 0.0
    fun formatNum(v: Double): String {
        val rounded = kotlin.math.round(v * 100.0) / 100.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }

    fun applyPending() {
        val op = pendingOp ?: return
        val left = lhs ?: return
        val right = currentVal()
        val res = when (op) {
            '+' -> left + right
            '-' -> left - right
            '×' -> left * right
            '÷' -> if (right != 0.0) left / right else 0.0
            else -> right
        }
        display = formatNum(res)
        lhs = res
        pendingOp = null
        resetNext = true
    }

    fun tapDigit(ch: String) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (resetNext || display == "0") {
            display = if (ch == ".") "0." else ch
            resetNext = false
        } else {
            if (ch == "." && display.contains(".")) return
            display += ch
        }
    }

    fun tapOp(op: Char) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        applyPending()
        lhs = currentVal()
        pendingOp = op
        resetNext = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Quick Billing Calculator", style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(display))
                        onCopyResult("Copied $display")
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy result", tint = TealPrimary)
                    }
                    IconButton(onClick = {
                        display = if (display.length > 1) display.dropLast(1) else "0"
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    if (lhs != null && pendingOp != null) {
                        Text("${formatNum(lhs!!)} $pendingOp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(display, style = MaterialTheme.typography.displaySmall, color = TealPrimary, maxLines = 1)
                }
            }

            // Instant GST Shortcut Bar
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("+18%" to 1.18, "+12%" to 1.12, "+5%" to 1.05, "-GST 18" to (1.0 / 1.18)).forEach { (lbl, mult) ->
                    FilledTonalButton(
                        onClick = {
                            display = formatNum(currentVal() * mult)
                            resetNext = true
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(lbl, maxLines = 1) }
                }
            }

            val rows = listOf(
                listOf("7", "8", "9", "÷"),
                listOf("4", "5", "6", "×"),
                listOf("1", "2", "3", "-"),
                listOf("C", "0", ".", "+")
            )
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        val isOp = key in listOf("÷", "×", "-", "+")
                        Button(
                            onClick = {
                                when (key) {
                                    "C" -> { display = "0"; lhs = null; pendingOp = null; resetNext = false }
                                    "÷", "×", "-", "+" -> tapOp(key[0])
                                    else -> tapDigit(key)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOp) TealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isOp) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(52.dp).testTag("calc_key_$key")
                        ) { Text(key, style = MaterialTheme.typography.titleMedium) }
                    }
                }
            }

            Button(
                onClick = { applyPending() },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("calc_key_equals")
            ) { Text("= Calculate", style = MaterialTheme.typography.titleMedium) }
        }
    }
}
