package client;

import handling.channel.handler.AttackInfo;
import java.math.BigInteger;
import server.life.MapleMonster;
import tools.AttackPair;
import tools.Pair;

import java.util.ArrayList;
import java.util.List;
import server.MapleStatEffect;

/**
 * @author 이호영
 */
public class CalcDamage {

    CRand32 rndGenForCharacter;
    private int numRand = 7;

    public CalcDamage() {
        rndGenForCharacter = new CRand32();
    }

    public void SetSeed(int seed1, int seed2, int seed3) {
        rndGenForCharacter.Seed(seed1, seed2, seed3);
    }

    public long Random() {
        return rndGenForCharacter.Random();
    }

    public double RandomInRange(long randomNum, double max, double min) { //adjust_ramdom_damage
        if (min != max) {
            if (min > max) {
                double tmp = max;
                max = min;
                min = tmp;
            }
            return (max - min) * (randomNum % 0x989680) * 0.000000100000010000001 + min;
        } else {
            return max;
        }
    }

    public List<Pair<Integer, Boolean>> PDamage(MapleCharacter chr, AttackInfo attack) {
        List<Pair<Integer, Boolean>> realDamageList = new ArrayList<>();
        for (AttackPair eachMob : attack.allDamage) {//For each monster
            byte index = 0;
            boolean Miss = false;
            MapleMonster monster = chr.getMap().getMonsterByOid(eachMob.objectid);
            long rand[] = new long[numRand];//we need save it as long to store unsigned int
            for (int i = 0; i < numRand; i++) {
                rand[i] = rndGenForCharacter.Random();
            }
            double maxDamage = chr.getStat().getCurrentMaxBaseDamage();
            double minDamage = 9; //공사중
            for (Pair<Integer, Boolean> att : eachMob.attack) {//For each attack
                double realDamage = 0.0;
                boolean critical = false;
                //index++;
                long unkRand1 = rand[index++ % numRand];

                //Adjusted Random Damage 
                double adjustedRandomDamage = RandomInRange(rand[index++ % numRand], maxDamage, minDamage);
                realDamage += adjustedRandomDamage;

                //Adjusted Damage By Monster's Physical Defense Rate
                chr.dropMessage(6, "monsterPDRate" + monster.getStats().getPDRate());
                chr.dropMessage(6, "monsterPDDamage" + monster.getStats().getPDDamage());
                byte monsterPDRate = monster.getStats().getPDRate();
                double percentDmgAfterPDRate = Math.max(0.0, 100.0 - monsterPDRate);
                realDamage = percentDmgAfterPDRate / 100.0 * realDamage;
                chr.dropMessage(5, "스공 : [" + minDamage + " ~ " + maxDamage + "]");
                //index++;
                //Adjusted Damage By Skill
                MapleStatEffect skillEffect = null;
                if (attack.skill > 0) {
                    skillEffect = SkillFactory.getSkill(attack.skill).getEffect(chr.getTotalSkillLevel(attack.skill));
                }
                if (skillEffect != null) {
                    realDamage = realDamage * (double) skillEffect.getDamage() / 100.0;
                }
                //index++;
                // Critical Attack
                double value = RandomInRange(rand[index++ % numRand], 100, 0);///109기준 00784713
                if (value < chr.getStat().getSharpEyeRate()) {
                    critical = true;
                    int maxCritDamage = chr.getStat().getSharpEyeDam();
                    int minCritDamage = chr.getStat().getSharpEyeDam();
                    //index++;
                    int criticalDamageRate = 120;//(int) RandomInRange(rand[index++ % numRand], maxCritDamage, minCritDamage);
                    realDamage = realDamage + (criticalDamageRate / 100.0 * (int) realDamage);
                    att.right = true;
                }

                realDamageList.add(new Pair<>((int) realDamage * (Miss ? 0 : 1), critical));
                chr.dropMessage(5, "=====================================");
                chr.dropMessage(5, "DAMAGE ON CLIENT :\t" + att.left);
                chr.dropMessage(5, "RandomInRange" + value+ "critical:\t" + critical);
                chr.dropMessage(5, "value :\t" +value);
                chr.dropMessage((critical ? 6 : (Miss ? 2 : 5)), "DAMAGE ON SERVER\t : \t" + (int) realDamage * (Miss ? 0 : 1));
                chr.dropMessage(5, "getSharpEyeRate :\t" + chr.getStat().getSharpEyeRate());
                chr.dropMessage(5, "=====================================");
            }
        }
        return realDamageList;
    }
}
