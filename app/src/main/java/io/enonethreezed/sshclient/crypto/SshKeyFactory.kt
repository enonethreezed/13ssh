package io.enonethreezed.sshclient.crypto

import io.enonethreezed.sshclient.model.KeySource
import io.enonethreezed.sshclient.model.SshAlgorithm
import io.enonethreezed.sshclient.model.SshKeySpec
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECKey
import java.security.interfaces.RSAKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.UUID

object SshKeyFactory {
    fun generate(name: String, algorithm: SshAlgorithm, sizeLabel: String): SshKeySpec {
        val generated = when (algorithm) {
            SshAlgorithm.ED25519 -> generateEd25519Pair()
            else -> {
                val pair = createGenerator(algorithm, sizeLabel).generateKeyPair()
                GeneratedKeyMaterial(pair.public.encoded, pair.private.encoded)
            }
        }

        return SshKeySpec(
            id = "key-${UUID.randomUUID()}",
            name = name,
            algorithm = algorithm,
            sizeLabel = sizeLabel,
            source = KeySource.GENERATED,
            fingerprint = fingerprint(generated.publicBytes),
            publicKey = pem("PUBLIC KEY", generated.publicBytes),
            privateKey = pem("PRIVATE KEY", generated.privateBytes),
        )
    }

    fun importValidated(
        name: String,
        publicKeyPem: String,
        privateKeyPem: String,
    ): Result<SshKeySpec> = runCatching {
        val publicBytes = decodePem(publicKeyPem, "PUBLIC KEY")
        val privateBytes = decodePem(privateKeyPem, "PRIVATE KEY")

        val candidates = listOf(
            SshAlgorithm.ED25519 to "Ed25519",
            SshAlgorithm.RSA to "RSA",
            SshAlgorithm.ECDSA to "EC",
        )

        val match = candidates.firstNotNullOfOrNull { (algorithm, factoryName) ->
            val factory = runCatching { KeyFactory.getInstance(factoryName) }.getOrNull() ?: return@firstNotNullOfOrNull null
            val publicKey = runCatching { factory.generatePublic(X509EncodedKeySpec(publicBytes)) }.getOrNull() ?: return@firstNotNullOfOrNull null
            val privateKey = runCatching { factory.generatePrivate(PKCS8EncodedKeySpec(privateBytes)) }.getOrNull() ?: return@firstNotNullOfOrNull null
            if (isMatchingPair(algorithm, publicKey, privateKey)) {
                Triple(algorithm, publicKey, privateKey)
            } else {
                null
            }
        } ?: error("The imported public and private keys do not belong to the same pair.")

        val algorithm = match.first
        val publicKey = match.second

        SshKeySpec(
            id = "key-${UUID.randomUUID()}",
            name = name,
            algorithm = algorithm,
            sizeLabel = inferSizeLabel(algorithm, publicKey),
            source = KeySource.IMPORTED,
            fingerprint = fingerprint(publicBytes),
            publicKey = publicKeyPem.trim(),
            privateKey = privateKeyPem.trim(),
        )
    }

    private fun createGenerator(algorithm: SshAlgorithm, sizeLabel: String): KeyPairGenerator {
        return when (algorithm) {
            SshAlgorithm.ED25519 -> error("Ed25519 uses the dedicated generator path")
            SshAlgorithm.RSA -> KeyPairGenerator.getInstance("RSA").apply {
                initialize(sizeLabel.substringBefore('-').toInt())
            }
            SshAlgorithm.ECDSA -> KeyPairGenerator.getInstance("EC").apply {
                initialize(ecCurve(sizeLabel))
            }
        }
    }

    private fun generateEd25519Pair(): GeneratedKeyMaterial {
        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val pair: AsymmetricCipherKeyPair = generator.generateKeyPair()
        val publicBytes = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(pair.public).encoded
        val privateBytes = PrivateKeyInfoFactory.createPrivateKeyInfo(pair.private).encoded
        return GeneratedKeyMaterial(publicBytes, privateBytes)
    }

    private fun ecCurve(sizeLabel: String): ECGenParameterSpec = when (sizeLabel) {
        "256-bit (P-256)" -> ECGenParameterSpec("secp256r1")
        "384-bit (P-384)" -> ECGenParameterSpec("secp384r1")
        else -> ECGenParameterSpec("secp521r1")
    }

    private fun isMatchingPair(algorithm: SshAlgorithm, publicKey: PublicKey, privateKey: PrivateKey): Boolean {
        val signature = Signature.getInstance(signatureName(algorithm))
        val payload = "13ssh-key-check".encodeToByteArray()
        signature.initSign(privateKey)
        signature.update(payload)
        val signed = signature.sign()
        signature.initVerify(publicKey)
        signature.update(payload)
        return signature.verify(signed)
    }

    private fun signatureName(algorithm: SshAlgorithm): String = when (algorithm) {
        SshAlgorithm.ED25519 -> "Ed25519"
        SshAlgorithm.RSA -> "SHA256withRSA"
        SshAlgorithm.ECDSA -> "SHA256withECDSA"
    }

    private fun inferSizeLabel(algorithm: SshAlgorithm, publicKey: PublicKey): String = when (algorithm) {
        SshAlgorithm.ED25519 -> "255-bit (Ed25519)"
        SshAlgorithm.RSA -> "${(publicKey as RSAKey).modulus.bitLength()}-bit"
        SshAlgorithm.ECDSA -> when ((publicKey as ECKey).params.order.bitLength()) {
            256 -> "256-bit (P-256)"
            384 -> "384-bit (P-384)"
            else -> "521-bit (P-521)"
        }
    }

    private fun pem(label: String, bytes: ByteArray): String {
        val body = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(bytes)
        return "-----BEGIN $label-----\n$body\n-----END $label-----"
    }

    private fun decodePem(pem: String, label: String): ByteArray {
        val normalized = pem
            .replace("-----BEGIN $label-----", "")
            .replace("-----END $label-----", "")
            .replace("\\s".toRegex(), "")
        require(normalized.isNotBlank()) { "Invalid $label PEM block." }
        return Base64.getDecoder().decode(normalized)
    }

    private fun fingerprint(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(digest).take(16)
    }
}

private data class GeneratedKeyMaterial(
    val publicBytes: ByteArray,
    val privateBytes: ByteArray,
)
