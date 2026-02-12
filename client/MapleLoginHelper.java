/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author 티썬
 */
public class MapleLoginHelper {

    public static enum LoginResult {

        IS_BANNED,
        UNKNOWN_ERROR,
        NOT_REGISTERED_ACCOUNT,
        INVALID_PASSWORD,
        NOT_CONNECTED_ACCOUNT,
        SHOULD_UPDATE_PW,
        CANNOT_RENEW_PW,
        CAN_RENEW_PW,
        OK
    }

    public static int checkRenewPassword(Connection con, String login, String code) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT * FROM `redopassword` WHERE `name` = ?");
        ps.setString(1, login);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            //status

            if (rs.getString("authcode").equals(code)) {
                //0 : mail sended
                int status = rs.getInt("status");
                rs.close();
                ps.close();
                return status;
            } else {
                return -2;
            }
        }
        rs.close();
        ps.close();
        return -1;
    }

    public static void insertPasswordRenewDB(Connection con, String login, String email, String code) throws SQLException {
        PreparedStatement ps = con.prepareStatement("INSERT INTO `redopassword` (`name`, `email`, `authcode`, `status`) VALUES (?, ?, ?, ?)");
        ps.setString(1, login);
        ps.setString(2, email);
        ps.setString(3, code);
        ps.setInt(4, 0);
        ps.executeUpdate();
        ps.close();
    }

    public static void DeleteAndUpdatePasswordDB(Connection con, String login) throws SQLException {
        PreparedStatement ps = con.prepareStatement("DELETE FROM `redopassword` WHERE `name` = ?");
        ps.setString(1, login);
        ps.executeUpdate();
        ps.close();
    }
}
