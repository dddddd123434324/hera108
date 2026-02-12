/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.log;

import constants.ServerConstants;
import handling.gm.GMPacket;
import handling.gm.GMServer;
import tools.FileoutputUtil;

public class ServerLogger {

    private static final ServerLogger instance = new ServerLogger();

    public static ServerLogger getInstance() {
        return instance;
    }

    public void logChat(LogType.Chat type, int cid, String charname, String message, String etc) {
        GMServer.broadcast(GMPacket.chatLog(type.i, "[" + type.name() + "] " + charname + " : " + message + "(" + etc + ")"));
        if (!ServerConstants.logChat) {
            return;
        }
        FileoutputUtil.dbLog("chat/" + type.name(), "캐릭터ID : " + cid + " / 닉네임 : " + charname + " / 내용 : " + message + " / " + etc);
    }

    public void logItem(LogType.Item type, int cid, String name, int itemid, int quantity, String itemname, int meso, String etc) {
        if (!ServerConstants.logItem) {
            return;
        }
        FileoutputUtil.dbLog("item/" + type.name(), "캐릭터ID : " + cid + " / 닉네임 : " + name + " / 아이템이름 : " + itemname + " / 아이템ID : " + itemid + " / 개수 : " + quantity + " / 가격 : " + meso + " / " + etc);
    }

    public void logTrade(LogType.Trade type, int cid, String name, String partnername, String item, String etc) {
        if (!ServerConstants.logTrade) {
            return;
        }
        if (name.equals(partnername)) {
            return;
        }
        FileoutputUtil.dbLog("item/" + type.name(), "캐릭터ID : " + cid + " / 닉네임 : " + name + " / 상대방 : " + partnername + " / 아이템 : " + item + " / " + etc);
    }

    public void logCoupon(LogType.Trade type, int cid, String name, String couponCode, String couponType, String item, String etc) {
        FileoutputUtil.dbLog("item/" + type.name(), "캐릭터ID : " + cid + " / 닉네임 : " + name + " / 쿠폰번호 : " + couponCode + " / 타입 : " + couponType + item + etc);
    }

    public void logDailyQuestBonus(LogType.Item type, int cid, String name, boolean isSpecial, boolean isObtained) {
        FileoutputUtil.dbLog("item/" + type.name(), "캐릭터ID : " + cid + " / 닉네임 : " + name + " / 수령한아이템 : " + (isSpecial ? "캔디" : "펩시콜라") + " / 아이템수령여부 : " + isObtained);
    }

    public void hackLog(String text) {
        FileoutputUtil.log("log/핵로그.txt", text);
    }

    public static void logMacro(LogType.Hack type, String name, String name2) {
        FileoutputUtil.dbLog("hack/" + type.name(), "핵 ID : " + name + " / 적발자 : " + name2);
    }

    public static void logStorage(LogType.Item type, String text, int acc) {
        FileoutputUtil.dbLog("item/" + type.name(), text);
    }

    public static void logParty(LogType.Etc type, String text, int acc) {
        FileoutputUtil.dbLog("Party/" + type.name(), text);
    }

    public static void logLevelUp(LogType.Etc type, String text, int acc) {
        FileoutputUtil.dbLog("LevelUp/" + type.name(), text);
    }

    public void copyLog(String text) {
        FileoutputUtil.log("SeverLog/복사로그/" + FileoutputUtil.CurrentReadable_Date() + ".txt", text);
    }

    public void getGMLog(String text) {
        FileoutputUtil.log("SeverLog/지엠획득로그/" + FileoutputUtil.CurrentReadable_Date() + ".txt", text);
    }
}
