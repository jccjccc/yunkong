package com.dashabi.control

import com.dashabi.control.data.ApiResult
import com.dashabi.control.data.AppSettings
import com.dashabi.control.data.Repository
import com.dashabi.control.data.SettingsProvider
import com.dashabi.control.data.remote.DashaApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 针对真实开放 API 的只读集成测试。用于验证鉴权头、User-Agent、分页与 DTO 解析。
 * 不会执行任何写操作（暂停/恢复/出售/停止/备注/定时）。
 */
class ApiLiveTest {

    private val apiKey: String = System.getenv("DASHA_API_KEY")
        ?: "zh_live_d4bbadc5_867d2fb44e0a18d030d1312d862cf3aaff8dbfd93dffcd8fb0bf1f272f0aadd7"

    private val baseUrl: String = System.getenv("DASHA_BASE_URL")
        ?: "https://a.12123.pro/index.php/open/v1"

    private val repo = Repository(
        DashaApi(object : SettingsProvider {
            override fun current() = AppSettings(apiKey, baseUrl)
        })
    )

    private fun <T> ApiResult<T>.value(): T {
        assertTrue("expected Ok but was $this", this is ApiResult.Ok)
        return (this as ApiResult.Ok).value
    }

    @Test
    fun me_returns_identity_and_scopes() = runBlocking {
        val me = repo.me().value()
        assertNotNull(me.name)
        assertTrue("scopes should not be empty", me.scopes.isNotEmpty())
        assertTrue(me.scopes.contains("devices:read"))
        assertTrue(me.scopes.contains("accounts:read"))
    }

    @Test
    fun summary_returns_device_and_account_counts() = runBlocking {
        val summary = repo.summary().value()
        assertTrue("devices >= 0", summary.devices >= 0)
        assertTrue("accounts >= 0", summary.accounts >= 0)
        assertTrue(
            "status buckets should sum to device count",
            summary.running + summary.waitingNextRound + summary.stopped +
                summary.accessibilityOffline + summary.unknown == summary.devices
        )
    }

    @Test
    fun devices_parse_with_status_and_pagination() = runBlocking {
        val devices = repo.devices().value()
        assertTrue("devices should not be empty", devices.isNotEmpty())
        val first = devices.first()
        assertTrue("device_code required", first.deviceCode.isNotBlank())
        assertNotNull("status required", first.status)
        assertNotNull("status code required", first.status?.code)
        assertTrue("account_count >= 0", first.accountCount >= 0)
    }

    @Test
    fun device_detail_live_status_and_accounts() = runBlocking {
        val device = repo.devices().value().first()
        val detail = repo.device(device.deviceCode).value()
        assertEquals(device.deviceCode, detail.deviceCode)

        val live = repo.deviceLive(device.deviceCode).value()
        assertEquals(device.deviceCode, live.deviceCode)
        assertNotNull(live.status)

        val accounts = repo.deviceAccounts(device.deviceCode).value()
        assertTrue("device accounts should not be empty", accounts.isNotEmpty())
        accounts.forEach { account ->
            assertEquals(device.deviceCode, account.deviceCode)
            assertTrue(account.accountCode.isNotBlank())
            assertNotNull(account.facilities)
        }
    }

    @Test
    fun accounts_overview_parses_assets_and_images() = runBlocking {
        val accounts = repo.accounts().value()
        assertTrue("accounts should not be empty", accounts.isNotEmpty())
        val first = accounts.first()
        assertTrue(first.accountCode.isNotBlank())
        assertNotNull(first.gameNick)
        assertNotNull(first.status)
        assertNotNull(first.images)
        assertTrue("total_asset present", !first.totalAsset.isNullOrBlank())
    }

    @Test
    fun account_detail_and_image_metadata() = runBlocking {
        val account = repo.accounts().value().first()
        val deviceCode = requireNotNull(account.deviceCode)

        val detail = repo.account(deviceCode, account.accountCode).value()
        assertEquals(account.accountCode, detail.accountCode)

        val images = repo.accountImages(deviceCode, account.accountCode).value()
        assertNotNull(images.profile)
        assertNotNull(images.tqc)
        assertNotNull(images.store)

        val available = listOfNotNull(images.profile, images.tqc, images.store).firstOrNull { it.available }
        if (available != null) {
            assertNotNull("available image should expose url", available.url)
            assertTrue(
                repo.imageUrl(deviceCode, account.accountCode, "profile").startsWith("https://")
            )
        }

        if (images.tqc?.available == true) {
            val bytes = repo.tqcImageBytes(deviceCode, account.accountCode)
            assertTrue("图片应有内容", bytes.size > 1000)
            assertTrue("应为 JPEG", bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte())
        }
    }

    @Test
    fun pause_schedules_parse() = runBlocking {
        val device = repo.devices().value().first()
        val schedules = repo.schedules(device.deviceCode).value()
        // 允许为空，但必须是可解析的数组
        assertTrue(schedules.size >= 0)
    }

    @Test
    fun sell_status_is_readable() = runBlocking {
        val device = repo.devices().value().first()
        when (val status = repo.sellStatus(device.deviceCode)) {
            is ApiResult.Ok -> assertNotNull(status.value)
            is ApiResult.Err -> {
                // 无出售任务时上游可能返回 503
                assertTrue(
                    "unexpected error: ${status.message}",
                    status.http == 503 || status.http == 404 || status.http == 200
                )
            }
        }
    }

    @Test
    fun invalid_key_returns_401_with_error_envelope() = runBlocking {
        val bad = Repository(
            DashaApi(object : SettingsProvider {
                override fun current() = AppSettings("zh_live_invalid_key_for_test", baseUrl)
            })
        )
        val result = bad.me()
        assertTrue("should fail", result is ApiResult.Err)
        result as ApiResult.Err
        assertEquals(401, result.http)
        assertFalse("error message should be present", result.message.isBlank())
        assertTrue(
            "should carry api error code",
            result.code == "INVALID_API_KEY" || result.code == "UNAUTHORIZED" || result.code == "API_KEY_EXPIRED"
        )
    }
}
