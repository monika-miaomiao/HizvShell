package com.hizv.shell.adb

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ADB over TCP 直连客户端。
 * 连接 localhost:port，完成 CNXN/OPEN 握手后获得 shell。
 * 不依赖 Shizuku，需要用户先在系统无线调试中配对过一次。
 */
class AdbTcpClient {

    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    var isConnected: Boolean = false
        private set

    // ADB command constants
    private val A_CNXN = 0x4e58434e
    private val A_OPEN = 0x4e4e504f
    private val A_OKAY = 0x59414b4f
    private val A_WRTE = 0x45545257
    private val A_CLSE = 0x45534c43

    private var localId = 1
    private var remoteId = 0

    /**
     * 连接到 localhost:port 并打开 shell。
     */
    fun connect(host: String = "localhost", port: Int): Boolean {
        return try {
            socket = Socket(host, port)
            outputStream = DataOutputStream(socket!!.getOutputStream())
            inputStream = DataInputStream(socket!!.getInputStream())

            // 1. 发送 CNXN
            val cnxnPayload = "host::features=shell,exec,sendrecv,".toByteArray(Charsets.UTF_8)
            sendMessage(A_CNXN, 0x01000001, cnxnPayload.size, cnxnPayload)

            // 2. 接收 CNXN
            val (cmd, _, _, payload) = readMessage()
            if (cmd != A_CNXN) throw IOException("Expected CNXN, got $cmd")

            // 3. 发送 OPEN shell:
            val openPayload = "shell:\u0000".toByteArray(Charsets.UTF_8)
            sendMessage(A_OPEN, localId, 0, openPayload)

            // 4. 接收 OKAY
            val (cmd2, rId, _, _) = readMessage()
            if (cmd2 != A_OKAY) throw IOException("Expected OKAY, got $cmd2")
            remoteId = rId

            isConnected = true
            true
        } catch (e: Exception) {
            disconnect()
            false
        }
    }

    /**
     * 执行命令并返回输出。
     */
    fun exec(command: String): String {
        if (!isConnected) throw IOException("Not connected")
        return try {
            // 发送命令 + exit
            val cmd = "$command\nexit\n"
            outputStream!!.write(cmd.toByteArray())
            outputStream!!.flush()

            // 读取所有 WRTE 消息直到 CLSE
            val sb = StringBuilder()
            while (true) {
                val (msgCmd, _, _, payload) = readMessage()
                when (msgCmd) {
                    A_WRTE -> sb.append(String(payload, Charsets.UTF_8))
                    A_CLSE -> break
                    else -> {}
                }
            }
            sb.toString().trim()
        } catch (e: Exception) {
            isConnected = false
            throw e
        }
    }

    /**
     * 执行命令（非阻塞读取模式，用于终端）。
     */
    fun writeStdin(data: String) {
        if (!isConnected) return
        outputStream?.write(data.toByteArray())
        outputStream?.flush()
    }

    /**
     * 读取 shell 输出（阻塞）。
     */
    fun readStdout(): String {
        if (!isConnected) return ""
        return try {
            val (cmd, _, _, payload) = readMessage()
            if (cmd == A_WRTE) String(payload, Charsets.UTF_8) else ""
        } catch (e: Exception) {
            isConnected = false
            ""
        }
    }

    private fun sendMessage(command: Int, arg0: Int, dataLength: Int, payload: ByteArray) {
        val magic = command xor 0xffffffff.toInt()
        val crc = 0 // ADB doesn't strictly check CRC for localhost
        val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(command)
            .putInt(arg0)
            .putInt(remoteId)
            .putInt(dataLength)
            .putInt(crc)
            .putInt(magic)
            .array()
        outputStream!!.write(header)
        if (dataLength > 0) outputStream!!.write(payload, 0, dataLength)
        outputStream!!.flush()
    }

    private fun readMessage(): Message {
        val header = ByteArray(24)
        inputStream!!.readFully(header)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val command = buf.int
        val arg0 = buf.int
        val arg1 = buf.int
        val dataLength = buf.int
        val crc = buf.int
        val magic = buf.int

        val payload = if (dataLength > 0) {
            ByteArray(dataLength).also { inputStream!!.readFully(it) }
        } else ByteArray(0)

        return Message(command, arg0, arg1, payload)
    }

    fun disconnect() {
        try { outputStream?.close() } catch (_: Exception) {}
        try { inputStream?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        isConnected = false
    }

    data class Message(val command: Int, val arg0: Int, val arg1: Int, val payload: ByteArray)
}
