/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.extract;

import database.DatabaseConnection;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 티썬
 */
public class MapleShopParser {

    public static void main(String[] args) throws Exception {
        DatabaseConnection.init();
        File fr = new File("imgs");
        MapleDataProvider pro = MapleDataProviderFactory.getDataProvider(fr);
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement del1 = con.prepareStatement("TRUNCATE `shops`");
        PreparedStatement del2 = con.prepareStatement("TRUNCATE `shopitems`");
        PreparedStatement del3 = con.prepareStatement("ALTER TABLE  `shops` AUTO_INCREMENT =1");
        //
        del1.executeUpdate();
        del2.executeUpdate();
        del3.executeUpdate();
        del1.close();
        del2.close();
        del3.close();
        int shopid = 1;
        PreparedStatement ps1 = con.prepareStatement("INSERT INTO shops (`npcid`) VALUES (?)");
        PreparedStatement ps2 = con.prepareStatement("INSERT INTO shopitems (shopid, itemid, price, position, max, expiretime, level) VALUES (?, ?, ?, ?, ?, ?, ?)");
        //PreparedStatement ps3 = con.prepareStatement("INSERT INTO shopitems (shopid, itemid, price, position, max) VALUES (?, ?, ?, ?, ?)");

        System.setProperty("net.sf.odinms.wzpath", "wz");
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        ii.runItems();
        ii.runEtc();
        MapleLifeFactory.loadQuestCounts();

        for (MapleData dd : pro.getData("NpcShop.img")) {
            int npcid = Integer.parseInt(dd.getName());
            try {
                if (MapleLifeFactory.getNPC(npcid) == null) {
                    System.out.println(npcid + " does not exists NPC.. continue.");
                    continue;
                }
            } catch (Exception e) {
                System.out.println(npcid + " does not exists NPC.. continue.");
                continue;
            }
            try {
                ps1.setInt(1, npcid);
                for (MapleData sp : dd.getChildren()) {
                    int i = Integer.parseInt(sp.getName()) * 10;
                    int item = MapleDataTool.getInt("item", sp);
                    if (item / 10000 == 207 && item != 2070000) {
                        continue;
                    }
                    if (!ii.itemExists(item)) {
                        System.err.println(item + " Item does not exists.. continue.");
                        continue;
                    }
                    int price = MapleDataTool.getInt("price", sp, -1);
                    if (price == -1) {
                        continue;
                    }
                    ps2.setInt(1, shopid);
                    ps2.setInt(2, item);
                    ps2.setInt(3, price);
                    ps2.setInt(4, i);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();
                    if (item == 2060000) { //화살 2천개
                        ps2.setInt(1, shopid);
                        ps2.setInt(2, item);
                        ps2.setInt(3, 1600);
                        ps2.setInt(4, i + 11);
                        ps2.setInt(5, 2000);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();
                    }
                    if (item == 2061000) { //석궁 화살 2천개
                        ps2.setInt(1, shopid);
                        ps2.setInt(2, item);
                        ps2.setInt(3, 1600);
                        ps2.setInt(4, i + 2);
                        ps2.setInt(5, 2000);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();
                    }
                    if (item == 2070000) { //불릿
                        ps2.setInt(1, shopid);
                        ps2.setInt(2, 2330000);
                        ps2.setInt(3, 600);
                        ps2.setInt(4, i + 1);
                        ps2.setInt(5, 0);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();
                    }
                    if (item == 2120008) { //땅콩
                        ps2.setInt(1, shopid);
                        ps2.setInt(2, 4170023);
                        ps2.setInt(3, 200);
                        ps2.setInt(4, i + 1);
                        ps2.setInt(5, 0);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();
                    }
                    if (item == 2044700) {
                        ps2.setInt(1, shopid);
                        ps2.setInt(2, 2044800); //너클공격력
                        ps2.setInt(3, 70000);
                        ps2.setInt(4, i + 1);
                        ps2.setInt(5, 0);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();

                        ps2.setInt(1, shopid);
                        ps2.setInt(2, 2044900); //건공격력
                        ps2.setInt(3, 70000);
                        ps2.setInt(4, i + 2);
                        ps2.setInt(5, 0);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();

                        ps2.setInt(1, shopid);
                        ps2.setInt(2, 2043400); //블레이드공격력
                        ps2.setInt(3, 70000);
                        ps2.setInt(4, i + 3);
                        ps2.setInt(5, 0);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();
                    }
                    if (item == 1472001) { //스틸티탄즈가 있는 하위상점에 30제
                        ps2.setInt(1, shopid);
                        ps2.setInt(2, 1482004); //프라임 핸즈
                        ps2.setInt(3, 150000);
                        ps2.setInt(4, i + 100);
                        ps2.setInt(5, 0);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();

                        ps2.setInt(1, shopid);
                        ps2.setInt(2, 1492004); //콜드 마인드
                        ps2.setInt(3, 150000);
                        ps2.setInt(4, i + 200);
                        ps2.setInt(5, 0);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();
                    }
                    if (item == 1472004) { //브론즈 이고르가 있는 중위상점 35제 추가
                        ps2.setInt(1, shopid);
                        ps2.setInt(2, 1482005); //데빌클로
                        ps2.setInt(3, 350000);
                        ps2.setInt(4, i + 101);
                        ps2.setInt(5, 0);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();

                        ps2.setInt(1, shopid);
                        ps2.setInt(2, 1492005); //슈팅 스타
                        ps2.setInt(3, 350000);
                        ps2.setInt(4, i + 201);
                        ps2.setInt(5, 0);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();
                    }
                    if (item == 1472007) { //메바가 있는 상위상점 40제 추가
                        ps2.setInt(1, shopid);
                        ps2.setInt(2, 1482006); //데빌클로
                        ps2.setInt(3, 225000);
                        ps2.setInt(4, i + 102);
                        ps2.setInt(5, 0);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();

                        ps2.setInt(1, shopid);
                        ps2.setInt(2, 1492006); //슈팅 스타
                        ps2.setInt(3, 225000);
                        ps2.setInt(4, i + 202);
                        ps2.setInt(5, 0);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();
                    }
                    if (item == 1072171) { //불릿
                        ps2.setInt(1, shopid);
                        ps2.setInt(2, 1072338);
                        ps2.setInt(3, 30000);
                        ps2.setInt(4, i + 1);
                        ps2.setInt(5, 0);
                        ps2.setInt(6, 0);
                        ps2.setInt(7, 0);
                        ps2.addBatch();
                    }
                }
                //Shop HardCoding
                if (isFancyGoodsShop(shopid)) {
                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 2190000);//거짓말 탐지기
                    ps2.setInt(3, 10000);
                    ps2.setInt(4, 100 * 10 + 1);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();

                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 2460000);//돋보기
                    ps2.setInt(3, 2000);
                    ps2.setInt(4, 100 * 10 + 2);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();

                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 2460001);//돋보기
                    ps2.setInt(3, 8000);
                    ps2.setInt(4, 100 * 10 + 3);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();

                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 2460002);//돋보기
                    ps2.setInt(3, 32000);
                    ps2.setInt(4, 100 * 10 + 4);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();

                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 2460003);//돋보기
                    ps2.setInt(3, 128000);
                    ps2.setInt(4, 100 * 10 + 5);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();
                }
                if (armoryShopGrade(shopid) == 1) { //해적 30제 방어구
                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 1002622); //화이트 오시니아 캡
                    ps2.setInt(3, 128000);
                    ps2.setInt(4, 1000 * 10 + 1);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();

                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 1052107); //브라운 폴라아드
                    ps2.setInt(3, 128000);
                    ps2.setInt(4, 1000 * 20 + 1);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();

                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 1072294); //브라운 폴티 부츠
                    ps2.setInt(3, 128000);
                    ps2.setInt(4, 1000 * 30 + 1);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();

                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 1082189); //옐로우 타르티스
                    ps2.setInt(3, 128000);
                    ps2.setInt(4, 1000 * 40 + 1);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();
                } else if (armoryShopGrade(shopid) == 2) { //해적 40제 방어구
                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 1002622); //화이트 오시니아 캡
                    ps2.setInt(3, 128000);
                    ps2.setInt(4, 1000 * 10 + 1);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();

                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 1052107); //브라운 폴라아드
                    ps2.setInt(3, 128000);
                    ps2.setInt(4, 1000 * 20 + 1);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();

                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 1072294); //브라운 폴티 부츠
                    ps2.setInt(3, 128000);
                    ps2.setInt(4, 1000 * 30 + 1);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();

                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 1082189); //옐로우 타르티스
                    ps2.setInt(3, 128000);
                    ps2.setInt(4, 1000 * 40 + 1);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();

                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 1002628); //레드 로얄미스티
                    ps2.setInt(3, 128000);
                    ps2.setInt(4, 1000 * 10 + 2);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();

                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 1052113); //레드 바르베이
                    ps2.setInt(3, 128000);
                    ps2.setInt(4, 1000 * 20 + 2);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();

                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 1072300); //브라운 레더크라그
                    ps2.setInt(3, 128000);
                    ps2.setInt(4, 1000 * 30 + 2);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();

                    ps2.setInt(1, shopid);
                    ps2.setInt(2, 1082195); //브라운 바르비
                    ps2.setInt(3, 128000);
                    ps2.setInt(4, 1000 * 40 + 2);
                    ps2.setInt(5, 0);
                    ps2.setInt(6, 0);
                    ps2.setInt(7, 0);
                    ps2.addBatch();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Shopid : " + npcid);
                return;
            }
            ps1.addBatch();
            shopid++;
        }
        ps1.executeBatch();
        ps2.executeBatch();

        //상점추가(1100001);
        //상점추가(1100002);
        copyShop(1, 2150001);//에레브 무기 상점(방어구 추가 해야함)
        copyShop(9, 2150002);//에델슈타인
        copyShop(9, 1055002);//커닝스퀘어잡화상점
        copyShop(9, 1052116);//커닝스퀘어잡화상점
        copyShop(1, 1100001);//에레브 무기 상점(방어구 추가 해야함)
        copyShop(9, 1100002);//에레브 잡화 상점
        copyShop(1, 1200001);//리엔 무기 상점(방어구 추가 해야함)
        copyShop(9, 1200002);//리엔 잡화 상점
        copyShop(9, 1301000);//버섯의성
        copyShop(9, 2130000);//커닝스퀘어잡화상점
        copyShop(9, 9000081);//황금사원 
        copyShop(9, 9090000);//묘묘

        //Shop HardCoding
        addItem(9090000, 9090000, 2022003, 1150, 141, 1, 0);
        addItem(9090000, 9090000, 2022000, 1600, 142, 1, 0);
        addItem(9090000, 9090000, 2001000, 1600, 143, 1, 0);
        addItem(9090000, 9090000, 2001001, 2200, 144, 1, 0);
        addItem(9090000, 9090000, 2001002, 4000, 145, 1, 0);
        addItem(9090000, 9090000, 2020012, 4400, 146, 1, 0);
        addItem(9090000, 9090000, 2020013, 5600, 147, 1, 0);
        addItem(9090000, 9090000, 2020014, 8100, 148, 1, 0);
        addItem(9090000, 9090000, 2020015, 10200, 149, 1, 0);

        //모건 1091000
        addShop(1091000);
        addItem(1091000, 1091000, 1492000, 3000, 100, 1, 0);
        addItem(1091000, 1091000, 1492001, 6000, 101, 1, 0);
        addItem(1091000, 1091000, 1492002, 10000, 102, 1, 0);
        addItem(1091000, 1091000, 1492003, 22000, 103, 1, 0);
        addItem(1091000, 1091000, 1492004, 50000, 104, 1, 0);
        addItem(1091000, 1091000, 1482000, 3000, 105, 1, 0);
        addItem(1091000, 1091000, 1482001, 6000, 106, 1, 0);
        addItem(1091000, 1091000, 1482002, 10000, 107, 1, 0);
        addItem(1091000, 1091000, 1482003, 20000, 108, 1, 0);
        addItem(1091000, 1091000, 1482004, 52000, 109, 1, 0);
        addItem(1091000, 1091000, 1442004, 24000, 110, 1, 0);
        addItem(1091000, 1091000, 1302007, 3000, 111, 1, 0);
        addItem(1091000, 1091000, 1322007, 6000, 112, 1, 0);

        //영채(방어구 추가 필요)
        addShop(1055000);
        addItem(1055000, 1055000, 1332000, 4000, 100, 1, 0);
        addItem(1055000, 1055000, 1332006, 7000, 101, 1, 0);
        addItem(1055000, 1055000, 1332002, 8000, 102, 1, 0);
        addItem(1055000, 1055000, 1332008, 10000, 103, 1, 0);
        addItem(1055000, 1055000, 1332013, 15000, 104, 1, 0);
        addItem(1055000, 1055000, 1332010, 22000, 105, 1, 0);
        addItem(1055000, 1055000, 1332004, 38000, 106, 1, 0);
        addItem(1055000, 1055000, 1332012, 40000, 107, 1, 0);
        addItem(1055000, 1055000, 1332009, 42000, 108, 1, 0);
        addItem(1055000, 1055000, 1342000, 15000, 109, 1, 0);
        addItem(1055000, 1055000, 1342001, 50000, 110, 1, 0);

        //캔디
        addShop(9270022);
        addItem(9270022, 9270022, 2000000, 50, 100, 1, 0);
        addItem(9270022, 9270022, 2000001, 160, 101, 1, 0);
        addItem(9270022, 9270022, 2000002, 320, 102, 1, 0);
        addItem(9270022, 9270022, 2000003, 200, 103, 1, 0);
        addItem(9270022, 9270022, 2000006, 620, 104, 1, 0);
        addItem(9270022, 9270022, 2001515, 500, 105, 1, 0);
        addItem(9270022, 9270022, 2001516, 400, 106, 1, 0);
        addItem(9270022, 9270022, 2001517, 500, 107, 1, 0);
        addItem(9270022, 9270022, 2001519, 500, 108, 1, 0);
        addItem(9270022, 9270022, 2001520, 500, 109, 1, 0);
        addItem(9270022, 9270022, 2022003, 1100, 110, 1, 0);
        addItem(9270022, 9270022, 2022000, 1650, 111, 1, 0);
        addItem(9270022, 9270022, 2001000, 3200, 112, 1, 0);
        addItem(9270022, 9270022, 2001001, 2300, 113, 1, 0);
        addItem(9270022, 9270022, 2001002, 4000, 113, 1, 0);
        addItem(9270022, 9270022, 2010000, 30, 114, 1, 0);
        addItem(9270022, 9270022, 2010002, 50, 115, 1, 0);
        addItem(9270022, 9270022, 2010001, 106, 116, 1, 0);
        addItem(9270022, 9270022, 2010003, 100, 117, 1, 0);
        addItem(9270022, 9270022, 2010004, 310, 118, 1, 0);
        addItem(9270022, 9270022, 2020028, 3000, 119, 1, 0);
        addItem(9270022, 9270022, 2050000, 200, 120, 1, 0);
        addItem(9270022, 9270022, 2050001, 200, 121, 1, 0);
        addItem(9270022, 9270022, 2050002, 300, 122, 1, 0);
        addItem(9270022, 9270022, 2050003, 500, 123, 1, 0);
        addItem(9270022, 9270022, 2030000, 400, 124, 1, 0);
        addItem(9270022, 9270022, 2060000, 1, 125, 1, 0);
        addItem(9270022, 9270022, 2061000, 1, 126, 1, 0);
        addItem(9270022, 9270022, 2070000, 500, 127, 1, 0);

        //라이우 무사
        addShop(9110001);
        addItem(9110001, 9110001, 1432009, 60000, 100, 1, 0);

        /*//벼루
        addShop(9000069);
        addItem(9000069, 9000069, 5510000, 3000000, 0, 1, 0);
        addItem(9000069, 9000069, 5520000, 5000000, 0, 1, 0);
        addItem(9000069, 9000069, 5520001, 10000000, 0, 1, 0);
        addCSItem(9000069, 9000069, 5121016, 0, 0, 4310007, 1, 1, 60 * 24 * 3, 0);
        addCSItem(9000069, 9000069, 5122000, 0, 1, 4310007, 1, 1, 60 * 24 * 3, 0);
        addCSItem(9000069, 9000069, 5530013, 0, 2, 4310007, 5, 1, 60 * 24 * 1, 0);
        addCSItem(9000069, 9000069, 5041001, 0, 3, 4310007, 5, 1, 60, 0);
        addCSItem(9000069, 9000069, 2049401, 0, 4, 4310007, 30, 1, 60 * 24 * 7, 0);
        addCSItem(9000069, 9000069, 2049400, 0, 5, 4310007, 50, 1, 60 * 24 * 7, 0);
        addCSItem(9000069, 9000069, 2049301, 0, 6, 4310007, 30, 1, 60 * 24 * 7, 0);
        addCSItem(9000069, 9000069, 2049300, 0, 7, 4310007, 50, 1, 60 * 24 * 7, 0);
        addCSItem(9000069, 9000069, 5530068, 0, 8, 4310007, 50, 1, 60 * 24 * 7, 0);
        addCSItem(9000069, 9000069, 2022462, 0, 9, 4310007, 50, 1, 60 * 24 * 3, 0);
        addCSItem(9000069, 9000069, 5211000, 0, 10, 4310007, 50, 1, 60 * 24, 0);

        addCSItem(9000069, 9000069, 5062000, 0, 20, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 5062000, 0, 21, 4310007, 20, 11, 0, 0);
        addCSItem(9000069, 9000069, 5062000, 0, 22, 4310007, 200, 110, 0, 0);
        addCSItem(9000069, 9000069, 5530034, 0, 23, 4310007, 30, 1, 0, 0);
        addCSItem(9000069, 9000069, 1122074, 0, 24, 4310007, 30, 1, 0, 0);
        addCSItem(9000069, 9000069, 1122075, 0, 25, 4310007, 50, 1, 0, 0);
        addCSItem(9000069, 9000069, 1122077, 0, 26, 4310007, 50, 1, 0, 0);
        addCSItem(9000069, 9000069, 2430144, 0, 27, 4310007, 50, 1, 0, 0);

//        addCSItem(9000069, 9000069, 4170023, 0, 8, 4310007, 100, 1, 0, 0);
//        addCSItem(9000069, 9000069, 5060003, 0, 9, 4310007, 100, 1, 0, 0);
        //연반
        addCSItem(9000069, 9000069, 1112400, 0, 30, 4310007, 50, 1, 0, 0);
        addCSItem(9000069, 9000069, 1112401, 0, 31, 4310007, 50, 1, 0, 0);

        addCSItem(9000069, 9000069, 1112405, 0, 32, 4310007, 300, 1, 0, 0);
        addCSItem(9000069, 9000069, 1112427, 0, 33, 4310007, 300, 1, 0, 0);
        addCSItem(9000069, 9000069, 1112428, 0, 34, 4310007, 300, 1, 0, 0);
        addCSItem(9000069, 9000069, 1112429, 0, 35, 4310007, 300, 1, 0, 0);
        addCSItem(9000069, 9000069, 1112445, 0, 36, 4310007, 300, 1, 0, 0);

        //연성서 
        addCSItem(9000069, 9000069, 2047300, 0, 90, 4310007, 10, 1, 0, 0);
        addCSItem(9000069, 9000069, 2047301, 0, 91, 4310007, 10, 1, 0, 0);
        addCSItem(9000069, 9000069, 2047302, 0, 92, 4310007, 10, 1, 0, 0);
        addCSItem(9000069, 9000069, 2047303, 0, 93, 4310007, 10, 1, 0, 0);

        //무기 30% 주문서
        addCSItem(9000069, 9000069, 2043105, 0, 100, 4310007, 10, 2, 60, 0);
        addCSItem(9000069, 9000069, 2043205, 0, 100, 4310007, 10, 2, 60, 0);
        addCSItem(9000069, 9000069, 2043305, 0, 100, 4310007, 10, 2, 60, 0);
        addCSItem(9000069, 9000069, 2043705, 0, 101, 4310007, 10, 2, 60, 0);
        addCSItem(9000069, 9000069, 2043805, 0, 102, 4310007, 10, 2, 60, 0);
        addCSItem(9000069, 9000069, 2044005, 0, 103, 4310007, 10, 2, 60, 0);
        addCSItem(9000069, 9000069, 2044105, 0, 104, 4310007, 10, 2, 60, 0);
        addCSItem(9000069, 9000069, 2044205, 0, 105, 4310007, 10, 2, 60, 0);
        addCSItem(9000069, 9000069, 2044305, 0, 106, 4310007, 10, 2, 60, 0);
        addCSItem(9000069, 9000069, 2044405, 0, 107, 4310007, 10, 2, 60, 0);
        addCSItem(9000069, 9000069, 2044505, 0, 108, 4310007, 10, 2, 60, 0);
        addCSItem(9000069, 9000069, 2044605, 0, 109, 4310007, 10, 2, 60, 0);
        addCSItem(9000069, 9000069, 2044705, 0, 110, 4310007, 10, 2, 60, 0);
        addCSItem(9000069, 9000069, 2044804, 0, 111, 4310007, 10, 2, 60, 0);
        addCSItem(9000069, 9000069, 2044904, 0, 112, 4310007, 10, 2, 60, 0);*/
        //벼루
        addShop(9000069);

        //장비
        addCSItem(9000069, 9000069, 1003946, 0, 0, 4310007, 25, 1, 0, 0);
        addCSItem(9000069, 9000069, 1052647, 0, 1, 4310007, 25, 1, 0, 0);
        addCSItem(9000069, 9000069, 1072853, 0, 2, 4310007, 30, 1, 0, 0);
        addCSItem(9000069, 9000069, 1082540, 0, 3, 4310007, 30, 1, 0, 0);
        addCSItem(9000069, 9000069, 1112402, 0, 4, 4310007, 15, 1, 14400, 0);
        addCSItem(9000069, 9000069, 1122017, 0, 5, 4310007, 3, 1, 1440, 0);
        addCSItem(9000069, 9000069, 1122017, 0, 6, 4310007, 8, 1, 4320, 0);
        addCSItem(9000069, 9000069, 1122017, 0, 7, 4310007, 15, 1, 10080, 0);
        addCSItem(9000069, 9000069, 1122280, 0, 8, 4310007, 8, 1, 0, 0);
        addCSItem(9000069, 9000069, 1302289, 0, 9, 4310007, 35, 1, 0, 0);
        addCSItem(9000069, 9000069, 1312165, 0, 10, 4310007, 35, 1, 0, 0);
        addCSItem(9000069, 9000069, 1322215, 0, 11, 4310007, 35, 1, 0, 0);
        addCSItem(9000069, 9000069, 1332238, 0, 12, 4310007, 35, 1, 0, 0);
        addCSItem(9000069, 9000069, 1372188, 0, 13, 4310007, 35, 1, 0, 0);
        addCSItem(9000069, 9000069, 1382222, 0, 14, 4310007, 35, 1, 0, 0);
        addCSItem(9000069, 9000069, 1402210, 0, 15, 4310007, 35, 1, 0, 0);
        addCSItem(9000069, 9000069, 1412147, 0, 16, 4310007, 35, 1, 0, 0);
        addCSItem(9000069, 9000069, 1422152, 0, 17, 4310007, 35, 1, 0, 0);
        addCSItem(9000069, 9000069, 1432178, 0, 18, 4310007, 35, 1, 0, 0);
        addCSItem(9000069, 9000069, 1442234, 0, 19, 4310007, 35, 1, 0, 0);
        addCSItem(9000069, 9000069, 1452216, 0, 20, 4310007, 35, 1, 0, 0);
        addCSItem(9000069, 9000069, 1462204, 0, 21, 4310007, 35, 1, 0, 0);
        addCSItem(9000069, 9000069, 1472226, 0, 22, 4310007, 35, 1, 0, 0);
        addCSItem(9000069, 9000069, 1482179, 0, 23, 4310007, 35, 1, 0, 0);
        addCSItem(9000069, 9000069, 1492190, 0, 24, 4310007, 35, 1, 0, 0);

        //장비 30% 주문서
        addCSItem(9000069, 9000069, 2040013, 0, 25, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040030, 0, 26, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040038, 0, 27, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040407, 0, 28, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040426, 0, 29, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040509, 0, 30, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040519, 0, 31, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040521, 0, 32, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040533, 0, 33, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040607, 0, 34, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040626, 0, 35, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040713, 0, 36, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040715, 0, 37, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040717, 0, 38, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040809, 0, 39, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040811, 0, 40, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040907, 0, 41, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040932, 0, 42, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2041035, 0, 43, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2041037, 0, 44, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2041039, 0, 45, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2041041, 0, 46, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040305, 0, 46, 4310007, 4, 1, 0, 0); //귀지 30%

        //무기 30% 주문서
        addCSItem(9000069, 9000069, 2043005, 0, 47, 4310007, 3, 1, 0, 0);
        addCSItem(9000069, 9000069, 2043105, 0, 48, 4310007, 3, 1, 0, 0);
        addCSItem(9000069, 9000069, 2043205, 0, 49, 4310007, 3, 1, 0, 0);
        addCSItem(9000069, 9000069, 2043305, 0, 50, 4310007, 3, 1, 0, 0);
        addCSItem(9000069, 9000069, 2043705, 0, 51, 4310007, 3, 1, 0, 0);
        addCSItem(9000069, 9000069, 2043805, 0, 52, 4310007, 3, 1, 0, 0);
        addCSItem(9000069, 9000069, 2044005, 0, 53, 4310007, 3, 1, 0, 0);
        addCSItem(9000069, 9000069, 2044105, 0, 54, 4310007, 3, 1, 0, 0);
        addCSItem(9000069, 9000069, 2044205, 0, 55, 4310007, 3, 1, 0, 0);
        addCSItem(9000069, 9000069, 2044305, 0, 56, 4310007, 3, 1, 0, 0);
        addCSItem(9000069, 9000069, 2044405, 0, 57, 4310007, 3, 1, 0, 0);
        addCSItem(9000069, 9000069, 2044505, 0, 58, 4310007, 3, 1, 0, 0);
        addCSItem(9000069, 9000069, 2044605, 0, 59, 4310007, 3, 1, 0, 0);
        addCSItem(9000069, 9000069, 2044705, 0, 60, 4310007, 3, 1, 0, 0);
        addCSItem(9000069, 9000069, 2044804, 0, 61, 4310007, 3, 1, 0, 0);
        addCSItem(9000069, 9000069, 2044904, 0, 62, 4310007, 3, 1, 0, 0);

        //특수 주문서
        addCSItem(9000069, 9000069, 2040914, 0, 63, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2040919, 0, 64, 4310007, 2, 1, 0, 0);
        addCSItem(9000069, 9000069, 2049100, 0, 65, 4310007, 6, 1, 1440, 0);
        addCSItem(9000069, 9000069, 2049400, 0, 66, 4310007, 1, 1, 0, 0); //고급 잠재능력 부여 주문서

        //스페셜
        addCSItem(9000069, 9000069, 2439991, 0, 67, 4310007, 3, 1, 0, 0); //미라클 큐브 3개 교환권
        addCSItem(9000069, 9000069, 2439999, 0, 67, 4310007, 2, 1, 1440, 0);
        addCSItem(9000069, 9000069, 2439985, 0, 67, 4310007, 10, 1, 1440, 0); //각인의 큐브

        //표창
        addCSItem(9000069, 9000069, 2070016, 0, 68, 4310007, 20, 1, 0, 0);

        //가위
        addItem(9000069, 9000069, 5510000, 50000000, -1, 1, 0);
        addItem(9000069, 9000069, 5520000, 1000000, -1, 1, 0);
        addItem(9000069, 9000069, 5520001, 100000000, -1, 1, 0);

        //파퀘상점
        addShop(9330008);

        //장비
        addCSItem(9330008, 9330008, 2049401, 0, 1, 4310195, 1, 1, 0, 0);
        addCSItem(9330008, 9330008, 2439990, 0, 2, 4310195, 1, 1, 0, 0);
        addCSItem(9330008, 9330008, 2040914, 0, 3, 4310195, 5, 1, 0, 0);
        addCSItem(9330008, 9330008, 2040919, 0, 4, 4310195, 5, 1, 0, 0);
        addCSItem(9330008, 9330008, 2046308, 0, 5, 4310195, 10, 1, 0, 0);
        addCSItem(9330008, 9330008, 2046313, 0, 6, 4310195, 10, 1, 0, 0);
        addCSItem(9330008, 9330008, 2046309, 0, 7, 4310195, 20, 1, 0, 0);
        addCSItem(9330008, 9330008, 2046314, 0, 8, 4310195, 20, 1, 0, 0);
        addCSItem(9330008, 9330008, 2049100, 0, 9, 4310195, 20, 1, 0, 0);
        addCSItem(9330008, 9330008, 1132013, 0, 10, 4310195, 15, 1, 0, 0);
        
        //크리스마스 피크 상점
        addShop(2001009);
        
        //크리스마스 피크 코인샵
        addCSItem(2001009, 2001009, 5062000, 0, 1, 4310003, 100, 1, 0, 0); //미라클 큐브 1개
        addCSItem(2001009, 2001009, 1082559, 0, 2, 4310003, 150, 1, 0, 0); //아인크라드 구원자의 장갑
        addCSItem(2001009, 2001009, 1052679, 0, 3, 4310003, 150, 1, 0, 0); //아인크라드 구원자의 코트
        addCSItem(2001009, 2001009, 2450020, 0, 4, 4310003, 200, 1, 1440, 0); //경험치 50% 추가 쿠폰
        addCSItem(2001009, 2001009, 2022462, 0, 5, 4310003, 300, 1, 1440, 0); //드롭률 50% 추가 쿠폰
        addCSItem(2001009, 2001009, 2070025, 0, 6, 4310003, 300, 1, 0, 0); //샤이닝 일비 표창
        addCSItem(2001009, 2001009, 2330008, 0, 7, 4310003, 300, 1, 0, 0); //풀 메탈 불릿
        addCSItem(2001009, 2001009, 1122017, 0, 8, 4310003, 300, 1, 1440, 0); //정령의 펜던트 1일
        addCSItem(2001009, 2001009, 2049402, 0, 9, 4310003, 300, 1, 1440, 0); //스페셜 잠재능력 부여 주문서
        addCSItem(2001009, 2001009, 2046309, 0, 10, 4310003, 500, 1, 1440, 0); //악세서리 공격력 주문서 70%
        addCSItem(2001009, 2001009, 2046314, 0, 11, 4310003, 500, 1, 1440, 0); //악세서리 마력 주문서 70%
        addCSItem(2001009, 2001009, 2049100, 0, 12, 4310003, 500, 1, 1440, 0); //혼돈의 주문서 60%
        addCSItem(2001009, 2001009, 2470001, 0, 13, 4310003, 1000, 1, 1440, 0); //황금 망치 50%
        addCSItem(2001009, 2001009, 2439977, 0, 14, 4310003, 1000, 1, 0, 0); //돌의 정령 펫상자
        addCSItem(2001009, 2001009, 5062000, 0, 15, 4310003, 1000, 11, 0, 0); //미라클 큐브 11개
        addCSItem(2001009, 2001009, 2430144, 0, 16, 4310003, 1000, 1, 1440, 0); //비밀의 마스터리 북
        addCSItem(2001009, 2001009, 2048020, 0, 17, 4310003, 1000, 1, 1440, 0); //펫장비 공격력 60% 교불
        addCSItem(2001009, 2001009, 2048021, 0, 18, 4310003, 1000, 1, 1440, 0); //펫장비 마력 60% 교불
        addCSItem(2001009, 2001009, 5062002, 0, 19, 4310003, 1500, 3, 0, 0); //마스터 미라클 큐브 교불
        addCSItem(2001009, 2001009, 2049404, 0, 20, 4310003, 3000, 1, 2, 0); //에픽 잠재능력 부여 주문서 100%
        addCSItem(2001009, 2001009, 2439978, 0, 21, 4310003, 3000, 1, 0, 0); //별하늘 난초 세트 상자

        //잭 바리케이드 상점
        addShop(9201112);
        
        //잭 바리케이드 cwk 코인샵
        addCSItem(9201112, 9201112, 1102207, 0, 1, 4310005, 8, 1, 0, 0); //크림슨 케이프
        addCSItem(9201112, 9201112, 1022082, 0, 2, 4310005, 10, 1, 0, 0); //스펙트럼 고글
        addCSItem(9201112, 9201112, 1122059, 0, 3, 4310005, 30, 1, 0, 0); //대악마 나리케인의 징표
        addCSItem(9201112, 9201112, 1312061, 0, 4, 4310005, 10, 1, 0, 0); //크림슨 엑스
        addCSItem(9201112, 9201112, 1322089, 0, 5, 4310005, 10, 1, 0, 0); //크림슨 블런트
        addCSItem(9201112, 9201112, 1332124, 0, 6, 4310005, 10, 1, 0, 0); //크림슨 대거
        addCSItem(9201112, 9201112, 1382082, 0, 7, 4310005, 10, 1, 0, 0); //크림슨 메이지 스태프
        addCSItem(9201112, 9201112, 1412040, 0, 8, 4310005, 10, 1, 0, 0); //크림슨 니르그 엑스
        addCSItem(9201112, 9201112, 1422062, 0, 9, 4310005, 10, 1, 0, 0); //크림슨 팔라딘 블런트
        addCSItem(9201112, 9201112, 1432063, 0, 10, 4310005, 10, 1, 0, 0); //크림슨 엑자일
        addCSItem(9201112, 9201112, 1442087, 0, 11, 4310005, 10, 1, 0, 0); //크림슨 디에스이라에
        addCSItem(9201112, 9201112, 1452060, 0, 12, 4310005, 10, 1, 0, 0); //크림슨 엔가우
        addCSItem(9201112, 9201112, 1462049, 0, 13, 4310005, 10, 1, 0, 0); //크림슨 마그나크로우
        addCSItem(9201112, 9201112, 1472067, 0, 14, 4310005, 10, 1, 0, 0); //크림슨 마그나클로
        addCSItem(9201112, 9201112, 1482048, 0, 15, 4310005, 10, 1, 0, 0); //크림슨 크루시오
        addCSItem(9201112, 9201112, 1492031, 0, 16, 4310005, 10, 1, 0, 0); //크림슨 저지먼트
        addCSItem(9201112, 9201112, 2049402, 0, 17, 4310005, 2, 1, 0, 0); //스페셜 잠재능력 부여 주문서
        addCSItem(9201112, 9201112, 2430144, 0, 18, 4310005, 6, 1, 0, 0); //비밀의 마스터리 북
        addCSItem(9201112, 9201112, 2040914, 0, 19, 4310005, 3, 1, 0, 0); //방패공 60%
        addCSItem(9201112, 9201112, 2040922, 0, 20, 4310005, 3, 1, 0, 0); //방패마력 60%
        
        //이시라즈
        addShop(9110102);
        addItem(9110102, 9110102, 2060000, 40, 100, 1, 0);
        addItem(9110102, 9110102, 2000001, 150, 101, 1, 0);
        addItem(9110102, 9110102, 2000002, 320, 102, 1, 0);
        addItem(9110102, 9110102, 2001001, 2300, 103, 1, 0);
        addItem(9110102, 9110102, 2020012, 4500, 104, 1, 0);
        addItem(9110102, 9110102, 2000003, 200, 105, 1, 0);
        addItem(9110102, 9110102, 2001002, 4000, 106, 1, 0);
        addItem(9110102, 9110102, 2020014, 8100, 107, 1, 0);
        addItem(9110102, 9110102, 2070000, 500, 108, 1, 0);

        //엘윈
        addShop(9270027);
        addItem(9270027, 9270027, 2022203, 800, 100, 1, 0);

        //알리
        addShop(9270065);
        addItem(9270065, 9270065, 2022203, 800, 100, 1, 0);

//        //마야(파괴된 헤네시스)
//        addShop(2142000);
//        addItem(2142000, 2142000, 2022000, 1600, 100, 1, 0);
//        addItem(2142000, 2142000, 2001000, 1600, 101, 1, 0);
//        addItem(2142000, 2142000, 2001001, 2200, 102, 1, 0);
//        addItem(2142000, 2142000, 2001002, 4000, 103, 1, 0);
//        addItem(2142000, 2142000, 2020012, 4400, 104, 1, 0);
//        addItem(2142000, 2142000, 2020013, 5600, 105, 1, 0);
//        addItem(2142000, 2142000, 2020014, 8100, 106, 1, 0);
//        addItem(2142000, 2142000, 2020015, 10200, 107, 1, 0);
//        addItem(2142000, 2142000, 2030000, 400, 108, 1, 0);
//        addItem(2142000, 2142000, 2020011, 10000, 109, 1, 0);
        /*try {
            ps1.setInt(1, 9090000);
            ps2.setInt(1, 9090000);
            ps2.setInt(2, 1002140);//거짓말 탐지기
            ps2.setInt(3, 10000);
            ps2.setInt(4, 100 * 10);
            ps2.setInt(5, 0);
            ps2.setInt(6, 0);
            ps2.setInt(7, 0);
            ps2.addBatch();
        } catch (Exception e) {
            e.printStackTrace();
            //System.out.println("Shopid : " + npcid);
            return;
        }*/
        ps1.executeBatch();
        ps2.executeBatch();
        ps1.close();
        ps2.close();
    }

    public static void addItem(int npcid, int shopid, int itemid, int price, int position, int count, int pitch) throws Exception {
        addItem(npcid, shopid, itemid, price, position, count, pitch, 0, 0);
    }

    public static void addItem(int npcid, int shopid, int itemid, int price, int position, int count, int pitch, int minutes, int level) throws Exception {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps1 = con.prepareStatement("INSERT INTO shops (`npcid`) VALUES (?)");
        PreparedStatement ps2 = con.prepareStatement("INSERT INTO shopitems (shopid, itemid, price, position, max, expiretime, level) VALUES (?, ?, ?, ?, ?, ?, ?)");
        try {
            ps1.setInt(1, npcid);
            ps2.setInt(1, shopid);
            ps2.setInt(2, itemid);//거짓말 탐지기
            ps2.setInt(3, price);
            ps2.setInt(4, position);
            ps2.setInt(5, count);
            ps2.setInt(6, minutes); //기간 (분단위)
            ps2.setInt(7, level);
            ps2.addBatch();
        } catch (Exception e) {
            e.printStackTrace();
            //System.out.println("Shopid : " + npcid);
            return;
        }
        ps1.executeBatch();
        ps2.executeBatch();
        con.close();
        ps1.close();
        ps2.close();
    }

    public static void addCSItem(int npcid, int shopid, int itemid, int price, int position, int coinId, int coinq, int count, int minutes, int level) throws Exception {//CoinShop
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps1 = con.prepareStatement("INSERT INTO shops (`npcid`) VALUES (?)");
        PreparedStatement ps2 = con.prepareStatement("INSERT INTO shopitems (shopid, itemid, price, position, reqitem, reqitemq, max, expiretime, level) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
        try {
            ps1.setInt(1, npcid);
            ps2.setInt(1, shopid);
            ps2.setInt(2, itemid);//거짓말 탐지기
            ps2.setInt(3, price);
            ps2.setInt(4, position);
            ps2.setInt(5, coinId);
            ps2.setInt(6, coinq);
            ps2.setInt(7, count);
            ps2.setInt(8, minutes); //기간 (분단위)
            ps2.setInt(9, level);
            ps2.addBatch();
        } catch (Exception e) {
            e.printStackTrace();
            //System.out.println("Shopid : " + npcid);
            return;
        }
        ps1.executeBatch();
        ps2.executeBatch();
    }

    public static boolean isFancyGoodsShop(final int sid) {
        switch (sid) {
            case 2:
            case 6:
            case 9://헤네시스
            case 13://페리온
            case 16://엘리니아
            case 20://커닝시티
            case 23://갬굴광장
            case 24://사우나
            case 25://플로리나
            case 27://해적마을
            case 33://오르비스
            case 36://엘나스
            case 38://엘나스골짜기
            case 41://루디
            case 44://뭐지?
            case 45://시계탑깊은곳
            case 48://지구방위본부
            case 50://아쿠아리움
            case 52://아랫마을
            case 55://리프레
            case 61://무릉
            case 65://백초마을
            case 68://아리안트
            case 69://마가티아
            case 72://코크
            case 80://태국
            case 84://중국
            case 85://어디지 야시장인가봄
            case 88://세계여행어딘가
            case 91://N:C
            case 1055002://태하
            case 9090000://묘묘
                return true;
        }
        return false;
    }

    public static int armoryShopGrade(final int sid) {
        switch (sid) {
            case 32:
            case 64:
            case 67:
            case 89:
                return 1;//30제까지만 파는 상점
            case 49:
            case 83:
            case 87:
                return 2;//40제까지만 파는 상점
            default:
                return 0;
        }
    }

    public static boolean isShopNpc(final int sid) {
        switch (sid) {
            case 1301000://버섯의성
                return true;
        }
        return false;
    }

    public static int addShop(int npcid) throws Exception {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO shops (shopid ,npcid) VALUES (?, ?)");
            ps.setInt(1, npcid);
            ps.setInt(2, npcid);
            ps.executeUpdate();
            ps.close();
            con.close();
            System.err.println("상점을 추가 했습니다. npcid : " + npcid);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Shop Dupe Err..");
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception e) {
            }
        }
        return 1;
    }

    public static int copyShop(int shop1, int shop2) {

        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;
        ResultSet rs2 = null;

        ArrayList<Integer> shopitems3 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems4 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems5 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems6 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems7 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems8 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems9 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems10 = new ArrayList<Integer>();
        ArrayList<Integer> shopitems11 = new ArrayList<Integer>();

        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM shops WHERE shopid = ?");
            ps.setInt(1, shop1);
            rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println("존재하는 않는 상점 엔피시로 추정됩니다. [상점 1]");
                return 1;
            }
            ps.close();
            rs.close();

            ps = con.prepareStatement("SELECT * FROM shops WHERE npcid = ?");
            ps.setInt(1, shop2);
            rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("이미 존재하는 상점 엔피시로 추정됩니다. [상점 2]");
                return 1;
            }
            ps.close();
            rs.close();

            ps = con.prepareStatement("SELECT * FROM shopitems WHERE shopid = ?");
            ps.setInt(1, shop1);
            rs = ps.executeQuery();
            while (rs.next()) {
                shopitems3.add(rs.getInt(3));
                shopitems4.add(rs.getInt(4));
                shopitems5.add(rs.getInt(5));
                shopitems6.add(rs.getInt(6));
                shopitems7.add(rs.getInt(7));
                shopitems8.add(rs.getInt(8));
                shopitems9.add(rs.getInt(9));
                shopitems10.add(rs.getInt(10));
                shopitems11.add(rs.getInt(11));
            }
            ps.close();
            rs.close();

            ps = con.prepareStatement("INSERT INTO shops (shopid ,npcid) VALUES (?, ?)");
            ps.setInt(1, shop2);
            ps.setInt(2, shop2);
            //int shopid2 = rs.getInt("shopid");
            ps.executeUpdate();
            ps.close();

            ps = con.prepareStatement("DELETE FROM shopitems WHERE shopid = ?");
            ps.setInt(1, shop2);
            ps.executeUpdate();
            ps.close();
            for (int i = 0; i < shopitems3.size(); i++) {
                ps = con.prepareStatement("INSERT INTO shopitems (shopid, itemid, price, position, reqitem, reqitemq, rank, max, expiretime, level) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setInt(1, shop2);
                ps.setInt(2, shopitems3.get(i));
                ps.setInt(3, shopitems4.get(i));
                ps.setInt(4, shopitems5.get(i));
                ps.setInt(5, shopitems6.get(i));
                ps.setInt(6, shopitems7.get(i));
                ps.setInt(7, shopitems8.get(i));
                ps.setInt(8, shopitems9.get(i));
                ps.setInt(9, shopitems10.get(i));
                ps.setInt(10, shopitems11.get(i));
                ps.executeUpdate();
                ps.close();
            }
            System.out.println("모든 작업을 완료했습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Shop Dupe Err..");
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
            }
        }
        return 1;
    }
}
