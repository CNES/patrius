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
 *
 * HISTORY
 * VERSION:4.9.2:FA:FA-3236:21/09/2022:[PATRIUS] Vérification du repère pseudo-inertiel dans shiftedBy()
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.5:FA:FA-2468:27/05/2020:Robustesse methode shiftedBy de la classe Orbit
 * VERSION:4.4:FA:FA-2134:04/10/2019:Modifications mineures d'api - corrections
 * VERSION:4.3:DM:DM-2082:15/05/2019:Modifications mineures d'api
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
 * VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:481:05/10/2015: TU for method getJacobian of class Orbit
 * VERSION::DM:426:05/10/2015: Add test to create an orbit in a non inertial frame
 * VERSION::DM:482:02/12/2015 :Move tests to class KeplerianParametersTest
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.orbits;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.CheckUtils;
import fr.cnes.sirius.patrius.math.linear.DecompositionSolver;
import fr.cnes.sirius.patrius.math.linear.DiagonalMatrix;
import fr.cnes.sirius.patrius.math.linear.MatrixUtils;
import fr.cnes.sirius.patrius.math.linear.QRDecomposition;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Pair;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.AlternateEquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.ApsisOrbit;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.EquatorialOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.tools.validationTool.Validate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Tests class for OREKIT Orbit class. Here we want to test orbit parameters conversion accuracy vs. 3rd party provided
 * outputs. 3rd party data come from CNES MSLIB 90 V6.13 library.
 * </p>
 * 
 * @author Sylvain VRESK
 * @author Philippe Pavero
 * 
 * @version $Id: OrbitTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 * 
 */
public class OrbitTest {

    /** validation tool */
    static Validate validate;

    /** Default parameter date sets to J2000 epoch. */
    private final AbsoluteDate date = new AbsoluteDate();
    /** Earth parameter sets as the MSLIB used one. */
    private final double mu = 3.9860047e14;

    /** Epsilon used for distance comparison. */
    private final double epsilonDistance = Utils.epsilonTest;

    /** Epsilon used for distance comparison. */
    private final double epsilonAngle = Utils.epsilonAngle;

    /** Epsilon used for distance comparison. */
    private final double epsilonE = Utils.epsilonE;

    /** Non regression Epsilon */
    private final double epsilonNonReg = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon used for double comparisons. */
    private final double epsilonComparison = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Convert cartesian parameters to circular equatorial adapted ones.
         * 
         * @featureDescription the purpose of this feature is to increase the cover of the Orbit class
         * 
         * @coveredRequirements DV-COORD_90, DV-COORD_100, DV-COORD_110
         */
        CARTESIAN2CIRCULAREQUATORIAL,
        /**
         * @featureTitle Convert cartesian parameters to circular non-equatorial ones.
         * 
         * @featureDescription the purpose of this feature is to increase the cover of the Orbit class
         * 
         * @coveredRequirements DV-COORD_90, DV-COORD_100, DV-COORD_110
         */
        CARTESIAN2CIRCULAR,
        /**
         * @featureTitle Convert cartesian parameters to keplerian ones.
         * 
         * @featureDescription the purpose of this feature is to increase the cover of the Orbit class
         * 
         * @coveredRequirements DV-COORD_90, DV-COORD_100, DV-COORD_110
         */
        CARTESIAN2KEPLERIAN,
        /**
         * @featureTitle Convert circular non-equatorial parameters to keplerian ones.
         * 
         * @featureDescription the purpose of this feature is to increase the cover of the Orbit class
         * 
         * @coveredRequirements DV-COORD_90, DV-COORD_100, DV-COORD_110
         */
        CIRCULAR2CARTESIAN,
        /**
         * @featureTitle Convert circular equatorial parameters to cartesian ones.
         * 
         * @featureDescription the purpose of this feature is to increase the cover of the Orbit class
         * 
         * @coveredRequirements DV-COORD_90, DV-COORD_100, DV-COORD_110
         */
        CIRCULAREQUATORIAL2CARTESIAN,
        /**
         * @featureTitle Convert circular equatorial parameters to keplerian ones.
         * 
         * @featureDescription the purpose of this feature is to increase the cover of the Orbit class
         * 
         * @coveredRequirements DV-COORD_90, DV-COORD_100, DV-COORD_110
         */
        CIRCULAREQUATORIAL2KEPLERIAN,
        /**
         * @featureTitle Convert circular non-equatorial parameters to keplerian ones.
         * 
         * @featureDescription the purpose of this feature is to increase the cover of the Orbit class
         * 
         * @coveredRequirements DV-COORD_90, DV-COORD_100, DV-COORD_110
         */
        CIRCULAR2KEPLERIAN,
        /**
         * @featureTitle Convert keplerian parameters to cartesian ones.
         * 
         * @featureDescription the purpose of this feature is to increase the cover of the Orbit class
         * 
         * @coveredRequirements DV-COORD_90, DV-COORD_100, DV-COORD_110
         */
        KEPLERIAN2CARTESIAN,
        /**
         * @featureTitle Convert keplerian parameters to circular equatorial ones.
         * 
         * @featureDescription the purpose of this feature is to increase the cover of the Orbit class
         * 
         * @coveredRequirements DV-COORD_90, DV-COORD_100, DV-COORD_110
         */
        KEPLERIAN2CIRCULAREQUATORIAL,
        /**
         * @featureTitle Convert keplerian parameters to circular non-equatorial ones.
         * 
         * @featureDescription the purpose of this feature is to increase the cover of the Orbit class
         * 
         * @coveredRequirements DV-COORD_90, DV-COORD_100, DV-COORD_110
         */
        KEPLERIAN2CIRCULAR,
        /**
         * @featureTitle Convert keplerian parameters to equinoctial ones.
         * 
         * @featureDescription the purpose of this feature is to increase the cover of the Orbit class
         * 
         * @coveredRequirements DV-COORD_90, DV-COORD_100, DV-COORD_110
         */
        KEPLERIAN2EQUINOCTIAL,
        /**
         * @featureTitle Convert keplerian parameters to equinoctial ones.
         * 
         * @featureDescription the purpose of this feature is to increase the cover of the Orbit class
         * 
         * @coveredRequirements DV-COORD_90, DV-COORD_100, DV-COORD_110
         */
        EQUINOCTIAL2KEPLERIAN,
        /**
         * @featureTitle coverage
         * 
         * @featureDescription coverage tests
         * 
         * @coveredRequirements DV-COORD_90, DV-COORD_100, DV-COORD_110
         * 
         */
        COVERAGE
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#CARTESIAN2CIRCULAREQUATORIAL}
     * 
     * @testedMethod {@link CircularOrbit#getA()}
     * @testedMethod {@link CircularOrbit#getEquinoctialEx()}
     * @testedMethod {@link CircularOrbit#getEquinoctialEy()}
     * @testedMethod {@link CircularOrbit#getI()}
     * @testedMethod {@link CircularOrbit#getRightAscensionOfAscendingNode()}
     * @testedMethod {@link CircularOrbit#getLM()}
     * 
     * @description convert cartesian parameters to circular adapted ones:
     *              <p>
     *              <ul>
     *              <li>a</li>
     *              <li>e cos(&omega; + &Omega;)</li>
     *              <li>e sin(&omega; + &Omega;)</li>
     *              <li>2 sin(i/2) cos(&Omega;)</li>
     *              <li>2 sin(i/2) cos(&Omega;)</li>
     *              <li>&omega; + &Omega; + M</li>
     *              </ul>
     *              </p>
     * 
     * @input Vector3D position = (-29536113.0, 30329259.0, -100125.0) : position vector coordinates
     * @input Vector3D velocity = (-2194.0, -2141.0, -8.0) : velocity vector coordinates
     * @output a
     * @output ex
     * @output ey
     * @output i
     * @output RAAN
     * @output AoP + M
     * 
     * @testPassCriteria deviations from reference below thresholds. For distances comparison, the epsilon is set to
     *                   1e-12; for angles comparison, the epsilon is set to 1e-7; for eccentricity comparison the
     *                   epsilon is set to 1e-7. These epsilons take into account the fact that the measures are
     *                   physical and that their references come from another software.
     * 
     * @comments MSLIB 90 provided outputs are hard coded in tests cases
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void Cartesian2CircularEquatorial() {

        // Case #1...
        // . awaited output values (MSLIB F90 V13)
        final double expected_a = 0.422551700028257e+08;
        final double expected_ex = 0.592732497856475e-03;
        final double expected_ey = -0.206274396964359e-02;
        final double expected_ix = 0.128021863908325e-03;
        final double expected_iy = -0.352136186881817e-02;
        final double expected_LM = 0.234498139679291e+01;
        // . input cartesian parameters
        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);

        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final CircularOrbit orbit = new CircularOrbit(pvCoordinates, FramesFactory.getEME2000(), this.date, this.mu);

        // ... computes ix and iy as there is no getters for them in OREKIT
        final double actual_ix = 2. * MathLib.sin(orbit.getI() / 2.)
            * MathLib.cos(orbit.getRightAscensionOfAscendingNode());
        final double actual_iy = 2. * MathLib.sin(orbit.getI() / 2.)
            * MathLib.sin(orbit.getRightAscensionOfAscendingNode());
        // ... and &omega; + &Omega; + M
        final double actual_LM = MathUtils.normalizeAngle(orbit.getLM(), FastMath.PI);

        // ... expected vs. actual values comparisons
        validate.assertEqualsWithRelativeTolerance(orbit.getA(), 4.225517000282565E7, this.epsilonNonReg, expected_a,
            this.epsilonDistance, "Cartesian2CircularEquatorial_A");
        validate.assertEqualsWithRelativeTolerance(orbit.getEquinoctialEx(), 5.927324978565528E-4, this.epsilonNonReg,
            expected_ex, this.epsilonE, "Cartesian2CircularEquatorial_ex");
        validate.assertEqualsWithRelativeTolerance(orbit.getEquinoctialEy(), -0.002062743969643666, this.epsilonNonReg,
            expected_ey, this.epsilonE, "Cartesian2CircularEquatorial_ey");
        validate.assertEqualsWithRelativeTolerance(actual_ix, 1.2802186390832473E-4, this.epsilonNonReg, expected_ix,
            this.epsilonAngle, "Cartesian2CircularEquatorial_ix");
        validate.assertEqualsWithRelativeTolerance(actual_iy, -0.003521361868818174, this.epsilonNonReg, expected_iy,
            this.epsilonAngle, "Cartesian2CircularEquatorial_iy");
        validate.assertEqualsWithRelativeTolerance(actual_LM, 2.3449813967929107, this.epsilonNonReg, expected_LM,
            this.epsilonAngle, "Cartesian2CircularEquatorial_LM");
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#CARTESIAN2CIRCULAR}
     * 
     * @testedMethod {@link CircularOrbit#getA()}
     * @testedMethod {@link CircularOrbit#getCircularEx()}
     * @testedMethod {@link CircularOrbit#getCircularEy()}
     * @testedMethod {@link CircularOrbit#getI()}
     * @testedMethod {@link CircularOrbit#getRightAscensionOfAscendingNode()}
     * @testedMethod {@link CircularOrbit#getAlphaM()}
     * 
     * @description convert Cartesian parameters to non-equatorial circular adapted ones:
     *              <p>
     *              <ul>
     *              <li>a</li>
     *              <li>e cos(&omega; + &Omega;)</li>
     *              <li>e sin(&omega; + &Omega;)</li>
     *              <li>i</li>
     *              <li>&Omega;</li>
     *              <li>&omega; + M</li>
     *              </ul>
     *              </p>
     * 
     * @input Vector3D position = (-5910180.0, 4077714.0, -620640.0) : position vector coordinates
     * @input Vector3D velocity = (129.0, -1286.0, -7325.0) : velocity vector coordinates
     * @output a
     * @output ex
     * @output ey
     * @output i
     * @output RAAN
     * @output AoP + M
     * 
     * @testPassCriteria deviations from reference below thresholds. For distances comparison, the epsilon is set to
     *                   1e-12; for angles comparison, the epsilon is set to 1e-7; for eccentricity comparison the
     *                   epsilon is set to 1e-7. These epsilons take into account the fact that the measures are
     *                   physical and that their references come from another software. For the jacobian matrices
     *                   comparison, a relative epsilon of 1e-12 has been choose i.e. 100 times the epsilon for double
     *                   comparison because we compare physical measurements and the references come from another
     *                   software.
     * 
     * @comments MSLIB 90 provided outputs are hard coded in tests cases
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void Cartesian2Circular() {

        // Case #1...
        // . awaited output values (MSLIB F90 V13)
        final double expected_a = 0.720975463897266e+07;
        final double expected_ex = 0.274726836351512e-02;
        final double expected_ey = -0.271013410790988e-01;
        final double expected_i = 0.143839407777104e+01;
        final double expected_raan = 0.566772604045703e+01;
        final double expected_alphaM = 0.328306099515957e+01;
        // . input cartesian parameters
        final Vector3D position = new Vector3D(-5910180.0, 4077714.0, -620640.0);
        final Vector3D velocity = new Vector3D(129.0, -1286.0, -7325.0);

        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final CircularOrbit orbit = new CircularOrbit(pvCoordinates, FramesFactory.getEME2000(), this.date, this.mu);

        validate.assertEqualsWithRelativeTolerance(orbit.getA(), 7209754.6389726605, this.epsilonNonReg, expected_a,
            this.epsilonDistance, "Cartesian2Circular_A");
        validate.assertEqualsWithRelativeTolerance(orbit.getCircularEx(), 0.0027472683635153448, this.epsilonNonReg,
            expected_ex, this.epsilonE, "Cartesian2Circular_ex");
        validate.assertEqualsWithRelativeTolerance(orbit.getCircularEy(), -0.027101341079098732, this.epsilonNonReg,
            expected_ey, this.epsilonE, "Cartesian2Circular_ey");
        this.assertAngleEqualsRelative(orbit.getI(), 1.4383940777710431, this.epsilonNonReg, expected_i,
            this.epsilonAngle,
            "Cartesian2Circular_I");
        this.assertAngleEqualsRelative(orbit.getRightAscensionOfAscendingNode(), -0.6154592667225562,
            this.epsilonNonReg,
            expected_raan, this.epsilonAngle, "Cartesian2Circular_raan");
        this.assertAngleEqualsRelative(orbit.getAlphaM(), -3.000124312020019, this.epsilonNonReg, expected_alphaM,
            this.epsilonAngle,
            "Cartesian2Circular_alphaM");

        final double[][] expected_jacobian = new double[6][6];
        expected_jacobian[0][0] = -0.164126594910825e+01;
        expected_jacobian[1][0] = 0.113402693003580e-06;
        expected_jacobian[2][0] = 0.123089646642988e-07;
        expected_jacobian[3][0] = 0.905366610220937e-08;
        expected_jacobian[4][0] = -0.796130629299504e-07;
        expected_jacobian[5][0] = 0.965379669767051e-08;
        expected_jacobian[0][1] = 0.113238736187426e+01;
        expected_jacobian[1][1] = -0.750973535500126e-07;
        expected_jacobian[2][1] = -0.306149699247824e-07;
        expected_jacobian[3][1] = 0.128043726307041e-07;
        expected_jacobian[4][1] = -0.112594755816985e-06;
        expected_jacobian[5][1] = 0.377878769793844e-07;
        expected_jacobian[0][2] = -0.172352669234193e+00;
        expected_jacobian[1][2] = 0.274170516151221e-07;
        expected_jacobian[2][2] = -0.134717771002270e-06;
        expected_jacobian[3][2] = -0.208853246087379e-08;
        expected_jacobian[4][2] = 0.183654294693078e-07;
        expected_jacobian[5][2] = 0.134403073131062e-06;
        expected_jacobian[0][3] = 0.336451810610772e+02;
        expected_jacobian[1][3] = 0.108899411187307e-04;
        expected_jacobian[2][3] = -0.109697681309514e-03;
        expected_jacobian[3][3] = 0.766761297987564e-04;
        expected_jacobian[4][3] = 0.674553602414258e-05;
        expected_jacobian[5][3] = 0.219603122138341e-03;
        expected_jacobian[0][4] = -0.335408549182521e+03;
        expected_jacobian[1][4] = 0.355070357790733e-04;
        expected_jacobian[2][4] = 0.800284709387734e-04;
        expected_jacobian[3][4] = 0.108441125035959e-03;
        expected_jacobian[4][4] = 0.954004221846468e-05;
        expected_jacobian[5][4] = -0.152798746950723e-03;
        expected_jacobian[0][5] = -0.191047249048365e+04;
        expected_jacobian[1][5] = 0.265213202506583e-03;
        expected_jacobian[2][5] = 0.151378139518269e-04;
        expected_jacobian[3][5] = -0.176879271061030e-04;
        expected_jacobian[4][5] = -0.155608466154692e-05;
        expected_jacobian[5][5] = 0.269754102806348e-04;

        final double[][] actual_jacobian = new double[6][6];
        final double[][] jacobian_nonReg = {
            { -1.6412659491082473, 1.1323873618742555, -0.17235266923419296, 33.64518106107715, -335.4085491825211,
                -1910.4724904836446 },
            { 1.1340269300358035E-7, -7.509735355001262E-8, 2.7417051615122066E-8, 1.0889941118730742E-5,
                3.550703577907328E-5, 2.65213202506583E-4 },
            { 1.2308964664298807E-8, -3.061496992478239E-8, -1.347177710022699E-7, -1.0969768130951445E-4,
                8.002847093877333E-5, 1.5137813951826867E-5 },
            { 9.053666102209374E-9, 1.2804372630704127E-8, -2.0885324608737883E-9, 7.66761297987564E-5,
                1.0844112503595947E-4, -1.768792710610297E-5 },
            { -7.96130629299504E-8, -1.1259475581698545E-7, 1.8365429469307803E-8, 6.745536024142583E-6,
                9.54004221846469E-6, -1.556084661546937E-6 },
            { 9.653796697670509E-9, 3.77878769793844E-8, 1.3440307313106187E-7, 2.196031221383407E-4,
                -1.52798746950723E-4, 2.697541028063479E-5 } };
        orbit.getJacobianWrtCartesian(PositionAngle.MEAN, actual_jacobian);

        assert2DArrayEqualsAbsolute(actual_jacobian, jacobian_nonReg, this.epsilonNonReg, expected_jacobian,
            1.e4 * this.epsilonNonReg);
        assert2DArrayEqualsRelative(actual_jacobian, jacobian_nonReg, this.epsilonNonReg, expected_jacobian,
            1.e2 * this.epsilonNonReg);
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#CARTESIAN2KEPLERIAN}
     * 
     * @testedMethod {@link KeplerianOrbit#getA()}
     * @testedMethod {@link KeplerianOrbit#getE()}
     * @testedMethod {@link KeplerianOrbit#getI()}
     * @testedMethod {@link KeplerianOrbit#getPerigeeArgument()}
     * @testedMethod {@link KeplerianOrbit#getRightAscensionOfAscendingNode()}
     * @testedMethod {@link KeplerianOrbit#getMeanAnomaly()}
     * 
     * @description convert Cartesian parameters to classical keplerian ones:
     *              <p>
     *              <ul>
     *              <li>a</li>
     *              <li>e</li>
     *              <li>i</li>
     *              <li>&omega;</li>
     *              <li>&Omega;</li>
     *              <li>M, E or &nu;</li>
     *              </ul>
     *              </p>
     * 
     * @input Vector3D position = (-26655470.0 ,29881667.0, -113657.0) : position vector coordinates
     * @input Vector3D velocity = (-1125.0, -1122.0, 195.0) : velocity vector coordinates
     * @output a
     * @output e
     * @output i
     * @output AoP
     * @output RAAN
     * @output M
     * 
     * @testPassCriteria deviations from reference below thresholds. For distances comparison, the epsilon is set to
     *                   1e-12; for angles comparison, the epsilon is set to 1e-7; for eccentricity comparison the
     *                   epsilon is set to 1e-7. These epsilons take into account the fact that the measures are
     *                   physical and that their references come from another software. For the jacobian matrices
     *                   comparison, a relative epsilon of 1e-12 has been choose i.e. 100 times the epsilon for double
     *                   comparison because we compare physical measurements and the references come from another
     *                   software.
     * 
     * @comments MSLIB 90 provided outputs are hard coded in tests cases
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void Cartesian2Keplerian() {

        // Case #1...
        // . awaited output values (MSLIB F90 V13)
        final double expected_a = 0.229792653030773e+08;
        final double expected_e = 0.743502611664700e+00;
        final double expected_i = 0.122182096220906e+00;
        final double expected_w = 0.309909041016672e+01;
        final double expected_W = 0.232231010979999e+01;
        final double expected_M = 0.322888977629034e+01;
        // . input cartesian parameters
        final Vector3D position = new Vector3D(-26655470.0, 29881667.0, -113657.0);
        final Vector3D velocity = new Vector3D(-1125.0, -1122.0, 195.0);

        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final KeplerianOrbit orbit = new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(), this.date, this.mu);

        validate.assertEqualsWithRelativeTolerance(orbit.getA(), 2.297926530307729E7, this.epsilonNonReg, expected_a,
            this.epsilonDistance, "Cartesian2Keplerian_A");
        validate.assertEqualsWithRelativeTolerance(orbit.getE(), 0.7435026116647002, this.epsilonNonReg, expected_e,
            this.epsilonE, "Cartesian2Keplerian_E");
        this.assertAngleEqualsRelative(orbit.getI(), 0.12218209622090577, this.epsilonNonReg, expected_i,
            this.epsilonAngle,
            "Cartesian2Keplerian_I");
        this.assertAngleEqualsRelative(orbit.getPerigeeArgument(), 3.0990904101667147, this.epsilonNonReg, expected_w,
            this.epsilonAngle, "Cartesian2Keplerian_w");
        this.assertAngleEqualsRelative(orbit.getRightAscensionOfAscendingNode(), 2.322310109799988, this.epsilonNonReg,
            expected_W, this.epsilonAngle, "Cartesian2Keplerian_W");
        this.assertAngleEqualsRelative(orbit.getMeanAnomaly(), -3.054295530889249, this.epsilonNonReg, expected_M,
            this.epsilonAngle,
            "Cartesian2Keplerian_M");

        final double[][] expected_jacobian = new double[6][6];
        expected_jacobian[0][0] = -0.438438954539129e+00;
        expected_jacobian[1][0] = 0.488031679287745e-08;
        expected_jacobian[2][0] = 0.719453589126244e-10;
        expected_jacobian[3][0] = -0.629611862756901e-08;
        expected_jacobian[4][0] = -0.182654951154998e-07;
        expected_jacobian[5][0] = 0.280889103733995e-07;
        expected_jacobian[0][1] = 0.491504626981494e+00;
        expected_jacobian[1][1] = -0.422450416363797e-08;
        expected_jacobian[2][1] = 0.672277584200444e-10;
        expected_jacobian[3][1] = -0.569689469658728e-08;
        expected_jacobian[4][1] = -0.170677902175538e-07;
        expected_jacobian[5][1] = 0.268618695607263e-07;
        expected_jacobian[0][2] = -0.186947205418077e-02;
        expected_jacobian[1][2] = -0.836927787203211e-10;
        expected_jacobian[2][2] = 0.801887557558934e-09;
        expected_jacobian[3][2] = 0.206154896193309e-06;
        expected_jacobian[4][2] = -0.203583295533501e-06;
        expected_jacobian[5][2] = -0.477215287822542e-08;
        expected_jacobian[0][3] = -0.298069123251592e+04;
        expected_jacobian[1][3] = 0.239812042357971e-03;
        expected_jacobian[2][3] = 0.557004212594298e-04;
        expected_jacobian[3][3] = -0.118112755204554e-03;
        expected_jacobian[4][3] = -0.106461609145762e-04;
        expected_jacobian[5][3] = 0.643083267852883e-03;
        expected_jacobian[0][4] = -0.297274272256255e+04;
        expected_jacobian[1][4] = 0.209638448235643e-03;
        expected_jacobian[2][4] = 0.520480336872222e-04;
        expected_jacobian[3][4] = 0.184417257386707e-03;
        expected_jacobian[4][4] = -0.994807093721294e-05;
        expected_jacobian[5][4] = -0.741173153653056e-03;
        expected_jacobian[0][5] = 0.516653146969427e+03;
        expected_jacobian[1][5] = -0.390913739926046e-04;
        expected_jacobian[2][5] = 0.620824962635497e-03;
        expected_jacobian[3][5] = 0.114687184521618e-03;
        expected_jacobian[4][5] = -0.118659828822826e-03;
        expected_jacobian[5][5] = 0.444021505417651e-05;

        final double[][] actual_jacobian = new double[6][6];
        final double[][] jacobian_nonReg = {
            { -0.43843895453912946, 0.49150462698149405, -0.001869472054180768, -2980.6912325159246,
                -2972.742722562549, 516.6531469694269 },
            { 4.8803167928774536E-9, -4.224504163637966E-9, -8.369277872032105E-11, 2.3981204235797103E-4,
                2.0963844823564302E-4, -3.9091373992604554E-5 },
            { 7.194535891262601E-11, 6.722775842004332E-11, 8.018875575589237E-10, 5.5700421259429844E-5,
                5.204803368722222E-5, 6.208249626354966E-4 },
            { -6.296118627569002E-9, -5.6968946965872795E-9, 2.0615489619330886E-7, -1.1811275520455386E-4,
                1.8441725738670744E-4, 1.1468718452162013E-4 },
            { -1.826549511549973E-8, -1.7067790217553815E-8, -2.0358329553350038E-7, -1.0646160914576168E-5,
                -9.948070937212893E-6, -1.1865982882282603E-4 },
            { 2.8088910373399505E-8, 2.6861869560726286E-8, -4.772152878225415E-9, 6.430832678528825E-4,
                -7.411731536530562E-4, 4.440215054176513E-6 } };
        orbit.getJacobianWrtCartesian(PositionAngle.MEAN, actual_jacobian);

        assert2DArrayEqualsAbsolute(actual_jacobian, jacobian_nonReg, this.epsilonNonReg, expected_jacobian,
            1.e4 * this.epsilonNonReg);
        assert2DArrayEqualsRelative(actual_jacobian, jacobian_nonReg, this.epsilonNonReg, expected_jacobian,
            1.e2 * this.epsilonNonReg);

    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#CIRCULAR2CARTESIAN}
     * 
     * @testedMethod {@link CircularOrbit#getPVCoordinates()}
     * @testedMethod {@link PVCoordinates#getPosition()}
     * @testedMethod {@link PVCoordinates#getVelocity()}
     * 
     * @description convert circular adapted non-equatorial parameters to cartesian ones
     * 
     * @input double a = 7204649.0 : semi-major axis
     * @input double ex = -2.9000e-04 : first component of the circular eccentricity vector
     * @input double ey = 1.3400e-03 : second component of the circular eccentricity vector
     * @input double i = 1.7233 : inclination
     * @input double RAAN = 1.5745 : right ascension of the ascending node
     * @input double &alpha;<sub>M</sub> = 0.5726 : mean latitude argument (AoP + M)
     * @output Vector3D position
     * @output Vector3D velocity
     * 
     * @testPassCriteria deviations from reference below thresholds. The state vector is as expected with an epsilon of
     *                   1e-12 for distances comparison (due to the fact that we compare physical measurements and that
     *                   the references come from another software).
     * 
     * @comments MSLIB 90 provided outputs are hard coded in tests cases
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void Circular2Cartesian() {

        // Case #1...
        // . awaited output values
        final double px = 0.567904804506205e+06;
        final double py = 0.606470745508254e+07;
        final double pz = 0.384107915671549e+07;
        final double vx = 0.965854416952944e+03;
        final double vy = -0.402046110384721e+04;
        final double vz = 0.618721910712774e+04;

        // . input circular non-equatorial parameters
        final double a = 7204649.0;
        final double ex = -2.9000e-04;
        final double ey = 1.3400e-03;
        final double i = 1.7233;
        final double raan = 1.5745;
        final double alphaM = 0.5726;

        final CircularOrbit orbit = new CircularOrbit(a, ex, ey, i, raan, alphaM, PositionAngle.MEAN,
            FramesFactory.getEME2000(), this.date, this.mu);
        final PVCoordinates pv = orbit.getPVCoordinates();
        final Vector3D position = pv.getPosition();
        final Vector3D velocity = pv.getVelocity();

        // ... expected vs. actual values comparisons
        validate.assertEqualsWithRelativeTolerance(position.getX(), 567904.8045062054, this.epsilonNonReg, px,
            this.epsilonDistance, "Circular2Cartesian_px");
        validate.assertEqualsWithRelativeTolerance(position.getY(), 6064707.455082541, this.epsilonNonReg, py,
            this.epsilonDistance, "Circular2Cartesian_py");
        validate.assertEqualsWithRelativeTolerance(position.getZ(), 3841079.15671549, this.epsilonNonReg, pz,
            this.epsilonDistance, "Circular2Cartesian_pz");
        validate.assertEqualsWithRelativeTolerance(velocity.getX(), 965.8544169529438, this.epsilonNonReg, vx,
            this.epsilonDistance, "Circular2Cartesian_vx");
        validate.assertEqualsWithRelativeTolerance(velocity.getY(), -4020.4611038472067, this.epsilonNonReg, vy,
            this.epsilonDistance, "Circular2Cartesian_vy");
        validate.assertEqualsWithRelativeTolerance(velocity.getZ(), 6187.21910712774, this.epsilonNonReg, vz,
            this.epsilonDistance, "Circular2Cartesian_vz");
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#CIRCULAREQUATORIAL2CARTESIAN}
     * 
     * @testedMethod {@link CircularOrbit#getPVCoordinates()}
     * @testedMethod {@link PVCoordinates#getPosition()}
     * @testedMethod {@link PVCoordinates#getVelocity()}
     * 
     * @description convert circular adapted equatorial parameters to Cartesian ones
     * 
     * @input double a = 42166.712 : semi-major axis
     * @input double ex = -7.900e-06 : first component of the circular eccentricity vector
     * @input double ey = 1.100e-04 : second component of the circular eccentricity vector
     * @input double ix = 1.200e-04 : first component of the inclination vector
     * @input double iy = -1.16e-04 : second component of the inclination vector
     * @input double L<sub>M</sub> = 5.300 : mean longitude argument (AoP + RAAN + M)
     * @output Vector3D position
     * @output Vector3D velocity
     * 
     * @testPassCriteria deviations from reference below thresholds. The state vector is as expected with an epsilon of
     *                   1e-12 for distances comparison (due to the fact that we compare physical measurements and that
     *                   the references come from another software).
     * 
     * @comments MSLIB 90 provided outputs are hard coded in tests cases
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void CircularEquatorial2Cartesian() {

        // Case #1...
        // . awaited output values
        final double px = 0.233745668678733e+05;
        final double py = -0.350998914352669e+05;
        final double pz = -0.150053723123334e+01;
        final double vx = 0.809135038364960e+05;
        final double vy = 0.538902268252598e+05;
        final double vz = 0.158527938296630e+02;

        // . input circular non-equatorial parameters
        final double a = 42166.712;
        final double ex = -7.900e-06;
        final double ey = 1.100e-04;
        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double LM = 5.300;

        final double i = 2. * MathLib.asin(MathLib.sqrt(ix * ix + iy * iy) / 2.);
        final double twoSinIo2 = 2. * MathLib.sin(i / 2.);
        final double raan = MathLib.atan2((iy / twoSinIo2), (ix / twoSinIo2));
        final double e = MathLib.sqrt((ex * ex) + (ey * ey));
        final double aop_raan = MathLib.atan2((ey / e), (ex / e));
        final double aop = aop_raan - raan;
        final double alphaM = LM - raan;

        final CircularOrbit orbit = new CircularOrbit(a, e * MathLib.cos(aop), e * MathLib.sin(aop), i, raan, alphaM,
            PositionAngle.MEAN, FramesFactory.getEME2000(), this.date, this.mu);
        final PVCoordinates pv = orbit.getPVCoordinates();
        final Vector3D position = pv.getPosition();
        final Vector3D velocity = pv.getVelocity();

        // ... expected vs. actual values comparisons
        validate.assertEqualsWithRelativeTolerance(position.getX(), 23374.566867873353, this.epsilonNonReg, px,
            this.epsilonDistance, "CircularEquatorial2Cartesian_px");
        validate.assertEqualsWithRelativeTolerance(position.getY(), -35099.89143526694, this.epsilonNonReg, py,
            this.epsilonDistance, "CircularEquatorial2Cartesian_py");
        validate.assertEqualsWithRelativeTolerance(position.getZ(), -1.5005372312333352, this.epsilonNonReg, pz,
            this.epsilonDistance, "CircularEquatorial2Cartesian_pz");
        validate.assertEqualsWithRelativeTolerance(velocity.getX(), 80913.50383649596, this.epsilonNonReg, vx,
            this.epsilonDistance, "CircularEquatorial2Cartesian_vx");
        validate.assertEqualsWithRelativeTolerance(velocity.getY(), 53890.22682525979, this.epsilonNonReg, vy,
            this.epsilonDistance, "CircularEquatorial2Cartesian_vy");
        validate.assertEqualsWithRelativeTolerance(velocity.getZ(), 15.852793829662989, this.epsilonNonReg, vz,
            this.epsilonDistance, "CircularEquatorial2Cartesian_vz");
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#CIRCULAREQUATORIAL2KEPLERIAN}
     * 
     * @testedMethod {@link KeplerianOrbit#getA()}
     * @testedMethod {@link KeplerianOrbit#getE()}
     * @testedMethod {@link KeplerianOrbit#getI()}
     * @testedMethod {@link KeplerianOrbit#getPerigeeArgument()}
     * @testedMethod {@link KeplerianOrbit#getRightAscensionOfAscendingNode()}
     * @testedMethod {@link KeplerianOrbit#getMeanAnomaly()}
     * 
     * @description convert circular adapted equatorial parameters to keplerian ones
     * 
     * @input double a = 42166.712 : semi-major axis
     * @input double ex = -7.900e-06 : first component of the circular eccentricity vector
     * @input double ey = 1.100e-04 : second component of the circular eccentricity vector
     * @input double ix = 1.200e-04 : first component of the inclination vector
     * @input double iy = -1.16e-04 : second component of the inclination vector
     * @input double &alpha;<sub>M</sub> = 5.300 : mean latitude argument (AoP + M)
     * @output double a
     * @output double e
     * @output double i
     * @output double AoP
     * @output double RAAN
     * @output double M
     * 
     * @testPassCriteria deviations from reference below thresholds. For distances comparison, the epsilon is set to
     *                   1e-12; for angles comparison, the epsilon is set to 1e-7; for eccentricity comparison the
     *                   epsilon is set to 1e-7. These epsilons take into account the fact that the measures are
     *                   physical and that their references come from another software.
     * 
     * @comments MSLIB 90 provided outputs are hard coded in tests cases
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void CircularEquatorial2Keplerian() {

        // Case #1...
        // . awaited output values
        final double expected_a = 0.421667120000000e+05;
        final double expected_e = 0.110283316961361e-03;
        final double expected_i = 0.166901168553917e-03;
        final double expected_aop = MathUtils.normalizeAngle(-0.387224326008837e+01, FastMath.PI);
        final double expected_raan = 0.551473467358854e+01;
        final double expected_M = 0.365750858649982e+01;

        // . input circular non-equatorial parameters
        final double a = 42166.712;
        final double etx = -7.900e-06;
        final double ety = 1.100e-04;
        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double LM = 5.300;

        final double i = 2. * MathLib.asin(MathLib.sqrt(ix * ix + iy * iy) / 2.);
        final double twoSinIo2 = 2. * MathLib.sin(i / 2.);
        final double raan = MathLib.atan2((iy / twoSinIo2), (ix / twoSinIo2));
        final double e = MathLib.sqrt((etx * etx) + (ety * ety));
        final double aop_raan = MathLib.atan2((ety / e), (etx / e));
        final double aop = MathUtils.normalizeAngle((aop_raan - raan), FastMath.PI);
        final double alphaM = MathUtils.normalizeAngle((LM - raan), FastMath.PI);
        final double ex = e * MathLib.cos(aop);
        final double ey = e * MathLib.sin(aop);

        final CircularOrbit cOrbit = new CircularOrbit(a, ex, ey, i, raan, alphaM, PositionAngle.MEAN,
            FramesFactory.getEME2000(), this.date, this.mu);
        final KeplerianOrbit kOrbit =
            new KeplerianOrbit(cOrbit.getPVCoordinates(), FramesFactory.getEME2000(), this.date,
                this.mu);

        // ... expected vs. actual values comparisons
        validate.assertEqualsWithRelativeTolerance(kOrbit.getA(), 42166.712, this.epsilonNonReg, expected_a,
            this.epsilonDistance, "CircularEquatorial2Keplerian_A");
        validate.assertEqualsWithRelativeTolerance(kOrbit.getE(), 1.1028331696129433E-4, this.epsilonNonReg,
            expected_e,
            this.epsilonE, "CircularEquatorial2Keplerian_E");
        this.assertAngleEqualsRelative(kOrbit.getI(), 1.6690116855391711E-4, this.epsilonNonReg, expected_i,
            this.epsilonAngle,
            "CircularEquatorial2Keplerian_I");
        this.assertAngleEqualsRelative(kOrbit.getPerigeeArgument(), 2.4109420470904794, this.epsilonNonReg,
            expected_aop,
            this.epsilonAngle, "CircularEquatorial2Keplerian_w");
        this.assertAngleEqualsRelative(kOrbit.getRightAscensionOfAscendingNode(), -0.7684506335910435,
            this.epsilonAngle,
            expected_raan, this.epsilonAngle, "CircularEquatorial2Keplerian_W");
        this.assertAngleEqualsRelative(kOrbit.getMeanAnomaly(), -2.625676720679058, this.epsilonNonReg, expected_M,
            this.epsilonAngle,
            "CircularEquatorial2Keplerian_M");
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#CIRCULAR2KEPLERIAN}
     * 
     * @testedMethod {@link KeplerianOrbit#getA()}
     * @testedMethod {@link KeplerianOrbit#getE()}
     * @testedMethod {@link KeplerianOrbit#getI()}
     * @testedMethod {@link KeplerianOrbit#getPerigeeArgument()}
     * @testedMethod {@link KeplerianOrbit#getRightAscensionOfAscendingNode()}
     * @testedMethod {@link KeplerianOrbit#getMeanAnomaly()}
     * 
     * @description convert circular adapted non-equatorial parameters to keplerian ones
     * 
     * @input double a = 7204649.0 : semi-major axis
     * @input double ex = -2.9000e-04 : first component of the circular eccentricity vector
     * @input double ey = 1.3400e-03 : second component of the circular eccentricity vector
     * @input double i = 1.7233 : first component of the inclination vector
     * @input double raan = 1.5745 : second component of the inclination vector
     * @input double &alpha;<sub>M</sub> = 0.5726 : mean latitude argument (&omega; + M)
     * @output double a
     * @output double e
     * @output double i
     * @output double AoP
     * @output double RAAN
     * @output double M
     * 
     * @testPassCriteria deviations from reference below thresholds. For distances comparison, the epsilon is set to
     *                   1e-12; for angles comparison, the epsilon is set to 1e-7; for eccentricity comparison the
     *                   epsilon is set to 1e-7. These epsilons take into account the fact that the measures are
     *                   physical and that their references come from another software.
     * 
     * @comments MSLIB 90 provided outputs are hard coded in tests cases
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void Circular2Keplerian() {

        // Case #1...
        // . awaited output values
        final double expected_a = 0.720464900000000e+07;
        final double expected_e = 0.137102151697193e-02;
        final double expected_i = 0.172330000000000e+01;
        final double expected_aop = 0.178392735459927e+01;
        final double expected_raan = 0.157450000000000e+01;
        final double expected_M = MathUtils.normalizeAngle(-0.121132735459927e+01, FastMath.PI);

        // . input circular non-equatorial parameters
        final double a = 7204649.0;
        final double ex = -2.9000e-04;
        final double ey = 1.3400e-03;
        final double i = 1.7233;
        final double raan = 1.5745;
        final double alphaM = 0.5726;

        final CircularOrbit cOrbit = new CircularOrbit(a, ex, ey, i, raan, alphaM, PositionAngle.MEAN,
            FramesFactory.getEME2000(), this.date, this.mu);
        final KeplerianOrbit kOrbit = new KeplerianOrbit(cOrbit);

        // ... expected vs. actual values comparisons
        validate.assertEqualsWithRelativeTolerance(kOrbit.getA(), 7204649.000000004, this.epsilonNonReg, expected_a,
            this.epsilonDistance, "Circular2Keplerian_A");
        validate.assertEqualsWithRelativeTolerance(kOrbit.getE(), 0.001371021516971925, this.epsilonNonReg, expected_e,
            this.epsilonE, "Circular2Keplerian_E");
        this.assertAngleEqualsRelative(kOrbit.getI(), 1.7233, this.epsilonNonReg, expected_i, this.epsilonAngle,
            "Circular2Keplerian_I");
        this.assertAngleEqualsRelative(kOrbit.getPerigeeArgument(), 1.7839273545992698, this.epsilonNonReg,
            expected_aop,
            this.epsilonAngle, "Circular2Keplerian_w");
        this.assertAngleEqualsRelative(kOrbit.getRightAscensionOfAscendingNode(), 1.5745, this.epsilonNonReg,
            expected_raan,
            this.epsilonAngle, "Circular2Keplerian_W");
        this.assertAngleEqualsRelative(kOrbit.getMeanAnomaly(), -1.2113273545992698, this.epsilonNonReg, expected_M,
            this.epsilonAngle, "Circular2Keplerian_M");
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#KEPLERIAN2CARTESIAN}
     * 
     * @testedMethod {@link KeplerianOrbit#getPVCoordinates()}
     * @testedMethod {@link PVCoordinates#getPosition()}
     * @testedMethod {@link PVCoordinates#getVelocity()}
     * 
     * @description convert keplerian parameters to cartesian ones
     * 
     * @input double a = 24464560.0 : semi-major axis
     * @input double e = 0.7311 : eccentricity
     * @input double i = 0.122138 : inclination
     * @input double aop = 3.10686 : argument of perigee
     * @input double raan = 1.00681 : right ascension of the ascending node
     * @input double M = 0.048363 : mean anomaly
     * @output position vector
     * @output velocity vector
     * 
     * @testPassCriteria deviations from reference below thresholds. The state vector is as expected with an epsilon of
     *                   1e-12 for distances comparison (due to the fact that we compare physical measurements and that
     *                   the references come from another software).
     * 
     * @comments MSLIB 90 provided outputs are hard coded in tests cases
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void Keplerian2Cartesian() {

        // Case #1...
        // . awaited output values
        final double px1 = -0.107622532467967e+07;
        final double py1 = -0.676589636432773e+07;
        final double pz1 = -0.332308783350379e+06;
        final double vx1 = 0.935685775154103e+04;
        final double vy1 = -0.331234775037644e+04;
        final double vz1 = -0.118801577532701e+04;

        // . input keplerian parameters
        final double a1 = 24464560.0;
        final double e1 = 0.7311;
        final double i1 = 0.122138;
        final double aop1 = 3.10686;
        final double raan1 = 1.00681;
        final double M1 = 0.048363;

        final KeplerianOrbit orbit1 = new KeplerianOrbit(a1, e1, i1, aop1, raan1, M1, PositionAngle.MEAN,
            FramesFactory.getEME2000(), this.date, this.mu);
        final PVCoordinates pv1 = orbit1.getPVCoordinates();
        final Vector3D position1 = pv1.getPosition();
        final Vector3D velocity1 = pv1.getVelocity();

        // ... expected vs. actual values comparisons
        validate.assertEqualsWithRelativeTolerance(position1.getX(), -1076225.324679696, this.epsilonNonReg, px1,
            this.epsilonDistance, "Keplerian2Cartesian_px");
        validate.assertEqualsWithRelativeTolerance(position1.getY(), -6765896.364327722, this.epsilonNonReg, py1,
            this.epsilonDistance, "Keplerian2Cartesian_py");
        validate.assertEqualsWithRelativeTolerance(position1.getZ(), -332308.7833503755, this.epsilonNonReg, pz1,
            this.epsilonDistance, "Keplerian2Cartesian_pz");
        validate.assertEqualsWithRelativeTolerance(velocity1.getX(), 9356.85775154103, this.epsilonNonReg, vx1,
            this.epsilonDistance, "Keplerian2Cartesian_vx");
        validate.assertEqualsWithRelativeTolerance(velocity1.getY(), -3312.347750376466, this.epsilonNonReg, vy1,
            this.epsilonDistance, "Keplerian2Cartesian_vy");
        validate.assertEqualsWithRelativeTolerance(velocity1.getZ(), -1188.015775327014, this.epsilonNonReg, vz1,
            this.epsilonDistance, "Keplerian2Cartesian_vz");

        // Case #2...
        // . awaited output values
        final double px2 = -0.682967149018510e+01;
        final double py2 = 0.136421733267227e+02;
        final double pz2 = 0.488425070532545e+01;
        final double vx2 = -0.107346656953253e+08;
        final double vy2 = -0.547241596951838e+07;
        final double vz2 = 0.205806040807563e+07;

        // . input keplerian parameters
        final double a2 = -4.0;
        final double e2 = 5.0;
        final double i2 = 0.349065850399;
        final double aop2 = 0.104719755120e1;
        final double raan2 = 0.959931088597;
        final double M2 = 0.174532925199;

        final KeplerianOrbit orbit2 = new KeplerianOrbit(a2, e2, i2, aop2, raan2, M2, PositionAngle.MEAN,
            FramesFactory.getEME2000(), this.date, this.mu);
        final PVCoordinates pv2 = orbit2.getPVCoordinates();
        final Vector3D position2 = pv2.getPosition();
        final Vector3D velocity2 = pv2.getVelocity();

        // ... expected vs. actual values comparisons
        validate.assertEqualsWithRelativeTolerance(position2.getX(), -6.8296714901851, this.epsilonNonReg, px2,
            this.epsilonDistance, "Keplerian2Cartesian_px");
        validate.assertEqualsWithRelativeTolerance(position2.getY(), 13.642173326722668, this.epsilonNonReg, py2,
            this.epsilonDistance, "Keplerian2Cartesian_py");
        validate.assertEqualsWithRelativeTolerance(position2.getZ(), 4.88425070532545, this.epsilonNonReg, pz2,
            this.epsilonDistance, "Keplerian2Cartesian_pz");
        validate.assertEqualsWithRelativeTolerance(velocity2.getX(), -1.073466569532532E7, this.epsilonNonReg, vx2,
            this.epsilonDistance, "Keplerian2Cartesian_vx");
        validate.assertEqualsWithRelativeTolerance(velocity2.getY(), -5472415.96951838, this.epsilonNonReg, vy2,
            this.epsilonDistance, "Keplerian2Cartesian_vy");
        validate.assertEqualsWithRelativeTolerance(velocity2.getZ(), 2058060.4080756318, this.epsilonNonReg, vz2,
            this.epsilonDistance, "Keplerian2Cartesian_vz");
    }

    /**
     * @throws IllegalArgumentException
     * @testType TVT
     * 
     * @testedFeature {@link features#KEPLERIAN2CARTESIAN}
     * 
     * @testedMethod {@link KeplerianOrbit#getPVCoordinates()}
     * @testedMethod {@link PVCoordinates#getPosition()}
     * @testedMethod {@link PVCoordinates#getVelocity()}
     * 
     * @description convert keplerian parameters to Cartesian ones
     * 
     * @input double a = 4.0 : semi-major axis
     * @input double e = 1.0 : eccentricity
     * @input double i = 0.785398163397 : inclination
     * @input double aop = 0.785398163397e1 : argument of perigee
     * @input double raan = 0.000000000000 : right ascension of the ascending node
     * @input double M = 0.523598775598 : mean anomaly
     * @output position vector
     * @output velocity vector
     * 
     * @testPassCriteria exception (OrekitException). The state vector is as expected with an epsilon of 1e-12 for
     *                   distances comparison (due to the fact that we compare physical measurements and that the
     *                   references come from another software).
     * 
     * @comments MSLIB 90 provided outputs are hard coded in tests cases
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * @throws IllegalArgumentException
     *         if frame is not a pseudo-inertial frame or a and e don't match for hyperbolic orbits, or v is out of
     *         range for hyperbolic orbits
     */
    @Test(expected = IllegalArgumentException.class)
    public final void Keplerian2CartesianAnomalyOutOfRange() throws IllegalArgumentException {
        // Case #3...
        // . awaited output values
        final double px3 = -0.338252078902993e+01;
        final double py3 = 0.402922977226739e+00;
        final double pz3 = 0.402922977226377e+00;
        final double vx3 = -0.116407745835214e+08;
        final double vy3 = -0.696061102023180e+07;
        final double vz3 = -0.696061102022556e+07;

        // . input keplerian parameters
        final double a3 = 4.0;
        final double e3 = 1.0;
        final double i3 = 0.785398163397;
        final double aop3 = 0.785398163397e1;
        final double raan3 = 0.000000000000;
        final double M3 = 0.523598775598;

        final KeplerianOrbit orbit3 = new KeplerianOrbit(a3, e3, i3, aop3, raan3, M3, PositionAngle.MEAN,
            FramesFactory.getEME2000(), this.date, this.mu);

        final PVCoordinates pv3 = orbit3.getPVCoordinates();
        final Vector3D position3 = pv3.getPosition();
        final Vector3D velocity3 = pv3.getVelocity();

        // ... expected vs. actual values comparisons
        validate.assertEqualsWithRelativeTolerance(position3.getX(), px3, this.epsilonNonReg, px3,
            this.epsilonDistance,
            "Keplerian2CartesianAnomalyOutOfRange_px");
        validate.assertEqualsWithRelativeTolerance(position3.getY(), py3, this.epsilonNonReg, py3,
            this.epsilonDistance,
            "Keplerian2CartesianAnomalyOutOfRange_py");
        validate.assertEqualsWithRelativeTolerance(position3.getZ(), pz3, this.epsilonNonReg, pz3,
            this.epsilonDistance,
            "Keplerian2CartesianAnomalyOutOfRange_pz");
        validate.assertEqualsWithRelativeTolerance(velocity3.getX(), vx3, this.epsilonNonReg, vx3,
            this.epsilonDistance,
            "Keplerian2CartesianAnomalyOutOfRange_vx");
        validate.assertEqualsWithRelativeTolerance(velocity3.getY(), vy3, this.epsilonNonReg, vy3,
            this.epsilonDistance,
            "Keplerian2CartesianAnomalyOutOfRange_vy");
        validate.assertEqualsWithRelativeTolerance(velocity3.getZ(), vz3, this.epsilonNonReg, vz3,
            this.epsilonDistance,
            "Keplerian2CartesianAnomalyOutOfRange_vz");
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#KEPLERIAN2CIRCULAREQUATORIAL}
     * 
     * @testedMethod {@link CircularOrbit#getA()}
     * @testedMethod {@link CircularOrbit#getEquinoctialEx()}
     * @testedMethod {@link CircularOrbit#getEquinoctialEy()}
     * @testedMethod {@link CircularOrbit#getI()}
     * @testedMethod {@link CircularOrbit#getRightAscensionOfAscendingNode()}
     * @testedMethod {@link CircularOrbit#getLM()}
     * 
     * @description convert keplerian parameters to circular equatorial ones
     * 
     * @input double a = 24464560.0 : semi-major axis
     * @input double e = 0.7311 : eccentricity
     * @input double i = 0.122138 : inclination
     * @input double aop = 3.10686 : argument of perigee
     * @input double raan = 1.00681 : right ascension of the ascending node
     * @input double M = 0.048363 : mean anomaly
     * @output semi-major axis
     * @output first component of the eccentricity vector
     * @output second component of the eccentricity vector
     * @output first component of the inclination vector
     * @output second component of the inclination vector
     * @output mean longitude argument
     * 
     * @testPassCriteria deviations from reference below thresholds. For distances comparison, the epsilon is set to
     *                   1e-12; for angles comparison, the epsilon is set to 1e-7; for eccentricity comparison the
     *                   epsilon is set to 1e-7. These epsilons take into account the fact that the measures are
     *                   physical and that their references come from another software.
     * 
     * @comments MSLIB 90 provided outputs are hard coded in tests cases
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void Keplerian2CircularEquatorial() {

        // Case #1...
        // . awaited output values
        final double expected_a = 0.244645600000000e+08;
        final double expected_ex = -0.412036802887626e+00;
        final double expected_ey = -0.603931190671706e+00;
        final double expected_ix = 0.652494417368829e-01;
        final double expected_iy = 0.103158450084864e+00;
        final double expected_LM = 0.416203300000000e+01;

        // . input keplerian parameters
        final double a = 24464560.0;
        final double e = 0.7311;
        final double i = 0.122138;
        final double aop = 3.10686;
        final double raan = 1.00681;
        final double M = 0.048363;

        final KeplerianOrbit kOrbit = new KeplerianOrbit(a, e, i, aop, raan, M, PositionAngle.MEAN,
            FramesFactory.getEME2000(), this.date, this.mu);
        final CircularOrbit cOrbit = new CircularOrbit(kOrbit);

        // ... expected vs. actual values comparisons
        final double actual_ix = 2. * MathLib.sin(cOrbit.getI() / 2.)
            * MathLib.cos(cOrbit.getRightAscensionOfAscendingNode());
        final double actual_iy = 2. * MathLib.sin(cOrbit.getI() / 2.)
            * MathLib.sin(cOrbit.getRightAscensionOfAscendingNode());

        validate.assertEqualsWithRelativeTolerance(cOrbit.getA(), 2.446456E7, this.epsilonNonReg, expected_a,
            this.epsilonDistance, "Keplerian2CircularEquatorial_A");
        validate.assertEqualsWithRelativeTolerance(cOrbit.getEquinoctialEx(), -0.4120368028876257, this.epsilonE,
            expected_ex, this.epsilonDistance, "Keplerian2CircularEquatorial_ex");
        validate.assertEqualsWithRelativeTolerance(cOrbit.getEquinoctialEy(), -0.6039311906717054, this.epsilonE,
            expected_ey, this.epsilonDistance, "Keplerian2CircularEquatorial_ey");
        validate.assertEqualsWithRelativeTolerance(actual_ix, 0.06524944173688287, this.epsilonNonReg, expected_ix,
            this.epsilonAngle, "Keplerian2CircularEquatorial_ix");
        validate.assertEqualsWithRelativeTolerance(actual_iy, 0.10315845008486389, this.epsilonNonReg, expected_iy,
            this.epsilonAngle, "Keplerian2CircularEquatorial_iy");
        this.assertAngleEqualsRelative(cOrbit.getLM(), 4.162032999999999, this.epsilonNonReg, expected_LM,
            this.epsilonAngle,
            "Keplerian2CircularEquatorial_LM");
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#KEPLERIAN2CIRCULAR}
     * 
     * @testedMethod {@link CircularOrbit#getA()}
     * @testedMethod {@link CircularOrbit#getCircularEx()}
     * @testedMethod {@link CircularOrbit#getCircularEy()}
     * @testedMethod {@link CircularOrbit#getI()}
     * @testedMethod {@link CircularOrbit#getRightAscensionOfAscendingNode()}
     * @testedMethod {@link CircularOrbit#getAlphaM()}
     * 
     * @description convert keplerian parameters to circular non-equatorial ones
     * 
     * @input double a = 24464560.0 : semi-major axis
     * @input double e = 0.7311 : eccentricity
     * @input double i = 0.122138 : inclination
     * @input double aop = 3.10686 : argument of perigee
     * @input double raan = 1.00681 : right ascension of the ascending node
     * @input double M = 0.048363 : mean anomaly
     * @output semi-major axis
     * @output first component of the eccentricity vector
     * @output second component of the eccentricity vector
     * @output inclination
     * @output argument of perigee
     * @output mean latitude argument
     * 
     * @testPassCriteria deviations from reference below thresholds. For distances comparison, the epsilon is set to
     *                   1e-12; for angles comparison, the epsilon is set to 1e-7; for eccentricity comparison the
     *                   epsilon is set to 1e-7. These epsilons take into account the fact that the measures are
     *                   physical and that their references come from another software.
     * 
     * @comments MSLIB 90 provided outputs are hard coded in tests cases
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void Keplerian2Circular() {

        // Case #1...
        // . awaited output values
        final double expected_a = 0.244645600000000e+08;
        final double expected_ex = -0.730659060446484e+00;
        final double expected_ey = 0.253879378339516e-01;
        final double expected_i = 0.122138000000000e-00;
        final double expected_raan = 0.100681000000000e+01;
        final double expected_alphaM = 0.315522300000000e+01;

        // . input keplerian parameters
        final double a = 24464560.0;
        final double e = 0.7311;
        final double i = 0.122138;
        final double aop = 3.10686;
        final double raan = 1.00681;
        final double M = 0.048363;

        final KeplerianOrbit kOrbit = new KeplerianOrbit(a, e, i, aop, raan, M, PositionAngle.MEAN,
            FramesFactory.getEME2000(), this.date, this.mu);
        final CircularOrbit cOrbit = new CircularOrbit(kOrbit);

        // ... expected vs. actual values comparisons
        validate.assertEqualsWithRelativeTolerance(cOrbit.getA(), 2.446456E7, this.epsilonNonReg, expected_a,
            this.epsilonDistance, "Keplerian2Circular_A");
        validate.assertEqualsWithRelativeTolerance(cOrbit.getCircularEx(), -0.7306590604464844, this.epsilonNonReg,
            expected_ex, this.epsilonE, "Keplerian2Circular_ex");
        validate.assertEqualsWithRelativeTolerance(cOrbit.getCircularEy(), 0.025387937833951824, this.epsilonNonReg,
            expected_ey, this.epsilonE, "Keplerian2Circular_ey");
        this.assertAngleEqualsRelative(cOrbit.getI(), 0.122138, this.epsilonNonReg, expected_i, this.epsilonAngle,
            "Keplerian2Circular_I");
        this.assertAngleEqualsRelative(cOrbit.getRightAscensionOfAscendingNode(), 1.00681, this.epsilonNonReg,
            expected_raan,
            this.epsilonAngle, "Keplerian2Circular_raan");
        this.assertAngleEqualsRelative(cOrbit.getAlphaM(), 3.1552229999999994, this.epsilonNonReg, expected_alphaM,
            this.epsilonAngle,
            "Keplerian2Circular_alphaM");
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#KEPLERIAN2EQUINOCTIAL}
     * 
     * @testedMethod {@link EquinoctialOrbit#getA()}
     * @testedMethod {@link EquinoctialOrbit#getEquinoctialEx()}
     * @testedMethod {@link EquinoctialOrbit#getEquinoctialEy()}
     * @testedMethod {@link EquinoctialOrbit#getHx()}
     * @testedMethod {@link EquinoctialOrbit#getHy()}
     * @testedMethod {@link EquinoctialOrbit#getLM()}
     * 
     * @description convert keplerian parameters to equinoctial ones
     * 
     * @input double a = 24464560.0 : semi-major axis
     * @input double e = 0.7311 : eccentricity
     * @input double i = 0.122138 : inclination
     * @input double aop = 3.10686 : argument of perigee
     * @input double raan = 1.00681 : right ascension of the ascending node
     * @input double M = 0.048363 : mean anomaly
     * @output semi-major axis
     * @output first component of the eccentricity vector
     * @output second component of the eccentricity vector
     * @output first component of the inclination vector
     * @output first component of the inclination vector
     * @output mean longitude argument
     * 
     * @testPassCriteria deviations from reference below thresholds. For distances comparison, the epsilon is set to
     *                   1e-12; for angles comparison, the epsilon is set to 1e-7; for eccentricity comparison the
     *                   epsilon is set to 1e-7. These epsilons take into account the fact that the measures are
     *                   physical and that their references come from another software.
     * 
     * @comments scilab provided outputs are hard coded in tests cases
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void Keplerian2Equinoctial() {

        // Case #1...
        // . awaited output values
        final double expected_a = 2.446456000000000000e+07;
        final double expected_ex = -4.120368028876256750e-01;
        final double expected_ey = -6.039311906717055312e-01;
        final double expected_hx = 3.268565123448801540e-02;
        final double expected_hy = 5.167555509456963486e-02;
        final double expected_lm = 4.162033000000000094e+00;

        // . input keplerian parameters
        final double a = 24464560.0;
        final double e = 0.7311;
        final double i = 0.122138;
        final double aop = 3.10686;
        final double raan = 1.00681;
        final double M = 0.048363;

        final KeplerianOrbit kOrbit = new KeplerianOrbit(a, e, i, aop, raan, M, PositionAngle.MEAN,
            FramesFactory.getEME2000(), this.date, this.mu);
        final EquinoctialOrbit eOrbit = new EquinoctialOrbit(kOrbit);
        validate.assertEqualsWithRelativeTolerance(eOrbit.getA(), 2.446456E7, this.epsilonNonReg, expected_a,
            this.epsilonDistance, "Keplerian2Equinoctial()_A");
        validate.assertEqualsWithRelativeTolerance(eOrbit.getEquinoctialEx(), -0.4120368028876257, this.epsilonNonReg,
            expected_ex, this.epsilonE, "Keplerian2Equinoctial()_ex");
        validate.assertEqualsWithRelativeTolerance(eOrbit.getEquinoctialEy(), -0.6039311906717055, this.epsilonNonReg,
            expected_ey, this.epsilonE, "Keplerian2Equinoctial()_ey");
        validate.assertEqualsWithRelativeTolerance(eOrbit.getHx(), 0.032685651234488015, this.epsilonNonReg,
            expected_hx,
            this.epsilonAngle, "Keplerian2Equinoctial()_hx");
        validate.assertEqualsWithRelativeTolerance(eOrbit.getHy(), 0.051675555094569635, this.epsilonNonReg,
            expected_hy,
            this.epsilonAngle, "Keplerian2Equinoctial()_hy");
        this.assertAngleEqualsRelative(eOrbit.getLM(), 4.162032999999999, this.epsilonNonReg, expected_lm,
            this.epsilonAngle,
            "Keplerian2Equinoctial()_LM");
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#EQUINOCTIAL2KEPLERIAN}
     * 
     * @testedMethod {@link KeplerianOrbit#getA()}
     * @testedMethod {@link KeplerianOrbit#getE()}
     * @testedMethod {@link KeplerianOrbit#getI()}
     * @testedMethod {@link KeplerianOrbit#getPerigeeArgument()}
     * @testedMethod {@link KeplerianOrbit#getRightAscensionOfAscendingNode()}
     * @testedMethod {@link KeplerianOrbit#getMeanAnomaly()}
     * 
     * @description convert keplerian parameters to equinoctial ones
     * 
     * @input double a = : semi-major axis
     * @input double ex = : first componant of the eccentricity vector
     * @input double ey = : second componant of the eccentricity vector
     * @input double hx = : first component of the inclination vector
     * @input double hy = : second component of the inclination vector
     * @input double Lv = : true longitude argument
     * @output semi-major axis
     * @output eccentricity
     * @output inclination
     * @output argument of perigee
     * @output right ascension of the ascending node
     * @output mean anomaly
     * 
     * @testPassCriteria deviations from reference below thresholds. For distances comparison, the epsilon is set to
     *                   1e-12; for angles comparison, the epsilon is set to 1e-7; for eccentricity comparison the
     *                   epsilon is set to 1e-7. These epsilons take into account the fact that the measures are
     *                   physical and that their references come from another software.
     * 
     * @comments scilab provided outputs are hard coded in tests cases. Some test case use false assertion since Orekit
     *           has problem to deal with singularities where eccentricity and inclination are perfectly equals to zero.
     *           This can be annoying especially for people doing mission analysis. A workaround to prevent this from
     *           happening is to change inclination value to non-zero value. Slightly change in inclination value is
     *           sufficient.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     */
    @Test
    public final void Equinoctial2Keplerian() throws PatriusException {

        final Frame veis1950 = FramesFactory.getVeis1950();

        // Case #1...
        // . awaited output values
        final double expected_a1 = 4.216671199999999953e+04;
        final double expected_e1 = 1.102833169613609402e-04;
        final double expected_i1 = 3.338023336209351450e-04;
        final double expected_aop1 = MathUtils.normalizeAngle(-3.872243260088367833e+00, FastMath.PI);
        final double expected_raan1 = 5.514734673588542968e+00;
        final double expected_M1 = 3.657508586499825576e+00;

        // . input equinoctial parameters
        final double a1 = 4.216671200000000000e+04;
        final double ex1 = -7.900000000000000000e-06;
        final double ey1 = 1.100000000000000000e-04;
        final double hx1 = 1.200000000000000000e-04;
        final double hy1 = -1.160000000000000000e-04;
        final double lm1 = 5.300000000000000000e+00;

        final EquinoctialOrbit eOrbit1 = new EquinoctialOrbit(a1, ex1, ey1, hx1, hy1, lm1, PositionAngle.MEAN,
            veis1950, this.date, this.mu);
        final KeplerianOrbit kOrbit1 = new KeplerianOrbit(eOrbit1);

        validate.assertEqualsWithRelativeTolerance(kOrbit1.getA(), 42166.711999999956, this.epsilonNonReg, expected_a1,
            this.epsilonDistance, "Equinoctial2Keplerian_A");
        validate.assertEqualsWithRelativeTolerance(kOrbit1.getE(), 1.1028331696136094E-4, this.epsilonNonReg,
            expected_e1,
            this.epsilonE, "Equinoctial2Keplerian_E");
        this.assertAngleEqualsRelative(kOrbit1.getI(), 3.3380233362093515E-4, this.epsilonNonReg, expected_i1,
            this.epsilonAngle,
            "Equinoctial2Keplerian_I");
        this.assertAngleEqualsRelative(kOrbit1.getPerigeeArgument(), 2.4109420470912184, this.epsilonNonReg,
            expected_aop1,
            this.epsilonAngle, "Equinoctial2Keplerian_w");
        this.assertAngleEqualsRelative(kOrbit1.getRightAscensionOfAscendingNode(), -0.7684506335910435,
            this.epsilonNonReg,
            expected_raan1, this.epsilonAngle, "Equinoctial2Keplerian_W");
        this.assertAngleEqualsRelative(kOrbit1.getMeanAnomaly(), 3.6575085864998247, this.epsilonNonReg, expected_M1,
            this.epsilonAngle, "Equinoctial2Keplerian_M");

        // Case #2...
        // . awaited output values
        final double expected_a2 = 4.216671199999999953e+04;
        final double expected_e2 = 1.102833169613609402e-04;
        final double expected_i2 = 0.000000000000000000e+00;
        final double expected_aop2 = 1.642491413500175135e+00;
        final double expected_raan2 = 0.000000000000000000e+00;
        final double expected_M2 = 3.657508586499824688e+00;

        // . input equinoctial parameters
        final double a2 = 4.216671200000000000e+04;
        final double ex2 = -7.900000000000000000e-06;
        final double ey2 = 1.100000000000000000e-04;
        final double hx2 = 0.000000000000000000e+00;
        final double hy2 = 0.000000000000000000e+00;
        final double lm2 = 5.300000000000000000e+00;

        final EquinoctialOrbit eOrbit2 = new EquinoctialOrbit(a2, ex2, ey2, hx2, hy2, lm2, PositionAngle.TRUE,
            veis1950, this.date, this.mu);
        final KeplerianOrbit kOrbit2 = new KeplerianOrbit(eOrbit2);

        validate.assertEqualsWithRelativeTolerance(kOrbit2.getA(), 42166.711999999985, this.epsilonNonReg, expected_a2,
            this.epsilonDistance, "Equinoctial2Keplerian_A");
        validate.assertEqualsWithRelativeTolerance(kOrbit2.getE(), 1.1028331696136094E-4, this.epsilonNonReg,
            expected_e2,
            this.epsilonE, "Equinoctial2Keplerian_E");
        this.assertAngleEqualsRelative(kOrbit2.getI(), 0.0, this.epsilonNonReg, expected_i2, this.epsilonAngle,
            "Equinoctial2Keplerian_I");

        boolean testFail = false;
        try {
            this.assertAngleEqualsRelative(kOrbit2.getPerigeeArgument(), 2.625676720680361, this.epsilonNonReg,
                expected_aop2,
                this.epsilonAngle, "Equinoctial2Keplerian_w");
            testFail = true;
        } catch (final AssertionError e) {
        }
        Assert.assertFalse(testFail);
        this.assertAngleEqualsRelative(kOrbit2.getRightAscensionOfAscendingNode(), -0.0, this.epsilonNonReg,
            expected_raan2,
            this.epsilonAngle, "Equinoctial2Keplerian_W");
        this.assertAngleEqualsRelative(kOrbit2.getTrueAnomaly(), 3.6575085864998247, this.epsilonNonReg, expected_M2,
            this.epsilonAngle, "Equinoctial2Keplerian_M");

        // Case #3...
        // . awaited output values
        final double expected_a3 = 4.216671199999999953e+04;
        final double expected_e3 = 0.000000000000000000e+00;
        final double expected_i3 = 3.338023336209351450e-04;
        final double expected_aop3 = -5.514734673588542968e+00;
        final double expected_raan3 = 5.514734673588542968e+00;
        final double expected_M3 = 5.299999999999999822e+00;

        // . input equinoctial parameters
        final double a3 = 4.216671200000000000e+04;
        final double ex3 = 0.000000000000000000e+00;
        final double ey3 = 0.000000000000000000e+00;
        final double hx3 = 1.200000000000000000e-04;
        final double hy3 = -1.160000000000000000e-04;
        final double lm3 = 5.300000000000000000e+00;

        final EquinoctialOrbit eOrbit3 = new EquinoctialOrbit(a3, ex3, ey3, hx3, hy3, lm3, PositionAngle.MEAN,
            veis1950, this.date, this.mu);
        final KeplerianOrbit kOrbit3 = new KeplerianOrbit(eOrbit3);

        validate.assertEqualsWithRelativeTolerance(kOrbit3.getA(), 42166.711999999956, this.epsilonNonReg, expected_a3,
            this.epsilonDistance, "Equinoctial2Keplerian_A");
        validate.assertEquals(kOrbit3.getE(), 7.771952368597887E-16, this.epsilonNonReg, expected_e3, this.epsilonE,
            "Equinoctial2Keplerian_E");
        this.assertAngleEqualsRelative(kOrbit3.getI(), 3.3380233362093515E-4, this.epsilonNonReg, expected_i3,
            this.epsilonAngle,
            "Equinoctial2Keplerian_I");

        testFail = false;
        try {
            this.assertAngleEqualsRelative(kOrbit3.getPerigeeArgument(), -3.346293905146258, this.epsilonNonReg,
                expected_aop3,
                this.epsilonAngle, "Equinoctial2Keplerian_w");
        } catch (final AssertionError e) {
            // handle exception
        }

        this.assertAngleEqualsRelative(kOrbit3.getRightAscensionOfAscendingNode(), -0.7684506335910434,
            this.epsilonNonReg,
            expected_raan3, this.epsilonAngle, "Equinoctial2Keplerian_W");

        testFail = false;
        try {
            this.assertAngleEqualsRelative(kOrbit3.getMeanAnomaly(), 3.1315592315577145, this.epsilonNonReg,
                expected_M3,
                this.epsilonAngle, "Equinoctial2Keplerian_M");
        } catch (final AssertionError e) {
            // handle exception
        }
        Assert.assertFalse(testFail);

        // Case #4...
        // . awaited output values
        final double expected_a4 = 4.216671199999999953e+04;
        final double expected_e4 = 0.000000000000000000e+00;
        final double expected_i4 = 0.000000000000000000e+00;
        final double expected_aop4 = 0.000000000000000000e+00;
        final double expected_raan4 = 0.000000000000000000e+00;
        final double expected_M4 = 5.299999999999999822e+00;

        // . input equinoctial parameters
        final double a4 = 4.216671200000000000e+04;
        final double ex4 = 0.000000000000000000e+00;
        final double ey4 = 0.000000000000000000e+00;
        final double hx4 = 0.000000000000000000e+00;
        final double hy4 = 0.000000000000000000e+00;
        final double lm4 = 5.300000000000000000e+00;

        final EquinoctialOrbit eOrbit4 = new EquinoctialOrbit(a4, ex4, ey4, hx4, hy4, lm4, PositionAngle.MEAN,
            veis1950, this.date, this.mu);
        final KeplerianOrbit kOrbit4 = new KeplerianOrbit(eOrbit4);
        validate.assertEqualsWithRelativeTolerance(kOrbit4.getA(), 42166.712, this.epsilonNonReg, expected_a4,
            this.epsilonDistance, "Equinoctial2Keplerian_A");
        validate.assertEquals(kOrbit4.getE(), 5.81549013445668E-17, this.epsilonNonReg, expected_e4, this.epsilonE,
            "Equinoctial2Keplerian_E");
        this.assertAngleEqualsRelative(kOrbit4.getI(), 0.0, this.epsilonNonReg, expected_i4, this.epsilonAngle,
            "Equinoctial2Keplerian_I");

        testFail = false;
        try {
            this.assertAngleEqualsRelative(kOrbit4.getPerigeeArgument(), -1.5707963267948966, this.epsilonNonReg,
                expected_aop4,
                this.epsilonAngle, "Equinoctial2Keplerian_w");
        } catch (final AssertionError e) {
            // handle exception
        }
        Assert.assertFalse(testFail);

        testFail = false;
        try {
            this.assertAngleEqualsRelative(kOrbit4.getRightAscensionOfAscendingNode(), 0.0, this.epsilonNonReg,
                expected_raan4,
                this.epsilonAngle, "Equinoctial2Keplerian_W");
        } catch (final AssertionError e) {
            // handle exception
        }
        Assert.assertFalse(testFail);

        testFail = false;
        try {
            this.assertAngleEqualsRelative(kOrbit4.getMeanAnomaly(), 1.5707963267948966, this.epsilonNonReg,
                expected_M4,
                this.epsilonAngle, "Equinoctial2Keplerian_M");
        } catch (final AssertionError e) {
            // handle exception
        }
        Assert.assertFalse(testFail);
    }

    /**
     * @testType PT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link CartesianOrbit#getHx()}
     * @testedMethod {@link CartesianOrbit#getHy()}
     * 
     * @description test of component of the inclination vector with equatorial retrograde orbit
     * 
     * @input equatorial retrograde orbit (7204649.0, -2.9000e-04, 1.3400e-05, FastMath.PI, FastMath.PI/2, 0)
     * 
     * @output hx : first component of the inclination vector
     * @output hy : second component of the inclination vector
     * 
     * @testPassCriteria hx = NaN and hy = NaN
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     */
    @Test
    public final void testEquatorialRetrogadeOrbit() {

        final CircularOrbit pc = new CircularOrbit(7204649.0, -2.9000e-04, 1.3400e-05, FastMath.PI, FastMath.PI / 2, 0,
            PositionAngle.TRUE, FramesFactory.getEME2000(), this.date, this.mu);
        final CartesianOrbit p =
            new CartesianOrbit(pc.getPVCoordinates(), FramesFactory.getEME2000(), this.date, this.mu);

        Assert.assertEquals(p.getHx(), Double.NaN, 0);
        Assert.assertEquals(p.getHy(), Double.NaN, 0);
    }

    /**
     * @testType PT
     * 
     * @testedFeature {@link features#COVERAGE}
     * 
     * @testedMethod {@link KeplerianOrbit#getJacobianWrtParameters(PositionAngle, double[][])}
     * @testedMethod {@link KeplerianOrbit#getJacobianWrtCartesian(PositionAngle, double[][])}
     * 
     * @description test of jacobian parameters
     * 
     * @input Cartesian Orbit (-29536113.0, 30329259.0, -100125.0, -2194.0, -2141.0, -8.0)
     * 
     * @output jacobian
     * 
     * @testPassCriteria the jacobian of the orbital parameters is the inverse of the jacobian of the cartesian
     *                   parameters
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     */
    @Test
    public final void testgetJacobianWrtParameters() throws PatriusException {

        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        final AbsoluteDate dateP = new AbsoluteDate(2000, 04, 01, 0, 0, 0.000, TimeScalesFactory.getUTC());
        final KeplerianOrbit orbKep = new KeplerianOrbit(pvCoordinates, FramesFactory.getEME2000(), dateP, this.mu);

        for (final PositionAngle type : PositionAngle.values()) {

            final double[][] jacobianP = new double[6][6];
            orbKep.getJacobianWrtParameters(type, jacobianP);
            final double[][] jacobianC = new double[6][6];
            orbKep.getJacobianWrtCartesian(type, jacobianC);
            final RealMatrix matrix = MatrixUtils.createRealMatrix(jacobianC);
            final DecompositionSolver solver = new QRDecomposition(matrix).getSolver();
            final double[][] jacobianCInv = solver.getInverse().getData(false);

            Assert.assertEquals(jacobianP.length, jacobianCInv.length, 0);

            for (int i = 0; i < jacobianP.length; i++) {
                final double[] rowP = jacobianP[i];
                final double[] rowC = jacobianCInv[i];

                for (int j = 0; j < rowC.length; j++) {
                    Assert.assertEquals(rowP[j], rowC[j], this.epsilonComparison);
                }
            }
        }
    }

    private static void assert2DArrayEqualsAbsolute(final double[][] actual, final double[][] nonReg,
                                                    final double epsNonReg, final double[][] expected,
                                                    final double delta) {
        assert (expected.length > 0);
        assert (expected.length == actual.length);
        assert (expected[0].length == actual[0].length);
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                validate.assertEquals(actual[i][j], nonReg[i][j], epsNonReg, expected[i][j], delta,
                    "jacobian matrix component");
                // Assert.assertEquals(expected[i][j], actual[i][j], delta);
            }
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * @testedFeature {@link features#COVERAGE}
     * @testedMethod {@link Orbit}
     * @description This test aims at showing that one can create an orbit using a non inertial frame
     * @input Frame used to create the orbit : TIRF
     * @output KeplerianOrbit(8.0E6, 0.0, 0.0, 0.0, 0.0, 0.0)
     * @testPassCriteria The orbit is created without trouble, althought the frame is non inertial
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void testConstructorOrbit() throws IllegalArgumentException, PatriusException {

        // Instanciation of a Keplerian orbit with a non pseudo-inertial frame
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Frame nonInertialFrame = FramesFactory.getTIRF();
        final KeplerianOrbit orbit = new KeplerianOrbit(8000E3, 0, 0, 0, 0, 0, PositionAngle.TRUE, nonInertialFrame,
            initDate, Constants.EGM96_EARTH_MU);

        Assert.assertTrue(!nonInertialFrame.isPseudoInertial());
        Assert.assertEquals(8000E3, orbit.getA(), 0.0);
        Assert.assertEquals(0.0, orbit.getE(), 0.0);
        Assert.assertEquals(0.0, orbit.getPerigeeArgument(), 0.0);
        Assert.assertEquals(0.0, orbit.getRightAscensionOfAscendingNode(), 0.0);
        Assert.assertEquals(0.0, orbit.getPerigeeArgument(), 0.0);
        Assert.assertEquals(0.0, orbit.getTrueAnomaly(), 0.0);
        Assert.assertEquals(0.0, orbit.getPerigeeArgument(), 0.0);
        Assert.assertEquals(Constants.EGM96_EARTH_MU, orbit.getMu(), 0.0);

        // Test getNativeFrame
        Assert.assertEquals(nonInertialFrame, orbit.getNativeFrame(null, null));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * @testedFeature {@link features#COVERAGE}
     * @testedMethod {@link Orbit}
     * @description Test orbit propagation (methods addKeplerContribution et shiftedBy) with non-inertial frames
     * @input Frame used to create the orbit : TIRF
     * @output Exception
     * @testPassCriteria An exception is thrown
     * @referenceVersion 3.1
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void testNonInertialFrame() throws IllegalArgumentException, PatriusException {

        // Instanciation of a Keplerian orbit with a non pseudo-inertial frame
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Frame nonInertialFrame = FramesFactory.getTIRF();
        final KeplerianOrbit orbit = new KeplerianOrbit(8000E3, 0, 0, 0, 0, 0, PositionAngle.TRUE, nonInertialFrame,
            initDate, Constants.EGM96_EARTH_MU);

        try {
            orbit.shiftedBy(10);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            orbit.addKeplerContribution(PositionAngle.TRUE, 1, new double[6]);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    private static void assert2DArrayEqualsRelative(final double[][] actual, final double[][] nonReg,
                                                    final double epsNonReg, final double[][] expected,
                                                    final double delta) {
        assert (expected.length > 0);
        assert (expected.length == actual.length);
        assert (expected[0].length == actual[0].length);
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                // final boolean result = Precision.equalsWithRelativeTolerance(expected[i][j], actual[i][j], delta);
                // if (!result) {
                // Assert.assertTrue(false);
                // return;
                // }
                validate.assertEqualsWithRelativeTolerance(actual[i][j], nonReg[i][j], epsNonReg, expected[i][j],
                    delta, "jacobian matrix component");

            }
        }
        Assert.assertTrue(true);
    }

    private void assertAngleEqualsRelative(final double actual, final double nonRegExpected, final double nonRegEps,
                                           final double externalRefExpected, final double externalRefEps,
                                           final String deviationDesciption) {
        final double actual_angle = MathUtils.normalizeAngle(actual, FastMath.PI);
        final double nonRegExpected_angle = MathUtils.normalizeAngle(nonRegExpected, FastMath.PI);
        final double externalRefExpected_angle = MathUtils.normalizeAngle(externalRefExpected, FastMath.PI);
        validate.assertEqualsWithRelativeTolerance(actual_angle, nonRegExpected_angle, nonRegEps,
            externalRefExpected_angle, externalRefEps, deviationDesciption);
    }

    /**
     * @testType UT
     * 
     * @testedFeature none
     * 
     * @testedMethod {@link Orbit#getJacobian(OrbitType, OrbitType)}
     * @testedMethod {@link Orbit#getJacobian(OrbitType, OrbitType, PositionAngle)}
     * @testedMethod {@link AlternateEquinoctialOrbit#getKeplerianTransitionMatrix(double)}
     * @testedMethod {@link ApsisOrbit#getKeplerianTransitionMatrix(double)}
     * @testedMethod {@link CircularOrbit#getKeplerianTransitionMatrix(double)}
     * @testedMethod {@link EquatorialOrbit#getKeplerianTransitionMatrix(double)}
     * @testedMethod {@link EquinoctialOrbit#getKeplerianTransitionMatrix(double)}
     * @testedMethod {@link KeplerianOrbit#getKeplerianTransitionMatrix(double)}
     * @testedMethod {@link CartesianOrbit#getKeplerianTransitionMatrix(double)}
     * 
     * @description Test diffents methods for {@link Orbit} class and childrens.
     * 
     * @input parameters
     * 
     * @output orbit
     * 
     * @testPassCriteria The position angle is well taken into account in the new constructors
     *                   resulting in a proper jacobian calculation and the transition matrix is
     *                   well calculated for each orbit model as well.
     * 
     * @referenceVersion 4.3
     * 
     * @nonRegressionVersion 4.3
     */
    @Test
    public final void testFT2082() throws IllegalArgumentException, PatriusException {

        // Test FT Part 2
        /*
         * Cover test for the new Orbit::getJacobian(final OrbitType numerator, final OrbitType
         * denominator, PositionAngle positionAngle) method :
         */
        final double a = 7187990.1979844316;
        final double e = 0.5e-4;
        final double i = 1.7105407051081795;
        final double omega = 1.9674147913622104;
        final double OMEGA = MathLib.toRadians(261);
        final double lv = 0;
        final Frame frame = FramesFactory.getEME2000();
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
            TimeComponents.H00, TimeScalesFactory.getUTC());
        final double mu = 3.9860047e14;

        final Orbit orbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
            frame, date, mu);

        final OrbitType cart = OrbitType.CARTESIAN;
        final OrbitType kep = OrbitType.KEPLERIAN;
        final PositionAngle posEcc = PositionAngle.ECCENTRIC;
        final PositionAngle posMean = PositionAngle.MEAN;

        final RealMatrix jacob1 = orbit.getJacobian(cart, kep);
        final RealMatrix jacob2 = orbit.getJacobian(cart, kep, posMean);
        final RealMatrix jacob3 = orbit.getJacobian(cart, kep, posEcc);

        // Comparison between jacob1 & jacob2 (result1) should be equal and comparison between
        // jacob1 & jacob3 (result2) should be different.

        final boolean result1 = jacob1.equals(jacob2);
        final boolean result2 = jacob1.equals(jacob3);

        Assert.assertTrue(result1);
        Assert.assertFalse(result2);

        // Test FT Part 3

        final Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        final Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);

        final PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);

        final double ix = 1.200e-04;
        final double iy = -1.16e-04;
        final double inc = 2 * MathLib.asin(MathLib.sqrt((ix * ix + iy * iy) / 4.));
        final double hx = MathLib.tan(inc / 2.) * ix / (2 * MathLib.sin(inc / 2.));
        final double hy = MathLib.tan(inc / 2.) * iy / (2 * MathLib.sin(inc / 2.));

        final double n = MathLib.sqrt(mu / a) / a;

        final double ex = -2.9000e-04;
        final double ey = 1.3400e-03;
        final double raan = 1.5745;
        final double alphaM = 0.5726;

        final Frame veis1950 = FramesFactory.getVeis1950();
        final double a1 = 4.216671200000000000e+04;
        final double ex1 = -7.900000000000000000e-06;
        final double ey1 = 1.100000000000000000e-04;
        final double hx1 = 1.200000000000000000e-04;
        final double hy1 = -1.160000000000000000e-04;
        final double lm1 = 5.300000000000000000e+00;

        final double per = 24464560.0 * (1. - 0.7311);
        final double apo = 24464560.0 * (1. + 0.7311);

        // Build each orbit objects using the constructor with the PositionAngle (set to MEAN)
        // attribute.
        final Orbit alternateEqinoctial = new AlternateEquinoctialOrbit(n, 0.5, -0.5, hx, hy,
            5.300, PositionAngle.MEAN, FramesFactory.getEME2000(), date, mu);
        final Orbit apsis = new ApsisOrbit(per, apo, 0.122138, 3.10686, 1.00681, 0.048363,
            PositionAngle.MEAN, frame,
            date, mu);
        final Orbit circular = new CircularOrbit(a, ex, ey, i, raan, alphaM, PositionAngle.MEAN,
            frame, date, mu);
        final Orbit equatorial = new EquatorialOrbit(7000000.0, 0.01, 6.2631853071796044,
            9.1699286388104417E-01, 1.6047375117918272, 6.9813170079771392E-01,
            PositionAngle.MEAN, frame, date, mu);
        final Orbit equinoctial = new EquinoctialOrbit(a1, ex1, ey1, hx1, hy1, lm1,
            PositionAngle.MEAN, veis1950, date, mu);
        final Orbit keplerian = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.MEAN,
            frame, date, mu);
        final Orbit cartesian = new CartesianOrbit(pvCoordinates, frame, date, mu);

        final double dt = 60;

        final RealMatrix stateTransMatAlter = alternateEqinoctial.getKeplerianTransitionMatrix(dt);
        final RealMatrix stateTransMatApsis = apsis.getKeplerianTransitionMatrix(dt);
        final RealMatrix stateTransMatCarte = cartesian.getKeplerianTransitionMatrix(dt);
        final RealMatrix stateTransMatCircu = circular.getKeplerianTransitionMatrix(dt);
        final RealMatrix stateTransMatEquat = equatorial.getKeplerianTransitionMatrix(dt);
        final RealMatrix stateTransMatEquin = equinoctial.getKeplerianTransitionMatrix(dt);
        final RealMatrix stateTransMatKeple = keplerian.getKeplerianTransitionMatrix(dt);

        final double dMda0Alter = -3. / 2. * dt * alternateEqinoctial.getKeplerianMeanMotion() / alternateEqinoctial.getA();
        final double dMda0Apsis = -3. / 2. * dt * apsis.getKeplerianMeanMotion() / apsis.getA();
        final double dMda0Carte = -3. / 2. * dt * cartesian.getKeplerianMeanMotion() / cartesian.getA();
        final double dMda0Circu = -3. / 2. * dt * circular.getKeplerianMeanMotion() / circular.getA();
        final double dMda0Equat = -3. / 2. * dt * equatorial.getKeplerianMeanMotion() / equatorial.getA();
        final double dMda0Equin = -3. / 2. * dt * equinoctial.getKeplerianMeanMotion() / equinoctial.getA();
        final double dMda0Keple = -3. / 2. * dt * keplerian.getKeplerianMeanMotion() / keplerian.getA();

        final int STATE_DIM = 6;

        RealMatrix expectAlter = new Array2DRowRealMatrix(STATE_DIM, STATE_DIM)
            .add(new DiagonalMatrix(new ArrayRealVector(STATE_DIM, 1.0).toArray()));
        expectAlter.setEntry(5, 0, dMda0Alter);

        RealMatrix expectApsis = new Array2DRowRealMatrix(STATE_DIM, STATE_DIM)
            .add(new DiagonalMatrix(new ArrayRealVector(STATE_DIM, 1.0).toArray()));
        expectApsis.setEntry(5, 0, dMda0Apsis);

        RealMatrix expectCarte = new Array2DRowRealMatrix(STATE_DIM, STATE_DIM)
            .add(new DiagonalMatrix(new ArrayRealVector(STATE_DIM, 1.0).toArray()));
        expectCarte.setEntry(5, 0, dMda0Carte);

        final RealMatrix expectCircu = new Array2DRowRealMatrix(STATE_DIM, STATE_DIM)
            .add(new DiagonalMatrix(new ArrayRealVector(STATE_DIM, 1.0).toArray()));
        expectCircu.setEntry(5, 0, dMda0Circu);

        final RealMatrix expectEquat = new Array2DRowRealMatrix(STATE_DIM, STATE_DIM)
            .add(new DiagonalMatrix(new ArrayRealVector(STATE_DIM, 1.0).toArray()));
        expectEquat.setEntry(5, 0, dMda0Equat);

        final RealMatrix expectEquin = new Array2DRowRealMatrix(STATE_DIM, STATE_DIM)
            .add(new DiagonalMatrix(new ArrayRealVector(STATE_DIM, 1.0).toArray()));
        expectEquin.setEntry(5, 0, dMda0Equin);

        final RealMatrix expectKeple = new Array2DRowRealMatrix(STATE_DIM, STATE_DIM)
            .add(new DiagonalMatrix(new ArrayRealVector(STATE_DIM, 1.0).toArray()));
        expectKeple.setEntry(5, 0, dMda0Keple);

        // Expected specific case for the cartesian orbit
        final RealMatrix jacobT0Carte = cartesian.getJacobian(OrbitType.EQUINOCTIAL, cartesian.getType());
        final RealMatrix jacobTCarte = cartesian.shiftedBy(dt).getJacobian(cartesian.getType(), OrbitType.EQUINOCTIAL);
        expectCarte = jacobTCarte.multiply(expectCarte.multiply(jacobT0Carte));

        // Expected specific case for the apsis orbit
        final RealMatrix jacobT0Apsis = apsis.getJacobian(OrbitType.EQUINOCTIAL, apsis.getType());
        final RealMatrix jacobTApsis = apsis.shiftedBy(dt).getJacobian(apsis.getType(), OrbitType.EQUINOCTIAL);
        expectApsis = jacobTApsis.multiply(expectApsis.multiply(jacobT0Apsis));
        
        // Expected specific case for the alternate eqinoctial orbit
        final RealMatrix jacobT0Alter = alternateEqinoctial.getJacobian(OrbitType.EQUINOCTIAL, alternateEqinoctial.getType());
        final RealMatrix jacobTAlter = alternateEqinoctial.shiftedBy(dt).getJacobian(alternateEqinoctial.getType(), OrbitType.EQUINOCTIAL);
        expectAlter = jacobTAlter.multiply(expectAlter.multiply(jacobT0Alter));

        final double threshold = 0.0;
        for (int j = 0; j < 6; j++) {
            for (int k = 0; k < 6; k++) {
                Assert.assertEquals(stateTransMatAlter.getEntry(j, k), expectAlter.getEntry(j, k),
                    threshold);
                Assert.assertEquals(stateTransMatApsis.getEntry(j, k), expectApsis.getEntry(j, k),
                    threshold);
                Assert.assertEquals(stateTransMatCarte.getEntry(j, k), expectCarte.getEntry(j, k),
                    threshold);
                Assert.assertEquals(stateTransMatCircu.getEntry(j, k), expectCircu.getEntry(j, k),
                    threshold);
                Assert.assertEquals(stateTransMatEquat.getEntry(j, k), expectEquat.getEntry(j, k),
                    threshold);
                Assert.assertEquals(stateTransMatEquin.getEntry(j, k), expectEquin.getEntry(j, k),
                    threshold);
                Assert.assertEquals(stateTransMatKeple.getEntry(j, k), expectKeple.getEntry(j, k),
                    threshold);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @description check that a Keplerian orbit shifted by 0s returns the exact same orbit
     * 
     * @testPassCriteria The returned orbit is exactly the same as the initial orbit
     * 
     * @referenceVersion 4.5
     * 
     * @nonRegressionVersion 4.5
     */
    @Test
    public final void testFT2468() throws IllegalArgumentException, PatriusException {

        // Create reference orbit
        final KeplerianOrbit refOrbit = new KeplerianOrbit(6600.0e3, 0.9, 0.9, 0.5, 0.6, 1.2, PositionAngle.TRUE,
                FramesFactory.getCIRF(), new AbsoluteDate(), Constants.CNES_STELA_MU);

        // Create shifted orbit
        final KeplerianOrbit shiftedOrbit = (KeplerianOrbit) refOrbit.shiftedBy(0.0);

        // Check
        Assert.assertEquals(refOrbit.getAnomaly(PositionAngle.TRUE), shiftedOrbit.getAnomaly(PositionAngle.TRUE), 0.);
        Assert.assertEquals(0, refOrbit.getPVCoordinates().getPosition().distance(shiftedOrbit.getPVCoordinates().getPosition()), 0);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link Orbit#shiftedBy(double)}
     * 
     * @description check that Orbit.shiftedBy method does not raise exception if dt = 0
     * 
     * @testPassCriteria no exception is thrown
     * 
     * @referenceVersion 4.9.2
     * 
     * @nonRegressionVersion 4.9.2
     */
    @Test
    public void testFT3236() throws PatriusException {
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getITRF(), AbsoluteDate.J2000_EPOCH, Constants.CNES_STELA_MU);
        try {
            orbit.shiftedBy(0.);
            Assert.assertTrue(true);
        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }
    }

    /**
     * @testType UT
     * @testedFeature {@link Features#COLOSUS}
     *
     * @description Tests the computation of the Jacobians matrix.
     *
     * @testedMethod {@link TransformationUtils#getJacobian(Orbit, Frame, Frame, OrbitType, OrbitType, PositionAngle, PositionAngle)}
     *
     * @testPassCriteria The Jacobians matrix computed is the one expected.
     *
     * @referenceVersion 4.7
     * @nonRegressionVersion 4.7
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransformJacobianAllDifferent() throws PatriusException {
        Utils.setDataRoot("patriusdataset140");
        FramesFactory.setConfiguration(FramesConfigurationFactory.getIERS2010Configuration());
        // Orbit used for the tests
        final AbsoluteDate DATE = AbsoluteDate.J2000_EPOCH;
        final Orbit ORBIT = new KeplerianOrbit(7000000, 0.05, MathLib.toRadians(87), 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getEME2000(), DATE, Constants.CNES_STELA_MU);
        // Expected Jacobians matrix
        final double[][] data = new double[][] {
                { +2.204306835773834E+00, +3.669944621123036E-06, +2.112586370976925E-07, -1.572050644978659E-04,
                        -1.704332216611806E+01, +1.936376436167167E+03 },
                { +2.790373450945979E-08, +1.136625868316173E-09, +1.479339834266202E-07, +1.300902487119967E-04,
                        -4.119931847401096E-07, +4.679052043793395E-05 },
                { +1.551538246640517E-07, -2.038978366889197E-10, -2.660526106150696E-08, -2.339618294857573E-05,
                        -2.289902374454298E-06, +2.601704259749061E-04 },
                { +1.643522396317823E-09, -1.493057256702297E-07, -1.314093189553968E-09, -1.579355588130542E-12,
                        -2.253848742465685E-05, -1.983760466926656E-07 },
                { +9.138577951737561E-09, +2.685197754972047E-08, +2.365902854170666E-10, -8.781729621961646E-12,
                        -1.253213044949092E-04, -1.103035198576391E-06 },
                { -2.250783255889947E-13, +1.423051064260026E-07, -2.215922530622735E-09, -1.292001406190715E-04,
                        +3.236323218084077E-09, +1.799587214307209E-11 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);

        // Computed Jacobians matrix
        final RealMatrix actual = ORBIT.getJacobian(0., FramesFactory.getGCRF(),
                FramesFactory.getITRF(), OrbitType.CARTESIAN, OrbitType.EQUINOCTIAL, PositionAngle.MEAN,
                PositionAngle.ECCENTRIC);

        // Check equality
        CheckUtils.checkEquality(expected, actual, 0, 1E-14);
    }

    /**
     * @testType UT
     * @testedFeature {@link Features#COLOSUS}
     *
     * @description Tests the computation of the Jacobians matrix between frames in Cartesian
     *              coordinates.
     *
     * @testedMethod {@link TransformationUtils#getJacobian(Orbit, Frame, Frame, OrbitType, OrbitType, PositionAngle, PositionAngle)}
     *
     * @testPassCriteria The Jacobians matrices computed are the ones expected.
     *
     * @referenceVersion 4.7
     * @nonRegressionVersion 4.7
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransformDifferentFrameOnly() throws PatriusException {
        // Orbit used for the tests
        final AbsoluteDate DATE = AbsoluteDate.J2000_EPOCH;
        final Orbit ORBIT = new KeplerianOrbit(7000000, 0.05, MathLib.toRadians(87), 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getEME2000(), DATE, Constants.CNES_STELA_MU);
        // Fixed orbit type and position angle
        final OrbitType orbitType = OrbitType.CARTESIAN;
        final PositionAngle angleType = PositionAngle.MEAN;

        // Frames tested
        final List<Frame> frames = new ArrayList<>();
        frames.add(FramesFactory.getGCRF());
        frames.add(FramesFactory.getEME2000());
        frames.add(FramesFactory.getCIRF());
        frames.add(FramesFactory.getTIRF());
        frames.add(FramesFactory.getTEME());
        frames.add(FramesFactory.getITRF());
        frames.add(FramesFactory.getVeis1950());
        frames.add(FramesFactory.getEODFrame(false));
        frames.add(FramesFactory.getEODFrame(true));
        frames.add(FramesFactory.getGTOD(false));
        frames.add(FramesFactory.getGTOD(true));

        for (final Pair<Frame, Frame> pair : generatePermutations(frames)) {
            // Initial and destination frames
            final Frame initFrame = pair.getFirst();
            final Frame destFrame = pair.getSecond();

            // Expected Jacobians matrix
            final RealMatrix expected = initFrame.getTransformJacobian(destFrame, DATE);

            // Computed Jacobians matrix
            final RealMatrix actual = ORBIT.getJacobian(0., initFrame, destFrame, orbitType,
                    orbitType, angleType, angleType);

            // Check equality
            CheckUtils.checkEquality(expected, actual, 0, 1E-14);
        }
    }

    /**
     * @testType UT
     * @testedFeature {@link Features#COLOSUS}
     *
     * @description Tests the computation of the Jacobians matrix between orbit types, without
     *              changing the frame and position angle.
     *
     * @testedMethod {@link TransformationUtils#getJacobian(Orbit, Frame, Frame, OrbitType, OrbitType, PositionAngle, PositionAngle)}
     *
     * @testPassCriteria The Jacobians matrices computed are the ones expected.
     *
     * @referenceVersion 4.7
     * @nonRegressionVersion 4.7
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransformJacobianDifferentOrbitTypeOnly() throws PatriusException {
        // Orbit used for the tests
        final AbsoluteDate DATE = AbsoluteDate.J2000_EPOCH;
        final Orbit ORBIT = new KeplerianOrbit(7000000, 0.05, MathLib.toRadians(87), 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getEME2000(), DATE, Constants.CNES_STELA_MU);
        // Fixed orbit type and position angle
        final Frame frame = FramesFactory.getGCRF();
        final PositionAngle angleType = PositionAngle.MEAN;

        // Frames tested
        final List<OrbitType> orbitTypes = new ArrayList<>();
        orbitTypes.add(OrbitType.CARTESIAN);
        orbitTypes.add(OrbitType.KEPLERIAN);
        orbitTypes.add(OrbitType.EQUINOCTIAL);
        orbitTypes.add(OrbitType.CIRCULAR);
        orbitTypes.add(OrbitType.EQUATORIAL);
        orbitTypes.add(OrbitType.APSIS);
        orbitTypes.add(OrbitType.ALTERNATE_EQUINOCTIAL);

        for (final Pair<OrbitType, OrbitType> pair : generatePermutations(orbitTypes)) {
            // Initial and destination frames
            final OrbitType initOrbitType = pair.getFirst();
            final OrbitType destOrbitType = pair.getSecond();

            // Expected Jacobians matrix
            RealMatrix expected = MatrixUtils.createRealIdentityMatrix(6);

            if (!initOrbitType.equals(destOrbitType)) {
                // Jacobians matrix between the initial orbit type and Cartesian coordinates
                if (!initOrbitType.equals(OrbitType.CARTESIAN)) {
                    final Orbit orbit1 = initOrbitType.convertOrbit(ORBIT, frame);
                    final double[][] data1 = new double[6][6];
                    orbit1.getJacobianWrtParameters(angleType, data1);
                    expected = expected.preMultiply(new Array2DRowRealMatrix(data1));
                }

                // Jacobians matrix between Cartesian coordinates and the destination orbit type
                if (!destOrbitType.equals(OrbitType.CARTESIAN)) {
                    final Orbit orbit2 = destOrbitType.convertOrbit(ORBIT, frame);
                    final double[][] data2 = new double[6][6];
                    orbit2.getJacobianWrtCartesian(angleType, data2);
                    expected = expected.preMultiply(new Array2DRowRealMatrix(data2));
                }
            }

            // Computed Jacobians matrix
            final RealMatrix actual = ORBIT.getJacobian(0., frame, frame, initOrbitType,
                    destOrbitType, angleType, angleType);

            // Check equality
            CheckUtils.checkEquality(expected, actual, 0, 1E-14);
        }
    }

    /**
     * @testType UT
     * @testedFeature {@link Features#COLOSUS}
     *
     * @description Tests the computation of the Jacobians matrix between position angle type,
     *              without changing the frame and orbit type.
     *
     * @testedMethod {@link TransformationUtils#getJacobian(Orbit, Frame, Frame, OrbitType, OrbitType, PositionAngle, PositionAngle)}
     *
     * @testPassCriteria The Jacobians matrices computed are the ones expected.
     *
     * @referenceVersion 4.7
     * @nonRegressionVersion 4.7
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testTransformJacobianDifferentPositionAngleOnly() throws PatriusException {
        // Orbit used for the tests
        final AbsoluteDate DATE = AbsoluteDate.J2000_EPOCH;
        final Orbit ORBIT = new KeplerianOrbit(7000000, 0.05, MathLib.toRadians(87), 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getEME2000(), DATE, Constants.CNES_STELA_MU);
        // Fixed orbit type and position angle
        final Frame frame = FramesFactory.getGCRF();
        final OrbitType orbitType = OrbitType.KEPLERIAN;

        // Position angle types tested
        final List<PositionAngle> angleTypes = new ArrayList<>();
        angleTypes.add(PositionAngle.TRUE);
        angleTypes.add(PositionAngle.MEAN);
        angleTypes.add(PositionAngle.ECCENTRIC);

        for (final Pair<PositionAngle, PositionAngle> pair : generatePermutations(angleTypes)) {
            // Initial and destination frames
            final PositionAngle initAngleType = pair.getFirst();
            final PositionAngle destAngleType = pair.getSecond();

            // Expected Jacobians matrix
            RealMatrix expected = MatrixUtils.createRealIdentityMatrix(6);

            if (!orbitType.equals(OrbitType.CARTESIAN) && !initAngleType.equals(destAngleType)) {
                final Orbit orbit = orbitType.convertOrbit(ORBIT, frame);

                final double[][] data1 = new double[6][6];
                orbit.getJacobianWrtParameters(initAngleType, data1);
                expected = expected.preMultiply(new Array2DRowRealMatrix(data1));

                final double[][] data2 = new double[6][6];
                orbit.getJacobianWrtCartesian(destAngleType, data2);
                expected = expected.preMultiply(new Array2DRowRealMatrix(data2));
            }

            // Computed Jacobians matrix
            final RealMatrix actual = ORBIT.getJacobian(0., frame, frame, orbitType,
                    orbitType, initAngleType, destAngleType);

            // Check equality
            CheckUtils.checkEquality(expected, actual, 0, 1E-14);
        }
    }

    /**
     * @testType UT
     * @testedFeature {@link Features#COLOSUS}
     *
     * @description Tests the computation of the Keplerian transition matrix with no actual time
     *              shift (dt =0).
     *
     * @testedMethod {@link TransformationUtils#getKeplerianTransitionMatrix(double, Orbit, Frame, Frame, OrbitType, OrbitType, PositionAngle, PositionAngle)}
     *
     * @testPassCriteria The Keplerian transition matrix computed is the one expected (i.e. just the
     *                   transformation matrix between the initial and destination frames, orbit
     *                   types and position angle types).
     *
     * @referenceVersion 4.7
     * @nonRegressionVersion 4.7
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testKeplerianTransitionMatrixNoTimeShift() throws PatriusException {
        // Orbit used for the tests
        final AbsoluteDate DATE = AbsoluteDate.J2000_EPOCH;
        final Orbit ORBIT = new KeplerianOrbit(7000000, 0.05, MathLib.toRadians(87), 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getEME2000(), DATE, Constants.CNES_STELA_MU);
        final double dt = 0;

        final Frame initFrame = FramesFactory.getGCRF();
        final Frame destFrame = FramesFactory.getITRF();

        final OrbitType initOrbitType = OrbitType.EQUINOCTIAL;
        final OrbitType destOrbitType = OrbitType.CIRCULAR;

        final PositionAngle initAngleType = PositionAngle.MEAN;
        final PositionAngle destAngleType = PositionAngle.ECCENTRIC;

        final RealMatrix actual = ORBIT.getJacobian(dt, initFrame, destFrame,
                initOrbitType, destOrbitType, initAngleType, destAngleType);

        final RealMatrix expected = ORBIT.getJacobian(initFrame, destFrame,
                initOrbitType, destOrbitType, initAngleType, destAngleType);

        CheckUtils.checkEquality(expected, actual, 0, 1E-14);
    }

    /**
     * @testType UT
     * @testedFeature {@link Features#COLOSUS}
     *
     * @description Tests the computation of the Keplerian transition matrix (negative time shift,
     *              different frames, orbit types and position angle types).
     *
     * @testedMethod {@link TransformationUtils#getKeplerianTransitionMatrix(double, Orbit, Frame, Frame, OrbitType, OrbitType, PositionAngle, PositionAngle)}
     *
     * @testPassCriteria The Keplerian transition matrix computed is the one expected.
     *
     * @referenceVersion 4.7
     * @nonRegressionVersion 4.7
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testKeplerianTransitionMatrix1() throws PatriusException {
        Utils.setDataRoot("patriusdataset140");
        FramesFactory.setConfiguration(FramesConfigurationFactory.getIERS2010Configuration());
        // Orbit used for the tests
        final AbsoluteDate DATE = AbsoluteDate.J2000_EPOCH;
        final Orbit ORBIT = new KeplerianOrbit(7000000, 0.05, MathLib.toRadians(87), 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getEME2000(), DATE, Constants.CNES_STELA_MU);
        final double dt = -150;

        final Frame initFrame = FramesFactory.getGCRF();
        final Frame destFrame = FramesFactory.getITRF();

        final OrbitType initOrbitType = OrbitType.ALTERNATE_EQUINOCTIAL;
        final OrbitType destOrbitType = OrbitType.APSIS;

        final PositionAngle initAngleType = PositionAngle.TRUE;
        final PositionAngle destAngleType = PositionAngle.ECCENTRIC;

        final RealMatrix actual = ORBIT.getJacobian(dt, initFrame, destFrame,
                initOrbitType, destOrbitType, initAngleType, destAngleType);

        final double[][] data = {
                { -4.113029217053092E+09, -7.000838307218823E+06, -6.962899661348412E+04, +1.537009326106234E+04,
                        -1.643528348457151E+02, +3.637804328920121E+03 },
                { -4.533665742762486E+09, +6.883057294102346E+06, +8.911055700668189E+04, +1.960224086667627E+06,
                        -2.096073069364949E+04, +1.548539035684762E+04 },
                { -5.845169617810908E+01, -1.277425765399821E-01, +2.075208573099659E-02, +1.051721551974414E+00,
                        -2.252954114680912E-02, +2.036686744443759E-02 },
                { +1.424627987647924E+01, -1.911682802581024E-01, +2.118370419314648E+01, -5.391856550957956E-01,
                        -1.046669459851269E+00, -6.597416764955573E-02 },
                { +4.077445020023145E-01, +6.922522637407933E-04, -1.887718534824362E-03, -4.841178010716536E-06,
                        +9.933419368790238E-01, +5.744791744025219E-02 },
                { -1.717175908863360E+02, +1.369740432276837E-02, -2.019314858147541E+01, +5.279208429008163E-01,
                        -5.645072260276308E-03, +1.015027848403688E+00 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);

        CheckUtils.checkEquality(expected, actual, 0, 1E-14);
    }

    /**
     * @testType UT
     * @testedFeature {@link Features#COLOSUS}
     *
     * @description Tests the computation of the Keplerian transition matrix (positive time shift,
     *              different frames, orbit types and position angle types).
     *
     * @testedMethod {@link TransformationUtils#getKeplerianTransitionMatrix(double, Orbit, Frame, Frame, OrbitType, OrbitType, PositionAngle, PositionAngle)}
     *
     * @testPassCriteria The Keplerian transition matrix computed is the one expected.
     *
     * @referenceVersion 4.7
     * @nonRegressionVersion 4.7
     *
     * @throws PatriusException
     *         if an error occurs during the test
     */
    @Test
    public void testKeplerianTransitionMatrix2() throws PatriusException {
        Utils.setDataRoot("patriusdataset140");
        FramesFactory.setConfiguration(FramesConfigurationFactory.getIERS2010Configuration());
        // Orbit used for the tests
        final AbsoluteDate DATE = AbsoluteDate.J2000_EPOCH;
        final Orbit ORBIT = new KeplerianOrbit(7000000, 0.05, MathLib.toRadians(87), 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getEME2000(), DATE, Constants.CNES_STELA_MU);
        final double dt = +75;

        final Frame initFrame = FramesFactory.getGCRF();
        final Frame destFrame = FramesFactory.getITRF();

        final OrbitType initOrbitType = OrbitType.APSIS;
        final OrbitType destOrbitType = OrbitType.KEPLERIAN;

        final PositionAngle initAngleType = PositionAngle.MEAN;
        final PositionAngle destAngleType = PositionAngle.ECCENTRIC;

        final RealMatrix actual = ORBIT.getJacobian(dt, initFrame, destFrame,
                initOrbitType, destOrbitType, initAngleType, destAngleType);

        final double[][] data = {
                { +5.037675915878851E-01, +4.954453705864932E-01, +9.389162649247157E+05, -5.100546956739944E+03,
                        +2.565880102628035E+01, -5.373873237394642E+03 },
                { -7.451138896665683E-08, +6.763918775553840E-08, +1.276600631467243E-01, -6.934975683250265E-04,
                        +3.488707440208775E-06, -4.617104106788447E-04 },
                { +1.607910555270652E-08, -1.962592846642505E-09, +9.994508402434646E-01, -1.086193413710230E-02,
                        +2.571456836227956E-05, -1.144389951544147E-02 },
                { -6.119932614291081E-09, +7.084907277740665E-09, +2.565952830849497E-01, +9.972793998539775E-01,
                        -2.047543278236604E-05, -6.743333469426079E-02 },
                { +1.204768472583595E-11, +5.892595341847223E-14, -2.806478002459775E-05, +6.017602974677235E-02,
                        +9.999997844268929E-01, +6.661949626498108E-02 },
                { -9.662955167535264E-09, -1.013645566243493E-08, -2.511608721498421E-01, +1.364400500786995E-03,
                        -6.863750353106600E-06, +1.118366452022012E+00 } };
        final RealMatrix expected = new Array2DRowRealMatrix(data);

        CheckUtils.checkEquality(expected, actual,0, 1E-14);
    }

    /**
     * Gets every pair of elements that can be build from the provided list.
     *
     * @param <T>
     *        the type of the elements in the list
     * @param list
     *        the list
     *
     * @return the different pairs of elements that can be build from the list
     */
    private static <T> List<Pair<T, T>> generatePermutations(final List<T> list) {
        final List<Pair<T, T>> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            for (int j = 0; j <= i; j++) {
                out.add(new Pair<T, T>(list.get(i), list.get(j)));
            }
        }
        return out;
    }

    /**
     * @throws PatriusException
     * @description start ot the test
     * 
     * 
     * @since 1.0
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));

        try {
            validate = new Validate(OrbitTest.class);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @description end of the test
     * 
     * @since 1.0
     */
    @AfterClass
    public static void tearDown() {
        try {
            validate.produceLog();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
