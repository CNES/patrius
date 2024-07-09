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
 */
package fr.cnes.sirius.patrius.tools.parallel;

/**
 * <p>
 * -TODO Class "abstract" (description in 2-3 sentences)
 * </p>
 * 
 * @see the references for this type - TODO
 * 
 * @author cardosop
 * 
 * @version $Id: TaskOneFactory.java 17915 2017-09-11 12:35:44Z bignon $
 * 
 * @since version when the file was created - TODO
 * 
 */
public class TaskOneFactory implements ParallelTaskFactory<TaskOne> {

    private int counter = 0;

    /**
     * TODO describe the changes with regard to the overriden method
     * 
     * @see fr.cnes.sirius.patrius.tools.parallel.ParallelTaskFactory#newInstance()
     */
    @Override
    public TaskOne newInstance() {
        return new TaskOne(++this.counter);
    }

    @Override
    public void reset() {
        this.counter = 0;
    }

}
