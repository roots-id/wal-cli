@file:OptIn(ExperimentalCli::class)

import kotlinx.cli.*

fun main(args: Array<String>) {
    val parser = ArgParser("wal")
    parser.subcommands(
        NewWallet(),
        ListWallets(),
        ShowMnemonic(),
        ExportWallet(),
        ImportWallet(),
        NewDID(),
        ListDID(),
        ShowDID(),
        PublishDID(),
        IssueCred(),
        VerifyCred(),
        ExportCred(),
        ImportCred(),
        RevokeCred(),
        AddKey(),
        RevokeKey()
    )
    parser.parse(args)
}
