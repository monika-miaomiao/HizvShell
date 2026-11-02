package com.hizv.shell.adb

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigInteger
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * ADB over TCP 直连客户端（支持 Android 11+ 无线调试 TLS 加密）。
 * 连接流程：TCP → STLS/ATLS → TLS 1.3 → CNXN/OPEN
 */
class AdbTcpClient(private val context: Context) {

    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    var isConnected: Boolean = false
        private set

    private val A_CNXN = 0x4e58434e
    private val A_OPEN = 0x4e4e504f
    private val A_OKAY = 0x59414b4f
    private val A_WRTE = 0x45545257
    private val A_CLSE = 0x45534c43

    private var localId = 1
    private var remoteId = 0

    companion object {
        private const val KEYSTORE_FILE = "hizv_adb_keystore.bks"
        private const val KEY_ALIAS = "hizv_adb"
        private const val KEY_PASSWORD = "hizv"
    }

    fun connect(host: String = "localhost", port: Int): Boolean {
        return try {
            val rawSocket = Socket(host, port)
            rawSocket.tcpNoDelay = true

            val din = DataInputStream(rawSocket.getInputStream())
            val greeting = ByteArray(4)
            din.readFully(greeting)
            val greetingStr = String(greeting, Charsets.US_ASCII)

            if (greetingStr == "STLS") {
                rawSocket.getOutputStream().write("ATLS".toByteArray(Charsets.US_ASCII))
                rawSocket.getOutputStream().flush()
                val sslSocket = upgradeToTls(rawSocket)
                socket = sslSocket
                outputStream = DataOutputStream(sslSocket.outputStream)
                inputStream = DataInputStream(sslSocket.inputStream)
            } else {
                // 旧式明文连接（adb tcpip 模式）
                socket = rawSocket
                outputStream = DataOutputStream(rawSocket.getOutputStream())
                inputStream = DataInputStream(rawSocket.getInputStream())
            }

            val cnxnPayload = "host::features=shell,exec,sendrecv,\u0000".toByteArray(Charsets.UTF_8)
            sendMessage(A_CNXN, 0x01000001, cnxnPayload.size, cnxnPayload)

            val (cmd, _, _, _) = readMessage()
            if (cmd != A_CNXN) {
                if (cmd == 0x48545541) { // AUTH
                    throw IOException("设备未授权：需要先配对（第1步填配对码）")
                }
                throw IOException("Expected CNXN, got $cmd (${Integer.toHexString(cmd)})")
            }

            val openPayload = "shell:\u0000".toByteArray(Charsets.UTF_8)
            sendMessage(A_OPEN, localId, 0, openPayload)

            val (cmd2, rId, _, _) = readMessage()
            if (cmd2 != A_OKAY) throw IOException("Expected OKAY, got $cmd2")
            remoteId = rId

            isConnected = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            disconnect()
            false
        }
    }

    private fun upgradeToTls(rawSocket: Socket): SSLSocket {
        val keyStore = getOrCreateKeyStore()
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, KEY_PASSWORD.toCharArray())

        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(kmf.keyManagers, trustAll, SecureRandom())

        val sslSocket = sslContext.socketFactory.createSocket(
            rawSocket, rawSocket.inetAddress.hostAddress, rawSocket.port, true
        ) as SSLSocket
        sslSocket.useClientMode = true
        sslSocket.enabledProtocols = arrayOf("TLSv1.3")
        sslSocket.startHandshake()
        return sslSocket
    }

    private fun getOrCreateKeyStore(): KeyStore {
        val ksFile = File(context.filesDir, KEYSTORE_FILE)
        val ks = KeyStore.getInstance("BKS")

        if (ksFile.exists()) {
            FileInputStream(ksFile).use { ks.load(it, KEY_PASSWORD.toCharArray()) }
            return ks
        }

        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keyPair = kpg.generateKeyPair()
        val cert = generateSelfSignedCert(keyPair)

        ks.load(null, KEY_PASSWORD.toCharArray())
        ks.setKeyEntry(KEY_ALIAS, keyPair.private, KEY_PASSWORD.toCharArray(), arrayOf(cert))
        FileOutputStream(ksFile).use { ks.store(it, KEY_PASSWORD.toCharArray()) }
        return ks
    }

    private fun generateSelfSignedCert(keyPair: KeyPair): X509Certificate {
        val now = System.currentTimeMillis()
        val dn = X500Name("CN=HizvShell, O=Hizv, C=CN")
        val builder = JcaX509v3CertificateBuilder(
            dn, BigInteger.valueOf(1),
            Date(now - 1000), Date(now + 100L * 365 * 24 * 60 * 60 * 1000),
            dn, keyPair.public
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        return JcaX509CertificateConverter().getCertificate(builder.build(signer))
    }

    fun exec(command: String): String {
        if (!isConnected) throw IOException("Not connected")
        return try {
            val cmd = "$command\nexit\n"
            outputStream!!.write(cmd.toByteArray())
            outputStream!!.flush()
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

    fun writeStdin(data: String) {
        if (!isConnected) return
        outputStream?.write(data.toByteArray())
        outputStream?.flush()
    }

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
        val crc = 0
        val header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(command).putInt(arg0).putInt(remoteId)
            .putInt(dataLength).putInt(crc).putInt(magic).array()
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
