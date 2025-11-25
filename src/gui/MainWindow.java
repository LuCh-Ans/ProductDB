package gui;

import database.FileSystem;
import model.Product;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.ArrayList;

public class MainWindow extends JFrame {
    private FileSystem dataBase;
    private JTable productTable;
    private DefaultTableModel tableModel;
    private JTextField idField, nameField, priceField, brandField, categoryField, volumeField;
    private JTextArea descriptionArea;
    private JComboBox<String> searchFieldCombo;
    private JTextField searchValueField;
    private JLabel statusLabel;

    //конструктор всего
    public MainWindow() {
        dataBase = new FileSystem();
        setTitle("Product Database - Золотое Яблоко");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(255, 228, 225));
        createMenuBar();
        createMainPanel();
        createStatusBar();
        setVisible(true);
    }

    //строка меню вверху окна
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Файл");

        JMenuItem createItem = new JMenuItem("Создать БД");
        JMenuItem openItem = new JMenuItem("Открыть БД");
        JMenuItem closeItem = new JMenuItem("Закрыть БД");
        JMenuItem backupItem = new JMenuItem("Создать backup");
        JMenuItem restoreItem = new JMenuItem("Восстановить из backup");
        JMenuItem clearItem = new JMenuItem("Очистить БД");
        JMenuItem deleteFileItem = new JMenuItem("Удалить файл БД");
        JMenuItem exportItem = new JMenuItem("Экспорт CSV");
        JMenuItem exitItem = new JMenuItem("Выход");

        createItem.addActionListener(e -> createDatabase());
        openItem.addActionListener(e -> openDatabase());
        closeItem.addActionListener(e -> closeDatabase());
        backupItem.addActionListener(e -> createBackup());
        restoreItem.addActionListener(e -> restoreFromBackup());
        clearItem.addActionListener(e -> clearDatabase());
        deleteFileItem.addActionListener(e -> deleteDatabaseFile());
        exportItem.addActionListener(e -> exportToCSV());
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(createItem);
        fileMenu.add(openItem);
        fileMenu.add(closeItem);
        fileMenu.addSeparator();
        fileMenu.add(backupItem);
        fileMenu.add(restoreItem);
        fileMenu.add(clearItem);
        fileMenu.add(deleteFileItem);
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    //панели ввода таблицы и поиска
    private void createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(255, 228, 225));
        mainPanel.add(createInputPanel(), BorderLayout.NORTH);
        mainPanel.add(createTablePanel(), BorderLayout.CENTER);
        mainPanel.add(createSearchPanel(), BorderLayout.SOUTH);

        add(mainPanel);
    }

    //поля ввода и кнопки
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Данные товара"));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel("ID:"));
        idField = new JTextField(8);
        row1.add(idField);
        row1.add(new JLabel("Название:"));
        nameField = new JTextField(20);
        row1.add(nameField);
        row1.add(new JLabel("Цена:"));
        priceField = new JTextField(8);
        row1.add(priceField);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(new JLabel("Бренд ID:"));
        brandField = new JTextField(5);
        row2.add(brandField);
        row2.add(new JLabel("Категория ID:"));
        categoryField = new JTextField(5);
        row2.add(categoryField);
        row2.add(new JLabel("Объем:"));
        volumeField = new JTextField(8);
        row2.add(volumeField);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row3.add(new JLabel("Описание:"));
        descriptionArea = new JTextArea(2, 30);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        row3.add(descScroll);

        JButton addButton = new JButton("Добавить");
        JButton findButton = new JButton("Найти по ID");
        JButton deleteButton = new JButton("Удалить по ID");
        JButton updateButton = new JButton("Обновить");
        JButton clearButton = new JButton("Очистить");

        addButton.addActionListener(e -> addProduct());
        findButton.addActionListener(e -> findProduct());
        deleteButton.addActionListener(e -> deleteProduct());
        updateButton.addActionListener(e -> updateProduct());
        clearButton.addActionListener(e -> clearInputFields());

        row3.add(addButton);
        row3.add(findButton);
        row3.add(deleteButton);
        row3.add(updateButton);
        row3.add(clearButton);

        panel.add(row1);
        panel.add(row2);
        panel.add(row3);
        return panel;
    }

    //таблица с данными
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Список товаров"));

        String[] columns = {"ID", "Название", "Цена", "Бренд ID", "Категория ID", "Объем", "Описание"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        productTable = new JTable(tableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    loadSelectedProduct();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(productTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    //для поиска и удаления по полям
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Поиск и удаление по любому полю"));

        String[] fields = {"name", "brandId", "price", "volumeWeight"};
        searchFieldCombo = new JComboBox<>(fields);
        searchValueField = new JTextField(15);

        JButton findAnyButton = new JButton("Найти");
        JButton deleteAnyButton = new JButton("Удалить все найденные");

        findAnyButton.addActionListener(e -> findRecordsByField());
        deleteAnyButton.addActionListener(e -> deleteRecordsByField());

        panel.add(new JLabel("Поле:"));
        panel.add(searchFieldCombo);
        panel.add(new JLabel("Значение:"));
        panel.add(searchValueField);
        panel.add(findAnyButton);
        panel.add(deleteAnyButton);

        return panel;
    }

    //статус бд
    private void createStatusBar() {
        statusLabel = new JLabel("База данных не открыта");
        add(statusLabel, BorderLayout.SOUTH);
    }

    //Операции

    //создание новой бд
    private void createDatabase() {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            int res = dataBase.create(fc.getSelectedFile().getAbsolutePath());
            handleResult(res, "создания", true);
        }
    }

    //открытие существующей бд
    private void openDatabase() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            int res = dataBase.open(fc.getSelectedFile().getAbsolutePath());
            handleResult(res, "открытия", true);
            if (res == 0) refreshTable();
        }
    }

    //закрытие бд
    private void closeDatabase() {
        dataBase.close();
        updateStatus("База данных закрыта");
        clearTable();
        JOptionPane.showMessageDialog(this, "База данных закрыта");
    }

    //добавить новый товар
    private void addProduct() {
        try {
            Product p = createProductFromInput();
            if (p == null) return;
            int res = dataBase.addRecord(p);
            handleResult(res, "добавления", true);
            if (res == 0) {
                clearInputFields();
                refreshTable();
            }
        } catch (Exception e) {
            showError("Ошибка в данных: " + e.getMessage());
        }
    }

    //найти товар
    private void findProduct() {
        try {
            int id = Integer.parseInt(idField.getText().trim());
            Product p = dataBase.findRecordById(id);
            if (p != null) {
                fillInputFields(p);
                JOptionPane.showMessageDialog(this, "Товар найден!");
            } else {
                JOptionPane.showMessageDialog(this, "Товар не найден!", "Не найдено", JOptionPane.WARNING_MESSAGE);
            }
        } catch (NumberFormatException e) {
            showError("Введите корректный ID!");
        }
    }

    //удалить товар по id
    private void deleteProduct() {
        try {
            int id = Integer.parseInt(idField.getText().trim());
            int res = dataBase.deleteRecordById(id);
            handleResult(res, "удаления", true);
            if (res == 0) {
                clearInputFields();
                refreshTable();
            }
        } catch (NumberFormatException e) {
            showError("Введите корректный ID!");
        }
    }

    //обновление данных товара
    private void updateProduct() {
        try {
            Product p = createProductFromInput();
            if (p == null) return;
            int res = dataBase.updateRecord(p);
            handleResult(res, "обновления", true);
            if (res == 0) {
                clearInputFields();
                refreshTable();
            }
        } catch (Exception e) {
            showError("Ошибка: " + e.getMessage());
        }
    }

    //обновление таблички
    private void refreshTable() {
        clearTable();
        if (!dataBase.isOpen()) return;
        List<Product> all = dataBase.getAllRecords();
        for (Product p : all) {
            tableModel.addRow(new Object[]{
                    p.getId(), p.getName(), p.getPrice(), p.getBrandId(),
                    p.getCategoryId(), p.getVolumeWeight(), p.getDescription()
            });
        }
        updateStatus("Обновлено");
    }

    //поиск товаров по любому полю
    private void findRecordsByField() {
        if (!dataBase.isOpen()) return;
        String field = (String) searchFieldCombo.getSelectedItem();
        String value = searchValueField.getText().trim();
        List<Product> results = new ArrayList<>();
        try {
            if ("brandId".equals(field)) {
                results = dataBase.findRecordsByField(field, Integer.parseInt(value));
            } else if ("price".equals(field)) {
                results = dataBase.findRecordsByField(field, Double.parseDouble(value));
            } else {
                results = dataBase.findRecordsByField(field, value);
            }
            displaySearchResults(results);
        } catch (NumberFormatException e) {
            showError("Некорректное значение для числового поля");
        }
    }

    //удаление товаров по любому полю
    private void deleteRecordsByField() {
        if (!dataBase.isOpen()) return;
        String field = (String) searchFieldCombo.getSelectedItem();
        String value = searchValueField.getText().trim();
        try {
            int res;
            if ("brandId".equals(field)) {
                res = dataBase.deleteRecordsByField(field, Integer.parseInt(value));
            } else if ("price".equals(field)) {
                res = dataBase.deleteRecordsByField(field, Double.parseDouble(value));
            } else {
                res = dataBase.deleteRecordsByField(field, value);
            }
            handleResult(res, "удаления", true);
            if (res == 0) refreshTable();
        } catch (NumberFormatException e) {
            showError("Некорректное значение");
        }
    }

    //создание резервной копии
    private void createBackup() {
        if (!dataBase.isOpen()) return;
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                dataBase.backup(fc.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Backup создан");
            } catch (Exception e) {
                showError("Ошибка backup: " + e.getMessage());
            }
        }
    }

    //загрузка данных из backup файла (восстановление из резервной копии)
    private void restoreFromBackup() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            int res = dataBase.restoreFromBackup(fc.getSelectedFile().getAbsolutePath());
            handleResult(res, "восстановления", true);
            if (res == 0) refreshTable();
        }
    }

    //очистка базы данных
    private void clearDatabase() {
        if (confirm("Очистить всю базу?")) {
            dataBase.clear();
            refreshTable();
            JOptionPane.showMessageDialog(this, "База очищена");
        }
    }

    //удаление файла бд
    private void deleteDatabaseFile() {
        if (confirm("Удалить файл базы данных?")) {
            String fname = dataBase.getFilename();
            dataBase.deleteDatabaseFile();
            JOptionPane.showMessageDialog(this, "Файл удалён: " + fname);
            updateStatus("Файл БД удалён");
            clearTable();
        }
    }

    //экспорт в SCV
    private void exportToCSV() {
        if (!dataBase.isOpen()) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("products.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String path = fc.getSelectedFile().getAbsolutePath();
                dataBase.exportToCSV(path);
                JOptionPane.showMessageDialog(this,
                        "Экспорт в CSV завершён!\n\nФайл сохранён:\n" + path +
                                "\n\nОткройте двойным кликом — откроется в Excel или Numbers",
                        "Успешно", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                showError("Ошибка экспорта: " + e.getMessage());
            }
        }
    }

    //создание объекта Product из полей ввода
    private Product createProductFromInput() {
        try {
            Product p = new Product();
            p.setId(Integer.parseInt(idField.getText().trim()));
            p.setName(nameField.getText().trim());
            p.setPrice(Double.parseDouble(priceField.getText().trim()));
            p.setBrandId(Integer.parseInt(brandField.getText().trim()));
            p.setCategoryId(Integer.parseInt(categoryField.getText().trim()));
            p.setVolumeWeight(volumeField.getText().trim());
            p.setDescription(descriptionArea.getText().trim());
            if (!p.isValid()) {
                showError("Не все обязательные поля заполнены");
                return null;
            }
            return p;
        } catch (NumberFormatException e) {
            showError("Проверьте числовые поля");
            return null;
        }
    }

    //перенос данных из Product в поля формы
    private void fillInputFields(Product p) {
        idField.setText(String.valueOf(p.getId()));
        nameField.setText(p.getName());
        priceField.setText(String.valueOf(p.getPrice()));
        brandField.setText(String.valueOf(p.getBrandId()));
        categoryField.setText(String.valueOf(p.getCategoryId()));
        volumeField.setText(p.getVolumeWeight());
        descriptionArea.setText(p.getDescription());
    }

    //загрузка выбранного товара из таблицы при двойном клике
    private void loadSelectedProduct() {
        int row = productTable.getSelectedRow();
        if (row >= 0) {
            int id = (int) tableModel.getValueAt(row, 0);
            Product p = dataBase.findRecordById(id);
            if (p != null) fillInputFields(p);
        }
    }

    //очистка полей ввода
    private void clearInputFields() {
        idField.setText("");
        nameField.setText("");
        priceField.setText("");
        brandField.setText("");
        categoryField.setText("");
        volumeField.setText("");
        descriptionArea.setText("");
    }

    //очистка таблицы
    private void clearTable() {
        tableModel.setRowCount(0);
    }

    //отображение результатов поиска
    private void displaySearchResults(List<Product> results) {
        clearTable();
        for (Product p : results) {
            tableModel.addRow(new Object[]{
                    p.getId(), p.getName(), p.getPrice(), p.getBrandId(),
                    p.getCategoryId(), p.getVolumeWeight(), p.getDescription()
            });
        }
        JOptionPane.showMessageDialog(this, "Найдено записей: " + results.size());
    }

    //обновление строки статуса
    private void updateStatus(String msg) {
        String status = msg;
        if (dataBase.isOpen()) {
            status += " | Записей: " + dataBase.getRecordCount();
        }
        statusLabel.setText(status);
    }

    //работа над ошибками (в слова)
    private void handleResult(int code, String op, boolean showAlert) {
        if (code == 0) {
            if (showAlert) JOptionPane.showMessageDialog(this, "Успешно: " + op);
            return;
        }
        String err = switch (code) {
            case 1 -> "Файл не найден";
            case 2 -> "Неверный формат файла";
            case 3 -> "ID уже существует";
            case 4 -> "Запись не найдена";
            default -> "Неизвестная ошибка";
        };
        if (showAlert) showError("Ошибка " + op + ": " + err);
    }

    //окно с ошибкой
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    //запрос на подтверждение
    private boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(this, msg, "Подтверждение", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    //запуск gui
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(MainWindow::new);
    }
}