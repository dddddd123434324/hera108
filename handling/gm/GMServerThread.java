package handling.gm;

import server.GeneralThreadPool;
import tools.SystemUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class GMServerThread extends Thread {

    protected ServerSocket _serverSocket;
    private static Logger _log = Logger.getLogger(GMServerThread.class
            .getName());

    @Override
    public void run() {

        System.out.println("GM Server Thread Started. Memory used : " + SystemUtils.getUsedMemoryMB() + "MB");

        while (true) {
            try {
                Socket socket = _serverSocket.accept();
                GMClient client = new GMClient(socket);
                GMServer.add(client);
                GeneralThreadPool.getInstance().execute(client);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
