/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
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
package fr.cnes.sirius.patrius.concurrency.propagation.numerical;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.tools.parallel.ParallelResult;

/**
 * Results, as a list of EvtPair list.
 * 
 * @author cardosop
 * 
 * @version $Id: EphEventsResult.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class EphEventsResult implements ParallelResult {

    /**
     * Event list
     */
    final List<EvtPair> resList = new ArrayList<EvtPair>();

    /**
     * Event as a name and a date.
     * 
     * @author cardosop
     * 
     * @version $Id: EphEventsResult.java 17911 2017-09-11 12:02:31Z bignon $
     * 
     * @since 1.2
     */
    private final class EvtPair {

        /** Event name. */
        private final String evtName;

        /** Event date. */
        private final AbsoluteDate evtDate;

        /**
         * Constructor
         * 
         * @param eventName
         *        : event name
         * @param eventDate
         *        : event date
         */
        private EvtPair(final String eventName, final AbsoluteDate eventDate) {
            this.evtName = eventName;
            this.evtDate = eventDate;
        }

        /**
         * @return the evtName
         */
        private String getEvtName() {
            return this.evtName;
        }

        /**
         * @return the evtDate
         */
        private AbsoluteDate getEvtDate() {
            return this.evtDate;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == null) {
                return false;
            }
            final boolean rez = false;
            if (o instanceof EvtPair) {
                return (this.getEvtName().equals(((EvtPair) o).getEvtName()) && this.getEvtDate().equals(
                    ((EvtPair) o).getEvtDate()));
            }
            return rez;
        }
    }

    /**
     * Adds an event to the list.
     * 
     * @param eName
     *        : event name
     * @param eDate
     *        ; event date
     */
    public void addEvt(final String eName, final AbsoluteDate eDate) {
        this.resList.add(new EvtPair(eName, eDate));
    }

    @Override
    public double[][] getDataAsArray() {
        // Not needed
        return null;
    }

    @Override
    public boolean resultEquals(final ParallelResult other) {
        boolean rez = false;
        if (other instanceof EphEventsResult) {
            final EphEventsResult eo = (EphEventsResult) other;
            if (eo.resList.size() == this.resList.size()) {
                rez = true;
                for (int i = 0; i < this.resList.size(); i++) {
                    if (!eo.resList.get(i).equals(this.resList.get(i))) {
                        rez = false;
                        break;
                    }
                }
            }
        }
        return rez;
    }
}
