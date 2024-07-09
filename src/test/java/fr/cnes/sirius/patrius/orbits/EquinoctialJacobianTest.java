/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history creation 25/06/2012
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Unit test for the equinoctial parameters jacobian computation.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: EquinoctialJacobianTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 */
public class EquinoctialJacobianTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Equinoctial jacobian
         * 
         * @featureDescription Equinoctial orbit jacobian computation.
         * 
         * @coveredRequirements DV-COORD_30, DV-TRAJ_80
         * 
         */
        EQUINOCTIAL_JACOBIAN
    }

    /** Computation date */
    private AbsoluteDate date;

    /** Body mu */
    private double mu;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EQUINOCTIAL_JACOBIAN}
     * 
     * @testedMethod {@link EquinoctialOrbit#computeJacobianTrueWrtCartesian()}
     * @testedMethod {@link EquinoctialOrbit#getJacobianWrtCartesian(PositionAngle, double[][])}
     * @testedMethod {@link EquinoctialOrbit#getJacobianWrtParameters(PositionAngle, double[][])}
     * 
     * @description unit test for the equinoctial orbit jacobian computation : circular orbit.
     * 
     * @input an equinoctial circular orbit
     * 
     * @output none
     * 
     * @testPassCriteria the true anomaly equinoctial parameters wrt cartesian parameters jacobian
     *                   multiplied by the cartesian parameters wrt anomaly equinoctial parameters jacobian is the
     *                   identity matrix.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testCircularOrbit() {

        // initialisations
        final double[][] idRef = { { 1., 0., 0., 0., 0., 0. },
            { 0., 1., 0., 0., 0., 0. },
            { 0., 0., 1., 0., 0., 0. },
            { 0., 0., 0., 1., 0., 0. },
            { 0., 0., 0., 0., 1., 0. },
            { 0., 0., 0., 0., 0., 1. } };
        final double[][] jacobianParam = new double[6][6];
        final double[][] jacobianCart = new double[6][6];
        final double[][] jacobianParam2 = new double[6][6];
        final double[][] jacobianCart2 = new double[6][6];

        // orbit
        final double a = 7200000.;
        final double e = 0.e-3;
        final double i = 63. * MathUtils.DEG_TO_RAD;
        final double pa = 20. * MathUtils.DEG_TO_RAD;
        final double raan = 30 * MathUtils.DEG_TO_RAD;
        final KeplerianOrbit initOrb = new KeplerianOrbit(a, e, i, pa, raan, 0., PositionAngle.TRUE,
            FramesFactory.getEME2000(), this.date, this.mu);
        final double hx = initOrb.getHx();
        final double hy = initOrb.getHy();
        final double ex = initOrb.getEquinoctialEx();
        final double ey = initOrb.getEquinoctialEy();

        for (int m = 0; m < 10; m++) {
            final double anomaly = FastMath.PI / 5. * m;

            final EquinoctialOrbit equi =
                new EquinoctialOrbit(a, ex, ey, hx, hy,
                    anomaly, PositionAngle.TRUE,
                    FramesFactory.getEME2000(), this.date, this.mu);
            final EquinoctialOrbit equi2 =
                new EquinoctialOrbit(a, ex, ey, hx, hy,
                    anomaly, PositionAngle.ECCENTRIC,
                    FramesFactory.getEME2000(), this.date, this.mu);
            // jacobians computation
            equi.getJacobianWrtParameters(PositionAngle.TRUE, jacobianParam);
            equi.getJacobianWrtCartesian(PositionAngle.TRUE, jacobianCart);
            final Array2DRowRealMatrix jacobianParamMatrix = new Array2DRowRealMatrix(jacobianParam);
            final Array2DRowRealMatrix jacobianCartMatrix = new Array2DRowRealMatrix(jacobianCart);
            equi2.getJacobianWrtParameters(PositionAngle.ECCENTRIC, jacobianParam2);
            equi2.getJacobianWrtCartesian(PositionAngle.ECCENTRIC, jacobianCart2);
            final Array2DRowRealMatrix jacobianParamMatrix2 = new Array2DRowRealMatrix(jacobianParam2);
            final Array2DRowRealMatrix jacobianCartMatrix2 = new Array2DRowRealMatrix(jacobianCart2);

            // expected identity matrix : JacobianWrtParameters * JacobianWrtCartesian
            final Array2DRowRealMatrix expectedIdentity = jacobianParamMatrix.multiply(jacobianCartMatrix);
            final Array2DRowRealMatrix expectedIdentity2 = jacobianParamMatrix2.multiply(jacobianCartMatrix2);

            // identity test
            for (int k = 0; k < 6; k++) {
                final double[] row = expectedIdentity.getRow(k);
                final double[] row2 = expectedIdentity2.getRow(k);
                final double[] rowRef = idRef[k];
                for (int j = 0; j < row.length; j++) {
                    Assert.assertEquals(rowRef[j], row[j], 1.0e-12);
                    Assert.assertEquals(rowRef[j], row2[j], 1.0e-11);
                }
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#EQUINOCTIAL_JACOBIAN}
     * 
     * @testedMethod {@link EquinoctialOrbit#computeJacobianTrueWrtCartesian()}
     * @testedMethod {@link EquinoctialOrbit#getJacobianWrtCartesian(PositionAngle, double[][])}
     * @testedMethod {@link EquinoctialOrbit#getJacobianWrtParameters(PositionAngle, double[][])}
     * 
     * @description unit test for the equinoctial orbit jacobian computation : elliptic orbit.
     * 
     * @input an equinoctial elliptic orbit
     * 
     * @output none
     * 
     * @testPassCriteria the true anomaly equinoctial parameters wrt cartesian parameters jacobian
     *                   multiplied by the cartesian parameters wrt anomaly equinoctial parameters jacobian is the
     *                   identity matrix.
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testEllipticOrbit() {

        // initialisations
        final double[][] idRef = { { 1., 0., 0., 0., 0., 0. },
            { 0., 1., 0., 0., 0., 0. },
            { 0., 0., 1., 0., 0., 0. },
            { 0., 0., 0., 1., 0., 0. },
            { 0., 0., 0., 0., 1., 0. },
            { 0., 0., 0., 0., 0., 1. } };
        final double[][] jacobianParam = new double[6][6];
        final double[][] jacobianCart = new double[6][6];

        // orbit
        final double a = 40000000.;
        final double e = 0.8;
        final double i = 15. * MathUtils.DEG_TO_RAD;
        final double pa = 20. * MathUtils.DEG_TO_RAD;
        final double raan = 30 * MathUtils.DEG_TO_RAD;
        final KeplerianOrbit initOrb = new KeplerianOrbit(a, e, i, pa, raan, 0., PositionAngle.TRUE,
            FramesFactory.getEME2000(), this.date, this.mu);
        final double hx = initOrb.getHx();
        final double hy = initOrb.getHy();
        final double ex = initOrb.getEquinoctialEx();
        final double ey = initOrb.getEquinoctialEy();

        for (int m = 0; m < 10; m++) {
            final double anomaly = FastMath.PI / 5. * m;

            final EquinoctialOrbit equi =
                new EquinoctialOrbit(a, ex, ey, hx, hy,
                    anomaly, PositionAngle.TRUE,
                    FramesFactory.getEME2000(), this.date, this.mu);

            // jacobians computation
            equi.getJacobianWrtParameters(PositionAngle.TRUE, jacobianParam);
            equi.getJacobianWrtCartesian(PositionAngle.TRUE, jacobianCart);
            final Array2DRowRealMatrix jacobianParamMatrix = new Array2DRowRealMatrix(jacobianParam);
            final Array2DRowRealMatrix jacobianCartMatrix = new Array2DRowRealMatrix(jacobianCart);

            // expected identity matrix : JacobianWrtParameters * JacobianWrtCartesian
            final Array2DRowRealMatrix expectedIdentity = jacobianParamMatrix.multiply(jacobianCartMatrix);

            // identity test
            for (int k = 0; k < 6; k++) {
                final double[] row = expectedIdentity.getRow(k);
                final double[] rowRef = idRef[k];
                for (int j = 0; j < row.length; j++) {
                    Assert.assertEquals(rowRef[j], row[j], 1.0e-10);
                }
            }
        }
    }

    /**
     * Before method
     */
    @Before
    public void setUp() {

        // Computation date
        this.date = AbsoluteDate.J2000_EPOCH;

        // Body mu
        this.mu = 3.9860047e14;
    }
}
