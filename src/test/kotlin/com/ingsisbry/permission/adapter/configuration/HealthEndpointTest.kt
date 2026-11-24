package com.ingsisbry.permission.adapter.configuration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `health is reachable without a token`() {
        mockMvc.get("/actuator/health").andExpect {
            status { isOk() }
            jsonPath("\$.status") { value("UP") }
            jsonPath("\$.components") { doesNotExist() }
        }
    }

    @Test
    fun `readiness and liveness probes are reachable without a token`() {
        mockMvc.get("/actuator/health/readiness").andExpect { status { isOk() } }
        mockMvc.get("/actuator/health/liveness").andExpect { status { isOk() } }
    }

    @Test
    fun `other actuator endpoints are not reachable without a token`() {
        mockMvc.get("/actuator/info").andExpect { status { isUnauthorized() } }
    }
}
