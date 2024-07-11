/**
 * 
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 * @history created 23/08/12
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2095:15/05/2019:[PATRIUS] preparation au deploiement sur les depots centraux maven
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.tools.parallel;

/**
 * Simple, abstract implementation for a ParallelTask.
 * Serves as a base for generic, simple implementations of ParallelTask,
 * or as a starting example for other implementations. Provides simple implementations
 * for {@link #getTaskLabel()} and {@link #getTaskInfo()}, which may be enough for most cases.
 * The developer extending this class only needs to provide
 * the {@link #callImpl()} method.
 * 
 * @concurrency unknown since it's abstract
 * 
 * @author cardosop
 * 
 * @version $Id: AbstractSimpleParallelTaskImpl.java 17578 2017-05-10 12:20:20Z bignon $
 * 
 * @since 1.2
 * 
 */
public abstract class AbstractSimpleParallelTaskImpl implements ParallelTask {

    /** Instance id. */
    private final int id;

    /** Calling thread name. */
    private String callThreadName;

    /** Result copy. */
    private ParallelResult result;

    /**
     * Constructor with instance id parameter.
     * 
     * @param iid
     *        the instance id, an integer. Identifies the task instance
     *        in the getTaskInfo string, has otherwise no use.
     */
    protected AbstractSimpleParallelTaskImpl(final int iid) {
        this.id = iid;
    }

    @Override
    public String getTaskLabel() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getTaskInfo() {
        return this.getTaskLabel() + " n°" + this.getId() + " in thread " + this.getCallThreadName();
    }

    @Override
    public ParallelResult call() {
        // Updates the calling thread name
        this.setCallThreadName(Thread.currentThread().getName());
        // The callImpl is from the implementing class,
        // and provides the result...
        this.setResult(this.callImpl());
        // ...which is returned.
        return this.getResult();
    }

    /**
     * Call implementation to be provided by the implementing class.
     * The implementation only has to handle the task in itself, and
     * returning the result (the result is saved by the base class).
     * 
     * @return a ParallelResult instance
     */
    protected abstract ParallelResult callImpl();

    @Override
    public ParallelResult getResult() {
        return this.result;
    }

    /**
     * @return the id
     */
    public int getId() {
        return this.id;
    }

    /**
     * Setter for the thread name.
     * 
     * @param callThName
     *        the callThreadName to set
     */
    private void setCallThreadName(final String callThName) {
        this.callThreadName = callThName;
    }

    /**
     * @return the callThreadName
     */
    protected String getCallThreadName() {
        return this.callThreadName;
    }

    /**
     * Setter for the ParallelResult.
     * 
     * @param res
     *        the result to set
     */
    private void setResult(final ParallelResult res) {
        this.result = res;
    }

}
