package at.htlle.service;

import at.htlle.repository.RewardRedemptionRepository;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class RewardRedemptionCodeGenerator {

    private static final char[] ALLOWED = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 20;

    private final SecureRandom secureRandom = new SecureRandom();
    private final RewardRedemptionRepository rewardRedemptionRepository;

    public RewardRedemptionCodeGenerator(RewardRedemptionRepository rewardRedemptionRepository) {
        this.rewardRedemptionRepository = rewardRedemptionRepository;
    }

    public String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = generateCode();
            if (!rewardRedemptionRepository.existsByRedemptionCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate unique redemption code");
    }

    String generateCode() {
        char[] chars = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            chars[i] = ALLOWED[secureRandom.nextInt(ALLOWED.length)];
        }
        return new String(chars);
    }
}
