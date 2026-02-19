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
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.Timer;
import server.life.MapleMonster;
import server.maps.*;
import server.movement.LifeMovementFragment;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.MobPacket;
import tools.packet.TemporaryStatsPacket;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import server.Randomizer;

public class SummonHandler {

    private static boolean isMechanicRobotSummon(final int skillId) {
        switch (skillId) {
            case 35111002: // 마그네틱 필드
            case 35111011: // 힐링로봇
            case 35111005: // 액셀레이터
            case 35121009: // 로보 팩토리
            case 35121010: // 앰플리파이어
                return true;
        }
        return false;
    }

    public static final void MoveDragon(final LittleEndianAccessor slea, final MapleCharacter chr) {
        slea.skip(8); //POS
        final List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 5);
        if (chr != null && chr.getDragon() != null && res.size() > 0) {
            final Point pos = chr.getDragon().getTruePosition();
            MovementParse.updatePosition(res, chr.getDragon(), 0);
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.moveDragon(chr.getDragon(), pos, res), chr.getTruePosition());
        }
    }

    public static final void MoveSummon(final LittleEndianAccessor slea, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MapleMapObject obj = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.SUMMON);
        if (obj == null) {
            return;
        }
        final MapleSummon sum = (MapleSummon) obj;
        if (sum == null) {
            return;
        }
        /*if (obj instanceof MapleDragon) {
         MoveDragon(slea, chr);
         return;
         }*/ //용은 나중에
        slea.skip(8); //startPOS
        //  Point startPos = new Point(slea.readShort(), slea.readShort());
        final List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 4);
        final Point pos = sum.getTruePosition();
        MovementParse.updatePosition(res, sum, 0);
        if (res.size() > 0) {
            //  chr.dropMessage(6, "chr 아이디" + chr.getId() + "오브아이디" + sum.getObjectId() + "시작좌표"+ startPos + "res" + res );
            chr.getMap().broadcastMessage(chr, MaplePacketCreator.moveSummon(chr.getId(), sum.getObjectId(), sum.getObjectId(), pos, res, slea), sum.getTruePosition());
        }
    }

    public static final void DamageSummon(final LittleEndianAccessor slea, final MapleCharacter chr) {

        final int monsterIdFrom = slea.readInt(); //oid 왜인지 모르겠지만 oid가 1개 작게 나옴
        final int unkByte = slea.readByte();
        final int damage = slea.readInt();
        final int monsterID = slea.readInt();//몹 코드
        final byte stance = slea.readByte(); // stance
        MapleSummon sum = null;
        try {
            for (MapleSummon ss : chr.getSummonsReadLock()) {
                if (ss != null
                        && ss.getObjectId() == ss.getSkill()
                        && ss.getSkillLevel() > 0
                        && ss.getMovementType() != SummonMovementType.STATIONARY)
                    /* chr.dropMessage(6,"1111111111111111111")*/ ;
                {
                    sum = ss;
                    break;
                }
            }
        } finally {
            chr.unlockSummonsReadLock();
        }
        final int skillid = sum.getSkill();
        final Iterator<MapleSummon> iter = chr.getSummonsReadLock().iterator();
        MapleSummon summon;
        boolean remove = false;
        try {
            while (iter.hasNext()) {
                summon = iter.next();
                if (summon.isPuppet() && summon.getOwnerId() == chr.getId() && damage > 0) { //We can only have one puppet(AFAIK O.O) so this check is safe.
                    summon.addHP((short) -damage);
                    if (summon.getHP() <= 0 && (skillid != 35111001 || skillid != 35111009 || skillid != 35111010)) {
                        remove = true;
                    }
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.damageSummon(chr.getId(), skillid, damage, unkByte, monsterID), summon.getTruePosition());
                    break;
                }
            }
        } finally {
            chr.unlockSummonsReadLock();
        }
        if (remove) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
        }
    }

    public static void SummonAttack(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMap() == null) {
            return;
        }
        final MapleMap map = chr.getMap();
        final MapleMapObject obj = map.getMapObject(slea.readInt(), MapleMapObjectType.SUMMON);
        if (obj == null || !(obj instanceof MapleSummon)) {
            //chr.dropMessage(5, "The summon has disappeared.");
            return;
        }
        final MapleSummon summon = (MapleSummon) obj;
        if (summon.getOwnerId() != chr.getId() || summon.getSkillLevel() <= 0) {
            chr.dropMessage(5, "Error.");
            return;
        }
        final SummonSkillEntry sse = SkillFactory.getSummonData(summon.getSkill());
        if (summon.getSkill() / 1000000 != 35 && summon.getSkill() != 33101008 && sse == null) {
            chr.dropMessage(5, "Error in processing attack.");
            return;
        }
        int tick = slea.readInt();
        final byte animation = slea.readByte();
        final byte numAttacked = slea.readByte();
        slea.skip(summon.getSkill() == 35111002 ? 24 : 12); //some pos stuff
        final List<Pair<Integer, Integer>> allDamage = new ArrayList<Pair<Integer, Integer>>();
        for (int i = 0; i < numAttacked; i++) {
            final MapleMonster mob = map.getMonsterByOid(slea.readInt());

            if (mob == null) {
                continue;
            }
            slea.skip(18); // who knows
            final int damge = slea.readInt();
            allDamage.add(new Pair<Integer, Integer>(mob.getObjectId(), damge));
        }
        //if (!summon.isChangedMap()) {
        map.broadcastMessage(chr, MaplePacketCreator.summonAttack(summon.getOwnerId(), summon.getObjectId(), animation, allDamage, chr.getLevel(), false), summon.getTruePosition());
        //}
        final Skill summonSkill = SkillFactory.getSkill(summon.getSkill());
        final MapleStatEffect summonEffect = summonSkill.getEffect(summon.getSkillLevel());
        if (summonEffect == null) {
            chr.dropMessage(5, "Error in attack.");
            return;
        }
        if (summon.getSkill() == 35121003) {
            chr.cancelEffect(summonEffect, -1);
        }
        for (Pair<Integer, Integer> attackEntry : allDamage) {
            final int toDamage = attackEntry.right;
            final MapleMonster mob = map.getMonsterByOid(attackEntry.left);
            if (mob == null) {
                continue;
            }
            if (sse != null && sse.delay > 0 && summon.getMovementType() != SummonMovementType.STATIONARY && summon.getMovementType() != SummonMovementType.CIRCLE_STATIONARY && summon.getMovementType() != SummonMovementType.WALK_STATIONARY && chr.getTruePosition().distanceSq(mob.getTruePosition()) > 400000.0) {
                chr.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER_SUMMON);
            }
            if (toDamage > 0 && summonEffect.getMonsterStati().size() > 0) {
                if (summonEffect.makeChanceResult()) {
                    for (Map.Entry<MonsterStatus, Integer> z : summonEffect.getMonsterStati().entrySet()) {
                        mob.applyStatus(chr, new MonsterStatusEffect(z.getKey(), z.getValue(), summonSkill.getId(), null, false), summonEffect.isPoison(), 4000, true, summonEffect);
                    }
                }
            }
            if (chr.isGM() || toDamage < 9999999) {//(chr.getStat().getCurrentMaxBaseDamage() * 5.0 * (summonEffect.getSelfDestruction() + summonEffect.getDamage()) / 100.0)) { //10 x dmg.. eh
                mob.damage(chr, toDamage, true);
                chr.checkMonsterAggro(mob);
                if (!mob.isAlive()) {
                    chr.getClient().getSession().write(MobPacket.killMonster(mob.getObjectId(), 1));
                }
            } else {
                chr.dropMessage(5, "Warning - high damage.");
                //AutobanManager.getInstance().autoban(c, "High Summon Damage (" + toDamage + " to " + attackEntry.right + ")");
                // TODO : Check player's stat for damage checking.
                break;
            }
        }
        if (!summon.isMultiAttack()) {
            chr.getMap().broadcastMessage(MaplePacketCreator.removeSummon(summon, true));
            chr.getMap().removeMapObject(summon);
            chr.removeVisibleMapObject(summon);
            chr.removeSummon(summon);
            if (summon.getSkill() != 35121011) {
                switch (summon.getSkill()) {
                    case 35111002:
                        chr.cancelEffectFromBuffStat(MapleBuffStat.SUMMON2);
                        break;
                    case 35111005:
                        chr.cancelEffectFromBuffStat(MapleBuffStat.REAPER);
                        break;
                    case 35121009:
                        chr.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
                        break;
                    default:
                        chr.cancelEffectFromBuffStat(MapleBuffStat.SUMMON);
                        break;
                }
            }
        }
    }

    public static final void RemoveSummon(final LittleEndianAccessor slea, final MapleClient c) {
        final MapleMapObject obj = c.getPlayer().getMap().getMapObject(slea.readInt(), MapleMapObjectType.SUMMON);
        if (obj == null || !(obj instanceof MapleSummon)) {
            return;
        }
        final MapleSummon summon = (MapleSummon) obj;
        if (summon.getOwnerId() != c.getPlayer().getId() || summon.getSkillLevel() <= 0) {
            c.getPlayer().dropMessage(5, "Error.");
            return;
        }
        if (isMechanicRobotSummon(summon.getSkill())) {
            final long expire = summon.getServerExpireTime();
            if (expire > 0 && System.currentTimeMillis() < expire) {
                c.getSession().write(MaplePacketCreator.spawnSummon(summon, true));
                c.getSession().write(MaplePacketCreator.enableActions());
                return;
            }
        }
        if (summon.getSkill() == 35121010) { // Amplifier AF-11
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeSummon(summon, true));
            c.getPlayer().getMap().removeMapObject(summon);
            c.getPlayer().removeVisibleMapObject(summon);
            c.getPlayer().removeSummon(summon);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.PIRATES_REVENGE);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SUMMON);
            return;
        }
        if (summon.getSkill() == 35121003) {
            final Skill summonSkill = SkillFactory.getSkill(summon.getSkill());
            final MapleStatEffect summonEffect = summonSkill.getEffect(summon.getSkillLevel());
            c.getPlayer().cancelEffect(summonEffect, -1);
        }
        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeSummon(summon, true));
        c.getPlayer().getMap().removeMapObject(summon);
        c.getPlayer().removeVisibleMapObject(summon);
        c.getPlayer().removeSummon(summon);

        switch (summon.getSkill()) {
            case 35111001:
            case 35111009:
            case 35111010:
                c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.donno4);
                break;
        }
        if (summon.getSkill() != 35121011 && summon.getSkill() != 35121003) {
            switch (summon.getSkill()) {
                case 35111002:
                    c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SUMMON2);
                    break;
                case 35111005:
                    c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.REAPER);
                    break;
                case 35121009:
                    c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
                    break;
                default:
                    c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SUMMON);
                    break;
            }
        }
    }

    public static final void SubSummon(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMapObject obj = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.SUMMON);
        if (obj == null || !(obj instanceof MapleSummon)) {
            return;
        }
        final MapleSummon sum = (MapleSummon) obj;
        if (sum == null || sum.getOwnerId() != chr.getId() || sum.getSkillLevel() <= 0 || !chr.isAlive()) {
            return;
        }
        switch (sum.getSkill()) {
            case 35121009:
                if (!chr.canSummon(2000)) {
                    return;
                }
                final int skillId = slea.readInt(); // 35121009?
                if (sum.getSkill() != skillId) {
                    return;
                }
                slea.skip(1); // 0E?
                chr.updateTick(slea.readInt());
                for (int i = 0; i < 3; i++) {
                    final MapleSummon tosummon = new MapleSummon(chr, SkillFactory.getSkill(35121011).getEffect(sum.getSkillLevel()), new Point(sum.getTruePosition().x, sum.getTruePosition().y - 5), SummonMovementType.WALK_STATIONARY);
                    chr.getMap().spawnSummon(tosummon);
                    chr.addSummon(tosummon);
                }
                break;
            case 35111005:
                for (MapleSummon s : chr.getMap().getAllSummonsThreadsafe()) {
                    if (s.getSkill() == 35111005) {
                        final MapleStatEffect effect = SkillFactory.getSkill(s.getSkill()).getEffect(s.getSkillLevel());
                        for (Map.Entry<MonsterStatus, Integer> stat : effect.getMonsterStati().entrySet()) {
                            for (MapleMonster mob : chr.getMap().getAllMonstersThreadsafe()) {
                                mob.applyStatus(s.getOwner(),
                                        new MonsterStatusEffect(stat.getKey(), stat.getValue(), s.getSkill(), null, false), false, effect.getDuration(), false, effect);
                            }
                        }
                    }
                }
                break;
            case 35111011: { // healing robot
                if (!chr.canSummon(1000)) {
                    return;
                }

                final MapleStatEffect eff = SkillFactory.getSkill(sum.getSkill()).getEffect(sum.getSkillLevel());

                // 로봇 주변 힐 범위
                final double range = 250.0;

                for (final MapleCharacter target : chr.getMap().getCharactersThreadsafe()) {
                    if (target == null || !target.isAlive()) {
                        System.out.println("[35111011] target=" + target.getName()
    + " targetParty=" + (target.getParty() == null ? "null" : target.getParty().getId())
    + " chrParty=" + (chr.getParty() == null ? "null" : chr.getParty().getId()));

                        continue;
                    }

                    if (target != chr) {
                        if (!isInSameParty(chr, target)) {
                            
                            continue;
                        }
                    }

                    if (sum.getPosition().distance(target.getPosition()) > range) {
                        continue;
                    }

                    System.out.println("[35111011] hp=" + eff.getHp() + " x=" + eff.getX() + " y=" + eff.getY());
                    final int heal = (int) (target.getStat().getCurrentMaxHp() * eff.getHp() / 100.0);
                    if (heal > 0) {
                        target.addHP(heal);

                        target.getClient().getSession().write(
                                MaplePacketCreator.showOwnBuffEffect(sum.getSkill(), 2, target.getLevel(), sum.getSkillLevel())
                        );
                        target.getMap().broadcastMessage(
                                target,
                                MaplePacketCreator.showBuffeffect(target.getId(), sum.getSkill(), 2, target.getLevel(), sum.getSkillLevel()),
                                false
                        );
                    }
                }
                break;
            }
            case 1321007: //beholder
                byte buffEff = 0;
                int s = slea.readInt();
                byte donno = slea.readByte();
                Skill bHealing = SkillFactory.getSkill(s);
                final int bHealingLvl = chr.getTotalSkillLevel(bHealing);
                if (bHealingLvl <= 0 || bHealing == null) {
                    return;
                }
                final MapleStatEffect healEffect = bHealing.getEffect(bHealingLvl);
                if (bHealing.getId() == 1320009) {
                    healEffect.applyTo(chr);
                    buffEff = slea.readByte();

                    if (buffEff < 0 || buffEff > 4) {
                        return;
                    }
                    int buffid = 2022125 + buffEff;
                    MapleStatEffect stateff = MapleItemInformationProvider.getInstance().getItemEffect(buffid);
                    Map<MapleBuffStat, Integer> localstatups = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);

                    if (buffid == 2022125) {
                        final int wdefBuff = Math.max(healEffect.getEnhancedWdef(), healEffect.getWdef());
                        if (wdefBuff > 0) {
                            localstatups.put(MapleBuffStat.ENHANCED_WDEF, wdefBuff);
                        }
                    } else if (buffid == 2022126) {
                        final int mdefBuff = Math.max(healEffect.getEnhancedMdef(), healEffect.getMdef());
                        if (mdefBuff > 0) {
                            localstatups.put(MapleBuffStat.ENHANCED_MDEF, mdefBuff);
                        }
                    } else if (buffid == 2022127) {
                        if (healEffect.getAcc() > 0) {
                            localstatups.put(MapleBuffStat.ACC, (int) healEffect.getAcc());
                        }
                    } else if (buffid == 2022128) {
                        if (healEffect.getAvoid() > 0) {
                            localstatups.put(MapleBuffStat.AVOID, (int) healEffect.getAvoid());
                        }
                    } else if (buffid == 2022129) {
                        final int watkBuff = Math.max(healEffect.getEnhancedWatk(), healEffect.getWatk());
                        if (watkBuff > 0) {
                            localstatups.put(MapleBuffStat.ENHANCED_WATK, watkBuff);
                        }
                    }
                    if (localstatups.isEmpty()) {
                        return;
                    }
                    //chr.cancelEffect(stateff, -1, localstatups, true);
                    chr.getClient().getSession().write(TemporaryStatsPacket.giveBuff(buffid, healEffect.getDuration(), localstatups, stateff));
                    final long starttime = System.currentTimeMillis();
                    final MapleStatEffect.CancelEffectAction cancelAction = new MapleStatEffect.CancelEffectAction(chr, stateff, starttime, localstatups);
                    final ScheduledFuture<?> schedule = Timer.BuffTimer.getInstance().schedule(cancelAction, healEffect.getDuration());
                    chr.registerEffect(stateff, starttime, schedule, localstatups, false, healEffect.getDuration(), chr.getId());
                } else if (bHealing.getId() == 1320008) {
                    if (!chr.canSummon(healEffect.getX() * 1000)) {
                        return;
                    }
                    chr.addHP(healEffect.getHp());
                }
                chr.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(sum.getSkill(), 2, chr.getLevel(), bHealingLvl));//얘도 잘나감
                //chr.getMap().broadcastMessage(MaplePacketCreator.summonAttack(sum.getOwnerId(), sum.getObjectId(), (byte) 0x83, null, chr.getLevel(), true));
                chr.getMap().broadcastMessage(MaplePacketCreator.summonSkill(chr.getId(), sum.getObjectId(), bHealing.getId() == 1320008 ? 7 : buffEff + 7), chr.getTruePosition());//4번인가 리벤지 이펙트
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), sum.getSkill(), 2, chr.getLevel(), bHealingLvl), false);//얜 잘 나감
        }
    }

    private static boolean isInSameParty(final MapleCharacter chr, final MapleCharacter target) {
        if (chr.getParty() == null) {
            return false;
        }
        for (final handling.world.MaplePartyCharacter mpc : chr.getParty().getMembers()) {
            if (mpc.getId() == target.getId()) {
                return true;
            }
        }
        return false;
    }
}
