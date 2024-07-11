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
 * @history created 18/03/2015
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3172:10/05/2022:[PATRIUS] Ajout d'un throws PatriusException a la methode init de l'interface EDet
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:454:24/11/2015:Add method shouldBeRemoved() to manage detector suppression
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events.multi;

import java.util.Map;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This class is copied from {@link fr.cnes.sirius.patrius.propagation.events.AbstractDetector AbstractDetector} and
 * adapted to multi propagation.
 * </p>
 * 
 * <p>
 * Common parts shared by several events finders. A default implementation of most of the methods of
 * {@link MultiEventDetector MultiEventDetector interface}. Make it easier to create a new detector.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.propagation.MultiPropagator#addEventDetector(MultiEventDetector)
 * 
 * @author maggioranic
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
//CHECKSTYLE: stop AbstractClassName check
@SuppressWarnings("PMD.AbstractNaming")
public abstract class MultiAbstractDetector implements MultiEventDetector {
    // CHECKSTYLE: resume AbstractClassName check

    /** Default maximum checking interval (s). */
    public static final double DEFAULT_MAXCHECK = 600;

    /** Default convergence threshold (s). */
    public static final double DEFAULT_THRESHOLD = 1.e-6;

    /** Default maximal number of iterations in the event time search. */
    public static final int DEFAULT_MAX_ITERATION_COUNT = 100;

    /** Max check interval. */
    private final double maximumCheck;

    /** Convergence threshold. */
    private final double convergenceThreshold;

    /** Select all events, increasing g related events or decreasing g related events only. */
    private final int slopeSelect;

    /**
     * Build a new instance. The detector will detect both ascending and descending g-function
     * related events.
     * 
     * @param maxCheck
     *        maximum checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     */
    protected MultiAbstractDetector(final double maxCheck, final double threshold) {
        this.maximumCheck = maxCheck;
        this.convergenceThreshold = threshold;
        this.slopeSelect = INCREASING_DECREASING;
    }

    /**
     * Build a new instance.
     * 
     * @param slopeSelection
     *        g-function slope selection (0, 1, or 2)
     * @param maxCheck
     *        maximum checking interval (s)
     * @param threshold
     *        convergence threshold (s)
     */
    public MultiAbstractDetector(final int slopeSelection, final double maxCheck, final double threshold) {
        this.maximumCheck = maxCheck;
        this.convergenceThreshold = threshold;
        // Validate input
        if (slopeSelection != 0 && slopeSelection != 1 && slopeSelection != 2) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.UNSUPPORTED_SLOPE_SELECTION_TYPE);
        }
        this.slopeSelect = slopeSelection;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    public void init(final Map<String, SpacecraftState> s0, final AbsoluteDate t) throws PatriusException {
        // do nothing by default
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public abstract double g(final Map<String, SpacecraftState> s) throws PatriusException;

    /** {@inheritDoc} */
    @Override
    public abstract Action eventOccurred(final Map<String, SpacecraftState> s, final boolean increasing,
                                         final boolean forward) throws PatriusException;

    /** {@inheritDoc} */
    @Override
    public abstract boolean shouldBeRemoved();

    /** {@inheritDoc} */
    @Override
    public Map<String, SpacecraftState>
            resetStates(
                        final Map<String, SpacecraftState> oldStates) throws PatriusException {
        return oldStates;
    }

    /** {@inheritDoc} */
    @Override
    public double getThreshold() {
        return this.convergenceThreshold;
    }

    /** {@inheritDoc} */
    @Override
    public double getMaxCheckInterval() {
        return this.maximumCheck;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxIterationCount() {
        return DEFAULT_MAX_ITERATION_COUNT;
    }

    /** {@inheritDoc} */
    @Override
    public int getSlopeSelection() {
        return this.slopeSelect;
    }

}
