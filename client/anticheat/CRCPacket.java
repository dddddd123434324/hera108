/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.anticheat;

import client.MapleClient;
import handling.SendPacketOpcode;
import server.Randomizer;
import tools.FileoutputUtil;
import tools.data.MaplePacketLittleEndianWriter;
/**
 *
 * @author Celino
 */
public class CRCPacket {
    public static final int LOG_TYPE_ILLEGAL = 0;
    public static final int LOG_TYPE_TIMEOUT = 1;
    public static final int LOG_TYPE_UNKNOWN = 2; 
    public static final int LOG_TYPE_WZ = 3;
    private static final int CURRENT_CRC = -1127058209;
    private static final int CURRENT_WZ = -1248747720;
    public static final int SECRET_KEY = 0xDEADC0DE;
    public static final long REQUEST_PERIOD = 1 * 60 * 1000L;
    public static final long TIME_OUT = 1 * 30 * 1000L;

    public static int makeXorKey() {
        int ret;
        do {
            ret = Randomizer.nextInt();
        } while (ret == 0); //ret이 0일 수는 없다.
        return ret;
    }

    public static boolean checkCRC(int clientCRC) {
        return clientCRC == CURRENT_CRC;
    }
    
    public static boolean checkWZ(int skillHash) {
        return skillHash == CURRENT_WZ;
    }

    public static final byte[] makeRequestPacket(int key) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.SEND_CRC_REQUEST.getValue());
        mplew.writeInt(key ^ SECRET_KEY);
        return mplew.getPacket();
    }

    public static void log(MapleClient c, int logType) {
        switch(logType)
        {
            case 0:
                FileoutputUtil.log(FileoutputUtil.AntiCheat_Log, "["+FileoutputUtil.getDCurrentTime() + "] 캐릭터 아디 : " + c.getPlayer().getId() + ", 캐릭터 이름 : " + c.getPlayer().getName() + ", 분류 : " + "메모리 변조\r\n");
                break;
            case 1:
                FileoutputUtil.log(FileoutputUtil.AntiCheat_Log, "["+FileoutputUtil.getDCurrentTime() + "] 캐릭터 아디 : " + c.getPlayer().getId() + ", 캐릭터 이름 : " + c.getPlayer().getName() + ", 분류 : " + "하트비트\r\n");
                break;
            case 2:
                FileoutputUtil.log(FileoutputUtil.AntiCheat_Log, "["+FileoutputUtil.getDCurrentTime() + "] 캐릭터 아디 : " + c.getPlayer().getId() + ", 캐릭터 이름 : " + c.getPlayer().getName() + ", 분류 : " + "다른로컬 접속\r\n");
                break;
            case 3:
                FileoutputUtil.log(FileoutputUtil.AntiCheat_Log, "["+FileoutputUtil.getDCurrentTime() + "] 캐릭터 아디 : " + c.getPlayer().getId() + ", 캐릭터 이름 : " + c.getPlayer().getName() + ", 분류 : " + "WZ 변조\r\n");
                break;
        }
    }
}
