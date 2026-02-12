package server;

public class StructBonusExp {

    public int incExpR;
    public int termStart;

    public boolean checkTerm(long now, long equipped) {
        int eHour = (int) ((now - equipped) / 60 / 60 / 1000);
        return eHour >= this.termStart;
    }
}
