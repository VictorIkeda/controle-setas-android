package com.setas.controle

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class PhoneLinkTest {
    private lateinit var server: ServerSocket
    private var port = 0
    private lateinit var link: PhoneLink
    private val received = CopyOnWriteArrayList<Int>()
    private val connects = AtomicInteger()
    @Volatile private var accepted: Socket? = null
    @Volatile private var readerDone = false

    private var sendHello = true

    private fun openServer() {
        server = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress(InetAddress.getLoopbackAddress(), port))
        }
        port = server.localPort
        val s = server
        Thread {
            try {
                val c = s.accept()
                if (sendHello) c.getOutputStream().apply { write(0xA5); flush() }
                accepted = c
                val input = c.getInputStream()
                while (true) {
                    val b = input.read()
                    if (b < 0) break
                    received.add(b)
                }
            } catch (_: Exception) {
            } finally {
                readerDone = true
            }
        }.apply { isDaemon = true }.start()
    }

    private fun eventually(ms: Long, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + ms
        while (!cond() && System.currentTimeMillis() < end) Thread.sleep(5)
        assertTrue(cond())
    }

    @Before fun setUp() {
        openServer()
        link = PhoneLink("127.0.0.1", port, 100)
        link.onConnectionChanged = { if (it) connects.incrementAndGet() }
        link.start()
        eventually(1000) { accepted != null }
    }

    @After fun tearDown() {
        link.stop()
        server.close()
    }

    @Test fun sendsStateOnChange() {
        link.send(10)
        eventually(100) { received.contains(10) }
    }

    @Test fun heartbeatRepeatsState() {
        link.send(1)
        Thread.sleep(400)
        assertTrue("recebidos: $received", received.count { it == 1 } >= 3)
    }

    @Test fun reconnectsAfterServerRestart() {
        eventually(1000) { connects.get() == 1 }
        accepted?.close()
        server.close()
        accepted = null
        openServer()
        eventually(2000) { connects.get() == 2 }
    }

    @Test fun withoutHello_neverReportsConnected() {
        eventually(1000) { connects.get() == 1 }
        accepted?.close()
        server.close()
        accepted = null
        sendHello = false
        openServer()
        Thread.sleep(2500)
        assertEquals(1, connects.get())
    }

    @Test fun stop_sendsZeroFirst() {
        link.send(4)
        eventually(100) { received.contains(4) }
        link.stop()
        eventually(1000) { readerDone }
        assertEquals(0, received.last())
    }
}
