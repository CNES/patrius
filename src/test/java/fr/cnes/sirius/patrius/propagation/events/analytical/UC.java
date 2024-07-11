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
 * @history created 12/09/2014
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events.analytical;

import java.util.List;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Generic test case (analytical and semi-analytical propagators).
 * 
 * @version $Id: UC.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 */
public abstract class UC {

    /** t0. */
    private final AbsoluteDate t0 = AbsoluteDate.J2000_EPOCH;

    /** t1. */
    protected final AbsoluteDate t1 = this.t0.shiftedBy(5.5);

    /** t2. */
    protected final AbsoluteDate t2 = this.t0.shiftedBy(5.7);

    /** tf. */
    private final AbsoluteDate tf = this.t0.shiftedBy(10);

    /** Result. */
    private boolean result;

    /**
     * Constructor (analytical).
     * 
     * @param convergence
     *        convergence parameter
     * @param ordered
     *        true if detector 1 is added before detector 2
     */
    public UC(final double convergence, final boolean ordered) {

        // Initialization
        this.result = true;

        final Detector1 d1 = this.getDetector1();
        final Detector2 d2 = this.getDetector2();
        final Detector3 d3 = this.getDetector3();

        try {
            final Orbit orbit = new KeplerianOrbit(7100000, 0, 0, 0, 0, 0, PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.t0, Constants.EGM96_EARTH_MU);
            final KeplerianPropagator propagator = new KeplerianPropagator(orbit);

            if (ordered) {
                propagator.addEventDetector(d1);
                propagator.addEventDetector(d2);
            } else {
                propagator.addEventDetector(d2);
                propagator.addEventDetector(d1);
            }
            propagator.addEventDetector(d3);

            propagator.propagate(this.tf);

        } catch (final PropagationException e) {
            this.result = false;
            e.printStackTrace();
        }

        // Check event list
        final List<Event> expectedEventList = this.getExpectedEventList();
        final List<Event> actualEventList = d1.getEventList();
        actualEventList.addAll(d2.getEventList());
        actualEventList.addAll(d3.getEventList());
        this.result &= expectedEventList.equals(actualEventList);

        // Retropolation : does not work because interpolator computes "forward" variable asymmetrically
        // retropolate(convergence, ordered, yf);
    }

    //
    // /**
    // * Constructor (semi-analytical).
    // * @param integrator integrator
    // * @param convergence convergence parameter
    // * @param ordered true if detector 1 is added before detector 2
    // */
    // public UC(final FirstOrderIntegrator integrator, final double convergence, final boolean ordered) {
    //
    // // Initialization
    // result = true;
    //
    // final Detector1 d1 = getDetector1();
    // final Detector2 d2 = getDetector2();
    // final Detector3 d3 = getDetector3();
    //
    // try {
    // final Orbit orbit = new KeplerianOrbit(7100000, 0, 0, 0, 0, 0, PositionAngle.MEAN, FramesFactory.getEME2000(),
    // t0, Constants.EGM96_EARTH_MU);
    //
    // final StelaGTOPropagator propagator = new StelaGTOPropagator(integrator);
    // final double massIn = 1000.;;
    // propagator.setInitialState(new SpacecraftState(orbit), massIn, true);
    //
    // if (ordered) {
    // propagator.addEventDetector(d1);
    // propagator.addEventDetector(d2);
    // } else {
    // propagator.addEventDetector(d2);
    // propagator.addEventDetector(d1);
    // }
    // propagator.addEventDetector(d3);
    //
    // // Propagation
    // propagator.propagate(tf);
    //
    // } catch (PropagationException e) {
    // result = false;
    // } catch (OrekitException e) {
    // result = false;
    // }
    //
    // // Check event list
    // final List<Event> expectedEventList = getExpectedEventList();
    // final List<Event> actualEventList = d1.getEventList();
    // actualEventList.addAll(d2.getEventList());
    // actualEventList.addAll(d3.getEventList());
    // result &= expectedEventList.equals(actualEventList);
    // }

    /**
     * Returns detector 1.
     * 
     * @return detector 1
     */
    public abstract Detector1 getDetector1();

    /**
     * Returns detector 2.
     * 
     * @return detector 2
     */
    public abstract Detector2 getDetector2();

    /**
     * Returns detector 3.
     * 
     * @return detector 3
     */
    public abstract Detector3 getDetector3();

    /**
     * Returns expected events list.
     * 
     * @return expected events list
     */
    public abstract List<Event> getExpectedEventList();

    /**
     * Returns expected events list for retropolation process.
     * 
     * @return expected events list for retropolation process
     */
    public abstract List<Event> getExpectedEventListRetropolation();

    /**
     * Returns test result
     * 
     * @return test result
     */
    public boolean getTestResult() {
        return this.result;
    }
}
