package database;

import model.DBConstants;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DBHeader {
    private String signature;
    private int version;
    private int recordCount;
    private long dataOffset;
    private long indexOffset;
    private long freeSpaceOffset;

    //Конструктор
    public DBHeader() {
        this.signature = DBConstants.SIGNATURE;
        this.version = DBConstants.VERSION;
        this.recordCount = 0;
        this.dataOffset = DBConstants.HEADER_SIZE;
        this.indexOffset = -1;
        this.freeSpaceOffset = -1;
    }

    //Сериализация
    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(DBConstants.HEADER_SIZE);
        byte[] sigBytes = signature.getBytes(StandardCharsets.UTF_8);
        buffer.put(sigBytes);
        for (int i = sigBytes.length; i < 16; i++) buffer.put((byte) 0);
        buffer.putInt(version);
        buffer.putInt(recordCount);
        buffer.putLong(dataOffset);
        buffer.putLong(indexOffset);
        buffer.putLong(freeSpaceOffset);
        while (buffer.position() < DBConstants.HEADER_SIZE) buffer.put((byte) 0);
        return buffer.array();
    }

    //Десериализация
    public void deserialize(byte[] data) throws IOException {
        if (data.length != DBConstants.HEADER_SIZE)
            throw new IOException("Неверный размер заголовка");
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte[] sigBytes = new byte[16];
        buffer.get(sigBytes);
        signature = new String(sigBytes, StandardCharsets.UTF_8).trim();
        version = buffer.getInt();
        recordCount = buffer.getInt();
        dataOffset = buffer.getLong();
        indexOffset = buffer.getLong();
        freeSpaceOffset = buffer.getLong();
    }

    //Валидация заголовка
    public boolean isValid() {
        return signature.equals(DBConstants.SIGNATURE) &&
                version == DBConstants.VERSION &&
                dataOffset >= DBConstants.HEADER_SIZE;
    }

    //Геттеры и сеттеры
    public int getRecordCount() { return recordCount; }
    public void setRecordCount(int count) { this.recordCount = count; }
    public long getDataOffset() { return dataOffset; }
}