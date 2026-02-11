package com.old.silence.data.mybatis.test;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author moryzang
 */
@DataMyBatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public abstract class MyBatisMapperTests<T extends BaseMapper<S, ID>, S, ID> extends AbstractJdbcRepositoryTests<T S, ID> {
}
