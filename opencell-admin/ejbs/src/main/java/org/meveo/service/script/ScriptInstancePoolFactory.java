/**
 * 
 */
package org.meveo.service.script;

import java.util.function.Supplier;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.meveo.commons.utils.ReflectionUtils;

/**
 * Factory for creating script instances for a pool
 */
public class ScriptInstancePoolFactory implements PooledObjectFactory<ScriptInterface> {

    /**
     * A function that returns a new instance of the script
     */
    private Supplier<ScriptInterface> functionToGetInstance;

    /**
     * Constructor
     * 
     * @param functionToGetInstance A function that returns a new instance of the script
     */
    public ScriptInstancePoolFactory(Supplier<ScriptInterface> functionToGetInstance) {
        this.functionToGetInstance = functionToGetInstance;
    }

    @Override
    public void activateObject(PooledObject<ScriptInterface> p) throws Exception {
        p.getObject().resetState();
    }

    @Override
    public void destroyObject(PooledObject<ScriptInterface> p) throws Exception {
        p.getObject().cancel();
        p.getObject().terminate(null);
//		instance.destroy(p.getObject());
    }

    @Override
    public PooledObject<ScriptInterface> makeObject() throws Exception {

        ScriptInterface scriptInterface = functionToGetInstance.get();
        scriptInterface.init(null);
        return new DefaultPooledObject<ScriptInterface>(scriptInterface);
    }

    @Override
    public void passivateObject(PooledObject<ScriptInterface> p) throws Exception {
        ReflectionUtils.resetSetterValues(p.getObject());

    }

    @Override
    public boolean validateObject(PooledObject<ScriptInterface> p) {
        return true;
    }
}