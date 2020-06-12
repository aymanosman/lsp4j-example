import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.*
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

fun main() {
    LSPLauncher
            .createServerLauncher(ExampleServer(), System.`in`, System.out)
            .startListening()
            .get()
}

class ExampleServer : LanguageServer, LanguageClientAware {
    private var errorCode: Int = 1
    private var client: LanguageClient? = null
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

    override fun connect(client: LanguageClient?) {
        this.client = client
    }
}

class ExampleTextDocumentService : TextDocumentService {
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

    override fun didChange(params: DidChangeTextDocumentParams?) {
    }
}

class ExampleWorkspaceService : WorkspaceService {
    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams?) {
    }

    override fun didChangeConfiguration(params: DidChangeConfigurationParams?) {
    }
}
