package com.zuolema.app;

/** GitHub 仓库与更新检查地址。发布前把 OWNER 改成实际账号。 */
public final class Github {
    public static final String OWNER = "YOUR_GITHUB_USER";
    public static final String REPO = "zuolema";
    public static final String URL = "https://github.com/" + OWNER + "/" + REPO;
    public static final String LATEST_RELEASE_API =
            "https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases/latest";

    private Github() {}
}
