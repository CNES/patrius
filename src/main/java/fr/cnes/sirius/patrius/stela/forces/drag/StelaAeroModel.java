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
 * @history created 03/03/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:FA:FA-2499:27/01/2021:[PATRIUS] Anomalie dans la gestion des panneaux solaires de la classe Vehicle 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:31/03/2014:changed API for partial derivatives
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::FA:399:09/03/2015:remove C_D parameter
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.drag;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.models.AeroModel;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.drag.DragSensitive;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.Parameterizable;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.stela.bodies.EarthRotation;
import fr.cnes.sirius.patrius.stela.bodies.GeodPosition;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This class represents a STELA aero model, based on a spherical spacecraft.
 * </p>
 * <p>
 * It contains the STELA algorithm for the drag computation, as well as the STELA algorithm for the computation of the
 * partial derivatives with respect to position and velocity, in the TNW frame. <br>
 * As this class is an implementation of the {@link DragSensitive} interface, it is intended to be used in the
 * {@link StelaAtmosphericDrag} class.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @see AeroModel
 * @see StelaAtmosphericDrag
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
@SuppressWarnings("PMD.NullAssignment")
public final class StelaAeroModel extends Parameterizable implements DragSensitive {

    /** Generated serial UID. */
    private static final long serialVersionUID = 4898707923970013932L;

    /** The spacecraft mass. */
    private final double mass;

    /** The spacecraft drag coefficient. */
    private final StelaCd cd;

    /** The spacecraft cross section (m<sup>2</sup>). */
    private final double surface;

    /** The atmosphere. */
    private final Atmosphere atmosphere;

    /** The class representing the geodetic position of the spacecraft. */
    private final GeodPosition geodPosition;

    /** Atmospheric density derivatives computation by full finite differences or altitude finite differences flag. */
    private final boolean densityDerivativesByFullFiniteDifference;

    /**
     * Parameters representing:<br>
     * - dX in atmospheric density derivatives full finite differences computation (m).<br>
     * - dH in atmospheric density derivatives altitude finite differences computation (m).
     */
    private final double atmosDp;

    /**
     * Constructor to be used when partial derivatives should not be computed.
     * 
     * @param inMass
     *        the spacecraft mass.
     * @param inCd
     *        the spacecraft drag coefficient.
     * @param inSurface
     *        the spacecraft cross section (m<sup>2</sup>).
     * @throws PatriusException
     *         if the frame factory fails.
     */
    public StelaAeroModel(final double inMass, final StelaCd inCd, final double inSurface) throws PatriusException {
        super();
        this.mass = inMass;
        this.cd = inCd;
        this.surface = inSurface;
        // Atmosphere is used only when computing partial derivatives: it is set to null in this constructor
        this.atmosphere = null;
        // GeodPosition is used only when computing partial derivatives: it is set to null in this constructor
        this.geodPosition = null;
        // dX/dH is used only when computing partial derivatives: it is set to 0 in this constructor
        this.atmosDp = 0.0;
        // the densityDerivativesByFullFiniteDifference parameters is used only when computing partial derivatives
        this.densityDerivativesByFullFiniteDifference = true;
    }

    /**
     * Constructor to be used when partial derivatives are computed using the full finite differences method.
     * 
     * @param inMass
     *        the spacecraft mass.
     * @param inCd
     *        the spacecraft drag coefficient.
     * @param inSurface
     *        the spacecraft cross section (m<sup>2</sup>).
     * @param inAtmosphere
     *        the atmospheric model
     * @param atmosDX
     *        dX in atmospheric density derivatives full finite differences computation
     * @throws PatriusException
     *         if the frame factory fails.
     */
    public StelaAeroModel(final double inMass, final StelaCd inCd, final double inSurface,
        final Atmosphere inAtmosphere, final double atmosDX) throws PatriusException {
        super();
        this.mass = inMass;
        this.cd = inCd;
        this.surface = inSurface;
        this.atmosphere = inAtmosphere;
        // GeodPosition will not be used only when computing partial derivatives: it is set to null in this constructor
        this.geodPosition = null;
        this.atmosDp = atmosDX;
        // partial derivatives are computed using the full finite differences method:
        this.densityDerivativesByFullFiniteDifference = true;
    }

    /**
     * Constructor to be used when partial derivatives are computed using the altitude finite differences method.
     * 
     * @param inMass
     *        the spacecraft mass.
     * @param inCd
     *        the spacecraft drag coefficient.
     * @param inSurface
     *        the spacecraft cross section (m<sup>2</sup>).
     * @param inAtmosphere
     *        the atmospheric model
     * @param atmosDH
     *        dH in atmospheric density derivatives altitude finite differences computation
     * @param inGeodPosition
     *        the spacecraft geodetic position model
     * @throws PatriusException
     *         if the frame factory fails.
     */
    public StelaAeroModel(final double inMass, final StelaCd inCd, final double inSurface,
        final Atmosphere inAtmosphere, final double atmosDH,
        final GeodPosition inGeodPosition) throws PatriusException {
        super();
        this.mass = inMass;
        this.cd = inCd;
        this.surface = inSurface;
        this.atmosphere = inAtmosphere;
        this.geodPosition = inGeodPosition;
        // partial derivatives are computed using the altitude finite differences method:
        this.densityDerivativesByFullFiniteDifference = false;
        this.atmosDp = atmosDH;
    }

    /**
     * Return the drag acceleration in the CIRF frame.
     * 
     * @param state
     *        current state information: date, kinematics, attitude
     * @param density
     *        atmospheric density at spacecraft position
     * @param relativeVelocity
     *        relative velocity of atmosphere with respect to spacecraft, in the same inertial frame as spacecraft
     *        orbit (m/s)
     * @return spacecraft acceleration in the celestial mean of date. (m/s<sup>2</sup>)
     * @throws PatriusException
     *         if acceleration cannot be computed
     */
    @Override
    public Vector3D dragAcceleration(final SpacecraftState state, final double density,
                                     final Vector3D relativeVelocity) throws PatriusException {
        final Vector3D velocityInTIRF = this.computeVelocity(state);
        final double sCd2m = this.surface * this.cd.getCd(state.getPVCoordinates().getPosition()) / (2 * this.mass);
        final double vTIRFNorm = velocityInTIRF.getNorm();
        final double atmosCoeff = -(density * sCd2m * vTIRFNorm);
        return velocityInTIRF.scalarMultiply(atmosCoeff);
    }

    /**
     * Compute a velocity expressed in the TIRF, given a state.
     * 
     * @param state
     *        a state.
     * @return velocity in TIRF.
     * @throws PatriusException
     *         if velocity cannot be computed
     */
    private Vector3D computeVelocity(final SpacecraftState state) throws PatriusException {

        final PVCoordinates pvs = state.getPVCoordinates(state.getFrame());
        final Vector3D positionInMeanOfDate = pvs.getPosition();
        final Vector3D velocityInMeanOfDate = pvs.getVelocity();

        final double omegaDouble = EarthRotation.getERADerivative(state.getDate());
        final Vector3D omega = new Vector3D(0.0, 0.0, -omegaDouble);
        return velocityInMeanOfDate.add(Vector3D.crossProduct(omega, positionInMeanOfDate));
    }

    /** {@inheritDoc} */
    @Override
    public void addDDragAccDParam(final SpacecraftState s, final Parameter param, final double density,
                                  final Vector3D relativeVelocity, final double[] dAccdParam) throws PatriusException {
        throw PatriusException.createIllegalArgumentException(PatriusMessages.UNKNOWN_PARAMETER, param);
    }

    /** {@inheritDoc} */
    @Override
    public void addDDragAccDState(final SpacecraftState s, final double[][] dAccdPos, final double[][] dAccdVel,
            final double density, final Vector3D acceleration, final Vector3D relativeVelocity,
            final boolean computeGradientPosition, final boolean computeGradientVelocity) throws PatriusException {

        if (computeGradientPosition || computeGradientVelocity) {
            final Vector3D[] dFDECartMean = new Vector3D[6];

            final PVCoordinates pv = s.getPVCoordinates(s.getFrame());
            final Vector3D pos = pv.getPosition();
            final Vector3D vel = pv.getVelocity();

            final Vector3D posCrossVel = Vector3D.crossProduct(pos, vel);
            final double velNorm = vel.getNorm();
            final double posCrossVelNorm = posCrossVel.getNorm();

            // ********** Computation of dF/dE in CIRF frame **********

            // Densities derivatives
            final Vector3D dRho = this.computeDensitiesDerivatives(s.getDate(), pos, density, s.getFrame());

            final Vector3D vTIRF = this.computeVelocity(s);
            final double vTIRFNorm = vTIRF.getNorm();
            final double omega = EarthRotation.getERADerivative(s.getDate());
            final double sCd2m = this.surface * this.cd.getCd(pos) / (2 * this.mass);

            // V * V derivatives
            final Vector3D dVVdX = (vTIRF.scalarMultiply(MathLib.divide(-omega * (vel.getY() - omega * pos.getX()),
                vTIRFNorm))).add(Vector3D.PLUS_J.scalarMultiply(-omega * vTIRFNorm));
            final Vector3D dVVdY = (vTIRF.scalarMultiply(MathLib.divide(omega * (vel.getX() + omega * pos.getY()),
                vTIRFNorm))).add(Vector3D.PLUS_I.scalarMultiply(omega * vTIRFNorm));
            final Vector3D dVVdZ = Vector3D.ZERO;
            final Vector3D dVVdVX = (vTIRF.scalarMultiply(MathLib.divide((vel.getX() + omega * pos.getY()),
                vTIRFNorm))).add(Vector3D.PLUS_I.scalarMultiply(vTIRFNorm));
            final Vector3D dVVdVY = (vTIRF.scalarMultiply(MathLib.divide((vel.getY() - omega * pos.getX()),
                vTIRFNorm))).add(Vector3D.PLUS_J.scalarMultiply(vTIRFNorm));
            final Vector3D dVVdVZ = (vTIRF.scalarMultiply(MathLib.divide(vel.getZ(), vTIRFNorm))).add(Vector3D.PLUS_K
                .scalarMultiply(vTIRFNorm));

            // Computation in Celestial Mean of Date frame
            dFDECartMean[0] = (vTIRF.scalarMultiply(dRho.getX() * vTIRFNorm).add(dVVdX.scalarMultiply(density)))
                .scalarMultiply(-sCd2m);

            dFDECartMean[1] = (vTIRF.scalarMultiply(dRho.getY() * vTIRFNorm).add(dVVdY.scalarMultiply(density)))
                .scalarMultiply(-sCd2m);

            dFDECartMean[2] = (vTIRF.scalarMultiply(dRho.getZ() * vTIRFNorm).add(dVVdZ.scalarMultiply(density)))
                .scalarMultiply(-sCd2m);

            dFDECartMean[3] = dVVdVX.scalarMultiply(-sCd2m * density);
            dFDECartMean[4] = dVVdVY.scalarMultiply(-sCd2m * density);
            dFDECartMean[5] = dVVdVZ.scalarMultiply(-sCd2m * density);

            // ********** Computation of dF/dE in TNW frame **********

            // CIRF to TNW is simply given by a projection in this section

            // t, n, w expressions in CIRF
            final Vector3D t = vel.scalarMultiply(MathLib.divide(1., velNorm));
            final Vector3D w = posCrossVel.scalarMultiply(MathLib.divide(1., posCrossVelNorm));
            final Vector3D n = Vector3D.crossProduct(w, t).normalize();

            // t,n,w derivatives in CIRF
            final Vector3D[] tDeriv = this.computeTDerivatives(vel, velNorm);
            final Vector3D[] wDeriv = this.computeWDerivatives(pos, vel, posCrossVel, posCrossVelNorm);
            final Vector3D[] nDeriv = this.computeNDerivatives(t, w, tDeriv, wDeriv);

            // Computation in TNW frame
            if (computeGradientPosition) {
                // Derivatives with respect to x
                dAccdPos[0][0] = Vector3D.dotProduct(dFDECartMean[0], t) + Vector3D.dotProduct(acceleration, tDeriv[0]);
                dAccdPos[0][1] = Vector3D.dotProduct(dFDECartMean[0], n) + Vector3D.dotProduct(acceleration, nDeriv[0]);
                dAccdPos[0][2] = Vector3D.dotProduct(dFDECartMean[0], w) + Vector3D.dotProduct(acceleration, wDeriv[0]);

                // Derivatives with respect to y
                dAccdPos[1][0] = Vector3D.dotProduct(dFDECartMean[1], t) + Vector3D.dotProduct(acceleration, tDeriv[1]);
                dAccdPos[1][1] = Vector3D.dotProduct(dFDECartMean[1], n) + Vector3D.dotProduct(acceleration, nDeriv[1]);
                dAccdPos[1][2] = Vector3D.dotProduct(dFDECartMean[1], w) + Vector3D.dotProduct(acceleration, wDeriv[1]);

                // Derivatives with respect to z
                dAccdPos[2][0] = Vector3D.dotProduct(dFDECartMean[2], t) + Vector3D.dotProduct(acceleration, tDeriv[2]);
                dAccdPos[2][1] = Vector3D.dotProduct(dFDECartMean[2], n) + Vector3D.dotProduct(acceleration, nDeriv[2]);
                dAccdPos[2][2] = Vector3D.dotProduct(dFDECartMean[2], w) + Vector3D.dotProduct(acceleration, wDeriv[2]);
            }

            if (computeGradientVelocity) {
                // Derivatives with respect to Vx
                dAccdVel[0][0] = Vector3D.dotProduct(dFDECartMean[3], t) + Vector3D.dotProduct(acceleration, tDeriv[3]);
                dAccdVel[0][1] = Vector3D.dotProduct(dFDECartMean[3], n) + Vector3D.dotProduct(acceleration, nDeriv[3]);
                dAccdVel[0][2] = Vector3D.dotProduct(dFDECartMean[3], w) + Vector3D.dotProduct(acceleration, wDeriv[3]);

                // Derivatives with respect to Vy
                dAccdVel[1][0] = Vector3D.dotProduct(dFDECartMean[4], t) + Vector3D.dotProduct(acceleration, tDeriv[4]);
                dAccdVel[1][1] = Vector3D.dotProduct(dFDECartMean[4], n) + Vector3D.dotProduct(acceleration, nDeriv[4]);
                dAccdVel[1][2] = Vector3D.dotProduct(dFDECartMean[4], w) + Vector3D.dotProduct(acceleration, wDeriv[4]);

                // Derivatives with respect to Vz
                dAccdVel[2][0] = Vector3D.dotProduct(dFDECartMean[5], t) + Vector3D.dotProduct(acceleration, tDeriv[5]);
                dAccdVel[2][1] = Vector3D.dotProduct(dFDECartMean[5], n) + Vector3D.dotProduct(acceleration, nDeriv[5]);
                dAccdVel[2][2] = Vector3D.dotProduct(dFDECartMean[5], w) + Vector3D.dotProduct(acceleration, wDeriv[5]);
            }
        }
    }

    /**
     * Compute density derivatives with respect to x, y, z. Two methods are possible.
     * 
     * @param date
     *        the current date
     * @param pos
     *        the spacecraft position
     * @param density
     *        the density
     * @param frame
     *        the reference frame
     * @return vector containing the three derivatives
     * @throws PatriusException
     *         if density cannot be computed
     */
    private Vector3D computeDensitiesDerivatives(final AbsoluteDate date, final Vector3D pos, final double density,
                                                 final Frame frame) throws PatriusException {
        final double[] result = new double[3];

        // Atmospheric density derivatives computation by full finite differences
        if (this.densityDerivativesByFullFiniteDifference) {
            // Get position in the defined frame
            final Vector3D posX = new Vector3D(pos.getX() + this.atmosDp, pos.getY(), pos.getZ());
            final Vector3D posY = new Vector3D(pos.getX(), pos.getY() + this.atmosDp, pos.getZ());
            final Vector3D posZ = new Vector3D(pos.getX(), pos.getY(), pos.getZ() + this.atmosDp);

            // Get local atmosphere density for the position in the frame
            final double densityDx = this.atmosphere.getDensity(date, posX, frame);
            final double densityDy = this.atmosphere.getDensity(date, posY, frame);
            final double densityDz = this.atmosphere.getDensity(date, posZ, frame);
            // Direct method
            result[0] = MathLib.divide((densityDx - density), this.atmosDp);
            result[1] = MathLib.divide((densityDy - density), this.atmosDp);
            result[2] = MathLib.divide((densityDz - density), this.atmosDp);
        } else {
            // Altitude method (faster)
            final Vector3D posNewAlt = pos.scalarMultiply(1.0 + MathLib.divide(this.atmosDp, pos.getNorm()));
            final double densityDH = this.atmosphere.getDensity(date, posNewAlt, frame);
            final double dRhodH = MathLib.divide((densityDH - density), this.atmosDp);
            final double longGeodCIRF = MathLib.atan2(pos.getY(), pos.getX());
            final double latGeod = this.geodPosition.getGeodeticLatitude(pos);
            final double[] sincosLat = MathLib.sinAndCos(latGeod);
            final double sinLat = sincosLat[0];
            final double cosLat = sincosLat[1];
            final double[] sincosLon = MathLib.sinAndCos(longGeodCIRF);
            final double sinLon = sincosLon[0];
            final double cosLon = sincosLon[1];
            result[0] = cosLon * cosLat * dRhodH;
            result[1] = sinLon * cosLat * dRhodH;
            result[2] = sinLat * dRhodH;
        }

        // return density derivatives
        return new Vector3D(result);
    }

    /**
     * Compute t derivatives coordinates.
     * 
     * @param vel
     *        the spacecraft velocity
     * @param velNorm
     *        the spacecraft velocity norm
     * @return t derivatives
     */
    private Vector3D[] computeTDerivatives(final Vector3D vel, final double velNorm) {

        final Vector3D[] res = new Vector3D[6];
        res[0] = Vector3D.ZERO;
        res[1] = Vector3D.ZERO;
        res[2] = Vector3D.ZERO;
        res[3] = (vel.scalarMultiply(MathLib.divide(-vel.getX(), MathLib.pow(velNorm, 3)))).add(Vector3D.PLUS_I
            .scalarMultiply(1 / velNorm));
        res[4] = (vel.scalarMultiply(MathLib.divide(-vel.getY(), MathLib.pow(velNorm, 3)))).add(Vector3D.PLUS_J
            .scalarMultiply(1 / velNorm));
        res[5] = (vel.scalarMultiply(MathLib.divide(-vel.getZ(), MathLib.pow(velNorm, 3)))).add(Vector3D.PLUS_K
            .scalarMultiply(1 / velNorm));

        return res;
    }

    /**
     * Compute w derivatives coordinates.
     * 
     * @param pos
     *        the spacecraft position
     * @param vel
     *        the spacecraft velocity
     * @param posCrossVel
     *        (pos, vel) cross product
     * @param posCrossVelNorm
     *        (pos, vel) cross product norm
     * @return w derivatives
     */
    private Vector3D[] computeWDerivatives(final Vector3D pos, final Vector3D vel, final Vector3D posCrossVel,
                                           final double posCrossVelNorm) {
        // initialization
        final Vector3D[] res = new Vector3D[6];
        // Get spacecraft position
        final double posX = pos.getX();
        final double posY = pos.getY();
        final double posZ = pos.getZ();
        // Get spacecraft velocity
        final double velX = vel.getX();
        final double velY = vel.getY();
        final double velZ = vel.getZ();
        // Derivatives of crossProduct(pos, vel)
        final Vector3D[] pCrossVDeriv = new Vector3D[6];
        pCrossVDeriv[0] = new Vector3D(0., -velZ, velY);
        pCrossVDeriv[1] = new Vector3D(velZ, 0., -velX);
        pCrossVDeriv[2] = new Vector3D(-velY, velX, 0.);
        pCrossVDeriv[3] = new Vector3D(0., posZ, -posY);
        pCrossVDeriv[4] = new Vector3D(-posZ, 0., posX);
        pCrossVDeriv[5] = new Vector3D(posY, -posX, 0.);

        // Derivatives of 1 / norm(crossProduct(pos,vel))
        final double[] pCrossVNormInvDeriv = new double[6];
        pCrossVNormInvDeriv[0] = -velZ * (posZ * velX - posX * velZ) + velY * (posX * velY - posY * velX);
        pCrossVNormInvDeriv[1] = velZ * (posY * velZ - posZ * velY) - velX * (posX * velY - posY * velX);
        pCrossVNormInvDeriv[2] = -velY * (posY * velZ - posZ * velY) + velX * (posZ * velX - posX * velZ);
        pCrossVNormInvDeriv[3] = posZ * (posZ * velX - posX * velZ) - posY * (posX * velY - posY * velX);
        pCrossVNormInvDeriv[4] = -posZ * (posY * velZ - posZ * velY) + posX * (posX * velY - posY * velX);
        pCrossVNormInvDeriv[5] = posY * (posY * velZ - posZ * velY) - posX * (posZ * velX - posX * velZ);

        // Loop on derivatives
        for (int i = 0; i < 6; i++) {
            pCrossVNormInvDeriv[i] *= MathLib.divide(-1, MathLib.pow(posCrossVelNorm, 3));

            res[i] = (pCrossVDeriv[i].scalarMultiply(MathLib.divide(1., posCrossVelNorm))).add(posCrossVel
                .scalarMultiply(pCrossVNormInvDeriv[i]));
        }
        // return w derivatives in Celestial Mean of Date frame
        return res;
    }

    /**
     * Compute n derivatives coordinates.
     * 
     * @param t
     *        t vector
     * @param w
     *        w vector
     * @param tDeriv
     *        t derivatives
     * @param wDeriv
     *        w derivatives
     * @return n derivatives
     */
    private Vector3D[] computeNDerivatives(final Vector3D t, final Vector3D w, final Vector3D[] tDeriv,
                                           final Vector3D[] wDeriv) {
        final Vector3D[] res = new Vector3D[6];
        for (int i = 0; i < 6; i++) {
            res[i] = (Vector3D.crossProduct(wDeriv[i], t)).add(Vector3D.crossProduct(w, tDeriv[i]));

        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getJacobianParameters() {
        // return all parameters
        return this.getParameters();
    }

    /** {@inheritDoc} */
    @Override
    public DragSensitive copy(final Assembly assembly) {
        // Copy is not necessary since this model does not depend on assembly
        return this;
    }
}
