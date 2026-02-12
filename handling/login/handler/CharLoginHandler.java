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
package handling.login.handler;

import client.LoginCrypto;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.auth.AuthEntry;
import handling.auth.AuthServer;
import handling.channel.ChannelServer;
import handling.login.LoginHelper;
import handling.login.LoginInformationProvider;
import handling.login.LoginInformationProvider.JobType;
import static handling.login.LoginInformationProvider.JobType.Aran;
import static handling.login.LoginInformationProvider.JobType.Cygnus;
import static handling.login.LoginInformationProvider.JobType.Evan;
import static handling.login.LoginInformationProvider.JobType.Resistance;
import handling.login.LoginServer;
import handling.login.LoginWorker;
import handling.world.World;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.Timer.PingTimer;
import tools.MaplePacketCreator;
import tools.SystemUtils;
import tools.data.LittleEndianAccessor;
import tools.packet.LoginPacket;
import tools.packet.PacketHelper;

import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import server.quest.MapleQuest;

public class CharLoginHandler {

    private static final boolean loginFailCount(final MapleClient c) {
        c.loginAttempt++;
        if (c.loginAttempt > 5) {
            return true;
        }
        return false;
    }

    public static final void login(final LittleEndianAccessor slea, final MapleClient c) {
        String login = slea.readMapleAsciiString();
        String pwd = slea.readMapleAsciiString();
        if (login.getBytes(Charset.forName("MS949")).length > 64) {
            IpBan(c.getIp());
            return;
        }
        if (pwd.getBytes(Charset.forName("MS949")).length > 12) {
            IpBan(c.getIp());
            return;
        }
        if (login.contains("'") || login.contains("`") || login.contains("\"") || login.contains("=")) {
            c.sendPacket(LoginPacket.getLoginFailed(5));
            return;
        }
        int loginok = c.login(login, pwd);
        if (AutoRegister.CheckAccount(login) != false) { //가입여부 확인
            loginok = c.login(login, pwd);
        } else if (AutoRegister.AutoRegister != false && (!c.hasBannedIP() || !c.hasBannedMac())) { //자동가입 여부와 ip밴 체크
            AutoRegister.createAccount(login, pwd, c.getSessionIPAddress(), c);
            return;
        } else {
            c.clearInformation();
            c.getSession().write(LoginPacket.getLoginFailed(20));
            c.getSession().write(MaplePacketCreator.serverNotice(1, "회원가입이 불가능합니다."));
            return;
        }
        AuthEntry auth = AuthServer.ENTRY.get(c.getIp());
        if (loginok == 0 || loginok == 5) {
            if (auth != null) {
                c.updateMacs(auth.mac);
            }
        }
        auth = null; //더 이상 auth를 사용하지 마시오.
        final Calendar tempbannedTill = c.getTempBanCalendar();
        final boolean ipBan = c.hasBannedIP();
        final boolean macBan = c.hasBannedMac();
        if (loginok == 0 && (ipBan || macBan) && !c.isGm()) {
            loginok = 3;
            if (macBan) {
                // this is only an ipban o.O" - maybe we should refactor this a bit so it's more readable
                MapleCharacter.ban(c.getIp(), "Enforcing account ban, account " + login, false, 4, false, "[시스템]");
            }
        }
        if (loginok == 5 && (ipBan || macBan)) {
            loginok = 3; //MAC 우회하고 신규 계정 시도
        }
        if (loginok != 0) {
            if (!loginFailCount(c)) {
                c.clearInformation();
                c.getSession().write(LoginPacket.getLoginFailed(loginok));
            } else {
                c.getSession().close();
            }
        } else if (tempbannedTill.getTimeInMillis() != 0) {
            if (!loginFailCount(c)) {
                c.clearInformation();
                c.getSession().write(LoginPacket.getTempBan(PacketHelper.getTime(tempbannedTill.getTimeInMillis()), c.getBanReason()));
            } else {
                c.getSession().close();
            }
        } else {
            c.loginAttempt = 0;
            LoginWorker.registerClient(c);
            AuthServer.ENTRY.remove(c.getIp());
        }
    }

    public static int[] rcdServerList = {1, 6, 9, 12, 15, 20, 25, 29};

    public static final void ServerListRequest(final MapleClient c) {
        try {
            c.getSession().write(LoginPacket.getServerList(22, LoginServer.getLoad()));//하셀로
            c.getSession().write(LoginPacket.getServerList(17, LoginServer.getLoad()));//카디아
            c.getSession().write(LoginPacket.getServerList(16, LoginServer.getLoad()));//쥬디스
            c.getSession().write(LoginPacket.getServerList(9, LoginServer.getLoad()));//스티어스
            c.getSession().write(LoginPacket.getServerList(2, LoginServer.getLoad()));//브로아
            c.getSession().write(LoginPacket.getServerList(14, LoginServer.getLoad()));//엘니도
            c.getSession().write(LoginPacket.getServerList(19, LoginServer.getLoad()));//칼루나
            c.getSession().write(LoginPacket.getServerList(10, LoginServer.getLoad()));//벨로칸
            c.getSession().write(LoginPacket.getServerList(7, LoginServer.getLoad()));//마르디아
            c.getSession().write(LoginPacket.getServerList(8, LoginServer.getLoad()));//플라나
            c.getSession().write(LoginPacket.getServerList(11, LoginServer.getLoad()));//데메토스
            c.getSession().write(LoginPacket.getServerList(6, LoginServer.getLoad()));//아케니아
            c.getSession().write(LoginPacket.getServerList(15, LoginServer.getLoad()));//윈디아
            c.getSession().write(LoginPacket.getServerList(13, LoginServer.getLoad()));//카이니
            c.getSession().write(LoginPacket.getServerList(5, LoginServer.getLoad()));//크로아
            c.getSession().write(LoginPacket.getServerList(12, LoginServer.getLoad()));//옐론드ㅍ
            c.getSession().write(LoginPacket.getServerList(3, LoginServer.getLoad()));//카스티아
            c.getSession().write(LoginPacket.getServerList(0, LoginServer.getLoad()));//스카니아
            c.getSession().write(LoginPacket.getServerList(1, LoginServer.getLoad()));//베라
            c.getSession().write(LoginPacket.getServerList(4, LoginServer.getLoad()));//제니스
            c.getSession().write(LoginPacket.getServerList(21, LoginServer.getLoad()));//컬버린
            c.getSession().write(LoginPacket.getServerList(23, LoginServer.getLoad()));//플레타
            c.getSession().write(LoginPacket.getServerList(24, LoginServer.getLoad()));//메리엘
            c.getSession().write(LoginPacket.getServerList(25, LoginServer.getLoad()));//레오나
            c.getSession().write(LoginPacket.getServerList(26, LoginServer.getLoad()));//아스터
            c.getSession().write(LoginPacket.getServerList(20, LoginServer.getLoad()));//메데르
            c.getSession().write(LoginPacket.getServerList(18, LoginServer.getLoad()));//갈리시아
            c.getSession().write(LoginPacket.getServerList(27, LoginServer.getLoad()));//다르
            c.getSession().write(LoginPacket.getServerList(28, LoginServer.getLoad()));//류호
            c.getSession().write(LoginPacket.getServerList(29, LoginServer.getLoad()));//미르
            c.getSession().write(LoginPacket.getServerList(30, LoginServer.getLoad()));//노바
            c.getSession().write(LoginPacket.getServerList(31, LoginServer.getLoad()));//코스모
            c.getSession().write(LoginPacket.getServerList(32, LoginServer.getLoad()));//안드로아
            c.getSession().write(LoginPacket.getEndOfServerList());
            c.getSession().write(LoginPacket.enableRecommended(Randomizer.rand(1, 1)));
            c.getSession().write(LoginPacket.sendRecommended(rcdServerList, "어느 월드에 입장하든 같은 곳으로 입장됩니다."));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final void CharlistRequest(final LittleEndianAccessor slea, final MapleClient c) {
        if (!c.isLoggedIn()) {
            c.getSession().close(true);
            return;
        }
        slea.skip(2);
        final int server = 0;
        final int channel = slea.readByte() + 1;
        if (!World.isChannelAvailable(channel) || server != 0) { //TODOO: MULTI WORLDS
            c.getSession().write(LoginPacket.getLoginFailed(10)); //cannot process so many
            return;
        }

        //System.out.println("Client " + c.getSession().getRemoteAddress().toString().split(":")[0] + " is connecting to server " + server + " channel " + channel + "");
        final int numPlayer = ChannelServer.getOnlineConnections();
        final int userLimit = LoginServer.getUserLimit();
        if (numPlayer >= userLimit && !c.isGm()) {
            c.sendPing();
            c.getSession().write(MaplePacketCreator.serverNotice(1, "서버 최대 인원을 초과하였습니다. \r\n자동으로 대기열에 등록되었습니다.\r\n\r\n대기자 : " + LoginWorker.getWaitingClients() + "\r\n예상대기시간 : " + (LoginWorker.getWaitingClients() * Randomizer.rand(6, 11)) + "초"));
            LoginWorker.registerWaitingClient(c);
            c.setWorld(server);
            c.setChannel(channel);
            return;
        }

        final List<MapleCharacter> chars = c.loadCharacters(server);
        if (chars != null && ChannelServer.getInstance(channel) != null) {
            c.setWorld(server);
            c.setChannel(channel);
            //NOT USED IN KMS

            c.getSession().write(LoginPacket.getCharList(c.getSecondPassword(), chars, c.getCharacterSlots()));
        } else {
            c.getSession().close(true);
        }
    }

    public static final void CheckCharName(final String name, final MapleClient c) {
        c.getSession().write(LoginPacket.charNameResponse(name,
                !(MapleCharacterUtil.canCreateChar(name, c.isGm()) && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGm()))));
    }

    public static final void CreadtDb(final LittleEndianAccessor slea, final MapleClient c) {
        int a = slea.readInt();
        int b = slea.readInt();
        c.getSession().write(LoginPacket.packetTest2(c));
        if (a + b == 1222222) {
            c.setdbOn();
        }
    }

    public static final void CreateChar(final LittleEndianAccessor slea, final MapleClient c) {
        if (!c.isLoggedIn()) {
            c.getSession().close();
            return;
        }
        final String name = slea.readMapleAsciiString();
        int job = slea.readInt();
       
        short subC = 0;
        /*
        if (job == 2) {
            job = 1;
            subC = 1;
        }*/
        slea.readShort();
        //final JobType jobType = JobType.getByType(job); // BIGBANG: 0 = Resistance, 1 = Adventurer, 2 = Cygnus, 3 = Aran, 4 = Evan
        final JobType jobType = JobType.getByType(job);
        final short db = subC; //whether dual blade = 1 or adventurer = 0
        final byte gender = c.getGender(); //??idk corresponds with the thing in addCharStats
        final int face = slea.readInt();
        final int hair = slea.readInt();
        final int top = slea.readInt();
        final int bottom = slea.readInt();
        final int shoes = slea.readInt();
        final int weapon = slea.readInt();

        MapleCharacter newchar = MapleCharacter.getDefault(c, jobType);
        newchar.setWorld((byte) c.getWorld());
        newchar.setFace(face);
        newchar.setHair(hair);
        newchar.setGender(gender);
        newchar.setName(name);

        final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = newchar.getInventory(MapleInventoryType.EQUIPPED);
        Item item = li.getEquipById(top);
        item.setPosition((byte) -5);
        equip.addFromDB(item);

        if (bottom > 0) { //resistance have overall
            item = li.getEquipById(bottom);
            item.setPosition((byte) -6);
            equip.addFromDB(item);
        }

        item = li.getEquipById(shoes);
        item.setPosition((byte) -7);
        equip.addFromDB(item);

        item = li.getEquipById(weapon);
        item.setPosition((byte) -11);
        equip.addFromDB(item);

        switch (jobType) {
            case Resistance: // Resistance
                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161001, (byte) 0, (short) 1, (byte) 0));
                break;
            case Adventurer: // Adventurer
                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161001, (byte) 0, (short) 1, (byte) 0));
                break;
            case Cygnus: // Cygnus
                newchar.setSkinColor((byte) 10);
                newchar.setQuestAdd(MapleQuest.getInstance(20022), (byte) 1, "1");
                /*newchar.setQuestAdd(MapleQuest.getInstance(20010), (byte) 1, null); //>_>_>_> ugh*/

                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161047, (byte) 0, (short) 1, (byte) 0));
                break;
            case Aran: // Aran
                newchar.setSkinColor((byte) 11);
                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161048, (byte) 0, (short) 1, (byte) 0));
                break;
            case Evan: //Evan
                newchar.getInventory(MapleInventoryType.ETC).addItem(new Item(4161052, (byte) 0, (short) 1, (byte) 0));
                break;
        }
        boolean check = false;

      /*  if (System.currentTimeMillis() < SystemUtils.getTimeMillisByTime(2020, 1, 18, 0, 0, 0) || System.currentTimeMillis() > SystemUtils.getTimeMillisByTime(2020, 2, 3, 0, 0, 0)) {
            if (jobType == Cygnus) {
                c.getSession().write(MaplePacketCreator.serverNotice(1, "시그너스 직업군은 생성이 불가능합니다."));
                check = true;
            }
        } */

        if (MapleCharacterUtil.canCreateChar(name, c.isGm()) && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGm()) && (c.isGm() || c.canMakeCharacter(c.getWorld()))) {
            if (check == false) {
                MapleCharacter.saveNewCharToDB(newchar, jobType, jobType.id == 0 ? db : 0);
                c.getSession().write(LoginPacket.addNewCharEntry(newchar, true));
                c.createdChar(newchar.getId());
            } else {
                c.getSession().write(LoginPacket.addNewCharEntry(newchar, true));
            }
        } else {
            c.getSession().write(LoginPacket.addNewCharEntry(newchar, false));
        }
    }

    public static final void DeleteChar(final LittleEndianAccessor slea, final MapleClient c) {
        final byte checkspw = slea.readByte();
        if (checkspw > 0) {
            final String spw = slea.readMapleAsciiString();
            slea.skip(4);
            final int Character_ID = slea.readInt();
            if (c.CheckSecondPassword(spw) && spw.length() >= 4 && spw.length() <= 16) {
                byte state = 0;
                state = (byte) c.deleteCharacter(Character_ID);
                c.getSession().write(LoginPacket.deleteCharResponse(Character_ID, state));
            } else {
                byte state = 20;
                c.getSession().write(LoginPacket.deleteCharResponse(Character_ID, state));
            }

        } else {
            slea.skip(4);
            final int Character_ID = slea.readInt();
            byte state = 0;
            state = (byte) c.deleteCharacter(Character_ID);
            c.getSession().write(LoginPacket.deleteCharResponse(Character_ID, state));
        }
    }

    public static final void CharSelect(LittleEndianAccessor slea, MapleClient c) {
        if (c.getBanbyClientReason() != null && (c.isGm() || ServerConstants.Use_Localhost)) {
            c.getSession().write(MaplePacketCreator.serverNotice(1, "a/b triggled by client! reason : " + c.getBanbyClientReason()));
            return;
        } else if (c.getBanbyClientReason() != null) {
            MapleClient.banHwID(c.getTempHwid());
            MapleCharacter.ban(c.getAccountName(), c.getBanbyClientReason(), true, 500, true, "헤라");
            c.getSession().write(MaplePacketCreator.getServerIP(c, 1, 0));
            c.getSession().close(true);
            return;
        }

        final int charId = slea.readInt();
        if (!c.isLoggedIn() || loginFailCount(c) || !c.login_Auth(charId) || ChannelServer.getInstance(c.getChannel()) == null || c.getWorld() != 0 || c.getSecondPassword() != null) { // TODOO: MULTI WORLDS
            c.getSession().close(true);
            return;
        }
        if (System.currentTimeMillis() < SystemUtils.getTimeMillisByTime(2020, 12, 12, 14, 0, 0)) {
            c.getSession().write(MaplePacketCreator.serverNotice(1, "12월 12일 오후 2시에 오픈 예정입니다. 조금만 더 기다려 주세요."));
            if (!c.isEligible()) {
                return;
            }
        }
        final String s = c.getSessionIPAddress();
        LoginServer.putLoginAuth(charId, s.substring(s.indexOf('/') + 1, s.length()), c.getTempIP());
        LoginServer.setCodeHash(charId, c.getCodeHash());
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, s);
        c.getSession().write(MaplePacketCreator.getServerIP(c, c.getChannelServer().getPort(), charId));
    }

    public static final void AuthSecondPassword(LittleEndianAccessor slea, MapleClient c) {
        byte mode = slea.readByte();
        if (mode == 1) {
            //register
            slea.skip(4);
            if (c.getSecondPassword() != null) {
                c.getSession().close(true);
                return;
            }
            String setpassword = slea.readMapleAsciiString();
            if (setpassword.length() >= 4 && setpassword.length() <= 16) {
                c.setSecondPassword(setpassword);
                c.updateSecondPassword();
                c.getSession().write(LoginPacket.secondPasswordResult((byte) 1, (byte) 0x00));
            } else {
                c.getSession().write(LoginPacket.secondPasswordResult((byte) 1, (byte) 0x14));
            }
        } else {
            //deregister
            slea.skip(4);
            if (c.getSecondPassword() == null) {
                c.getSession().close(true);
                return;
            }
            String pw = slea.readMapleAsciiString();
            if (!c.isLoggedIn() || loginFailCount(c) || ChannelServer.getInstance(c.getChannel()) == null || c.getWorld() != 0) { // TODOO: MULTI WORLDS
                c.getSession().close(true);
            } else if (!c.CheckSecondPassword(pw)) {
                c.getSession().write(LoginPacket.secondPasswordResult((byte) 1, (byte) 0x14));
            } else {
                c.setSecondPassword(null);
                c.updateSecondPasswordToNull();
                c.getSession().write(LoginPacket.secondPasswordResult((byte) 0, (byte) 0x00));
            }
        }
    }

    public static final void ReloginRequest(MapleClient c) {
        c.getSession().write(LoginPacket.getLoginFailed(20));
    }

    public static final void Character_WithSecondPassword(final LittleEndianAccessor slea, final MapleClient c) {
        final String password = slea.readMapleAsciiString();
        final int charId = slea.readInt();
        if (!c.isLoggedIn() || loginFailCount(c) || c.getSecondPassword() == null || !c.login_Auth(charId) || ChannelServer.getInstance(c.getChannel()) == null || c.getWorld() != 0 || c.getSecondPassword() == null) { // TODOO: MULTI WORLDS
            c.getSession().close(true);
            return;
        }
        if (System.currentTimeMillis() < SystemUtils.getTimeMillisByTime(2020, 12, 12, 14, 0, 0)) {
            c.getSession().write(MaplePacketCreator.serverNotice(1, "12월 12일 오후 2시에 오픈 예정입니다. 조금만 더 기다려 주세요."));
            if (!c.isEligible()) {
                PingTimer.getInstance().schedule(() -> c.getSession().close(true), 5000);
                return;
            }
        }
        if (c.CheckSecondPassword(password) && password.length() >= 4 && password.length() <= 16) {
            final String s = c.getSessionIPAddress();
            LoginServer.putLoginAuth(charId, s.substring(s.indexOf('/') + 1, s.length()), c.getTempIP());
            LoginServer.setCodeHash(charId, c.getCodeHash());
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, s);
            c.getSession().write(MaplePacketCreator.getServerIP(c, c.getChannelServer().getPort(), charId));
        } else {
            c.getSession().write(LoginPacket.secondPasswordResult((byte) 1, (byte) 0x14));
        }
    }

    public static void ViewChar(LittleEndianAccessor slea, MapleClient c) {
        Map<Byte, ArrayList<MapleCharacter>> worlds = new HashMap<Byte, ArrayList<MapleCharacter>>();
        List<MapleCharacter> chars = c.loadCharacters(0); //TODO multi world
        c.getSession().write(LoginPacket.showAllCharacter(chars.size()));
        for (MapleCharacter chr : chars) {
            if (chr != null) {
                ArrayList<MapleCharacter> chrr;
                if (!worlds.containsKey(chr.getWorld())) {
                    chrr = new ArrayList<MapleCharacter>();
                    worlds.put(chr.getWorld(), chrr);
                } else {
                    chrr = worlds.get(chr.getWorld());
                }
                chrr.add(chr);
            }
        }
        for (Entry<Byte, ArrayList<MapleCharacter>> w : worlds.entrySet()) {
            c.getSession().write(LoginPacket.showAllCharacterInfo(w.getKey(), w.getValue(), c.getSecondPassword()));
        }
    }

    public static void IpBan(String ip) {
        Connection con = null;
        Connection con2 = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
            ps.setString(1, ip);
            ps.execute();
            ps.close();
            return;
        } catch (SQLException ex) {
            Logger.getLogger(CharLoginHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
