import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler

class PingHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange he) throws IOException {
        try {
            def outText = "PONG"
            def bs = outText.getBytes("UTF-8")

            he.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8")
            he.sendResponseHeaders(200, bs.length)
            he.getResponseBody().write(bs)
        } finally {
            he.close()
        }
    }
}

