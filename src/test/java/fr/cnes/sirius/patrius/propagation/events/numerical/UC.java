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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.propagation.events.numerical;

import java.util.List;

import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;

/**
 * Generic test case (numerical propagators).
 * 
 * @version $Id: UC.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 */
public abstract class UC {

    /** t0. */
    private final double t0 = 0.0;

    /** t1. */
    protected final double t1 = 5.5;

    /** t2. */
    protected final double t2 = 5.7;

    /** tf. */
    private final double tf = 10;

    /** Result. */
    private boolean result;

    /**
     * Constructor (numerical).
     * 
     * @param integrator
     *        integrator
     * @param convergence
     *        convergence parameter
     * @param ordered
     *        true if detector 1 is added before detector 2
     */
    public UC(final FirstOrderIntegrator integrator, final double convergence, final boolean ordered) {

        // Initialization
        integrator.clearEventHandlers();
        integrator.clearStepHandlers();

        integrator.addStepHandler(new EphemerisStepHandler());

        final Detector1 d1 = this.getDetector1();
        final Detector2 d2 = this.getDetector2();
        final Detector3 d3 = this.getDetector3();

        if (ordered) {
            integrator.addEventHandler(d1, 0.1, convergence, 100);
            integrator.addEventHandler(d2, 0.1, convergence, 100);
        } else {
            integrator.addEventHandler(d2, 0.1, convergence, 100);
            integrator.addEventHandler(d1, 0.1, convergence, 100);
        }
        integrator.addEventHandler(d3, 0.1, convergence, 100);

        // Integration
        this.result = true;
        final double[] y0 = { 0 };
        final double[] yf = { 0 };
        try {
            integrator.integrate(new ODE(), this.t0, y0, this.tf, yf);
        } catch (final NoBracketingException e) {
            this.result = false;
        }

        // Check event list
        final List<Event> expectedEventList = this.getExpectedEventList();
        final List<Event> actualEventList = d1.getEventList();
        actualEventList.addAll(d2.getEventList());
        actualEventList.addAll(d3.getEventList());
        this.result &= expectedEventList.equals(actualEventList);

        // Retropolation
        this.retropolate(integrator, convergence, ordered, yf);
    }

    /**
     * 
     * @param integrator
     *        FirstOrderIntegrator
     * @param convergence
     *        double
     * @param ordered
     *        boolean
     * @param y0
     *        double[]
     */
    private void retropolate(final FirstOrderIntegrator integrator, final double convergence, final boolean ordered,
                             final double[] y0) {

        // Initialization
        integrator.clearEventHandlers();
        integrator.clearStepHandlers();

        integrator.addStepHandler(new EphemerisStepHandler());

        final Detector1 d1 = this.getDetector1();
        final Detector2 d2 = this.getDetector2();
        final Detector3 d3 = this.getDetector3();

        integrator.addEventHandler(d3, 0.1, convergence, 100);
        if (ordered) {
            integrator.addEventHandler(d2, 0.1, convergence, 100);
            integrator.addEventHandler(d1, 0.1, convergence, 100);
        } else {
            integrator.addEventHandler(d1, 0.1, convergence, 100);
            integrator.addEventHandler(d2, 0.1, convergence, 100);
        }

        // Integration
        try {
            integrator.integrate(new ODE(), this.tf, y0, this.t0, y0);
        } catch (final NoBracketingException e) {
            this.result = false;
        }

        // Check event list
        final List<Event> expectedEventList = this.getExpectedEventListRetropolation();
        final List<Event> actualEventList = d1.getEventList();
        actualEventList.addAll(d2.getEventList());
        actualEventList.addAll(d3.getEventList());
        this.result &= expectedEventList.equals(actualEventList);
    }

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
