package com.old.silence.data.mybatis.test;

/**
 * Column metadata from database
 * 
 * @author moryzang
 */
public class ColumnMetaData {

    private final String columnName;

    private final int sqlType;

    private final int size;

    private final int decimalDigits;

    private final boolean nullable;

    private final boolean signed;

    public ColumnMetaData(String columnName, int sqlType, int size, int decimalDigits, boolean nullable, boolean signed) {
        this.columnName = columnName;
        this.sqlType = sqlType;
        this.size = size;
        this.decimalDigits = decimalDigits;
        this.nullable = nullable;
        this.signed = signed;
    }

    public String getColumnName() {
        return columnName;
    }

    public int getSqlType() {
        return sqlType;
    }

    public int getSize() {
        return size;
    }

    public int getDecimalDigits() {
        return decimalDigits;
    }

    public boolean isNullable() {
        return nullable;
    }

    public boolean isSigned() {
        return signed;
    }
}
