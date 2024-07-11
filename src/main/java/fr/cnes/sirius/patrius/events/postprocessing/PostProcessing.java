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
 * created 13/03/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.postprocessing;

/**
 * @description
 *              <p>
 *              This interface handles post processings on a timeline.
 *              </p>
 * 
 * @author Julie Anton
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public interface PostProcessing {

    /**
     * @description
     *              This function describes one specific processing that has to be performed on the timeline.
     * 
     * @param list
     *        : the timeline that has to be processed
     */
    void applyTo(final Timeline list);
}
