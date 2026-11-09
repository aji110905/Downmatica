package top.ajitech.downmatica.api;

import top.ajitech.downmatica.core.SchematicAcquirerManager;

import java.util.Collection;

/**
 * 表示原理图获取器。
 * <p>
 * 管理器按下面的方式使用本接口：
 * <ol>
 *     <li>关键词变化时调用一次{@link #setKeyword(String)}</li>
 *     <li>每次查询先读{@link #getTotalSchematic()}算总页数</li>
 *     <li>再反复"先问{@link #hasNext()}，为真才调{@link #getNext()}"，直到长表够填满请求的那一页</li>
 * </ol>
 * 也就是说本接口是一个"批次迭代器"：{@link #getNext()}返回的批次会被原样追加到所有源拼起来的长表上；返回空批次或{@link #hasNext()}为false，管理器都会认为这个源当前不再交付并换下一个源。
 */
public interface SchematicAcquirer {
    /**
     * 设置当前关键词，空字符串表示不按关键词过滤。
     * <p>
     * 只在关键词变化时由管理器调用，所以实现要把关键词字段初始化为空字符串，不能假设这个方法一定会在读取之前被调用。
     * <p>
     * 实现应当在这里把"可交付"状态复位，让{@link #hasNext()}重新为真。
     *
     * @param keyword 关键词，不会为null
     */
    void setKeyword(String keyword);

    /**
     * 返回当前关键词下这个源一共有多少条原理图，用于计算总页数。
     * <p>
     * 这个方法可以是耗时的。
     *
     * @return 条目总数
     */
    int getTotalSchematic();

    /**
     * 交付下一批条目，会被原样追加到所有源拼起来的长表上。
     * <p>
     * 只有在{@link #hasNext()}返回true时才会被调用。
     * <p>
     * 注意：长表按"本批次一次性交付"来定位页码，所以本地过滤型实现应当在{@link #setKeyword(String)}里就把清单过滤好，而不是在这里临时缩小返回范围。
     *
     * @return 一批条目，不会为null
     */
    Collection<Schematic> getNext();

    /**
     * 是否还有可以交付的批次。
     * <p>
     * 语义是"还有没有东西要交给管理器"，不是"这个源有没有数据"：
     *
     * @return 是否还能继续交付
     */
    boolean hasNext();

    /**
     * 注册一个原理图获取器。
     * <p>
     * 推荐在入口点调用。
     * @param acquirer 原理图获取器
     */
    static void register(SchematicAcquirer acquirer){
        SchematicAcquirerManager.INSTANCE.register(acquirer);
    }
}

