/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.MapleCharacter;
import client.MapleClient;
import constants.GameConstants;
import handling.SendPacketOpcode;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import handling.world.exped.MapleExpedition;
import handling.world.exped.PartySearch;
import handling.world.exped.PartySearchType;
import java.util.ArrayList;
import java.util.List;
import tools.data.MaplePacketLittleEndianWriter;

/**
 *
 * @author 큐티버크
 */
public class PartyPacket {

    public static byte[] partyCreated(int partyid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(8);
        mplew.writeInt(partyid);
        mplew.writeInt(999999999);
        mplew.writeInt(999999999);
        mplew.writeLong(0);
        mplew.write(0);
        mplew.write(1);

        return mplew.getPacket();
    }

    private static void addPartyStatus(int forchannel, MapleParty party, MaplePacketLittleEndianWriter lew, boolean leaving, boolean exped) {
        List<MaplePartyCharacter> partymembers;
        if (party == null) {
            partymembers = new ArrayList<>();
        } else {
            partymembers = new ArrayList<>(party.getMembers());
        }
        while (partymembers.size() < 6) {
            partymembers.add(new MaplePartyCharacter());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeAsciiString(partychar.getName(), 13);
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getJobId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getLevel());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.isOnline()) {
                lew.writeInt(partychar.getChannel() - 1);
            } else {
                lew.writeInt(-2);
            }
        }
        lew.writeInt(party == null ? 0 : party.getLeader().getId());
        if (exped) {
            return;
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forchannel) {
                lew.writeInt(partychar.getMapid());
            } else {
                lew.writeInt(0);
            }
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forchannel && !leaving) {
                lew.writeInt(partychar.getDoorTown());
                lew.writeInt(partychar.getDoorTarget());
                lew.writeInt(partychar.getDoorSkill());
                lew.writeInt(partychar.getDoorPosition().x);
                lew.writeInt(partychar.getDoorPosition().y);
            } else {
                lew.writeInt(leaving ? 999999999 : 0);
                lew.writeInt(leaving ? 999999999 : 0);
                lew.writeLong(leaving ? -1 : 0);
            }
        }
    }

    private static void addPartyStatus(MapleClient c, int forchannel, MapleParty party, MaplePacketLittleEndianWriter lew, boolean leaving, boolean exped) {
        List<MaplePartyCharacter> partymembers;
        if (party == null) {
            partymembers = new ArrayList<>();
        } else {
            partymembers = new ArrayList<>(party.getMembers());
            if (leaving) {
                for (MaplePartyCharacter pch : partymembers) {
                    if (pch.getId() == c.getPlayer().getId()) {
                        partymembers.remove(pch);
                        break;
                    }
                }
            }
        }
        while (partymembers.size() < 6) {
            partymembers.add(new MaplePartyCharacter());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeAsciiString(partychar.getName(), 13);
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getJobId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getLevel());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.isOnline()) {
                lew.writeInt(partychar.getChannel() - 1);
            } else {
                lew.writeInt(-2);
            }
        }
        lew.writeInt(party == null ? 0 : party.getLeader().getId());
        if (exped) {
            return;
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forchannel) {
                lew.writeInt(partychar.getMapid());
            } else {
                lew.writeInt(0);
            }
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forchannel && !leaving) {
                lew.writeInt(partychar.getDoorTown());
                lew.writeInt(partychar.getDoorTarget());
                lew.writeInt(partychar.getDoorSkill());
                lew.writeInt(partychar.getDoorPosition().x);
                lew.writeInt(partychar.getDoorPosition().y);
            } else {
                lew.writeInt(leaving ? 999999999 : 0);
                lew.writeInt(leaving ? 999999999 : 0);
                lew.writeLong(leaving ? -1 : 0);
            }
        }
    }

    public static byte[] expeditionUpdate(final MapleExpedition exped) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        mplew.write(56);
        mplew.writeInt(exped.getType().exped);
        mplew.writeInt(0); //eh?
        for (int i = 0; i < 5; i++) { //all parties in the exped other than the leader
            if (i < exped.getParties().size()) {
                MapleParty party = World.Party.getParty(exped.getParties().get(i));
                if (party != null) {
                    addPartyStatus(-1, party, mplew, false, true);
                } else {
                    mplew.writeZeroBytes(2020);
                }
            } else {
                mplew.writeZeroBytes(2020);
            }
        }
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    public static byte[] expeditionRemove() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        mplew.write(57); // 말 없이 나감

        return mplew.getPacket();
    }

    public static byte[] expeditionStatus(final MapleExpedition exped, boolean created) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        mplew.write(created ? 58 : 60); // true 0x3B
        mplew.writeInt(exped.getType().exped); // 2001
        mplew.writeInt(0); //eh?
        for (int i = 0; i < 5; i++) { //all parties in the exped other than the leader
            if (i < exped.getParties().size()) {
                MapleParty party = World.Party.getParty(exped.getParties().get(i));
                if (party != null) {
                    addPartyStatus(-1, party, mplew, false, true);
                } else {
                    mplew.writeZeroBytes(2020); //length of the addPartyStatus.
                }
            } else {
                mplew.writeZeroBytes(2020); //length of the addPartyStatus.
            }
        }
        mplew.writeShort(0); //wonder if this goes here or at bottom

        return mplew.getPacket();
    }

    public static byte[] expeditionJoined(final String name) { // '%s'님이 원정대에 가입하셨습니다.
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        mplew.write(59);
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    public static byte[] expeditionLeft(final String name) { // '%s'님이 원정대를 탈퇴하셨습니다.
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        mplew.write(63);
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    public static byte[] expeditionLeft() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        mplew.write(64); // 원정대를 탈퇴하였습니다.

        return mplew.getPacket();
    }

    public static byte[] expeditionKick(final String name) { // '%s'님이 원정대에서 강퇴당하셨습니다.
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        mplew.write(65);
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    public static byte[] expeditionKick() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        mplew.write(66); // 원정대에서 강퇴당했습니다.

        return mplew.getPacket();
    }

    public static byte[] expeditionBreak() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        mplew.write(67); // 원정대를 해체

        return mplew.getPacket();
    }

    public static byte[] expeditionLeaderChanged() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        mplew.write(68);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] expeditionUpdate(final int partyIndex, final MapleParty party) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        mplew.write(69);
        mplew.writeInt(0); //lol?
        mplew.writeInt(partyIndex);
        if (party == null) {
            mplew.writeZeroBytes(178); //length of the addPartyStatus.
        } else {
            addPartyStatus(-1, party, mplew, false, true);
        }
        return mplew.getPacket();
    }

    public static byte[] expeditionUpdate(MapleClient c, final int partyIndex, final MapleParty party) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        mplew.write(69);
        mplew.writeInt(0); //lol?
        mplew.writeInt(partyIndex);
        if (party == null) {
            mplew.writeZeroBytes(178); //length of the addPartyStatus.
        } else {
            addPartyStatus(c, -1, party, mplew, true, true);
        }
        return mplew.getPacket();
    }

    public static byte[] expeditionInvite(MapleCharacter from, int exped) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        mplew.write(71);
        mplew.writeInt(from.getLevel());
        mplew.writeInt(from.getJob());
        mplew.writeMapleAsciiString(from.getName());
        mplew.writeInt(exped);

        return mplew.getPacket();
    }

    public static byte[] expeditionError(final int errcode, final String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.EXPEDITION_OPERATION.getValue());
        mplew.write(72);
        mplew.writeInt(errcode); //0 = not found, 1 = admin, 2 = already in a part, 3 = not right lvl, 4 = blocked, 5 = taking another, 6 = already in, 7 = all good
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    public static byte[] getPartyListing(final PartySearchType pst) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(76);
        mplew.writeInt(pst.id);//파티퀘스트 아이디
        final List<PartySearch> parties = World.Party.searchParty(pst);
        mplew.writeInt(parties.size());
        for (PartySearch party : parties) {
            mplew.writeInt(9728744); //ive no clue,either E8 72 94 00 or D8 72 94 00 
            mplew.writeInt(2); //again, no clue, seems to remain constant?
            if (pst.exped) {
                MapleExpedition me = World.Party.getExped(party.getId());
                mplew.writeInt(me.getType().maxMembers);
                System.out.println("me.getType().maxMembers"+me.getType().maxMembers);
                mplew.writeInt(party.getId());
                mplew.writeAsciiString(party.getName(), 48);
                for (int i = 0; i < 5; i++) { //all parties in the exped other than the leader
                    if (i < me.getParties().size()) {
                        MapleParty part = World.Party.getParty(me.getParties().get(i));
                        if (part != null) {
                            addPartyStatus(-1, part, mplew, false, true);
                        } else {
                            mplew.writeZeroBytes(202); //length of the addPartyStatus.
                        }
                    } else {
                        mplew.writeZeroBytes(202); //length of the addPartyStatus.
                    }
                }
            } else {
                mplew.writeInt(6); //??
                mplew.writeInt(party.getId());
                mplew.writeAsciiString(party.getName(), 48);
                addPartyStatus(-1, World.Party.getParty(party.getId()), mplew, false, true); //if exped, send 0, if not then skip
            }

            mplew.writeShort(0); //wonder if this goes here or at bottom
        }

        return mplew.getPacket();
    }

    public static byte[] partyListingAdded(final PartySearch ps) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(74);
        mplew.writeInt(ps.getType().id);
        mplew.writeInt(0); //ive no clue,either 48 DB 60 00 or 18 DB 60 00
        mplew.writeInt(1);
        if (ps.getType().exped) {
            MapleExpedition me = World.Party.getExped(ps.getId());
            mplew.writeInt(me.getType().maxMembers);
            mplew.writeInt(ps.getId());
            mplew.writeAsciiString(ps.getName(), 48);
            for (int i = 0; i < 5; i++) { //all parties in the exped other than the leader
                if (i < me.getParties().size()) {
                    MapleParty party = World.Party.getParty(me.getParties().get(i));
                    if (party != null) {
                        addPartyStatus(-1, party, mplew, false, true);
                    } else {
                        mplew.writeZeroBytes(202); //length of the addPartyStatus.
                    }
                } else {
                    mplew.writeZeroBytes(202); //length of the addPartyStatus.
                }
            }
        } else {
            mplew.writeInt(6); //파티장아이디같은데
            mplew.writeInt(ps.getId());
            mplew.writeAsciiString(ps.getName(), 48);
            addPartyStatus(-1, World.Party.getParty(ps.getId()), mplew, false, true); //if exped, send 0, if not then skip
        }
        mplew.writeShort(0); //wonder if this goes here or at bottom

        return mplew.getPacket();
    }
}
