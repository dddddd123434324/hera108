package server;

import client.MapleCharacter;
import client.MapleQuestStatus;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import server.quest.MapleQuest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.StringJoiner;

/**
 * 매일 00:00(서버시간)에 특정 퀘스트들의 queststatus.customData를 초기화합니다.
 *
 * 중요:
 * - DB만 UPDATE하면 온라인 유저는 이후 saveToDB() 시 메모리 값으로 다시 덮어쓸 수 있음
 *   -> 온라인 유저의 MapleQuestStatus.customData도 같이 초기화
 */
public final class DailyQuestStatusCustomDataResetter {

    /**
     * 초기화 대상 questId 목록
     * TODO: 여기에 원하는 퀘스트 ID들을 넣으세요.
     */
    private static final int[] TARGET_QUEST_IDS = {
250925, 260126,
    };

    private DailyQuestStatusCustomDataResetter() {
    }

    /** 자정 작업 엔트리 */
    public static void resetAtMidnight() {
        resetQuestStatusCustomData(TARGET_QUEST_IDS);
    }

    /** 지정 questId들의 queststatus.customData를 빈 문자열로 초기화 */
    public static void resetQuestStatusCustomData(final int[] questIds) {
        if (questIds == null || questIds.length == 0) {
            return;
        }

        // 1) 온라인 유저 메모리 초기화 (DB 덮어쓰기 방지)
        resetOnlineCharacters(questIds);

        // 2) DB 전체(오프라인 포함) 초기화
        resetDatabase(questIds);
    }

    private static void resetOnlineCharacters(final int[] questIds) {
        try {
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    for (int questId : questIds) {
                        MapleQuestStatus qs = chr.getQuestNoAdd(MapleQuest.getInstance(questId));
                        if (qs != null) {
                            qs.setCustomData(""); // 초기화
                        }
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[DailyQuestStatusCustomDataResetter] online reset failed");
            t.printStackTrace();
        }
    }

    private static void resetDatabase(final int[] questIds) {
        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = DatabaseConnection.getConnection();

            final String inClause = makeInClause(questIds.length);
            ps = con.prepareStatement(
                    "UPDATE queststatus SET customData = '' WHERE quest IN (" + inClause + ")"
            );

            bindInts(ps, questIds);
            final int updated = ps.executeUpdate();

            System.out.println("[DailyQuestStatusCustomDataResetter] reset done. rows=" + updated);

        } catch (Exception e) {
            System.err.println("[DailyQuestStatusCustomDataResetter] db reset failed");
            e.printStackTrace();
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception ignore) {}
            try { if (con != null) con.close(); } catch (Exception ignore) {}
        }
    }

    private static String makeInClause(final int n) {
        StringJoiner sj = new StringJoiner(",");
        for (int i = 0; i < n; i++) {
            sj.add("?");
        }
        return sj.toString();
    }

    private static void bindInts(final PreparedStatement ps, final int[] values) throws Exception {
        for (int i = 0; i < values.length; i++) {
            ps.setInt(i + 1, values[i]);
        }
    }
}
