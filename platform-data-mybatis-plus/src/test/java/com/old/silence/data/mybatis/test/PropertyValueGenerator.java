package com.old.silence.data.mybatis.test;

import com.old.silence.core.test.data.RandomData;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Generator for property values based on type and column metadata
 * 
 * @author moryzang
 */
enum PropertyValueGenerator {
    BOOLEAN(Boolean.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            return RandomData.randomBoolean();
        }
    },
    BYTE(Byte.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            int digits = columnMetaData.getSize();
            int bound = digits >= 3 ? Byte.MAX_VALUE : (int) Math.pow(10, digits) - 1;
            bound = Math.min(bound, getUpperBoundForSqlType(columnMetaData));
            return (byte) (RandomData.randomPositiveInt(bound) + 1);
        }
    },
    SHORT(Short.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            int digits = columnMetaData.getSize();
            int bound = digits >= 5 ? Short.MAX_VALUE : (int) Math.pow(10, digits) - 1;
            bound = Math.min(bound, getUpperBoundForSqlType(columnMetaData));
            return (short) (RandomData.randomPositiveInt(bound) + 1);
        }
    },
    INTEGER(Integer.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            int digits = columnMetaData.getSize();
            int bound = digits >= 10 ? Integer.MAX_VALUE : (int) Math.pow(10, digits) - 1;
            bound = Math.min(bound, getUpperBoundForSqlType(columnMetaData));
            return RandomData.randomPositiveInt(bound) + 1;
        }
    },
    LONG(Long.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            int digits = columnMetaData.getSize();
            long bound = digits >= 19 ? Long.MAX_VALUE : (long) Math.pow(10, digits) - 1;
            bound = Math.min(bound, getUpperBoundLongForSqlType(columnMetaData));
            return RandomData.randomPositiveLong(bound) + 1;
        }
    },
    FLOAT(Float.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            int digits = columnMetaData.getSize();
            int decimalDigits = columnMetaData.getDecimalDigits();
            int integerDigits = digits - decimalDigits;
            long bound = integerDigits == 0 ? 1 : (long) Math.pow(10, integerDigits);
            return RandomData.randomPositiveFloat(bound);
        }
    },
    DOUBLE(Double.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            int digits = columnMetaData.getSize();
            int decimalDigits = columnMetaData.getDecimalDigits();
            int integerDigits = digits - decimalDigits;
            long bound = integerDigits == 0 ? 1 : (long) Math.pow(10, integerDigits);
            return RandomData.randomPositiveDouble(bound);
        }
    },
    BIG_INTEGER(BigInteger.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            long value = (Long) LONG.generate(propertyName, propertyType, columnMetaData);
            return BigInteger.valueOf(value);
        }
    },
    BIG_DECIMAL(BigDecimal.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            int digits = columnMetaData.getSize();
            int decimalDigits = columnMetaData.getDecimalDigits();
            int integerDigits = digits - decimalDigits;
            long bound = integerDigits == 0 ? 1 : (long) Math.pow(10, integerDigits);
            return RandomData.randomPositiveBigDecimal(bound, decimalDigits);
        }
    },
    STRING(String.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            return RandomData.randomName(StringUtils.capitalize(propertyName), Math.min(columnMetaData.getSize(), 10000));
        }
    },
    BYTE_ARRAY(byte[].class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            int size = columnMetaData.getSize();
            return RandomData.randomBytes(size);
        }
    },
    DATE(Date.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            return new Date();
        }
    },
    YEAR(Year.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            return Year.now();
        }
    },
    YEAR_MONTH(YearMonth.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            return YearMonth.now();
        }
    },
    MONTH_DAY(MonthDay.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            return MonthDay.now();
        }
    },
    LOCAL_DATE(LocalDate.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            return LocalDate.now();
        }
    },
    ZONED_DATE_TIME(ZonedDateTime.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            return ZonedDateTime.now();
        }
    },
    INSTANT(Instant.class) {
        @Override
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            return Instant.now();
        }
    },
    ENUM(Enum.class) {
        @Override
        boolean supports(Class<?> type) {
            return Enum.class.isAssignableFrom(type);
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData) {
            return RandomData.randomEnum((Class<Enum>) propertyType);
        }
    };

    private final Class<?> type;

    PropertyValueGenerator(Class<?> type) {
        this.type = type;
    }

    boolean supports(Class<?> type) {
        return type.equals(this.type);
    }

    int getUpperBoundForSqlType(ColumnMetaData columnMetaData) {
        long bound = getUpperBoundLongForSqlType(columnMetaData);
        return bound > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) bound;
    }

    long getUpperBoundLongForSqlType(ColumnMetaData columnMetaData) {
        boolean signed = columnMetaData.isSigned();
        switch (columnMetaData.getSqlType()) {
            case Types.TINYINT:
                return signed ? 127 : 255;
            case Types.SMALLINT:
                return signed ? 32767 : 65535;
            case Types.INTEGER:
                return signed ? Integer.MAX_VALUE : 4294967295L;
            case Types.BIGINT:
                return Long.MAX_VALUE;
            default:
                throw new IllegalArgumentException("Sql type: " + columnMetaData.getSqlType() + " is not numeric.");
        }
    }

    /**
     * Generate value for the property
     * 
     * @param propertyName property name
     * @param propertyType property type
     * @param columnMetaData column metadata
     * @return generated value
     */
    abstract Object generate(String propertyName, Class<?> propertyType, ColumnMetaData columnMetaData);
}
