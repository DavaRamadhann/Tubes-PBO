package com.financetracker.service;

import com.financetracker.model.Category;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import com.financetracker.patterns.observer.BudgetObserver;
import com.financetracker.patterns.observer.BudgetSubject;
import com.financetracker.storage.StorageManager;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service utama yang mengelola logika bisnis transaksi
 * dan bertindak sebagai Subject untuk Observer Pattern (Budget).
 */
public class TransactionService implements BudgetSubject {

    private List<Transaction> transactions;
    private final StorageManager storageManager;
    private final List<BudgetObserver> observers;

    private double monthlyBudget = 0.0;
    private boolean budgetNotificationSent = false;

    public TransactionService() {
        this.storageManager = StorageManager.getInstance();
        this.transactions = storageManager.loadTransactions();
        this.observers = new ArrayList<>();
        // Inisialisasi budget (contoh)
        this.monthlyBudget = 2000000.0; // Default budget 2jt
    }

    // --- Manajemen Transaksi ---

    public void addTransaction(Transaction transaction) {
        this.transactions.add(transaction);
        saveAndNotify();
    }

    public void deleteTransaction(String id) {
        transactions.removeIf(tx -> tx.getId().equals(id));
        saveAndNotify();
    }

    public List<Transaction> getAllTransactions() {
        // Kembalikan salinan agar list asli tidak termodifikasi
        return new ArrayList<>(transactions);
    }

    /**
     * Melakukan filter transaksi berdasarkan kriteria yang diberikan.
     * Kriteria null atau "ALL" akan diabaikan.
     */
    public List<Transaction> filterTransactions(Category category, LocalDate startDate, LocalDate endDate) {
        return transactions.stream()
                .filter(tx -> (category == null || tx.getCategory() == category))
                .filter(tx -> (startDate == null || !tx.getDate().isBefore(startDate)))
                .filter(tx -> (endDate == null || !tx.getDate().isAfter(endDate)))
                .collect(Collectors.toList());
    }

    private void saveAndNotify() {
        storageManager.saveTransactions(transactions);
        // Setelah data berubah, cek budget lagi
        checkBudgetStatus();
    }

    // --- Manajemen Budget (Subject/Observable) ---

    public double getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(double monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
        System.out.println("Budget bulanan di-set ke: " + monthlyBudget);
        checkBudgetStatus(); // Cek ulang status budget setelah diubah
    }

    /**
     * Menghitung total pengeluaran untuk bulan ini.
     */
    public double getCurrentMonthSpending() {
        LocalDate today = LocalDate.now();
        Month currentMonth = today.getMonth();
        int currentYear = today.getYear();

        return transactions.stream()
                .filter(tx -> tx.getType() == TransactionType.EXPENSE &&
                             tx.getDate().getMonth() == currentMonth &&
                             tx.getDate().getYear() == currentYear)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /**
     * Memeriksa status budget dan mengirim notifikasi jika perlu.
     */
    public void checkBudgetStatus() {
        if (monthlyBudget <= 0) {
            budgetNotificationSent = false; // Reset notifikasi jika tidak ada budget
            return;
        }

        double currentSpending = getCurrentMonthSpending();
        double percentage = (currentSpending / monthlyBudget) * 100;

        if (percentage >= 100 && !budgetNotificationSent) {
            // Kirim notifikasi HANYA SEKALI saat budget terlampaui
            String message = String.format(
                    "PERINGATAN BUDGET: Pengeluaran bulan ini (Rp %,.2f) " +
                    "telah melampaui budget (Rp %,.2f)!",
                    currentSpending, monthlyBudget
            );
            notifyObservers(message);
            budgetNotificationSent = true;
        } else if (percentage < 100) {
            // Reset flag jika pengeluaran kembali di bawah budget (misal karena penghapusan transaksi)
            budgetNotificationSent = false;
        }
    }


    @Override
    public void addObserver(BudgetObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(BudgetObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String message) {
        for (BudgetObserver observer : observers) {
            observer.update(message);
        }
    }
}