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
 * @history created 10/04/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:180:17/03/2014:removed a break instruction inside a while loop
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class handles the generation of attitude ephemeris from an attitude laws
 * sequence {@link StrictAttitudeLegsSequence}, using a variable time step.<br>
 * The ephemeris generation can be done setting the generation time interval
 * (the default value is the time interval of the sequence), and the treatment
 * to apply to the transition points of the sequence (ignore them, compute the
 * attitude of the initial date of the laws, compute the attitude of the initial
 * and final date of the laws).
 * 
 * @concurrency not thread-safe
 * @concurrency.comment The AttitudeLegsSequence attribute is mutable.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: VariableStepAttitudeEphemerisGenerator.java 8434 2013-04-10
 *          15:34:31Z SabatiniT $
 * 
 * @since 1.3
 * 
 */
public class VariableStepAttitudeEphemerisGenerator extends
    AbstractAttitudeEphemerisGenerator {

    /** The minimum time step. */
    private final double dMin;

    /** The maximum time step. */
    private final double dMax;

    /**
     * The maximum allowed angular distance between two consecutive attitude
     * ephemeris.
     */
    private final double angMax;

    /**
     * Builds an attitude ephemeris generator using a variable time step and
     * ignoring the attitude law transition points of the sequence.
     * 
     * @param legsSequence
     *        the sequence of attitude legs.
     * @param stepMin
     *        the minimum time step
     * @param stepMax
     *        the maximum time step
     * @param angDistMax
     *        the maximum allowed angular distance between two consecutive
     *        attitude ephemeris
     * @param providerIn PV coordinates provider
     */
    public VariableStepAttitudeEphemerisGenerator(
        final StrictAttitudeLegsSequence<AttitudeLeg> legsSequence, final double stepMin,
        final double stepMax, final double angDistMax,
        final PVCoordinatesProvider providerIn) {
        super(legsSequence, NO_TRANSITIONS, providerIn);
        this.dMin = stepMin;
        this.dMax = stepMax;
        this.angMax = angDistMax;
    }

    /**
     * Builds an attitude ephemeris generator using a variable time step and
     * choosing the treatment to apply to the transition points of the sequence.
     * 
     * @param legsSequence
     *        the sequence of attitude legs.
     * @param stepMin
     *        the minimum time step
     * @param stepMax
     *        the maximum time step
     * @param angDistMax
     *        the maximum allowed angular distance between two consecutive
     *        attitude ephemeris
     * @param transitions
     *        what to do with the attitude laws transition points:
     *        <ul>
     *        <li>
     *        {@link AbstractAttitudeEphemerisGenerator#NO_TRANSITIONS} to ignore the transition 
     *        points of the sequence</li>
     *        <li>
     *        {@link AbstractAttitudeEphemerisGenerator#START_TRANSITIONS} to compute the initial dates of the attitude
     *        laws</li>
     *        <li>
     *        {@link AbstractAttitudeEphemerisGenerator#START_END_TRANSITIONS} to compute the initial and final dates of
     *        the attitude laws.</li>
     *        </ul>
     * @param providerIn PV coordinates provider
     */
    public VariableStepAttitudeEphemerisGenerator(
        final StrictAttitudeLegsSequence<AttitudeLeg> legsSequence, final double stepMin,
        final double stepMax, final double angDistMax, final int transitions,
        final PVCoordinatesProvider providerIn) {
        super(legsSequence, transitions, providerIn);
        this.dMin = stepMin;
        this.dMax = stepMax;
        this.angMax = angDistMax;
    }

    /**
     * Computes the step used during the variable step ephemeris generation.
     * 
     * @param date
     *        the initial date
     * @param ephemerisInterval
     *        the interval of validity of the ephemeris
     * @return the computed step
     * @throws PatriusException
     *         an orekit exception
     */
    @Override
    protected double computeStep(final AbsoluteDate date,
                                 final AbsoluteDateInterval ephemerisInterval) throws PatriusException {
        // this default frame is used to compute the angular distance between
        // two attitude ephemeris:
        final Frame frame = FramesFactory.getEME2000();
        // creates a meaningless PVCoordinatesProvider in order to call the
        // getAttitude method:
        // IMPORTANT: this object in not used.
        double step = this.dMax;
        AbsoluteDate currentDate = date.shiftedBy(this.dMax);
        Attitude current;
        if (ephemerisInterval.contains(currentDate)) {
            // computes the attitude using the maximum time step:
            current = this.sequence.getAttitude(this.provider, currentDate, frame);
        } else {
            current = this.sequence.getAttitude(this.provider,
                ephemerisInterval.getUpperData(), frame);
        }
        final Attitude previous = this.sequence.getAttitude(this.provider, date, frame);
        double distance = Rotation.distance(current.getRotation(),
            previous.getRotation());
        // checks that the angular distance between two consecutive attitude
        // ephemeris is smaller than
        // the threshold value and if not uses a smaller time step:
        boolean isStepInvalid = (distance - this.angMax) > Precision.DOUBLE_COMPARISON_EPSILON;
        while (isStepInvalid) {
            // computes a smaller step using an iterative algorithm:
            step = MathLib.divide(step * this.angMax, distance);
            if (step < this.dMin) {
                // the computed step is smaller than the allowed minimum value:
                isStepInvalid = true;
            }
            // shifts the date one step:
            currentDate = date.shiftedBy(step);
            current = this.sequence.getAttitude(this.provider, currentDate, frame);
            distance = Rotation.distance(current.getRotation(),
                previous.getRotation());
            isStepInvalid = (distance - this.angMax) > Precision.DOUBLE_COMPARISON_EPSILON;
        }
        return step;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean addLastPoint(final AbsoluteDateInterval ephemerisInterval) {
        return true;
    }
}
