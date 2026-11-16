package vn.iotstar.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import vn.iotstar.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUserNameIgnoreCase(String userName);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUserNameIgnoreCaseOrEmailIgnoreCase(String userName, String email);

    boolean existsByUserNameIgnoreCase(String userName);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    boolean existsByUserNameIgnoreCaseAndIdNot(String userName, Integer id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);

    boolean existsByPhoneAndIdNot(String phone, Integer id);

    Page<User> findByUserNameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String userName, String fullName, String email, Pageable pageable);

    long countByRoleid(Integer roleid);
}
