@file:OptIn(ExperimentalCli::class)

import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.rootsid.wal.library.*
import io.iohk.atala.prism.identity.Did
import io.iohk.atala.prism.identity.PrismDid
import io.iohk.atala.prism.identity.PrismKeyType
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

// Use jsonFormat.encodeToString(did) to convert data class to pretty print json
val jsonFormat = Json { prettyPrint = true; encodeDefaults = true ; }

/**
 * now
 *
 * @return String representation of current date and time
 */
private fun now(): String? {
    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return current.format(formatter)
}

/**
 * Sample credential claim
 *
 * @param holderUri subject did
 * @return sample credential claim to use on credentials
 */
fun sampleCredentialClaim(holderUri: String): Claim {
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

enum class Format {
    JWK,
    BASE58,
    MULTIBASE
}

/**
 * New wallet
 *
 * @constructor Create New wallet command
 */
class NewWallet : Subcommand("new-wallet", "Create a wallet") {
    private val walletName by argument(ArgType.String, "name", "Wallet name")
    private val mnemonic by option(
        ArgType.String,
        "mnemonic",
        "m",
        "Mnemonic phrase. Use '${Constant.MNEMONIC_SEPARATOR}' separated words, no spaces"
    ).default("")
    private val passphrase by option(ArgType.String, "passphrase", "p", "Passphrase.").default("")
    override fun execute() {
        try {
            val db = openDb()
            if (!walletExists(db, walletName)) {
                val wal = newWallet(walletName, mnemonic, passphrase)
                insertWallet(db, wal)
                println(green("-- $name --"))
                println("wallet created")
            } else {
                throw Exception("wallet name already exists")
            }
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * List wallets
 *
 * @constructor Create List wallets command
 */
class ListWallets : Subcommand("list-wallets", "List wallets") {
    override fun execute() {
        try {
            val db = openDb()
            val wallets = listWallets(db)
            println(green("-- $name --"))
            if (wallets.isNotEmpty()) {
                for (wallet in wallets) {
                    println(wallet._id)
                }
            }
            println("\t${wallets.size} wallet(s)")
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * Show mnemonic
 *
 * @constructor Create Show mnemonic command
 */
class ShowMnemonic : Subcommand("show-mnemonic", "Show wallet mnemonic phrase and passphrase") {
    private val walletName by argument(ArgType.String, "name", "Wallet name")
    override fun execute() {
        try {
            val db = openDb()
            val wallet = findWallet(db, walletName)
            val mnemonic = wallet.mnemonic.reduce { mnemonic, word -> "$mnemonic,$word" }
            println(green("-- $name --"))
            println("Mnemonic: $mnemonic")
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * Export wallet
 *
 * @constructor Create empty Export wallet
 */
class ExportWallet : Subcommand("export-wallet", "Export a wallet") {
    private val walletName by argument(ArgType.String, "wallet", "Wallet name")
    private var filename by argument(ArgType.String, "filename", "Output filename (json)")
    override fun execute() {
        try {
            val db = openDb()
            val wallet = findWallet(db, walletName)
            val walletString = jsonFormat.encodeToString(wallet)
            if (! filename.endsWith(".json")) {
                filename = "$filename.json"
            }
            File(filename).writeText(walletString)
            println(green("-- $name --"))
            println("Wallet exported")
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * Import wallet
 *
 * @constructor Create empty Import wallet
 */
class ImportWallet : Subcommand("import-wallet", "Import a wallet") {
    private val filename by argument(ArgType.String, "filename", "Input filename (json)")
    override fun execute() {
        try {
            val walletString = File(filename).readText()
            val wallet = Json.decodeFromString<Wallet>(walletString)
            val db = openDb()
            updateWallet(db, wallet)
            println(green("-- $name --"))
            println("Wallet imported")
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * New d i d
 *
 * @constructor Create empty New d i d
 */
class NewDID : Subcommand("new-did", "Create a DID") {
    private val walletName by argument(ArgType.String, "wallet", "Wallet name")
    private val didAlias by argument(ArgType.String, "alias", "DID alias")
    private val issuer by option(ArgType.Boolean, "issuer", "i", "Add issuing and revocation keys").default(false)
    override fun execute() {
        try {
            val db = openDb()
            var wallet = findWallet(db, walletName)

            if (didAliasExists(db, walletName, didAlias)) {
                throw Exception("Duplicated DID alias")
            }
            wallet = newDid(wallet, didAlias, issuer)
            updateWallet(db, wallet)
            println(green("-- $name --"))
            println("DID created")
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * List d i d
 *
 * @constructor Create empty List d i d
 */
class ListDID : Subcommand("list-dids", "List wallet DIDs") {
    private val walletName by argument(ArgType.String, "wallet", "Wallet name")
    override fun execute() {
        try {
            val db = openDb()
            val wallet = findWallet(db, walletName)
            val didList = wallet.dids
            println(green("-- $name --"))
            if (didList.isNotEmpty()) {
                for (did in didList) {
                    println(did.alias)
                }
            }
            println("\t${didList.size} DID(s)")
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * Show d i d
 *
 * @constructor Create empty Show d i d
 */
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
                println(green("-- $name --"))
                println(jsonFormat.encodeToString(did))
            } else {
                throw Exception("DID alias not found.")
            }
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * Publish d i d
 *
 * @constructor Create empty Publish d i d
 */
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
                println(green("-- $name --"))
                println("DID published")
            } else {
                throw Exception("DID not found")
            }
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * Issue cred
 *
 * @constructor Create empty Issue cred
 */
class IssueCred : Subcommand("issue-cred", "Issue a credential") {
    private val walletName by argument(ArgType.String, "wallet", "Issuer wallet name")
    private val didAlias by argument(ArgType.String, "issuer", "Issuer DID alias")
    private val holderUri by argument(ArgType.String, "holder", "Holder DID uri")
    private val credentialAlias by argument(ArgType.String, "credential", "Credential alias")
    private val jsonFilename by option(
        ArgType.String, "Credential claim json file", "j",
        "Credential Subject json file. Placeholder json will be used if a filename isn't provided"
    ).default("")
    // TODO: enable use of credential claim from json file
    override fun execute() {
        try {
            val db = openDb()
            var wallet = findWallet(db, walletName)
            if (didAliasExists(db, walletName, didAlias) &&
                !issuedCredentialAliasExists(db, walletName, credentialAlias)
            ) {
                // Just for validation
                PrismDid.fromDid(Did.fromString(holderUri))
                val credential = IssuedCredential(
                    credentialAlias,
                    "",
                    sampleCredentialClaim(holderUri),
                    VerifiedCredential("", Proof("", 0, mutableListOf())),
                    "",
                    "",
                    "",
                    false
                )
                wallet = issueCredential(wallet, didAlias, credential)
                updateWallet(db, wallet)
                println(green("-- $name --"))
                println("Credential issued")
            } else {
                throw Exception("Duplicated credential alias, wallet not found or DID not found")
            }
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * Verify cred
 *
 * @constructor Create empty Verify cred
 */
class VerifyCred : Subcommand("verify-cred", "Verify a credential") {
    private val walletName by argument(ArgType.String, "wallet", "Issuer wallet name")
    private val list by argument(ArgType.Choice(listOf("issued", "imported"), { it }), "list", "List storing the credential")
    private val credentialAlias by argument(ArgType.String, "alias", "Credential alias")
    override fun execute() {
        try {
            val db = openDb()
            val wallet = findWallet(db, walletName)
            val result = if (list == "issued") {
                verifyIssuedCredential(wallet, credentialAlias)
            } else {
                verifyImportedCredential(wallet, credentialAlias)
            }
            println(green("-- $name --"))
            if (result.verificationErrors.isEmpty()) {
                println(green("Valid credential."))
            } else {
                println(red("Invalid credential."))
            }
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * Revoke cred
 *
 * @constructor Create empty Revoke cred
 */
class RevokeCred : Subcommand("revoke-cred", "Revoke a credential") {
    private val walletName by argument(ArgType.String, "wallet", "Issuer wallet name")
    private val credentialAlias by argument(ArgType.String, "credential", "Credential alias")

    override fun execute() {
        try {
            val db = openDb()
            val wallet = findWallet(db, walletName)
            revokeCredential(wallet, credentialAlias)
            updateWallet(db, wallet)
            println(green("-- $name --"))
            println("Credential revoked")
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * Export cred
 *
 * @constructor Create empty Export cred
 */
class ExportCred : Subcommand("export-cred", "Export an issued credential") {
    private val walletName by argument(ArgType.String, "wallet", "Issuer wallet name")
    private val credentialAlias by argument(ArgType.String, "alias", "Credential alias")
    private var filename by argument(ArgType.String, "filename", "Output filename (json)")
    override fun execute() {
        try {
            val db = openDb()
            val wallet = findWallet(db, walletName)
            val credentials = wallet.issuedCredentials.filter { it.alias == credentialAlias }
            if (credentials.isNotEmpty()) {
                val credential = credentials[0]
                if (!filename.endsWith(".json")) {
                    filename = "$filename.json"
                }
                File(filename).writeText(jsonFormat.encodeToString(credential.verifiedCredential))
                println(green("-- $name --"))
                println("Credential exported")
            } else {
                throw Exception("Credential not found")
            }
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * Import cred
 *
 * @constructor Create empty Import cred
 */
class ImportCred : Subcommand("import-cred", "Import a credential") {
    private val walletName by argument(ArgType.String, "wallet", "Issuer wallet name")
    private val credentialAlias by argument(ArgType.String, "alias", "Credential alias")
    private var filename by argument(ArgType.String, "filename", "Input filename (json)")
    override fun execute() {
        try {
            val db = openDb()
            if (!credentialAliasExists(db, walletName, credentialAlias)) {
                val wallet = findWallet(db, walletName)
                val text = File(filename).readText()
                val importedCredential = ImportedCredential(
                    credentialAlias,
                    Json.decodeFromString<VerifiedCredential>(text)
                )
                wallet.importedCredentials.add(importedCredential)
                updateWallet(db, wallet)
                println(green("-- $name --"))
                println("Credential imported")
            } else {
                throw Exception("Credential alias already in use")
            }
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * Add key
 *
 * @constructor Create empty Add key
 */
class AddKey : Subcommand("add-key", "Add a key to a DID") {
    private val walletName by argument(ArgType.String, "wallet", "Wallet name")
    private val didAlias by argument(ArgType.String, "alias", "DID alias")
    private val keyId by argument(ArgType.String, "keyId", "Key identifier")
    private val keyPurpose by argument(ArgType.Choice(listOf("master", "issuing", "revocation"), { it }), "keyType", "Key type")
    override fun execute() {
        try {
            val db = openDb()
            var wallet = findWallet(db, walletName)
            val keyType = when (keyPurpose) {
                "master" -> PrismKeyType.MASTER_KEY
                "issuing" -> PrismKeyType.ISSUING_KEY
                "revocation" -> PrismKeyType.REVOCATION_KEY
                else -> {
                    throw Exception("Unknown key type")
                }
            }
            if (didAliasExists(db, walletName, didAlias) &&
                ! keyIdExists(db, walletName, didAlias, keyId)
            ) {
                wallet = addKey(wallet, didAlias, keyId, keyType)
                updateWallet(db, wallet)
                println(green("-- $name --"))
                println("Key Added")
                return
            } else {
                throw Exception("Duplicated keyId, wallet not found or DID not found")
            }
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * Revoke key
 *
 * @constructor Create empty Revoke key
 */
class RevokeKey : Subcommand("revoke-key", "Revoke DID key") {
    private val walletName by argument(ArgType.String, "wallet", "Issuer wallet name")
    private val didAlias by argument(ArgType.String, "issuer", "Issuer DID alias")
    private val keyId by argument(ArgType.String, "keyId", "Key identifier")

    override fun execute() {
        try {
            val db = openDb()
            var wallet = findWallet(db, walletName)
            if (keyIdExists(db, walletName, didAlias, keyId)) {
                wallet = revokeKey(wallet, didAlias, keyId)
                updateWallet(db, wallet)
            } else{
                throw Exception("keyId not found")
            }
        } catch (e: Exception) {
            println(red("-- $name error --"))
            e.printStackTrace()
        }
    }
}

/**
 * Peer d i d creator command
 *
 * @constructor Create empty Peer d i d creator command
 */
class PeerDIDCreatorCommand : Subcommand("create-peer-did", "Creates a new Peer DID and corresponding secrets") {

    private val authKeysCount by option(
        ArgType.Int,
        description = "Number of authentication keys",
        fullName = "auth-keys-count"
    ).default(1)
    private val agreementKeysCount by option(
        ArgType.Int,
        description = "Number of agreement keys",
        fullName = "agreement-keys-count"
    ).default(1)
    private val serviceEndpoint by option(
        ArgType.String,
        description = "Service endpoint",
        fullName = "service-endpoint"
    )
    private val serviceRoutingKeys by option(
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

/**
 * Resolve peer d i d command
 *
 * @constructor Create empty Resolve peer d i d command
 */
class ResolvePeerDIDCommand : Subcommand("resolve-peer-did", "Resolve a Peer DID to DID Doc JSON") {

    private val did by argument(ArgType.String, description = "Peer DID to be resolved")
    private val format by option(
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

/**
 * Pack command
 *
 * @constructor Create empty Pack command
 */
class PackCommand : Subcommand("pack", "Packs the message") {
    private val message by argument(ArgType.String, description = "Message to pack")
    private val to by option(ArgType.String, description = "Receiver's DID").required()
    private val from by option(ArgType.String, description = "Sender's DID. Anonymous encryption is used if not set.")
    private val signFrom by option(
        ArgType.String,
        fullName = "sign-from",
        description = "Sender's DID for optional signing. The message is not signed if not set."
    )
    private val protectSender by option(
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

/**
 * Unpack command
 *
 * @constructor Create empty Unpack command
 */
class UnpackCommand : Subcommand("unpack", "Unpacks the message") {

    private val message by argument(ArgType.String, description = "Message to unpack. Use single quotes '<json>'")

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
