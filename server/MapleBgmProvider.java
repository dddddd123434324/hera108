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

import provider.MapleData;
import provider.MapleDataFileEntry;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Lunatic
 */
public class MapleBgmProvider {

    public static final List<String> BGM_CATEGORY_LIST = new LinkedList<>();
    public static final List<String> BGM_SPECIAL_CATEGORY_LIST = new LinkedList<>();
    public static final Map<String, List<BgmInfo>> BGM_LIST_MAP = new LinkedHashMap<>();

    public static void load() {
        final Map<String, String> bgmNameMap = new HashMap<>();
        try {
            for (String line : Files.readAllLines(Paths.get("BGMName.map"))) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] pair = line.split("/", 2);
                if (bgmNameMap.containsKey(pair[0])) {
                    System.err.println("Duplicated BGMName key. : " + pair[0]);
                } else {
                    bgmNameMap.put(pair[0], pair[1]);
                }
            }
        } catch (IOException ex) {
            System.out.println("BGMName.map not found.");
        }

        final Map<String, String> bgmCategoryRemap = new HashMap<>();
        try {
            for (String line : Files.readAllLines(Paths.get("BGMCategoryRe.map"))) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] pair = line.split("/", 2);
                if (bgmCategoryRemap.containsKey(pair[1])) {
                    System.err.println("Duplicated BGMName key. : " + pair[1]);
                } else {
                    bgmCategoryRemap.put(pair[1], pair[0]);
                }
            }
        } catch (IOException ex) {
            System.out.println("bgmCategoryRe.map not found.");
        }

        final List<String> bgmExceptionList = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(Paths.get("BGMException.lst"))) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                bgmExceptionList.add(line);
            }
        } catch (IOException ex) {
            System.out.println("BGMException.lst not found.");
        }

        MapleDataProvider soundProvider = MapleDataProviderFactory.getDataProvider(new File(ServerProperties.WZ_PATH + "/Sound.wz"));
        for (MapleDataFileEntry file : soundProvider.getRoot().getFiles()) {
            final String name = file.getName();
            if (name.startsWith("Bgm")) {
                MapleData data = soundProvider.getData(name);
                final String categoryPath = data.getName();
                final List<BgmInfo> list = new LinkedList<>();
                for (MapleData sound : data) {
                    final String path = sound.getName();
                    if (bgmExceptionList.contains(path)) {
                        continue;
                    }
                    String bgmName = bgmNameMap.getOrDefault(path, path);
                    String newCategory = bgmCategoryRemap.get(path);
                    final BgmInfo e = new BgmInfo(categoryPath, path, bgmName);
                    if (newCategory != null) {
                        BGM_LIST_MAP.computeIfAbsent(newCategory, key -> {
                            BGM_CATEGORY_LIST.add(key);
                            BGM_SPECIAL_CATEGORY_LIST.add(key);
                            return new LinkedList<>();
                        }).add(e);
                    } else {
                        list.add(e);
                    }
                }
                if (!list.isEmpty()) {
                    BGM_CATEGORY_LIST.add(categoryPath);
                    BGM_LIST_MAP.put(categoryPath, list);
                }
            }
        }
        BGM_CATEGORY_LIST.sort(Comparator.naturalOrder());
        BGM_SPECIAL_CATEGORY_LIST.sort(Comparator.naturalOrder());
    }

    public static class BgmInfo {

        private final String categoryPath;
        private final String path;
        private final String name;
        private final String fullPath;

        public BgmInfo(String categoryPath, String path, String name) {
            this.categoryPath = categoryPath;
            this.path = path;
            this.name = name;
            this.fullPath = categoryPath + "/" + path;
        }

        public String getCategoryPath() {
            return categoryPath;
        }

        public String getPath() {
            return path;
        }

        public String getName() {
            return name;
        }

        public String getFullPath() {
            return fullPath;
        }

        @Override
        public String toString() {
            return fullPath;
        }
    }
}
