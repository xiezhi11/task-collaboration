package com.taskcollab.controller;

import com.taskcollab.common.Result;
import com.taskcollab.common.UserContext;
import com.taskcollab.entity.SysUser;
import com.taskcollab.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam String username) {
        Result<SysUser> userResult = sysUserService.getUserByUsername(username);
        if (userResult.getCode() != 200) {
            return Result.error("登录失败：" + userResult.getMessage());
        }

        SysUser user = userResult.getData();
        UserContext.setUserId(user.getId());
        UserContext.setUserName(user.getName());
        UserContext.setUserRole(user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("name", user.getName());
        result.put("role", user.getRole());

        return Result.success(result);
    }

    @GetMapping("/users")
    public Result<List<SysUser>> getAllUsers() {
        return sysUserService.getAllUsers();
    }

    @GetMapping("/current-user")
    public Result<Map<String, Object>> getCurrentUser() {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", UserContext.getUserId());
        result.put("name", UserContext.getUserName());
        result.put("role", UserContext.getUserRole());
        return Result.success(result);
    }
}
