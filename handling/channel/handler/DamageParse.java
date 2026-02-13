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
import client.anticheat.CheatTracker;
import client.anticheat.CheatingOffense;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import handling.RecvPacketOpcode;
import server.AutobanManager;
import server.MapleStatEffect;
import server.Randomizer;
import server.life.Element;
import server.life.ElementalEffectiveness;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.AttackPair;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.TemporaryStatsPacket;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import server.MapleItemInformationProvider;
import server.maps.MapleMist;
import tools.packet.MobPacket;

public class DamageParse {

    private static byte masteryByte;

    public static int getSnipingDamage(final MapleCharacter player) {
        if (player == null) {
            return 500000;
        }
        final PlayerStats stats = player.getStat();
        stats.calculateMaxBaseDamage(player);
        long fixed = (long) Math.floor(stats.getCurrentMaxBaseDamage() * 10.0D);
        if (fixed <= 500000L) {
            fixed = 500000L;
        }
        if (fixed > (long) Integer.MAX_VALUE) {
            fixed = (long) Integer.MAX_VALUE;
        }
        return (int) fixed;
    }

    public static void applySnipingDisplayDamage(final AttackInfo attack, final MapleCharacter player) {
        if (attack == null || player == null || attack.skill != 3221007 || attack.allDamage == null) {
            return;
        }
        final int fixed = getSnipingDamage(player);
        for (final AttackPair oned : attack.allDamage) {
            if (oned == null || oned.attack == null) {
                continue;
            }
            int overallAttackCount = 0;
            for (final Pair<Integer, Boolean> eachde : oned.attack) {
                overallAttackCount++;
                eachde.left = overallAttackCount > 3 ? 0 : fixed;
            }
        }
    }

    public static void applyAttack(final AttackInfo attack, final Skill theSkill, final MapleCharacter player, int attackCount, final double maxDamagePerMonster, final MapleStatEffect effect, final AttackType attack_type) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (player == null) {
            return;
        }
        if (!player.isAlive()) {
            player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }

        if (player.getBuffedValue(MapleBuffStat.DARKSIGHT) != null) {
            Skill darksight = SkillFactory.getSkill(4330001);
            int darklevel = player.getSkillLevel(darksight);
            if (darklevel > 0) {
                if (Randomizer.nextInt(100) > darksight.getEffect(darklevel).getProb()) {
                    player.cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
                }
            } else {
                player.cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
            }
        }
        if (player.isGM() && !player.hasGmLevel(6)) {
            boolean isBossMap = player.getMapId() == 280030000 || player.getMapId() == 220080001 || player.getMapId() == 230040420 || player.getMapId() == 240060000 || player.getMapId() == 240060100 || player.getMapId() == 240060200 || player.getMapId() == 240060002 || player.getMapId() == 240060102 || player.getMapId() == 240060300;
            if (isBossMap) {
                player.dropMessage(5, "GM은 이곳에서 공격할 수 없습니다. - 데미지가 적용되지 않습니다.");
                player.getClient().sendPacket(MaplePacketCreator.enableActions());
                return;
            }
        }
        /*if (!player.checkOwnerMap()) {
         return;
         }*/
        if (attack.real && GameConstants.getAttackDelay(attack.skill, theSkill) >= 100) {
            player.getCheatTracker().checkAttack(attack.skill, attack.lastAttackTickCount);
        }
        if (attack.skill != 0) {
            if (effect == null) {
                player.getClient().getSession().write(MaplePacketCreator.enableActions());
                return;
            }
            if (GameConstants.isPyramidSkill(attack.skill)) {
                if (player.getMapId() / 1000000 != 926) {
                    //AutobanManager.getInstance().autoban(player.getClient(), "Using Pyramid skill outside of pyramid maps.");
                    return;
                } else if (player.getPyramidSubway() == null || !player.getPyramidSubway().onSkillUse(player)) {
                    return;
                }
            }
            //if (attack.targets > effect.getMobCount() && attack.skill != 1211002 && attack.skill != 1220010) { // Must be done here, since NPE with normal atk
            //    player.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT);
            //    return;
            //}
        }
        if (player.getClient().getChannelServer().isAdminOnly()) {
            player.dropMessage(-1, "Animation: " + Integer.toHexString(((attack.display & 0x7F) != 0 ? (attack.display - 0x7F) : attack.display)));
        }
        if (attack.hits > 0 && attack.targets > 0) {
            player.getStat().checkEquipDurabilitys(player, -1, false, false);
            // Don't ever do this. it's too expensive.
            /*if (!player.getStat().checkEquipDurabilitys(player, -10)) { //i guess this is how it works ?
             player.dropMessage(5, "An item has run out of durability but has no inventory room to go to.");
             return;
             } //lol*/
        }
        //final boolean useAttackCount = attack.skill != 4211006 && attack.skill != 3221007 && attack.skill != 23121003 && (attack.skill != 1311001 || player.getJob() != 132) && attack.skill != 3211006;
        //if (attack.hits > attackCount) {
        //    if (useAttackCount) { //buster
        //        player.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT);
        //        return;
        //    }
        //}
        int totDamage = 0;
        final MapleMap map = player.getMap();

        if (attack.skill == 4211006) { // meso explosion
            for (AttackPair oned : attack.allDamage) {
                if (oned.attack != null) {
                    continue;
                }
                final MapleMapObject mapobject = map.getMapObject(oned.objectid, MapleMapObjectType.ITEM);

                if (mapobject != null) {
                    final MapleMapItem mapitem = (MapleMapItem) mapobject;
                    mapitem.getLock().lock();
                    try {
                        if (mapitem.getMeso() > 0) {
                            if (mapitem.isPickedUp()) {
                                return;
                            }
                            map.removeMapObject(mapitem);
                            map.broadcastMessage(MaplePacketCreator.explodeDrop(mapitem.getObjectId()));
                            mapitem.setPickedUp(true);
                        } else {
                            player.getCheatTracker().registerOffense(CheatingOffense.ETC_EXPLOSION);
                            return;
                        }
                    } finally {
                        mapitem.getLock().unlock();
                    }
                } else {
                    player.getCheatTracker().registerOffense(CheatingOffense.EXPLODING_NONEXISTANT);
                    return; // etc explosion, exploding nonexistant things, etc.
                }
            }
        }
        int fixeddmg, totDamageToOneMonster = 0;
        long hpMob = 0;
        final PlayerStats stats = player.getStat();

        int CriticalDamage = stats.getSharpEyeDam();
        int ShdowPartnerAttackPercentage = 0;
        int shadowPartnerSource = 0;
        int shadowPartnerX = 0;
        if (attack_type == AttackType.RANGED_WITH_SHADOWPARTNER || attack_type == AttackType.NON_RANGED_WITH_MIRROR) {
            final MapleStatEffect shadowPartnerEffect = player.getStatForBuff(MapleBuffStat.SHADOWPARTNER);
            if (shadowPartnerEffect != null) {
                ShdowPartnerAttackPercentage += shadowPartnerEffect.getX();
                shadowPartnerSource = shadowPartnerEffect.getSourceId();
                shadowPartnerX = shadowPartnerEffect.getX();
            }
            attackCount /= 2; // hack xD
        }
        ShdowPartnerAttackPercentage *= (CriticalDamage + 100) / 100;
        if (attack.skill == 4221001) { //amplifyDamage
            ShdowPartnerAttackPercentage *= 10;
        }
        byte overallAttackCount; // Tracking of Shadow Partner additional damage.
        MapleMonster monster;
        MapleMonsterStats monsterstats;
        boolean Tempest;

        int v22 = Math.max(0, player.getStat().getAccuracy());

        for (final AttackPair oned : attack.allDamage) {
            monster = map.getMonsterByOid(oned.objectid);
            if (monster != null && monster.getLinkCID() <= 0) {
                monsterstats = monster.getStats();
                totDamageToOneMonster = 0;
                hpMob = monster.getMobMaxHp();
                fixeddmg = monsterstats.getFixedDamage();
                Tempest = monster.getStatusSourceID(MonsterStatus.FREEZE) == 21120006 || attack.skill == 21120006;
                if (!Tempest && !player.isGM()) {
                    if (attack.skill == 1009 || attack.skill == 10001009) { //죽간천격
                        //maxDamagePerHit = hpMob;
                    }
                }
//                maxDamagePerHit = CalculateMaxWeaponDamagePerHit(player, monster, attack, theSkill, effect, CriticalDamage, attack.display);
                overallAttackCount = 0; // Tracking of Shadow Partner additional damage.
                Integer eachd;
                final boolean nwShadowPartnerFix = (shadowPartnerSource == 14111000 && shadowPartnerX > 0);
                final int[] mainDamages;
                if (nwShadowPartnerFix) {
                    int n = Math.min(attackCount, oned.attack.size());
                    mainDamages = new int[n];
                    for (int i = 0; i < n; i++) {
                        mainDamages[i] = oned.attack.get(i).left;
                    }
                } else {
                    mainDamages = null;
                }

                for (Pair<Integer, Boolean> eachde : oned.attack) {
                    eachd = eachde.left;
                    overallAttackCount++;
                    final boolean useAttackCount = attack.skill != 4211006 && attack.skill != 3221007 && attack.skill != 23121003 && (attack.skill != 1311001 || player.getJob() != 132) && attack.skill != 3211006;

                    if (nwShadowPartnerFix && useAttackCount && overallAttackCount > attackCount) {
                        int idx = overallAttackCount - attackCount - 1; // 0-based
                        if (mainDamages != null && idx >= 0 && idx < mainDamages.length) {
                            long calc = (long) mainDamages[idx] * (long) shadowPartnerX / 100L;
                            if (calc < 1L) {
                                calc = 1L;
                            }
                            if (calc > Integer.MAX_VALUE) {
                                calc = Integer.MAX_VALUE;
                            }
                            eachd = (int) calc;
                        }
                    }

//                    if (useAttackCount && overallAttackCount - 1 == attackCount) { // Is a Shadow partner hit so let's divide it once
//                        maxDamagePerHit = (maxDamagePerHit / 100) * (ShdowPartnerAttackPercentage * (monsterstats.isBoss() ? stats.bossdam_r : stats.dam_r) / 100);
//                    }
                    //player.dropMessage(6, "Client damage : " + eachd + " Server : " + maxDamagePerHit);
                    // System.out.println("Client damage : " + eachd + " Server : " + maxDamagePerHit);
                    if (fixeddmg != -1) {
                        if (monsterstats.getOnlyNoramlAttack()) {
                            eachd = attack.skill != 0 ? 0 : fixeddmg;
                        } else {
                            eachd = fixeddmg;
                        }
                    } else {
                        /*if (monsterstats.getOnlyNoramlAttack()) {
                         eachd = attack.skill != 0 ? 0 : Math.min(eachd, (int) maxDamagePerHit);  // Convert to server calculated damage
                         } else if (!player.isGM()) {
                         if (Tempest) { // Buffed with Tempest
                         // In special case such as Chain lightning, the damage will be reduced from the maxMP.
                         if (eachd > monster.getMobMaxHp()) {
                         eachd = (int) Math.min(monster.getMobMaxHp(), Integer.MAX_VALUE);
                         player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_MAGIC);
                         }
                         } else if ((!monster.isBuffed(MonsterStatus.WEAPON_DAMAGE_REFLECT) && !monster.isBuffed(MonsterStatus.DAMAGE_IMMUNITY) && !monster.isBuffed(MonsterStatus.WEAPON_IMMUNITY))) {
                         if (eachd > maxDamagePerHit) {
                         player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE, "[Damage: " + eachd + ", Expected: " + maxDamagePerHit + ", Mob: " + monster.getId() + "] [Job: " + player.getJob() + ", Level: " + player.getLevel() + ", Skill: " + attack.skill + "]");
                         if (attack.real) {
                         player.getCheatTracker().checkSameDamage(eachd, maxDamagePerHit);
                         }
                         if (eachd > maxDamagePerHit * 4) {
                         player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_2, "[Damage: " + eachd + ", Expected: " + (maxDamagePerHit * 4) + ", Mob: " + monster.getId() + "] [Job: " + player.getJob() + ", Level: " + player.getLevel() + ", Skill: " + attack.skill + "]");
                         eachd = (int) (maxDamagePerHit * 4); // Convert to server calculated damage
                         if (eachd >= 999999) { //ew
                         player.getClient().getSession().close(true);
                         return;
                         }
                         }
                         }
                         } else {
                         if (eachd > maxDamagePerHit) {
                         eachd = (int) (maxDamagePerHit);
                         }
                         }
                         }*/
                    }
                    if (player == null) { // o_O
                        return;
                    }
                    if (attack.skill == 3221007) { // 스나이핑 데미지
                        eachd = getSnipingDamage(player);
                        if (overallAttackCount > 3) { // 혹시 패킷이 3타 이상으로 오면 초과타는 무시(악용 방지)
                            eachd = 0;
                        }
                    }

                    totDamageToOneMonster += eachd;
                    //force the miss even if they dont miss. popular wz edit
                    if (attack.skill == 4331003 && attack.skill != 3221007 && eachd == monster.getMobMaxHp()) {//아울데드 즉사
                        MapleStatEffect owldead = SkillFactory.getSkill(attack.skill).getEffect(player.getTotalSkillLevel(attack.skill));
                        player.setBattleshipHP(owldead.getX());
                    }
                    if (attack.skill != 4331003 && attack.skill != 3221007 && eachd == monster.getMobMaxHp()) {//아이템 즉사스킬
                        final Skill skill = SkillFactory.getSkill(90000000);
                        int skilllvl = player.getSkillLevel(skill);
                        final MapleStatEffect eff = skill.getEffect(skilllvl);//위젯에 1레벨짜리 스킬로댐
                        if (eff != null && skilllvl >= 1) {
                            final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.BLIND, 1, 90000000, null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, 1000000, true, eff); //1초
                            player.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(90000000, 1, player.getLevel(), skilllvl, (byte) 1));
                            player.getMap().broadcastMessage(MaplePacketCreator.showBuffeffect(player.getId(), 90000000, 1, player.getLevel(), skilllvl, (byte) 1), player.getPosition());
                        }
                    }
                    if (eachd == 0 && player.getPyramidSubway() != null) { //miss
                        player.getPyramidSubway().onMiss(player);
                    }
                }
                totDamage += totDamageToOneMonster;
                player.checkMonsterAggro(monster);

                if (GameConstants.getAttackDelay(attack.skill, theSkill) >= 100 && !GameConstants.isNoDelaySkill(attack.skill) && attack.skill != 3101005 && !monster.getStats().isBoss() && player.getTruePosition().distanceSq(monster.getTruePosition()) > GameConstants.getAttackRange(effect, player.getStat().defRange)) {
                    player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER, "[Distance: " + player.getTruePosition().distanceSq(monster.getTruePosition()) + ", Expected Distance: " + GameConstants.getAttackRange(effect, player.getStat().defRange) + " Job: " + player.getJob() + "]"); // , Double.toString(Math.sqrt(distance))
                }
                // pickpocket
                if (player.getBuffedValue(MapleBuffStat.PICKPOCKET) != null) {
                    switch (attack.skill) {
                        //4001334 || attack.skill == 4201005 || attack.skill == 0 || attack.skill == 4211002 || attack.skill == 4211004
                        case 0:
                        case 4001334:
                        case 4201005:
                        case 4211002:
                        case 4211004:
                        case 4221003:
                        case 4221007:
                            handlePickPocket(player, monster, oned);
                            break;
                    }
                }

                if (!monster.getStats().isBoss()) {
                    if (player.hasEquipped(1402073)) {//아스카론
                        final Skill skill = SkillFactory.getSkill(90000000);
                        final MapleStatEffect eff = skill.getEffect(1);//위젯에 1레벨짜리 스킬로댐
                        if (Randomizer.nextInt(100) <= 1) {//1퍼센트확률
                            final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.BLIND, 1, 90000000, null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, 1000, true, eff); //1초
                            monster.damage(player, monster.getHp(), true, attack.skill, System.currentTimeMillis());
                            //player.getMap().broadcastMessage(player, MobPacket.damageMonster(monster.getObjectId(), monster.getHp()), false);//이거안쏴주면 데미지안보임         
                            //player.getMap().broadcastMessage(MobPacket.damageMonster(monster.getObjectId(), monster.getHp()), player.getPosition());
                            player.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(90000000, 1, player.getLevel(), 1, (byte) 1));
                            player.getMap().broadcastMessage(MaplePacketCreator.showBuffeffect(player.getId(), 90000000, 1, player.getLevel(), 1, (byte) 1), player.getPosition());
                        }
                    }
                    if (player.hasEquipped(1492024)) {//템페스트
                        final Skill skill = SkillFactory.getSkill(90000000);
                        final MapleStatEffect eff = skill.getEffect(1);//위젯에 1레벨짜리 스킬로댐
                        if (Randomizer.nextInt(100) <= 1) {//1퍼센트확률
                            final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.BLIND, 1, 90000000, null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, 1000, true, eff); //1초
                            monster.damage(player, monster.getHp(), true, attack.skill, System.currentTimeMillis());
                            //player.getMap().broadcastMessage(player, MobPacket.damageMonster(monster.getObjectId(), monster.getHp()), false);//이거안쏴주면 데미지안보임         
                            //player.getMap().broadcastMessage(MobPacket.damageMonster(monster.getObjectId(), monster.getHp()), player.getPosition());
                            player.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(90000000, 1, player.getLevel(), 1, (byte) 1));
                            player.getMap().broadcastMessage(MaplePacketCreator.showBuffeffect(player.getId(), 90000000, 1, player.getLevel(), 1, (byte) 1), player.getPosition());
                        }
                    }
                }
                if (totDamageToOneMonster > 0 || attack.skill == 1221011 || attack.skill == 3221007 || attack.skill == 21120006) {
                    if (attack.skill != 1221011) {
                        monster.damage(player, totDamageToOneMonster + player.getExtraDamage(), true, attack.skill, System.currentTimeMillis());
                    } else { //생츄어리
                        monster.damage(player, (monster.getStats().isBoss() ? 500000 : (monster.getHp() - 1)), true, attack.skill, System.currentTimeMillis());
                    }

                    if (monster.isBuffed(MonsterStatus.WEAPON_DAMAGE_REFLECT) && attack.skill != 3120010) {//벤젼스
                        int damage = 0;
                        damage = Randomizer.rand(stats.getCurrentMaxHp() / 50, stats.getCurrentMaxHp());
                        //player.dropMessage(6,"damage"+damage);
                        player.addHP(-damage);
                        player.getClient().getSession().write(MaplePacketCreator.playerDamage(player.getId(), damage));
                        //damage = monster.getBuff(MonsterStatus.WEAPON_DAMAGE_REFLECT).getX() + Randomizer.nextInt() - getRef / 2;
                    }

                    if (player.getMapId() / 100000 == 9250) { // 무릉
                        if (!monster.getStats().isBoss()) {
                            int guageUp = (int) Math.max(1, Math.floor(Math.min(totDamageToOneMonster, monster.getStats().getHp()) / (double) monster.getStats().getHp() / (0.1D / 5.0D)));
                            player.mulung_EnergyModify(guageUp);
                        }
                    }

                    player.onAttack(monster.getMobMaxHp(), monster.getMobMaxMp(), attack.skill, monster.getObjectId(), totDamage);
                    switch (attack.skill) {
                        case 14001004:
                        case 14111002:
                        case 14111005:
                        case 4301001:
                        case 4311002:
                        case 4311003:
                        case 4331000:
                        case 4331004:
                        case 4331005:
                        case 4341002:
                        case 4341004:
                        case 4341005:
                        case 4331006:
                        case 4341009:
                        case 4221007: // Boomerang Stab
                        case 4221001: // Assasinate
                        case 4211002: // Assulter
                        case 4201005: // Savage Blow
                        case 4001002: // Disorder
                        case 4001334: // Double Stab
                        case 4121007: // Triple Throw
                        case 4111005: // Avenger
                        case 4001344: { // Lucky Seven
                            // Venom
                            int[] skills = {4120005, 4220005, 4340001, 14110004};
                            for (int i : skills) {
                                final Skill skill = SkillFactory.getSkill(i);
                                if (player.getTotalSkillLevel(skill) > 0) {
                                    final MapleStatEffect venomEffect = skill.getEffect(player.getTotalSkillLevel(skill));
                                    if (!venomEffect.makeChanceResult()) {
                                        break;
                                    }
                                    monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.POISON, 1, i, null, false), true, venomEffect.getDuration(), true, venomEffect);
                                    break;
                                }
                            }

                            break;
                        }
                        case 4201004: { //steal
                            monster.handleSteal(player);
                            break;
                        }
                        case 21000002: // Double attack
                        case 21100001: // Triple Attack
                        case 21100002: // Pole Arm Push
                        case 21100004: // Pole Arm Smash
                        case 21110002: // Full Swing
                        case 21110003: // Pole Arm Toss
                        case 21110004: // Fenrir Phantom
                        case 21110006: // Whirlwind
                        case 21110007: // (hidden) Full Swing - Double Attack
                        case 21110008: // (hidden) Full Swing - Triple Attack
                        case 21120002: // Overswing
                        case 21120005: // Pole Arm finale
                        case 21120006: // Tempest
                        case 21120009: // (hidden) Overswing - Double Attack
                        case 21120010: { // (hidden) Overswing - Triple Attack
                            if (player.getBuffedValue(MapleBuffStat.WK_CHARGE) != null && !monster.getStats().isBoss()) {
                                final MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.WK_CHARGE);
                                if (eff != null) {
                                    monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.SPEED, eff.getX(), eff.getSourceId(), null, false), false, eff.getY() * 1000, true, eff);
                                }
                            }
                            if (player.getBuffedValue(MapleBuffStat.BODY_PRESSURE) != null && !monster.getStats().isBoss()) {
                                final MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.BODY_PRESSURE);

                                if (eff != null && eff.makeChanceResult() && !monster.isBuffed(MonsterStatus.NEUTRALISE)) {
                                    monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.NEUTRALISE, 1, eff.getSourceId(), null, false), false, eff.getX() * 1000, true, eff);
                                }
                            }
                            break;
                        }
                        default: //passives attack bonuses
                            break;
                    }
                    if (totDamageToOneMonster > 0) {
                        Item weapon_ = player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
                        if (weapon_ != null) {
                            MonsterStatus stat = GameConstants.getStatFromWeapon(weapon_.getItemId()); //10001 = acc/darkness. 10005 = speed/slow.
                            if (stat != null && Randomizer.nextInt(100) < GameConstants.getStatChance()) {
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(stat, GameConstants.getXForStat(stat), GameConstants.getSkillForStat(stat), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, 10000, false, null);
                            }
                        }
                        if (player.hasEquipped(1332100)) {//나겔링
                            final Skill skill = SkillFactory.getSkill(90001006);
                            final MapleStatEffect eff = skill.getEffect(1);//위젯에 1레벨짜리 스킬로댐
                            if (Randomizer.nextInt(100) <= 10) { //10% 확률     
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.FREEZE, 1, 90001006, null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, 1000, true, eff); //1초
                                player.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(90001006, 1, player.getLevel(), 1, (byte) 1));
                                player.getMap().broadcastMessage(MaplePacketCreator.showBuffeffect(player.getId(), 90001006, 1, player.getLevel(), 1, (byte) 1), player.getPosition());
                            }
                        }
                        if (player.hasEquipped(1342009)) {//용연도
                            final Skill skill = SkillFactory.getSkill(90001004);
                            final MapleStatEffect eff = skill.getEffect(1);//위젯에 1레벨짜리 스킬로댐
                            if (Randomizer.nextInt(100) <= 10) { //10% 확률     
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.BLIND, 1, 90001004, null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, 1000, true, eff); //1초
                                player.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(90001004, 1, player.getLevel(), 1, (byte) 1));
                                player.getMap().broadcastMessage(MaplePacketCreator.showBuffeffect(player.getId(), 90001004, 1, player.getLevel(), 1, (byte) 1), player.getPosition());
                            }
                        }
                        if (player.hasEquipped(1482051)) {//크루시오
                            final Skill skill = SkillFactory.getSkill(90001001);
                            final MapleStatEffect eff = skill.getEffect(1);//위젯에 1레벨짜리 스킬로댐
                            if (Randomizer.nextInt(100) <= 10) { //10% 확률     
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.STUN, 1, 90001001, null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, 1000, true, eff); //1초
                                player.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(90001001, 1, player.getLevel(), 1, (byte) 1));
                                player.getMap().broadcastMessage(MaplePacketCreator.showBuffeffect(player.getId(), 90001001, 1, player.getLevel(), 1, (byte) 1), player.getPosition());
                            }
                        }
                        /*임시처리*/
                        Equip equip = (Equip) weapon_;
                        int poten1 = equip.getPotential1();
                        int poten2 = equip.getPotential2();
                        int poten3 = equip.getPotential3();
                        final int itemLevel = ii.getReqLevel(equip.getItemId());
                        if (poten1 == 10241 || poten2 == 10241 || poten3 == 10241) {//빙결옵션
                            final Skill skill = SkillFactory.getSkill(1211006);
                            final MapleStatEffect eff = skill.getEffect(1);
                            if (Randomizer.nextInt(100) <= 5) { //5% 확률                            
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.FREEZE, 1, skill.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 71 ? 1000 : 2000, true, eff); //2레벨 빙결효과
                            }
                        }
                        if (poten1 == 10221 || poten2 == 10221 || poten3 == 10221) { //포이즌
                            final Skill skill = SkillFactory.getSkill(2101005);
                            final MapleStatEffect eff = skill.getEffect(1);
                            int randomdamage = Randomizer.nextInt(3);
                            if (Randomizer.nextInt(100) <= 100) { //10% 확률                            
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.POISON, (3 * (equip.getMatk() == 0 ? equip.getWatk() : equip.getWatk() != 0 ? (int) (equip.getWatk() + equip.getMatk() / 1.4) : equip.getMatk()) * randomdamage), skill.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 21 ? 1000 : itemLevel < 41 ? 2000 : itemLevel < 61 ? 3000 : itemLevel < 81 ? 4000 : itemLevel < 101 ? 5000 : 6000, true, eff); //2레벨 포이즌효과?
                            }
                        }
                        if (poten1 == 10226 || poten2 == 10226 || poten3 == 10226) {//스턴
                            final Skill skill = SkillFactory.getSkill(1121001);
                            final MapleStatEffect eff = skill.getEffect(1);
                            if (Randomizer.nextInt(100) <= 5) { //5% 확률                            
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.STUN, 1, skill.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 71 ? 1000 : 2000, true, eff); //스턴효과
                            }
                        }
                        if (poten1 == 10231 || poten2 == 10231 || poten3 == 10231) {//슬로우
                            final Skill skill = SkillFactory.getSkill(2101003);
                            final MapleStatEffect eff = skill.getEffect(1);
                            final int slowX = itemLevel < 71 ? 10 : 20;
                            if (Randomizer.nextInt(100) <= 10) { //10% 확률                            
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.SPEED, -4 * slowX, skill.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 71 ? 2000 : 4000, true, eff); //슬로우
                            }
                        }
                        if (poten1 == 10236 || poten2 == 10236 || poten3 == 10236) {//암흑
                            final Skill skill = SkillFactory.getSkill(3221006);
                            final MapleStatEffect eff = skill.getEffect(1);
                            final int blindX = itemLevel < 71 ? 10 : itemLevel < 101 ? 20 : 30;
                            if (Randomizer.nextInt(100) <= 10) { //10% 확률                            
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.ACC, blindX, skill.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 71 ? 2000 : itemLevel < 101 ? 4000 : 9000, true, eff); //암흑
                                //player.dropMessage(5, "applied!");
                            }
                        }
                        if (poten1 == 10246 || poten2 == 10246 || poten3 == 10246) {//봉인
                            final Skill skill = SkillFactory.getSkill(2111004);
                            final MapleStatEffect eff = skill.getEffect(1);
                            if (Randomizer.nextInt(100) <= 5) { //5% 확률                            
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.SEAL, 1, skill.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 71 ? 1000 : 2000, true, eff); //봉인
                            }
                        }
                        if (player.getBuffedValue(MapleBuffStat.BLIND) != null) {
                            final MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.BLIND);

                            if (eff != null && eff.makeChanceResult()) {
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.ACC, eff.getX(), eff.getSourceId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, eff.getY() * 1000, true, eff);
                            }

                        }
                        if (player.getBuffedValue(MapleBuffStat.HAMSTRING) != null) {
                            final MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.HAMSTRING);

                            if (eff != null && eff.makeChanceResult()) {
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.SPEED, eff.getX(), 3121007, null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, eff.getY() * 1000, true, eff);
                            }
                        }
                        if (player.getJob() == 121 || player.getJob() == 122) { // WHITEKNIGHT
                            final Skill icecharge = SkillFactory.getSkill(1211006); // Ice Charge - Blunt
                            if (player.isBuffFrom(MapleBuffStat.WK_CHARGE, icecharge)) {
                                final MapleStatEffect eff = icecharge.getEffect(player.getTotalSkillLevel(icecharge));
                                final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.FREEZE, 1, icecharge.getId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, eff.getY() * 2000, true, eff);
                            }
                        }
                        if (player.getJob() == 312 && attack.skill == 3120010) { // 벤젼스
                            final Skill vengeance = SkillFactory.getSkill(3120010);
                            final MapleStatEffect eff = vengeance.getEffect(player.getTotalSkillLevel(vengeance));
                            final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.STUN, eff.getX(), vengeance.getId(), null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, eff.getX() * 1000, true, eff);
                        }
                    }
                    if (effect != null && effect.getMonsterStati().size() > 0) {
                        if (effect.makeChanceResult()) {
                            for (Map.Entry<MonsterStatus, Integer> z : effect.getMonsterStati().entrySet()) {
                                final int statusValue = z.getKey() == MonsterStatus.POISON ? Math.max(1, totDamageToOneMonster) : z.getValue();
                                monster.applyStatus(player, new MonsterStatusEffect(z.getKey(), statusValue, theSkill.getId(), null, false), effect.isPoison(), effect.getDuration(), true, effect);
                            }
                        }
                    }
                }
            }
        }
        if (hpMob > 0 && totDamageToOneMonster > 0) {
            player.afterAttack(attack.targets, attack.hits, attack.skill);
        }
        if (!GameConstants.isNoDelaySkill(attack.skill)) {
            switch (attack.skill) {
                case 0: // 기본 공격
                    break;
                case 1311005: // 세크리 파이스
                    int dama = Math.min(player.getStat().getHp() - 1, totDamage * theSkill.getEffect(player.getSkillLevel(theSkill)).getX() / 100);
                    effect.applyTo(player, attack.position);
                    break;
                case 4331003: // 아울 데드
                    if (player.currentBattleshipHP() > 0) {
                        effect.applyTo(player, attack.position);
                    } else {
                        player.addMP(-effect.getMPCon());
                    }
                    break;
                case 4341002: // 파이널 컷
                    if (attack.targets > 0) {
                        player.setFinalCut((int) ((double) effect.getY() * Final_Cut_Damage(attack.charge) / 100));
                        effect.applyTo(player, attack.position);
                        player.addHP((int) -(Math.min((double) player.getStat().getMaxHp() * effect.getX() * Final_Cut_Damage(attack.charge) * 0.0001, player.getStat().getHp() - 1)));
                    } else {
                        player.addHP((int) -(Math.min((double) player.getStat().getMaxHp() * effect.getX() * Final_Cut_Damage(attack.charge) * 0.0001, player.getStat().getHp() - 1)));
                        player.addMP(-effect.getMPCon());
                    }
                    break;
                default:
                    if (!GameConstants.isThrowSkill(attack.skill)) { // 투척 스킬은 X
                        effect.applyTo(player, attack.position);
                    }
                    break;
            }
        }
        if (totDamage > 1 && GameConstants.getAttackDelay(attack.skill, theSkill) >= 100) {
            final CheatTracker tracker = player.getCheatTracker();

            tracker.setAttacksWithoutHit(true);
            if (tracker.getAttacksWithoutHit() > 1000) {
                tracker.registerOffense(CheatingOffense.ATTACK_WITHOUT_GETTING_HIT, Integer.toString(tracker.getAttacksWithoutHit()));
            }
        }
    }

    public static int Final_Cut_Damage(int charge) {
        switch (charge) {
            case 30:
                return 14;
            case 60:
                return 19;
            case 90:
                return 23;
            case 120:
                return 28;
            case 150:
                return 32;
            case 180:
                return 37;
            case 210:
                return 41;
            case 240:
                return 46;
            case 270:
                return 50;
            case 300:
                return 55;
            case 330:
                return 59;
            case 360:
                return 64;
            case 390:
                return 68;
            case 420:
                return 73;
            case 450:
                return 77;
            case 480:
                return 82;
            case 510:
                return 86;
            case 540:
                return 91;
            case 570:
                return 95;
            case 600:
                return 100;
            default:
                return 100;
        }
    }

    public static final void applyAttackMagic(final AttackInfo attack, final Skill theSkill, final MapleCharacter player, final MapleStatEffect effect, double maxDamagePerHit) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (!player.isAlive()) {
            player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        if (player.isGM() && !player.hasGmLevel(6)) {
            boolean isBossMap = player.getMapId() == 280030000 || player.getMapId() == 220080001 || player.getMapId() == 230040420 || player.getMapId() == 240060000 || player.getMapId() == 240060100 || player.getMapId() == 240060200 || player.getMapId() == 240060002 || player.getMapId() == 240060102 || player.getMapId() == 240060300;
            if (isBossMap) {
                player.dropMessage(5, "GM은 이곳에서 공격할 수 없습니다. - 데미지가 적용되지 않습니다.");
                player.getClient().sendPacket(MaplePacketCreator.enableActions());
                return;
            }
        }
        /*if (!player.checkOwnerMap()) {
         return;
         }*/
        if (attack.real && GameConstants.getAttackDelay(attack.skill, theSkill) >= 100) {
            player.getCheatTracker().checkAttack(attack.skill, attack.lastAttackTickCount);
        }
        if (GameConstants.isPyramidSkill(attack.skill)) {
            if (player.getMapId() / 1000000 != 926) {
                //AutobanManager.getInstance().autoban(player.getClient(), "Using Pyramid skill outside of pyramid maps.");
                return;
            } else if (player.getPyramidSubway() == null || !player.getPyramidSubway().onSkillUse(player)) {
                return;
            }
        }
//	if (attack.skill != 2301002) { // heal is both an attack and a special move (healing) so we'll let the whole applying magic live in the special move part
//	    effect.applyTo(player);
//	}
        //if (attack.hits > effect.getAttackCount() || attack.targets > effect.getMobCount()) {
        //    player.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT);
        //    return;
        //}
        if (player.getClient().getChannelServer().isAdminOnly()) {
            player.dropMessage(-1, "Animation: " + Integer.toHexString(((attack.display & 0x7F) != 0 ? (attack.display - 0x7F) : attack.display)));
        }
        final PlayerStats stats = player.getStat();
        final Element element = theSkill.getElement();

        double MaxDamagePerHit = 0;
        int totDamageToOneMonster, totDamage = 0, fixeddmg;
        byte overallAttackCount;
        boolean Tempest;
        MapleMonsterStats monsterstats;
        int CriticalDamage = stats.getSharpEyeDam();
        final Skill eaterSkill = SkillFactory.getSkill(GameConstants.getMPEaterForJob(player.getJob()));
        final int eaterLevel = player.getTotalSkillLevel(eaterSkill);
        if (attack.hits > 0 && attack.targets > 0) {
            player.getStat().checkEquipDurabilitys(player, -1, false, false);
            // Don't ever do this. it's too expensive.
            /*if (!player.getStat().checkEquipDurabilitys(player, -1000)) { //i guess this is how it works ?
             player.dropMessage(5, "An item has run out of durability but has no inventory room to go to.");
             return;
             } //lol*/
        }
        final MapleMap map = player.getMap();

        for (final AttackPair oned : attack.allDamage) {
            final MapleMonster monster = map.getMonsterByOid(oned.objectid);
            if (monster != null && monster.getLinkCID() <= 0) {
                totDamageToOneMonster = 0;
                monsterstats = monster.getStats();
                fixeddmg = monsterstats.getFixedDamage();
                Integer eachd;
                for (Pair<Integer, Boolean> eachde : oned.attack) {
                    eachd = eachde.left;
                    if (fixeddmg != -1) {
                        if (eachd > 0) {
                            eachd = monsterstats.getOnlyNoramlAttack() ? 0 : fixeddmg; // Magic is always not a normal attack
                        }
                    } else if (monsterstats.getOnlyNoramlAttack()) {
                        eachd = 0; // Magic is always not a normal attack
                    }
                    totDamageToOneMonster += eachd;
                    if (eachd == 0 && player.getPyramidSubway() != null) { //miss
                        player.getPyramidSubway().onMiss(player);
                    }
                }
                totDamage += totDamageToOneMonster;
                //player.dropMessage(6,"totDamage:"+totDamage);
                player.checkMonsterAggro(monster);

                if (GameConstants.getAttackDelay(attack.skill, theSkill) >= 100 && !GameConstants.isNoDelaySkill(attack.skill) && !monster.getStats().isBoss() && player.getTruePosition().distanceSq(monster.getTruePosition()) > GameConstants.getAttackRange(effect, player.getStat().defRange)) {
                    player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER, "[Distance: " + player.getTruePosition().distanceSq(monster.getTruePosition()) + ", Expected Distance: " + GameConstants.getAttackRange(effect, player.getStat().defRange) + " Job: " + player.getJob() + "]"); // , Double.toString(Math.sqrt(distance))
                }
                if (attack.skill == 2301002 && !monsterstats.getUndead()) {
                    player.getCheatTracker().registerOffense(CheatingOffense.HEAL_ATTACKING_UNDEAD);
                    return;
                }

                if (totDamageToOneMonster > 0) {
                    /*임시처리*/
                    Item weapon_ = player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
                    Equip equip = (Equip) weapon_;
                    int poten1 = equip.getPotential1();
                    int poten2 = equip.getPotential2();
                    int poten3 = equip.getPotential3();
                    final int itemLevel = ii.getReqLevel(equip.getItemId());
                    if (poten1 == 10241 || poten2 == 10241 || poten3 == 10241) {//빙결옵션
                        final Skill skill = SkillFactory.getSkill(1211006);
                        final MapleStatEffect eff = skill.getEffect(1);
                        if (Randomizer.nextInt(100) <= 5) { //5% 확률                            
                            final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.FREEZE, 1, skill.getId(), null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 71 ? 1000 : 2000, true, eff); //2레벨 빙결효과
                        }
                    }
                    if (poten1 == 10221 || poten2 == 10221 || poten3 == 10221) { //포이즌
                        final Skill skill = SkillFactory.getSkill(2101005);
                        final MapleStatEffect eff = skill.getEffect(1);
                        int randomdamage = Randomizer.nextInt(3);
                        if (Randomizer.nextInt(100) <= 10) { //10% 확률                            
                            final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.POISON, (3 * (equip.getMatk() == 0 ? equip.getWatk() : equip.getWatk() != 0 ? (int) (equip.getWatk() + equip.getMatk() / 1.4) : equip.getMatk()) * randomdamage), skill.getId(), null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 21 ? 1000 : itemLevel < 41 ? 2000 : itemLevel < 61 ? 3000 : itemLevel < 81 ? 4000 : itemLevel < 101 ? 5000 : 6000, true, eff); //2레벨 포이즌효과?
                        }
                    }
                    if (poten1 == 10226 || poten2 == 10226 || poten3 == 10226) {//스턴
                        final Skill skill = SkillFactory.getSkill(1121001);
                        final MapleStatEffect eff = skill.getEffect(1);
                        if (Randomizer.nextInt(100) <= 5) { //5% 확률                            
                            final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.STUN, 1, skill.getId(), null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 71 ? 1000 : 2000, true, eff); //스턴효과
                        }
                    }
                    if (poten1 == 10231 || poten2 == 10231 || poten3 == 10231) {//슬로우
                        final Skill skill = SkillFactory.getSkill(2101003);
                        final MapleStatEffect eff = skill.getEffect(1);
                        final int slowX = itemLevel < 71 ? 10 : 20;
                        if (Randomizer.nextInt(100) <= 10) { //10% 확률                            
                            final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.SPEED, -4 * slowX, skill.getId(), null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 71 ? 2000 : 4000, true, eff); //슬로우
                        }
                    }
                    if (poten1 == 10236 || poten2 == 10236 || poten3 == 10236) {//암흑
                        final Skill skill = SkillFactory.getSkill(3221006);
                        final MapleStatEffect eff = skill.getEffect(1);
                        final int blindX = itemLevel < 71 ? 10 : itemLevel < 101 ? 20 : 30;
                        if (Randomizer.nextInt(100) <= 10) { //10% 확률                            
                            final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.ACC, blindX, skill.getId(), null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 71 ? 2000 : itemLevel < 101 ? 4000 : 9000, true, eff); //암흑
                            //player.dropMessage(5, "applied!");
                        }
                    }
                    if (poten1 == 10246 || poten2 == 10246 || poten3 == 10246) {//봉인
                        final Skill skill = SkillFactory.getSkill(2111004);
                        final MapleStatEffect eff = skill.getEffect(1);
                        if (Randomizer.nextInt(100) <= 5) { //5% 확률                            
                            final MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.SEAL, 1, skill.getId(), null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, itemLevel < 71 ? 1000 : 2000, true, eff); //봉인
                        }
                    }
                    if (monster.isBuffed(MonsterStatus.MAGIC_DAMAGE_REFLECT)) { //test
                        int damage = 0;
                        damage = Randomizer.rand(stats.getCurrentMaxHp() / 50, stats.getCurrentMaxHp());
                        //player.dropMessage(6,"damage"+damage);
                        player.addHP(-damage);
                        player.getClient().getSession().write(MaplePacketCreator.playerDamage(player.getId(), damage));
                    }
                    monster.damage(player, totDamageToOneMonster + player.getExtraDamage(), true, attack.skill, System.currentTimeMillis());
                    if (player.getBuffedValue(MapleBuffStat.SLOW) != null) {
                        final MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.SLOW);

                        if (eff != null && eff.makeChanceResult() && !monster.isBuffed(MonsterStatus.SPEED)) {
                            monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.SPEED, eff.getX(), eff.getSourceId(), null, false), false, eff.getY() * 1000, true, eff);
                        }
                    }

                    if (player.getMapId() / 100000 == 9250) { // 무릉
                        if (!monster.getStats().isBoss()) {
                            int guageUp = (int) Math.max(1, Math.floor(Math.min(totDamageToOneMonster, monster.getStats().getHp()) / (double) monster.getStats().getHp() / (0.1D / 5.0D)));
                            player.mulung_EnergyModify(guageUp);
                        }
                    }

                    player.onAttack(monster.getMobMaxHp(), monster.getMobMaxMp(), attack.skill, monster.getObjectId(), totDamage);

                    if (effect != null && effect.getMonsterStati().size() > 0) {
                        if (effect.makeChanceResult()) {
                            for (Map.Entry<MonsterStatus, Integer> z : effect.getMonsterStati().entrySet()) {
                                final int statusValue = z.getKey() == MonsterStatus.POISON ? Math.max(1, totDamageToOneMonster) : z.getValue();
                                monster.applyStatus(player, new MonsterStatusEffect(z.getKey(), statusValue, theSkill.getId(), null, false), effect.isPoison(), effect.getDuration(), true, effect);
                            }
                        }
                    }
                    if (eaterLevel > 0) {
                        eaterSkill.getEffect(eaterLevel).applyPassive(player, monster);
                    }
                }
            }
        }
        if (attack.skill != 2301002) {
            effect.applyTo(player);
        }
        player.afterAttack(attack.targets, attack.hits, attack.skill);
        if (totDamage > 1 && GameConstants.getAttackDelay(attack.skill, theSkill) >= 100) {
            final CheatTracker tracker = player.getCheatTracker();
            tracker.setAttacksWithoutHit(true);

            if (tracker.getAttacksWithoutHit() > 1000) {
                tracker.registerOffense(CheatingOffense.ATTACK_WITHOUT_GETTING_HIT, Integer.toString(tracker.getAttacksWithoutHit()));
            }
        }
    }

    private static int get_weapon_type(int nItemID) {
        int result = nItemID / 10000 % 100; // eax@2

        if (nItemID / 1000000 != 1
                || (result < 30)
                || result > 33 && (result <= 36 || result > 38 && (result <= 39 || result > 47))) {
            result = 0;
        }
        return result;
    }

    private static final double CalculateMaxMagicDamagePerHit(final MapleCharacter chr, final Skill skill, final MapleMonster monster, final MapleMonsterStats mobstats, final PlayerStats stats, final Element elem, final Integer sharpEye, final double maxDamagePerMonster, final MapleStatEffect attackEffect) {
        if (skill.getId() % 10000 == 1000) {
            //달팽이 세마리
            if (chr.getSkillLevel(skill.getId()) == 1) {
                return 10;
            } else if (chr.getSkillLevel(skill.getId()) == 2) {
                return 25;
            } else if (chr.getSkillLevel(skill.getId()) == 3) {
                return 40;
            }
        }
        final ElementalEffectiveness ee = monster.getEffectiveness(elem);

        double elementalMod = 1.0;
        switch (ee) {
            case IMMUNE:
                elementalMod = 0.25;
                break;
            case STRONG:
                elementalMod = 0.5;
                break;
            case WEAK:
                elementalMod = 1.5;
                break;
        }
        float nAmp = 1.0F;
        if (chr.getJob() / 10 == 21) {
            if (chr.getSkillLevel(2110001) > 0) {
                MapleStatEffect effz = SkillFactory.getSkill(2110001).getEffect(chr.getSkillLevel(2110001));
                nAmp = effz.getY() / 100.0F;
            }
        }
        if (chr.getJob() / 10 == 22) {
            if (chr.getSkillLevel(2210001) > 0) {
                MapleStatEffect effz = SkillFactory.getSkill(2210001).getEffect(chr.getSkillLevel(2210001));
                nAmp = effz.getY() / 100.0F;
            }
        }
        int mad = stats.getTotalMagic();
        int int_ = stats.getTotalInt();
        int skillmad = attackEffect.getMatk();
        double mastery = ((attackEffect.getMastery() * 5 + 10) * 0.009000000000000001) * mad * 2;
        double v22 = mastery + (mad * 2 - mastery); //max
//        System.out.println("mad : "+mad+" int : "+int_+" skillmad : "+skillmad);
        double elemMaxDamagePerMob = ((v22 * 3.3 + mad * mad * 0.003365 + int_ * 0.5) * (skillmad * 0.01) * elementalMod * nAmp);
//        System.out.println("elemMaxDamagePerMob : "+elemMaxDamagePerMob);
//        int CritPercent = sharpEye;
        // Calculate monster magic def
        // Min damage = (MIN before defense) - MDEF*.6
        // Max damage = (MAX before defense) - MDEF*.5
        int MDRate = monster.getStats().getMDRate();
        MonsterStatusEffect pdr = monster.getBuff(MonsterStatus.MDEF);
        if (pdr != null) {
            MDRate += pdr.getX();
        }
        elemMaxDamagePerMob -= elemMaxDamagePerMob * (Math.max(MDRate/* - stats.ignoreTargetDEF*/ - attackEffect.getIgnoreMob(), 0) / 100.0);
        // Calculate Sharp eye bonus
//        elemMaxDamagePerMob += ((double) elemMaxDamagePerMob / 100.0) * CritPercent;

        if (skill.isChargeSkill()) {
            elemMaxDamagePerMob = ((90 * ((System.currentTimeMillis() - chr.getKeyDownSkill_Time())) + 10000) * elemMaxDamagePerMob * 0.00001);
        }
        if (skill.isChargeSkill() && chr.getKeyDownSkill_Time() == 0) {
            return 1;
        }
        elemMaxDamagePerMob *= (monster.getStats().isBoss() ? chr.getStat().bossdam_r : chr.getStat().dam_r) / 100.0;
        final MonsterStatusEffect imprint = monster.getBuff(MonsterStatus.IMPRINT);
        if (imprint != null) {
            elemMaxDamagePerMob += (elemMaxDamagePerMob * imprint.getX() / 100.0);
        }
        elemMaxDamagePerMob += (elemMaxDamagePerMob * chr.getDamageIncrease(monster.getObjectId()) / 100.0);
        if (elemMaxDamagePerMob > 999999) {
            elemMaxDamagePerMob = 999999;
        } else if (elemMaxDamagePerMob <= 0) {
            elemMaxDamagePerMob = 1;
        }

        return elemMaxDamagePerMob;
    }

    private static void handlePickPocket(final MapleCharacter player, final MapleMonster mob, AttackPair oned) {
        final int maxmeso = player.getBuffedValue(MapleBuffStat.PICKPOCKET).intValue();
        for (final Pair<Integer, Boolean> eachde : oned.attack) {
            final Integer eachd = eachde.left;
            if (player.getStat().pickRate >= 100 || Randomizer.nextInt(99) < player.getStat().pickRate) {
                player.getMap().spawnMesoDrop(Math.min((int) Math.max(((double) eachd / (double) 12300) * (double) maxmeso, (double) 1), maxmeso), new Point((int) (mob.getTruePosition().getX() + Randomizer.nextInt(100) - 50), (int) (mob.getTruePosition().getY())), mob, player, true, (byte) 0);
            }
        }
    }

    public static int getMastery(MapleCharacter player, Item weapon) {
        int mastery = 10;
        int weaponT = 0;
        if (weapon != null) {
            weaponT = weapon.getItemId() / 10000 % 100;
        }
        int job = player.getJob() / 10 % 100;
        switch (weaponT) {
            case 30:
                if (job == 11) {
                    mastery += addMasterySkill(player, 1100000);
                } else {
                    mastery += addMasterySkill(player, 1200000);
                }
                break;
            case 31:
                mastery += addMasterySkill(player, 1100001);
                break;
            case 32:
                mastery += addMasterySkill(player, 1200001);
                break;
            case 33:
                mastery += addMasterySkill(player, 4200000);
                break;
            case 37:
                break;
            case 38:
                break;
            case 40:
                if (job == 11) {
                    mastery += addMasterySkill(player, 1100000);
                } else {
                    mastery += addMasterySkill(player, 1200000);
                }
                break;
            case 41:
                mastery += addMasterySkill(player, 1100001);
                break;
            case 42:
                mastery += addMasterySkill(player, 1200001);
                break;
            case 43:
                mastery += addMasterySkill(player, 1300000);
                break;
            case 44:
                mastery += addMasterySkill(player, 1300001);
                break;
            case 45:
                mastery += addMasterySkill(player, 3100000);
                break;
            case 46:
                mastery += addMasterySkill(player, 3200000);
                break;
            case 47:
                mastery += addMasterySkill(player, 4100000);
                break;
        }
        mastery = Math.min(mastery, 100);
        return mastery;
    }

    private static double CalculateMaxWeaponDamagePerHit(final MapleCharacter player, final MapleMonster monster, final AttackInfo attack, final Skill theSkill, final MapleStatEffect attackEffect, final Integer CriticalDamagePercent, int stance) {

        Equip weapon = (Equip) player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        if (weapon == null) {
            return 0;
        }

        double maximumDamageToMonster = 0;
        int mastery = 10;
        int weaponT = weapon.getItemId() / 10000 % 100;
        MapleWeaponType weaponType = MapleWeaponType.NOT_A_WEAPON;
        switch (weaponT) {
            case 30:
                weaponType = MapleWeaponType.SWORD1H;
                mastery += addMasterySkill(player, 1100000);
                mastery += addMasterySkill(player, 1200000);
                break;
            case 31:
                weaponType = MapleWeaponType.AXE1H;
                mastery += addMasterySkill(player, 1100001);
                break;
            case 32:
                weaponType = MapleWeaponType.BLUNT1H;
                mastery += addMasterySkill(player, 1200001);
                break;
            case 33:
                weaponType = MapleWeaponType.DAGGER;
                mastery += addMasterySkill(player, 4200000);
                break;
            case 37:
                weaponType = MapleWeaponType.WAND;
                break;
            case 38:
                weaponType = MapleWeaponType.STAFF;
                break;
            case 40:
                weaponType = MapleWeaponType.SWORD2H;
                mastery += addMasterySkill(player, 1100000);
                mastery += addMasterySkill(player, 1200000);
                break;
            case 41:
                weaponType = MapleWeaponType.AXE2H;
                mastery += addMasterySkill(player, 1100001);
                break;
            case 42:
                weaponType = MapleWeaponType.BLUNT2H;
                mastery += addMasterySkill(player, 1200001);
                break;
            case 43:
                weaponType = MapleWeaponType.SPEAR;
                mastery += addMasterySkill(player, 1300000);
                break;
            case 44:
                weaponType = MapleWeaponType.POLE_ARM;
                mastery += addMasterySkill(player, 1300001);
                break;
            case 45:
                weaponType = MapleWeaponType.BOW;
                mastery += addMasterySkill(player, 3100000);
                break;
            case 46:
                weaponType = MapleWeaponType.CROSSBOW;
                mastery += addMasterySkill(player, 3200000);
                break;
            case 47:
                weaponType = MapleWeaponType.CLAW;
                mastery += addMasterySkill(player, 4100000);
                break;
            case 48:
                weaponType = MapleWeaponType.KNUCKLE;
                mastery += addMasterySkill(player, 5100001);
                mastery += addMasterySkill(player, 15100001);
                break;
            case 49:
                weaponType = MapleWeaponType.GUN;
                mastery += addMasterySkill(player, 5100001);
                mastery += addMasterySkill(player, 15100001);
                break;
        }
        mastery = Math.min(mastery, 100);

        switch (weaponT) {
            case 30:
            case 40:
                maximumDamageToMonster = ((player.getStat().getTotalStr() * weaponType.getMaxDamageMultiplier() + player.getStat().getTotalDex()) * player.getStat().getTotalWatk() / 100.0F);
                break;
            case 31:
            case 32:
                float motionWeapon = weaponType.getMaxDamageMultiplier();
                switch (stance) {
                    case -112:
                    case -111:
                    case 16:
                    case 17:
                        motionWeapon = 3.2F;
                }

                maximumDamageToMonster = ((player.getStat().getTotalStr() * motionWeapon + player.getStat().getTotalDex()) * player.getStat().getTotalWatk() / 100.0F);
                break;
            case 37:
            case 38:
                maximumDamageToMonster = ((player.getStat().getTotalStr() * weaponType.getMaxDamageMultiplier() + player.getStat().getTotalDex()) * player.getStat().getTotalWatk() / 100.0F);
                break;
            case 41:
            case 42:
                motionWeapon = weaponType.getMaxDamageMultiplier();
                switch (stance) {
                    case -112:
                    case -111:
                    case 16:
                    case 17:
                        motionWeapon = 3.4F;
                }
                maximumDamageToMonster = ((player.getStat().getTotalStr() * motionWeapon + player.getStat().getTotalDex()) * player.getStat().getTotalWatk() / 100.0F);

                break;
            case 43:
            case 44:
                motionWeapon = weaponType.getMaxDamageMultiplier();
                switch (stance) {
                    case -109:
                    case -108:
                    case 19:
                    case 20:
                        motionWeapon = 3.0F;
                }
                maximumDamageToMonster = ((player.getStat().getTotalStr() * motionWeapon + player.getStat().getTotalDex()) * player.getStat().getTotalWatk() / 100.0F);
                break;
            case 45:
            case 46:
            case 49:
                maximumDamageToMonster = ((player.getStat().getTotalDex() * weaponType.getMaxDamageMultiplier() + player.getStat().getTotalStr()) * player.getStat().getTotalWatk() / 100.0F);
                break;
            case 33:
            case 47:
            case 48:
                maximumDamageToMonster = ((player.getStat().getTotalLuk() * weaponType.getMaxDamageMultiplier() + player.getStat().getTotalDex() + player.getStat().getTotalStr()) * player.getStat().getTotalWatk() / 100.0F);
                break;
        }

        if (player.getStat().Berserk) {
            maximumDamageToMonster *= 2;
        }

        /*
         final int dLevel = Math.max(monster.getStats().getLevel() - player.getLevel(), 0) * 2;
         int HitRate = Math.min((int) Math.floor(Math.sqrt(player.getStat().getAccuracy())) - (int) Math.floor(Math.sqrt(monster.getStats().getEva())) + 100, 100);
         if (dLevel > HitRate) {
         HitRate = dLevel;
         }
         HitRate -= dLevel;
         if (HitRate <= 0) { // miss :P or HACK :O
         return 0;
         }
         * */
        List<Element> elements = new ArrayList<Element>();
        boolean defined = false;
        int CritPercent = CriticalDamagePercent;
        int PDRate = monster.getStats().getPDRate();
        MonsterStatusEffect pdr = monster.getBuff(MonsterStatus.WDEF);
        if (pdr != null) {
            PDRate += pdr.getX(); //x will be negative usually
        }
        if (theSkill != null) {
            elements.add(theSkill.getElement());
            switch (theSkill.getId()) {
                case 1311005:
                    PDRate = (monster.getStats().isBoss() ? PDRate : 0);
                    break;
                case 3221001:
                    maximumDamageToMonster *= attackEffect.getMobCount();
                    defined = true;
                    break;
                case 3101005:
                    defined = true; //can go past 99999
                    break;
                case 3221007: //snipe
                case 1221007: //BLAST FK
                    if (!monster.getStats().isBoss()) {
                        maximumDamageToMonster = (monster.getMobMaxHp());
                    }
                    defined = true;
                    break;
                case 1221011://Heavens Hammer
                case 21120006: //Combo Tempest
                    maximumDamageToMonster = (monster.getStats().isBoss() ? 500000 : (monster.getHp() - 1));
                    defined = true;
                    break;
                case 3121006: //스트레이프
                    if (monster.getStatusSourceID(MonsterStatus.FREEZE) == 3211003) { //blizzard in effect
                        defined = true;
                        maximumDamageToMonster = 99999;
                    }
                    break;
            }
        }
        if (attack.skill == 1211002) {
            maximumDamageToMonster *= 1.25; //test
        }
        double elementalMaxDamagePerMonster = maximumDamageToMonster;
        if (player.getJob() == 311 || player.getJob() == 312 || player.getJob() == 321 || player.getJob() == 322) {
            //FK mortal blow
            Skill mortal = SkillFactory.getSkill(player.getJob() == 311 || player.getJob() == 312 ? 3110001 : 3210001);
            if (player.getTotalSkillLevel(mortal) > 0) {
                final MapleStatEffect mort = mortal.getEffect(player.getTotalSkillLevel(mortal));
                if (mort != null && monster.getHPPercent() < mort.getX()) {
                    elementalMaxDamagePerMonster = 999999;;
                    defined = true;
                    if (mort.getZ() > 0) {
                        player.addHP((player.getStat().getMaxHp() * mort.getZ()) / 100);
                    }
                }
            }
        } else if (player.getJob() == 221 || player.getJob() == 222) {
            //FK storm magic
            Skill mortal = SkillFactory.getSkill(2210000);
            if (player.getTotalSkillLevel(mortal) > 0) {
                final MapleStatEffect mort = mortal.getEffect(player.getTotalSkillLevel(mortal));
                if (mort != null && monster.getHPPercent() < mort.getX()) {
                    elementalMaxDamagePerMonster = 999999;;
                    defined = true;
                }
            }
        }
        if (!defined || (theSkill != null && theSkill.getId() == 3221001)) {
            if (player.getBuffedValue(MapleBuffStat.WK_CHARGE) != null) {
                int chargeSkillId = player.getBuffSource(MapleBuffStat.WK_CHARGE);

                switch (chargeSkillId) {
                    case 1211003:
                    case 1211004:
                        elements.add(Element.FIRE);
                        break;
                    case 1211005:
                    case 1211006:
                        elements.add(Element.ICE);
                        break;
                    case 1211007:
                    case 1211008:
                        elements.add(Element.LIGHTING);
                        break;
                    case 1221003:
                    case 1221004:
                    case 11111007:
                        elements.add(Element.HOLY);
                        break;
                    case 12101005:
                        //elements.clear(); //neutral
                        break;
                }
            }
            if (elements.size() > 0) {
                double elementalEffect;

                switch (attack.skill) {
                    case 3211003:
                    case 3111003: // inferno and blizzard
                        elementalEffect = attackEffect.getX() / 100.0;
                        break;
                    default:
                        elementalEffect = (0.5 / elements.size());
                        break;
                }
                for (Element element : elements) {
                    switch (monster.getEffectiveness(element)) {
                        case IMMUNE:
                            elementalMaxDamagePerMonster = 1;
                            break;
                        case WEAK:
                            elementalMaxDamagePerMonster *= (1.0 + elementalEffect);
                            break;
                        case STRONG:
                            elementalMaxDamagePerMonster *= (1.0 - elementalEffect);
                            break;
                    }
                }
            }
            if (!defined) {
                // Calculate mob def
                elementalMaxDamagePerMonster -= elementalMaxDamagePerMonster * (Math.max(PDRate /*- Math.max(player.getStat().ignoreTargetDEF, 0)*/ - Math.max(attackEffect == null ? 0 : attackEffect.getIgnoreMob(), 0), 0) / 100.0);

                // Calculate passive bonuses + Sharp Eye
                elementalMaxDamagePerMonster += ((double) elementalMaxDamagePerMonster / 100.0) * CritPercent;
                if (attack.skill != 0 && theSkill != null) {
                    if (theSkill.isChargeSkill()) {
                        elementalMaxDamagePerMonster = (double) (90 * (System.currentTimeMillis() - player.getKeyDownSkill_Time()) / 2000 + 10) * elementalMaxDamagePerMonster * 0.01;
                    }
                    if (theSkill != null && theSkill.isChargeSkill() && player.getKeyDownSkill_Time() == 0) {
                        return 0;
                    }
                }
            }

            // handle combo calc
            int numFinisherOrbs = 0;
            final MapleStatEffect comboBuff = player.getStatForBuff(MapleBuffStat.COMBO);
            final Integer comboBuff2 = player.getBuffedValue(MapleBuffStat.COMBO);
            if (comboBuff != null && comboBuff2 != null) {
                numFinisherOrbs = comboBuff2.intValue() - 1;
                elementalMaxDamagePerMonster *= Math.max(numFinisherOrbs * (comboBuff.getDamage() / 3.0D), 1);
            }

            if (attackEffect != null) {
                if (attackEffect.getDamage() > 0) {
                    elementalMaxDamagePerMonster *= (attackEffect.getDamage() / 100.0D); //Damage Calc
                }
            }

//            final MonsterStatusEffect imprint = monster.getBuff(MonsterStatus.IMPRINT);
//            if (imprint != null) {
//                elementalMaxDamagePerMonster += (elementalMaxDamagePerMonster * imprint.getX() / 100.0);
//            }
//            elementalMaxDamagePerMonster += (elementalMaxDamagePerMonster * player.getDamageIncrease(monster.getObjectId()) / 100.0);
        }
        if (elementalMaxDamagePerMonster > 99999) {
            if (!defined) {
                elementalMaxDamagePerMonster = 99999;
            }
        } else if (elementalMaxDamagePerMonster <= 0) {
            elementalMaxDamagePerMonster = 1;
        }
        return elementalMaxDamagePerMonster;
    }

    public static final AttackInfo DivideAttack(final AttackInfo attack, final int rate) {
        attack.real = false;
        if (rate <= 1) {
            return attack; //lol
        }
        for (AttackPair p : attack.allDamage) {
            if (p.attack != null) {
                for (Pair<Integer, Boolean> eachd : p.attack) {
                    eachd.left /= rate; //too ex.
                }
            }
        }
        return attack;
    }

    public static AttackInfo Modify_AttackCrit(final AttackInfo attack, final MapleCharacter chr, final int type, final MapleStatEffect effect) {
        if (attack.skill != 4211006 && attack.skill != 3211003 && attack.skill != 4111004) { //blizz + shadow meso + m.e no crits
            final int CriticalRate = chr.getStat().getSharpEyeRate();
            final boolean shadow = chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null && (type == 1 || type == 2);
            final List<Integer> damages = new ArrayList<Integer>(), damage = new ArrayList<Integer>();
            int hit, toCrit, mid_att;
            for (AttackPair p : attack.allDamage) {
                if (p.attack != null) {
                    hit = 0;
                    mid_att = shadow ? (p.attack.size() / 2) : p.attack.size();
                    //grab the highest hits
                    toCrit = attack.skill == 4221001 || attack.skill == 3221007 || attack.skill == 3221001 ? mid_att : 0;
                    if (toCrit == 0) {
                        for (Pair<Integer, Boolean> eachd : p.attack) {
                            if (!eachd.right && hit < mid_att) {
                                if (Randomizer.nextInt(100) < CriticalRate) {
                                    toCrit++;
                                }
                                damage.add(eachd.left);
                            }
                            hit++;
                        }
                        if (toCrit == 0) {
                            damage.clear();
                            continue; //no crits here
                        }
                        Collections.sort(damage); //least to greatest
                        for (int i = damage.size(); i > damage.size() - toCrit; i--) {
                            damages.add(damage.get(i - 1));
                        }
                        damage.clear();
                    }
                    hit = 0;
                    for (Pair<Integer, Boolean> eachd : p.attack) {
                        if (!eachd.right) {
                            if (attack.skill == 4221001) { //assassinate never crit first 3, always crit last
                                eachd.right = hit == 3;
                            } else if (attack.skill == 3221007 || attack.skill == 23121003 || attack.skill == 21120005 || attack.skill == 4341005 || attack.skill == 4331006 || eachd.left > 999999) { //snipe always crit
                                eachd.right = true;
                            } else if (hit >= mid_att) { //shadowpartner copies second half to first half
                                eachd.right = p.attack.get(hit - mid_att).right;
                            } else {
                                //rough calculation
                                eachd.right = damages.contains(eachd.left);
                            }
                        }
                        hit++;
                    }
                    damages.clear();
                }
            }
        }
        return attack;
    }

    public static final AttackInfo parseAttack(final LittleEndianAccessor lea, final MapleCharacter chr, RecvPacketOpcode recv) {
        final AttackInfo ret = new AttackInfo();
        lea.skip(1);
        ret.tbyte = lea.readByte();
        ret.targets = (byte) ((ret.tbyte >>> 4) & 0xF);
        ret.hits = (byte) (ret.tbyte & 0xF);
        ret.skill = lea.readInt();
        //ret.skilllevel = lea.readByte();
        //chr.dropMessage(6, "ret.skilllevel" + ret.skilllevel);
        lea.skip(1);//스킬레벨
        if (recv == RecvPacketOpcode.RANGED_ATTACK) {
            lea.skip(1);
        }
        lea.skip(4);//crc
        switch (ret.skill) {
            case 2121001:
            case 2221001:
            case 2321001:
            case 3221001:
            case 3121004:
            case 4341002: // 파이널 컷
            case 4341003: // 듀블 몬스터 봄
            case 5101004:
            case 5221004:
            case 5201002:
            case 13111002:
            case 14111006: // 포이즌 봄
            case 15101003: // 샤크 웨이브
            case 22121000: // 에반 아이스 브레스
            case 22151001: // 에반 브레스
            case 33121009: // 와일드 발칸
            case 35001001: // 플레임 런처
            case 35101009:
                ret.charge = lea.readInt();
                break;
            default:
                ret.charge = 0;
                break;
        }
        if ((ret.skill == 35001001 || ret.skill == 35101009) && ret.charge == 0) {
            long kd = chr.getKeyDownSkill_Time();
            if (kd > 0) {
                int elapsed = (int) (System.currentTimeMillis() - kd);
                if (elapsed < 0) {
                    elapsed = 0;
                }
                if (elapsed > 8000) {
                    elapsed = 8000; // 스킬 최대 8초라면 캡
                }
                ret.charge = elapsed;
            }
        }
        ret.unk = lea.readByte();
        ret.display = lea.readUShort();
        ret.bigbang = lea.readInt(); //big bang 모르겠다 음수로 ㅈㄴ큰값나옴
        ret.donno1 = lea.readByte();//무기종류??
        ret.speed = lea.readByte(); // sure 100%
        ret.lastAttackTickCount = lea.readInt(); // Ticks
        lea.skip(4); //항상 0 4개
        //ret.bigbang = lea.readInt(); //big bang
        //chr.dropMessage(6, /*"bigbang: " + ret.bigbang + " animation: " + ret.animation + */ "WeaponClass: " + ret.donno1 + " unk: " + ret.unk + " display: " + ret.display + " speed: " + ret.speed + " TickCount: " + ret.lastAttackTickCount);
        int oid;
        List<Pair<Integer, Boolean>> allDamageNumbers;

        ret.allDamage = new ArrayList<AttackPair>();
        if ((ret.speed < 0 || ret.speed > 10) && recv == RecvPacketOpcode.CLOSE_RANGE_ATTACK) {
            AutobanManager.getInstance().autoban(chr.getClient(), "팅패킷 사용");
            System.out.println("팅패킷 사용 : " + chr.getName());
            return null;
        }
        if (recv == RecvPacketOpcode.CLOSE_RANGE_ATTACK && ret.skill == 4211006) { // Meso Explosion
            return parseMesoExplosion(lea, ret, chr);
        }
        if (recv == RecvPacketOpcode.RANGED_ATTACK) {
            ret.slot = (byte) lea.readShort();
            ret.csstar = (byte) lea.readShort();
            ret.AOE = lea.readByte(); // is AOE or not, TT/ Avenger = 41, Showdown = 0
//            if (ret.skill != 4121003 && ret.skill != 14101006 && chr.getBuffedValue(MapleBuffStat.SPIRIT_CLAW) != null) {
//                lea.skip(4);
//            }
        }
        for (int i = 0; i < ret.targets; i++) {
            oid = lea.readInt();
            lea.skip(14);
            allDamageNumbers = new ArrayList<Pair<Integer, Boolean>>();
            for (int j = 0; j < ret.hits; j++) {
                int damageWithCrit = lea.readInt();
                int damage = damageWithCrit & 0x7FFFFFFF;
                boolean crit = (damageWithCrit >> 31 & 0x1) != 0;
                allDamageNumbers.add(new Pair<Integer, Boolean>(Integer.valueOf(damage), crit));
                //chr.dropMessage(6, "damage: " + damage);
            }
            lea.skip(4); // CRC of monster [Wz Editing]
            ret.allDamage.add(new AttackPair(Integer.valueOf(oid), allDamageNumbers));
        }
        ret.position = lea.readPos();
        if (lea.available() >= 4) {
            ret.position = lea.readPos();
            if (ret.skill == 14111006) {
                final Skill poisonBomb = SkillFactory.getSkill(14111006);
                final MapleStatEffect poison = poisonBomb.getEffect(chr.getTotalSkillLevel(poisonBomb));

                Point mistPos = ret.position;

                if (lea.available() >= 4) {
                    mistPos = lea.readPos();
                }

                final Rectangle bounds = poison.calculateBoundingBox(mistPos, chr.isFacingLeft());
                final MapleMist mist = new MapleMist(bounds, chr, poison);
                chr.getMap().spawnMist(mist, poison.getDuration(), false);
            }
        }
        chr.LastSkill = ret.skill;
        return ret;
    }

    public static final AttackInfo parseMesoExplosion(final LittleEndianAccessor lea, final AttackInfo ret, final MapleCharacter chr) {
//        System.out.println(lea.toString(true));
        byte bullets;
        if (ret.hits == 0) {
            lea.skip(4);
            bullets = lea.readByte();
            for (int j = 0; j < bullets; j++) {
                ret.allDamage.add(new AttackPair(Integer.valueOf(lea.readInt()), null));
                lea.skip(1);
            }
            lea.skip(2); // 8F 02
            return ret;
        }
        int oid;
        List<Pair<Integer, Boolean>> allDamageNumbers;

        for (int i = 0; i < ret.targets; i++) {
            oid = lea.readInt();
            //if (chr.getMap().isTown()) {
            //    final MapleMonster od = chr.getMap().getMonsterByOid(oid);
            //    if (od != null && od.getLinkCID() > 0) {
            //	    return null;
            //    }
            //}
            lea.skip(12);
            bullets = lea.readByte();
            allDamageNumbers = new ArrayList<Pair<Integer, Boolean>>();
            for (int j = 0; j < bullets; j++) {
                allDamageNumbers.add(new Pair<Integer, Boolean>(Integer.valueOf(lea.readInt()), false)); //m.e. never crits
            }
            ret.allDamage.add(new AttackPair(Integer.valueOf(oid), allDamageNumbers));
            lea.skip(4); // C3 8F 41 94, 51 04 5B 01
        }
        lea.skip(4);
        bullets = lea.readByte();

        for (int j = 0; j < bullets; j++) {
            ret.allDamage.add(new AttackPair(Integer.valueOf(lea.readInt()), null));
            lea.skip(1);
        }
        // 8F 02/ 63 02

        return ret;
    }

    private static int addMasterySkill(final MapleCharacter player, int sid) {
        int skillLevel = player.getSkillLevel(sid);
        int result = skillLevel / 2;
        if (skillLevel % 2 == 1) {
            result = result + 1;
        }
        masteryByte = (byte) result;
        if (result > 0) {
            return get_mastery_from_skill(sid, result) * 5;
        }
        return 0;
    }

    public static byte getMasteryByte() {
        return masteryByte;
    }

    private static int get_mastery_from_skill(int skill, int skilllevel) {
        Skill skillz = SkillFactory.getSkill(skill);
        if (skillz != null) {
            MapleStatEffect stat = skillz.getEffect(skilllevel);
            if (stat != null) {
                if (stat.getX() > 0) {
                    return stat.getX();
                } else if (stat.getMastery() > 0) {
                    return stat.getMastery();
                }
            }
        }
        return 0;
    }

    public static void critModify(/*MapleMonster monster, */final MapleCharacter player, /*int v22, */ final AttackInfo attack) {
        //  if (true) return;
        long randoms[] = new long[7];
        int nIdx = 0;
        for (int z = 0; z < randoms.length; ++z) {
            randoms[z] = player.getCRand1().Random();
        }
//        MapleMonsterStats monsterstats;
//        monsterstats = monster.getStats();
//        int v157 = Math.max(0, monsterstats.getLevel() - player.getLevel());
//        //double MobPDDValue = v22 * 100.0 / (v157 * 10.0 + 255.0);
//        int v33 = monsterstats.getEva();
//        if (monster.getBuff(MonsterStatus.AVOID) != null) {
//            v33 += monster.getBuff(MonsterStatus.AVOID).getX();
//        }
//        v33 = Math.max(0, v33);
//        v33 = Math.min(999, v33);
        //int nMobPDD = v33;
        //                int pad = 0;
        Item ipp = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        //                if (ipp != null) {
        //                    pad += MapleItemInformationProvider.getInstance().getWatkForProjectile(ipp.getItemId());
        //                }
        //                pad += player.getStat().getTotalWatk();
        //                pad = Math.min(1999, pad);
        //                if (player.getBuffedValue(MapleBuffStat.ECHO_OF_HERO) != null) {
        //                    int za = player.getBuffedValue(MapleBuffStat.ECHO_OF_HERO);
        //                    pad += pad * za / 100;
        //                    pad = Math.min(1999, pad);
        //                }
        int nCSLV = getMastery(player, ipp);
        int nCriticalAttackProb = 0;
        int nCriticalAttackParam = 0;
        double M = nCSLV * 0.009000000000000001;
        if (attack.display == 58) {
            nCSLV = player.getSkillLevel(attack.skill);
            MapleStatEffect stateffect = SkillFactory.getSkill(attack.skill).getEffect(nCSLV);
            nCriticalAttackProb = stateffect.getProb();
            nCriticalAttackParam = stateffect.getCr();
        } else {
            if (ipp != null) {
                if (GameConstants.getWeaponType(ipp.getItemId()) == MapleWeaponType.BOW
                        || GameConstants.getWeaponType(ipp.getItemId()) == MapleWeaponType.CROSSBOW) {
                    if (player.getSkillLevel(3000001) > 0) {
                        nCSLV = player.getSkillLevel(3000001);
                        MapleStatEffect stateffect = SkillFactory.getSkill(3000001).getEffect(nCSLV);
                        nCriticalAttackProb = stateffect.getProb();
                        nCriticalAttackParam = stateffect.getDamage();
                    }
                } else if (GameConstants.getWeaponType(ipp.getItemId()) == MapleWeaponType.CLAW) {
                    if (player.getSkillLevel(4100001) > 0) {
                        nCSLV = player.getSkillLevel(4100001);
                        MapleStatEffect stateffect = SkillFactory.getSkill(4100001).getEffect(nCSLV);
                        nCriticalAttackProb = stateffect.getProb();
                        nCriticalAttackParam = stateffect.getDamage();
                    }
                } else if (GameConstants.getWeaponType(ipp.getItemId()) == MapleWeaponType.KNUCKLE) {
                    if (player.getSkillLevel(15110000) > 0) {
                        nCSLV = player.getSkillLevel(15110000);
                        MapleStatEffect stateffect = SkillFactory.getSkill(15110000).getEffect(nCSLV);
                        nCriticalAttackProb = stateffect.getProb();
                        nCriticalAttackParam = stateffect.getDamage();
                    }
                }
            }
        }
        Integer value = player.getBuffedValue(MapleBuffStat.SHARP_EYES);
        if (value != null) {
            int v27 = value >> 8;
            v27 = Math.max(0, v27);
            v27 = Math.min(v27, 100);
            nCriticalAttackProb += v27;
            if (value > 0) {
                if (nCriticalAttackParam > 0) {
                    nCriticalAttackParam += (value & 0xFF);
                } else {
                    nCriticalAttackParam = (value & 0xFF) + 100;
                }
            }
        }
        if (attack.skill == 3221007 || attack.skill == 4121008) {
            nIdx++;
        }
        nIdx++;
//                double v47d = MobPDDValue * 1.3;
//                if (nMobPDD > v47d) {
//                    continue;
//                }
        nIdx++;
        nIdx++;
        nIdx++;
        boolean hasDark = false;
        if (player.getAllDiseases().size() > 0) {
            for (MapleDiseaseValueHolder mdvh : player.getAllDiseases()) {
                if (mdvh.disease == MapleDisease.DARKNESS) {
                    hasDark = true;
                    break;
                }
            }
        }
//                if (hasDark) {
//                    double b = 0.0;
//                    double damage = 100.0;
//                    double buff = 0.0;
//                    buff = b;
//                    b = damage;
//                    damage = buff;
//                    int v52 = nIdx % 7;
//                    ++nIdx;
//                    long v53 = randoms[v52];
//                    boolean d = (b - damage) * (v53 % 0x989680) * 0.000000100000010000001 + damage <= 20.0;
//                    if (d) {
//                        int nSkillID = attack.skill;
//                        if (nSkillID == 1121006 || nSkillID == 1221007 || nSkillID == 1321003 || nSkillID == 1221009) {
//                            b = 0.0;
//                            damage = 5.0;
//                            buff = b;
//                            b = damage;
//                            damage = buff;
//                            v52 = nIdx % 7;
//                            ++nIdx;
//                            v53 = randoms[v52];
//                            d = (b - damage) * (double) (v53 % 0x989680) * 0.000000100000010000001 + damage < 3.0;
//
//                        }
//                        if (ipp.getItemId() / 10000 == 145 || ipp.getItemId() / 10000 == 146) {
//                            int nAction = attack.display;
//                            if ((nAction < 22 || nAction > 27) && nAction != 54) {
//                                if (nSkillID != 3201003 && nSkillID != 3101003) {
//                                    calc damage but skipp..
//                                    nIdx++;
//                                    if (nSkillID != 1311005 && nSkillID != 4111004 && nSkillID != 4211002) {
//                                        ++nIdx;
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
        //                System.out.println("nCriticalAttackParam : " + nCriticalAttackParam + " / nCriticalAttackProb : " + nCriticalAttackProb + " / nCSLV : " + nCSLV);
        for (AttackPair oned : attack.allDamage) {
            if ((attack.skill != 3211003 && attack.skill != 4111004 && attack.skill != 4221001 && attack.skill != 4211006)) {
                if (player.getStat().getSharpEyeRate() > 0) {
                    for (Pair<Integer, Boolean> app : oned.attack) {
                        int v47 = nIdx % 7;
                        nIdx++;
                        boolean isCrit = (randoms[v47] % 0x989680) * 0.0000100000010000001 < player.getStat().getSharpEyeRate();
                        if (attack.skill == 35111004
                                || attack.skill == 35121013) {
                            isCrit = true;
                        }
                        app.right = isCrit;
                    }

                }
            }
        }
    }
}
