package server;

import client.MapleCharacter;
import handling.channel.ChannelServer;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.FileoutputUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;

public class BurningHuntingFieldManager {

    // Change this value to adjust reroll period (currently 3 hours).
    public static final long REFRESH_INTERVAL_MILLIS = 3L * 60L * 60L * 1000L;

    // trueexp bonus multiplier on selected maps (1.3 = +30%).
    public static final double TRUEEXP_MULTIPLIER = 1.3D;

    // Curated lineup file path (override with -Dburning.hunting.pool.file=... if needed).
    public static final String DEFAULT_POOL_FILE_PATH = "ini/burning_hunting_fields_pool.txt";

    private static final int PICK_COUNT_PER_BUCKET = 3;
    private static final int MIN_AVG_LEVEL = 1;
    private static final int MAX_AVG_LEVEL = 250;
    private static final int OPEN_ENDED_BUCKET = 999;

    private static final Object LOCK = new Object();

    private static final Map<Integer, List<BurningFieldInfo>> POOL_BY_BUCKET = new TreeMap<Integer, List<BurningFieldInfo>>();

    private static volatile Map<Integer, List<BurningFieldInfo>> currentByBucket = Collections.emptyMap();
    private static volatile Set<Integer> currentMapIds = Collections.emptySet();
    private static volatile long lastRefreshTime = 0L;

    private static boolean initialized = false;
    private static ScheduledFuture<?> refreshTask = null;

    public static final class BurningFieldInfo {

        private final int mapId;
        private final String mapName;
        private final double avgLevel;
        private final int roundedAvgLevel;

        private BurningFieldInfo(final int mapId, final String mapName, final double avgLevel, final int roundedAvgLevel) {
            this.mapId = mapId;
            this.mapName = mapName;
            this.avgLevel = avgLevel;
            this.roundedAvgLevel = roundedAvgLevel;
        }

        public int getMapId() {
            return mapId;
        }

        public String getMapName() {
            return mapName;
        }

        public double getAvgLevel() {
            return avgLevel;
        }

        public int getRoundedAvgLevel() {
            return roundedAvgLevel;
        }
    }

    public static final class BurningFieldGroup {

        private final int displayStartLevel;
        private final int displayEndLevel;
        private final List<BurningFieldInfo> entries;

        private BurningFieldGroup(final int displayStartLevel, final int displayEndLevel, final List<BurningFieldInfo> entries) {
            this.displayStartLevel = displayStartLevel;
            this.displayEndLevel = displayEndLevel;
            this.entries = entries;
        }

        public int getDisplayStartLevel() {
            return displayStartLevel;
        }

        public int getDisplayEndLevel() {
            return displayEndLevel;
        }

        public List<BurningFieldInfo> getEntries() {
            return entries;
        }
    }

    private BurningHuntingFieldManager() {
    }

    public static void init() {
        synchronized (LOCK) {
            if (initialized) {
                return;
            }

            buildPool();
            rerollInternal();
            refreshTask = Timer.WorldTimer.getInstance().register(new Runnable() {
                @Override
                public void run() {
                    reroll();
                }
            }, REFRESH_INTERVAL_MILLIS, REFRESH_INTERVAL_MILLIS);

            initialized = true;
            //System.out.println("[BurningHuntingFieldManager] init completed. poolBuckets=" + POOL_BY_BUCKET.size());
        }
    }

    public static void reroll() {
        synchronized (LOCK) {
            if (!initialized) {
                return;
            }
            rerollInternal();
        }
    }

    public static double getTrueExpMultiplier(final int mapId) {
        return currentMapIds.contains(Integer.valueOf(mapId)) ? TRUEEXP_MULTIPLIER : 1.0D;
    }

    public static List<BurningFieldGroup> getCurrentGroups() {
        final Map<Integer, List<BurningFieldInfo>> snapshot = currentByBucket;
        final List<BurningFieldGroup> groups = new ArrayList<BurningFieldGroup>();
        for (Map.Entry<Integer, List<BurningFieldInfo>> entry : snapshot.entrySet()) {
            final int bucketEnd = entry.getKey().intValue();
            final int bucketStart = getBucketDisplayStart(bucketEnd);
            final int displayEnd = getBucketDisplayEnd(bucketEnd);
            groups.add(new BurningFieldGroup(bucketStart, displayEnd, new ArrayList<BurningFieldInfo>(entry.getValue())));
        }
        return groups;
    }

    public static long getLastRefreshTime() {
        return lastRefreshTime;
    }

    public static long getNextRefreshTime() {
        if (lastRefreshTime <= 0L) {
            return 0L;
        }
        return lastRefreshTime + REFRESH_INTERVAL_MILLIS;
    }

    private static void buildPool() {
        POOL_BY_BUCKET.clear();

        final File poolFile = resolvePoolFile();
        if (poolFile == null) {
            System.err.println("[BurningHuntingFieldManager] curated pool file not found: " + DEFAULT_POOL_FILE_PATH);
            return;
        }

        final Map<Integer, String> mapNameLookup = buildMapNameLookupFromStringWz();
        final Set<String> dedupe = new HashSet<String>();

        int added = 0;
        int skipped = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(poolFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                final String[] parts = line.split("\\|");
                if (parts.length < 3) {
                    skipped++;
                    continue;
                }

                final int bucketEnd = parseIntSafe(parts[0].trim(), -1);
                final int mapId = parseIntSafe(parts[1].trim(), -1);
                final int roundedAvgLevel = parseIntSafe(parts[2].trim(), -1);

                if (!isSupportedBucket(bucketEnd) || mapId < 0 || roundedAvgLevel < MIN_AVG_LEVEL || roundedAvgLevel > MAX_AVG_LEVEL) {
                    skipped++;
                    continue;
                }

                final String dedupeKey = bucketEnd + ":" + mapId;
                if (!dedupe.add(dedupeKey)) {
                    continue;
                }

                String mapName = mapNameLookup.get(Integer.valueOf(mapId));
                if ("NO-NAME".equals(mapName)) {
                    skipped++;
                    continue;
                }
                if (mapName == null || mapName.isEmpty()) {
                    mapName = String.valueOf(mapId);
                }

                List<BurningFieldInfo> bucket = POOL_BY_BUCKET.get(Integer.valueOf(bucketEnd));
                if (bucket == null) {
                    bucket = new ArrayList<BurningFieldInfo>();
                    POOL_BY_BUCKET.put(Integer.valueOf(bucketEnd), bucket);
                }
                bucket.add(new BurningFieldInfo(mapId, mapName, (double) roundedAvgLevel, roundedAvgLevel));
                added++;
            }
        } catch (IOException ex) {
            System.err.println("[BurningHuntingFieldManager] failed to read curated pool file: " + poolFile.getPath());
            ex.printStackTrace();
            return;
        }

        for (List<BurningFieldInfo> bucket : POOL_BY_BUCKET.values()) {
            Collections.sort(bucket, new Comparator<BurningFieldInfo>() {
                @Override
                public int compare(final BurningFieldInfo o1, final BurningFieldInfo o2) {
                    return o1.mapId - o2.mapId;
                }
            });
        }
    }

    private static File resolvePoolFile() {
        final String configuredPath = System.getProperty("burning.hunting.pool.file", DEFAULT_POOL_FILE_PATH);
        final File configured = new File(configuredPath);
        if (configured.exists() && configured.isFile()) {
            return configured;
        }
        final String[] fallbackPaths = new String[]{
                DEFAULT_POOL_FILE_PATH,
                "../" + DEFAULT_POOL_FILE_PATH,
                "../../" + DEFAULT_POOL_FILE_PATH
        };
        for (String fallbackPath : fallbackPaths) {
            final File fallback = new File(fallbackPath);
            if (fallback.exists() && fallback.isFile()) {
                return fallback;
            }
        }
        return null;
    }

    private static Map<Integer, String> buildMapNameLookupFromStringWz() {
        final Map<Integer, String> result = new HashMap<Integer, String>();

        final String wzPath = System.getProperty("net.sf.odinms.wzpath", "wz");
        final File stringWz = new File(wzPath + "/String.wz");
        if (!stringWz.exists() || !stringWz.isDirectory()) {
            System.err.println("[BurningHuntingFieldManager] String.wz path is invalid: " + stringWz.getPath());
            return result;
        }

        final MapleDataProvider stringProvider = MapleDataProviderFactory.getDataProvider(stringWz);
        final MapleData mapStringData;
        try {
            mapStringData = stringProvider.getData("Map.img");
        } catch (RuntimeException ex) {
            System.err.println("[BurningHuntingFieldManager] failed to load String.wz/Map.img");
            ex.printStackTrace();
            return result;
        }

        if (mapStringData == null) {
            return result;
        }

        for (MapleData areaData : mapStringData.getChildren()) {
            for (MapleData mapIdData : areaData.getChildren()) {
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

    private static void rerollInternal() {
        final Set<Integer> previousMapIds = currentMapIds;
        final Map<Integer, List<BurningFieldInfo>> nextByBucket = new TreeMap<Integer, List<BurningFieldInfo>>();
        final Set<Integer> nextMapIds = new HashSet<Integer>();

        for (Map.Entry<Integer, List<BurningFieldInfo>> entry : POOL_BY_BUCKET.entrySet()) {
            final List<BurningFieldInfo> candidates = new ArrayList<BurningFieldInfo>(entry.getValue());
            if (candidates.isEmpty()) {
                continue;
            }

            Collections.shuffle(candidates);
            final int count = Math.min(PICK_COUNT_PER_BUCKET, candidates.size());
            final List<BurningFieldInfo> picked = new ArrayList<BurningFieldInfo>(candidates.subList(0, count));
            Collections.sort(picked, new Comparator<BurningFieldInfo>() {
                @Override
                public int compare(final BurningFieldInfo o1, final BurningFieldInfo o2) {
                    return o1.mapId - o2.mapId;
                }
            });

            nextByBucket.put(entry.getKey(), Collections.unmodifiableList(picked));
            for (BurningFieldInfo info : picked) {
                nextMapIds.add(Integer.valueOf(info.mapId));
            }
        }

        final Set<Integer> endedMapIds = new HashSet<Integer>(previousMapIds);
        endedMapIds.removeAll(nextMapIds);

        currentByBucket = Collections.unmodifiableMap(nextByBucket);
        currentMapIds = Collections.unmodifiableSet(nextMapIds);
        lastRefreshTime = System.currentTimeMillis();
        notifyBurningEndedPlayers(endedMapIds);

        final String now = FileoutputUtil.CurrentReadable_Time();
        //System.out.println("[BurningHuntingFieldManager] rerolled at " + now + ", selectedMaps=" + nextMapIds.size() + ", endedMaps=" + endedMapIds.size());
    }

    private static void notifyBurningEndedPlayers(final Set<Integer> endedMapIds) {
        if (endedMapIds == null || endedMapIds.isEmpty()) {
            return;
        }

        int notified = 0;
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            if (cserv == null || cserv.getPlayerStorage() == null) {
                continue;
            }
            for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                if (chr == null) {
                    continue;
                }
                if (endedMapIds.contains(Integer.valueOf(chr.getMapId()))) {
                    chr.dropMessage(-1, "버닝 사냥터 종료!");
                    notified++;
                }
            }
        }

        //if (notified > 0) {
        //    System.out.println("[BurningHuntingFieldManager] notified burning-end message to " + notified + " player(s).");
        //}
    }

    private static boolean isSupportedBucket(final int bucketEnd) {
        if (bucketEnd == OPEN_ENDED_BUCKET || bucketEnd == 20) {
            return true;
        }
        return bucketEnd >= 30 && bucketEnd <= 140 && bucketEnd % 10 == 0;
    }

    private static int getBucketDisplayStart(final int bucketEnd) {
        if (bucketEnd == OPEN_ENDED_BUCKET) {
            return 141;
        }
        return bucketEnd <= 20 ? 10 : bucketEnd - 9;
    }

    private static int getBucketDisplayEnd(final int bucketEnd) {
        return bucketEnd == OPEN_ENDED_BUCKET ? -1 : bucketEnd;
    }

    private static int parseIntSafe(final String text, final int def) {
        try {
            return Integer.parseInt(text);
        } catch (Throwable ignore) {
            return def;
        }
    }

    public static String getDebugSummary() {
        final StringBuilder sb = new StringBuilder();
        sb.append("selected=").append(currentMapIds.size()).append(", buckets=").append(currentByBucket.size());
        if (lastRefreshTime > 0L) {
            sb.append(", last=").append(new Date(lastRefreshTime));
        }
        return sb.toString();
    }
}
