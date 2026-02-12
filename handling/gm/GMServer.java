/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.gm;

import handling.world.World;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import static handling.gm.SimpleCrypt.memcpy;

/**
 * @author 티썬
 */
public class GMServer {

    private static GMServerThread thread;
    private static final List<GMClient> gmclients = new ArrayList<>();

    public static void start() throws Exception {
        thread = new GMServerThread();
        thread._serverSocket = new ServerSocket(9699);
        System.out.println("Port 9699 Opened.");
        thread.start();
    }

    public static void add(GMClient c) {
        synchronized (gmclients) {
            gmclients.add(c);
        }
        broadcastConnection(c);
    }

    public static void remove(GMClient c) {
        synchronized (gmclients) {
            gmclients.remove(c);
        }
        broadcastConnection(c);
    }

    public static int size() {
        synchronized (gmclients) {
            return gmclients.size();
        }
    }

    public static void broadcast(byte[] data) {
        broadcast(data, null);
    }

    public static void broadcast(byte[] data, GMClient ex) {
        synchronized (gmclients) {
            for (GMClient cli : gmclients) {
                if (cli != ex) {
                    cli.sendPacket(memcpy(data));
                }
            }
        }
    }

    public static void broadcastConnection(GMClient ex) {
        GMServer.broadcast(GMPacket.updateConnectionSize(World.Find.size(), GMServer.size()), ex);
    }
}
