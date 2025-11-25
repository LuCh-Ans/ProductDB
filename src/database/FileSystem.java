package database;
import java.util.ArrayList;
import model.Product;
import model.DBConstants;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FileSystem {
    private String filename;
    private RandomAccessFile file;
    private DBHeader header;
    private boolean isOpen = false;

    private final Map<Integer, Long> idIndex = new HashMap<>();
    private final Map<Integer, Set<Long>> brandIndex = new HashMap<>();
    private final Map<Double, Set<Long>> priceIndex = new HashMap<>();
    private final Map<String, Set<Long>> nameIndex = new HashMap<>();
    private final Map<String, Set<Long>> descriptionIndex = new HashMap<>();
    private final Map<String, Set<Long>> volumeWeightIndex = new HashMap<>();
    private BitSet freeSpaceMap;
    private int totalSlots;

    public FileSystem() {
        this.header = new DBHeader();
    }

    //Создание бд
    public int create(String dbname) {
        try {
            close();
            this.filename = dbname;
            file = new RandomAccessFile(filename, "rw");
            header = new DBHeader();
            byte[] headerData = header.serialize();
            file.write(headerData);
            initializeFreeSpaceMap();
            isOpen = true;
            return DBConstants.SUCCESS;
        } catch (IOException e) {
            return DBConstants.ERROR_INVALID_FORMAT;
        }
    }

    //Открытие бд
    public int open(String dbname) {
        try {
            close();
            this.filename = dbname;
            if (!Files.exists(Paths.get(filename)))
                return DBConstants.ERROR_FILE_NOT_FOUND;
            file = new RandomAccessFile(filename, "rw");
            byte[] headerData = new byte[DBConstants.HEADER_SIZE];
            int read = file.read(headerData);
            if (read != DBConstants.HEADER_SIZE) {
                close();
                return DBConstants.ERROR_INVALID_FORMAT;
            }
            header.deserialize(headerData);
            if (!header.isValid()) {
                close();
                return DBConstants.ERROR_INVALID_FORMAT;
            }
            rebuildIndexes();
            isOpen = true;
            return DBConstants.SUCCESS;
        } catch (IOException e) {
            return DBConstants.ERROR_INVALID_FORMAT;
        }
    }

    //Закрытие бд
    public void close() {
        try {
            if (file != null) {
                file.close();
                file = null;
            }
            isOpen = false;
            clearIndexes();
        } catch (IOException ignored) {}
    }

    //Добавление записи
    public int addRecord(Product record) {
        if (!isOpen) return DBConstants.ERROR_FILE_NOT_FOUND;
        if (!record.isValid()) return DBConstants.ERROR_INVALID_FORMAT;
        if (idIndex.containsKey(record.getId()))
            return DBConstants.ERROR_DUPLICATE_ID;

        try {
            long position = findFreeSpace();
            if (position == -1) position = file.length();
            writeRecordAtPosition(record, position);
            addToIndexes(record, position);
            header.setRecordCount(header.getRecordCount() + 1);
            updateHeader();
            return DBConstants.SUCCESS;
        } catch (IOException e) {
            return DBConstants.ERROR_INVALID_FORMAT;
        }
    }

    //Поиск по id
    public Product findRecordById(int id) {
        if (!isOpen) return null;
        Long pos = idIndex.get(id);
        if (pos == null) return null;
        try {
            Product p = readRecordAtPosition(pos);
            return (p != null && p.isValid()) ? p : null;
        } catch (IOException e) {
            return null;
        }
    }

    //Удаление по id
    public int deleteRecordById(int id) {
        if (!isOpen) return DBConstants.ERROR_FILE_NOT_FOUND;
        Long pos = idIndex.get(id);
        if (pos == null) return DBConstants.ERROR_RECORD_NOT_FOUND;
        try {
            Product p = readRecordAtPosition(pos);
            if (p != null) {
                removeFromIndexes(p, pos);
                markSpaceAsFree(pos);
                file.seek(pos);
                file.write(new byte[DBConstants.RECORD_SIZE]);
                header.setRecordCount(header.getRecordCount() - 1);
                updateHeader();
                return DBConstants.SUCCESS;
            } else {
                return DBConstants.ERROR_RECORD_NOT_FOUND;
            }
        } catch (IOException e) {
            return DBConstants.ERROR_INVALID_FORMAT;
        }
    }

    //Получение всех записей
    public List<Product> getAllRecords() {
        if (!isOpen) return new ArrayList<>();
        List<Product> records = new ArrayList<>();
        try {
            long pos = header.getDataOffset();
            long end = file.length();
            while (pos + DBConstants.RECORD_SIZE <= end) {
                Product p = readRecordAtPosition(pos);
                if (p != null && p.isValid()) records.add(p);
                pos += DBConstants.RECORD_SIZE;
            }
        } catch (IOException ignored) {}
        return records;
    }


    private void addToIndex(Map<String, Set<Long>> index, String key, long pos) {
        if (key == null || key.isEmpty()) return;
        index.computeIfAbsent(key, k -> new HashSet<>()).add(pos);
    }

    private void removeFromIndex(Map<String, Set<Long>> index, String key, long pos) {
        if (key == null || key.isEmpty()) return;
        Set<Long> set = index.get(key);
        if (set != null) {
            set.remove(pos);
            if (set.isEmpty()) {
                index.remove(key);
            }
        }
    }

    //Поиск по полю (использует индексы, если они есть)
    public List<Product> findRecordsByField(String field, Object value) {
        if (!isOpen) return new ArrayList<>();
        List<Product> results = new ArrayList<>();
        try {
            if ("id".equals(field) && value instanceof Integer) {
                Product p = findRecordById((Integer) value);
                if (p != null) results.add(p);
                return results;
            } else if ("brandId".equals(field) && value instanceof Integer) {
                Set<Long> positions = brandIndex.get((Integer) value);
                if (positions != null) {
                    for (Long pos : positions) {
                        Product p = readRecordAtPosition(pos);
                        if (p != null && p.isValid()) results.add(p);
                    }
                }
                return results;
            } else if ("price".equals(field) && value instanceof Double) {
                Set<Long> positions = priceIndex.get((Double) value);
                if (positions != null) {
                    for (Long pos : positions) {
                        Product p = readRecordAtPosition(pos);
                        if (p != null && p.isValid()) results.add(p);
                    }
                }
                return results;
            } else if ("name".equals(field) && value instanceof String) {
                Set<Long> positions = nameIndex.get((String) value);
                if (positions != null) {
                    for (Long pos : positions) {
                        Product p = readRecordAtPosition(pos);
                        if (p != null && p.isValid()) results.add(p);
                    }
                }
                return results;
            } else if ("volumeWeight".equals(field) && value instanceof String) {
                Set<Long> positions = volumeWeightIndex.get((String) value);
                if (positions != null) {
                    for (Long pos : positions) {
                        Product p = readRecordAtPosition(pos);
                        if (p != null && p.isValid()) results.add(p);
                    }
                }
                return results;
            } else if ("description".equals(field) && value instanceof String) {
                Set<Long> positions = descriptionIndex.get((String) value);
                if (positions != null) {
                    for (Long pos : positions) {
                        Product p = readRecordAtPosition(pos);
                        if (p != null && p.isValid()) results.add(p);
                    }
                }
                return results;
            } else {
                // fallback: full scan for unknown field or pattern search
                return linearSearchByStringField(field, (value != null) ? value.toString() : "");
            }
        } catch (Exception ignored) {}
        return results;
    }

    //Линейный поиск в строке (оставляем как fallback)
    private List<Product> linearSearchByStringField(String field, String value) throws IOException {
        List<Product> results = new ArrayList<>();
        long pos = header.getDataOffset();
        long end = file.length();
        String target = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        while (pos + DBConstants.RECORD_SIZE <= end) {
            Product p = readRecordAtPosition(pos);
            if (p != null && p.isValid()) {
                String fieldValue = switch (field) {
                    case "name" -> p.getName();
                    case "volumeWeight" -> p.getVolumeWeight();
                    case "description" -> p.getDescription();
                    default -> "";
                };
                if (fieldValue != null && fieldValue.trim().toLowerCase(Locale.ROOT).equals(target)) {
                    results.add(p);
                }
            }
            pos += DBConstants.RECORD_SIZE;
        }
        return results;
    }

    //Удаление по полю (оптимизировано для индексированных полей)
    public int deleteRecordsByField(String field, Object value) {
        if (!isOpen) return DBConstants.ERROR_FILE_NOT_FOUND;
        List<Product> toDelete = findRecordsByField(field, value);
        if (toDelete.isEmpty()) return DBConstants.ERROR_RECORD_NOT_FOUND;

        try {
            for (Product p : toDelete) {
                Long pos = idIndex.get(p.getId());
                if (pos != null) {
                    removeFromIndexes(p, pos);
                    markSpaceAsFree(pos);
                    file.seek(pos);
                    file.write(new byte[DBConstants.RECORD_SIZE]);
                }
            }
            header.setRecordCount(header.getRecordCount() - toDelete.size());
            updateHeader();
            return DBConstants.SUCCESS;
        } catch (Exception e) {
            return DBConstants.ERROR_INVALID_FORMAT;
        }
    }

    //Обновление записи
    public int updateRecord(Product updated) {
        if (!isOpen) return DBConstants.ERROR_FILE_NOT_FOUND;
        if (!updated.isValid()) return DBConstants.ERROR_INVALID_FORMAT;
        Long pos = idIndex.get(updated.getId());
        if (pos == null) return DBConstants.ERROR_RECORD_NOT_FOUND;

        try {
            Product old = readRecordAtPosition(pos);
            if (old != null) removeFromIndexes(old, pos);
            writeRecordAtPosition(updated, pos);
            addToIndexes(updated, pos);
            return DBConstants.SUCCESS;
        } catch (IOException e) {
            return DBConstants.ERROR_INVALID_FORMAT;
        }
    }

    //Очистка бд
    public void clear() {
        if (!isOpen) return;
        try {
            clearIndexes();
            header.setRecordCount(0);
            file.setLength(DBConstants.HEADER_SIZE);
            initializeFreeSpaceMap();
            updateHeader();
        } catch (IOException ignored) {}
    }

    //Создание backup
    public void backup(String backupPath) throws IOException {
        if (!isOpen) throw new IOException("База не открыта");
        Files.copy(Paths.get(filename), Paths.get(backupPath), StandardCopyOption.REPLACE_EXISTING);
    }

    //Восстановление из backup
    public int restoreFromBackup(String backupPath) {
        close();
        try {
            Files.copy(Paths.get(backupPath), Paths.get(filename), StandardCopyOption.REPLACE_EXISTING);
            return open(filename);
        } catch (IOException e) {
            return DBConstants.ERROR_FILE_NOT_FOUND;
        }
    }

    //Удаление файла бд
    public void deleteDatabaseFile() {
        close();
        try {
            Files.deleteIfExists(Paths.get(filename));
            filename = null;
        } catch (IOException ignored) {}
    }

    //Создание CSV
    public void exportToCSV(String path) throws IOException {
        if (!isOpen) throw new IOException("База не открыта");

        //Делаем расширение .csv
        if (!path.toLowerCase().endsWith(".csv")) {
            path = path.substring(0, path.lastIndexOf('.')) + ".csv";
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"))) {
            // Заголовок
            writer.write("ID;Название;Цена;Бренд ID;Категория ID;Объём;Описание");
            writer.newLine();

            //Данные
            for (Product p : getAllRecords()) {
                String line = String.format("%d;%s;%.2f;%d;%d;%s;%s",
                        p.getId(),
                        p.getName().replace(";", ","),           // защита от точки с запятой
                        p.getPrice(),
                        p.getBrandId(),
                        p.getCategoryId(),
                        p.getVolumeWeight().replace(";", ","),
                        p.getDescription().replace(";", ",")
                );
                writer.write(line);
                writer.newLine();
            }
        }
    }

    //Вспомогательные методы для методов выше
    //Управление свободным местом
    private long findFreeSpace() {
        if (freeSpaceMap == null) return -1;
        int freeBit = freeSpaceMap.nextClearBit(0);
        if (freeBit < totalSlots) {
            freeSpaceMap.set(freeBit);
            return header.getDataOffset() + (freeBit * DBConstants.RECORD_SIZE);
        }
        return -1;
    }

    private void markSpaceAsFree(long position) {
        if (freeSpaceMap != null) {
            int bitIndex = (int) ((position - header.getDataOffset()) / DBConstants.RECORD_SIZE);
            if (bitIndex >= 0 && bitIndex < totalSlots) {
                freeSpaceMap.clear(bitIndex);
            }
        }
    }

    //Работа с записями в файле
    private void writeRecordAtPosition(Product record, long position) throws IOException {
        file.seek(position);
        file.write(record.serialize());
    }

    private Product readRecordAtPosition(long position) throws IOException {
        file.seek(position);
        byte[] buffer = new byte[DBConstants.RECORD_SIZE];
        int read = file.read(buffer);
        if (read != DBConstants.RECORD_SIZE) return null;
        Product p = new Product();
        p.deserialize(buffer);
        return p;
    }

    //Перестроение индексов (исправлено)
    private void rebuildIndexes() throws IOException {
        clearIndexes();

        long fileSize = file.length();
        long position = header.getDataOffset();
        if (fileSize < header.getDataOffset()) {
            // пустая БД (нет записей)
            totalSlots = 0;
            freeSpaceMap = new BitSet();
            return;
        }

        totalSlots = (int) ((fileSize - header.getDataOffset()) / DBConstants.RECORD_SIZE);
        freeSpaceMap = new BitSet(totalSlots);

        while (position + DBConstants.RECORD_SIZE <= fileSize) {
            Product record = readRecordAtPosition(position);
            if (record != null && record.isValid()) {
                addToIndexes(record, position);
                int bitIndex = (int) ((position - header.getDataOffset()) / DBConstants.RECORD_SIZE);
                if (bitIndex >= 0 && bitIndex < totalSlots) {
                    freeSpaceMap.set(bitIndex);
                }
            }
            position += DBConstants.RECORD_SIZE;
        }
    }

    //Добавление в индексы (включая строковые)
    private void addToIndexes(Product record, long position) {
        idIndex.put(record.getId(), position);

        brandIndex.computeIfAbsent(record.getBrandId(), k -> new HashSet<>()).add(position);
        priceIndex.computeIfAbsent(record.getPrice(), k -> new HashSet<>()).add(position);

        addToIndex(nameIndex, record.getName(), position);
        addToIndex(descriptionIndex, record.getDescription(), position);
        addToIndex(volumeWeightIndex, record.getVolumeWeight(), position);
    }

    //Удаление из индексов (включая строковые)
    private void removeFromIndexes(Product record, long position) {
        idIndex.remove(record.getId());

        Set<Long> brandSet = brandIndex.get(record.getBrandId());
        if (brandSet != null) {
            brandSet.remove(position);
            if (brandSet.isEmpty()) brandIndex.remove(record.getBrandId());
        }

        Set<Long> priceSet = priceIndex.get(record.getPrice());
        if (priceSet != null) {
            priceSet.remove(position);
            if (priceSet.isEmpty()) priceIndex.remove(record.getPrice());
        }

        removeFromIndex(nameIndex, record.getName(), position);
        removeFromIndex(descriptionIndex, record.getDescription(), position);
        removeFromIndex(volumeWeightIndex, record.getVolumeWeight(), position);
    }

    //Инициализация пустой карты свободных мест
    private void initializeFreeSpaceMap() {
        totalSlots = 0;
        freeSpaceMap = new BitSet();
    }

    //Перемещение в начало и запись обновленного заголовка
    private void updateHeader() throws IOException {
        file.seek(0);
        file.write(header.serialize());
    }

    //Очищение всех индексов и карты свободного места
    private void clearIndexes() {
        idIndex.clear();
        brandIndex.clear();
        priceIndex.clear();
        nameIndex.clear();
        descriptionIndex.clear();
        volumeWeightIndex.clear();
        if (freeSpaceMap != null) freeSpaceMap.clear();
    }

    // Геттеры
    public boolean isOpen() { return isOpen; }
    public String getFilename() { return filename; }
    public int getRecordCount() { return header.getRecordCount(); }
}
