package at.htlle.dto;

import at.htlle.entity.Reward;

public record AdminRewardRequest(
        Long restaurantId,
        String rewardCode,
        String name,
        String description,
        Integer costPoints,
        Reward.RewardType rewardType,
        boolean active
) {
}
