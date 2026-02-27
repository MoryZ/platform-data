package com.old.silence.data.mybatis.projection;

import java.util.List;

/**
 * Invalid mapper contract: LIST mode without projectionType and without Class parameter.
 */
public interface BadProjectionMapperMissingProjectionTypeTest {

    List<TestUserProjection> findByQuery(TestUserQuery query);
}
