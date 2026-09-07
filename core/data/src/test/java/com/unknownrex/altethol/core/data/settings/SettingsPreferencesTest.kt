package com.unknownrex.altethol.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class SettingsPreferencesTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var dataStoreFile: File
    private lateinit var scope: CoroutineScope

    @BeforeEach
    fun setUp() {
        dataStoreFile = File.createTempFile("altethol_settings_test", ".preferences_pb")
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { dataStoreFile },
        )
    }

    @AfterEach
    fun tearDown() {
        scope.cancel()
        dataStoreFile.delete()
    }

    private fun settings() = SettingsPreferences(dataStore)

    @Test
    fun `default interval is 5 minutes`() = runTest {
        assertThat(settings().pollIntervalMinutes.first())
            .isEqualTo(SettingsStorage.DEFAULT_POLL_INTERVAL_MINUTES)
    }

    @Test
    fun `set interval persists value`() = runTest {
        settings().setPollIntervalMinutes(10)

        assertThat(settings().pollIntervalMinutes.first()).isEqualTo(10)
    }

    @Test
    fun `interval below minimum is clamped`() = runTest {
        settings().setPollIntervalMinutes(1)

        assertThat(settings().pollIntervalMinutes.first())
            .isEqualTo(SettingsStorage.MIN_POLL_INTERVAL_MINUTES)
    }
}
