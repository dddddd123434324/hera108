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
package client.inventory;

public enum ItemFlag {

    LOCK(0x01),
    SPIKES(0x02), //장비에 0x2가 붙으면!
    COLD(0x04),
    UNTRADEABLE(0x08), //-8이면 교환가능
    KARMA_EQ(0x10), //교불인 아이템만 교환가능하게 
    KARMA_USE(0x02), //장비가 아닌 소비나 기타템등에 교환가능이 붙으면!
    CHARM_EQUIPPED(0x20),
    DONNO(0x40),
    CRAFTED(0x80),
    CRAFTED_USE(0x10),
    SHIELD_WARD(0x100), //shield icon
    LUCKS_KEY(0x200), //this has some clover leaf thing at bottomleft
    KARMA_ACC_USE(0x400),
    KARMA_ACC(0x1000);
    private final int i;

    private ItemFlag(int i) {
        this.i = i;
    }

    public final int getValue() {
        return i;
    }

    public final boolean check(int flag) {
        return (flag & i) == i;
    }
}
