@file:OptIn(ExperimentalCli::class)

import kotlinx.cli.*

fun main(args: Array<String>) {
    val parser = ArgParser("wal")
    val newWallet = NewWallet()
    val listWallets = ListWallets()
    val showMnemonic = ShowMnemonic()
    val exportWallet = ExportWallet()
    val importWallet = ImportWallet()

    val newDID = NewDID()
    val listDID = ListDID()
    val showDID = ShowDID()
    val publishDID = PublishDID()
    val issueCred = IssueCred()
    val verifyCred = VerifyCred()
    val exportCred = ExportCred()
    val importCred = ImportCred()

    val revokeCred = RevokeCred()
    val issueBatch = IssueBatch()
    val revokeBatch = RevokeBatch()
    val addKey = AddKey()
    val revokeKey = RevokeKey()
    val rotateKey = RotateKey()

    parser.subcommands(
        newWallet,
        listWallets,
        showMnemonic,
        exportWallet,
        importWallet,
        newDID,
        listDID,
        showDID,
        publishDID,
        issueCred,
        verifyCred,
        revokeCred,
        exportCred,
        importCred,
        issueBatch,
        revokeBatch,
        addKey,
        revokeKey,
        rotateKey
    )
    parser.parse(args)

//    println( "Message: ${scanQR(10)}")

//    showQRImage("did:prism:8c63b976dcd827fd9f2029bd5e9364331700e57932c47b0af3dc0301daee7f6b:Cj8KPRI7CgdtYXN0ZXIwEAFKLgoJc2VjcDI1NmsxEiECXDgJGePR5CqeYccccA6rOZEGTa6Kjuj6abN44rHbIqQ")

//    val t = Terminal()
//    t.println(table {
//        header { row("x", "x") }
//        body { row("x", "x") }
//    })
}
