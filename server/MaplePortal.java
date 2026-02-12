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
package server;

import client.MapleClient;
import client.anticheat.CheatingOffense;
import handling.channel.ChannelServer;
import scripting.PortalScriptManager;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

import java.awt.*;

public class MaplePortal {

    public static final int MAP_PORTAL = 2;
    public static final int DOOR_PORTAL = 6;

    private String name, target, scriptName;
    private Point position;
    public int targetmap, type, id;
    private boolean portalState = true;

    public MaplePortal(final int type) {
        this.type = type;
    }

    public final int getId() {
        return id;
    }

    public final void setId(int id) {
        this.id = id;
    }

    public final String getName() {
        return name;
    }

    public final Point getPosition() {
        return position;
    }

    public final String getTarget() {
        return target;
    }

    public final int getTargetMapId() {
        return targetmap;
    }

    public final int getType() {
        return type;
    }

    public final String getScriptName() {
        return scriptName;
    }

    public final void setName(final String name) {
        this.name = name;
    }

    public final void setPosition(final Point position) {
        this.position = position;
    }

    public final void setTarget(final String target) {
        this.target = target;
    }

    public final void setTargetMapId(final int targetmapid) {
        this.targetmap = targetmapid;
    }

    public final void setScriptName(final String scriptName) {
        this.scriptName = scriptName;
    }

    public final void enterPortal(final MapleClient c) {
        if (getPosition().distanceSq(c.getPlayer().getPosition()) > 40000 && !c.getPlayer().isGM() && c.getPlayer().getMapId() != 922010501) {
            c.getSession().write(MaplePacketCreator.enableActions());
            c.getPlayer().getCheatTracker().registerOffense(CheatingOffense.USING_FARAWAY_PORTAL);
            return;
        }
        final MapleMap currentmap = c.getPlayer().getMap();
        if (!c.getPlayer().hasBlockedInventory() && (portalState || c.getPlayer().isGM())) {
            if (getScriptName() != null) {
                try {
                    PortalScriptManager.getInstance().executePortalScript(this, c);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            } else if (getTargetMapId() != 999999999) {
                c.getPlayer().setFh(0);
                final MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(getTargetMapId());
                if (to == null) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                if (!c.getPlayer().isGM()) {
                    if (to.getLevelLimit() > 0 && to.getLevelLimit() > c.getPlayer().getLevel()) {
                        c.getPlayer().dropMessage(-1, "레벨이 부족합니다.");
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    //if (to.getForceMove() > 0 && to.getForceMove() < c.getPlayer().getLevel()) {
                    //    c.getPlayer().dropMessage(-1, "You are too high of a level to enter this place.");
                    //    c.getSession().write(MaplePacketCreator.enableActions());
                    //    return;
                    //}
                }
                /*if (targetmap == 100000100) { //헤네시스시장
                 if (getId() == 9 || getId() == 18 || getId() == 13) {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 1);
                 } else {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 0);
                 }
                 } else if (targetmap == 101030400) { //동쪽바위산1
                 if (getId() == 10 || getId() == 12 || getId() == 11) {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 1);
                 } else {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 0);
                 }
                 } else if (targetmap == 105070000) { //깊은개미굴
                 if (getId() == 5) {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 1);
                 } else if (getId() == 7) {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) -1);
                 } else {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 0);
                 }
                 } else if (targetmap == 100040105) { //사악한기운의숲1
                 if (getId() == 13) {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) -2);
                 } else {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 0);
                 }
                 } else if (targetmap == 105090100) { //빛을잃은동굴
                 if (getId() == 6) {
                 if (currentmap.mapid == 105090000) {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 2);
                 } else {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 0);
                 }
                 } else if (getId() == 7) {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) -2);
                 } else {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 0);
                 }
                 } else if (targetmap == 105090200) { //또다른입구
                 if (getId() == 6) {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 1);
                 } else if (getId() == 7) {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) -1);
                 } else {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 0);
                 }
                 } else if (targetmap == 101040001) { //와일드보어의땅
                 if (getId() == 10) {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) -1);
                 } else {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 0);
                 }
                 } else if (targetmap == 105030000) { //깊은숲
                 if (getId() == 18) {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) -1);
                 } else if (getId() == 8) {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 2);
                 } else {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 0);
                 }
                 } else if (targetmap == 105070200 || targetmap == 105070300) { //이블아이의굴2/3
                 if (getId() == 6 || getId() == 2) {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) -1);
                 } else if (getId() == 1 || getId() == 4) {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 1);
                 } else {
                 c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 0);
                 }
                 } else {
                        c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 0);
                 }*/
                switch (c.getPlayer().getMapId()) {
                    case 910000007:
                        c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) -5);
                        break;
                    case 910000008:
                        c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) -6);
                        break;
                    case 910000013:
                        c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) -7);
                        break;
                    case 910000018:
                        c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) -8);
                        break;
                    default:
                        c.getPlayer().changeMapPortal(to, to.getPortal(getTarget()) == null ? to.getPortal(0) : to.getPortal(getTarget()), (byte) 0);
                        break;
                }
            }
        }
        if (c != null && c.getPlayer() != null && c.getPlayer().getMap() == currentmap) { // Character is still on the same map.
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public boolean getPortalState() {
        return portalState;
    }

    public void setPortalState(boolean ps) {
        this.portalState = ps;
    }
}
