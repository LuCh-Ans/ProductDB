package model;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Product {
    private int id;
    private String name;
    private double price;
    private int brandId;
    private int categoryId;
    private String volumeWeight;
    private String description;

    public Product() {
        this.id = 0;
        this.name = "";
        this.price = 0.0;
        this.brandId = 0;
        this.categoryId = 0;
        this.volumeWeight = "";
        this.description = "";
    }

    //Валидация
    public boolean isValid() {
        return id > 0 && name != null && !name.trim().isEmpty() && price > 0;
    }

    //Геттеры
    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getBrandId() { return brandId; }
    public int getCategoryId() { return categoryId; }
    public String getVolumeWeight() { return volumeWeight; }
    public String getDescription() { return description; }

    //Сеттеры
    public void setId(int id) { this.id = id; }
    public void setName(String name) {
        this.name = truncateString(name, DBConstants.NAME_SIZE);
    }
    public void setPrice(double price) { this.price = price; }
    public void setBrandId(int brandId) { this.brandId = brandId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public void setVolumeWeight(String volumeWeight) {
        this.volumeWeight = truncateString(volumeWeight, DBConstants.VOLUME_SIZE);
    }
    public void setDescription(String description) {
        this.description = truncateString(description, DBConstants.DESC_SIZE);
    }

    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    // Сериализация
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(id);
        dos.writeDouble(price);
        dos.writeInt(brandId);
        dos.writeInt(categoryId);

        writeFixedLengthString(dos, name, DBConstants.NAME_SIZE);
        writeFixedLengthString(dos, volumeWeight, DBConstants.VOLUME_SIZE);
        writeFixedLengthString(dos, description, DBConstants.DESC_SIZE);

        dos.close();
        return baos.toByteArray();
    }

    // Десериализация
    public void deserialize(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);

        id = dis.readInt();
        price = dis.readDouble();
        brandId = dis.readInt();
        categoryId = dis.readInt();

        name = readFixedLengthString(dis, DBConstants.NAME_SIZE);
        volumeWeight = readFixedLengthString(dis, DBConstants.VOLUME_SIZE);
        description = readFixedLengthString(dis, DBConstants.DESC_SIZE);

        dis.close();
    }

    private void writeFixedLengthString(DataOutputStream dos, String str, int length) throws IOException {
        byte[] bytes = (str != null ? str : "").getBytes(StandardCharsets.UTF_8);
        byte[] fixedBytes = new byte[length];
        int copyLength = Math.min(bytes.length, length);
        System.arraycopy(bytes, 0, fixedBytes, 0, copyLength);
        for (int i = copyLength; i < length; i++) {
            fixedBytes[i] = 0;
        }
        dos.write(fixedBytes);
    }

    private String readFixedLengthString(DataInputStream dis, int length) throws IOException {
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        int actualLength = 0;
        while (actualLength < length && bytes[actualLength] != 0) {
            actualLength++;
        }
        return new String(bytes, 0, actualLength, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return String.format("Product[ID=%d, Name=%s, Price=%.2f, Brand=%d, Category=%d]", id, name, price, brandId, categoryId);
    }
}