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
package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import client.Skill;
import client.SkillFactory;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.life.MobSkill;
import tools.MaplePacketCreator;

import java.awt.*;
import java.util.concurrent.ScheduledFuture;

public class MapleMist extends MapleMapObject {

    private Rectangle mistPosition;
    private MapleStatEffect source;
    private MobSkill skill;
    private MapleCharacter owner = null;
    private MapleMonster mob = null;
    private boolean isMobMist;
    private boolean isSmMist;
    private boolean isRoMist;

    private int skillDelay, skilllevel, isPoisonMist, ownerId;
    private int party = -1, chr = -1;
    private ScheduledFuture<?> schedule = null, poisonSchedule = null;

    public MapleMist(Rectangle mistPosition, MapleMonster mob, MobSkill skill) {
        this.mistPosition = mistPosition;
        this.ownerId = mob.getId();
        this.skill = skill;
        this.skilllevel = skill.getSkillLevel();
        this.mob = mob;

        isMobMist = true;
        isSmMist = false;
        isRoMist = false;
        isPoisonMist = 1;
        skillDelay = 0;
    }

    public MapleMist(Rectangle mistPosition, MapleCharacter owner, MapleStatEffect source) {
        this.mistPosition = mistPosition;
        this.owner = owner;
        this.ownerId = owner.getId();
        this.source = source;
        this.skillDelay = 8;
        this.isMobMist = false;
        this.isSmMist = false;
        this.isRoMist = false;
        this.skilllevel = owner.getTotalSkillLevel(SkillFactory.getSkill(source.getSourceId()));

        switch (source.getSourceId()) {
            case 2111003: // FP mist
            case 12111005: // Flame wizard, [Flame Gear]
            case 14111006:
                isPoisonMist = 0;
                break;
            case 4221006:
            case 32121006:
                isSmMist = true;
                isPoisonMist = 2;
                break;
            case 22161003: //Recovery Aura
                if (owner.getParty() != null) {
                    party = owner.getParty().getId();
                }
                chr = owner.getId();
                isRoMist = true;
                isPoisonMist = 4;
                break;
        }
    }

    //fake
    public MapleMist(Rectangle mistPosition, MapleCharacter owner) {
        this.mistPosition = mistPosition;
        this.ownerId = owner.getId();
        this.owner = owner;
        this.isSmMist = false;
        this.source = new MapleStatEffect();
        this.source.setSourceId(2111003);
        this.skilllevel = 30;

        isMobMist = true;
        isPoisonMist = 1;
        skillDelay = 8;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.MIST;
    }

    @Override
    public Point getPosition() {
        return mistPosition.getLocation();
    }

    public Skill getSourceSkill() {
        return SkillFactory.getSkill(source.getSourceId());
    }

    public void setSchedule(ScheduledFuture<?> s) {
        this.schedule = s;
    }

    public ScheduledFuture<?> getSchedule() {
        return schedule;
    }

    public void setPoisonSchedule(ScheduledFuture<?> s) {
        this.poisonSchedule = s;
    }

    public ScheduledFuture<?> getPoisonSchedule() {
        return poisonSchedule;
    }

    public int isPoisonMist() {
        return isPoisonMist;
    }

    public boolean isSmMist() {
        return isSmMist;
    }

    public boolean isRoMist() {
        return isRoMist;
    }

    public boolean isMobMist() {
        return isMobMist;
    }

    public int getSkillDelay() {
        return skillDelay;
    }

    public int getSkillLevel() {
        return skilllevel;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public MobSkill getMobSkill() {
        return this.skill;
    }

    public Rectangle getBox() {
        return mistPosition;
    }

    public MapleStatEffect getSource() {
        return source;
    }
    
    public MapleMonster getMobOwner() {
        return mob;
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    @Override
    public void setPosition(Point position) {
    }

    public byte[] fakeSpawnData(int level) {
        if (owner != null) {
            return MaplePacketCreator.spawnMist(getObjectId(), owner.getId(), getSourceSkill().getId(), level, this);
        }
        return MaplePacketCreator.spawnMist(getObjectId(), mob.getId(), skill.getSkillId(), skill.getSkillLevel(), this);
    }

    @Override
    public void sendSpawnData(final MapleClient c) {
        if (owner != null) {
            c.getSession().write(MaplePacketCreator.spawnMist(getObjectId(), owner.getId(), getSourceSkill().getId(), owner.getSkillLevel(SkillFactory.getSkill(source.getSourceId())), this));
        } else {
            c.getSession().write(MaplePacketCreator.spawnMist(getObjectId(), mob.getId(), skill.getSkillId(), skill.getSkillLevel(), this));
        }
    }

    @Override
    public void sendDestroyData(final MapleClient c) {
        c.getSession().write(MaplePacketCreator.removeMist(getObjectId(), false));
    }

    public boolean makeChanceResult() {
        return source.makeChanceResult();
    }

    public int getParty() {
        return party;
    }

    public int getChr() {
        return chr;
    }
}
