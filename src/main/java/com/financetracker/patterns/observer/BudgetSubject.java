package com.financetracker.patterns.observer;

/**
 * Interface Subject (Observable) untuk Observer Pattern.
 */
public interface BudgetSubject {
    void addObserver(BudgetObserver observer);
    void removeObserver(BudgetObserver observer);
    void notifyObservers(String message);
}