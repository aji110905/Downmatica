package top.ajitech.downmatica.api.impl;

import top.ajitech.downmatica.api.Schematic;
import top.ajitech.downmatica.api.SchematicAcquirer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

/**
 * "只能一次性取回全部条目"的源的通用实现。
 * <p>
 * 注意：本地过滤要求条目的{@link Schematic#getTitle()}等字段在构造时就绪、取值无副作用。
 */
public abstract class CachedListSchematicAcquirer implements SchematicAcquirer {
    private Collection<Schematic> all = null;
    private Collection<Schematic> filtered = null;
    private String keyword = "";
    private boolean delivered = false;

    /**
     * 一次性取回本源的条目。
     * <p>
     * 只会被调用一次，实现里可以放心地逐个构建完整的{@link Schematic}
     * @return 本源的全部条目
     */
    protected abstract Collection<Schematic> getSchematics();

    @Override
    public void setKeyword(String keyword) {
        if (!this.keyword.equals(keyword)) {
            this.keyword = keyword;
            this.filtered = null;
        }
        this.delivered = false;
    }

    @Override
    public int getTotalSchematic() {
        return visible().size();
    }

    @Override
    public Collection<Schematic> getNext() {
        Collection<Schematic> rel = visible();
        delivered = true;
        return rel;
    }

    @Override
    public boolean hasNext() {
        return !delivered;
    }

    private Collection<Schematic> visible() {
        if (filtered == null) {
            if (all == null) {
                all = getSchematics();
            }
            filtered = keyword.isEmpty() ? all : filter(all, keyword);
        }
        return filtered;
    }

    private ArrayList<Schematic> filter(Collection<Schematic> source, String keyword) {
        String needle = keyword.toLowerCase(Locale.ROOT);
        ArrayList<Schematic> rel = new ArrayList<>();
        for (Schematic schematic : source) {
            if (
                    contains(schematic.getSource(), needle) ||
                    contains(schematic.getTitle(), needle) ||
                    contains(schematic.getAuthor(), needle) ||
                    contains(schematic.getDescription(), needle)
            ) {
                rel.add(schematic);
            }
        }
        return rel;
    }

    private boolean contains(String text, String needle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
    }
}
