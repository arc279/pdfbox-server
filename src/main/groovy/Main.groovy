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


class PdfBoxHttpHandler implements HttpHandler {
    public static String pdf2text(ifs) throws IOException {
        def doc = PDDocument.load(ifs)
        try {
            def stripper = new PDFTextStripper()
            return stripper.getText(doc)
        } finally {
            doc.close()
        }
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        def logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        try {
            def ifs = he.getRequestBody()
            def outText = pdf2text(ifs)
            def bs = outText.getBytes("UTF-8");
            logger.info "post data size: {}", bs.length

            he.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8")
            he.sendResponseHeaders(200, bs.length)
            he.getResponseBody().write(bs)
        } catch(IOException e) {
            def outText = e.toString()
            def bs = outText.getBytes("UTF-8");
            logger.error "{}", outText

            he.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8")
            he.sendResponseHeaders(400, bs.length)
            he.getResponseBody().write(bs)
        } finally {
            he.close()
        }
    }
}

//-------
// main
def logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)

if (options.p) {
    PORT = options.p as int
}
logger.info "start port: {}", PORT

HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
server.createContext("/", new PdfBoxHttpHandler())
server.start()

