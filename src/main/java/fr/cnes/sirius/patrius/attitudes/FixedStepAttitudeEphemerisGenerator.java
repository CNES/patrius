/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history created 10/04/2013
 * 
 * HISTORY
* VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
* VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
* VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class handles the generation of attitude ephemeris from an attitude laws sequence
 * {@link StrictAttitudeLegsSequence},
 * using a fixed time step.<br>
 * The ephemeris generation can be done setting the generation time interval
 * (the default value is the time interval of the sequence), and the treatment to apply to the transition
 * points of the sequence (ignore them, compute the attitude of the initial date of the laws, compute the attitude of
 * the initial and final date of the laws).
 * 
 * @concurrency not thread-safe
 * @concurrency.comment The AttitudeLegsSequence attribute is mutable.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: FixedStepAttitudeEphemerisGenerator.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.3
 * 
 */
public final class FixedStepAttitudeEphemerisGenerator extends AbstractAttitudeEphemerisGenerator {

    /** The fixed time step. */
    private final double step;

    /**
     * Builds an attitude ephemeris generator using a fixed time step and ignoring the attitude law transition points of
     * the sequence.
     * 
     * @param legsSequence
     *        the sequence of attitude legs.
     * @param fixedStep
     *        the fixed time step.
     * @param providerIn PV coordinates provider
     */
    public FixedStepAttitudeEphemerisGenerator(final StrictAttitudeLegsSequence legsSequence,
            final double fixedStep,
            final PVCoordinatesProvider providerIn) {
        super(legsSequence, NO_TRANSITIONS, providerIn);
        this.step = fixedStep;
    }

    /**
     * Builds an attitude ephemeris generator using a fixed time step and choosing the treatment to apply to the
     * transition points of the sequence.
     * 
     * @param legsSequence
     *        the sequence of attitude legs.
     * @param fixedStep
     *        the fixed time step
     * @param transitions
     *        what to do with the attitude laws transition points:
     *        <ul>
     *        <li>
     *        {@link AbstractAttitudeEphemerisGenerator#NO_TRANSITIONS} to ignore the transition points
     *        of the sequence</li>
     *        <li>
     *        {@link AbstractAttitudeEphemerisGenerator#START_TRANSITIONS} to compute the initial dates of the attitude
     *        laws</li>
     *        <li>
     *        {@link AbstractAttitudeEphemerisGenerator#START_END_TRANSITIONS} to compute the initial and final dates of
     *        the attitude laws.</li>
     *        </ul>
     * @param providerIn PV coordinates provider
     */
    public FixedStepAttitudeEphemerisGenerator(final StrictAttitudeLegsSequence legsSequence,
            final double fixedStep,
            final int transitions,
            final PVCoordinatesProvider providerIn) {
        super(legsSequence, transitions, providerIn);
        this.step = fixedStep;
    }

    /** {@inheritDoc} */
    @Override
    protected double computeStep(final AbsoluteDate date,
            final AbsoluteDateInterval ephemerisInterval) throws PatriusException {
        return this.step;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean addLastPoint(final AbsoluteDateInterval ephemerisInterval) {
        boolean rez = false;
        if (this.transitions == 0 && !ephemerisInterval.includes(getTimeInterval())) {
            // adds the last point of the time interval
            // (not when the interval of validity is equal to the sequence time interval!):
            rez = true;
        } else if (this.transitions != 0) {
            rez = true;
        }
        return rez;
    }
}
