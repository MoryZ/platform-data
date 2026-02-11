package com.old.silence.data.mybatis.test;

import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.filter.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * @author moryzang
 */
public class DataMyJdbcTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<DataJdbcTest> {

    public DataMyJdbcTypeExcludeFilter(Class<?> testClass) {
        super(testClass);
    }
}
