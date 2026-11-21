package vn.iotstar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import vn.iotstar.config.CloudinaryService;
import vn.iotstar.dto.ProfileUpdateForm;
import vn.iotstar.entity.User;
import vn.iotstar.exception.DuplicateUserFieldException;
import vn.iotstar.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void updateProfileChangesOnlyAuthenticatedUserFields() {
        User user = new User();
        user.setId(4);
        user.setFullName("Tên cũ");
        user.setPhone("0901111111");
        ProfileUpdateForm form = new ProfileUpdateForm();
        form.setFullName("Tên mới");
        form.setPhone("0902222222");
        when(userRepository.findById(4)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("0902222222", 4)).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        User updated = profileService.updateProfile(4, form);

        assertEquals("Tên mới", updated.getFullName());
        assertEquals("0902222222", updated.getPhone());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfileRejectsPhoneUsedByAnotherUser() {
        User user = new User();
        user.setId(4);
        ProfileUpdateForm form = new ProfileUpdateForm();
        form.setFullName("Tên mới");
        form.setPhone("0902222222");
        when(userRepository.findById(4)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("0902222222", 4)).thenReturn(true);

        assertThrows(DuplicateUserFieldException.class, () -> profileService.updateProfile(4, form));
    }

    @Test
    void updateAvatarStoresCloudinaryUrlAfterUpload() {
        User user = new User();
        user.setId(4);
        user.setAvatar("https://old-avatar");
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(userRepository.findById(4)).thenReturn(Optional.of(user));
        when(cloudinaryService.uploadAvatar(file)).thenReturn("https://res.cloudinary.com/demo/image/upload/new-avatar.png");
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        TransactionSynchronizationManager.initSynchronization();
        try {
            User updated = profileService.updateAvatar(4, file);

            assertEquals("https://res.cloudinary.com/demo/image/upload/new-avatar.png", updated.getAvatar());
            verify(cloudinaryService, org.mockito.Mockito.never()).deleteAvatarIfManaged("https://old-avatar");
            TransactionSynchronizationManager.getSynchronizations().forEach(
                    synchronization -> synchronization.afterCommit());
            verify(cloudinaryService).deleteAvatarIfManaged("https://old-avatar");
            verify(userRepository).saveAndFlush(any(User.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
