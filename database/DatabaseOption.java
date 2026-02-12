/*
 * Copyright (C) 2013 Nemesis Maple Story Online Server Program

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package database;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * @author Eternal
 */
public class DatabaseOption {

    //
    public static String MySQLURL = "jdbc:mysql://localhost:3306/ocelitworld?useSSL=false&characterEncoding=euckr";
    public static String MySQLUSER = "root";
    public static String MySQLPASS = "root";

    static {
        Properties p = new Properties();
        try {
            Reader r;
            if (Files.exists(Paths.get("db.properties"))) {
                r = Files.newBufferedReader(Paths.get("db.properties"));
            } else {
                r = Files.newBufferedReader(Paths.get("../db.properties")); //Dump?
            }
            p.load(r);
            MySQLURL = p.getProperty("url", MySQLURL);
            MySQLUSER = p.getProperty("user", MySQLUSER);
            MySQLPASS = p.getProperty("pass", MySQLPASS);
        } catch (IOException ex) {
        }
    }

    //
    public static int MySQLMINCONNECTION = 10;
    public static int MySQLMAXCONNECTION = 5000;

}
