package at.htlle.dto;

public record AdminBranchResponse(
        Long id,
        Long restaurantId,
        String branchCode,
        String name,
        boolean defaultBranch
) {
}
