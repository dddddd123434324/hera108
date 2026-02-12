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
package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.Skill;
import client.SkillFactory;
import client.inventory.*;
import constants.GameConstants;
import handling.world.MaplePartyCharacter;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.RateManager;
import server.life.MapleMonster;
import server.log.LogType;
import server.log.ServerLogger;
import server.maps.FieldLimitType;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.movement.LifeMovementFragment;
import tools.MaplePacketCreator;
import tools.data.LittleEndianAccessor;
import tools.packet.PetPacket;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import server.movement.AbsoluteLifeMovement;
import server.quest.MapleQuest;

public class PetHandler {

    public static final void SpawnPet(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        chr.updateTick(slea.readInt());
        chr.spawnPet(slea.readByte(), slea.readByte() > 0);
    }

    public static void Pet_AutoBuff(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        int petid = slea.readInt();
        MaplePet pet = chr.getPet(petid);
        if ((chr == null) || (chr.getMap() == null) || (pet == null)) {
            return;
        }
        int skillId = slea.readInt();
        if (skillId == 1111002) {
            chr.dropMessage(1, "이 스킬은 등록할 수 없습니다.");
            return;
        }
        Skill buffId = SkillFactory.getSkill(skillId);
        /*if (petid == 0) {
         chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.PET_SKILL1)).setCustomData(String.valueOf(skillId));
         } else if (petid == 1) {
         chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.PET_SKILL1)).setCustomData(String.valueOf(skillId));
         } else if (petid == 2) {
         chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.PET_SKILL1)).setCustomData(String.valueOf(skillId));
         }
         //        chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.HP_ITEM)).setCustomData(String.valueOf(data));*/
        if ((chr.getSkillLevel(buffId) > 0) || (skillId == 0)) {
            pet.setBuffSkill(skillId);
            c.getSession().write(PetPacket.updatePet(pet, chr.getInventory(MapleInventoryType.CASH).getItem((short) (byte) pet.getInventoryPosition()), true));
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void Pet_AutoPotion(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        slea.skip(1);
        chr.updateTick(slea.readInt());
        final short slot = slea.readShort();
        if (chr == null || !chr.isAlive() || chr.getMap() == null) {
            return;
        }
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);

        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != slea.readInt()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        boolean oneCube = false;
        switch (toUse.getItemId()) {
            case 2000024:
            case 2000025:
            case 2000026:
            case 2000027:
                oneCube = true;
                break;
        }
        final long time = System.currentTimeMillis();
        if (!oneCube && chr.getMap().getConsumeItemCoolTime() <= 0 && chr.getNextConsume() > time) {
            chr.dropMessage(5, "아직 아이템을 사용할 수 없습니다.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) { //cwk quick hack
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                if (chr.getMap().getConsumeItemCoolTime() > 0) {
                    chr.setNextConsume(time + (chr.getMap().getConsumeItemCoolTime() * 1000));
                }
            }
        } else {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static final void PetChat(final int petid, final short command, final String text, MapleCharacter chr, final MapleClient c) {
        if (chr == null || chr.getMap() == null || chr.getPet(0) == null) {
            return;
        }
        if (!chr.getCanTalk()) {
            chr.getClient().sendPacket(MaplePacketCreator.yellowChat("대화 금지 상태이므로 채팅이 불가능합니다."));
            return;
        }
        ServerLogger.getInstance().logChat(LogType.Chat.Pet, c.getPlayer().getId(), c.getPlayer().getName(), text, c.getPlayer().getMap().getStreetName() + " - " + c.getPlayer().getMap().getMapName() + " (" + c.getPlayer().getMap().getId() + ")");
        chr.getMap().broadcastMessage(chr, PetPacket.petChat(chr.getId(), command, text, (byte) petid), true);
    }

    public static final void PetCommand(final LittleEndianAccessor slea, final MapleClient c) {//여긴 나중에 확인
        final MapleCharacter chr = c.getPlayer();
        boolean addProb = slea.readByte() != 0;
        MaplePet pet = c.getPlayer().getPet(0);
        if (pet == null) {
            return;
        }
        byte d = slea.readByte();
        byte petIndex = (byte) chr.getPetIndex(pet);
        PetCommand petCommand = PetDataFactory.getPetCommand(pet.getPetItemId(), d);

        if (petCommand == null) {
            chr.getMap().broadcastMessage(chr, PetPacket.commandResponse(chr.getId(), (byte) d, (byte) petIndex, false, false), true);
            return;
        }
        boolean success = false;
        if (Randomizer.rand(0, 100) <= petCommand.getProbability() + (addProb ? 10 : 0)) {
            success = true;
            if (pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + (RateManager.TRAIT * petCommand.getIncrease());
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                    pet.setLevel(pet.getLevel() + 1);
                    c.getSession().write(PetPacket.showOwnPetLevelUp(petIndex));
                    chr.getMap().broadcastMessage(PetPacket.showPetLevelUp(chr, petIndex));
                }
                c.getSession().write(PetPacket.updatePet(pet, chr.getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
            }
        }
        chr.getMap().broadcastMessage(chr, PetPacket.commandResponse(chr.getId(), (byte) petCommand.getSkillId(), petIndex, success, false), true);
    }

    public static final void PetFood(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        int previousFullness = 100;
        MaplePet pet = null;
        if (chr == null) {
            return;
        }
        for (final MaplePet pets : chr.getPets()) {
            if (pets.getSummoned()) {
                if (pets.getFullness() <= previousFullness) {
                    previousFullness = pets.getFullness();
                    pet = pets;
                }
            }
        }
        if (pet == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        c.getPlayer().updateTick(slea.readInt());
        short slot = slea.readShort();
        final int itemId = slea.readInt();
        Item petFood = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (petFood == null || petFood.getItemId() != itemId || petFood.getQuantity() <= 0 || itemId / 10000 != 212) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        boolean gainCloseness = false;

        if (Randomizer.nextInt(99) <= 50) {
            gainCloseness = true;
        }
        if (pet.getFullness() < 100) {
            int newFullness = pet.getFullness() + 30;
            if (newFullness > 100) {
                newFullness = 100;
            }
            pet.setFullness(newFullness);
            final byte index = chr.getPetIndex(pet);

            if (gainCloseness && pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + RateManager.TRAIT;
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                    pet.setLevel(pet.getLevel() + 1);

                    c.getSession().write(PetPacket.showOwnPetLevelUp(index));
                    chr.getMap().broadcastMessage(PetPacket.showPetLevelUp(chr, index));
                }
            }
            c.getSession().write(PetPacket.updatePet(pet, chr.getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
            chr.getMap().broadcastMessage(c.getPlayer(), PetPacket.commandResponse(chr.getId(), (byte) 1, index, true, true), true);
        } else {
            if (gainCloseness) {
                int newCloseness = pet.getCloseness() - RateManager.TRAIT;
                if (newCloseness < 0) {
                    newCloseness = 0;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness < GameConstants.getClosenessNeededForLevel(pet.getLevel())) {
                    pet.setLevel(pet.getLevel() - 1);
                }
            }
            c.getSession().write(PetPacket.updatePet(pet, chr.getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
            chr.getMap().broadcastMessage(chr, PetPacket.commandResponse(chr.getId(), (byte) 0, chr.getPetIndex(pet), false, true), true);
        }
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, true, false);
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void PetExceptionPickup(final LittleEndianAccessor slea, final MapleCharacter chr) {
        MaplePet pet = chr.getPet(slea.readInt());
        if (pet != null && pet.getSummoned()) {
            pet.getPickupExceptionList().clear();
            short size = slea.readByte();
            for (int i = 0; i < size; ++i) {
                pet.getPickupExceptionList().add(slea.readInt());
            }
            pet.changeException();
            pet.saveToDb();
        }
    }

    public static final void MovePet(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int petId = slea.readInt();
        if (chr == null) {
            return;
        }
        if (chr.getChangeTime() + 1000 > System.currentTimeMillis() || chr.isMovePlayerFucking()) {
            return;
        }
        final Point pos = slea.readPos();
        int donno = slea.readInt();
        final List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 3);
        if (res != null && chr != null && res.size() != 0 && chr.getMap() != null) { // map crash hack
            final MaplePet pet = chr.getPet(petId);
            if (pet == null) {
                return;
            }
            for (final LifeMovementFragment move : res) {
                if (move instanceof AbsoluteLifeMovement) {
                    move.getUnk();
                    pet.setFh(move.getUnk());
                }
            }
            pet.setPos(pos);
            chr.getMap().broadcastMessage(chr, PetPacket.movePet(chr.getId(), pet.getUniqueId(), (byte) petId, res, pos), false);
            //chr.getMap().broadcastMessage(chr, PetPacket.movePet(chr.getId(), res, pet.getPos()), false);
            pet.updatePosition(res);
            if (chr.hasBlockedInventory() || chr.getStat().pickupRange <= 0.0) {
                return;
            }
            chr.setScrolledPosition((short) 0);
            List<MapleMapObject> objects = chr.getMap().getMapObjectsInRange(chr.getTruePosition(), chr.getRange(), Arrays.asList(MapleMapObjectType.ITEM));
            for (LifeMovementFragment move : res) {
                final Point pp = move.getPosition();
                boolean foundItem = false;
                for (MapleMapObject mapitemz : objects) {
                    if (mapitemz instanceof MapleMapItem && (Math.abs(pp.x - mapitemz.getTruePosition().x) <= chr.getStat().pickupRange || Math.abs(mapitemz.getTruePosition().x - pp.x) <= chr.getStat().pickupRange) && (Math.abs(pp.y - mapitemz.getTruePosition().y) <= chr.getStat().pickupRange || Math.abs(mapitemz.getTruePosition().y - pp.y) <= chr.getStat().pickupRange)) {
                        final MapleMapItem mapitem = (MapleMapItem) mapitemz;
                        final Lock lock = mapitem.getLock();
                        lock.lock();
                        try {
                            if (mapitem.isPickedUp()) {
                                continue;
                            }
                            if (mapitem.getQuest() > 0 && chr.getQuestStatus(mapitem.getQuest()) != 1) {
                                continue;
                            }
                            if (mapitem.getOwner() != chr.getId() && mapitem.isPlayerDrop()) {
                                continue;
                            }
                            if (mapitem.getOwner() != chr.getId() && ((!mapitem.isPlayerDrop() && mapitem.getDropType() == 0) || (mapitem.isPlayerDrop() && chr.getMap().getEverlast()))) {
                                continue;
                            }
                            if (!mapitem.isPlayerDrop() && (mapitem.getDropType() == 1 || mapitem.getDropType() == 3) && mapitem.getOwner() != chr.getId()) {
                                continue;
                            }
                            if (mapitem.getDropType() == 2 && mapitem.getOwner() != chr.getId()) {
                                continue;
                            }
                            if (mapitem.getMeso() > 0) {
                                if (chr.getParty() != null && mapitem.getOwner() != chr.getId()) {
                                    final List<MapleCharacter> toGive = new LinkedList<MapleCharacter>();
                                    final int splitMeso = mapitem.getMeso() * 40 / 100;
                                    for (MaplePartyCharacter z : chr.getParty().getMembers()) {
                                        MapleCharacter m = chr.getMap().getCharacterById(z.getId());
                                        if (m != null && m.getId() != chr.getId()) {
                                            toGive.add(m);
                                        }
                                    }
                                    for (final MapleCharacter m : toGive) {
                                        m.gainMeso(splitMeso / toGive.size(), true, true);
                                    }
                                    chr.gainMeso(mapitem.getMeso() - splitMeso, true, true);
                                } else {
                                    chr.gainMeso(mapitem.getMeso(), true, true);
                                }
                                InventoryHandler.removeItem_Pet(chr, mapitem, petId);
                                foundItem = true;
                            } else if (!MapleItemInformationProvider.getInstance().isPickupBlocked(mapitem.getItem().getItemId()) && mapitem.getItem().getItemId() / 10000 != 291) {
                                if (InventoryHandler.useItem(chr.getClient(), mapitem.getItemId())) {
                                    InventoryHandler.removeItem_Pet(chr, mapitem, petId);
                                } else if (MapleInventoryManipulator.checkSpace(chr.getClient(), mapitem.getItem().getItemId(), mapitem.getItem().getQuantity(), mapitem.getItem().getOwner())) {
                                    if (mapitem.getItem().getQuantity() >= 50 && mapitem.getItem().getItemId() == 2340000) {
                                        chr.getClient().setMonitored(true); //hack check
                                    }
                                    if (MapleInventoryManipulator.addFromDrop(chr.getClient(), mapitem.getItem(), true, mapitem.getDropper() instanceof MapleMonster)) {
                                        InventoryHandler.removeItem_Pet(chr, mapitem, petId);
                                        foundItem = true;
                                    }
                                }
                            }
                        } finally {
                            lock.unlock();
                        }
                    }
                }
                if (foundItem) {
                    return;
                }
            }
        }
    }
}
