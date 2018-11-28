package io.zhudy.notty

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass

/**
 * @author Kevin Zou (yong.zou@2339.com)
 */
open class HttpMockServer {

    private val wireMockServer = WireMockServer(options().port(0))

    /**
     *
     */
    val baseUri: String
        get() = "http://localhost:${wireMockServer.port()}"

    @BeforeClass
    fun beforeClass() {
        wireMockServer.start()
        initStub(wireMockServer)
    }

    @AfterClass
    fun afterClass() {
        wireMockServer.shutdown()
    }

    private fun initStub(wireMockServer: WireMockServer) = wireMockServer.apply {

        stubFor(get("/hello")
                .willReturn(
                        ok()
                                .withBody("Hello World!!!")
                ))

    }
}