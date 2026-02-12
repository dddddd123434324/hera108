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
package handling.channel;

import client.MapleCharacter;
import handling.login.LoginServer;
import handling.world.CheaterData;
import scripting.EventScriptManager;
import server.MapleSquad;
import server.MapleSquad.MapleSquadType;
import server.ServerProperties;
import server.events.*;
import server.life.PlayerNPC;
import server.maps.AramiaFireWorks;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.maps.MapleMapObject;
import server.shops.AbstractPlayerStore;
import server.shops.HiredMerchant;
import server.shops.HiredMerchantSave;
import server.shops.MaplePlayerShop;
import tools.ConcurrentEnumMap;
import tools.MaplePacketCreator;
import tools.Pair;

import java.io.File;
import java.net.ServerSocket;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ChannelServer {

    public static long serverStartTime;
    private short port = 8585;
    private static final short DEFAULT_PORT = 8585;
    private int channel, running_MerchantID = 0, flags = 0;
    private String serverMessage, ip, serverName;
    private boolean shutdown = false, finishedShutdown = false, MegaphoneMuteState = false, adminOnly = false;
    private PlayerStorage players;
    private final MapleMapFactory mapFactory;
    private EventScriptManager eventSM;
    private AramiaFireWorks works = new AramiaFireWorks();
    private static final Map<Integer, ChannelServer> instances = new HashMap<Integer, ChannelServer>();
    private final Map<MapleSquadType, MapleSquad> mapleSquads = new ConcurrentEnumMap<MapleSquadType, MapleSquad>(MapleSquadType.class);
    private final Map<Integer, HiredMerchant> merchants = new HashMap<Integer, HiredMerchant>();
    private final List<PlayerNPC> playerNPCs = new LinkedList<PlayerNPC>();
    private final ReentrantReadWriteLock merchLock = new ReentrantReadWriteLock(); //merchant
    private int eventmap = -1;
    private final Map<MapleEventType, MapleEvent> events = new EnumMap<MapleEventType, MapleEvent>(MapleEventType.class);
    private ChannelServerThread thread;

    private ChannelServer(final int channel) {
        this.channel = channel;
        mapFactory = new MapleMapFactory(channel);
    }

    public static Set<Integer> getAllInstance() {
        return new HashSet<Integer>(instances.keySet());
    }

    public static int getOnlineConnections() {
        int r = 0;
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            r += cserv.getConnectedClients();
        }
        return r;
    }

    public final void loadEvents() {
        if (events.size() != 0) {
            return;
        }
        events.put(MapleEventType.CokePlay, new MapleCoconut(channel, MapleEventType.CokePlay)); //yep, coconut. same shit
        events.put(MapleEventType.Coconut, new MapleCoconut(channel, MapleEventType.Coconut));
        events.put(MapleEventType.Fitness, new MapleFitness(channel, MapleEventType.Fitness));
        events.put(MapleEventType.OlaOla, new MapleOla(channel, MapleEventType.OlaOla));
        events.put(MapleEventType.OxQuiz, new MapleOxQuiz(channel, MapleEventType.OxQuiz));
        events.put(MapleEventType.Snowball, new MapleSnowball(channel, MapleEventType.Snowball));
        events.put(MapleEventType.Survival, new MapleSurvival(channel, MapleEventType.Survival));
    }

    public final void run_startup_configurations() throws Exception {
        System.out.print("Channel Server " + channel + " Initializing.. Retriving Login Server Properties..");
        setChannel(channel); //instances.put
        try {
            serverMessage = ServerProperties.getProperty("serverMessage");
            flags = Integer.parseInt(ServerProperties.getProperty("tempFlags", "0"));
            adminOnly = Boolean.parseBoolean(ServerProperties.getProperty("adminOnly", "false"));
            eventSM = new EventScriptManager(this, getEvents());
            port = Short.parseShort(ServerProperties.getProperty("channel" + channel + "Port", String.valueOf(DEFAULT_PORT + channel)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ip = ServerProperties.getProperty("host") + ":" + port;

        System.out.println("OK!\r\n");
        System.out.println(":: Channel Server " + channel + " Information ::");
        System.out.println("ServerMessage              = " + serverMessage);
        thread = new ChannelServerThread(channel, port);
        thread._serverSocket = new ServerSocket(port);
        System.out.println("Port " + port + " Opened.");
        thread.start();

//        ByteBuffer.setUseDirectBuffers(false);
//        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
        players = new PlayerStorage(channel);
        loadEvents();

        eventSM.init();
    }

    private static String[] getEvents() {
        List<String> events = new ArrayList<>();
        for (File file : new File("scripts/event").listFiles()) {
            events.add(file.getName().replace(".js", ""));
        }
        return events.toArray(new String[0]);
    }

    public final void shutdown() {
        if (finishedShutdown) {
            return;
        }
        broadcastPacket(MaplePacketCreator.serverNotice(0, "현재 채널이 종료됩니다."));
        // dc all clients by hand so we get sessionClosed...
        shutdown = true;

        System.out.println("Channel " + channel + ", Saving characters...");
        System.out.println("Channel " + channel + ", saved " + getPlayerStorage().save() + " characters.");

        System.out.println("Channel " + channel + ", Unbinding...");

        //temporary while we dont have !addchannel
        instances.remove(channel);
        setFinishShutdown();
    }

    public final boolean hasFinishedShutdown() {
        return finishedShutdown;
    }

    public final MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    public static final ChannelServer newInstance(final int channel) {
        return new ChannelServer(channel);
    }

    public static final ChannelServer getInstance(final int channel) {
        return instances.get(channel);
    }

    public final void addPlayer(final MapleCharacter chr) {
        getPlayerStorage().registerPlayer(chr);
    }

    public final PlayerStorage getPlayerStorage() {
        if (players == null) { //wth
            players = new PlayerStorage(channel); //wthhhh
        }
        return players;
    }

    public final void removePlayer(final MapleCharacter chr) {
        getPlayerStorage().deregisterPlayer(chr);
    }

    public final void removePlayer(final int idz, final String namez) {
        getPlayerStorage().deregisterPlayer(idz, namez);
    }

    public final String getServerMessage() {
        return serverMessage;
    }

    public final void setServerMessage(final String newMessage) {
        serverMessage = newMessage;
        broadcastPacket(MaplePacketCreator.serverMessage(serverMessage));
    }

    public final void broadcastPacket(final byte[] data) {
        getPlayerStorage().broadcastPacket(data);
    }

    public final void broadcastSmegaPacket(final byte[] data) {
        getPlayerStorage().broadcastSmegaPacket(data);
    }

    public final void broadcastGMPacket(final byte[] data) {
        getPlayerStorage().broadcastGMPacket(data);
    }

    public final int getChannel() {
        return channel;
    }

    public final void setChannel(final int channel) {
        instances.put(channel, this);
        LoginServer.addChannel(channel);
    }

    public static final ArrayList<ChannelServer> getAllInstances() {
        return new ArrayList<ChannelServer>(instances.values());
    }

    public final String getIP() {
        return ip;
    }

    public final boolean isShutdown() {
        return shutdown;
    }

    public final int getLoadedMaps() {
        return mapFactory.getLoadedMaps();
    }

    public final EventScriptManager getEventSM() {
        return eventSM;
    }

    public final void reloadEvents() {
        eventSM.cancel();
        eventSM = new EventScriptManager(this, getEvents());
        eventSM.init();
    }

    public static final void startChannel_Main() throws Exception {
        serverStartTime = System.currentTimeMillis();

        for (int i = 0; i < Integer.parseInt(ServerProperties.getProperty("channelcount", "0")); i++) {
            newInstance(i + 1).run_startup_configurations();
        }
    }

    public static final void startChannel(int ch) throws Exception {
        newInstance(ch).run_startup_configurations();
    }

    public Map<MapleSquadType, MapleSquad> getAllSquads() {
        return Collections.unmodifiableMap(mapleSquads);
    }

    public final MapleSquad getMapleSquad(final String type) {
        return getMapleSquad(MapleSquadType.valueOf(type.toLowerCase()));
    }

    public final MapleSquad getMapleSquad(final MapleSquadType type) {
        return mapleSquads.get(type);
    }

    public final boolean addMapleSquad(final MapleSquad squad, final String type) {
        final MapleSquadType types = MapleSquadType.valueOf(type.toLowerCase());
        if (types != null && !mapleSquads.containsKey(types)) {
            mapleSquads.put(types, squad);
            squad.scheduleRemoval();
            return true;
        }
        return false;
    }

    public final boolean removeMapleSquad(final MapleSquadType types) {
        if (types != null && mapleSquads.containsKey(types)) {
            mapleSquads.remove(types);
            return true;
        }
        return false;
    }

    public final int closeAllMerchant() {
        int ret = 0;
        merchLock.writeLock().lock();
        try {
            final Iterator<Entry<Integer, HiredMerchant>> merchants_ = merchants.entrySet().iterator();
            while (merchants_.hasNext()) {
                HiredMerchant hm = merchants_.next().getValue();
                HiredMerchantSave.QueueShopForSave(hm);
                hm.getMap().removeMapObject(hm);
                merchants_.remove();
                ret++;
            }
        } finally {
            merchLock.writeLock().unlock();
        }
        //hacky
        for (int i = 910000001; i <= 910000022; i++) {
            for (MapleMapObject mmo : mapFactory.getMap(i).getAllHiredMerchantsThreadsafe()) {
                HiredMerchantSave.QueueShopForSave((HiredMerchant) mmo);
                ret++;
            }
        }
        return ret;
    }

    public final int addMerchant(final HiredMerchant hMerchant) {
        merchLock.writeLock().lock();
        try {
            running_MerchantID++;
            merchants.put(running_MerchantID, hMerchant);
            return running_MerchantID;
        } finally {
            merchLock.writeLock().unlock();
        }
    }

    public final void removeMerchant(final HiredMerchant hMerchant) {
        merchLock.writeLock().lock();

        try {
            merchants.remove(hMerchant.getStoreId());
        } finally {
            merchLock.writeLock().unlock();
        }
    }

    public final boolean containsMerchant(final int accid, int cid) {
        boolean contains = false;

        merchLock.readLock().lock();
        try {
            final Iterator itr = merchants.values().iterator();

            while (itr.hasNext()) {
                HiredMerchant hm = (HiredMerchant) itr.next();
                if (hm.getOwnerAccId() == accid || hm.getOwnerId() == cid) {
                    contains = true;
                    break;
                }
            }
        } finally {
            merchLock.readLock().unlock();
        }
        return contains;
    }

    public final Pair<Integer, Integer> findMerchant(final int accid, int cid) {

        merchLock.readLock().lock();
        try {
            final Iterator itr = merchants.values().iterator();

            while (itr.hasNext()) {
                HiredMerchant hm = (HiredMerchant) itr.next();
                if (hm.getOwnerAccId() == accid || hm.getOwnerId() == cid) {
                    return new Pair<Integer, Integer>(getChannel(), hm.getMap().getId() % 100);
                }
            }
        } finally {
            merchLock.readLock().unlock();
        }
        return null;
    }

    public final HiredMerchant findAndGetMerchant(final int accid, int cid) {

        merchLock.readLock().lock();
        try {
            final Iterator itr = merchants.values().iterator();

            while (itr.hasNext()) {
                HiredMerchant hm = (HiredMerchant) itr.next();
                if (hm.getOwnerAccId() == accid || hm.getOwnerId() == cid) {
                    return hm;
                }
            }
        } finally {
            merchLock.readLock().unlock();
        }
        return null;
    }

    public final List<AbstractPlayerStore> searchShop(final int itemSearch, List<AbstractPlayerStore> list) {
        merchLock.readLock().lock();
        try {
            final Iterator itr = merchants.values().iterator();
            while (itr.hasNext()) {
                HiredMerchant hm = (HiredMerchant) itr.next();
                if (hm.searchItem(itemSearch).size() > 0) {
                    list.add(hm);
                }
            }
        } finally {
            merchLock.readLock().unlock();
        }
        for (int m = 910000001; m <= 910000022; ++m) {
            MapleMap map = getMapFactory().getMap(m);
            for (MapleCharacter chr : map.getCharactersThreadsafe()) {
                if (chr.getMapId() == m && chr.getPlayerShop() != null && chr.getPlayerShop() instanceof MaplePlayerShop) {
                    MaplePlayerShop shop = (MaplePlayerShop) chr.getPlayerShop();
                    if (shop.isOpen()) {
                        if (shop.searchItem(itemSearch).size() > 0) {
                            list.add(shop);
                        }
                    }
                }
            }
        }
        return list;
    }

    public final void toggleMegaphoneMuteState() {
        this.MegaphoneMuteState = !this.MegaphoneMuteState;
    }

    public final boolean getMegaphoneMuteState() {
        return MegaphoneMuteState;
    }

    public int getEvent() {
        return eventmap;
    }

    public final void setEvent(final int ze) {
        this.eventmap = ze;
    }

    public MapleEvent getEvent(final MapleEventType t) {
        return events.get(t);
    }

    public final Collection<PlayerNPC> getAllPlayerNPC() {
        return playerNPCs;
    }

    public final String getRealChannelName() {
        return String.valueOf(channel == 1 ? "1" : channel == 2 ? "20세이상"
                : channel - 1);
    }

    public final void addPlayerNPC(final PlayerNPC npc) {
        if (playerNPCs.contains(npc)) {
            return;
        }
        playerNPCs.add(npc);
        getMapFactory().getMap(npc.getMapId()).addMapObject(npc);
    }

    public final void removePlayerNPC(final PlayerNPC npc) {
        if (playerNPCs.contains(npc)) {
            playerNPCs.remove(npc);
            getMapFactory().getMap(npc.getMapId()).removeMapObject(npc);
        }
    }

    public final String getServerName() {
        return serverName;
    }

    public final void setServerName(final String sn) {
        this.serverName = sn;
    }

    public final String getTrueServerName() {
        return serverName;
    }

    public final int getPort() {
        return port;
    }

    public static final Set<Integer> getChannelServer() {
        return new HashSet<Integer>(instances.keySet());
    }

    public final void setShutdown() {
        this.shutdown = true;
        System.out.println("Channel " + channel + " has set to shutdown and is closing Hired Merchants...");
    }

    public final void setFinishShutdown() {
        this.finishedShutdown = true;
        System.out.println("Channel " + channel + " has finished shutdown.");
    }

    public final boolean isAdminOnly() {
        return adminOnly;
    }

    public final static int getChannelCount() {
        return instances.size();
    }

    public final int getTempFlag() {
        return flags;
    }

    public static Map<Integer, Integer> getChannelLoad() {
        Map<Integer, Integer> ret = new HashMap<Integer, Integer>();
        for (ChannelServer cs : instances.values()) {
            ret.put(cs.getChannel(), cs.getConnectedClients());
        }
        return ret;
    }

    public int getConnectedClients() {
        return getPlayerStorage().getConnectedClients();
    }

    public List<CheaterData> getCheaters() {
        List<CheaterData> cheaters = getPlayerStorage().getCheaters();

        Collections.sort(cheaters);
        return cheaters;
    }

    public List<CheaterData> getReports() {
        List<CheaterData> cheaters = getPlayerStorage().getReports();

        Collections.sort(cheaters);
        return cheaters;
    }

    public void broadcastMessage(byte[] message) {
        broadcastPacket(message);
    }

    public void broadcastSmega(byte[] message) {
        broadcastSmegaPacket(message);
    }

    public void broadcastGMMessage(byte[] message) {
        broadcastGMPacket(message);
    }

    public AramiaFireWorks getFireWorks() {
        return works;
    }
}
