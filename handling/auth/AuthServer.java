package handling.auth;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

public class AuthServer {

    private static AuthServerThread thread;
    public static final Map<String, AuthEntry> ENTRY = new HashMap<>(); //로그인 서버에서 AuthEntry가 없는 아이피는 무시

    public static void start() throws Exception {
        thread = new AuthServerThread();
        thread._serverSocket = new ServerSocket(25634);
        System.out.println("Port 25634 Opened.");
        thread.start();
    }
}
