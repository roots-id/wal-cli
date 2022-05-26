@file:OptIn(ExperimentalCli::class)

import com.github.ajalt.mordant.rendering.TextColors
import kotlinx.cli.*

fun main(args: Array<String>) {
    var command: String = ""
    try {
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
            // TODO: ShowCred(),
            // TODO: ShowDecodedCred()
            // TODO: ListCred(),
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
