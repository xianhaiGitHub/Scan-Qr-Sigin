package com.tony.admin.web.service.impl;

import com.tony.admin.web.mapper.SysUserMapper;
import com.tony.admin.web.model.SysUser;
import com.tony.admin.web.service.ISystemService;
import tk.mybatis.mapper.entity.Example;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 系统管理，安全相关实体的管理类,包括用户、角色、菜单.
 *
 * @author Guoqing
 */
@Service
public class SystemServiceImpl implements ISystemService {

    /**
     * 系统用户Mapper
     */
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Override
    public SysUser getUserByLoginName(String loginName) {
    	Example example = new Example(SysUser.class);
    	Example.Criteria criteria = example.createCriteria();
    	criteria.andEqualTo("loginName", loginName);
        List<SysUser> userList = sysUserMapper.selectByExample(example);
        
        if (userList.size() == 0) {
            return null;
        }

        return userList.get(0);
    }
}
