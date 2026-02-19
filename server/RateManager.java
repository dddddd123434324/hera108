/*
 * The MIT License
 *
 * Copyright 2017 J?펉ar.
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

import tools.data.MaplePacketLittleEndianWriter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * @author Lunatic
 */
public class RateManager {

    private static final String FILENAME = "eventschedule.db";
    public static final int DISPLAY_DROP, DISPLAY_MESO;
    public static final int TRUEEXP0, EXP0, DROP0, TRUEDROP0, MESO0, QEXP0, PEXP0;
    public static int EXP, TRUEEXP, DROP, TRUEDROP, MESO, QEXP, PEXP, CASH = 1, TRAIT = 1, BURNING = 0;
    public static final List<EventSchedule> SCHEDULES = new ArrayList<>(256);
    public static EventSchedule CURRENT_SCHEDULE = null;

    static {
        EXP = EXP0 = Integer.parseInt(ServerProperties.getProperty("exp"));
        //TRUEEXP = TRUEEXP0 = Integer.parseInt(ServerProperties.getProperty("trueexp", "1"));
        TRUEEXP = TRUEEXP0 = 1;
        QEXP = QEXP0 = Integer.parseInt(ServerProperties.getProperty("qexp"));
        PEXP = PEXP0 = Integer.parseInt(ServerProperties.getProperty("pexp"));
        DROP = DROP0 = Integer.parseInt(ServerProperties.getProperty("drop", "1"));
        TRUEDROP = TRUEDROP0 = Integer.parseInt(ServerProperties.getProperty("truedrop", "1"));
        MESO = MESO0 = Integer.parseInt(ServerProperties.getProperty("meso"));
        DISPLAY_DROP = Integer.parseInt(ServerProperties.getProperty("fakedrop"));
        DISPLAY_MESO = Integer.parseInt(ServerProperties.getProperty("fakemeso"));
    }

    public static int getTrueExpByLevel(final int level) { // 寃쏀뿕移?諛곗쑉 ?섏젙
        final int tier;
        if (level >= 221) {
            tier = 1;
        } else if (level >= 200) {
            tier = 2;      // 200~220
        } else if (level >= 151) {
            tier = 3;      // 151~199
        } else if (level >= 121) {
            tier = 5;      // 121~150
        } else if (level >= 101) {
            tier = 8;      // 101~120
        } else if (level >= 10) {
            tier = 10;      // 10~100
        } else {
            tier = 10;
        }
        return tier * EXP * TRUEEXP;
    }

    public static int getTrueDropRate() {
        return (int) Math.min((long) DROP * TRUEDROP, Integer.MAX_VALUE);
    }

    public static synchronized void load() {
        Path path = Paths.get(FILENAME);
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(path))) {
            try {
                final List<EventSchedule> initial = (List<EventSchedule>) ois.readObject();
                initial.forEach(RateManager::setTimer);
                SCHEDULES.addAll(initial);
            } catch (ClassNotFoundException ex) {
                Files.delete(path);
            }
        } catch (IOException ex) {
        }
    }

    public static synchronized void save() {
        Path path = Paths.get(FILENAME);
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING))) {
            oos.writeObject(SCHEDULES);
        } catch (IOException ex) {
        }
    }

    public static void set(EventSchedule k) {
        EXP = k.exp;
        DROP = k.drop;
        MESO = k.meso;
        CURRENT_SCHEDULE = k;
    }

    public static void reset() {
        BURNING = 0;
        EXP = EXP0;
        DROP = DROP0;
        MESO = MESO0;
        if (CURRENT_SCHEDULE != null && CURRENT_SCHEDULE.state == 2) {
            CURRENT_SCHEDULE.future.cancel(false);
            CURRENT_SCHEDULE = null;
            purge();
        }
    }

    public static EventSchedule setTimer(EventSchedule k) {
        k.future = Timer.WorldTimer.getInstance().scheduleAtTimestamp(k, k.start);
        return k;
    }

    public static void insert(String name, long start, long end, int exp, int drop, int meso, String msg) {
        synchronized (SCHEDULES) {
            if (checkDuplicatedSchedule(start, end)) {
                SCHEDULES.add(setTimer(new EventSchedule(name, start, end, exp, drop, meso, msg)));
            }
        }
    }

    public static void delete(long start) {
        synchronized (SCHEDULES) {
            SCHEDULES.removeIf(k -> k.checkDead(start));
        }
    }

    public static void writeSchedules(MaplePacketLittleEndianWriter mplew) {
        synchronized (SCHEDULES) {
            mplew.write(SCHEDULES.size());
            for (EventSchedule eventSchedule : SCHEDULES) {
                mplew.writeMapleAsciiString(eventSchedule.name);
                mplew.writeLong(eventSchedule.start);
                mplew.writeLong(eventSchedule.end);
                mplew.writeInt(eventSchedule.exp);
                mplew.writeInt(eventSchedule.drop);
                mplew.writeInt(eventSchedule.meso);
                mplew.writeMapleAsciiString(eventSchedule.msg);
                mplew.write(eventSchedule.state);
            }
        }
    }

    private static void purge() {
        SCHEDULES.removeIf(k -> k.state == 2);
    }

    private static boolean checkDuplicatedSchedule(long start, long end) {
        return SCHEDULES.stream().allMatch(k -> Math.min(k.end, end) - Math.max(k.start, start) < 0);
    }

    public static class EventSchedule implements Runnable {

        public final String name;
        public final long start;
        public final long end;
        public final int exp;
        public final int drop;
        public final int meso;
        public final String msg;
        public int state;
        public ScheduledFuture future;

        public EventSchedule(String name, long start, long end, int exp, int drop, int meso, String msg) {
            this.name = name;
            this.start = start;
            this.end = end;
            this.exp = exp;
            this.drop = drop;
            this.meso = meso;
            this.msg = msg;
        }

        public boolean checkDead(long start) {
            if (this.start == start) {
                if (state == 1) {
                    reset();
                }
                state = 2;
                if (future != null) {
                    future.cancel(true);
                    future = null;
                }
                return true;
            }
            return false;
        }

        @Override
        public void run() {
            try {
                state = 0;
                sleepTill(start);

                state = 1;
                set(this);
                sleepTill(end);

                state = 2;
                reset();
            } catch (InterruptedException ex) {
            }
        }

        private void sleepTill(long t) throws InterruptedException {
            t = System.currentTimeMillis() - t;
            if (t > 0) {
                Thread.sleep(t);
            }
        }
    }
}
