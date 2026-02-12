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
import client.inventory.MapleInventoryType;
import server.MapleInventoryManipulator;
import server.Randomizer;
import server.Timer.MapTimer;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.MapleMap;
import server.maps.MapleNodes.MapleNodeInfo;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.MobPacket;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import server.MapleStatEffect;
import server.Timer;
import server.life.MapleLifeFactory;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleReactor;

public class MobHandler {

    public static final void MoveMonster(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return; //?
        }
        final int oid = slea.readInt();
        final MapleMonster monster = chr.getMap().getMonsterByOid(oid);
        if (monster == null) { // movin something which is not a monster
            return;
        }
        if (monster.getLinkCID() > 0) {
            return;
        }
        final short moveid = slea.readShort();
        byte v9 = slea.readByte();
        final boolean bCheatResult = (v9 & 0xF) != 0;
        final boolean v56 = (v9 & 0xF0) != 0;

        int pCenterSplit = slea.readByte();
        int nAction = pCenterSplit;
        int skill1 = slea.readByte() & 0xFF; // unsigned?
        int skill2 = slea.readByte() & 0xFF;
        short option = slea.readShort();//skill_3,skill_4 
        if (nAction < 0) {
            nAction = -1;
        } else {
            nAction = nAction >> 1;
        }

        int realskill = 0;
        int level = 0;

        monster.setNextAttackPossible(bCheatResult);

        if ((nAction >= 21 && nAction <= 25) || bCheatResult) { // Monster Skill
            boolean madeSkill = !(nAction >= 21 && nAction <= 25);
            int skillid = skill1;
            int skilllevel = skill2;
            final int skillDelay = option;
            final byte size = monster.getNoSkills();

            if (size > 0) {
                if (madeSkill) {
                    for (final Pair<Integer, Integer> skillToUse : monster.getSkills()) {
                        final Pair<Integer, Integer> skillToUse2 = monster.getSkills().get((byte) Randomizer.nextInt(size));
                        skillid = skillToUse.getLeft();
                        skilllevel = skillToUse2.getRight();
                        //skilllevel = skillToUse.getRight();
                        if (monster.hasSkill(skillid, skilllevel)) {
                            final MobSkill mobSkill = MobSkillFactory.getMobSkill(skillid, skilllevel);

                            if (mobSkill != null && !mobSkill.checkCurrentBuff(chr, monster)) {
                                final long now = System.currentTimeMillis();
                                final long ls = monster.getLastSkillUsed(skillid);

                                if (ls == 0 || (((now - ls) > mobSkill.getCoolTime()) && !mobSkill.onlyOnce())) {
                                    final int reqHp = (int) (((float) monster.getHp() / monster.getMobMaxHp()) * 100); // In case this monster have 2.1b and above HP
                                    if (reqHp <= mobSkill.getHP()) {
                                        monster.setLastSkillUsed(skillid, now, mobSkill.getCoolTime());
                                        realskill = skillid;
                                        level = skilllevel;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    /*if (skillid == 200 || skillid == 129) {
                     chr.dropMessage(6, "skillid: " + skillid + "skilllevel: " + skilllevel + "option: " + option);
                     }*/
                    if (monster.hasSkill(skillid, skilllevel)) {
                        //chr.dropMessage(6, "skilllevel" + skilllevel);
                        final MobSkill mobSkill = MobSkillFactory.getMobSkill(skillid, skilllevel);
                        if (monster.isAlive()) {
                            c.getSession().write(MobPacket.MobSkillDelay(oid, skillid, skilllevel, 0, (short) option));
                            //mobSkill.applyEffect(chr, monster, true, (short) skillDelay);
                        }
                    }
                }
            }
        }

        if (monster.getController() != null && monster.getController().getId() != c.getPlayer().getId()) {
            if (!v56/* || monster.getNextAttackPossible()*/) { // 동시에 컨트롤 방지.. 안그럼 문워크함 ㅠㅠ
//                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(5, c.getPlayer().getName() + "- !v56 : " + !v56/* + " / mb : " + monster.getNextAttackPossible()*/));
                c.sendPacket(MobPacket.stopControllingMonster(oid));
                return;
            } else {
                monster.switchController(chr, true);
            }
        }
        List<Integer> abb = new ArrayList<>();
        List<Integer> abc = new ArrayList<>();
        int ass = slea.readInt();
        for (int i = 0; i < ass; i++) {
            abb.add(slea.readInt());
            abc.add(slea.readInt());
        }

        int as = slea.readInt();
        List<Integer> ab = new ArrayList<>();
        for (int i = 0; i < as; i++) {
            ab.add(slea.readInt());
        }

        slea.skip(21);
        slea.skip(4);
        final Point startPos = monster.getPosition();
        List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 2);

        if (res != null && chr != null && res.size() > 0) {
            final MapleMap map = chr.getMap();
            for (final LifeMovementFragment move : res) {
                if (move instanceof AbsoluteLifeMovement) {
                    final Point endPos = ((LifeMovement) move).getPosition();
                    if (endPos.x < (map.getLeft() - 250) || endPos.y < (map.getTop() - 250) || endPos.x > (map.getRight() + 250) || endPos.y > (map.getBottom() + 250)) { //experimental
                        chr.getCheatTracker().checkMoveMonster(endPos);
                        return;
                    }
                }
            }
            monster.receiveMovePacket();
            //c.getPlayer().dropMessage(5, "bCheatResult : " + bCheatResult + " nAction : " + nAction + " / s1 : " + skill1 + " / s2 : " + skill2 + " / s3 : " + skill3 + " / s4 : " + skill4 + " / realskill : " + realskill + " / reallevel : " + level);
            c.getSession().write(MobPacket.moveMonsterResponse(monster.getObjectId(), moveid, Math.max(monster.getMp(), Math.min(monster.getStats().getMp(), 500)), bCheatResult, realskill, level));
            /*if (slea.available() != 9) { //9.. 0 -> endPos? -> endPos again? -> 0 -> 0
             //FileoutputUtil.log(FileoutputUtil.PacketEx_Log, "slea.available != 25 (movement parsing error)\n" + slea.toString(true));
             //c.getSession().close();
             return;
             }*/ //1.2.41 주석해제 

            MovementParse.updatePosition(res, monster, -1);
            final Point endPos = monster.getTruePosition();
            map.moveMonster(monster, endPos);
            map.broadcastMessage(chr, MobPacket.moveMonster(bCheatResult, pCenterSplit, skill1, skill2, option, monster.getObjectId(), startPos, res, abb, abc, ab), endPos);
            chr.getCheatTracker().checkMoveMonster(endPos);
        }
    }

    public static final void FriendlyDamage(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMap map = chr.getMap();
        if (map == null) {
            return;
        }
        final MapleMonster mobfrom = map.getMonsterByOid(slea.readInt());
        slea.skip(4); // Player ID
        final MapleMonster mobto = map.getMonsterByOid(slea.readInt());

        if (mobfrom != null && mobto != null && mobto.getStats().isFriendly()) {
//            final int damage = (mobto.getStats().getLevel() * Randomizer.nextInt(mobto.getStats().getLevel())) / 2;
            final int damage = (mobto.getStats().getLevel() * Randomizer.nextInt(mobto.getStats().getLevel())) / 3; // Temp for now until I figure out something more effective
            //final int damage = (Randomizer.rand(mobto.getStats().getLevel() / 3, mobto.getStats().getLevel() / 2)) * 4; // Temp for now until I figure out something more effective
            mobto.damage(chr, damage, true);
            mobto.setHittedTime();
            checkShammos(chr, mobto, map);
        }
    }

    public static final void MobSkillDelayEnd(LittleEndianAccessor slea, MapleCharacter chr) {
        MapleMonster monster = chr.getMap().getMonsterByOid(slea.readInt());
        if (monster != null) {
            int skillID = slea.readInt();
            int skillLv = slea.readInt();
            int option = slea.readInt();
            if (monster.hasSkill(skillID, skillLv)) {
                MobSkillFactory.getMobSkill(skillID, skillLv).applyEffect(chr, monster, true, (short) option);
            }
        }
    }

    public static final void MobBomb(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMap map = chr.getMap();
        final MapleMonster mobfrom = map.getMonsterByOid(slea.readInt());
        if (mobfrom == null) {
            return;
        }
        final int readCharPosX = slea.readInt();
        final int readCharPosY = slea.readInt();
        Skill skill = SkillFactory.getSkill(4341003);
        MapleStatEffect effect = skill.getEffect(chr.getSkillLevel(skill));
        int damage = 0;
        //map.broadcastMessage(MaplePacketCreator.showbomb(chr.getId(), mobfrom.getObjectId(), mobfrom.getPosition().x, mobfrom.getPosition().y));
        map.broadcastMessage(MobPacket.showMonsterBomb(mobfrom.getObjectId(),chr.getId()));
        final Rectangle bounds = effect.calculateBoundingBox(new Point(mobfrom.getPosition().x, mobfrom.getPosition().y + 150), mobfrom.isFacingLeft());
        final List<MapleMapObject> affecteds = mobfrom.getMap().getMapObjectsInRect(bounds, Arrays.asList(MapleMapObjectType.PLAYER));
        for (MapleMapObject affectedmo : affecteds) {
            MapleCharacter affected = (MapleCharacter) affectedmo;
            if (affected == chr) {
                damage = chr.getStat().getMaxHp() / 10;
                break;
            }
        }
        chr.getClient().getSession().write(MaplePacketCreator.getTimeBombAttack(mobfrom.getPosition(), 4341003, damage));
        if (damage > 0) {
            chr.addMPHP(-damage, 0);
            map.broadcastMessage(MaplePacketCreator.damagePlayer(0, mobfrom.getId(), chr.getId(), (int) damage, damage, 0, (byte) 0, 0, false, (byte) 0, readCharPosX, readCharPosY, 0));
        }
    }

    public static final void checkShammos(final MapleCharacter chr, final MapleMonster mobto, final MapleMap map) {
        if (!mobto.isAlive() && mobto.getStats().isEscort()) { //shammos
            for (MapleCharacter chrz : map.getCharactersThreadsafe()) { //check for 2022698
                if (chrz.getParty() != null && chrz.getParty().getLeader().getId() == chrz.getId()) {
                    //leader
                    if (chrz.haveItem(2022698)) {
                        MapleInventoryManipulator.removeById(chrz.getClient(), MapleInventoryType.USE, 2022698, 1, false, true);
                        mobto.heal((int) mobto.getMobMaxHp(), mobto.getMobMaxMp(), true);
                        return;
                    }
                    break;
                }
            }
            map.broadcastMessage(MaplePacketCreator.serverNotice(6, "Your party has failed to protect the monster."));
            final MapleMap mapp = chr.getMap().getForcedReturnMap();
            for (MapleCharacter chrz : map.getCharactersThreadsafe()) {
                chrz.changeMap(mapp, mapp.getPortal(0));
            }
        } else if (mobto.getStats().isEscort() && mobto.getEventInstance() != null) {
            mobto.getEventInstance().setProperty("HP", String.valueOf(mobto.getHp()));
        }
    }

    public static final void MonsterBomb(final int oid, final MapleCharacter chr) {
        final MapleMonster monster = chr.getMap().getMonsterByOid(oid);

        if (monster == null || !chr.isAlive() || chr.isHidden() || chr.isVacFucking() || monster.getLinkCID() > 0) {
            return;
        }
        final byte selfd = monster.getStats().getSelfD();
        if (selfd != -1) {
            chr.getMap().killMonster(monster, chr, false, false, selfd);
        }
    }

    public static final void AutoAggro(final int monsteroid, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null || chr.isHidden() || chr.isVacFucking()) { //no evidence :)
            return;
        }
        final MapleMonster monster = chr.getMap().getMonsterByOid(monsteroid);

        if (monster != null && chr.getTruePosition().distanceSq(monster.getTruePosition()) < 200000 && monster.getLinkCID() <= 0) {
            if (monster.getController() != null) {
                if (chr.getMap().getCharacterById(monster.getController().getId()) == null) {
                    monster.switchController(chr, true);
                }
//                else {
//                    monster.switchController(monster.getController(), true);
//                }
            } else {
                monster.switchController(chr, true);
            }
        }
    }

    public static final void HypnotizeDmg(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMonster mob_from = chr.getMap().getMonsterByOid(slea.readInt()); // From
        slea.skip(4); // Player ID
        final int to = slea.readInt(); // mobto
        slea.skip(1); // Same as player damage, -1 = bump, integer = skill ID
        final int damage = slea.readInt();
        slea.skip(1); // Facing direction
        slea.skip(4); // Some type of pos, damage display, I think

        final MapleMonster mob_to = chr.getMap().getMonsterByOid(to);

        if (mob_from != null && mob_to != null) { //temp for nowfr
            mob_to.damage(chr, damage, true);
            //chr.getMap().broadcastMessage(chr, MobPacket.damageMonster(to, damage), false);//이거안쏴주면 데미지안보임         
            checkShammos(chr, mob_to, chr.getMap());
        }

    }

    public static final void DisplayNode(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMonster mob_from = chr.getMap().getMonsterByOid(slea.readInt()); // From
        if (mob_from != null) {
            chr.getClient().getSession().write(MaplePacketCreator.getNodeProperties(mob_from, chr.getMap()));
        }
    }

    public static final void MobNode(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMonster mob_from = chr.getMap().getMonsterByOid(slea.readInt()); // From
        final int newNode = slea.readInt();
        final int nodeSize = chr.getMap().getNodes().size();
        if (mob_from != null && nodeSize > 0) {
            final MapleNodeInfo mni = chr.getMap().getNode(newNode);
            if (mni == null) {
                return;
            }
            if (mni.attr == 2) { //talk
                switch (chr.getMapId() / 100) {
                    case 9211200:
                    case 9211201:
                    case 9211202:
                    case 9211203:
                    case 9211204:
                        chr.getMap().talkMonster("나를 잘 호위하도록 해. 너무 먼저 멀리 가버리면 아마 모든것은 실패하게 될거야.", 5120035, mob_from.getObjectId()); //temporary for now. itemID is located in WZ file
                        break;
                    case 9211205:
                        if (newNode == 2) {
                            chr.getMap().talkMonster("봉인이 잘 되었는지 확인을 해볼까?", 0, mob_from.getObjectId());
                        } else if (newNode == 4) {
                            chr.getMap().getReactorById(2118003).forceHitReactor((byte) 1);
                            Timer.EventTimer.getInstance().schedule(new Runnable() {
                                @Override
                                public final void run() {
                                    chr.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9300281), new Point(328, 174));
                                }
                            }, 2000);
                            chr.getMap().talkMonster("이런 자물쇠 따위. 낄낄.", 0, mob_from.getObjectId());
                            chr.getMap().getReactorById(2118003).forceHitReactor((byte) 1);
                            Timer.EventTimer.getInstance().schedule(new Runnable() {
                                @Override
                                public final void run() {
                                    chr.getMap().killMonster(mob_from, null, false, false, (byte) 0);
                                }
                            }, 2000);
                        } else if (newNode == 5) {
                            chr.getMap().talkMonster("어리석은 인간들같으니라고! 이제 깨달았나? 너흰 단지 내가 렉스의 봉인을 푸는 것을 도와준 것 뿐이라는 것을!", 0, mob_from.getObjectId());
                        } else if (newNode == 6) {
                            chr.getMap().talkMonster("힘을내라 렉스!", 0, mob_from.getObjectId());
                        }
                        break;
                    case 9320001:
                    case 9320002:
                    case 9320003:
                        chr.getMap().talkMonster("이런! 렉스가 봉인된 동굴로 가는 길에 몬스터가 너무 많아. 이것들로부터 모두 처리해줘.", 5120051, mob_from.getObjectId()); //temporary for now. itemID is located in WZ file
                        break;
                }
            }
            mob_from.setLastNode(newNode);
            if (chr.getMap().isLastNode(newNode)) { //the last node on the map.
                switch (chr.getMapId() / 100) {
                    case 9211200:
                    case 9211201:
                    case 9211202:
                    case 9211203:
                    case 9211204:
                    case 9320001:
                    case 9320002:
                    case 9320003:
                        chr.getMap().broadcastMessage(MaplePacketCreator.serverNotice(5, "샤모스가 도착했습니다. 다음 스테이지로 이동하시기 바랍니다."));
                        chr.getMap().removeMonster(mob_from);
                        break;

                }
            }
        }
    }
}
