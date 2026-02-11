package com.old.silence.data.mybatis.test;

import org.springframework.boot.test.autoconfigure.filter.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * @author moryzang
 */
public class DataMyBatisTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<DataMyBatisTest> {

    public DataMyBatisTypeExcludeFilter(Class<?> testClass) {
        super(testClass);
    }
}
