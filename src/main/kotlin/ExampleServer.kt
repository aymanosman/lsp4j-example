import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.*
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

fun main() {
    val server = ExampleServer()
    val launcher = LSPLauncher.createServerLauncher(server, System.`in`, System.out)
    server.connect(launcher.remoteProxy)
    launcher.startListening().get()
}

class ExampleServer : LanguageServer, LanguageClientAware {
    private var errorCode: Int = 1
    private val textDocumentService = ExampleTextDocumentService()
    private val workspaceService = ExampleWorkspaceService()

    // https://microsoft.github.io/language-server-protocol/specification#initialize
    override fun initialize(params: InitializeParams?): CompletableFuture<InitializeResult>? {
        val capabilities = ServerCapabilities()

        capabilities.textDocumentSync = Either.forLeft(TextDocumentSyncKind.Full)
        capabilities.completionProvider = CompletionOptions()

        return CompletableFuture.completedFuture(InitializeResult(capabilities))
    }

    // https://microsoft.github.io/language-server-protocol/specification#shutdown
    override fun shutdown(): CompletableFuture<Any>? {
        errorCode = 0
        return null
    }

    // https://microsoft.github.io/language-server-protocol/specification#exit
    override fun exit() {
        exitProcess(errorCode)
    }

    override fun getTextDocumentService(): TextDocumentService? {
        return textDocumentService
    }

    override fun getWorkspaceService(): WorkspaceService? {
        return workspaceService
    }

    override fun connect(client: LanguageClient) {
        textDocumentService.client = client
    }
}

class ExampleTextDocumentService : TextDocumentService {
    var client: LanguageClient? = null

    override fun completion(position: CompletionParams?): CompletableFuture<Either<MutableList<CompletionItem>, CompletionList>> {
        val item1 = CompletionItem("Racket")
        item1.data = 1
        item1.detail = "Racket details"
        item1.documentation = Either.forLeft("Racket documentation")

        val item2 = CompletionItem("snippetExample")
        item2.data = 3
        item2.detail = "snippetExample details"
        item2.insertText = "snippetExample(){\n  print(\"hello lsp!\")\n}"

        return CompletableFuture.completedFuture(Either.forRight(CompletionList(listOf(item1, item2))))
    }

    override fun didOpen(params: DidOpenTextDocumentParams?) {
    }

    override fun didSave(params: DidSaveTextDocumentParams?) {
    }

    override fun didClose(params: DidCloseTextDocumentParams?) {
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        client?.let { warnAllCaps(it, params) }
    }
}

// https://code.visualstudio.com/api/language-extensions/language-server-extension-guide#adding-a-simple-validation
fun warnAllCaps(client: LanguageClient, params: DidChangeTextDocumentParams) {
    val pattern: Regex = """\b[A-Z]{2,}\b""".toRegex()

    val diagnostics = mutableListOf<Diagnostic>()

    for ((index, line) in params.contentChanges[0].text.lines().withIndex()) {
        for (match in pattern.findAll(line)) {
            val d = Diagnostic()
            d.severity = DiagnosticSeverity.Warning
            val start = Position(index, match.range.first)
            val end = Position(index, match.range.last + 1)
            d.range = Range(start, end)
            d.message = "${match.value} is all uppercase."
            d.source = "ex"
            diagnostics.add(d)
        }
    }

    client.publishDiagnostics(PublishDiagnosticsParams(params.textDocument.uri, diagnostics))
}

class ExampleWorkspaceService : WorkspaceService {
    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams?) {
    }

    override fun didChangeConfiguration(params: DidChangeConfigurationParams?) {
    }
}
