package com.financetracker.factory;

import com.financetracker.model.Category;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Factory Pattern.
 * Bertanggung jawab untuk membuat instance Transaction baru
 * dengan ID unik.
 */
public class TransactionFactory {

    /**
     * Membuat objek Transaction baru.
     *
     * @param date        Tanggal transaksi
     * @param description Deskripsi transaksi
     * @param amount      Jumlah uang
     * @param type        Tipe (INCOME/EXPENSE)
     * @param category    Kategori transaksi
     * @return Objek Transaction yang baru dibuat
     */
    public static Transaction createTransaction(LocalDate date, String description, double amount, TransactionType type, Category category) {
        if (date == null || description == null || description.trim().isEmpty() || amount <= 0 || type == null || category == null) {
            throw new IllegalArgumentException("Input untuk transaksi tidak valid.");
        }

        String id = UUID.randomUUID().toString();
        return new Transaction(id, date, description, amount, type, category);
    }
}