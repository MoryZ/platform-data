package com.old.silence.data.mybatis.test;

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

import org.springframework.util.StringUtils;
import com.old.silence.core.test.data.RandomData;

/**
 * @author moryzang
 */
enum PropertyValueGenerator {
    BOOLEAN(Boolean.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            return RandomData.randomBoolean();
        }
    },
    BYTE(Byte.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            var digits = columnMetaData.getSize();
            var bound = digits >= 3 ? Byte.MAX_VALUE : (int) Math.pow(10, digits) - 1;
            bound = Math.min(bound, getUpperBoundForSqlType(columnMetaData));
            return RandomData.randomPositiveInt(bound) + 1;
        }
    }
    ,
    SHORT(Short.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            var digits = columnMetaData.getSize();
            var bound = digits >= 5 ? Short.MAX_VALUE : (int) Math.pow(10, digits) - 1;
            bound = Math.min(bound, getUpperBoundForSqlType(columnMetaData));
            return RandomData.randomPositiveInt(bound) + 1;
        }
    },
    INTEGER(Integer.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            var digits = columnMetaData.getSize();
            var bound = digits >= 10 ? Integer.MAX_VALUE : (int) Math.pow(10, digits) - 1;
            bound = Math.min(bound, getUpperBoundForSqlType(columnMetaData));
            return RandomData.randomPositiveInt(bound) + 1;
        }
    },
    LONG(Long.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            var digits = columnMetaData.getSize();
            var bound = digits >= 19 ? Long.MAX_VALUE : (long) Math.pow(10, digits) - 1;
            bound = Math.min(bound, getUpperBoundLongForSqlType(columnMetaData));
            return RandomData.randomPositiveLong(bound) + 1;
        }
    },
    FLOAT(Float.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            var digits = columnMetaData.getSize();
            var decimalDigits = columnMetaData.getDecimalDigits();
            var integerDigits = digits - decimalDigits;
            var bound = integerDigits == 0 ? 1 : (long) Math.pow(10, integerDigits);
            return RandomData.randomPositiveFloat(bound);
        }
    },
    DOUBLE(Double.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            var digits = columnMetaData.getSize();
            var decimalDigits = columnMetaData.getDecimalDigits();
            var integerDigits = digits - decimalDigits;
            var bound = integerDigits == 0 ? 1 : (long) Math.pow(10, integerDigits);
            return RandomData.randomPositiveDouble(bound);
        }
    },
    BIG_INTEGER(BigInteger.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            var value = (Long) LONG.generate(property, columnMetaData)
            return BigInteger.valueOf(value);
        }
    },
    BIG_DECIMAL(BigDecimal.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            var digits = columnMetaData.getSize();
            var decimalDigits = columnMetaData.getDecimalDigits();
            var integerDigits = digits - decimalDigits;
            var bound = integerDigits == 0 ? 1 : (long) Math.pow(10, integerDigits);
            return RandomData.randomPositiveBigDecimal(bound, decimalDigits);
        }
    },
    STRING(String.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            var propertyName = property.getName();
            return RandomData.randomName(StringUtils.capitalize(propertyName), Math.min(columnMetaData.getSize(), 10000));
        }
    },
    BYTE_ARRAY(byte[].class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            var size = columnMetaData.getSize();
            return RandomData.randomBytes(size);
        }
    },
    DATE(Date.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            return new Date();
        }
    },
    YEAR(Year.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            return Year.now();
        }
    },
    YEAR_MONTH(YearMonth.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            return YearMonth.now();
        }
    },
    MONTH_DAY(YearMonth.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            return MonthDay.now();
        }
    },
    LOCAL_DATE(LocalDate.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            return LocalDate.now();
        }
    },
    ZONED_DATE_TIME(ZonedDateTime.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            return ZonedDateTime.now();
        }
    },
    INSTANT(Instant.class) {

        @Override
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            return Instant.now();
        }
    },
    ENUM(Enum.class) {

        @Override
        boolean supports(Class<?> type) {
            return Enum.class.isAssignableFrom(type);
        }

        @Override
        @SuppressWarnings({"rawtype", "unchecked"})
        Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData) {
            return RandomData.randomEnum((Class<Enum>)property.getType());
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
        var bound = getUpperBoundLongForSqlType(columnMetaData);
        return bound > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) bound;
    }

    long getUpperBoundLongForSqlType(ColumnMetaData columnMetaData) {

        var signed = columnMetaData.isSigned();
        switch (columnMetaData.getSqlType()) {
            case Types.TINYINT:
                return signed ? 127 : 65535;
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

    abstract Object generate(JdbcPersistentProperty property, ColumnMetaData columnMetaData);
}
