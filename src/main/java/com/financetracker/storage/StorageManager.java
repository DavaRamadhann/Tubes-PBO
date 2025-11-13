package com.financetracker.storage;

import com.financetracker.model.Transaction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton Pattern.
 * Mengelola penyimpanan dan pemuatan data (transactions.json)
 * dan logging notifikasi (notifications.log).
 */
public class StorageManager {

    private static volatile StorageManager instance;
    private final Gson gson;
    private static final String TRANSACTIONS_FILE = "data/transactions.json";
    private static final String NOTIFICATIONS_FILE = "data/notifications.log";

    /**
     * Private constructor untuk Singleton.
     * Menginisialisasi Gson dengan adapter untuk LocalDate.
     */
    private StorageManager() {
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .setPrettyPrinting()
                .create();
    }

    /**
     * Mendapatkan instance tunggal dari StorageManager (Thread-safe).
     */
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

    /**
     * Menyimpan daftar transaksi ke file transactions.json.
     */
    public void saveTransactions(List<Transaction> transactions) {
        try (Writer writer = new FileWriter(TRANSACTIONS_FILE, StandardCharsets.UTF_8)) {
            gson.toJson(transactions, writer);
        } catch (IOException e) {
            System.err.println("Gagal menyimpan transaksi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Memuat daftar transaksi dari file transactions.json.
     */
    public List<Transaction> loadTransactions() {
        File file = new File(TRANSACTIONS_FILE);
        if (!file.exists() || file.length() == 0) {
            // Jika file tidak ada atau kosong, kembalikan list kosong
            // dan buat file kosong
            saveTransactions(new ArrayList<>());
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(TRANSACTIONS_FILE, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<Transaction>>() {}.getType();
            List<Transaction> transactions = gson.fromJson(reader, listType);
            return (transactions != null) ? transactions : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Gagal memuat transaksi: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Mencatat pesan notifikasi ke notifications.log.
     */
    public void logNotification(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String logEntry = String.format("[%s] %s%n", timestamp, message);

        try {
            Files.write(Paths.get(NOTIFICATIONS_FILE), logEntry.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Gagal menulis log notifikasi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}