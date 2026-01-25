package at.htlle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import at.htlle.repository.RewardRedemptionRepository;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RewardRedemptionCodeGeneratorTest {

    @Test
    void generatesUniqueReadableCodes() {
        RewardRedemptionRepository repository = mock(RewardRedemptionRepository.class);
        when(repository.existsByRedemptionCode(anyString())).thenReturn(false);
        RewardRedemptionCodeGenerator generator = new RewardRedemptionCodeGenerator(repository);

        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String code = generator.generateUniqueCode();
            assertThat(code).matches("^[A-HJ-NP-Z2-9]{6}$");
            assertThat(codes.add(code)).isTrue();
        }
    }
}
