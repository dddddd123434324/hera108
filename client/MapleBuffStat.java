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
package client;

import constants.GameConstants;
import handling.Buffstat;
import java.io.Serializable;

public enum MapleBuffStat implements Buffstat {

    WATK(0x1, 1),
    WDEF(0x2, 1),
    MATK(0x4, 1),
    MDEF(0x8, 1),
    ACC(0x10, 1),
    AVOID(0x20, 1),
    HANDS(0x40, 1),
    SPEED(0x80, 1),
    JUMP(0x100, 1),
    MAGIC_GUARD(0x200, 1),
    DARKSIGHT(0x400, 1),
    BOOSTER(0x800, 1),
    POWERGUARD(0x1000, 1),
    MAXHP(0x2000, 1),
    MAXMP(0x4000, 1),
    INVINCIBLE(0x8000, 1),
    SOULARROW(0x10000, 1),
    STUN(0x20000, 1),
    //POISON(0x40000, 1, 125),
    //SEAL(0x80000, 1, 120),

    //DARKNESS(0x100000, 1, 121),
    COMBO(0x200000, 1),
    SUMMON(0x20000000, 1), //hack buffstat for summons ^.- (does/should not increase damage... hopefully <3)
    SUMMON2(0x200000, 1), //hack buffstat for summons ^.- (does/should not increase damage... hopefully <3)
    WK_CHARGE(0x400000, 1),
    DRAGONBLOOD(0x800000, 1),
    HOLY_SYMBOL(0x1000000, 1),
    HOLY_SYMBOL2(0x1000000, 1),
    MESOUP(0x2000000, 1),
    SHADOWPARTNER(0x4000000, 1),
    PICKPOCKET(0x8000000, 1),
    PUPPET(0x8000000, 1), // HACK - shares buffmask with pickpocket - odin special ^.-

    MESOGUARD(0x10000000, 1),
    HP_LOSS_GUARD(0x20000000, 1),
    //WEAKEN(0x40000000, 1, 122),
    //CURSE(0x80000000, 1, 123),

    //SLOW(0x1, 2, 126),
    MORPH(0x2, 2),
    RECOVERY(0x4, 2),
    MAPLE_WARRIOR(0x8, 2),
    STANCE(0x10, 2),
    SHARP_EYES(0x20, 2),
    MANA_REFLECTION(0x40, 2),
    //SEDUCE(0x80, 2, 128),

    SPIRIT_CLAW(0x100, 2),
    INFINITY(0x200, 2),
    HOLY_SHIELD(0x400, 2),
    HAMSTRING(0x800, 2),
    BLIND(0x1000, 2),
    CONCENTRATE(0x2000, 2),
    //4 - debuff
    ECHO_OF_HERO(0x8000, 2),//확인
    donno(0x10000, 2), // _ZtlSecurePut_nBarrier_
    donno1(0x20000, 2), // _ZtlSecurePut_nDojangShield_
    GHOST_MORPH(0x20000, 2),
    ARIANT_COSS_IMU(0x40000, 2), // The white ball around you
    //REVERSE_DIRECTION(0x80000, 2, 132),
    MESO_RATE(0x100000, 2), // MesoUpByItem 패밀리용 
    DROP_RATE(0x200000, 2), // ItemUpByItem 패밀리용
    /*need to check*/
    donno2(0x400000, 2), //nRespectPImmune
    donno3(0x800000, 2), //RespectMImmune
    donno4(0x1000000, 2), //nDefenseAtt_
    donno5(0x2000000, 2), //rDefenseState
    /*뭘까*/
    BERSERK_FURY(0x2000000, 2), //1.2.109 ok
    DIVINE_BODY(0x4000000, 2), //rDojangInvincible 1.2.109 ok
    SPARK(0x8000000, 2), //1.2.109 ok
    ARIANT_COSS_IMU2(0x10000000, 2), //1.2.109 ok
    FINALATTACK(0x20000000, 2), //소마
    FINALATTACK2(0x40000000, 2), //윈브
    ELEMENT_RESET(0x80000000, 2),
    
    WIND_WALK(0x1, 3),
    MESO_DROP_RATE(0x2, 3), //nEventRate
    ARAN_COMBO(0x4, 3),
    COMBO_DRAIN(0x8, 3),
    COMBO_BARRIER(0x10, 3),
    BODY_PRESSURE(0x20, 3),
    SMART_KNOCKBACK(0x40, 3),
    PYRAMID_PQ(0x80, 3),
    EXPRATE(0x100, 3), //nExpBuffRate
    //POTION(0x200, 3, 134),
    //SHADOW(0x400, 3, 135), //receiving damage/moving
    //BLIND(0x800, 3, 136),
    SLOW(0x1000, 3),
    MAGIC_SHIELD(0x2000, 3),
    MAGIC_RESISTANCE(0x4000, 3),
    SOUL_STONE(0x8000, 3),
    SOARING(0x10000, 3),
    //FREEZE(0x20000, 3, 137),
    LIGHTNING_CHARGE(0x40000, 3),
    ENRAGE(0x80000, 3),
    OWL_SPIRIT(0x100000, 3),
    //0x200000 nNotDamaged
    FINAL_CUT(0x400000, 3),
    THORNS_EFFECT(0x800000, 3), //쏜즈
    /*
    순서아니야 ㅠㅠ
    SwallowAttackDamage
    MorewildDamageUp
    Mine
    Cyclone
    SwallowCritical
    SwallowMaxMP
    SwallowDefence
    SwallowEvasion
    nConversion
    Revive
    Sneak
    Mechanic
    */
    ATTACK_BUFF(0x1000000, 3), //SwallowAttackDamage
    MorewildDamageUp(0x2000000, 3), //nMorewildDamageUp
    RAINING_MINES(0x4000000, 3), //mine
    //중첩버프 부분
    ENHANCED_MATK(0x4000000, 3),//donno?
    ENHANCED_MAXHP(0x8000000, 3),
    ENHANCED_MAXMP(0x10000000, 3),//MP가오르
    ENHANCED_WATK(0x20000000, 3),
    ENHANCED_WDEF(0x40000000, 3),
    ENHANCED_MDEF(0x80000000, 3),
    PERFECT_ARMOR(0x1, 4),
    SATELLITESAFE_PROC(0x2, 4),
    SATELLITESAFE_ABSORB(0x4, 4),
    TORNADO(0x8, 4), 
    CRITICAL_RATE_BUFF(0x10, 4),
    MP_BUFF(0x20, 4),
    DAMAGE_TAKEN_BUFF(0x40, 4),
    DODGE_CHANGE_BUFF(0x80, 4),
    CONVERSION(0x100, 4),
    REAPER(0x200, 4),
    INFILTRATE(0x400, 4),
    MECH_CHANGE(0x800, 4),
    AURA(0x1000, 4),
    DARK_AURA(0x2000, 4),
    BLUE_AURA(0x4000, 4),
    YELLOW_AURA(0x8000, 4),
    BODY_BOOST(0x10000, 4),
    FELINE_BERSERK(0x20000, 4), //nMorewildMaxHP
    DICE_ROLL(0x40000, 4),
    DIVINE_SHIELD(0x80000, 4),
    PIRATES_REVENGE(0x100000, 4),
    TELEPORT_MASTERY(0x200000, 4), //1.2.109 OK
    COMBAT_ORDERS(0x400000, 4), //1.2.109 OK
    BEHOLDER(0x800000, 4),
    ENERGY_CHARGE(0x1000000, 4),
    DASH_SPEED(0x2000000, 4),
    DASH_JUMP(0x4000000, 4),
    MONSTER_RIDING(0x8000000, 4),
    SPEED_INFUSION(0x10000000, 4),
    HOMING_BEACON(0x20000000, 4),
    DEFAULT_BUFFSTAT(0x40000000, 4); //end speshulness,     


    /*not used for this version*/
    /*  EXPRATE(0x2000000, 1),
     DROP_RATE(0x2000000, 1),
     MESO_RATE(0x2000000, 1),
     //1 = debuff
     GIANT_POTION(0x2000000, 4),
     ONYX_SHROUD(0x4000000, 4),
     ONYX_WILL(0x8000000, 4),
     //1 = debuff
     BLESS(0x20000000, 4),
     //4 8 unknown

     THREATEN_PVP(0x1, 5),
     ICE_KNIGHT(0x2, 5),
     //4 unknown
     STR(0x8, 5),
     DEX(GameConstants.GMS ? 0x40 : 0x10, 5),
     INT(GameConstants.GMS ? 0x80 : 0x20, 5),
     LUK(GameConstants.GMS ? 0x100 : 0x40, 5),
     //8 unknown

     //1 2 unknown
     ANGEL_ATK(GameConstants.GMS ? 0x1000 : 0x400, 5, true),
     ANGEL_MATK(GameConstants.GMS ? 0x2000 : 0x800, 5, true),
     HP_BOOST(GameConstants.GMS ? 0x4000 : 0x1000, 5, true), //indie hp
     MP_BOOST(GameConstants.GMS ? 0x8000 : 0x2000, 5, true),
     ANGEL_ACC(GameConstants.GMS ? 0x10000 : 0x4000, 5, true),
     ANGEL_AVOID(GameConstants.GMS ? 0x20000 : 0x8000, 5, true),
     ANGEL_JUMP(GameConstants.GMS ? 0x40000 : 0x10000, 5, true),
     ANGEL_SPEED(GameConstants.GMS ? 0x80000 : 0x20000, 5, true),
     ANGEL_STAT(GameConstants.GMS ? 0x100000 : 0x40000, 5, true),
     PVP_DAMAGE(GameConstants.GMS ? 0x200000 : 0x4000, 5),
     PVP_ATTACK(GameConstants.GMS ? 0x400000 : 0x8000, 5), //skills
     INVINCIBILITY(GameConstants.GMS ? 0x800000 : 0x10000, 5),
     HIDDEN_POTENTIAL(GameConstants.GMS ? 0x1000000 : 0x20000, 5),
     ELEMENT_WEAKEN(GameConstants.GMS ? 0x2000000 : 0x40000, 5),
     SNATCH(GameConstants.GMS ? 0x4000000 : 0x80000, 5), //however skillid is 90002000, 1500 duration
     FROZEN(GameConstants.GMS ? 0x8000000 : 0x100000, 5),
     //4, unknown
     ICE_SKILL(GameConstants.GMS ? 0x20000000 : 0x400000, 5),
     //1, 2, 4 unknown
     //8 = debuff

     //1, 2 unknown
     HOLY_MAGIC_SHELL(0x4, 6), //max amount of attacks absorbed
     //8 unknown

     ARCANE_AIM(0x10, 6, true),
     BUFF_MASTERY(0x20, 6), //buff duration increase
     //4, 8 unknown

     WATER_SHIELD(GameConstants.GMS ? 0x400 : 0x100, 6),
     //2, 4, unknown
     SPIRIT_SURGE(GameConstants.GMS ? 0x2000 : 0x800, 6),
     SPIRIT_LINK(GameConstants.GMS ? 0x4000 : 0x1000, 6),
     //2 unknown
     VIRTUE_EFFECT(GameConstants.GMS ? 0x10000 : 0x4000, 6),
     //8, 1, 2 unknown

     NO_SLIP(GameConstants.GMS ? 0x100000 : 0x40000, 6),
     FAMILIAR_SHADOW(GameConstants.GMS ? 0x200000 : 0x80000, 6),
     SIDEKICK_PASSIVE(GameConstants.GMS ? 0x400000 : 0x100000, 6), //skillid 79797980

     //speshul0x1000000
     쏜즈이펙트(0x800000, 3),
     미러이미징(0x80000, 3);*/
    private static final long serialVersionUID = 0L;
    private final int buffstat;
    private final int first;
    private boolean stacked = false;

    private MapleBuffStat(int buffstat, int first) {
        this.buffstat = buffstat;
        this.first = first;
    }

    private MapleBuffStat(int buffstat, int first, boolean stacked) {
        this.buffstat = buffstat;
        this.first = first;
        this.stacked = stacked;
    }

    public final int getPosition() {
        return first;
    }

    public final int getValue() {
        return buffstat;
    }

    public final boolean canStack() {
        return stacked;
    }
}
