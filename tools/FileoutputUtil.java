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
package tools;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FileoutputUtil {

    // Logging output file
    public static final String Acc_Stuck = "log/Log_AccountStuck.txt",
            PacketEx_Log = "log/Log_Packet_error.txt", // I cba looking for every error, adding this back in.
            CommandEx_Log = "log/Log_Command_Except.txt",
            ScriptEx_Log = "log/Log_Script_Except.txt",
            Hacker_Log = "log/Log_Hacker.txt",
            AntiCheat_Log = "log/Log_CRC.txt",
            LieDetector_log = "log/Log_LieDetector.txt",
            NULL = "";
    // End
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat sdfGMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat sdf_ = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");

    static {
        sdfGMT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    
    public static String getDCurrentTime() {
        Calendar calz = Calendar.getInstance(TimeZone.getTimeZone("KST"), Locale.KOREAN);
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("yyyy-MM-dd");
        String time = simpleTimeFormat.format(calz.getTime());
        return time;
    }

    public static void dbLog(final String file, final String msg) {
        String path = "specified_log/" + CurrentReadable_Date() + "/" + file + ".txt";
        FileOutputStream out = null;
        try {
            final File f = new File(path);
            f.getParentFile().mkdirs();
            out = new FileOutputStream(f, true);
            out.write(("\r\n[" + CurrentReadable_TimeOnly() + "] " + msg).getBytes(Charset.forName("UTF-8")));
        } catch (IOException ess) {
            ess.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    public static void log(final String file, final String msg) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file, true);
            out.write(("\r\n------------------------ " + CurrentReadable_Time() + " ------------------------\r\n").getBytes(Charset.forName("UTF-8")));
            out.write(msg.getBytes(Charset.forName("UTF-8")));
        } catch (IOException ess) {
            ess.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    public static void outputFileError(final String file, final Throwable t) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file, true);
            out.write(("\r\n------------------------ " + CurrentReadable_Time() + " ------------------------\r\n").getBytes());
            out.write(getString(t).getBytes());
        } catch (IOException ess) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    public static String CurrentReadable_Date() {
        return sdf_.format(Calendar.getInstance().getTime());
    }

    public static String CurrentReadable_Time() {
        return sdf.format(Calendar.getInstance().getTime());
    }

    public static String CurrentReadable_TimeOnly() {
        return sdfTime.format(Calendar.getInstance().getTime());
    }

    public static String CurrentReadable_TimeGMT() {
        return sdfGMT.format(new Date());
    }

    public static String getString(final Throwable e) {
        String retValue = null;
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            retValue = sw.toString();
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
                if (sw != null) {
                    sw.close();
                }
            } catch (IOException ignore) {
            }
        }
        return retValue;
    }
}
