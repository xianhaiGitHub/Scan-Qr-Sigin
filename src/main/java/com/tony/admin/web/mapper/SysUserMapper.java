package com.tony.admin.web.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.tony.admin.web.model.SysUser;

/**
 * 用户DAO接口
 *
 * @author Guoqing.Lee
 */
@Mapper
public interface SysUserMapper extends tk.mybatis.mapper.common.Mapper<SysUser> {

}
