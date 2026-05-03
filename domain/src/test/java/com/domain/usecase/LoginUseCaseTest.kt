package com.domain.usecase

import com.utils.network.Resource
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class LoginUseCaseTest {
    private val repository = mockk<AuthRepository>()
    private lateinit var loginUseCase: LoginUseCase

    @Before
    fun setup() {
        loginUseCase = LoginUseCase(repository)
    }

    @Test
    fun `when credentials are empty, return error immediately`() = runTest {
        // No repository call should even happen
        val result = loginUseCase("", "").first()

        assert(result is Resource.Error)
        assertEquals("Credentials cannot be empty", (result as Resource.Error).message)
        coVerify(exactly = 0) { repository.login(any(), any()) }    }
}