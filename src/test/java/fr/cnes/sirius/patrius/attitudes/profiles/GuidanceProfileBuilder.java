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
 * @history Created 20/03/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:1950:14/11/2018:new attitude profile design
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.profiles;

import fr.cnes.sirius.patrius.attitudes.AttitudeLawLeg;
import fr.cnes.sirius.patrius.attitudes.kinematics.KinematicsToolkit.IntegrationType;
import fr.cnes.sirius.patrius.attitudes.profiles.AbstractAngularVelocitiesAttitudeProfile.AngularVelocityIntegrationType;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.analysis.UnivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.integration.IterativeLegendreGaussIntegrator;
import fr.cnes.sirius.patrius.math.analysis.polynomials.FourierDecompositionEngine;
import fr.cnes.sirius.patrius.math.analysis.polynomials.FourierSeries;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

/**
 * 
 * Old class kept there to avoid the process of rebuilding a profile from input data.
 * 
 * @concurrency immutable
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 * @deprecated since v4.2. Classes inheriting {@link AttitudeProfile} should be used instead.
 */
@Deprecated
final class GuidanceProfileBuilder {

    /** Max evaluations */
    private static final int MAXEVALS = 100000;

    /** Private constructor */
    private GuidanceProfileBuilder() {
    }

    /**
     * <p>
     * Compute the angular velocities harmonic guidance profile. The spin components are decomposed into Fourier Series
     * and the rotation is computed from the resulting series using the specified {@link IntegrationType integration
     * method} and integStep.
     * </p>
     * <p>
     * The Fourier coefficients are computed between {@code t_ref - T/2} and {@code t_ref + T/2}.
     * </p>
     * 
     * 
     * 
     * @param attitude
     *        attitude provider
     * @param provider
     *        pv coordinates provider (can be a propagator)
     * @param frame
     *        frame in which the guidance profile is to be expressed
     * @param tref
     *        reference date, usually date of reference raan
     * @param period
     *        period of the spin vector components
     * @param order
     *        order of development of the components of the spin vector
     * @param integType
     *        integration type (Wilcox or Edwards)
     * @param integStep
     *        integration step
     * @return the generated guidance profile
     * @throws PatriusException
     *         if guidance pofile cannot be computed
     */
    public static AngularVelocitiesHarmonicProfile
            computeAngularVelocitiesHarmonicProfile(
                                                    final AttitudeLawLeg attitude,
                                                    final PVCoordinatesProvider provider, final Frame frame,
                                                    final AbsoluteDate tref, final double period, final int order,
                                                    final AngularVelocityIntegrationType integType,
                                                    final double integStep) throws PatriusException {

        // Fourier decomposition engine
        final IterativeLegendreGaussIntegrator integrator = new IterativeLegendreGaussIntegrator(4, 1e-12, 1.5e-10);
        // final SimpsonIntegrator integrator = new SimpsonIntegrator();
        final FourierDecompositionEngine engine = new FourierDecompositionEngine(integrator);

        // Start and end dates of new profile
        final AbsoluteDate start = attitude.getTimeInterval().getLowerData();
        final AbsoluteDate end = attitude.getTimeInterval().getUpperData();

        // x coordinate of angular velocity as a UnivariateFunction
        final UnivariateFunction xf = new UnivariateFunction(){
            @Override
            public double value(final double x) {
                try {
                    return attitude.getAttitudeLaw().getAttitude(provider, tref.shiftedBy(x), frame).getSpin().getX();
                } catch (final PatriusException e) {
                    throw new PatriusExceptionWrapper(e);
                }
            }
        };

        // y coordinate of angular velocity as a UnivariateFunction
        final UnivariateFunction yf = new UnivariateFunction(){
            @Override
            public double value(final double x) {
                try {
                    return attitude.getAttitudeLaw().getAttitude(provider, tref.shiftedBy(x), frame).getSpin().getY();
                } catch (final PatriusException e) {
                    throw new PatriusExceptionWrapper(e);
                }
            }
        };

        // z coordinate of angular velocity as a UnivariateFunction
        final UnivariateFunction zf = new UnivariateFunction(){
            @Override
            public double value(final double x) {
                try {
                    return attitude.getAttitudeLaw().getAttitude(provider, tref.shiftedBy(x), frame).getSpin().getZ();
                } catch (final PatriusException e) {
                    throw new PatriusExceptionWrapper(e);
                }
            }
        };

        // Fourier decomposition of x component
        engine.setFunction(xf, period);
        engine.setOrder(order);
        engine.setMaxEvals(MAXEVALS);
        final FourierSeries xfs = engine.decompose().getFourier();

        // Fourier decomposition of x component
        engine.setFunction(yf, period);
        engine.setOrder(order);
        final FourierSeries yfs = engine.decompose().getFourier();

        // Fourier decomposition of x component
        engine.setFunction(zf, period);
        engine.setOrder(order);
        final FourierSeries zfs = engine.decompose().getFourier();

        // Initial orientation
        final Rotation initialOrientation = attitude.getAttitudeLaw().getAttitude(provider, tref, frame)
            .getRotation();

        // Time interval of validity
        final AbsoluteDateInterval interval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED, start, end,
            IntervalEndpointType.CLOSED);

        // build guidance profile
        return new AngularVelocitiesHarmonicProfile(xfs, yfs, zfs, frame, interval, initialOrientation, tref,
            integType, integStep);
    }

}
