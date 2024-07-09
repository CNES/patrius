/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
* VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2095:15/05/2019:[PATRIUS] preparation au deploiement sur les depots centraux maven
 * END-HISTORY
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
 */
package fr.cnes.sirius.patrius.tools.parallel;

/**
 * Task one.
 *
 * @author cardosop
 * 
 * @version $Id: TaskOne.java 17915 2017-09-11 12:35:44Z bignon $
 */
public class TaskOne extends AbstractSimpleParallelTaskImpl {

    /** Useless computation result. */
    public double pointlessSin = 0.;

    /**
     * Constructor with id.
     * 
     * @param iid the id
     */
    public TaskOne(final int iid) {
        super(iid);
    }

    @Override
    protected ParallelResult callImpl() {
        System.out.println("BEGIN : " + this.getTaskInfo());
        // Fixed duration
        final long loops = 100000;
        for (int i = 0; i < loops; i++) {
            // Compute pointless sin
            this.pointlessSin = Math.sin(Math.random());
        }
        final ParallelResult rez = new ResultOne(1, 2, 3, 4);
        System.out.println("-END- : " + this.getTaskInfo());
        return rez;
    }

}
