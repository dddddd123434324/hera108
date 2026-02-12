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

import client.MapleCharacter;
import client.MapleCharacterUtil;
import handling.world.World;
import server.ItemInfo;
import server.LoginRestrictor;
import server.RateManager;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.data.LittleEndianAccessor;

/**
 * @author Lunatic
 */
public class GMHandler {

    public static void onPacket(GMClient c, LittleEndianAccessor lea) {
        int op = lea.readByte() & 0xFF;
        switch (op) {
            case 0:
                c.sendPacket(GMPacket.initialInformation());
                break;
            case 1: //Change Permanant Rate
            {
                int type = lea.readByte();
                int value = lea.readInt();
                switch (type) {
                    case 1:
                        RateManager.EXP = value;
                        break;
                    case 2:
                        RateManager.DROP = value;
                        break;
                    case 3:
                        RateManager.MESO = value;
                        break;
                }
                break;
            }
            case 2: //Broadcast ServerNotice
            {
                int type = lea.readByte();
                String msg = lea.readMapleAsciiString();
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(type, msg));
                break;
            }
            case 3: //Player Action
            {
                String playerName = lea.readMapleAsciiString();
                MapleCharacter player = World.getCharacterByName(playerName);
                if (player == null) {
                    c.sendPacket(GMPacket.playerActionResult(1));
                    return;
                }
                /**
                 * 0 : 접속 해제 파일의 끝 접속중으로 처리 주변에 있는 사람 디스커넥트 주변에 있는 사람 파일의 끝 포스
                 * 리턴맵으로 강제 이동 아래 레벨로 권한 설정 아래 맵으로 이동 요청 아래 메시지로 귓속말 (GM) 아래 이유로
                 * 정지 아래 이유로 아이피밴
                 */
                int type = lea.readByte();
                String arg = lea.readMapleAsciiString();
                try {
                    switch (type) {
                        case 0: {
                            player.getClient().setStop();
                            LoginRestrictor.VictimAccountID.remove(player.getAccountID());
                            break;
                        }
                        case 1: {
                            player.getClient().sendPacket(MaplePacketCreator.sombra());
                            break;
                        }
                        case 2: {
                            LoginRestrictor.VictimAccountID.add(player.getAccountID());
                            break;
                        }
                        case 3: {
                            for (MapleCharacter victim : player.getMap().getCharactersThreadsafe()) {
                                victim.getClient().setStop();
                            }
                            break;
                        }
                        case 4: {
                            player.getMap().broadcastMessage(player, MaplePacketCreator.sombra(), player.getPosition());
                            break;
                        }
                        case 5: {
                            MapleMap to = player.getMap().getForcedReturnMap();
                            player.changeMap(to, to.getPortal(0));
                            break;
                        }
                        case 6: {
                            int ia = Integer.parseInt(arg);
                            player.setGMLevel(ia);
                            break;
                        }
                        case 7: {
                            int ia = Integer.parseInt(arg);
                            MapleMap to = player.getClient().getChannelServer().getMapFactory().getMap(ia);
                            player.changeMap(to, to.getPortal(0));
                            break;
                        }
                        case 8: {
                            player.getClient().sendPacket(MaplePacketCreator.getWhisper("GM", player.getClient().getChannel(), arg));
                            break;
                        }
                        case 9: {
                            player.ban(arg, false, false, false, "GM");
                            break;
                        }
                        case 10: {
                            player.ban(arg, true, false, true, "GM");
                            break;
                        }
                    }
                    c.sendPacket(GMPacket.playerActionResult(0));
                } catch (Exception ex) {
                    c.sendPacket(GMPacket.playerActionResult(2));
                }
                break;
            }
            case 101: //CreateItemWizard
            {
                int sub = lea.readByte() & 0xFF;
                switch (sub) {
                    case 1: //confirm charname
                        c.sendPacket(GMPacket.callback_existChar(MapleCharacterUtil.getIdByName(lea.readMapleAsciiString()) != -1));
                        break;
                    case 2: //confirm itemid
                        c.sendPacket(GMPacket.callback_existItem(ItemInfo.isExist(lea.readInt())));
                        break;
                    case 3: //result
                        String charName = lea.readMapleAsciiString();
                        int itemID = lea.readInt();
                        int quantity = lea.readInt();
                        String text = lea.readMapleAsciiString();
                        boolean r = World.sendPackage(charName, itemID, quantity, "GM", text);
                        c.sendPacket(GMPacket.create_result(r));
                        break;
                }
                break;
            }
            case 102: //EventSchedulerWizard
            {
                int sub = lea.readByte() & 0xFF;
                switch (sub) {
                    case 0:
                        c.sendPacket(GMPacket.updateSchedules());
                        return;
                    case 1: //insert
                        RateManager.insert(lea.readMapleAsciiString(), lea.readLong(), lea.readLong(), lea.readInt(), lea.readInt(), lea.readInt(), lea.readMapleAsciiString());
                        break;
                    case 2: //delete
                        RateManager.delete(lea.readLong());
                        break;
                }
                RateManager.save();
                GMServer.broadcast(GMPacket.updateSchedules());
                break;
            }
        }
    }
}
