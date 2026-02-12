/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.MapleBuffStat;
import client.MapleDisease;
import constants.GameConstants;
import handling.SendPacketOpcode;
import server.MapleStatEffect;
import tools.Pair;
import tools.data.MaplePacketLittleEndianWriter;

import java.util.List;
import java.util.Map;

/**
 * @author 티썬
 */
public class TemporaryStatsPacket {

    public static byte[] giveBuff(int buffid, int bufflength, Map<MapleBuffStat, Integer> statups, MapleStatEffect effect) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.GIVE_BUFF.getValue());
        PacketHelper.writeBuffMask(mplew, statups);
        for (Map.Entry<MapleBuffStat, Integer> stat : statups.entrySet()) {
            if (stat.getKey() == MapleBuffStat.ENHANCED_MAXHP) { //스피드+점프+enhanced 최우선순위
                mplew.writeShort(stat.getValue().intValue());
                mplew.writeInt(buffid);
                mplew.writeInt(bufflength);
            }
        }
        if (buffid == 2301004) {
            for (Map.Entry<MapleBuffStat, Integer> stat : statups.entrySet()) {
                mplew.writeShort(stat.getValue().intValue());
                mplew.writeInt(buffid);
                mplew.writeInt(bufflength);
            }
        } else {
            for (Map.Entry<MapleBuffStat, Integer> stat : statups.entrySet()) {
                if (stat.getKey() != MapleBuffStat.ENHANCED_MAXHP) {
                    if (stat.getKey() == MapleBuffStat.ENHANCED_WATK
                            || stat.getKey() == MapleBuffStat.ENHANCED_MAXHP
                            || stat.getKey() == MapleBuffStat.ENHANCED_MAXMP
                            || stat.getKey() == MapleBuffStat.ENHANCED_MATK
                            || stat.getKey() == MapleBuffStat.ENHANCED_WDEF
                            || stat.getKey() == MapleBuffStat.ENHANCED_MDEF
                            || stat.getKey() == MapleBuffStat.SPEED
                            || stat.getKey() == MapleBuffStat.JUMP) { //스피드+점프+enhanced 최우선순위
                        mplew.writeShort(stat.getValue().intValue());
                        mplew.writeInt(buffid);
                        mplew.writeInt(bufflength);
                    }
                }
            }
            for (Map.Entry<MapleBuffStat, Integer> stat : statups.entrySet()) {
                if (stat.getKey() != MapleBuffStat.ENHANCED_WATK
                        && stat.getKey() != MapleBuffStat.ENHANCED_MAXHP
                        && stat.getKey() != MapleBuffStat.ENHANCED_MAXMP
                        && stat.getKey() != MapleBuffStat.ENHANCED_MATK
                        && stat.getKey() != MapleBuffStat.ENHANCED_WDEF
                        && stat.getKey() != MapleBuffStat.ENHANCED_MDEF
                        && stat.getKey() != MapleBuffStat.SPEED
                        && stat.getKey() != MapleBuffStat.JUMP) {
                    mplew.writeShort(stat.getValue().intValue());
                    if (buffid == 35101005 || buffid == 35001002) {
                        mplew.writeInt(0);
                    } else {
                        mplew.writeInt(buffid);
                    }
                    mplew.writeInt(bufflength);
                }
            }
        }
        mplew.writeShort(0); // delay
        if (effect != null && effect.isDivineShield()) {
            mplew.writeInt(effect.getEnhancedWatk());
        }
        if (buffid == 33101006) {
            mplew.writeShort(bufflength / 1000); //와헌 스왈로우 스킬 
        } else {
            mplew.writeShort(0);
        }
        mplew.write(1);
        mplew.write(effect != null && effect.isShadow() ? 1 : 4); // Test
        return mplew.getPacket();
    }

    public static byte[] giveMount(int buffid, int skillid, Map<MapleBuffStat, Integer> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GIVE_BUFF.getValue());

        PacketHelper.writeBuffMask(mplew, statups);

        mplew.writeShort(0);
        mplew.writeInt(buffid); // 1902000 saddle
        mplew.writeInt(skillid); // skillid
        mplew.writeInt(0); // ??
        mplew.writeShort(0);
        mplew.write(1);
        mplew.write(4); // 버프시간

        return mplew.getPacket();
    }

    public static byte[] giveForeignMount(int cid, int saddle, int skill, Map<MapleBuffStat, Integer> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        PacketHelper.writeBuffMask(mplew, statups);
        mplew.writeShort(0);
        mplew.writeInt(saddle); // 1902000 saddle
        mplew.writeInt(skill); // skillid
        mplew.writeInt(0); // ??
        mplew.writeInt(0);
        mplew.write(0); // Total buffed times
        return mplew.getPacket();
    }

    public static byte[] giveEnergyChargeTest(int bar, int bufflength) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GIVE_BUFF.getValue());
        PacketHelper.writeSingleMask(mplew, MapleBuffStat.ENERGY_CHARGE);
        mplew.writeShort(0);
        mplew.writeInt(Math.min(bar, 10000)); // 0 = no bar, 10000 = full bar
        mplew.writeLong(0); //skillid, but its 0 here
        mplew.write(0);
        mplew.writeInt(bar >= 10000 ? bufflength : 0);//short - bufflength...50
        return mplew.getPacket();
    }

    public static byte[] giveEnergyChargeTest(int cid, int bar, int bufflength) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        PacketHelper.writeSingleMask(mplew, MapleBuffStat.ENERGY_CHARGE);
        mplew.writeShort(0);
        mplew.writeInt(Math.min(bar, 10000)); // 0 = no bar, 10000 = full bar
        mplew.writeLong(0); //skillid, but its 0 here
        mplew.write(0);
        mplew.writeInt(bar >= 10000 ? bufflength : 0);//short - bufflength...50
        return mplew.getPacket();
    }

    public static byte[] givePirate(Map<MapleBuffStat, Integer> statups, int duration, int skillid) {
        final boolean infusion = skillid == 5121009 || skillid == 15111005 || skillid % 10000 == 8006;
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GIVE_BUFF.getValue());
        PacketHelper.writeBuffMask(mplew, statups);

        mplew.writeShort(0);
        for (Integer stat : statups.values()) {
            mplew.writeInt(stat.intValue());
            mplew.writeLong(skillid);
            mplew.writeZeroBytes(infusion ? 6 : 1);
            mplew.writeShort(duration);
        }
        mplew.writeShort(0);
        mplew.writeShort(0);
        mplew.write(1);
        mplew.write(1); //does this only come in dash?
        return mplew.getPacket();
    }

    public static byte[] giveForeignPirate(Map<MapleBuffStat, Integer> statups, int duration, int cid, int skillid) {
        final boolean infusion = skillid == 5121009 || skillid == 15111005;
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        PacketHelper.writeBuffMask(mplew, statups);
        mplew.writeShort(0);
        for (Integer stat : statups.values()) {
            mplew.writeInt(stat.intValue());
            mplew.writeLong(skillid);
            mplew.writeZeroBytes(infusion ? 6 : 1);
            mplew.writeShort(duration);//duration... seconds
        }
        mplew.writeShort(0);
        mplew.writeShort(0);
        mplew.write(1);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] giveHoming(int skillid, int mobid, int x) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.GIVE_BUFF.getValue());
        PacketHelper.writeSingleMask(mplew, MapleBuffStat.HOMING_BEACON);
        mplew.writeShort(0);
        mplew.writeInt(x);
        mplew.writeLong(skillid);
        mplew.write(0);
        mplew.writeLong(mobid);
        mplew.writeShort(0);
        mplew.writeShort(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] giveForeignHoming(int skillid, int mobid, int x, int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        PacketHelper.writeSingleMask(mplew, MapleBuffStat.HOMING_BEACON);
        mplew.writeShort(0);
        mplew.writeInt(x);
        mplew.writeLong(skillid);
        mplew.write(0);
        mplew.writeLong(mobid);
        mplew.writeShort(0);
        mplew.writeShort(0);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] cancelHoming() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.CANCEL_BUFF.getValue());
        PacketHelper.writeSingleMask(mplew, MapleBuffStat.HOMING_BEACON);
        return mplew.getPacket();
    }

    public static byte[] giveDice(int buffid, int skillid, int duration, Map<MapleBuffStat, Integer> statups) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GIVE_BUFF.getValue());

        PacketHelper.writeBuffMask(mplew, statups);

        mplew.writeShort(Math.max(buffid / 100, Math.max(buffid / 10, buffid % 10))); // 1-6

        mplew.writeInt(skillid); // skillid
        mplew.writeInt(duration);
        mplew.writeShort(0);

        mplew.writeInt(GameConstants.getDiceStat(buffid, 3));
        mplew.writeInt(GameConstants.getDiceStat(buffid, 3));
        mplew.writeInt(GameConstants.getDiceStat(buffid, 4));
        mplew.writeZeroBytes(20); //idk
        mplew.writeInt(GameConstants.getDiceStat(buffid, 2));
        mplew.writeZeroBytes(12); //idk
        mplew.writeInt(GameConstants.getDiceStat(buffid, 5));
        mplew.writeZeroBytes(16); //idk
        mplew.writeInt(GameConstants.getDiceStat(buffid, 6));
        mplew.writeZeroBytes(16);
        mplew.write(1);
        mplew.write(4); // Total buffed times

        return mplew.getPacket();
    }

    public static byte[] giveDebuff(MapleDisease statups, int x, int skillid, int level, int duration, short tDelay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GIVE_BUFF.getValue());

        PacketHelper.writeSingleMask(mplew, statups);

        mplew.writeShort(x);
        mplew.writeShort(skillid);
        mplew.writeShort(level);
        mplew.writeInt(duration);
        mplew.writeShort(0); // ??? wk charges have 600 here o.o
        mplew.writeShort(tDelay); //Delay
        mplew.write(1);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] giveForeignDebuff(int cid, final MapleDisease statups, int skillid, int level, int x, short tDelay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);

        PacketHelper.writeSingleMask(mplew, statups);
        if (skillid == 125) {
            mplew.writeShort(x);
        }
        mplew.writeShort(skillid);
        mplew.writeShort(level);
        mplew.writeShort(0); // same as give_buff
        mplew.writeShort(tDelay); //Delay
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] giveForeignDebuffSlow(int cid, final MapleDisease statups, int skillid, int level, int x, int tDelay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);

        mplew.writeInt(0);
        mplew.writeInt(2048);
        mplew.writeLong(0);

        mplew.writeShort(skillid);
        mplew.writeShort(level);

        mplew.writeShort(0); //Delay
        mplew.writeShort(tDelay);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] cancelForeignDebuff(int cid, MapleDisease mask) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);

        PacketHelper.writeSingleMask(mplew, mask);
        mplew.write(3);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] cancelForeignDebuffSlow(int cid, MapleDisease mask) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);

        mplew.writeInt(0);
        mplew.writeInt(2048);
        mplew.writeLong(0);
        mplew.write(3);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] giveForeignBuff(int cid, Map<MapleBuffStat, Integer> statups, MapleStatEffect effect) { //2
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);

        PacketHelper.writeBuffMask(mplew, statups);
        for (Map.Entry<MapleBuffStat, Integer> statup : statups.entrySet()) {
            if (statup.getKey() == MapleBuffStat.MAGIC_SHIELD
                    || statup.getKey() == MapleBuffStat.SHADOWPARTNER
                    || statup.getKey() == MapleBuffStat.MIRROR_IMAGE
                    || statup.getKey() == MapleBuffStat.MECH_CHANGE
                    || statup.getKey() == MapleBuffStat.DARK_AURA
                    || statup.getKey() == MapleBuffStat.YELLOW_AURA
                    || statup.getKey() == MapleBuffStat.BLUE_AURA
                    || statup.getKey() == MapleBuffStat.PYRAMID_PQ
                    || statup.getKey() == MapleBuffStat.WK_CHARGE
                    || statup.getKey() == MapleBuffStat.LIGHTNING_CHARGE
                    || statup.getKey() == MapleBuffStat.MORPH
                    || statup.getKey() == MapleBuffStat.FINAL_CUT
                    || statup.getKey() == MapleBuffStat.OWL_SPIRIT) {
                mplew.writeShort(statup.getValue().shortValue());
                mplew.writeInt(effect.isSkill() ? effect.getSourceId() : -effect.getSourceId());
            } else {
                mplew.writeShort(statup.getValue().shortValue());
            }
        }
        mplew.writeShort(0);
        mplew.writeShort(0);
        mplew.write(1);
        mplew.write(1);
//        System.out.println(HexTool.toString(mplew.getPacket()));

        return mplew.getPacket();
    }
    
    public static byte[] giveMechForeignBuff(int cid, Map<MapleBuffStat, Integer> statups, MapleStatEffect effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);

        PacketHelper.writeBuffMask(mplew, statups);
        for (Map.Entry<MapleBuffStat, Integer> statup : statups.entrySet()) {
            if (statup.getKey() == MapleBuffStat.MECH_CHANGE) {
                mplew.writeInt(effect.getSourceId());
            }
        }
        mplew.writeShort(0); // same as give_buff
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static byte[] cancelForeignBuff(int cid, List<MapleBuffStat> statups) { //3
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        PacketHelper.writeMask(mplew, statups);
        mplew.write(3);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] cancelBuff(List<MapleBuffStat> statups) { //4
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CANCEL_BUFF.getValue());

        PacketHelper.writeMask(mplew, statups);
        mplew.write(3);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] cancelDebuff(MapleDisease mask) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(19);
        mplew.writeOpcode(SendPacketOpcode.CANCEL_BUFF.getValue());
        PacketHelper.writeSingleMask(mplew, mask);
        mplew.write(3);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static boolean isForRemoteStat(MapleBuffStat stat) {
        return stat == MapleBuffStat.SPEED
                || stat == MapleBuffStat.COMBO
                || stat == MapleBuffStat.WK_CHARGE
                || stat == MapleBuffStat.LIGHTNING_CHARGE
                || stat == MapleBuffStat.SHADOWPARTNER
                || stat == MapleBuffStat.MIRROR_IMAGE
                || stat == MapleBuffStat.DARKSIGHT
                || stat == MapleBuffStat.SOULARROW
                || stat == MapleBuffStat.MORPH
                || stat == MapleBuffStat.SPIRIT_CLAW
                || stat == MapleBuffStat.BERSERK_FURY
                || stat == MapleBuffStat.DIVINE_BODY;
    }
}
