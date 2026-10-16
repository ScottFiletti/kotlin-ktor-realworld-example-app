package io.realworld.app.web.rules

import io.realworld.app.config.setup
import io.realworld.app.web.util.HttpUtil
import org.junit.rules.ExternalResource
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

class AppRule : ExternalResource() {
    // Each test app gets its own free port. On the fixed port 8080, an app already running locally
    // (e.g. `gradlew run`) made the test server fail to bind silently, and tests hit that app instead.
    val port = ServerSocket(0).use { it.localPort }
    private val app = setup(port = port)
    lateinit var http: HttpUtil

    override fun before() {
        app.start()
        TimeUnit.MILLISECONDS.sleep(500)
        http = HttpUtil(port)
    }

    override fun after() {
        app.stop(500, 500, TimeUnit.MILLISECONDS)
    }
}
