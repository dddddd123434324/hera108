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

import client.MapleClient;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import server.ItemMakerFactory;
import server.ItemMakerFactory.GemCreateEntry;
import server.ItemMakerFactory.ItemMakerCreateEntry;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.log.LogType;
import server.log.ServerLogger;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.LittleEndianAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemMakerHandler {

    public static final void ItemMaker(final LittleEndianAccessor slea, final MapleClient c) {
        //System.out.println(slea.toString()); //change?
        final int makerType = slea.readInt();

        switch (makerType) {
            case 1: { // Gem
                final int toCreate = slea.readInt();

                if (GameConstants.isGem(toCreate)) {
                    final GemCreateEntry gem = ItemMakerFactory.getInstance().getGemInfo(toCreate);
                    if (gem == null) {
                        return;
                    }
                    if (!hasSkill(c, gem.getReqSkillLevel())) {
                        return; // H4x
                    }
                    if (c.getPlayer().getMeso() < gem.getCost()) {
                        return; // H4x
                    }
                    final int randGemGiven = getRandomGem(gem.getRandomReward());

                    if (c.getPlayer().getInventory(GameConstants.getInventoryType(randGemGiven)).isFull()) {
                        return; // We'll do handling for this later
                    }
                    final int taken = checkRequiredNRemove(c, gem.getReqRecipes());
                    if (taken == 0) {
                        return; // We'll do handling for this later
                    }
                    int cost = (int) (gem.getCost() * 1.1);
                    if (c.getPlayer().getMeso() >= cost) {
                        c.getPlayer().gainMeso(-cost, false);
                    } else {
                        c.getPlayer().dropMessage(1, "메소가 " + (cost - c.getPlayer().getMeso()) + "메소 부족합니다.");
                        c.sendPacket(MaplePacketCreator.enableActions());
                        return;
                    }
                    final int quantity = taken == randGemGiven ? 9 : 1;
                    MapleInventoryManipulator.addById(c, randGemGiven, (byte) quantity, "Made by Gem " + toCreate + " on " + FileoutputUtil.CurrentReadable_Date()); // Gem is always 1
                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    ServerLogger.getInstance().logItem(LogType.Item.ItemMaker, c.getPlayer().getId(), c.getPlayer().getName(), randGemGiven, quantity, ii.getName(randGemGiven), 0, "보석 제작, 원래 아이템 : " + ii.getName(toCreate));

                    //c.getSession().write(MaplePacketCreator.getShowItemGain(randGemGiven, (short) 1, true));
                    c.getSession().write(MaplePacketCreator.showMesoGain(-cost, true));

                    c.getSession().write(MaplePacketCreator.Maker(true, randGemGiven, quantity));
                    c.getSession().write(MaplePacketCreator.ItemMaker_Success(true));
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.ItemMaker_Success_3rdParty(c.getPlayer().getId(), true), false);
                } else if (GameConstants.isOtherGem(toCreate)) {
                    //non-gems that are gems
                    //stim and numEnchanter always 0
                    final GemCreateEntry gem = ItemMakerFactory.getInstance().getGemInfo(toCreate);
                    if (gem == null) {
                        return;
                    }
                    if (!hasSkill(c, gem.getReqSkillLevel())) {
                        return; // H4x
                    }
                    if (c.getPlayer().getMeso() < gem.getCost()) {
                        return; // H4x
                    }

                    if (c.getPlayer().getInventory(GameConstants.getInventoryType(toCreate)).isFull()) {
                        return; // We'll do handling for this later
                    }
                    if (checkRequiredNRemove(c, gem.getReqRecipes()) == 0) {
                        return; // We'll do handling for this later
                    }
                    c.getPlayer().gainMeso(-gem.getCost(), false);
                    final int rewardAmount = gem.getRewardAmount();
                    if (GameConstants.getInventoryType(toCreate) == MapleInventoryType.EQUIP) {
                        MapleInventoryManipulator.addbyItem(c, MapleItemInformationProvider.getInstance().getEquipById(toCreate));
                    } else {
                        MapleInventoryManipulator.addById(c, toCreate, (short) rewardAmount, "Made by Gem " + toCreate + " on " + FileoutputUtil.CurrentReadable_Date()); // Gem is always 1
                    }
                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    ServerLogger.getInstance().logItem(LogType.Item.ItemMaker, c.getPlayer().getId(), c.getPlayer().getName(), toCreate, rewardAmount, ii.getName(toCreate), 0, "장비 제작");

                    c.getSession().write(MaplePacketCreator.Maker(true, toCreate, rewardAmount));
                    c.getSession().write(MaplePacketCreator.ItemMaker_Success(true));
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.ItemMaker_Success_3rdParty(c.getPlayer().getId(), true), false);
                } else {
                    final boolean stimulator = slea.readByte() > 0;
                    final int numEnchanter = slea.readInt();

                    final ItemMakerCreateEntry create = ItemMakerFactory.getInstance().getCreateInfo(toCreate);
                    if (create == null) {
                        return;
                    }
                    if (numEnchanter > create.getTUC()) {
                        return; // h4x
                    }
                    if (!hasSkill(c, create.getReqSkillLevel())) {
                        return; // H4x
                    }
                    if (c.getPlayer().getInventory(GameConstants.getInventoryType(toCreate)).isFull()) {
                        return; // We'll do handling for this later
                    }
                    if (checkRequiredNRemove(c, create.getReqItems()) == 0) {
                        return; // We'll do handling for this later
                    }
                    int totalCost = create.getCost();

                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    final Equip toGive = (Equip) ii.getEquipById(toCreate);
                    int baseCost = 0;
                    int reqLevel = ii.getReqLevel(toCreate);
                    baseCost = getEnchantAddCost(toCreate, reqLevel, baseCost);

                    int[] enchanters = new int[numEnchanter];
                    byte prop = 0;
                    boolean potential = false;
                    if (stimulator || numEnchanter > 0) {
                        for (int i = 0; i < numEnchanter; i++) {
                            enchanters[i] = slea.readInt();
                        }
                        for (int enchant : enchanters) {
                            int gemQuality = enchant % 10;
                            int enchantCost = 0;
                            if (gemQuality == 0)// 하급
                            {
                                enchantCost = baseCost * 1;
                            } else if (gemQuality == 1)// 중급
                            {
                                enchantCost = baseCost * 2;
                            } else if (gemQuality == 2)// 상급
                            {
                                enchantCost = baseCost * 3 + 1000;
                            }
                            totalCost += enchantCost;
                            switch (enchant) {
                                case 4251200:
                                    prop = 50;
                                    break;
                                case 4251201:
                                    prop = 60;
                                    break;
                                case 4251202:
                                    prop = 70;
                                    break;
                            }
                        }
                    }

                    if (c.getPlayer().getMeso() >= totalCost) {
                        //c.getPlayer().dropMessage(6, "템가격" + ii.getPrice(toCreate) + "Item해체 코스트 : " + totalCost + " / 아이템등급 : ");
                        c.getPlayer().gainMeso(-totalCost, false);
                    } else {
                        c.getPlayer().dropMessage(1, "메소가 " + (totalCost - c.getPlayer().getMeso()) + "메소 부족합니다.");
                        c.sendPacket(MaplePacketCreator.enableActions());
                        return;
                    }
                    if (stimulator || numEnchanter > 0) {
                        for (int enchant : enchanters) {
                            if (c.getPlayer().haveItem(enchant, 1, false, true)) {
                                final Map<String, Integer> stats = ii.getEquipStats(enchant);
                                if (stats != null) {
                                    addEnchantStats(stats, toGive);
                                    MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, enchant, 1, false, false);
                                }
                            }
                        }
                        if (c.getPlayer().haveItem(create.getStimulator(), 1, false, true)) {
                            ii.randomizeStats_Above(toGive);
                            MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, create.getStimulator(), 1, false, false);
                        }
                    }
                    boolean success;
                    int item;
                    boolean pung = false;
                    if (prop > 0 && Randomizer.nextInt(100) < prop) {
                        potential = true;
                    } else {
                        potential = false;
                        if (Randomizer.nextInt(100) < 50) {
                            pung = true;
                        }
                    }
                    if (!stimulator || Randomizer.nextInt(10) != 0) {
                        if (potential) {
                            toGive.resetPotential_Fuse(0);
                            MapleInventoryManipulator.addbyItem(c, toGive);
                        } else {
                            MapleInventoryManipulator.addbyItem(c, toGive);
                        }
                        ServerLogger.getInstance().logItem(LogType.Item.ItemMaker, c.getPlayer().getId(), c.getPlayer().getName(), toGive.getItemId(), 1, ii.getName(toGive.getItemId()), 0, "장비 제작");
                        success = true;
                        item = toGive.getItemId();
                    } else {
                        success = false;
                        item = 0;
                    }
                    c.getSession().write(MaplePacketCreator.ItemMaker_Success(success ? true : false));
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.ItemMaker_Success_3rdParty(c.getPlayer().getId(), success ? true : false), false);
                    c.getSession().write(MaplePacketCreator.Maker(success, item, 1));
                }
                break;
            }
            case 3: { // Making Crystals
                final int 재료템 = slea.readInt();//재료템
                final int 크리스탈 = getCreateCrystal(재료템);//재료템
                if (c.getPlayer().haveItem(재료템, 100, false, true)) {
                    MapleInventoryManipulator.addById(c, 크리스탈, (short) 1, "Made by Maker " + 재료템 + " on " + FileoutputUtil.CurrentReadable_Date());
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 재료템, 100, false, false);
                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    ServerLogger.getInstance().logItem(LogType.Item.ItemMaker, c.getPlayer().getId(), c.getPlayer().getName(), 크리스탈, 100, ii.getName(크리스탈), 0, "[몬스터결정 : " + 재료템 + "(" + ii.getName(재료템) + ")]");
                    c.getSession().write(MaplePacketCreator.Maker(크리스탈, 재료템));
                    c.getSession().write(MaplePacketCreator.ItemMaker_Success(true));
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.ItemMaker_Success_3rdParty(c.getPlayer().getId(), true), false);
                }
                break;
            }
            case 4: { // Disassembling EQ.
                final int itemId = slea.readInt();
                c.getPlayer().updateTick(slea.readInt());
                //slea.skip(4);
                final byte slot = (byte) slea.readInt();

                final Item toUse = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(slot);
                final ItemMakerCreateEntry create = ItemMakerFactory.getInstance().getCreateInfo(itemId);
                if (create == null || toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1) {
                    return;
                }
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                int equipQuality = GameConstants.getEquipQuality((Equip) toUse);
                int cost = getItemMakerDisassemblingCost(itemId, create.getCost(), ii.getReqLevel(itemId), equipQuality);

                if (c.getPlayer().getMeso() < cost) {
                    return; // H4x
                }

                if (!ii.isDropRestricted(itemId) && !ii.isAccountShared(itemId)) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.EQUIP, slot, (byte) 1, false);
                    final int[] toGive = getCrystal(itemId, ii.getReqLevel(itemId), equipQuality);
                    c.getPlayer().gainMeso(-cost, true, true, false);
                    ServerLogger.getInstance().logItem(LogType.Item.ItemMaker, c.getPlayer().getId(), c.getPlayer().getName(), toGive[0], toGive[1], ii.getName(toGive[0]), 0, "[장비해체 : " + itemId + "(" + ii.getName(itemId) + ")]");
                    MapleInventoryManipulator.addById(c, toGive[0], (byte) toGive[1], "Made by disassemble " + itemId + " on " + FileoutputUtil.CurrentReadable_Date());
                    c.getSession().write(MaplePacketCreator.Maker(itemId));
                    c.getSession().write(MaplePacketCreator.getShowItemGain(toGive[0], (short) toGive[1], true));
                    c.getSession().write(MaplePacketCreator.ItemMaker_Success(toGive[1] > 4 ? true : false));
                    c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.ItemMaker_Success_3rdParty(c.getPlayer().getId(), toGive[1] > 4 ? true : false), false);
                }
                break;
            }
        }
    }

    public static int getItemMakerDisassemblingCost(int itemId, int price, int reqLevel, int equipQual) {
        int v6 = 150;
        //60, 7000
        switch (equipQual) {
            case -1:
                v6 = 100;
                break;
            case 1:
                v6 = 200;
                break;
            case 2:
                v6 = 250;
                break;
            case 3:
                v6 = 300;
            case 4:
                v6 = 350;
            case 5:
                v6 = 400;
                break;
        }
        double v5 = price * (reqLevel <= 75 ? 0.5D : (reqLevel <= 100 ? 0.7D : 1D)) / 10;
        double result = v6 * v5 / 100 - (v6 * v5 / 100 % 1000 <= 0 ? 0 : v6 * v5 / 100 % 1000);
        return (int) result;
    }

    private static final int getCreateCrystal(final int etc) {
        int itemid;
        final short level = MapleItemInformationProvider.getInstance().getItemMakeLevel(etc);

        if (level >= 31 && level <= 50) {
            itemid = 4260000;
        } else if (level >= 51 && level <= 60) {
            itemid = 4260001;
        } else if (level >= 61 && level <= 70) {
            itemid = 4260002;
        } else if (level >= 71 && level <= 80) {
            itemid = 4260003;
        } else if (level >= 81 && level <= 90) {
            itemid = 4260004;
        } else if (level >= 91 && level <= 100) {
            itemid = 4260005;
        } else if (level >= 101 && level <= 110) {
            itemid = 4260006;
        } else if (level >= 111 && level <= 120) {
            itemid = 4260007;
        } else if (level >= 121) {
            itemid = 4260008;
        } else {
            throw new RuntimeException("Invalid Item Maker id");
        }
        return itemid;
    }

    private static final int[] getCrystal(final int itemid, final int level, int equipquality) {
        int[] all = new int[2];
        all[0] = -1;
        if (level >= 31 && level <= 50) {
            all[0] = 4260000;
        } else if (level >= 51 && level <= 60) {
            all[0] = 4260001;
        } else if (level >= 61 && level <= 70) {
            all[0] = 4260002;
        } else if (level >= 71 && level <= 80) {
            all[0] = 4260003;
        } else if (level >= 81 && level <= 90) {
            all[0] = 4260004;
        } else if (level >= 91 && level <= 100) {
            all[0] = 4260005;
        } else if (level >= 101 && level <= 110) {
            all[0] = 4260006;
        } else if (level >= 111 && level <= 120) {
            all[0] = 4260007;
        } else if (level >= 121 && level <= 200) {
            all[0] = 4260008;
        } else {
            throw new RuntimeException("Invalid Item Maker type" + level);
        }
        if (GameConstants.isWeapon(itemid) || GameConstants.isOverall(itemid)) {
            all[1] = Randomizer.rand(5, 11);
        } else {
            all[1] = Randomizer.rand(3, 7);
        }
        if (equipquality > 0) {
            all[1] += Randomizer.nextInt(equipquality) + 1;
        } else {
            all[1] -= Randomizer.nextInt((-equipquality) + 1);
        }

        return all;
    }

    private static final void addEnchantStats(final Map<String, Integer> stats, final Equip item) {
        Integer s = stats.get("PAD");
        if (s != null && s != 0) {
            item.setWatk((short) (item.getWatk() + s));
        }
        s = stats.get("MAD");
        if (s != null && s != 0) {
            item.setMatk((short) (item.getMatk() + s));
        }
        s = stats.get("ACC");
        if (s != null && s != 0) {
            item.setAcc((short) (item.getAcc() + s));
        }
        s = stats.get("EVA");
        if (s != null && s != 0) {
            item.setAvoid((short) (item.getAvoid() + s));
        }
        s = stats.get("Speed");
        if (s != null && s != 0) {
            item.setSpeed((short) (item.getSpeed() + s));
        }
        s = stats.get("Jump");
        if (s != null && s != 0) {
            item.setJump((short) (item.getJump() + s));
        }
        s = stats.get("MaxHP");
        if (s != null && s != 0) {
            item.setHp((short) (item.getHp() + s));
        }
        s = stats.get("MaxMP");
        if (s != null && s != 0) {
            item.setMp((short) (item.getMp() + s));
        }
        s = stats.get("STR");
        if (s != null && s != 0) {
            item.setStr((short) (item.getStr() + s));
        }
        s = stats.get("DEX");
        if (s != null && s != 0) {
            item.setDex((short) (item.getDex() + s));
        }
        s = stats.get("INT");
        if (s != null && s != 0) {
            item.setInt((short) (item.getInt() + s));
        }
        s = stats.get("LUK");
        if (s != null && s != 0) {
            item.setLuk((short) (item.getLuk() + s));
        }
        s = stats.get("randOption");
        if (s != null && s != 0) {
            final int ma = item.getMatk(), wa = item.getWatk();
            if (wa > 0) {
                item.setWatk((short) (Randomizer.nextBoolean() ? (wa + s) : (wa - s)));
            }
            if (ma > 0) {
                item.setMatk((short) (Randomizer.nextBoolean() ? (ma + s) : (ma - s)));
            }
        }
        s = stats.get("randStat");
        if (s != null && s != 0) {
            final int str = item.getStr(), dex = item.getDex(), luk = item.getLuk(), int_ = item.getInt();
            if (str > 0) {
                item.setStr((short) (Randomizer.nextBoolean() ? (str + s) : (str - s)));
            }
            if (dex > 0) {
                item.setDex((short) (Randomizer.nextBoolean() ? (dex + s) : (dex - s)));
            }
            if (int_ > 0) {
                item.setInt((short) (Randomizer.nextBoolean() ? (int_ + s) : (int_ - s)));
            }
            if (luk > 0) {
                item.setLuk((short) (Randomizer.nextBoolean() ? (luk + s) : (luk - s)));
            }
        }
    }

    private static final int getRandomGem(final List<Pair<Integer, Integer>> rewards) {
        int itemid;
        final List<Integer> items = new ArrayList<Integer>();

        for (final Pair p : rewards) {
            itemid = (Integer) p.getLeft();
            for (int i = 0; i < (Integer) p.getRight(); i++) {
                items.add(itemid);
            }
        }
        return items.get(Randomizer.nextInt(items.size()));
    }

    private static final int checkRequiredNRemove(final MapleClient c, final List<Pair<Integer, Integer>> recipe) {
        int itemid = 0;
        for (final Pair<Integer, Integer> p : recipe) {
            if (!c.getPlayer().haveItem(p.getLeft(), p.getRight(), false, true)) {
                return 0;
            }
        }
        for (final Pair<Integer, Integer> p : recipe) {
            itemid = p.getLeft();
            MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(itemid), itemid, p.getRight(), false, false);
        }
        return itemid;
    }

    private static final boolean hasSkill(final MapleClient c, final int reqlvl) {
        return c.getPlayer().getSkillLevel(SkillFactory.getSkill(c.getPlayer().getStat().getSkillByJob(1007, c.getPlayer().getJob()))) >= reqlvl;
    }

    private static int getEnchantAddCost(final int toCreate, int reqLevel, int baseCost) {
        switch (toCreate / 10000) {
            case 100: {
                if (reqLevel < 60) {
                    baseCost = 6000;
                } else if (reqLevel < 70) {
                    baseCost = 8000;
                } else if (reqLevel < 80) {
                    baseCost = 13000;
                } else if (reqLevel < 90) {
                    baseCost = 21000;
                } else if (reqLevel < 100) {
                    baseCost = 24000;
                } else if (reqLevel < 110) {
                    baseCost = 27000;
                } else if (reqLevel < 120) {
                    baseCost = 45000;
                } else {
                    baseCost = 51000;
                }
                break;
            }
            case 105: {
                if (reqLevel < 60) {
                    baseCost = 14000;
                } else if (reqLevel < 70) {
                    baseCost = 15000;
                } else if (reqLevel < 80) {
                    baseCost = 17000;
                } else if (reqLevel < 90) {
                    baseCost = 38000;
                } else if (reqLevel < 100) {
                    baseCost = 42000;
                } else if (reqLevel < 110) {
                    baseCost = 51000;
                } else if (reqLevel < 120) {
                    baseCost = 90000;
                } else {
                    baseCost = 108000;
                }
                break;
            }
            case 107: {
                if (reqLevel < 60) {
                    baseCost = 8000;
                } else if (reqLevel < 70) {
                    baseCost = 10000;
                } else if (reqLevel < 80) {
                    baseCost = 14000;
                } else if (reqLevel < 90) {
                    baseCost = 23000;
                } else if (reqLevel < 100) {
                    baseCost = 30000;
                } else if (reqLevel < 110) {
                    baseCost = 36000;
                } else if (reqLevel < 120) {
                    baseCost = 54000;
                } else {
                    baseCost = 60000;
                }
                break;
            }
            case 108: {
                if (reqLevel < 60) {
                    baseCost = 13000;
                } else if (reqLevel < 70) {
                    baseCost = 17000;
                } else if (reqLevel < 80) {
                    baseCost = 23000;
                } else if (reqLevel < 90) {
                    baseCost = 42000;
                } else if (reqLevel < 100) {
                    baseCost = 48000;
                } else if (reqLevel < 110) {
                    baseCost = 55000;
                } else if (reqLevel < 120) {
                    baseCost = 90000;
                } else if (reqLevel < 130) {
                    baseCost = 101000;
                }
                break;
            }
            case 109: {
                if (reqLevel < 130) {
                    baseCost = 105000;
                } else {
                    baseCost = 20000;
                }
                break;
            }
            default: {
                if (GameConstants.isWeapon(toCreate)) {
                    if (reqLevel < 60) {
                        baseCost = 20000;
                    } else if (reqLevel < 70) {
                        baseCost = 36000;
                    } else if (reqLevel < 80) {
                        baseCost = 45000;
                    } else if (reqLevel < 90) {
                        baseCost = 74000;
                    } else if (reqLevel < 100) {
                        baseCost = 82000;
                    } else if (reqLevel < 110) {
                        baseCost = 93000;
                    } else if (reqLevel < 120) {
                        baseCost = 153000;
                    } else {
                        baseCost = 168000;
                    }
                }
                break;
            }
        }
        return baseCost;
    }
}
