package org.ooni.probe.data.repositories

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorItem
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.models.UpdateStatus
import org.ooni.testing.createPreferenceDataStore
import org.ooni.testing.factories.DescriptorFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreferenceRepositoryTest {
    private lateinit var preferenceRepository: PreferenceRepository

    @BeforeTest
    fun before() {
        preferenceRepository = PreferenceRepository(createPreferenceDataStore())
    }

    @AfterTest
    fun after() =
        runTest {
            preferenceRepository.clear()
        }

    @Test
    fun testAllSettings() =
        runTest {
            val value = "value"
            preferenceRepository.setValueByKey(SettingsKey.LANGUAGE_SETTING, value)
            val setting: Map<SettingsKey, Any?> = preferenceRepository.allSettings(listOf(SettingsKey.LANGUAGE_SETTING)).first()
            assertEquals(value, setting.values.first())
        }

    @Test
    fun testGetPreferenceKey() {
        assertEquals(
            SettingsKey.LANGUAGE_SETTING.value,
            preferenceRepository.getPreferenceKey(SettingsKey.LANGUAGE_SETTING.value),
        )
        assertEquals(
            "prefix_${SettingsKey.LANGUAGE_SETTING.value}",
            preferenceRepository.getPreferenceKey(SettingsKey.LANGUAGE_SETTING.value, "prefix"),
        )
        assertEquals(
            "autorun_${SettingsKey.LANGUAGE_SETTING.value}",
            preferenceRepository.getPreferenceKey(
                SettingsKey.LANGUAGE_SETTING.value,
                autoRun = true,
            ),
        )
        assertEquals(
            "prefix_autorun_${SettingsKey.LANGUAGE_SETTING.value}",
            preferenceRepository.getPreferenceKey(
                SettingsKey.LANGUAGE_SETTING.value,
                "prefix",
                true,
            ),
        )
    }

    @Test
    fun testGetValueByKey() =
        runTest {
            val value = "value"
            preferenceRepository.setValueByKey(SettingsKey.LANGUAGE_SETTING, value)
            assertEquals(value, preferenceRepository.getValueByKey(key = SettingsKey.LANGUAGE_SETTING).first())
        }

    /**
     * Timestamp keys must be mapped to LongKey explicitly. Anything not listed falls through to
     * the `else -> BooleanKey` branch, and writing a Long to it throws ClassCastException at
     * runtime - a crash no test using a fake preference map would ever catch.
     */
    @Test
    fun timestampKeysRoundTripAsLongs() =
        runTest {
            val timestampKeys = listOf(
                SettingsKey.LAST_ARTICLES_REFRESH,
                SettingsKey.LAST_ARTICLES_REFRESH_ATTEMPT,
                SettingsKey.MMDB_LAST_CHECK,
            )

            timestampKeys.forEach { key ->
                preferenceRepository.setValueByKey(key, 1_700_000_000L)
                assertEquals(
                    1_700_000_000L,
                    preferenceRepository.getValueByKey(key).first(),
                    "$key must store a Long",
                )
            }
        }

    @Test
    fun testSetValueByKey() =
        runTest {
            val value = "value"
            preferenceRepository.setValueByKey(SettingsKey.LANGUAGE_SETTING, value)
            assertEquals(value, preferenceRepository.getValueByKey(SettingsKey.LANGUAGE_SETTING).first())
        }

    @Test
    fun testClear() =
        runTest {
            val value = "value"
            preferenceRepository.setValueByKey(SettingsKey.LANGUAGE_SETTING, value)
            preferenceRepository.clear()
            assertNull(preferenceRepository.getValueByKey(SettingsKey.LANGUAGE_SETTING).first())
        }

    @Test
    fun testRemove() =
        runTest {
            val value = "value"
            preferenceRepository.setValueByKey(SettingsKey.LANGUAGE_SETTING, value)
            preferenceRepository.remove(SettingsKey.LANGUAGE_SETTING)
            assertNull(preferenceRepository.getValueByKey(SettingsKey.LANGUAGE_SETTING).first())
        }

    @Test
    fun testContains() =
        runTest {
            val value = "value"
            preferenceRepository.setValueByKey(SettingsKey.LANGUAGE_SETTING, value)
            assertEquals(true, preferenceRepository.contains(SettingsKey.LANGUAGE_SETTING))
        }

    @Test
    fun netTestDefaultsComeFromTestType() =
        runTest {
            // Descriptors from the OONI Run API don't set reliable defaults, so they're ignored
            val netTest = NetTest(
                test = TestType.WebConnectivity,
                isManualRunEnabled = false,
                isBackgroundRunEnabled = false,
            )
            val descriptor = buildDescriptor(Descriptor.Id("10004"), netTest)

            assertTrue(preferenceRepository.isNetTestEnabled(descriptor, netTest, isAutoRun = false).first())
            assertTrue(preferenceRepository.isNetTestEnabled(descriptor, netTest, isAutoRun = true).first())
        }

    @Test
    fun localNetTestDefaultsComeFromDescriptor() =
        runTest {
            val netTest = NetTest(
                test = TestType.WebConnectivity,
                isManualRunEnabled = true,
                isBackgroundRunEnabled = false,
            )
            val descriptor = buildDescriptor(Descriptor.Id("${Descriptor.LOCAL_ID_PREFIX}test"), netTest)

            assertTrue(preferenceRepository.isNetTestEnabled(descriptor, netTest, isAutoRun = false).first())
            assertFalse(preferenceRepository.isNetTestEnabled(descriptor, netTest, isAutoRun = true).first())

            preferenceRepository.setAreNetTestsEnabled(listOf(descriptor to netTest), isAutoRun = true, isEnabled = true)

            assertTrue(preferenceRepository.isNetTestEnabled(descriptor, netTest, isAutoRun = true).first())
        }

    private fun buildDescriptor(
        id: Descriptor.Id,
        netTest: NetTest,
    ) = DescriptorItem(
        descriptor = DescriptorFactory.buildInstalledModel(id = id, netTests = listOf(netTest)),
        updateStatus = UpdateStatus.NoNewUpdate,
    )
}
