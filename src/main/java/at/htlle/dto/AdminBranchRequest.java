package at.htlle.dto;

public record AdminBranchRequest(
        Long restaurantId,
        String branchCode,
        String name,
        boolean defaultBranch
) {
}
