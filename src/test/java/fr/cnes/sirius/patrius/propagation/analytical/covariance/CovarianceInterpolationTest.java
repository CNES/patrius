/**
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
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:277:21/08/2014:Covariance Matrix Interpolation
 * VERSION::DM:289:27/08/2014:Add exception to SpacececraftState.getAttitude()
 * VERSION::FA:387:05/12/2014:Problem in Covariance Matrix Interpolation
 * VERSION::DM:284:06/01/2015:New architecture for parameterizable Parameters
 * VERSION::FA:381:14/01/2015:deleting DEFAULT_LAW in Propagator interface
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::FA:437:06/05/2015:Problem in Covariance Matrix Interpolation Algorithm
 * VERSION::FA:482:02/11/2015:Changes in tests due to the new implementation of the class CovarianceInterpolation
 * VERSION::FA:482:02/12/2015:Move tests for class Orbit to Orekit
 * VERSION::FA:579:17/03/2016:Bug in method createApproximatedTransitionMatrix() of CovarianceInterpolation class
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
/**
 *
 *
 * @history creation 21/08/2014
 */
package fr.cnes.sirius.patrius.propagation.analytical.covariance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix;
import fr.cnes.sirius.patrius.math.linear.ArrayRowSymmetricMatrix.SymmetryType;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * 
 * This class verifies the interpolation of two covariance matrices by comparison with experiments
 * 
 * @author Sophie Laurens
 * 
 * @version $Id: CovarianceInterpolationTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 2.3
 * 
 */
public class CovarianceInterpolationTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle CovarianceInterpolationTest
         * @featureDescription tests the class CovarianceInterpolation
         * @coveredRequirements
         */
        COVARIANCE_INTERPOLATION,
        /**
         * @featureTitle OrbitCovarianceTest
         * @featureDescription tests the construction of class OrbitCovariance : its getters and setters.
         * @coveredRequirements
         */
        ORBIT_COVARIANCE,
    }

    /** Parameter name for absorption coefficient. */
    public static final String ABSORPTION_COEFFICIENT = "absorption coefficient";

    /** Parameter name for reflection coefficient. */
    public static final String SPECULAR_COEFFICIENT = "specular reflection coefficient";

    /** Parameter name for diffusion coefficient. */
    public static final String DIFFUSION_COEFFICIENT = "diffusion reflection coefficient";

    /** List of the parameters names. */
    private static final ArrayList<Parameter> PARAMETERS;
    static {
        PARAMETERS = new ArrayList<>();
        PARAMETERS.add(new Parameter(ABSORPTION_COEFFICIENT, 0.));
        PARAMETERS.add(new Parameter(SPECULAR_COEFFICIENT, 0.));
        PARAMETERS.add(new Parameter(DIFFUSION_COEFFICIENT, 0.));
    }

    /** Covariance Matrix */
    private RealMatrix[] CovMatrix;
    /** OrbitArray */
    private Orbit[] OrbitArray;
    /** Date shift with respect to some reference istant */
    private double[] dt;

    // constants
    /** Constant = 6. */
    private static final int DIM_SIX = 6;
    /** Precision requested for the assert.equals. */
    private static final double EPSILON = 10e-15;

    // attributes
    /** Gravitational potential. */
    final double mu = Constants.GRIM5C1_EARTH_MU;
    /** Keplerian orbit */
    KeplerianOrbit orbit;
    /** t0 */
    AbsoluteDate t0;
    /** t1 */
    AbsoluteDate t1;

    /** Covariance matrix at t0 */
    RealMatrix covarianceMatrixTest0;
    /** Covariance matrix at t1 */
    RealMatrix covarianceMatrixTest1;

    // This test will not be run at each execution
    // see simpleInterpolationTest() below
    //
    // This test is to be used only to generate validation data (will be written on file)
    // as of 06/05/2015
    // validation results are in accord with CNES results for order 0, 1
    // apparently small differences arise for the order 2 case
    public void testInterpolation() throws PatriusException, IOException {

        // Load CNES ephemeris file (pos, vel, 6x6 covariance matrix)
        this.loadEphem();

        final int length = this.CovMatrix.length;

        // Index step (10 = 600s)
        final int deltai = 10;

        // Interpolation Order
        final int interpOrder = 1;

        final String fileName = "res_ordre" + String.valueOf(interpOrder);

        // Output folder
        final String out_folder = "C:/Users/chabaudp/Desktop/resultats_test_covariance_interpolation/interp_";

        final File f1Interp = new File(out_folder + fileName + ".txt");

        final FileWriter fw1Interp = new FileWriter(f1Interp);

        // Create instance of Covariance Interpolation
        final CovarianceInterpolation interpolation =
            new CovarianceInterpolation(this.t0, this.CovMatrix[0], this.t1, this.CovMatrix[10],
                interpOrder,
                this.OrbitArray[deltai / 2], this.mu);

        for (int i = 0; i < length - deltai; i = i + deltai) {

            final AbsoluteDate from = AbsoluteDate.J2000_EPOCH.shiftedBy(this.dt[i]);
            final AbsoluteDate to = AbsoluteDate.J2000_EPOCH.shiftedBy(this.dt[i + deltai]);

            // Orbit corresponding to the middle point of [tstart, tend]
            interpolation.setFirstCovarianceMatrix(this.CovMatrix[i], from);
            interpolation.setSecondCovarianceMatrix(this.CovMatrix[i + deltai], to);

            // Position corresponding to the mid-point of the interpolation interval is used
            final Orbit midTimeOrbit = this.OrbitArray[i + deltai / 2];
            interpolation.setOrbit(midTimeOrbit);

            // Interpolate Covariance Matrix in interval
            // with a 60 sec step
            // [tstart, tend]
            for (int jj = 0; jj < deltai; jj++) {

                final AbsoluteDate dateInterp = AbsoluteDate.J2000_EPOCH.shiftedBy(this.dt[i + jj]);

                final double[][] interpcov = interpolation.interpolate(dateInterp).getData();

                fw1Interp.write(this.dt[i + jj] + " ");

                final Vector3D Pos = this.OrbitArray[i + jj].getPVCoordinates().getPosition();
                final Vector3D Vel = this.OrbitArray[i + jj].getPVCoordinates().getVelocity();

                // Write current position
                fw1Interp.write(Pos.getX() + " ");
                fw1Interp.write(Pos.getY() + " ");
                fw1Interp.write(Pos.getZ() + " ");

                // Write current velocity
                fw1Interp.write(Vel.getX() + " ");
                fw1Interp.write(Vel.getY() + " ");
                fw1Interp.write(Vel.getZ() + " ");

                for (final double[] element : interpcov) {
                    for (int l = 0; l < interpcov[0].length; l++) {
                        fw1Interp.write(element[l] + " ");
                    }
                }
                fw1Interp.write("\r\n");

            }

        }
        // Close file
        fw1Interp.close();

    }

    /**
     * @testType RVT
     * 
     * @testedFeature {@link features#COVARIANCE_INTERPOLATION}
     * 
     * @testedMethod {@link CovarianceInterpolation#interpolate(AbsoluteDate)}
     * 
     * @description Test covariance interpolation
     * 
     * @input Private attributes dt, covMatrix, orbitArray filled from reference ephemeris file with loadEphem method
     * 
     * @output OrbitCovariance propagation
     * 
     * @testPassCriteria Result covariance matrix propagation should be equal to the expected covariance matrix
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void simpleInterpolationTest() throws PatriusException, IOException {

        // Load CNES ephemeris file (pos, vel, 6x6 covariance matrix)
        this.loadEphem();

        // Values obtained from validation test (majorate interpolation residuals)
        // TOL_REL(i) is for interpolation order i
        // thresholds values are in relative threshold and serve to compare position standard deviations values
        final double[] TOL_REL = { 4.1e-2, 3.88e-4, 9.4e-6 };

        // Sub-sampling of covariance matrix with a 600s step

        // Index step (10 = 600s)
        final int deltai = 10;

        // Loop on Interpolation Order 0, 1, 2
        for (int ii = 0; ii < 3; ii++) {

            // Create instance of Covariance Interpolation

            final CovarianceInterpolation interpolation = new CovarianceInterpolation(new AbsoluteDate
                (AbsoluteDate.J2000_EPOCH, this.dt[0]), this.CovMatrix[0], new AbsoluteDate(AbsoluteDate.J2000_EPOCH,
                    this.dt[deltai]),
                this.CovMatrix[deltai], ii, this.OrbitArray[deltai / 2], this.mu);

            for (int i = 0; i < 30; i = i + deltai) {

                // Orbit corresponding to the middle point of [tstart, tend]
                final AbsoluteDate from = AbsoluteDate.J2000_EPOCH.shiftedBy(this.dt[i]);
                final AbsoluteDate to = AbsoluteDate.J2000_EPOCH.shiftedBy(this.dt[i + deltai]);
                interpolation.setFirstCovarianceMatrix(this.CovMatrix[i], from);
                interpolation.setSecondCovarianceMatrix(this.CovMatrix[i + deltai], to);

                // Position corresponding to the mid-point of the interpolation interval is used
                interpolation.setOrbit(this.OrbitArray[i + deltai / 2]);

                // Interpolate Covariance Matrix in interval
                // with a 60 sec step
                // [tstart, tend]
                for (int jj = 0; jj < deltai; jj++) {

                    final double[][] Covref = this.CovMatrix[i + jj].getData();
                    final AbsoluteDate dateInterp = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, this.dt[i + jj]);
                    final double[][] interpcov = interpolation.interpolateArray(dateInterp);

                    // compare actual and expected with relative threshold
                    for (int k = 0; k < 3; k++) {
                        final double sigmapos = MathLib.sqrt(interpcov[k][k]);
                        final double sigmaref = MathLib.sqrt(Covref[k][k]);
                        Assert.assertEquals(0, MathLib.abs((sigmapos - sigmaref) / sigmaref), TOL_REL[ii]);
                    }
                }
            }
        }
    }

    /**
     * @testType RVT
     * 
     * @testedFeature {@link features#ORBIT_COVARIANCE}
     * 
     * @testedMethod {@link OrbitCovariance#propagate(Orbit, AbsoluteDate)}
     * 
     * @description Extract the first line from the input reference ephemeris file to have initial date and
     *              covariance matrix Cov0. The initial orbit is loaded via loadEphem : OrbitArray[0].
     *              For each line, propagate with the same step than the input file and check that the propagation
     *              result is
     *              equal to the expected covariance matrix Cov, given by the formula :
     *              Cov = STM * Cov0 * STM^T, where STM is the State Transition Matrix
     * 
     * @input Private attributes dt, covMatrix, orbitArray filled from reference ephemeris file with loadEphem method
     * 
     * @output OrbitCovariance propagation
     * 
     * @testPassCriteria Result covariance matrix propagation should be equal to the expected covariance matrix
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void orbitCovariancePropagateTest() throws IOException {

        // Load CNES ephemeris file (pos, vel, 6x6 covariance matrix)
        this.loadEphem();

        final int length = this.CovMatrix.length;

        // threshold value used for comparison
        final double TOL = 1.0E-16;

        // Dates and frame used
        AbsoluteDate datePropag = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate from = this.OrbitArray[0].getDate();
        final Frame frame = FramesFactory.getGCRF();

        // Create an orbit covariance from current date and an initial covariance matrix (Cov0)
        final OrbitCovariance initCov = new OrbitCovariance(datePropag, frame, OrbitType.KEPLERIAN, this.CovMatrix[0]);

        // Actual covariance matrix obtained with method propagate of class OrbitCovariance
        // to be compared with the expected covariance matrix
        ArrayRowSymmetricMatrix covMatrixActual;
        RealMatrix covMatrixExpected;

        for (int i = 1; i < length; i++) {

            datePropag = datePropag.shiftedBy(this.dt[i]);

            // State Transition Matrix
            final RealMatrix transitionMatrix = this.OrbitArray[0].getKeplerianTransitionMatrix(datePropag
                .durationFrom(from));

            // Expected covariance matrix
            covMatrixExpected = transitionMatrix.multiply(this.CovMatrix[0]).multiply(transitionMatrix.transpose());

            // Computing actual covariance matrix
            covMatrixActual = initCov.propagate(this.OrbitArray[0], datePropag).getCovarianceMatrix();

            for (int k = 0; k < 6; k++) {
                for (int l = 0; l < 6; l++) {
                    Assert.assertEquals(covMatrixActual.getEntry(k, l), covMatrixExpected.getEntry(k, l), TOL);
                }
            }
        }
    }

    /**
     * Load CNES ephemeris
     * Position, Velocity, 6x6 Covariance Matrix
     * 
     * @throws IOException
     * @since 3.0
     */
    public void loadEphem() throws IOException {
        // Read ephemeris file
        // interpolate with a 60s step in a 600s interval [ta, tb] with tb = ta + 600s
        // write results to file
        // residual wrt reference values will be plotted in scilab and serve as validation

        // Read reference ephemeris
        final URL url = CovarianceInterpolationTest.class.getClassLoader().getResource(
            "covariance_interpolation/" + "ephem_PV_covariance.txt");
        final MatrixFileReader demo = new MatrixFileReader(url.getPath());
        final double[][] data = demo.getData();

        final int length = data.length;

        // Initialize
        final Orbit[] OrbitArray = new Orbit[length];
        final double[] dt = new double[length];
        final Vector3D[] Pos = new Vector3D[length]; // m
        final Vector3D[] Vel = new Vector3D[length]; // m/s

        final RealMatrix[] Cov = new RealMatrix[length];

        for (int i = 0; i < data.length; i++) {

            final double[] pos = { 0, 0, 0 };
            final double[] vel = { 0, 0, 0 };
            final double[][] cov = new double[6][6];

            // Read pos
            for (int j = 1; j < 4; j++) {
                pos[j - 1] = data[i][j];
            }
            // Read vel
            for (int j = 4; j < 7; j++) {
                vel[j - 4] = data[i][j];
            }

            // Read covariance matrix
            int irow = -1;
            int icol = -1;
            for (int j = 7; j < data[0].length; j++) {

                icol += 1;
                icol = icol % 6;

                if ((j - 7) % 6 == 0) {
                    // increment row
                    irow += 1;
                }
                cov[irow][icol] = data[i][j];
            }

            dt[i] = data[i][0];
            Pos[i] = new Vector3D(pos);
            Vel[i] = new Vector3D(vel);

            Cov[i] = new ArrayRowSymmetricMatrix(SymmetryType.LOWER, cov);

            OrbitArray[i] = new KeplerianOrbit(new PVCoordinates(Pos[i], Vel[i]), FramesFactory.getGCRF(),
                new AbsoluteDate(AbsoluteDate.J2000_EPOCH, dt[i]), this.mu);

        }
        this.dt = dt;
        this.CovMatrix = Cov;
        this.OrbitArray = OrbitArray;
    }

    /**
     * Prepares the context for tests.
     * Returns
     * propagator : a numerical propagator with additional state and a step handler
     * that allows to get the transition matrix.
     * jacobiHandler : a specific step handler for partial derivative eq, with a method getdYdY0().
     * 
     * @throws PatriusException
     *         if something goes wrong with the SpacecraftState
     * 
     * @since 3.0
     */
    @Before
    public void setUp() throws PatriusException {

        // usefull datas (JJCNES 21330 75360.0 : 2008-05-26:20:56:00 conv PHRLIB)
        final TimeScale tt = TimeScalesFactory.getTT();
        this.t0 = new AbsoluteDate(2008, 05, 26, 20, 56, 00, tt);
        this.t1 = this.t0.shiftedBy(10.);
        final Frame gcrf = FramesFactory.getGCRF();

        // orbital parameters (from file sol_niveau0_lirfs.dat sent 2014-08-27)
        final double x = -.44953016076297E+07;
        final double y = -.28014455839905E+07;
        final double z = 0.48762520407275E+07;
        final double vx = 0.33883408502692E+04;
        final double vy = 0.39037530746349E+04;
        final double vz = 0.53531021298816E+04;

        // creating Vector3D instances for position and speed
        final Vector3D pos = new Vector3D(x, y, z);
        final Vector3D vit = new Vector3D(vx, vy, vz);

        // creating PVCoordinates instance
        final PVCoordinates pvCoord = new PVCoordinates(pos, vit);

        // initial keplerian orbit
        this.orbit = new KeplerianOrbit(pvCoord, gcrf, this.t0, this.mu);

        // initial covariance matrix at t0
        final RealMatrix covMatrix = CovarianceInterpolation.createDiagonalMatrix(DIM_SIX, 0.);

        covMatrix.addToEntry(0, 0, 0.72748112033738E-03);

        covMatrix.addToEntry(1, 0, 0.42815120208706E-03);
        covMatrix.addToEntry(1, 1, 0.94227622166957E-03);

        covMatrix.addToEntry(2, 0, 0.77469009827896E-03);
        covMatrix.addToEntry(2, 1, 0.96066321830005E-03);
        covMatrix.addToEntry(2, 2, 0.17982113166405E-02);

        covMatrix.addToEntry(3, 0, 0.74542526736838E-06);
        covMatrix.addToEntry(3, 1, 0.83445548839229E-06);
        covMatrix.addToEntry(3, 2, 0.10801665518870E-05);
        covMatrix.addToEntry(3, 3, 0.11533338317619E-08);

        covMatrix.addToEntry(4, 0, 0.48961137851742E-06);
        covMatrix.addToEntry(4, 1, 0.51808725606655E-06);
        covMatrix.addToEntry(4, 2, 0.59621513602559E-06);
        covMatrix.addToEntry(4, 3, 0.49544881717110E-09);
        covMatrix.addToEntry(4, 4, 0.61999933342066E-09);

        covMatrix.addToEntry(5, 0, -.68236496601830E-06);
        covMatrix.addToEntry(5, 1, -.92081355817961E-06);
        covMatrix.addToEntry(5, 2, -.16260661682470E-05);
        covMatrix.addToEntry(5, 3, -.99519634424845E-09);
        covMatrix.addToEntry(5, 4, -.60901268663979E-09);
        covMatrix.addToEntry(5, 5, 0.15188526726135E-08);

        // re scaling the matrix (correction JE
        covMatrix.scalarMultiply(1E8);

        // Covariance matrix = matrix 6x6 number i at time ti
        this.covarianceMatrixTest0 = new OrbitCovariance(this.t0, null, null, covMatrix).getCovarianceMatrix();
        this.covarianceMatrixTest1 = new OrbitCovariance(this.t1, null, null, new Array2DRowRealMatrix(
            CovarianceInterpolation.createDiagonalArray(DIM_SIX, 2.))).getCovarianceMatrix();
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVARIANCE_INTERPOLATION}
     * 
     * @testedMethod {@link CovarianceInterpolation#getFirstCovarianceMatrix}
     * @testedMethod {@link CovarianceInterpolation#setFirstCovarianceMatrix},
     * @testedMethod {@link CovarianceInterpolation#getMu}
     * @testedMethod {@link CovarianceInterpolation#setMu},
     * @testedMethod {@link CovarianceInterpolation#getPolynomialOrder}
     * @testedMethod {@link CovarianceInterpolation#setPolynomialOrder},
     * @testedMethod {@link CovarianceInterpolation#getSecondCovarianceMatrix}
     * @testedMethod {@link CovarianceInterpolation#setSecondCovarianceMatrix},
     * @testedMethod {@link CovarianceInterpolation#getOrbit}
     * @testedMethod {@link CovarianceInterpolation#setOrbit}
     * 
     * @description tests the getters and setters for the class CovarianceInterpolation and builds coverage.
     * 
     * @testPassCriteria nominal case
     * 
     * @referenceVersion 2.3
     * @nonRegressionVersion 3.0
     * @throws PatriusException
     *         if something goes wrong with SpacecraftState
     */
    @Test
    public void testGettersAndSettersCovarianceInterpolation() throws PatriusException {

        final CovarianceInterpolation interpolation =
            new CovarianceInterpolation(this.t0, this.covarianceMatrixTest0, this.t1,
                this.covarianceMatrixTest1, 0,
                this.orbit, this.mu);

        // tests the getters
        final RealMatrix getCovarianceMatrix1 = interpolation.getFirstCovarianceMatrix();

        Assert.assertEquals(this.covarianceMatrixTest0, getCovarianceMatrix1);
        final double getmu = interpolation.getMu();
        Assert.assertEquals(this.mu, getmu, EPSILON);
        final int order = interpolation.getPolynomialOrder();
        Assert.assertEquals(order, 0);
        final RealMatrix getCovarianceMatrix3 = interpolation.getSecondCovarianceMatrix();
        Assert.assertEquals(this.covarianceMatrixTest1, getCovarianceMatrix3);
        final Orbit getOrbit = interpolation.getOrbit();
        Assert.assertEquals(this.orbit, getOrbit);

        // tests the setters
        interpolation.setFirstCovarianceMatrix(this.covarianceMatrixTest1, this.t1);
        Assert.assertEquals(this.covarianceMatrixTest1, interpolation.getFirstCovarianceMatrix());
        interpolation.setMu(14.);
        Assert.assertEquals(14., interpolation.getMu(), EPSILON);
        interpolation.setPolynomialOrder(1);
        Assert.assertEquals(1, interpolation.getPolynomialOrder());

        interpolation.setSecondCovarianceMatrix(this.covarianceMatrixTest0, this.t0);
        Assert.assertEquals(this.covarianceMatrixTest0, interpolation.getSecondCovarianceMatrix());
        final PVCoordinates pvCoord = new PVCoordinates(new Vector3D(14., 7., 83.), new Vector3D(23., 2., 84.));
        final KeplerianOrbit orbit2 = new KeplerianOrbit(pvCoord, FramesFactory.getGCRF(), this.t1, 14.);
        interpolation.setOrbit(orbit2);
        Assert.assertEquals(orbit2, interpolation.getOrbit());

        final AbsoluteDate d0 = interpolation.getT1();
        final AbsoluteDate d1 = interpolation.getT2();
        Assert.assertEquals(d0, this.t1);
        Assert.assertEquals(d1, this.t0);

    }

/**
     * @testType UT
     *
     * @testedFeature {@link features#COVARIANCE_INTERPOLATION}
     *
     * @testedMethod {@link CovarianceInterpolation#CovarianceInterpolation(AbsoluteDate, RealMatrix, AbsoluteDate, RealMatrix, int, Orbit, double)
     *               more precisely, the tested method is createApproximatedTransitionMatrix() (private)
     * 
     * @description This test builds coverage
     * @input an equinoctial orbit with PV coordinates which position is nearly null vector
     *
     * @testPassCriteria An exception must be risen
     *
     * @referenceVersion 3.1
     * @nonRegressionVersion 3.1
     * @throws PatriusException
     * @throws IOException
     */
    @Test(expected = IllegalArgumentException.class)
    public void exceptionTestCovarianceInterpolation() throws PatriusException, IOException {

        this.loadEphem();

        // Create instance of Covariance Interpolation
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(new Vector3D(1.0E-15, 1.0E-15, 1.0E-15),
            new Vector3D(1.0, 5.0E12, -6.0)), FramesFactory.getGCRF(),
            new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 0), this.mu);

        new CovarianceInterpolation(new AbsoluteDate
            (AbsoluteDate.J2000_EPOCH, this.dt[0]), this.CovMatrix[0], new AbsoluteDate(AbsoluteDate.J2000_EPOCH,
                this.dt[10]),
            this.CovMatrix[10], 1, orbit, this.mu);

    }

    /**
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#ORBIT_COVARIANCE}
     * 
     * @testedMethod {@link OrbitCovariance#OrbitCovariance(Orbit, RealMatrix)}
     * @testedMethod {@link OrbitCovariance#OrbitCovariance(Orbit, OrbitType, RealMatrix)}
     * @testedMethod {@link OrbitCovariance#getCovarianceMatrix(Orbit, OrbitType, Frame)}
     * @testedMethod {@link OrbitCovariance#getDate()}
     * @testedMethod {@link OrbitCovariance#propagate(Orbit, AbsoluteDate)}
     * 
     * @description coverage tests for methods of class {@link OrbitCovariance} in particular, this test is up to cover
     *              conditional cases in methods
     *              such as inFrame =! outFrame, inType =! outType
     * 
     * @testPassCriteria the results obtained are the same than the one expected
     * 
     * @referenceVersion 3.1
     * @nonRegressionVersion 3.1
     * @throws PatriusException
     *         if something goes wrong with SpacecraftState
     */
    @Test
    public void testMethodsForOrbitCovariance() throws IOException, PatriusException {

        // Load CNES ephemeris
        this.loadEphem();

        // Thresholds for matrices comparison
        final double eps = 1.0E-16;
        final double eps_rel = 1.0E-12;

        // Create an orbit covariance from current date and an initial covariance matrix (Cov0)
        final Orbit orbit = this.OrbitArray[0];
        final RealMatrix cov = this.CovMatrix[0];
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame eme = FramesFactory.getEME2000();
        final AbsoluteDate date = orbit.getDate();
        final OrbitType cart = OrbitType.CARTESIAN;
        final OrbitType kep = OrbitType.KEPLERIAN;

        final OrbitCovariance Cov = new OrbitCovariance(orbit, kep, cov);

        Assert.assertEquals(Cov.getDate(), date);

        // Test on covariance matrix with same orbit type and same frame :
        // the same matrix is expected
        final OrbitCovariance Cov1 = new OrbitCovariance(orbit, cov);
        final ArrayRowSymmetricMatrix sym1 = Cov1.getCovarianceMatrix(orbit, kep, gcrf);

        // Test on covariance with different orbit type, different frame :
        // conversions have to be performed
        final ArrayRowSymmetricMatrix sym2 = Cov1.getCovarianceMatrix(orbit, cart, eme);

        // Type conversion
        RealMatrix m = cov.copy();
        final RealMatrix jacob = orbit.getJacobian(cart, kep);
        m = jacob.multiply(m.multiply(jacob.transpose()));
        final ArrayRowSymmetricMatrix sym = new ArrayRowSymmetricMatrix(SymmetryType.UPPER, m);
        m = sym.copy();

        // Frame conversion
        final RealMatrix jacobFrame = gcrf.getTransformJacobian(eme, date);
        m = jacobFrame.multiply(m.multiply(jacobFrame.transpose()));
        final ArrayRowSymmetricMatrix temp = new ArrayRowSymmetricMatrix(SymmetryType.UPPER, m);
        m = temp.copy();
        final ArrayRowSymmetricMatrix sym3 = new ArrayRowSymmetricMatrix(SymmetryType.UPPER, m);

        // Comparisons
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                Assert.assertEquals(sym1.getEntry(i, j), Cov1.getCovarianceMatrix().getEntry(i, j), eps);
                // Validation done on relative error because the elements of the matrices
                // are big.
                Assert.assertEquals(0.0, (sym3.getEntry(i, j) - sym2.getEntry(i, j)) / sym3.getEntry(i, j), eps_rel);
            }
        }

        // Simple propagation test of an orbit of non keplerian type
        // to cover the method propagate(Orbit, AbsoluteDate)

        final OrbitCovariance cartesianOrbit = new OrbitCovariance(orbit, cart, cov);
        final double t = this.dt[1];
        final AbsoluteDate datePropag = date.shiftedBy(t);

        // State Transition Matrix
        RealMatrix transitionMatrix = orbit.getKeplerianTransitionMatrix(datePropag.durationFrom(date));

        // Perform conversion type
        final RealMatrix jac = orbit.getJacobian(kep, cart);
        final RealMatrix jacInv = orbit.shiftedBy(t).getJacobian(cart, kep);
        transitionMatrix = jacInv.multiply(transitionMatrix.multiply(jac));

        // Expected covariance matrix
        final RealMatrix covMatrixExpected = transitionMatrix.multiply(cov.multiply(transitionMatrix.transpose()));

        // Actual covariance matrix
        final ArrayRowSymmetricMatrix covMatrixActual = cartesianOrbit.propagate(orbit, datePropag).getCovarianceMatrix();

        // Comparisons
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {

                if (i == j) {
                    // Big terms on the diagonal, so that relative error is prefered
                    Assert.assertEquals(0.0, (covMatrixExpected.getEntry(i, i) - covMatrixActual.getEntry(i, i)) /
                        covMatrixExpected.getEntry(i, i), eps_rel);
                } else {
                    // Absolute error on out diagonal values is lower than 1.0-10 except
                    // for some terms, but it can be verified that the computation using
                    // method propagate() offers more precision on digits than the simple
                    // computation involving product of matrices in order to validate.
                    // Therefore, validation can't be done with error lower than 1.0E-7.
                    Assert.assertEquals(covMatrixExpected.getEntry(i, j), covMatrixActual.getEntry(i, j), 1.0E-7);
                }
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVARIANCE_INTERPOLATION}
     * 
     * @testedMethod {@link CovarianceInterpolation#constructor}
     * 
     * @description get PatriusMessages.DATE_OUTSIDE_INTERVAL from the constructor because the interpolation
     *              date is inferior to the lower bound of the time interval.
     * 
     * @testPassCriteria PatriusMessages.DATE_OUTSIDE_INTERVAL
     * 
     * @referenceVersion 2.3
     * @nonRegressionVersion 2.3
     * @throws PatriusException
     *         if something goes wrong with SpacecraftState
     */
    @Test(expected = PatriusException.class)
    public void wrongInterpolationDateInferior() throws PatriusException {

        final CovarianceInterpolation interp =
            new CovarianceInterpolation(this.t0, this.covarianceMatrixTest0.getData(), this.t1,
                this.covarianceMatrixTest1.getData(), 0,
                this.orbit, this.mu);
        // exception should occur !
        interp.interpolate(this.t0.shiftedBy(-100.));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVARIANCE_INTERPOLATION}
     * 
     * @testedMethod {@link CovarianceInterpolation#constructor}
     * 
     * @description get PatriusMessages.DATE_OUTSIDE_INTERVAL from the constructor because the interpolation
     *              date is superior to the upper bound of the time interval.
     * 
     * @testPassCriteria PatriusMessages.DATE_OUTSIDE_INTERVAL
     * 
     * @referenceVersion 2.3
     * @nonRegressionVersion 2.3
     * @throws PatriusException
     *         if something goes wrong with SpacecraftState
     */
    @Test(expected = PatriusException.class)
    public void wrongInterpolationDateSuperior() throws PatriusException {

        final CovarianceInterpolation interp =
            new CovarianceInterpolation(this.t0, this.covarianceMatrixTest0, this.t1,
                this.covarianceMatrixTest1, 0,
                this.orbit, this.mu);
        // exception should occur !
        interp.interpolate(this.t1.shiftedBy(100.));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVARIANCE_INTERPOLATION}
     * 
     * @testedMethod {@link CovarianceInterpolation#constructor}
     * 
     * @description get PatriusMessages.EMPTY_INTERPOLATION_SAMPLE from the constructor because
     *              the upper bound of the time interval is strictly inferior to the lower bound
     * 
     * @testPassCriteria PatriusMessages.EMPTY_INTERPOLATION_SAMPLE
     * 
     * @referenceVersion 2.3
     * @nonRegressionVersion 2.3
     * @throws PatriusException
     *         if something goes wrong with SpacecraftState
     */
    @Test(expected = PatriusException.class)
    public void wrongInterpolationInterval() throws PatriusException {

        // exception should occur !
        new CovarianceInterpolation(this.t1, this.covarianceMatrixTest1, this.t0, this.covarianceMatrixTest0, 0,
            this.orbit, this.mu);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#COVARIANCE_INTERPOLATION}
     * 
     * @testedMethod {@link CovarianceInterpolation#constructor}
     * 
     * @description tests that no interpolation is done if the requested date is the lower
     *              (respectively upper) boundary of the interpolation interval
     * 
     * @testPassCriteria the interpolated matrix should be the same than the lower
     *                   (respectively upper) bound
     * 
     * @referenceVersion 2.3
     * @nonRegressionVersion 2.3.1
     * @throws PatriusException
     *         if something goes wrong with SpacecraftState
     */
    @Test
    public void testNoInterpolationBecauseBoundaries() throws PatriusException {

        // lower bound
        final CovarianceInterpolation interpolation =
            new CovarianceInterpolation(this.t0, this.covarianceMatrixTest0, this.t1,
                this.covarianceMatrixTest1, 0,
                this.orbit, this.mu);

        double[][] matrix = interpolation.interpolateArray(this.t0);
        Assert.assertArrayEquals(matrix, this.covarianceMatrixTest0.getData(false));

        // upper bound
        matrix = interpolation.interpolateArray(this.t1);
        Assert.assertArrayEquals(matrix, this.covarianceMatrixTest1.getData(false));
    }

    /**
     * @throws PatriusException
     *         if something goes wrong with SpacecraftState
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#COVARIANCE_INTERPOLATION}
     * 
     * @testedMethod {@link CovarianceInterpolation#constructor}
     * 
     * @description get PatriusMessages.OUT_OF_RANGE_POLYNOMIAL_ORDER from the constructor
     * 
     * @testPassCriteria PatriusMessages.OUT_OF_RANGE_POLYNOMIAL_ORDER
     * 
     * @referenceVersion 2.3
     * @nonRegressionVersion 2.3
     */
    @Test(expected = PatriusException.class)
    public void negativePolynomialOrderOfInterpolationDate() throws PatriusException {

        // exception should occur !
        new CovarianceInterpolation(this.t0, this.covarianceMatrixTest0, this.t1, this.covarianceMatrixTest1, -1,
            this.orbit, this.mu);
    }

    /**
     * @throws PatriusException
     *         if something goes wrong with SpacecraftState
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#COVARIANCE_INTERPOLATION}
     * 
     * @testedMethod {@link CovarianceInterpolation#constructor}
     * 
     * @description get PatriusMessages.OUT_OF_RANGE_POLYNOMIAL_ORDER from the constructor
     * 
     * @testPassCriteria PatriusMessages.OUT_OF_RANGE_POLYNOMIAL_ORDER
     * 
     * @referenceVersion 2.3
     * @nonRegressionVersion 2.3
     */
    @Test(expected = PatriusException.class)
    public void wrongPolynomialOrderOfInterpolationDate() throws PatriusException {
        // exception should occur !
        new CovarianceInterpolation(this.t0, this.covarianceMatrixTest0, this.t1, this.covarianceMatrixTest1, 3,
            this.orbit, this.mu);
    }

    /**
     * Utility class to store data from a txt file in matrices
     */
    private class MatrixFileReader {
        private final String filePath;

        private double[][] dataTransposed;
        private double[][] data;

        /**
         * Renvoie la matrice contenue dans le fichier
         * data[0] contient la premiere ligne du fichier
         * data[1] contient la deuxieme ligne du fichier
         * etc...
         * 
         * @return data
         */
        public double[][] getData() {
            return this.data;
        }

        /**
         * Lecture d'un fichier texte contenant une matrice
         * (Commentaire possibles dans le fichier pour les lignes commence par #)
         * 
         * @param filePath
         * @throws IOException
         */
        public MatrixFileReader(final String filePath) throws IOException {
            super();
            this.filePath = filePath;
            this.parseFile();
        }

        private void parseFile() throws IOException {
            final BufferedReader reader = new BufferedReader(new FileReader(this.filePath));
            String line = null;
            final List<String> items = new ArrayList<>();
            StringTokenizer splitter;
            while ((line = reader.readLine()) != null) {
                // Les lignes qui démarrent avec # ou qui ne contiennent rien ou que des espaces
                // sont ignorées.
                if (!line.startsWith("#") && !line.trim().isEmpty()) {
                    items.add(line);
                }
            }

            final int nbLig = items.size();
            int nbCol = 0;

            if (nbLig == 0) {
                this.dataTransposed = new double[nbCol][nbLig];
            }
            else {

                final String firstLigne = items.get(0);
                splitter = new StringTokenizer(firstLigne, " ");

                nbCol = splitter.countTokens();

                this.dataTransposed = new double[nbCol][nbLig];
                int counter = 0;
                for (final String item : items) {
                    splitter = new StringTokenizer(item, " ");
                    for (int i = 0; i < nbCol; i++) {
                        // Attention: Double.parseDouble ne fonctionne pas si
                        // les nombres sont écrits avec une virgule comme séparateur !!
                        this.dataTransposed[i][counter] = Double.parseDouble((String) splitter.nextElement());
                    }
                    counter++;
                }
            }

            this.data = new double[nbLig][nbCol];
            for (int i = 0; i < nbLig; i++) {
                for (int j = 0; j < nbCol; j++) {
                    this.data[i][j] = this.dataTransposed[j][i];
                }
            }

            reader.close();
        }

    }
}
