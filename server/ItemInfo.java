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
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Lunatic
 */
public class ItemInfo {

    private static final MapleDataProvider etc = MapleDataProviderFactory.getDataProvider(new File(ServerProperties.WZ_PATH + "/Etc.wz"));

    public static final List<SetItemInfo> setItemInfoList = new LinkedList<>();

    public static void initSetItem() {
        MapleData img = etc.getData("SetItemInfo.img");
        for (MapleData setItemInfo : img) {
            setItemInfoList.add(new SetItemInfo(setItemInfo));
        }
    }

    public static String getName(int itemID) {
        return MapleItemInformationProvider.getInstance().getName(itemID);
    }

    public static boolean isExist(int itemID) {
        return MapleItemInformationProvider.getInstance().getItemInformation(itemID) != null;
    }
}
