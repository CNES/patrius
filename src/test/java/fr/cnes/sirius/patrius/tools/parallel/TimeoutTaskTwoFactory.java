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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.3:DM:DM-2095:15/05/2019:[PATRIUS] preparation au deploiement sur les depots centraux maven
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.tools.parallel;

/**
 * TimeoutTaskTwoFactory.
 */
public class TimeoutTaskTwoFactory implements ParallelTaskFactory<TimeoutTaskTwo> {

    private int counter = 0;

    /**
     * 
     * @see fr.cnes.sirius.patrius.tools.parallel.ParallelTaskFactory#newInstance()
     */
    @Override
    public TimeoutTaskTwo newInstance() {
        return new TimeoutTaskTwo(++this.counter);
    }

    @Override
    public void reset() {
        this.counter = 0;
    }

}
