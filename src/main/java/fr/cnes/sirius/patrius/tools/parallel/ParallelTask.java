/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
* VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2095:15/05/2019:[PATRIUS] preparation au deploiement sur les depots centraux maven
 * END-HISTORY
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
 * @history created 21/08/12
 */
package fr.cnes.sirius.patrius.tools.parallel;

import java.util.concurrent.Callable;

/**
 * A ParallelTask instance is meant to be run once by the ParallelRunner
 * in a multithreaded context.
 * 
 * @author cardosop
 * 
 * @version $Id: ParallelTask.java 17578 2017-05-10 12:20:20Z bignon $
 * 
 * @since 1.2
 * 
 */
public interface ParallelTask extends Callable<ParallelResult> {

    /**
     * Returns a label identifying the task "class".
     * It's the same for all instances of the task.
     * 
     * @return the task label
     */
    String getTaskLabel();

    /**
     * Returns human-readable info on the status of the task.
     * Is intended to change depending on the current state of the task.
     * 
     * @return the status of the task.
     */
    String getTaskInfo();

    @Override
    ParallelResult call();

    /**
     * Asynchronous getter for the results.
     * Is meant to be called after call to call(), and call() has ended.
     * Otherwise behavior is unpredictable.
     * 
     * @return the same ParallelResult object already returned by call()
     */
    ParallelResult getResult();

}
