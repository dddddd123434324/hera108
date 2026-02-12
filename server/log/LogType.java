/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.log;

/**
 * @author 티썬
 */
public class LogType {

    public static enum Chat {

        General(0), Buddy(1), Party(2), Guild(3), Messenger(4), Trade(5), PlayerShop(6), HiredMerchant(7), MiniGame(8), Megaphone(9), SuperMegaphone(10), MessageBox(11), Weather(12), Pet(13), Note(14), Whisper(15), TripleMegaphone(16), ItemMegaphone(17), Alliance(18);
        public final int i;

        private Chat(int i) {
            this.i = i;
        }
    }

    public static enum Item {

        FromScript(0), Sell(1), Quest(2), Incubator(3), ItemMaker(4), DailyQuestBonus(5), Stroage(6);
        public final int i;

        private Item(int i) {
            this.i = i;
        }
    }

    public static enum Trade {

        Trade(0), PlayerShop(1), HiredMerchant(2), Duey(3), CashShopGift(4), DropAndPick(5), WeddingPresent(6), Coupon(7);
        public final int i;

        private Trade(int i) {
            this.i = i;
        }
    }

    public static enum Hack {

        Macro(0);
        public final int i;

        private Hack(int i) {
            this.i = i;
        }
    }

    public static enum Etc {

        Party(0), LevelUp(1);
        public final int i;

        private Etc(int i) {
            this.i = i;
        }
    }

}
