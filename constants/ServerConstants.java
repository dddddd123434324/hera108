/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package constants;

import client.MapleClient;

import java.util.LinkedList;
import java.util.List;

public class ServerConstants {

    public static double cubeDayValue = 1.0;
    public static MapleClient cli = null;

    public static byte[] Gateway_IP = {(byte) 127, (byte) 0, (byte) 0, (byte) 1};

    public static boolean SHOW_RECV = false;
    public static boolean SHOW_SEND = false;
    public static boolean Use_Localhost = false;//Boolean.parseBoolean(ServerProperties.getProperty("net.sf.odinms.world.admin")); // true = packets are logged, false = others can connect to server

    public static boolean logChat = true;
    public static boolean logTrade = true;
    public static boolean logItem = true;

    public static final short MAPLE_VERSION = (short) 111;
    public static final byte MAPLE_CHECK = 1;
    public static final byte MAPLE_PATCH = 1;
    public static boolean Use_Fixed_IV = false; // true = disable sniffing, false = server can connect to itself

    public static boolean ExprateByLevel = true;
    public static int CashRate = 10;

    public static boolean SHOP_DISCOUNT = false;
    public static final float SHOP_DISCOUNT_PERCENT = 20f; // float = round up.

    public static boolean dailyQuestBonus = false;
    public static boolean Event_Bonus = false;
    public static boolean specialDailyQuestBonus = true;

    public static final List<String> localhostIP = new LinkedList<String>();

    public static enum PlayerGMRank {

        NORMAL('@', 0),
        DONATOR('#', 1),
        SUPERDONATOR('$', 2),
        INTERN('%', 3),
        GM('!', 4),
        SUPERGM('!', 5),
        ADMIN('!', 6);
        private char commandPrefix;
        private int level;

        PlayerGMRank(char ch, int level) {
            commandPrefix = ch;
            this.level = level;
        }

        public char getCommandPrefix() {
            return commandPrefix;
        }

        public int getLevel() {
            return level;
        }
    }

    public static enum CommandType {

        NORMAL(0),
        TRADE(1);
        private int level;

        CommandType(int level) {
            this.level = level;
        }

        public int getType() {
            return level;
        }
    }

    public static boolean isIPLocalhost(final String sessionIP) {
        return localhostIP.contains(sessionIP.replace("/", "")) && ServerConstants.Use_Localhost;
    }

    static {
        localhostIP.add("127.0.0.1");
    }

    public static ServerConstants instance;
}
