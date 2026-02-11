package com.old.silence.data.mybatis.test;

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * @author moryzang
 */
class DataMyBatisTestContextBootstrapper extends SpringBootTestContextBootstrapper {

    @Override
    protected String[] getProperties(Class<?> testClass) {
        var dataMyBatisTest = TestContextAnnotationUtils.findMergedAnnotation(testClass, DataMyBatisTest.class);
        return dataMyBatisTest != null ? dataMyBatisTest.properties() : null;
    }
}
