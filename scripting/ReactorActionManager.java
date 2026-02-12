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
package scripting;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import handling.channel.ChannelServer;
import server.MapleCarnivalFactory;
import server.MapleCarnivalFactory.MCSkill;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.RateManager;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleReactor;
import server.maps.ReactorDropEntry;

import java.awt.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import server.Timer.EtcTimer;

public class ReactorActionManager extends AbstractPlayerInteraction {

    private MapleReactor reactor;

    public ReactorActionManager(MapleClient c, MapleReactor reactor) {
        super(c, reactor.getReactorId(), c.getPlayer().getMapId());
        this.reactor = reactor;
    }

    public void dropItems() {
        dropItems(true);
    }

    public void special(boolean wide) {
        final List<ReactorDropEntry> chances = ReactorScriptManager.getInstance().getDrops(reactor.getReactorId());
        final List<ReactorDropEntry> items = new LinkedList<ReactorDropEntry>();
        int numItems = 0;
        final Iterator<ReactorDropEntry> iter = chances.iterator();
        while (iter.hasNext()) {
            ReactorDropEntry d = (ReactorDropEntry) iter.next();
            if (Randomizer.nextInt(999999) < d.chance && (d.questid <= 0 || getPlayer().getQuestStatus(d.questid) == 1)) {
                numItems++;
                items.add(d);
            }
        }

        final Point dropPos = reactor.getPosition();
        int j = 0;
        for (int i = 0; i < numItems; i++) {
            if (reactor.getMap().calcPointBelow(new Point(dropPos.x - 12, dropPos.y)) != null) {
                if (reactor.getMap().calcPointBelow(new Point(dropPos.x - 24, dropPos.y)) != null) {
                    dropPos.x -= 12;
                } else {
                    j++;
                }
            } else {
                j++;
            }
        }
        int range, mesoDrop;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MapleCharacter play = getPlayer();
        if (play == null) {
            return;
        }
        int channel = play.getClient().getChannel();
        for (final ReactorDropEntry d : items) {
            if (wide) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                }
            }
            if (d.itemId == 0) {
                range = d.max - d.min;
                mesoDrop = Randomizer.nextInt(range) + d.min * RateManager.MESO;
                reactor.getMap().spawnMesoDrop(mesoDrop, dropPos, reactor, play, false, (byte) 1);
            } else {
                Item drop;
                if (GameConstants.getInventoryType(d.itemId) != MapleInventoryType.EQUIP) {
                    drop = new Item(d.itemId, (byte) 0, (short) (Randomizer.rand(d.min, d.max)), (byte) 0);
                } else {
                    drop = ii.randomizeStats((Equip) ii.getEquipById(d.itemId));
                }
                drop.setGMLog("Dropped from reactor " + reactor.getReactorId() + " on map " + play.getMapId());
                reactor.getMap().spawnItemDrop(reactor, play, drop, dropPos, false, false);
            }
            if (j <= 0) {
                if (reactor.getMap().calcPointBelow(new Point(dropPos.x + 24, dropPos.y)) != null) {
                    if (reactor.getMap().calcPointBelow(new Point(dropPos.x + 48, dropPos.y)) != null) {
                        dropPos.x += 24;
                    }
                }
            } else {
                j -= 2;
            }
        }
    }

    // only used for meso = false, really. No minItems because meso is used to fill the gap
    public void dropItems(boolean wide) {
        switch (reactor.getReactorId()) {
            case 1052001://노말발록
            case 1052002://이지발록
            case 3102000://도토리나무
            case 2202004://루디파퀘 보너스 맵 리엑터
            case 6802000://결혼
            case 6802001://결혼2
                EtcTimer.getInstance().schedule(new Runnable() {
                    @Override
                    public void run() {
                        special(true);
                    }
                }, 600);
                return;
        }
        final List<ReactorDropEntry> chances = ReactorScriptManager.getInstance().getDrops(reactor.getReactorId());
        final List<ReactorDropEntry> items = new LinkedList<ReactorDropEntry>();
        int numItems = 0;
        final Iterator<ReactorDropEntry> iter = chances.iterator();
        while (iter.hasNext()) {
            ReactorDropEntry d = (ReactorDropEntry) iter.next();
            if (Randomizer.nextInt(999999) < d.chance && (d.questid <= 0 || getPlayer().getQuestStatus(d.questid) == 1)) {
                numItems++;
                items.add(d);
            }
        }

        final Point dropPos = reactor.getPosition();
        int j = 0;
        if (wide) {
            for (int i = 0; i < numItems; i++) {
                if (reactor.getMap().calcPointBelow(new Point(dropPos.x - 12, dropPos.y)) != null) {
                    if (reactor.getMap().calcPointBelow(new Point(dropPos.x - 24, dropPos.y)) != null) {
                        dropPos.x -= 12;
                    } else {
                        j++;
                    }
                } else {
                    j++;
                }
            }
        }
        int range, mesoDrop;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (final ReactorDropEntry d : items) {
            if (d.itemId == 0) {
                range = d.max - d.min;
                mesoDrop = Randomizer.nextInt(range) + d.min * RateManager.MESO;
                reactor.getMap().spawnMesoDrop(mesoDrop, dropPos, reactor, getPlayer(), false, (byte) 0);
            } else {
                Item drop;
                if (GameConstants.getInventoryType(d.itemId) != MapleInventoryType.EQUIP) {
                    drop = new Item(d.itemId, (byte) 0, (short) (Randomizer.rand(d.min, d.max)), (byte) 0);
                } else {
                    drop = ii.randomizeStats((Equip) ii.getEquipById(d.itemId));
                }
                drop.setGMLog("Dropped from reactor " + reactor.getReactorId() + " on map " + getPlayer().getMapId());
                reactor.getMap().spawnItemDrop(reactor, getPlayer(), drop, dropPos, false, false);
            }
            if (wide) {
                if (j <= 0) {
                    if (reactor.getMap().calcPointBelow(new Point(dropPos.x + 24, dropPos.y)) != null) {
                        if (reactor.getMap().calcPointBelow(new Point(dropPos.x + 48, dropPos.y)) != null) {
                            dropPos.x += 24;
                        }
                    }
                } else {
                    j -= 2;
                }
            }
        }
    }

    public void dropSingleItem(int itemId) {
        Item drop;
        if (GameConstants.getInventoryType(itemId) != MapleInventoryType.EQUIP) {
            drop = new Item(itemId, (byte) 0, (short) 1, (byte) 0);
        } else {
            drop = MapleItemInformationProvider.getInstance().randomizeStats((Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId));
        }
        drop.setGMLog("Dropped from reactor " + reactor.getReactorId() + " on map " + getPlayer().getMapId());
        reactor.getMap().spawnItemDrop(reactor, getPlayer(), drop, reactor.getPosition(), false, false);
    }

    @Override
    public void spawnNpc(int npcId) {
        spawnNpc(npcId, getPosition());
    }

    // returns slightly above the reactor's position for monster spawns
    public Point getPosition() {
        Point pos = reactor.getPosition();
        pos.y -= 10;
        return pos;
    }

    public MapleReactor getReactor() {
        return reactor;
    }

    public void spawnZakum() {
        reactor.getMap().spawnZakum(getPosition().x, getPosition().y);
    }

    public void spawnFakeMonster(int id) {
        spawnFakeMonster(id, 1, getPosition());
    }

    // summon one monster, remote location
    public void spawnFakeMonster(int id, int x, int y) {
        spawnFakeMonster(id, 1, new Point(x, y));
    }

    // multiple monsters, reactor location
    public void spawnFakeMonster(int id, int qty) {
        spawnFakeMonster(id, qty, getPosition());
    }

    // multiple monsters, remote location
    public void spawnFakeMonster(int id, int qty, int x, int y) {
        spawnFakeMonster(id, qty, new Point(x, y));
    }

    // handler for all spawnFakeMonster
    private void spawnFakeMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; i++) {
            reactor.getMap().spawnFakeMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), pos);
        }
    }

    public void killAll() {
        reactor.getMap().killAllMonsters(true);
    }

    public void killMonster(int monsId) {
        reactor.getMap().killMonster(monsId);
    }

    // summon one monster on reactor location
    @Override
    public void spawnMonster(int id) {
        spawnMonster(id, 1, getPosition());
    }

    // summon monsters on reactor location
    @Override
    public void spawnMonster(int id, int qty) {
        spawnMonster(id, qty, getPosition());
    }

    public void dispelAllMonsters(final int num) { //dispels all mobs, cpq
        final MCSkill skil = MapleCarnivalFactory.getInstance().getGuardian(num);
        if (skil != null) {
            for (MapleMonster mons : getMap().getAllMonstersThreadsafe()) {
                mons.dispelSkill(skil.getSkill());
            }
        }
    }
}
