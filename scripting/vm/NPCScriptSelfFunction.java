/*
 * Copyright (C) 2013 Nemesis Maple Story Online Server Program

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting.vm;

import client.MapleCharacter;
import handling.world.World;
import scripting.EventInstanceManager;
import scripting.EventManager;
import server.MapleBgmProvider;
import server.Randomizer;
import server.RankingWorker;
import server.RateManager;
import server.life.*;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;

import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import server.MapleItemInformationProvider;

/**
 * @author Eternal
 */
public class NPCScriptSelfFunction {

    private NPCScriptVirtualMachine vm;

    public NPCScriptSelfFunction(NPCScriptVirtualMachine vms) {
        this.vm = vms;
    }

    public boolean isStop() {
        return vm.isStop();
    }

    public void say(String str) {
        if (vm.isStop()) {
            return;
        }
        vm.addSay(str);
    }

    public int askYesNo(String str) {
        if (vm.isStop()) {
            return -1;
        }
        return vm.askYesNo(str);
    }

    public int askAccept(String str) {
        if (vm.isStop()) {
            return -1;
        }
        return vm.askAccept(str);
    }

    public int askAcceptNoESC(String str) {
        if (vm.isStop()) {
            return -1;
        }
        return vm.askAcceptNoESC(str);
    }

    public int askMapSelection(String str) {
        if (vm.isStop()) {
            return -1;
        }
        return vm.askMapSelection(str);
    }

    public int askMenu(String str) {
        if (vm.isStop()) {
            return -1;
        }
        return vm.askMenu(str);
    }

    public int askMenuAuto(String str, String... menus) {
        if (vm.isStop()) {
            return -1;
        }
        StringBuilder sb = new StringBuilder(str);
        int idx = 0;
        for (String menu : menus) {
            sb.append("\r\n#b#L").append(idx++).append("#").append(menu).append("#l");
        }
        return vm.askMenu(sb.toString());
    }

    public String askText(String str) {
        if (vm.isStop()) {
            return "";
        }
        return vm.askText(str);
    }

    public int askNumber(String str, int def, int min, int max) {
        if (vm.isStop()) {
            return def;
        }
        return vm.askNumber(str, def, min, max);
    }

    public int askAvatar(String str, int... style) {
        int a = vm.askAvatar(str, style);
        if (a < 0) {
            return a;
        }
        return a;
    }

    public final void spawnMonster(final int mobid, int x, int y) {
        MapleMonster mob = MapleLifeFactory.getMonster(mobid);
        vm.getClient().getPlayer().getMap().spawnMonster_sSack(mob, new Point(x, y), -2);
    }

    public void flushSay() {
        vm.flushSay();
    }

    public final EventManager getEventManager(final String event) {
        return vm.getClient().getChannelServer().getEventSM().getEventManager(event);
    }

    public final EventInstanceManager getEventInstance() {
        return vm.getClient().getPlayer().getEventInstance();
    }

    public MapleCharacter askCharacter(String ask, int range) {
        String name = askText(ask);
        if (name.isEmpty()) {
            return null;
        }
        switch (range) {
            case 0:
                return vm.getClient().getPlayer().getMap().getCharacterByName(name);
            case 1:
                return vm.getClient().getChannelServer().getPlayerStorage().getCharacterByName(name);
            case 2:
                return World.getCharacterByName(name);
        }
        return null;
    }

    //self.askBGM("카테고리를 선택해주세요.", "BGM을 선택해주세요.", 1 );
    public boolean askBGM(String categoryPreMenu, String bgmPreMenu, int flag) {
        boolean adminMenu = (flag & 1) != 0;
        boolean specialOnly = (flag & 2) != 0;
        if (vm.isStop()) {
            return false;
        }
        final List<String> categoryList = specialOnly ? MapleBgmProvider.BGM_SPECIAL_CATEGORY_LIST : MapleBgmProvider.BGM_CATEGORY_LIST;
        final Map<String, List<MapleBgmProvider.BgmInfo>> listMap = MapleBgmProvider.BGM_LIST_MAP;
        final int categorySize = categoryList.size();
        final int categoryMenuMax = 9;
        int categoryPage = 0;
        final int categoryPageMax = (categorySize + (categoryMenuMax - 1)) / categoryMenuMax;
        while (!vm.isStop()) {
            String categoryMenu = categoryPreMenu + "#b";
            for (int i = categoryPage * categoryMenuMax; i < Math.min((categoryPage + 1) * categoryMenuMax, categorySize); i++) {
                String category = categoryList.get(i)
                        .replace(".img", "");
                categoryMenu += "\r\n#L" + i + "# " + category + "#l";
            }
            categoryMenu += "\r\n#r#L" + (categorySize + 0) + "# 이전#l  #L" + (categorySize + 1) + "# 다음#l";
            int categoryIdx = vm.askMenu(categoryMenu);
            if (categoryIdx < categorySize) {
                try {
                    String category = categoryList.get(categoryIdx);
                    final List<MapleBgmProvider.BgmInfo> bgmList = listMap.get(category);
                    final int bgmSize = bgmList.size();
                    final int bgmMenuMax = 7;
                    int bgmPage = 0;
                    final int bgmPageMax = (bgmSize + (bgmMenuMax - 1)) / bgmMenuMax;
                    BGM_PAGING:
                    while (!vm.isStop()) {
                        String bgmMenu = bgmPreMenu;
                        bgmMenu += "\r\n선택된 카테고리 = #b#e" + category + "#n#k\r\n#r#L" + (bgmSize + 2) + "# 뒤로 가기#l#b\r\n";
                        for (int i = bgmPage * bgmMenuMax; i < Math.min((bgmPage + 1) * bgmMenuMax, bgmSize); i++) {
                            MapleBgmProvider.BgmInfo bgm = bgmList.get(i);
                            bgmMenu += "\r\n#L" + i + "# " + bgm.getName() + "#l";
                        }
                        bgmMenu += "\r\n#r#L" + (bgmSize + 0) + "# 이전#l  #L" + (bgmSize + 1) + "# 다음#l";
                        int bgmIdx = vm.askMenu(bgmMenu);
                        if (bgmIdx < bgmSize) {
                            MapleBgmProvider.BgmInfo bgm = bgmList.get(bgmIdx);
                            int menu;
                            if (adminMenu) {
                                menu = askMenu("선택된 BGM = #b#e" + bgm.getName() + "#n#k\r\n#r#L0# 뒤로 가기#l#b\r\n\r\n#L1# 미리 듣기#l\r\n#L2# 자신에게만 재생#l\r\n#L3# 맵 전체에 재생#l\r\n#L101# 맵에 적용#l\r\n#L102# 월드 전체에 재생#l");
                            } else {
                                menu = askMenu("선택된 BGM = #b#e" + bgm.getName() + "#n#k\r\n#r#L0# 뒤로 가기#l#b\r\n\r\n#L1# 미리 듣기#l\r\n#L2# 자신에게만 재생#l\r\n#L3# 맵 전체에 재생#l");
                            }
                            if (menu > 0) {
                                MapleCharacter user = vm.getClient().getPlayer();
                                String path = bgm.getFullPath();
                                switch (menu) {
                                    case 1:
                                        user.getClient().sendPacket(MaplePacketCreator.musicChange(path));
                                        continue;
                                    case 2:
                                        user.getClient().sendPacket(MaplePacketCreator.musicChange(path));
                                        break;
                                    case 3:
                                        user.getMap().broadcastMessage(MaplePacketCreator.musicChange(path));
                                        break;
                                }
                                if (adminMenu && menu > 100) {
                                    switch (menu) {
                                        case 101:
                                            user.getMap().changeMusic(path);
                                            user.getMap().broadcastMessage(MaplePacketCreator.musicChange(path));
                                            break;
                                        case 102:
                                            World.Broadcast.broadcastMessage(MaplePacketCreator.musicChange(path));
                                            break;
                                    }
                                }
                                user.dropMessage(5, "BGM [" + bgm.getName() + "]을(를) 재생합니다.");
                                return true;
                            }
                        } else {
                            int menu = bgmIdx - bgmSize;
                            switch (menu) {
                                case 0:
                                    if (bgmPage > 0) {
                                        --bgmPage;
                                    }
                                    continue;
                                case 1:
                                    if (bgmPage + 1 < bgmPageMax) {
                                        ++bgmPage;
                                    }
                                    continue;
                                case 2:
                                    break BGM_PAGING;
                                default:
                                    return false;
                            }
                        }
                    }
                } catch (IndexOutOfBoundsException ex) {
                    say("IOOBE");
                    return false;
                }
            } else {
                int menu = categoryIdx - categorySize;
                switch (menu) {
                    case 0:
                        if (categoryPage > 0) {
                            --categoryPage;
                        }
                        continue;
                    case 1:
                        if (categoryPage + 1 < categoryPageMax) {
                            ++categoryPage;
                        }
                        continue;
                    default:
                        return false;
                }
            }
        }
        return false;
    }

    public void askRanking(String strBase) {
        StringBuilder sb = new StringBuilder(strBase);
        boolean empty = true;
        for (Map.Entry<String, Integer> entry : RankingWorker.getJobCommands().entrySet()) {
            int cmd = entry.getValue();
            List<RankingWorker.RankingInformation> list = RankingWorker.getRankingInfo(cmd);
            if (!list.isEmpty()) {
                sb.append("\r\n#L").append(cmd == -1 ? 999 : cmd).append("# ").append(entry.getKey()).append("#l");
                empty = false;
            }
        }
        if (empty) {
            say("표시할 랭킹이 없습니다.");
            return;
        }
        int cmd = askMenu(sb.toString());
        if (cmd != -1) {
            if (cmd == 999) {
                cmd = -1;
            }
            List<RankingWorker.RankingInformation> list = RankingWorker.getRankingInfo(cmd);
            int list_size = list.size();
            final int pageMax = 10; //한 페이지에 몇 개?
            int pc;
            int page = 1;
            do {
                sb = new StringBuilder();
                int start = (page - 1) * pageMax;
                for (int i = start; i < Math.min(list_size, start + pageMax); i++) {
                    sb.append(list.get(i)).append("\r\n");
                }
                sb.append("#L0#이전#l #L999#").append(page).append(" 페이지#l #L1#다음#l");
                switch (pc = askMenu(sb.toString())) {
                    case 0:
                        if (page > 1) {
                            --page;
                        }
                        break;
                    case 1:
                        if (page <= list_size / pageMax) {
                            ++page;
                        }
                        break;
                    case 999:
                        askRanking(strBase);
                        pc = -1;
                        break;
                }
            } while (pc != -1);
        }
    }

    public int askMob(String str) {
        if (vm.isStop()) {
            return -1;
        }
        String query = askText(str).replace(" ", "").toLowerCase();
        if (query.isEmpty()) {
            return -1;
        }
        Map<Integer, String> searchMap = new HashMap<>();
        for (Map.Entry<Integer, String> entry : MapleMonsterStats.MobNameMap.entrySet()) {
            int key = entry.getKey();
            String value = entry.getValue();
            switch (key) { //검색 대상 제외몹
                case 9999998:
                case 9999999:
                    continue;
            }
            String r = value.replace(" ", "").toLowerCase();
            if (r.contains(query)) {
                searchMap.put(key, value);
            }
        }
        if (searchMap.isEmpty()) {
            say("검색 결과가 없습니다.");
            return -1;
        }
        if (searchMap.size() == 1) {
            return searchMap.keySet().iterator().next();
        } else {
            StringBuilder sb = new StringBuilder("검색 결과가 여러 개입니다.#b");
            for (Map.Entry<Integer, String> entry : searchMap.entrySet()) {
                int key = entry.getKey();
                String value = entry.getValue();
                sb.append("\r\n").append("#L").append(key).append("#").append(value).append(" [").append(key).append("]").append("#l");
            }
            return askMenu(sb.toString());
        }
    }

    public String getDropInfo(int mobId) {
        final List<MonsterDropEntry> ranks = MapleMonsterInformationProvider.getInstance().retrieveDrop(mobId);
        if (ranks != null && !ranks.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int n = 1;
            Collections.sort(ranks, new Comparator<MonsterDropEntry>() {
                @Override
                public final int compare(final MonsterDropEntry o1, final MonsterDropEntry o2) {
                    final int thisVal = o1.itemId;
                    final int anotherVal = o2.itemId;
                    return (thisVal > anotherVal ? 1 : (thisVal == anotherVal ? 0 : -1));
                }
            });
            for (MonsterDropEntry de : ranks) {
                if (de.chance > 0 && (de.questid <= 0 || (de.questid > 0 && !MapleQuest.getInstance(de.questid).getName().isEmpty()))) {
                    int itemId = de.itemId;
                    if (n == 1) {
                        MapleMonster mob = null;
                        mob = MapleLifeFactory.getMonster(mobId);
                        if (mob == null) {
                            return "존재하지 않는 몬스터입니다.";
                        }
                        switch (mobId) {
                            case 9400409: //두꺼비 영주
                            case 8810122:
                            case 8300006: //드래고니카
                                sb.append("(#o" + mobId + "#은(는) 이미지가 너무 커 생략합니다.)\r\n\r\n");
                                break;
                            case 8810018: //혼테일
                            case 9420522: //크렉셀
                            case 8830000: //마왕 발록
                            case 9400296: //코어 블레이즈
                                sb.append("#fMob/" + mobId + ".img/info/default/0#\r\n");
                                break;
                            case 8510000:
                            case 8520000:
                            case 9300294:
                                sb.append("#fMob/8510000.img/info/default/0#\r\n"); //피아누스
                                break;
                            case 7220003:
                            case 9400265:
                                sb.append("#fMob/7220003.img/info/default/0#\r\n"); //베르가모트
                                break;
                            case 8220015:
                            case 9400273:
                                sb.append("#fMob/8220015.img/info/default/0#\r\n"); //니베룽
                                break;
                            case 8800002:
                            case 8800102:
                                sb.append("#fMob/8800002.img/info/default/0#\r\n"); //자쿰·카오스 자쿰
                                break;
                            default:
                                sb.append("   ");
                                if (mob.getStats().getFly()) {
                                    if (mob.getStats().getLink() > 0) {
                                        if (mob.getStats().getLink() < 1000000) {
                                            sb.append("#fMob/0" + mob.getStats().getLink() + ".img/fly/0# (이미지 몬스터 코드 : " + mob.getStats().getLink() + ")\r\n");
                                        } else {
                                            sb.append("#fMob/" + mob.getStats().getLink() + ".img/fly/0# (이미지 몬스터 코드 : " + mob.getStats().getLink() + ")\r\n");
                                        }
                                    } else if (mobId < 1000000) {
                                        sb.append("#fMob/0" + mobId + ".img/fly/0#\r\n");
                                    } else {
                                        sb.append("#fMob/" + mobId + ".img/fly/0#\r\n");
                                    }
                                } else if (mob.getStats().getLink() > 0) {
                                    if (mob.getStats().getLink() < 1000000) {
                                        sb.append("#fMob/0" + mob.getStats().getLink() + ".img/stand/0# (이미지 몬스터 코드 : " + mob.getStats().getLink() + ")\r\n");
                                    } else {
                                        sb.append("#fMob/" + mob.getStats().getLink() + ".img/stand/0# (이미지 몬스터 코드 : " + mob.getStats().getLink() + ")\r\n");
                                    }
                                } else if (mobId < 1000000) {
                                    sb.append("#fMob/0" + mobId + ".img/stand/0#\r\n");
                                } else {
                                    sb.append("#fMob/" + mobId + ".img/stand/0#\r\n");
                                }
                                break;
                        }
                        sb.append("   #b" + mob.getStats().getName() + "#k (Lv. " + mob.getStats().getLevel() + ") (몬스터 코드 : " + mob.getId() + ")\r\n\r\n");
                        sb.append("   체력 : " + getBanJum(mob.getStats().getHp()) + " / 마나 : " + getBanJum((long) mob.getStats().getMp()) + "\r\n");
                        sb.append("   물리 : " + mob.getStats().getPhysicalAttack() + " / 마법 : " + mob.getStats().getMagicAttack() + " / 물방 : " + mob.getStats().getPDDamage() + " / 마방 : " + mob.getStats().getMDDamage() + "\r\n");
                        sb.append("   명중 : " + mob.getStats().getAcc() + " / 회피 : " + mob.getStats().getEva() + " / ");
                        sb.append("경험치 : " + getBanJum((long) (mob.getStats().getExp())) + "\r\n\r\n");

                        sb.append("#o").append(mobId).append("#의 드롭 정보입니다.\r\n");
                        sb.append("--------------------------------------\r\n");
                    }
                    String namez = "#t" + itemId + "#";
                    if (itemId == 0) { //meso
                        itemId = 4031041; //display sack of cash
                        namez = (de.Minimum * RateManager.DISPLAY_MESO) + "~" + (de.Maximum * RateManager.DISPLAY_MESO) + " 메소";
                    } else {
                        if (de.Maximum != 1) {
                            namez = " " + de.Minimum + "~" + de.Maximum + "개";
                        } else {
                            namez = "";
                        }
                    }
                    int chance = de.chance * RateManager.getTrueDropRate();

                    if (!MapleItemInformationProvider.getInstance().itemExists(itemId)) {
                        sb.append("wz에 존재하지 않는 아이템 : " + itemId + "\r\n");
                    } else {
                        if (itemId == 4031041) {
                            sb.append(n).append(") #v").append(itemId).append("#").append(namez).append(" - " + "드롭률 ").append(Integer.valueOf(chance >= 999999 ? 1000000 : chance).doubleValue() / 10000.0).append("% ").append(de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? "(" + (MapleQuest.getInstance(de.questid).getName() + " 퀘스트 진행 중에만 드롭)") : "").append("\r\n");
                        } else {
                            sb.append(n).append(") #i").append(itemId).append("#").append("#z").append(itemId).append("#").append(namez).append(" - " + "드롭률 ").append(Integer.valueOf(chance >= 999999 ? 1000000 : chance).doubleValue() / 10000.0).append("% ").append(de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? "(" + (MapleQuest.getInstance(de.questid).getName() + " 퀘스트 진행 중에만 드롭)") : "").append("\r\n");
                        }
                    }
                    n++;
                }
            }
            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        return "드롭하는 아이템이 없습니다.";
    }

    public String getBanJum(Long S) {
//        StringBuilder SS = new StringBuilder().append("" + S);
//        StringBuilder retValue = new StringBuilder();
//        for (int i = 0; i < SS.length(); i++) {
//            if (i > 0 && (i % 3) == 0) {
//                retValue.append(SS.charAt(SS.length() - i -1) + "," + retValue.toString());
//            } else {
//                retValue.append(SS.charAt(SS.length() - i -1) + retValue.toString());
//            }
//        }
//        return retValue.toString();
        StringBuilder SS = new StringBuilder("" + S);
        if (S < 0) {
            return SS.toString();
        }
        SS.reverse();
        int SSS = 0;
        for (int i = 0; i < SS.length(); i++) {
            SSS++;
            if (SSS == 4) {
                SS.insert(i, ",");
                SSS = 0;
            }
        }
        SS.reverse();
        return SS.toString();
    }

    public int random(int min, int max) {
        return Randomizer.rand(min, max);
    }
}
