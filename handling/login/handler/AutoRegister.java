package handling.login.handler;

import client.LoginCrypto;
import client.LoginCryptoLegacy;
import client.MapleClient;
import database.DatabaseConnection;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import tools.MaplePacketCreator;
import tools.packet.LoginPacket;

public class AutoRegister {

    public static final int ACCOUNTS_IP_COUNT = 2;
    public static final boolean AutoRegister = true;

    private static final String FEMALE_PREFIX = "g_";
    private static final String MALE_PREFIX = "m_";
    private static final String PASSWORD_PREFIX = "pw_";
    private static final String TEMP_PASSWORD_PREFIX = "tp_";
    private static final int TEMP_PASSWORD_LEN = 6;
    private static final int MIN_PASSWORD_LEN = 4;
    private static final int MAX_PASSWORD_LEN = 12;
    private static final char[] TEMP_LOWER = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final char[] TEMP_DIGITS = "0123456789".toCharArray();
    private static final char[] TEMP_SYMBOLS = "!@#$%^&".toCharArray();
    private static final char[] TEMP_ALL = "abcdefghijklmnopqrstuvwxyz0123456789!@#$%^&".toCharArray();
    private static final SecureRandom TEMP_RANDOM = new SecureRandom();

    private static boolean isPrefixLogin(final String id, final String prefix) {
        return id != null && id.toLowerCase(Locale.ROOT).startsWith(prefix);
    }

    private static boolean isBlockedRegistrationId(final String id) {
        if (id == null) {
            return true;
        }
        final String lower = id.toLowerCase(Locale.ROOT);
        return lower.contains(FEMALE_PREFIX)
                || lower.contains(MALE_PREFIX)
                || lower.startsWith(PASSWORD_PREFIX) // includes pw__
                || lower.startsWith(TEMP_PASSWORD_PREFIX) // includes tp__
                || lower.contains("_"); // block any underscore in new account IDs
    }

    private static char randomFrom(final char[] source) {
        return source[TEMP_RANDOM.nextInt(source.length)];
    }

    private static String generateTemporaryPasswordCode() {
        final char[] token = new char[TEMP_PASSWORD_LEN];
        token[0] = randomFrom(TEMP_LOWER);
        token[1] = randomFrom(TEMP_DIGITS);
        token[2] = randomFrom(TEMP_SYMBOLS);
        for (int i = 3; i < TEMP_PASSWORD_LEN; i++) {
            token[i] = randomFrom(TEMP_ALL);
        }
        for (int i = token.length - 1; i > 0; i--) {
            final int j = TEMP_RANDOM.nextInt(i + 1);
            final char tmp = token[i];
            token[i] = token[j];
            token[j] = tmp;
        }
        return new String(token);
    }

    private static boolean isAllowedTempChar(final char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9')
                || "!@#$%^&".indexOf(c) >= 0;
    }

    private static boolean isValidTemporaryPasswordCode(final String code) {
        if (code == null || code.length() != TEMP_PASSWORD_LEN) {
            return false;
        }
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;
        for (int i = 0; i < code.length(); i++) {
            final char c = code.charAt(i);
            if (!isAllowedTempChar(c)) {
                return false;
            }
            if (c >= 'a' && c <= 'z') {
                hasLower = true;
            } else if (c >= '0' && c <= '9') {
                hasDigit = true;
            } else {
                hasSymbol = true;
            }
        }
        return hasLower && hasDigit && hasSymbol;
    }

    private static boolean isPasswordMatched(final String pwd, final String passhash, final String salt) {
        if (passhash == null || passhash.isEmpty()) {
            return false;
        }
        if (LoginCryptoLegacy.isLegacyPassword(passhash) && LoginCryptoLegacy.checkPassword(pwd, passhash)) {
            return true;
        }
        if (salt == null && LoginCrypto.checkSha1Hash(passhash, pwd)) {
            return true;
        }
        return LoginCrypto.checkSaltedSha512Hash(passhash, pwd, salt);
    }

    private static void sendPrefixLoginFailed(final MapleClient c) {
        c.clearInformation();
        c.getSession().write(LoginPacket.getLoginFailed(5));
    }

    private static void sendPrefixLoginNotice(final MapleClient c, final String message) {
        c.clearInformation();
        c.getSession().write(LoginPacket.getLoginFailed(20));
        c.getSession().write(MaplePacketCreator.serverNotice(1, message));
    }

    private static boolean handleGenderPrefixLogin(final String login, final String pwd, final MapleClient c,
            final String prefix, final int targetGender) {
        if (!isPrefixLogin(login, prefix)) {
            return false;
        }

        final String originalId = login.substring(prefix.length());
        if (originalId.isEmpty()) {
            sendPrefixLoginFailed(c);
            return true;
        }

        Connection con = null;
        PreparedStatement psSelect = null;
        PreparedStatement psUpdate = null;
        ResultSet rs = null;

        try {
            con = DatabaseConnection.getConnection();
            psSelect = con.prepareStatement("SELECT id, password, salt FROM accounts WHERE name = ?");
            psSelect.setString(1, originalId);
            rs = psSelect.executeQuery();

            if (!rs.next()) {
                sendPrefixLoginFailed(c);
                return true;
            }
            if (!isPasswordMatched(pwd, rs.getString("password"), rs.getString("salt"))) {
                sendPrefixLoginFailed(c);
                return true;
            }

            psUpdate = con.prepareStatement("UPDATE accounts SET gender = ? WHERE id = ?");
            psUpdate.setInt(1, targetGender);
            psUpdate.setInt(2, rs.getInt("id"));
            psUpdate.executeUpdate();

            sendPrefixLoginNotice(c, "계정의 성별이 변경되었습니다. 원래 아이디로 다시 로그인해 주세요.");
            return true;
        } catch (SQLException ex) {
            System.out.println(ex);
            sendPrefixLoginNotice(c, "Failed to process gender change request.");
            return true;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
            }
            try {
                if (psUpdate != null) {
                    psUpdate.close();
                }
            } catch (Exception e) {
            }
            try {
                if (psSelect != null) {
                    psSelect.close();
                }
            } catch (Exception e) {
            }
            try {
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
            }
        }
    }

    private static boolean handlePasswordPrefixLogin(final String login, final String pwd, final MapleClient c) {
        if (!isPrefixLogin(login, PASSWORD_PREFIX)) {
            return false;
        }

        // format: pw_<newPassword>_<originalId>
        final String payload = login.substring(PASSWORD_PREFIX.length());
        final int split = payload.indexOf('_');
        if (split <= 0 || split >= payload.length() - 1) {
            sendPrefixLoginFailed(c);
            return true;
        }

        final String newPassword = payload.substring(0, split);
        final String originalId = payload.substring(split + 1);
        if (newPassword.length() < MIN_PASSWORD_LEN || newPassword.length() > MAX_PASSWORD_LEN || originalId.isEmpty()) {
            sendPrefixLoginFailed(c);
            return true;
        }

        Connection con = null;
        PreparedStatement psSelect = null;
        PreparedStatement psUpdate = null;
        ResultSet rs = null;

        try {
            con = DatabaseConnection.getConnection();
            psSelect = con.prepareStatement("SELECT id, password, salt FROM accounts WHERE name = ?");
            psSelect.setString(1, originalId);
            rs = psSelect.executeQuery();

            if (!rs.next()) {
                sendPrefixLoginFailed(c);
                return true;
            }
            if (!isPasswordMatched(pwd, rs.getString("password"), rs.getString("salt"))) {
                sendPrefixLoginFailed(c);
                return true;
            }

            final String newSalt = LoginCrypto.makeSalt();
            final String newHash = LoginCrypto.makeSaltedSha512Hash(newPassword, newSalt);

            psUpdate = con.prepareStatement("UPDATE accounts SET password = ?, salt = ? WHERE id = ?");
            psUpdate.setString(1, newHash);
            psUpdate.setString(2, newSalt);
            psUpdate.setInt(3, rs.getInt("id"));
            psUpdate.executeUpdate();

            sendPrefixLoginNotice(c, "비밀번호가 변경되었습니다. 원래 아이디와 변경된 비밀번호로 다시 로그인해 주세요.");
            return true;
        } catch (SQLException ex) {
            System.out.println(ex);
            sendPrefixLoginNotice(c, "Failed to process password change request.");
            return true;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
            }
            try {
                if (psUpdate != null) {
                    psUpdate.close();
                }
            } catch (Exception e) {
            }
            try {
                if (psSelect != null) {
                    psSelect.close();
                }
            } catch (Exception e) {
            }
            try {
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
            }
        }
    }

    private static boolean handleTempPasswordPrefixLogin(final String login, final MapleClient c) {
        if (!isPrefixLogin(login, TEMP_PASSWORD_PREFIX)) {
            return false;
        }

        // format: tp_<temporaryPassword>_<originalId>
        final String payload = login.substring(TEMP_PASSWORD_PREFIX.length());
        final int split = payload.indexOf('_');
        if (split <= 0 || split >= payload.length() - 1) {
            sendPrefixLoginFailed(c);
            return true;
        }

        final String temporaryPassword = payload.substring(0, split);
        final String originalId = payload.substring(split + 1);
        if (!isValidTemporaryPasswordCode(temporaryPassword) || originalId.isEmpty()) {
            sendPrefixLoginFailed(c);
            return true;
        }

        Connection con = null;
        PreparedStatement psSelect = null;
        PreparedStatement psUpdate = null;
        ResultSet rs = null;

        try {
            con = DatabaseConnection.getConnection();
            psSelect = con.prepareStatement("SELECT id, phonenum FROM accounts WHERE name = ?");
            psSelect.setString(1, originalId);
            rs = psSelect.executeQuery();

            if (!rs.next()) {
                sendPrefixLoginFailed(c);
                return true;
            }

            final String currentTempCode = rs.getString("phonenum");
            if (currentTempCode == null || !temporaryPassword.equals(currentTempCode)) {
                sendPrefixLoginFailed(c);
                return true;
            }

            final String newSalt = LoginCrypto.makeSalt();
            final String newHash = LoginCrypto.makeSaltedSha512Hash(temporaryPassword, newSalt);
            final String nextTempCode = generateTemporaryPasswordCode();

            psUpdate = con.prepareStatement("UPDATE accounts SET password = ?, salt = ?, phonenum = ? WHERE id = ?");
            psUpdate.setString(1, newHash);
            psUpdate.setString(2, newSalt);
            psUpdate.setString(3, nextTempCode);
            psUpdate.setInt(4, rs.getInt("id"));
            psUpdate.executeUpdate();

            sendPrefixLoginNotice(c, "임시 비밀번호로 비밀번호가 변경되었습니다. 운영자에게 발급받은 임시 비밀번호로 로그인해 주세요.");
            return true;
        } catch (SQLException ex) {
            System.out.println(ex);
            sendPrefixLoginNotice(c, "임시 비밀번호 발급이 실패했습니다.");
            return true;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
            }
            try {
                if (psUpdate != null) {
                    psUpdate.close();
                }
            } catch (Exception e) {
            }
            try {
                if (psSelect != null) {
                    psSelect.close();
                }
            } catch (Exception e) {
            }
            try {
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public static boolean handleSpecialLogin(final String login, final String pwd, final MapleClient c) {
        if (handleTempPasswordPrefixLogin(login, c)) {
            return true;
        }
        if (handlePasswordPrefixLogin(login, pwd, c)) {
            return true;
        }
        if (handleGenderPrefixLogin(login, pwd, c, FEMALE_PREFIX, 1)) {
            return true;
        }
        if (handleGenderPrefixLogin(login, pwd, c, MALE_PREFIX, 0)) {
            return true;
        }
        return false;
    }

    public static boolean CheckAccount(String id) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT name FROM accounts WHERE name = ?");
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (Exception ex) {
            System.out.println(ex);
            return false;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
            }
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception e) {
            }
            try {
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
            }
        }
        return false;
    }

    public static void createAccount(String id, String pwd, String ip, final MapleClient c) {
        if (isBlockedRegistrationId(id)) {
            c.clearInformation();
            c.getSession().write(LoginPacket.getLoginFailed(20));
            c.getSession().write(MaplePacketCreator.serverNotice(1,
                    "회원가입할 아이디에는 _가 미포함되어야 합니다."));
            return;
        }

        Connection con = null;
        PreparedStatement ipc = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DatabaseConnection.getConnection();
            ipc = con.prepareStatement("SELECT COUNT(*) FROM accounts WHERE SessionIP = ?");
            ipc.setString(1, ip);
            rs = ipc.executeQuery();

            int ipAccountCount = 0;
            if (rs.next()) {
                ipAccountCount = rs.getInt(1);
            }

            if (ipAccountCount < ACCOUNTS_IP_COUNT) {
                ps = con.prepareStatement("INSERT INTO accounts (name, password, email, birthday, macs, SessionIP, gender, phonenum) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setString(1, id);
                ps.setString(2, LoginCryptoLegacy.hashPassword(pwd));
                ps.setString(3, "no@email.com");
                ps.setString(4, "2013-12-25");
                ps.setString(5, "00-00-00-00-00-00");
                ps.setString(6, ip);
                ps.setString(7, "0");
                ps.setString(8, generateTemporaryPasswordCode());
                ps.executeUpdate();

                c.clearInformation();
                c.getSession().write(LoginPacket.getLoginFailed(20));
                c.getSession().write(MaplePacketCreator.serverNotice(1, "회원가입이 완료되었습니다. 즐거운 게임되세요.\r\n\r\n여성으로 변경법 : ID 앞에 g_를 쓴 뒤 로그인 시도\r\n\r\n남성으로 변경법 : ID 앞에 m_를 쓴 뒤 로그인 시도\r\n\r\n비밀번호 변경법 : 아이디 앞에 pw_'변경할 비밀번호'_를 쓴 뒤 로그인 시도"));
            } else {
                c.clearInformation();
                c.getSession().write(LoginPacket.getLoginFailed(20));
                c.getSession().write(MaplePacketCreator.serverNotice(1, "회원가입 가능 횟수를 초과하셨습니다.\r\n\r\n여성으로 변경법 : ID 앞에 g_를 쓴 뒤 로그인 시도\r\n\r\n남성으로 변경법 : ID 앞에 m_를 쓴 뒤 로그인 시도\r\n\r\n비밀번호 변경법 : 아이디 앞에 pw_'변경할 비밀번호'_를 쓴 뒤\r\n로그인 시도"));
            }
        } catch (SQLException ex) {
            System.out.println(ex);
            c.clearInformation();
            c.getSession().write(LoginPacket.getLoginFailed(20));
            c.getSession().write(MaplePacketCreator.serverNotice(1, "회원가입에 실패하였습니다."));
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
            }
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception e) {
            }
            try {
                if (ipc != null) {
                    ipc.close();
                }
            } catch (Exception e) {
            }
            try {
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
            }
        }
    }
}
