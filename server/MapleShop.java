package server;

import client.MapleClient;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import server.log.LogType;
import server.log.ServerLogger;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MapleShop {

    private static final Set<Integer> rechargeableItems = new LinkedHashSet<Integer>();
    private int id;
    private int npcId;
    private List<MapleShopItem> items = new LinkedList<MapleShopItem>();
    private List<Pair<Integer, String>> ranks = new ArrayList<Pair<Integer, String>>();

    static {
        rechargeableItems.add(2070000);
        rechargeableItems.add(2070001);
        rechargeableItems.add(2070002);
        rechargeableItems.add(2070003);
        rechargeableItems.add(2070004);
        rechargeableItems.add(2070005);
        rechargeableItems.add(2070006);
        rechargeableItems.add(2070007);
        rechargeableItems.add(2070008);
        rechargeableItems.add(2070009);
        rechargeableItems.add(2070010);
        rechargeableItems.add(2070011);
        rechargeableItems.add(2070012);
        rechargeableItems.add(2070013);
        rechargeableItems.add(2070016); //크리스탈 일비 표창
        rechargeableItems.add(2070019); //악마의 표창
        rechargeableItems.add(2070023); //플레임 표창
        rechargeableItems.add(2070024); //무한의 수리검
        rechargeableItems.add(2070025); //샤이닝 일비 표창
        rechargeableItems.add(2070026); //나중에 써먹을놈 데이터만 있음

        rechargeableItems.add(2330000);
        rechargeableItems.add(2330001);
        rechargeableItems.add(2330002);
        rechargeableItems.add(2330003);
        rechargeableItems.add(2330004);
        rechargeableItems.add(2330005);
        rechargeableItems.add(2330007); //아머 피어싱 불릿
        rechargeableItems.add(2330008); //풀 메탈 불릿

        rechargeableItems.add(2331000); // Capsules
        rechargeableItems.add(2332000); // Capsules
    }

    /**
     * Creates a new instance of MapleShop
     */
    private MapleShop(int id, int npcId) {
        this.id = id;
        this.npcId = npcId;
    }

    public void addItem(MapleShopItem item) {
        items.add(item);
    }

    public List<MapleShopItem> getItems() {
        return items;
    }

    public void sendShop(MapleClient c) {
        if (c.getPlayer().isGM()) {
            c.getPlayer().dropMessage(6, "상점아이디 : " + getId());
        }
        c.getPlayer().setShop(this);
        c.getSession().write(MaplePacketCreator.getNPCShop(getNpcId(), this, c));
    }

    public void sendShop(MapleClient c, int customNpc) {
        c.getPlayer().setShop(this);
        c.getSession().write(MaplePacketCreator.getNPCShop(customNpc, this, c));
    }

    public void buy(MapleClient c, int itemId, short quantity, short select) {
        if (quantity <= 0) {
            AutobanManager.getInstance().addPoints(c, 1000, 0, "Buying " + quantity + " " + itemId);
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MapleShopItem item = findById(itemId, select);
        if (item != null && item.getPrice() > 0 && item.getReqItem() == 0) {
            if (item.getMax() > 1) {
                final int price = item.getPrice();
                final short max = (short) item.getMax();
                final int tprice;
                if (ServerConstants.SHOP_DISCOUNT && ((itemId / 10000) >= 200 && (itemId / 10000) <= 202)) {
                    tprice = (price - (int) Math.round((price * ServerConstants.SHOP_DISCOUNT_PERCENT) / 100f));
                } else {
                    tprice = price;
                }
                if (tprice >= 0 && c.getPlayer().getMeso() >= tprice) {
                    if (MapleInventoryManipulator.checkSpace(c, itemId, max, "")) {
                        c.getPlayer().gainMeso(-tprice, false);
                        if (GameConstants.isPet(itemId)) {
                            MapleInventoryManipulator.addById(c, itemId, max, "", MaplePet.createPet(itemId, MapleInventoryIdentifier.getInstance()), -1, "Bought from shop " + id + ", " + npcId + " on " + FileoutputUtil.CurrentReadable_Date());
                        } else {
                            if (item.getExpire() > 0) {
                                MapleInventoryManipulator.addById(c, itemId, max, "Bought from shop " + id + ", " + npcId + " on " + FileoutputUtil.CurrentReadable_Date(), (long) item.getExpire());
                            } else {
                                MapleInventoryManipulator.addById(c, itemId, max, "Bought from shop " + id + ", " + npcId + " on " + FileoutputUtil.CurrentReadable_Date());
                            }
                        }
                    } else {
                        c.getPlayer().dropMessage(1, "인벤토리 공간이 부족합니다.");
                    }
                    c.getSession().write(MaplePacketCreator.confirmShopTransaction((byte) 0));
                }
            } else {
                final int price = GameConstants.isRechargable(itemId) ? item.getPrice() : (item.getPrice() * quantity);
                final int tprice;
                if (ServerConstants.SHOP_DISCOUNT && ((itemId / 10000) >= 200 && (itemId / 10000) <= 202)) {
                    tprice = (price - (int) Math.round((price * ServerConstants.SHOP_DISCOUNT_PERCENT) / 100f));
                } else {
                    tprice = price;
                }
                if (tprice >= 0 && c.getPlayer().getMeso() >= tprice) {
                    if (MapleInventoryManipulator.checkSpace(c, itemId, quantity, "")) {
                        c.getPlayer().gainMeso(-tprice, false);
                        if (GameConstants.isPet(itemId)) {
                            MapleInventoryManipulator.addById(c, itemId, quantity, "", MaplePet.createPet(itemId, MapleInventoryIdentifier.getInstance()), -1, "Bought from shop " + id + ", " + npcId + " on " + FileoutputUtil.CurrentReadable_Date());
                        } else {
                            if (GameConstants.isRechargable(itemId)) {
                                quantity = ii.getSlotMax(item.getItemId());
                            }
                            if (item.getExpire() > 0) {
                                MapleInventoryManipulator.addById(c, itemId, quantity, "Bought from shop " + id + ", " + npcId + " on " + FileoutputUtil.CurrentReadable_Date(), (long) item.getExpire());
                            } else {
                                MapleInventoryManipulator.addById(c, itemId, quantity, "Bought from shop " + id + ", " + npcId + " on " + FileoutputUtil.CurrentReadable_Date());
                            }
                        }
                    } else {
                        c.getPlayer().dropMessage(1, "인벤토리 공간이 부족합니다.");
                    }
                    c.getSession().write(MaplePacketCreator.confirmShopTransaction((byte) 0));
                }
            }
        } else if (item != null && item.getReqItem() > 0 && c.getPlayer().haveItem(item.getReqItem(), item.getReqItemQ(), false, true)) {
            if (MapleInventoryManipulator.checkSpace(c, itemId, quantity, "")) {
                if (item.getExpire() > 0) {
                    if (c.getPlayer().getInvSlots(GameConstants.getInventoryType(itemId).getType()) < 1) {
                        c.getPlayer().dropMessage(1, "인벤토리 공간이 부족합니다.");
                        c.getSession().write(MaplePacketCreator.confirmShopTransaction((byte) 0));
                        return;
                    }
                }
                MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(item.getReqItem()), item.getReqItem(), item.getReqItemQ(), false, false);
                if (GameConstants.isPet(itemId)) {
                    MapleInventoryManipulator.addById(c, itemId, quantity, "", MaplePet.createPet(itemId, MapleInventoryIdentifier.getInstance()), -1, "Bought from shop " + id + ", " + npcId + " on " + FileoutputUtil.CurrentReadable_Date());
                } else {
                    if (GameConstants.isRechargable(itemId)) {
                        quantity = ii.getSlotMax(item.getItemId());
                    } else {
                        if (item.getMax() != 0) {
                            quantity = (short) item.getMax();
                        }
                    }
                    if (GameConstants.isEquip(itemId) && item.getExpire() > 0) {
                        Item eq = ii.getEquipById(itemId);
                        final Equip eqq = (Equip) eq;
                        eqq.setExpiration(System.currentTimeMillis() + item.getExpire() * 1000 * 60);
                        if (itemId == 1112402) {
                            eqq.setPotential1(20086);
                            eqq.setPotential2(20086);
                            eqq.setPotential3(20086);
                        }
                        else if (itemId == 1102207 || itemId == 1022082) {
                            eqq.setPotential1(30086);
                            eqq.setPotential2(20086);
                            eqq.setPotential3(20046);
                        }
                        else if (itemId == 1122059) {
                            eqq.setPotential1(30086);
                            eqq.setPotential2(20086);
                            eqq.setPotential3(10081);
                        }
                        else if (itemId == 1312061 || itemId == 1322089 || itemId == 1332124 || itemId == 1382082 || itemId == 1412040 || itemId == 1422062 || itemId == 1432063 || itemId == 1442087 || itemId == 1452060 || itemId == 1462049 || itemId == 1472067 || itemId == 1482048 || itemId == 1492031) {
                            eqq.setPotential1(30601);
                            eqq.setPotential2(30070);
                            eqq.setPotential3(30086);
                        }
                        MapleInventoryManipulator.addbyItem(c, eqq.copy());
                    } else if (item.getExpire() > 0) {
                        Item itemz;
                        Item itemz2;
                        itemz = new client.inventory.Item(itemId, (byte) 0, quantity, (byte) 0);
                        itemz.setExpiration(System.currentTimeMillis() + item.getExpire() * 1000 * 60);
                        itemz2 = itemz.copy();
                        MapleInventoryManipulator.addbyItem(c, itemz2);
                    } else {
                        if (getId() == 9000069 && GameConstants.isEquip(itemId)) {
                            MapleInventoryManipulator.addByIdPotential(c, itemId, quantity, null, null, "Bought from shop " + id + ", " + npcId + " on " + FileoutputUtil.CurrentReadable_Date(), true);
                    }  else if (GameConstants.isEquip(itemId)) {
                        Item eq = ii.getEquipById(itemId);
                        final Equip eqq = (Equip) eq;
                        if (itemId == 1102207 || itemId == 1022082) {
                            eqq.setPotential1(30086);
                            eqq.setPotential2(20086);
                            eqq.setPotential3(20045);
                        }
                        else if (itemId == 1122059) {
                            eqq.setPotential1(30086);
                            eqq.setPotential2(20086);
                            eqq.setPotential3(10081);
                        }
                        else if (itemId == 1312061 || itemId == 1322089 || itemId == 1332124 || itemId == 1382082 || itemId == 1412040 || itemId == 1422062 || itemId == 1432063 || itemId == 1442087 || itemId == 1452060 || itemId == 1462049 || itemId == 1472067 || itemId == 1482048 || itemId == 1492031) {
                            eqq.setPotential1(30601);
                            eqq.setPotential2(30070);
                            eqq.setPotential3(30086);
                        }
                        MapleInventoryManipulator.addbyItem(c, eqq.copy());    
                        } else {
                            MapleInventoryManipulator.addById(c, itemId, quantity, "Bought from shop " + id + ", " + npcId + " on " + FileoutputUtil.CurrentReadable_Date());
                        }
                    }
                }
            } else {
                c.getPlayer().dropMessage(1, "인벤토리 공간이 부족합니다.");
            }
            c.getSession().write(MaplePacketCreator.confirmShopTransaction((byte) 0));
        }
    }

    public void sell(MapleClient c, MapleInventoryType type, byte slot, short quantity) {
        if (quantity == 0xFFFF || quantity == 0) {
            quantity = 1;
        }
        Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (item == null) {
            return;
        }

        if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
            quantity = item.getQuantity();
        }
        if (quantity < 0) {
            AutobanManager.getInstance().addPoints(c, 1000, 0, "Selling " + quantity + " " + item.getItemId() + " (" + type.name() + "/" + slot + ")");
            return;
        }
        short iQuant = item.getQuantity();
        if (iQuant == 0xFFFF) {
            iQuant = 1;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (ii.cantSell(item.getItemId()) || GameConstants.isPet(item.getItemId())) {
            return;
        }
        if (quantity <= iQuant && iQuant > 0) {
            MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
            double price;
            if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
                price = ii.getWholePrice(item.getItemId()) / (double) ii.getSlotMax(item.getItemId());
            } else {
                price = ii.getPrice(item.getItemId());
            }
            final int recvMesos = (int) Math.max(Math.ceil(price * quantity), 0);
            if (price != -1.0 && recvMesos > 0) {
                c.getPlayer().gainMeso(recvMesos, false);
                ServerLogger.getInstance().logItem(LogType.Item.Sell, c.getPlayer().getId(), c.getPlayer().getName(), item.getItemId(), quantity, ii.getName(item.getItemId()), recvMesos, "Shop : " + this.getNpcId() + " (ShopID : " + getId() + ")");
            }
            c.getSession().write(MaplePacketCreator.confirmShopTransaction((byte) 0x8));
        }
    }

    public static void playersell(MapleClient c, MapleInventoryType type, byte slot, short quantity) { //일괄판매
        if (quantity == 0xFFFF || quantity == 0) {
            quantity = 1;
        }
        Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (item == null) {
            return;
        }

        if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
            quantity = item.getQuantity();
        }
        if (quantity < 0) {
            AutobanManager.getInstance().addPoints(c, 1000, 0, "Selling " + quantity + " " + item.getItemId() + " (" + type.name() + "/" + slot + ")");
            return;
        }
        short iQuant = item.getQuantity();
        if (iQuant == 0xFFFF) {
            iQuant = 1;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (ii.cantSell(item.getItemId()) || GameConstants.isPet(item.getItemId())) {
            return;
        }
        if (quantity <= iQuant && iQuant > 0) {
            MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
            double price;
            if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
                price = ii.getWholePrice(item.getItemId()) / (double) ii.getSlotMax(item.getItemId());
            } else {
                price = ii.getPrice(item.getItemId());
            }
            final int recvMesos = (int) Math.max(Math.ceil(price * quantity), 0);
            if (price != -1.0 && recvMesos > 0) {
                c.getPlayer().gainMeso(recvMesos, false);
            }
            c.getSession().write(MaplePacketCreator.confirmShopTransaction((byte) 0x8));
        }
    }
    
    public void recharge(final MapleClient c, final byte slot) {
        final Item item = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);

        if (item == null || (!GameConstants.isThrowingStar(item.getItemId()) && !GameConstants.isBullet(item.getItemId()))) {
            return;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        short slotMax = ii.getSlotMax(item.getItemId());
        final int skill = GameConstants.getMasterySkill(c.getPlayer().getJob());

        if (skill != 0) {
            slotMax += c.getPlayer().getTotalSkillLevel(SkillFactory.getSkill(skill)) * 10;
        }
        if (item.getQuantity() < slotMax) {
            final int price = (int) Math.round(ii.getPrice(item.getItemId()) * (slotMax - item.getQuantity()));
            if (c.getPlayer().getMeso() >= price) {
                item.setQuantity(slotMax);
                c.getSession().write(MaplePacketCreator.updateInventorySlot(MapleInventoryType.USE, (Item) item, false));
                c.getPlayer().gainMeso(-price, false, false);
                c.getSession().write(MaplePacketCreator.confirmShopTransaction((byte) 0x8));
            } else {
                c.getSession().write(MaplePacketCreator.confirmShopTransaction((byte) 0xA));
            }
        }
    }

    protected MapleShopItem findById(int itemId, int select) {
        MapleShopItem item = items.get(select);
        if (item != null && item.getItemId() == itemId) {
            return item;
        }
        return null;
    }

    public static MapleShop createFromDB(int id, boolean isShopId) {
        MapleShop ret = null;
        int shopId;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement(isShopId ? "SELECT * FROM shops WHERE shopid = ?" : "SELECT * FROM shops WHERE npcid = ?");

            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                shopId = rs.getInt("shopid");
                ret = new MapleShop(shopId, rs.getInt("npcid"));
                rs.close();
                ps.close();
            } else {
                rs.close();
                ps.close();
                return null;
            }
            ps = con.prepareStatement("SELECT * FROM shopitems WHERE shopid = ? ORDER BY position ASC");
            ps.setInt(1, shopId);
            rs = ps.executeQuery();
            List<Integer> recharges = new ArrayList<Integer>(rechargeableItems);
            while (rs.next()) {
                if (!ii.itemExists(rs.getInt("itemid"))) {
                    continue;
                }
                if (GameConstants.isThrowingStar(rs.getInt("itemid")) || GameConstants.isBullet(rs.getInt("itemid"))) {
                    MapleShopItem starItem = new MapleShopItem((short) 1, rs.getInt("itemid"), rs.getInt("price"), rs.getInt("reqitem"), rs.getInt("reqitemq"), rs.getByte("rank"), rs.getInt("max"), rs.getInt("expiretime"), rs.getInt("level"));
                    ret.addItem(starItem);
                    if (rechargeableItems.contains(starItem.getItemId())) {
                        recharges.remove(Integer.valueOf(starItem.getItemId()));
                    }
                } else {
                    ret.addItem(new MapleShopItem((short) 1000, rs.getInt("itemid"), rs.getInt("price"), rs.getInt("reqitem"), rs.getInt("reqitemq"), rs.getByte("rank"), rs.getInt("max"), rs.getInt("expiretime"), rs.getInt("level")));
                }
            }
            for (Integer recharge : recharges) {
                ret.addItem(new MapleShopItem((short) 1, recharge.intValue(), 0, 0, 0, (byte) 0, 0, 0, 0));
            }
            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT * FROM shopranks WHERE shopid = ? ORDER BY rank ASC");
            ps.setInt(1, shopId);
            rs = ps.executeQuery();
            while (rs.next()) {
                if (!ii.itemExists(rs.getInt("itemid"))) {
                    continue;
                }
                ret.ranks.add(new Pair<Integer, String>(rs.getInt("itemid"), rs.getString("name")));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Could not load shop");
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
        return ret;
    }

    public int getNpcId() {
        return npcId;
    }

    public int getId() {
        return id;
    }

    public List<Pair<Integer, String>> getRanks() {
        return ranks;
    }
}
