package com.taskcollab.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taskcollab.common.Result;
import com.taskcollab.entity.SysUser;
import com.taskcollab.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {

    public Result<SysUser> getUserByUsername(String username) {
        SysUser user = lambdaQuery().eq(SysUser::getUsername, username).one();
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    public Result<List<SysUser>> getAllUsers() {
        List<SysUser> users = list();
        return Result.success(users);
    }

    public Result<List<SysUser>> getUsersByRole(String role) {
        List<SysUser> users = lambdaQuery().eq(SysUser::getRole, role).list();
        return Result.success(users);
    }
}
