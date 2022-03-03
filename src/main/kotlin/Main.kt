@file:OptIn(ExperimentalCli::class)

import com.github.ajalt.mordant.rendering.TextColors
import kotlinx.cli.*

fun main(args: Array<String>) {
    var command: String = ""
    try {
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
            ResolvePrismDid(),
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
        command = parser.parse(args).commandName
    } catch (e: Exception) {
        println(TextColors.red("-- $command error --"))
        e.printStackTrace()
    }
}

// TODO: add action log to db
// TODO: add transactions to model
// TODO: check how to use seed instead of mnemonic (DLT)
// TODO: Modify secret resolver to use DB
