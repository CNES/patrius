/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
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
 * @history created 15/03/12
 */
package fr.cnes.sirius.patrius.events.postprocessing;

import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.Phenomenon;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * @description <p>
 *              This class is a post processing criterion that merges two different timelines.
 *              </p>
 * 
 * @concurrency immutable
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public class MergeTimelines implements PostProcessing {

    /**
     * The timeline to add.
     */
    private final Timeline listToAdd;

    /**
     * Constructor
     * 
     * @description build an instance of MergeTimelines
     * 
     * @param list
     *        : timeline to be merged to the other timeline
     */
    public MergeTimelines(final Timeline list) {
        this.listToAdd = list;
    }

    /** {@inheritDoc} */
    @Override
    public void applyTo(final Timeline list) {
        if (list.getIntervalOfValidity().equals(this.listToAdd.getIntervalOfValidity())) {
            for (final CodedEvent eventToAdd : this.listToAdd.getCodedEventsList()) {
                // add all the new events to the list:
                list.addCodedEvent(eventToAdd);
            }
            for (final Phenomenon phenomenonToAdd : this.listToAdd.getPhenomenaList()) {
                // add all the new phenomena to the list:
                list.addPhenomenon(phenomenonToAdd);
            }
        } else {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.INVALID_INTERVAL_OF_VALIDITY);
        }
    }
}