/*
 * The MIT License
 *
 * Copyright 2017 Jŭbar.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package server;

import client.inventory.Equip;
import provider.MapleData;
import provider.MapleDataTool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * @author Lunatic
 */
public class SetItemInfo {

    private final String name;
    private final Set<Integer> itemID = new HashSet<>();
    private final Map<Integer, Effect> effectMap = new HashMap<>();

    public SetItemInfo(MapleData setItemInfo) {
        this.name = MapleDataTool.getString("setItemName", setItemInfo);
        MapleData ids = setItemInfo.getChildByPath("ItemID");
        for (MapleData id : ids) {
            itemID.add(MapleDataTool.getInt(id));
        }
        MapleData effs = setItemInfo.getChildByPath("Effect");
        for (MapleData eff : effs) {
            int count = Integer.parseInt(eff.getName());
            Effect effect = new Effect();
            for (MapleData e : eff) {
                if (e.getName().equals("Option")) {
                    continue;
                }
                effect.keyValuePairs.put(e.getName(), MapleDataTool.getInt(e));
            }
            effectMap.put(count, effect);
        }
    }

    public String getName() {
        return name;
    }

    public int totalCount(ToIntFunction<Integer> counter) {
        return itemID.stream().mapToInt(counter).sum();
    }

    public Equip totalEquip(Equip eqp, int count) {
        boolean oneshot = false;
        for (Map.Entry<Integer, Effect> pair : effectMap.entrySet()) {
            int key = pair.getKey();
            if (count >= key) {
                if (!oneshot) {
                    oneshot = true;
                    if (eqp == null) {
                        eqp = new Equip(1112428, (short) -122, (byte) 1);
                        eqp.setOwner(headonly(name));
                    } else {
                        eqp.setOwner(eqp.getOwner() + "&" + headonly(name));
                    }
                }
                Effect value = pair.getValue();
                eqp = value.inc(eqp, count);
            }
        }
        return eqp;
    }

    private String headonly(String x) {
        StringBuilder sb = new StringBuilder();
        String[] split = x.split(" ");
        for (int i = 0; i < split.length - 1; i++) {
            String string = split[i];
            sb.append(string.charAt(0));
        }
        return sb.toString();
    }

    public static class Effect {

        private final Map<String, Integer> keyValuePairs = new HashMap<>();

        public Equip inc(Equip eqp, int count) {
            if (!keyValuePairs.isEmpty()) {
                eqp.setLevel((byte) (eqp.getLevel() + count));
                for (Map.Entry<String, Integer> stat : keyValuePairs.entrySet()) {
                    final String key = stat.getKey();
                    final int value = stat.getValue();
                    switch (key) {
                        case "incSTR":
                            eqp.setStr((short) (eqp.getStr() + value));
                            break;
                        case "incDEX":
                            eqp.setDex((short) (eqp.getDex() + value));
                            break;
                        case "incINT":
                            eqp.setInt((short) (eqp.getInt() + value));
                            break;
                        case "incLUK":
                            eqp.setLuk((short) (eqp.getLuk() + value));
                            break;
                        case "incAllStat":
                            eqp.setStr((short) (eqp.getStr() + value));
                            eqp.setDex((short) (eqp.getDex() + value));
                            eqp.setInt((short) (eqp.getInt() + value));
                            eqp.setLuk((short) (eqp.getLuk() + value));
                            break;
                        case "incPAD":
                            eqp.setWatk((short) (eqp.getWatk() + value));
                            break;
                        case "incPDD":
                            eqp.setWdef((short) (eqp.getWdef() + value));
                            break;
                        case "incMAD":
                            eqp.setMatk((short) (eqp.getMatk() + value));
                            break;
                        case "incMDD":
                            eqp.setMdef((short) (eqp.getMdef() + value));
                            break;
                        case "incACC":
                            eqp.setAcc((short) (eqp.getAcc() + value));
                            break;
                        case "incEVA":
                            eqp.setAvoid((short) (eqp.getAvoid() + value));
                            break;
                        case "incSpeed":
                            eqp.setSpeed((short) (eqp.getSpeed() + value));
                            break;
                        case "incJump":
                            eqp.setJump((short) (eqp.getJump() + value));
                            break;
                        case "incMHP":
                            eqp.setHp((short) (eqp.getHp() + value));
                            break;
                        case "incMMP":
                            eqp.setMp((short) (eqp.getMp() + value));
                            break;
                    }
                }
            }
            return eqp;
        }
    }
}
