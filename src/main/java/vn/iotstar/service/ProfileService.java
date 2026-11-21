package vn.iotstar.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import vn.iotstar.config.CloudinaryService;
import vn.iotstar.dto.ProfileUpdateForm;
import vn.iotstar.entity.User;
import vn.iotstar.exception.DuplicateUserFieldException;
import vn.iotstar.exception.UserNotFoundException;
import vn.iotstar.repository.UserRepository;

@Service
@Transactional
public class ProfileService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public ProfileService(UserRepository userRepository, CloudinaryService cloudinaryService) {
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional(readOnly = true)
    public User getProfile(Integer userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    public User updateProfile(Integer userId, ProfileUpdateForm form) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        String phone = form.getPhone().trim();
        if (userRepository.existsByPhoneAndIdNot(phone, userId)) {
            throw new DuplicateUserFieldException("phone", "Số điện thoại đã được sử dụng.");
        }
        user.setFullName(form.getFullName().trim());
        user.setPhone(phone);
        return userRepository.save(user);
    }

    public User updateAvatar(Integer userId, MultipartFile file) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        String oldAvatar = user.getAvatar();
        String newAvatar = cloudinaryService.uploadAvatar(file);
        user.setAvatar(newAvatar);
        User saved = userRepository.saveAndFlush(user);
        if (oldAvatar != null && !oldAvatar.equals(newAvatar)) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cloudinaryService.deleteAvatarIfManaged(oldAvatar);
                }
            });
        }
        return saved;
    }
}
