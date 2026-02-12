/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.movement;

import tools.data.MaplePacketLittleEndianWriter;

import java.awt.*;

/**
 * @author 티썬
 */
public class NoneMovement extends AbstractLifeMovement {

    public NoneMovement(int i, Point p, int i2, int i3, int i4) {
        super(i, p, i2, i3, i4);
    }

    @Override
    public void serialize(MaplePacketLittleEndianWriter lew) {
        lew.write(getType());
        lew.write(getNewstate());
    }

}
