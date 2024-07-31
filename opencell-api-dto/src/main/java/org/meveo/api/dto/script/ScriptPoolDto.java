package org.meveo.api.dto.script;

import java.io.Serializable;

import org.meveo.model.scripts.ScriptPool;

/**
 * Configuration of the script instances pool of an endpoint
 * 
 * @author Andrius Karpavicius
 */
public class ScriptPoolDto implements Serializable {

    private static final long serialVersionUID = 2073391848114421869L;

    /**
     * Use a pool of script instances
     */
    private Boolean usePool;

    /**
     * Minimum number of script instance in the pool
     */
    private Integer min;

    /**
     * Maximum number of script instance in the pool
     */
    private Integer max;

    /**
     * Maximum idle time before script get evicted (in seconds)
     */
    private Integer maxIdleTime;

    public ScriptPoolDto() {
    }

    public ScriptPoolDto(ScriptPool scriptPool) {
        usePool = scriptPool.isUsePool();
        min = scriptPool.getMin();
        max = scriptPool.getMax();
        maxIdleTime = scriptPool.getMaxIdleTime();
    }

    /**
     * @return Minimum number of script instance in the pool
     */
    public Integer getMin() {
        return min;
    }

    /**
     * @param min Minimum number of script instance in the pool
     */
    public void setMin(Integer min) {
        this.min = min;
    }

    /**
     * @return Maximum number of script instance in the pool
     */
    public Integer getMax() {
        return max;
    }

    /**
     * @param max Maximum number of script instance in the pool
     */
    public void setMax(Integer max) {
        this.max = max;
    }

    /**
     * @return Maximum idle time before script get evicted (in seconds)
     */
    public Integer getMaxIdleTime() {
        return maxIdleTime;
    }

    /**
     * @param maxIdle Maximum idle time before script get evicted (in seconds)
     */
    public void setMaxIdleTime(Integer maxIdle) {
        this.maxIdleTime = maxIdle;
    }

    /**
     * @return Use a pool of script instances
     */
    public Boolean getUsePool() {
        return usePool;
    }

    /**
     * @param usePool Use a pool of script instances
     */
    public void setUsePool(Boolean usePool) {
        this.usePool = usePool;
    }
}