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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:523:08/02/2016: add solid tides effects in STELA PATRIUS
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.stela.forces.gravity;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.StelaEquinoctialParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaLagrangeContribution;
import fr.cnes.sirius.patrius.stela.forces.StelaLagrangeEquations;
import fr.cnes.sirius.patrius.stela.orbits.JacobianConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Class representing the tidal contribution.
 * </p>
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread safe if the CelestialBody used is thread safe
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id$
 * 
 * @since 3.2
 * 
 */
public class SolidTidesAcc extends AbstractStelaLagrangeContribution {

     /** Serializable UID. */
    private static final long serialVersionUID = 7391985871705908766L;

    /**
     * Inverse of tidal Love coefficients.
     * First value (degree 1) is 0.
     * NB: valid for Earth only
     */
    private static final double[] TIDAL_LOVE_COEFFICIENTS = { 0, 0, 0.3, 0.094 };

    /** Sun computation switch. */
    private final boolean computeSunContribution;

    /** Moon computation switch. */
    private final boolean computeMoonContribution;

    /** Sun. */
    private final CelestialBody sun;

    /** Moon. */
    private final CelestialBody moon;

    /**
     * Default constructor: both sun and moon contributions have to be computed.
     * 
     * @param sunBody
     *        Sun
     * @param moonBody
     *        Moon
     */
    public SolidTidesAcc(final CelestialBody sunBody, final CelestialBody moonBody) {
        this(true, true, sunBody, moonBody);
    }

    /**
     * Advanced constructor: the user can choose if the sun or the moon contribution has to be deactivated.
     * 
     * @param computeSunContributionFlag
     *        true if sun contribution to be taken into account
     * @param computeMoonContributionFlag
     *        true if moon contribution to be taken into account
     * @param sunBody
     *        Sun (may be null if Sun contribution is not taken into account)
     * @param moonBody
     *        Moon (may be null if Moon contribution is not taken into account)
     */
    public SolidTidesAcc(final boolean computeSunContributionFlag, final boolean computeMoonContributionFlag,
        final CelestialBody sunBody, final CelestialBody moonBody) {
        super();
        this.computeSunContribution = computeSunContributionFlag;
        this.computeMoonContribution = computeMoonContributionFlag;
        this.sun = sunBody;
        this.moon = moonBody;
    }

    /** {@inheritDoc} */
    @Override
    public double[] computePerturbation(final StelaEquinoctialOrbit orbit) throws PatriusException {
        double[] result = new double[6];

        if (this.computeSunContribution) {
            result = vectAdd(result, computePerturbation(orbit, this.sun));
        }
        if (this.computeMoonContribution) {
            result = vectAdd(result, computePerturbation(orbit, this.moon));
        }

        return result;
    }

    /**
     * Compute perturbation for the considered third body
     * 
     * @param orbit
     *        type 8 Position-velocity
     * @param body
     *        third body
     * @return tides perturbation for this third body
     * @throws PatriusException
     *         Orekit exception needed for using the provider
     */
    private static double[] computePerturbation(final StelaEquinoctialOrbit orbit,
                                         final CelestialBody body) throws PatriusException {

        // ==== Initialization
        final Vector3D celBodyVect = body.getPVCoordinates(orbit.getDate(), orbit.getFrame()).getPosition();

        // position of satellite in the type 1 bulletin
        final CartesianParameters pvT1R0 = orbit.getEquinoctialParameters().getCartesianParameters();

        // ==== 1. Define new frame

        // computes the angular momentum and normalize (no mass needed in product)
        final Vector3D pos = pvT1R0.getPosition();
        final Vector3D vel = pvT1R0.getVelocity();
        final Vector3D angularMomentumUnit = pos.crossProduct(vel).normalize();

        // rZ is oriented parallel of the vector joining the center of the earth to the celestial object.
        int sign = 1;
        if (angularMomentumUnit.dotProduct(celBodyVect) < 0) {
            sign = -1;
        }

        // Defining a new reference frame fixed w.r.t R0
        final Vector3D rZ = celBodyVect.normalize().scalarMultiply(sign);
        final Vector3D rX = Vector3D.PLUS_K;
        // rY is automatically defined by the rotation;

        // CommonsMath conventions on rotations bring need to transpose
        final double[][] rotationMatrix = new Rotation(Vector3D.PLUS_K, Vector3D.PLUS_I, rZ, rX).revert().getMatrix();

        final double[][] dR0dR = new double[6][6];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                dR0dR[i][j] = rotationMatrix[i][j];
                dR0dR[i + 3][j + 3] = rotationMatrix[i][j];
            }
        }

        // ==== 2. Express state in new frame

        // Defining the position velocity in this new frame:
        // computes pvT1R parameters using the R0 to R transformation Jacobian
        final double[] cartesianParams = { pos.getX(), pos.getY(), pos.getZ(), vel.getX(), vel.getY(), vel.getZ() };
        final double[] pvT1RValues = JavaMathAdapter.matrixVectorMultiply(dR0dR, cartesianParams);
        final Vector3D p = new Vector3D(pvT1RValues[0], pvT1RValues[1], pvT1RValues[2]);
        final Vector3D v = new Vector3D(pvT1RValues[3], pvT1RValues[4], pvT1RValues[5]);
        final CartesianParameters pvT1R = new CartesianParameters(new PVCoordinates(p, v), pvT1R0.getMu());

        // Convert in type 8
        final StelaEquinoctialParameters pvT8R = pvT1R.getStelaEquinoctialParameters();
        final StelaEquinoctialOrbit pvT8ROrb = new StelaEquinoctialOrbit(pvT8R, orbit.getFrame(), orbit.getDate());

        // J1 = E2 -> pv2
        final double[][] jac1 = JacobianConverter.computeEquinoctialToCartesianJacobian(pvT8ROrb);
        // J2 = pv2 -> pv
        final double[][] jac2 = JavaMathAdapter.matrixTranspose(dR0dR);
        // J3 = pv -> E1
        final Orbit stelaorb = new StelaEquinoctialOrbit(pvT1R0, orbit.getFrame(), orbit.getDate());
        final double[][] jac3 = new double[6][6];
        stelaorb.getJacobianWrtCartesian(PositionAngle.MEAN, jac3);
        // final double[][] jac3 = JacobianConverter.computeT1toT8Jacobian(pvT1R0);

        // J3 * J2 * J1
        final double[][] jacE2ToE = JavaMathAdapter.matrixMultiply(JavaMathAdapter.matrixMultiply(jac3, jac2), jac1);

        // ==== 3. Compute zonals derivatives

        // Contributions of the zonals derivatives in E2 with a new reference radius, a new mu and new zonal coefficient
        final double muCelesBody = body.getGM();
        final double distEarthCelestBody = celBodyVect.getNorm();

        final double tidesRefRadius = MathLib.divide(Constants.CNES_STELA_AE
            * Constants.CNES_STELA_AE, distEarthCelestBody);
        final double tidesMu = MathLib.divide(muCelesBody * Constants.CNES_STELA_AE, distEarthCelestBody);

        // Zonals partial derivatives
        final PotentialCoefficientsProviderTides provider = new PotentialCoefficientsProviderTides(tidesRefRadius,
            tidesMu);
        final StelaZonalAttraction zonalAttraction = new StelaZonalAttraction(provider, 3, false, 2, 3, false);
        final double[] zonalsContrib = zonalAttraction.computeJ3(pvT8ROrb, tidesMu);

        // Conversion to temporal derivatives using Poisson brackets with a specific mu
        final StelaLagrangeEquations lagrangeEq = new StelaLagrangeEquations();
        final double[] pertE2 = JavaMathAdapter.matrixVectorMultiply(lagrangeEq.computeLagrangeEquations(pvT8ROrb,
            tidesMu), zonalsContrib);

        // Factor
        final double factor = MathLib.sqrt(tidesMu / Constants.CNES_STELA_MU);
        for (int i = 0; i < zonalsContrib.length; ++i) {
            pertE2[i] = factor * pertE2[i];
        }

        // Result: we transform back the derivatives to the first frame
        return JavaMathAdapter.matrixVectorMultiply(jacE2ToE, pertE2);
    }

    /**
     * Returns a vector corresponding to the addition of the two input vectors.
     * 
     * @param u0
     *        1st vector
     * @param u1
     *        2nd vector
     * @return u0 + u1
     */
    private static double[] vectAdd(final double[] u0, final double[] u1) {
        final double[] res = new double[6];
        for (int i = 0; i < 6; i++) {
            res[i] = u0[i] + u1[i];
        }
        return res;
    }

    /**
     * Get the Sun.
     * 
     * @return Sun
     */
    public CelestialBody getSun() {
        return this.sun;
    }

    /**
     * Get the Moon.
     * 
     * @return Moon
     */
    public CelestialBody getMoon() {
        return this.moon;
    }

    /** {@inheritDoc} */
    @Override
    public double[] computeShortPeriods(final StelaEquinoctialOrbit orbit) throws PatriusException {
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
     * Local class to provide potential coefficients
     */
    private static class PotentialCoefficientsProviderTides implements PotentialCoefficientsProvider {

         /** Serializable UID. */
        private static final long serialVersionUID = -9039653513823221317L;

        /** Central body reference radius : ae (m) */
        private final double ae;

        /** Central body attraction coefficient : mu (m<sup>3</sup>/s<sup>2</sup>) */
        private final double mu;

        /**
         * Create a PotentialCoefficientsProvider to deal with Tides effects.
         * 
         * @param aeParam
         *        Central body reference radius : ae (m)
         * @param muParam
         *        Central body attraction coefficient : mu (m<sup>3</sup>/s<sup>2</sup>)
         * 
         * @since 3.1
         */
        public PotentialCoefficientsProviderTides(final double aeParam, final double muParam) {
            this.ae = aeParam;
            this.mu = muParam;
        }

        /** {@inheritDoc} */
        @Override
        @SuppressWarnings("PMD.MethodReturnsInternalArray")
        public double[] getJ(final boolean normalized, final int n) throws PatriusException {
            return TIDAL_LOVE_COEFFICIENTS;
        }

        /** {@inheritDoc} */
        @Override
        public double[][] getC(final int n, final int m, final boolean normalized) throws PatriusException {
            // This method is not called: return null
            return new double[0][0];
        }

        /** {@inheritDoc} */
        @Override
        public double[][] getS(final int n, final int m, final boolean normalized) throws PatriusException {
            // This method is not called: return null
            return new double[0][0];
        }

        /** {@inheritDoc} */
        @Override
        public double getMu() {
            return this.mu;
        }

        /** {@inheritDoc} */
        @Override
        public double getAe() {
            return this.ae;
        }
    }
}
