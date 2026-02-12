package handling.login.handler;

import client.LoginCryptoLegacy;
import client.MapleClient;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tools.MaplePacketCreator;
import tools.packet.LoginPacket;

public class AutoRegister {
    public static final int ACCOUNTS_IP_COUNT = 5;
    public static int fm;
    public static final boolean AutoRegister = true;
    public static boolean CheckAccount(String id) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT name FROM accounts WHERE name = ?");
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.first()) {
                return true;
            }
            rs.close();
            ps.close();
            con.close();
        } catch (Exception ex) {
            System.out.println(ex);
            return false;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close(); 
                }
                if (con != null) {
                    con.close(); 
                }
            } catch (Exception e) {
            }
        }
        return false;
    }
    
    public static void createAccount(String id, String pwd, String ip, final MapleClient c) {
        Connection con = null;
        PreparedStatement ipc = null;
        PreparedStatement ps = null;
                
        ResultSet rs = null;
        
        try {
            con = DatabaseConnection.getConnection();
            ipc = con.prepareStatement("SELECT SessionIP FROM accounts WHERE SessionIP = ?");
            ipc.setString(1, ip);
            rs = ipc.executeQuery();
            if (rs.first() == false || rs.last() == true && rs.getRow() < ACCOUNTS_IP_COUNT) {
                try {
                    ps = con.prepareStatement("INSERT INTO accounts (name, password, email, birthday, macs, SessionIP,gender) VALUES (?, ?, ?, ?, ?, ?, ?)");
                    fm = id.indexOf("f_");
                    if (fm == 0){
                        ps.setString(1, id);
                    	ps.setString(2, LoginCryptoLegacy.hashPassword(pwd));
                        ps.setString(3, "no@email.com");
                        ps.setString(4, "2013-12-25");
                        ps.setString(5, "00-00-00-00-00-00");
                        ps.setString(6, ip);
                        ps.setString(7, "1");
                        ps.executeUpdate();
                        rs.close();
                        c.clearInformation();
                        c.getSession().write(LoginPacket.getLoginFailed(20));
                        c.getSession().write(MaplePacketCreator.serverNotice(1, "회원가입이 완료되었습니다.\r\n헤라에 오신 것을 환영합니다.\r\n성별 : 여자"));
                    } else if (fm == -1){
                        ps.setString(1, id);
                    	ps.setString(2, LoginCryptoLegacy.hashPassword(pwd));
                        ps.setString(3, "no@email.com");
                        ps.setString(4, "2013-12-25");
                        ps.setString(5, "00-00-00-00-00-00");
                        ps.setString(6, ip);
                        ps.setString(7, "0");
                        ps.executeUpdate();
                        rs.close();
                        c.clearInformation();
                        c.getSession().write(LoginPacket.getLoginFailed(20));
                        c.getSession().write(MaplePacketCreator.serverNotice(1, "회원가입이 완료되었습니다.\r\n헤라에 오신 것을 환영합니다.\r\n성별 : 남자\r\n\r\n여자계정 생성하는법 :\r\n계정앞에 f_를 붙여주세요.\r\nex) f_admin"));
                    }
                } catch (SQLException ex) {
                    System.out.println(ex);
                }
            } else {
                c.clearInformation();
                c.getSession().write(LoginPacket.getLoginFailed(20));
                c.getSession().write(MaplePacketCreator.serverNotice(1, "회원가입 제한 횟수를 초과하였습니다.\r\n계정생성이 불가능합니다."));
            }
            rs.close();
            ipc.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            System.out.println(ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (ipc != null) {
                    ipc.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
            }
        }
    }
}