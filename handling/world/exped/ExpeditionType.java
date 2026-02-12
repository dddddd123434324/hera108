package handling.world.exped;

public enum ExpeditionType {
    Easy_Balrog("이지발록 원정대", 6, 2000, 50, 70),
    Normal_Balrog("노말발록 원정대", 15, 2001, 50, 200),
    Zakum("자쿰 원정대", 30, 2002, 50, 200),
    Horntail("혼테일 원정대", 30, 2003, 80, 200),
    Pink_Bean("핑크빈 원정대", 30, 2004, 140, 200),
//    Chaos_Zakum(30, 2005, 100, 200),
//    ChaosHT(30, 2006, 110, 200),
//    CWKPQ(30, 2007, 90, 200),
//    Von_Leon(30, 2008, 120, 200),
//    Cygnus(18, 2009, 170, 200),
    ;

    private String name;
    public int maxMembers, maxParty, exped, minLevel, maxLevel, lastParty;

    private ExpeditionType(String name, int maxMembers, int exped, int minLevel, int maxLevel) {
        this.name = name;
        this.maxMembers = maxMembers;
        this.exped = exped;
        //this.maxParty = (maxMembers / 2) + (maxMembers % 2 > 0 ? 1 : 0);
        this.maxParty = (maxMembers / 6) + (maxMembers % 6 > 0 ? 1 : 0);
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.lastParty = maxMembers % 6;
    }

    public static ExpeditionType getById(int id) {
        for (ExpeditionType pst : ExpeditionType.values()) {
            if (pst.exped == id) {
                return pst;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return exped;
    }
}