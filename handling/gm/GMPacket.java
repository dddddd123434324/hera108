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

import handling.world.World;
import server.RateManager;
import tools.data.MaplePacketLittleEndianWriter;

/**
 * @author Lunatic
 */
public class GMPacket {

    public static byte[] ping() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(1);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] initialInformation() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(1);
        mplew.writeInt(RateManager.EXP); //경험치
        mplew.writeInt(RateManager.DROP); //드롭
        mplew.writeInt(RateManager.MESO); //메소
        mplew.writeInt(World.Find.size()); //게임 접속자
        mplew.writeInt(GMServer.size()); //GMC 접속자
        return mplew.getPacket();
    }

    public static byte[] chatLog(int type, String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(2);
        mplew.write(type);
        mplew.writeMapleAsciiString(msg);
        return mplew.getPacket();
    }

    public static byte[] updateRate(int type, int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(3);
        mplew.write(type);
        mplew.writeInt(value);
        return mplew.getPacket();
    }

    public static byte[] playerActionResult(int err) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(4);
        mplew.write(err);
        return mplew.getPacket();
    }

    public static byte[] appendLog(String head, String ctx) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(5);
        mplew.writeMapleAsciiString(head);
        mplew.writeMapleAsciiString(ctx);
        return mplew.getPacket();
    }

    public static byte[] updateConnectionSize(int total, int gm) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(6);
        mplew.writeInt(total);
        mplew.writeInt(gm);
        return mplew.getPacket();
    }

    public static byte[] callback_existChar(boolean b) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(101);
        mplew.write(1);
        mplew.write(b ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] callback_existItem(boolean b) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(101);
        mplew.write(2);
        mplew.write(b ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] create_result(boolean b) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(101);
        mplew.write(3);
        mplew.write(b ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] updateSchedules() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(102);
        RateManager.writeSchedules(mplew);
        return mplew.getPacket();
    }
}
