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
package client;

import server.Randomizer;
import tools.data.MaplePacketLittleEndianWriter;

public class CRand32 {

    public transient long m_s1, m_past_s1, m_s2, m_past_s2, m_s3, m_past_s3;
    private transient long seed1, seed2, seed3;
    private transient long seed1_, seed2_, seed3_;
    private transient long seed1__, seed2__, seed3__;

    private rndGenForCharacter rndGenForCharacter;
    private rndForCheckDamageMiss rndForCheckDamageMiss;
    private rndGenForMob rndGenForMob;

    public CRand32() {
        final int v4 = 5;
        this.Seed(Randomizer.nextInt(), 1170746341 * v4 - 755606699, 1170746341 * v4 - 755606699);
    }

    public final void Seed(final long s1, final long s2, final long s3) {
        m_s1 = s1 | 0x100000;
        m_past_s1 = s1 | 100000;
        m_s2 = s2 | 0x1000;
        m_past_s2 = s2 | 0x1000;
        m_s3 = s3 | 0x10;
        m_past_s3 = s3 | 0x10;
        rndGenForCharacter = new rndGenForCharacter(m_s1, m_s2, m_s3);
        rndForCheckDamageMiss = new rndForCheckDamageMiss(m_s1, m_s2, m_s3);
        rndGenForMob = new rndGenForMob(m_s1, m_s2, m_s3);
    }

    class rndGenForCharacter {

        private long m_s1, m_s2, m_s3;

        public rndGenForCharacter(final long s1, final long s2, final long s3) {
            this.m_s1 = s1;
            this.m_s2 = s2;
            this.m_s3 = s3;
        }

        public final long Random() {
            long v4 = this.m_s1;
            long v5 = this.m_s2;
            long v6 = this.m_s3;
            long v7 = this.m_s1;

            long v8 = ((v4 & 0xFFFFFFFE) << 12) ^ ((v7 & 0x7FFC0 ^ (v4 >> 13)) >> 6);
            long v9 = 16 * (v5 & 0xFFFFFFF8) ^ (((v5 >> 2) ^ v5 & 0x3F800000) >> 23);
            long v10 = ((v6 & 0xFFFFFFF0) << 17) ^ (((v6 >> 3) ^ v6 & 0x1FFFFF00) >> 8);
            this.m_s3 = v10 & 0xffffffffL;
            this.m_s1 = v8 & 0xffffffffL;
            this.m_s2 = v9 & 0xffffffffL;
            return (v8 ^ v9 ^ v10) & 0xffffffffL; // to be confirmed, I am not experienced in converting signed > unsigned
        }
    }

    public rndGenForCharacter rndGenForCharacter() {
        return rndGenForCharacter;
    }

    class rndForCheckDamageMiss {

        private long m_s1, m_s2, m_s3;

        public rndForCheckDamageMiss(final long s1, final long s2, final long s3) {
            this.m_s1 = s1;
            this.m_s2 = s2;
            this.m_s3 = s3;
        }

        public final long Random() {
            long v4 = this.m_s1;
            long v5 = this.m_s2;
            long v6 = this.m_s3;
            long v7 = this.m_s1;

            long v8 = ((v4 & 0xFFFFFFFE) << 12) ^ ((v7 & 0x7FFC0 ^ (v4 >> 13)) >> 6);
            long v9 = 16 * (v5 & 0xFFFFFFF8) ^ (((v5 >> 2) ^ v5 & 0x3F800000) >> 23);
            long v10 = ((v6 & 0xFFFFFFF0) << 17) ^ (((v6 >> 3) ^ v6 & 0x1FFFFF00) >> 8);
            this.m_s3 = v10 & 0xffffffffL;
            this.m_s1 = v8 & 0xffffffffL;
            this.m_s2 = v9 & 0xffffffffL;
            return (v8 ^ v9 ^ v10) & 0xffffffffL; // to be confirmed, I am not experienced in converting signed > unsigned
        }
    }

    public rndForCheckDamageMiss rndForCheckDamageMiss() {
        return rndForCheckDamageMiss;
    }

    class rndGenForMob {

        private long m_s1, m_s2, m_s3;

        public rndGenForMob(final long s1, final long s2, final long s3) {
            this.m_s1 = s1;
            this.m_s2 = s2;
            this.m_s3 = s3;
        }

        public final long Random() {
            long v4 = this.m_s1;
            long v5 = this.m_s2;
            long v6 = this.m_s3;
            long v7 = this.m_s1;

            long v8 = ((v4 & 0xFFFFFFFE) << 12) ^ ((v7 & 0x7FFC0 ^ (v4 >> 13)) >> 6);
            long v9 = 16 * (v5 & 0xFFFFFFF8) ^ (((v5 >> 2) ^ v5 & 0x3F800000) >> 23);
            long v10 = ((v6 & 0xFFFFFFF0) << 17) ^ (((v6 >> 3) ^ v6 & 0x1FFFFF00) >> 8);
            this.m_s3 = v10 & 0xffffffffL;
            this.m_s1 = v8 & 0xffffffffL;
            this.m_s2 = v9 & 0xffffffffL;
            return (v8 ^ v9 ^ v10) & 0xffffffffL; // to be confirmed, I am not experienced in converting signed > unsigned
        }
    }

    public final long Random() {
        long v4 = m_s1;
        long v5 = m_s2;
        long v6 = m_s3;
        long v7 = m_s1;

        long v8 = ((v4 & 0xFFFFFFFE) << 12) ^ ((v7 & 0x7FFC0 ^ (v4 >> 13)) >> 6);
        long v9 = 16 * (v5 & 0xFFFFFFF8) ^ (((v5 >> 2) ^ v5 & 0x3F800000) >> 23);
        long v10 = ((v6 & 0xFFFFFFF0) << 17) ^ (((v6 >> 3) ^ v6 & 0x1FFFFF00) >> 8);
        this.m_s3 = v10 & 0xffffffffL;
        this.m_s1 = v8 & 0xffffffffL;
        this.m_s2 = v9 & 0xffffffffL;
        return (v8 ^ v9 ^ v10) & 0xffffffffL; // to be confirmed, I am not experienced in converting signed > unsigned
    }

    public rndGenForMob rndGenForMob() {
        return rndGenForMob;
    }

    public final void connectData(final MaplePacketLittleEndianWriter mplew) {
        long v5 = Random();
        long s2 = Random();
        long v6 = Random();
        Seed(v5, s2, v6);

        mplew.writeInt(v5);
        mplew.writeInt(s2);
        mplew.writeInt(v6);
    }
}
