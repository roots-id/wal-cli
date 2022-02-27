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
        RevokeKey(),
        PeerDIDCreatorCommand(),
        ResolvePeerDIDCommand(),
        PackCommand(),
        UnpackCommand()
    )
    parser.parse(args)
}

// TODO: add resolve prism did
// TODO: add action log to db
// TODO: add transactions to model
// TODO: check how to use seed instead of mnemonic (DLT)
// TODO: Modify secret resolver to use DB
// TODO: Update nodeAuthApi.getOperationStatus (DLT)
