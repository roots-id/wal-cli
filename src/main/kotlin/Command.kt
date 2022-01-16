@file:OptIn(ExperimentalCli::class)

import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.rootsid.wal.library.*
import io.iohk.atala.prism.identity.Did
import io.iohk.atala.prism.identity.PrismDid
import kotlinx.cli.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.didcommx.didcomm.exceptions.DIDCommException
import org.didcommx.peerdid.MalformedPeerDIDException
import org.didcommx.peerdid.VerificationMaterialFormatPeerDID
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NewWallet : Subcommand("new-wallet", "Create a wallet") {
    private val walletName by argument(ArgType.String, "name", "Wallet name")
    private val mnemonic by option(
        ArgType.String,
        "mnemonic",
        "m",
        "Mnemonic phrase. Use '${Constant.MNEMONIC_SEPARATOR}' separated words"
    ).default("")
    private val passphrase by option(ArgType.String, "passphrase", "p", "Passphrase.").default("")

    override fun execute() {
        try {
            val wal = newWallet(walletName, mnemonic, passphrase)
            insertWallet(openDb(), wal)
        } catch (e: Exception) {
            println("new-wallet command failed:")
            println(red(e.message ?: ""))
        }
    }
}

/**
 * List wallets
 *
 * @constructor Create empty List wallets
 */
class ListWallets : Subcommand("list-wallets", "List wallets") {
    override fun execute() {
        try {
            val db = openDb()
            val wallets = findWallets(db)
            if (wallets.isNotEmpty()) {
                println("Wallets list:\n")
                for (wallet in wallets) {
                    println(wallet._id)
                }
            } else {
                println("No wallets to show.")
            }
        } catch (e: Exception) {
            println("list-wallets ${red("failed")}:")
            println(e.message)
        }
    }
}

class ShowMnemonic : Subcommand("show-mnemonic", "Show wallet mnemonic phrase and passphrase") {
    private val walletName by argument(ArgType.String, "name", "Wallet name")
    override fun execute() {
        try {
            val db = openDb()
            val wallet = findWallet(db, walletName)
            val mnemonic = wallet.mnemonic.reduce { mnemonic, word -> "$mnemonic,$word" }
            println("Mnemonic: $mnemonic")
        } catch (e: Exception) {
            println("get-mnemonic ${red("failed")}:")
            println(e.message)
        }
    }
}

class ExportWallet : Subcommand("export-wallet", "Export a wallet") {
    private val walletName by argument(ArgType.String, "wallet", "Wallet name")
    private var filename by argument(ArgType.String, "filename", "Output filename (json)")
    override fun execute() {
        try {
            val db = openDb()
            val wallet = findWallet(db, walletName)
            val walletString = Json.encodeToString(wallet)
            if (! filename.endsWith(".json")) {
                filename = "$filename.json"
            }
            File(filename).writeText(walletString)
        } catch (e: Exception) {
            println(e.message)
        }
    }
}

class ImportWallet : Subcommand("import-wallet", "Import a wallet") {
    private val filename by argument(ArgType.String, "filename", "Input filename (json)")
    override fun execute() {
        try {
            val walletString = File(filename).readText()
            val wallet = Json.decodeFromString<Wallet>(walletString)
            val db = openDb()
            updateWallet(db, wallet)
        } catch (e: Exception) {
            println(e.message)
        }
    }
}

class NewDID : Subcommand("new-did", "Create a DID") {
    private val walletName by argument(ArgType.String, "wallet", "Wallet name")
    private val didAlias by argument(ArgType.String, "alias", "DID alias")
    private val issuer by option(ArgType.Boolean, "issuer", "i", "Add issuing and revocation keys").default(false)
    override fun execute() {
        try {
            val db = openDb()
            var wallet = findWallet(db, walletName)

            if (didAliasExists(db, walletName, didAlias)) {
                println("new-did command failed:")
                println("Duplicated DID alias '${red(didAlias)}'")
                return
            }
            wallet = newDid(wallet, didAlias, issuer)
            updateWallet(db, wallet)
        } catch (e: Exception) {
            println(e.message)
        }
    }
}

class ListDID : Subcommand("list-dids", "List wallet DIDs") {
    private val walletName by argument(ArgType.String, "wallet", "Wallet name")
    override fun execute() {
        try {
            val db = openDb()
            val wallet = findWallet(db, walletName)
            val didList = wallet.dids
            if (didList.isNotEmpty()) {
                println("DID alias list:\n")
                for (did in didList) {
                    println(did.alias)
                }
            } else {
                println("Wallet '${red(walletName)}' is empty.")
            }
        } catch (e: Exception) {
            println("get-did command ${red("failed")}:")
            println(e.message)
        }
    }
}

class ShowDID : Subcommand("show-did", "Show a DID document") {
    private val walletName by argument(ArgType.String, "wallet", "Wallet name")
    private val didAlias by argument(ArgType.String, "alias", "DID alias")
    override fun execute() {
        try {
            val db = openDb()
            val wallet = findWallet(db, walletName)
            val didList = wallet.dids.filter { it.alias == didAlias }
            if (didList.isNotEmpty()) {
                val did = didList[0]
                println("DID alias: ${did.alias}")
                println("DID index: ${did.didIdx}")
                println("URI Canonical: ${did.uriCanonical}")
                println("URI Long: ${green(did.uriLongForm)}")
            } else {
                println("DID alias '${red(didAlias)}' not found.")
            }
        } catch (e: Exception) {
            println("get-did command ${red("failed")}:")
            println(e.message)
        }
    }
}

class PublishDID : Subcommand("publish-did", "Publish a DID") {
    private val walletName by argument(ArgType.String, "wallet", "Wallet name")
    private val didAlias by argument(ArgType.String, "alias", "DID alias")
    override fun execute() {
        try {
            val db = openDb()
            var wallet = findWallet(db, walletName)

            if (didAliasExists(db, walletName, didAlias)) {
                wallet = publishDid(wallet, didAlias)
                updateWallet(db, wallet)
            } else {
                println("publish-did command failed:")
                println("DID '${red(didAlias)}' not found")
                return
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }
}

private fun now(): String? {
    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return current.format(formatter)
}

private fun sampleCredentialClaim(holderUri: String): Claim {
    val name = listOf("Alice", "Bob", "Charlie", "David", "Eve", "Felix", "Gavin")
        .asSequence().shuffled().find { true }
    val degree = listOf("Law", "Data Science", "Economics", "Computer Science", "Politics", "Education")
        .asSequence().shuffled().find { true }

    return Claim(
        subjectDid = holderUri,
        content = JsonObject(
            mapOf(
                "name" to JsonPrimitive(name),
                "degree" to JsonPrimitive(degree),
                "date" to JsonPrimitive(now())
            )
        ).toString()
    )
}

class IssueCred : Subcommand("issue-cred", "Issue a credential") {
    private val walletName by argument(ArgType.String, "wallet", "Issuer wallet name")
    private val didAlias by argument(ArgType.String, "issuer", "Issuer DID alias")
    private val holderUri by argument(ArgType.String, "holder", "Holder DID uri")
    private val credentialAlias by argument(ArgType.String, "credential", "Credential alias")
    private val jsonFilename by option(
        ArgType.String, "Credential claim json file", "j",
        "Credential Subject json file. Placeholder json will be used if a filename isn't provided"
    ).default("")

    override fun execute() {
        try {
            val db = openDb()
            val wallet = findWallet(db, walletName)
            if (didAliasExists(db, walletName, didAlias)) {
                // TODO: Verify credential alias not duplicate on DB
                // Just for validation
                PrismDid.fromDid(Did.fromString(holderUri))
                val credential = Credential(
                    credentialAlias,
                    sampleCredentialClaim(holderUri),
                    VerifiedCredential("", ""),
                    "",
                    "",
                    ""
                )
                issueCredential(wallet, didAlias, credential)
                insertCredential(db, credential)
                updateWallet(db, wallet)
            }
        } catch (e: Exception) {
            println("issue-credential command ${red("failed")}:")
            println(e.message)
        }
    }
}

class VerifyCred : Subcommand("verify-cred", "Verify a credential") {
    private val credentialAlias by argument(ArgType.String, "alias", "Credential alias")
    override fun execute() {
        try {
            val db = openDb()
            val credential = findCredential(db, credentialAlias)
            val result = verifyCredential(credential)
            if (result.verificationErrors.isEmpty()) {
                println(green("Valid credential."))
            } else {
                println(red("Invalid credential."))
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }
}

class RevokeCred : Subcommand("revoke-cred", "Revoke a credential") {
    private val walletName by argument(ArgType.String, "wallet", "Issuer wallet name")
    private val didAlias by argument(ArgType.String, "issuer", "Issuer DID alias")
    private val credentialAlias by argument(ArgType.String, "credential", "Credential alias")

    override fun execute() {
        try {
            val db = openDb()
            val wallet = findWallet(db, walletName)
            if (didAliasExists(db, walletName, didAlias)) {
                val credential = findCredential(db, credentialAlias)
                revokeCredential(wallet, didAlias, credential)
                // TODO: flag credential revoked on db
                // insertCredential(db, credential)
                // updateWallet(db, wallet)
            }
        } catch (e: Exception) {
            println("$name command ${red("failed")}:")
            println(e.message)
        }
    }
}

class ExportCred : Subcommand("export-cred", "Export an issued credential") {
    private val credentialAlias by argument(ArgType.String, "alias", "Credential alias")
    private var filename by argument(ArgType.String, "filename", "Output filename (json)")
    override fun execute() {
        try {
            val db = openDb()
            val verifiedCredential = findCredential(db, credentialAlias).verifiedCredential
            val credentialJson = JsonObject(
                mapOf(
                    "encodedSignedCredential" to JsonPrimitive(verifiedCredential.encodedSignedCredential),
                    "proof" to Json.parseToJsonElement(verifiedCredential.proof)
                )
            )
            if (! filename.endsWith(".json")) {
                filename = "$filename.json"
            }
            File(filename).writeText(credentialJson.toString())
        } catch (e: Exception) {
            println(e.message)
        }
    }
}

class ImportCred : Subcommand(gray("import-cred"), "Import a credential") {
    override fun execute() {
    }
}

class AddKey : Subcommand(gray("add-key"), "Add a key to a DID") {
    override fun execute() {
    }
}

class RevokeKey : Subcommand(gray("revoke-key"), "Revoke DID key") {
    override fun execute() {
    }
}

class PeerDIDCreatorCommand : Subcommand("create-peer-did", "Creates a new Peer DID and corresponding secrets") {

    val authKeysCount by option(
        ArgType.Int,
        description = "Number of authentication keys",
        fullName = "auth-keys-count"
    ).default(1)
    val agreementKeysCount by option(
        ArgType.Int,
        description = "Number of agreement keys",
        fullName = "agreement-keys-count"
    ).default(1)
    val serviceEndpoint by option(
        ArgType.String,
        description = "Service endpoint",
        fullName = "service-endpoint"
    )
    val serviceRoutingKeys by option(
        ArgType.String,
        description = "Service routing keys",
        fullName = "service-routing-key"
    ).multiple()

    override fun execute() {
        val res = try {
            createPeerDID(
                authKeysCount = authKeysCount, agreementKeysCount = agreementKeysCount,
                serviceEndpoint = serviceEndpoint, serviceRoutingKeys = serviceRoutingKeys,
                SecretResolver()
            )
        } catch (e: IllegalArgumentException) {
            e.localizedMessage
        }
        println()
        println(res)
        println()
    }
}

enum class Format {
    JWK,
    BASE58,
    MULTIBASE
}

class ResolvePeerDIDCommand : Subcommand("resolve-peer-did", "Resolve a Peer DID to DID Doc JSON") {

    val did by argument(ArgType.String, description = "Peer DID to be resolved")
    val format by option(
        ArgType.Choice<Format>(),
        shortName = "f",
        description = "Peer DID to be resolved"
    ).default(Format.JWK)

    override fun execute() {
        val res = try {
            resolvePeerDID(
                did,
                format = when (format) {
                    Format.JWK -> VerificationMaterialFormatPeerDID.JWK
                    Format.BASE58 -> VerificationMaterialFormatPeerDID.BASE58
                    Format.MULTIBASE -> VerificationMaterialFormatPeerDID.MULTIBASE
                }
            )
        } catch (e: MalformedPeerDIDException) {
            e.localizedMessage
        }
        println()
        println(res)
        println()
    }
}

class PackCommand : Subcommand("pack", "Packs the message") {

    val message by argument(ArgType.String, description = "Message to pack")
    val to by option(ArgType.String, description = "Receiver's DID").required()
    val from by option(ArgType.String, description = "Sender's DID. Anonymous encryption is used if not set.")
    val signFrom by option(
        ArgType.String,
        fullName = "sign-from",
        description = "Sender's DID for optional signing. The message is not signed if not set."
    )
    val protectSender by option(
        ArgType.Boolean,
        fullName = "protect-sender",
        description = "Whether the sender's ID (DID) must be hidden. True by default."
    ).default(true)

    override fun execute() {
        val res = try {
            pack(
                data = message, to = to, from = from, signFrom = signFrom, protectSender = protectSender,
                SecretResolver()
            ).packedMessage
        } catch (e: DIDCommException) {
            e.localizedMessage
        }
        println()
        println(res)
        println()
    }
}

class UnpackCommand : Subcommand("unpack", "Unpacks the message") {

    val message by argument(ArgType.String, description = "Message to unpack. Use single quotes '<json>'")

    override fun execute() {
        println("Message:$message")
        val res = try {
            val unpackRes = unpack(message, SecretResolver())
            unpackRes.from?.let {
                "authcrypted '${unpackRes.message}' from ${unpackRes.from} to ${unpackRes.to}"
            } ?: {
                "anoncrypted '${unpackRes.message}' to ${unpackRes.to}"
            }
        } catch (e: DIDCommException) {
            e.localizedMessage
        }
        println()
        println(res)
        println()
    }
}
