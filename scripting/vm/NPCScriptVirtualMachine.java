/*
 * Copyright (C) 2013 Nemesis Maple Story Online Server Program

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting.vm;

import client.MapleClient;
import server.ServerProperties;
import tools.MaplePacketCreator;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import server.MapleItemInformationProvider;

/**
 * @author Eternal
 */
public class NPCScriptVirtualMachine implements Runnable {

    private final String funcName;
    private final Object gate = new Object();
    private final Invocable iv;
    private final MapleClient c;
    private int objectid;
    private int sayindex = 0;
    List<String> says = new ArrayList<>();
    private boolean stop = false;
    private int npcid;
    private int lastmsg = -1;
    private int selection = -1;
    private String text = "";
    private boolean end = false;
    protected boolean openLegacyNpc = false;

    public NPCScriptVirtualMachine(MapleClient c, Invocable iv, int npcid, int objectid, String funcName) {
        this.iv = iv;
        this.c = c;
        this.npcid = npcid;
        this.objectid = objectid;
        this.funcName = funcName;
    }

    @Override
    public void run() {
        try {
            iv.invokeFunction(funcName);
        } catch (ScriptException | NoSuchMethodException E) {
            E.printStackTrace(System.err);
        }
        end = true;
        flushSay();
        if (!openLegacyNpc) {
            c.getPlayer().setConversation(0);
            NPCScriptInvoker.dispose(c);
        }
    }

    public MapleClient getClient() {
        return c;
    }

    public void forceStop() {
        stop = true;
        selection = -1;
        text = "";
        says.clear();
        try {
            synchronized (gate) {
                gate.notify();
            }
        } catch (Exception e) {
        }
    }

    public boolean isStop() {
        return stop;
    }

    public void addSay(String say) {
        says.add(say);
    }

    public int getLastMsg() {
        return lastmsg;
    }

    public void processSay(int mode) {
        if (mode == 1) {
            sayindex++;
        } else if (mode == 0) {
            sayindex--;
        } else {
            stop = true;
        }
        try {
            synchronized (gate) {
                gate.notify();
            }
        } catch (Exception e) {
        }
    }

    public void flushSay() {
        while (sayindex < says.size() && !stop) {
            String endBytes = "00 00";
            if (sayindex != 0 && sayindex + 1 == says.size()) {
                if (end) {
                    endBytes = "01 00";
                } else {
                    endBytes = "01 01";
                }
            } else if (sayindex == 0 && says.size() >= 2) {
                endBytes = "00 01";
            } else if (sayindex > 0 && sayindex + 1 < says.size()) {
                endBytes = "01 01";
            } else if (!end) {
                endBytes = "00 01";
            }
            lastmsg = 0;
            c.sendPacket(MaplePacketCreator.getNPCTalk(npcid, (byte) 0, says.get(sayindex), endBytes, (byte) 0));
            try {
                synchronized (gate) {
                    gate.wait();
                }
            } catch (Exception e) {
            }
        }
        sayindex = 0;
        says.clear();
    }

    public void processAnswer(int answer) {
        this.selection = answer;
        try {
            synchronized (gate) {
                gate.notify();
            }
        } catch (Exception e) {
        }
    }

    public void processText(String answer) {
        text = answer;
        try {
            synchronized (gate) {
                gate.notify();
            }
        } catch (Exception e) {
        }
    }

    public int askYesNo(String str) {
        flushSay();
        if (stop) {
            return -1;
        }
        lastmsg = 1;
        c.sendPacket(MaplePacketCreator.getNPCTalk(npcid, (byte) 2, str, "", (byte) 0));
        try {
            synchronized (gate) {
                gate.wait();
            }
        } catch (Exception e) {
        }
        return selection;
    }

    public int askAccept(String str) {
        flushSay();
        if (stop) {
            return -1;
        }
        lastmsg = 11;
        c.sendPacket(MaplePacketCreator.getNPCTalk(npcid, (byte) 11, str, "", (byte) 0));
        try {
            synchronized (gate) {
                gate.wait();
            }
        } catch (Exception e) {
        }
        return selection;
    }

    public int askAcceptNoESC(String str) {
        flushSay();
        if (stop) {
            return -1;
        }
        lastmsg = 12;
        c.sendPacket(MaplePacketCreator.getNPCTalk(npcid, (byte) 12, str, "", (byte) 1));
        try {
            synchronized (gate) {
                gate.wait();
            }
        } catch (Exception e) {
        }
        return selection;
    }

    public int askMenu(String str) {
        flushSay();
        if (stop) {
            return -1;
        }
        lastmsg = 5;
        c.sendPacket(MaplePacketCreator.getNPCTalk(npcid, (byte) 5, str, "", (byte) 0));
        try {
            synchronized (gate) {
                gate.wait();
            }
        } catch (Exception e) {
        }
        return selection;
    }

    public int askMapSelection(String str) {
        flushSay();
        if (stop) {
            return -1;
        }
        lastmsg = 14;
        c.sendPacket(MaplePacketCreator.getMapSelection(npcid, str));
        try {
            synchronized (gate) {
                gate.wait();
            }
        } catch (Exception e) {
        }
        return selection;
    }

    public boolean itemExists(final int itemId) {
        return MapleItemInformationProvider.getInstance().itemExists(itemId);
    }

    public String askText(String str) {
        flushSay();
        if (stop) {
            return "";
        }
        lastmsg = 2;
        c.sendPacket(MaplePacketCreator.getNPCTalkText(npcid, str));
        try {
            synchronized (gate) {
                gate.wait();
            }
        } catch (Exception e) {
        }
        return text;
    }

    public int askNumber(String text, int def, int min, int max) {
        flushSay();
        if (stop) {
            return 0;
        }
        lastmsg = 3;
        c.sendPacket(MaplePacketCreator.getNPCTalkNum(npcid, text, def, min, max));
        try {
            synchronized (gate) {
                gate.wait();
            }
        } catch (Exception e) {
        }
        return Math.max(min, Math.min(selection, max));
    }

    public int askAvatar(String text, int... args) {
        flushSay();
        if (stop) {
            return -1;
        }
        lastmsg = 5;
        List<Integer> avatars = new ArrayList<Integer>();
        for (int i : args) {
            if (hasPath(i)) {
                avatars.add(i);
            }
        }
        int[] avat = new int[avatars.size()];
        if (avat.length == 0 || avat.length >= 256) {
            return -2;
        }
        for (int i = 0; i < avatars.size(); ++i) {
            avat[i] = avatars.get(i);
        }
        int[] packetStyles = avat;
        int selectionShift = 0;
        if (needsFace50000PreviewFix(avat) && avat.length < 255) {
            packetStyles = new int[avat.length + 1];
            packetStyles[0] = 20000; // Force face-style preview mode for 50000+ face IDs.
            System.arraycopy(avat, 0, packetStyles, 1, avat.length);
            selectionShift = 1;
        }
        c.getSession().write(MaplePacketCreator.getNPCTalkStyle(npcid, text, packetStyles));
        try {
            synchronized (gate) {
                gate.wait();
            }
        } catch (Exception e) {
        }
        try {
            int selectedIndex = selection;
            if (selectionShift > 0 && selectedIndex >= 0) {
                selectedIndex = Math.max(0, selectedIndex - selectionShift);
            }
            return avat[selectedIndex];
        } catch (ArrayIndexOutOfBoundsException aiobe) {
            return -3;
        }
    }

    private static boolean isFaceAvatar(int avatar) {
        return (avatar >= 20000 && avatar < 30000) || (avatar >= 50000 && avatar < 60000);
    }

    private static boolean needsFace50000PreviewFix(int[] styles) {
        return styles != null
                && styles.length > 0
                && styles[0] >= 50000
                && styles[0] < 60000
                && isFaceAvatar(styles[0]);
    }

    public boolean hasPath(int avatar) {
        String path = ServerProperties.WZ_PATH + "/Character.wz/";
        if (isFaceAvatar(avatar)) {
            path += "Face/";
        } else if (avatar >= 30000 && avatar < 50000 || avatar >= 60000) {
            path += "Hair/";
        } else if (avatar < 100) {
            return true;
        }
        path += "000" + avatar + ".img.xml";
        File f = new File(path);
        return f.exists();
    }
}
