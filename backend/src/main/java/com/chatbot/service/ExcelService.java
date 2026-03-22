package com.chatbot.service;

import com.chatbot.model.Chat;
import com.chatbot.model.Transaction;
import com.chatbot.model.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ExcelService — the core data layer.
 * All data is persisted to/read from .xlsx files in the /data folder.
 *
 * Uses ReentrantReadWriteLock per file to prevent concurrent write corruption.
 * Supports: read all rows, add row, update existing row.
 */
@Service
public class ExcelService {

    // ---- Config paths from application.properties ----
    @Value("${excel.users.path}")
    private String usersPath;

    @Value("${excel.transactions.path}")
    private String transactionsPath;

    @Value("${excel.chats.path}")
    private String chatsPath;

    // One lock per Excel file to handle concurrent access
    private final ReentrantReadWriteLock usersLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock transactionsLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock chatsLock = new ReentrantReadWriteLock();

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ---- Column indexes for users.xlsx ----
    private static final int U_ID = 0, U_USERNAME = 1, U_EMAIL = 2,
            U_PASSWORD = 3, U_TOKENS = 4, U_LAST_RESET = 5;

    // ---- Column indexes for transactions.xlsx ----
    private static final int T_ID = 0, T_USER_ID = 1, T_AMOUNT = 2,
            T_TOKENS_ADDED = 3, T_STATUS = 4, T_TIMESTAMP = 5, T_ORDER_ID = 6;

    // ---- Column indexes for chats.xlsx ----
    private static final int C_ID = 0, C_USER_ID = 1, C_MODE = 2,
            C_MESSAGE = 3, C_ROLE = 4, C_TIMESTAMP = 5;

    /**
     * Called at startup — creates Excel files with headers if they don't exist.
     */
    @PostConstruct
    public void initExcelFiles() {
        createIfNotExists(usersPath, new String[]{"id", "username", "email", "password", "tokens", "last_reset_time"});
        createIfNotExists(transactionsPath, new String[]{"id", "user_id", "amount", "tokens_added", "status", "timestamp", "razorpay_order_id"});
        createIfNotExists(chatsPath, new String[]{"id", "user_id", "mode", "message", "role", "timestamp"});
    }

    // =========================================================
    // GENERIC UTILITIES
    // =========================================================

    /**
     * Creates an Excel file with a header row if the file doesn't already exist.
     */
    private void createIfNotExists(String filePath, String[] headers) {
        File file = new File(filePath);
        if (file.exists()) return;

        // Ensure parent directories exist
        file.getParentFile().mkdirs();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create Excel file: " + filePath, e);
        }
    }

    /**
     * Reads a workbook from disk.
     */
    private Workbook readWorkbook(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            return new XSSFWorkbook(fis);
        }
    }

    /**
     * Writes the workbook back to disk, overwriting the file.
     */
    private void writeWorkbook(Workbook workbook, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
    }

    /**
     * Helper to safely read a String cell value (handles null and numeric cells).
     */
    private String getCellString(Row row, int colIndex) {
        if (row == null) return "";
        Cell cell = row.getCell(colIndex);
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }

    /**
     * Helper to safely read a numeric cell value.
     */
    private double getCellDouble(Row row, int colIndex) {
        if (row == null) return 0;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        if (cell.getCellType() == CellType.STRING) {
            try { return Double.parseDouble(cell.getStringCellValue()); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    /**
     * Helper to parse a LocalDateTime from a cell.
     */
    private LocalDateTime getCellDateTime(Row row, int colIndex) {
        String val = getCellString(row, colIndex);
        if (val == null || val.isEmpty()) return LocalDateTime.now();
        try { return LocalDateTime.parse(val, DT_FORMAT); } catch (Exception e) { return LocalDateTime.now(); }
    }

    // =========================================================
    // USER OPERATIONS
    // =========================================================

    /**
     * Returns all users from users.xlsx
     */
    public List<User> getAllUsers() {
        usersLock.readLock().lock();
        try {
            Workbook wb = readWorkbook(usersPath);
            Sheet sheet = wb.getSheet("Data");
            List<User> users = new ArrayList<>();
            // Row 0 is header — skip it
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || getCellString(row, U_ID).isEmpty()) continue;
                users.add(rowToUser(row));
            }
            wb.close();
            return users;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read users.xlsx", e);
        } finally {
            usersLock.readLock().unlock();
        }
    }

    /**
     * Find a user by username (case-insensitive)
     */
    public Optional<User> findByUsername(String username) {
        return getAllUsers().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    /**
     * Find a user by email (case-insensitive)
     */
    public Optional<User> findByEmail(String email) {
        return getAllUsers().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    /**
     * Find a user by ID
     */
    public Optional<User> findUserById(String id) {
        return getAllUsers().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    /**
     * Append a new user row to users.xlsx
     */
    public void saveUser(User user) {
        usersLock.writeLock().lock();
        try {
            Workbook wb = readWorkbook(usersPath);
            Sheet sheet = wb.getSheet("Data");
            int lastRow = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(lastRow);
            userToRow(user, row);
            writeWorkbook(wb, usersPath);
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save user", e);
        } finally {
            usersLock.writeLock().unlock();
        }
    }

    /**
     * Update an existing user row (matched by user.getId())
     */
    public void updateUser(User updatedUser) {
        usersLock.writeLock().lock();
        try {
            Workbook wb = readWorkbook(usersPath);
            Sheet sheet = wb.getSheet("Data");
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                if (getCellString(row, U_ID).equals(updatedUser.getId())) {
                    userToRow(updatedUser, row); // overwrite the row
                    break;
                }
            }
            writeWorkbook(wb, usersPath);
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to update user", e);
        } finally {
            usersLock.writeLock().unlock();
        }
    }

    // =========================================================
    // TRANSACTION OPERATIONS
    // =========================================================

    /**
     * Append a new transaction row to transactions.xlsx
     */
    public void saveTransaction(Transaction transaction) {
        transactionsLock.writeLock().lock();
        try {
            Workbook wb = readWorkbook(transactionsPath);
            Sheet sheet = wb.getSheet("Data");
            int lastRow = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(lastRow);
            transactionToRow(transaction, row);
            writeWorkbook(wb, transactionsPath);
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save transaction", e);
        } finally {
            transactionsLock.writeLock().unlock();
        }
    }

    /**
     * Update an existing transaction (matched by transaction ID)
     */
    public void updateTransaction(Transaction updated) {
        transactionsLock.writeLock().lock();
        try {
            Workbook wb = readWorkbook(transactionsPath);
            Sheet sheet = wb.getSheet("Data");
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                if (getCellString(row, T_ID).equals(updated.getId())) {
                    transactionToRow(updated, row);
                    break;
                }
            }
            writeWorkbook(wb, transactionsPath);
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to update transaction", e);
        } finally {
            transactionsLock.writeLock().unlock();
        }
    }

    /**
     * Find a transaction by Razorpay order ID
     */
    public Optional<Transaction> findTransactionByOrderId(String orderId) {
        transactionsLock.readLock().lock();
        try {
            Workbook wb = readWorkbook(transactionsPath);
            Sheet sheet = wb.getSheet("Data");
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                if (getCellString(row, T_ORDER_ID).equals(orderId)) {
                    Transaction t = rowToTransaction(row);
                    wb.close();
                    return Optional.of(t);
                }
            }
            wb.close();
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read transactions.xlsx", e);
        } finally {
            transactionsLock.readLock().unlock();
        }
    }

    // =========================================================
    // CHAT OPERATIONS
    // =========================================================

    /**
     * Append a new chat message row to chats.xlsx
     */
    public void saveChat(Chat chat) {
        chatsLock.writeLock().lock();
        try {
            Workbook wb = readWorkbook(chatsPath);
            Sheet sheet = wb.getSheet("Data");
            int lastRow = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(lastRow);
            chatToRow(chat, row);
            writeWorkbook(wb, chatsPath);
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save chat", e);
        } finally {
            chatsLock.writeLock().unlock();
        }
    }

    /**
     * Get all chat messages for a specific user, optionally filtered by mode.
     * Returns messages in chronological order.
     */
    public List<Chat> getChatsByUser(String userId, String mode) {
        chatsLock.readLock().lock();
        try {
            Workbook wb = readWorkbook(chatsPath);
            Sheet sheet = wb.getSheet("Data");
            List<Chat> chats = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || getCellString(row, C_ID).isEmpty()) continue;
                Chat chat = rowToChat(row);
                // Filter by userId; if mode is provided, also filter by mode
                if (chat.getUserId().equals(userId)) {
                    if (mode == null || mode.isEmpty() || chat.getMode().equalsIgnoreCase(mode)) {
                        chats.add(chat);
                    }
                }
            }
            wb.close();
            return chats;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read chats.xlsx", e);
        } finally {
            chatsLock.readLock().unlock();
        }
    }

    // =========================================================
    // ROW MAPPERS (POJO <-> Excel Row)
    // =========================================================

    private User rowToUser(Row row) {
        User u = new User();
        u.setId(getCellString(row, U_ID));
        u.setUsername(getCellString(row, U_USERNAME));
        u.setEmail(getCellString(row, U_EMAIL));
        u.setPassword(getCellString(row, U_PASSWORD));
        u.setTokens((int) getCellDouble(row, U_TOKENS));
        u.setLastResetTime(getCellDateTime(row, U_LAST_RESET));
        return u;
    }

    private void userToRow(User user, Row row) {
        row.createCell(U_ID).setCellValue(user.getId());
        row.createCell(U_USERNAME).setCellValue(user.getUsername());
        row.createCell(U_EMAIL).setCellValue(user.getEmail());
        row.createCell(U_PASSWORD).setCellValue(user.getPassword());
        row.createCell(U_TOKENS).setCellValue(user.getTokens());
        row.createCell(U_LAST_RESET).setCellValue(
                user.getLastResetTime() != null ? user.getLastResetTime().format(DT_FORMAT) : "");
    }

    private Transaction rowToTransaction(Row row) {
        Transaction t = new Transaction();
        t.setId(getCellString(row, T_ID));
        t.setUserId(getCellString(row, T_USER_ID));
        t.setAmount(getCellDouble(row, T_AMOUNT));
        t.setTokensAdded((int) getCellDouble(row, T_TOKENS_ADDED));
        t.setStatus(getCellString(row, T_STATUS));
        t.setTimestamp(getCellDateTime(row, T_TIMESTAMP));
        t.setRazorpayOrderId(getCellString(row, T_ORDER_ID));
        return t;
    }

    private void transactionToRow(Transaction t, Row row) {
        row.createCell(T_ID).setCellValue(t.getId());
        row.createCell(T_USER_ID).setCellValue(t.getUserId());
        row.createCell(T_AMOUNT).setCellValue(t.getAmount());
        row.createCell(T_TOKENS_ADDED).setCellValue(t.getTokensAdded());
        row.createCell(T_STATUS).setCellValue(t.getStatus());
        row.createCell(T_TIMESTAMP).setCellValue(
                t.getTimestamp() != null ? t.getTimestamp().format(DT_FORMAT) : "");
        row.createCell(T_ORDER_ID).setCellValue(t.getRazorpayOrderId() != null ? t.getRazorpayOrderId() : "");
    }

    private Chat rowToChat(Row row) {
        Chat c = new Chat();
        c.setId(getCellString(row, C_ID));
        c.setUserId(getCellString(row, C_USER_ID));
        c.setMode(getCellString(row, C_MODE));
        c.setMessage(getCellString(row, C_MESSAGE));
        c.setRole(getCellString(row, C_ROLE));
        c.setTimestamp(getCellDateTime(row, C_TIMESTAMP));
        return c;
    }

    private void chatToRow(Chat chat, Row row) {
        row.createCell(C_ID).setCellValue(chat.getId());
        row.createCell(C_USER_ID).setCellValue(chat.getUserId());
        row.createCell(C_MODE).setCellValue(chat.getMode());
        row.createCell(C_MESSAGE).setCellValue(chat.getMessage());
        row.createCell(C_ROLE).setCellValue(chat.getRole());
        row.createCell(C_TIMESTAMP).setCellValue(
                chat.getTimestamp() != null ? chat.getTimestamp().format(DT_FORMAT) : "");
    }
}

