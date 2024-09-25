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
 * @history creation 15/06/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3238:03/11/2022:[PATRIUS] Masquages par des corps celestes dans VisibilityFromStationDetector
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.io.Serializable;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Secondary spacecraft to be used in events detections. It is described by its assembly of parts and a propagator.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment the direct use of a not thread-safe Assembly makes this class
 *                      not thread-safe itself
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 */
public class SecondarySpacecraft implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 4244801907421842435L;

    /** the spacecraft's assembly */
    private final Assembly inAssembly;

    /** the spacecraft's propagator */
    private final Propagator inPropagator;

    /** the spacecraft's name */
    private final String inName;

    /**
     * Constructor for a secondary spacecraft to be used in events detections.
     * 
     * @param assembly
     *        the spacecraft's assembly
     * @param propagator
     *        the spacecraft's propagator
     * @param name
     *        the spacecraft's name
     */
    public SecondarySpacecraft(final Assembly assembly, final Propagator propagator, final String name) {
        this.inAssembly = assembly;
        this.inPropagator = propagator;
        this.inName = name;
    }

    /**
     * @return the spacecraft's assembly
     */
    public Assembly getAssembly() {
        return this.inAssembly;
    }

    /**
     * @return the spacecraft's name
     */
    public String getName() {
        return this.inName;
    }

    /**
     * @return the propagator
     */
    public Propagator getPropagator() {
        return this.inPropagator;
    }

    /**
     * Updates the assembly frames at a given date from the orbit and attitude
     * information provided by the propagator.
     * 
     * @param date
     *        the date
     * @throws PatriusException
     *         if some frames problem occurs
     */
    public void updateSpacecraftState(final AbsoluteDate date) throws PatriusException {
        final SpacecraftState state = this.inPropagator.propagate(date);
        this.inAssembly.updateMainPartFrame(state);
    }

}
