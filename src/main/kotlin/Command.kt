@file:OptIn(ExperimentalCli::class)

import com.github.ajalt.mordant.rendering.TextColors.*
import com.rootsid.wal.library.*
import io.iohk.atala.prism.identity.Did
import io.iohk.atala.prism.identity.PrismDid
import io.iohk.atala.prism.protos.GrpcOptions
import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.cli.ExperimentalCli
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
            val mnemonic = wallet.mnemonic.reduce() { mnemonic, word -> "$mnemonic,$word" }
            println("Mnemonic: $mnemonic")
        } catch (e: Exception) {
            println("get-mnemonic ${red("failed")}:")
            println(e.message)
        }
    }
}

class ExportWallet : Subcommand(gray("export-wallet"), "Export a wallet") {
    override fun execute() {
    }
}

class ImportWallet : Subcommand(gray("import-wallet"), "Import a wallet") {
    override fun execute() {
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
                // Just for validation
                PrismDid.fromDid(Did.fromString(holderUri))
                val credential = Credential(
                    credentialAlias,
                    sampleCredentialClaim(holderUri),
                    VerifiedCredential("", "")
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

class RevokeCred : Subcommand(gray("revoke-cred"), "Revoke a credential") {
    override fun execute() {
    }
}

class ExportCred : Subcommand(gray("export-cred"), "Export a credential") {
    override fun execute() {
    }
}

class ImportCred : Subcommand(gray("import-cred"), "Import a credential") {
    override fun execute() {
    }
}

class IssueBatch : Subcommand(gray("issue-batch"), "Issue a batch of credentials") {
    override fun execute() {
    }
}

class RevokeBatch : Subcommand(gray("revoke-batch"), "Revoke a batch of credentials") {
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

class RotateKey : Subcommand(gray("rotate-key"), "Rotate DID key") {
    override fun execute() {
    }
}
// TODO: get values from environment
class EnvVar {
    companion object {
        val grpcOptions = GrpcOptions("https", "ppp.atalaprism.io", 50053)
    }
}
