package com.dashabi.control.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dashabi.control.ui.components.OperationFeedback
import com.dashabi.control.ui.theme.*
import com.dashabi.control.ui.vm.RecognizeMode
import com.dashabi.control.ui.vm.RecognitionViewModel

/** The dialog only dispatches user intent; recognition stays in its existing view model. */
@Composable
fun RecognitionDialog(vm: RecognitionViewModel, onDismiss: () -> Unit) {
    if (!vm.running && !vm.finished) return
    RecognitionStatusDialog(vm.running, vm.mode, vm.done, vm.total, vm.succeeded, vm.failed, vm.currentName, vm.error,
        onCancel = { vm.cancel() }, onClose = { vm.dismiss(); onDismiss() })
}

@Composable
internal fun RecognitionStatusDialog(
    running: Boolean, mode: RecognizeMode, done: Int, total: Int, succeeded: Int, failed: Int,
    currentName: String, error: String?, onCancel: () -> Unit, onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!running) onClose() },
        containerColor = Surface1,
        icon = { Icon(Icons.Outlined.AutoAwesome, null, tint = Amber) },
        title = { Text(if (running) "识别任务进行中" else "识别任务已结束", style = MaterialTheme.typography.titleLarge, color = TextPrimary) },
        text = {
            RecognitionProgressContent(running, mode, done, total, succeeded, failed, currentName, error,
                Modifier.verticalScroll(rememberScrollState()))
        },
        confirmButton = {
            if (running) OutlinedButton(onClick = onCancel) { Text("取消识别", color = Rose) }
            else Button(onClick = onClose) { Text("完成") }
        }
    )
}

@Composable
internal fun RecognitionProgressContent(
    running: Boolean, mode: RecognizeMode, done: Int, total: Int, succeeded: Int, failed: Int,
    currentName: String, error: String?, modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(when (mode) {
            RecognizeMode.LEVELS -> "体力 / 负重识别"
            RecognizeMode.AW -> "AW 子弹识别"
            RecognizeMode.BOTH -> "体力 / 负重 / AW 识别"
        }, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        if (running) {
            if (total > 0) {
                Text("已处理 $done / $total 个账号", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(progress = { (done.toFloat() / total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Amber, trackColor = Surface3)
            } else {
                Text("正在准备账号与截图…", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Amber, trackColor = Surface3)
            }
            Text("当前：${currentName.ifBlank { "准备中" }}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        } else {
            Text("共 $total 个账号", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text(succeeded.toString(), color = Amber, style = MaterialTheme.typography.headlineMedium)
                Text("识别成功", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            Column(Modifier.weight(1f)) {
                Text(failed.toString(), color = if (failed > 0) Rose else TextPrimary, style = MaterialTheme.typography.headlineMedium)
                Text("识别失败", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (!running && total == 0 && error == null) {
            OperationFeedback("没有需要识别的账号。请检查登记状态，以及特勤处或仓库截图是否可用。")
        }
        if (!running && failed > 0) {
            OperationFeedback("部分账号未完成识别。请查看错误信息并核对网络、截图和识别配置；稍后可使用「只识别未登记的账号」重试。")
        }
        error?.let { OperationFeedback(it, error = true) }
        if (running) Text("取消后会保留已经完成的识别结果。", color = TextMuted, style = MaterialTheme.typography.bodySmall)
    }
}
