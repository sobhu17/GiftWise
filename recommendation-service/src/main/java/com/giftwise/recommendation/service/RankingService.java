package com.giftwise.recommendation.service;

import com.giftwise.recommendation.dto.ProductResult;
import com.giftwise.recommendation.dto.RecipientProfile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class RankingService {
    private record ScoredProduct(ProductResult product, int score) {}

    /**
     * Re-rank product-service's semantically-ordered results using business-rule bonuses, then
     * return the full re-sorted list — no separate top-N cap, since
     * {@code giftwise.product.search-limit} already bounds how many candidates arrive here.
     * <p>
     * Each product's position in {@code products} becomes its base score (first = highest),
     * since that position already reflects semantic similarity to the recipient profile;
     * occasion, price-fit, and interest/category bonuses are added on top before the final sort.
     *
     * @param products : products already ordered by semantic similarity, nearest first
     * @param profile  : the recipient profile the bonuses are scored against
     * @return the same products, re-ordered by combined semantic + business-rule score,
     * highest first
     */
    public List<ProductResult> rank(List<ProductResult> products, RecipientProfile profile) {
        int total = products.size();

        return IntStream.range(0, total)
                .mapToObj(position -> {
                    ProductResult product = products.get(position);
                    int score = (total - position);
                    score += occasionBonus(product, profile);
                    score += priceBonus(product, profile);
                    score += interestBonus(product, profile);
                    return new ScoredProduct(product, score);
                })
                .sorted(Comparator.comparingInt(ScoredProduct::score).reversed())
                .map(ScoredProduct::product)
                .toList();
    }


    /**
     * Bonus for an exact, case-insensitive match between the profile's occasion and the
     * product's occasion. Returns 0, not a penalty, when either side hasn't specified one —
     * an unset occasion is "no signal," not a mismatch.
     */
    private int occasionBonus(ProductResult product, RecipientProfile recipientProfile) {
        if (recipientProfile.getOccasion() == null || product.getOccasion() == null) return 0;
        return recipientProfile.getOccasion().equalsIgnoreCase(product.getOccasion()) ? 3 : 0;
    }

    /**
     * Bonus when the product's price falls within the profile's min/max budget. Any bound
     * left unset on the profile counts as satisfied — an unset budget shouldn't penalize any
     * product, and since that awards the identical bonus to every product in that case, it
     * has no effect on the final relative ranking.
     */
    private int priceBonus(ProductResult product, RecipientProfile recipientProfile) {
        BigDecimal price = product.getPrice();
        if (price == null) return 0;

        boolean aboveMin = recipientProfile.getMinPrice() == null
                || price.compareTo(recipientProfile.getMinPrice()) >= 0;
        boolean belowMax = recipientProfile.getMaxPrice() == null
                || price.compareTo(recipientProfile.getMaxPrice()) <= 0;

        return (aboveMin && belowMax) ? 2 : 0;
    }

    /**
     * Bonus when any of the recipient's interests matches the product's category,
     * case-insensitively. Returns 0 if the profile has no interests yet or the product has no
     * category, rather than treating either as a mismatch.
     */
    private int interestBonus(ProductResult product, RecipientProfile profile) {
        if (profile.getInterests() == null || profile.getInterests().isEmpty()) return 0;
        if (product.getCategory() == null) return 0;

        return profile.getInterests().stream()
                .anyMatch(interest -> interest.equalsIgnoreCase(product.getCategory())) ? 2 : 0;
    }
}
