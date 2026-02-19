package server;

import database.DatabaseOption;
import tools.FileoutputUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseBackupManager {

    private static final DatabaseBackupManager instance = new DatabaseBackupManager();
    private static final long ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L;
    private static final SimpleDateFormat BACKUP_FILE_TIME_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static final String BACKUP_LOG_FILE = "log/Log_DBBackup.txt";
    private static final String MYSQL_JDBC_PREFIX = "jdbc:mysql://";

    private final AtomicBoolean schedulerStarted = new AtomicBoolean(false);
    private final AtomicBoolean backupInProgress = new AtomicBoolean(false);

    public static DatabaseBackupManager getInstance() {
        return instance;
    }

    private DatabaseBackupManager() {
    }

    public static final class BackupResult {

        private final boolean success;
        private final String message;
        private final String backupFilePath;

        private BackupResult(boolean success, String message, String backupFilePath) {
            this.success = success;
            this.message = message;
            this.backupFilePath = backupFilePath;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getBackupFilePath() {
            return backupFilePath;
        }
    }

    private static final class ParsedDatabaseInfo {

        private final String host;
        private final int port;
        private final String databaseName;
        private final String dumpCharset;

        private ParsedDatabaseInfo(String host, int port, String databaseName, String dumpCharset) {
            this.host = host;
            this.port = port;
            this.databaseName = databaseName;
            this.dumpCharset = dumpCharset;
        }
    }

    public void startDailyBackupScheduler() {
        if (!schedulerStarted.compareAndSet(false, true)) {
            return;
        }

        final long initialDelay = getInitialDelayMillisAt0005();
        Timer.WorldTimer.getInstance().register(this::runScheduledBackup, ONE_DAY_MILLIS, initialDelay);
        System.out.println("[DatabaseBackupManager] Daily DB backup scheduler enabled (00:05).");
    }

    private void runScheduledBackup() {
        GeneralThreadPool.getInstance().execute(() -> {
            final BackupResult result = backupNow("AUTO_00:05");
            if (!result.isSuccess()) {
                System.err.println("[DatabaseBackupManager] Auto backup failed: " + result.getMessage());
            }
        });
    }

    public BackupResult backupNow(final String trigger) {
        if (!backupInProgress.compareAndSet(false, true)) {
            return new BackupResult(false, "[DB백업] 이미 다른 백업이 진행 중입니다.", null);
        }

        try {
            return runBackupInternal(trigger == null ? "MANUAL" : trigger);
        } finally {
            backupInProgress.set(false);
        }
    }

    private BackupResult runBackupInternal(final String trigger) {
        final File backupDir = resolveBackupDirectory();
        if (backupDir == null) {
            final String msg = "[DB백업] dbbackup 폴더를 찾거나 생성하지 못했습니다.";
            logResult(false, trigger, msg);
            return new BackupResult(false, msg, null);
        }

        if (!backupDir.exists() && !backupDir.mkdirs()) {
            final String msg = "[DB백업] 백업 폴더 생성 실패: " + backupDir.getPath();
            logResult(false, trigger, msg);
            return new BackupResult(false, msg, null);
        }

        final ParsedDatabaseInfo dbInfo;
        try {
            dbInfo = parseDatabaseInfo(DatabaseOption.MySQLURL);
        } catch (IllegalArgumentException ex) {
            final String msg = "[DB백업] DB URL 파싱 실패: " + ex.getMessage();
            logResult(false, trigger, msg);
            return new BackupResult(false, msg, null);
        }

        final String timestamp = BACKUP_FILE_TIME_FORMAT.format(new Date());
        final String fileName = sanitizeFileNamePart(dbInfo.databaseName) + "_" + timestamp + ".sql";
        final File backupFile = new File(backupDir, fileName);

        final List<String> dumpExecutables = resolveDumpExecutables();
        for (String executable : dumpExecutables) {
            final BackupResult result;
            try {
                result = executeDumpCommand(executable, dbInfo, backupFile, trigger);
            } catch (IOException ioException) {
                continue;
            }
            logResult(result.isSuccess(), trigger, result.getMessage());
            return result;
        }

        final String msg = "[DB백업] mysqldump 실행 파일을 찾지 못했습니다. "
                + "server.properties에 mysqldump.path를 설정하거나 PATH를 확인해 주세요.";
        logResult(false, trigger, msg);
        return new BackupResult(false, msg, null);
    }

    private BackupResult executeDumpCommand(final String executable, final ParsedDatabaseInfo dbInfo,
                                            final File backupFile, final String trigger) throws IOException {
        final List<String> command = new ArrayList<String>();
        command.add(executable);
        command.add("--host=" + dbInfo.host);
        command.add("--port=" + dbInfo.port);
        command.add("--user=" + DatabaseOption.MySQLUSER);

        if (DatabaseOption.MySQLPASS != null && !DatabaseOption.MySQLPASS.isEmpty()) {
            command.add("--password=" + DatabaseOption.MySQLPASS);
        }

        command.add("--single-transaction");
        command.add("--quick");
        command.add("--skip-lock-tables");
        command.add("--default-character-set=" + dbInfo.dumpCharset);
        command.add("--databases");
        command.add(dbInfo.databaseName);
        command.add("--result-file=" + backupFile.getAbsolutePath());

        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        final Process process = processBuilder.start();
        final String output = readProcessOutput(process);
        final int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            final String msg = "[DB백업] 백업이 중단되었습니다(인터럽트).";
            return new BackupResult(false, msg, null);
        }

        if (exitCode != 0) {
            final String details = trimForMessage(output);
            final String msg = "[DB백업] mysqldump 실패(exit=" + exitCode + ")"
                    + (details.isEmpty() ? "" : " - " + details);
            return new BackupResult(false, msg, null);
        }

        if (!backupFile.exists() || backupFile.length() <= 0L) {
            final String msg = "[DB백업] 백업 파일이 생성되지 않았습니다: " + backupFile.getPath();
            return new BackupResult(false, msg, null);
        }

        final String msg = "[DB백업] 완료(" + trigger + "): " + backupFile.getPath();
        return new BackupResult(true, msg, backupFile.getPath());
    }

    private List<String> resolveDumpExecutables() {
        final List<String> executables = new ArrayList<String>();

        final String configuredPath = ServerProperties.getProperty("mysqldump.path", "");
        if (configuredPath != null && !configuredPath.trim().isEmpty()) {
            executables.add(configuredPath.trim());
            return executables;
        }

        executables.add("mysqldump");
        executables.add("mysqldump.exe");
        return executables;
    }

    private File resolveBackupDirectory() {
        final File direct = new File("dbbackup");
        if (direct.exists() && direct.isDirectory()) {
            return direct;
        }

        final File parent = new File("..", "dbbackup");
        if (parent.exists() && parent.isDirectory()) {
            return parent;
        }

        if ((direct.mkdirs() || direct.exists()) && direct.isDirectory()) {
            return direct;
        }
        if ((parent.mkdirs() || parent.exists()) && parent.isDirectory()) {
            return parent;
        }
        return null;
    }

    private ParsedDatabaseInfo parseDatabaseInfo(final String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith(MYSQL_JDBC_PREFIX)) {
            throw new IllegalArgumentException("지원하지 않는 JDBC URL 형식입니다: " + jdbcUrl);
        }

        String body = jdbcUrl.substring(MYSQL_JDBC_PREFIX.length());
        String query = "";
        final int queryIndex = body.indexOf('?');
        if (queryIndex >= 0) {
            query = body.substring(queryIndex + 1);
            body = body.substring(0, queryIndex);
        }

        final int slashIndex = body.indexOf('/');
        if (slashIndex <= 0 || slashIndex >= body.length() - 1) {
            throw new IllegalArgumentException("DB 이름을 확인할 수 없습니다: " + jdbcUrl);
        }

        final String hostPort = body.substring(0, slashIndex);
        String databaseName = body.substring(slashIndex + 1);
        final int nextSlash = databaseName.indexOf('/');
        if (nextSlash >= 0) {
            databaseName = databaseName.substring(0, nextSlash);
        }
        if (databaseName.isEmpty()) {
            throw new IllegalArgumentException("DB 이름이 비어 있습니다.");
        }

        String host = hostPort;
        int port = 3306;
        final int colonIndex = hostPort.lastIndexOf(':');
        if (colonIndex > 0 && colonIndex < hostPort.length() - 1 && hostPort.indexOf(':') == colonIndex) {
            host = hostPort.substring(0, colonIndex);
            try {
                port = Integer.parseInt(hostPort.substring(colonIndex + 1));
            } catch (NumberFormatException ignore) {
                port = 3306;
            }
        }

        if (host.isEmpty()) {
            host = "localhost";
        }

        final String dumpCharset = resolveDumpCharset(query);
        return new ParsedDatabaseInfo(host, port, databaseName, dumpCharset);
    }

    private static String resolveDumpCharset(final String query) {
        if (query != null && !query.isEmpty()) {
            final String[] params = query.split("&");
            for (String param : params) {
                final int eqIndex = param.indexOf('=');
                if (eqIndex <= 0 || eqIndex >= param.length() - 1) {
                    continue;
                }
                final String key = param.substring(0, eqIndex);
                if (!"characterEncoding".equalsIgnoreCase(key)) {
                    continue;
                }
                final String value = param.substring(eqIndex + 1);
                final String normalized = value.replaceAll("[^a-zA-Z0-9_-]", "");
                if (!normalized.isEmpty()) {
                    return normalized;
                }
            }
        }
        return "utf8";
    }

    private static String readProcessOutput(final Process process) {
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
        } catch (IOException ignore) {
        }
        return sb.toString();
    }

    private static String trimForMessage(final String text) {
        if (text == null) {
            return "";
        }
        final String trimmed = text.trim().replace('\r', ' ').replace('\n', ' ');
        if (trimmed.length() <= 200) {
            return trimmed;
        }
        return trimmed.substring(0, 200) + "...";
    }

    private static String sanitizeFileNamePart(String text) {
        if (text == null || text.isEmpty()) {
            return "database";
        }
        return text.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static long getInitialDelayMillisAt0005() {
        final Calendar now = Calendar.getInstance();
        final Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 5);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_MONTH, 1);
        }
        return next.getTimeInMillis() - now.getTimeInMillis();
    }

    private void logResult(boolean success, String trigger, String message) {
        final String status = success ? "SUCCESS" : "FAIL";
        final String logMessage = "[" + status + "][" + trigger + "] " + message;
        FileoutputUtil.log(BACKUP_LOG_FILE, logMessage);
    }
}
