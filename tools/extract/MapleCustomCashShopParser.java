/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.extract;

import constants.GameConstants;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import server.Randomizer;

/**
 * @author 티썬
 * @수정자 카와이 뚜뚜뚜
 */
public class MapleCustomCashShopParser {

    private static List<Integer> cashItem, cashItemCount, cashItemPrice, cashItemPriority, cashItemFlag = null;

    public static void main(String[] args) throws Exception {
        DatabaseConnection.init();
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        ps = con.prepareStatement("DELETE FROM cashshop_modified_items");
        ps.executeUpdate();
        List<Integer> csItemArray = new ArrayList<Integer>();
        List<Integer> csItemCountArray = new ArrayList<Integer>();
        byte catecory_header = 0;
        byte subcategory_header = 0;
        int price = 0;
        //하드코딩 부분
        addItem(10002093, 1000, 0, true, 2439986, 111, 0, 1, 2, true); //후캐 1000
        addItem(10002094, 5000, 0, true, 2439987, 112, 0, 1, 2, true); //후캐 5000
        addItem(10002095, 10000, 0, true, 2439988, 113, 0, 1, 2, true); //후캐 10000
        addItem(10002096, 50000, 0, true, 2439989, 114, 0, 1, 2, true); //후캐 50000

        addItem(10001993, 5000, 0, false, 0, 111, 0, 13, 2, true); //미라클 큐브
        addItem(10001988, 500, 0, false, 0, 110, 0, 0, 2, true); //미라클 큐브

        addItem(50200067, 5000, 0, false, 0, 113, 0, 13, 2, true); //미라클 큐브
        addItem(50200066, 500, 0, false, 0, 112, 0, 0, 2, true); //미라클 큐브

        addItem(50200052, 0, 0, true, 0, 90, 0, 0, 2, true); //펜던트 슬롯 확장 
        addItem(50200052, 50000, 0, true, 0, 90, 0, 0, 2, false); //펜던트 슬롯 확장 
        addItem(50200059, 0, 0, false, 0, 112, 0, 0, 2, true); //플가
        addItem(50200043, 1000, 0, true, 0, -1, 0, 0, 2, true); //그냥 가위
        addItem(50200042, 0, 0, false, 0, 112, 0, 0, 2, true); //수레바퀴
        addItem(10001854, 0, 0, false, 0, 112, 0, 0, 2, true); //플가
        addItem(10001666, 1000, 0, true, 0, -1, 0, 0, 2, true); //그냥 가위
        addItem(10001665, 0, 0, false, 0, 112, 0, 0, 2, true); //수레바퀴

        addItem(50000098, 0, 0, true, 0, -1, 0, 0, 2, false); //로얄 쿠폰
        addItem(50000098, 1000, 0, true, 0, 112, 0, 0, 2, true); //로얄 쿠폰
        addItem(50000101, 0, 0, false, 0, 112, 0, 0, 2, true); //썸머 쿠폰
        addItem(50000101, 0, 0, false, 0, 112, 0, 0, 2, false); //썸머쿠폰

        addItem(10002097, 500, 0, true, 2009999, 100, 0, 1, 2, true); //스페셜 뷰티
        addItem(10002098, 10000, 0, true, 2009999, 100, 0, 1, 2, false); //스페셜 뷰티

        addItem(10001960, 1400, 0, true, 0, 99, 0, 0, 2, true); //땅콩
        addItem(50200064, 1400, 0, true, 0, 99, 0, 0, 2, true); //땅콩
        addItem(50200065, 9900, 1, true, 0, 99, 0, 8, 2, true); //땅콩

        for (int i = 0; i <= 89; ++i) {
            addItem(20000355 + i, 0, -1, true, 0, 100, 0, 0, -1, true);//모자
            addItem(20000355 + i, 0, -1, true, 0, 100, 0, 0, -1, false);//모자
        }
        for (int i = 0; i <= 12; ++i) {
            addItem(20300289 + i, 0, -1, true, 0, 100, 0, 0, -1, true); //한벌
            addItem(20300289 + i, 0, -1, true, 0, 100, 0, 0, -1, false); //한벌
        }
        for (int i = 0; i <= 23; ++i) {
            addItem(20400259 + i, 0, -1, true, 0, 100, 0, 0, -1, true); //상의
            addItem(20400259 + i, 0, -1, true, 0, 100, 0, 0, -1, false); //상의
        }
        for (int i = 0; i <= 118; ++i) {
            addItem(20500100 + i, 0, -1, true, 0, -1, 0, 0, -1, true); //바지
            addItem(20500100 + i, 0, -1, true, 0, -1, 0, 0, -1, false); //바지
        }
        for (int i = 0; i <= 11; ++i) {
            if (20600190 + i != 20600200) {
                addItem(20600190 + i, 0, -1, true, 0, 100, 0, 0, -1, true); //신발
                addItem(20600190 + i, 0, -1, true, 0, 100, 0, 0, -1, false); //신발
            }
        }
        for (int i = 0; i <= 13; ++i) {
            if (20700040 + i != 20700051) {
                addItem(20700040 + i, 0, -1, true, 0, -1, 0, 0, -1, true); //장갑
                addItem(20700040 + i, 0, -1, true, 0, -1, 0, 0, -1, false); //장갑
            }
        }
        for (int i = 0; i <= 64; ++i) {
            switch (20800189 + i) {
                case 20800192:
                case 20800193:
                case 20800194:
                case 20800195:
                case 20800202:
                case 20800241:
                case 20800242:
                case 20800243:
                case 20800244:
                    continue;
            }
            addItem(20800189 + i, 0, -1, true, 0, -1, 0, 0, -1, true); //무기
            addItem(20800189 + i, 0, -1, true, 0, -1, 0, 0, -1, false); //무기
        }

        //피시방
        addItem(21000000, 2700, 0, false, 0, 50, 0, 0, 2, false);
        addItem(21000001, 2700, 0, true, 0, 50, 0, 0, 2, false);
        addItem(21000002, 2700, 0, true, 0, 50, 0, 0, 2, false);
        addItem(21000003, 2700, 0, true, 0, 50, 0, 0, 2, false);
        addItem(21000004, 1500, 0, true, 0, 50, 0, 0, 2, false);
        addItem(21000005, 1700, 0, true, 0, 50, 0, 0, 2, false);
        addItem(21000006, 1500, 0, true, 0, 50, 0, 0, 2, false);
        addItem(21000007, 1400, 0, true, 0, 50, 0, 0, 2, false);
        addItem(21000008, 1700, 0, true, 0, 50, 0, 0, 2, false);
        addItem(21000009, 1300, 0, true, 0, 50, 0, 0, 2, false);
        addItem(21000010, 2700, 0, true, 0, 50, 0, 0, 2, false);
        addItem(21000011, 2000, 0, true, 0, 50, 0, 0, 2, false);
        addItem(21000012, 2000, 0, true, 0, 50, 0, 0, 2, false);
        addItem(21000013, 1500, 0, true, 0, 50, 0, 0, 2, false);
        addItem(21000014, 2000, 0, true, 0, 50, 0, 0, 2, false);
        addItem(21000015, 200, 0, true, 0, 50, 0, 0, 2, false);
        addItem(21000016, 2000, 0, true, 0, 50, 0, 0, 2, false);

        addItem(21000000, 2700, 0, false, 0, 50, 0, 0, 2, true);
        addItem(21000001, 2700, 0, true, 0, 50, 0, 0, 2, true);
        addItem(21000002, 2700, 0, true, 0, 50, 0, 0, 2, true);
        addItem(21000003, 2700, 0, true, 0, 50, 0, 0, 2, true);
        addItem(21000004, 1500, 0, true, 0, 50, 0, 0, 2, true);
        addItem(21000005, 1700, 0, true, 0, 50, 0, 0, 2, true);
        addItem(21000006, 1500, 0, true, 0, 50, 0, 0, 2, true);
        addItem(21000007, 1400, 0, true, 0, 50, 0, 0, 2, true);
        addItem(21000008, 1700, 0, true, 0, 50, 0, 0, 2, true);
        addItem(21000009, 1300, 0, true, 0, 50, 0, 0, 2, true);
        addItem(21000010, 2700, 0, true, 0, 50, 0, 0, 2, true);
        addItem(21000011, 2000, 0, true, 0, 50, 0, 0, 2, true);
        addItem(21000012, 2000, 0, true, 0, 50, 0, 0, 2, true);
        addItem(21000013, 1500, 0, true, 0, 50, 0, 0, 2, true);
        addItem(21000014, 2000, 0, true, 0, 50, 0, 0, 2, true);
        addItem(21000015, 200, 0, true, 0, 50, 0, 0, 2, true);
        addItem(21000016, 2000, 0, true, 0, 50, 0, 0, 2, true);

        //빠진템들 일부
        addItem(20000306, 2700, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20000307, 2700, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20000309, 2700, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20000310, 2700, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20000315, 2400, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20000318, 2400, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20000382, 4500, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20000413, 2700, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20000407, 2600, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20000417, 3200, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20000420, 2600, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20000430, 2100, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20000306, 2700, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20000307, 2700, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20000309, 2700, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20000310, 2700, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20000315, 2400, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20000318, 2400, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20000382, 4500, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20000413, 2700, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20000407, 2600, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20000417, 3200, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20000420, 2600, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20000430, 2100, 0, true, 0, 90, 0, 0, 2, false);

        addItem(20200075, 1800, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20200081, 1700, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20200075, 1800, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20200081, 1700, 0, true, 0, 90, 0, 0, 2, false);

        addItem(20300203, 3000, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300204, 3000, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300205, 3000, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300246, 3300, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300248, 3000, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300260, 2900, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300265, 3200, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300266, 3200, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300272, 3200, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300274, 3200, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300275, 3200, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300276, 3200, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300280, 3200, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300282, 3200, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300283, 3200, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300287, 3300, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20300203, 3000, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20300204, 3000, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20300205, 3000, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20300246, 3300, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20300248, 3000, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20300260, 2900, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20300265, 3200, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20300266, 3200, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20300272, 3200, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20300274, 3200, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20300275, 3200, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20300276, 3200, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20300280, 3200, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20300282, 3200, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20300283, 3200, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20300287, 3300, 0, true, 0, 90, 0, 0, 2, false);

        addItem(20400054, 1700, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20400055, 1700, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20400209, 2500, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20400207, 2400, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20400225, 2400, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20400233, 2600, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20400267, 2700, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20400054, 1700, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20400055, 1700, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20400209, 2500, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20400207, 2400, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20400225, 2400, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20400233, 2600, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20400267, 2700, 0, true, 0, 90, 0, 0, 2, false);

        addItem(20500176, 1600, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20500177, 1600, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20500183, 1600, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20500176, 1600, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20500177, 1600, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20500183, 1600, 0, true, 0, 90, 0, 0, 2, false);

        addItem(20600171, 1200, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20600172, 1200, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20600176, 1300, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20600192, 1500, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20600171, 1200, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20600172, 1200, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20600176, 1300, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20600192, 1500, 0, true, 0, 90, 0, 0, 2, false);

        addItem(20700045, 1300, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20700048, 1300, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20700049, 1300, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20700045, 1300, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20700048, 1300, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20700049, 1300, 0, true, 0, 90, 0, 0, 2, false);

        addItem(20800152, 4700, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20800153, 4700, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20800156, 4300, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20800204, 4900, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20800206, 5000, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20800217, 4900, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20800231, 4800, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20800232, 4700, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20800234, 4600, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20800240, 4900, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20800152, 4700, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20800153, 4700, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20800156, 4300, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20800204, 4900, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20800206, 5000, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20800217, 4900, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20800231, 4800, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20800232, 4700, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20800234, 4600, 0, true, 0, 90, 0, 0, 2, false);
        addItem(20800240, 4900, 0, true, 0, 90, 0, 0, 2, false);

        addItem(20900078, 2500, 0, true, 0, 90, 0, 0, 2, true);
        addItem(20900078, 2500, 0, true, 0, 90, 0, 0, 2, false);

        addItem(21100050, 3500, 0, true, 0, 90, 0, 0, 2, true);
        addItem(21100051, 3500, 0, true, 0, 90, 0, 0, 2, true);
        addItem(21100052, 3500, 0, true, 0, 90, 0, 0, 2, true);
        addItem(21100091, 3000, 0, true, 0, 90, 0, 0, 2, true);
        addItem(21100109, 2900, 0, true, 0, 90, 0, 0, 2, true);
        addItem(21100050, 3500, 0, true, 0, 90, 0, 0, 2, false);
        addItem(21100051, 3500, 0, true, 0, 90, 0, 0, 2, false);
        addItem(21100052, 3500, 0, true, 0, 90, 0, 0, 2, false);
        addItem(21100091, 3000, 0, true, 0, 90, 0, 0, 2, false);
        addItem(21100109, 2900, 0, true, 0, 90, 0, 0, 2, false);

        addItem(30200057, 2000, 0, true, 0, 90, 0, 0, 2, true);
        addItem(30200058, 200, 0, true, 0, 90, 0, 0, 2, true);
        addItem(30200075, 2500, 0, true, 0, 90, 0, 0, 2, true);
        addItem(30200076, 250, 0, true, 0, 90, 0, 0, 2, true);
        addItem(30200057, 2000, 0, true, 0, 90, 0, 0, 2, false);
        addItem(30200058, 200, 0, true, 0, 90, 0, 0, 2, false);
        addItem(30200075, 2500, 0, true, 0, 90, 0, 0, 2, false);
        addItem(30200076, 250, 0, true, 0, 90, 0, 0, 2, false);

        addItem(50100032, 2900, 0, true, 0, 90, 0, 0, 2, true);
        addItem(50500065, 8900, 0, true, 0, 90, 0, 0, 2, true);
        addItem(50100032, 2900, 0, true, 0, 90, 0, 0, 2, false); //포장마차
        addItem(50500065, 8900, 0, true, 0, 90, 0, 0, 2, false); //세라핌의 날개

        addItem(60000030, 4500, 0, true, 0, 90, 0, 0, 2, true); //펭귄
        addItem(60000043, 9900, 0, true, 0, 90, 0, 0, 2, true); //아기호랑이
        addItem(60000042, 9900, 0, true, 0, 90, 0, 0, 2, true); //황금돼지
        addItem(60000030, 4500, 0, true, 0, 90, 0, 0, 2, false); //펭귄
        addItem(60000042, 9900, 0, true, 0, 90, 0, 0, 2, false); //황금돼지
        addItem(60000043, 9900, 0, true, 0, 90, 0, 0, 2, false); //아기호랑이

        addItem(60100041, 1900, 0, true, 0, 90, 0, 0, 2, true);
        addItem(60100053, 2500, 0, true, 0, 90, 0, 0, 2, true);
        addItem(60100054, 2500, 0, true, 0, 90, 0, 0, 2, true);
        addItem(60100041, 1900, 0, true, 0, 90, 0, 0, 2, false);
        addItem(60100053, 2500, 0, true, 0, 90, 0, 0, 2, false);
        addItem(60100054, 2500, 0, true, 0, 90, 0, 0, 2, false);

        //보이지 않게 해둔것들
        addItem(10001906, 2900, 0, false, 0, 90, 0, 0, 2, true);
        addItem(10001907, 3900, 0, false, 0, 90, 0, 0, 2, true);
        addItem(10001908, 5900, 0, false, 0, 90, 0, 0, 2, true);
        addItem(10001909, 5900, 0, false, 0, 90, 0, 0, 2, true);
        addItem(10001910, 8900, 0, false, 0, 90, 0, 0, 2, true);
        addItem(10001911, 8900, 0, false, 0, 90, 0, 0, 2, true);
        addItem(10001994, 8900, 0, false, 0, 90, 0, 0, 2, true);

        addItem(10001918, 5000, 0, false, 0, 111, 0, 13, 2, true);
        addItem(10001919, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(10001920, 5000, 0, false, 0, 111, 0, 13, 2, true);
        addItem(10001921, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(10001922, 5000, 0, false, 0, 111, 0, 13, 2, true);
        addItem(10001923, 500, 0, false, 0, 110, 0, 0, 2, true);

        addItem(40000000, 2900, 0, false, 0, 90, 0, 0, 2, true);
        addItem(40000001, 3900, 0, false, 0, 90, 0, 0, 2, true);
        addItem(40000002, 5900, 0, false, 0, 90, 0, 0, 2, true);
        addItem(40000003, 5900, 0, false, 0, 90, 0, 0, 2, true);
        addItem(40000004, 8900, 0, false, 0, 90, 0, 0, 2, true);
        addItem(40000005, 8900, 0, false, 0, 90, 0, 0, 2, true);
        addItem(40000006, 5000, 0, false, 0, 111, 0, 13, 2, true);
        addItem(40000007, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(40000008, 5000, 0, false, 0, 111, 0, 13, 2, true);
        addItem(40000009, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(40000010, 5000, 0, false, 0, 111, 0, 13, 2, true);
        addItem(40000011, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(40000012, 500, 0, false, 0, 111, 0, 13, 2, true);
        addItem(40000013, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(40000014, 500, 0, false, 0, 111, 0, 13, 2, true);
        addItem(40000015, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(40000016, 500, 0, false, 0, 111, 0, 13, 2, true);
        addItem(40000017, 500, 0, false, 0, 111, 0, 13, 2, true);

        addItem(70000263, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(70000264, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(70000265, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(70000273, 500, 0, false, 0, 110, 0, 0, 2, true);
        
        addItem(10001664, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(50200041, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(50200044, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(50200045, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(50200048, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(50200049, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(92000020, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(93000022, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(92000069, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(50200053, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(10001772, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(10001773, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(50200054, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(92000036, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(93000005, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(93000064, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(50200055, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(10001815, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(10001816, 500, 0, false, 0, 110, 0, 0, 2, true);
        addItem(50200056, 500, 0, false, 0, 110, 0, 0, 2, true);
        //끝

        /*
        1. 메인 1~2
        2. 장비 1~13
        3. 소비 1~3
        4. 스페셜 1
        5. 기타 1~6
        6. 펫 1~3
        7. 패키지 1
         */
        //////1-0
        catecory_header = 1;
        subcategory_header = 0;
        price = 2500;
        divideCategory(catecory_header, subcategory_header, itemlist1_0, price, true);

        //////1-1
        catecory_header = 1;
        subcategory_header = 1;
        price = 3000;
        //divideCategory(catecory_header, subcategory_header, itemlist1_1, price, true);

        //////2-0
        catecory_header = 2;
        subcategory_header = 0;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_0_Donate, true);
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_0, false);

        //////2-1
        catecory_header = 2;
        subcategory_header = 1;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_1, true);
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_1, false);

        //////2-2
        catecory_header = 2;
        subcategory_header = 2;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_2, true);
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_2, false);

        //////2-3
        catecory_header = 2;
        subcategory_header = 3;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_3_Donate, true);
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_3, false);

        //////2-4
        catecory_header = 2;
        subcategory_header = 4;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_4, true);
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_4, false);

        //////2-5
        catecory_header = 2;
        subcategory_header = 5;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_5, true);
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_5, false);

        //////2-6 신발
        catecory_header = 2;
        subcategory_header = 6;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_6, true);
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_6, false);

        //////2-7 장갑
        catecory_header = 2;
        subcategory_header = 7;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_7, true);
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_7, false);

        //////2-8 무기
        catecory_header = 2;
        subcategory_header = 8;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_8_Donate, true);
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_8, false);
        
        //////2-9 반지
        catecory_header = 2;
        subcategory_header = 9;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_9_Donate, true);
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_9, false);

        //////2-11 망토
        catecory_header = 2;
        subcategory_header = 11;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_11, true);
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist2_11, false);

        //////4-0
        catecory_header = 4;
        subcategory_header = 0;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist4_0, true);

        //////5-0
        catecory_header = 5;
        subcategory_header = 0;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist5_0_Donate, true);
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist5_0, false);

        //////5-3
        catecory_header = 5;
        subcategory_header = 2;
        divideCategory(catecory_header, subcategory_header, itemlist5_2, price, true);

        //////6-1
        catecory_header = 6;
        subcategory_header = 0;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist6_0, true);

        //////6-2
        catecory_header = 6;
        subcategory_header = 1;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist6_1, true);

        //////6-3
        catecory_header = 6;
        subcategory_header = 2;
        divideCategoryWithFullOption(catecory_header, subcategory_header, itemlist6_2, true);

        ps.close();
        con.close();
        System.out.println("끝");
    }

    public static int getItemId(int i) {
        return cashItem.get(i);
    }

    public static int getItemCount(int i) {
        return cashItemCount.get(i);
    }

    public static int getItemPrice(int i) {
        return cashItemPrice.get(i);
    }

    private static void processRewards(final List<Integer> csItemArray, final List<Integer> csItemCountArray, final int[] list) {
        int lastitem = 0;
        int itemcount = 0;
        for (int i = 0; i < list.length; i++) {
            if (i % 2 == 0) { // Even
                lastitem = list[i];
                csItemArray.add(lastitem);
            } else if (i % 2 == 1) { // Odd
                itemcount = list[i];
                csItemCountArray.add(itemcount);
            }
        }
    }

    private static void processRewards2(final List<Integer> csItemArray, final List<Integer> csItemCountArray, final List<Integer> csItemPriceArray, final int[] list) {
        int lastitem = 0;
        int itemcount = 0;
        int itemPrice = 0;
        for (int i = 0; i < list.length; i++) {
            if (i % 3 == 0) { // Even
                lastitem = list[i];
                csItemArray.add(lastitem);
            } else if (i % 3 == 1) { // Odd
                itemcount = list[i];
                csItemCountArray.add(itemcount);
            } else if (i % 3 == 2) { // Odd
                itemPrice = list[i];
                csItemPriceArray.add(itemPrice);
            }
        }
    }

    private static void processRewardsFull(final List<Integer> csItemArray, final List<Integer> csItemCountArray, final List<Integer> csItemPriceArray, final List<Integer> csItemPriorityArray, final List<Integer> csItemFlagArray, final int[] list) {
        int imteId = 0;
        int itemCount = 0;
        int itemPrice = 0;
        int itemPrority = 0;
        int itemFalg = 0;
        for (int i = 0; i < list.length; i++) {
            if (i % 5 == 0) { // Even
                imteId = list[i];
                csItemArray.add(imteId);
            } else if (i % 5 == 1) { // Odd
                itemCount = list[i];
                csItemCountArray.add(itemCount);
            } else if (i % 5 == 2) { // Odd
                itemPrice = list[i];
                csItemPriceArray.add(itemPrice);
            } else if (i % 5 == 3) { // Odd
                itemPrority = list[i];
                csItemPriorityArray.add(itemPrority);
            } else if (i % 5 == 4) { // Odd
                itemFalg = list[i];
                csItemFlagArray.add(itemFalg);
            }
        }
    }

    public static void divideCategory(byte catecory_header, byte subcategory_header, int[] itemlist, int price, boolean donate) throws Exception {
        List<Integer> csItemArray = new ArrayList<Integer>();
        List<Integer> csItemCountArray = new ArrayList<Integer>();
        processRewards(csItemArray, csItemCountArray, itemlist);
        cashItem = csItemArray;
        cashItemCount = csItemCountArray;
        int serialid = 0;
        serialid = (catecory_header * 10000000) + (subcategory_header * 100000);

        for (int i = 0; i < cashItem.size(); i++) {
            serialid += i;
            if (catecory_header == 1 && subcategory_header == 0 || catecory_header == 5 && subcategory_header == 2) {
                switch (i) {
                    case 0:
                        price = 990;
                        break;
                    case 1:
                        price = 4950;
                        break;
                    case 2:
                        price = 9900;
                        break;
                }
            }
            addItem(
                    serialid + getPlus(serialid / 100000) + 1, //sn
                    price, //가격
                    0, //마크
                    true, //showup
                    getItemId(i), //아이템아이디
                    100 + i, //priority
                    0, //period
                    getItemCount(i), //count
                    2, //gender
                    donate //donate
            );
        }
    }

    public static void divideCategory2(byte catecory_header, byte subcategory_header, int[] itemlist, boolean donate) throws Exception {
        List<Integer> csItemArray = new ArrayList<Integer>();
        List<Integer> csItemCountArray = new ArrayList<Integer>();
        List<Integer> csItemPriceArray = new ArrayList<Integer>();
        processRewards2(csItemArray, csItemCountArray, csItemPriceArray, itemlist);
        cashItem = csItemArray;
        cashItemCount = csItemCountArray;
        cashItemPrice = csItemPriceArray;
        int serialid = 0;
        serialid = (catecory_header * 10000000) + (subcategory_header * 100000);

        for (int i = 0; i < cashItem.size(); i++) {
            serialid += i;
            addItem(
                    serialid + getPlus(serialid / 100000) + 1, //sn
                    getItemPrice(i), //가격
                    0, //마크
                    true, //showup
                    getItemId(i), //아이템아이디
                    100 + i, //priority
                    0, //period
                    getItemCount(i), //count
                    2, //gender
                    donate //donate
            );
        }
    }

    public static void divideCategoryWithFullOption(byte catecory_header, byte subcategory_header, int[] itemlist, boolean donate) throws Exception {
        List<Integer> csItemArray = new ArrayList<Integer>();
        List<Integer> csItemCountArray = new ArrayList<Integer>();
        List<Integer> csItemPriceArray = new ArrayList<Integer>();
        List<Integer> csItemPriorityArray = new ArrayList<Integer>();
        List<Integer> csItemFlagArray = new ArrayList<Integer>();
        processRewardsFull(csItemArray, csItemCountArray, csItemPriceArray, csItemPriorityArray, csItemFlagArray, itemlist);
        cashItem = csItemArray;
        cashItemCount = csItemCountArray;
        cashItemPrice = csItemPriceArray;
        cashItemPriority = csItemPriorityArray;
        cashItemFlag = csItemFlagArray;
        int serialid = 0;
        serialid = (catecory_header * 10000000) + (subcategory_header * 100000);

        for (int i = 0; i < cashItem.size(); i++) {
            serialid += i;
            addItem(
                    serialid + getPlus(serialid / 100000) + 1, //sn
                    getItemPrice(i), //가격
                    cashItemFlag.get(i), //마크
                    true, //showup
                    getItemId(i), //아이템아이디
                    100 + i, //priority
                    0, //period
                    getItemCount(i), //count
                    2, //gender
                    donate //donate
            );
        }
    }

    public static void addItem(int serialId, int price, int mark, boolean showup, int itemid, int priority, int period, int count, int gender, boolean donate) throws Exception {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("INSERT INTO cashshop_modified_items (serial, discount_price, mark, showup, itemid, priority, package, period, gender, count, meso, unk_1, for_pc, unk_3, extra_flags, donate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        try {
            ps.setInt(1, serialId);
            ps.setInt(2, price);
            ps.setInt(3, mark); // 
            ps.setInt(4, showup ? 1 : 0);
            ps.setInt(5, itemid);
            ps.setInt(6, priority);
            ps.setInt(7, 0);
            ps.setInt(8, period); //기간 (일단위)
            ps.setInt(9, gender);
            ps.setInt(10, count);
            ps.setInt(11, 0);
            ps.setInt(12, 0);
            ps.setInt(13, 0);
            ps.setInt(14, 0);
            ps.setInt(15, 0);
            ps.setInt(16, donate ? 1 : 0);
            System.err.println("캐시 아이템을 추가 했습니다. serialId: " + serialId + " 가격: " + price + " 아이템: " + itemid);
            ps.addBatch();
        } catch (Exception e) {
            e.printStackTrace();
            //System.out.println("Shopid : " + npcid);
            return;
        }
        ps.executeBatch();
        ps.close();
        con.close();
        con = null;
    }

    public static int getPlus(final int category_header) {
        switch (category_header) {
            case 100:
                return 2100;
            case 101:
                return 162;
            case 200:
                return 444;
            case 201:
                return 87;
            case 202:
                return 89;
            case 203:
                return 301;
            case 204:
                return 282;
            case 205:
                return 218;
            case 206:
                return 201;
            case 207:
                return 53;
            case 208:
                return 253;
            case 209:
                return 100;
            case 210:
                return 47;
            case 211:
                return 121;
            case 212:
                return 9;
            case 300:
                return 86;
            case 301:
                return 66;
            case 302:
                return 86;
            case 400:
                return 17;
            case 500:
                return 109;
            case 501:
                return 51;
            case 502:
                return 67;
            case 503:
                return 70;
            case 504:
                return 11;
            case 505:
                return 72;
            case 600:
                return 45;
            case 601:
                return 56;
            case 602:
                return 68;
            case 700:
                return 284;
            case 800:
                return 1150;
            case 900:
                return 61;
            case 910:
                return 15;
            case 920:
                return 88;
            case 930:
                return 64;
            default:
                return 0;
        }
    }

    public static int[] itemlist1_0 = {
        5062100, 2, //레드큐브
        5062100, 11,
        5062100, 23
    };

    public static int[] itemlist1_1 = {};

    public static int[] itemlist2_0 = {
        //아이디, 카운트, 프라이스, 안씀, 플래그
        1002998, 1, 2800, 0, 0,
        1002999, 1, 2800, 0, 0,
        1003001, 1, 2800, 0, 0,
        1004336, 1, 3000, 0, 0,
        1004393, 1, 3000, 0, 0,
        1004394, 1, 3000, 0, 0,
        1004395, 1, 3000, 0, 0,
        1004396, 1, 3000, 0, 0,
        1004398, 1, 3000, 0, 0,
        1004399, 1, 3000, 0, 0,
        1004400, 1, 3000, 0, 0,
        1004401, 1, 3000, 0, 0,
        1004402, 1, 3000, 0, 0,
        1004406, 1, 3000, 0, 0,
        1004467, 1, 3100, 0, 0,
        1004848, 1, 3000, 0, 0,
        1004849, 1, 3300, 0, 0,
        1004850, 1, 3000, 0, 0,
        1004851, 1, 3200, 0, 0,
        1004857, 1, 3000, 0, 0,
        1004884, 1, 3500, 0, 0,
        1005017, 1, 3000, 0, 0,
        1005028, 1, 3000, 0, 0,
        1005029, 1, 3000, 0, 0,
        1005030, 1, 3000, 0, 0,
        1005031, 1, 3000, 0, 0,
        1005055, 1, 3600, 0, 0,
        1005089, 1, 3600, 0, 0,
        1005094, 1, 3400, 0, 0,
        1005123, 1, 3300, 0, 0,
        1005151, 1, 3100, 0, 0,
        1005153, 1, 3000, 0, 0,
        1005154, 1, 3400, 0, 0,
        1005172, 1, 3600, 0, 0,
        1005176, 1, 3000, 0, 0,
        1005177, 1, 3000, 0, 0,
        1005178, 1, 3000, 0, 0,
        1005179, 1, 3000, 0, 0,
        1005180, 1, 3000, 0, 0,
        1005181, 1, 3000, 0, 0,
        1005182, 1, 3000, 0, 0,
        1005192, 1, 3600, 0, 0,
        1005194, 1, 3500, 0, 0,
        1005234, 1, 3000, 0, 0,
        1005235, 1, 3100, 0, 0,
        1005245, 1, 3200, 0, 0,
        1005250, 1, 3000, 0, 0,
        1005251, 1, 3000, 0, 0,
        1005262, 1, 3100, 0, 0,
        1005273, 1, 3000, 0, 0,
        1005283, 1, 3000, 0, 0,
        1005285, 1, 3500, 0, 0,
        1005287, 1, 3000, 0, 0,
        1005307, 1, 3500, 0, 0,
        1005362, 1, 3400, 0, 0,
        1005364, 1, 3000, 0, 0,
        1005366, 1, 3300, 0, 0,
        1005367, 1, 3000, 0, 0,
        1005388, 1, 3300, 0, 0,
        1005393, 1, 3000, 0, 0,
        1005409, 1, 3000, 0, 0,
        1005410, 1, 3200, 0, 0,
        1005431, 1, 2900, 0, 0,
        1005432, 1, 3100, 0, 0,
        1005433, 1, 3300, 0, 0,
        1005434, 1, 3300, 0, 0,
        1005459, 1, 3300, 0, 0,
        1005460, 1, 3400, 0, 0,
        1005504, 1, 3000, 0, 0,
        1005036, 1, 3300, 0, 0,
        1005135, 1, 3200, 0, 0,
        1005136, 1, 3100, 0, 0,
        1005137, 1, 3200, 0, 0,
        1005158, 1, 3100, 0, 0,
        1005159, 1, 3200, 0, 0,
        1005160, 1, 3200, 0, 0,
        1005161, 1, 3000, 0, 0,
        1005162, 1, 3000, 0, 0,
        1005582, 1, 3000, 0, 0,
        1005597, 1, 3100, 0, 0,
        1005611, 1, 2800, 0, 0,
        1000115, 1, 2900, 0, 0,
        1001137, 1, 3400, 0, 0,
        1005564, 1, 3300, 0, 0,
        1005579, 1, 3300, 0, 0,
        1005594, 1, 2900, 0, 0,
        1005595, 1, 3200, 0, 0,
        1005609, 1, 2900, 0, 0,
        1005610, 1, 3200, 0, 0,
        1005633, 1, 3100, 0, 0,
        1005659, 1, 3200, 0, 0,
        1004863, 1, 3200, 0, 0,
        1004506, 1, 3100, 0, 0,
        1005016, 1, 3100, 0, 0,
        1004919, 1, 3000, 0, 0,
        1004170, 1, 3000, 0, 0,
        1004793, 1, 3000, 0, 0,
        1005381, 1, 3000, 0, 0,
        1005391, 1, 3000, 0, 0,
        1005401, 1, 3000, 0, 0,
        1005416, 1, 3000, 0, 0,
        1005417, 1, 3000, 0, 0,
        1005421, 1, 3000, 0, 0,
        1005422, 1, 3000, 0, 0,
        1005423, 1, 3000, 0, 0,
        1004819, 1, 3000, 0, 0,
        1004820, 1, 3000, 0, 0,
        1000106, 1, 3000, 0, 0,
        1001129, 1, 3000, 0, 0,
        1004164, 1, 3000, 0, 0,
        1004776, 1, 3000, 0, 0,
        1004847, 1, 3000, 0, 0,
        1004931, 1, 3000, 0, 0,
        1005253, 1, 3000, 0, 0,
        1005268, 1, 3000, 0, 0,
        1005430, 1, 3000, 0, 0,
        1005500, 1, 3000, 0, 0,
        1005529, 1, 3000, 0, 0,
        1005585, 1, 3000, 0, 0,
        1005586, 1, 3000, 0, 0,
        1001136, 1, 2400, 0, 0,
        1005507, 1, 2800, 0, 0,
        1005649, 1, 3300, 0, 0,
        1005651, 1, 3300, 0, 0,
        1005652, 1, 3300, 0, 0,
        1005653, 1, 3300, 0, 0,
        1005654, 1, 3300, 0, 0,
        1005655, 1, 3300, 0, 0,
        1005656, 1, 3600, 0, 0,
        1005657, 1, 3300, 0, 0,
        1005658, 1, 3300, 0, 0,
        1005702, 1, 3000, 0, 0,
        1005703, 1, 3000, 0, 0
    };
    
    public static int[] itemlist2_0_Donate = {
        //아이디, 카운트, 프라이스, 안씀, 플래그
        1002998, 1, 2800, 0, 0,
        1002999, 1, 2800, 0, 0,
        1003001, 1, 2800, 0, 0,
        1004336, 1, 3000, 0, 0,
        1004393, 1, 3000, 0, 0,
        1004394, 1, 3000, 0, 0,
        1004395, 1, 3000, 0, 0,
        1004396, 1, 3000, 0, 0,
        1004398, 1, 3000, 0, 0,
        1004399, 1, 3000, 0, 0,
        1004400, 1, 3000, 0, 0,
        1004401, 1, 3000, 0, 0,
        1004402, 1, 3000, 0, 0,
        1004406, 1, 3000, 0, 0,
        1004467, 1, 3100, 0, 0,
        1004848, 1, 3000, 0, 0,
        1004849, 1, 3300, 0, 0,
        1004850, 1, 3000, 0, 0,
        1004851, 1, 3200, 0, 0,
        1004857, 1, 3000, 0, 0,
        1004884, 1, 3500, 0, 0,
        1005017, 1, 3000, 0, 0,
        1005028, 1, 3000, 0, 0,
        1005029, 1, 3000, 0, 0,
        1005030, 1, 3000, 0, 0,
        1005031, 1, 3000, 0, 0,
        1005055, 1, 3600, 0, 0,
        1005089, 1, 3600, 0, 0,
        1005094, 1, 3400, 0, 0,
        1005123, 1, 3300, 0, 0,
        1005151, 1, 3100, 0, 0,
        1005153, 1, 3000, 0, 0,
        1005154, 1, 3400, 0, 0,
        1005172, 1, 3600, 0, 0,
        1005176, 1, 3000, 0, 0,
        1005177, 1, 3000, 0, 0,
        1005178, 1, 3000, 0, 0,
        1005179, 1, 3000, 0, 0,
        1005180, 1, 3000, 0, 0,
        1005181, 1, 3000, 0, 0,
        1005182, 1, 3000, 0, 0,
        1005192, 1, 3600, 0, 0,
        1005194, 1, 3500, 0, 0,
        1005234, 1, 3000, 0, 0,
        1005235, 1, 3100, 0, 0,
        1005245, 1, 3200, 0, 0,
        1005250, 1, 3000, 0, 0,
        1005251, 1, 3000, 0, 0,
        1005262, 1, 3100, 0, 0,
        1005273, 1, 3000, 0, 0,
        1005283, 1, 3000, 0, 0,
        1005285, 1, 3500, 0, 0,
        1005287, 1, 3000, 0, 0,
        1005307, 1, 3500, 0, 0,
        1005362, 1, 3400, 0, 0,
        1005364, 1, 3000, 0, 0,
        1005366, 1, 3300, 0, 0,
        1005367, 1, 3000, 0, 0,
        1005388, 1, 3300, 0, 0,
        1005393, 1, 3000, 0, 0,
        1005409, 1, 3000, 0, 0,
        1005410, 1, 3200, 0, 0,
        1005431, 1, 2900, 0, 0,
        1005432, 1, 3100, 0, 0,
        1005433, 1, 3300, 0, 0,
        1005434, 1, 3300, 0, 0,
        1005459, 1, 3300, 0, 0,
        1005460, 1, 3400, 0, 0,
        1005036, 1, 3300, 0, 0,
        1005135, 1, 3200, 0, 0,
        1005136, 1, 3100, 0, 0,
        1005137, 1, 3200, 0, 0,
        1005158, 1, 3100, 0, 0,
        1005159, 1, 3200, 0, 0,
        1005160, 1, 3200, 0, 0,
        1005161, 1, 3000, 0, 0,
        1005162, 1, 3000, 0, 0,
        1005504, 1, 3000, 0, 0,
        1005582, 1, 3000, 0, 0,
        1005597, 1, 3100, 0, 0,
        1005611, 1, 2800, 0, 0,
        1000115, 1, 2900, 0, 0,
        1001137, 1, 3400, 0, 0,
        1005564, 1, 3300, 0, 0,
        1005579, 1, 3300, 0, 0,
        1005594, 1, 2900, 0, 0,
        1005595, 1, 3200, 0, 0,
        1005609, 1, 2900, 0, 0,
        1005610, 1, 3200, 0, 0,
        1005633, 1, 3100, 0, 0,
        1005659, 1, 3200, 0, 0,
        1004863, 1, 3200, 0, 0,
        1004506, 1, 3100, 0, 0,
        1005016, 1, 3100, 0, 0,
        1004919, 1, 3000, 0, 0,
        1004170, 1, 3000, 0, 0,
        1004793, 1, 3000, 0, 0,
        1005381, 1, 3000, 0, 0,
        1005391, 1, 3000, 0, 0,
        1005401, 1, 3000, 0, 0,
        1005416, 1, 3000, 0, 0,
        1005417, 1, 3000, 0, 0,
        1005421, 1, 3000, 0, 0,
        1005422, 1, 3000, 0, 0,
        1005423, 1, 3000, 0, 0,
        1004819, 1, 3000, 0, 0,
        1004820, 1, 3000, 0, 0,
        1000106, 1, 3000, 0, 0,
        1001129, 1, 3000, 0, 0,
        1004164, 1, 3000, 0, 0,
        1004776, 1, 3000, 0, 0,
        1004847, 1, 3000, 0, 0,
        1004931, 1, 3000, 0, 0,
        1005253, 1, 3000, 0, 0,
        1005268, 1, 3000, 0, 0,
        1005430, 1, 3000, 0, 0,
        1005500, 1, 3000, 0, 0,
        1005529, 1, 3000, 0, 0,
        1005585, 1, 3000, 0, 0,
        1005586, 1, 3000, 0, 0,
        1001136, 1, 2400, 0, 0,
        1005507, 1, 2800, 0, 0,
        1005702, 1, 3000, 0, 0,
        1005703, 1, 3000, 0, 0,
        1009999, 1, 4200, 0, 0,
        1009998, 1, 4200, 0, 0,
        1009997, 1, 4200, 0, 0,
        1009996, 1, 4200, 0, 0,
        1009995, 1, 4200, 0, 0,
        1009994, 1, 4500, 0, 0,
        1009993, 1, 4500, 0, 0
    };

    public static int[] itemlist2_1 = {
        //아이디, 카운트, 프라이스, 안씀, 플래그
        1012329, 1, 2000, 0, 0,
        1012517, 1, 2100, 0, 0,
        1012592, 1, 2100, 0, 0,
        1012636, 1, 2200, 0, 0,
        1012675, 1, 2100, 0, 0,
        1012676, 1, 2100, 0, 0,
        1012702, 1, 2000, 0, 0,
        1012703, 1, 2400, 0, 0,
        1012379, 1, 2300, 0, 0,
        1012472, 1, 2200, 0, 0,
        1012704, 1, 2300, 0, 0,
        1012668, 1, 2300, 0, 0,
        1012669, 1, 2500, 0, 0,
        1012601, 1, 2500, 0, 0,
        1012633, 1, 2500, 0, 0,
        1012659, 1, 2500, 0, 0,
        1012697, 1, 2300, 0, 0,
        1012712, 1, 2400, 0, 0,
        1012730, 1, 2200, 0, 0,
        1032322, 1, 2800, 0, 0
    };

    public static int[] itemlist2_2 = {
        //아이디, 카운트, 프라이스, 안씀, 플래그
        1022259, 1, 2100, 0, 0,
        1022279, 1, 2200, 0, 0,
        1022280, 1, 2100, 0, 0,
        1022285, 1, 2100, 0, 0,};

    public static int[] itemlist2_3 = {
        //아이디, 카운트, 프라이스, 안씀, 플래그
        1050168, 1, 3000, 0, 0,
        1050436, 1, 3100, 0, 0,
        1050479, 1, 3200, 0, 0,
        1050482, 1, 2900, 0, 0,
        1050488, 1, 3000, 0, 0,
        1050489, 1, 2900, 0, 0,
        1050504, 1, 3100, 0, 0,
        1050506, 1, 3100, 0, 0,
        1050537, 1, 3300, 0, 0,
        1051175, 1, 3000, 0, 0,
        1051546, 1, 3400, 0, 0,
        1051549, 1, 2900, 0, 0,
        1051555, 1, 3300, 0, 0,
        1051556, 1, 2900, 0, 0,
        1051572, 1, 3300, 0, 0,
        1051574, 1, 3000, 0, 0,
        1051576, 1, 3100, 0, 0,
        1051607, 1, 3200, 0, 0,
        1052211, 1, 2800, 0, 0,
        1052210, 1, 2800, 0, 0,
        1052213, 1, 2800, 0, 0,
        1052655, 1, 3400, 0, 0,
        1052671, 1, 3300, 0, 0,
        1052852, 1, 3200, 0, 0,
        1052870, 1, 3000, 0, 0,
        1052871, 1, 3000, 0, 0,
        1052873, 1, 3000, 0, 0,
        1052874, 1, 3000, 0, 0,
        1052876, 1, 3000, 0, 0,
        1053082, 1, 2800, 0, 0,
        1053095, 1, 2900, 0, 0,
        1053097, 1, 3000, 0, 0,
        1053098, 1, 3200, 0, 0,
        1053158, 1, 3200, 0, 0,
        1053296, 1, 3100, 0, 0,
        1053336, 1, 3000, 0, 0,
        1053346, 1, 3000, 0, 0,
        1053437, 1, 3400, 0, 0,
        1053358, 1, 3000, 0, 0,
        1053359, 1, 3000, 0, 0,
        1053376, 1, 2900, 0, 0,
        1053385, 1, 3000, 0, 0,
        1053386, 1, 3300, 0, 0,
        1053388, 1, 3300, 0, 0,
        1053440, 1, 3400, 0, 0,
        1053442, 1, 3200, 0, 0,
        1053443, 1, 3100, 0, 0,
        1053447, 1, 3100, 0, 0,
        1053500, 1, 2900, 0, 0,
        1053501, 1, 3000, 0, 0,
        1053502, 1, 3200, 0, 0,
        1053503, 1, 3000, 0, 0,
        1053518, 1, 3200, 0, 0,
        1053519, 1, 3200, 0, 0,
        1053520, 1, 3200, 0, 0,
        1053521, 1, 3200, 0, 0,
        1053522, 1, 3200, 0, 0,
        1053523, 1, 3200, 0, 0,
        1050462, 1, 3200, 0, 0,
        1051529, 1, 3000, 0, 0,
        1053234, 1, 3000, 0, 0,
        1053572, 1, 3200, 0, 0,
        1053614, 1, 3200, 0, 0,
        1053629, 1, 2000, 0, 0,
        1050556, 1, 2000, 0, 0,
        1050558, 1, 3300, 0, 0,
        1050562, 1, 2800, 0, 0,
        1050568, 1, 2900, 0, 0,
        1050570, 1, 2900, 0, 0,
        1052831, 1, 2800, 0, 0,
        1052832, 1, 2800, 0, 0,
        1052833, 1, 2800, 0, 0,
        1052834, 1, 2800, 0, 0,
        1053321, 1, 2800, 0, 0,
        1053322, 1, 2800, 0, 0,
        1053323, 1, 2800, 0, 0,
        1053324, 1, 2800, 0, 0,
        1053325, 1, 2800, 0, 0,
        1051626, 1, 3300, 0, 0,
        1051628, 1, 3200, 0, 0,
        1051634, 1, 3200, 0, 0,
        1051640, 1, 3200, 0, 0,
        1051642, 1, 3100, 0, 0,
        1052975, 1, 3000, 0, 0,
        1053625, 1, 3000, 0, 0,
        1053626, 1, 3000, 0, 0,
        1053637, 1, 3000, 0, 0,
        1053641, 1, 3200, 0, 0,
        1053643, 1, 3100, 0, 0,
        1053644, 1, 3200, 0, 0,
        1051635, 1, 2500, 0, 0,
        1053547, 1, 3000, 0, 0,
        1050451, 1, 3100, 0, 0,
        1053233, 1, 3200, 0, 0,
        1053310, 1, 3200, 0, 0,
        1053441, 1, 3300, 0, 0,
        1053457, 1, 3200, 0, 0,
        1053458, 1, 3200, 0, 0,
        1053459, 1, 3200, 0, 0,
        1053460, 1, 3200, 0, 0,
        1050437, 1, 3000, 0, 0,
        1051504, 1, 3000, 0, 0,
        1053464, 1, 3000, 0, 0,
        1053545, 1, 3000, 0, 0,
        1050533, 1, 3200, 0, 0,
        1051604, 1, 3200, 0, 0,
        1053049, 1, 3200, 0, 0,
        1053096, 1, 3200, 0, 0,
        1053141, 1, 3200, 0, 0,
        1053366, 1, 3200, 0, 0,
        1053499, 1, 3200, 0, 0,
        1053491, 1, 3100, 0, 0,
        1053492, 1, 3100, 0, 0,
        1053493, 1, 3100, 0, 0,
        1053586, 1, 2800, 0, 0,
        1053587, 1, 2800, 0, 0,
        1053588, 1, 2800, 0, 0,
        1053589, 1, 2800, 0, 0,
        1053590, 1, 2800, 0, 0,
        1053593, 1, 3000, 0, 0,
        1053594, 1, 3000, 0, 0,
        1053600, 1, 3300, 0, 0,
        1050574, 1, 3000, 0, 0,
        1051646, 1, 3000, 0, 0,
        1053640, 1, 4000, 0, 0
    };
    
    public static int[] itemlist2_3_Donate = {
        //아이디, 카운트, 프라이스, 안씀, 플래그
        1050168, 1, 3000, 0, 0,
        1050436, 1, 3100, 0, 0,
        1050479, 1, 3200, 0, 0,
        1050482, 1, 2900, 0, 0,
        1050488, 1, 3000, 0, 0,
        1050489, 1, 2900, 0, 0,
        1050504, 1, 3100, 0, 0,
        1050506, 1, 3100, 0, 0,
        1050537, 1, 3300, 0, 0,
        1051175, 1, 3000, 0, 0,
        1051546, 1, 3400, 0, 0,
        1051549, 1, 2900, 0, 0,
        1051555, 1, 3300, 0, 0,
        1051556, 1, 2900, 0, 0,
        1051572, 1, 3300, 0, 0,
        1051574, 1, 3000, 0, 0,
        1051576, 1, 3100, 0, 0,
        1051607, 1, 3200, 0, 0,
        1052211, 1, 2800, 0, 0,
        1052210, 1, 2800, 0, 0,
        1052213, 1, 2800, 0, 0,
        1052655, 1, 3400, 0, 0,
        1052671, 1, 3300, 0, 0,
        1052852, 1, 3200, 0, 0,
        1052870, 1, 3000, 0, 0,
        1052871, 1, 3000, 0, 0,
        1052873, 1, 3000, 0, 0,
        1052874, 1, 3000, 0, 0,
        1052876, 1, 3000, 0, 0,
        1053082, 1, 2800, 0, 0,
        1053095, 1, 2900, 0, 0,
        1053097, 1, 3000, 0, 0,
        1053098, 1, 3200, 0, 0,
        1053158, 1, 3200, 0, 0,
        1053296, 1, 3100, 0, 0,
        1053336, 1, 3000, 0, 0,
        1053346, 1, 3000, 0, 0,
        1053437, 1, 3400, 0, 0,
        1053358, 1, 3000, 0, 0,
        1053359, 1, 3000, 0, 0,
        1053376, 1, 2900, 0, 0,
        1053385, 1, 3000, 0, 0,
        1053386, 1, 3300, 0, 0,
        1053388, 1, 3300, 0, 0,
        1053440, 1, 3400, 0, 0,
        1053442, 1, 3200, 0, 0,
        1053443, 1, 3100, 0, 0,
        1053447, 1, 3100, 0, 0,
        1053500, 1, 2900, 0, 0,
        1053501, 1, 3000, 0, 0,
        1053502, 1, 3200, 0, 0,
        1053503, 1, 3000, 0, 0,
        1053518, 1, 3200, 0, 0,
        1053519, 1, 3200, 0, 0,
        1053520, 1, 3200, 0, 0,
        1053521, 1, 3200, 0, 0,
        1053522, 1, 3200, 0, 0,
        1053523, 1, 3200, 0, 0,
        1050462, 1, 3200, 0, 0,
        1051529, 1, 3000, 0, 0,
        1053234, 1, 3000, 0, 0,
        1053572, 1, 3200, 0, 0,
        1053614, 1, 3200, 0, 0,
        1053629, 1, 2000, 0, 0,
        1050556, 1, 2000, 0, 0,
        1050558, 1, 3300, 0, 0,
        1050562, 1, 2800, 0, 0,
        1050568, 1, 2900, 0, 0,
        1050570, 1, 2900, 0, 0,
        1052831, 1, 2800, 0, 0,
        1052832, 1, 2800, 0, 0,
        1052833, 1, 2800, 0, 0,
        1052834, 1, 2800, 0, 0,
        1053321, 1, 2800, 0, 0,
        1053322, 1, 2800, 0, 0,
        1053323, 1, 2800, 0, 0,
        1053324, 1, 2800, 0, 0,
        1053325, 1, 2800, 0, 0,
        1051626, 1, 3300, 0, 0,
        1051628, 1, 3200, 0, 0,
        1051634, 1, 3200, 0, 0,
        1051640, 1, 3200, 0, 0,
        1051642, 1, 3100, 0, 0,
        1052975, 1, 3000, 0, 0,
        1053625, 1, 3000, 0, 0,
        1053626, 1, 3000, 0, 0,
        1053637, 1, 3000, 0, 0,
        1053641, 1, 3200, 0, 0,
        1053643, 1, 3100, 0, 0,
        1053644, 1, 3200, 0, 0,
        1051635, 1, 2500, 0, 0,
        1053547, 1, 3000, 0, 0,
        1050451, 1, 3100, 0, 0,
        1053233, 1, 3200, 0, 0,
        1053310, 1, 3200, 0, 0,
        1053441, 1, 3300, 0, 0,
        1053457, 1, 3200, 0, 0,
        1053458, 1, 3200, 0, 0,
        1053459, 1, 3200, 0, 0,
        1053460, 1, 3200, 0, 0,
        1050437, 1, 3000, 0, 0,
        1051504, 1, 3000, 0, 0,
        1053464, 1, 3000, 0, 0,
        1053545, 1, 3000, 0, 0,
        1050533, 1, 3200, 0, 0,
        1051604, 1, 3200, 0, 0,
        1053049, 1, 3200, 0, 0,
        1053096, 1, 3200, 0, 0,
        1053141, 1, 3200, 0, 0,
        1053366, 1, 3200, 0, 0,
        1053499, 1, 3200, 0, 0,
        1053491, 1, 3100, 0, 0,
        1053492, 1, 3100, 0, 0,
        1053493, 1, 3100, 0, 0,
        1053586, 1, 2800, 0, 0,
        1053587, 1, 2800, 0, 0,
        1053588, 1, 2800, 0, 0,
        1053589, 1, 2800, 0, 0,
        1053590, 1, 2800, 0, 0,
        1053593, 1, 3000, 0, 0,
        1053594, 1, 3000, 0, 0,
        1053600, 1, 3300, 0, 0,
        1050574, 1, 3000, 0, 0,
        1051646, 1, 3000, 0, 0,
        1059999, 1, 4800, 0, 0
    };

    public static int[] itemlist2_4 = {
        //아이디, 카운트, 프라이스, 안씀, 플래그
        1042349, 1, 2900, 0, 0,
        1042357, 1, 3100, 0, 0,
        1042358, 1, 3000, 0, 0,
        1042422, 1, 3000, 0, 0
    };

    public static int[] itemlist2_5 = { //아이디, 카운트, 프라이스, 안씀, 플래그
        1062282, 1, 2800, 0, 0
    };

    public static int[] itemlist2_6 = {
        //아이디, 카운트, 프라이스, 안씀, 플래그
        1072443, 1, 1400, 0, 0,
        1073358, 1, 1500, 0, 0,
        1073359, 1, 1500, 0, 0,
        1070106, 1, 1500, 0, 0,
        1071122, 1, 1400, 0, 0,
        1073169, 1, 1300, 0, 0,
        1073259, 1, 1600, 0, 0,
        1073272, 1, 1600, 0, 0,
        1073315, 1, 1700, 0, 0,
        1073323, 1, 1500, 0, 0,
        1073404, 1, 1800, 0, 0,
        1073415, 1, 1800, 0, 0,
        1073428, 1, 1500, 0, 0,
        1073436, 1, 1500, 0, 0,
        1073438, 1, 1400, 0, 0,
        1073444, 1, 1600, 0, 0,
        1073450, 1, 1800, 0, 0,
        1073455, 1, 1800, 0, 0,
        1073463, 1, 1700, 0, 0,
        1073468, 1, 1600, 0, 0,
        1073313, 1, 1500, 0, 0,
        1073314, 1, 1500, 0, 0,
        1073430, 1, 1500, 0, 0,
        1073475, 1, 1700, 0, 0,
        1073484, 1, 1400, 0, 0,
        1073486, 1, 1400, 0, 0,
        1073491, 1, 1500, 0, 0,
        1073284, 1, 1200, 0, 0,
        1073287, 1, 1200, 0, 0,
        1073489, 1, 1300, 0, 0,
        1073490, 1, 1300, 0, 0,
        1073509, 1, 1300, 0, 0
    };

    public static int[] itemlist2_7 = {
        //아이디, 카운트, 프라이스, 안씀, 플래그
        1080008, 1, 1500, 0, 0,
        1081014, 1, 1600, 0, 0,
        1082520, 1, 1600, 0, 0,
        1082565, 1, 1600, 0, 0,
        1082272, 1, 1700, 0, 0,
        1082742, 1, 1400, 0, 0,
        1082743, 1, 1700, 0, 0,
        1082690, 1, 1600, 0, 0,
        1082703, 1, 1700, 0, 0,
        1082704, 1, 1700, 0, 0,
        1082705, 1, 1400, 0, 0,
        1082730, 1, 1400, 0, 0,
        1082744, 1, 1500, 0, 0,
        1082751, 1, 1800, 0, 0
    };

    public static int[] itemlist2_8 = {
        //아이디, 카운트, 프라이스, 안씀, 플래그
        1702235, 1, 4200, 0, 0,
        1702336, 1, 4500, 0, 0,
        1702639, 1, 4600, 0, 0,
        1702680, 1, 4600, 0, 0,
        1702718, 1, 4600, 0, 0,
        1702721, 1, 4500, 0, 0,
        1702809, 1, 4700, 0, 0,
        1702811, 1, 4800, 0, 0,
        1702829, 1, 4700, 0, 0,
        1702831, 1, 4500, 0, 0,
        1702880, 1, 4900, 0, 0,
        1702839, 1, 4500, 0, 0,
        1702726, 1, 4500, 0, 0,
        1702728, 1, 4600, 0, 0,
        1702735, 1, 4400, 0, 0,
        1702736, 1, 4700, 0, 0,
        1702744, 1, 4700, 0, 0,
        1702755, 1, 4700, 0, 0,
        1702759, 1, 4600, 0, 0,
        1702766, 1, 4500, 0, 0,
        1702770, 1, 4500, 0, 0,
        1702774, 1, 4500, 0, 0,
        1702779, 1, 4700, 0, 0,
        1702790, 1, 4800, 0, 0,
        1702795, 1, 4800, 0, 0,
        1702804, 1, 4900, 0, 0,
        1702807, 1, 4600, 0, 0,
        1702810, 1, 4900, 0, 0,
        1702815, 1, 4900, 0, 0,
        1702826, 1, 4900, 0, 0,
        1702830, 1, 4300, 0, 0,
        1702837, 1, 4600, 0, 0,
        1702844, 1, 4500, 0, 0,
        1702849, 1, 4600, 0, 0,
        1702850, 1, 4900, 0, 0,
        1702858, 1, 4800, 0, 0,
        1702866, 1, 4800, 0, 0,
        1702867, 1, 4800, 0, 0,
        1702871, 1, 4800, 0, 0,
        1702877, 1, 4800, 0, 0,
        1702881, 1, 4900, 0, 0,
        1702911, 1, 4900, 0, 0,
        1702925, 1, 4900, 0, 0,
        1702950, 1, 4900, 0, 0,
        1702971, 1, 4600, 0, 0,
        1709999, 1, 4500, 0, 0,
        1342069, 1, 5500, 0, 0,
        1702541, 1, 4500, 0, 0,
        1702716, 1, 4800, 0, 0,
        1702745, 1, 4800, 0, 0,
        1702952, 1, 4500, 0, 0,
        1702994, 1, 4800, 0, 0,
        1702817, 1, 4600, 0, 0,
        1702838, 1, 4500, 0, 0,
        1702963, 1, 4500, 0, 0,
        1703014, 1, 4600, 0, 0,
        1703022, 1, 4500, 0, 0,
        1703030, 1, 4700, 0, 0,
        1703037, 1, 4700, 0, 0,
        1703043, 1, 4800, 0, 0,
        1703045, 1, 4800, 0, 0,
        1703046, 1, 4900, 0, 0,
        1703052, 1, 5900, 0, 0,
        1703053, 1, 4500, 0, 0,
        1702943, 1, 4500, 0, 0,
        1702983, 1, 4500, 0, 0,
        1703029, 1, 4500, 0, 0,
        1702556, 1, 4800, 0, 0,
        1702964, 1, 5000, 0, 0,
        1703005, 1, 5200, 0, 0,
        1703024, 1, 5500, 0, 0,
        1703049, 1, 5500, 0, 0,
        1703051, 1, 5000, 0, 0,
        1703061, 1, 4800, 0, 0
    };

    public static int[] itemlist2_8_Donate = {
        //아이디, 카운트, 프라이스, 안씀, 플래그
        1702235, 1, 4200, 0, 0,
        1702336, 1, 4500, 0, 0,
        1702639, 1, 4600, 0, 0,
        1702680, 1, 4600, 0, 0,
        1702718, 1, 4600, 0, 0,
        1702721, 1, 4500, 0, 0,
        1702809, 1, 4700, 0, 0,
        1702811, 1, 4800, 0, 0,
        1702829, 1, 4700, 0, 0,
        1702831, 1, 4500, 0, 0,
        1702880, 1, 4900, 0, 0,
        1702839, 1, 4500, 0, 0,
        1702726, 1, 4500, 0, 0,
        1702728, 1, 4600, 0, 0,
        1702735, 1, 4400, 0, 0,
        1702736, 1, 4700, 0, 0,
        1702744, 1, 4700, 0, 0,
        1702755, 1, 4700, 0, 0,
        1702759, 1, 4600, 0, 0,
        1702766, 1, 4500, 0, 0,
        1702770, 1, 4500, 0, 0,
        1702774, 1, 4500, 0, 0,
        1702779, 1, 4700, 0, 0,
        1702790, 1, 4800, 0, 0,
        1702795, 1, 4800, 0, 0,
        1702804, 1, 4900, 0, 0,
        1702807, 1, 4600, 0, 0,
        1702810, 1, 4900, 0, 0,
        1702815, 1, 4900, 0, 0,
        1702826, 1, 4900, 0, 0,
        1702830, 1, 4300, 0, 0,
        1702837, 1, 4600, 0, 0,
        1702844, 1, 4500, 0, 0,
        1702849, 1, 4600, 0, 0,
        1702850, 1, 4900, 0, 0,
        1702858, 1, 4800, 0, 0,
        1702866, 1, 4800, 0, 0,
        1702867, 1, 4800, 0, 0,
        1702871, 1, 4800, 0, 0,
        1702877, 1, 4800, 0, 0,
        1702881, 1, 4900, 0, 0,
        1702911, 1, 4900, 0, 0,
        1702925, 1, 4900, 0, 0,
        1702950, 1, 4900, 0, 0,
        1702971, 1, 4600, 0, 0,
        1709999, 1, 4500, 0, 0,
        1342069, 1, 2000, 0, 0,
        1702541, 1, 4500, 0, 0,
        1702716, 1, 4800, 0, 0,
        1702745, 1, 4800, 0, 0,
        1702952, 1, 4500, 0, 0,
        1702994, 1, 4800, 0, 0,
        1702817, 1, 4600, 0, 0,
        1702838, 1, 4500, 0, 0,
        1702963, 1, 4500, 0, 0,
        1703014, 1, 4600, 0, 0,
        1703022, 1, 4500, 0, 0,
        1703030, 1, 4700, 0, 0,
        1703037, 1, 4700, 0, 0,
        1703043, 1, 4800, 0, 0,
        1703045, 1, 4800, 0, 0,
        1703046, 1, 4900, 0, 0,
        1703052, 1, 5900, 0, 0,
        1703053, 1, 4500, 0, 0,
        1702943, 1, 4500, 0, 0,
        1702983, 1, 4500, 0, 0,
        1703029, 1, 4500, 0, 0,
        1702556, 1, 4800, 0, 0,
        1702964, 1, 5000, 0, 0,
        1703005, 1, 5200, 0, 0,
        1703024, 1, 5500, 0, 0,
        1702585, 1, 5000, 0, 0,
        1703051, 1, 5000, 0, 0,
        1703061, 1, 4800, 0, 0
    };
    
    public static int[] itemlist2_9 = {
        1115210, 1, 2500, 0, 0,
        1115311, 1, 2500, 0, 0    
    };
        
    public static int[] itemlist2_9_Donate = {
    };

    public static int[] itemlist2_11 = {
        1103075, 1, 3300, 0, 0,
        1100004, 1, 2800, 0, 0,
        1102848, 1, 3000, 0, 0,
        1102992, 1, 3300, 0, 0,
        1103096, 1, 3000, 0, 0,
        1103119, 1, 3500, 0, 0,
        1103131, 1, 3300, 0, 0,
        1103149, 1, 2800, 0, 0,
        1103193, 1, 2800, 0, 0,
        1103250, 1, 2800, 0, 0,
        1103301, 1, 3000, 0, 0,
        1103304, 1, 3000, 0, 0,
        1103305, 1, 3000, 0, 0,
        1103315, 1, 2500, 0, 0
    };

    public static int[] itemlist3 = {
        1152000, 1,
        1152001, 1, //toenail only comes when db is out.
        3010018, 1, //chairs
        2000005, 100, //chairs
    };

    public static int[] itemlist4_0 = {
        1492190, 1, 10000, 81, 0,
        1482179, 1, 10000, 82, 0,
        1472226, 1, 10000, 83, 0,
        1462204, 1, 10000, 84, 0,
        1452216, 1, 10000, 85, 0,
        1442234, 1, 10000, 86, 0,
        1432178, 1, 10000, 87, 0,
        1422152, 1, 10000, 88, 0,
        1412147, 1, 10000, 89, 0,
        1402210, 1, 10000, 90, 0,
        1382222, 1, 10000, 91, 0,
        1372188, 1, 10000, 92, 0,
        1332238, 1, 10000, 93, 0,
        1322215, 1, 10000, 94, 0,
        1312165, 1, 10000, 95, 0,
        1302289, 1, 10000, 96, 0,
        1072853, 1, 5000, 98, 0,//레볼루션 슈즈
        1082540, 1, 5000, 97, 0,//레볼루션 글러브
        1052647, 1, 5000, 99, 0,//레볼루션 슈트
        1003946, 1, 5000, 100, 0,//레볼루션 햇
    };

    public static int[] itemlist5_0 = {
        2434561, 1, 1000, 113, 0
    };
    
    public static int[] itemlist5_0_Donate = {
        2434561, 1, 1000, 113, 0,
        2439983, 1, 4500, 114, 0,
        2439984, 1, 4500, 115, 0
    };

    public static int[] itemlist5_2 = {
        5062100, 2, //레드큐브
        5062100, 11,
        5062100, 23,};

    public static int[] itemlist6_0 = {
        5000013, 1, 1000, 0, 1,//코끼리
        5000017, 1, 1000, 0, 1,//로봇
        5000015, 1, 3000, 0, 2,//루돌프
        5000041, 1, 3000, 0, 2,//꼬마 눈사람
        5000043, 1, 3000, 0, 3,//고슴도치
        5000065, 1, 5000, 0, 4,//스쿠버 더키
        5000072, 1, 5000, 0, 4,//스컹크
        5000055, 1, 9900, 0, 4,//쁘띠 오르카
        5000056, 1, 9900, 0, 4,//쁘띠 스우
        5000057, 1, 9900, 0, 4,//병약오르카
        5000058, 1, 9900, 0, 4,//촉촉 케익
        5000059, 1, 9900, 0, 4,//고소 파이
        5000060, 1, 9900, 0, 4,//달콤 캔디
        5000069, 1, 9900, 0, 4,//블랙 루시드
        5000070, 1, 9900, 0, 4,//크림 루시드
        5000071, 1, 9900, 0, 4,//핑크 루시드
        5000075, 1, 9900, 0, 4,//핑크빈 빠방
        5000076, 1, 9900, 0, 4,//슬라임 빠방
        5000077, 1, 9900, 0, 4,//예티 빠방
        5000672, 1, 9900, 0, 4,//바다 오르카
        5000673, 1, 9900, 0, 4,//사과 오르카
        5000674, 1, 9900, 0, 4,//망고 오르카
        5000721, 1, 9900, 0, 4,//우주 오르카
        5000722, 1, 9900, 0, 4,//우주 스우
        5000723, 1, 9900, 0, 4,//우주 팬텀
        5000751, 1, 9900, 0, 4,//쿠키
        5000752, 1, 9900, 0, 4,//크로캉
        5000753, 1, 9900, 0, 4,//머랭
        5000979, 1, 9900, 0, 4,//자냥
        5000980, 1, 9900, 0, 4,//조냥
        5000981, 1, 9900, 0, 4,//후냥
    };

    public static int[] itemlist6_1 = {
        1802022, 1, 500, 0, 0,//원숭이 전용 희귀템1
        1802023, 1, 500, 0, 0,//원숭이 전용 희귀템2
        1802021, 1, 500, 0, 0,//코끼리 모자
        //5000017, 1, 1000, 0, 1,//로봇은 없다!
        1802019, 1, 1900, 0, 0,//루돌프의 썰매
        1802053, 1, 1900, 0, 0,//눈사람 모자
        1802047, 1, 1900, 0, 0,//고슴도치
        1802067, 1, 2900, 0, 0,//스쿠버 더키
        1802068, 1, 2900, 0, 0,//스컹크
        1802082, 1, 3300, 0, 0,//쁘 오
        1802083, 1, 3300, 0, 0,//쁘 스
        1802084, 1, 3300, 0, 0,//병 오
        1802085, 1, 3300, 0, 0,//케
        1802086, 1, 3300, 0, 0,//파
        1802087, 1, 3300, 0, 0,//캔
        1802088, 1, 3300, 0, 0,//연
        1802089, 1, 3300, 0, 0,//순
        1802090, 1, 3300, 0, 0,//자
        1802091, 1, 3300, 0, 0,//냥크빈
        1802092, 1, 3300, 0, 0,//덕라임
        1802093, 1, 3300, 0, 0,//펭예티
        1802589, 1, 3300, 0, 0,//빨간두건 오르카
        1802590, 1, 3300, 0, 0,//노란두건 오르카
        1802591, 1, 3300, 0, 0,//파란두건 오르카
        1802603, 1, 3300, 0, 0,//우주 꼬맹이
        1802615, 1, 3300, 0, 0,//왜뭐왜
        1802667, 1, 3300, 0, 0,//졸려졸려
    };

    public static int[] itemlist6_2 = {
        5240017, 6, 2500, 0, 0,//눈사람 밥
        5240017, 1, 500, 0, 0,//눈사람 밥
        5240001, 6, 2500, 0, 0,//건전지
        5240001, 1, 500, 0, 0,//건전지
        5240019, 6, 2500, 0, 0,//건전지
        5240019, 1, 500, 0, 0,//건전지
        5240169, 6, 2500, 0, 0,//오가닉 원더 쿠키
        5240169, 1, 500, 0, 0,//오가닉 원더 쿠키
        5240155, 6, 2500, 0, 0,//동화나라 빵
        5240155, 1, 500, 0, 0,//동화나라 빵
        5240162, 6, 2500, 0, 0,//우주식량
        5240162, 1, 500, 0, 0,//우주식량
        5240167, 6, 2500, 0, 0,//강아지 맘마
        5240167, 1, 500, 0, 0,//강아지 맘마
        5240184, 6, 2500, 0, 0,//잘자냥 까까
        5240184, 1, 500, 0, 0,//잘자냥 까까
    };

    public static int[] itemlist4 = {
        2340000, 1, //rares
        1152000, 5, 1152001, 5, 1152004, 5, 1152005, 5, 1152006, 5, 1152007, 5, 1152008, 5, //toenail only comes when db is out.
        1152064, 5, 1152065, 5, 1152066, 5, 1152067, 5, 1152070, 5, 1152071, 5, 1152072, 5, 1152073, 5,
        3010019, 2, //chairs
    };

    public static int[] itemlist5 = {
        2340000, 1, //rares
        1152000, 5, 1152001, 5, 1152004, 5, 1152005, 5, 1152006, 5, 1152007, 5, 1152008, 5, //toenail only comes when db is out.
        1152064, 5, 1152065, 5, 1152066, 5, 1152067, 5, 1152070, 5, 1152071, 5, 1152072, 5, 1152073, 5,
        3010019, 2, //chairs
    };

    public static int[] itemlist6 = {
        2340000, 1, //rares
        1152000, 5, 1152001, 5, 1152004, 5, 1152005, 5, 1152006, 5, 1152007, 5, 1152008, 5, //toenail only comes when db is out.
        1152064, 5, 1152065, 5, 1152066, 5, 1152067, 5, 1152070, 5, 1152071, 5, 1152072, 5, 1152073, 5,
        3010019, 2, //chairs
    };
}
