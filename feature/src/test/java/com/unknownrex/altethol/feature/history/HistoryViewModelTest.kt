package com.unknownrex.altethol.feature.history

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.unknownrex.altethol.core.data.local.db.dao.AbsensiHistoriDao
import com.unknownrex.altethol.core.data.local.db.entity.AbsensiHistoriEntity
import com.unknownrex.altethol.core.data.model.AttendanceStatus
import com.unknownrex.altethol.core.data.model.AttendanceStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAbsensiHistoriDao(
        private val entities: List<AbsensiHistoriEntity>,
    ) : AbsensiHistoriDao {
        override suspend fun insert(histori: AbsensiHistoriEntity): Long = 1L

        override fun observeAll(): Flow<List<AbsensiHistoriEntity>> = flowOf(entities)

        override suspend fun clear() = Unit
    }

    private fun entity(
        id: Long,
        matakuliah: String,
        status: AttendanceStatus,
        failedStep: AttendanceStep? = null,
    ) = AbsensiHistoriEntity(
        id = id,
        matakuliah = matakuliah,
        kuliahId = 219110,
        waktuDeteksi = 1000L,
        waktuAbsenDieksekusi = 2000L,
        status = status,
        pesanServer = "pesan",
        failedStep = failedStep,
    )

    @Test
    fun `maps dao entities into state items`() = runTest {
        val dao = FakeAbsensiHistoriDao(
            listOf(
                entity(1L, "English for academic", AttendanceStatus.SUCCESS),
                entity(2L, "Matematika", AttendanceStatus.FAILED, AttendanceStep.SUBMIT),
            ),
        )
        val viewModel = HistoryViewModel(dao)

        val state = viewModel.state.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.items).hasSize(2)
        val first = state.items.first()
        assertThat(first.matakuliah).isEqualTo("English for academic")
        assertThat(first.kuliahId).isEqualTo(219110)
        assertThat(first.waktuDeteksi).isEqualTo(1000L)
        assertThat(first.waktuAbsenDieksekusi).isEqualTo(2000L)
        assertThat(first.status).isEqualTo(AttendanceStatus.SUCCESS)
        assertThat(first.failedStep).isNull()
        assertThat(first.pesanServer).isEqualTo("pesan")
        val second = state.items[1]
        assertThat(second.status).isEqualTo(AttendanceStatus.FAILED)
        assertThat(second.failedStep).isEqualTo(AttendanceStep.SUBMIT)
    }

    @Test
    fun `emits empty list when dao has no records`() = runTest {
        val viewModel = HistoryViewModel(FakeAbsensiHistoriDao(emptyList()))

        val state = viewModel.state.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.items).isEmpty()
    }
}
