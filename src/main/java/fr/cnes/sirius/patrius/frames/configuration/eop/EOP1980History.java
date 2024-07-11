/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.util.Collection;

/**
 * This class holds Earth Orientation Parameters (IAU1980) data throughout a large time range.
 * 
 * @author Pascal Parraud
 */
public class EOP1980History extends AbstractEOPHistory {

    /** Serializable UID. */
    private static final long serialVersionUID = 3000752420705950441L;

    /**
     * Simple constructor.
     * 
     * @param interpMethod
     *        method to interpolate EOP data
     */
    public EOP1980History(final EOPInterpolators interpMethod) {
        super(interpMethod);
    }

    /**
     * Populates a {@link EOP1980History} instance from a collection of {@link EOP1980Entry}.
     * 
     * @param entries
     *        collection of {@link EOP1980Entry}
     * @param history
     *        instance to be populated
     */
    public static void fillHistory(final Collection<EOP1980Entry> entries, final EOP1980History history) {
        AbstractEOPHistory.fillHistory(entries, history);
    }

}
