/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server;

import database.DatabaseConnection;
import tools.FileoutputUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class RankingWorker {

    private final static Map<Integer, List<RankingInformation>> rankings = new HashMap<Integer, List<RankingInformation>>();
    private static RankingWorker instance = new RankingWorker();
    private static List<RankingInformation> ranks = new LinkedList<RankingInformation>();
    private final static Map<String, Integer> jobCommands = new LinkedHashMap<>();

    public final static Integer getJobCommand(final String job) {
        return jobCommands.get(job);
    }
    public static RankingWorker getInstance() {
        return instance;
    }
    public static List<RankingInformation> getRank() {
        return ranks;
    }

    public final static Map<String, Integer> getJobCommands() {
        return jobCommands;
    }

    public final static List<RankingInformation> getRankingInfo(final int job) {
        return rankings.get(job);
    }

    public final static void run() {
        System.out.println("Loading Rankings::");
        long startTime = System.currentTimeMillis();
        loadJobCommands();
        try {
            updateRanking();
        } catch (Exception ex) {
            ex.printStackTrace();
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
            System.err.println("Could not update rankings");
        }
        System.out.println("Done loading Rankings in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds :::"); //keep
    }

    private static void updateRanking() throws Exception {
        StringBuilder sb = new StringBuilder("SELECT c.id, c.job, c.exp, c.level, c.name, c.jobRank, c.rank, c.fame");
        sb.append(" FROM characters AS c LEFT JOIN accounts AS a ON c.accountid = a.id WHERE c.gm = 0 AND a.banned = 0 AND c.level >= 20");
        sb.append(" ORDER BY c.level DESC , c.exp DESC , c.rank ASC , c.fame DESC");

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        PreparedStatement charSelect = null;
        try {
            con = DatabaseConnection.getConnection();
            charSelect = con.prepareStatement(sb.toString());
            rs = charSelect.executeQuery();
            ps = con.prepareStatement("UPDATE characters SET jobRank = ?, jobRankMove = ?, rank = ?, rankMove = ? WHERE id = ?");
            int rank = 0; //for "all"
            final Map<Integer, Integer> rankMap = new LinkedHashMap<Integer, Integer>();
            for (int i : jobCommands.values()) {
                rankMap.put(i, 0); //job to rank
                rankings.put(i, new LinkedList<>());
            }
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int job = rs.getInt("job");
                int jcmd = job / 10;
                if (!rankMap.containsKey(jcmd)) { //not supported.
                    continue;
                }
                int jobRank = rankMap.get(jcmd) + 1;
                rankMap.put(jcmd, jobRank);
                rank++;
                int dJobRank = rs.getInt("jobRank") - jobRank;
                int dRank = rs.getInt("rank") - rank;
                int level = rs.getInt("level");
                int exp = rs.getInt("exp");
                int fame = rs.getInt("fame");
                rankings.get(-1).add(new RankingInformation(name, job, level, exp, rank, dRank, fame));
                rankings.get(jcmd).add(new RankingInformation(name, job, level, exp, jobRank, dJobRank, fame));
                ps.setInt(1, jobRank);
                ps.setInt(2, dJobRank);
                ps.setInt(3, rank);
                ps.setInt(4, dRank);
                ps.setInt(5, id);
                ps.addBatch(); 
            }
            ps.executeBatch(); //Batch update should be faster.
        } catch (Exception e) {
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
            if (charSelect != null) {
                try {
                    charSelect.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
        }
        ranks.clear();
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM characters ORDER BY `level` DESC LIMIT 200");
            rs = ps.executeQuery();

            while (rs.next()) {
                //String name, int job, int level, int exp, int rank, int dRank, int fame
                final RankingInformation rank = new RankingInformation(
                        rs.getString("name"),
                        rs.getInt("job"),
                        rs.getInt("level"),
                        rs.getInt("exp"),
                        rs.getInt("rank"),
                        rs.getInt("fame"));

                ranks.add(rank);
            }
        } catch (SQLException e) {
            System.err.println("Error handling guildRanking");
            e.printStackTrace();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public final static void loadJobCommands() {
        //job / 10
        jobCommands.put("종합", -1);

        jobCommands.put("초보자", 0);

        jobCommands.put("검사", 10);
        jobCommands.put("히어로 계열", 11);
        jobCommands.put("팔라딘 계열", 12);
        jobCommands.put("다크나이트 계열", 13);

        jobCommands.put("매지션", 20);
        jobCommands.put("아크메이지(불,독) 계열", 21);
        jobCommands.put("아크메이지(썬,콜) 계열", 22);
        jobCommands.put("비숍 계열", 23);

        jobCommands.put("아처", 30);
        jobCommands.put("보우마스터 계열", 31);
        jobCommands.put("신궁 계열", 32);

        jobCommands.put("로그", 40);
        jobCommands.put("나이트로드 계열", 41);
        jobCommands.put("섀도어 계열", 42);
        jobCommands.put("듀얼블레이드 계열", 43);

        jobCommands.put("해적", 50);
        jobCommands.put("바이퍼 계열", 51);
        jobCommands.put("캡틴 계열", 52);
        
        jobCommands.put("노블레스", 100);
        
        jobCommands.put("소울마스터", 110);
        jobCommands.put("소울마스터", 111);
        jobCommands.put("플레임위자드", 120);
        jobCommands.put("플레임위자드", 121);
        jobCommands.put("윈드브레이커", 130);
        jobCommands.put("윈드브레이커", 131);
        jobCommands.put("나이트워커", 140);
        jobCommands.put("나이트워커", 141);
        jobCommands.put("스트라이커", 150);
        jobCommands.put("스트라이커", 151);
        
        jobCommands.put("레전드", 200);
        
        jobCommands.put("아란", 210);
        jobCommands.put("아란", 211);
        jobCommands.put("에반", 220);
        jobCommands.put("에반", 221);
        
        jobCommands.put("시티즌", 300);
        
        jobCommands.put("배틀메이지", 320);
        jobCommands.put("배틀메이지", 321);
        jobCommands.put("와일드헌터", 330);
        jobCommands.put("와일드헌터", 331);
        jobCommands.put("메카닉", 350);
        jobCommands.put("메카닉", 351);
    }

    public static class RankingInformation {

        public String toString, name;
        public int rank, level, job;

        public RankingInformation(String name, int job, int level, int exp, int rank, int dRank, int fame) {
            this.rank = rank;
//            final StringBuilder builder = new StringBuilder("Rank ");
//            builder.append(rank);
//            builder.append(" : ");
//            builder.append(name);
//            builder.append(" - Level ");
//            builder.append(level);
//            builder.append(" ");
//            builder.append(MapleCarnivalChallenge.getJobNameById(job));
//            builder.append(" | ");
//            builder.append(exp);
//            builder.append(" EXP, ");
//            builder.append(fame);
//            builder.append(" Fame");
//            this.toString = builder.toString(); //Rank 1 : KiDALex - Level 200 Blade Master | 0 EXP, 30000 Fame
            final StringBuilder builder = new StringBuilder("#e");
            builder.append(rank);
            switch (rank) {
                case 1:
                    builder.append("st");
                    break;
                case 2:
                    builder.append("nd");
                    break;
                case 3:
                    builder.append("rd");
                    break;
                default:
                    builder.append("th");
                    break;
            }
            builder.append(". #n");
            builder.append(name);
            builder.append(" Lv.");
            builder.append(level);
            builder.append(" ");
            builder.append(MapleCarnivalChallenge.getJobNameById(job));
            builder.append(" ");
            if (dRank > 0) {
                builder.append("#r(▲").append(dRank).append(")#k");
            } else if (dRank == 0) {
                builder.append("#d(■0)#k");
            } else { //if (dRank < 0)
                builder.append("#b(▼").append(-dRank).append(")#k");
            }
            this.toString = builder.toString(); //#e1st. #n캐릭터닉네임 Lv.111 드래곤나이트 #r(▲1)#k\r\n
        }


        public RankingInformation(String name, int job, int level, int exp, int rank, int fame) {
            this.rank = rank;
            this.name = name;
            this.level = level;
            this.job = job;
        }

        @Override
        public String toString() {
            return toString;
        }

        public int getJob() {
            return job;
        }

        public int getRank() {
            return rank;
        }

        public int getLevel() {
            return level;
        }

        public String getName() {
            return name;
        }
    }
}
