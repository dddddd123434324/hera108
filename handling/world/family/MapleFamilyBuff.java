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
package handling.world.family;

import client.MapleBuffStat;
import client.MapleCharacter;
import java.util.EnumMap;
import java.util.concurrent.ScheduledFuture;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.MapleStatEffect.CancelEffectAction;
import server.Timer.BuffTimer;
import tools.packet.TemporaryStatsPacket;

public enum MapleFamilyBuff {

    //※ 나만의 경험치 효과나 드롭률 이벤트와 겹칠 경우 효력이 무시된다.
    Teleport("패밀리원에게 바로 이동", "[대상] 자기 자신\r\n[효과] 원하는 패밀리원이 있는 장소로 바로 이동한다.", 0, 0, 0, 300, 190000),
    Summon("패밀리원 바로 소환", "[대상] 패밀리원 1명\r\n[효과] 원하는 패밀리원을 자신이 있는 맵으로 바로 소환한다.", 1, 0, 0, 500, 190001),
    Drop_15_15("나만의 드롭률 1.5배(15분)", "[대상] 자기 자신\r\n[지속시간] 15분\r\n[효과] 몬스터 사냥 드롭률이 1.5배로 향상.\r\n※ 다른 효과와 겹칠 경우 효력이 무시된다.", 3, 15, 150, 700, 190002),
    EXP_15_15("나만의 경험치 1.5배(15분)", "[대상] 자기 자신\r\n[지속시간] 15분\r\n[효과] 몬스터 사냥 시 얻는 경험치가 1.5배로 향상.\r\n※ 다른 효과와 겹칠 경우 효력이 무시된다.", 2, 15, 150, 800, 190003),
    Bonding("패밀리원의 단결(30분)", "[발동조건] 가계도에 보이는 하위 패밀리원이 6명 이상 로그인\r\n[지속시간] 30분\r\n[효과] 드롭률과 경험치를 2배로 향상. ※ 다른 효과와 겹칠 경우 효력이 무시된다.", 4, 30, 200, 3000, 190006),
    Drop_20_15("나만의 드롭률 2배(15분)", "[대상] 자기 자신\r\n[지속시간] 15분\r\n[효과] 몬스터 사냥 드롭률이 2배로 향상.\r\n※ 다른 효과와 겹칠 경우 효력이 무시된다.", 3, 15, 200, 1200, 190004), // 나만의 경험치 효과와 겹칠 경우 효력이 무시된다
    EXP_20_15("나만의 경험치 2배(15분)", "[대상] 자기 자신\r\n[지속시간] 15분\r\n[효과] 몬스터 사냥 시 얻는 경험치가 2배로 향상.\r\n※ 다른 효과와 겹칠 경우 효력이 무시된다.", 2, 15, 200, 1500, 190005), // 나만의 드롭률과 겹칠 경우 효력이 무시된다
    Drop_20_30("나만의 드롭률 2배(30분)", "[대상] 자기 자신\r\n[지속시간] 30분\r\n[효과] 몬스터 사냥 드롭률이 2배로 향상.\r\n※ 다른 효과와 겹칠 경우 효력이 무시된다.", 3, 30, 200, 2000, 190007),
    EXP_20_30("나만의 경험치 2배(30분)", "[대상] 자기 자신\r\n[지속시간] 30분\r\n[효과] 몬스터 사냥 시 얻는 경험치가 2배로 향상.\r\n※ 다른 효과와 겹칠 경우 효력이 무시된다.", 2, 30, 200, 2500, 190008),
    Drop_Party("파티 드롭률 2배(30분)", "[대상] 파티\r\n[지속시간] 30분\r\n[효과] 몬스터 사냥 드롭률이 2배로 향상.\r\n※ 다른 효과와 겹칠 경우 효력이 무시된다.", 3, 30, 200, 4000, 190009),
    EXP_Party("파티 경험치 2배(30분)", "[대상] 파티\r\n[지속시간] 30분\r\n[효과] 몬스터 사냥 시 얻는 경험치가 2배로 향상.\r\n※ 다른 효과와 겹칠 경우 효력이 무시된다.", 2, 30, 200, 5000, 190010), //EXP_20_30("My Drop Rate 1.5x (15min)", "[Target] Me\n[Time] 15 min.\n[Effect] Monster drop rate will be increased #c1.5x#.\n*  If the event is in progress, this will be nullified.", 2, 15, 150, 1500, 190010),
    //Drop_20_30("My Drop Rate 1.5x (30min)", "[Target] Me\n[Time] 30 min.\n[Effect] Monster drop rate will be increased #c1.5x#.\n*  If the event is in progress, this will be nullified.", 2, 30, 150, 2000, 190011),    
    //Drop_Party_15("My Party Drop Rate 1.5x (30min)", "[Target] Party\n[Time] 30 min.\n[Effect] Monster drop rate will be increased #c1.5x#.\n*  If the event is in progress, this will be nullified.", 2, 30, 150, 7000, 190011);
    // 0=tele, 1=summ, 2=drop, 3=exp, 4=both
    ;
    public String name, desc;
    public int rep, type, questID, duration, effect;
    public EnumMap<MapleBuffStat, Integer> effects;

    private MapleFamilyBuff(String name, String desc, int type, int duration, int effect, int rep, int questID) {
        this.name = name;
        this.desc = desc;
        this.rep = rep;
        this.type = type;
        this.questID = questID;
        this.duration = duration;
        this.effect = effect;
        setEffects();
    }

    public int getEffectId() {
//        switch (type) {
//            case 2: //exp
//                return 2450018;
//            case 3: //drop
//                return 2022694;
//        }
        return 2000000; //custom
    }

    public final void setEffects() {
        //custom
        this.effects = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
        //this.effects = new EnumMap<>(MapleBuffStat.class);
        switch (type) {
            case 2: //exp
                effects.put(MapleBuffStat.EXPRATE, effect);
                effects.put(MapleBuffStat.DROP_RATE, 100);
//                effects.remove(MapleBuffStat.DROP_RATE);
                break;
            case 3: //drop
                effects.put(MapleBuffStat.DROP_RATE, effect);
                effects.put(MapleBuffStat.EXPRATE, 100);
//                effects.remove(MapleBuffStat.EXPRATE);
                //effects.put(MapleBuffStat.MESO_RATE, effect);
                break;
            case 4: //both
                effects.put(MapleBuffStat.EXPRATE, effect);
                effects.put(MapleBuffStat.DROP_RATE, effect);
                //effects.put(MapleBuffStat.MESO_RATE, effect);
                break;
        }
    }

    public void applyTo(MapleCharacter chr) {
        MapleStatEffect eff = MapleItemInformationProvider.getInstance().getItemEffect(getEffectId());
        eff.applyTo(chr);
        final CancelEffectAction cancelAction = new CancelEffectAction(chr, eff, System.currentTimeMillis(), effects);
        final ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(cancelAction, duration * 60000);
        chr.getClient().getSession().write(TemporaryStatsPacket.giveBuff(-getEffectId(), duration * 60000, effects, null));
        chr.cancelEffect(eff, -1, effects, true, false);
        chr.registerEffect(eff, System.currentTimeMillis(), schedule, effects, false, duration * 60000, chr.getId());
    }
}
