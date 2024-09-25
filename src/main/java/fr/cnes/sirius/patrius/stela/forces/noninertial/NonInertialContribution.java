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
 * @history created 02/03/2015
 *
 * HISTORY
 * VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3287:22/05/2023:[PATRIUS] Courtes periodes traînee atmospherique et prs
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:317:02/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.noninertial;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.modprecession.IAUMODPrecession;
import fr.cnes.sirius.patrius.frames.configuration.precessionnutation.CIPCoordinates;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaGaussContribution;
import fr.cnes.sirius.patrius.stela.forces.Squaring;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * Class representing the non-inertial contribution for STELA propagator.
 *
 * @author Emmanuel Bignon
 * @concurrency thread-safe
 * @version $Id$
 * @since 3.0
 *
 */
public class NonInertialContribution extends AbstractStelaGaussContribution {

     /** Serializable UID. */
    private static final long serialVersionUID = 990001814363351174L;

    /** Omega derivative finite differences delta time (s). */
    private static final double DT = Constants.JULIAN_DAY;

    /** Number of points for Simpson' squaring. */
    private final int quadPoints;

    /** Integration frame ({@link FramesFactory#getCIRF()}. */
    private final Frame integrationFrame = FramesFactory.getCIRF();

    /** Reference system. */
    private final Frame referenceSystem;

    /**
     * Constructor.
     *
     * @param quadPointsIn
     *        number of points for Simpson' squaring (must be odd)
     * @param referenceSystemIn
     *        reference system
     */
    public NonInertialContribution(final int quadPointsIn, final Frame referenceSystemIn) {
        super();
        this.quadPoints = quadPointsIn;
        this.referenceSystem = referenceSystemIn;
    }

    /** {@inheritDoc} */
    @Override
    public double[] computePerturbation(final StelaEquinoctialOrbit orbit,
                                        final OrbitNatureConverter converter) throws PatriusException {

        double[] res = new double[6];

        // Computation performed only if integration frame different from reference system
        if (this.integrationFrame != this.referenceSystem) {

            // Initialization
            final double[] daDt = new double[this.quadPoints];
            final double[] dexDt = daDt.clone();
            final double[] deyDt = daDt.clone();
            final double[] dixDt = daDt.clone();
            final double[] diyDt = daDt.clone();
            final double[] dLamEqDt = daDt.clone();

            // Computation of squaring points (in mean parameters)
            final StelaEquinoctialOrbit[] pvSquaring = Squaring.computeSquaringPointsEccentric(this.quadPoints, orbit);

            // Rotation vector and its derivative between integration frame and reference system
            final Vector3D omega = this.computeOmega(orbit.getDate(), this.referenceSystem, this.integrationFrame);
            final Vector3D domegadt =
                this.computeOmegaDerivative(orbit.getDate(), this.referenceSystem, this.integrationFrame, DT);

            // Delta in eccentric anomaly between two points
            final double deltavi = 2. * FastMath.PI / (this.quadPoints - 1);

            // Loop on quadrature points
            for (int i = 0; i < this.quadPoints; i++) {

                // Convert into cartesian parameters
                final PVCoordinates pvT1 = pvSquaring[i].getPVCoordinates();

                // Compute acceleration (sum of 3 terms)
                final Vector3D t1 = Vector3D.crossProduct(omega, pvT1.getVelocity()).scalarMultiply(2.);
                final Vector3D t2 = Vector3D.crossProduct(omega, Vector3D.crossProduct(omega, pvT1.getPosition()));
                final Vector3D t3 = Vector3D.crossProduct(domegadt, pvT1.getPosition());
                Vector3D acceleration = new Vector3D(
                    -t1.getX() - t2.getX() - t3.getX(),
                    -t1.getY() - t2.getY() - t3.getY(),
                    -t1.getZ() - t2.getZ() - t3.getZ());

                // Project acceleration in local orbital frame TNW
                final LocalOrbitalFrame tnw = new LocalOrbitalFrame(orbit.getFrame(), LOFType.TNW, pvSquaring[i],
                    "TNW");
                final Transform transform = tnw.getParent().getTransformTo(tnw, pvSquaring[i].getDate());
                acceleration = transform.transformVector(acceleration);

                // Computation of contribution to Gauss equations (in mean parameters)
                final double[][] gaussEq = computeGaussEquations(pvSquaring[i]);
                final double[] instantGaussEq = JavaMathAdapter.matrixVectorMultiply(gaussEq, acceleration.toArray());
                final double adjustCoef = computeAdjustCoef(pvSquaring[i], i * deltavi);
                daDt[i] = adjustCoef * instantGaussEq[0];
                dLamEqDt[i] = adjustCoef * instantGaussEq[1];
                dexDt[i] = adjustCoef * instantGaussEq[2];
                deyDt[i] = adjustCoef * instantGaussEq[3];
                dixDt[i] = adjustCoef * instantGaussEq[4];
                diyDt[i] = adjustCoef * instantGaussEq[5];
            }

            // Simpson's rule
            res = new double[] {
                Squaring.simpsonMean(daDt, deltavi),
                Squaring.simpsonMean(dLamEqDt, deltavi),
                Squaring.simpsonMean(dexDt, deltavi),
                Squaring.simpsonMean(deyDt, deltavi),
                Squaring.simpsonMean(dixDt, deltavi),
                Squaring.simpsonMean(diyDt, deltavi) };
        }

        return res;
    }

    /** {@inheritDoc} */
    @Override
    public double[] computeShortPeriods(final StelaEquinoctialOrbit orbit,
            final OrbitNatureConverter converter) throws PatriusException {
        // Not implemented yet
        return new double[6];
    }

    /** {@inheritDoc} */
    @Override
    public double[][] computePartialDerivatives(final StelaEquinoctialOrbit orbit) throws PatriusException {
        // Not implemented yet
        return new double[6][6];
    }

    /**
     * Compute a coefficient to represent the irregular distribution of eccentric anomalies.
     *
     * @param orbit
     *        an orbit
     * @param eAnom
     *        orbit eccentric anomaly
     * @return adjustment coefficient
     */
    private static double computeAdjustCoef(final StelaEquinoctialOrbit orbit, final double eAnom) {
        final double ex = orbit.getEquinoctialEx();
        final double ey = orbit.getEquinoctialEy();
        return 1.0 - MathLib.sqrt(ex * ex + ey * ey) * MathLib.cos(eAnom);
    }

    /**
     * Compute rotation vector derivative from frame1 to frame2 using finite differences.
     *
     * @param date
     *        a date
     * @param frame1
     *        reference frame
     * @param frame2
     *        expression frame
     * @param dt
     *        time difference for finite differences (s)
     * @return rotation vector derivative from frame1 to frame2
     * @throws PatriusException
     *         thrown if conversion is not available
     */
    public Vector3D computeOmegaDerivative(final AbsoluteDate date, final Frame frame1,
                                           final Frame frame2, final double dt) throws PatriusException {
        final Vector3D omega1 = this.computeOmega(date, frame1, frame2);
        final AbsoluteDate date2 = date.shiftedBy(dt);
        final Vector3D omega2 = this.computeOmega(date2, frame1, frame2);
        return new Vector3D(MathLib.divide(1., dt), omega2.subtract(omega1));
    }

    // ============================== Omega vector computation ============================== //

    /**
     * Compute rotation vector of frame2 with respect to frame1 expressed in frame2,
     * which is the rotation vector from frame1 to frame2.
     * Currently coded conversion are:
     * <ul>
     * <li>Frame 1 (reference system): CIRF, ICRF, MOD</li>
     * <li>Frame 2 (expression/integration frame): CIRF</li>
     * </ul>
     *
     * @param date
     *        a date
     * @param frame1
     *        reference frame
     * @param frame2
     *        expression frame
     * @return rotation vector of frame2 with respect to frame1 expressed in frame2
     * @throws PatriusException
     *         thrown if conversion is not available
     */
    public Vector3D computeOmega(final AbsoluteDate date, final Frame frame1,
                                 final Frame frame2) throws PatriusException {

        // Initialization
        Vector3D res = Vector3D.ZERO;

        // Currently frame tree is not entirely coded because only few conversions are available
        // and are not meant to be used out of the integration process.
        // Conversions: frame1 => frame2
        if (FramesFactory.getCIRF().equals(frame2)) {

            if (FramesFactory.getGCRF().equals(frame1)) {
                // Direct conversion
                res = computeOmegaICRFToCIRF(date);

            } else if (FramesFactory.getMOD(false).equals(frame1)) {

                // Transform from ICRF to CIRF
                final Rotation tICRFToCIRF = FramesFactory.getGCRF().getTransformTo(FramesFactory.getCIRF(),
                    date).getRotation();
                // Transform from EME2000 to ICRF
                final Rotation tEME2000ToICRF = FramesFactory.getEME2000().getTransformTo(FramesFactory.getGCRF(),
                    date).getRotation();
                // Rotation vector from MOD to EME2000
                final Vector3D omegaMODToEME2000 = computeOmegaMODToEME2000(date);
                // Rotation vector from MOD to ICRF
                final Vector3D omegaMODToICRF = tEME2000ToICRF.applyInverseTo(omegaMODToEME2000);
                // Rotation vector from ICRF to CIRF
                final Vector3D omegaICRFToCIRF = computeOmegaICRFToCIRF(date);
                // Final rotation vector: MOD to CIRF
                res = tICRFToCIRF.applyInverseTo(omegaMODToICRF).add(omegaICRFToCIRF);

            } else if (!FramesFactory.getCIRF().equals(frame1)) {
                throw new PatriusException(PatriusMessages.STELA_REFERENCE_SYSTEM_NOT_SUPPORTED);
            }
        } else {
            throw new PatriusException(PatriusMessages.STELA_INTEGRATION_FRAME_NOT_SUPPORTED);
        }

        return res;
    }

    /**
     * Compute rotation vector from ICRF to CIRF.
     *
     * @param date
     *        a date
     * @return rotation vector from ICRF to CIRF
     */
    private static Vector3D computeOmegaICRFToCIRF(final AbsoluteDate date) {

        // Compute coefficients
        final CIPCoordinates cipCoordinates = FramesFactory.getConfiguration().getCIPCoordinates(date);
        final double x = cipCoordinates.getX();
        final double y = cipCoordinates.getY();
        final double s = cipCoordinates.getS();
        final double dx = cipCoordinates.getxP();
        final double dy = cipCoordinates.getyP();
        final double ds = cipCoordinates.getsP();

        // Build intermediate variables
        final double u = x * x + y * y;
        final double mE = MathLib.atan2(y, x);
        final double d = MathLib.asin(MathLib.min(1.0, MathLib.sqrt(u)));
        final double dE = MathLib.divide(x * dy - y * dx, u);
        final double dd = MathLib.divide(x * dx + y * dy, MathLib.sqrt(MathLib.max(0.0, u * (1. - u))));

        // Build angles
        final double[] theta = { mE, d, -(mE + s) };
        final double[] dtheta = { dE, dd, -(dE + ds) };

        // Compute omega
        return computeOmega(theta, dtheta);
    }

    /**
     * Compute rotation vector from MOD to EME2000.
     *
     * @param date
     *        a date
     * @return rotation vector from MOD to EME2000
     */
    private static Vector3D computeOmegaMODToEME2000(final AbsoluteDate date) {
        final double[] euler = getEulerAngles(date);
        final double[] theta = { euler[2], -euler[1], euler[0] };
        final double[] dtheta = { euler[5], -euler[4], euler[3] };
        return computeOmega(theta, dtheta);
    }

    /**
     * Compute Euler angles (3, 2, 3) of MOD => GCRF/EME2000 transformation and their derivatives with respect to time
     * only in case if IAU precession model.
     * <p>
     * Frames configuration must contain a {@link IAUMODPrecession} model, an exception is thrown otherwise.
     * </p>
     * .
     * @param date
     *        a date
     * @return Euler angles (3, 2, 3) of MOD => GCRF/EME2000 transformation and their derivatives with respect to time
     */
    private static double[] getEulerAngles(final AbsoluteDate date) {

        // Get model
        final FramesConfiguration config = FramesFactory.getConfiguration();
        if (!(config.getMODPrecessionModel() instanceof IAUMODPrecession)) {
            // STELA configuration has an IAUMODPrecession, but nothing prevents the user from using a different
            // configuration
            throw new PatriusRuntimeException(PatriusMessages.WRONG_MOD_TYPE, null);
        }
        final IAUMODPrecession model = (IAUMODPrecession) config.getMODPrecessionModel();
        // Compute polynomial derivatives
        final PolynomialFunction zetaDerivative = model.getPolynomialPrecessionZeta().derivative();
        final PolynomialFunction thetaDerivative = model.getPolynomialPrecessionTheta().derivative();
        final PolynomialFunction zDerivative = model.getPolynomialPrecessionZ().derivative();

        // Duration in centuries since J2000 epoch
        final double ttc = date.durationFromJ2000EpochInCenturies();

        // Euler angles
        final double zeta = model.getPolynomialPrecessionZeta().value(ttc);
        final double theta = model.getPolynomialPrecessionTheta().value(ttc);
        final double z = model.getPolynomialPrecessionZ().value(ttc);

        // Euler angles derivatives
        final double dzeta = zetaDerivative.value(ttc);
        final double dtheta = thetaDerivative.value(ttc);
        final double dz = zDerivative.value(ttc);

        // Return result
        return new double[] { zeta, theta, z, dzeta / Constants.JULIAN_CENTURY, dtheta / Constants.JULIAN_CENTURY,
            dz / Constants.JULIAN_CENTURY };
    }

    /**
     * Compute rotation vector between two frames given Euler angles and their derivatives.
     *
     * @param theta
     *        Euler angles
     * @param dtheta
     *        Euler angles derivatives
     * @return rotation vector
     */
    private static Vector3D computeOmega(final double[] theta, final double[] dtheta) {
        final double[] sincos2 = MathLib.sinAndCos(theta[1]);
        final double sin2 = sincos2[0];
        final double cos2 = sincos2[1];
        final double[] sincos3 = MathLib.sinAndCos(theta[2]);
        final double sin3 = sincos3[0];
        final double cos3 = sincos3[1];
        return new Vector3D(
            -cos3 * sin2 * dtheta[0] + sin3 * dtheta[1],
            sin3 * sin2 * dtheta[0] + cos3 * dtheta[1],
            cos2 * dtheta[0] + dtheta[2]);
    }
}
