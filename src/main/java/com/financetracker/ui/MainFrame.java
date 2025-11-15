package com.financetracker.ui;

import com.financetracker.factory.TransactionFactory;
import com.financetracker.model.Category;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;
import com.financetracker.patterns.strategy.*;
import com.financetracker.service.NotificationService;
import com.financetracker.service.OpenAIService;
import com.financetracker.service.ReportService;
import com.financetracker.service.TransactionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import javax.swing.SpinnerDateModel;

public class MainFrame extends JFrame {

    // Services
    private TransactionService transactionService;
    private ReportService reportService;
    private OpenAIService openAIService;

    // Table & Models
    private JTable transactionTable;
    private TransactionTableModel tableModel;

    // Budget UI
    private JLabel budgetLabel;
    private JProgressBar budgetProgressBar;
    private JButton setBudgetButton;

    // Input panel components
    private JTextField dateField;
    private JButton datePickerButton;
    private JTextField descriptionField;
    private JTextField amountField;
    private JComboBox<TransactionType> typeComboBox;
    private JComboBox<Category> categoryComboBox;
    private JButton addButton;

    // Filter panel components
    private JComboBox<Category> filterCategoryComboBox;
    private JTextField filterStartDateField;
    private JTextField filterEndDateField;
    private JButton filterButton;
    private JButton deleteButton;

    // Report & AI
    private JComboBox<ReportStrategy> reportComboBox;
    private JButton reportButton;
    private JButton aiAdviceButton;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public MainFrame() {
        initServices();
        initUI();
        loadInitialData();
    }

    private void initServices() {
        transactionService = new TransactionService();
        reportService = new ReportService();
        openAIService = new OpenAIService();

        NotificationService logger = new NotificationService();
        transactionService.addObserver(logger);
    }

    private void initUI() {

        setTitle("Personal Finance Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        getContentPane().setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        topPanel.add(createInputPanel());
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(createFilterPanel());

        add(topPanel, BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    // ============================================================
    // PANEL INPUT TRANSAKSI (FIXED WITH GRIDBAGLAYOUT)
    // ============================================================

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Tambah Transaksi Baru"));
        panel.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;

        // Tanggal
        g.gridx = 0; g.gridy = y;
        panel.add(new JLabel("Tanggal:"), g);

        dateField = new JTextField(LocalDate.now().format(dateFormatter), 12);
        g.gridx = 1;
        panel.add(dateField, g);

        datePickerButton = new JButton("📅");
        datePickerButton.setMargin(new Insets(2, 8, 2, 8));
        datePickerButton.addActionListener(e -> showDatePickerDialog());
        g.gridx = 2;
        panel.add(datePickerButton, g);

        // Deskripsi
        y++;
        g.gridx = 0; g.gridy = y;
        panel.add(new JLabel("Deskripsi:"), g);

        descriptionField = new JTextField(20);
        g.gridx = 1; g.gridwidth = 2;
        panel.add(descriptionField, g);
        g.gridwidth = 1;

        // Jumlah
        y++;
        g.gridx = 0; g.gridy = y;
        panel.add(new JLabel("Jumlah (Rp):"), g);

        amountField = new JTextField(12);
        g.gridx = 1; g.gridwidth = 2;
        panel.add(amountField, g);
        g.gridwidth = 1;

        // Tipe & Kategori
        y++;
        g.gridx = 0; g.gridy = y;
        panel.add(new JLabel("Tipe:"), g);

        typeComboBox = new JComboBox<>(TransactionType.values());
        g.gridx = 1;
        panel.add(typeComboBox, g);

        g.gridx = 2;
        categoryComboBox = new JComboBox<>(Category.values());
        panel.add(categoryComboBox, g);

        // Tombol Tambah
        y++;
        addButton = new JButton("Tambah");
        addButton.addActionListener(e -> addTransaction());

        g.gridx = 0; g.gridy = y; g.gridwidth = 3;
        panel.add(addButton, g);

        return panel;
    }

    // ============================================================
    // FILTER PANEL
    // ============================================================

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(new TitledBorder("Filter & Aksi"));
        panel.setOpaque(false);

        Category[] categories = new Category[Category.values().length + 1];
        categories[0] = null;
        System.arraycopy(Category.values(), 0, categories, 1, Category.values().length);

        filterCategoryComboBox = new JComboBox<>(categories);
        filterCategoryComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) setText("Semua Kategori");
                return this;
            }
        });

        filterStartDateField = new JTextField(10);
        filterEndDateField = new JTextField(10);

        filterButton = new JButton("Filter");
        filterButton.addActionListener(e -> filterTransactions());

        deleteButton = new JButton("Hapus Terpilih");
        deleteButton.setBackground(new Color(220, 50, 50));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteTransaction());

        panel.add(new JLabel("Kategori:"));
        panel.add(filterCategoryComboBox);

        panel.add(new JLabel("Dari:"));
        panel.add(filterStartDateField);

        panel.add(new JLabel("Sampai:"));
        panel.add(filterEndDateField);

        panel.add(filterButton);
        panel.add(deleteButton);

        return panel;
    }

    // ============================================================
    // TABLE PANEL
    // ============================================================

    private JScrollPane createTablePanel() {
        tableModel = new TransactionTableModel();
        transactionTable = new JTable(tableModel);

        transactionTable.setRowHeight(26);

        // sembunyikan kolom ID (model index 0)
        TableColumn idCol = transactionTable.getColumnModel().getColumn(0);
        transactionTable.removeColumn(idCol);

        transactionTable.getSelectionModel().addListSelectionListener(
            e -> deleteButton.setEnabled(transactionTable.getSelectedRow() >= 0)
        );

        JScrollPane scroll = new JScrollPane(transactionTable);
        scroll.setBorder(new TitledBorder("Daftar Transaksi"));
        scroll.setPreferredSize(new Dimension(900, 350));
        return scroll;
    }

    // ============================================================
    // BOTTOM PANEL (BUDGET, REPORT, AI)
    // ============================================================

    private JPanel createBottomPanel() {

        JPanel main = new JPanel(new BorderLayout(10,10));
        main.setOpaque(false);

        // ----- BUDGET PANEL -----
        JPanel budgetPanel = new JPanel();
        budgetPanel.setLayout(new BoxLayout(budgetPanel, BoxLayout.Y_AXIS));
        budgetPanel.setBorder(new TitledBorder("Budget Bulanan"));
        budgetPanel.setOpaque(false);

        budgetLabel = new JLabel("Pengeluaran: Rp 0 / Rp 0", SwingConstants.CENTER);
        budgetLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        budgetProgressBar = new JProgressBar(0,100);
        budgetProgressBar.setStringPainted(true);

        setBudgetButton = new JButton("Set Budget");
        setBudgetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        setBudgetButton.addActionListener(e -> setBudget());

        budgetPanel.add(budgetLabel);
        budgetPanel.add(Box.createVerticalStrut(5));
        budgetPanel.add(budgetProgressBar);
        budgetPanel.add(Box.createVerticalStrut(5));
        budgetPanel.add(setBudgetButton);

        // ----- ACTION PANEL -----
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionPanel.setBorder(new TitledBorder("Laporan & Analisis"));

        ReportStrategy[] options = {
            new DailyReportStrategy(),
            new MonthlyReportStrategy(),
            new YearlyReportStrategy()
        };

        reportComboBox = new JComboBox<>(options);
        reportComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(((ReportStrategy)value).getReportName());
                return this;
            }
        });

        reportButton = new JButton("Buat Laporan");
        reportButton.addActionListener(e -> generateReport());

        aiAdviceButton = new JButton("Dapatkan Saran Keuangan (AI)");
        aiAdviceButton.setBackground(new Color(50, 150, 70));
        aiAdviceButton.setForeground(Color.WHITE);
        aiAdviceButton.addActionListener(e -> openAIChatDialog());

        actionPanel.add(new JLabel("Jenis Laporan:"));
        actionPanel.add(reportComboBox);
        actionPanel.add(reportButton);
        actionPanel.add(aiAdviceButton);

        main.add(budgetPanel, BorderLayout.WEST);
        main.add(actionPanel, BorderLayout.CENTER);

        return main;
    }

    // ============================================================
    // LOGIC METHODS
    // ============================================================

    private void loadInitialData() {
        refreshTable(transactionService.getAllTransactions());
        refreshBudget();
    }

    private void refreshTable(List<Transaction> list) {
        tableModel.setTransactions(list);
        deleteButton.setEnabled(false);
    }

    private void refreshBudget() {

        double budget = transactionService.getMonthlyBudget();
        double spending = transactionService.getCurrentMonthSpending();

        int percent = (budget > 0) ? (int)((spending / budget)*100) : 0;

        budgetLabel.setText(String.format(
            "Pengeluaran: Rp %,.2f / Rp %,.2f",
            spending, budget
        ));

        budgetProgressBar.setValue(Math.min(100, percent));
        budgetProgressBar.setString(percent + "%");

        if (percent > 90) budgetProgressBar.setForeground(Color.RED);
        else if (percent > 70) budgetProgressBar.setForeground(Color.ORANGE);
        else budgetProgressBar.setForeground(new Color(40,170,80));
    }

    private void addTransaction() {
        try {

            LocalDate date = LocalDate.parse(dateField.getText(), dateFormatter);
            String desc = descriptionField.getText().trim();
            if (desc.isEmpty()) desc = "(No description)";

            double amount = Double.parseDouble(amountField.getText());
            TransactionType type = (TransactionType) typeComboBox.getSelectedItem();
            Category category = (Category) categoryComboBox.getSelectedItem();

            Transaction t = TransactionFactory.createTransaction(
                    date, desc, amount, type, category
            );

            transactionService.addTransaction(t);
            loadInitialData();

            descriptionField.setText("");
            amountField.setText("");
            dateField.setText(LocalDate.now().format(dateFormatter));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Input tidak valid: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteTransaction() {
        int row = transactionTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Tidak ada transaksi yang dipilih.");
            return;
        }

        int modelRow = transactionTable.convertRowIndexToModel(row);
        String id = (String) tableModel.getValueAt(modelRow, 0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Yakin ingin menghapus transaksi ini?",
                "Konfirmasi",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            transactionService.deleteTransaction(id);
            loadInitialData();
        }
    }

    private void filterTransactions() {
        try {
            Category cat = (Category) filterCategoryComboBox.getSelectedItem();
            LocalDate start = filterStartDateField.getText().isBlank() ? null :
                    LocalDate.parse(filterStartDateField.getText(), dateFormatter);
            LocalDate end = filterEndDateField.getText().isBlank() ? null :
                    LocalDate.parse(filterEndDateField.getText(), dateFormatter);

            List<Transaction> list = transactionService.filterTransactions(cat, start, end);
            refreshTable(list);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Format tanggal salah.");
        }
    }

    private void setBudget() {
        String current = String.valueOf(transactionService.getMonthlyBudget());

        String input = JOptionPane.showInputDialog(
                this,
                "Masukkan budget bulanan:",
                current
        );

        if (input == null) return;

        try {
            double value = Double.parseDouble(input);
            if (value < 0) throw new Exception("Budget harus ≥ 0");

            transactionService.setMonthlyBudget(value);
            refreshBudget();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Input budget tidak valid.");
        }
    }

    // ============================================================
    // AI SECTION (UNCHANGED)
    // ============================================================

    private void generateReport() {
        ReportStrategy strategy = (ReportStrategy) reportComboBox.getSelectedItem();
        reportService.setStrategy(strategy);

        String content = reportService.generateReport(transactionService.getAllTransactions());

        JTextArea area = new JTextArea(content);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(this, scroll, strategy.getReportName(), JOptionPane.INFORMATION_MESSAGE);
    }

    private void openAIChatDialog() {
        String summary = reportService.generateReport(transactionService.getAllTransactions());

        JDialog loading = new JDialog(this, "Menghubungi AI...", true);
        JProgressBar pb = new JProgressBar();
        pb.setIndeterminate(true);
        loading.add(pb);
        loading.setSize(300, 75);
        loading.setLocationRelativeTo(this);

        SwingWorker<String,Void> worker = new SwingWorker<>() {
            protected String doInBackground() {
                return openAIService.startFinancialAdviceSession(summary);
            }
            protected void done() {
                loading.dispose();
                String reply;
                try { reply = get(); }
                catch (Exception ex) { reply = "Gagal: " + ex.getMessage(); }

                showChatDialog(reply);
            }
        };

        worker.execute();
        loading.setVisible(true);
    }

    private void showChatDialog(String firstMessage) {
        JDialog dlg = new JDialog(this, "AI Financial Advisor", true);
        dlg.setSize(700, 500);
        dlg.setLayout(new BorderLayout(10,10));
        dlg.setLocationRelativeTo(this);

        JTextArea chat = new JTextArea();
        chat.setEditable(false);
        chat.setLineWrap(true);
        chat.append("AI: " + firstMessage + "\n\n");

        JScrollPane scroll = new JScrollPane(chat);

        JTextField input = new JTextField();
        JButton send = new JButton("Kirim");

        JPanel bottom = new JPanel(new BorderLayout(5,5));
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(send, BorderLayout.EAST);

        send.addActionListener(e -> {
            String msg = input.getText().trim();
            if (msg.isEmpty()) return;

            chat.append("YOU: " + msg + "\n");
            input.setText("");

            SwingWorker<String,Void> talk = new SwingWorker<>() {
                protected String doInBackground() {
                    return openAIService.continueChat(msg);
                }
                protected void done() {
                    try {
                        chat.append("AI: " + get() + "\n\n");
                        chat.setCaretPosition(chat.getDocument().getLength());
                    } catch (Exception ex) {
                        chat.append("AI ERROR: " + ex.getMessage());
                    }
                }
            };

            talk.execute();
        });

        dlg.add(scroll, BorderLayout.CENTER);
        dlg.add(bottom, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    // ============================================================
    // DATE PICKER
    // ============================================================

    private void showDatePickerDialog() {
        JDialog dialog = new JDialog(this, "Pilih Tanggal", true);
        dialog.setSize(300,140);
        dialog.setLayout(new BorderLayout(10,10));
        dialog.setLocationRelativeTo(this);

        SpinnerDateModel model = new SpinnerDateModel(new Date(), null, null,
                java.util.Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));

        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Batal");

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(cancel);
        btnPanel.add(ok);

        dialog.add(spinner, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        ok.addActionListener(e -> {
            Date selected = model.getDate();
            String formatted = new SimpleDateFormat("yyyy-MM-dd").format(selected);
            dateField.setText(formatted);
            dialog.dispose();
        });

        cancel.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    // ============================================================
    // MAIN
    // ============================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
