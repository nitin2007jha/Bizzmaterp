package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.FormatUtils
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealBright
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealMint
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DailyChartPoint
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class PrimaryNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val primaryNavItems = listOf(
    PrimaryNavItem("dashboard", "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    PrimaryNavItem("records", "Records", Icons.AutoMirrored.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    PrimaryNavItem("inventory", "Inventory", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    PrimaryNavItem("clients", "Clients", Icons.Filled.People, Icons.Outlined.People),
    PrimaryNavItem("more", "More", Icons.Filled.GridView, Icons.Outlined.GridView)
)

@Composable
fun ErpTopBar(
    title: String,
    subtitle: String,
    canNavigateBack: Boolean,
    notificationCount: Int,
    trashCount: Int,
    userInitials: String,
    onBack: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(TealDark, TealPrimary, TealBright)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canNavigateBack) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("topbar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            } else {
                Surface(
                    modifier = Modifier
                        .padding(start = 4.dp, end = 10.dp)
                        .size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEFFFF),
                    shadowElevation = 2.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_brand_logo),
                        contentDescription = "My Business ERP Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(3.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onOpenNotifications,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("topbar_notifications_button")
            ) {
                BadgedBox(
                    badge = {
                        if (notificationCount > 0) {
                            Badge(containerColor = DangerRed, contentColor = Color.White) {
                                Text(notificationCount.coerceAtMost(9).toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = Color.White
                    )
                }
            }

            if (trashCount > 0) {
                IconButton(
                    onClick = onOpenTrash,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("topbar_trash_button")
                ) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = WarningAmber, contentColor = Color.White) {
                                Text(trashCount.coerceAtMost(9).toString())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Recycle Bin",
                            tint = Color.White
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(40.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = onOpenSettings)
                    .testTag("topbar_account_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userInitials.ifBlank { "ER" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun ErpBottomNavBar(
    currentRoute: String,
    onSelectRoute: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val selectedIndex = primaryNavItems.indexOfFirst { it.route == currentRoute }.let {
        if (it >= 0) it else 4
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            val itemWidth = maxWidth / primaryNavItems.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "nav_pill_offset"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .height(54.dp)
                    .padding(horizontal = 4.dp, vertical = 3.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                primaryNavItems.forEachIndexed { idx, item ->
                    val selected = idx == selectedIndex
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.12f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "nav_icon_pop_$idx"
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectRoute(item.route)
                            }
                            .testTag("bottom_nav_${item.route}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(iconScale)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErpNavigationRail(
    currentRoute: String,
    onSelectRoute: (String) -> Unit
) {
    val selectedIndex = primaryNavItems.indexOfFirst { it.route == currentRoute }.let {
        if (it >= 0) it else 4
    }
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        primaryNavItems.forEachIndexed { idx, item ->
            val selected = idx == selectedIndex
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectRoute(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                modifier = Modifier.testTag("rail_nav_${item.route}")
            )
        }
    }
}

@Composable
fun SwipeActionCard(
    modifier: Modifier = Modifier,
    onSwipeRight: (() -> Unit)? = null,
    rightLabel: String = "Paid",
    rightColor: Color = SuccessGreen,
    onSwipeLeft: (() -> Unit)? = null,
    leftLabel: String = "Trash",
    leftColor: Color = DangerRed,
    content: @Composable () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val thresholdPx = with(LocalDensity.current) { 88.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        val current = offsetX.value
        if (current != 0f) {
            val isRight = current > 0f
            val bg = if (isRight) rightColor else leftColor
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(bg.copy(alpha = 0.9f))
                    .padding(horizontal = 20.dp),
                contentAlignment = if (isRight) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isRight) Icons.Default.DoneAll else Icons.Default.DeleteOutline,
                        contentDescription = if (isRight) rightLabel else leftLabel,
                        tint = Color.White
                    )
                    Text(
                        text = if (isRight) rightLabel else leftLabel,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(onSwipeRight, onSwipeLeft) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            val target = offsetX.value + dragAmount
                            val minVal = if (onSwipeLeft != null) -thresholdPx * 1.4f else 0f
                            val maxVal = if (onSwipeRight != null) thresholdPx * 1.4f else 0f
                            scope.launch {
                                offsetX.snapTo(target.coerceIn(minVal, maxVal))
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetX.value >= thresholdPx && onSwipeRight != null -> {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSwipeRight()
                                    }
                                    offsetX.value <= -thresholdPx && onSwipeLeft != null -> {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSwipeLeft()
                                    }
                                }
                                offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                            }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f, spring()) }
                        }
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
fun ErpStatusBadge(
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

@Composable
fun HealthScoreRing(
    score: Int,
    modifier: Modifier = Modifier
) {
    val animatedSweep by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f) * 360f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "health_ring"
    )
    val ringColor = when {
        score >= 80 -> SuccessGreen
        score >= 60 -> TealBright
        score >= 40 -> WarningAmber
        else -> DangerRed
    }
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    Box(modifier = modifier.size(72.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$score",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = ringColor
        )
    }
}

@Composable
fun RevenueExpenseBarChart(
    points: List<DailyChartPoint>,
    modifier: Modifier = Modifier
) {
    val maxVal = (points.maxOfOrNull { maxOf(it.revenue, it.expense) } ?: 1.0).coerceAtLeast(1.0)
    val revColor = TealPrimary
    val expColor = DangerRed.copy(alpha = 0.78f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    Column(modifier = modifier.fillMaxWidth()) {
        if (points.all { it.revenue == 0.0 && it.expense == 0.0 }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create an invoice or expense to see 7-day cash activity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
            ) {
                val count = points.size.coerceAtLeast(1)
                val slotWidth = size.width / count
                val barW = (slotWidth * 0.28f).coerceIn(6.dp.toPx(), 16.dp.toPx())
                val chartH = size.height - 10.dp.toPx()

                drawLine(
                    color = gridColor,
                    start = Offset(0f, chartH),
                    end = Offset(size.width, chartH),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = gridColor.copy(alpha = 0.2f),
                    start = Offset(0f, chartH * 0.5f),
                    end = Offset(size.width, chartH * 0.5f),
                    strokeWidth = 1.dp.toPx()
                )

                points.forEachIndexed { idx, pt ->
                    val centerX = slotWidth * idx + slotWidth / 2f
                    val revH = ((pt.revenue / maxVal).toFloat() * (chartH - 6.dp.toPx())).coerceAtLeast(if (pt.revenue > 0) 4.dp.toPx() else 0f)
                    val expH = ((pt.expense / maxVal).toFloat() * (chartH - 6.dp.toPx())).coerceAtLeast(if (pt.expense > 0) 4.dp.toPx() else 0f)
                    if (revH > 0f) {
                        drawRoundRect(
                            color = revColor,
                            topLeft = Offset(centerX - barW - 2.dp.toPx(), chartH - revH),
                            size = Size(barW, revH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                    if (expH > 0f) {
                        drawRoundRect(
                            color = expColor,
                            topLeft = Offset(centerX + 2.dp.toPx(), chartH - expH),
                            size = Size(barW, expH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                points.forEach { pt ->
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentDonutChart(
    paymentTotals: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val entries = listOf(
        Triple("UPI", paymentTotals["UPI"] ?: 0.0, TealPrimary),
        Triple("Cash", paymentTotals["Cash"] ?: 0.0, EmeraldAccent),
        Triple("Credit", paymentTotals["Credit"] ?: 0.0, WarningAmber),
        Triple("Bank", paymentTotals["Bank"] ?: 0.0, IndigoAccent)
    )
    val total = entries.sumOf { it.second }
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx()
                if (total <= 0.0) {
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke)
                    )
                } else {
                    var startAngle = -90f
                    entries.forEach { (_, amt, color) ->
                        if (amt > 0.0) {
                            val sweep = ((amt / total) * 360.0).toFloat()
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Butt)
                            )
                            startAngle += sweep
                        }
                    }
                }
            }
            Text(
                text = if (total > 0) "Modes" else "0%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            entries.forEach { (label, amt, color) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = FormatUtils.inr(amt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private data class ConfettiParticle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)

@Composable
fun ConfettiOverlay(
    triggerKey: Int,
    onFinished: () -> Unit
) {
    if (triggerKey <= 0) return
    val progress = remember(triggerKey) { Animatable(0f) }
    val particles = remember(triggerKey) {
        val colors = listOf(TealPrimary, TealBright, EmeraldAccent, WarningAmber, IndigoAccent, PurpleAccent)
        List(36) { idx ->
            ConfettiParticle(
                angle = (idx * 10f + Random.nextFloat() * 14f) * (Math.PI.toFloat() / 180f),
                speed = 160f + Random.nextFloat() * 340f,
                size = 6f + Random.nextFloat() * 7f,
                color = colors[idx % colors.size]
            )
        }
    }

    LaunchedEffect(triggerKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing))
        onFinished()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val t = progress.value
        val originX = size.width / 2f
        val originY = size.height * 0.35f
        val alpha = (1f - t).coerceIn(0f, 1f)
        particles.forEach { p ->
            val dx = cos(p.angle) * p.speed * t
            val dy = sin(p.angle) * p.speed * t + (260f * t * t)
            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size,
                center = Offset(originX + dx, originY + dy)
            )
        }
    }
}

@Composable
fun ExpandableQuickFab(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenAria: () -> Unit,
    onNewInvoice: () -> Unit,
    onOpenPos: () -> Unit,
    onAddExpense: () -> Unit,
    onAddProduct: () -> Unit,
    onAddClient: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)) + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut(tween(150)) + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickFabItem("Add Client", Icons.Default.People, IndigoAccent, "fab_add_client", onAddClient)
                QuickFabItem("Add Product", Icons.Default.Inventory2, PurpleAccent, "fab_add_product", onAddProduct)
                QuickFabItem("Add Expense", Icons.Default.RequestQuote, WarningAmber, "fab_add_expense", onAddExpense)
                QuickFabItem("Quick POS", Icons.Default.PointOfSale, EmeraldAccent, "fab_quick_pos", onOpenPos)
                QuickFabItem("New Invoice", Icons.AutoMirrored.Filled.ReceiptLong, TealPrimary, "fab_new_invoice", onNewInvoice)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallFloatingActionButton(
                onClick = onOpenAria,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TealPrimary,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("aria_voice_fab")
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Aria Voice Assistant")
            }

            FloatingActionButton(
                onClick = onToggleExpand,
                containerColor = TealPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("quick_create_main_fab")
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (expanded) "Close Quick Actions" else "Quick Create"
                )
            }
        }
    }
}

@Composable
private fun QuickFabItem(
    label: String,
    icon: ImageVector,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
