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
package client.inventory;

import client.MapleCharacter;
import constants.GameConstants;

import java.io.Serializable;
import server.Randomizer;

public class Equip extends Item implements Serializable {

    public static enum ScrollResult {

        SUCCESS, FAIL, CURSE
    }

    public static final int ARMOR_RATIO = 350000;
    public static final int WEAPON_RATIO = 700000;
    //charm: -1 = has not been initialized yet, 0 = already been worn, >0 = has teh charm exp
    private byte upgradeSlots = 0, level = 0, vicioushammer = 0, enhance = 0;
    private short str = 0, dex = 0, _int = 0, luk = 0, hp = 0, mp = 0, watk = 0, matk = 0, wdef = 0, mdef = 0, acc = 0, avoid = 0, hands = 0, speed = 0, jump = 0, hpR = 0, mpR = 0, charmExp = 0, pvpDamage = 0;
    private int itemEXP = 0, durability = -1, incSkill = -1, potential1 = 0, potential2 = 0, potential3 = 0, cubedC = 0;
    private long equippedTime = 0;

    public Equip(int id, short position, byte flag) {
        super(id, position, (short) 1, flag);
    }

    public Equip(int id, short position, int uniqueid, short flag) {
        super(id, position, (short) 1, flag, uniqueid);
    }

    @Override
    public Item copy() {
        Equip ret = new Equip(getItemId(), getPosition(), getUniqueId(), getFlag());
        ret.str = str;
        ret.dex = dex;
        ret._int = _int;
        ret.luk = luk;
        ret.hp = hp;
        ret.mp = mp;
        ret.matk = matk;
        ret.mdef = mdef;
        ret.watk = watk;
        ret.wdef = wdef;
        ret.acc = acc;
        ret.avoid = avoid;
        ret.hands = hands;
        ret.speed = speed;
        ret.jump = jump;
        ret.enhance = enhance;
        ret.upgradeSlots = upgradeSlots;
        ret.level = level;
        ret.itemEXP = itemEXP;
        ret.durability = durability;
        ret.vicioushammer = vicioushammer;
        ret.potential1 = potential1;
        ret.potential2 = potential2;
        ret.potential3 = potential3;
        ret.charmExp = charmExp;
        ret.pvpDamage = pvpDamage;
        ret.hpR = hpR;
        ret.mpR = mpR;
        ret.incSkill = incSkill;
        ret.equippedTime = equippedTime;
        ret.cubedC = cubedC;
        ret.setGiftFrom(getGiftFrom());
        ret.setOwner(getOwner());
        ret.setQuantity(getQuantity());
        ret.setExpiration(getExpiration());
        ret.setMarriageId(getMarriageId());
        return ret;
    }

    @Override
    public byte getType() {
        return 1;
    }

    @Override
    public long getEquippedTime() {
        return equippedTime;
    }

    @Override
    public void setEquippedTime(long equippedTime) {
        this.equippedTime = equippedTime;
    }

    public byte getUpgradeSlots() {
        return upgradeSlots;
    }

    public short getStr() {
        return str;
    }

    public short getDex() {
        return dex;
    }

    public short getInt() {
        return _int;
    }

    public short getLuk() {
        return luk;
    }

    public short getHp() {
        return hp;
    }

    public short getMp() {
        return mp;
    }

    public short getWatk() {
        return watk;
    }

    public short getMatk() {
        return matk;
    }

    public short getWdef() {
        return wdef;
    }

    public short getMdef() {
        return mdef;
    }

    public short getAcc() {
        return acc;
    }

    public short getAvoid() {
        return avoid;
    }

    public short getHands() {
        return hands;
    }

    public short getSpeed() {
        return speed;
    }

    public short getJump() {
        return jump;
    }

    public void setStr(short str) {
        if (str < 0) {
            str = 0;
        }
        this.str = str;
    }

    public void setDex(short dex) {
        if (dex < 0) {
            dex = 0;
        }
        this.dex = dex;
    }

    public void setInt(short _int) {
        if (_int < 0) {
            _int = 0;
        }
        this._int = _int;
    }

    public void setLuk(short luk) {
        if (luk < 0) {
            luk = 0;
        }
        this.luk = luk;
    }

    public void setHp(short hp) {
        if (hp < 0) {
            hp = 0;
        }
        this.hp = hp;
    }

    public void setMp(short mp) {
        if (mp < 0) {
            mp = 0;
        }
        this.mp = mp;
    }

    public void setWatk(short watk) {
        if (watk < 0) {
            watk = 0;
        }
        this.watk = watk;
    }

    public void setMatk(short matk) {
        if (matk < 0) {
            matk = 0;
        }
        this.matk = matk;
    }

    public void setWdef(short wdef) {
        if (wdef < 0) {
            wdef = 0;
        }
        this.wdef = wdef;
    }

    public void setMdef(short mdef) {
        if (mdef < 0) {
            mdef = 0;
        }
        this.mdef = mdef;
    }

    public void setAcc(short acc) {
        if (acc < 0) {
            acc = 0;
        }
        this.acc = acc;
    }

    public void setAvoid(short avoid) {
        if (avoid < 0) {
            avoid = 0;
        }
        this.avoid = avoid;
    }

    public void setHands(short hands) {
        if (hands < 0) {
            hands = 0;
        }
        this.hands = hands;
    }

    public void setSpeed(short speed) {
        if (speed < 0) {
            speed = 0;
        }
        this.speed = speed;
    }

    public void setJump(short jump) {
        if (jump < 0) {
            jump = 0;
        }
        this.jump = jump;
    }

    public void setUpgradeSlots(byte upgradeSlots) {
        this.upgradeSlots = upgradeSlots;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    public byte getViciousHammer() {
        return vicioushammer;
    }

    public void setViciousHammer(byte ham) {
        vicioushammer = ham;
    }

    public int getItemEXP() {
        return itemEXP;
    }

    public void setItemEXP(int itemEXP) {
        if (itemEXP < 0) {
            itemEXP = 0;
        }
        this.itemEXP = itemEXP;
    }

    public int getEquipExp() {
        if (itemEXP <= 0) {
            return 0;
        }
        return itemEXP;
    }

    public int getEquipExpForLevel() {
        if (getEquipExp() <= 0) {
            return 0;
        }
        int expz = getEquipExp();
        for (int i = getBaseLevel(); i <= GameConstants.getMaxLevel(getItemId()); i++) {
            if (expz >= GameConstants.getExpForLevel(i, getItemId())) {
                expz -= GameConstants.getExpForLevel(i, getItemId());
            } else { //for 0, dont continue;
                break;
            }
        }
        return expz;
    }

    public int getExpPercentage() {
        if (getEquipLevel() < getBaseLevel() || getEquipLevel() > GameConstants.getMaxLevel(getItemId()) || GameConstants.getExpForLevel(getEquipLevel(), getItemId()) <= 0) {
            return 0;
        }
        return getEquipExpForLevel() * 100 / GameConstants.getExpForLevel(getEquipLevel(), getItemId());
    }

    public int getEquipLevel() {
        if (GameConstants.getMaxLevel(getItemId()) <= 0) {
            return 0;
        } else if (getEquipExp() <= 0) {
            return getBaseLevel();
        }
        int levelz = getBaseLevel();
        int expz = getEquipExp();
        for (int i = levelz; (GameConstants.getStatFromWeapon(getItemId()) == null ? (i <= GameConstants.getMaxLevel(getItemId())) : (i < GameConstants.getMaxLevel(getItemId()))); i++) {
            if (expz >= GameConstants.getExpForLevel(i, getItemId())) {
                levelz++;
                expz -= GameConstants.getExpForLevel(i, getItemId());
            } else { //for 0, dont continue;
                break;
            }
        }
        return levelz;
    }

    public int getBaseLevel() {
        return (GameConstants.getStatFromWeapon(getItemId()) == null ? 1 : 0);
    }

    @Override
    public void setQuantity(short quantity) {
        if (quantity < 0 || quantity > 1) {
            throw new RuntimeException("Setting the quantity to " + quantity + " on an equip (itemid: " + getItemId() + ")");
        }
        super.setQuantity(quantity);
    }

    public int getDurability() {
        return durability;
    }

    public void setDurability(final int dur) {
        this.durability = dur;
    }

    public byte getEnhance() {
        return enhance;
    }

    public void setEnhance(final byte en) {
        this.enhance = en;
    }

    public int getPotential1() {
        return potential1;
    }

    public void setPotential1(final int en) {
        this.potential1 = en;
    }

    public int getPotential2() {
        return potential2;
    }

    public void setPotential2(final int en) {
        this.potential2 = en;
    }

    public int getPotential3() {
        return potential3;
    }

    public void setPotential3(final int en) {
        this.potential3 = en;
    }

    public int getCubedCount() {
        return cubedC;
    }

    public void setCubedCount(final int en) {
        this.cubedC = en;
    }

    public byte getState() {
        final int pots = potential1 + potential2 + potential3;
        if (potential1 >= 30000 || potential2 >= 30000 || potential3 >= 30000) {
            return 7;
        } else if (potential1 >= 20000 || potential2 >= 20000 || potential3 >= 20000) {
            return 6;
        } else if (pots >= 1) {
            return 5;
        } else if (pots < 0) {
            return 1;
        }
        return 0;
    }

    public void resetPotential_Fuse(int potentialState) { //메이커로 만들때
        //2% chance epic, else rare
        int rank = Randomizer.rand(1, 100) < 3 ? -6 : -5;
        setPotential1((short) rank);
        //setPotential2((short) 0); //무조건 두줄
        setPotential2((short) (Randomizer.rand(1, 100) <= 95 ? 0 : rank)); //5퍼센트의 확률로 3줄
        setPotential3((short) 0); //just set it theoretically
    }

    public void resetPotential(int itemId) { //드롭으로 먹을때
        //2% chance epic, else rare
        int rank = -5;
        switch (itemId) {
            case 1002357: //자쿰의 투구 
            case 1032075: //자유로운 영혼의 피어싱  
            case 1082254: //레더 글러브
            case 1003112: //카오스 자쿰의 투구
            case 1122000: //혼테일의 목걸이
            case 1122076: //카오스 혼테일의 목걸이
            case 1022073: //금이 간 안경    
            case 1032060: //알테어 이어링
            case 1032061: //빛나는 알테어 이어링    
            case 1003626: //갑옷무사의 투구
            case 1052509: //갑옷무사의 갑옷
            case 1132013: //불벨    

            /*드래곤장비*/
            case 1302059:
            case 1312031:
            case 1322052:
            case 1332049:
            case 1332050:
            case 1342009:
            case 1372032:
            case 1382036:
            case 1402036:
            case 1412026:
            case 1422028:
            case 1432038:
            case 1442045:
            case 1452044:
            case 1462039:
            case 1472051:
            case 1472052:
            case 1482013:
            case 1492013:

            /*리버스장비*/
            case 1302086:
            case 1312038:
            case 1322061:
            case 1332075:
            case 1332076:
            case 1342012:
            case 1372045:
            case 1382059:
            case 1402047:
            case 1412034:
            case 1422038:
            case 1432049:
            case 1442067:
            case 1452059:
            case 1462051:
            case 1472071:
            case 1482024:
            case 1492025:

            /*레볼장비*/
            case 1003946:
            case 1052647:
            case 1072853:
            case 1082540:
            case 1302289:
            case 1312165:
            case 1322215:
            case 1332238:
            case 1372188:
            case 1382222:
            case 1402210:
            case 1412147:
            case 1422152:
            case 1432178:
            case 1442234:
            case 1452216:
            case 1462204:
            case 1472226:
            case 1482179:
            case 1492190:
                
            /*베르살 장비*/
            case 1092070:
            case 1092080:
            case 1092075:
            case 1302143:
            case 1332116:
            case 1342029:
            case 1382095:
            case 1412058:
            case 1442107:
            case 1432077:
            case 1452102:
            case 1462087:
            case 1472113:
            case 1482075:
            case 1492075:
                
            /*레이븐 장비*/
            case 1312060:  
            case 1322062:
            case 1332072:
            case 1342031:
            case 1382056:
            case 1402070:
            case 1432062:
            case 1442060:
            case 1452087:
            case 1462054:
            case 1472075:
            case 1482044:
            case 1492047:
            case 1092052:
            case 1092061:
            case 1092050:  
                
            /*스윗워터 장비*/
            case 1003976:
            case 1052669:
            case 1072870:
            case 1082556:
            case 1102623:  
            case 1302297:
            case 1312173:
            case 1322223:
            case 1332247:
            case 1342090:
            case 1372195:
            case 1382231:
            case 1402220:
            case 1412152:
            case 1422158:
            case 1432187:
            case 1442242:
            case 1452226:
            case 1462213:
            case 1472235:
            case 1482189:
            case 1492199:
            case 1092090:
            case 1002972:   
            case 1032062:    
                rank = -5; //레어 고정
                setPotential1(rank);
                setPotential2(rank); //무조건 3줄
                setPotential3(0); //just set it theoretically
                break;
            case 1022114: //미카엘의 안경
            case 1022115: //미카엘라의 안경
            case 1032077:
            case 1032078:
            case 1032079: //렉스의 이어링 시리즈 
            case 1113180: //갑옷의 반지    
                rank = -6;
                setPotential1(rank);
                setPotential2(0); //무조건 두줄
                setPotential3(0); //just set it theoretically
                break;
            default: //노말 아이템들
                rank = Randomizer.rand(1, 100) < 3 ? -6 : -5;
                setPotential1(rank);
                setPotential2((Randomizer.rand(1, 100) <= 90 ? 0 : rank)); //10퍼센트의 확률로 3줄
                setPotential3(0); //just set it theoretically
                break;
        }
    }

    public void resetPotentialWithScroll(int itemId, int rank) { //줌서 바를떄
        //2% chance epic, else rare
        switch (itemId) {
            case 1002357: //자쿰의 투구 
            case 1032075: //자유로운 영혼의 피어싱  
            case 1082254: //레더 글러브
            case 1003112: //카오스 자쿰의 투구
            case 1122000: //혼테일의 목걸이
            case 1122076: //카오스 혼테일의 목걸이
            case 1022073: //금이 간 안경    
            case 1032060: //알테어 이어링
            case 1032061: //빛나는 알테어 이어링   
            case 1003626: //갑옷무사의 투구
            case 1052509: //갑옷무사의 갑옷
            case 1132013: //불벨     

            /*드래곤장비*/
            case 1302059:
            case 1312031:
            case 1322052:
            case 1332049:
            case 1332050:
            case 1342009:
            case 1372032:
            case 1382036:
            case 1402036:
            case 1412026:
            case 1422028:
            case 1432038:
            case 1442045:
            case 1452044:
            case 1462039:
            case 1472051:
            case 1472052:
            case 1482013:
            case 1492013:

            /*리버스장비*/
            case 1302086:
            case 1312038:
            case 1322061:
            case 1332075:
            case 1332076:
            case 1342012:
            case 1372045:
            case 1382059:
            case 1402047:
            case 1412034:
            case 1422038:
            case 1432049:
            case 1442067:
            case 1452059:
            case 1462051:
            case 1472071:
            case 1482024:
            case 1492025:

            /*레볼장비*/
            case 1003946:
            case 1052647:
            case 1072853:
            case 1082540:
            case 1302289:
            case 1312165:
            case 1322215:
            case 1332238:
            case 1372188:
            case 1382222:
            case 1402210:
            case 1412147:
            case 1422152:
            case 1432178:
            case 1442234:
            case 1452216:
            case 1462204:
            case 1472226:
            case 1482179:
            case 1492190:
                
            /*베르살 장비*/
            case 1092070:
            case 1092080:
            case 1092075:
            case 1302143:
            case 1332116:
            case 1342029:
            case 1382095:
            case 1412058:
            case 1442107:
            case 1432077:
            case 1452102:
            case 1462087:
            case 1472113:
            case 1482075:
            case 1492075:
                
            /*레이븐 장비*/
            case 1312060:  
            case 1322062:
            case 1332072:
            case 1342031:
            case 1382056:
            case 1402070:
            case 1432062:
            case 1442060:
            case 1452087:
            case 1462054:
            case 1472075:
            case 1482044:
            case 1492047:
            case 1092052:
            case 1092061:
            case 1092050:
            
            /*스윗워터 장비*/
            case 1003976:
            case 1052669:
            case 1072870:
            case 1082556:
            case 1102623:  
            case 1302297:
            case 1312173:
            case 1322223:
            case 1332247:
            case 1342090:
            case 1372195:
            case 1382231:
            case 1402220:
            case 1412152:
            case 1422158:
            case 1432187:
            case 1442242:
            case 1452226:
            case 1462213:
            case 1472235:
            case 1482189:
            case 1492199:
            case 1092090:
            case 1002972:   
            case 1032062:    
                if (rank == 0) {
                    rank = -5; //레어 고정
                }
                setPotential1(rank);
                setPotential2(rank); //무조건 3줄
                break;
            case 1022114: //미카엘의 안경
            case 1022115: //미카엘라의 안경
            case 1032077:
            case 1032078:
            case 1032079: //렉스의 이어링 시리즈
            case 1113180: //갑옷의 반지    
                if (rank == 0) {
                    rank = -6;
                }
                setPotential1(-6);
                setPotential2(0); //무조건 두줄
                break;
            default: //노말 아이템들
                if (rank == 0) {
                    rank = Randomizer.rand(1, 100) < 3 ? -6 : -5;
                }
                setPotential1(rank);
                setPotential2((Randomizer.rand(1, 100) <= 90 ? 0 : rank)); //10퍼센트의 확률로 3줄
                break;
        }
        setPotential3((short) 0); //just set it theoretically
    }

    public void renewPotential(boolean addLine, MapleCharacter chr, int itemid) {
        byte rank = 0; //등급업확률 4프로로 동일
        /*if (chr.getMapId() == 104010100 && Randomizer.nextInt(1000 + 1) < 100 && getState() == 6) { //오솔길에 있으면 10퍼센트 확률
            rank = -7;//유니크
        } else if (chr.getMapId() == 104010100 && Randomizer.nextInt(1000 + 1) < 300 && getState() == 5) { //오솔길에 있으면 30퍼센트 확률
            rank = -6;//유니크
        } else*/
        switch (itemid) {
            case 5062000://미라클큐브
                if (Randomizer.rand(1, 1000) < 10 && getState() == 5) { //레어에서 에픽 1.0%확률
                    rank = -6;//에픽
                } else {
                    rank = (byte) -getState();//그대로
                }
                break;
            case 5062002: //마스터 미라클
                if (Randomizer.rand(1, 1000) < 10 && getState() == 6) { //에픽에서 유니크 1.0%확률
                    rank = -7;//유니크
                } else if (Randomizer.rand(1, 1000) < 30 && getState() == 5) { //레어에서 에픽 3.0%확률
                    rank = -6;//에픽
                } else {
                    rank = (byte) -getState();//그대로
                }
                break;
            case 5062100: //8주년(레드큐브)
                if (Randomizer.rand(1, 1000) < 30 && getState() == 6) { //에픽에서 유니크 3%확률
                    rank = -7;//유니크
                } else if (Randomizer.rand(1, 1000) < 50 && getState() == 5) { //레어에서 에픽  5%확률
                    rank = -6;//에픽
                } else {
                    rank = (byte) -getState();//그대로
                }
                break;
            case 5062001: //각인의 큐브
                rank = (byte) -getState();//그대로
                break;
            default:
                if (Randomizer.rand(1, 1000) < 30 && getState() == 5) { //레어고 3%확률
                    rank = -6;//에픽
                } else {
                    rank = (byte) -getState();//그대로
                }
                break;
        }
        //rank = (byte) -getState();//무조건 그대로
        /*prem true시 라인개방 가능*/
        setPotential1((short) rank);
        setPotential2((short) (getPotential3() > 0 || (addLine && Randomizer.nextInt(10) == 0) ? rank : 0)); //1/10 chance of 3 line 세줄로 늘어나지는 않습니다
        setPotential3((short) 0); //just set it theoretically
    }

    /*rank가 -7이면 유니크 -6이면 에픽 -5면 레어*/
    public short getHpR() {
        return hpR;
    }

    public void setHpR(final short hp) {
        this.hpR = hp;
    }

    public short getMpR() {
        return mpR;
    }

    public void setMpR(final short mp) {
        this.mpR = mp;
    }

    public int getIncSkill() {
        return incSkill;
    }

    public void setIncSkill(int inc) {
        this.incSkill = inc;
    }

    public short getCharmEXP() {
        return charmExp;
    }

    public short getPVPDamage() {
        return pvpDamage;
    }

    public void setCharmEXP(short s) {
        this.charmExp = s;
    }

    public void setPVPDamage(short p) {
        this.pvpDamage = p;
    }

}
