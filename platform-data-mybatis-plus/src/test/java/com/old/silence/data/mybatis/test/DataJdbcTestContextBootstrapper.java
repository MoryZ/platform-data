package com.old.silence.data.mybatis.test;

import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * @author moryzang
 */
class DataJdbcTestContextBootstrapper extends SpringBootTestContextBootstrapper {

    @Override
    protected String[] getProperties(Class<?> testClass) {
        var dataMyBatisTest = TestContextAnnotationUtils.findMergedAnnotation(testClass, DataJdbcTest.class);
        return dataMyBatisTest != null ? dataMyBatisTest.properties() : null;
    }
}
