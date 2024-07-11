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
 * @history 09/04/2013
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2450:27/01/2021:[PATRIUS] moyennage au sens du modele Analytical2D 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:94:09/10/2013:Updated jdoc
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:266:29/04/2015:add various centered analytical models
 * VERSION::FA:556:24/02/2016:change max orders vs dev orders
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.twod;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.AbstractPropagator;
import fr.cnes.sirius.patrius.propagation.MeanOsculatingElementsProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * This class propagates an analytical 2D orbit model and extends the {@link AbstractPropagator} class. Thus, this
 * propagator can handle events and all functionalities of the {@link AbstractPropagator}.
 * 
 * @concurrency not thread-safe
 * @concurrency.comment extends the AbstractPropagator class
 * 
 * @see Analytical2DOrbitModel
 * @see Analytical2DParameterModel
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: Analytical2DPropagator.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.3
 * 
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class Analytical2DPropagator extends AbstractPropagator implements MeanOsculatingElementsProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -4001156899150568452L;

    /** Analytical 2D orbit model */
    private final Analytical2DOrbitModel orbitModel;

    /** Orders of trigonometric developments. [sma, ex, ey, inc, lna, aol]. */
    private final int[] devOrders;

    /**
     * Create an instance of a 2D propagator with default EME2000 aligned attitude.
     * 
     * @param model
     *        analytical 2D orbit model
     * @param initialDate
     *        initial date
     */
    public Analytical2DPropagator(final Analytical2DOrbitModel model, final AbsoluteDate initialDate) {
        this(null, model, initialDate);
    }

    /**
     * Create an instance of a 2D propagator with default EME2000 aligned attitude.
     * 
     * @param model
     *        analytical 2D orbit model
     * @param initialDate
     *        initial date
     * @param orders
     *        orders of trigonometric developments. [sma, ex, ey, inc, lna, aol]
     */
    public Analytical2DPropagator(final Analytical2DOrbitModel model, final AbsoluteDate initialDate,
        final int[] orders) {
        this(null, model, initialDate, orders);
    }

    /**
     * Create an instance of a 2D propagator.
     * 
     * @param attitudeProvider
     *        spacecraft attitude provider
     * @param model
     *        analytical 2D orbit model
     * @param initialDate
     *        initial date
     */
    public Analytical2DPropagator(final AttitudeProvider attitudeProvider, final Analytical2DOrbitModel model,
        final AbsoluteDate initialDate) {
        this(attitudeProvider, model, initialDate, model.getDevelopmentOrders());
    }

    /**
     * Create an instance of a 2D propagator.
     * 
     * @param attitudeProvider
     *        spacecraft attitude provider
     * @param model
     *        analytical 2D orbit model
     * @param initialDate
     *        initial date
     * @param orders
     *        orders of trigonometric developments. [sma, ex, ey, inc, lna, aol]
     */
    public Analytical2DPropagator(final AttitudeProvider attitudeProvider, final Analytical2DOrbitModel model,
        final AbsoluteDate initialDate, final int[] orders) {
        super(attitudeProvider);
        this.orbitModel = model;
        this.devOrders = orders;

        try {
            final Orbit orbit = this.propagateOrbit(initialDate);

            Attitude attitude = null;
            if (this.getAttitudeProvider() != null) {
                attitude = this.getAttitudeProvider().getAttitude(orbit, initialDate, FramesFactory.getCIRF());
            }

            final SpacecraftState state = new SpacecraftState(orbit, attitude, this.orbitModel.getMassModel());
            this.resetInitialState(state);

            this.addAdditionalStateProvider(this.orbitModel.getMassModel());

        } catch (final PatriusException e) {
            throw new PatriusExceptionWrapper(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected Orbit propagateOrbit(final AbsoluteDate date) throws PropagationException {
        try {
            // Get values of parameters
            final double[] val = this.orbitModel.propagateModel(date, this.devOrders);
            // Create the resulting circular orbit
            return new CircularOrbit(val[0], val[1], val[2], val[3], val[4], val[5], PositionAngle.MEAN,
                FramesFactory.getCIRF(), date, this.orbitModel.getMu());
        } catch (final PatriusException e) {
            throw new PropagationException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Orbit osc2mean(final Orbit orbit) throws PatriusException {
        return this.orbitModel.osc2mean(orbit, this.devOrders);
    }

    /** {@inheritDoc} */
    @Override
    public Orbit mean2osc(final Orbit orbit) throws PatriusException {
        return this.orbitModel.mean2osc(orbit, this.devOrders);
    }

    /** {@inheritDoc} */
    @Override
    public Orbit propagateMeanOrbit(final AbsoluteDate date) throws PatriusException {
        return this.orbitModel.propagateMeanOrbit(date);
    }

    /**
     * Setter for relative convergence threshold for osculating to mean conversion used by method
     * {@link #osc2mean(Orbit)}.
     * @param newThreshold
     *        new relative threshold
     */
    public void setThreshold(final double newThreshold) {
        this.orbitModel.setThreshold(newThreshold);
    }
}
