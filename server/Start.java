package server;

import client.MapleCharacter;
import client.SkillFactory;
import client.inventory.MapleInventoryIdentifier;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.auth.AuthServer;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.MapleGuildRanking;
import handling.gm.GMServer;
import handling.login.LoginInformationProvider;
import handling.login.LoginServer;
import handling.world.World;
import handling.world.family.MapleFamily;
import handling.world.guild.MapleGuild;
import server.Timer.*;
import server.events.MapleOxQuizFactory;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.life.MobSkillFactory;
import server.life.PlayerNPC;
import server.marriage.MarriageManager;
import server.quest.MapleQuest;
import server.shops.MinervaOwlSearchTop;
import tools.DeadLockDetector;
import tools.MemoryUsageWatcher;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Start {

    public static long startTime = System.currentTimeMillis();
    public static final Start instance = new Start();
    public static AtomicInteger CompletedLoadingThreads = new AtomicInteger(0);
    public static int TotalLoadingThreads = 0;

    public static void startGC(long start) {
        System.gc();
        float end = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("[GarbageCollection] This Thread Memory : " + ((start - end) / (1024 * 1024)) + "Bytes Clean");
    }

    public static void main(final String args[]) throws InterruptedException {
        instance.run();
    }
    
    public void run() throws InterruptedException {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (osName.startsWith("windows")) {
            try {
                PrintStream out = new PrintStream(System.out, true, "EUC-KR");
                PrintStream err = new PrintStream(System.out, true, "EUC-KR");
                System.setOut(out);
                System.setErr(err);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(Start.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        DatabaseConnection.init();

        if (Boolean.parseBoolean(ServerProperties.getProperty("adminOnly")) || ServerConstants.Use_Localhost) {
            ServerConstants.Use_Fixed_IV = false;
            System.out.println("[!!! Admin Only Mode Active !!!]");
        }

        String ip = ServerProperties.getProperty("host");
        if (ip != null) {
            try {
                InetAddress address = InetAddress.getByName(ip);
                String raw_address = address.getHostAddress();

                ServerConstants.Gateway_IP = address.getAddress();

                System.out.println("[Gateway IP] Presented Host : " + ip);
                System.out.println("[Gateway IP] Resolved Host Address of Server Machine : " + raw_address);
            } catch (Exception e) {
                System.err.println("Error : Cannot set Gateway IP ");
                System.err.println("Set default gateway ip - 127.0.0.1 (loopback)");
                e.printStackTrace();

                ServerConstants.Gateway_IP = new byte[]{(byte) 127, (byte) 0, (byte) 0, (byte) 1};
            }
        } else {
            System.out.println("Gateway IP was not specified. default : " + ServerConstants.Gateway_IP);
        }

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("UPDATE accounts SET loggedin = 0");
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            throw new RuntimeException("[EXCEPTION] Please check if the SQL server is active.");
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
        }

        Start ld = new Start();
        try {
            LoadingThread();
        } catch (Exception ex) {
            Logger.getLogger(Start.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Initializes rotating burning hunting fields (period is configured in BurningHuntingFieldManager.REFRESH_INTERVAL_MILLIS).
        BurningHuntingFieldManager.init();

        System.out.println("[Loading Login]");
        try {
            LoginServer.run_startup_configurations();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("[Login Initialized]");

        System.out.println("[Loading Channel]");
        try {
            ChannelServer.startChannel_Main();
        } catch (Exception e) {
            throw new RuntimeException();
        }
        System.out.println("[Channel Initialized]");

        System.out.println("[Loading CS]");
        try {
            CashShopServer.run_startup_configurations();
        } catch (Exception e) {
            throw new RuntimeException();
        }

        System.out.println("[CS Initialized]");

        //threads.
        Timer.CheatTimer.getInstance().register(AutobanManager.getInstance(), 60000L);
        Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown()));
        World.registerRespawn();
        ShutdownServer.registerMBean();
        // 서버 최적화 CPU 샘플러 << 지랄 개구라 메모리누수 원인
        // 에녹 : 헬지와 셈플러는 거르고봅니다.

        //CPUSampler.getInstance().start();
        // 메모리 최적화 가비지 콜렉션
        Start.startGC(System.currentTimeMillis());
        PlayerNPC.loadAll();// touch - so we see database problems early...
        try {
            AuthServer.start();
            GMServer.start();
        } catch (Exception e) {
            throw new RuntimeException();
        }
        LoginServer.setOn(); //now or later
        System.out.println("[Fully Initialized in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds]");
        //AutoShutdownScheduler.getInstance().start(); // 서버 자동 리붓 기능
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long time = cal.getTimeInMillis();
        long schedulewait = 0;
        if (time > System.currentTimeMillis()) {
            schedulewait = time - System.currentTimeMillis();
        } else {
            schedulewait = time + 86400000L - System.currentTimeMillis();
        }
        if (schedulewait < 3600000) {
            schedulewait += 86400000L;
        }
        Timer.WorldTimer.getInstance().register(MapleCharacter::initDailyQuestBonus, schedulewait);
        
        Calendar goldrichquestCal = Calendar.getInstance();
        goldrichquestCal.set(Calendar.HOUR_OF_DAY, 0);
        goldrichquestCal.set(Calendar.MINUTE, 0);
        goldrichquestCal.set(Calendar.SECOND, 0);
        goldrichquestCal.set(Calendar.MILLISECOND, 0);
        if (goldrichquestCal.getTimeInMillis() <= System.currentTimeMillis()) {
            goldrichquestCal.add(Calendar.DAY_OF_MONTH, 1);
        }
        long goldrichquestMidnightDelay = goldrichquestCal.getTimeInMillis() - System.currentTimeMillis();
        Timer.WorldTimer.getInstance().register(MapleQuest::resetGoldrichquestDailyLimit, 86400000L, goldrichquestMidnightDelay);

        Timer.WorldTimer.getInstance().register(RankingWorker::run, 30 * 60 * 1000L);
        new MemoryUsageWatcher(88).start();
        new DeadLockDetector(60, DeadLockDetector.RESTART).start();
    }

    public static class Shutdown implements Runnable {

        public void run() {
            ShutdownServer.getInstance().run();
            ShutdownServer.getInstance().run();
        }
    }
    
    public static final void LoadingThread() throws Exception {
        Start start = new Start();
        start.ThreadLoader();
    }

    public final void ThreadLoader() throws InterruptedException {
        World.init();
        WorldTimer.getInstance().start();
        EtcTimer.getInstance().start();
        MapTimer.getInstance().start();
        CloneTimer.getInstance().start();
        EventTimer.getInstance().start();
        BuffTimer.getInstance().start();
        PingTimer.getInstance().start();

        LoadingThread WorldLoader = new LoadingThread(new Runnable() {
            public void run() {
                MapleGuildRanking.getInstance().load();
                MapleGuild.loadAll();
            }
        }, "WorldLoader", this);

        LoadingThread MarriageLoader = new LoadingThread(new Runnable() {
            public void run() {
                MarriageManager.getInstance();
            }
        }, "MarriageLoader", this);

        LoadingThread MedalRankingLoader = new LoadingThread(new Runnable() {
            public void run() {
                MedalRanking.loadAll();
            }
        }, "MedalRankingLoader", this);

        LoadingThread FamilyLoader = new LoadingThread(new Runnable() {
            public void run() {
                MapleFamily.loadAll();
            }
        }, "FamilyLoader", this);

        LoadingThread QuestLoader = new LoadingThread(new Runnable() {
            public void run() {
                MapleLifeFactory.loadQuestCounts();
                MapleQuest.initQuests();
            }
        }, "QuestLoader", this);

        LoadingThread ProviderLoader = new LoadingThread(new Runnable() {
            public void run() {
                MapleItemInformationProvider.getInstance().runEtc();
            }
        }, "ProviderLoader", this);

        LoadingThread MonsterLoader = new LoadingThread(new Runnable() {
            public void run() {
                MapleMonsterInformationProvider.getInstance().load();
            }
        }, "MonsterLoader", this);

        LoadingThread ItemLoader = new LoadingThread(new Runnable() {
            public void run() {
                MapleItemInformationProvider.getInstance().runItems();
            }
        }, "ItemLoader", this);

        LoadingThread SkillFactoryLoader = new LoadingThread(new Runnable() {
            public void run() {
                SkillFactory.load();
            }
        }, "SkillFactoryLoader", this);

        LoadingThread BasicLoader = new LoadingThread(new Runnable() {
            public void run() {
                LoginInformationProvider.getInstance();
                RandomRewards.load();
                RandomRewards.loadGachaponRewardFromINI("ini/gachapon.ini");
                MapleOxQuizFactory.getInstance();
                MapleCarnivalFactory.getInstance();
                MobSkillFactory.getInstance();
                SpeedRunner.loadSpeedRuns();
                MinervaOwlSearchTop.getInstance().loadFromFile();
                CashItemSaleRank.setUp();
            }
        }, "BasicLoader", this);

        LoadingThread MIILoader = new LoadingThread(new Runnable() {
            public void run() {
                MapleInventoryIdentifier.getInstance();
            }
        }, "MIILoader", this);

        LoadingThread CashItemLoader = new LoadingThread(new Runnable() {
            public void run() {
                CashItemFactory.getInstance().initialize();
            }
        }, "CashItemLoader", this);

        LoadingThread BgmLoader = new LoadingThread(new Runnable() {
            public void run() {
                MapleBgmProvider.load();
            }
        }, "BgmLoader", this);

        LoadingThread[] LoadingThreads = {WorldLoader, FamilyLoader, QuestLoader, ProviderLoader, SkillFactoryLoader, BasicLoader, CashItemLoader, MIILoader, MonsterLoader, ItemLoader, MarriageLoader, MedalRankingLoader, BgmLoader};
        TotalLoadingThreads = LoadingThreads.length;

        for (Thread t : LoadingThreads) {
            t.start();
        }
        synchronized (this) {
            wait();
        }
        while (CompletedLoadingThreads.get() != TotalLoadingThreads) {
            synchronized (this) {
                wait();
            }
        }
        System.out.println("[Loading] Caching Quest Item Information...");
        MapleItemInformationProvider.getInstance().runQuest();
        System.out.println("[LoadingComplete] Cached Quest Item Information...");
    }

    private static class LoadingThread extends Thread {

        protected String LoadingThreadName;

        private LoadingThread(Runnable r, String t, Object o) {
            super(new NotifyingRunnable(r, o, t));
            LoadingThreadName = t;
        }

        @Override
        public synchronized void start() {
            System.out.println("[Loading...] Started " + LoadingThreadName + " Thread");
            super.start();
        }
    }

    private static class NotifyingRunnable implements Runnable {

        private String LoadingThreadName;
        private long StartTime;
        private Runnable WrappedRunnable;
        private final Object ToNotify;

        private NotifyingRunnable(Runnable r, Object o, String name) {
            WrappedRunnable = r;
            ToNotify = o;
            LoadingThreadName = name;
        }

        public void run() {
            StartTime = System.currentTimeMillis();
            WrappedRunnable.run();
            System.out.println("[Loading Completed] " + LoadingThreadName + " | Completed in " + (System.currentTimeMillis() - StartTime) + " Milliseconds. (" + (CompletedLoadingThreads.get() + 1) + "/" + TotalLoadingThreads + ")");
            synchronized (ToNotify) {
                CompletedLoadingThreads.incrementAndGet();
                ToNotify.notify();
            }
        }
    }
}
