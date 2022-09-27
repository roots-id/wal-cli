@file:OptIn(ExperimentalCli::class)

import com.mongodb.client.MongoDatabase
import com.rootsid.wal.library.*
import io.iohk.atala.prism.crypto.derivation.KeyDerivation
import kotlinx.cli.ArgParser
import kotlinx.cli.ExperimentalCli
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertContains

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommandTest {

    private lateinit var db: MongoDatabase

    private fun newParser(): ArgParser {
        val parser = ArgParser("wal")
        parser.subcommands(
            NewWallet(),
            ShowMnemonic(),
            ExportWallet(),
            ImportWallet(),
            ListWallets(),
            NewDID(),
            PublishDID(),
            ShowDIDData(),
            ShowDID(),
            ResolvePrismDid(),
            ListDID(),
            IssueCred(),
            VerifyCred(),
            ExportCred(),
            ImportCred(),
            RevokeCred(),
            AddKey(),
            RevokeKey(),
            PeerDIDCreatorCommand(),
            ResolvePeerDIDCommand(),
            PackCommand(),
            UnpackCommand()
        )
        return parser
    }

    private fun getFilePath(filename: String): String {
        val resourceDirectory: Path = Paths.get("src", "test", "resources", filename)
        return resourceDirectory.toFile().absolutePath
    }

    @BeforeEach
    fun beforeEach() {
        Config.DB_NAME = "walTest"
        db = openDb()
        db.getCollection("wallet").drop()
    }

    @Test
    fun newWalletTest() {
        val walletName = "issuer_wallet"
        // New wallet
        assertDoesNotThrow("New wallet") {
            newParser().parse(arrayOf("new-wallet", walletName))
        }
        assertEquals(true, walletExists(db, walletName), "Wallet stored on db")
        // Duplicated name
        assertThrows<Exception> ("Duplicated name") {
            newParser().parse(arrayOf("new-wallet", walletName))
        }
    }

    @Test
    fun listWalletTest() {
        val walletName = "issuer_wallet"
        // Zero wallets
        assertEquals(0, listWallets(db).size, "Expect zero wallets")
        assertDoesNotThrow("List wallet") {
            newParser().parse(arrayOf("list-wallets"))
        }
        // One wallet
        assertDoesNotThrow("New wallet") {
            newParser().parse(arrayOf("new-wallet", walletName))
        }
        assertDoesNotThrow("List wallet") {
            newParser().parse(arrayOf("list-wallets"))
        }
        assertEquals(1, listWallets(db).size, "Expect one wallet")
    }

    @Test
    fun showMnemonicTest() {
        val walletName = "issuer_wallet"
//        val mnemonic = "dragon,crouch,globe,width,vanish,frown,sadness,rose,mule,noodle,help,cover"
        val mnemonic = KeyDerivation.randomMnemonicCode().words.reduce{ acc, string -> "$acc,$string" }
        val passphrase = "secret"
        // New wallet
        println("generated mnemonic $mnemonic")
        assertDoesNotThrow("New wallet") {
            newParser().parse(arrayOf<String>("new-wallet", walletName, "-m", mnemonic, "-p", passphrase))
        }
        assertDoesNotThrow("Show mnemonic from existing wallet") {
            newParser().parse(arrayOf("show-mnemonic", walletName))
        }
        // Check mnemonic and passphrase
        val wallet = findWallet(db, walletName)
        assertContains(
            wallet.mnemonic.reduce { mnemonic, word -> "$mnemonic,$word" },
            mnemonic,
            true,
            "Check mnemonic phrase"
        )
        assertEquals(
            passphrase,
            wallet.passphrase,
            "Check passphrase"
        )
        // Try on non existing wallet
        assertThrows<Exception> ("Show mnemonic from non existing wallet") {
            newParser().parse(arrayOf("show-mnemonic", "not-a-wallet"))
        }
    }

    @Test
    fun exportWalletTest() {
        val walletName = "issuer_wallet"
        val fileName = getFilePath("test_wallet_export.json")
        // Export wallet
        assertDoesNotThrow("New wallet") {
            newParser().parse(arrayOf("new-wallet", walletName))
        }
        assertDoesNotThrow("Export from existing wallet") {
            newParser().parse(arrayOf("export-wallet", walletName, fileName))
        }
        assertEquals(true, File(fileName).exists(), "wallet json file created")
        File(fileName).delete()
        // non-existing wallet
        assertThrows<Exception>("Export from non existing wallet") {
            newParser().parse(arrayOf("export-wallet", "not-a-wallet", fileName))
        }
    }

    @Test
    fun importWalletTest() {
        val walletName = "export_wallet"
        val otherName = "other_wallet"
        val fileName = getFilePath("test_wallet_import.json")
        // Export a wallet
        assertDoesNotThrow("New wallet") {
            newParser().parse(arrayOf("new-wallet", walletName))
        }
        assertDoesNotThrow("Export from existing wallet") {
            newParser().parse(arrayOf("export-wallet", walletName, fileName))
        }
        // To import the same wallet name
        db.getCollection("wallet").drop()
        // using same name
        assertDoesNotThrow("Import wallet") {
            newParser().parse(arrayOf("import-wallet", fileName))
        }
        assertEquals(true, walletExists(db, walletName), "Check wallet imported")
        // using duplicated name
        assertThrows<Exception>("Duplicated wallet name") {
            newParser().parse(arrayOf("import-wallet", fileName, "-n", walletName))
        }
        // Using new name
        assertDoesNotThrow("Import wallet with new name") {
            newParser().parse(arrayOf("import-wallet", fileName, "-n", otherName))
        }
        assertEquals(true, walletExists(db, otherName), "Check wallet with other name imported")
        // using bad file
        assertThrows<Exception>("Import wallet with bad file") {
            newParser().parse(arrayOf("import-wallet", "not-a-file"))
        }
        File(fileName).delete()
    }

    @Test
    fun newDIDTest() {
        val holderWallet = "holder_wallet"
        val issuerWallet = "issuer_wallet"
        val holderDidAlias = "holder_did"
        val issuerDidAlias = "issuer_did"
        // Holder did
        assertDoesNotThrow("New holder wallet") {
            newParser().parse(arrayOf("new-wallet", holderWallet))
        }
        assertDoesNotThrow("New holder DID") {
            newParser().parse(arrayOf("new-did", holderWallet, holderDidAlias))
        }
        assertEquals(
            true,
            didAliasExists(db, holderWallet, holderDidAlias),
            "Check holder DID created"
        )
        // issuer did
        assertDoesNotThrow("New issuer wallet") {
            newParser().parse(arrayOf("new-wallet", issuerWallet))
        }
        assertDoesNotThrow("New issuer DID") {
            newParser().parse(arrayOf("new-did", issuerWallet, issuerDidAlias, "-i"))
        }
        assertEquals(
            true,
            didAliasExists(db, issuerWallet, issuerDidAlias),
            "Check issuer DID created"
        )
        // Non existing wallet
        assertThrows<Exception>("New DID on non existing wallet") {
            newParser().parse(arrayOf("new-did", "not-a-wallet", issuerDidAlias))
        }
        // Duplicated did alias
        assertThrows<Exception>("Duplicated DID alias") {
            newParser().parse(arrayOf("new-did", issuerWallet, issuerDidAlias))
        }
    }

    @Test
    fun listDidsTest() {
        val walletName = "holder_wallet"
        val holderDidAlias = "holder_did"
        assertDoesNotThrow("New wallet") {
            newParser().parse(arrayOf("new-wallet", walletName))
        }
        // Zero DIDs
        var wallet = findWallet(db, walletName)
        assertEquals(0, wallet.dids.size, "Expect zero did")
        assertDoesNotThrow("List dids") {
            newParser().parse(arrayOf("list-dids", walletName))
        }
        // One DID
        assertDoesNotThrow("New DID") {
            newParser().parse(arrayOf("new-did", walletName, holderDidAlias))
        }
        wallet = findWallet(db, walletName)
        assertEquals(1, wallet.dids.size, "Expect one did")
        assertDoesNotThrow("List dids") {
            newParser().parse(arrayOf("list-dids", walletName))
        }
        // List DID on unknown wallet
        assertThrows<Exception>("List DID on non existing wallet") {
            newParser().parse(arrayOf("list-dids", "not-a-wallet"))
        }
    }

    @Test
    fun showDIDDataTest() {
        val walletName = "holder_wallet"
        val holderDidAlias = "holder_did"
        assertDoesNotThrow("New wallet") {
            newParser().parse(arrayOf("new-wallet", walletName))
        }
        assertDoesNotThrow("New DID") {
            newParser().parse(arrayOf("new-did", walletName, holderDidAlias))
        }
        // Show DID
        assertDoesNotThrow("Show dids") {
            newParser().parse(arrayOf("show-did-data", walletName, holderDidAlias))
        }
        // unknown wallet
        assertThrows<Exception>("unknown wallet") {
            newParser().parse(arrayOf("show-did-data", "not-a-wallet", holderDidAlias))
        }
        // unknown did
        assertThrows<Exception>("unknown did") {
            newParser().parse(arrayOf("show-did-data", walletName, "not-a-did"))
        }
    }

    @Test
    fun publishDIDTest() {
        val issuerWallet = "issuer_wallet"
        val issuerDidAlias = "issuer_did"
        // publish issuer did
        assertDoesNotThrow("New issuer wallet") {
            newParser().parse(arrayOf("new-wallet", issuerWallet))
        }
        assertDoesNotThrow("New issuer DID") {
            newParser().parse(arrayOf("new-did", issuerWallet, issuerDidAlias, "-i"))
        }
        assertDoesNotThrow("Publish issuer DID") {
            newParser().parse(arrayOf("publish-did", issuerWallet, issuerDidAlias))
        }
        // unknown wallet
        assertThrows<Exception>("unknown wallet") {
            newParser().parse(arrayOf("publish-did", "not-a-wallet", issuerDidAlias))
        }
        // unknown did alias
        assertThrows<Exception>("unknown DID alias") {
            newParser().parse(arrayOf("publish-did", issuerWallet, "not-an-alias"))
        }
    }

    @Test
    fun showDidTest() {
        val issuerWallet = "issuer_wallet"
        val issuerDidAlias = "issuer_did"
        // resolve issuer did
        assertDoesNotThrow("New issuer wallet") {
            newParser().parse(arrayOf("new-wallet", issuerWallet))
        }
        assertDoesNotThrow("New issuer DID") {
            newParser().parse(arrayOf("new-did", issuerWallet, issuerDidAlias, "-i"))
        }
        assertDoesNotThrow("Resolve issuer DID") {
            newParser().parse(arrayOf("show-did", issuerWallet, issuerDidAlias))
        }
        // unknown wallet
        assertThrows<Exception>("unknown wallet") {
            newParser().parse(arrayOf("show-did", "not-a-wallet", issuerDidAlias))
        }
        // unknown did alias
        assertThrows<Exception>("unknown DID alias") {
            newParser().parse(arrayOf("show-did", issuerWallet, "not-an-alias"))
        }
    }

    @Test
    fun issueCredTest() {
        val walletName = "wallet"
        val issuerDidAlias = "issuer_did"
        val holderDidAlias = "holder_did"
        // Wallet and dids
        assertDoesNotThrow("New wallet") {
            newParser().parse(arrayOf("new-wallet", walletName))
        }
        assertDoesNotThrow("New holder DID") {
            newParser().parse(arrayOf("new-did", walletName, holderDidAlias))
        }
        assertDoesNotThrow("New issuer DID") {
            newParser().parse(arrayOf("new-did", walletName, issuerDidAlias, "-i"))
        }
        assertDoesNotThrow("Publish issuer DID") {
            newParser().parse(arrayOf("publish-did", walletName, issuerDidAlias))
        }
        // Issue credential
        val wallet = findWallet(db, walletName)
        val holderDid = wallet.dids.filter { it.alias == holderDidAlias }[0]
        assertDoesNotThrow("Issue credential") {
            newParser().parse(
                arrayOf(
                    "issue-cred",
                    walletName,
                    issuerDidAlias,
                    holderDid.uriLongForm,
                    "credential"
                )
            )
        }
        // Unknown wallet
        assertThrows<Exception>("Unknown wallet") {
            newParser().parse(
                arrayOf(
                    "issue-cred",
                    "not-a-wallet",
                    issuerDidAlias,
                    holderDid.uriLongForm,
                    "credential"
                )
            )
        }
        // Unknown issuer did
        assertThrows<Exception>("Unknown issuer did") {
            newParser().parse(
                arrayOf(
                    "issue-cred",
                    walletName,
                    "not-a-did",
                    holderDid.uriLongForm,
                    "credential"
                )
            )
        }
        // Invalid holder did
        assertThrows<Exception>("Invalid holder did") {
            newParser().parse(
                arrayOf(
                    "issue-cred",
                    walletName,
                    issuerDidAlias,
                    "not-a-did",
                    "credential"
                )
            )
        }
        // Duplicated credential alias
        assertThrows<Exception>("Duplicated credential alias ") {
            newParser().parse(
                arrayOf(
                    "issue-cred",
                    walletName,
                    issuerDidAlias,
                    holderDid.uriLongForm,
                    "credential"
                )
            )
        }
    }

    @Test
    fun verifyCredTest() {
        val walletName = "wallet"
        val issuerDidAlias = "issuer_did"
        val holderDidAlias = "holder_did"
        val credentialAlias = "credential"
        // Wallet and dids
        assertDoesNotThrow("New wallet") {
            newParser().parse(arrayOf("new-wallet", walletName))
        }
        assertDoesNotThrow("New holder DID") {
            newParser().parse(arrayOf("new-did", walletName, holderDidAlias))
        }
        assertDoesNotThrow("New issuer DID") {
            newParser().parse(arrayOf("new-did", walletName, issuerDidAlias, "-i"))
        }
        assertDoesNotThrow("Publish issuer DID") {
            newParser().parse(arrayOf("publish-did", walletName, issuerDidAlias))
        }
        // Issue credential
        val wallet = findWallet(db, walletName)
        val holderDid = wallet.dids.filter { it.alias == holderDidAlias }[0]
        assertDoesNotThrow("Issue credential") {
            newParser().parse(
                arrayOf(
                    "issue-cred",
                    walletName,
                    issuerDidAlias,
                    holderDid.uriLongForm,
                    credentialAlias
                )
            )
        }
        // verify credential
        assertDoesNotThrow("verify credential") {
            newParser().parse(
                arrayOf(
                    "verify-cred",
                    walletName,
                    "issued",
                    credentialAlias
                )
            )
        }
    }

    @Test
    fun exportCredTest() {
        val walletName = "wallet"
        val issuerDidAlias = "issuer_did"
        val holderDidAlias = "holder_did"
        val credentialAlias = "credential"
        val fileName = getFilePath("credential.json")
        // Wallet and dids
        assertDoesNotThrow("New wallet") {
            newParser().parse(arrayOf("new-wallet", walletName))
        }
        assertDoesNotThrow("New holder DID") {
            newParser().parse(arrayOf("new-did", walletName, holderDidAlias))
        }
        assertDoesNotThrow("New issuer DID") {
            newParser().parse(arrayOf("new-did", walletName, issuerDidAlias, "-i"))
        }
        assertDoesNotThrow("Publish issuer DID") {
            newParser().parse(arrayOf("publish-did", walletName, issuerDidAlias))
        }
        // Issue credential
        val wallet = findWallet(db, walletName)
        val holderDid = wallet.dids.filter { it.alias == holderDidAlias }[0]
        assertDoesNotThrow("Issue credential") {
            newParser().parse(
                arrayOf(
                    "issue-cred",
                    walletName,
                    issuerDidAlias,
                    holderDid.uriLongForm,
                    credentialAlias
                )
            )
        }
        // Export credential
        assertDoesNotThrow("Export credential") {
            newParser().parse(
                arrayOf(
                    "export-cred",
                    walletName,
                    credentialAlias,
                    fileName
                )
            )
        }
        // Check credential file created
        assertEquals(true, File(fileName).exists(), "Credential json file created")
        File(fileName).delete()
    }

    @Test
    fun importCredTest() {
        val walletName = "wallet"
        val issuerDidAlias = "issuer_did"
        val holderDidAlias = "holder_did"
        val credentialAlias = "credential"
        val fileName = getFilePath("credential.json")
        // Wallet and dids
        assertDoesNotThrow("New wallet") {
            newParser().parse(arrayOf("new-wallet", walletName))
        }
        assertDoesNotThrow("New holder DID") {
            newParser().parse(arrayOf("new-did", walletName, holderDidAlias))
        }
        assertDoesNotThrow("New issuer DID") {
            newParser().parse(arrayOf("new-did", walletName, issuerDidAlias, "-i"))
        }
        assertDoesNotThrow("Publish issuer DID") {
            newParser().parse(arrayOf("publish-did", walletName, issuerDidAlias))
        }
        // Issue credential
        val wallet = findWallet(db, walletName)
        val holderDid = wallet.dids.filter { it.alias == holderDidAlias }[0]
        assertDoesNotThrow("Issue credential") {
            newParser().parse(
                arrayOf(
                    "issue-cred",
                    walletName,
                    issuerDidAlias,
                    holderDid.uriLongForm,
                    credentialAlias
                )
            )
        }
        // Export credential
        assertDoesNotThrow("Export credential") {
            newParser().parse(
                arrayOf(
                    "export-cred",
                    walletName,
                    credentialAlias,
                    fileName
                )
            )
        }
        // Import credential
        assertDoesNotThrow("Import credential") {
            newParser().parse(
                arrayOf(
                    "import-cred",
                    walletName,
                    credentialAlias,
                    fileName
                )
            )
        }
    }

    @Test
    fun revokeCredTest() {
        val walletName = "wallet"
        val issuerDidAlias = "issuer_did"
        val holderDidAlias = "holder_did"
        val credentialAlias = "credential"
        // Wallet and dids
        assertDoesNotThrow("New wallet") {
            newParser().parse(arrayOf("new-wallet", walletName))
        }
        assertDoesNotThrow("New holder DID") {
            newParser().parse(arrayOf("new-did", walletName, holderDidAlias))
        }
        assertDoesNotThrow("New issuer DID") {
            newParser().parse(arrayOf("new-did", walletName, issuerDidAlias, "-i"))
        }
        assertDoesNotThrow("Publish issuer DID") {
            newParser().parse(arrayOf("publish-did", walletName, issuerDidAlias))
        }
        // Issue credential
        val wallet = findWallet(db, walletName)
        val holderDid = wallet.dids.filter { it.alias == holderDidAlias }[0]
        assertDoesNotThrow("Issue credential") {
            newParser().parse(
                arrayOf(
                    "issue-cred",
                    walletName,
                    issuerDidAlias,
                    holderDid.uriLongForm,
                    credentialAlias
                )
            )
        }
        // verify credential
        assertDoesNotThrow("Revoke credential") {
            newParser().parse(
                arrayOf(
                    "revoke-cred",
                    walletName,
                    credentialAlias
                )
            )
        }
        // verify credential
        assertDoesNotThrow("verify credential") {
            newParser().parse(
                arrayOf(
                    "verify-cred",
                    walletName,
                    "issued",
                    credentialAlias
                )
            )
        }
    }

    @Test
    fun addKeyTest() {
        val walletName = "wallet"
        val issuerDidAlias = "issuer_did"
        val keyId = "master1"
        val keyPurpose = "master"
        // Wallet and dids
        assertDoesNotThrow("addKeyTest - New wallet") {
            newParser().parse(arrayOf("new-wallet", walletName))
        }
        assertDoesNotThrow("addKeyTest - New issuer DID") {
            newParser().parse(arrayOf("new-did", walletName, issuerDidAlias, "-i"))
        }
        assertDoesNotThrow("addKeyTest - Publish issuer DID") {
            newParser().parse(arrayOf("publish-did", walletName, issuerDidAlias))
        }
        // New Key
        assertDoesNotThrow("addKeyTest - New key") {
            newParser().parse(
                arrayOf(
                    "add-key",
                    walletName,
                    issuerDidAlias,
                    keyId,
                    keyPurpose
                )
            )
        }
    }

    @Test
    fun revokeKeyTest() {
        val walletName = "wallet"
        val issuerDidAlias = "issuer_did"
        val keyId = "master0"
        // Wallet and dids
        assertDoesNotThrow("New wallet") {
            newParser().parse(arrayOf("new-wallet", walletName))
        }
        assertDoesNotThrow("New issuer DID") {
            newParser().parse(arrayOf("new-did", walletName, issuerDidAlias, "-i"))
        }
        assertDoesNotThrow("Publish issuer DID") {
            newParser().parse(arrayOf("publish-did", walletName, issuerDidAlias))
        }
        // Revoke master Key
        assertDoesNotThrow("Revoke key") {
            newParser().parse(
                arrayOf(
                    "revoke-key",
                    walletName,
                    issuerDidAlias,
                    keyId
                )
            )
        }
    }
}
