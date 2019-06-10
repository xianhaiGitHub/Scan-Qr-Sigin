package com.tony.admin.web.service;

import com.tony.admin.web.model.SysUser;

/**
 * 系统管理，安全相关实体的管理类,包括用户、角色、菜单.
 *
 * @author Guoqing
 */
public interface ISystemService {
	
	/**
     * 根据登录名获取用户
     *
     * @param loginName 登录名
     * @return SysUser user by login name
     */
    SysUser getUserByLoginName(String loginName);

}
