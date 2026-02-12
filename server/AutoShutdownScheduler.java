package server;

import client.MapleCharacter;
import handling.channel.ChannelServer;
import handling.world.World;
import server.Timer.WorldTimer;
import server.marriage.MarriageManager;
import server.shops.MinervaOwlSearchTop;
import tools.MaplePacketCreator;

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;

public class AutoShutdownScheduler {

    private static final AutoShutdownScheduler instance = new AutoShutdownScheduler();

    public static AutoShutdownScheduler getInstance() {
        return instance;
    }

    private ScheduledFuture<?> task = null;

    private static Thread shutdownThread = null;

    private int last0500_30m = -1, last0500_5m = -1, last0500_save = -1, last0500_shutdown = -1;
    private int last1700_30m = -1, last1700_5m = -1, last1700_save = -1, last1700_shutdown = -1;

    private int last0000_reset = -1;
    
    private int lastMidnightResetDay = -1;
    
    public void start() {
        if (task != null) {
            return;
        }
        // 30초마다 체크(분 단위 이벤트 놓치지 않게)
        task = WorldTimer.getInstance().register(this::tick, 30_000L);
        System.out.println("[AutoShutdownScheduler] enabled (05:00 / 17:00)");
    }

    private void tick() {
        try {
            Calendar now = Calendar.getInstance();
            int dayKey = now.get(Calendar.YEAR) * 1000 + now.get(Calendar.DAY_OF_YEAR);

            int h = now.get(Calendar.HOUR_OF_DAY);
            int m = now.get(Calendar.MINUTE);

            
            if (h == 0 && m == 0 && last0000_reset != dayKey) {
            DailyQuestStatusCustomDataResetter.resetAtMidnight();
            last0000_reset = dayKey;
            }
            
            // ---- 05:00 종료 스케줄 ----
            if (h == 4 && m == 30 && last0500_30m != dayKey) {
                notice("서버가 30분 후(05:00)에 재시작됩니다.");
                last0500_30m = dayKey;
            }
            if (h == 4 && m == 55 && last0500_5m != dayKey) {
                notice("서버가 5분 후(05:00)에 재시작됩니다. 게임을 종료해 주세요.");
                last0500_5m = dayKey;
            }
            if (h == 4 && m == 59 && last0500_save != dayKey) {
                notice("서버가 1분 후(05:00)에 재시작됩니다. 게임을 종료해 주세요.");
                saveAllLikeAdminCommand();
                last0500_save = dayKey;
            }
            if (h == 5 && m == 0 && last0500_shutdown != dayKey) {
                shutdownLikeAdminCommand();
                last0500_shutdown = dayKey;
            }

            // ---- 17:00 종료 스케줄 ----
            if (h == 16 && m == 30 && last1700_30m != dayKey) {
                notice("서버가 30분 후(17:00)에 재시작됩니다.");
                last1700_30m = dayKey;
            }
            if (h == 16 && m == 55 && last1700_5m != dayKey) {
                notice("서버가 5분 후(17:00)에 재시작됩니다. 게임을 종료해 주세요.");
                last1700_5m = dayKey;
            }
            if (h == 16 && m == 59 && last1700_save != dayKey) {
                notice("서버가 1분 후(17:00)에 재시작됩니다. 게임을 종료해 주세요.");
                saveAllLikeAdminCommand();
                last1700_save = dayKey;
            }
            if (h == 17 && m == 0 && last1700_shutdown != dayKey) {
                shutdownLikeAdminCommand();
                last1700_shutdown = dayKey;
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void notice(String msg) {
        World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(0, msg));
    }

    private void saveAllLikeAdminCommand() {
        // User Data Save Start
        for (ChannelServer ch : ChannelServer.getAllInstances()) {
            for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                chr.saveToDB(false, false);
            }
        }
        // User Data Save End

        // Server Data Save Start
        World.Guild.save();
        World.Alliance.save();
        World.Family.save();
        MarriageManager.getInstance().saveAll();
        MinervaOwlSearchTop.getInstance().saveToFile();
        MedalRanking.saveAll();
        // Server Data Save End
    }

    private void shutdownLikeAdminCommand() {
        notice("서버를 종료합니다.");

        if (shutdownThread == null || !shutdownThread.isAlive()) {
            shutdownThread = new Thread(ShutdownServer.getInstance());
            ShutdownServer.getInstance().shutdown();
            shutdownThread.start();
        }
    }
}
