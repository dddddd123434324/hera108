/*
 * GMCommand.java (Skill dump with proper token expansion)
 */
package client.messages.commands;

import client.MapleClient;
import client.Skill;
import client.SkillFactory;
import constants.ServerConstants.PlayerGMRank;
import provider.MapleData;
import provider.MapleDataDirectoryEntry;
import provider.MapleDataEntry;
import provider.MapleDataFileEntry;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.MapleStatEffect;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import tools.FileoutputUtil;
import tools.StringUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GMCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.GM;
    }

    private static int parseIntSafe(final String text, final int def) {
        try {
            return Integer.parseInt(text);
        } catch (Throwable ignore) {
            return def;
        }
    }

    private static final int LEVEL_GROUP_MIN = 1;
    private static final int LEVEL_GROUP_MAX = 250;
    private static final int LEVEL_GROUP_STEP = 10;

    private static final class MapAverageLevelResult {

        private final double averageLevel;
        private final int roundedAverageLevel;
        private final int sampledMobCount;

        private MapAverageLevelResult(final double averageLevel, final int roundedAverageLevel, final int sampledMobCount) {
            this.averageLevel = averageLevel;
            this.roundedAverageLevel = roundedAverageLevel;
            this.sampledMobCount = sampledMobCount;
        }
    }

    private static final class MobMapLevelEntry {

        private final int mapId;
        private final String mapName;
        private final double averageLevel;
        private final int roundedAverageLevel;
        private final int sampledMobCount;

        private MobMapLevelEntry(final int mapId, final String mapName,
                                 final double averageLevel, final int roundedAverageLevel, final int sampledMobCount) {
            this.mapId = mapId;
            this.mapName = mapName;
            this.averageLevel = averageLevel;
            this.roundedAverageLevel = roundedAverageLevel;
            this.sampledMobCount = sampledMobCount;
        }
    }

    private static int getMobLevelCached(final int mobId, final Map<Integer, Integer> mobLevelCache) {
        Integer level = mobLevelCache.get(Integer.valueOf(mobId));
        if (level != null) {
            return level.intValue();
        }

        int resolvedLevel = -1;
        final MapleMonster monster = MapleLifeFactory.getMonster(mobId);
        if (monster != null && monster.getStats() != null) {
            resolvedLevel = monster.getStats().getLevel();
        }
        mobLevelCache.put(Integer.valueOf(mobId), Integer.valueOf(resolvedLevel));
        return resolvedLevel;
    }

    private static MapAverageLevelResult computeMapAverageLevel(final MapleData mapData, final Map<Integer, Integer> mobLevelCache) {
        if (mapData == null) {
            return null;
        }

        final MapleData lifeData = mapData.getChildByPath("life");
        if (lifeData == null) {
            return null;
        }

        long levelSum = 0L;
        int sampledMobCount = 0;

        for (MapleData life : lifeData) {
            if (!"m".equals(MapleDataTool.getString("type", life, ""))) {
                continue;
            }

            final int mobId = parseIntSafe(MapleDataTool.getString("id", life, ""), -1);
            if (mobId < 0) {
                continue;
            }

            final int mobLevel = getMobLevelCached(mobId, mobLevelCache);
            if (mobLevel <= 0) {
                continue;
            }

            levelSum += mobLevel;
            sampledMobCount++;
        }

        if (sampledMobCount <= 0) {
            return null;
        }

        final double averageLevel = (double) levelSum / (double) sampledMobCount;
        final int roundedAverageLevel = (int) Math.round(averageLevel);
        return new MapAverageLevelResult(averageLevel, roundedAverageLevel, sampledMobCount);
    }

    private static int getLevelBucketStart(final int roundedAverageLevel) {
        if (roundedAverageLevel < LEVEL_GROUP_MIN || roundedAverageLevel > LEVEL_GROUP_MAX) {
            return -1;
        }
        return ((roundedAverageLevel - 1) / LEVEL_GROUP_STEP) * LEVEL_GROUP_STEP + 1;
    }

    private static MapleData resolveLinkedMapData(final MapleDataProvider mapProvider, MapleData mapData) {
        if (mapProvider == null || mapData == null) {
            return mapData;
        }

        final Set<Integer> visited = new HashSet<Integer>();
        while (mapData != null) {
            final MapleData linkData = mapData.getChildByPath("info/link");
            if (linkData == null) {
                return mapData;
            }

            final int linkedMapId = MapleDataTool.getIntConvert(linkData, -1);
            if (linkedMapId < 0 || !visited.add(Integer.valueOf(linkedMapId))) {
                return mapData;
            }

            final String linkedMapPath = "Map/Map" + (linkedMapId / 100000000) + "/"
                    + StringUtil.getLeftPaddedStr(String.valueOf(linkedMapId), '0', 9) + ".img";
            try {
                mapData = mapProvider.getData(linkedMapPath);
            } catch (RuntimeException ignore) {
                return mapData;
            }
        }
        return null;
    }

    private static Map<Integer, String> buildMapNameLookup(final MapleData mapStringData) {
        final Map<Integer, String> result = new HashMap<Integer, String>();
        if (mapStringData == null) {
            return result;
        }

        for (MapleData mapAreaData : mapStringData.getChildren()) {
            for (MapleData mapIdData : mapAreaData.getChildren()) {
                final int mapId = parseIntSafe(mapIdData.getName(), -1);
                if (mapId < 0) {
                    continue;
                }

                final String streetName = MapleDataTool.getString(mapIdData.getChildByPath("streetName"), "");
                final String mapName = MapleDataTool.getString(mapIdData.getChildByPath("mapName"), "");
                final String displayName;
                if (!streetName.isEmpty() && !mapName.isEmpty()) {
                    displayName = streetName + " - " + mapName;
                } else if (!mapName.isEmpty()) {
                    displayName = mapName;
                } else if (!streetName.isEmpty()) {
                    displayName = streetName;
                } else {
                    displayName = "NO-NAME";
                }
                result.put(Integer.valueOf(mapId), displayName);
            }
        }

        return result;
    }

    public static class MobMapLog extends CommandExecute {

        @Override
        public int execute(final MapleClient c, final String[] splitted) {
            final String wzPath = System.getProperty("net.sf.odinms.wzpath", "wz");
            final File mapWz = new File(wzPath + "/Map.wz");
            final File stringWz = new File(wzPath + "/String.wz");

            if (!mapWz.exists() || !mapWz.isDirectory()) {
                c.getPlayer().dropMessage(6, "[mobmaplog] Map.wz path not found: " + mapWz.getPath());
                return 0;
            }
            if (!stringWz.exists() || !stringWz.isDirectory()) {
                c.getPlayer().dropMessage(6, "[mobmaplog] String.wz path not found: " + stringWz.getPath());
                return 0;
            }

            final MapleDataProvider mapProvider = MapleDataProviderFactory.getDataProvider(mapWz);
            final MapleDataProvider stringProvider = MapleDataProviderFactory.getDataProvider(stringWz);
            final MapleData mapStringData;
            try {
                mapStringData = stringProvider.getData("Map.img");
            } catch (RuntimeException ex) {
                c.getPlayer().dropMessage(6, "[mobmaplog] Failed to load String.wz/Map.img");
                FileoutputUtil.outputFileError(FileoutputUtil.CommandEx_Log, ex);
                return 0;
            }
            final Map<Integer, String> mapNameLookup = buildMapNameLookup(mapStringData);

            final MapleDataEntry mapEntry = mapProvider.getRoot().getEntry("Map");
            if (!(mapEntry instanceof MapleDataDirectoryEntry)) {
                c.getPlayer().dropMessage(6, "[mobmaplog] Map.wz/Map directory not found.");
                return 0;
            }
            final MapleDataDirectoryEntry mapDir = (MapleDataDirectoryEntry) mapEntry;

            final Map<Integer, Map<Integer, List<MobMapLevelEntry>>> groupedByLevel = new TreeMap<Integer, Map<Integer, List<MobMapLevelEntry>>>();
            final Map<Integer, Integer> mobLevelCache = new HashMap<Integer, Integer>();
            int scannedMapCount = 0;
            int loadFailCount = 0;
            int mobMapCount = 0;
            int noNameSkippedCount = 0;
            int outOfRangeSkippedCount = 0;
            int noLevelDataSkippedCount = 0;
            int groupedMapCount = 0;

            for (MapleDataDirectoryEntry mapCategoryDir : mapDir.getSubdirectories()) {
                final String categoryName = mapCategoryDir.getName();
                if (categoryName == null || !categoryName.startsWith("Map")) {
                    continue;
                }

                for (MapleDataFileEntry mapFile : mapCategoryDir.getFiles()) {
                    final String fileName = mapFile.getName();
                    if (fileName == null || !fileName.endsWith(".img")) {
                        continue;
                    }

                    final int mapId = parseIntSafe(fileName.substring(0, fileName.length() - 4), -1);
                    if (mapId < 0) {
                        continue;
                    }
                    scannedMapCount++;

                    final String mapPath = "Map/" + categoryName + "/" + fileName;
                    MapleData mapData;
                    try {
                        mapData = mapProvider.getData(mapPath);
                    } catch (RuntimeException ex) {
                        loadFailCount++;
                        continue;
                    }

                    mapData = resolveLinkedMapData(mapProvider, mapData);
                    final MapAverageLevelResult averageLevel = computeMapAverageLevel(mapData, mobLevelCache);
                    if (averageLevel == null) {
                        noLevelDataSkippedCount++;
                        continue;
                    }
                    mobMapCount++;

                    String mapName = mapNameLookup.get(Integer.valueOf(mapId));
                    if (mapName == null || mapName.isEmpty() || "NO-NAME".equals(mapName)) {
                        noNameSkippedCount++;
                        continue;
                    }

                    final int levelBucketStart = getLevelBucketStart(averageLevel.roundedAverageLevel);
                    if (levelBucketStart < 0) {
                        outOfRangeSkippedCount++;
                        continue;
                    }

                    final int firstDigit = StringUtil.getLeftPaddedStr(String.valueOf(mapId), '0', 9).charAt(0) - '0';
                    Map<Integer, List<MobMapLevelEntry>> byDigit = groupedByLevel.get(Integer.valueOf(levelBucketStart));
                    if (byDigit == null) {
                        byDigit = new TreeMap<Integer, List<MobMapLevelEntry>>();
                        groupedByLevel.put(Integer.valueOf(levelBucketStart), byDigit);
                    }
                    List<MobMapLevelEntry> grouped = byDigit.get(Integer.valueOf(firstDigit));
                    if (grouped == null) {
                        grouped = new ArrayList<MobMapLevelEntry>();
                        byDigit.put(Integer.valueOf(firstDigit), grouped);
                    }
                    grouped.add(new MobMapLevelEntry(
                            mapId,
                            mapName,
                            averageLevel.averageLevel,
                            averageLevel.roundedAverageLevel,
                            averageLevel.sampledMobCount
                    ));
                    groupedMapCount++;
                }
            }

            try {
                File dir = new File("log");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
            } catch (Throwable ignore) {
            }

            final String ts = FileoutputUtil.CurrentReadable_Time().replace(":", "-").replace(" ", "_");
            final String outFile = "log/MobMapByLevelThenFirstDigit_" + ts + ".txt";

            try (Writer w = new OutputStreamWriter(new FileOutputStream(outFile, false), StandardCharsets.UTF_8)) {
                w.write("=== WZ Mob Map List by Avg Level(1~250 step 10) then First Digit (" + FileoutputUtil.CurrentReadable_Time() + ") ===\n");
                w.write("wzPath: " + wzPath + "\n");
                w.write("scannedMaps: " + scannedMapCount
                        + ", mobMaps: " + mobMapCount
                        + ", groupedMaps: " + groupedMapCount
                        + ", noNameSkipped: " + noNameSkippedCount
                        + ", outOfRangeSkipped: " + outOfRangeSkippedCount
                        + ", noLevelDataSkipped: " + noLevelDataSkippedCount
                        + ", loadFails: " + loadFailCount + "\n\n");

                for (int levelStart = LEVEL_GROUP_MIN; levelStart <= LEVEL_GROUP_MAX; levelStart += LEVEL_GROUP_STEP) {
                    final int levelEnd = Math.min(levelStart + LEVEL_GROUP_STEP - 1, LEVEL_GROUP_MAX);
                    final Map<Integer, List<MobMapLevelEntry>> byDigit = groupedByLevel.get(Integer.valueOf(levelStart));

                    int bucketCount = 0;
                    if (byDigit != null) {
                        for (List<MobMapLevelEntry> entries : byDigit.values()) {
                            bucketCount += entries.size();
                        }
                    }
                    w.write("[" + levelStart + "-" + levelEnd + "] count=" + bucketCount + "\n");

                    if (byDigit != null) {
                        for (Map.Entry<Integer, List<MobMapLevelEntry>> digitEntry : byDigit.entrySet()) {
                            final int firstDigit = digitEntry.getKey().intValue();
                            final List<MobMapLevelEntry> entries = digitEntry.getValue();
                            Collections.sort(entries, new Comparator<MobMapLevelEntry>() {
                                @Override
                                public int compare(final MobMapLevelEntry o1, final MobMapLevelEntry o2) {
                                    return o1.mapId - o2.mapId;
                                }
                            });

                            w.write("  <" + firstDigit + "> count=" + entries.size() + "\n");
                            for (MobMapLevelEntry entry : entries) {
                                w.write("  " + entry.mapId
                                        + " | avg=" + String.format(Locale.US, "%.1f", entry.averageLevel)
                                        + " (rounded=" + entry.roundedAverageLevel + ", mobs=" + entry.sampledMobCount + ")"
                                        + " | " + entry.mapName + "\n");
                            }
                        }
                    }

                    w.write("\n");
                }

                w.write("=== END ===\n");
            } catch (Exception ex) {
                FileoutputUtil.outputFileError(FileoutputUtil.CommandEx_Log, ex);
                c.getPlayer().dropMessage(6, "[mobmaplog] Failed to write log file.");
                return 0;
            }

            c.getPlayer().dropMessage(6, "[mobmaplog] Completed. file=" + outFile + ", groupedMaps=" + groupedMapCount);
            return 1;
        }
    }

    public static class 몹맵로그 extends MobMapLog {
    }

    public static class 스킬정보 extends CommandExecute {

        // 요청하신 직업 코드 목록
        private static final int[] JOBS = {
            100, 110, 111, 112, 120, 121, 122, 130, 131, 132,
            200, 210, 211, 212, 220, 221, 222, 230, 231, 232,
            300, 310, 311, 312, 320, 321, 322,
            400, 410, 411, 412, 420, 421, 422,
            500, 510, 511, 512, 520, 521, 522,
            2100, 2110, 2111, 2112,
            2200, 2210, 2211, 2212, 2213, 2214, 2215, 2216, 2217, 2218,
            3200, 3210, 3211, 3212,
            3300, 3310, 3311, 3312,
            3500, 3510, 3511, 3512
        };

        private static MapleData SKILL_STRING_DATA = null;
        private static provider.MapleDataProvider SKILL_WZ_PROVIDER = null;

        // Skill.wz 파일 캐시(파일 단위)
        private static final Map<String, MapleData> SKILL_WZ_FILE_CACHE = new HashMap<>();

        // String.wz 내 토큰: #damage, #mobCount, #mpCon, #prop, #time ...
        private static final Pattern TOKEN = Pattern.compile("#([A-Za-z][A-Za-z0-9_]*)");

        private static MapleData getSkillStringData() {
            if (SKILL_STRING_DATA != null) {
                return SKILL_STRING_DATA;
            }
            final String wzPath = System.getProperty("net.sf.odinms.wzpath");
            if (wzPath == null) {
                return null;
            }
            SKILL_STRING_DATA = MapleDataProviderFactory
                    .getDataProvider(new File(wzPath + "/String.wz"))
                    .getData("Skill.img");
            return SKILL_STRING_DATA;
        }

        private static provider.MapleDataProvider getSkillWzProvider() {
            if (SKILL_WZ_PROVIDER != null) {
                return SKILL_WZ_PROVIDER;
            }
            final String wzPath = System.getProperty("net.sf.odinms.wzpath");
            if (wzPath == null) {
                return null;
            }
            SKILL_WZ_PROVIDER = MapleDataProviderFactory.getDataProvider(new File(wzPath + "/Skill.wz"));
            return SKILL_WZ_PROVIDER;
        }

        private static MapleData getSkillStringRoot(MapleData stringData, int skillId) {
            if (stringData == null) {
                return null;
            }
            String key = StringUtil.getLeftPaddedStr(String.valueOf(skillId), '0', 7);
            return stringData.getChildByPath(key);
        }

        private static String getSkillStringField(MapleData root, String field) {
            if (root == null) {
                return "";
            }
            MapleData d = root.getChildByPath(field);
            if (d == null) {
                return "";
            }
            return MapleDataTool.getString(d, "");
        }

        /**
         * String.wz 서식 코드 제거/보정 - #c #k #e #n #r #b #g #d 등 - 문제: "#cMP"처럼 #c가
         * 글자에 붙어 있으면 토큰이 "cMP"로 잡혀버림 => 이 경우 "c"는 서식코드, "MP"는 일반 텍스트이므로 "MP"를
         * 남겨야 합니다.
         */
        private static boolean isFormatCodeChar(char ch) {
            return ch == 'c' || ch == 'k' || ch == 'e' || ch == 'n' || ch == 'r' || ch == 'b'
                    || ch == 'g' || ch == 'd' || ch == 's' || ch == 't';
        }

        private static boolean isPureFormatCode(String token) {
            return token != null && token.length() == 1 && isFormatCodeChar(token.charAt(0));
        }

        /**
         * Skill.wz에서 숫자 키를 직접 찾아오는 fallback - MapleStatEffect에 없는
         * 값(dotInterval/subTime/subProp 등)을 여기서 해결합니다. - 조회 순서:
         * skill/<id>/level/<level>/<key>, common/<key>, info/<key>
         */
        private static Integer lookupSkillWzInt(int skillId, int level, String key) {
            try {
                provider.MapleDataProvider prov = getSkillWzProvider();
                if (prov == null) {
                    return null;
                }

                // 일반적으로 파일명은 (skillId / 10000)을 3자리로 패딩한 xxx.img
                final int jobFile = skillId / 10000;
                final String fileName = StringUtil.getLeftPaddedStr(String.valueOf(jobFile), '0', 3) + ".img";

                MapleData fileData = SKILL_WZ_FILE_CACHE.get(fileName);
                if (fileData == null) {
                    fileData = prov.getData(fileName);
                    if (fileData == null) {
                        return null;
                    }
                    SKILL_WZ_FILE_CACHE.put(fileName, fileData);
                }

                MapleData skillRoot = fileData.getChildByPath(String.valueOf(skillId));
                if (skillRoot == null) {
                    return null;
                }

                // level/<level>/<key>
                MapleData lvRoot = skillRoot.getChildByPath("level/" + level + "/" + key);
                if (lvRoot != null) {
                    return MapleDataTool.getInt(lvRoot, Integer.MIN_VALUE);
                }

                // common/<key>
                MapleData common = skillRoot.getChildByPath("common/" + key);
                if (common != null) {
                    return MapleDataTool.getInt(common, Integer.MIN_VALUE);
                }

                // info/<key>
                MapleData info = skillRoot.getChildByPath("info/" + key);
                if (info != null) {
                    return MapleDataTool.getInt(info, Integer.MIN_VALUE);
                }

            } catch (Throwable ignore) {
            }
            return null;
        }

        /**
         * MapleStatEffect 기반 치환 변수 맵 구성 + alias 추가
         */
        private static Map<String, String> buildVarMap(Skill skil, int level, MapleStatEffect eff) {
            Map<String, String> vars = new HashMap<>();

            if (skil != null) {
                vars.put("skillId", String.valueOf(skil.getId()));
                vars.put("level", String.valueOf(level));
                vars.put("maxLevel", String.valueOf(skil.getMaxLevel()));
                vars.put("masterLevel", String.valueOf(skil.getMasterLevel()));
            }
            if (eff == null) {
                return vars;
            }

            // 1) getter 기반 자동 등록 (getWatk -> watk, getWdef -> wdef, getIgnoreMob -> ignoreMob 등)
            try {
                for (Method m : MapleStatEffect.class.getDeclaredMethods()) {
                    if (!m.getName().startsWith("get")) {
                        continue;
                    }
                    if (m.getParameterTypes().length != 0) {
                        continue;
                    }

                    Class<?> rt = m.getReturnType();
                    if (!(rt == int.class || rt == short.class || rt == byte.class || rt == long.class || rt == double.class)) {
                        continue;
                    }

                    m.setAccessible(true);
                    Object v = m.invoke(eff);
                    if (v == null) {
                        continue;
                    }

                    String key = m.getName().substring(3);
                    if (key.isEmpty()) {
                        continue;
                    }

                    String tokenKey = Character.toLowerCase(key.charAt(0)) + key.substring(1);

                    if (v instanceof Double) {
                        // hpR/mpR 같은 소수는 보통 %표시라서 깔끔하게(필요하면 반올림 규칙 조정 가능)
                        double dv = (Double) v;
                        if (Math.abs(dv - Math.rint(dv)) < 1e-9) {
                            vars.put(tokenKey, String.valueOf((long) Math.rint(dv)));
                        } else {
                            vars.put(tokenKey, String.valueOf(dv));
                        }
                    } else {
                        vars.put(tokenKey, String.valueOf(((Number) v).longValue()));
                    }
                }
            } catch (Throwable ignore) {
            }

            // 2) 필드 기반 숫자 등록(혹시 getter가 빠진 버전 대비)
            try {
                for (Field f : MapleStatEffect.class.getDeclaredFields()) {
                    f.setAccessible(true);
                    Object v = f.get(eff);
                    if (v instanceof Number) {
                        vars.put(f.getName(), String.valueOf(((Number) v).longValue()));
                    }
                }
            } catch (Throwable ignore) {
            }

        // 3) String.wz에서 자주 쓰는 별칭(alias) 추가 (여기가 핵심)
            // - pdd/mdd/pad/mad/eva 는 MapleStatEffect의 wdef/mdef/watk/matk/avoid에 매핑되는 경우가 많습니다.
            try {
                vars.put("pdd", String.valueOf(eff.getWdef()));
            } catch (Throwable ignore) {
            }
            try {
                vars.put("mdd", String.valueOf(eff.getMdef()));
            } catch (Throwable ignore) {
            }
            try {
                vars.put("pad", String.valueOf(eff.getWatk()));
            } catch (Throwable ignore) {
            }
            try {
                vars.put("mad", String.valueOf(eff.getMatk()));
            } catch (Throwable ignore) {
            }
            try {
                vars.put("eva", String.valueOf(eff.getAvoid()));
            } catch (Throwable ignore) {
            }

            // - 확률: 이 서버는 getProb()
            try {
                vars.put("prop", String.valueOf(eff.getProb()));
            } catch (Throwable ignore) {
            }
            try {
                vars.put("prob", String.valueOf(eff.getProb()));
            } catch (Throwable ignore) {
            }

            // - time: duration(ms) -> 초
            try {
                vars.put("time", String.valueOf(eff.getDuration() / 1000));
            } catch (Throwable ignore) {
            }

            // - 방어율 무시: String.wz는 ignoreMobpdpR 같은 이름을 쓰지만, 서버는 ignoreMob 로 들고 있음
            try {
                vars.put("ignoreMobpdpR", String.valueOf(eff.getIgnoreMob()));
            } catch (Throwable ignore) {
            }

            // - 쿨: 둘 다 커버
            try {
                vars.put("cooltime", String.valueOf(eff.getCooldown()));
            } catch (Throwable ignore) {
            }
            try {
                vars.put("cooldown", String.valueOf(eff.getCooldown()));
            } catch (Throwable ignore) {
            }

            return vars;
        }

        /**
         * 토큰 치환: - 서식코드(#c 등)는 제거 - "#cMP"처럼 붙어있는 케이스는 "MP"만 남김 - vars에 없으면
         * Skill.wz(level/common/info)에서 찾아보고 있으면 치환 - 그래도 없으면 #만 제거하고 텍스트로
         * 남김(디버깅용)
         */
        private static String expandTokens(String text, Map<String, String> vars, int skillId, int level) {
            if (text == null || text.isEmpty()) {
                return "";
            }
            text = text.replace("\r", "");

            Matcher m = TOKEN.matcher(text);
            StringBuffer sb = new StringBuffer();

            while (m.find()) {
                String key = m.group(1);

                // 1) 순수 서식코드(#c #k #e #n #r #b #g #d #s #t 등)만 제거
                if (isPureFormatCode(key)) {
                    m.appendReplacement(sb, "");
                    continue;
                }

                // 2) 먼저 “정상 토큰” 치환을 시도 (여기서 #damage, #time, #cooltime 등이 해결되어야 함)
                String val = vars.get(key);
                if (val == null) {
                    Integer wzVal = lookupSkillWzInt(skillId, level, key);
                    if (wzVal != null && wzVal != Integer.MIN_VALUE) {
                        val = String.valueOf(wzVal);
                        vars.put(key, val);
                    }
                }

                if (val != null) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(val));
                    continue;
                }

        // 3) 그래도 못 찾으면, 이제서야 "#cMP" 같은 케이스를 처리:
                //    - 첫 글자가 서식코드이고
                //    - 다음 글자가 대문자면(대개 'MP', 'HP' 같은 일반 텍스트)
                //    => "#c"는 날리고 "MP"만 남긴다
                if (key.length() > 1 && isFormatCodeChar(key.charAt(0)) && Character.isUpperCase(key.charAt(1))) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(key.substring(1)));
                    continue;
                }

                // 4) 나머지는 디버깅/가독성을 위해 #만 제거하고 키만 남김
                m.appendReplacement(sb, Matcher.quoteReplacement(key));
            }

            m.appendTail(sb);
            return sb.toString();
        }

        private static String indentMultiline(String s, String indent) {
            if (s == null || s.isEmpty()) {
                return "";
            }
            s = s.replace("\r", "");
            String[] lines = s.split("\\\\n|\\n", -1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                sb.append(indent).append(lines[i]);
                if (i != lines.length - 1) {
                    sb.append("\n");
                }
            }
            return sb.toString();
        }

        private static int parseIntSafe(String s, int def) {
            try {
                return Integer.parseInt(s);
            } catch (Throwable t) {
                return def;
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            List<Integer> targetJobs = new ArrayList<>();
            if (splitted.length >= 2) {
                targetJobs.add(parseIntSafe(splitted[1], -1));
                if (targetJobs.get(0) < 0) {
                    c.getPlayer().dropMessage(6, "사용법: !스킬정보 [jobId]");
                    return 0;
                }
            } else {
                for (int j : JOBS) {
                    targetJobs.add(j);
                }
            }

            MapleData stringData = getSkillStringData();
            if (stringData == null) {
                c.getPlayer().dropMessage(6, "String.wz 경로를 찾지 못했습니다. (system property net.sf.odinms.wzpath 확인)");
                return 0;
            }
            if (getSkillWzProvider() == null) {
                c.getPlayer().dropMessage(6, "Skill.wz 경로를 찾지 못했습니다. (system property net.sf.odinms.wzpath 확인)");
                return 0;
            }

            // log 폴더 준비
            try {
                File dir = new File("log");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
            } catch (Throwable ignore) {
            }

            final String ts = FileoutputUtil.CurrentReadable_Time().replace(":", "-").replace(" ", "_");
            final String outFile = "log/SkillInfoDump_" + ts + ".txt";

            int totalSkills = 0;

            try (Writer w = new OutputStreamWriter(new FileOutputStream(outFile, false), StandardCharsets.UTF_8)) {
                w.write("=== SkillInfo Dump (" + FileoutputUtil.CurrentReadable_Time() + ") ===\n");
                w.write("Jobs: " + targetJobs + "\n\n");

                for (int jobId : targetJobs) {
                    List<Integer> skills = SkillFactory.getSkillsByJob(jobId);
                    if (skills == null || skills.isEmpty()) {
                        w.write("---- Job " + jobId + " : (no skills) ----\n\n");
                        continue;
                    }
                    Collections.sort(skills);

                    w.write("---- Job " + jobId + " : " + skills.size() + " skills ----\n\n");

                    for (int skillId : skills) {
                        Skill s = SkillFactory.getSkill(skillId);
                        if (s == null) {
                            continue;
                        }

                        int baseLevel = (s.getMasterLevel() > 0 ? s.getMasterLevel() : s.getMaxLevel());
                        if (baseLevel <= 0) {
                            baseLevel = 1;
                        }

                        MapleStatEffect eff = s.getEffect(baseLevel);
                        Map<String, String> vars = buildVarMap(s, baseLevel, eff);

                        MapleData root = getSkillStringRoot(stringData, skillId);

                        String name = getSkillStringField(root, "name");
                        String desc = getSkillStringField(root, "desc");
                        String h = getSkillStringField(root, "h");

                        // 토큰 치환(중요: skillId/level을 같이 넘겨 Skill.wz fallback이 가능하게)
                        name = expandTokens(name, vars, skillId, baseLevel);
                        desc = expandTokens(desc, vars, skillId, baseLevel);
                        h = expandTokens(h, vars, skillId, baseLevel);

                        String displayName = (name != null && !name.isEmpty())
                                ? name
                                : SkillFactory.getSkillName(skillId);

                        w.write("[" + skillId + "] " + (displayName == null ? "" : displayName) + "\n");
                        w.write("  - 기준레벨: " + baseLevel + " (masterLevel=" + s.getMasterLevel() + ", maxLevel=" + s.getMaxLevel() + ")\n");

                        // 운영자용 “한 줄 요약”
                        if (eff != null) {
                            StringBuilder summary = new StringBuilder();
                            if (eff.getMobCount() > 0) {
                                summary.append("대상 ").append(eff.getMobCount()).append(" / ");
                            }
                            if (eff.getAttackCount() > 0) {
                                summary.append("타수 ").append(eff.getAttackCount()).append(" / ");
                            }
                            if (eff.getDamage() > 0) {
                                summary.append("데미지 ").append(eff.getDamage()).append("% / ");
                            }
                            if (eff.getMPCon() > 0) {
                                summary.append("MP ").append(eff.getMPCon()).append(" / ");
                            }
                            if (eff.getProb() > 0) {
                                summary.append("확률 ").append(eff.getProb()).append("% / ");
                            }
                            if (eff.getDuration() > 0) {
                                summary.append("지속 ").append(eff.getDuration() / 1000).append("초 / ");
                            }
                            if (eff.getCooldown() > 0) {
                                summary.append("쿨 ").append(eff.getCooldown()).append("초 / ");
                            }
                            if (summary.length() > 0) {
                                summary.setLength(summary.length() - 3);
                                w.write("  - 요약: " + summary + "\n");
                            }
                        }

                        if (desc != null && !desc.isEmpty()) {
                            w.write("  - desc:\n");
                            w.write(indentMultiline(desc, "    ") + "\n");
                        }
                        if (h != null && !h.isEmpty()) {
                            w.write("  - h:\n");
                            w.write(indentMultiline(h, "    ") + "\n");
                        }

                        w.write("\n");
                        totalSkills++;
                    }

                    w.write("\n");
                }

                w.write("=== END (total skills dumped: " + totalSkills + ") ===\n");

            } catch (Exception e) {
                FileoutputUtil.outputFileError(FileoutputUtil.CommandEx_Log, e);
                c.getPlayer().dropMessage(6, "덤프 중 오류가 발생했습니다. (log/Log_Command_Except.txt 확인)");
                return 0;
            }

            c.getPlayer().dropMessage(6, "스킬 정보 덤프 완료. 파일: " + outFile + " (총 " + totalSkills + "개)");
            return 1;
        }
    }
}
