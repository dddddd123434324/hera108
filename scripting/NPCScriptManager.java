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
package scripting;

import client.MapleClient;
import server.quest.MapleQuest;
import tools.FileoutputUtil;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;

import client.Skill;
import client.SkillFactory;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.MapleStatEffect;
import tools.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;



public class NPCScriptManager extends AbstractScriptManager {

    private final Map<MapleClient, NPCConversationManager> cms = new WeakHashMap<MapleClient, NPCConversationManager>();
    private static final NPCScriptManager instance = new NPCScriptManager();

    public static final NPCScriptManager getInstance() {
        return instance;
    }

    public final void start(final MapleClient c, int npc) {
        start(c, npc, null);
    }

    public final void start(final MapleClient c, int npc, String customjs) {
        start(c, npc, customjs, -1);
    }

    public final void start(final MapleClient c, final int npc, String customjs, int oid) {
        try {
            final Lock lock = c.getNPCLock();
            lock.lock();
            try {
                if (!cms.containsKey(c) && c.canClickNPC()) {
                    Invocable iv = null;
                    if (npc >= 9901000 && npc <= 9901409) {
                        iv = getInvocable("npc/level200.js", c, true);
                    } else {
                        iv = getInvocable("npc/" + (customjs == null ? npc : customjs) + ".js", c, true);
//                        System.out.println("NPCID started4: " + npc);
                    }
                    if (iv == null) {
                        iv = getInvocable("npc/notcoded.js", c, true); //safe disposal
                        if (iv == null) {
                            dispose(c);
                            return;
                        }
                    }
                    final ScriptEngine scriptengine = (ScriptEngine) iv;
                    final NPCConversationManager cm = new NPCConversationManager(c, npc, -1, (byte) -1, iv);
                    if (oid > 0) {
                        cm.setObjectId(oid);
                    }
                    cms.put(c, cm);
                    scriptengine.put("cm", cm);

                    c.getPlayer().setConversation(1);
                    c.setClickedNPC();
                    try {
                        iv.invokeFunction("start"); // Temporary until I've removed all of start
                    } catch (NoSuchMethodException nsme) {
                        iv.invokeFunction("action", (byte) 1, (byte) 0, 0);
                    }
                } else {
                    //c.getPlayer().dropMessage(-1, "You already are talking to an NPC. Use @ea if this is not intended.");
                }

            } catch (final Exception e) {
                String msg = "Error executing NPC script, NPC ID : " + npc + ".";
                if (customjs != null) {
                    msg += " CustomJS : " + customjs + ".";
                }
                System.err.println(msg + e);
                FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, msg + e);
                dispose(c);
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final void action(final MapleClient c, final byte mode, final byte type, final int selection) {
        if (mode != -1) {
            final NPCConversationManager cm = cms.get(c);
            if (cm == null || cm.getLastMsg() > -1) {
                return;
            }
            final Lock lock = c.getNPCLock();
            lock.lock();
            try {
                if (cm.pendingDisposal) {
                    dispose(c);
                } else {
                    c.setClickedNPC();
                    cm.getIv().invokeFunction("action", mode, type, selection);
                }
            } catch (final Exception e) {
                System.err.println("Error executing NPC script. NPC ID : " + cm.getNpc() + ":" + e);
                dispose(c);
                FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Error executing NPC script, NPC ID : " + cm.getNpc() + "." + e);
            } finally {
                lock.unlock();
            }
        }
    }

    public final void startQuest(final MapleClient c, final int npc, final int quest) {
        if (!MapleQuest.getInstance(quest).canStart(c.getPlayer(), null)) {
            return;
        }
        final Lock lock = c.getNPCLock();
        lock.lock();
        try {
            if (!cms.containsKey(c) && c.canClickNPC()) {
                final Invocable iv = getInvocable("quest/" + quest + ".js", c, true);
                if (iv == null) {
                    dispose(c);
                    return;
                }
                final ScriptEngine scriptengine = (ScriptEngine) iv;
                final NPCConversationManager cm = new NPCConversationManager(c, npc, quest, (byte) 0, iv);
                cms.put(c, cm);
                scriptengine.put("qm", cm);

                c.getPlayer().setConversation(1);
                c.setClickedNPC();
                //System.out.println("NPCID started: " + npc + " startquest " + quest);
                iv.invokeFunction("start", (byte) 1, (byte) 0, 0); // start it off as something
            } else {
                //c.getPlayer().dropMessage(-1, "You already are talking to an NPC. Use @ea if this is not intended.");
            }
        } catch (final Exception e) {
            System.err.println("Error executing Quest script. (" + quest + ")..NPCID: " + npc + ":" + e);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Error executing Quest script. (" + quest + ")..NPCID: " + npc + ":" + e);
            dispose(c);
        } finally {
            lock.unlock();
        }
    }

    public final void startQuest(final MapleClient c, final byte mode, final byte type, final int selection) {
        final Lock lock = c.getNPCLock();
        final NPCConversationManager cm = cms.get(c);
        if (cm == null || cm.getLastMsg() > -1) {
            return;
        }
        lock.lock();
        try {
            if (cm.pendingDisposal) {
                dispose(c);
            } else {
                c.setClickedNPC();
                cm.getIv().invokeFunction("start", mode, type, selection);
            }
        } catch (Exception e) {
            System.err.println("Error executing Quest script. (" + cm.getQuest() + ")...NPC: " + cm.getNpc() + ":" + e);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Error executing Quest script. (" + cm.getQuest() + ")..NPCID: " + cm.getNpc() + ":" + e);
            dispose(c);
        } finally {
            lock.unlock();
        }
    }

    public final void endQuest(final MapleClient c, final int npc, final int quest, final boolean customEnd) {
        if (!customEnd && !MapleQuest.getInstance(quest).canComplete(c.getPlayer(), null)) {
            return;
        }
        final Lock lock = c.getNPCLock();
        lock.lock();
        try {
            if (!cms.containsKey(c) && c.canClickNPC()) {
                final Invocable iv = getInvocable("quest/" + quest + ".js", c, true);
                if (iv == null) {
                    dispose(c);
                    return;
                }
                final ScriptEngine scriptengine = (ScriptEngine) iv;
                final NPCConversationManager cm = new NPCConversationManager(c, npc, quest, (byte) 1, iv);
                cms.put(c, cm);
                scriptengine.put("qm", cm);

                c.getPlayer().setConversation(1);
                c.setClickedNPC();
                //System.out.println("NPCID started: " + npc + " endquest " + quest);
                iv.invokeFunction("end", (byte) 1, (byte) 0, 0); // start it off as something
            } else {
                //c.getPlayer().dropMessage(-1, "You already are talking to an NPC. Use @ea if this is not intended.");
            }
        } catch (Exception e) {
            System.err.println("Error executing Quest script. (" + quest + ")..NPCID: " + npc + ":" + e);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Error executing Quest script. (" + quest + ")..NPCID: " + npc + ":" + e);
            dispose(c);
        } finally {
            lock.unlock();
        }
    }

    public final void endQuest(final MapleClient c, final byte mode, final byte type, final int selection) {
        final Lock lock = c.getNPCLock();
        final NPCConversationManager cm = cms.get(c);
        if (cm == null || cm.getLastMsg() > -1) {
            return;
        }
        lock.lock();
        try {
            if (cm.pendingDisposal) {
                dispose(c);
            } else {
                c.setClickedNPC();
                cm.getIv().invokeFunction("end", mode, type, selection);
            }
        } catch (Exception e) {
            System.err.println("Error executing Quest script. (" + cm.getQuest() + ")...NPC: " + cm.getNpc() + ":" + e);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, "Error executing Quest script. (" + cm.getQuest() + ")..NPCID: " + cm.getNpc() + ":" + e);
            dispose(c);
        } finally {
            lock.unlock();
        }
    }

    public final void dispose(final MapleClient c) {
        final NPCConversationManager npccm = cms.get(c);
        if (npccm != null) {
            cms.remove(c);
            if (npccm.getType() == -1) {
                c.removeScriptEngine("scripts/npc/" + npccm.getNpc() + ".js");
                c.removeScriptEngine("scripts/npc/notcoded.js");
                c.removeScriptEngine("scripts/npc/OpenCS.js");
                c.removeScriptEngine("scripts/npc/level200.js");
            } else {
                c.removeScriptEngine("scripts/quest/" + npccm.getQuest() + ".js");
            }
        }
        if (c.getPlayer() != null && c.getPlayer().getConversation() == 1) {
            c.getPlayer().setConversation(0);
        }
    }

    public final NPCConversationManager getCM(final MapleClient c) {
        return cms.get(c);
    }
    
private static volatile MapleData SKILL_STRING_DATA = null;

private static MapleData getSkillStringData() {
    MapleData data = SKILL_STRING_DATA;
    if (data == null) {
        synchronized (NPCScriptManager.class) {
            data = SKILL_STRING_DATA;
            if (data == null) {
                String wzPath = System.getProperty("net.sf.odinms.wzpath", "wz");
                MapleDataProvider prov = MapleDataProviderFactory.getDataProvider(
                        new File(wzPath + "/String.wz"));
                SKILL_STRING_DATA = data = prov.getData("Skill.img");
            }
        }
    }
    return data;
}

private static String padSkillId7(int id) {
    String s = Integer.toString(id);
    return StringUtil.getLeftPaddedStr(s, '0', 7);
}

private static String getSkillStringField(MapleData skillNode, String field) {
    if (skillNode == null) return "";
    MapleData child = skillNode.getChildByPath(field);
    if (child == null) return "";
    return MapleDataTool.getString(child, "");
}

public static final class SkillSearchHit {
    private final int id;
    private final String name;
    private final String desc;

    public SkillSearchHit(int id, String name, String desc) {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDesc() { return desc; }
}

public static List<SkillSearchHit> searchSkills(String keyword, int limit) {
    List<SkillSearchHit> out = new ArrayList<>();
    if (keyword == null) return out;

    String kw = keyword.trim();
    if (kw.isEmpty()) return out;
    String kwLower = kw.toLowerCase();

    // 숫자 입력이면 우선 ID 직행 매칭
    Integer directId = null;
    try {
        directId = Integer.parseInt(kw);
    } catch (NumberFormatException ignore) {
    }

    MapleData root = getSkillStringData();

    // directId가 있으면 먼저 넣고, 이후 일반 검색
    if (directId != null) {
        SkillSearchHit hit = getSkillHitById(root, directId);
        if (hit != null) {
            out.add(hit);
            if (out.size() >= limit) return out;
        }
    }

    for (MapleData child : root.getChildren()) {
        if (out.size() >= limit) break;

        int id;
        try {
            id = Integer.parseInt(child.getName());
        } catch (NumberFormatException nfe) {
            continue;
        }

        String name = getSkillStringField(child, "name");
        String desc = getSkillStringField(child, "desc");
        if (desc.isEmpty()) {
            // 데이터에 따라 "h"(help)만 있는 경우가 있어 보완
            desc = getSkillStringField(child, "h");
        }

        String nameLower = name.toLowerCase();
        String descLower = desc.toLowerCase();

        if (nameLower.contains(kwLower) || descLower.contains(kwLower)) {
            out.add(new SkillSearchHit(id, name, desc));
        }
    }
    return out;
}

private static SkillSearchHit getSkillHitById(MapleData root, int skillId) {
    String key = padSkillId7(skillId);
    MapleData node = root.getChildByPath(key);
    if (node == null) return null;
    String name = getSkillStringField(node, "name");
    String desc = getSkillStringField(node, "desc");
    if (desc.isEmpty()) desc = getSkillStringField(node, "h");
    return new SkillSearchHit(skillId, name, desc);
}

/**
 * 스킬 설명 + 주요 수치(데미지/공격횟수/타겟수/지속/쿨 등)를 보기좋게 문자열로 반환
 */
public static String getSkillInfoText(int skillId, int level) {
    MapleData root = getSkillStringData();
    SkillSearchHit hit = getSkillHitById(root, skillId);

    Skill s = SkillFactory.getSkill(skillId);
    if (s == null) {
        return "스킬을 찾을 수 없습니다. (ID: " + skillId + ")";
    }

    int max = s.getMaxLevel();
    int lv = Math.max(1, Math.min(level, max));

    MapleStatEffect eff = s.getEffect(lv);
    String name = (hit != null && !hit.getName().isEmpty()) ? hit.getName() : SkillFactory.getSkillName(skillId);
    String desc = (hit != null) ? hit.getDesc() : "";

    StringBuilder sb = new StringBuilder();
    sb.append("스킬 ID: ").append(skillId).append("\r\n");
    sb.append("이름: ").append(name == null ? "" : name).append("\r\n");
    sb.append("레벨: ").append(lv).append(" / ").append(max).append("\r\n");

    if (desc != null && !desc.isEmpty()) {
        sb.append("\r\n[설명]\r\n").append(desc).append("\r\n");
    }

    if (eff == null) {
        sb.append("\r\n(이 레벨의 스킬 효과 데이터를 찾을 수 없습니다.)");
        return sb.toString();
    }

    // MapleStatEffect에 public getter 있는 것들 위주로
    sb.append("\r\n[주요 수치]\r\n");
    sb.append("데미지(%): ").append(eff.getDamage()).append("\r\n");
    sb.append("공격 횟수: ").append(eff.getAttackCount()).append("\r\n");
    sb.append("타겟 수: ").append(eff.getMobCount()).append("\r\n");
    sb.append("지속시간(ms): ").append(eff.getDuration()).append("\r\n");
    sb.append("쿨타임(초): ").append(eff.getCooldown()).append("\r\n");
    sb.append("X/Y/Z: ").append(eff.getX()).append(" / ").append(eff.getY()).append(" / ").append(eff.getZ()).append("\r\n");

    return sb.toString();
}
}