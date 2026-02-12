/*
 * The MIT License
 *
 * Copyright 2017 Jŭbar.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package handling.gm;

import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static handling.gm.SimpleCrypt.simple_crypt;

/**
 * The class managed client instance for GMServer base on WS MapleClient
 *
 * @author Lunatic
 */
public class GMClient implements Runnable {

    private static final String AUTH_CODE = "34628b7ec4d9076657946783c63b53b36b4bb8ffeb41cc93749aed7a6e529699c2e6b9ab63b0952fe716d9ce594e7ac5f5973dfca06f894ab3a6fba9cbc7b579";

    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;
    private boolean stop = false;

    public GMClient(Socket socket) throws IOException {
        this.socket = socket;
        input = socket.getInputStream();
        output = new BufferedOutputStream(socket.getOutputStream());
    }

    public synchronized void sendPacket(final byte[] data) {
        if (data == null) {
            return;
        }
        simple_crypt(data);
        try {
            int length = data.length;
            byte[] header = {(byte) (length & 0xFF), (byte) ((length >> 8) & 0xFF), (byte) ((length >> 16) & 0xFF), (byte) ((length >> 24) & 0xFF)};
            output.write(header);
            output.write(data);
            output.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void stop() {
        stop = true;
    }

    public void clear() {
        GMServer.remove(this);
        try {
            input.close();
        } catch (Exception ex) {

        }
        try {
            output.close();
        } catch (Exception ex) {

        }
        try {
            socket.close();
        } catch (Exception ex) {

        }
    }

    private int read4() throws Exception {
        int _0 = input.read() & 0xFF;
        int _1 = input.read() & 0xFF;
        int _2 = input.read() & 0xFF;
        int _3 = input.read() & 0xFF;
        return _0 | (_1 << 8) | (_2 << 16) | (_3 << 24);
    }

    private byte[] readPacket() throws Exception {
        try {
            int length = read4();
            byte[] data = new byte[length];
            int readed = 0;
            for (int i = 0; i != -1 && readed < length; readed += i) {
                i = input.read(data, readed, length - readed);
            }
            if (readed != length) {
                throw new RuntimeException("Incomplete packet is recv-ed.");
            }
            simple_crypt(data);
            return data;
        } catch (IOException e) {
            throw e;
        }
    }

    @Override
    public void run() {
        try {
            sendPacket(GMPacket.ping());
            while (!stop) {
                byte[] data;
                try {
                    data = readPacket();
                } catch (Exception e) {
                    break;
                }
                LittleEndianAccessor lea = new LittleEndianAccessor(new ByteArrayByteStream(data));
                String str = lea.readMapleAsciiString();
                if (!str.equals(AUTH_CODE)) {
                    throw new RuntimeException("Invalid packet is recv-ed.");
                }
                GMHandler.onPacket(this, lea);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            clear();
        }
    }
}
