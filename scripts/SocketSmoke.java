import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.regex.Pattern;

// Local-only test of the authenticated STOMP handshake and destination guards.
class SocketSmoke {
    static final String BASE="http://127.0.0.1:8080";
    static final CookieManager COOKIES=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
    static final HttpClient CLIENT=HttpClient.newBuilder().cookieHandler(COOKIES).connectTimeout(Duration.ofSeconds(10)).build();
    static String csrf;
    static class Frames implements WebSocket.Listener {
        final BlockingQueue<String> received=new LinkedBlockingQueue<>();
        final StringBuilder buffer=new StringBuilder();
        public CompletionStage<?> onText(WebSocket ws,CharSequence text,boolean last) {
            buffer.append(text);
            if (last) { received.add(buffer.toString()); buffer.setLength(0); }
            ws.request(1); return null;
        }
        String next() throws Exception { String frame=received.poll(10,TimeUnit.SECONDS); if(frame==null) throw new AssertionError("STOMP response timed out"); return frame; }
    }
    static void post(String path,String body) throws Exception {
        var response=CLIENT.send(HttpRequest.newBuilder(URI.create(BASE+"/api/v1/auth/"+path)).timeout(Duration.ofSeconds(15)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf).POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.discarding());
        if(response.statusCode()/100!=2) throw new AssertionError(path+" failed: "+response.statusCode());
    }
    static WebSocket connect(Frames frames) {
        return CLIENT.newWebSocketBuilder().header("Origin","http://localhost:5173").subprotocols("v12.stomp").connectTimeout(Duration.ofSeconds(10)).buildAsync(URI.create("ws://127.0.0.1:8080/api/v1/ws"),frames).join();
    }
    public static void main(String[] args) throws Exception {
        var response=CLIENT.send(HttpRequest.newBuilder(URI.create(BASE+"/api/v1/auth/csrf")).GET().build(),HttpResponse.BodyHandlers.ofString());
        var match=Pattern.compile("\"token\":\"([^\"]+)\"").matcher(response.body());
        if(!match.find()) throw new AssertionError("CSRF token missing");
        csrf=match.group(1);
        post("login","{\"email\":\"tester@pashe.local\",\"password\":\"Pashe-Local-Test-2026!\"}");
        try {
            Frames valid=new Frames(); WebSocket ws=connect(valid);
            ws.sendText("CONNECT\naccept-version:1.2\nhost:localhost\nX-CSRF-TOKEN:"+csrf+"\n\n\0",true).join();
            if(!valid.next().startsWith("CONNECTED")) throw new AssertionError("Authenticated CONNECT failed");
            ws.sendText("SUBSCRIBE\nid:1\ndestination:/topic/private\n\n\0",true).join();
            if(!valid.next().startsWith("ERROR")) throw new AssertionError("Forbidden subscription accepted");
            ws.abort();
            Frames missing=new Frames(); WebSocket denied=connect(missing);
            denied.sendText("CONNECT\naccept-version:1.2\nhost:localhost\n\n\0",true).join();
            if(!missing.next().startsWith("ERROR")) throw new AssertionError("Missing CSRF accepted");
            denied.abort();
            System.out.println("PASS: authenticated STOMP CONNECT, denied destination, required CONNECT CSRF.");
        } finally { post("logout","{}"); }
    }
}
