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

import constants.GameConstants;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import server.maps.AnimatedMapleMapObject;
import server.movement.*;
import tools.FileoutputUtil;
import tools.data.LittleEndianAccessor;

public class MovementParse {

    //1 = player, 2 = mob, 3 = pet, 4 = summon, 5 = dragon
    public static final List<LifeMovementFragment> parseMovement(final LittleEndianAccessor lea, int kind) {
        final List<LifeMovementFragment> res = new ArrayList<LifeMovementFragment>();
        final byte numCommands = lea.readByte();

        for (byte i = 0; i < numCommands; i++) {
            final byte command = lea.readByte();
            /*if (command != 0) {
             System.out.println("command" + command);
             }*/
            switch (command) {
                case 0: // normal move
                case 5:
                case 12:
                case 14:
                case 35:
                case 36: {
                    final short xpos = lea.readShort();
                    final short ypos = lea.readShort();
                    final short xwobble = lea.readShort();
                    final short ywobble = lea.readShort();
                    final short unk = lea.readShort();
                    short fh = 0;
                    if (command == 12) {
                        fh = lea.readShort();
                    }
                    final short xoffset = lea.readShort();
                    final short yoffset = lea.readShort();
                    final byte newstate = lea.readByte();
                    final short duration = lea.readShort();
                    final AbsoluteLifeMovement alm = new AbsoluteLifeMovement(command, new Point(xpos, ypos), duration, newstate, unk);
                    alm.setUnk(unk);
                    alm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    alm.setFH(fh);
                    alm.setOffset(new Point(xoffset, yoffset));
                    res.add(alm);
                    break;
                }
                case 99: { //?... 00 00 7A 03 0F 02 64 01 00 00 0F 00 04 5A 00
                    final short xpos = lea.readShort(); //not xpos
                    final short ypos = lea.readShort(); //not ypos
                    final short xwobble = lea.readShort();
                    final short ywobble = lea.readShort();
                    final short unk = lea.readShort();
                    final byte newstate = lea.readByte();
                    final short duration = lea.readShort();
                    final UnknownMovement um = new UnknownMovement(command, new Point(xpos, ypos), duration, newstate, unk);
                    um.setUnk(unk);
                    um.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(um);
                    break;
                }
                case 1:
                case 2:
                case 13:
                case 16:
                case 18:
                case 31:
                case 32:
                case 33:
                case 34: {
                    final short xmod = lea.readShort();
                    final short ymod = lea.readShort();
                    final byte newstate = lea.readByte();
                    final short duration = lea.readShort();
                    final RelativeLifeMovement rlm = new RelativeLifeMovement(command, new Point(xmod, ymod), duration, newstate, duration);
                    res.add(rlm);
                    break;
                }
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 26:
                case 27:
                case 28:
                case 29:
                case 30: {
                    final byte newstate = lea.readByte();
                    final short duration = lea.readShort();
                    final AranMovement am = new AranMovement(command, new Point(0, 0), duration, newstate, 0);
                    res.add(am);
                    break;
                }
                case 3:
                case 4: // assaulter
                case 6: // assaulter
                case 7: //same structure
                case 8://same structure
                case 10://chair
                {
                    final short xpos = lea.readShort();
                    final short ypos = lea.readShort();
                    final short unk = lea.readShort();
                    final byte newstate = lea.readByte();
                    final short duration = lea.readShort();
                    final TeleportMovement tm = new TeleportMovement(command, new Point(xpos, ypos), duration, newstate, 0);
                    tm.setUnk(unk);
                    res.add(tm);
                    break;
                }
                case 11: // jump down??
                {
                    final short xpos = lea.readShort();
                    final short ypos = lea.readShort();
                    final short fh = lea.readShort();
                    final byte newstate = lea.readByte();
                    final short duration = lea.readShort();
                    final ChairMovement cm = new ChairMovement(command, new Point(xpos, ypos), duration, newstate, 0);
                    cm.setFh(fh);
                    res.add(cm);
                    break;
                }
                case 17: // 확실
                {
                    final short xpos = lea.readShort();
                    final short ypos = lea.readShort();
                    final short xwobble = lea.readShort();
                    final short ywobble = lea.readShort();
                    final byte newstate = lea.readByte();
                    final short duration = lea.readShort();
                    final BounceMovement bm = new BounceMovement(command, new Point(xpos, ypos), duration, newstate, 0);
                    bm.setPixelsPerSecond(new Point(xwobble, ywobble));
                    res.add(bm);
                    break;
                }

                case 9: {
                    res.add(new ChangeEquipSpecialAwesome(command, lea.readByte()));
                    break;
                }
                default:
                    final byte newstate = lea.readByte();
                    final short unk = lea.readShort();
                    final AranMovement am = new AranMovement(command, new Point(0, 0), unk, newstate, unk);
                    res.add(am);
                    break;
                //FileoutputUtil.log(FileoutputUtil.Movement_Log, "Kind movement: " + kind + ", Remaining : " + (numCommands - res.size()) + " New type of movement ID : " + command + ", packet : " + lea.toString(true));
                // return null;
            }
        }

        if (numCommands != res.size()) {
            System.out.println("사이즈가 불일치합니다. numCommands : " + numCommands + " kind : " + kind);
            return null; // Probably hack
        }
        return res;
    }

    public static final void updatePosition(final List<LifeMovementFragment> movement, final AnimatedMapleMapObject target, final int yoffset) {
        if (movement == null) {
            return;
        }
        for (final LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    final Point position = ((LifeMovement) move).getPosition();
                    position.y += yoffset;
                    target.setPosition(position);
                }
                target.setStance(((LifeMovement) move).getNewstate());
            }
        }
    }
}
