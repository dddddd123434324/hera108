/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.messages.commands;

import client.*;
import client.anticheat.CheatingOffense;
import client.inventory.*;
import client.messages.CommandProcessorUtil;
import constants.GameConstants;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.channel.handler.DueyHandler;
import handling.world.World;
import scripting.PortalScriptManager;
import scripting.ReactorScriptManager;
import scripting.vm.NPCScriptInvoker;
import server.*;
import server.Timer;
import server.Timer.*;
import server.events.OnTimeGiver;
import server.life.*;
import server.maps.*;
import server.quest.MapleQuest;
import server.shops.HiredMerchant;
import tools.HexTool;
import tools.MaplePacketCreator;
import tools.StringUtil;
import tools.packet.MobPacket;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import scripting.vm.NPCScriptTargetFunction;
import tools.data.MaplePacketLittleEndianWriter;

import java.util.Map;
import server.ItemInformation;
import server.MapleItemInformationProvider;
import constants.GameConstants;
import tools.FileoutputUtil;
import client.MapleClient;
import client.messages.commands.CommandExecute;

/**
 * @author Emilyx3
 */
public class SuperGMCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.SUPERGM;
    }

    public static class 스킬마스터 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int Job = c.getPlayer().getJob();
            int Job1 = Job / 100 * 100;
            int Job2 = Job1 + Job % 100 - Job % 10;
            int Job3 = Job2 + 1;
            int Job4 = Job3 + 1;
            for (Skill skill : SkillFactory.getAllSkills()) {
                /*if (!c.getPlayer().checkMasterSkill(skill.getId())) {
                 continue;
                 }*/
                if (skill.getId() / 10000 == Job1) {
                    c.getPlayer().changeSkillLevel(skill, skill.getMaxLevel(), (byte) skill.getMaxLevel());
                }
                if (skill.getId() / 10000 >= Job2 && skill.getId() / 10000 <= Job) {
                    c.getPlayer().changeSkillLevel(skill, skill.getMaxLevel(), (byte) skill.getMaxLevel());
                }
            }
            c.getPlayer().teachSkill(1004, (byte) Math.max(1, c.getPlayer().getSkillLevel(1004)), (byte) Math.max(1, c.getPlayer().getSkillLevel(1004)));
            c.getPlayer().teachSkill(1005, (byte) Math.max(1, c.getPlayer().getSkillLevel(1005)), (byte) Math.max(1, c.getPlayer().getSkillLevel(1005)));
            c.getPlayer().teachSkill(1007, (byte) 3, (byte) 3);
            return 1;
        }
        /*public int execute(MapleClient c, String[] splitted) {
         for (Skill skill : SkillFactory.getAllSkills()) {
         c.getPlayer().changeSkillLevel(skill, skill.getMaxLevel(), (byte) skill.getMaxLevel());
         }
         return 1;
         }*/
    }

    public static class 스킬삭제 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (Skill skill : SkillFactory.getAllSkills()) {
                c.getPlayer().changeSkillLevel(skill, 0, (byte) 0);
            }
            return 1;
        }
    }

    public static class GiveSkill extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            Skill skill = SkillFactory.getSkill(Integer.parseInt(splitted[2]));
            byte level = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1);
            byte masterlevel = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 4, 1);

            if (level > skill.getMaxLevel()) {
                level = (byte) skill.getMaxLevel();
            }
            if (masterlevel > skill.getMaxLevel()) {
                masterlevel = (byte) skill.getMaxLevel();
            }
            victim.changeSkillLevel(skill, level, masterlevel);
            return 1;
        }
    }

    public static class UnlockInv extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            java.util.Map<Item, MapleInventoryType> eqs = new HashMap<Item, MapleInventoryType>();
            boolean add = false;
            if (splitted.length < 2 || splitted[1].equals("all")) {
                for (MapleInventoryType type : MapleInventoryType.values()) {
                    for (Item item : c.getPlayer().getInventory(type)) {
                        if (ItemFlag.LOCK.check(item.getFlag())) {
                            item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                            add = true;
                            //c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                        }
                        if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                            item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                            add = true;
                            //c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                        }
                        if (add) {
                            eqs.put(item, type);
                        }
                        add = false;
                    }
                }
            } else if (splitted[1].equals("eqp")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).newList()) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                        //c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                        //c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.EQUIP);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("eq")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIP)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                        //c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                        //c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.EQUIP);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("u")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.USE)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                        //c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                        //c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.USE);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("s")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.SETUP)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                        //c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                        //c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.SETUP);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("e")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                        //c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                        //c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.ETC);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("c")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                        add = true;
                        //c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                        add = true;
                        //c.getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.CASH);
                    }
                    add = false;
                }
            } else {
                c.getPlayer().dropMessage(6, "[all/eqp/eq/u/s/e/c]");
            }

            for (Entry<Item, MapleInventoryType> eq : eqs.entrySet()) {
                c.getPlayer().forceReAddItem_NoUpdate(eq.getKey().copy(), eq.getValue());
            }
            return 1;
        }
    }

    public static class 아이템드롭 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int itemId = Integer.parseInt(splitted[1]);
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (GameConstants.isPet(itemId)) {
                c.getPlayer().dropMessage(5, "Please purchase a pet from the cash shop instead.");
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " does not exist");
            } else {
                Item toDrop;
                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    toDrop = ii.getEquipById(itemId);
                } else {
                    toDrop = new client.inventory.Item(itemId, (byte) 0, (short) quantity, (byte) 0);
                }
                if (!c.getPlayer().isAdmin()) {
                    toDrop.setGMLog(c.getPlayer().getName() + " used !drop");
                    toDrop.setOwner(c.getPlayer().getName());
                }
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
            }
            return 1;
        }
    }

    public static class Marry extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "Need <name> <itemid>");
                return 0;
            }
            int itemId = Integer.parseInt(splitted[2]);
            if (!GameConstants.isEffectRing(itemId)) {
                c.getPlayer().dropMessage(6, "Invalid itemID.");
            } else {
                MapleCharacter fff = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (fff == null) {
                    c.getPlayer().dropMessage(6, "Player must be online");
                } else {
                    int[] ringID = {MapleInventoryIdentifier.getInstance(), MapleInventoryIdentifier.getInstance()};
                    try {
                        MapleCharacter[] chrz = {fff, c.getPlayer()};
                        for (int i = 0; i < chrz.length; i++) {
                            Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId, ringID[i]);
                            if (eq == null) {
                                c.getPlayer().dropMessage(6, "Invalid itemID.");
                                return 0;
                            }
                            MapleInventoryManipulator.addbyItem(chrz[i].getClient(), eq.copy());
                            chrz[i].dropMessage(6, "Successfully married with " + chrz[i == 0 ? 1 : 0].getName());
                        }
                        MapleRing.addToDB(itemId, c.getPlayer(), fff.getName(), fff.getId(), ringID);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return 1;
        }
    }

    /*
     public static class Vac extends CommandExecute {

     @Override
     public int execute(MapleClient c, String[] splitted) {
     if (!c.getPlayer().isHidden()) {
     c.getPlayer().dropMessage(6, "You can only vac monsters while in hide.");
     return 0;
     } else {
     for (final MapleMapObject mmo : c.getPlayer().getMap().getAllMonstersThreadsafe()) {
     final MapleMonster monster = (MapleMonster) mmo;
     c.getPlayer().getMap().broadcastMessage(MobPacket.moveMonster(false, -1, 0, 0, (short) 0, monster.getObjectId(), monster.getTruePosition(), c.getPlayer().getLastRes(), null, null));
     monster.setPosition(c.getPlayer().getPosition());
     }
     }
     return 1;
     }
     }*/
    public static class SpeakMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter victim : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (victim.getId() != c.getPlayer().getId()) {
                    victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
                }
            }
            return 1;
        }
    }

    public static class SpeakChn extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter victim : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                if (victim.getId() != c.getPlayer().getId()) {
                    victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
                }
            }
            return 1;
        }
    }

    public static class SpeakWorld extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter victim : cserv.getPlayerStorage().getAllCharacters()) {
                    if (victim.getId() != c.getPlayer().getId()) {
                        victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
                    }
                }
            }
            return 1;
        }
    }

    public static class Monitor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (target != null) {
                if (target.getClient().isMonitored()) {
                    target.getClient().setMonitored(false);
                    c.getPlayer().dropMessage(5, "Not monitoring " + target.getName() + " anymore.");
                } else {
                    target.getClient().setMonitored(true);
                    c.getPlayer().dropMessage(5, "Monitoring " + target.getName() + ".");
                }
            } else {
                c.getPlayer().dropMessage(5, "Target not found on channel.");
                return 0;
            }
            return 1;
        }
    }

    public static class ResetOther extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forfeit(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]));
            return 1;
        }
    }

    public static class FStartOther extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forceStart(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]), Integer.parseInt(splitted[3]), splitted.length > 4 ? splitted[4] : null);
            return 1;
        }
    }

    public static class FCompleteOther extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forceComplete(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]), Integer.parseInt(splitted[3]));
            return 1;
        }
    }

    public static class Threads extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            String filter = "";
            if (splitted.length > 1) {
                filter = splitted[1];
            }
            for (int i = 0; i < threads.length; i++) {
                String tstring = threads[i].toString();
                if (tstring.toLowerCase().indexOf(filter.toLowerCase()) > -1) {
                    c.getPlayer().dropMessage(6, i + ": " + tstring);
                }
            }
            return 1;
        }
    }

    public static class ShowTrace extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                throw new IllegalArgumentException();
            }
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            Thread t = threads[Integer.parseInt(splitted[1])];
            c.getPlayer().dropMessage(6, t.toString() + ":");
            for (StackTraceElement elem : t.getStackTrace()) {
                c.getPlayer().dropMessage(6, elem.toString());
            }
            return 1;
        }
    }

    public static class ToggleOffense extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                CheatingOffense co = CheatingOffense.valueOf(splitted[1]);
                co.setEnabled(!co.isEnabled());
            } catch (IllegalArgumentException iae) {
                c.getPlayer().dropMessage(6, "Offense " + splitted[1] + " not found");
            }
            return 1;
        }
    }

    public static class TMegaphone extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            World.toggleMegaphoneMuteState();
            c.getPlayer().dropMessage(6, "Megaphone state : " + (c.getChannelServer().getMegaphoneMuteState() ? "Enabled" : "Disabled"));
            return 1;
        }
    }

    public static class SReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleReactor reactor = new MapleReactor(MapleReactorFactory.getReactor(Integer.parseInt(splitted[1])), Integer.parseInt(splitted[1]));
            reactor.setDelay(-1);
            c.getPlayer().getMap().spawnReactorOnGroundBelow(reactor, new Point(c.getPlayer().getTruePosition().x, c.getPlayer().getTruePosition().y - 20));
            return 1;
        }
    }

    public static class ClearSquads extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final Collection<MapleSquad> squadz = new ArrayList<MapleSquad>(c.getChannelServer().getAllSquads().values());
            for (MapleSquad squads : squadz) {
                squads.clear();
            }
            return 1;
        }
    }

    public static class HitMonsterByOID extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            int targetId = Integer.parseInt(splitted[1]);
            int damage = Integer.parseInt(splitted[2]);
            MapleMonster monster = map.getMonsterByOid(targetId);
            if (monster != null) {
                map.broadcastMessage(MobPacket.damageMonster(targetId, damage));
                monster.damage(c.getPlayer(), damage, false);
            }
            return 1;
        }
    }

    public static class HitAll extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;
            if (splitted.length > 1) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "존재하지 않는 맵입니다.");
                return 0;
            }
            int damage = Integer.parseInt(splitted[1]);
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                map.broadcastMessage(MobPacket.damageMonster(mob.getObjectId(), damage));
                mob.damage(c.getPlayer(), damage, false);
            }
            return 1;
        }
    }

    public static class HitMonster extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;
            int damage = Integer.parseInt(splitted[1]);
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (mob.getId() == Integer.parseInt(splitted[2])) {
                    map.broadcastMessage(MobPacket.damageMonster(mob.getObjectId(), damage));
                    mob.damage(c.getPlayer(), damage, false);
                }
            }
            return 1;
        }
    }

    public static class 킬올 extends SuperGMCommand.KillMonster {
    }

    public static class KillMonster extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;

            if (splitted.length > 1) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "Map does not exist");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (!mob.getStats().isBoss() || mob.getStats().isPartyBonus() || c.getPlayer().isGM()) {
                    map.killMonster(mob, c.getPlayer(), false, false, (byte) 1);
                }
            }
            return 1;
        }
    }

    public static class 킬올드롭 extends SuperGMCommand.KillAllDrops {
    }

    public static class KillAllDrops extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;

            if (splitted.length > 1) {
                //&& !splitted[0].equals("!killmonster") && !splitted[0].equals("!hitmonster") && !splitted[0].equals("!hitmonsterbyoid") && !splitted[0].equals("!killmonsterbyoid")) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "존재하지 않는 맵입니다.");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                map.killMonster(mob, c.getPlayer(), true, false, (byte) 1);
            }
            return 1;
        }
    }

    public static class 킬올경험치 extends SuperGMCommand.KillAllExp {
    }

    public static class KillAllExp extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;

            if (splitted.length > 1) {
                //&& !splitted[0].equals("!killmonster") && !splitted[0].equals("!hitmonster") && !splitted[0].equals("!hitmonsterbyoid") && !splitted[0].equals("!killmonsterbyoid")) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "존재하지 않는 맵입니다.");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                mob.damage(c.getPlayer(), mob.getHp(), false);
            }
            return 1;
        }
    }

    public static class 엔피시 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(c.getPlayer().getPosition().y);
                npc.setRx0(c.getPlayer().getPosition().x + 50);
                npc.setRx1(c.getPlayer().getPosition().x - 50);
                npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                npc.setCustom(true);
                c.getPlayer().getMap().addMapObject(npc);
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc, true));
            } else {
                c.getPlayer().dropMessage(6, "존재하지 않는 NPC입니다.");
                return 0;
            }
            return 1;
        }
    }

    public static class 고정엔피시 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "설치할 엔피시 코드를 입력해주세요.");
                return 0;
            }
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                con = DatabaseConnection.getConnection();
                if (npc != null && !npc.getName().equals("MISSINGNO")) {
                    ps = con.prepareStatement("SELECT * FROM spawnnpc WHERE mapid = ? && npcid = ?");
                    ps.setInt(1, c.getPlayer().getMapId());
                    ps.setInt(2, npcId);
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        ps.close();
                        rs.close();
                        ps = con.prepareStatement("DELETE FROM spawnnpc WHERE mapid = ? && npcid = ?");
                        ps.setInt(1, c.getPlayer().getMapId());
                        ps.setInt(2, npcId);
                        ps.executeUpdate();
                        ps.close();
                        c.getPlayer().getMap().removeNpc(npcId);
                        c.getPlayer().dropMessage(6, "현재 맵에는 이미 NPC가 설치되어 있어서 좌표를 변경합니다.");
                    } else {
                        ps.close();
                        rs.close();
                    }
                    npc.setPosition(c.getPlayer().getPosition());
                    npc.setCy(c.getPlayer().getPosition().y);
                    npc.setRx0(c.getPlayer().getPosition().x + 50);
                    npc.setRx1(c.getPlayer().getPosition().x - 50);
                    npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                    npc.setCustom(true);
                    c.getPlayer().getMap().addMapObject(npc);
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc, true));
                } else {
                    c.getPlayer().dropMessage(5, "WZ에 존재하지 않는 NPC를 입력했습니다.");
                    return 0;
                }
                ps = con.prepareStatement("INSERT INTO spawnnpc(rx0, rx1, cy, fh, mapid, npcid) VALUES (? ,? ,? ,? ,? ,?)");
                ps.setInt(1, c.getPlayer().getPosition().x - 50);
                ps.setInt(2, c.getPlayer().getPosition().x + 50);
                ps.setInt(3, c.getPlayer().getPosition().y);
                ps.setInt(4, c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                ps.setInt(5, c.getPlayer().getMapId());
                ps.setInt(6, npcId);
                ps.executeUpdate();
                ps.close();
                con.close();
            } catch (Exception e) {
                System.err.println("fix npc error.");
                e.printStackTrace();
            } finally {
                try {
                    if (con != null) {
                        con.close();
                    }
                    if (ps != null) {
                        ps.close();
                    }
                    if (rs != null) {
                        rs.close();
                    }
                } catch (Exception e) {
                }
            }
            return 1;
        }
    }

    public static class 고정엔피시삭제 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "삭제할 엔피시 코드를 입력해주세요.");
                return 0;
            }
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                con = DatabaseConnection.getConnection();
                if (npc != null && !npc.getName().equals("MISSINGNO")) {
                    ps = con.prepareStatement("SELECT * FROM spawnnpc WHERE mapid = ? && npcid = ?");
                    ps.setInt(1, c.getPlayer().getMapId());
                    ps.setInt(2, npcId);
                    rs = ps.executeQuery();
                    if (!rs.next()) {
                        c.getPlayer().dropMessage(5, "현재 맵에는 설치한 NPC가 없습니다.");
                        return 0;
                    }
                    ps.close();
                    rs.close();
                    c.getPlayer().getMap().removeMapObject(npc);
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeNPC(npcId));
                    c.getPlayer().getMap().removeNpc(npcId);
                } else {
                    c.getPlayer().dropMessage(5, "WZ에 존재하지 않는 NPC를 입력했습니다.");
                    return 0;
                }
                ps = con.prepareStatement("DELETE FROM spawnnpc WHERE mapid = ? && npcid = ?");
                ps.setInt(1, c.getPlayer().getMapId());
                ps.setInt(2, npcId);
                ps.executeUpdate();
                ps.close();
                con.close();
                c.getPlayer().dropMessage(6, "엔피시를 삭제하였습니다.");
            } catch (Exception e) {
                System.err.println("fix npc error.");
                e.printStackTrace();
            } finally {
                try {
                    if (con != null) {
                        con.close();
                    }
                    if (ps != null) {
                        ps.close();
                    }
                    if (rs != null) {
                        rs.close();
                    }
                } catch (Exception e) {
                }
            }
            return 1;
        }
    }

    public static class MakePNPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                c.getPlayer().dropMessage(6, "Making playerNPC...");
                MapleCharacter chhr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (chhr == null) {
                    c.getPlayer().dropMessage(6, splitted[1] + " is not online");
                    return 0;
                }
                PlayerNPC npc = new PlayerNPC(chhr, Integer.parseInt(splitted[2]), c.getPlayer().getMap(), c.getPlayer());
                npc.addToServer();
                c.getPlayer().dropMessage(6, "Done");
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "NPC failed... : " + e.getMessage());
                e.printStackTrace();
            }
            return 1;
        }
    }

    public static class MakeOfflineP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "Cannot run command.");
            return 1;
        }
//            try {
//                c.getPlayer().dropMessage(6, "Making playerNPC...");
//                MapleClient cs = new MapleClient(null, 0, false);
//                MapleCharacter chhr = MapleCharacter.loadCharFromDB(MapleCharacterUtil.getIdByName(splitted[1]), cs, false);
//                if (chhr == null) {
//                    c.getPlayer().dropMessage(6, splitted[1] + " does not exist");
//                    return 0;
//                }
//                PlayerNPC npc = new PlayerNPC(chhr, Integer.parseInt(splitted[2]), c.getPlayer().getMap(), c.getPlayer());
//                npc.addToServer();
//                c.getPlayer().dropMessage(6, "Done");
//            } catch (Exception e) {
//                c.getPlayer().dropMessage(6, "NPC failed... : " + e.getMessage());
//                e.printStackTrace();
//            }
//            return 1;
//        }
    }

    public static class DestroyPNPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                c.getPlayer().dropMessage(6, "Destroying playerNPC...");
                final MapleNPC npc = c.getPlayer().getMap().getNPCByOid(Integer.parseInt(splitted[1]));
                if (npc instanceof PlayerNPC) {
                    ((PlayerNPC) npc).destroy(true);
                    c.getPlayer().dropMessage(6, "Done");
                } else {
                    c.getPlayer().dropMessage(6, "!destroypnpc [objectid]");
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "NPC failed... : " + e.getMessage());
                e.printStackTrace();
            }
            return 1;
        }
    }

    public static class 서버메세지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            String outputMessage = StringUtil.joinStringFrom(splitted, 1);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                cserv.setServerMessage(outputMessage);
            }
            return 1;
        }
    }

    public static class 스폰 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int mid = Integer.parseInt(splitted[1]);
            final int num = Math.min(CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1), 1000);
            Integer level = CommandProcessorUtil.getNamedIntArg(splitted, 1, "lvl");
            Long hp = CommandProcessorUtil.getNamedLongArg(splitted, 1, "hp");
            Integer exp = CommandProcessorUtil.getNamedIntArg(splitted, 1, "exp");
            Double php = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "php");
            Double pexp = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "pexp");

            int flag = 0;
            for (String str : splitted) {
                if (str.equalsIgnoreCase("핑쿠")) {
                    flag |= 0x1;
                }
                if (str.equalsIgnoreCase("블쿠")) {
                    flag |= 0x2;
                }
                if (str.equalsIgnoreCase("사랑의의자")) {
                    flag |= 0x4;
                }
                if (str.equalsIgnoreCase("릴렉스체어")) {
                    flag |= 0x8;
                }
                if (str.equalsIgnoreCase("주황버섯")) {
                    flag |= 0x10;
                }
                if (str.equalsIgnoreCase("리본돼지")) {
                    flag |= 0x20;
                }
                if (str.equalsIgnoreCase("그레이")) {
                    flag |= 0x40;
                }
                if (str.equalsIgnoreCase("칠판")) {
                    flag |= 0x80;
                }
            }

            MapleMonster onemob;
            try {
                onemob = MapleLifeFactory.getMonster(mid);
            } catch (RuntimeException e) {
                c.getPlayer().dropMessage(5, "Error: " + e.getMessage());
                return 0;
            }
            if (onemob == null) {
                c.getPlayer().dropMessage(5, "존재하지 않는 몬스터입니다.");
                return 0;
            }
            long newhp = 0;
            int newexp = 0;
            if (hp != null) {
                newhp = hp.longValue();
            } else if (php != null) {
                newhp = (long) (onemob.getMobMaxHp() * (php.doubleValue() / 100));
            } else {
                newhp = onemob.getMobMaxHp();
            }
            if (exp != null) {
                newexp = exp.intValue();
            } else if (pexp != null) {
                newexp = (int) (onemob.getMobExp() * (pexp.doubleValue() / 100));
            } else {
                newexp = onemob.getMobExp();
            }
            if (newhp < 1) {
                newhp = 1;
            }

            final OverrideMonsterStats overrideStats = new OverrideMonsterStats(newhp, onemob.getMobMaxMp(), newexp, false);
            for (int i = 0; i < num; i++) {
                MapleMonster mob = MapleLifeFactory.getMonster(mid);
                mob.setEventDropFlag(flag);
                mob.setHp(newhp);
                mob.setOverrideStats(overrideStats);

                c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, c.getPlayer().getTruePosition());
            }
            return 1;
        }
    }

    public static class 이벤트스폰 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int mid = Integer.parseInt(splitted[1]);
            final int num = CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            Integer level = CommandProcessorUtil.getNamedIntArg(splitted, 1, "lvl");
            Long hp = CommandProcessorUtil.getNamedLongArg(splitted, 1, "hp");
            Integer exp = CommandProcessorUtil.getNamedIntArg(splitted, 1, "exp");
            Double php = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "php");
            Double pexp = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "pexp");

            int flag = 0;
            for (String str : splitted) {
                if (str.equalsIgnoreCase("핑쿠")) {
                    flag |= 0x1;
                }
                if (str.equalsIgnoreCase("블쿠")) {
                    flag |= 0x2;
                }
                if (str.equalsIgnoreCase("사랑의의자")) {
                    flag |= 0x4;
                }
                if (str.equalsIgnoreCase("릴렉스체어")) {
                    flag |= 0x8;
                }
                if (str.equalsIgnoreCase("주황버섯")) {
                    flag |= 0x10;
                }
                if (str.equalsIgnoreCase("리본돼지")) {
                    flag |= 0x20;
                }
                if (str.equalsIgnoreCase("그레이")) {
                    flag |= 0x40;
                }
                if (str.equalsIgnoreCase("칠판")) {
                    flag |= 0x80;
                }
            }

            MapleMonster onemob;
            try {
                onemob = MapleLifeFactory.getMonster(mid);
            } catch (RuntimeException e) {
                c.getPlayer().dropMessage(5, "Error: " + e.getMessage());
                return 0;
            }
            if (onemob == null) {
                c.getPlayer().dropMessage(5, "Mob does not exist");
                return 0;
            }
            long newhp = 0;
            int newexp = 0;
            if (hp != null) {
                newhp = hp.longValue();
            } else if (php != null) {
                newhp = (long) (onemob.getMobMaxHp() * (php.doubleValue() / 100));
            } else {
                newhp = onemob.getMobMaxHp();
            }
            if (exp != null) {
                newexp = exp.intValue();
            } else if (pexp != null) {
                newexp = (int) (onemob.getMobExp() * (pexp.doubleValue() / 100));
            } else {
                newexp = onemob.getMobExp();
            }
            if (newhp < 1) {
                newhp = 1;
            }

            final OverrideMonsterStats overrideStats = new OverrideMonsterStats(newhp, onemob.getMobMaxMp(), newexp, false);
            MapleMonster mob = MapleLifeFactory.getMonster(mid);
            mob.setEventDropFlag(flag);
            mob.setOverrideStats(overrideStats);
            mob.setHp(num);
            c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, c.getPlayer().getPosition());
            return 1;
        }
    }

    public static class PS extends CommandExecute {

        protected static StringBuilder builder = new StringBuilder();

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (builder.length() > 1) {
                c.getSession().write(MaplePacketCreator.getPacketFromHexString(builder.toString()));
                builder = new StringBuilder();
            } else {
                c.getPlayer().dropMessage(6, "Please enter packet data!");
            }
            return 1;
        }
    }

    public static class APS extends PS {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                builder.append(StringUtil.joinStringFrom(splitted, 1));
                c.getPlayer().dropMessage(6, "String is now: " + builder.toString());
            } else {
                c.getPlayer().dropMessage(6, "Please enter packet data!");
            }
            return 1;
        }
    }

    public static class CPS extends PS {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            builder = new StringBuilder();
            return 1;
        }
    }

    public static class P extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                c.getSession().write(MaplePacketCreator.getPacketFromHexString(StringUtil.joinStringFrom(splitted, 1)));
            } else {
                c.getPlayer().dropMessage(6, "Please enter packet data!");
            }
            return 1;
        }
    }

    public static class 리로드맵 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int mapId = Integer.parseInt(splitted[1]);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                if (cserv.getMapFactory().isMapLoaded(mapId) && cserv.getMapFactory().getMap(mapId).getCharactersSize() > 0) {
                    c.getPlayer().dropMessage(5, "There exists characters on channel " + cserv.getChannel());
                    return 0;
                }
            }
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                if (cserv.getMapFactory().isMapLoaded(mapId)) {
                    cserv.getMapFactory().removeMap(mapId);
                }
            }
            return 1;
        }
    }

    public static class Respawn extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().respawn(true);
            return 1;
        }
    }

    public abstract static class TestTimer extends CommandExecute {

        protected Timer toTest = null;

        @Override
        public int execute(final MapleClient c, String[] splitted) {
            final int sec = Integer.parseInt(splitted[1]);
            c.getPlayer().dropMessage(5, "Message will pop up in " + sec + " seconds.");
            c.getPlayer().dropMessage(5, "Active: " + toTest.getSES().getActiveCount() + " Core: " + toTest.getSES().getCorePoolSize() + " Largest: " + toTest.getSES().getLargestPoolSize() + " Max: " + toTest.getSES().getMaximumPoolSize() + " Current: " + toTest.getSES().getPoolSize() + " Status: " + toTest.getSES().isShutdown() + toTest.getSES().isTerminated() + toTest.getSES().isTerminating());
            final long oldMillis = System.currentTimeMillis();
            toTest.schedule(new Runnable() {
                public void run() {
                    c.getPlayer().dropMessage(5, "Message has popped up in " + ((System.currentTimeMillis() - oldMillis) / 1000) + " seconds, expected was " + sec + " seconds");
                    c.getPlayer().dropMessage(5, "Active: " + toTest.getSES().getActiveCount() + " Core: " + toTest.getSES().getCorePoolSize() + " Largest: " + toTest.getSES().getLargestPoolSize() + " Max: " + toTest.getSES().getMaximumPoolSize() + " Current: " + toTest.getSES().getPoolSize() + " Status: " + toTest.getSES().isShutdown() + toTest.getSES().isTerminated() + toTest.getSES().isTerminating());
                }
            }, sec * 1000);
            return 1;
        }
    }

    public static class TestEventTimer extends TestTimer {

        public TestEventTimer() {
            toTest = EventTimer.getInstance();
        }
    }

    public static class TestCloneTimer extends TestTimer {

        public TestCloneTimer() {
            toTest = CloneTimer.getInstance();
        }
    }

    public static class TestEtcTimer extends TestTimer {

        public TestEtcTimer() {
            toTest = EtcTimer.getInstance();
        }
    }

    public static class TestMapTimer extends TestTimer {

        public TestMapTimer() {
            toTest = MapTimer.getInstance();
        }
    }

    public static class TestWorldTimer extends TestTimer {

        public TestWorldTimer() {
            toTest = WorldTimer.getInstance();
        }
    }

    public static class TestBuffTimer extends TestTimer {

        public TestBuffTimer() {
            toTest = BuffTimer.getInstance();
        }
    }

    public static class Crash extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null && c.getPlayer().getGMLevel() >= victim.getGMLevel()) {
                victim.getClient().getSession().write(HexTool.getByteArrayFromHexString("1A 00")); //give_buff with no data :D
                return 1;
            } else {
                c.getPlayer().dropMessage(6, "대상을 찾을 수 없습니다.");
                return 0;
            }
        }
    }

    public static class Subcategory extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setSubcategory(Byte.parseByte(splitted[1]));
            return 1;
        }
    }

    public static class 메소 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().gainMeso(Integer.MAX_VALUE - c.getPlayer().getMeso(), true);
            return 1;
        }
    }

    public static class 캐시 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) { //비리가 가능한 캐시지급!!!!!!!!!!!!! 지우면 3대가 고자
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "기본 문법: !캐시 <숫자>");
                return 0;
            }
            if (splitted.length == 3) {
                int va = 0;
                try {
                    va = Integer.parseInt(splitted[2]);
                } catch (Exception e) {
                    c.getPlayer().dropMessage(5, "기본 문법: !캐시 <캐릭터> <숫자>");
                    return 0;
                }
                MapleCharacter player = null;
                for (int i = 1; i <= ChannelServer.getChannelCount(); i++) {
                    for (MapleCharacter other : ChannelServer.getInstance(i).getPlayerStorage().getAllCharacters()) {
                        if (other != null && other.getName().equals(splitted[1])) {
                            player = other;
                        }
                    }
                }
                if (player != null) {
                    player.modifyCSPoints(1, va, false);
                    if (player.getId() != c.getPlayer().getId()) {
                        player.dropMessage(6, "" + c.getPlayer().getBanJum((long) va) + " 캐시를 지급받았습니다.");
                    }
                    c.getPlayer().dropMessage(6, splitted[1] + " 님에게 " + c.getPlayer().getBanJum((long) va) + " 캐시를 지급하였습니다.");
                    c.getPlayer().dropMessage(6, splitted[1] + " 님의 캐시가 " + c.getPlayer().getBanJum((long) player.getCSPoints(1)) + " 캐시가 되었습니다.");
                    return 0;
                } else {
                    c.getPlayer().dropMessage(6, splitted[1] + " 님은 접속 중이지 않습니다.");
                    return 0;
                }
            } else {
                int va = 0;
                try {
                    va = Integer.parseInt(splitted[1]);
                } catch (Exception e) {
                    c.getPlayer().dropMessage(5, "기본 문법: !캐시 <숫자>");
                    return 0;
                }
                c.getPlayer().modifyCSPoints(1, Integer.parseInt(splitted[1]), false);
                c.getPlayer().dropMessage(6, c.getPlayer().getBanJum((long) va) + " 캐시를 획득하였습니다. (잔량 : " + c.getPlayer().getCSPoints(1) + ")");
            }
            return 1;
        }
    }

    public static class 메이플포인트 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "획득량이 정의되지 않았습니다.");
                return 0;
            }
            c.getPlayer().modifyCSPoints(2, Integer.parseInt(splitted[1]), true);
            return 1;
        }
    }

    public static class 본섭캐시 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "획득량이 정의되지 않았습니다.");
                return 0;
            }
            c.getPlayer().modifyCSPoints(3, Integer.parseInt(splitted[1]), true);
            return 1;
        }
    }

    public static class 옵코드리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            SendPacketOpcode.reloadValues();
            RecvPacketOpcode.reloadValues();
            c.getPlayer().dropMessage(6, "완료되었습니다.");
            return 1;
        }
    }

    public static class 드롭리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMonsterInformationProvider.getInstance().clearDrops();
            ReactorScriptManager.getInstance().clearDrops();
            c.getPlayer().dropMessage(6, "완료되었습니다.");
            return 1;
        }
    }

    public static class 포탈리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            PortalScriptManager.getInstance().clearScripts();
            c.getPlayer().dropMessage(6, "완료되었습니다.");
            return 1;
        }
    }

    public static class 캐시샵리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            CashItemFactory.getInstance().initialize();
            CashItemSaleRank.cleanUp();
            c.getPlayer().dropMessage(6, "완료되었습니다.");
            return 1;
        }
    }

    public static class 채널추가 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "!채널추가 채널넘버 (순서대로 늘리는걸 추천)");
                return 0;
            }
            try {
                ChannelServer.startChannel(Integer.parseInt(splitted[1]));
            } catch (Exception e) {
                throw new RuntimeException();
            }
            c.getPlayer().dropMessage(6, "완료되었습니다.");
            return 1;
        }
    }

    public static class 스킬리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            SkillFactory.load();
            c.getPlayer().dropMessage(6, "완료되었습니다.");
            return 1;
        }
    }

    public static class 상점리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleShopFactory.getInstance().clear();
            c.getPlayer().dropMessage(6, "완료되었습니다.");
            return 1;
        }
    }

    public static class 이벤트리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer instance : ChannelServer.getAllInstances()) {
                instance.reloadEvents();
            }
            return 1;
        }
    }

    public static class 맵리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetFully();
            c.getPlayer().dropMessage(6, "완료되었습니다.");
            return 1;
        }
    }

    public static class 퀘스트포기 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                MapleQuest.getInstance(Integer.parseInt(splitted[1])).forfeit(c.getPlayer());
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "사용법: !퀘스트완료 <코드> <커스텀 값>");
            }
            return 1;
        }
    }

    public static class 퀘스트시작 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
//                if (c.getPlayer().getQuestStatus(Integer.parseInt(splitted[1])) >= 1) {
//                    c.getPlayer().dropMessage(5, "이미 시작/완료된 퀘스트입니다.");
//                    return 1;
//                }
                if (splitted.length < 3) {
                    MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceStart(c.getPlayer(), 0, null);
                } else {
                    MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceStart(c.getPlayer(), 0, splitted[2]);
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "사용법: !퀘스트완료 <코드> <커스텀 값>");
            }
            return 1;
        }
    }

    public static class 트라이퀘스트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                int questid = Integer.parseInt(splitted[1]);
                if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
                    c.getPlayer().dropMessage(5, "이 퀘스트는 존재하지 않거나 파티퀘스트가 아닙니다.");
                    return 1;
                }
                c.getPlayer().tryPartyQuest(questid);
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "사용법: !트라이퀘스트 <코드>");
            }
            return 1;
        }
    }

    public static class 앤드퀘스트 extends SuperGMCommand.엔드퀘스트 {
    }

    public static class 엔드퀘스트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                int questid = Integer.parseInt(splitted[1]);
                if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
                    c.getPlayer().dropMessage(5, "이 퀘스트는 존재하지 않거나 파티퀘스트가 아닙니다.");
                    return 1;
                }
                int count = 1;
                try {
                    count = Integer.parseInt(splitted[3]);
                } catch (Exception e) {
                }
                for (int i = 0; i < count; i++) {
                    c.getPlayer().endPartyQuest(questid, Integer.parseInt(splitted[2]));
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "사용법: !엔드이퀘스트 <코드> <선택:1(승), 2(패), 3(무)><커스텀 값>");
            }
            return 1;
        }
    }

    public static class 퀘스트완료 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                if (c.getPlayer().getQuestStatus(Integer.parseInt(splitted[1])) == 2) {
                    c.getPlayer().dropMessage(5, "이미 완료된 퀘스트입니다.");
                    return 1;
                }
                c.getPlayer().forceCompleteQuest(Integer.parseInt(splitted[1]));
                c.getSession().write(MaplePacketCreator.showSpecialEffect(11)); // Quest completion
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showSpecialEffect(c.getPlayer().getId(), 11), false);
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "사용법: !퀘스트완료 <코드>");
            }
            return 1;
        }
    }

    public static class ResetQuest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forfeit(c.getPlayer());
            return 1;
        }
    }

    public static class StartQuest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).start(c.getPlayer(), Integer.parseInt(splitted[2]));
            return 1;
        }
    }

    public static class CompleteQuest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).complete(c.getPlayer(), Integer.parseInt(splitted[2]), Integer.parseInt(splitted[3]));
            return 1;
        }
    }

    public static class FStartQuest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceStart(c.getPlayer(), Integer.parseInt(splitted[2]), splitted.length >= 4 ? splitted[3] : null);
            return 1;
        }
    }

    public static class FCompleteQuest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceComplete(c.getPlayer(), Integer.parseInt(splitted[2]));
            return 1;
        }
    }

    public static class killQuestMob extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuestStatus q = c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(Integer.parseInt(splitted[1])));
            if (q == null) {
                c.getPlayer().dropMessage(6, "NULL QUEST");
                return 1;
            }
            for (int i = 0; i < Integer.parseInt(splitted[3]); ++i) {
                c.getPlayer().mobKilled(i, i);
                q.mobKilled(Integer.parseInt(splitted[2]), splitted.length >= 5 ? Integer.parseInt(splitted[4]) : 0);
            }
            c.getSession().write(MaplePacketCreator.updateQuestMobKills(q));
            if (q.getQuest().canComplete(c.getPlayer(), null)) {
                c.getSession().write(MaplePacketCreator.getShowQuestCompletion(q.getQuest().getId()));
            }

            return 1;
        }
    }

    public static class killMobCount extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (int i = 0; i < Integer.parseInt(splitted[2]); ++i) {
                c.getPlayer().mobKilled(Integer.parseInt(splitted[1]), splitted.length >= 4 ? Integer.parseInt(splitted[3]) : 0);
            }
            return 1;
        }
    }

    public static class HReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).hitReactor(c);
            return 1;
        }
    }

    public static class FHReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).forceHitReactor(Byte.parseByte(splitted[2]));
            return 1;
        }
    }

    public static class DReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            List<MapleMapObject> reactors = map.getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.REACTOR));
            if (splitted[1].equals("all")) {
                for (MapleMapObject reactorL : reactors) {
                    MapleReactor reactor2l = (MapleReactor) reactorL;
                    c.getPlayer().getMap().destroyReactor(reactor2l.getObjectId());
                }
            } else {
                c.getPlayer().getMap().destroyReactor(Integer.parseInt(splitted[1]));
            }
            return 1;
        }
    }

    public static class SetReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().setReactorState(Byte.parseByte(splitted[1]));
            return 1;
        }
    }

    public static class 리액터리셋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetReactors();
            c.getPlayer().dropMessage(6, "완료 되었습니다.");
            return 1;
        }
    }

    public static class SendAllNote extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            if (splitted.length >= 1) {
                String text = StringUtil.joinStringFrom(splitted, 1);
                for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                    c.getPlayer().sendNote(mch.getName(), text);
                }
            } else {
                c.getPlayer().dropMessage(6, "Use it like this, !sendallnote <text>");
                return 0;
            }
            return 1;
        }
    }

    public static class DC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[splitted.length - 1]);
            if (victim != null && c.getPlayer().getGMLevel() >= victim.getGMLevel()) {
                victim.getClient().sclose();
                return 1;
            } else {
                c.getPlayer().dropMessage(6, "The victim does not exist.");
                return 0;
            }
        }
    }

    public static class BuffSkill extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            SkillFactory.getSkill(Integer.parseInt(splitted[1])).getEffect(Integer.parseInt(splitted[2])).applyTo(c.getPlayer());
            return 0;
        }
    }

    public static class BuffItem extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleItemInformationProvider.getInstance().getItemEffect(Integer.parseInt(splitted[1])).applyTo(c.getPlayer());
            return 0;
        }
    }

    public static class BuffItemEX extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleItemInformationProvider.getInstance().getItemEffectEX(Integer.parseInt(splitted[1])).applyTo(c.getPlayer());
            return 0;
        }
    }

    public static class ItemSize extends CommandExecute { //test

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "Number of items: " + MapleItemInformationProvider.getInstance().getAllItems().size());
            return 0;
        }
    }

    public static class BlockMobAdd extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().setMobGen(Integer.parseInt(splitted[1]), false);
            c.getPlayer().dropMessage(5, "Added Blocked Mob");
            return 0;
        }
    }

    public static class BlockMobRemove extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().setMobGen(Integer.parseInt(splitted[1]), true);
            c.getPlayer().dropMessage(5, "Removed Blocked Mob");
            return 0;
        }
    }

    public static class BlockMobList extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, " === Blocked Mob Gen === ");
            for (int z : c.getPlayer().getMap().getBlockedMobs()) {
                c.getPlayer().dropMessage(6, z + "");
            }
            return 0;
        }
    }

    public static class SBuff extends StrongGmBuff {
    }

    public static class StrongGmBuff extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().toggleStrongBuff();
            c.getPlayer().dropMessage(6, "Current Using Strong Gm Buff : " + c.getPlayer().isStrongBuff());
            return 1;
        }
    }

    public static class ChangeMapMusic extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 0) {
                String splitString = StringUtil.joinStringFrom(splitted, 1);
                c.getPlayer().getMap().changeMusic(splitString);
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange(splitString));
            } else {
                c.getPlayer().getMap().changeMusic("");
            }
            return 1;
        }
    }

    public static class SetMapTimer extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                if (splitted.length > 1) {
                    c.getPlayer().getMap().setCommandTimer(System.currentTimeMillis() + (Long.parseLong(splitted[1]) * 1000));
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getClock(Integer.parseInt(splitted[1])));
                } else {
                    c.getPlayer().getMap().setCommandTimer(System.currentTimeMillis());
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, e.toString());
            }
            return 1;
        }
    }

    public static class ExecuteOnTimeEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            OnTimeGiver.giveOnTimeBonus();
            return 1;
        }
    }

    public static class 아이템지급 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) {
                c.getPlayer().dropMessage(5, "사용법: !아이템지급 <캐릭터이름, 아이템코드, 수량, 메시지>");
                return 1;
            }
            int cid = MapleCharacterUtil.getIdByName(splitted[1]);
            final int item = Integer.parseInt(splitted[2]);
            final int q = Integer.parseInt(splitted[3]);
            int channel = World.Find.findChannel(cid);
            if (channel >= 0) {
                World.Broadcast.sendPacket(cid, MaplePacketCreator.sendDuey((byte) 28, null, null));
                World.Broadcast.sendPacket(cid, MaplePacketCreator.serverNotice(5, "아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!"));
            }
            DueyHandler.addNewItemToDb(item, q, cid, c.getPlayer().getName(), splitted[4], channel >= 0);
            return 1;
        }
    }

    public abstract static class HideMobInternal extends CommandExecute {

        protected MapleCharacter chr = null;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            for (MapleMonster mob : map.getAllMonstersThreadsafe()) {
                c.sendPacket(MobPacket.killMonster(mob.getObjectId(), 1));
            }
            return 1;
        }
    }

    public static class 핫타임 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int item = Integer.parseInt(splitted[1]);
            final int q = Integer.parseInt(splitted[2]);
            OnTimeGiver.Hottimes((int) item, (short) q);
            return 1;
        }
    }

    public static class FakeHiredMerchant extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            MapleCharacter chr = c.getPlayer();
            boolean canOpen = true;
            for (MapleMapObject shop : chr.getMap().getAllShopsThreadsafe()) {
                if (shop.getPosition().distanceSq(chr.getTruePosition()) < 15000) {
                    chr.dropMessage(5, "열 수 없는 지역!");
                    canOpen = false;
                    break;
                }
            }
            if (!canOpen) {
                return 0;
            }
            HiredMerchant merch = new HiredMerchant(c.getPlayer(), 5030000, "FakeShop");
            merch.setPosition(c.getPlayer().getPosition());
            map.addMapObject(merch);
            merch.setAvailable(true);
            merch.setOpen(true);
//            map.broadcastMessage(PlayerShopPacket.spawnFakeHiredMerchant(merch));
            return 0;
        }
    }

    public static class 뮤직플레이어 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptInvoker.runNpc(c, 1052015, 0);
            return 1;
        }
    }

    public static class 몬스터습격시작 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            switch (c.getPlayer().getMapId()) {
                case 100000000: //헤네시스
                case 101000000: //엘리니아
                case 102000000: //페리온
                case 103000000: //커닝시티
                case 104000000: //리스항구
                case 200000000: //오르비스
                case 220000000: //루디브리엄
                case 222000000: //아랫마을
                case 910000000: //자유시장
                    break;
                default:
                    c.getPlayer().dropMessage(5, "현재 맵에선 몬스터 습격이 불가능합니다.");
                    return 0;
            }
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            List<Integer> mons = new ArrayList<Integer>();
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT * FROM drop_data WHERE itemid = ?");
                ps.setInt(1, 4031282);
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (!mons.contains(rs.getInt(2))) {
                        mons.add(rs.getInt(2));
                    }
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "에러발생");
            } finally {
                try {
                    if (con != null) {
                        con.close();
                    }
                    if (ps != null) {
                        ps.close();
                    }
                    if (rs != null) {
                        rs.close();
                    }
                } catch (Exception e) {
                }
            }
            if (mons.size() < 1) {
                c.getPlayer().dropMessage(6, "악의 기운을 드롭하는 몬스터가 없는 것 같습니다.");
                return 1;
            }
            MapleMonster Mob = null;
            MaplePortal randPortal = null;
            int qty = 1, count = 0;
            for (int i = 0; i < mons.size(); i++) {
                Mob = MapleLifeFactory.getMonster(mons.get(i));
                if (Mob.getStats().getLevel() < 30) {
                    qty = 15;
                } else if (Mob.getStats().getLevel() < 50) {
                    qty = 10;
                } else if (Mob.getStats().getLevel() < 70) {
                    qty = 5;
                } else {
                    qty = 1;
                }
                for (int j = 0; j < qty; j++) {
                    randPortal = c.getPlayer().getMap().getPortal((int) (Math.random() * c.getPlayer().getMap().getPortals().size()));
                    if (randPortal == null) {
                        j--;
                        continue;
                    }
                    try {
                        c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mons.get(i)), new Point(randPortal.getPosition().x, randPortal.getPosition().y));
                    } catch (Exception e) {
                        j--;
                        continue;
                    }
                    count++;
                }
            }
            //World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[몬스터습격] " + mons.size() + "종의 몬스터(총 " + count + "마리)가 " + c.getPlayer().getMap().getMapName() + "을(를) 습격했습니다."));
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[공지] 몬스터들이 마을을 침략합니다아아아아아~~ 꼭 마을을 무사히 구해주세요ㅜㅜ"));
            return 0;//
        }
    }

    public static class 몬스터습격준비 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[공지] 안뇽 칭구들~~~ 봉인에서 풀려난 몬스터들이 " + c.getPlayer().getMap().getMapName() + "를 침략하려고 해!! 모두 " + c.getPlayer().getMap().getMapName() + "로 모여서 (할 수 있을 리가 없지만) 몬스터를 퇴치해줘~~~"));
            return 1;
        }
    }

    public static class 몬스터습격종료 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;

            if (splitted.length > 1) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "존재하지 않는 맵입니다.");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (!mob.getStats().isBoss() || mob.getStats().isPartyBonus() || c.getPlayer().isGM()) {
                    map.killMonster(mob, c.getPlayer(), false, false, (byte) 1);
                }
            }
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "[공지] 몬스터의 침략으로부터 (너희들 말고 이리스가) 무사히 마을을 구했습니다. '악의 기운'을 획득하신 분은 각 마을의 운영자 NPC를 찾아가시기 바랍니다."));
            return 1;
        }
    }

    public static class 패킷출력r extends SuperGMCommand.패킷보기r {
    }

    public static class 패킷보기r extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (MapleClient.showp) {
                c.getPlayer().dropMessage(5, "리시브 패킷 출력을 중지합니다.");
            } else {
                c.getPlayer().dropMessage(6, "리시브 패킷 출력을 시작합니다.");
            }
            MapleClient.showp = !MapleClient.showp;
            return 0;
        }
    }

    public static class 패킷출력s extends SuperGMCommand.패킷보기s {
    }

    public static class 패킷보기s extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (MaplePacketLittleEndianWriter.showp) {
                c.getPlayer().dropMessage(5, "센드 패킷 출력을 중지합니다.");
            } else {
                c.getPlayer().dropMessage(6, "센드 패킷 출력을 시작합니다.");
            }
            MaplePacketLittleEndianWriter.showp = !MaplePacketLittleEndianWriter.showp;
            return 0;
        }
    }

    public static class 퀘스트아이템 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "사용법: !퀘스트아이템 <퀘스트코드>");
                return 1;
            }
            try {
                //c.getPlayer().mobKilled(Integer.parseInt(splitted[1]), 0);
                if (c.getPlayer().getQuestStatus(Integer.parseInt(splitted[1])) == 1) {
                    final MapleQuest q = MapleQuest.getInstance(Integer.parseInt(splitted[1]));
                    q.gmQuest(c.getPlayer(), 0);
                } else {
                    c.getPlayer().dropMessage(5, "입력한 퀘스트는 진행중이지 않는 것 같습니다.");
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "사용법: !퀘스트아이템 <퀘스트코드>");
            }
            return 1;
        }
    }

    public static class 버닝 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "사용법: !버닝 (추가)경험치배율 (추가)드롭배율 (추가)메소배율 (분단위)시간");
                return 1;
            }
            try {
                c.startBurning(Byte.parseByte(splitted[1]), Byte.parseByte(splitted[2]), Byte.parseByte(splitted[3]), Integer.parseInt(splitted[4]), false);
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "사용법: !버닝 (추가)경험치배율 (추가)드롭배율 (추가)메소배율 (분단위)시간");
            }
            return 1;
        }
    }

    public static class 캐시템옵션로그 extends CommandExecute {

        // 이 소스의 equipStats 키 네이밍에 맞춘 옵션 키
        private static final String[] OPTION_KEYS = {
            "STR", "DEX", "INT", "LUK",
            "MHP", "MMP",
            "PAD", "MAD",
            "PDD", "MDD",
            "ACC", "EVA",
            "Speed", "Jump"
        };

        private static boolean hasAnyOption(final Map<String, Integer> stats) {
            if (stats == null) {
                return false;
            }

            for (String k : OPTION_KEYS) {
                Integer v = stats.get(k);
                if (v != null && v.intValue() != 0) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

            int scanned = 0;
            int equipCount = 0;
            int cashEquipCount = 0;
            int cashEquipWithOptionCount = 0;

            final StringBuilder sb = new StringBuilder(1024 * 256);

            for (ItemInformation info : ii.getAllItems()) {
                scanned++;
                if (info == null) {
                    continue;
                }

                final int itemId = info.itemId;

                // 장비만
                if (!GameConstants.isEquip(itemId)) {
                    continue;
                }
                equipCount++;

                final Map<String, Integer> stats = info.equipStats;
                if (stats == null) {
                    continue;
                }

                // WZ cash == 1 인 장비만
                final Integer cash = stats.get("cash");
                if (cash == null || cash.intValue() != 1) {
                    continue;
                }
                cashEquipCount++;

                // 옵션이 하나라도 붙은 장비만
                if (!hasAnyOption(stats)) {
                    continue;
                }
                cashEquipWithOptionCount++;

                final String name = (info.name != null ? info.name : ii.getName(itemId));
                sb.append(itemId)
                        .append(" : ")
                        .append(name == null ? "NO-NAME" : name)
                        .append("\r\n");
            }

            final String outFile = "log/Log_CashEquipOptions.txt";
            final String header
                    = "scanned=" + scanned
                    + ", equip=" + equipCount
                    + ", cashEquip(cash=1)=" + cashEquipCount
                    + ", cashEquipWithOption=" + cashEquipWithOptionCount
                    + "\r\n\r\n";

            FileoutputUtil.log(outFile, header + sb.toString());

            c.getPlayer().dropMessage(6,
                    "완료: 전체 " + scanned
                    + ", 장비 " + equipCount
                    + ", cash=1 장비 " + cashEquipCount
                    + ", 옵션장비 " + cashEquipWithOptionCount
                    + " (로그: " + outFile + ")");

            return 1;
        }
    }

    public static class 버닝종료 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.startBurning((byte) 0, (byte) 0, (byte) 0, 0, true);
            return 1;
        }
    }
}
