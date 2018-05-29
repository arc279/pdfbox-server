import groovy.util.CliBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sun.net.httpserver.HttpServer;
import java.util.concurrent.Executors;

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

HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
server.createContext("/ping", new PingHttpHandler())
server.createContext("/", new PdfBoxHttpHandler())
server.setExecutor(Executors.newCachedThreadPool())
server.start()

