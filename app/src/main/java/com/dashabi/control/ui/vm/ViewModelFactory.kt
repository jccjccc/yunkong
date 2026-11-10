package com.dashabi.control.ui.vm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * 轻量 ViewModel 工厂：VM 的构造参数（Repository、路由参数）由调用方闭包提供。
 * 与 Navigation 的 back stack entry 绑定，切页返回时保留数据。
 */
@Composable
fun <VM : ViewModel> rememberVm(
    vmClass: Class<VM>,
    key: String? = null,
    factory: () -> VM
): VM {
    val owner = LocalViewModelStoreOwner.current
        ?: error("No ViewModelStoreOwner available")
    val provider = remember(owner, key, vmClass) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = factory() as T
        }
    }
    return remember(owner, key, provider) {
        ViewModelProvider(owner, provider)
            .get(key ?: vmClass.canonicalName ?: vmClass.name, vmClass)
    }
}

data class UiMessage(val text: String, val error: Boolean = false)
