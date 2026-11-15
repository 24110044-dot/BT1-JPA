package vn.iotstar.service.impl;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.dto.AdminUserForm;
import vn.iotstar.dto.RegisterForm;
import vn.iotstar.entity.User;
import vn.iotstar.exception.DuplicateUserFieldException;
import vn.iotstar.exception.UserNotFoundException;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.IUserService;

@Service
@Transactional
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(RegisterForm form) {
        validateUnique(form.getUserName(), form.getEmail(), form.getPhone(), null);
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }
        User user = new User();
        applyCommon(user, form.getUserName(), form.getEmail(), form.getFullName(), form.getPhone(), null);
        user.setRoleid(5);
        user.setAvatar("/images/avatar.png");
        user.setPassWord(passwordEncoder.encode(form.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public User createByAdmin(AdminUserForm form) {
        validateUnique(form.getUserName(), form.getEmail(), form.getPhone(), null);
        validateAdminRole(form.getRoleid());
        if (form.getPassword() == null || form.getPassword().length() < 8 || form.getPassword().length() > 72) {
            throw new IllegalArgumentException("Mật khẩu tài khoản mới phải từ 8 đến 72 ký tự.");
        }
        User user = new User();
        applyCommon(user, form.getUserName(), form.getEmail(), form.getFullName(), form.getPhone(), form.getAvatar());
        user.setRoleid(form.getRoleid());
        user.setPassWord(passwordEncoder.encode(form.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public User updateByAdmin(Integer id, AdminUserForm form, Integer currentUserId) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        validateUnique(form.getUserName(), form.getEmail(), form.getPhone(), id);
        validateAdminRole(form.getRoleid());
        if (id.equals(currentUserId) && !user.getRoleid().equals(form.getRoleid())) {
            throw new IllegalArgumentException("Không thể tự thay đổi vai trò của tài khoản đang đăng nhập.");
        }
        if (Integer.valueOf(1).equals(user.getRoleid()) && !Integer.valueOf(1).equals(form.getRoleid())
                && userRepository.countByRoleid(1) <= 1) {
            throw new IllegalArgumentException("Không thể hạ quyền Admin cuối cùng.");
        }
        applyCommon(user, form.getUserName(), form.getEmail(), form.getFullName(), form.getPhone(), form.getAvatar());
        user.setRoleid(form.getRoleid());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            if (form.getPassword().length() < 8 || form.getPassword().length() > 72) {
                throw new IllegalArgumentException("Mật khẩu phải từ 8 đến 72 ký tự.");
            }
            user.setPassWord(passwordEncoder.encode(form.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    public void deleteByAdmin(Integer id, Integer currentUserId) {
        if (id.equals(currentUserId)) {
            throw new IllegalArgumentException("Không thể xóa tài khoản đang đăng nhập.");
        }
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        if (Integer.valueOf(1).equals(user.getRoleid()) && userRepository.countByRoleid(1) <= 1) {
            throw new IllegalArgumentException("Không thể xóa Admin cuối cùng.");
        }
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> search(String keyword, int page, int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Tham số phân trang không hợp lệ.");
        }
        String query = keyword == null ? "" : keyword.trim();
        PageRequest pageable = PageRequest.of(page, pageSize);
        if (query.isEmpty()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByUserNameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                query, query, query, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return userRepository.count();
    }

    private void validateUnique(String username, String email, String phone, Integer id) {
        if (id == null) {
            if (userRepository.existsByUserNameIgnoreCase(username.trim())) {
                throw new DuplicateUserFieldException("userName", "Username đã được sử dụng.");
            }
            if (userRepository.existsByEmailIgnoreCase(email.trim())) {
                throw new DuplicateUserFieldException("email", "Email đã được sử dụng.");
            }
            if (userRepository.existsByPhone(phone.trim())) {
                throw new DuplicateUserFieldException("phone", "Số điện thoại đã được sử dụng.");
            }
        } else {
            if (userRepository.existsByUserNameIgnoreCaseAndIdNot(username.trim(), id)) {
                throw new DuplicateUserFieldException("userName", "Username đã được sử dụng.");
            }
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(email.trim(), id)) {
                throw new DuplicateUserFieldException("email", "Email đã được sử dụng.");
            }
            if (userRepository.existsByPhoneAndIdNot(phone.trim(), id)) {
                throw new DuplicateUserFieldException("phone", "Số điện thoại đã được sử dụng.");
            }
        }
    }

    private void validateAdminRole(Integer roleid) {
        if (roleid == null || !java.util.Set.of(1, 2, 5).contains(roleid)) {
            throw new IllegalArgumentException("Role phải là Admin (1), Manager (2) hoặc User (5).");
        }
    }

    private void applyCommon(User user, String username, String email, String fullName, String phone, String avatar) {
        user.setUserName(username.trim());
        user.setEmail(email.trim().toLowerCase(java.util.Locale.ROOT));
        user.setFullName(fullName.trim());
        user.setPhone(phone.trim());
        if (avatar != null && !avatar.isBlank()) {
            user.setAvatar(avatar.trim());
        } else if (user.getAvatar() == null) {
            user.setAvatar("/images/avatar.png");
        }
    }
}
