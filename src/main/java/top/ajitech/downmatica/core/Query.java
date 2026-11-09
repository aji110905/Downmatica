package top.ajitech.downmatica.core;

public class Query {
    private final int page;
    private final String keyword;

    private Query(int page, String keyword) {
        this.page = page;
        this.keyword = keyword;
    }

    public int getPage() {
        return page;
    }

    public String getKeyword() {
        return keyword;
    }

    public static Query of(int page, String keyword) {
        return new Query(page, keyword != null ? keyword : "");
    }
}
