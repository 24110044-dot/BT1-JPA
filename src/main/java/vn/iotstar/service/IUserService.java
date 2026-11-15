package vn.iotstar.service;

import java.util.Optional;

import org.springframework.data.domain.Page;

import vn.iotstar.dto.AdminUserForm;
import vn.iotstar.dto.RegisterForm;
import vn.iotstar.entity.User;

public interface IUserService {

    User register(RegisterForm form);

    User createByAdmin(AdminUserForm form);

    User updateByAdmin(Integer id, AdminUserForm form, Integer currentUserId);

    void deleteByAdmin(Integer id, Integer currentUserId);

    Optional<User> findById(Integer id);

    Page<User> search(String keyword, int page, int pageSize);

    long count();
}
