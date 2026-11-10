package com.dashabi.control.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dashabi.control.ui.flattenJson
import com.dashabi.control.ui.jsonKeyLabel
import com.dashabi.control.ui.theme.Amber
import com.dashabi.control.ui.theme.Ink
import com.dashabi.control.ui.theme.Line
import com.dashabi.control.ui.theme.OnAccent
import com.dashabi.control.ui.theme.Rose
import com.dashabi.control.ui.theme.Surface1
import com.dashabi.control.ui.theme.Surface2
import com.dashabi.control.ui.theme.Surface3
import com.dashabi.control.ui.theme.TextMuted
import com.dashabi.control.ui.theme.TextPrimary
import com.dashabi.control.ui.theme.TextSecondary
import kotlinx.serialization.json.JsonElement

/* ------------------------------------------------------------------ */
/*  卡片                                                                */
/* ------------------------------------------------------------------ */

@Composable
fun DashaCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val radius = 20.dp
    val surface = Surface1
    val border = Line
    val base = modifier
        .fillMaxWidth()
        .shadow(6.dp, shape, clip = false)
        .clip(shape)
        .drawBehind {
            val r = radius.toPx()
            drawRoundRect(color = surface, cornerRadius = CornerRadius(r, r))
            accent?.let {
                drawRoundRect(
                    color = it,
                    size = Size(3.dp.toPx(), size.height),
                    cornerRadius = CornerRadius(r, r)
                )
            }
            drawRoundRect(
                color = border.copy(alpha = 0.85f),
                size = size,
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(contentPadding)
    Column(modifier = base, content = content)
}

/* ------------------------------------------------------------------ */
/*  状态                                                                */
/* ------------------------------------------------------------------ */

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier, dot: Boolean = true) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color.copy(alpha = 0.32f), shape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dot) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
fun StatusDot(color: Color, size: Dp = 9.dp, pulsing: Boolean = false) {
    val alpha = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "pulse")
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "pulseAlpha"
        ).value
    } else 1f
    Box(Modifier.size(size).clip(CircleShape).background(color.copy(alpha = alpha)))
}

/* ------------------------------------------------------------------ */
/*  文本与数据块                                                        */
/* ------------------------------------------------------------------ */

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(13.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Amber)
        )
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun StatBlock(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Column(modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                fontFamily = FontFamily.Monospace
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(1.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted, maxLines = 1)
    }
}

@Composable
fun KeyValueRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
    mono: Boolean = false,
    labelWidth: Dp = 84.dp
) {
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.width(labelWidth)
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "—",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
            ),
            color = if (value.isNullOrBlank()) TextMuted else valueColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun InfoChip(
    text: String,
    color: Color = TextSecondary,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Surface3)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
fun TinyAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = TextSecondary
) {
    val color = if (enabled) tint else TextMuted.copy(alpha = 0.45f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/* ------------------------------------------------------------------ */
/*  占位状态                                                            */
/* ------------------------------------------------------------------ */

@Composable
fun LoadingState(hint: String = "正在加载…", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = Amber,
            strokeWidth = 2.dp,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(hint, color = TextMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun EmptyState(
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 44.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Surface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, color = TextSecondary, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            hint,
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    DashaCard(modifier = modifier, accent = Rose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = Rose,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("加载失败", style = MaterialTheme.typography.titleSmall, color = Rose)
        }
        Spacer(Modifier.height(6.dp))
        Text(message, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 10.dp)) {
                Text("重试", color = Amber)
            }
        }
    }
}

/** 「不含 Key」安装包首次启动时的引导卡片。 */
@Composable
fun ConfigureKeyCard(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    DashaCard(modifier = modifier, accent = Amber) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Key,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("还没有配置 API Key", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "这个安装包没有内置 Key。请粘贴你在用户中心创建的开放 API Key（zh_live_…）。" +
                "保存后即可读取设备与账号，Key 只保存在本机。",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onOpenSettings,
            colors = ButtonDefaults.buttonColors(
                containerColor = Amber,
                contentColor = OnAccent
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("去设置 Key")
        }
    }
}

/* ------------------------------------------------------------------ */
/*  图片                                                                */
/* ------------------------------------------------------------------ */

@Composable
fun ThumbImage(
    url: String?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    corner: Dp = 13.dp
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(Surface3),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Person,
            contentDescription = null,
            tint = TextMuted.copy(alpha = 0.55f),
            modifier = Modifier.size(size * 0.42f)
        )
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  JSON 表格                                                           */
/* ------------------------------------------------------------------ */

@Composable
fun JsonFacts(
    element: JsonElement?,
    modifier: Modifier = Modifier,
    emptyText: String = "暂无数据"
) {
    val rows = flattenJson(element)
    if (rows.isEmpty()) {
        Text(emptyText, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        return
    }
    Column(modifier) {
        rows.forEach { (key, value) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                Text(
                    jsonKeyLabel(key),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.width(92.dp)
                )
                Text(
                    value.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  顶栏 / 底栏                                                         */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashaTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    loading: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column(Modifier.padding(vertical = 2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = TextPrimary
                    )
                }
            }
        },
        actions = {
            if (loading) {
                Box(Modifier.padding(end = 6.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Amber,
                        strokeWidth = 2.dp
                    )
                }
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Ink,
            scrolledContainerColor = Ink
        )
    )
}

@Composable
fun DashaBottomBar(
    selected: Int,
    devicesIcon: ImageVector,
    accountsIcon: ImageVector,
    abnormalIcon: ImageVector,
    abnormalCount: Int,
    deviceAbnormalCount: Int,
    onSelect: (Int) -> Unit
) {
    val dividerColor = Line
    NavigationBar(
        containerColor = Surface1,
        tonalElevation = 0.dp,
        modifier = Modifier.drawBehind {
            drawLine(
                color = dividerColor,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )
        }
    ) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Amber,
            selectedTextColor = Amber,
            unselectedIconColor = TextMuted,
            unselectedTextColor = TextMuted,
            indicatorColor = Amber.copy(alpha = 0.14f)
        )
        val hasDeviceAbnormal = deviceAbnormalCount > 0
        val deviceColors = if (hasDeviceAbnormal) {
            NavigationBarItemDefaults.colors(
                selectedIconColor = Rose,
                selectedTextColor = Rose,
                unselectedIconColor = Rose,
                unselectedTextColor = Rose,
                indicatorColor = Rose.copy(alpha = 0.12f)
            )
        } else {
            colors
        }
        NavigationBarItem(
            selected = selected == 0,
            onClick = { onSelect(0) },
            modifier = Modifier.testTag("tab_devices"),
            icon = {
                if (hasDeviceAbnormal) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = Rose, contentColor = Color.White) {
                                Text("$deviceAbnormalCount")
                            }
                        }
                    ) { Icon(devicesIcon, contentDescription = null) }
                } else {
                    Icon(devicesIcon, contentDescription = null)
                }
            },
            label = { Text("设备", style = MaterialTheme.typography.labelMedium) },
            colors = deviceColors
        )

        val hasAbnormal = abnormalCount > 0
        val abnormalColors = if (hasAbnormal) {
            NavigationBarItemDefaults.colors(
                selectedIconColor = Rose,
                selectedTextColor = Rose,
                unselectedIconColor = Rose,
                unselectedTextColor = Rose,
                indicatorColor = Rose.copy(alpha = 0.12f)
            )
        } else {
            colors
        }
        NavigationBarItem(
            selected = selected == 1,
            onClick = { onSelect(1) },
            modifier = Modifier.testTag("tab_abnormal"),
            icon = {
                if (hasAbnormal) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = Rose, contentColor = Color.White) {
                                Text("$abnormalCount")
                            }
                        }
                    ) {
                        Icon(abnormalIcon, contentDescription = null)
                    }
                } else {
                    Icon(abnormalIcon, contentDescription = null)
                }
            },
            label = { Text("异常", style = MaterialTheme.typography.labelMedium) },
            colors = abnormalColors
        )

        NavigationBarItem(
            selected = selected == 2,
            onClick = { onSelect(2) },
            modifier = Modifier.testTag("tab_accounts"),
            icon = { Icon(accountsIcon, contentDescription = null) },
            label = { Text("账号", style = MaterialTheme.typography.labelMedium) },
            colors = colors
        )
    }
}

/* ------------------------------------------------------------------ */
/*  确认弹窗                                                            */
/* ------------------------------------------------------------------ */

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    extra: (@Composable () -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                Text(message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                extra?.let {
                    Spacer(Modifier.height(10.dp))
                    it()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = if (destructive) Rose else Amber)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}
