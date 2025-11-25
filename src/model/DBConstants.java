package model;

public class DBConstants {
    public static final String SIGNATURE = "PRODDB1";
    public static final int VERSION = 1;
    public static final int HEADER_SIZE = 128;
    public static final int NAME_SIZE = 100;
    public static final int VOLUME_SIZE = 20;
    public static final int DESC_SIZE = 200;
    public static final int RECORD_SIZE = 4 + 8 + 4 + 4 + NAME_SIZE + VOLUME_SIZE + DESC_SIZE; // 340 байт

    // Коды ошибок
    public static final int SUCCESS = 0;
    public static final int ERROR_FILE_NOT_FOUND = 1;
    public static final int ERROR_INVALID_FORMAT = 2;
    public static final int ERROR_DUPLICATE_ID = 3;
    public static final int ERROR_RECORD_NOT_FOUND = 4;
}