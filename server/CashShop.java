/*
 This file is part of the ZeroFusion MapleStory Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>
 ZeroFusion organized by "RMZero213" <RMZero213@hotmail.com>

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

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.*;
import constants.GameConstants;
import database.DatabaseConnection;
import tools.FileoutputUtil;
import tools.Pair;
import tools.packet.CSPacket;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CashShop implements Serializable {

    private static final long serialVersionUID = 231541893513373579L;
    private int accountId, characterId;
    private ItemLoader factory = ItemLoader.CASHSHOP;
    private List<Item> inventory = new ArrayList<Item>();
    private List<Integer> uniqueids = new ArrayList<Integer>();

    public CashShop(int accountId, int characterId, int jobType) throws Exception {
        this.accountId = accountId;
        this.characterId = characterId;
        for (Pair<Item, MapleInventoryType> item : factory.loadItems(false, accountId).values()) {
            inventory.add(item.getLeft());
        }
    }

    public int getItemsSize() {
        return inventory.size();
    }

    public List<Item> getInventory() {
        return inventory;
    }

    public Item findByCashId(int cashId) {
        for (Item item : inventory) {
            if (item.getUniqueId() == cashId) {
                return item;
            }
        }

        return null;
    }

    public void checkExpire(MapleClient c) {
        List<Item> toberemove = new ArrayList<Item>();
        for (Item item : inventory) {
            if (item != null && !GameConstants.isPet(item.getItemId()) && item.getExpiration() > 0 && item.getExpiration() < System.currentTimeMillis()) {
                toberemove.add(item);
            }
        }
        if (toberemove.size() > 0) {
            for (Item item : toberemove) {
                removeFromInventory(item);
                c.getSession().write(CSPacket.cashItemExpired(item.getUniqueId()));
            }
            toberemove.clear();
        }
    }

    public Item toItem(CashItemInfo cItem, boolean donate) {
        return toItem(cItem, MapleInventoryManipulator.getUniqueId(cItem.getId(), null), "", donate);
    }

    public Item toItem(CashItemInfo cItem, String gift, boolean donate) {
        return toItem(cItem, MapleInventoryManipulator.getUniqueId(cItem.getId(), null), gift, donate);
    }

    public Item toItem(CashItemInfo cItem, int uniqueid, boolean donate) {
        return toItem(cItem, uniqueid, "", donate);
    }

    public Item toItem(CashItemInfo cItem, int uniqueid, String gift, boolean donate) {
        /*if (uniqueid <= 0) {
            uniqueid = MapleInventoryIdentifier.getInstance();
        }*/
        long period = cItem.getPeriod();
        if (GameConstants.isPet(cItem.getId())) {
            period = 90; //펫 90일
        }
        if (period == 365) {
            if (!donate) {
                period = 7;
            }
        }
        if (period <= 0 && !(cItem.getId() >= 1802000 && cItem.getId() < 1803000)) { //펫장비 외 캐시 장비에 기간제 설정 영구제로 정상화
            if (!donate) {
                period = 0;
            }
        }
        if (cItem.getId() >= 5000100 && cItem.getId() < 5000200) { //기간제 없는 펫
            period = 20000;
        }
        Item ret = null;
        if (GameConstants.getInventoryType(cItem.getId()) == MapleInventoryType.EQUIP) {
            Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(cItem.getId(), uniqueid);
            if (period > 0) {
                eq.setExpiration((long) (System.currentTimeMillis() + (long) (period * 24 * 60 * 60 * 1000))); // 캐시템 기간제한
            }
            eq.setGMLog("Cash Shop: " + cItem.getSN() + " on " + FileoutputUtil.CurrentReadable_Date());
            eq.setGiftFrom(gift);
            if (GameConstants.isEffectRing(cItem.getId()) && uniqueid > 0) {
                MapleRing ring = MapleRing.loadFromDb(uniqueid);
                if (ring != null) {
                    eq.setRing(ring);
                }
            }
            ret = eq.copy();
        } else {
            Item item = new Item(cItem.getId(), (byte) 0, (short) cItem.getCount(), (byte) 0, uniqueid);
            if (period > 0) {
                item.setExpiration((long) (System.currentTimeMillis() + (long) (period * 24 * 60 * 60 * 1000)));
            }
            item.setGMLog("Cash Shop: " + cItem.getSN() + " on " + FileoutputUtil.CurrentReadable_Date());
            item.setGiftFrom(gift);
            if (GameConstants.isPet(cItem.getId())) {
                final MaplePet pet = MaplePet.createPet(cItem.getId(), uniqueid);
                if (pet != null) {
                    item.setPet(pet);
                }
            }
            ret = item.copy();
        }
        return ret;
    }

    public void addToInventory(Item item) {
        inventory.add(item);
    }

    public void removeFromInventory(Item item) {
        inventory.remove(item);
    }

    public void gift(int recipient, String from, String message, int sn) {
        gift(recipient, from, message, sn, 0);
    }

    public void gift(int recipient, String from, String message, int sn, int uniqueid) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("INSERT INTO `gifts` VALUES (DEFAULT, ?, ?, ?, ?, ?)");
            ps.setInt(1, recipient);
            ps.setString(2, from);
            ps.setString(3, message);
            ps.setInt(4, sn);
            ps.setInt(5, uniqueid);
            ps.executeUpdate();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
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

    public boolean giftMax(int recipientid) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int max = 0, maxvalue = 15;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM `gifts` WHERE `recipient` = ?");
            ps.setInt(1, recipientid);
            rs = ps.executeQuery();
            while (rs.next()) {
                max++;
            }
            if (max >= maxvalue) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
        }
        return true;
    }

    public static int giftCount(int recipient) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT COUNT(1) FROM `gifts` WHERE `recipient` = ?");
            ps.setInt(1, recipient);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0;
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
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
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
        }
        return 0;
    }

    public List<Pair<Item, String>> loadGifts(boolean donate) {
        List<Pair<Item, String>> gifts = new ArrayList<Pair<Item, String>>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM `gifts` WHERE `recipient` = ?");
            ps.setInt(1, characterId);
            rs = ps.executeQuery();

            while (rs.next()) {
                CashItemInfo cItem = CashItemFactory.getInstance().getItem(rs.getInt("sn"), donate);
                if (cItem == null) {
                    continue;
                }
                Item item = toItem(cItem, rs.getInt("uniqueid"), rs.getString("from"), donate);
                gifts.add(new Pair<Item, String>(item, rs.getString("message")));
//                uniqueids.add(item.getUniqueId());
                List<Integer> packages = CashItemFactory.getInstance().getPackageItems(cItem.getId());
                if (packages != null && packages.size() > 0) {
                    for (int packageItem : packages) {
                        CashItemInfo pack = CashItemFactory.getInstance().getSimpleItem(packageItem);
                        if (pack != null) {
                            addToInventory(toItem(pack, rs.getString("from"), donate));
                        }
                    }
                } else {
                    addToInventory(item);
                }
            }

            rs.close();
            ps.close();
            ps = con.prepareStatement("DELETE FROM `gifts` WHERE `recipient` = ?");
            ps.setInt(1, characterId);
            ps.executeUpdate();
            save(con);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
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
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
        }
        return gifts;
    }

    public boolean canSendNote(int uniqueid) {
        return uniqueids.contains(uniqueid);
    }

    public void sendedNote(int uniqueid) {
        for (int i = 0; i < uniqueids.size(); i++) {
            if (uniqueids.get(i).intValue() == uniqueid) {
                uniqueids.remove(i);
            }
        }
    }

    public void save(Connection con) throws SQLException {
        List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<Pair<Item, MapleInventoryType>>();

        for (Item item : inventory) {
            itemsWithType.add(new Pair<Item, MapleInventoryType>(item, GameConstants.getInventoryType(item.getItemId())));
        }

        factory.saveItems(itemsWithType, con, accountId);
    }
}
