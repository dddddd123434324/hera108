/*
 * This file is part of the OdinMS MapleStory Private Server
 * Copyright (C) 2012 Patrick Huy and Matthias Butz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package client;

import scripting.LieDetectorScript;
import server.Timer.EtcTimer;
import server.maps.MapleMap;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.MaplePacketCreator;
import tools.Pair;

/**
 * @author AuroX
 */
public class MapleLieDetector {

    public MapleCharacter chr;
    public byte type; // 0 = Normal, 1 = Admin Macro (Manager Skill) 
    public int attempt;
    public String tester, answer;
    public boolean inProgress, passed;
    public boolean sent = false;

    public MapleLieDetector(final MapleCharacter c) {
        this.chr = c;
        reset();
    }

    public final boolean startLieDetector(final String tester, final boolean isItem, final boolean anotherAttempt) {
        if (!anotherAttempt && ((isPassed() && isItem) || inProgress() || attempt == 2/* || answer != null || tester != null*/)) {
            return false;
        }
        final Pair<String, String> captcha = LieDetectorScript.getImageBytes();
        if (captcha == null) {
            return false;
        }
        final byte[] image = HexTool.getByteArrayFromHexString(captcha.getLeft());
        this.answer = captcha.getRight();
        this.tester = tester;
        this.inProgress = true;
        this.type = (byte) (isItem ? 0 : 1);
        this.attempt++;

        chr.getClient().getSession().write(MaplePacketCreator.sendLieDetector(image, isItem));
        if (!sent) {
            EtcTimer.getInstance().schedule(new Runnable() {

                @Override
                public void run() {
                    if (!isPassed() && chr != null) {
                        if (attempt >= 2) {
                            final MapleCharacter search_chr = chr.getMap().getCharacterByName(tester);
                            if (search_chr != null && search_chr.getId() != chr.getId()) {
                                search_chr.dropMessage(5, chr.getName() + "님께서 거짓말 탐지기에 적발되었습니다.");
                                search_chr.gainMeso(5000, true);
                            }
                            end();
                            //chr.dropMessage(1, "[알림] 거짓말탐지기 테스트에 적발되셨으므로 마을로 이동됩니다.");
                            //chr.dropMessage(6, "[알림] 또한 거짓말탐지기 테스트에 여러번 적발될 시 아이피밴, 아이디삭제가 가해지니 유의해주시길 바랍니다.");
                            chr.getClient().getSession().write(MaplePacketCreator.LieDetectorResponse((byte) 7, (byte) 4)); 
                            final MapleMap to = chr.getMap().getReturnMap();
                            chr.changeMap(to, to.getPortal(0));
                            sent = false; // 이거 맞음?
                            FileoutputUtil.log(FileoutputUtil.LieDetector_log, chr.getName() + "님이 시간내에 거짓말탐지기에 대답하지 않으셨습니다. \r\n");
                        } else {
                            startLieDetector(tester, isItem, true);
                        }
                    }
                }
            }, 60000); // 60 secs 
        } else {
            return false;
        }
        return true;
    }

    public final int getAttempt() {
        return attempt;
    }

    public final byte getLastType() {
        return type;
    }

    public final String getTester() {
        return tester;
    }

    public final String getAnswer() {
        return answer;
    }

    public final boolean inProgress() {
        return inProgress;
    }

    public final boolean isPassed() {
        return passed;
    }

    public final void end() {
        this.inProgress = false;
        this.passed = true;
        this.attempt = 0;
    }

    public final void reset() { // called when change map, cc, reenter cs, or login 
        this.tester = "";
        this.answer = "";
        this.attempt = 0;
        this.inProgress = false;
        this.passed = false;
    }
}
