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
 * @history Created on 18/07/2013
 *
 *
 * HISTORY
 * VERSION:4.13:DM:DM-101:08/12/2023:[PATRIUS] Harmonisation des eclipses pour les evenements et pour la PRS
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3244:03/11/2022:[PATRIUS] Ajout propagation du signal dans ExtremaElevationDetector
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2899:15/11/2021:[PATRIUS] Autres corps occultants que la Terre pour la SRP 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.8:DM:DM-2900:15/11/2021:[PATRIUS] Possibilite de desactiver les eclipses pour la SRP 
 * VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:18/07/2013:Created the Solar Radiation wrench model
 * VERSION::DM:227:09/04/2014:Merged eclipse detectors
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:675:01/09/2016:corrected anomalies reducing the performances
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.wrenches;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressure;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.Parameterizable;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents a solar radiation wrench model. It requires a spacecraft capable
 * of computing the wrench caused by solar radiation pressure.
 * 
 * @see RadiationWrenchSensitive
 * 
 * @author Rami Houdroge
 * @version $Id$
 * @since 2.1
 * 
 * @concurrency not thread-safe
 * @concurrency.comment class uses internal mutable attributes and frames
 */
public class SolarRadiationWrench extends Parameterizable implements WrenchModel {

    /** Serializable UID. */
    private static final long serialVersionUID = 2682628484008113102L;

    /** Parameter name for absorption coefficient. */
    private static final String SUN_DISTANCE = "reference sun distance";

    /** Parameter name for reflection coefficient. */
    private static final String SUN_PRESSURE = "reference sun pressure";

    /** Sun radius (m). */
    private static final String SUN_RADIUS = "reference sun radius";

    /** Sun model. */
    private final PVCoordinatesProvider sun;

    /** Earth model. */
    private final BodyShape earthModel;

    /** Spacecraft. */
    private final RadiationWrenchSensitive spacecraft;

    /** Parameter for reference sun distance */
    private Parameter sunDistance = null;

    /** Parameter for reference sun pressure */
    private Parameter sunPressure = null;

    /** Cached Satellite-Sun vector. */
    private Vector3D cachedSatSunVector;

    /** Cached satellite position. */
    private Vector3D cachedPosition;

    /** Cached date. */
    private AbsoluteDate cachedDate;

    /**
     * Simple constructor with default reference values.
     * <p>
     * When this constructor is used, the reference values are:
     * </p>
     * <ul>
     * <li>d<sub>ref</sub> = 149597870000.0 m</li>
     * <li>p<sub>ref</sub> = 4.56 10<sup>-6</sup> N/m<sup>2</sup></li>
     * </ul>
     * 
     * @param sunBody
     *        Sun model
     * @param shape
     *        earth model (for umbra/penumbra computation)
     * @param spacecraftModel
     *        the object physical and geometrical information
     */
    public SolarRadiationWrench(final PVCoordinatesProvider sunBody, final BodyShape shape,
                                final RadiationWrenchSensitive spacecraftModel) {
        this(Constants.SEIDELMANN_UA, Constants.CONST_SOL_N_M2, Constants.IERS92_SUN_EQUATORIAL_RADIUS,
                sunBody, shape, spacecraftModel);
    }

    /**
     * Complete constructor.
     * <p>
     * Note that reference solar radiation pressure <code>pRef</code> in N/m<sup>2</sup> is linked to solar flux SF in
     * W/m<sup>2</sup> using formula pRef = SF/c where c is the speed of light (299792458 m/s). So at 1UA a 1367
     * W/m<sup>2</sup> solar flux is a 4.56 10<sup>-6</sup> N/m<sup>2</sup> solar radiation pressure.
     * </p>
     * 
     * @param distance
     *        reference distance for the solar radiation pressure
     * @param pressure
     *        reference solar radiation pressure at dRef
     * @param sunRadius
     *        reference sun radius
     * @param sunBody
     *        Sun model
     * @param shape
     *        earth model (for umbra/penumbra computation)
     * @param spacecraftModel
     *        the object physical and geometrical information
     */
    public SolarRadiationWrench(final double distance, final double pressure, final double sunRadius,
                                final PVCoordinatesProvider sunBody,
                                final BodyShape shape, final RadiationWrenchSensitive spacecraftModel) {
        this(new Parameter(SUN_DISTANCE, distance), new Parameter(SUN_PRESSURE, pressure), new Parameter(SUN_RADIUS,
            sunRadius), sunBody, shape, spacecraftModel);
    }

    /**
     * Complete constructor using {@link Parameter}.
     * 
     * @param distance
     *        the parameter representing the reference distance for the solar radiation pressure
     * @param pressure
     *        the parameter representing the reference solar radiation pressure at dRef
     * @param sunRadius
     *        the parameter representing the reference sun radius
     * @param sunBody
     *        Sun model
     * @param shape
     *        earth model (for umbra/penumbra computation)
     * @param spacecraftModel
     *        the object physical and geometrical information
     */
    public SolarRadiationWrench(final Parameter distance, final Parameter pressure, final Parameter sunRadius,
                                final PVCoordinatesProvider sunBody,
                                final BodyShape shape, final RadiationWrenchSensitive spacecraftModel) {
        super(distance, pressure, sunRadius);
        this.sunDistance = distance;
        this.sunPressure = pressure;
        this.sun = sunBody;
        this.earthModel = shape;
        this.spacecraft = spacecraftModel;
        this.cachedSatSunVector = Vector3D.NaN;
        this.cachedPosition = Vector3D.NaN;
        this.cachedDate = AbsoluteDate.PAST_INFINITY;
    }

    /**
     * Compute sat-Sun vector in spacecraft state frame.
     * 
     * @param state
     *        current spacecraft state
     * @return sat-Sun vector in spacecraft state frame
     * @exception PatriusException
     *            if sun position cannot be computed
     */
    protected Vector3D getSatSunVector(final SpacecraftState state) throws PatriusException {
        // Invalidate the cache if input date or position are different
        if (this.cachedDate.compareTo(state.getDate()) != 0. ||
                state.getPVCoordinates().getPosition().distance(this.cachedPosition) != 0.) {

            final PVCoordinates sunPV = this.sun.getPVCoordinates(state.getDate(), state.getFrame());
            final PVCoordinates satPV = state.getPVCoordinates();

            // Compute cached quantities
            this.cachedSatSunVector = sunPV.getPosition().subtract(satPV.getPosition());
            this.cachedDate = state.getDate();
            this.cachedPosition = state.getPVCoordinates().getPosition();
        }

        return this.cachedSatSunVector;
    }

    /**
     * Compute radiation coefficient.
     * 
     * @param s
     *        spacecraft state
     * @return coefficient for acceleration computation
     * @exception PatriusException
     *            if position cannot be computed
     */
    protected double computeRawP(final SpacecraftState s) throws PatriusException {
        final AbsoluteDate date = s.getDate();
        final Frame frame = s.getFrame();
        final Vector3D satSunVector = this.getSatSunVector(s);
        final double r2 = satSunVector.getNormSq();
        final SolarRadiationPressure srp = new SolarRadiationPressure(this.sun, this.earthModel, null);
        return MathLib.divide(this.sunPressure.getValue() * this.sunDistance.getValue() * this.sunDistance.getValue()
                * srp.getLightingRatio(satSunVector, this.earthModel, s.getOrbit(), frame, date), r2);
    }

    /** {@inheritDoc} */
    @Override
    public Wrench computeWrench(final SpacecraftState s) throws PatriusException {
        final Vector3D satSunVector = this.getSatSunVector(s);
        final double rawP = this.computeRawP(s);
        final Vector3D flux = new Vector3D(MathLib.divide(-rawP, satSunVector.getNorm()), satSunVector);
        // raw radiation pressure
        return this.spacecraft.radiationWrench(s, flux);
    }

    /** {@inheritDoc} */
    @Override
    public Wrench computeWrench(final SpacecraftState s, final Vector3D origin,
                                final Frame frame) throws PatriusException {
        final Vector3D satSunVector = this.getSatSunVector(s);
        final double rawP = this.computeRawP(s);
        final Vector3D flux = new Vector3D(MathLib.divide(-rawP, satSunVector.getNorm()), satSunVector);
        // raw radiation pressure
        return this.spacecraft.radiationWrench(s, flux, origin, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeTorque(final SpacecraftState s) throws PatriusException {
        return this.computeWrench(s).getTorque();
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeTorque(final SpacecraftState s, final Vector3D origin,
                                  final Frame frame) throws PatriusException {
        return this.computeWrench(s, origin, frame).getTorque();
    }
}
