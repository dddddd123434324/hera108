package server;

import client.MapleCharacter;
import handling.channel.ChannelServer;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

/**
 * Note for this class : MapleCharacter reference must be removed immediately
 * after cpq or upon dc.
 *
 * @author Rob
 */
public class MapleCarnivalParty {

    private List<Integer> members = new LinkedList<Integer>();
    private WeakReference<MapleCharacter> leader;
    private byte team;
    private int channel;
    private short availableCP = 0, totalCP = 0,  raidPoint = 0;
    private boolean winner = false;
    private boolean draw = false;

    public MapleCarnivalParty(final MapleCharacter owner, final List<MapleCharacter> members1, final byte team1) {
        leader = new WeakReference<MapleCharacter>(owner);
        for (MapleCharacter mem : members1) {
            members.add(mem.getId());
            mem.setCarnivalParty(this);
        }
        team = team1;
        channel = owner.getClient().getChannel();
    }

    public final MapleCharacter getLeader() {
        return leader.get();
    }

    public void addCP(MapleCharacter player, int ammount) {
        totalCP += ammount;
        availableCP += ammount;
        totalCP = (short) Math.max(0, totalCP);
        availableCP = (short) Math.max(0, availableCP);
        player.addCP(ammount);
    }

    public int getTotalCP() {
        return totalCP;
    }

    public int getAvailableCP() {
        return availableCP;
    }

    public void useCP(MapleCharacter player, int ammount) {
        availableCP -= ammount;
        availableCP = (short) Math.max(0, availableCP);
        player.useCP(ammount);
    }

    public List<Integer> getMembers() {
        return members;
    }
    
    public short getRaidPoint() {
        return raidPoint;
    }

    public void addRaidPoint(short raidPoint) {
        this.raidPoint += raidPoint;
    }

    public void setRaidPoint(short raidPoint) {
        this.raidPoint = raidPoint;
    }

    public int getTeam() {
        return team;
    }

    public void warp(final MapleMap map, final String portalname) {
        for (int chr : members) {
            final MapleCharacter c = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterById(chr);
            if (c != null) {
                c.changeMap(map, map.getPortal(portalname));
            }
        }
    }

    public void warp(final MapleMap map, final int portalid) {
        for (int chr : members) {
            final MapleCharacter c = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterById(chr);
            if (c != null) {
                c.changeMap(map, map.getPortal(portalid));
            }
        }
    }

    public boolean allInMap(MapleMap map) {
        for (int chr : members) {
            if (map.getCharacterById(chr) == null) {
                return false;
            }
        }
        return true;
    }

    public void removeMember(MapleCharacter chr) {
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i) == chr.getId()) {
                members.remove(i);
                chr.setCarnivalParty(null);
            }
        }

    }

    public boolean isWinner() {
        return winner;
    }

    public void setWinner(boolean status) {
        winner = status;
    }
    
    public boolean isDraw() {
        return draw;
    }

    public void setDraw(boolean draw) {
        this.draw = draw;
    }

    public void displayMatchResult() {
        final String effect = winner ? "quest/carnival/win" : "quest/carnival/lose";
        final String sound = winner ? "MobCarnival/Win" : "MobCarnival/Lose";
        boolean done = false;
        for (int chr : members) {
            final MapleCharacter c = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterById(chr);
            if (c != null) {
                if (c.getMapId() / 1000000 == 923) {
                    c.dropMessage(6, "유령선 함장을 처치하였습니다. 잠시 후 함장실을 벗어납니다.");
                    c.getClient().getSession().write(MaplePacketCreator.showEffect("killing/clear"));
                } else {
                    if (winner) {
                        c.dropMessage(6, "몬스터카니발에서 승리하였습니다. 잠시 후 자동으로 이동하니 잠시만 기다려 주세요.");
                    } else {
                        c.dropMessage(6, "몬스터카니발에서 안타깝게도 지고 말았습니다. 잠시 후 자동으로 이동하니 잠시만 기다려 주세요.");
                    }
                    c.getClient().getSession().write(MaplePacketCreator.showEffect(effect));
                    c.getClient().getSession().write(MaplePacketCreator.playSound(sound));
                }
                if (!done) {
                    done = true;
                    c.getMap().killAllMonsters(true);
                    c.getMap().setSpawns(false); //resetFully will take care of this
                }
            }
        }

    }
}
