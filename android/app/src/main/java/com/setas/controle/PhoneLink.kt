package com.setas.controle

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Envia o estado das setas ao PC (127.0.0.1:28777, que o adb reverse leva pelo cabo).
 * Envia na hora a cada mudança e repete o estado a cada [heartbeatMs] como sinal de vida.
 * [onConnectionChanged] é chamado na thread de envio.
 */
class PhoneLink(
    private val host: String = "127.0.0.1",
    private val port: Int = 28777,
    private val heartbeatMs: Long = 100,
) {
    companion object {
        /** Primeiro byte que o programa do PC envia ao aceitar a conexão. */
        const val HELLO = 0xA5
    }

    @Volatile var onConnectionChanged: ((Boolean) -> Unit)? = null

    private val lock = Object()
    private var mask = 0
    private var dirty = false
    private var running = false
    private var thread: Thread? = null

    fun start() {
        synchronized(lock) {
            if (running) return
            running = true
            mask = 0
            dirty = true
        }
        thread = Thread(::loop, "PhoneLink").apply {
            isDaemon = true
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    /** Não bloqueia: guarda o estado e acorda a thread de envio. */
    fun send(mask: Int) {
        synchronized(lock) {
            if (this.mask == mask) return
            this.mask = mask
            dirty = true
            lock.notifyAll()
        }
    }

    /** Envia 0 (solta tudo no PC) e encerra a conexão. */
    fun stop() {
        synchronized(lock) {
            if (!running) return
            mask = 0
            dirty = true
            running = false
            lock.notifyAll()
        }
        thread?.join(1000)
        thread = null
    }

    private fun loop() {
        while (true) {
            synchronized(lock) { if (!running) return }
            var connected = false
            try {
                Socket().use { socket ->
                    socket.tcpNoDelay = true
                    socket.connect(InetSocketAddress(host, port), 500)
                    // O adb aceita a conexão mesmo com o programa do PC fechado:
                    // só conta como conectado depois do "olá" do PC.
                    socket.soTimeout = 1000
                    if (socket.getInputStream().read() != HELLO) throw IOException("sem olá do PC")
                    socket.soTimeout = 0
                    connected = true
                    onConnectionChanged?.invoke(true)
                    val out = socket.getOutputStream()
                    while (true) {
                        val value: Int
                        val keepGoing: Boolean
                        synchronized(lock) {
                            if (!dirty && running) lock.wait(heartbeatMs)
                            dirty = false
                            value = mask
                            keepGoing = running
                        }
                        out.write(value)
                        out.flush()
                        if (!keepGoing) return
                    }
                }
            } catch (_: IOException) {
            } catch (_: InterruptedException) {
                return
            } finally {
                if (connected) onConnectionChanged?.invoke(false)
            }
            synchronized(lock) {
                if (!running) return
                lock.wait(500)
            }
        }
    }
}
