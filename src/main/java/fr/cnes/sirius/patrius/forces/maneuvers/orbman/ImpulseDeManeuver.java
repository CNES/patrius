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
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.4:DM:DM-2112:04/10/2019:[PATRIUS] Manoeuvres impulsionnelles sur increments orbitaux
 * VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION:4.4::-:04/10/2019:
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
 * Class defining an impulsive maneuver with an eccentricity and a semi major axis increment as input.
 * 
 * @author JFG
 * @since 4.4
 */
public class ImpulseDeManeuver extends ImpulseManeuver implements ImpulseParKepManeuver {

    /** Serializable UID. */
    private static final long serialVersionUID = -9102207894656821333L;

    /** Forced LOF. */
    private static final LOFType LOFTYPE = LOFType.QSW;

    /** Increment of inclination (rad). */
    private final double de;

    /** Increment of semi major axis (m). */
    private final double da;

    /** Flag to throw an exception if no solution found (true), returns 0 otherwise. */
    private final boolean throwExceptionIfNotSolution;

    /**
     * Build a new instance.
     * 
     * @param inTrigger
     *        triggering event (it must generate a <b>STOP</b> event action to trigger the maneuver)
     * @param de
     *        eccentricity increment
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
    public ImpulseDeManeuver(final EventDetector inTrigger, final double de, final double da, final double isp,
                             final MassProvider massModel, final String part, final boolean throwExceptionIfNotSolution)
        throws PatriusException {
        super(inTrigger, Vector3D.ZERO, isp, massModel, part, LOFTYPE);
        this.de = de;
        this.da = da;
        this.throwExceptionIfNotSolution = throwExceptionIfNotSolution;
    }

    /**
     * Build a new instance using propulsive and engine property.
     * 
     * @param inTrigger
     *        triggering event (it must generate a <b>STOP</b> event action to trigger the maneuver)
     * @param de
     *        eccentricity increment
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
    public ImpulseDeManeuver(final EventDetector inTrigger, final double de, final double da,
                             final PropulsiveProperty engine, final MassProvider massModel, final TankProperty tank,
                             final boolean throwExceptionIfNotSolution) {
        super(inTrigger, Vector3D.ZERO, engine, massModel, tank, LOFTYPE);
        this.de = de;
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
            // Get parameters and check final eccentricity
            final double ecc1 = param.getE();
            final double ecc2 = ecc1 + this.de;
            if (ecc2 < 0) {
                throw new ArithmeticException();
            }
            final double sma1 = param.getA();
            final double anv1 = param.getAnomaly(PositionAngle.TRUE);
            final double[] sincos = MathLib.sinAndCos(anv1);
            final double sinv1 = sincos[0];
            final double cosv1 = sincos[1];

            final double mu = param.getMu();

            final double sma2 = sma1 + this.da;

            // get the QSW velocity before
            final Vector3D velBefore = getVelocityQSW(sma1, ecc1, sinv1, cosv1, mu);

            final double p1 = sma1 * (1. - ecc1 * ecc1);
            final double p2 = sma2 * (1. - ecc2 * ecc2);

            final double cosv2 = (-1. + (1. + ecc1 * cosv1) * p2 / p1) / ecc2;

            final double ang1 = MathLib.acos(cosv2);
            final double sin1 = MathLib.sin(ang1);
            // get the QSW velocity after
            final Vector3D velAfter1 = getVelocityQSW(sma2, ecc2, sin1, cosv2, mu);
            final double dv1 = velAfter1.subtract(velBefore).getNorm();

            final double ang2 = -ang1;
            final double sin2 = MathLib.sin(ang2);
            // get the QSW velocity after
            final Vector3D velAfter2 = getVelocityQSW(sma2, ecc2, sin2, cosv2, mu);
            final double dv2 = velAfter2.subtract(velBefore).getNorm();

            // Final computation
            if (dv1 <= dv2) {
                this.deltaVSat = velAfter1.subtract(velBefore);
            } else {
                this.deltaVSat = velAfter2.subtract(velBefore);
            }
        } catch (final ArithmeticException e) {
            // Handle exception
            if (this.throwExceptionIfNotSolution) {
                // Throw exception
                throw new PatriusException(e, PatriusMessages.MANEUVER_DE_NO_FEASIBLE, this.da, this.de);
            }

            // Returns 0
            this.deltaVSat = Vector3D.ZERO;
        }
    }

    /**
     * Private method to get the QSW components of the velocity.
     * 
     * @param sma
     *        semi major axis (m)
     * @param ecc
     *        eccentricity
     * @param sinv
     *        sinus of the true anomaly
     * @param cosv
     *        cosinus of the true anomaly
     * @param mu
     *        central term of the attraction (m3/s2)
     * @return QSW components of the velocity.
     */
    private static Vector3D getVelocityQSW(final double sma, final double ecc, final double sinv, final double cosv,
                                           final double mu) {

        final double p = sma * (1. - ecc * ecc);
        final double ray = p / (1. + ecc * cosv);
        final double cin = MathLib.sqrt(mu * p);

        final double vCosGamma = cin / ray;
        final double vSinGamma = ecc * sinv * MathLib.sqrt(mu / p);

        return new Vector3D(vSinGamma, vCosGamma, 0.);
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
     * Getter for eccentricity axis increment.
     * 
     * @return eccentricity axis increment
     */
    public double getDe() {
        return this.de;
    }
}
