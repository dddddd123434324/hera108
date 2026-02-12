/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
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
package server;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MapleAchievementList {

    private Map<Integer, MapleAchievement> achievements = new LinkedHashMap<>();
    private static MapleAchievementList instance = new MapleAchievementList();

    protected MapleAchievementList() {
        //achievements.put(1, new MapleAchievement("got their first point", 1000, false));
        achievements.put(2, new MapleAchievement("레벨 30 달성!", 3000, false));//ㅇ
        achievements.put(3, new MapleAchievement("레벨 70 달성!", 7000, false));//ㅇ
        achievements.put(4, new MapleAchievement("레벨 120 달성!", 12000, false));//ㅇ
        achievements.put(5, new MapleAchievement("레벨 200 달성!", 20000, false));//ㅇ
        achievements.put(7, new MapleAchievement("인기도 10 달성!", 1000, false));//ㅇ
        achievements.put(8, new MapleAchievement("인기도 20 달성!", 5000, false));//ㅇ
        achievements.put(9, new MapleAchievement("리버스 아이템 착용하기", 4000, false));
        achievements.put(10, new MapleAchievement("타임리스 아이템 착용하기", 5000, false));
        achievements.put(11, new MapleAchievement("서버 만세 외치기", 1000, false));//ㅇ
        achievements.put(12, new MapleAchievement("아네고 처치하기", 3500, false));
        achievements.put(13, new MapleAchievement("파풀라투스 처치하기", 2500, false));//ㅇ
        achievements.put(14, new MapleAchievement("피아누스 처치하기", 2500, false));
        achievements.put(15, new MapleAchievement("자쿰 처치하기", 10000, false));//ㅇ
        achievements.put(16, new MapleAchievement("혼테일 처치하기", 30000, false));
        achievements.put(17, new MapleAchievement("핑크빈 처치하기", 30000, false));
        achievements.put(18, new MapleAchievement("보스 몬스터 처치하기", 1000, false));//ㅇ
        achievements.put(19, new MapleAchievement("OX퀴즈 우승하기'", 5000, false));
        achievements.put(20, new MapleAchievement("고지를향해서 우승하기", 5000, false));
        achievements.put(21, new MapleAchievement("올라올라 우승하기", 5000, false));
        achievements.put(22, new MapleAchievement("헬모드 보스 클리어하기", 50000));
        achievements.put(23, new MapleAchievement("카오스 자쿰 처치하기", 10000, false));
        achievements.put(24, new MapleAchievement("카오스 혼테일 처치하기", 20000, false));
        //achievements.put(25, new MapleAchievement("won the event 'Survival Challenge'", 5000, false));
        achievements.put(26, new MapleAchievement("데미지 1만 달성", 2000, false));
        achievements.put(27, new MapleAchievement("데미지 5만 달성", 3000, false));
        achievements.put(28, new MapleAchievement("데미지 10만 달성", 4000, false));
        achievements.put(29, new MapleAchievement("데미지 50만 달성", 5000, false));
        achievements.put(30, new MapleAchievement("데미지 99만 달성", 10000, false));
        achievements.put(31, new MapleAchievement("100만 메소 넘기", 1000, false));
        achievements.put(32, new MapleAchievement("1000만 메소 넘기", 2000, false));
        achievements.put(33, new MapleAchievement("1억 메소 넘기", 3000, false));
        achievements.put(34, new MapleAchievement("10억 메소 넘기", 4000, false));
        achievements.put(35, new MapleAchievement("길드 만들기", 2500, false));//ㅇ
        achievements.put(36, new MapleAchievement("패밀리 만들기", 2500, false));
        achievements.put(37, new MapleAchievement("크림슨우드 파티 퀘스트 클리어", 4000, false));
        achievements.put(38, new MapleAchievement("반 레온 처치하기", 25000, false));
        achievements.put(39, new MapleAchievement("시그너스 처치하기", 100000, false));
        achievements.put(40, new MapleAchievement("130제 아이템 착용하기", 10000, false));
        achievements.put(41, new MapleAchievement("140제 아이템 착용하기", 15000, false));
    }

    public static MapleAchievementList getInstance() {
        return instance;
    }

    public MapleAchievement getById(int id) {
        return achievements.get(id);
    }

    public Integer getByMapleAchievement(MapleAchievement ma) {
        for (Entry<Integer, MapleAchievement> achievement : this.achievements.entrySet()) {
            if (achievement.getValue() == ma) {
                return achievement.getKey();
            }
        }
        return null;
    }
}
