package com.dashabi.control.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dashabi.control.ui.theme.Amber
import com.dashabi.control.ui.theme.Rose
import com.dashabi.control.ui.theme.Surface3
import com.dashabi.control.ui.theme.Surface2
import com.dashabi.control.ui.theme.TextMuted
import com.dashabi.control.ui.theme.TextPrimary
import com.dashabi.control.ui.theme.TextSecondary
import com.dashabi.control.ui.vm.RecognizeMode
import com.dashabi.control.ui.vm.RecognitionViewModel

/** AI 识别进度 / 结果弹窗。 */
@Composable
fun RecognitionDialog(vm: RecognitionViewModel, onDismiss: () -> Unit) {
    if (!vm.running && !vm.finished) return

    AlertDialog(
        onDismissRequest = {
            if (!vm.running) {
                vm.dismiss()
                onDismiss()
            }
        },
        containerColor = Surface2,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                if (vm.running) {
                    when (vm.mode) {
                        RecognizeMode.LEVELS -> "正在识别体力 / 负重"
                        RecognizeMode.AW -> "正在识别 AW 子弹"
                        RecognizeMode.BOTH -> "正在识别体力 / 负重 / AW"
                    }
                } else "识别结束",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                if (vm.running) {
                    Text(
                        "进度 ${vm.done} / ${vm.total}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (vm.total > 0) vm.done.toFloat() / vm.total else 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = Amber,
                        trackColor = Surface3
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "当前：${vm.currentName.ifBlank { "准备中" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "成功 ${vm.succeeded} · 失败 ${vm.failed}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                } else if (vm.total == 0 && vm.error == null) {
                    Text(
                        "没有需要识别的账号（可能都已登记，或没有特勤处 / 仓库截图）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    Text(
                        "共 ${vm.total} 个账号，成功 ${vm.succeeded}，失败 ${vm.failed}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }

                if (!vm.running && vm.failed > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "图片接口限流 120 次/分钟，失败多为限流或网络抖动；稍后重新识别即可（默认只识别未登记）。",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                vm.error?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Rose
                    )
                }
            }
        },
        confirmButton = {
            if (vm.running) {
                TextButton(onClick = { vm.cancel() }) {
                    Text("取消", color = Rose)
                }
            } else {
                TextButton(onClick = {
                    vm.dismiss()
                    onDismiss()
                }) {
                    Text("完成", color = Amber)
                }
            }
        }
    )
}
