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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2450:27/01/2021:[PATRIUS] moyennage au sens du modele Analytical2D 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:94:30/08/2013:Corrected computation of lna and raan
 * VERSION::DM:211:08/04/2014:Modified analytical 2D propagator
 * VERSION::DM:211:30/04/2014:Updated javadoc
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:266:29/04/2015:add various centered analytical models
 * VERSION::FA:556:24/02/2016:change max orders vs dev orders
 * VERSION::FA:656:27/07/2016:make class threadsafe by cloning development orders
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.twod;

import java.io.Serializable;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.TIRFProvider;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.MeanOsculatingElementsProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * This class represents an analytical 2D orbit model, it is made of 6 parameter models, one per adapted circular
 * parameter.
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread safe if the underlying Analytical2DParameterModel and
 *                      AbsoluteDate objects are thread safe
 * 
 * @see Analytical2DParameterModel
 * @see Analytical2DPropagator
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: Analytical2DOrbitModel.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.3
 * 
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class Analytical2DOrbitModel implements Serializable, MeanOsculatingElementsProvider {

    /** Generated Serial UID */
    private static final long serialVersionUID = 4776243268159796018L;

    /** Default convergence threshold for osculating to mean algorithm. */
    private static final double DEFAULT_THRESHOLD = 1E-14;

    /** Maximum number of iterations for osculating to mean algorithm. */
    private static final int MAX_ITER = 100;

    /** Orbit parameter model */
    private final Analytical2DParameterModel smaModel;
    /** Orbit parameter model */
    private final Analytical2DParameterModel exModel;
    /** Orbit parameter model */
    private final Analytical2DParameterModel eyModel;
    /** Orbit parameter model */
    private final Analytical2DParameterModel incModel;
    /** Orbit parameter model */
    private final Analytical2DParameterModel lnaModel;
    /** Orbit parameter model */
    private final Analytical2DParameterModel aolModel;
    /** Spacecraft mass */
    private final MassProvider massModel;
    /** Standard gravitational parameter */
    private final double muValue;
    /** Trigonometric development orders [a, ex, ey, i lna, alpha] */
    private final int[] devOrders;

    /**
     * Relative convergence threshold for osculating to mean/secular algorithm.
     * Default threshold is 1E-14.
     */
    private double threshold = DEFAULT_THRESHOLD;

    /**
     * Create an analytical 2D orbit model with specified parameter models, development orders,
     * standard gravitational parameter and spacecraft mass.
     * 
     * @param a
     *        2D model of orbital parameter a
     * @param ex
     *        2D model of orbital parameter ex
     * @param ey
     *        2D model of orbital parameter ey
     * @param i
     *        2D model of parameter i
     * @param lna
     *        2D model of parameter lna
     * @param alpha
     *        2D model of parameter alpha
     * @param orders
     *        orders of trigonometric developments for [a, ex, ey, i, lna, alpha]
     * @param mass
     *        mass of spacecraft
     * @param mu
     *        standard gravitational parameter
     */
    public Analytical2DOrbitModel(final Analytical2DParameterModel a, final Analytical2DParameterModel ex,
        final Analytical2DParameterModel ey, final Analytical2DParameterModel i,
        final Analytical2DParameterModel lna, final Analytical2DParameterModel alpha,
        final int[] orders, final MassProvider mass, final double mu) {
        this(orders, mass, mu, a, ex, ey, i, lna, alpha);
    }

    /**
     * Create an analytical 2D orbit model with specified parameter models, standard gravitational
     * parameter and spacecraft mass. The trigonometric development orders will be the highest admissible.
     * 
     * @param a
     *        2D model of orbital parameter a
     * @param ex
     *        2D model of orbital parameter ex
     * @param ey
     *        2D model of parameter ey
     * @param i
     *        2D model of parameter i
     * @param lna
     *        2D model of parameter lna
     * @param alpha
     *        2D model of parameter alpha
     * @param mass
     *        mass of spacecraft
     * @param mu
     *        standard gravitational parameter
     */
    public Analytical2DOrbitModel(final Analytical2DParameterModel a, final Analytical2DParameterModel ex,
        final Analytical2DParameterModel ey, final Analytical2DParameterModel i,
        final Analytical2DParameterModel lna, final Analytical2DParameterModel alpha,
        final MassProvider mass, final double mu) {
        this(null, mass, mu, a, ex, ey, i, lna, alpha);
    }

    /**
     * Create an analytical 2D orbit model with specified parameter models. The pso and lna coefficients will be
     * computed using the user specified psoLnaCoefs array. The trigonometric development orders will be the
     * highest admissible.
     * 
     * @param a
     *        2D model of parameter a
     * @param ex
     *        2D model of parameter ex
     * @param ey
     *        2D model of parameter ey
     * @param i
     *        2D model of parameter i
     * @param lna
     *        2D model of parameter raan
     * @param alpha
     *        2D model of parameter alpha
     * @param mu
     *        standard gravitational parameter
     */
    public Analytical2DOrbitModel(final Analytical2DParameterModel a, final Analytical2DParameterModel ex,
        final Analytical2DParameterModel ey, final Analytical2DParameterModel i,
        final Analytical2DParameterModel lna, final Analytical2DParameterModel alpha,
        final double mu) {
        this(null, null, mu, a, ex, ey, i, lna, alpha);
    }

    /**
     * Create an analytical 2D orbit model with specified parameter models. The pso and lna coefficients will be
     * computed using the user specified psoLnaCoefs array. The development orders will be the highest admissible.
     * 
     * @param a
     *        2D model of parameter a
     * @param ex
     *        2D model of parameter ex
     * @param ey
     *        2D model of parameter ey
     * @param i
     *        2D model of parameter i
     * @param lna
     *        2D model of parameter raan
     * @param alpha
     *        2D model of parameter alpha
     * @param orders
     *        orders of trigonometric developments for [a, ex, ey, i, lna, alpha]
     * @param mu
     *        standard gravitational parameter
     */
    public Analytical2DOrbitModel(final Analytical2DParameterModel a, final Analytical2DParameterModel ex,
        final Analytical2DParameterModel ey, final Analytical2DParameterModel i,
        final Analytical2DParameterModel lna, final Analytical2DParameterModel alpha,
        final int[] orders, final double mu) {
        this(orders, null, mu, a, ex, ey, i, lna, alpha);
    }

    /**
     * Private constructor.
     * 
     * @param a
     *        2D model of orbital parameter a.
     * @param ex
     *        2D model of orbital parameter ex.
     * @param ey
     *        2D model of orbital parameter ey.
     * @param i
     *        2D model of parameter i.
     * @param lna
     *        2D model of parameter lna.
     * @param alpha
     *        2D model of parameter alpha.
     * @param orders
     *        orders of trigonometric developments for [a, ex, ey, i, lna, alpha].
     * @param mass
     *        mass of spacecraft.
     * @param mu
     *        standard gravitational parameter.
     */
    private Analytical2DOrbitModel(final int[] orders, final MassProvider mass, final double mu,
        final Analytical2DParameterModel a, final Analytical2DParameterModel ex,
        final Analytical2DParameterModel ey, final Analytical2DParameterModel i,
        final Analytical2DParameterModel lna, final Analytical2DParameterModel alpha) {

        this.smaModel = a;
        this.exModel = ex;
        this.eyModel = ey;
        this.incModel = i;
        this.lnaModel = lna;
        this.aolModel = alpha;
        this.massModel = mass;
        this.muValue = mu;

        if (orders == null) {
            this.devOrders = this.getMaxOrders();
        } else {
            // check user orders range
            this.checkOrders(orders);
            this.devOrders = orders.clone();
        }
    }

    /**
     * Check validity of user specified orders array by checking length and values ranges.
     * 
     * @param orders
     *        user specified orders
     */
    private void checkOrders(final int[] orders) {

        // Chech orders size
        if (orders.length != 6) {
            throw new IllegalArgumentException();
        }

        // Check range: order must be > 0 and lower than available models orders
        final int[] max = this.getMaxOrders();
        for (int i = 0; i < orders.length; i++) {
            if (orders[i] < 0 || orders[i] > max[i]) {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Propagate each parameter model to specified date and return an array of 6 values.
     * 
     * @param date
     *        date
     * @return array containing [a, ex, ey, i, raan, alpha]
     * @throws PatriusException
     *         thrown if Earth Rotation Angle (ERA) cannot be computed
     */
    public double[] propagateModel(final AbsoluteDate date) throws PatriusException {
        return this.propagateModel(date, this.devOrders);
    }

    /**
     * Propagate each parameter model to specified date and return an array of 6 values.
     * 
     * @param date
     *        date
     * @param orders
     *        orders [6 x trig]
     * @return array containing [a, ex, ey, i, raan, alpha]
     * @throws PatriusException
     *         thrown if Earth Rotation Angle (ERA) cannot be computed
     */
    public double[] propagateModel(final AbsoluteDate date, final int[] orders) throws PatriusException {

        // Lna/Pso
        final double lna = this.lnaModel.getCenteredValue(date);
        final double pso = this.aolModel.getCenteredValue(date);

        // Get the value from each model
        final double aValue = this.smaModel.getValue(date, pso, lna, orders[0]);
        final double exValue = this.exModel.getValue(date, pso, lna, orders[1]);
        final double eyValue = this.eyModel.getValue(date, pso, lna, orders[2]);
        final double iValue = this.incModel.getValue(date, pso, lna, orders[3]);
        final double raanValue = lna + this.lnaModel.getTrigonometricValue(pso, lna, orders[4])
            + TIRFProvider.getEarthRotationAngle(date);
        final double alphaValue = pso + this.aolModel.getTrigonometricValue(pso, lna, orders[5]);

        // Compile into array
        return new double[] { aValue, exValue, eyValue, iValue, raanValue, alphaValue };
    }

    /**
     * Return the array with models trigonometric orders. These orders are ordered as per :
     * [a, ex, ey, i, lna, alpha].
     * 
     * @return array with models trigonometric orders
     */
    public int[] getDevelopmentOrders() {
        return this.devOrders.clone();
    }

    /**
     * Return the array with the highest trigonometric orders available. These orders are ordered as per :
     * [a, ex, ey, i, lna, alpha].
     * 
     * @return array with highest orders
     */
    public int[] getMaxOrders() {
        return new int[] {
            this.smaModel.getMaxTrigonometricOrder(),
            this.exModel.getMaxTrigonometricOrder(),
            this.eyModel.getMaxTrigonometricOrder(),
            this.incModel.getMaxTrigonometricOrder(),
            this.lnaModel.getMaxTrigonometricOrder(),
            this.aolModel.getMaxTrigonometricOrder()
        };
    }

    /**
     * Returns the spacecraft mass model.
     * 
     * @return the mass model
     */
    public MassProvider getMassModel() {
        return this.massModel;
    }

    /**
     * Returns the standard gravitational parameter.
     * 
     * @return mu
     */
    public double getMu() {
        return this.muValue;
    }

    /**
     * Get the array of parameter models.
     * 
     * @return [sma, ex, ey, inc, lna, aol]
     */
    public Analytical2DParameterModel[] getParameterModels() {
        return new Analytical2DParameterModel[] { this.smaModel, this.exModel, this.eyModel, this.incModel,
            this.lnaModel, this.aolModel };
    }

    /**
     * Get the semi major axis parameter model.
     * 
     * @return the parameter model
     */
    public Analytical2DParameterModel getSmaModel() {
        return this.smaModel;
    }

    /**
     * Get the x eccentricity component parameter model.
     * 
     * @return the parameter model
     */
    public Analytical2DParameterModel getExModel() {
        return this.exModel;
    }

    /**
     * Get the y eccentricity component parameter model.
     * 
     * @return the parameter model
     */
    public Analytical2DParameterModel getEyModel() {
        return this.eyModel;
    }

    /**
     * Get the inclination parameter model.
     * 
     * @return the parameter model
     */
    public Analytical2DParameterModel getIncModel() {
        return this.incModel;
    }

    /**
     * Get the longitude of ascending node parameter model.
     * 
     * @return the parameter model
     */
    public Analytical2DParameterModel getLnaModel() {
        return this.lnaModel;
    }

    /**
     * Get the argument of latitude parameter model.
     * 
     * @return the parameter model
     */
    public Analytical2DParameterModel getAolModel() {
        return this.aolModel;
    }

    /** {@inheritDoc}
     * <p>This method returns the mean/centered part of the orbit (i.e. without the harmonic part).</p>
     * <p>Warning: the method assumes the provided orbit is an "osculating" orbit in the analytical 2D model sense.</p>
     */
    @Override
    public Orbit osc2mean(final Orbit orbit) throws PatriusException {
        return osc2mean(orbit, devOrders);
    }

    /**
     * Osculating to mean conversion with provided orders.
     * <p>This method returns the mean/centered part of the orbit (i.e. without the harmonic part).</p>
     * <p>Warning: the method assumes the provided orbit is an "osculating" orbit in the analytical 2D model sense.</p>
     * <p>Protected method because not to be used by users but only by this class and
     * {@link Analytical2DPropagator}</p>.
     * @param orbit osculating orbit
     * @param orders development orders
     * @return mean orbit in orbit frame and type
     * @throws PatriusException thrown if model propagation failed or frame conversion to output type failed or
     *         algorithm dit not converge
     */
    protected Orbit osc2mean(final Orbit orbit, final int[] orders) throws PatriusException {
        
        // Initialization
        final CircularOrbit osculatingOrbit = new CircularOrbit(this.convertFrame(orbit, FramesFactory.getCIRF()));
        final double[] osculating = { osculatingOrbit.getA(), osculatingOrbit.getCircularEx(),
                osculatingOrbit.getCircularEy(), osculatingOrbit.getI(),
                osculatingOrbit.getRightAscensionOfAscendingNode(), osculatingOrbit.getAlphaM() };

        // Mean parameter initialization to osculating parameters
        final double[] mean = new double[6];
        System.arraycopy(osculating, 0, mean, 0, 6);

        // Relative tolerance: threshold for each circular parameter
        final double[] tol = { threshold * (1. + MathLib.abs(osculatingOrbit.getA())),
            threshold * (1. + MathLib.abs(osculatingOrbit.getCircularEx())),
            threshold * (1. + MathLib.abs(osculatingOrbit.getCircularEy())),
            threshold * (1. + MathLib.abs(osculatingOrbit.getI())),
            threshold * (1. + MathLib.abs(osculatingOrbit.getRightAscensionOfAscendingNode())),
            threshold * FastMath.PI };

        // Earth sidereal time
        final double era = TIRFProvider.getEarthRotationAngle(orbit.getDate());

        // Current number of iterations
        int iter = 0;

        // Loop until convergence
        while (iter < MAX_ITER) {

            // Compute updated osculating parameters
            final double aolCen = mean[5];
            final double lnaCen = mean[4] - era;
            final double[] newOsc = new double[] {
                    mean[0] + getSmaModel().getTrigonometricValue(aolCen, lnaCen, orders[0]),
                    mean[1] + getExModel().getTrigonometricValue(aolCen, lnaCen, orders[1]),
                    mean[2] + getEyModel().getTrigonometricValue(aolCen, lnaCen, orders[2]),
                    mean[3] + getIncModel().getTrigonometricValue(aolCen, lnaCen, orders[3]),
                    mean[4] + getLnaModel().getTrigonometricValue(aolCen, lnaCen, orders[4]),
                    mean[5] + getAolModel().getTrigonometricValue(aolCen, lnaCen, orders[5]) };

            // Parameters residuals
            final double[] delta = new double[6];
            for (int i = 0; i < delta.length; i++) {
                delta[i] = newOsc[i] - osculating[i];
            }

            // Update mean parameters
            for (int i = 0; i < delta.length; i++) {
                mean[i] = mean[i] - delta[i];
            }

            // Check convergence
            boolean interrupt = true;
            for (int i = 0; i < delta.length; i++) {
                interrupt &= MathLib.abs(delta[i]) < tol[i];
            }
            if (interrupt) {
                final Orbit finalOrbit = new CircularOrbit(mean[0], mean[1], mean[2], mean[3], mean[4], mean[5],
                        PositionAngle.MEAN, FramesFactory.getCIRF(), orbit.getDate(), getMu());
                return orbit.getType().convertType(convertFrame(finalOrbit, orbit.getFrame()));
            }
            // Update loop counter
            iter++;
        }

        // Algorithm did not converge
        throw new PropagationException(PatriusMessages.UNABLE_TO_PERFORM_ANALYTICAL2D_OSC_MEAN_CONVERSION, MAX_ITER);
    }

    /** {@inheritDoc}
     * <p>Warning: the method assumes the provided orbit is a "mean" orbit in the analytical 2D model sense.</p>
     */
    @Override
    public Orbit mean2osc(final Orbit orbit) throws PatriusException {
        return mean2osc(orbit, this.devOrders);
    }

    /**
     * Mean to osculating conversion with provided orders.
     * <p>Warning: the method assumes the provided orbit is a "mean" orbit in the analytical 2D model sense.</p>
     * <p>Protected method because not to be used by users but only by this class and
     * {@link Analytical2DPropagator}</p>.
     * @param orbit mean orbit
     * @param orders development orders
     * @return osculating orbit in orbit frame and type
     * @throws PatriusException thrown if model propagation failed or frame conversion to output type failed
     */
    protected Orbit mean2osc(final Orbit orbit, final int[] orders) throws PatriusException {
        // Get values of parameters
        final double[] val = propagateModel(orbit.getDate(), orders);
        // Create the resulting circular orbit in CIRF frame
        final Orbit circOrbit = new CircularOrbit(val[0], val[1], val[2], val[3], val[4], val[5], PositionAngle.MEAN,
                FramesFactory.getCIRF(), orbit.getDate(), getMu());

        // Return orbit in input frame and type
        return orbit.getType().convertType(convertFrame(circOrbit, orbit.getFrame()));
    }

    /**
     * Convert provided orbit in output frame.
     * 
     * @param orbit
     *        orbit
     * @param outputFrame
     *        output frame
     * @return converted orbit in output frame
     * @throws PatriusException
     *         thrown if conversion failed
     */
    private Orbit convertFrame(final Orbit orbit,
            final Frame outputFrame) throws PatriusException {
        Orbit res = orbit;
        if (!orbit.getFrame().equals(outputFrame)) {
            res = new CircularOrbit(orbit.getPVCoordinates(outputFrame), outputFrame, orbit.getDate(), orbit.getMu());
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public Orbit propagateMeanOrbit(final AbsoluteDate date) throws PatriusException {
        // Propagate model with orders set to 0
        final double[] val = propagateModel(date, new int[6]);
        // Create the resulting circular orbit
        return new CircularOrbit(val[0], val[1], val[2], val[3], val[4], val[5], PositionAngle.MEAN,
                FramesFactory.getCIRF(), date, getMu());
    }

    /**
     * Setter for relative convergence threshold for osculating to mean conversion used by method
     * {@link #osc2mean(Orbit)}.
     * @param newThreshold
     *        new relative threshold
     */
    public void setThreshold(final double newThreshold) {
        threshold = newThreshold;
    }
}
