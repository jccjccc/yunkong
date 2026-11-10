package com.dashabi.control.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dashabi.control.data.Account
import com.dashabi.control.AppContainer
import com.dashabi.control.ui.components.DashaCard
import com.dashabi.control.ui.theme.Amber
import com.dashabi.control.ui.theme.TextMuted
import com.dashabi.control.ui.theme.TextPrimary
import androidx.compose.material3.MaterialTheme

/**
 * 战绩查询卡片：按当前账号游戏名查询「烽火地带」战绩。
 * 数据源暂未接入（等待第三方接口授权），先把账号切换与入口做好。
 */
@Composable
fun StatsQueryCard(
    container: AppContainer,
    account: Account,
    deviceCode: String
) {
    var queryAccount by remember { mutableStateOf(account.accountCode) }
    var showPicker by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var siblings by remember { mutableStateOf<List<Account>>(emptyList()) }
    var liquid by remember { mutableStateOf<String?>(null) }
    var updatedAt by remember { mutableStateOf<String?>(null) }
    var lastGame by remember { mutableStateOf<String?>(null) }
    var kills by remember { mutableStateOf<String?>(null) }
    var deaths by remember { mutableStateOf<String?>(null) }
    var kd by remember { mutableStateOf<String?>(null) }
    var rank by remember { mutableStateOf<String?>(null) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val text = com.dashabi.control.ui.stats.StatsResultBus.latest.orEmpty()
        if (text.isNotBlank()) {
            text.lineSequence().forEach { line ->
                when {
                    line.startsWith("段位") -> rank = line.substringAfter("：").trim()
                    line.startsWith("战损比") -> kd = line.substringAfter("：").trim()
                    line.startsWith("流动资产") -> liquid = line.substringAfter("：").trim()
                    line.startsWith("最后一场") -> lastGame = line.substringAfter("：").trim()
                    line.startsWith("更新时间") -> updatedAt = line.substringAfter("：").trim()
                }
            }
            message = null
        }
    }


    LaunchedEffect(deviceCode) {
        val r = container.repo.deviceAccounts(deviceCode)
        siblings = (r as? com.dashabi.control.data.ApiResult.Ok<List<Account>>)?.value ?: emptyList()
    }

    DashaCard(accent = Amber) {
        Column(Modifier.fillMaxWidth()) {
            Text("战绩查询", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "按当前账号的游戏名查询「烽火地带」战绩",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "段位：" + (rank ?: "—"),
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )
            Text(
                "战损比：" + (kd ?: "—"),
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )
            Text(
                "流动资产：" + (liquid ?: "—"),
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )
            Text(
                "最后一场：" + (lastGame ?: "—"),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                "更新时间：" + (updatedAt ?: "—"),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                OutlinedButton(onClick = {
                    com.dashabi.control.ui.stats.StatsResultBus.latest = null
                    launcher.launch(
                        com.dashabi.control.ui.stats.StatsWebActivity.intent(
                            ctx,
                            container.gameNames.get(account.accountCode) ?: account.gameNick
                        )
                    )
                }) { Text("查询战绩") }
                Spacer(Modifier.width(8.dp))
            }
            message?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}
