package com.financetracker.storage;

import com.financetracker.model.Transaction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * StorageManager — versi final, aman, lengkap.
 * - Auto create folder / file
 * - Menyimpan transaksi
 * - Menyimpan log
 * - Persistensi budget (save/load)
 * - Aman dari error Path
 */
public class StorageManager {

    private static volatile StorageManager instance;
    private final Gson gson;

    // Directories & files
    private static final String DATA_DIR = "data";
    private static final String TRANSACTIONS_FILE = DATA_DIR + "/transactions.json";
    private static final String NOTIFICATIONS_FILE = DATA_DIR + "/notifications.log";
    private static final String BUDGET_FILE = DATA_DIR + "/budget.txt";

    private StorageManager() {
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .setPrettyPrinting()
                .create();

        ensureStorage();
    }

    /**
     * Pastikan folder dan file penting ada.
     */
    private void ensureStorage() {
        try {
            // Buat folder data jika tidak ada
            Files.createDirectories(Paths.get(DATA_DIR));

            // Jika file transaksi belum ada → buat file kosong
            if (!Files.exists(Paths.get(TRANSACTIONS_FILE))) {
                Files.write(Paths.get(TRANSACTIONS_FILE), "[]".getBytes(StandardCharsets.UTF_8));
            }

            // Jika file budget tidak ada → buat budget default
            if (!Files.exists(Paths.get(BUDGET_FILE))) {
                Files.write(Paths.get(BUDGET_FILE), "2000000".getBytes(StandardCharsets.UTF_8)); // default 2 jt
            }

            // Jika log tidak ada, buat kosong
            if (!Files.exists(Paths.get(NOTIFICATIONS_FILE))) {
                Files.write(Paths.get(NOTIFICATIONS_FILE),
                        "".getBytes(StandardCharsets.UTF_8));
            }

        } catch (IOException e) {
            System.err.println("Gagal memastikan direktori penyimpanan: " + e.getMessage());
        }
    }

    public static StorageManager getInstance() {
        if (instance == null) {
            synchronized (StorageManager.class) {
                if (instance == null) {
                    instance = new StorageManager();
                }
            }
        }
        return instance;
    }

    // ============================================================
    //                  TRANSACTION STORAGE
    // ============================================================

    public void saveTransactions(List<Transaction> transactions) {
        try (Writer writer = new FileWriter(TRANSACTIONS_FILE, StandardCharsets.UTF_8)) {
            gson.toJson(transactions, writer);
        } catch (IOException e) {
            System.err.println("Gagal menyimpan transaksi: " + e.getMessage());
        }
    }

    public List<Transaction> loadTransactions() {
        try (Reader reader = new FileReader(TRANSACTIONS_FILE, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<Transaction>>() {}.getType();
            List<Transaction> transactions = gson.fromJson(reader, listType);
            return (transactions != null) ? transactions : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Gagal memuat transaksi: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ============================================================
    //                  NOTIFICATION LOGGING
    // ============================================================

    public void logNotification(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String logEntry = String.format("[%s] %s%n", timestamp, message);

        try {
            Files.write(Paths.get(NOTIFICATIONS_FILE),
                    logEntry.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Gagal menulis log notifikasi: " + e.getMessage());
        }
    }

    // ============================================================
    //                  BUDGET PERSISTENCE
    // ============================================================

    public void saveMonthlyBudget(double amount) {
        try {
            Files.write(
                    Paths.get(BUDGET_FILE),
                    String.valueOf(amount).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception e) {
            System.err.println("Gagal menyimpan budget: " + e.getMessage());
        }
    }

    public Double loadMonthlyBudget() {
        try {
            String text = Files.readString(Paths.get(BUDGET_FILE));
            return Double.parseDouble(text.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
