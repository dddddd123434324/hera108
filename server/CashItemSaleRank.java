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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Lunatic
 */
public class CashItemSaleRank {

    private static final String FILENAME = "commsale.db";
    private static final Map<Integer, AtomicInteger> SALE_INFO_M = new HashMap<>(),
            SALE_INFO_M_D = new HashMap<>(),
            SALE_INFO_F = new HashMap<>(),
            SALE_INFO_F_D = new HashMap<>();

    public static synchronized void setUp() {
        Path path = Paths.get(FILENAME);
        try ( ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(path))) {
            try {
                SALE_INFO_M.putAll((Map<Integer, AtomicInteger>) ois.readObject());
                SALE_INFO_M_D.putAll((Map<Integer, AtomicInteger>) ois.readObject());
                SALE_INFO_F.putAll((Map<Integer, AtomicInteger>) ois.readObject());
                SALE_INFO_F_D.putAll((Map<Integer, AtomicInteger>) ois.readObject());
            } catch (ClassNotFoundException ex) {
                Files.delete(path);
            }
        } catch (IOException ex) {
        }
    }

    public static synchronized void cleanUp() {
        Path path = Paths.get(FILENAME);
        try ( ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING))) {
            oos.writeObject(SALE_INFO_M);
            oos.writeObject(SALE_INFO_M_D);
            oos.writeObject(SALE_INFO_F);
            oos.writeObject(SALE_INFO_F_D);
        } catch (IOException ex) {
        }
    }

    private static Map<Integer, AtomicInteger> getForGender(int gender) {
        return gender == 0 ? SALE_INFO_M : SALE_INFO_F;
    }

    private static Map<Integer, AtomicInteger> getForGenderD(int gender) {
        return gender == 0 ? SALE_INFO_M_D : SALE_INFO_F_D;
    }

    public static synchronized void inc(int sn, int gender, boolean donate) {
        if (donate) {
            getForGenderD(gender).computeIfAbsent(sn, k -> new AtomicInteger()).incrementAndGet();
        } else {
            getForGender(gender).computeIfAbsent(sn, k -> new AtomicInteger()).incrementAndGet();
        }
    }

    public static synchronized int[] top5(int category, int gender, boolean donate) {
        if (donate) {
            return getForGenderD(gender).entrySet().stream()
                    .filter(entry -> entry.getKey() / 10000000 == category)
                    .sorted((e1, e2) -> Integer.compare(e1.getValue().get(), e2.getValue().get()))
                    .limit(5)
                    .mapToInt(entry -> entry.getKey())
                    .toArray();
        } else {
            return getForGender(gender).entrySet().stream()
                    .filter(entry -> entry.getKey() / 10000000 == category)
                    .sorted((e1, e2) -> Integer.compare(e1.getValue().get(), e2.getValue().get()))
                    .limit(5)
                    .mapToInt(entry -> entry.getKey())
                    .toArray();
        }
    }
}
