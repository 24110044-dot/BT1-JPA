package vn.iotstar.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.entity.Category;
import vn.iotstar.entity.Product;
import vn.iotstar.repository.CategoryRepository;
import vn.iotstar.repository.ProductRepository;

@Service
public class VideoCategorySuggestionService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public VideoCategorySuggestionService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Category> suggest(String title, String description) {
        List<String> contentTokens = tokens((title == null ? "" : title) + " "
                + (description == null ? "" : description));
        if (contentTokens.isEmpty()) {
            return Optional.empty();
        }

        Map<Integer, CategoryScore> scores = new HashMap<>();
        for (Category category : categoryRepository.findAll()) {
            List<String> categoryTokens = tokens(category.getCateName());
            if (!categoryTokens.isEmpty() && containsSequence(contentTokens, categoryTokens)) {
                addScore(scores, category, 4 + Math.min(categoryTokens.size(), 4));
            }
        }

        for (Product product : productRepository.findByStatusTrue()) {
            Category category = product.getCategory();
            if (category == null || product.getProductName() == null) {
                continue;
            }
            int matchScore = productMatchScore(contentTokens, tokens(product.getProductName()));
            if (matchScore > 0) {
                addScore(scores, category, matchScore);
            }
        }

        List<CategoryScore> ranked = new ArrayList<>(scores.values());
        ranked.sort(Comparator.comparingInt(CategoryScore::score).reversed());
        if (ranked.isEmpty()) {
            return Optional.empty();
        }
        if (ranked.size() > 1 && ranked.get(0).score() - ranked.get(1).score() < 2) {
            return Optional.empty();
        }
        return Optional.of(ranked.get(0).category());
    }

    private int productMatchScore(List<String> contentTokens, List<String> productTokens) {
        if (productTokens.isEmpty()) {
            return 0;
        }
        if (containsSequence(contentTokens, productTokens)) {
            return 5 + Math.min(productTokens.size(), 4);
        }

        Set<String> content = new HashSet<>(contentTokens);
        Set<String> product = new HashSet<>(productTokens);
        long matchingTokens = product.stream().filter(content::contains).count();
        if (matchingTokens < 2 || matchingTokens * 2 < product.size()) {
            return 0;
        }
        return (int) matchingTokens * 2;
    }

    private boolean containsSequence(List<String> content, List<String> phrase) {
        if (phrase.size() > content.size()) {
            return false;
        }
        for (int start = 0; start <= content.size() - phrase.size(); start++) {
            if (content.subList(start, start + phrase.size()).equals(phrase)) {
                return true;
            }
        }
        return false;
    }

    private List<String> tokens(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
        return normalized.isEmpty() ? List.of() : List.of(normalized.split("\\s+"));
    }

    private void addScore(Map<Integer, CategoryScore> scores, Category category, int score) {
        scores.compute(category.getCateId(), (id, current) -> current == null
                ? new CategoryScore(category, score)
                : new CategoryScore(category, current.score() + score));
    }

    private record CategoryScore(Category category, int score) {
    }
}
