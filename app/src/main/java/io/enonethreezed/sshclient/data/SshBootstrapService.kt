package io.enonethreezed.sshclient.data

import android.content.Context
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.IOUtils
import net.schmizz.sshj.common.Factory
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.File

class SshBootstrapService(context: Context) {
    private val appContext = context.applicationContext

    fun verifyPublicKeyLogin(
        host: String,
        port: Int,
        username: String,
        privateKey: String,
    ): Result<Unit> = runCatching {
        verifyPublicKeyLoginOrThrow(host, port, username, privateKey)
    }

    fun bootstrapWithPasswordThenKey(
        host: String,
        port: Int,
        username: String,
        password: String,
        publicKey: String,
        privateKey: String,
    ): Result<Unit> = runCatching {
        installPublicKey(host, port, username, password, publicKey)
        verifyPublicKeyLoginOrThrow(host, port, username, privateKey)
    }

    private fun installPublicKey(
        host: String,
        port: Int,
        username: String,
        password: String,
        publicKey: String,
    ) {
        createClient().use { ssh ->
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connect(host, port)
            ssh.authPassword(username, password)
            ssh.startSession().use { session ->
                val command = "mkdir -p ~/.ssh && chmod 700 ~/.ssh && touch ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys && grep -qxF ${shellQuote(publicKey)} ~/.ssh/authorized_keys || echo ${shellQuote(publicKey)} >> ~/.ssh/authorized_keys"
                val exec = session.exec(command)
                exec.join()
                val output = IOUtils.readFully(exec.inputStream).toString()
                val errorOutput = IOUtils.readFully(exec.errorStream).toString()
                val status = exec.exitStatus ?: 1
                require(status == 0) {
                    "Failed to install key on remote host. ${if (errorOutput.isNotBlank()) errorOutput else output}".trim()
                }
            }
            ssh.disconnect()
        }
    }

    private fun verifyPublicKeyLoginOrThrow(
        host: String,
        port: Int,
        username: String,
        privateKey: String,
    ) {
        val privateKeyFile = writeTempPrivateKey(privateKey)
        try {
            createClient().use { ssh ->
                ssh.addHostKeyVerifier(PromiscuousVerifier())
                ssh.connect(host, port)
                val provider = ssh.loadKeys(privateKeyFile.absolutePath)
                ssh.authPublickey(username, provider)
                ssh.disconnect()
            }
        } finally {
            privateKeyFile.delete()
        }
    }

    private fun writeTempPrivateKey(privateKey: String): File {
        val file = File.createTempFile("13ssh-key-", ".pem", appContext.cacheDir)
        file.writeText(privateKey)
        return file
    }

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }

    private fun createClient(): SSHClient {
        val config = DefaultConfig()
        val compatibleKex = config.getKeyExchangeFactories().filterNot(::requiresX25519)
        config.setKeyExchangeFactories(compatibleKex)
        return SSHClient(config)
    }

    private fun requiresX25519(factory: Factory.Named<*>): Boolean {
        val name = factory.name.lowercase()
        return name.contains("curve25519") || name.contains("x25519")
    }
}
