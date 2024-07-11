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
 * @history created 15/02/2016
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:FA:FA-2251:04/10/2019:[PATRIUS] Propagateurs analytique
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::FA:665:28/07/2016:forbid non-inertial frames
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical;

import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Lyddane secular propagator.
 * <p>
 * Lyddane propagator is an analytical propagator taking into account only mean secular effects of J2 to J5 zonal
 * harmonics.
 * </p>
 * <p>
 * This propagator is valid for orbits with eccentricity lower than 0.9 and inclination not close to critical
 * inclinations
 * </p>
 * 
 * @author Emmanuel Bignon
 * @since 3.2
 * @version $Id: LyddaneSecularPropagator.java 17582 2017-05-10 12:58:16Z bignon $
 */
public class LyddaneSecularPropagator extends AbstractLyddanePropagator {

    /** UID. */
    private static final long serialVersionUID = 5508421209800631622L;

    /**
     * Constructor without attitude provider and mass provider.
     * 
     * @param initialOrbit
     *        initial orbit
     * @param referenceRadiusIn
     *        reference radius of the central body attraction model (m)
     * @param muIn
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param c20In
     *        un-normalized 2nd zonal coefficient (about -1.08e-3 for Earth)
     * @param c30In
     *        un-normalized 3rd zonal coefficient (about +2.53e-6 for Earth)
     * @param c40In
     *        un-normalized 4th zonal coefficient (about +1.62e-6 for Earth)
     * @param c50In
     *        un-normalized 5th zonal coefficient (about +2.28e-7 for Earth)
     * @param frameIn
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param parametersTypeIn
     *        initial orbit parameters type (mean or osculating)
     * @throws PatriusException
     *         thrown if failed to build initial state or coefficients frame is not inertial
     */
    public LyddaneSecularPropagator(final Orbit initialOrbit, final double referenceRadiusIn, final double muIn,
        final double c20In, final double c30In, final double c40In, final double c50In,
        final Frame frameIn, final ParametersType parametersTypeIn) throws PatriusException {
        this(initialOrbit, referenceRadiusIn, muIn, c20In, c30In, c40In, c50In, frameIn, parametersTypeIn, null,
            null, null);
    }

    /**
     * Constructor without attitude provider.
     * 
     * @param initialOrbit
     *        initial orbit
     * @param referenceRadiusIn
     *        reference radius of the central body attraction model (m)
     * @param muIn
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param c20In
     *        un-normalized 2nd zonal coefficient (about -1.08e-3 for Earth)
     * @param c30In
     *        un-normalized 3rd zonal coefficient (about +2.53e-6 for Earth)
     * @param c40In
     *        un-normalized 4th zonal coefficient (about +1.62e-6 for Earth)
     * @param c50In
     *        un-normalized 5th zonal coefficient (about +2.28e-7 for Earth)
     * @param frameIn
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param parametersTypeIn
     *        initial orbit parameters type (mean or osculating)
     * @param massProvider
     *        mass provider
     * @throws PatriusException
     *         thrown if failed to build initial state or coefficients frame is not inertial
     */
    public LyddaneSecularPropagator(final Orbit initialOrbit, final double referenceRadiusIn, final double muIn,
        final double c20In, final double c30In, final double c40In, final double c50In,
        final Frame frameIn, final ParametersType parametersTypeIn,
        final MassProvider massProvider) throws PatriusException {
        this(initialOrbit, referenceRadiusIn, muIn, c20In, c30In, c40In,
            c50In, frameIn, parametersTypeIn, null, null, massProvider);
    }

    /**
     * Constructor without mass provider.
     * 
     * @param initialOrbit
     *        initial orbit
     * @param referenceRadiusIn
     *        reference radius of the central body attraction model (m)
     * @param muIn
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param c20In
     *        un-normalized 2nd zonal coefficient (about -1.08e-3 for Earth)
     * @param c30In
     *        un-normalized 3rd zonal coefficient (about +2.53e-6 for Earth)
     * @param c40In
     *        un-normalized 4th zonal coefficient (about +1.62e-6 for Earth)
     * @param c50In
     *        un-normalized 5th zonal coefficient (about +2.28e-7 for Earth)
     * @param frameIn
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param parametersTypeIn
     *        initial orbit parameters type (mean or osculating)
     * @param attitudeProvider
     *        attitude provider
     * @throws PatriusException
     *         thrown if failed to build initial state or coefficients frame is not inertial
     */
    public LyddaneSecularPropagator(final Orbit initialOrbit, final double referenceRadiusIn, final double muIn,
        final double c20In, final double c30In, final double c40In, final double c50In,
        final Frame frameIn, final ParametersType parametersTypeIn,
        final AttitudeProvider attitudeProvider) throws PatriusException {
        this(initialOrbit, referenceRadiusIn, muIn, c20In, c30In, c40In, c50In,
            frameIn, parametersTypeIn, attitudeProvider, null, null);
    }

    /**
     * Constructor without mass provider.
     * 
     * @param initialOrbit
     *        initial orbit
     * @param referenceRadiusIn
     *        reference radius of the central body attraction model (m)
     * @param muIn
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param c20In
     *        un-normalized 2nd zonal coefficient (about -1.08e-3 for Earth)
     * @param c30In
     *        un-normalized 3rd zonal coefficient (about +2.53e-6 for Earth)
     * @param c40In
     *        un-normalized 4th zonal coefficient (about +1.62e-6 for Earth)
     * @param c50In
     *        un-normalized 5th zonal coefficient (about +2.28e-7 for Earth)
     * @param frameIn
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param parametersTypeIn
     *        initial orbit parameters type (mean or osculating)
     * @param attitudeProvForces
     *        attitude provider for force computation
     * @param attitudeProvEvents
     *        attitude provider for events computation
     * @throws PatriusException
     *         thrown if failed to build initial state or coefficients frame is not inertial
     */
    public LyddaneSecularPropagator(final Orbit initialOrbit, final double referenceRadiusIn, final double muIn,
        final double c20In, final double c30In, final double c40In, final double c50In,
        final Frame frameIn, final ParametersType parametersTypeIn, final AttitudeProvider attitudeProvForces,
        final AttitudeProvider attitudeProvEvents) throws PatriusException {
        this(initialOrbit, referenceRadiusIn, muIn, c20In, c30In, c40In,
            c50In, frameIn, parametersTypeIn, attitudeProvForces, attitudeProvEvents, null);
    }

    /**
     * Generic constructor.
     * 
     * @param initialOrbit
     *        initial orbit
     * @param referenceRadiusIn
     *        reference radius of the central body attraction model (m)
     * @param muIn
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param c20In
     *        un-normalized 2nd zonal coefficient (about -1.08e-3 for Earth)
     * @param c30In
     *        un-normalized 3rd zonal coefficient (about +2.53e-6 for Earth)
     * @param c40In
     *        un-normalized 4th zonal coefficient (about +1.62e-6 for Earth)
     * @param c50In
     *        un-normalized 5th zonal coefficient (about +2.28e-7 for Earth)
     * @param frameIn
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param parametersTypeIn
     *        initial orbit parameters type (mean or osculating)
     * @param attitudeProvider
     *        attitude provider
     * @param massProvider
     *        mass provider
     * @throws PatriusException
     *         thrown if failed to build initial state or coefficients frame is not inertial
     */
    public LyddaneSecularPropagator(final Orbit initialOrbit, final double referenceRadiusIn, final double muIn,
        final double c20In, final double c30In, final double c40In, final double c50In,
        final Frame frameIn, final ParametersType parametersTypeIn, final AttitudeProvider attitudeProvider,
        final MassProvider massProvider) throws PatriusException {
        this(initialOrbit, referenceRadiusIn, muIn, c20In, c30In, c40In, c50In, frameIn, parametersTypeIn,
            attitudeProvider, null, massProvider);
    }

    /**
     * Generic constructor.
     * 
     * @param initialOrbit
     *        initial orbit
     * @param referenceRadiusIn
     *        reference radius of the central body attraction model (m)
     * @param muIn
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param c20In
     *        un-normalized 2nd zonal coefficient (about -1.08e-3 for Earth)
     * @param c30In
     *        un-normalized 3rd zonal coefficient (about +2.53e-6 for Earth)
     * @param c40In
     *        un-normalized 4th zonal coefficient (about +1.62e-6 for Earth)
     * @param c50In
     *        un-normalized 5th zonal coefficient (about +2.28e-7 for Earth)
     * @param frameIn
     *        Inertial or quasi-inertial frame in which the model is supposed valid, the Z axis of the frame being the
     *        polar axis of the body
     * @param parametersTypeIn
     *        initial orbit parameters type (mean or osculating)
     * @param attitudeProvForces
     *        attitude provider for force computation
     * @param attitudeProvEvents
     *        attitude provider for events computation
     * @param massProvider
     *        mass provider
     * @throws PatriusException
     *         thrown if failed to build initial state or coefficients frame is not inertial
     */
    public LyddaneSecularPropagator(final Orbit initialOrbit, final double referenceRadiusIn, final double muIn,
        final double c20In, final double c30In, final double c40In, final double c50In,
        final Frame frameIn, final ParametersType parametersTypeIn, final AttitudeProvider attitudeProvForces,
        final AttitudeProvider attitudeProvEvents, final MassProvider massProvider) throws PatriusException {
        super(initialOrbit, referenceRadiusIn, muIn, c20In, c30In, c40In, c50In, frameIn, parametersTypeIn,
            attitudeProvForces, attitudeProvEvents, massProvider);

        if (parametersTypeIn.equals(ParametersType.OSCULATING)) {
            // Compute secular Orbit (= mean orbit)
            final Orbit secularOrbit = this.computeSecular(initialOrbit, LyddaneParametersType.OSCULATING);
            this.updateSecularOrbit(secularOrbit);
        } else {
            // Provided mean orbit = secular orbit
            this.updateSecularOrbit(this.secularOrbitIn);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Orbit propagateMeanOrbit(final AbsoluteDate date) throws PatriusException {
        // Mean = secular
        return this.propagateOrbit(date, this.secularOrbitIn, this.getFrame(), LyddaneParametersType.SECULAR);
    }

    /** {@inheritDoc} */
    @Override
    public Orbit osc2mean(final Orbit orbit) throws PatriusException {
        // Mean = secular, hence returned parameters are mean parameters
        final Orbit secularOrbit = this.computeSecular(orbit, LyddaneParametersType.OSCULATING);
        return this.convertFrame(secularOrbit, orbit.getFrame());
    }

    /** {@inheritDoc} */
    @Override
    public Orbit mean2osc(final Orbit orbit) throws PatriusException {
        // Mean = secular, hence propagation from secular parameters = propagation from mean parameters
        return this.propagateOrbit(orbit.getDate(), orbit, orbit.getFrame(), LyddaneParametersType.OSCULATING);
    }
}
