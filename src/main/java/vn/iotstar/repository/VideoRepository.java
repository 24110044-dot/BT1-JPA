package vn.iotstar.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.iotstar.entity.Video;

public interface VideoRepository extends JpaRepository<Video, String> {

    long countByCategory_CateId(Integer categoryId);

    @EntityGraph(attributePaths = "category")
    Page<Video> findByActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Video> findByActiveTrueAndCategory_CateId(Integer categoryId, Pageable pageable);

    @EntityGraph(attributePaths = { "category", "user" })
    List<Video> findTop10ByActiveTrueOrderByUploadedAtDescTitleAsc();

    @EntityGraph(attributePaths = { "category", "user" })
    Optional<Video> findByVideoIdAndActiveTrue(String videoId);

    @EntityGraph(attributePaths = { "category", "user" })
    Page<Video> findByUser_IdOrderByUploadedAtDesc(Integer userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    List<Video> findByUser_Id(Integer userId);

    boolean existsByVideoUrl(String videoUrl);

    boolean existsByVideoUrlAndVideoIdNot(String videoUrl, String videoId);

    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Video> findById(String videoId);

    @EntityGraph(attributePaths = "category")
    @Query("""
            select v from Video v
            where (:keyword is null
                or lower(v.title) like lower(concat('%', :keyword, '%'))
                or lower(v.videoId) like lower(concat('%', :keyword, '%')))
              and (:categoryId is null or v.category.cateId = :categoryId)
              and (:active is null or v.active = :active)
            """)
    Page<Video> searchAdmin(@Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            @Param("active") Boolean active,
            Pageable pageable);
}
