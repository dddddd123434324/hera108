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
package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemLoader;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import database.DatabaseConnection;
import handling.world.World;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MerchItemPackage;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.PlayerShopPacket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HiredMerchantHandler {

    public static final boolean UseHiredMerchant(final MapleClient c, final boolean packet) {
        if (c.getPlayer().getMap() != null && c.getPlayer().getMap().allowPersonalShop()) {
            final byte state = checkExistance(c.getPlayer().getAccountID(), c.getPlayer().getId());

            switch (state) {
                case 1:
                    c.getSession().write(PlayerShopPacket.titleBoxMessage(9));
                    //c.getPlayer().dropMessage(1, "프레드릭에게서 먼저 아이템을 찾아가 주세요.");
                    break;
                case 0:
                    boolean merch = World.hasMerchant(c.getPlayer().getAccountID(), c.getPlayer().getId());
                    if (!merch) {
                        if (c.getChannelServer().isShutdown()) {
                            c.getPlayer().dropMessage(1, "The server is about to shut down.");
                            return false;
                        }
                        if (packet) {
                            c.getSession().write(PlayerShopPacket.sendTitleBox());
                        }
                        return true;
                    } else {
                        Pair<Integer, Integer> merch2 = World.findMerchant(c.getPlayer().getAccountID(), c.getPlayer().getId());
                        int OwnerChannel = World.Find.findChannel(c.getPlayer().getName());
                        if ((((c.getPlayer().getMapId() - 910000000) == merch2.getRight())) && (OwnerChannel != (merch2.getLeft()))) {
                            c.sendPacket(PlayerShopPacket.titleBoxAlready2(merch2.getRight(), merch2.getLeft() - 1));//라이트값 방 채널
                            //c.getPlayer().dropMessage(5, "1getRight()" + merch2.getRight() + " getLeft() " + (merch2.getLeft() - 1) + " OwnerChannel" + OwnerChannel);
                            //c.getPlayer().dropMessage(5, "맵좌표" + (c.getPlayer().getMapId() - 910000000) + " getRight()" + merch2.getRight() + " OwnerChannel" + OwnerChannel + "getLeft()" + (merch2.getLeft()));
                        } else {
                            c.sendPacket(PlayerShopPacket.titleBoxAlready1(merch2.getRight(), merch2.getLeft() - 1));//왼쪽이 방c.getPlayer().getMapId() - 
                            //c.getPlayer().dropMessage(5, "2getRight()" + merch2.getRight() + " getLeft()" + (merch2.getLeft() - 1));
                            //c.getPlayer().dropMessage(5, "맵좌표" + (c.getPlayer().getMapId() - 910000000) + " getRight()" + merch2.getRight() + " OwnerChannel" + OwnerChannel + "getLeft()" + (merch2.getLeft()));
                        }
                        //c.getSession().write(PlayerShopPacket.titleBoxMessage(9));
                    }
                    break;
                default:
                    c.getPlayer().dropMessage(1, "An unknown error occured.");
                    break;
            }
        } else {
            c.getPlayer().dropMessage(1, "사용할 수 없습니다.");
            c.getSession().write(MaplePacketCreator.enableActions());
        }
        return false;
    }

    private static final byte checkExistance(final int accid, final int cid) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * from hiredmerch where accountid = ? OR characterid = ?");
            ps.setInt(1, accid);
            ps.setInt(2, cid);
            rs = ps.executeQuery();

            if (rs.next()) {
                return 1;
            }
            return 0;
        } catch (SQLException se) {
            return -1;
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

    public static final void displayMerch(MapleClient c) {
        final int conv = c.getPlayer().getConversation();
        Pair<Integer, Integer> merch = World.findMerchant(c.getPlayer().getAccountID(), c.getPlayer().getId());
        if (merch != null) {
            c.sendPacket(PlayerShopPacket.merchItemAlreadyOpen(merch.getLeft() - 1, merch.getRight()));
            c.getPlayer().setConversation(0);
        } else if (c.getChannelServer().isShutdown()) {
            c.getPlayer().dropMessage(1, "서버 종료 중입니다.");
            c.getPlayer().setConversation(0);
        } else if (conv == 3) { // Hired Merch
            final MerchItemPackage pack = loadItemFrom_Database(c.getPlayer().getAccountID());

            if (pack == null) {
                //  c.getPlayer().dropMessage(1, "찾아갈 아이템이 없습니다.");
                c.getPlayer().setConversation(0);
                c.getSession().write(PlayerShopPacket.merchItemStore2()); //
            } else {
                c.getSession().write(PlayerShopPacket.merchItemStore_ItemData(pack));
            }
        }
    }

    public static final void MerchantItemStore(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer() == null) {
            return;
        }
        final byte operation = slea.readByte();

        if (operation == 0x19) {
            if (c.getPlayer().getConversation() != 3) {
                return;
            }
            boolean merch = World.hasMerchant(c.getPlayer().getAccountID(), c.getPlayer().getId());
            if (merch) {
                //c.getSession().write(PlayerShopPacket.titleBoxMessage(10));
                c.getPlayer().dropMessage(1, "해당 계정으로 열려있는 상점을 닫은 후 다시 시도하세요.");
                /*다른 캐릭터가 아이템을 사용 중입니다.\r\n다른 캐릭터로 접속해서 상점을 닫거나\r\n스토어뱅크를 비워 주세요.
                 3638*/
                c.getPlayer().setConversation(0);
                return;
            }
            final MerchItemPackage pack = loadItemFrom_Database(c.getPlayer().getAccountID());

            if (pack == null) {
                c.getPlayer().dropMessage(1, "An unknown error occured.");
                return;
            } else if (c.getChannelServer().isShutdown()) {
                c.getPlayer().dropMessage(1, "The world is going to shut down.");
                c.getPlayer().setConversation(0);
                return;
            }
            if (!check(c.getPlayer(), pack)) {
                c.getSession().write(PlayerShopPacket.merchItem_Message((byte) 0x21)); // 26 아이템과 메소를 모두 찾았어.
                c.getPlayer().setConversation(0);
                return;
            }
            if (deletePackage(c.getPlayer().getAccountID(), pack.getPackageid(), c.getPlayer().getId())) {
                c.getPlayer().gainMeso(pack.getMesos(), false);
                for (Item item : pack.getItems()) {
                    MapleInventoryManipulator.addFromDrop(c, item, false);
                }
                c.getSession().write(PlayerShopPacket.merchItem_Message((byte) 0x1D)); // 26 아이템과 메소를 모두 찾았어.
                /*
                 27: 스토어 뱅크에 돈이 너무많아
                 28: 하나밖에 가질수 없어
                 29: 수수료가 부족해 못찾아!
                 30: 인벤토리가 부족해서 못찾음!
                 */
                c.getPlayer().setConversation(0);
            } else {
                c.getPlayer().dropMessage(1, "An unknown error occured.");
            }
        } else if (operation == 0x1B) {
            c.getPlayer().setConversation(0);
        }
    }

    private static final boolean check(final MapleCharacter chr, final MerchItemPackage pack) {//고상 체크 (받을 때)
        /*
         * 이것은 나의 영혼이 담겼다 공유하면 15대가 고자
         * 제작 큐티버크
         */
        if (chr.getMeso() + pack.getMesos() < 0) {
            return false;
        }
        byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;
        int imsi = 0, slotMax = 0, quantity = 0, qq = 0;
        List<Integer> itemList = new ArrayList<Integer>();
        for (Item item : pack.getItems()) {
            if (MapleItemInformationProvider.getInstance().isPickupRestricted(item.getItemId()) && chr.haveItem(item.getItemId(), 1)) {
                return false;
            }
            if (item.getItemId() < 2000000) {
                eq++;
            } else {
                if (GameConstants.isRechargable(item.getItemId())) {
                    use++;
                } else {
                    if (!itemList.contains(item.getItemId())) {
                        itemList.add(item.getItemId());
                    }
                }
            }
        }
        for (int items : itemList) {
            qq = 0;
            imsi = 0;
            quantity = 0;
            slotMax = MapleItemInformationProvider.getInstance().getSlotMax(items);
            for (final Item invitem : chr.getInventory(GameConstants.getInventoryType(items))) {
                if (invitem.getItemId() == items) {
                    if (invitem.getQuantity() < slotMax) {
                        quantity += invitem.getQuantity();
                    } else {
                        quantity += slotMax;
                    }
                }
            }
            for (Item itemq : pack.getItems()) {
                if (itemq.getItemId() == items) {
                    qq += itemq.getQuantity();
                }
            }
            imsi = (int) Math.ceil((double) (qq + quantity - slotMax * (int) Math.ceil((double) quantity / slotMax)) / slotMax);
            switch (items / 1000000) {
                case 2:
                    use += imsi;
                    break;
                case 3:
                    setup += imsi;
                    break;
                case 4:
                    etc += imsi;
                    break;
                case 5:
                    cash += imsi;
                    break;
            }
        }
        if (chr.getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < eq || chr.getInventory(MapleInventoryType.USE).getNumFreeSlot() < use || chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < setup || chr.getInventory(MapleInventoryType.ETC).getNumFreeSlot() < etc || chr.getInventory(MapleInventoryType.CASH).getNumFreeSlot() < cash) {
            return false;
        }
        return true;
    }

    private static final boolean deletePackage(final int accid, final int packageid, final int chrId) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("DELETE from hiredmerch where accountid = ? OR packageid = ? OR characterid = ?");
            ps.setInt(1, accid);
            ps.setInt(2, packageid);
            ps.setInt(3, chrId);
            ps.executeUpdate();
            ItemLoader.HIRED_MERCHANT.saveItems(null, packageid);
            return true;
        } catch (SQLException e) {
            return false;
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
        }
    }

    private static final MerchItemPackage loadItemFrom_Database(final int accountid) {

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT * from hiredmerch where accountid = ?");
            ps.setInt(1, accountid);

            rs = ps.executeQuery();

            if (!rs.next()) {
                return null;
            }
            final int packageid = rs.getInt("PackageId");

            final MerchItemPackage pack = new MerchItemPackage();
            pack.setPackageid(packageid);
            pack.setMesos(rs.getInt("Mesos"));
            pack.setSentTime(rs.getLong("time"));

            Map<Long, Pair<Item, MapleInventoryType>> items = ItemLoader.HIRED_MERCHANT.loadItems(false, packageid);
            if (items != null) {
                List<Item> iters = new ArrayList<>();
                for (Pair<Item, MapleInventoryType> z : items.values()) {
                    iters.add(z.left);
                }
                pack.setItems(iters);
            }

            return pack;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
}
