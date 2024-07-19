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
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.4:DM:DM-2112:04/10/2019:[PATRIUS] Manoeuvres impulsionnelles sur increments orbitaux
 * VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.maneuvers.orbman;

import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.forces.maneuvers.ImpulseManeuver;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Class defining an impulsive maneuver with a inclination and eventually a semi major axis increment as input.
 * 
 * @author JFG
 * 
 * @since 4.4
 */
public class ImpulseDiManeuver extends ImpulseManeuver implements ImpulseParKepManeuver {

    /** Serializable UID. */
    private static final long serialVersionUID = -3917491588064716923L;

    /** Forced LOF. */
    private static final LOFType LOFTYPE = LOFType.TNW;

    /** Increment of inclination (rad). */
    private final double di;

    /** Increment of semi major axis (m). */
    private final double da;

    /** Flag to throw an exception if no solution found (true), returns 0 otherwise. */
    private final boolean throwExceptionIfNotSolution;

    /**
     * Build a new instance.
     * 
     * @param inTrigger
     *        triggering event (it must generate a <b>STOP</b> event action to trigger the maneuver)
     * @param di
     *        inclination increment (rad)
     * @param isp
     *        engine specific impulse (s)
     * @param massModel
     *        mass model
     * @param part
     *        part of the mass model that provides the propellant
     * @param throwExceptionIfNotSolution
     *        flag to throw an exception if no solution found (true), returns 0 otherwise
     * @throws PatriusException
     *         if mass from mass provider is negative
     */
    public ImpulseDiManeuver(final EventDetector inTrigger, final double di, final double isp,
                             final MassProvider massModel, final String part, final boolean throwExceptionIfNotSolution)
        throws PatriusException {
        this(inTrigger, di, 0., isp, massModel, part, throwExceptionIfNotSolution);
    }

    /**
     * Build a new instance.
     * 
     * @param inTrigger
     *        triggering event (it must generate a <b>STOP</b> event action to trigger the maneuver)
     * @param di
     *        inclination increment (rad)
     * @param da
     *        semi major axis increment (m)
     * @param isp
     *        engine specific impulse (s)
     * @param massModel
     *        mass model
     * @param part
     *        part of the mass model that provides the propellant
     * @param throwExceptionIfNotSolution
     *        flag to throw an exception if no solution found (true), returns 0 otherwise
     * @throws PatriusException
     *         if mass from mass provider is negative
     */
    public ImpulseDiManeuver(final EventDetector inTrigger, final double di, final double da, final double isp,
                             final MassProvider massModel, final String part, final boolean throwExceptionIfNotSolution)
        throws PatriusException {
        super(inTrigger, Vector3D.ZERO, isp, massModel, part, LOFTYPE);
        this.di = di;
        this.da = da;
        this.throwExceptionIfNotSolution = throwExceptionIfNotSolution;
    }

    /**
     * Build a new instance using propulsive and engine property.
     * 
     * @param inTrigger
     *        triggering event (it must generate a <b>STOP</b> event action to trigger the maneuver)
     * @param di
     *        inclination increment (rad)
     * @param engine
     *        engine property (specific impulse)
     * @param massModel
     *        mass model
     * @param tank
     *        tank property gathering mass and part name information
     * @param throwExceptionIfNotSolution
     *        flag to throw an exception if no solution found (true), returns 0 otherwise
     */
    public ImpulseDiManeuver(final EventDetector inTrigger, final double di, final PropulsiveProperty engine,
                             final MassProvider massModel, final TankProperty tank,
                             final boolean throwExceptionIfNotSolution) {
        this(inTrigger, di, 0., engine, massModel, tank, throwExceptionIfNotSolution);
    }

    /**
     * Build a new instance using propulsive and engine property.
     * 
     * @param inTrigger
     *        triggering event (it must generate a <b>STOP</b> event action to trigger the maneuver)
     * @param di
     *        inclination increment (rad)
     * @param da
     *        semi major axis increment (m)
     * @param engine
     *        engine property (specific impulse)
     * @param massModel
     *        mass model
     * @param tank
     *        tank property gathering mass and part name information
     * @param throwExceptionIfNotSolution
     *        flag to throw an exception if no solution found (true), returns 0 otherwise
     */
    public ImpulseDiManeuver(final EventDetector inTrigger, final double di, final double da,
                             final PropulsiveProperty engine, final MassProvider massModel, final TankProperty tank,
                             final boolean throwExceptionIfNotSolution) {
        super(inTrigger, Vector3D.ZERO, engine, massModel, tank, LOFTYPE);
        this.di = di;
        this.da = da;
        this.throwExceptionIfNotSolution = throwExceptionIfNotSolution;
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
        // DeltaV computation
        computeDV(oldState);
        return super.resetState(oldState);
    }

    /** {@inheritDoc} */
    @Override
    public void computeDV(final SpacecraftState state) throws PatriusException {

        try {
            // Getting Keplerian parameters
            final KeplerianParameters param = state.getOrbit().getParameters().getKeplerianParameters();

            // DV computation
            // Get parameters
            final double sma = param.getA();
            final double exc = param.getE();
            final double pom = param.getPerigeeArgument();
            final double anv = param.getAnomaly(PositionAngle.TRUE);
            final double ane = param.getAnomaly(PositionAngle.ECCENTRIC);
            final double mu = param.getMu();

            // Intermediate variables
            final double na2 = MathLib.sqrt(mu * sma);
            final double ray = sma * (1. - exc * MathLib.cos(ane));
            final double twoOnR = 2. / ray;
            final double vBefore = MathLib.sqrt(mu * (twoOnR - (1. / sma)));
            final double cosAol = MathLib.cos(pom + anv);

            // initialize dvi & dvT
            double dvi = 0.;
            double dvT = 0.;
            if (MathLib.abs(cosAol) > 0.) {
                dvi = na2 * MathLib.sqrt(1. - exc * exc) * this.di / (ray * cosAol);
                final double dvTCompensate = MathLib.sqrt(vBefore * vBefore - dvi * dvi) - vBefore;
                final double vAfter = MathLib.sqrt(mu * (twoOnR - (1. / (sma + this.da))));
                dvT = dvTCompensate + vAfter - vBefore;
            }

            // Build DV
            this.deltaVSat = new Vector3D(dvT, 0., dvi);
        } catch (final ArithmeticException e) {
            // Handle exception
            if (this.throwExceptionIfNotSolution) {
                // Throw exception
                throw new PatriusException(e, PatriusMessages.MANEUVER_DI_NO_FEASIBLE, this.da, this.di);
            }
            // Returns 0
            this.deltaVSat = Vector3D.ZERO;
        }
    }

    /**
     * Getter for semi-major axis increment.
     * 
     * @return semi-major axis increment
     */
    public double getDa() {
        return this.da;
    }

    /**
     * Getter for inclination increment.
     * 
     * @return inclination increment
     */
    public double getDi() {
        return this.di;
    }
}
