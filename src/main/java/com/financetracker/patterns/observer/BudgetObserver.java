package com.financetracker.patterns.observer;

/**
 * Interface Observer untuk Observer Pattern.
 */
public interface BudgetObserver {
    void update(String message);
}