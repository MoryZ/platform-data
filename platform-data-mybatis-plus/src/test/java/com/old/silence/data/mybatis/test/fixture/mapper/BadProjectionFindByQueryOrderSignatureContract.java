package com.old.silence.data.mybatis.test.fixture.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.old.silence.data.mybatis.test.fixture.projection.UserDto;

import java.util.List;

/**
 * Invalid mapper contract: findByQuery first argument must be Wrapper, not Page.
 */
public interface BadProjectionFindByQueryOrderSignatureContract {

    List<UserDto> findByQuery(Page<?> page, Class<UserDto> projectionType);
}
