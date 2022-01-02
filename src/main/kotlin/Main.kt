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
    val addKey = AddKey()
    val revokeKey = RevokeKey()

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
        addKey,
        revokeKey
    )
    parser.parse(args)
}
