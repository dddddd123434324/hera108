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
package server.shops;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import handling.channel.ChannelServer;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.Timer.EtcTimer;
import server.log.LogType;
import server.log.ServerLogger;
import server.maps.MapleMapObjectType;
import tools.MaplePacketCreator;
import tools.packet.PlayerShopPacket;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import tools.Pair;

public class HiredMerchant extends AbstractPlayerStore {

    public ScheduledFuture<?> schedule;
    private List<String> blacklist;
    private int storeid;
    private long start;
    protected List<Pair<String, Byte>> messages = new LinkedList<>();

    public HiredMerchant(MapleCharacter owner, int itemId, String desc) {
        super(owner, itemId, desc, "", 3);
        start = System.currentTimeMillis();
        blacklist = new LinkedList<String>();
        this.schedule = EtcTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (getMCOwner() != null && getMCOwner().getPlayerShop() == HiredMerchant.this) {
                    getMCOwner().setPlayerShop(null);
                }
                removeAllVisitors(-1, -1);
                closeShop(true, true, false);
            }
        }, 1000 * 60 * 60 * 24);
    }

    public byte getShopType() {
        return IMaplePlayerShop.HIRED_MERCHANT;
    }

    public final void setStoreid(final int storeid) {
        this.storeid = storeid;
    }

    public List<MaplePlayerShopItem> searchItem(final int itemSearch) {
        final List<MaplePlayerShopItem> itemz = new LinkedList<MaplePlayerShopItem>();
        for (MaplePlayerShopItem item : items) {
            if (item.item.getItemId() == itemSearch && item.bundles > 0) {
                itemz.add(item);
            }
        }
        return itemz;
    }

    @Override
    public void buy(MapleClient c, int item, short quantity) {
        final MaplePlayerShopItem pItem = items.get(item);
        final Item shopItem = pItem.item;
        final Item newItem = shopItem.copy();
        final short perbundle = newItem.getQuantity();
        final int theQuantity = (pItem.price * quantity);
        newItem.setQuantity((short) (quantity * perbundle));

        short flag = newItem.getFlag();

        if (ItemFlag.KARMA_EQ.check(flag)) {
            newItem.setFlag((short) (flag - ItemFlag.KARMA_EQ.getValue()));
            //newItem.setFlag((short) (newItem.getFlag() | ItemFlag.UNTRADEABLE.getValue()));
        } else if (ItemFlag.KARMA_USE.check(flag)) {
            newItem.setFlag((short) (flag - ItemFlag.KARMA_USE.getValue()));
        }

        if (MapleInventoryManipulator.checkSpace(c, newItem.getItemId(), newItem.getQuantity(), newItem.getOwner())) {
            final int gainmeso = getMeso() + theQuantity - GameConstants.EntrustedStoreTax(theQuantity);
            if (gainmeso > 0) {
                setMeso(gainmeso);
                pItem.bundles -= quantity; // Number remaining in the store
                MapleInventoryManipulator.addFromDrop(c, newItem, false);
                ServerLogger.getInstance().logTrade(LogType.Trade.HiredMerchant, c.getPlayer().getId(), c.getPlayer().getName(), getOwnerName(), "구매아이템 : " + MapleItemInformationProvider.getInstance().getName(newItem.getItemId()) + " " + newItem.getQuantity() + "개 / 구매가격 : " + theQuantity, "상점명 : " + getDescription());
                ServerLogger.getInstance().logTrade(LogType.Trade.HiredMerchant, this.getOwnerId(), this.getOwnerName(), c.getPlayer().getName(), "판매아이템 : " + MapleItemInformationProvider.getInstance().getName(newItem.getItemId()) + " " + newItem.getQuantity() + "개 / 판매가겨 : " + theQuantity + " / 최종메소 : " + gainmeso, "상점명 : " + getDescription());
                bought.add(new BoughtItem(newItem.getItemId(), quantity, theQuantity, c.getPlayer().getName()));
                c.getPlayer().gainMeso(-theQuantity, false);
                saveItems();
                MapleCharacter chr = getMCOwnerWorld();
                if (chr != null) {
                    chr.dropMessage(-5, "" + MapleItemInformationProvider.getInstance().getName(newItem.getItemId()) + " (" + perbundle + ")개의 " + quantity + "묶음이 고용상점에서 판매되었습니다. 남은묶음: " + pItem.bundles);
                }
            } else {
                c.getPlayer().dropMessage(1, "판매자의 소지금액이 한도를 초과하였습니다.");
                c.getSession().write(MaplePacketCreator.enableActions());
            }
        } else {
            c.getPlayer().dropMessage(1, "인벤토리가 꽉 찼습니다.");
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

//    @Override
//    public void closeShop(boolean saveItems, boolean remove, boolean soldout) {
//        if (schedule != null) {
//            schedule.cancel(false);
//        }
//        if (saveItems) {
//            saveItems();
//            items.clear();
//        }
//        if (remove) {
//            ChannelServer.getInstance(channel).removeMerchant(this);
//            getMap().broadcastMessage(PlayerShopPacket.destroyHiredMerchant(getOwnerId()));
//        }
//        getMap().removeMapObject(this);
//        schedule = null;
//    }
    @Override
    public void closeShop(boolean saveItems, boolean remove, boolean soldout) {//고상 닫을 때
        if (schedule != null) {
            schedule.cancel(false);
        }
        if (getMCOwner() != null && !ChannelServer.getInstance(channel).isShutdown()) {
            boolean CheckMeso = false;
            if (getMCOwner().getClient().getPlayer().getMeso() + getMeso() > 0) {
                getMCOwner().getClient().getPlayer().gainMeso(getMeso(), false);
                setMeso(0);
                CheckMeso = true;
            }
            if (!CheckMeso) {
                getMCOwner().getClient().getSession().write(PlayerShopPacket.shopTest(0x11, 1));
            } else {
                int check = check(getMCOwner().getClient().getPlayer());
                if (check == 0) {
                    for (MaplePlayerShopItem itemss : getItems()) {
                        if (itemss.bundles > 0) {
                            Item newItem = itemss.item.copy();
                            newItem.setQuantity((short) (itemss.bundles * newItem.getQuantity()));
                            if (MapleInventoryManipulator.addFromDrop(getMCOwner().getClient(), newItem, false)) {
                                itemss.bundles = 0;
                            }
                        }
                    }
                    getMCOwner().getClient().getSession().write(PlayerShopPacket.shopTest(0x11, 0));
                } else {
                    if (check == 1) {
                        getMCOwner().getClient().getSession().write(PlayerShopPacket.shopTest(0x11, 2));
                    } else {
                        getMCOwner().getClient().getSession().write(PlayerShopPacket.shopTest(0x11, 3));
                    }
                }
            }
        }
        saveItems();
        getItems().clear();
        if (remove) {
            ChannelServer.getInstance(channel).removeMerchant(this);
            getMap().broadcastMessage(PlayerShopPacket.destroyHiredMerchant(getOwnerId()));
        }
        getMap().removeMapObject(this);
        schedule = null;
    }

    public int check(final MapleCharacter chr) {//고상 체크 (닫을 때)
        byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;
        int imsi = 0, slotMax = 0, quantity = 0, qq = 0;
        List<Integer> itemList = new ArrayList<Integer>();
        for (MaplePlayerShopItem items : getItems()) {
            if (MapleItemInformationProvider.getInstance().isPickupRestricted(items.item.getItemId()) && chr.haveItem(items.item.getItemId(), 1)) {
                return 1;
            }
            if (items.item.getItemId() < 2000000) {
                eq++;
            } else {
                if (GameConstants.isRechargable(items.item.getItemId())) {
                    use++;
                } else {
                    if (!itemList.contains(items.item.getItemId())) {
                        itemList.add(items.item.getItemId());
                    }
                }
            }
        }
        for (int item : itemList) {
            qq = 0;
            imsi = 0;
            quantity = 0;
            slotMax = MapleItemInformationProvider.getInstance().getSlotMax(item);
            for (final Item invitem : chr.getInventory(GameConstants.getInventoryType(item))) {
                if (invitem.getItemId() == item) {
                    if (invitem.getQuantity() < slotMax) {
                        quantity += invitem.getQuantity();
                    } else {
                        quantity += slotMax;
                    }
                }
            }
            for (MaplePlayerShopItem itemq : getItems()) {
                if (itemq.item.getItemId() == item) {
                    qq += itemq.bundles;
                }
            }
            imsi = (int) Math.ceil((double) (qq + quantity - slotMax * (int) Math.ceil((double) quantity / slotMax)) / slotMax);
            //chr.dropMessage(6, "필요한 인벤 : " + imsi + " /  들어온 값 : " + qq + " / 소지 값 : " + quantity + " / 슬롯 값 : " + slotMax);
            switch (item / 1000000) {
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
            return 2;
        }
        return 0;
    }

    public int getTimeLeft() {
        return (int) ((System.currentTimeMillis() - start) / 60000);
    }

    public final int getStoreId() {
        return storeid;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.HIRED_MERCHANT;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        if (isAvailable()) {
            client.getSession().write(PlayerShopPacket.destroyHiredMerchant(getOwnerId()));
        }
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (isAvailable()) {
            client.getSession().write(PlayerShopPacket.spawnHiredMerchant(this));
        }
    }

    public final boolean isInBlackList(final String bl) {
        return blacklist.contains(bl);
    }

    public final void addBlackList(final String bl) {
        blacklist.add(bl);
    }

    public final void removeBlackList(final String bl) {
        blacklist.remove(bl);
    }

    public final void sendBlackList(final MapleClient c) {
        c.getSession().write(PlayerShopPacket.MerchantBlackListView(blacklist));
    }

    public final void sendVisitor(final MapleClient c) {
        c.getSession().write(PlayerShopPacket.MerchantVisitorView(visitors, (int) AbstractPlayerStore.calculateStayTime()));
    }

    public void clearMessages() {
        synchronized (messages) {
            messages.clear();
        }
    }

    public List<Pair<String, Byte>> getMessages() {
        synchronized (messages) {
            List<Pair<String, Byte>> msgList = new LinkedList<>();
            for (Pair<String, Byte> m : messages) {
                msgList.add(m);
            }

            return msgList;
        }
    }

    public void chat(final MapleClient c, String chat) {

        synchronized (messages) {
            messages.add(new Pair<>(c.getPlayer().getName() + " : " + chat, c.getPlayer().getPlayerShop().getVisitorSlot(c.getPlayer())));
        }
        c.getPlayer().getPlayerShop().broadcastToVisitors(PlayerShopPacket.shopChat(c.getPlayer().getName() + " : " + chat, c.getPlayer().getPlayerShop().getVisitorSlot(c.getPlayer())));

    }
}
