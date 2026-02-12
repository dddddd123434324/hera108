package handling.auth;

import tools.SystemUtils;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static handling.auth.SimpleCrypt.simple_crypt;

public class AuthServerThread extends Thread {

    protected ServerSocket _serverSocket;

    @Override
    public void run() {
        System.out.println("Auth Server Thread Started. Memory used : " + SystemUtils.getUsedMemoryMB() + "MB");
        while (true) {
            try {
                Socket socket = _serverSocket.accept();
                socket.setSoTimeout(10000);
                InputStream input = socket.getInputStream();
                OutputStream output = socket.getOutputStream();
//                int _0 = input.read() & 0xFF;
//                int _1 = input.read() & 0xFF;
//                int _2 = input.read() & 0xFF;
//                int _3 = input.read() & 0xFF;
//                int length = _0 | (_1 << 8) | (_2 << 16) | (_3 << 24);
                int length = 1024;
                byte[] data = new byte[length];
                int readed = 0;
                for (int i = 0; i != -1 && readed < length; readed += i) {
                    i = input.read(data, readed, length - readed);
                }
                if (readed != length) {
                    throw new RuntimeException("Incomplete packet is recv-ed.");
                }
                simple_crypt(data);
                String ip = socket.getInetAddress().getHostAddress();
                LittleEndianAccessor lea = new LittleEndianAccessor(new ByteArrayByteStream(data));
                String mac = lea.readAsciiString(lea.readByte());
                AuthEntry entry = new AuthEntry();
                {
                    entry.ip = ip;
                    entry.mac = mac;
                }
                AuthServer.ENTRY.put(ip, entry);
                simple_crypt(data);
                output.write(data);
                //...
                if (input != null) {
                    input.close();
                }
                if (output != null) {
                    output.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
