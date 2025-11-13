package com.financetracker.service;

import com.financetracker.patterns.observer.BudgetObserver;
import com.financetracker.storage.StorageManager;

/**
 * Implementasi Observer Pattern.
 * Service ini "mendengarkan" perubahan budget (dari TransactionService)
 * dan mencatat notifikasi ke file log saat di-update.
 */
public class NotificationService implements BudgetObserver {

    private final StorageManager storageManager;

    public NotificationService() {
        this.storageManager = StorageManager.getInstance();
    }

    /**
     * Dipanggil oleh Subject (TransactionService) ketika budget terlampaui.
     */
    @Override
    public void update(String message) {
        System.out.println("NotificationService Menerima Update: " + message);
        storageManager.logNotification(message);
    }
}