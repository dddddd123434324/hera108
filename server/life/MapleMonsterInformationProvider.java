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
package server.life;

import client.inventory.MapleInventoryType;
import constants.GameConstants;
import database.DatabaseConnection;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapleMonsterInformationProvider {

    private static final MapleMonsterInformationProvider instance = new MapleMonsterInformationProvider();
    private final Map<Integer, ArrayList<MonsterDropEntry>> drops = new HashMap<Integer, ArrayList<MonsterDropEntry>>();
    private final List<MonsterGlobalDropEntry> globaldrops = new ArrayList<MonsterGlobalDropEntry>();
    private static final MapleDataProvider stringDataWZ = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/String.wz"));
    private static MapleData mobStringData;

    public static MapleMonsterInformationProvider getInstance() {
        return instance;
    }

    public List<MonsterGlobalDropEntry> getGlobalDrop() {
        return globaldrops;
    }

    public void load() {
        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection con = null;

        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM drop_data_global WHERE chance > 0");
            rs = ps.executeQuery();

            while (rs.next()) {
                globaldrops.add(
                        new MonsterGlobalDropEntry(
                                rs.getInt("itemid"),
                                rs.getInt("chance"),
                                rs.getInt("continent"),
                                rs.getByte("dropType"),
                                rs.getInt("minimum_quantity"),
                                rs.getInt("maximum_quantity"),
                                rs.getInt("questid")));
            }
            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT dropperid FROM drop_data");
            List<Integer> mobIds = new ArrayList<Integer>();
            rs = ps.executeQuery();
            while (rs.next()) {
                if (!mobIds.contains(rs.getInt("dropperid"))) {
                    loadDrop(rs.getInt("dropperid"));
                    mobIds.add(rs.getInt("dropperid"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving drop" + e);
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
    }

    public ArrayList<MonsterDropEntry> retrieveDrop(final int monsterId) {
        return drops.get(Integer.valueOf(monsterId));
    }

    private void loadDrop(final int monsterId) {
        final ArrayList<MonsterDropEntry> ret = new ArrayList<MonsterDropEntry>();

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            final MapleMonsterStats mons = MapleLifeFactory.getMonsterStats(monsterId);
            if (mons == null) {
                return;
            }
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM drop_data WHERE dropperid = ?");
            ps.setInt(1, monsterId);
            rs = ps.executeQuery();
            int itemid;
            int chance;
            int miq;
            int mxq;
            boolean doneMesos = false;
            while (rs.next()) {
                itemid = rs.getInt("itemid");
                chance = rs.getInt("chance");
                miq = rs.getInt("minimum_quantity");
                mxq = rs.getInt("maximum_quantity");
                boolean isBoss = MapleLifeFactory.getMonsterStats(monsterId).isBoss();
                if (GameConstants.getInventoryType(itemid) == MapleInventoryType.EQUIP) {
                    if (monsterId >= 9500100 && monsterId <= 9500142) {
                        chance *= 10; //습격 이벤트 몬스터는 장비 드롭률 10배
                    } else {
                        if (isBoss) {
                            chance *= 1; //줌서 확률 조금 업업
                        } else {
                            chance *= 20; //일반몹 장비확률은 올려도 될듯
                        }
                    }
                }
                if (itemid >= 2000000 && itemid < 2000004 || itemid >= 2000006 && itemid < 2000011 || itemid == 2022003) { //하급물약중 하드코딩된 몹들을 위해
                    if (!isBoss && chance < 50000) {
                        chance = 100000;
                        miq = 1;
                        mxq = 10;
                    }
                    if (itemid == 2000002 && mons.getLevel() >= 80) { //하얀포션
                        itemid = 2022003;
                    }
                }
                if (itemid / 10000 == 229) {//마북
                    if (isBoss) {
                        chance *= 1; //보스는 냅두고
                    } else {
                        chance = 150; //일반몹 마북 100으로 통일
                    }
                }
                if (itemid == 2049301 && !isBoss) {
                    chance /= 10;
                }
                /*if (GameConstants.isThrowingStar(itemid)) { //머쉬맘같은 몹이 일비 잘줘버림
                    if (isBoss) {
                        chance *= 0.7; //표창 확률 살짝 낮춤
                    }
                }*/
                if (GameConstants.isUpgradeScroll(itemid)) {
                    chance *= 3; //줌서 확률 조금 업업
                }
                ret.add(new MonsterDropEntry(
                        monsterId,
                        itemid,
                        chance,
                        miq,
                        mxq,
                        rs.getInt("questid")));
                if (itemid == 0) {
                    doneMesos = true;
                }
            }
            if (!doneMesos) {
                addMeso(monsterId, mons, ret);
            }

        } catch (SQLException e) {
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
        drops.put(Integer.valueOf(monsterId), ret);
    }

    public void addMeso(int monsterId, MapleMonsterStats mons, ArrayList<MonsterDropEntry> ret) {
        final double divided = (mons.getLevel() < 100 ? (mons.getLevel() < 10 ? (double) mons.getLevel() : 10.0) : (mons.getLevel() / 10.0));
        final int max = mons.isBoss() && !mons.isPartyBonus() ? (mons.getLevel() * mons.getLevel()) : (mons.getLevel() * (int) Math.ceil(mons.getLevel() / divided));
        for (int i = 0; i < mons.dropsMeso(); i++) {
            ret.add(new MonsterDropEntry(monsterId, 0, mons.isBoss() && !mons.isPartyBonus() ? 1000000 : (mons.isPartyBonus() ? 100000 : 200000), (int) Math.floor(0.66 * max), max, 0));
        }
    }

    public void clearDrops() {
        drops.clear();
        globaldrops.clear();
        load();
//        addExtra();
    }

    public boolean contains(ArrayList<MonsterDropEntry> e, int toAdd) {
        for (MonsterDropEntry f : e) {
            if (f.itemId == toAdd) {
                return true;
            }
        }
        return false;
    }

    public int chanceLogic(int itemId) { //not much logic in here. most of the drops should already be there anyway.
        if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
            return 50000; //with *10
        } else if (GameConstants.getInventoryType(itemId) == MapleInventoryType.SETUP || GameConstants.getInventoryType(itemId) == MapleInventoryType.CASH) {
            return 500;
        } else {
            switch (itemId / 10000) {
                case 204:
                case 207:
                case 233:
                case 229:
                    return 500;
                case 401:
                case 402:
                    return 5000;
                case 403:
                    return 5000; //lol
            }
            return 20000;
        }
    }
    //MESO DROP: level * (level / 10) = max, min = 0.66 * max
    //explosive Reward = 7 meso drops
    //boss, ffaloot = 2 meso drops
    //boss = level * level = max
    //no mesos if: mobid / 100000 == 97 or 95 or 93 or 91 or 90 or removeAfter > 0 or invincible or onlyNormalAttack or friendly or dropitemperiod > 0 or cp > 0 or point > 0 or fixeddamage > 0 or selfd > 0 or mobType != null and mobType.charat(0) == 7 or PDRate <= 0
}
