package scripting;

import client.MapleClient;
import scripting.vm.NPCScriptInvoker;

public class ItemScriptMethods extends AbstractPlayerInteraction {

    private final String scriptName;

    public ItemScriptMethods(MapleClient c, int npc, int itemId, String scriptName) {
        super(c, npc, itemId);
        this.scriptName = scriptName;
    }

    public void runNpc() {
        getClient().removeClickedNPC();
        //비VM 스크립트는 지원하지 않음.
        NPCScriptInvoker.runNpc(c, id, 0, scriptName);
    }

    public void use() {
        gainItem(id2, (short) -1);
    }
}
