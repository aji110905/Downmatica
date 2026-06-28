package aji.downmatica.api;

import aji.downmatica.core.SchematicAcquirerManager;

import java.util.Collection;

/**
 * 表示原理图获取器。
 */
@FunctionalInterface
public interface SchematicAcquirer {
    /**
     * 获取原理图。
     * <p>
     * 这个方法可以是耗时的。
     * @return 原理图
     */
    Collection<Schematic> getSchematics();

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

