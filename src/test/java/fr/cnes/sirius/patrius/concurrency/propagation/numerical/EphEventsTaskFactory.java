/**
 * 
 * Copyright 2011-2022 CNES
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
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.concurrency.propagation.numerical;

import fr.cnes.sirius.patrius.tools.parallel.ParallelTaskFactory;

/**
 * Factory for ExtrapolTask.
 * 
 * @author cardosop
 * 
 * @version $Id: EphEventsTaskFactory.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class EphEventsTaskFactory implements ParallelTaskFactory<EphEventsTask> {

    /** Task counter. */
    private int counter = 0;

    /**
     * Constructor.
     */
    public EphEventsTaskFactory() {
    }

    @Override
    public EphEventsTask newInstance() {
        this.counter++;
        return new EphEventsTask(this.counter);
    }

    @Override
    public void reset() {
        this.counter = 0;
    }

}
