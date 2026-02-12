/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import client.MapleCharacter;
import handling.world.MaplePartyCharacter;

import java.lang.ref.WeakReference;

/**
 * TODO : Make this a function for NPC instead.. cleaner
 *
 * @author Rob
 */
public class MapleCarnivalChallenge {

    WeakReference<MapleCharacter> challenger;
    String challengeinfo = "";

    public MapleCarnivalChallenge(MapleCharacter challenger) {
        this.challenger = new WeakReference<MapleCharacter>(challenger);
        challengeinfo += "#b";
        for (MaplePartyCharacter pc : challenger.getParty().getMembers()) {
            MapleCharacter c = challenger.getMap().getCharacterById(pc.getId());
            if (c != null) {
                challengeinfo += (c.getName() + " / 레벨 : " + c.getLevel() + " / 직업 : " + getJobNameById(c.getJob()) + "\r\n");
            }
        }
        challengeinfo += "#k\r\n";
    }

    public MapleCharacter getChallenger() {
        return challenger.get();
    }

    public String getChallengeInfo() {
        return challengeinfo;
    }

    public static final String getJobNameById(int job) {
        switch (job) {
            case 0:
                return "초보자";

            case 100:
                return "검사";
            case 110:
                return "파이터";
            case 111:
                return "크루세이더";
            case 112:
                return "히어로";
            case 120:
                return "페이지";
            case 121:
                return "나이트";
            case 122:
                return "팔라딘";
            case 130:
                return "스피어맨";
            case 131:
                return "용기사";
            case 132:
                return "다크나이트";

            case 200:
                return "매지션";
            case 210:
                return "위자드(불,독)";
            case 211:
                return "메이지(불,독)";
            case 212:
                return "아크메이지(불,독)";
            case 220:
                return "위자드(썬,콜)";
            case 221:
                return "메이지(썬,콜)";
            case 222:
                return "아크메이지(썬,콜)";
            case 230:
                return "클레릭";
            case 231:
                return "프리스트";
            case 232:
                return "비숍";

            case 300:
                return "아처";
            case 310:
                return "헌터";
            case 311:
                return "레인저";
            case 312:
                return "보우마스터";
            case 320:
                return "사수";
            case 321:
                return "저격수";
            case 322:
                return "신궁";

            case 400:
                return "로그";
            case 410:
                return "어쌔신";
            case 411:
                return "허밋";
            case 412:
                return "나이트로드";
            case 420:
                return "시프";
            case 421:
                return "시프마스터";
            case 422:
                return "섀도어";

            case 500:
                return "해적";
            case 510:
                return "인파이터";
            case 511:
                return "버커니어";
            case 512:
                return "바이퍼";
            case 520:
                return "건슬링거";
            case 521:
                return "발키리";
            case 522:
                return "캡틴";

            case 900:
                return "운영자";

            case 1000:
                return "노블레스";

            case 1100:
            case 1110:
            case 1111:
            case 1112:
                return "소울마스터";

            case 1200:
            case 1210:
            case 1211:
            case 1212:
                return "플레임위자드";

            case 1300:
            case 1310:
            case 1311:
            case 1312:
                return "윈드브레이커";

            case 1400:
            case 1410:
            case 1411:
            case 1412:
                return "나이트워커";

            case 1500:
            case 1510:
            case 1511:
            case 1512:
                return "스트라이커";

                
            case 2000:
                return "레전드";
                
            case 2100:
            case 2110:
            case 2111:
            case 2112:
                return "아란";
                
            case 2001:
            case 2200:
            case 2210:
            case 2211:
            case 2212:
            case 2213:
            case 2214:
            case 2215:
            case 2216:
            case 2217:
            case 2218:
                return "에반";
                
            case 3000:
                return "시티즌";

            case 3200:
            case 3210:
            case 3211:
            case 3212:
                return "배틀메이지";

            case 3300:
            case 3310:
            case 3311:
            case 3312:
                return "와일드헌터";

            case 3500:
            case 3510:
            case 3511:
            case 3512:
                return "메카닉";

            default:
                return "?";
        }
    }

    public static final String getJobBasicNameById(int job) {
        switch (job) {
            case 0:
                return "초보자";

            case 100:
            case 110:
            case 111:
            case 112:
            case 120:
            case 121:
            case 122:
            case 130:
            case 131:
            case 132:
                return "전사";

            case 200:
            case 210:
            case 211:
            case 212:
            case 220:
            case 221:
            case 222:
            case 230:
            case 231:
            case 232:
                return "마법사";

            case 300:
            case 310:
            case 311:
            case 312:
            case 320:
            case 321:
            case 322:
                return "궁수";

            case 400:
            case 410:
            case 411:
            case 412:
            case 420:
            case 421:
            case 422:
                return "도적";

            case 500:
            case 510:
            case 511:
            case 512:
            case 520:
            case 521:
            case 522:
                return "해적";

            case 900:
                return "운영자";

            case 1000:
                return "노블레스";

            case 1100:
            case 1110:
            case 1111:
                return "소울마스터";

            case 1200:
            case 1210:
            case 1211:
                return "플레임위자드";

            case 1300:
            case 1310:
            case 1311:
                return "윈드브레이커";

            case 1400:
            case 1410:
            case 1411:
                return "나이트워커";

            case 1500:
            case 1510:
            case 1511:
                return "블래스터";

            default:
                return "?";
        }
    }
}
