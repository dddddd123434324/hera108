package server;

import database.DatabaseConnection;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.CashItemInfo.CashModInfo;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class CashItemFactory {

    private final static CashItemFactory instance = new CashItemFactory();
    private final static int[] bestItems = new int[]{30000031, 30000031, 30000031, 30000031, 30000031};
    private final Map<Integer, CashItemInfo> itemStats = new HashMap<Integer, CashItemInfo>();
    private final Map<Integer, List<Integer>> itemPackage = new HashMap<Integer, List<Integer>>();
    private final Map<Integer, CashModInfo> normalItemMods = new HashMap<Integer, CashModInfo>();
    private final Map<Integer, CashModInfo> donateItemMods = new HashMap<Integer, CashModInfo>();
    private final MapleDataProvider data = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/Etc.wz"));
    private final List<Integer> disabledSNs = new LinkedList<Integer>();

    public static final CashItemFactory getInstance() {
        return instance;
    }

    public void initialize() {
        final List<MapleData> cccc = data.getData("Commodity.img").getChildren();
        for (MapleData field : cccc) {
            int id = Integer.parseInt(field.getName());
            final int SN = MapleDataTool.getIntConvert("SN", field, 0);
            int PERIOD = MapleDataTool.getIntConvert("Period", field, 0);
            /*if (PERIOD == 365) {
             PERIOD = 7;
             System.out.println(PERIOD);
             }*/
            boolean bln = MapleDataTool.getIntConvert("OnSale", field, 0) > 0;
            final CashItemInfo stats = new CashItemInfo(MapleDataTool.getIntConvert("ItemId", field, 0),
                    MapleDataTool.getIntConvert("Count", field, 1),
                    MapleDataTool.getIntConvert("Price", field, 0),
                    SN,
                    PERIOD,
                    MapleDataTool.getIntConvert("Gender", field, 2),
                    bln && MapleDataTool.getIntConvert("Price", field, 0) > 0);

            if (SN > 0) {
                itemStats.put(SN, stats);
            }
        }

        //<editor-fold defaultstate="collapsed" desc="Disabled Items">
        //썸머로얄쿠폰
        //disabledSNs.add(50000101);
        //뿌리기/퀵배송 이용권
        //생일 축하해
        /*
        disabledSNs.add(10001153);
        disabledSNs.add(10001528);
        disabledSNs.add(30200049);
        disabledSNs.add(30200050);
        disabledSNs.add(30200069);
        disabledSNs.add(30200070);
        disabledSNs.add(30200063);
        disabledSNs.add(30200064);
        disabledSNs.add(30200060);
        disabledSNs.add(30200061);
        disabledSNs.add(30200079);
        disabledSNs.add(30200078);
        disabledSNs.add(30200058);
        disabledSNs.add(30200057);
        disabledSNs.add(30200075);
        disabledSNs.add(30200074);
        disabledSNs.add(30200076);
         */
        disabledSNs.add(40000000);
        disabledSNs.add(40000001);
        disabledSNs.add(40000002);
        disabledSNs.add(40000003);
        disabledSNs.add(40000004);
        disabledSNs.add(40000005);

        //disabledSNs.add(30000023); //고돌
        //퀵배송 이용권
        //disabledSNs.add(30100035);
        //disabledSNs.add(30100036);
        disabledSNs.add(10000804);
        disabledSNs.add(50200014);
        disabledSNs.add(50200033);
        disabledSNs.add(50200038);

        disabledSNs.add(10001570);
        disabledSNs.add(70000197);
        disabledSNs.add(10001608);
        disabledSNs.add(70000198);

        //모래시계
        //1일
        disabledSNs.add(50200041);
        disabledSNs.add(50200044);

        //7일
        disabledSNs.add(50200053);

        //20일
        disabledSNs.add(50200054);

        //50일
        disabledSNs.add(10001815);
        disabledSNs.add(50200055);

        //99일
        disabledSNs.add(50200056);

        //듀블 마북 + 블레이드 주문서 60% 패키지
        //메인 카테고리
        disabledSNs.add(10001918);
        disabledSNs.add(10001919);
        disabledSNs.add(10001920);
        disabledSNs.add(10001921);
        disabledSNs.add(10001922);
        disabledSNs.add(10001923);

        //스페셜 카테고리
        disabledSNs.add(40000012);
        disabledSNs.add(40000013);
        disabledSNs.add(40000014);
        disabledSNs.add(40000015);
        disabledSNs.add(40000016);
        disabledSNs.add(40000017);

        //카르마의 가위
        disabledSNs.add(10001666);
        disabledSNs.add(50200043);

        //플래티넘 카르마의 가위
        disabledSNs.add(10001854);
        disabledSNs.add(50200059);

        //펜던트 슬롯늘리기 : 7일
        disabledSNs.add(21000017);

        //PC방 전용 패키지
        disabledSNs.add(21000018);
        disabledSNs.add(21000019);
        disabledSNs.add(21000020);
        disabledSNs.add(21000021);
        disabledSNs.add(21000022);
        disabledSNs.add(21000023);
        disabledSNs.add(21000024);
        disabledSNs.add(21000025);
        disabledSNs.add(21000026);
        disabledSNs.add(21000027);
        disabledSNs.add(21000028);

        disabledSNs.add(10001988);
        disabledSNs.add(10001993);
        disabledSNs.add(10001906);
        disabledSNs.add(10001907);
        disabledSNs.add(10001908);
        disabledSNs.add(10001909);
        disabledSNs.add(10001910);
        disabledSNs.add(10001911);

        //큐브
        disabledSNs.add(50200066);
        disabledSNs.add(50200067);

        //미7 줌서
        disabledSNs.add(70000273);
        disabledSNs.add(10001994);

        //미분류
        disabledSNs.add(10001912);
        disabledSNs.add(10001913);
        disabledSNs.add(10001914);
        disabledSNs.add(10001915);
        disabledSNs.add(10001916);
        disabledSNs.add(10001917);
        disabledSNs.add(10001926);
        disabledSNs.add(10001927);
        disabledSNs.add(10001928);
        disabledSNs.add(10001665);
        //disabledSNs.add(10000872);

        disabledSNs.add(40000006);
        disabledSNs.add(40000007);
        disabledSNs.add(40000008);
        disabledSNs.add(40000009);
        disabledSNs.add(40000010);
        disabledSNs.add(40000011);

        disabledSNs.add(50200057);
        disabledSNs.add(50200042);
        disabledSNs.add(70000263);
        disabledSNs.add(70000264);
        disabledSNs.add(70000265);

        //</editor-fold>
        final MapleData b = data.getData("CashPackage.img");
        for (MapleData c : b.getChildren()) {
            if (c.getChildByPath("SN") == null) {
                continue;
            }
            final List<Integer> packageItems = new ArrayList<Integer>();
            for (MapleData d : c.getChildByPath("SN").getChildren()) {
                packageItems.add(MapleDataTool.getIntConvert(d));
            }
            itemPackage.put(Integer.parseInt(c.getName()), packageItems);
        }

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            normalItemMods.clear();
            donateItemMods.clear();
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * FROM cashshop_modified_items");
            rs = ps.executeQuery();
            while (rs.next()) {
                CashModInfo ret = new CashModInfo(rs.getInt("serial"), rs.getInt("discount_price"), rs.getInt("mark"), rs.getInt("showup") > 0, rs.getInt("itemid"), rs.getInt("priority"), rs.getInt("package") > 0, rs.getInt("period"), rs.getInt("gender"), rs.getInt("count"), rs.getInt("meso"), rs.getInt("unk_1"), rs.getInt("for_pc"), rs.getInt("unk_3"), rs.getInt("extra_flags"), rs.getInt("donate") > 0);
                if (ret.donateShop) {
                    donateItemMods.put(ret.sn, ret);
                    if (ret.showUp) {
                        final CashItemInfo cc = itemStats.get(Integer.valueOf(ret.sn));
                        if (cc != null) {
                            ret.toCItem(cc); //init
                        }
                    }
                } else {
                    normalItemMods.put(ret.sn, ret);
                    if (ret.showUp) {
                        final CashItemInfo cc = itemStats.get(Integer.valueOf(ret.sn));
                        if (cc != null) {
                            ret.toCItem(cc); //init
                        }
                    }
                }
            }

        } catch (Exception e) {
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

    public List<Integer> getIncubatorSN() {
        return disabledSNs;
    }

    public final CashItemInfo getSimpleItem(int sn) {
        return itemStats.get(sn);
    }

    public final CashItemInfo getItem(int sn, boolean donate) {
        CashItemInfo stats = itemStats.get(Integer.valueOf(sn));
        CashModInfo z = donate ? getDonateModInfo(sn) : getNormalModInfo(sn);

        if (z != null && z.showUp) {
            return z.toCItem(stats); //null doesnt matter
        }
        z = getDonateModInfo(sn);
        if (z != null && z.showUp) {
            return z.toCItem(stats); //null doesnt matter
        }
        if (stats == null || (!stats.onSale())) {
            return null;
        }
        //hmm
        return stats;
    }

    public CashItemInfo getItemForItemId(int itemid) {
        for (CashItemInfo cii : itemStats.values()) {
            if (cii.getId() == itemid) {
                return cii;
            }
        }
        return null;
    }

    public final List<Integer> getPackageItems(int itemId) {
        return itemPackage.get(itemId);
    }

    public final CashModInfo getNormalModInfo(int sn) {
        return normalItemMods.get(sn);
    }

    public final CashModInfo getDonateModInfo(int sn) {
        return donateItemMods.get(sn);
    }

    public final Collection<CashModInfo> getNormalModInfo() {
        return normalItemMods.values();
    }

    public final Collection<CashModInfo> getDonateModInfo() {
        return donateItemMods.values();
    }

    public final int[] getBestItems() {
        return bestItems;
    }

    public boolean isIncubator(int sN) {
        return getIncubatorSN().contains(sN);
    }
}
