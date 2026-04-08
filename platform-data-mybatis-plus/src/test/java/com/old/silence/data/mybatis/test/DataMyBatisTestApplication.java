package com.old.silence.data.mybatis.test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.old.silence.data.mybatis.projection.EnableProjectionRepositories;

/**
 * Test application for @DataMyBatisTest and repository-oriented tests.
 * This is automatically discovered by Spring Boot's test context loader.
 */
@SpringBootApplication(scanBasePackages = {})
@EnableProjectionRepositories
public class DataMyBatisTestApplication {

}
