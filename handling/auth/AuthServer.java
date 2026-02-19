package handling.auth;

import server.ServerProperties;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

public class AuthServer {

    private static AuthServerThread thread;
    private static final int PORT = Integer.parseInt(ServerProperties.getProperty("authPort", "25634"));
    public static final Map<String, AuthEntry> ENTRY = new HashMap<>(); //로그인 서버에서 AuthEntry가 없는 아이피는 무시

    public static void start() throws Exception {
        thread = new AuthServerThread();
        thread._serverSocket = new ServerSocket(PORT);
        System.out.println("Port " + PORT + " Opened.");
        thread.start();
    }
}
