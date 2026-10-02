package com.couplestory.util;

import java.util.List;

public final class PackageRank {
    private static final List<String> ORDER = List.of("FREE", "PLUS", "COUPLE", "PREMIUM");

    private PackageRank() {}

    public static int of(String code) {
        int i = ORDER.indexOf(code);
        return i < 0 ? 0 : i;
    }

    public static boolean covers(String userPlan, String requiredPackage) {
        return of(userPlan) >= of(requiredPackage);
    }
}
