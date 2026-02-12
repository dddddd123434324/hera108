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
package tools.packet;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import handling.SendPacketOpcode;
import server.CashItemFactory;
import server.CashItemInfo.CashModInfo;
import server.CashItemSaleRank;
import server.CashShop;
import tools.Pair;
import tools.data.MaplePacketLittleEndianWriter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import tools.HexTool;
import tools.MaplePacketCreator;

public class CSPacket {

    /*
     * 2b : 파끝 팅
     * 2c : 파끝 팅
     * 2d : 넥슨피방 전용 쿠폰이며 이미 사용염
     * 2e : 파끝팅
     */
    public static byte[] warpCS(MapleClient c) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPEN.getValue());

        PacketHelper.addCharacterInfo(mplew, c.getPlayer());
        mplew.writeMapleAsciiString(c.getAccountName());
        Collection<CashModInfo> cmiN = CashItemFactory.getInstance().getNormalModInfo();
        Collection<CashModInfo> cmiD = CashItemFactory.getInstance().getDonateModInfo();
        List<Integer> incubators = CashItemFactory.getInstance().getIncubatorSN();
        if (c.getPlayer().isDonateShop()) {
            mplew.writeShort(cmiD.size());
            for (CashModInfo cm : cmiD) {
                addModCashItemInfo(mplew, cm);
            }
        } else {
            mplew.writeShort(cmiN.size() + incubators.size());
            for (CashModInfo cm : cmiN) {
                addModCashItemInfo(mplew, cm);
            }
            for (int iz : incubators) {
                mplew.writeInt(iz);
                mplew.writeInt(0x400);
                mplew.write(0);
            }
        }

        mplew.writeShort(0); // ?
        mplew.write(0); // 카테고리 할인
//        mplew.write(1); // 카테고리 할인
//        mplew.write(1); // 카테고리 할인
//        mplew.write(1); // 카테고리 할인
//        mplew.write(30); // 카테고리 할인
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 2; j++) {
                int[] top5 = CashItemSaleRank.top5(i, j, c.getPlayer().isDonateShop());
                for (int k = 0; k < 5; k++) {
                    mplew.writeInt(i);
                    mplew.writeInt(j);
                    mplew.writeInt(k < top5.length ? top5[k] : 0);
                }
            }
        }
        mplew.writeShort(0);
        mplew.writeShort(0);//사이즈
//        mplew.writeInt(5060002);//아이템아이디
//        mplew.writeInt(50200014);//sn 1
//        mplew.writeInt(0);//sn 2
//        mplew.writeInt(0);//sn 3
//        mplew.writeInt(0);//sn 4
//        mplew.writeInt(0);//sn 5
//        mplew.writeInt(0);//sn 6
//        mplew.writeInt(0);//sn 7
//        mplew.writeInt(0);//sn 8
//        mplew.writeInt(0);//sn 9
//        mplew.writeInt(0);//sn 10
//
//        /*
//        0: 구매가능
//        1: 다 팔림
//        2: 판매시간이 아님
//        3: 판매시간이 아님
//         */
//        mplew.writeInt(1);//state
//        mplew.writeInt(200);//최대갯수
//        mplew.writeInt(100);//현재갯수
//        mplew.writeInt(11);//dwConditionFlag
//        /*
//        1: only 갯수 서두르세요!!
//        2: 년월일
//        3:
//        8: only시간
//        9: 시간, 갯수
//        10: 년월일, 시간
//        11: 년월일, 시간, 갯수
//         */
//        mplew.writeInt(20200415);//시작년월일
//        mplew.writeInt(20201215);//종료연월일
//        mplew.writeInt(0);//시작시간
//        mplew.writeInt(0);//종료시간
//        mplew.writeInt(0);//월요일
//        mplew.writeInt(0);//화요일
//        mplew.writeInt(0);//수요일
//        mplew.writeInt(0);//목요일
//        mplew.writeInt(0);//금요일
//        mplew.writeInt(0);//토요일
//        mplew.writeInt(0);//일요일

        mplew.write(0);//찜 이벤트

        return mplew.getPacket();
    }

    public static byte[] playCashSong(int itemid, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.CASH_SONG.getValue());
        mplew.writeInt(itemid);
        mplew.writeMapleAsciiString(name);
        return mplew.getPacket();
    }

    public static byte[] useCharm(byte charmsleft, byte daysleft) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(8);
        mplew.write(1);
        mplew.write(charmsleft);
        mplew.write(daysleft);

        return mplew.getPacket();
    }

    public static byte[] useWheel(byte charmsleft) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(21);
        mplew.writeLong(charmsleft);

        return mplew.getPacket();
    }

    public static byte[] useSoulStone() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(26);

        return mplew.getPacket();
    }

    public static byte[] itemExpired(int itemid, boolean cash) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        // 1E 00 02 83 C9 51 00

        // 21 00 08 02
        // 50 62 25 00
        // 50 62 25 00
        mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        if (cash) {
            mplew.write(2);
        } else {
            mplew.write(9);
            mplew.write(1);
        }
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static byte[] ViciousHammer(boolean start, int hammered) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.VICIOUS_HAMMER.getValue());
        mplew.write(start ? 61 : 65);
        mplew.writeInt(0);
        mplew.writeInt(hammered);
        return mplew.getPacket();
    }

    public static byte[] changePetFlag(int uniqueId, boolean added, int flagAdded) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.PET_FLAG_CHANGE.getValue());

        mplew.writeLong(uniqueId);
        mplew.write(added ? 1 : 0);
        mplew.writeShort(flagAdded);

        return mplew.getPacket();
    }

    public static byte[] changePetName(MapleCharacter chr, String newname, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.PET_NAMECHANGE.getValue());

        mplew.writeInt(chr.getId());
        mplew.writeInt(slot);
        mplew.writeMapleAsciiString(newname);
        mplew.writeInt(slot);

        return mplew.getPacket();
    }

    public static byte[] showNotes(MapleCharacter chr, ResultSet notes, int count) throws SQLException { //1.2.31ok
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_NOTES.getValue());
        mplew.write(3);
        mplew.write(count);
        for (int i = 0; i < count; i++) {
            mplew.writeInt(notes.getInt("id"));
            mplew.writeMapleAsciiString(notes.getString("from"));
            mplew.writeMapleAsciiString(notes.getString("message"));
            mplew.writeLong(PacketHelper.getKoreanTimestamp(notes.getLong("timestamp")));
            mplew.write(notes.getInt("gift"));
            if (notes.getInt("gift") > 0) {
                chr.addCanGainFame();
            }
            notes.next();
        }

        return mplew.getPacket();
    }

    public static byte[] SendNoteResult(byte Ocelit) { //1.2.31 ok
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_NOTES.getValue());
        mplew.write(Ocelit); // 3 이 뭐더라 
        if (Ocelit == (byte) 4) {
            mplew.writeZeroBytes(1); // 3 이 뭐더라 
        }
        /*4: 상대방이 이미 게임에 접속 중입니다. 귓속말을 이용해 주세요.
         3: 쪽지가 성공적으로 보내졌습니다.*/
        return mplew.getPacket();
    }

    public static byte[] useChalkboard(final int charid, final String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.CHALKBOARD.getValue());

        mplew.writeInt(charid);
        if (msg == null || msg.length() <= 0) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeMapleAsciiString(msg);
        }

        return mplew.getPacket();
    }

    public static byte[] getTrockRefresh(MapleCharacter chr, byte vip, boolean delete) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.TROCK_LOCATIONS.getValue());
        mplew.write(delete ? 2 : 3);
        mplew.write(vip);
        if (vip == 1) {
            int[] map = chr.getRocks();
            for (int i = 0; i < 10; i++) {
                mplew.writeInt(map[i]);
            }
        } else if (vip >= 2) {
            int[] map = chr.getHyperRocks();
            for (int i = 0; i < 13; i++) {
                mplew.writeInt(map[i]);
            }
        } else {
            int[] map = chr.getRegRocks();
            for (int i = 0; i < 5; i++) {
                mplew.writeInt(map[i]);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] sendWishList(MapleCharacter chr, boolean update) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(update ? 60 : 58); //+10
        int[] list = chr.getWishlist();
        for (int i = 0; i < 10; i++) {
            mplew.writeInt(list[i] != -1 ? list[i] : 0);
        }
        return mplew.getPacket();
    }

    public static byte[] showNXMapleTokens(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_UPDATE.getValue());
        if (chr.isDonateShop()) {
            mplew.writeInt(chr.getCSPoints(3)); // A-cash or NX Credit
            mplew.writeInt(0); // MPoint
        } else {
            mplew.writeInt(chr.getCSPoints(1)); // A-cash or NX Credit
            mplew.writeInt(chr.getCSPoints(2)); // MPoint
        }

        return mplew.getPacket();
    }

    public static byte[] showBoughtCSPackage(Map<Integer, Item> ccc, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(116); //use to be 7a
        mplew.write(ccc.size());
        for (Entry<Integer, Item> sn : ccc.entrySet()) {
            addCashItemInfo(mplew, sn.getValue(), accid, sn.getKey().intValue());
        }
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static byte[] showBoughtCSItem(Item item, int sn, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(62);
        addCashItemInfo(mplew, item, accid, sn);

        return mplew.getPacket();
    }

    public static byte[] payBackResult(int sn, int mp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(85);
        mplew.writeLong(sn);
        mplew.writeInt(mp);
        mplew.writeZeroBytes(10);
        return mplew.getPacket();
    }

    public static byte[] showXmasSurprise(int idFirst, Item item, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.XMAS_SURPRISE.getValue());
        mplew.write(0xE6);
        mplew.writeLong(idFirst); //uniqueid of the xmas surprise itself
        mplew.writeInt(0);
        addCashItemInfo(mplew, item, accid, 0); //info of the new item, but packet shows 0 for sn?
        mplew.writeInt(item.getItemId());
        mplew.write(1);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static final void addCashItemInfo(MaplePacketLittleEndianWriter mplew, Item item, int accId, int sn) {
        addCashItemInfo(mplew, item, accId, sn, true);
    }

    public static final void addCashItemInfo(MaplePacketLittleEndianWriter mplew, Item item, int accId, int sn, boolean isFirst) {
        addCashItemInfo(mplew, item.getUniqueId(), accId, item.getItemId(), sn, item.getQuantity(), item.getGiftFrom(), item.getExpiration(), isFirst); //owner for the lulz
    }

    public static final void addCashItemInfo(MaplePacketLittleEndianWriter mplew, int uniqueid, int accId, int itemid, int sn, int quantity, String sender, long expire) {
        addCashItemInfo(mplew, uniqueid, accId, itemid, sn, quantity, sender, expire, true);
    }

    public static final void addCashItemInfo(MaplePacketLittleEndianWriter mplew, int uniqueid, int accId, int itemid, int sn, int quantity, String sender, long expire, boolean isFirst) {
        mplew.writeLong(uniqueid > 0 ? uniqueid : 0); //16
        mplew.writeLong(accId);                       //16
        mplew.writeInt(itemid);                       //20
        mplew.writeInt(isFirst ? sn : 0);             //24
        mplew.writeShort(quantity);                   //26
        mplew.writeAsciiString(sender, 13);           //39owner for the lulzlzlzl
        PacketHelper.addExpirationTime(mplew, expire);//47
        mplew.writeLong(sn);
        //    mplew.writeLong(isFirst ? 0 : sn);            //55
//        mplew.writeZeroBytes(10);
        //additional 4 bytes for some stuff?
        //if (isFirst && uniqueid > 0 && GameConstants.isEffectRing(itemid)) {
        //	MapleRing ring = MapleRing.loadFromDb(uniqueid);
        //	if (ring != null) { //or is this only for friendship rings, i wonder. and does isFirst even matter
        //		mplew.writeMapleAsciiString(ring.getPartnerName());
        //		mplew.writeInt(itemid);
        //		mplew.writeShort(quantity);
        //	}
        //}
    }

    public static void addModCashItemInfo(MaplePacketLittleEndianWriter mplew, CashModInfo item) {
        int flags = item.flags;

        mplew.writeInt(item.sn);
        mplew.writeInt(flags);
        if ((flags & 0x1) != 0) {
            mplew.writeInt(item.itemid); //잘댐
        }
        if ((flags & 0x2) != 0) {
            mplew.writeShort(item.count);
        }
        if ((flags & 0x10) != 0) {
            mplew.write(item.priority); //우선순위. 숫자가 높을수록 앞에옴 잘댐
        }
        if ((flags & 0x4) != 0) {
            mplew.writeInt(item.discountPrice); //잘댐
        }
        if ((flags & 0x8) != 0) { //보너스인지 체크하는부분
            mplew.write(item.unk_1);
        }
        if ((flags & 0x20) != 0) { //0x20
            mplew.writeShort(item.period);
        }
        if ((flags & 0x40) != 0) { //메이플 포인트 ??
            mplew.writeInt(0);
        }
        if ((flags & 0x80) != 0) { //메소??
            mplew.writeInt(item.meso);
        }
        if ((flags & 0x100) >= 0) {
            mplew.write(item.forPremiumUser); //1.2.31 ok
        }
        if ((flags & 0x200) != 0) {
            mplew.write(item.gender); //1.2.31 ok
        }
        if ((flags & 0x400) != 0) {
            mplew.write(item.showUp ? 1 : 0); //not yet
        }
        if ((flags & 0x800) != 0) {
            //nexon call - class
            //-1 : None  0 : NEW  1 : SALE  2 : HOT  3 : EVENT 4: 한정판매
            mplew.write(item.mark); //1.2.31 ok
        }
        if ((flags & 0x1000) != 0) {
            mplew.write(item.unk_3 - 1); //Class
        }
        //pb = PayBack
        if ((flags & 0x2000) != 0) { //pbCash
            mplew.writeShort(0);
        }
        if ((flags & 0x4000) != 0) { //pbPoint
            mplew.writeShort(0);
        }
        if ((flags & 0x8000) != 0) { //pbGift
            mplew.writeShort(0);
        }
        if ((flags & 0x20000) != 0) { //ReqPOP
            mplew.writeShort(0);
        }
        if ((flags & 0x40000) != 0) { //ReqLEV
            mplew.writeShort(0);
        }
        if ((flags & 0x10000) != 0) {
            List<Integer> pack = CashItemFactory.getInstance().getPackageItems(item.sn);
            if (pack == null) {
                mplew.write(0);
            } else {
                mplew.write(pack.size());//패키지는 나중에
                for (int i = 0; i < pack.size(); i++) {
                    mplew.writeInt(pack.get(i));
                }
            }
        }
//        if ((flags & 0x80000) != 0) {
//            mplew.writeInt(0);
//        }
//        if ((flags & 0x100000) != 0) {
//            mplew.writeInt(0);
//        }
    }

    public static byte[] showBoughtCSNormalItem(int price, short quantity, byte position, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(120);
        mplew.writeInt(price);//패키지를 의식한듯? price가 아니라 for문 갯수임
        mplew.writeShort(quantity);
        mplew.writeShort(position);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    /*
     * 에러메시지
     *
     *
     * 97   작업 처리 시간이 초과되었습니다. 잠시 후 다시 시도하세요.
     * 99   캐시 잔액이 부족합니다.
     * 100  만 14세 미만이신 분은 캐시 아이템을선물하실 수 없습니다.
     * 101  선물 가능한 한도액을 초과하셨습니다.
     * 102  보유 가능한 캐시 아이템 개수를초과하지 않았는지 확인해주세요.
     * 103  
     * 104  
     * 105  기한 만료된 쿠폰입니다.
     * 106  이미 사용된 쿠폰입니다.
     * 107  넥슨 가맹 PC방에서만 사용할 수 있는 쿠폰입니다. 넥슨 가맹 PC방에서 이용하세요.
     * 108  넥슨 가맹 PC방 전용 쿠폰이며 이미 사용된 쿠폰입니다.
     * 109  넥슨 가맹 PC방 전용 쿠폰이며 기한 만료된 쿠폰입니다.
     * 104  쿠폰 번호가 맞는지 다시 확인 하여주십시오.
     * 111  성별이 맞지 않아 쿠폰을 사용하실 수 없습니다.
     * 112  이 쿠폰은 일반아이템 전용으로 다른 사람에게 선물할 수 없습니다.
     * 113  이 쿠폰은 메이플포인트 전용으로 다른 사람에게 선물할 수 없습니다.
     * 114  남은 아이템 슬롯이 부족하지 않은지 확인해보세요. 
     * 119  커플 아이템은 같은 월드의 성별이 다른 캐릭터에게만 선물해 줄 수 있습니다. 선물해 주려는 캐릭터가 같은 월드에 있는지 성별은 다른지 확인해보세요.
     * 118  지금은 구매 가능한 시간이 아닙니다.
     * 119  이 아이템은 더이상 남아있지 않아 구매할 수 없습니다.
     * 120  넥슨캐쉬 구매한도를 초과하였습니다.
     * 121  메소가 부족합니다.
     * 122  주민등록번호를 확인하고 다시 시도해 주세요.
     * 110  넥슨캐시쿠폰번호입니다. 넥슨닷컴(www.nexon.com)의 마이페이지>넥슨캐시 메뉴에 쿠폰등록하세요.
     * 123  해당 쿠폰은 캐시아이템 신규 구입자만 사용이 가능합니다.
     * 124  이미 응모하셨습니다.
     * @return 에러패킷
     */
    public static byte[] sendCSFail(int err) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(57); //wish fail
        mplew.write(err);

        return mplew.getPacket();
    }

    public static byte[] sendCouponFail(final MapleClient c, int err) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(31); //wish fail
        mplew.write(err);

        return mplew.getPacket();
    }

    public static byte[] showCouponRedeemedItem(int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.writeShort(56);
        mplew.writeInt(0);
        mplew.writeInt(1);
        mplew.writeShort(1);
        mplew.writeShort(0x1A);
        mplew.writeInt(itemid);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] redeemResponse() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0xAE);
        mplew.writeInt(0);
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] showCouponRedeemedItem(Map<Integer, Item> items, int mesos, int maplePoints, MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(55); //use to be 4c
        mplew.write(items.size());
        for (Entry<Integer, Item> item : items.entrySet()) {
            addCashItemInfo(mplew, item.getValue(), c.getAccID(), item.getKey().intValue());
        }
        mplew.writeLong(maplePoints);
        mplew.writeInt(mesos);

        return mplew.getPacket();
    }

    public static byte[] enableCSUse() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_USE.getValue());
        mplew.write(1);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] getCSInventory(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(54); // use to be 3e
        CashShop mci = c.getPlayer().getCashInventory();
        int size = 0;
        mplew.writeShort(mci.getItemsSize());
        for (Item itemz : mci.getInventory()) {
            addCashItemInfo(mplew, itemz, c.getAccID(), 0); //test
            if (GameConstants.isPet(itemz.getItemId()) || GameConstants.getInventoryType(itemz.getItemId()) == MapleInventoryType.EQUIP) {
                size++;
            }
        }
        mplew.writeShort(c.getPlayer().getStorage().getSlots());
        mplew.writeShort(c.getCharacterSlots());
        mplew.writeShort(10);
        mplew.writeShort(11); //04 00 <-- added?

        return mplew.getPacket();
    }

    public static byte[] extraSlotDone(int date) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(79); //use to be 5d
        mplew.writeShort(0);
        mplew.writeShort(date);

        return mplew.getPacket();
    }

    public static byte[] extraSlotFailed(byte reason) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(80); //use to be 5d
        mplew.write(reason);

        return mplew.getPacket();
    }

    /*public static byte[] get(MapleClient c) {
     MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

     mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
     mplew.write(54); // use to be 3e
     CashShop mci = c.getPlayer().getCashInventory();
     int size = 0;
     mplew.writeShort(mci.getItemsSize());
     for (Item itemz : mci.getInventory()) {
     addCashItemInfo(mplew, itemz, c.getAccID(), 0); //test
     if (GameConstants.isPet(itemz.getItemId()) || GameConstants.getInventoryType(itemz.getItemId()) == MapleInventoryType.EQUIP) {
     size++;
     }
     }
     if (mci.getInventory().size() > 0) {
     mplew.writeInt(size);
     for (Item itemz : mci.getInventory()) {
     if (GameConstants.isPet(itemz.getItemId()) || GameConstants.getInventoryType(itemz.getItemId()) == MapleInventoryType.EQUIP) {
     PacketHelper.addItemInfo(mplew, itemz, true, true);
     }
     }
     }
     mplew.writeInt(c.getPlayer().getStorage().getSlots());
     mplew.writeInt(c.getCharacterSlots());
     //        mplew.writeInt(c.getCharacterSlots()); //1.2.6 캐릭터 슬롯

     return mplew.getPacket();
     }*/
    //work on this packet a little more
    public static byte[] getCSGifts(MapleClient c, boolean donate) { //40 - 
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());

        mplew.write(56); //use to be 40
        List<Pair<Item, String>> mci = c.getPlayer().getCashInventory().loadGifts(donate);
        mplew.writeShort(mci.size());
        for (Pair<Item, String> mcz : mci) {
            mplew.writeLong(mcz.getLeft().getUniqueId());             //8
            mplew.writeInt(mcz.getLeft().getItemId());                //12
            mplew.writeAsciiString(mcz.getLeft().getGiftFrom(), 13);  //25
            mplew.writeAsciiString(mcz.getRight(), 73);               //98
        }

        return mplew.getPacket();
    }

    public static byte[] cashItemExpired(int uniqueid) { //109 ok
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(87); //use to be 5d
        mplew.writeLong(uniqueid);
        return mplew.getPacket();
    }

    public static byte[] sendGift(int price, int itemid, int quantity, String receiver) { //1.2.41 ok
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(69); //use to be 7C
        mplew.writeMapleAsciiString(receiver);
        mplew.writeInt(itemid);
        mplew.writeShort(quantity);
        mplew.writeShort(0); //maplePoints
        mplew.writeInt(price);

        return mplew.getPacket();
    }

    public static byte[] sendPackageGift(int price, int itemid, int quantity, String receiver) { //1.2.41 ok
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(118); //use to be 7C
        mplew.writeMapleAsciiString(receiver);
        mplew.writeInt(itemid);
        mplew.writeShort(quantity);
        mplew.writeShort(0); //maplePoints
        mplew.writeShort(price);

        return mplew.getPacket();
    }

    public static byte[] increasedInvSlots(int inv, int slots) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(71);
        mplew.write(inv);
        mplew.writeShort(slots);

        return mplew.getPacket();
    }

    //also used for character slots !
    public static byte[] increasedStorageSlots(int slots) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(73);
        mplew.writeShort(slots);

        return mplew.getPacket();
    }

    public static byte[] increasedCharacterSlots(int slots) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(75);
        mplew.writeShort(slots);

        return mplew.getPacket();
    }

    public static byte[] confirmToCSInventory(Item item, int accId, int sn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(83);
        addCashItemInfo(mplew, item, accId, sn, false);

        return mplew.getPacket();
    }

    public static byte[] confirmFromCSInventory(Item item, short pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(81);
        mplew.writeShort(pos);
        PacketHelper.addItemInfo(mplew, item, true, true);
        // mplew.writeInt(0); 1.2.6

        return mplew.getPacket();
    }

    public static byte[] sendMesobagFailed() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.MESOBAG_FAILURE.getValue());
        return mplew.getPacket();
    }

    public static byte[] sendMesobagSuccess(int mesos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.MESOBAG_SUCCESS.getValue());
        mplew.writeInt(mesos);
        return mplew.getPacket();
    }

    public static byte[] spawnMessageBox(int oid, int itemid, String sender, String msg, short x, short y) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.write(SendPacketOpcode.SPAWN_MESSAGEBOX.getValue());
        packet.writeInt(oid);
        packet.writeInt(itemid);
        packet.writeMapleAsciiString(msg);
        packet.writeMapleAsciiString(sender);
        packet.writeShort(x);
        packet.writeShort(y);
        return packet.getPacket();
    }

    public static byte[] destroyMessageBox(boolean animation, int oid) {
        MaplePacketLittleEndianWriter packet = new MaplePacketLittleEndianWriter();
        packet.write(SendPacketOpcode.DESTROY_MESSAGEBOX.getValue());
        packet.write(animation ? 0 : 1);
        packet.writeInt(oid);
        return packet.getPacket();
    }

    public static byte[] 응모() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(122);
        return mplew.getPacket();
    }
}
