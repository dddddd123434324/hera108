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

import client.*;
import client.anticheat.CheatingOffense;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import server.*;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.events.MapleSnowball.MapleSnowballs;
import server.life.MapleMonster;
import server.life.MobSkillFactory;
import server.maps.FieldLimitType;
import server.maps.MapleMap;
import server.maps.MapleMist;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.CSPacket;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import server.life.BanishInfo;
import server.life.MapleLifeFactory;
import server.life.MobAttackInfo;
import server.life.MobSkill;
import server.maps.SavedLocationType;
import server.movement.AbsoluteLifeMovement;
import tools.AttackPair;
import tools.packet.MobPacket;
import tools.packet.TemporaryStatsPacket;
import tools.packet.UIPacket;

public class PlayerHandler {

    public static int isFinisher(final int skillid) {
        switch (skillid) {
            case 1111003:
            case 1111004:
            case 1111005:
            case 1111006:
            case 11111002:
            case 11111003:
                return 10;
        }
        return 0;
    }

    public static void ThrowGrenadeRequest(LittleEndianAccessor slea, MapleCharacter chr) {
        final int x = slea.readInt();
        //slea.readShort();//00 00 도되었다가 FF FF도 되었다가
        final int y = slea.readInt();
        //slea.readShort();//위랑같음
        final Point realpos = slea.readPos();//캐릭터의 Y좌표
        final int charge = slea.readInt(); //게이지를 모은정도
        final int skillId = slea.readInt(); //스킬아이디
        final int skillLevel = slea.readInt(); //스킬레벨
        final int checkLevel = chr.getSkillLevel(skillId);
        if (!GameConstants.isThrowSkill(skillId)) {
            chr.dropMessage(6, "사용한 스킬은 코딩 되어있지 않습니다.");
            return;
        }
        if (checkLevel > 0) {
            MapleStatEffect stateffect = SkillFactory.getSkill(skillId).getEffect(checkLevel);
            if (stateffect == null) {
                return;
            }
            int mana = stateffect.getMPCon();
            if (chr.getStat().getMp() >= mana) {
                chr.addMP(-mana);
            } else {
//                chr.getCalcDamage().setCheckMana(true); // 마나핵
//                chr.dropMessage(5, "마나핵이 의심되어 이번 공격은 데미지가 박히지 않습니다.");
                return;
            }
        } else {
            return;
        }
        if (chr.isHidden()) {
            chr.getMap().broadcastGMMessage(chr, MaplePacketCreator.throwGrenadeResult(chr.getId(), x, y, charge, skillId, skillLevel), false);
        } else {
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.throwGrenadeResult(chr.getId(), x, y, charge, skillId, skillLevel), realpos);
        }
    }

    public static void WheelOfFortuneEffect(LittleEndianAccessor lea, MapleCharacter chr) {
        byte[] proxy = lea.read(0xC);
        chr.getMap().broadcastGMMessage(chr, MaplePacketCreator.showWheelEffect(chr.getId(), proxy), false);
    }

    public static void ChangeSkillMacro(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int num = slea.readByte();
        String name;
        int shout, skill1, skill2, skill3;
        SkillMacro macro;

        for (int i = 0; i < num; i++) {
            name = slea.readMapleAsciiString();
            shout = slea.readByte();
            skill1 = slea.readInt();
            skill2 = slea.readInt();
            skill3 = slea.readInt();
            //매크로 패킷방지
            if (skill1 < 0 || chr.getSkillLevel(skill1) == 0) {
                skill1 = 0;
            }
            if (skill2 < 0 || chr.getSkillLevel(skill2) == 0) {
                skill2 = 0;
            }
            if (skill3 < 0 || chr.getSkillLevel(skill3) == 0) {
                skill3 = 0;
            }
            macro = new SkillMacro(skill1, skill2, skill3, name, shout, i);
            chr.updateMacros(i, macro);
        }
    }

    public static final void ChangeKeymap(final LittleEndianAccessor slea, final MapleCharacter chr) {
        if (slea.available() > 8 && chr != null) { // else = pet auto pot
            slea.skip(4); //0
            final int numChanges = slea.readInt();

            for (int i = 0; i < numChanges; i++) {
                final int key = slea.readInt();
                final byte type = slea.readByte();
                final int action = slea.readInt();
                if (type == 1 && action >= 1000) { //0 = normal key, 1 = skill, 2 = item
                    final Skill skil = SkillFactory.getSkill(action);
                    if (skil != null) { //not sure about aran tutorial skills..lol
                        if ((!skil.isFourthJob() && !skil.isBeginnerSkill() && skil.isInvisible() && chr.getSkillLevel(skil) <= 0) || GameConstants.isLinkedAranSkill(action) || action % 10000 < 1000 || action >= 91000000) { //cannot put on a key
                            continue;
                        }
                    }
                }
                chr.changeKeybinding(key, type, action);
            }
        } else if (chr != null) {
            final int type = slea.readInt(), data = slea.readInt();
            switch (type) {
                case 1:
                    if (data <= 0) {
                        chr.getQuestRemove(MapleQuest.getInstance(GameConstants.HP_ITEM));
                    } else {
                        chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.HP_ITEM)).setCustomData(String.valueOf(data));
                    }
                    break;
                case 2:
                    if (data <= 0) {
                        chr.getQuestRemove(MapleQuest.getInstance(GameConstants.MP_ITEM));
                    } else {
                        chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.MP_ITEM)).setCustomData(String.valueOf(data));
                    }
                    break;
            }
            chr.updatePetAuto();
        }
    }

    public static final void UseChair(final int itemId, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final Item toUse = chr.getInventory(MapleInventoryType.SETUP).findById(itemId);
        if (toUse == null) {
            chr.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM, Integer.toString(itemId));
            return;
        }
//        if (GameConstants.isFishingMap(chr.getMapId()) && (!GameConstants.GMS || itemId == 3011000)) {
//            if (chr.getStat().canFish) {
//                chr.startFishingTask();
//            }
//        }
        chr.setChair(itemId);
        chr.getMap().broadcastMessage(chr, MaplePacketCreator.showChair(chr.getId(), itemId), false);
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void CancelChair(final short id, final MapleClient c, final MapleCharacter chr) {
        if (id == -1) { // Cancel Chair
            chr.setChair(0);
            c.getSession().write(MaplePacketCreator.cancelChair(-1));
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.showChair(chr.getId(), 0), false);
            c.getSession().write(MaplePacketCreator.enableActions());
        } else { // Use In-Map Chair
            chr.setChair(id);
            c.getSession().write(MaplePacketCreator.cancelChair(id));
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static final void TrockAddMap(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final byte addrem = slea.readByte();
        final byte vip = slea.readByte();

        if (vip == 1) {
            if (addrem == 0) {
                chr.deleteFromRocks(slea.readInt());
            } else if (addrem == 1) {
                if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                    chr.addRockMap();
                } else {
                    chr.dropMessage(1, "순간이동이 불가능한 지역입니다.");
                }
            }
        } else if (vip >= 2) {
            if (addrem == 0) {
                chr.deleteFromHyperRocks(slea.readInt());
            } else if (addrem == 1) {
                if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                    chr.addHyperRockMap();
                } else {
                    chr.dropMessage(1, "순간이동이 불가능한 지역입니다.");
                }
            }
        } else {
            if (addrem == 0) {
                chr.deleteFromRegRocks(slea.readInt());
            } else if (addrem == 1) {
                if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                    chr.addRegRockMap();
                } else {
                    chr.dropMessage(1, "순간이동이 불가능한 지역입니다.");
                }
            }
        }
        c.getSession().write(CSPacket.getTrockRefresh(chr, vip, addrem == 3));
    }

    public static final void CharInfoRequest(final int objectid, final MapleClient c, final MapleCharacter chr) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        final MapleCharacter player = c.getPlayer().getMap().getCharacterById(objectid);
        c.getSession().write(MaplePacketCreator.enableActions());
        if (player != null) {
            if (!player.isGM() || c.getPlayer().isGM()) {
                c.getSession().write(MaplePacketCreator.charInfo(player, c.getPlayer().getId() == objectid));
            }
        }
    }

    public static final void TakeDamage(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
//        System.out.println(slea.toString());
        chr.updateTick(slea.readInt());
        final byte type = slea.readByte(); //-4 is mist, -3 and -2 are map damage.
        final byte element = slea.readByte(); // Element - 0x00 = elementless, 0x01 = ice, 0x02 = fire, 0x03 = lightning
        int damage = slea.readInt();
        int rdamage = damage;
//        slea.skip(2);
        int oid = 0;
        int monsteridfrom = 0;
        int reflect = 0;
        int reflectP = 0;
        byte direction = 0;
        int pos_x = 0;
        int pos_y = 0;
        int fake = 0;
        int hpattack = 0;
        int mpattack = 0;
        boolean is_pg = false;
        boolean isDeadlyAttack = false;
        MapleMonster attacker = null;
        if (chr.isHidden() || chr.getMap() == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.getStat().getHp() <= 0) {
            chr.updateSingleStat(MapleStat.HP, 0);
            return;
        }
        if (chr.isGM() && chr.isInvincible()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final PlayerStats stats = chr.getStat();
        boolean mana_reflect = false;
        if (type != -2 && type != -3 && type != -4) { // Not map damage
            monsteridfrom = slea.readInt();//몬스터코드
            oid = slea.readInt();
            attacker = chr.getMap().getMonsterByOid(oid);
            //monsteridfrom = attacker.getId();//몬스터코드는 오브젝트로 받아오기
            final MapleMonster mons = MapleLifeFactory.getMonster(monsteridfrom);
            if (mons == null) {
                return;
            }
            if (attacker == null || attacker.getLinkCID() > 0 || attacker.isFake()) {
            }
            //if (attacker.getStats().getId())
            direction = slea.readByte();
            reflectP = slea.readByte() & 0xFF;
            //c.getPlayer().dropMessage(5, "reflect value : "+reflectP);
            if (reflectP >= 1 || reflectP < 0) {
                slea.skip(1); //COutPacket::Encode1(v215); 109 새로 추가
                slea.skip(1); //COutPacket::Encode1(v224 == 0 ? 0 : (v220 != 0) + 1);
                byte pg = slea.readByte(); //COutPacket::Encode1(v225 == 0 ? 0 : a10 != 0);
                mana_reflect = pg == 0;
                if (pg == 1) {
                    is_pg = true;
                } else {
                    is_pg = false;
                }
                oid = slea.readInt();
                slea.skip(10);
                if (is_pg) {
                    long bouncedamage = (long) (damage * reflectP / 100);
                    final MapleMonster attacker2 = (MapleMonster) attacker;
                    bouncedamage = Math.min(bouncedamage, attacker.getMobMaxHp() / 10);//몹 반감같은것도 생각해야함 그리프의 경우 절반만 들어감
                    /*if ( CMob::IsBossMob(a7) )
                     v221 /= 2; 보스면 뎀지 반감
                     */
                    if (attacker2.getStats().isBoss()) {
                        bouncedamage /= 2;
                    }
                    //chr.dropMessage(6, "댐지" + bouncedamage);
                    attacker2.damage(chr, bouncedamage, true);
                    chr.checkMonsterAggro(attacker2);
                }
                /*else {
                 //chr.dropMessage(6, "reflectP" + reflectP);

                 long bouncedamage = (long) (damage * reflectP / 100);
                 final MapleMonster attacker2 = (MapleMonster) attacker;
                 bouncedamage = Math.min(bouncedamage, attacker.getMobMaxHp() / 20);
                 //dropMessage(6, "bouncedamage2" + bouncedamage);
                 //  attacker.damage(this, bouncedamage, true);
                 //getMap().broadcastMessage(this, MobPacket.damageMonster(attacker.getObjectId(), bouncedamage), getTruePosition());

                 //bouncedamage = Math.min(bouncedamage, attacker.getMobMaxHp() / 10);//몹 반감같은것도 생각해야함 그리프의 경우 절반만 들어감
                 attacker2.damage(chr, bouncedamage, true);
                 chr.checkMonsterAggro(attacker2);
                 //chr.dropMessage(6, "댐지" + bouncedamage);
                 }*/

            }
            if (type != -1 && type != 0 && damage > 0) { // Bump damage
                if (attacker.getStats().getBanType() == 1) {
                    final BanishInfo info = attacker.getStats().getBanishInfo();
                    if (info != null) {
                        if (chr != null && !chr.hasBlockedInventory()) {
                            chr.changeMapBanish(info.getMap(), info.getPortal(), info.getMsg());
                        }
                    }
                }
            }
            if (type != -1 && damage > 0 && mons.getStats().getSelfDHp() <= 0) { // Bump damage
                final MobAttackInfo attackInfo = attacker.getStats().getMobAttack(type);
                if (attackInfo != null) {
                    if (attackInfo.isDeadlyAttack()) {
                        isDeadlyAttack = true;
                        mpattack = stats.getMp() - 1;
                    } else {
                        mpattack += attackInfo.getMpBurn();
                    }
                    attacker.setMp(attacker.getMp() - attackInfo.getMpCon());
                }
            }
            if (damage > 0 && chr.getMapId() / 10000 == 92502) {
                chr.mulung_EnergyModify(2);
            }
        } else if (type == -3) {
            if (slea.available() >= 2) {
                int lv = slea.readByte() & 0xFF;
                int skillid = slea.readByte() & 0xFF;
                MapleDisease dise = MapleDisease.getBySkill(skillid);
                if (lv > 0 && skillid > 0 && dise != null && damage > 0) {
                    try {
                        c.getPlayer().giveDebuff(dise, MobSkillFactory.getMobSkill(skillid, lv), (short) 0);
                    } catch (Exception e) {
                    }
                }
            }
        }
        if (damage == -1) {
            fake = 4020002 + ((chr.getJob() / 10 - 40) * 100000);
            if (fake != 4120002 && fake != 4220002) {
                fake = 4120002;
            }
            if (attacker != null && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10) != null) {
                if (chr.getJob() == 122) {
                    if (chr.getTotalSkillLevel(1220006) > 0) {
                        if (type == -1) {
                            final MapleStatEffect eff = SkillFactory.getSkill(1220006).getEffect(chr.getTotalSkillLevel(1220006));
                            attacker.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.STUN, 1, 1220006, null, false), false, eff.getDuration(), true, eff);
                        }
                        fake = 1220006;
                    }
                }
                /*else if (chr.getJob() == 112) {//히어로에게 블로킹은 더이상 없다!!
                 if (chr.getTotalSkillLevel(1120005) > 0) {
                 if (type == -1) {
                 final MapleStatEffect eff = SkillFactory.getSkill(1120005).getEffect(chr.getTotalSkillLevel(1120005));
                 attacker.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.STUN, 1, 1120005, null, false), false, eff.getDuration(), true, eff);
                 }
                 fake = 1120005;
                 }
                 }*/

            }
            if (chr.getTotalSkillLevel(fake) <= 0) {
//            System.out.println("Return 4");
                return;
            }
        } else if (damage < -1 || damage > 200000) {
//            System.out.println("Return 5");
            //AutobanManager.getInstance().addPoints(c, 1000, 60000, "Taking abnormal amounts of damge from " + monsteridfrom + ": " + damage);
            return;
        }
//        if (chr.getStat().dodgeChance > 0 && Randomizer.nextInt(100) < chr.getStat().dodgeChance) {
//            c.getSession().write(MaplePacketCreator.showSpecialEffect(21)); //lol
//            return;
//        }
        chr.getCheatTracker().checkTakeDamage(damage);
        Pair<Double, Boolean> modify = chr.modifyDamageTaken((double) damage, attacker, mana_reflect);
        damage = modify.left.intValue();
        if (damage > 0) {
            chr.getCheatTracker().setAttacksWithoutHit(false);

            if (chr.getBuffedValue(MapleBuffStat.MORPH) != null) {
                switch (chr.getBuffSource(MapleBuffStat.MORPH)) {
                    case 5111005:
                    case 5121003:
                    case 15111002:
                    case 13111005:

                    case 2210009: // 소필리아
                    case 2210022: // 페토
                        break;
                    default:
                        chr.cancelMorphs();
                        break;
                }
            }
            if ((slea.available() == 3 || slea.available() == 4) && type != -1 && type != -4) {
                //chr.dropMessage(6, "디버깅 메세지 제보주세요." + type);
                /*byte level = slea.readByte();
                 if (level > 0) {
                 final MobSkill skill = MobSkillFactory.getMobSkill(slea.readShort(), level);
                 if (skill != null) {
                 skill.applyEffect(chr, attacker, false, (short) 0);
                 }
                 }*/
            }
            double reduceR = 0;//Math.ceil(변수)
            final int[] achilles = {1120004, 1220005, 1320005, 21120004};
            for (int sid : achilles) {
                int slv = chr.getTotalSkillLevel(sid);
                if (slv != 0) {
                    reduceR = 0.5 * slv;
                    //chr.dropMessage(6, "아킬퍼센트" + reduceR + " 스킬레벨" + slv);
                    //chr.dropMessage(6, "damage전" + damage + " 계산값" + damage * reduceR / 100);
                    //chr.dropMessage(6, "damage후" + damage + " r댐" + (int) rdam);
                }
            }
            //chr.dropMessage(6, "totalr" + (damage * (reduceR + reflectP)) + "dd" + (double) (damage * (reduceR + reflectP)) / 100);

            //chr.dropMessage(6, "RRRRdamage" + damage);
            double reducetotal = 0;
            if (reduceR != 0) {
                reducetotal += reduceR * 10;
                //chr.dropMessage(6, "reduceR" + reduceR);
            }
            if (reducetotal > 0) {
                double rdam = Math.floor((double) (damage * reducetotal) / 1000);
                //chr.dropMessage(6, "damage" + damage + "r댐" + (int) rdam);
                damage -= (int) rdam;
            }
            reducetotal = 0;
            MapleStatEffect Cbarrier = chr.getStatForBuff(MapleBuffStat.COMBO_BARRIER);
            if (Cbarrier != null) {
                //double rdam = Math.ceil((double) (damage * Cbarrier.getX()) / 1000);
                //damage = (int) rdam;
                reducetotal += 1000 - Cbarrier.getX();
                //chr.dropMessage(6, "Cbarrier.getX(): " + (double) Cbarrier.getX());
            }
            if (reducetotal > 0) {
                double rdam = Math.floor((double) (damage * reducetotal) / 1000);
                //chr.dropMessage(6, "damage" + damage + "r댐" + (int) rdam);
                damage -= (int) rdam;
            }
            reducetotal = 0;
            if (reducetotal > 0) {
                double rdam = Math.floor((double) (damage * reducetotal) / 1000);
                //chr.dropMessage(6, "damage" + damage + "r댐" + (int) rdam);
                damage -= (int) rdam;
            }
            reducetotal = 0;
            if (reflectP != 0) {
                reducetotal += reflectP * 10;
                double rdam = Math.floor((double) (rdamage * reducetotal) / 1000);
                //chr.dropMessage(6, "damage" + damage + "r댐" + (int) rdam);
                damage -= (int) rdam;
                //chr.dropMessage(6, "reflectP" + reflectP);
            }

            MapleStatEffect Mshield = chr.getStatForBuff(MapleBuffStat.MAGIC_SHIELD);
            if (Mshield != null) {
                double rdam = Math.floor((double) (damage * Mshield.getX()) / 100);
                damage -= (int) rdam;
            }
            //chr.dropMessage(6, "reducetotal" + reducetotal);

            /*if (reflectP != 0 || reduceR != 0) {
             double rdam = Math.floor((double) (damage * (reduceR + reflectP)) / 100);
             //chr.dropMessage(6, "damage" + damage + "r댐" + (int) rdam);
             damage -= (int) rdam;
             } else if (reflectP != 0 || reduceR == 0) {
             double rdam = Math.floor((double) (damage * reflectP) / 100);
             damage -= (int) rdam;
             } else if (reflectP == 0 || reduceR != 0) {
             double rdam = Math.floor((double) (damage * reduceR) / 100);
             damage -= (int) rdam;
             }*/
            boolean mpAttack = chr.getBuffedValue(MapleBuffStat.MECH_CHANGE) != null && chr.getBuffSource(MapleBuffStat.MECH_CHANGE) != 35121005;
            if (chr.getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null) {
                int hploss = 0, mploss = 0;
                if (isDeadlyAttack) {
                    if (stats.getHp() > 1) {
                        hploss = stats.getHp() - 1;
                    }
                    if (stats.getMp() > 1) {
                        mploss = stats.getMp() - 1;
                    }
                    if (chr.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                        mploss = 0;
                    }
                    chr.addMPHP(-hploss, -mploss);
                    //} else if (mpattack > 0) {
                    //    chr.addMPHP(-damage, -mpattack);
                } else {
                    mploss = (int) (damage * (chr.getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0)) + mpattack;
                    hploss = damage - mploss;
                    if (chr.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                        mploss = 0;
                    } else if (mploss > stats.getMp()) {
                        mploss = stats.getMp();
                        hploss = damage - mploss + mpattack;
                    }
                    if (isInSmoke(chr)) {
                        hploss = 0;
                    }
                    chr.addMPHP(-hploss, -mploss);
                }

            } else if (chr.getBuffedValue(MapleBuffStat.MESOGUARD) != null && type != -3 && type != -4) {
                //메소 가드 수정
                //handled in client
                int mesoloss = 0;
                double reduceRbyM = 0.5 + chr.getStat().mesoGuard * 0.01;
                //educeRbyM /= (reduceRbyM + chr.getStat().TER * 0.01);
                double dam = Math.round((double) rdamage * reduceRbyM);
                //0.6/(0.6+0.2) = 0.6/0.8 = 3/4
                if (!isInSmoke(chr)) {
                    mesoloss = (int) (dam * (chr.getStat().mesoGuardMeso));
                    if (chr.getMeso() < mesoloss) {
                        chr.gainMeso(-chr.getMeso(), false);
                        mesoloss = chr.getMeso();
                        chr.cancelBuffStats(true, MapleBuffStat.MESOGUARD);
                    } else {
                        chr.gainMeso(-mesoloss, false);
                    }
                }
                if (isDeadlyAttack && stats.getMp() > 1) {
                    mpattack = stats.getMp() - 1;
                }
                if (isDeadlyAttack && stats.getHp() > 1) {
                    hpattack = stats.getHp() - 1;
                }

                damage -= (int) dam;
                if (Mshield != null) {
                    double rdam = Math.floor((double) (dam * Mshield.getX()) / 100);
                    damage += rdam;
                }
                if (isDeadlyAttack && stats.getHp() > 1) {
                    chr.addMPHP(-((int) hpattack), -mpattack);
                } else {
                    chr.addMPHP(-((int) damage), -mpattack);
                }
            } else if (isDeadlyAttack) {
                chr.addMPHP(stats.getHp() > 1 ? -(stats.getHp() - 1) : 0, stats.getMp() > 1 ? -(stats.getMp() - 1) : 0);
            } else {
                int hploss = damage;
                if (isInSmoke(chr)) {
                    hploss = 0;
                }
                chr.addMPHP(-hploss, -mpattack);
            }
            chr.handleBattleshipHP(-damage);
        }
        byte offset = 0;
        if (type == 0 || type == 1 || type == 2) {
            //타입 -1이랑 공존함 -1에서 패킷 다쓰고 1일경후 후처리해주는 케이스 attackInfo.attackAfter
            if (attacker == null) {
                return;
            }
            final MobAttackInfo attackInfo = attacker.getStats().getMobAttack(type);
            final MobSkill skill = MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(), attackInfo.getDiseaseLevel());
            if (skill != null && (damage == -1 || damage > 0)) {
                skill.applyEffect(chr, attacker, false, (short) 0);
            }
        }
//        if (slea.available() == 1) {
//            offset = slea.readByte();
//            if (offset < 0 || offset > 2) {
//                offset = 0;
//            }
//        }
        c.getSession().write(MaplePacketCreator.enableActions());
        chr.getMap().broadcastMessage(chr, MaplePacketCreator.damagePlayer(type, monsteridfrom, chr.getId(), rdamage, damage, fake, direction, reflect, is_pg, oid, pos_x, pos_y, offset), false);
    }

    public static boolean isInSmoke(MapleCharacter chr) {
        for (MapleMist mist : chr.getMap().getAllMistsThreadsafe()) {
            if (mist.getOwnerId() == chr.getId() && mist.isPoisonMist() == 2 && mist.getBox().contains(chr.getTruePosition())) {
                return true;
            }
        }
        return false;
    }

    public static final void UseItemEffect(final int itemId, final MapleClient c, final MapleCharacter chr) {
        final Item toUse = chr.getInventory(MapleInventoryType.CASH).findById(itemId);
        if (itemId != 0 && (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1)) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        short flag = toUse.getFlag();
        if (ItemFlag.KARMA_USE.check(flag)) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            flag |= ItemFlag.KARMA_USE.getValue();
            toUse.setFlag((byte) (flag - ItemFlag.KARMA_USE.getValue()));
            c.getSession().write(MaplePacketCreator.updateSpecialItemUse_(toUse, MapleInventoryType.CASH.getType(), c.getPlayer(), true));
            c.getSession().write(MaplePacketCreator.serverNotice(1, ii.getName(toUse.getItemId()) + "의 교환가능 횟수가 차감됐습니다."));
        }
        if (itemId != 5510000) {
            chr.setItemEffect(itemId);
        }
        chr.getMap().broadcastMessage(chr, MaplePacketCreator.itemEffect(chr.getId(), itemId), false);
    }

    public static final void CancelItemEffect(final int id, final MapleCharacter chr) {
        if (-id == 2022536) {//지하신전의 봉인
            //chr.dropMessage(5, "이 버프는 해제 하실 수 없습니다.");
            return;
        }
        chr.cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(-id), -1);
    }

    public static final void CancelBuffHandler(int sourceid, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        if (sourceid == 33101006) {
            chr.cancelBuffStats(true, MapleBuffStat.CRITICAL_RATE_BUFF);
            chr.cancelBuffStats(true, MapleBuffStat.MP_BUFF);
            chr.cancelBuffStats(true, MapleBuffStat.DAMAGE_TAKEN_BUFF);
            chr.cancelBuffStats(true, MapleBuffStat.DODGE_CHANGE_BUFF);
            chr.cancelBuffStats(true, MapleBuffStat.PIRATES_REVENGE);
            chr.cancelBuffStats(true, MapleBuffStat.ATTACK_BUFF);
            return;
        } else if (sourceid == 32111005) {
            chr.cancelBuffStats(true, MapleBuffStat.AURA);
            chr.cancelBuffStats(true, MapleBuffStat.DARK_AURA);
            chr.cancelBuffStats(true, MapleBuffStat.BLUE_AURA);
            chr.cancelBuffStats(true, MapleBuffStat.YELLOW_AURA);
        }
        if (sourceid == 32001003) {
            if (chr.getSkillLevel(32120000) >= 1) {
                sourceid = 32120000;
            }
        }
        if (sourceid == 32101003) {
            if (chr.getSkillLevel(32120001) >= 1) {
                sourceid = 32120001;
            }
        }
        if (sourceid == 32101002) {
            if (chr.getSkillLevel(32110000) >= 1) {
                sourceid = 32110000;
            }
        }
        final Skill skill = SkillFactory.getSkill(sourceid);
        if (sourceid == 35001001 || sourceid == 35101009) {
            chr.setKeyDownSkill_Time(0);
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.skillCancel(chr, sourceid), false);
            chr.cancelEffect(skill.getEffect(chr.getTotalSkillLevel(skill)), -1);
        } else if (skill.isChargeSkill()) {
            chr.setKeyDownSkill_Time(0);
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.skillCancel(chr, sourceid), false);
        } else {
            if (skill.getId() == 4331003) { // 아울 데드 버프 해제
                chr.setBattleshipHP(0);
            }
            chr.cancelEffect(skill.getEffect(chr.getTotalSkillLevel(skill)), -1);
        }
    }

    public static final void CancelMech(final LittleEndianAccessor slea, final MapleCharacter chr) {
        // Flame Launcher: ignore intermediate CancelMech to prevent stance toggling
        // (handled by duration/SkillEffect reset instead)
        // IDs: 35001001, 35101009

        if (chr == null) {
            return;
        }
        int sourceid = slea.readInt();
        if (sourceid % 10000 < 1000 && SkillFactory.getSkill(sourceid) == null) {
            sourceid += 1000;
        }
        final Skill skill = SkillFactory.getSkill(sourceid);
        int skillLevel = slea.readByte();
        if (skill == null) { //not sure
            return;
        }
        if (skill.isChargeSkill()) {
            // Key-down skill cancel
            chr.setKeyDownSkill_Time(0);

            // Always broadcast cancel so other clients stop the stance/effect properly.
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.skillCancel(chr, sourceid), false);

            // Use the skillLevel we already read, do not read extra bytes here.
            chr.cancelEffect(skill.getEffect(skillLevel), -1);
        } else {
            //if (sourceid != 35001001 && sourceid != 35101009) {
            chr.cancelEffect(skill.getEffect(slea.readByte()), -1);
            //}
        }
        if (sourceid == 35111004 || sourceid == 35121005 || sourceid == 35121013 || sourceid == 35001001) {
            chr.cancelEffect(skill.getEffect(skillLevel), -1);
            sourceid -= 1000;
            chr.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(sourceid, 1, skillLevel, skillLevel, (byte) 1));
            chr.getMap().broadcastMessage(MaplePacketCreator.showBuffeffect(chr.getId(), sourceid, 1, skillLevel, skillLevel, (byte) 1));
        }
    }

    public static final void QuickSlot(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final StringBuilder ret = new StringBuilder();
        for (int i = 0; i < 8; i++) { //really hacky way of doing it
            ret.append(slea.readAsciiString(1));
            slea.skip(3);
        }
        chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.QUICK_SLOT)).setCustomData(ret.toString());
    }

    public static final void SkillEffect(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int skillId = slea.readInt();
        final byte level = slea.readByte();
        final byte flags = slea.readByte();
        final byte speed = slea.readByte();
        final byte unk = slea.readByte(); // Added on v.82

        final Skill skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(skillId));
        if (chr == null || skill == null || chr.getMap() == null) {
            return;
        }

        // Mechanic Flame Launcher (hold up to 8s): handle start once, keep state via buff, avoid restart spam.
        if (skillId == 35001001 || skillId == 35101009) {
            final long now = System.currentTimeMillis();

            // Auto-recover if keydown got stuck.
            long kd = chr.getKeyDownSkill_Time();
            if (kd > 0 && (now - kd) > 9000) { // 8s + buffer
                chr.setKeyDownSkill_Time(0);
                kd = 0;
            }

            // Start only once while holding.
            if (kd > 0) {
                return;
            }
            chr.setKeyDownSkill_Time(now);

            // Apply stance/buff once (MECH_CHANGE should be configured with duration=8000 in MapleStatEffect)
            final MapleStatEffect eff = skill.getEffect(level);
            if (eff != null) {
                eff.applyTo(chr);
            }

            // Broadcast the start effect once.
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.skillEffect(chr, skillId, level, flags, speed, unk), false);
            return;
        }

        final int skilllevel_serv = chr.getTotalSkillLevel(skill);

        if (skillId == 33101005 || skill.isChargeSkill()) {
            chr.setKeyDownSkill_Time(System.currentTimeMillis());
            if (skillId == 33101005) {
                int oid = slea.readInt();
                chr.clearLinkMid();
                chr.setLinkMid(oid, 0);
            }
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.skillEffect(chr, skillId, level, flags, speed, unk), false);
        } else if (skillId == 4211001) { // Chakra
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.skillEffect(chr, skillId, level, flags, speed, unk), false);
        }
    }

    public static final void SpecialMove(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.hasBlockedInventory() || chr.getMap() == null /*|| slea.available() < 9*/) { //1.2.6 
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        //[UNKNOWN] 1F 00 C9 14 F9 01 20 00 03 뭐지
        Point oldpos = null;
        oldpos = slea.readPos();
        //slea.skip(4); // Old X and Y
        int skillid = slea.readInt();
        int skillLevel = slea.readByte();
        final Skill skill = SkillFactory.getSkill(skillid);

        if (skill == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) <= 0 || chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) != skillLevel) {
            if (!GameConstants.isMulungSkill(skillid) && !GameConstants.isPyramidSkill(skillid) && chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) <= 0) {
                c.getSession().close();
                return;
            }
            if (GameConstants.isMulungSkill(skillid)) {
                if (chr.getMapId() / 10000 != 92502) {
                    //AutobanManager.getInstance().autoban(c, "Using Mu Lung dojo skill out of dojo maps.");
                    return;
                } else {
                    if (chr.getMulungEnergy() < 300) {
                        return;
                    }
                    chr.mulung_EnergyModify(0);
                }
            } else if (GameConstants.isPyramidSkill(skillid)) {
                if (chr.getMapId() / 10000 != 92602 && chr.getMapId() / 10000 != 92601) {
                    //AutobanManager.getInstance().autoban(c, "Using Pyramid skill out of pyramid maps.");
                    return;
                }
            }
        }
        if (GameConstants.isEventMap(chr.getMapId())) {
            for (MapleEventType t : MapleEventType.values()) {
                final MapleEvent e = ChannelServer.getInstance(chr.getClient().getChannel()).getEvent(t);
                if (e.isRunning() && !chr.isGM()) {
                    for (int i : e.getType().mapids) {
                        if (chr.getMapId() == i) {
                            c.getSession().write(MaplePacketCreator.enableActions());
                            chr.dropMessage(5, "이곳에서 스킬을 사용할 수 없습니다.");
                            return; //non-skill cannot use
                        }
                    }
                }
            }
        }
        skillLevel = chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid));
        final MapleStatEffect effect = skill.getEffect(skillLevel);
        if (effect.isMPRecovery() && chr.getStat().getHp() < (chr.getStat().getMaxHp() / 100) * 10) { //less than 10% hp
            c.getPlayer().dropMessage(5, "스킬을 사용하는데 필요한 HP가 부족합니다.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (effect.getCooldown() > 0) { //&& !chr.isGM() //GM 노쿨 뗐음
            if (chr.skillisCooling(skillid)) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (skillid != 5221006 && skillid != 35111002) {
                c.getSession().write(MaplePacketCreator.skillCooldown(skillid, effect.getCooldown()));
                chr.addCooldown(skillid, System.currentTimeMillis(), effect.getCooldown() * 1000);
            }
        }
        if (skillid == 4341006) {
            if (chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.SHADOWPARTNER);
            } else {
                return;
            }
        }

        //chr.checkFollow(); //not msea-like but ALEX'S WISHES
        switch (skillid) {
            case 1121001:
            case 1221001:
            case 1321001:
                final byte number_of_mobs = slea.readByte();
                slea.skip(3); //number가 4바이트라는건 생각을 못했나? 아무튼 부호 있는 1바이트도 127까지 가능하니 상관 없음.
                for (int i = 0; i < number_of_mobs; i++) {
                    int mobId = slea.readInt();
                    byte success = slea.readByte();
                    final MapleMonster mob = chr.getMap().getMonsterByOid(mobId);
                    if (mob != null) {
                        chr.getMap().broadcastMessage(chr, MaplePacketCreator.showMagnet(mobId, success), oldpos);
                        mob.switchController(chr, mob.isControllerHasAggro());
                        //mob.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.STUN, 1, skillid, null, false), false, effect.getDuration(), true, effect);
                    }
                }
                byte direction = slea.readByte();
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), skillid, 1, chr.getLevel(), skillLevel, direction), oldpos);
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            case 30001061: //capture
                int mobID = slea.readInt();
                MapleMonster mob = chr.getMap().getMonsterByOid(mobID);
                if (mob != null) {
                    boolean success = mob.getHp() <= mob.getMobMaxHp() / 2;
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), skillid, 1, chr.getLevel(), skillLevel, (byte) (success ? 1 : 0)), oldpos);
                    if (success) {//이부분 첨에 null체크 안해줘서 팅기는것같은데
                        if ((mob.getId() >= 9304000) && (mob.getId() < 9305000)) {
                            chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.JAGUAR)).setCustomData(String.valueOf((mob.getId() - 9303999) * 10));
                        } else {
                            if (chr.getIntNoRecord(111113) == 0) {
                                chr.getQuestNAdd(MapleQuest.getInstance(111113)).setCustomData(String.valueOf(mob.getId()));
                            } else if (chr.getIntNoRecord(111114) == 0) {
                                chr.getQuestNAdd(MapleQuest.getInstance(111114)).setCustomData(String.valueOf(mob.getId()));
                            } else if (chr.getIntNoRecord(111115) == 0) {
                                chr.getQuestNAdd(MapleQuest.getInstance(111115)).setCustomData(String.valueOf(mob.getId()));
                            } else if (chr.getIntNoRecord(111116) == 0) {
                                chr.getQuestNAdd(MapleQuest.getInstance(111116)).setCustomData(String.valueOf(mob.getId()));
                            } else if (chr.getIntNoRecord(111117) == 0) {
                                chr.getQuestNAdd(MapleQuest.getInstance(111117)).setCustomData(String.valueOf(mob.getId()));
                            } else {
                                chr.getQuestNAdd(MapleQuest.getInstance(111118)).setCustomData(String.valueOf(chr.getIntNoRecord(111118) + 1));
                                chr.getQuestNAdd(MapleQuest.getInstance(111112 + (chr.getIntNoRecord(111118) % 5))).setCustomData(String.valueOf((mob.getId())));
                            }
                        }
                        chr.getMap().killMonster(mob, chr, false, false, (byte) 1);
                        chr.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
                        c.getSession().write(MaplePacketCreator.updateJaguar(chr));
                        chr.getMap().broadcastMessage(MaplePacketCreator.catchMonster(mob.getId(), skillid, (byte) 1));
                        chr.getMap().broadcastMessage(MaplePacketCreator.showMagnet(mob.getObjectId(), (byte) 1));
                    } else {
                        chr.dropMessage(5, "몬스터의 체력이 너무많습니다.");
                    }
                }
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            case 30001062:
                int mobid = slea.readInt();//몹다이디
                Point posi = slea.readPos();
                direction = slea.readByte();//왼쪽보는지 오른쪽보는지 체크
                mob = MapleLifeFactory.getMonster(mobid);
                mob.setPosition(posi);
                mob.setStance(2);
                //mob.getStats().setExp(0);
                final MapleStatEffect eff = skill.getEffect(1);//위젯에 1레벨짜리 스킬로댐
                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.HYPNOTIZE, chr.getId(), 30001062, null, false);
                chr.getMap().spawnMonsterOnGroundBelow(mob, posi);
                mob.applyStatus(chr, monsterStatusEffect, false, 20000, true, eff); //20초
                chr.getMap().broadcastMessage(MaplePacketCreator.showBuffeffect(chr.getId(), skillid, 1, chr.getLevel(), skillLevel, (byte) direction));
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            case 33101005: //jaguar oshi
                mobID = chr.getFirstLinkMid();
                mob = chr.getMap().getMonsterByOid(mobID);
                chr.setSwallowedMobID(mob.getId());
                chr.setKeyDownSkill_Time(0);
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.skillCancel(chr, skillid), false);
                if (mob != null) {
                    boolean success = mob.getStats().getLevel() < chr.getLevel() && mob.getId() < 9000000 && !mob.getStats().isBoss();
                    if (mob.getStats().isBoss()) {
                        System.out.println("스왈로우로 보스를 먹음 핵 의심!");
                    }
                    chr.getMap().broadcastMessage(MobPacket.suckMonster(mob.getObjectId(), chr.getId()));
                    mob.damage(c.getPlayer(), mob.getHp(), false);
                    chr.getMap().killMonster(mob, chr, true, false, (byte) -1);
                } else {
                    chr.dropMessage(5, "No monster was sucked. The skill failed.");
                }
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            case 1111007:
            case 1211009:
            case 1311007: {
                Point pos = null;
                pos = slea.readPos();
                int oid = 0;
                int count = slea.readByte();
                List<MonsterStatus> toCancel = new ArrayList<MonsterStatus>();
                toCancel.add(MonsterStatus.WEAPON_ATTACK_UP);
                toCancel.add(MonsterStatus.MAGIC_ATTACK_UP);
                toCancel.add(MonsterStatus.WEAPON_DEFENSE_UP);
                toCancel.add(MonsterStatus.MAGIC_DEFENSE_UP);
                toCancel.add(MonsterStatus.WEAPON_IMMUNITY);
                toCancel.add(MonsterStatus.MAGIC_IMMUNITY);
                toCancel.add(MonsterStatus.DAMAGE_IMMUNITY);
                toCancel.add(MonsterStatus.WEAPON_DAMAGE_REFLECT);
                toCancel.add(MonsterStatus.MAGIC_DAMAGE_REFLECT);
                for (int i = 0; i < count; i++) {
                    oid = slea.readInt();
                    MapleMonster mob32 = chr.getMap().getMonsterByOid(oid);
                    if (Randomizer.nextInt(100) < skill.getEffect(chr.getSkillLevel(skillid)).getProb()) {
                        for (MonsterStatus stat : toCancel) {
                            mob32.cancelStatus(stat);
                        }
                    }
                    mob32.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.MAGIC_CRASH, 1, skill.getId(), null, false), effect.isPoison(), effect.getDuration(), true, effect);
                }
                slea.skip(3);
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            }
            case 2311001: {
                Point pos = null;
                pos = slea.readPos();
                slea.skip(1);
                slea.skip(3);
                int oid = 0;
                int count = slea.readByte();
                List<MonsterStatus> toCancel = new ArrayList<MonsterStatus>();
                toCancel.add(MonsterStatus.WEAPON_ATTACK_UP);
                toCancel.add(MonsterStatus.MAGIC_ATTACK_UP);
                toCancel.add(MonsterStatus.WEAPON_DEFENSE_UP);
                toCancel.add(MonsterStatus.MAGIC_DEFENSE_UP);
                //  toCancel.add(MonsterStatus.DAMAGE_IMMUNITY);
                //  toCancel.add(MonsterStatus.WEAPON_IMMUNITY);
                //  toCancel.add(MonsterStatus.MAGIC_IMMUNITY);
                toCancel.add(MonsterStatus.SPEED);

                MapleStatEffect Dispel = skill.getEffect(chr.getSkillLevel(skillid));

                for (int i = 0; i < count; i++) {
                    oid = slea.readInt();
                    MapleMonster mob32 = chr.getMap().getMonsterByOid(oid);
                    if (Randomizer.nextInt(100) < skill.getEffect(chr.getSkillLevel(skillid)).getProb()) {
                        for (MonsterStatus stat : toCancel) {
                            mob32.cancelStatus(stat);
                        }
                    }
                    mob32.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.SEAL, 1, skill.getId(), null, false), effect.isPoison(), effect.getDuration(), true, effect);
                }
                slea.skip(3);
                effect.applyTo(c.getPlayer(), pos);
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            }
            case 9001000: {
                Point pos = null;
                pos = slea.readPos();
                slea.skip(3);
                mobid = 0;
                int count = slea.readByte();
                List<MonsterStatus> toCancel = new ArrayList<MonsterStatus>();
                toCancel.add(MonsterStatus.WEAPON_ATTACK_UP);
                toCancel.add(MonsterStatus.MAGIC_ATTACK_UP);
                toCancel.add(MonsterStatus.WEAPON_DEFENSE_UP);
                toCancel.add(MonsterStatus.MAGIC_DEFENSE_UP);
                toCancel.add(MonsterStatus.DAMAGE_IMMUNITY);
                toCancel.add(MonsterStatus.WEAPON_IMMUNITY);
                toCancel.add(MonsterStatus.MAGIC_IMMUNITY);
                toCancel.add(MonsterStatus.SPEED);

                MapleStatEffect Dispel = skill.getEffect(chr.getSkillLevel(skillid));

                for (int i = 0; i < count; i++) {
                    mobid = slea.readInt();
                    MapleMonster mob32 = chr.getMap().getMonsterByOid(mobid);
                    if (Randomizer.nextInt(100) < skill.getEffect(chr.getSkillLevel(skillid)).getProb()) {
                        for (MonsterStatus stat : toCancel) {
                            mob32.cancelStatus(stat);
                        }
                    }
                    mob32.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.SEAL, 1, skill.getId(), null, false), effect.isPoison(), effect.getDuration(), true, effect);
                }
                effect.applyTo(c.getPlayer(), pos);
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            }
            case 35111004:
                if (chr.getBuffSource(MapleBuffStat.MECH_CHANGE) == 35121005) {
                    effect.setSourceId(35121013);
                } else if (chr.getBuffSource(MapleBuffStat.MECH_CHANGE) == 35001002) {
                    effect.setSourceId(35111004);
                } else {
                }
                effect.applyTo(c.getPlayer(), null);
                break;
            case 35111002:
                if (slea.readByte() == 2) {
                    slea.readInt();//oid1
                    slea.readInt();//oid2
                }
                Point pos2 = null;
                pos2 = slea.readPos();
                effect.applyTo(c.getPlayer(), pos2);
                break;
            case 35101004:
                chr.getMap().broadcastMessage(MaplePacketCreator.showBuffeffect(chr.getId(), skillid, 1, chr.getLevel(), skillLevel, (byte) 1));
                break;
            default:
                if (skillid != 4341003 && skillid != 4341005) {
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), skillid, 1, chr.getLevel(), skillLevel, (byte) 1), oldpos);
                }
                if (skillid == 9001004) {
                    chr.setHidden(!chr.isHidden());
                    if (!chr.isHidden()) {
                        if (GameConstants.isEvan(chr.getJob()) && chr.getJob() >= 2200) {
                            chr.makeDragon();
                        }
                    }
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                Point pos = null;
                if (slea.available() == 4 || slea.available() == 5 || slea.available() == 7) {
                    pos = slea.readPos();
                }
                if (effect.isMagicDoor()) { // Mystic Door
                    if (!FieldLimitType.MysticDoor.check(chr.getMap().getFieldLimit())) {
                        effect.applyTo(c.getPlayer(), pos);
                    } else {
                        c.getSession().write(MaplePacketCreator.enableActions());
                    }
                } else {
                    final int mountid = MapleStatEffect.parseMountInfo(c.getPlayer(), skill.getId());
                    if ((mountid != 0) && (mountid != GameConstants.getMountItem(skill.getId(), c.getPlayer())) && (!c.getPlayer().isIntern()) && (c.getPlayer().getBuffedValue(MapleBuffStat.MONSTER_RIDING) == null)
                            && (c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -118) == null) && (!GameConstants.isMountItemAvailable(mountid, c.getPlayer().getJob()))) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    effect.applyTo(c.getPlayer(), pos);
                    //chr.dropMessage(6, "slea.available()" + slea.available());
                    //chr.dropMessage(6, "oldpos" + pos);
                }
                break;
        }
    }

    public static final void closeRangeAttack(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr, boolean energy) {
        if (chr == null) {
            return;
        }//일단 보류 타이탄 에너지 어택 무엇??!!
        /*if (energy
         && chr.getBuffedValue(MapleBuffStat.ENERGY_CHARGE) == null
         && chr.getBuffedValue(MapleBuffStat.ENERGY_CHARGE) == null
         && chr.getBuffedValue(MapleBuffStat.BODY_PRESSURE) == null
         && chr.getBuffedValue(MapleBuffStat.DARK_AURA) == null
         && chr.getBuffedValue(MapleBuffStat.TORNADO) == null
         && chr.getBuffedValue(MapleBuffStat.SUMMON) == null
         && chr.getBuffedValue(MapleBuffStat.RAINING_MINES) == null
         && chr.getBuffedValue(MapleBuffStat.TELEPORT_MASTERY) == null) {
         return;
         }*/
        if (chr.hasBlockedInventory() || chr.getMap() == null || chr.isApplyDamageFucking()) {
            return;
        }

        AttackInfo attack = DamageParse.parseAttack(slea, chr, RecvPacketOpcode.CLOSE_RANGE_ATTACK);
        if (attack == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final boolean mirror = chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null;
        double maxdamage = chr.getStat().getCurrentMaxBaseDamage();
        final Item shield = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
        int attackCount = (shield != null && shield.getItemId() / 10000 == 134 ? 2 : 1);
        int skillLevel = 0;
        MapleStatEffect effect = null;
        Skill skill = null;

        if (attack.skill != 0) {
            skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(attack.skill));
            if (skill == null) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            skillLevel = chr.getTotalSkillLevel(skill);
            //skillLevel = attack.skilllevel;
            effect = attack.getAttackEffect(chr, skillLevel, skill);
            //chr.dropMessage(6, "attack.skill" + attack.skill + " skillLevel" + skillLevel + " skill" + skill + " effect" + effect);
            if (effect == null) {
                return;
            }
            int skillid = attack.skill;
            if (chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) <= 0 || chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) != skillLevel) {
                if (!GameConstants.isMulungSkill(skillid) && !GameConstants.isPyramidSkill(skillid) && chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) <= 0) {
                    c.getSession().close();
                    return;
                }
                if (GameConstants.isMulungSkill(skillid)) {
                    if (chr.getMapId() / 10000 != 92502) {
                        //AutobanManager.getInstance().autoban(c, "Using Mu Lung dojo skill out of dojo maps.");
                        return;
                    } else {
                        if (chr.getMulungEnergy() < 300) {
                            return;
                        }
                        chr.mulung_EnergyModify(0);
                    }
                } else if (GameConstants.isPyramidSkill(skillid)) {
                    if (chr.getMapId() / 10000 != 92602 && chr.getMapId() / 10000 != 92601) {
                        //AutobanManager.getInstance().autoban(c, "Using Pyramid skill out of pyramid maps.");
                        return;
                    }
                }
            }
            if (GameConstants.isEventMap(chr.getMapId())) {
                for (MapleEventType t : MapleEventType.values()) {
                    final MapleEvent e = ChannelServer.getInstance(chr.getClient().getChannel()).getEvent(t);
                    if (e.isRunning() && !chr.isGM()) {
                        for (int i : e.getType().mapids) {
                            if (chr.getMapId() == i) {
                                chr.dropMessage(5, "이곳에서 스킬을 사용할 수 없습니다.");
                                return; //non-skill cannot use
                            }
                        }
                    }
                }
            }
            maxdamage *= effect.getDamage() / 100.0;
            attackCount = effect.getAttackCount();

            if (effect.getCooldown() > 0 && !energy) {
                if (chr.skillisCooling(attack.skill)) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect.getCooldown()));
                chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown() * 1000);
            }
        }
        if (!GameConstants.isComboSkill(attack.skill)) { //패닉 코마 멀티
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), attack.skill, 1, chr.getLevel(), skillLevel, (byte) 1), chr.getTruePosition());
        }
        //attack = DamageParse.Modify_AttackCrit(attack, chr, 1, effect);

        DamageParse.critModify(chr, attack);
        attackCount *= (mirror ? 2 : 1);
        /*if ( attack.unk >0) {
         AutobanManager.getInstance().autoban(chr.getClient(), "팅패킷 사용");
         System.out.println("팅패킷 사용 : " + chr.getName());
         return;
         }*/
        if (!energy) {
            if ((chr.getMapId() == 109060000 || chr.getMapId() == 109060002 || chr.getMapId() == 109060004) && attack.skill == 0) {
                MapleSnowballs.hitSnowball(chr);
            }
            // handle combo orbconsume
            int numFinisherOrbs = 0;
            final Integer comboBuff = chr.getBuffedValue(MapleBuffStat.COMBO);

            if (isFinisher(attack.skill) > 0) { // finisher
                if (comboBuff != null) {
                    numFinisherOrbs = comboBuff.intValue() - 1;
                }
                if (numFinisherOrbs <= 0) {
                    return;
                }
                chr.handleOrbconsume(isFinisher(attack.skill));
                maxdamage *= numFinisherOrbs;
            }
        }
        byte mastery = DamageParse.getMasteryByte();
        if (!chr.isHidden()) {
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.showAttack(chr.getId(), attack.tbyte, chr.getLevel(), attack.skill, skillLevel, (byte) 0, attack.display, attack.speed, attack.allDamage, energy ? SendPacketOpcode.ENERGY_ATTACK : SendPacketOpcode.CLOSE_RANGE_ATTACK, mastery, attack.unk, attack.charge, 0, attack.position, 0), chr.getTruePosition());
        } else {
            chr.getMap().broadcastGMMessage(chr, MaplePacketCreator.showAttack(chr.getId(), attack.tbyte, chr.getLevel(), attack.skill, skillLevel, (byte) 0, attack.display, attack.speed, attack.allDamage, energy ? SendPacketOpcode.ENERGY_ATTACK : SendPacketOpcode.CLOSE_RANGE_ATTACK, mastery, attack.unk, attack.charge, 0, attack.position, 0), false);
        }
        //chr.dropMessage(6, "tbyte : " + attack.tbyte + " skill : " + attack.skill + " display : " + attack.display + " speed : " + attack.speed + " allDamage : " + attack.allDamage + " unk : " + attack.unk);
        DamageParse.applyAttack(attack, skill, c.getPlayer(), attackCount, maxdamage, effect, mirror ? AttackType.NON_RANGED_WITH_MIRROR : AttackType.NON_RANGED);
//        WeakReference<MapleCharacter>[] clones = chr.getClones();
//        for (int i = 0; i < clones.length; i++) {
//            if (clones[i].get() != null) {
//                final MapleCharacter clone = clones[i].get();
//                final Skill skil2 = skill;
//                final int attackCount2 = attackCount;
//                final double maxdamage2 = maxdamage;
//                final MapleStatEffect eff2 = effect;
//                final AttackInfo attack2 = DamageParse.DivideAttack(attack, chr.isGM() ? 1 : 4);
//                CloneTimer.getInstance().schedule(new Runnable() {
//                    public void run() {
//                        if (!clone.isHidden()) {
//                            //clone.getMap().broadcastMessage(MaplePacketCreator.closeRangeAttack(clone.getId(), attack2.tbyte, attack2.skill, skillLevel2, attack2.display, attack2.speed, attack2.allDamage, energy, clone.getLevel(), clone.getStat().passive_mastery(), attack2.unk, attack2.charge));
//                        } else {
        //clone.getMap().broadcastGMMessage(clone, MaplePacketCreator.closeRangeAttack(clone.getId(), attack2.tbyte, attack2.skill, skillLevel2, attack2.display, attack2.speed, attack2.allDamage, energy, clone.getLevel(), clone.getStat().passive_mastery(), attack2.unk, attack2.charge), false);
//                        }
//                        DamageParse.applyAttack(attack2, skil2, chr, attackCount2, maxdamage2, eff2, mirror ? AttackType.NON_RANGED_WITH_MIRROR : AttackType.NON_RANGED);
//                    }
//                }, 500 * i + 500);
//            }
//        }
        //chr.getCalcDamage().PDamage(chr, attack);
    }

    public static final void rangedAttack(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        if (chr.hasBlockedInventory() || chr.getMap() == null || chr.isApplyDamageFucking()) {
            return;
        }
        AttackInfo attack = DamageParse.parseAttack(slea, chr, RecvPacketOpcode.RANGED_ATTACK);
        if (attack == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        int bulletCount = 1, skillLevel = 0;
        MapleStatEffect effect = null;
        Skill skill = null;
        boolean AOE = false;
        switch (attack.skill) {
            case 4111004:
            case 15111007:
            case 5121002:
            case 15111006:
                AOE = true;
                break;
        }
        if (c.getPlayer().getBuffedValue(MapleBuffStat.POWERGUARD) != null) {
            AOE = true;
        }
        if (attack.skill != 0) {
            skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(attack.skill));
            if (skill == null) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            skillLevel = chr.getTotalSkillLevel(skill);
            effect = attack.getAttackEffect(chr, skillLevel, skill);
            if (effect == null) {
                return;
            }
            int skillid = attack.skill;
            if (chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) <= 0 || chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) != skillLevel) {
                if (!GameConstants.isMulungSkill(skillid) && !GameConstants.isPyramidSkill(skillid) && chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) <= 0) {
                    c.getSession().close();
                    return;
                }
                if (GameConstants.isMulungSkill(skillid)) {
                    if (chr.getMapId() / 10000 != 92502) {
                        //AutobanManager.getInstance().autoban(c, "Using Mu Lung dojo skill out of dojo maps.");
                        return;
                    } else {
                        if (chr.getMulungEnergy() < 300) {
                            return;
                        }
                        chr.mulung_EnergyModify(0);
                    }
                } else if (GameConstants.isPyramidSkill(skillid)) {
                    if (chr.getMapId() / 10000 != 92602 && chr.getMapId() / 10000 != 92601) {
                        //AutobanManager.getInstance().autoban(c, "Using Pyramid skill out of pyramid maps.");
                        return;
                    }
                }
            }
            if (GameConstants.isEventMap(chr.getMapId())) {
                for (MapleEventType t : MapleEventType.values()) {
                    final MapleEvent e = ChannelServer.getInstance(chr.getClient().getChannel()).getEvent(t);
                    if (e.isRunning() && !chr.isGM()) {
                        for (int i : e.getType().mapids) {
                            if (chr.getMapId() == i) {
                                chr.dropMessage(5, "이곳에서 스킬을 사용할 수 없습니다.");
                                return; //non-skill cannot use
                            }
                        }
                    }
                }
            }
            switch (attack.skill) {
                case 13101005:
                case 21110004: // Ranged but uses attackcount instead
                case 14101006: // Vampure
                case 21120006:
                case 11101004:
                case 1077:
                case 1078:
                case 1079:
                case 11077:
                case 11078:
                case 11079:
                case 15111007:
                case 13111007: //Wind Shot
                case 33101007:
                case 33101002:
                case 33121002:
                case 33121001:
                case 21100004:
                case 21110011:
                case 21100007:
                case 21000004:
                case 5121002:
                case 4121003:
                case 4221003:
                    AOE = true;
                    bulletCount = effect.getAttackCount();
                    break;
                case 35121005:
                case 35111004:
                case 35121013:
                    AOE = true;
                    bulletCount = 6;
                    break;
                default:
                    bulletCount = effect.getBulletCount();
                    break;
            }
            bulletCount = effect.getBulletCount();
            if (effect.getBulletCount() < effect.getAttackCount()) {
                bulletCount = effect.getAttackCount();
            }
            if (effect.getCooldown() > 0 && ((attack.skill != 35111004 && attack.skill != 35121013) || chr.getBuffSource(MapleBuffStat.MECH_CHANGE) != attack.skill)) {
                if (chr.skillisCooling(attack.skill)) {
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return;
                }
                c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect.getCooldown()));
                chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown() * 1000);
            }
        }
        //attack = DamageParse.Modify_AttackCrit(attack, chr, 2, effect);
        DamageParse.critModify(chr, attack);
        final Integer ShadowPartner = chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER);
        if (ShadowPartner != null) {
            bulletCount *= 2;
        }
        int projectile = 0, visProjectile = 0;
        if (!AOE && chr.getBuffedValue(MapleBuffStat.SOULARROW) == null && attack.slot != 0) {
            Item ipp = chr.getInventory(MapleInventoryType.USE).getItem(attack.slot);
            if (ipp == null && !(chr.getJob() >= 3500 && chr.getJob() <= 3512)) {
                return;
            }
            projectile = ipp.getItemId();

            if (attack.csstar > 0) {
                if (chr.getInventory(MapleInventoryType.CASH).getItem(attack.csstar) == null) {
                    return;
                }
                visProjectile = chr.getInventory(MapleInventoryType.CASH).getItem(attack.csstar).getItemId();
            } else {
                visProjectile = projectile;
            }
            // Handle bulletcount
            if (chr.getBuffedValue(MapleBuffStat.SPIRIT_CLAW) == null) {
                int bulletConsume = bulletCount;
                if (effect != null && effect.getBulletConsume() != 0) {
                    bulletConsume = effect.getBulletConsume() * (ShadowPartner != null ? 2 : 1);
                }
                //chr.dropMessage(6, "쓰기전" + ipp.getQuantity() + "뭐지" + MapleItemInformationProvider.getInstance().getSlotMax(projectile));
                if ((chr.getJob() == 412 && bulletConsume > 0 && ipp.getQuantity() < MapleItemInformationProvider.getInstance().getSlotMax(projectile)) && !chr.isGM()) {
                    final Skill expert = SkillFactory.getSkill(4120010);
                    if (chr.getTotalSkillLevel(expert) > 0) {
                        //c.getSession().write(MaplePacketCreator.APPLYEXJABLIN());
                        final MapleStatEffect eff = expert.getEffect(chr.getTotalSkillLevel(expert));
                        if (eff.makeChanceResult()) {
                            ipp.setQuantity((short) (ipp.getQuantity() + 1));
                            c.getSession().write(MaplePacketCreator.updateInventorySlot(MapleInventoryType.USE, ipp, false));
                            //bulletConsume = 0; //뭐지 이건
                            c.getSession().write(MaplePacketCreator.getInventoryStatus());
                        }
                    }
                }
                if (bulletConsume > 0) {
                    if (GameConstants.isThrowingStar(projectile) || GameConstants.isBullet(projectile)) {
                        if (ipp.getQuantity() - bulletConsume >= 0) {
                            ipp.setQuantity((short) (ipp.getQuantity() - bulletConsume));
                            c.getSession().write(MaplePacketCreator.updateInventorySlot(MapleInventoryType.USE, ipp, false));
                            bulletConsume = 0; //regain a star after using
                            c.getSession().write(MaplePacketCreator.getInventoryStatus());
                        } else {
                            chr.dropMessage(5, "표창/화살이 부족합니다.");
                            return;
                        }
                    } else {
                        if (!MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, projectile, bulletConsume, false, true)) {
                            chr.dropMessage(5, "표창/화살/불릿이 부족합니다.");
                            return;
                        }
                    }
                }
            }
        }
        double basedamage = 1.0D;
        int projectileWatk = 0;
        if (projectile != 0) {
            projectileWatk = MapleItemInformationProvider.getInstance().getWatkForProjectile(projectile);
        }
        if (effect != null) {
            int money = effect.getMoneyCon();
            if (money != 0) {
                if (money > chr.getMeso()) {
                    money = chr.getMeso();
                }
                chr.gainMeso(-money, false);
            }
        }
        int mobid = 0;
        if (attack.skill == 33101007) {
            mobid = chr.getSwallowedMobID();
        }
        byte mastery = DamageParse.getMasteryByte();
        if (!chr.isHidden()) {
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.showAttack(chr.getId(), attack.tbyte, chr.getLevel(), attack.skill, skillLevel, (byte) 0, attack.display, attack.speed, attack.allDamage, SendPacketOpcode.RANGED_ATTACK, mastery, attack.unk, attack.charge, visProjectile, attack.position, mobid), chr.getTruePosition());
        } else {
            chr.getMap().broadcastGMMessage(chr, MaplePacketCreator.showAttack(chr.getId(), attack.tbyte, chr.getLevel(), attack.skill, skillLevel, (byte) 0, attack.display, attack.speed, attack.allDamage, SendPacketOpcode.RANGED_ATTACK, mastery, attack.unk, attack.charge, visProjectile, attack.position, mobid), false);
        }

        DamageParse.applyAttack(attack, skill, chr, bulletCount, basedamage, effect, ShadowPartner != null ? AttackType.RANGED_WITH_SHADOWPARTNER : AttackType.RANGED);
        //chr.getCalcDamage().PDamage(chr, attack);
    }

    public static final void MagicDamage(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.hasBlockedInventory() || chr.getMap() == null || chr.isApplyDamageFucking()) {
            return;
        }
        AttackInfo attack = DamageParse.parseAttack(slea, chr, RecvPacketOpcode.MAGIC_ATTACK);
        if (attack == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final Skill skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(attack.skill));
        if (skill == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final int skillLevel = chr.getTotalSkillLevel(skill);
        final MapleStatEffect effect = attack.getAttackEffect(chr, skillLevel, skill);

        if (effect == null) {
            return;
        }
        int skillid = attack.skill;
        if (chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) <= 0 || chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) != skillLevel) {
            if (!GameConstants.isMulungSkill(skillid) && !GameConstants.isPyramidSkill(skillid) && chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) <= 0) {
                c.getSession().close();
                return;
            }
            if (GameConstants.isMulungSkill(skillid)) {
                if (chr.getMapId() / 10000 != 92502) {
                    //AutobanManager.getInstance().autoban(c, "Using Mu Lung dojo skill out of dojo maps.");
                    return;
                } else {
                    if (chr.getMulungEnergy() < 300) {
                        return;
                    }
                    chr.mulung_EnergyModify(0);
                }
            } else if (GameConstants.isPyramidSkill(skillid)) {
                if (chr.getMapId() / 10000 != 92602 && chr.getMapId() / 10000 != 92601) {
                    //AutobanManager.getInstance().autoban(c, "Using Pyramid skill out of pyramid maps.");
                    return;
                }
            }
        }
        //attack = DamageParse.Modify_AttackCrit(attack, chr, 3, effect);
        DamageParse.critModify(chr, attack);
        if (GameConstants.isEventMap(chr.getMapId())) {
            for (MapleEventType t : MapleEventType.values()) {
                final MapleEvent e = ChannelServer.getInstance(chr.getClient().getChannel()).getEvent(t);
                if (e.isRunning() && !chr.isGM()) {
                    for (int i : e.getType().mapids) {
                        if (chr.getMapId() == i) {
                            chr.dropMessage(5, "이곳에서 스킬을 사용할 수 없습니다.");
                            return; //non-skill cannot use
                        }
                    }
                }
            }
        }
        if (effect.getCooldown() > 0) { //&& !chr.isGM() //GM 노쿨 뗐음
            if (chr.skillisCooling(attack.skill)) {
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            c.getSession().write(MaplePacketCreator.skillCooldown(attack.skill, effect.getCooldown()));
            chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown() * 1000);
        }
//        chr.checkFollow();
        if (!chr.isHidden()) {
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.showAttack(chr.getId(), attack.tbyte, chr.getLevel(), attack.skill, skillLevel, (byte) 0, attack.display, attack.speed, attack.allDamage, SendPacketOpcode.MAGIC_ATTACK, (byte) 0, attack.unk, attack.charge, 0, attack.position, 0), chr.getTruePosition());
        } else {
            chr.getMap().broadcastGMMessage(chr, MaplePacketCreator.showAttack(chr.getId(), attack.tbyte, chr.getLevel(), attack.skill, skillLevel, (byte) 0, attack.display, attack.speed, attack.allDamage, SendPacketOpcode.MAGIC_ATTACK, (byte) 0, attack.unk, attack.charge, 0, attack.position, 0), false);
        }

        DamageParse.applyAttackMagic(attack, skill, c.getPlayer(), effect, 500000);
    }

    public static final void DropMeso(final int meso, final MapleCharacter chr) {
        if (!chr.isAlive() || (meso < 10 || meso > 50000) || (meso > chr.getMeso())) {
            chr.getClient().getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        chr.gainMeso(-meso, false, true, true);
        chr.getMap().spawnMesoDrop(meso, chr.getTruePosition(), chr, chr, true, (byte) 0);
        chr.getCheatTracker().checkDrop(true);
    }

    public static final void ChangeEmotion(final int emote, final MapleCharacter chr) {
        if (emote > 7) {
            final int emoteid = 5159992 + emote;
            final MapleInventoryType type = GameConstants.getInventoryType(emoteid);
            if (chr.getInventory(type).findById(emoteid) == null) {
                chr.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM, Integer.toString(emoteid));
                return;
            }
        }
        if (emote > 0 && chr != null && chr.getMap() != null && !chr.isHidden()) { //O_o
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.facialExpression(chr, emote), false);
        }
    }

    public static final void Heal(final LittleEndianAccessor slea, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        chr.updateTick(slea.readInt());
        if (slea.available() >= 8) {
            slea.skip(4);
        }

        int healHP = slea.readShort();
        int healMP = slea.readShort();
        byte pRate = slea.readByte();

        final PlayerStats stats = chr.getStat();
        float recoveryRate = chr.getMap().getRecoveryRate();
        Item saunaCloth = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -5);
        if (saunaCloth != null) {
            switch (saunaCloth.getItemId()) {
                case 1050018:
                case 1051017:
                    if (recoveryRate != 1.0F) {
                        recoveryRate *= 1.5F;
                    }
                    break;
            }
        }
        healHP = Math.max(0, healHP);
        healMP = Math.max(0, healMP);
        if (stats.getHp() <= 0) {
            return;
        }
        //[R] 3C 00 14 00 00 46 00 00 00 01
        //[R] 3C 00 14 00 00 46 00 00 00 01
        long now = System.currentTimeMillis();
        if (healHP > 0) {
            float hpCheck = getHpRecoverCheck(chr) + 10 * recoveryRate;
            int time = 10000;
            if ((pRate & 0x1) == 1) {
                time = getEndureTime(chr);
                if (!chr.canHP(now, time)) {
                    return;
                }
                hpCheck *= 1.5F;
            }
            if ((pRate & 0x2) == 2 && chr.getChair() >= 3000000) {
                if (!chr.canHP2(now, time)) {
                    return;
                }
                switch (chr.getChair()) {
                    case 3010000:
                        hpCheck += 50;
                        break;
                    case 3010001:
                        hpCheck += 35;
                        break;
                    case 3010007:
                        hpCheck += 60;
                        break;
                    case 3010009:
                        hpCheck += 20;
                        break;
                }
            }
            if (Math.round(hpCheck) < healHP) {
                healHP = Math.round(hpCheck);
            }
            chr.addHP(healHP);
        }
        if (healMP > 0 && !GameConstants.isDemon(chr.getJob())) { //just for lag
            float mpCheck = getMpRecoverCheck(chr) + 3 * recoveryRate;
            if ((pRate & 0x1) == 1) {
                mpCheck += 20;
            }
            if ((pRate & 0x2) == 2 && chr.getChair() >= 3000000) {
                if (!chr.canMP2(now, 8000)) {
                    return;
                }
                switch (chr.getChair()) {
                    case 3010008:
                        mpCheck += 60;
                        break;
                    case 3010009:
                        mpCheck += 20;
                        break;
                }
            } else {
                if (!chr.canMP(now, 8000)) {
                    return;
                }
            }
            if (Math.round(mpCheck) < healMP) {
                healMP = Math.round(mpCheck);
            }
            chr.addMP(healMP);
        }
    }

    private static int getMpRecoverCheck(MapleCharacter p) {
        if (p.getJob() / 100 != 2) {
            if (p.getJob() >= 410 && p.getJob() <= 412) {
                int skilllevel = p.getSkillLevel(4100002);
                if (skilllevel > 0) {
                    MapleStatEffect effect = SkillFactory.getSkill(4100002).getEffect(skilllevel);
                    return effect.getMp();
                }
            }
            if (p.getJob() >= 420 && p.getJob() <= 422) {
                int skilllevel = p.getSkillLevel(4100002);
                if (skilllevel > 0) {
                    MapleStatEffect effect = SkillFactory.getSkill(4200001).getEffect(skilllevel);
                    return effect.getMp();
                }
            }
            if (p.getJob() >= 111 && p.getJob() <= 112) {
                int skilllevel = p.getSkillLevel(1110000);
                if (skilllevel > 0) {
                    MapleStatEffect effect = SkillFactory.getSkill(1110000).getEffect(skilllevel);
                    return effect.getMp();
                }
            }
            if (p.getJob() >= 121 && p.getJob() <= 122) {
                int skilllevel = p.getSkillLevel(1210000);
                if (skilllevel > 0) {
                    MapleStatEffect effect = SkillFactory.getSkill(1210000).getEffect(skilllevel);
                    return effect.getMp();
                }
            }
        }
        int skilllevel = p.getSkillLevel(2000000);
        return Math.round(skilllevel * p.getLevel() * 0.1F);
    }

    private static int getHpRecoverCheck(MapleCharacter p) {
        if (p.getJob() / 100 == 1) {
            int skilllevel = p.getSkillLevel(1000000);
            if (skilllevel > 0) {
                MapleStatEffect effect = SkillFactory.getSkill(1000000).getEffect(skilllevel);
                return effect.getHp();
            }
        } else if (p.getJob() / 100 == 4) {
            if (p.getJob() >= 410 && p.getJob() <= 412) {
                int skilllevel = p.getSkillLevel(4100002);
                if (skilllevel > 0) {
                    MapleStatEffect effect = SkillFactory.getSkill(4100002).getEffect(skilllevel);
                    return effect.getHp();
                }
            } else {
                int skilllevel = p.getSkillLevel(4200001);
                if (skilllevel > 0) {
                    MapleStatEffect effect = SkillFactory.getSkill(4200001).getEffect(skilllevel);
                    return effect.getHp();
                }
            }
        }
        return 0;
    }

    private static int getEndureTime(MapleCharacter p) {
        if (p.getJob() / 100 == 1) {
            int skilllevel = p.getSkillLevel(1000002);
            if (skilllevel > 0) {
                MapleStatEffect effect = SkillFactory.getSkill(1000002).getEffect(skilllevel);
                return effect.getDuration();
            }
        } else if (p.getJob() / 100 == 4) {
            if (p.getJob() >= 410 && p.getJob() <= 412) {
                int skilllevel = p.getSkillLevel(4100002);
                if (skilllevel > 0) {
                    MapleStatEffect effect = SkillFactory.getSkill(4100002).getEffect(skilllevel);
                    return effect.getDuration();
                }
            } else {
                int skilllevel = p.getSkillLevel(4200001);
                if (skilllevel > 0) {
                    MapleStatEffect effect = SkillFactory.getSkill(4200001).getEffect(skilllevel);
                    return effect.getDuration();
                }
            }
        }
        return 0;
    }

    public static final void MovePlayer(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        final Point Original_Pos = chr.getTruePosition(); // 4 bytes Added on v.80 MSEA
        slea.skip(13);

        // log.trace("Movement command received: unk1 {} unk2 {}", new Object[] { unk1, unk2 });
        List<LifeMovementFragment> res;
        try {
            res = MovementParse.parseMovement(slea, 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("AIOBE Type1:\n" + slea.toString(true));
            return;
        }

        if (res != null && c.getPlayer().getMap() != null) { // TODO more validation of input data
            if (slea.available() < 11 || slea.available() > 26) {
                //System.out.println("slea.available != 11-26 (movement parsing error)\n" + slea.toString(true));
                return;
            }
            final MapleMap map = c.getPlayer().getMap();

            for (final LifeMovementFragment move : res) {
                if (move instanceof AbsoluteLifeMovement) {
                    move.getUnk();
                    chr.setFh(move.getUnk());
                }
            }
            if (chr.isHidden()) {
                chr.setLastRes(res);
                c.getPlayer().getMap().broadcastGMMessage(chr, MaplePacketCreator.movePlayer(chr.getId(), res, Original_Pos), false);
            } else {
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.movePlayer(chr.getId(), res, Original_Pos), false);
            }

            MovementParse.updatePosition(res, chr, 0);
            final Point pos = chr.getTruePosition();
            map.movePlayer(chr, pos);
            if (chr.getFollowId() > 0 && chr.isFollowOn() && chr.isFollowInitiator()) {
                final MapleCharacter fol = map.getCharacterById(chr.getFollowId());
                if (fol != null) {
                    final Point original_pos = fol.getPosition();
                    fol.getClient().getSession().write(MaplePacketCreator.moveFollow(Original_Pos, original_pos, pos, res));
                    MovementParse.updatePosition(res, fol, 0);
                    map.movePlayer(fol, pos);
                    map.broadcastMessage(fol, MaplePacketCreator.movePlayer(fol.getId(), res, original_pos), false);
                } else {
                    chr.checkFollow();
                }
            }
            int count = c.getPlayer().getFallCounter();
            final boolean samepos = pos.y > c.getPlayer().getOldPosition().y && Math.abs(pos.x - c.getPlayer().getOldPosition().x) < 5;
            if (samepos && (pos.y > (map.getBottom() + 250) || map.getFootholds().findBelow(pos) == null)) {
                if (count > 5) {
                    c.getPlayer().changeMap(map, map.getPortal(0));
                    c.getPlayer().setFallCounter(0);
                } else {
                    c.getPlayer().setFallCounter(++count);
                }
            } else if (count > 0) {
                c.getPlayer().setFallCounter(0);
            }
            c.getPlayer().setOldPosition(pos);
            if (!samepos && c.getPlayer().getBuffSource(MapleBuffStat.DARK_AURA) == 32120000) { //dark aura
                c.getPlayer().getStatForBuff(MapleBuffStat.DARK_AURA).applyMonsterBuff(c.getPlayer());
            } else if (!samepos && c.getPlayer().getBuffSource(MapleBuffStat.YELLOW_AURA) == 32120001) { //yellow aura
                c.getPlayer().getStatForBuff(MapleBuffStat.YELLOW_AURA).applyMonsterBuff(c.getPlayer());
            }
        }
    }

    public static final void ChangeMapSpecial(final String portal_name, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MaplePortal portal = chr.getMap().getPortal(portal_name);
//	slea.skip(2);

        if (portal != null && !chr.hasBlockedInventory()) {
            if (chr.isGM()) {
                chr.dropMessage(6, "포탈 : " + portal.getName() + " / 스크립트 : " + portal.getScriptName());
            }
            portal.enterPortal(c);
        } else {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static final void ChangeMap(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        if (slea.available() != 0) {
            slea.skip(6); //D3 75 00 00 00 00
            slea.readByte(); // 1 = from dying 2 = regular portals
            int targetid = slea.readInt(); // FF FF FF FF
            if (!chr.isAlive()) { // allow revive even if targetid is -1 (e.g., reflect/self-damage death)
                slea.skip(3);
                byte revive = slea.readByte();
                final boolean wheel = revive > 0 && !GameConstants.isEventMap(chr.getMapId()) && chr.haveItem(5510000, 1, false, true) && chr.getMapId() / 1000000 != 925;
                chr.setStance(0);
                if (chr.getEventInstance() != null && chr.getEventInstance().revivePlayer(chr) && chr.isAlive()) {
                    return;
                }
                if (chr.getPyramidSubway() != null) {
                    chr.getStat().setHp((short) 50, chr);
                    chr.getPyramidSubway().fail(chr);
                    return;
                }
                if (revive == 1) {
                    final MapleStatEffect statss = chr.getStatForBuff(MapleBuffStat.SOUL_STONE);
                    if (statss != null) {
                        chr.setStance(0);
                        c.getSession().write(CSPacket.useSoulStone());
                        //chr.dropMessage(5, "영혼석의 효과에 의하여 현재 맵에서 부활하였습니다.");
                        chr.getStat().setHp(((chr.getStat().getMaxHp() * statss.getX()) / 100), chr);
                        chr.changeMap(chr.getMap(), chr.getMap().getPortal(0));
                        chr.cancelEffectFromBuffStat(MapleBuffStat.SOUL_STONE);
                    } else if (wheel) {
                        c.getSession().write(CSPacket.useWheel((byte) (chr.getInventory(MapleInventoryType.CASH).countById(5510000) - 1)));
                        chr.getStat().setHp(((chr.getStat().getMaxHp() * 40 / 100)), chr);
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5510000, 1, true, false);
                        final MapleMap to = chr.getMap();
                        chr.changeMap(to, to.getPortal(0));
                    } else if (chr.checkPcTime() && revive > 0) {
                        chr.getStat().setHp(((chr.getStat().getMaxHp() * 40 / 100)), chr);
                        chr.dropMessage(6, "프리미엄 PC방 효과로 현재 맵에서 부활하였습니다.");
                        final MapleMap to = chr.getMap();
                        final MaplePortal closest = to.findClosestSpawnpoint(chr.getTruePosition());
                        chr.changeMap(to, to.getPortal(closest != null ? closest.getId() : 0));
                    } else {
                        final MapleMap to = chr.getMap().getReturnMap();
                        if (to.getId() / 10000000 == 98) {
                            //carnival
                            chr.getStat().setHp((short) chr.getStat().getMaxHp() / 2, chr);
                            chr.getStat().setMp((short) chr.getStat().getMaxMp() / 2, chr);
                            chr.updateSingleStat(MapleStat.MP, chr.getStat().getMp());
                        } else {
                            chr.getStat().setHp((short) 50, chr);
                            chr.getStat().setMp((short) 0, chr);
                            chr.updateSingleStat(MapleStat.MP, chr.getStat().getMp());
                        }
                        chr.changeMap(to, to.getPortal(0));
                    }
                } else {
                    final MapleMap to = chr.getMap().getReturnMap();
                    if (to.getId() / 10000000 == 98) {
                        //carnival
                        chr.getStat().setHp((short) chr.getStat().getMaxHp() / 2, chr);
                        chr.getStat().setMp((short) chr.getStat().getMaxMp() / 2, chr);
                        chr.updateSingleStat(MapleStat.MP, chr.getStat().getMp());
                    } else {
                        chr.getStat().setHp((short) 50, chr);
                        chr.getStat().setMp((short) 0, chr);
                        chr.updateSingleStat(MapleStat.MP, chr.getStat().getMp());
                    }
                    chr.changeMap(to, to.getPortal(0));
                }

            } else if (targetid != -1) {
                final int divi = chr.getMapId() / 100;
                boolean unlock = false, warp = false;
                if (divi == 9130401) { // Only allow warp if player is already in Intro map, or else = hack
                    warp = targetid / 100 == 9130400 || targetid / 100 == 9130401; // Cygnus introduction
                    if (targetid / 10000 != 91304) {
                        warp = true;
                        unlock = true;
                        targetid = 130000000;
                    }
                } else if (divi == 9130400) { // Only allow warp if player is already in Intro map, or else = hack
                    warp = targetid / 100 == 9130400 || targetid / 100 == 9130401; // Cygnus introduction
                    if (targetid / 10000 != 91304) {
                        warp = true;
                        unlock = true;
                        targetid = 130030000;
                    }
                } else if (divi == 9140900) { // Aran Introductio
                    warp = targetid == 914090011 || targetid == 914090012 || targetid == 914090013 || targetid == 140090000;
                } else if (divi == 9120601 || divi == 9140602 || divi == 9140603 || divi == 9140604 || divi == 9140605) {
                    warp = targetid == 912060100 || targetid == 912060200 || targetid == 912060300 || targetid == 912060400 || targetid == 912060500 || targetid == 3000100;
                    unlock = true;
                } else if (divi == 9101500) {
                    warp = targetid == 910150006 || targetid == 101050010;
                    unlock = true;
                } else if (divi == 9140901 && targetid == 140000000) {
                    unlock = true;
                    warp = true;
                } else if (divi == 9240200 && targetid == 924020000) {
                    unlock = true;
                    warp = true;
                } else if (targetid == 980040000 && divi >= 9800410 && divi <= 9800450) {
                    warp = true;
                } else if (divi == 9140902 && (targetid == 140030000 || targetid == 140000000)) { //thing is. dont really know which one!
                    unlock = true;
                    warp = true;
                } else if (divi == 9000900 && targetid / 100 == 9000900 && targetid > chr.getMapId()) {
                    warp = true;
                } else if (divi / 1000 == 9000 && targetid / 100000 == 9000) {
                    unlock = targetid < 900090000 || targetid > 900090004; //1 movie
                    warp = true;
                } else if (divi / 10 == 1020 && targetid == 1020000) { // Adventurer movie clip Intro
                    unlock = true;
                    warp = true;
                } else if (chr.getMapId() == 900090101 && targetid == 100030100) {
                    unlock = true;
                    warp = true;
                } else if (chr.getMapId() == 2010000 && targetid == 104000000) {
                    unlock = true;
                    warp = true;
                } else if (chr.getMapId() == 106020001 || chr.getMapId() == 106020502) {
                    if (targetid == (chr.getMapId() - 1)) {
                        unlock = true;
                        warp = true;
                    }
                } else if (chr.getMapId() == 0 && targetid == 10000) {
                    unlock = true;
                    warp = true;
                } else if (chr.getMapId() == 931000011 && targetid == 931000012) {
                    unlock = true;
                    warp = true;
                } else if (chr.getMapId() == 931000021 && targetid == 931000030) {
                    unlock = true;
                    warp = true;
                }
                if (unlock) {
                    c.getSession().write(UIPacket.IntroDisableUI(false));
                    c.getSession().write(UIPacket.IntroLock(false));
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
                if (warp) {
                    final MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(targetid);
                    chr.changeMap(to, to.getPortal(0));
                }
            } else {
                final MaplePortal portal = chr.getMap().getPortal(slea.readMapleAsciiString());
                int returnMap = c.getPlayer().getSavedLocation(SavedLocationType.ARDENTMILL);
                /*if (chr.getMapId() == 193000000 && returnMap != -1) {
                 final MapleMap mapp = chr.getClient().getChannelServer().getMapFactory().getMap(returnMap);
                 c.getPlayer().changeMap(mapp);
                 c.getPlayer().clearSavedLocation(SavedLocationType.ARDENTMILL);
                 } else */
                if (portal != null && !chr.hasBlockedInventory()) {
                    if (chr.isGM()) {
                        chr.dropMessage(6, "포탈 : " + portal.getName() + " / 스크립트 : " + portal.getScriptName());
                    }
                    chr.removeMechDoor();
                    portal.enterPortal(c);
                } else if (chr.isGM()) {
                    MapleMap map = c.getChannelServer().getMapFactory().getMap(targetid);
                    if (map != null) {
                        chr.changeMap(map);
                    }
                } else {
//                    c.getPlayer().dropMessage(6, (portal != null) + " , " + (!chr.hasBlockedInventory()));
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
            }
        }
    }

    public static final void InnerPortal(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MaplePortal portal = chr.getMap().getPortal(slea.readMapleAsciiString());
        final int toX = slea.readShort();
        final int toY = slea.readShort();
//	slea.readShort(); // Original X pos
//	slea.readShort(); // Original Y pos
        if (chr.isGM()) {
            chr.dropMessage(6, "포탈 : " + portal.getName() + " / 스크립트 : " + portal.getScriptName());
        }
        if (portal == null) {
            return;
        } else if (portal.getPosition().distanceSq(chr.getTruePosition()) > 22500 && !chr.isGM()) {
            chr.getCheatTracker().registerOffense(CheatingOffense.USING_FARAWAY_PORTAL);
            return;
        }
        chr.getMap().movePlayer(chr, new Point(toX, toY));
    }

    public static final void snowBall(LittleEndianAccessor slea, MapleClient c) {
        //B2 00
        //01 [team]
        //00 00 [unknown]
        //89 [position]
        //01 [stage]
        c.getSession().write(MaplePacketCreator.enableActions());
        //empty, we do this in closerange
    }

    public static final void leftKnockBack(LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().getMapId() / 10000 == 10906) { //must be in snowball map or else its like infinite FJ
            //c.getPlayer().giveDebuff(MapleDisease.STUN, 1, 4000, MapleDisease.STUN.getDisease(), 1);
            //임시 주석
            c.getSession().write(MaplePacketCreator.leftKnockBack());
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static final void ReIssueMedal(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        final MapleQuest q = MapleQuest.getInstance(slea.readShort());
        if (q != null && q.getMedalItem() > 0 && chr.getQuestStatus(q.getId()) == 2 && !chr.haveItem(q.getMedalItem(), 1, true, true) && q.getMedalItem() == slea.readInt() && MapleInventoryManipulator.checkSpace(c, q.getMedalItem(), (short) 1, "")) {
            MapleInventoryManipulator.addById(c, q.getMedalItem(), (short) 1, "Redeemed item through medal quest " + q.getId() + " on " + FileoutputUtil.CurrentReadable_Date());
        }
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void ChangeMonsterBookCover(final int bookid, final MapleClient c, final MapleCharacter chr) {
        if (bookid == 0 || GameConstants.isMonsterCard(bookid)) {
            chr.setMonsterBookCover(bookid);
            chr.getMonsterBook().updateCard(c, bookid);
        }
    }

    public static final void AranCombo(final MapleClient c, final MapleCharacter chr, int toAdd) {
        if (chr != null && chr.getJob() >= 2000 && chr.getJob() <= 2112) {
            short combo = chr.getCombo();
            final long curr = System.currentTimeMillis();

            if (combo > 0 && (curr - chr.getLastCombo()) > 7000) {
                // Official MS timing is 3.5 seconds, so 7 seconds should be safe.
                //chr.getCheatTracker().registerOffense(CheatingOffense.ARAN_COMBO_HACK);
                combo = 0;
            }
            combo = (short) Math.min(30000, combo + toAdd);
            chr.setLastCombo(curr);
            chr.setCombo(combo);

            c.getSession().write(MaplePacketCreator.testCombo(combo));

            switch (combo) { // Hackish method xD
                case 10:
                case 20:
                case 30:
                case 40:
                case 50:
                case 60:
                case 70:
                case 80:
                case 90:
                case 100:
                    if (chr.getSkillLevel(21000000) >= (combo / 10)) {
                        SkillFactory.getSkill(21000000).getEffect(combo / 10).applyComboBuff(chr, combo);
                    }
                    break;
            }
        }
    }
}
