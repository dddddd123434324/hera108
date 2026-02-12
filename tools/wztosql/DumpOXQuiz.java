/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.wztosql;

import database.DatabaseConnection;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.ServerProperties;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author 티썬
 */
public class DumpOXQuiz {

    public static void main(String[] args) throws SQLException {
        MapleDataProvider pro = MapleDataProviderFactory.getDataProvider(new File(ServerProperties.WZ_PATH + "Etc.wz"));
        MapleData oxquiz = pro.getData("OXQuiz.img");
        DatabaseConnection.init();
        final Connection con = DatabaseConnection.getConnection();
        con.createStatement().executeUpdate("TRUNCATE `wz_oxdata`");
        PreparedStatement ps = con.prepareStatement("INSERT INTO wz_oxdata(`questionset`, `questionid`, `question`, `display`, `answer`) VALUES (?, ?, ?, ?, ?)");
        for (MapleData set : oxquiz) {
            ps.setInt(1, Integer.parseInt(set.getName()));
            for (MapleData quiz : set) {
                ps.setInt(2, Integer.parseInt(quiz.getName()));
                ps.setString(3, MapleDataTool.getString("q", quiz, ""));
                ps.setString(4, MapleDataTool.getString("d", quiz, ""));
                ps.setString(5, MapleDataTool.getInt("a", quiz, 0) == 1 ? "o" : "x");
                ps.addBatch();
            }
        }
        ps.executeBatch();
        ps.close();
    }
}
