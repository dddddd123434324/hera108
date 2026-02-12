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
package server.events;

import client.MapleCharacter;
import handling.channel.ChannelServer;
import handling.channel.handler.DueyHandler;
import tools.MaplePacketCreator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eternal
 */
public class OnTimeGiver {

    public static void giveOnTimeBonus() {
        List<MapleCharacter> toGiveChrs = new ArrayList<>();
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            toGiveChrs.addAll(cserv.getPlayerStorage().getAllCharacters());
        }
        for (MapleCharacter chr : toGiveChrs) {
            if (chr != null && chr.getClient() != null) {
                try {
                    giveItemByParcel(2022460, 1, chr);
                    chr.getClient().sendPacket(MaplePacketCreator.receiveParcel("핫타임 이벤트", true));
                    chr.dropMessage(6, "핫타임 이벤트 상품이 도착하였습니다. 택배함에서 상품을 확인하세요.");
                } catch (Exception e) {
                }
            }
        }
    }

    public static void giveItemByParcel(int itemid, int quantity, MapleCharacter chr) {
        DueyHandler.addNewItemToDb(itemid, quantity, chr.getId(), "이벤트", "핫타임 이벤트 상자입니다.", true);
    }

    public static void Hottimes(int item, short quan) {
        List<MapleCharacter> toGiveChrs = new ArrayList<>();
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            toGiveChrs.addAll(cserv.getPlayerStorage().getAllCharacters());
        }
        for (MapleCharacter chr : toGiveChrs) {

        }
        for (MapleCharacter chr : toGiveChrs) {
            if (chr != null && chr.getClient() != null) {
                try {
                    giveItemByParcel(item, quan, chr);
                    chr.getClient().sendPacket(MaplePacketCreator.receiveParcel("핫타임이벤트", true));
                    chr.dropMessage(6, "핫타임 이벤트 상품이 도착하였습니다. 택배함에서 상품을 확인하세요.");
                } catch (Exception e) {
                }
            }
        }
    }
}
