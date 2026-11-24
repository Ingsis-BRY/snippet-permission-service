package com.ingsisbry.permission.adapter.configuration

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.filter.OncePerRequestFilter
import kotlin.test.Test
import kotlin.test.assertContains

/**
 * snippet-service sends a W3C `traceparent`; its trace id must reach this service's logs, which
 * are ECS JSON in production (`LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`).
 */
@SpringBootTest(properties = ["logging.structured.format.console=ecs"])
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension::class)
class TracingTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @TestConfiguration
    class LoggingFilterConfiguration {
        @Bean
        fun requestLogger(): FilterRegistrationBean<OncePerRequestFilter> =
            FilterRegistrationBean<OncePerRequestFilter>(
                object : OncePerRequestFilter() {
                    override fun doFilterInternal(
                        request: HttpServletRequest,
                        response: HttpServletResponse,
                        filterChain: FilterChain,
                    ) {
                        LoggerFactory.getLogger(TracingTest::class.java).info(PROBE_MESSAGE)
                        filterChain.doFilter(request, response)
                    }
                },
            ).apply { order = Ordered.LOWEST_PRECEDENCE }
    }

    @Test
    fun `an incoming traceparent is continued and logged as ECS JSON`(output: CapturedOutput) {
        mockMvc.get("/actuator/health") {
            header("traceparent", "00-$TRACE_ID-00f067aa0ba902b7-01")
        }

        val line = output.out.lines().first { it.contains(PROBE_MESSAGE) }
        assertContains(line, "\"traceId\":\"$TRACE_ID\"")
        assertContains(line, "\"ecs\":{\"version\"")
    }

    private companion object {
        const val TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736"
        const val PROBE_MESSAGE = "tracing-probe"
    }
}
