import groovy.util.CliBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper

def PORT = 6001

def cli = new CliBuilder(usage: 'pdfbox http server')
cli.with {
    h longOpt: 'help', 'Show usage information'
    p longOpt: 'port', args: 1, argName: 'port', "listen port(default: ${PORT})"
}
def options = cli.parse(args)
if (!options) {
    return
}
if (options.h) {
    cli.usage()
    return
}

def logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)

if (options.p) {
    PORT = options.p as int
}
logger.info "start port: {}", PORT

def pdf2text(buf) {
    def doc = PDDocument.load(buf)
    try {
        def stripper = new PDFTextStripper()
        return stripper.getText(doc)
    } finally {
        doc.close()
    }
}

HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
server.createContext("/", new HttpHandler() {
    @Override
    public void handle(HttpExchange he) throws IOException {
        try {
            def ifs = he.getRequestBody()
            def outText = pdf2text(ifs)
            def bs = outText.getBytes("UTF-8");
            logger.info "post data size: {}", bs.length

            he.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8")
            he.sendResponseHeaders(200, bs.length)
            he.getResponseBody().write(bs)
        } catch(IOException e) {
            // NOTE: PDDocument で IOException になっても何故かここに入ってこない問題
            logger.error e
            he.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8")
            he.sendResponseHeaders(500, bs.length)
            he.getResponseBody().write(e)
        } finally {
            he.close()
        }
    }
})
server.start()

