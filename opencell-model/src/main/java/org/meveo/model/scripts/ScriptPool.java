/**
 * 
 */
package org.meveo.model.scripts;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Type;

/**
 * Configuration of the script instances pool of an endpoint
 * 
 * @author ClementBareth
 */
@Embeddable
public class ScriptPool implements Serializable {

    private static final long serialVersionUID = -8015938405821343193L;

    /**
     * Use a pool of script instances
     */
    @Column(name = "use_pool")
    @Type(type = "numeric_boolean")
    private boolean usePool = false;

    /**
     * Minimum number of script instance in the pool
     */
    @Column(name = "min_pool")
    private Integer min;

    /**
     * Maximum number of script instance in the pool
     */
    @Column(name = "max_pool")
    private Integer max;

    /**
     * Maximum idle time before script get evicted (in seconds)
     */
    @Column(name = "max_idle_time_pool")
    private Integer maxIdleTime;

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
    public boolean isUsePool() {
        return usePool;
    }

    /**
     * @param usePool Use a pool of script instances
     */
    public void setUsePool(boolean usePool) {
        this.usePool = usePool;
    }
}