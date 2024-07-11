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
 * VERSION:4.9:FA:FA-3144:10/05/2022:[PATRIUS] Classe TempDirectory en double 
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:91:26/07/2013: new test class
 * VERSION::DM:131:28/10/2013:Changed ConstanSolarActivity class
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::FA:359:31/03/2015: Proper management of reentry case with step size control
 * VERSION::FA:391:13/04/2015: system to retrieve STELA dE/dt
 * VERSION::DM:605:30/09/2016:gathered Meeus models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.SortedMap;
import java.util.TreeMap;

import junit.framework.Assert;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ClassicalMSISE2000SolarData;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.ode.nonstiff.RungeKutta6Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.PerigeeAltitudeDetector;
import fr.cnes.sirius.patrius.stela.bodies.MeeusMoonStela;
import fr.cnes.sirius.patrius.stela.forces.atmospheres.MSIS00Adapter;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaAeroModel;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaAtmosphericDrag;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaCd;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaTesseralAttraction;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaThirdBodyAttraction;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaZonalAttraction;
import fr.cnes.sirius.patrius.stela.forces.radiation.StelaSRPSquaring;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCTAILoader;
import fr.cnes.sirius.patrius.tools.validationTool.TemporaryDirectory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Test for transition matric computation
 * 
 * @author Rami Houdroge
 * 
 */
public class GenericTransitionMatrixTest {

    /** results directory */
    public static final String GENERIC_DIRECTORY = TemporaryDirectory.getTemporaryDirectory("pdb.validate.results",
        "stelaPDVValidationFiles");
    /** Output directory for the results. */
    protected static final String GENERIC_RESULTS_DIRECTORY = GENERIC_DIRECTORY + File.separator
        + "stelaPDVValidationFiles";

    /** Stela orbit */
    double aS;
    /** Stela orbit */
    double exS;
    /** Stela orbit */
    double eyS;
    /** Stela orbit */
    double ixS;
    /** Stela orbit */
    double iyS;
    /** Stela orbit */
    double lmS;

    /** Internal parameters */
    Frame cirf;
    /** Internal parameters */
    Assembly satellite;
    /** Internal parameters */
    Orbit orbit;
    /** Internal parameters */
    AbsoluteDate startDate;
    /** Internal parameters */
    AbsoluteDate endDate;
    /** Internal parameters */
    SpacecraftState initialState;
    /** Internal parameters */
    double[] solarActivity;
    /** Internal parameters */
    private StelaGTOPropagator propagator;
    /** Internal parameters */
    private MSIS00Adapter atmosphere;
    /** Internal parameters */
    private StelaEquinoctialOrbit stelaOrbit;
    /** Internal parameters */
    private TreeMap<AbsoluteDate, double[]> parDer;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemeris;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisAp;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisAm;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisLp;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisLm;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisExp;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisExm;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisEyp;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisEym;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisIxp;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisIxm;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisIyp;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisIym;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisK1p;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisK1m;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisK2p;
    /** Ephemeris */
    private static TreeMap<AbsoluteDate, SpacecraftState> ephemerisK2m;
    /** Internal parameters - call Assert for checks */
    private final boolean asserts = true;
    /** Internal parameters */
    private double[][] expectedResult;

    /** Parameters from specific test case */
    private final double[] delta;
    /** Parameters from specific test case */
    private final int jjfin;
    /** Parameters from specific test case */
    private final int jjdeb;
    /** Parameters from specific test case */
    private final double a;
    /** Parameters from specific test case */
    private final double eccentricity;
    /** Parameters from specific test case */
    private final double i;
    /** Parameters from specific test case */
    private final double pom;
    /** Parameters from specific test case */
    private final double gom;
    /** Parameters from specific test case */
    private final double M;
    /** Parameters from specific test case */
    private final double mu;
    /** Parameters from specific test case */
    private final double[][] tolerance;
    /** Parameters from specific test case */
    private final double dragArea;
    /** Parameters from specific test case */
    private final double refArea;
    /** Parameters from specific test case */
    private final boolean isOsculating;
    /** Parameters from specific test case */
    private final double refCoef;
    /** Parameters from specific test case */
    private final double mass;
    /** Parameters from specific test case */
    private final StelaCd Cx;
    /** Parameters from specific test case */
    private final double dt;
    /** Parameters from specific test case */
    private final double reentryAltitude;
    /** Parameters from specific test case */
    private final boolean dragFlag;
    /** Parameters from specific test case */
    private final boolean tesseralFlag;
    /** Parameters from specific test case */
    private ComparisonTypes[][] comparisonType;
    /** Parameters from specific test case */
    private double toleranceSingle;
    /** Parameters from specific test case */
    private final String testName;

    /**
     * Create a new test case
     * 
     * @param string
     *        test name
     * @param delta
     *        deltas for finite differences
     * @param jjfin
     *        start date
     * @param jjdeb
     *        end date
     * @param a
     *        semi major axis
     * @param eccentricity
     *        eccentricity
     * @param i
     *        Inclination
     * @param pom
     *        argument of perigee
     * @param gom
     *        right ascension of ascending node
     * @param m
     *        mean latitude argument
     * @param mu
     *        standard gravitational parameter
     * @param tolerance
     *        error tolerances
     * @param dragArea2
     *        drag area
     * @param refArea2
     *        srp reflection area
     * @param isOsculating
     *        are orbital parameters osculating
     * @param refCoef
     *        reflection coefficient Cr
     * @param mass
     *        mass of spacecraft
     * @param cx
     *        drag coefficient
     * @param dt
     *        RK6 integration step
     * @param reentryAltitude
     *        reentry altitude
     * @param drag
     *        drag flag
     * @param comparisonType2
     *        comparison types (absolute or relative)
     * @throws PatriusException
     *         if fails
     * @throws IOException
     *         if fails
     * @throws ParseException
     *         if fails
     */
    public GenericTransitionMatrixTest(final String string, final double[] delta, final int jjfin, final int jjdeb,
        final double a,
        final double eccentricity, final double i,
        final double pom, final double gom, final double m, final double mu, final double[][] tolerance,
        final double dragArea2, final double refArea2,
        final boolean isOsculating, final double refCoef, final double mass, final double cx, final double dt,
        final double reentryAltitude,
        final boolean drag, final boolean tess, final ComparisonTypes[][] comparisonType2) throws PatriusException,
        IOException,
        ParseException {

        if (!new File(GENERIC_RESULTS_DIRECTORY).isDirectory()) {
            new File(GENERIC_RESULTS_DIRECTORY).mkdirs();
        }

        // Store test data
        this.delta = delta;
        this.jjfin = jjfin;
        this.jjdeb = jjdeb;
        this.a = a;
        this.eccentricity = eccentricity;
        this.i = i;
        this.pom = pom;
        this.gom = gom;
        this.M = m;
        this.mu = mu;
        this.tolerance = tolerance;
        this.dragArea = dragArea2;
        this.refArea = refArea2;
        this.isOsculating = isOsculating;
        this.refCoef = refCoef;
        this.mass = mass;
        this.Cx = new StelaCd(cx);
        this.dt = dt;
        this.reentryAltitude = reentryAltitude;
        this.dragFlag = drag;
        this.tesseralFlag = tess;
        this.comparisonType = comparisonType2;
        this.expectedResult = null;
        this.toleranceSingle = -1;
        this.testName = string;

    }

    /**
     * Create a new test case
     * 
     * @param string
     *        test name
     * @param delta
     *        deltas for finite differences
     * @param jjfin
     *        start date
     * @param jjdeb
     *        end date
     * @param a
     *        semi major axis
     * @param eccentricity
     *        eccentricity
     * @param i
     *        Inclination
     * @param pom
     *        argument of perigee
     * @param gom
     *        right ascension of ascending node
     * @param m
     *        mean latitude argument
     * @param mu
     *        standard gravitational parameter
     * @param tolerance
     *        error tolerances
     * @param dragArea2
     *        drag area
     * @param refArea2
     *        srp reflection area
     * @param isOsculating
     *        are orbital parameters osculating
     * @param refCoef
     *        reflection coefficient Cr
     * @param mass
     *        mass of spacecraft
     * @param cx
     *        drag coefficient
     * @param dt
     *        RK6 integration step
     * @param reentryAltitude
     *        reentry altitude
     * @param drag
     *        drag flag
     * @param expected
     *        expected result
     * @throws PatriusException
     *         if fails
     * @throws IOException
     *         if fails
     * @throws ParseException
     *         if fails
     */
    public GenericTransitionMatrixTest(final String string, final double[] delta, final int jjfin, final int jjdeb,
        final double a,
        final double eccentricity, final double i,
        final double pom, final double gom, final double m, final double mu, final double tolerance,
        final double dragArea2, final double refArea2,
        final boolean isOsculating, final double refCoef, final double mass, final double cx, final double dt,
        final double reentryAltitude,
        final boolean drag, final double[][] expected)
        throws PatriusException, IOException,
        ParseException {
        this(string, delta, jjfin, jjdeb, a, eccentricity, i, pom, gom, m, mu, null, dragArea2, refArea2,
            isOsculating,
            refCoef, mass, cx, dt, reentryAltitude, drag, false, null);
        this.expectedResult = expected;
        this.toleranceSingle = tolerance;
    }

    /**
     * Run tests
     * 
     * @throws PatriusException
     *         if fails
     * @throws IOException
     *         if fails
     * @throws ParseException
     *         if fails
     */
    public void run() throws PatriusException, IOException, ParseException {
        // Setup test
        this.setup();

        // Run test
        this.test();
    }

    /**
     * Set up test case
     * 
     * @throws PatriusException
     *         if fails
     * @throws IOException
     *         if fails
     * @throws ParseException
     *         if fails
     */
    public void setup() throws PatriusException, IOException, ParseException {
        Utils.setDataRoot("stela");

        FramesFactory.setConfiguration(Utils.getIERS2010Configuration());
        this.cirf = FramesFactory.getCIRF();

        // Stela 35 s shift (no UTC-TAI leap seconds in STELA)
        this.startDate = new AbsoluteDate(AbsoluteDate.FIFTIES_EPOCH_TAI, this.jjdeb * Constants.JULIAN_DAY + 35);
        this.endDate = new AbsoluteDate(AbsoluteDate.FIFTIES_EPOCH_TAI, this.jjfin * Constants.JULIAN_DAY + 35);

        this.orbit =
            new KeplerianOrbit(this.a, this.eccentricity, this.i, this.pom, this.gom, this.M, PositionAngle.MEAN,
                this.cirf, this.startDate, this.mu);
        this.stelaOrbit = new StelaEquinoctialOrbit(this.orbit);

        this.aS = this.stelaOrbit.getA();
        this.exS = this.stelaOrbit.getEquinoctialEx();
        this.eyS = this.stelaOrbit.getEquinoctialEy();
        this.ixS = this.stelaOrbit.getIx();
        this.iyS = this.stelaOrbit.getIy();
        this.lmS = this.stelaOrbit.getLM();

        this.parDer = new TreeMap<AbsoluteDate, double[]>();
        ephemeris = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisAm = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisAp = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisLm = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisLp = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisExp = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisExm = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisEyp = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisEym = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisIxp = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisIxm = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisIyp = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisIym = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisK1p = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisK1m = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisK2p = new TreeMap<AbsoluteDate, SpacecraftState>();
        ephemerisK2m = new TreeMap<AbsoluteDate, SpacecraftState>();

    }

    /**
     * Run test
     * Creates propagator and computes expected result as well as actual transition matrix
     * 
     * @throws PatriusException
     *         if fails
     * @throws IOException
     *         if fails
     * @throws ParseException
     *         if fails
     */
    public void test() throws PatriusException, IOException, ParseException {

        // get computed derivatives
        this.configProp(FD.ORIG);
        final double[] pdResult = this.parDer.get(this.endDate);
        final double[][] mat1 = new double[6][8];
        JavaMathAdapter.vectorToMatrix(pdResult, mat1);

        // Finite difference validation
        if (this.expectedResult == null) {

            for (final FD value : FD.values()) {
                this.configProp(value);
                this.printParams(value);
            }

            // Compute expected transition matrix
            final double da0 = 2 * this.delta[0];
            final double dksi0 = 2 * this.delta[1];
            final double dex0 = 2 * this.delta[2];
            final double dey0 = 2 * this.delta[3];
            final double dix0 = 2 * this.delta[4];
            final double diy0 = 2 * this.delta[5];
            final double dK10 = 2 * this.delta[6] / this.dragArea;
            final double dK20 = 2 * this.delta[7] / this.refArea;

            // compute derivatives
            final double[] aDv =
                this.computeDvs(this.getParams(this.endDate, FD.AP), this.getParams(this.endDate, FD.AM), da0);
            final double[] lmDv =
                this.computeDvs(this.getParams(this.endDate, FD.LMP), this.getParams(this.endDate, FD.LMM), dksi0);
            final double[] exDv =
                this.computeDvs(this.getParams(this.endDate, FD.EXP), this.getParams(this.endDate, FD.EXM), dex0);
            final double[] eyDv =
                this.computeDvs(this.getParams(this.endDate, FD.EYP), this.getParams(this.endDate, FD.EYM), dey0);
            final double[] ixDv =
                this.computeDvs(this.getParams(this.endDate, FD.IXP), this.getParams(this.endDate, FD.IXM), dix0);
            final double[] iyDv =
                this.computeDvs(this.getParams(this.endDate, FD.IYP), this.getParams(this.endDate, FD.IYM), diy0);
            final double[] k1Dv =
                this.computeDvs(this.getParams(this.endDate, FD.SMP), this.getParams(this.endDate, FD.SMM), dK10);
            final double[] k2Dv =
                this.computeDvs(this.getParams(this.endDate, FD.SRP), this.getParams(this.endDate, FD.SRM), dK20);

            // store in a matrix, with columns containing derivatives of orbital parameters wrt to user parameters
            final double[][] dvRef = new double[6][8];
            for (int ii = 0; ii < 6; ii++) {
                dvRef[ii][0] = aDv[ii];
                dvRef[ii][1] = lmDv[ii];
                dvRef[ii][2] = exDv[ii];
                dvRef[ii][3] = eyDv[ii];
                dvRef[ii][4] = ixDv[ii];
                dvRef[ii][5] = iyDv[ii];
                dvRef[ii][6] = k1Dv[ii];
                dvRef[ii][7] = k2Dv[ii];
            }

            this.expectedResult = dvRef;

        } else {

            // Validation references provided
            this.configProp(FD.ORIG);
            this.printParams(FD.ORIG);

            this.comparisonType = new ComparisonTypes[this.expectedResult.length][this.expectedResult[0].length];
            for (int i = 0; i < this.comparisonType.length; i++) {
                for (int j = 0; j < this.comparisonType[i].length; j++) {
                    this.comparisonType[i][j] = ComparisonTypes.RELATIVE;
                }
            }
        }

        // run checks
        this.checkMatricesAreEqual(this.expectedResult, mat1, this.tolerance, this.comparisonType);
    }

    private void printParams(final FD value) throws IOException, PatriusException, ParseException {

        final String c = "; ";
        final String nl = "\n";

        if (value != FD.ORIG) {

            final String pathname = GENERIC_RESULTS_DIRECTORY + File.separator + this.testName.replace(" ", "_") + "_"
                + value + ".params";
            final File file = new File(pathname);
            if (!file.exists()) {
                file.createNewFile();
            }
            if (!file.canWrite()) {
                System.out.println(pathname);
                throw new RuntimeException();
            }
            final BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));

            br.write("# " + this.testName + " -- " + value + " orbital parameters" + nl);
            br.write("epoch; offset; a; ex; ey; ix; iy\n");

            // final String[] labels = { "aa", "lm", "ex", "ey", "ix", "iy", "k1", "k2" };
            //
            // String type;
            // String param;
            // final String[] types = { "ABS", "REL" };
            // String lStatus;

            // String compiled;
            // ComparisonTypes ctype;
            //
            // System.out.println(testName);
            // br.write(testName + nl);
            double[] elements;
            StringBuffer bf;

            for (final AbsoluteDate date : ephemerisAp.keySet()) {
                elements = this.getParams(date, value);

                bf = new StringBuffer();
                bf.append(date.getEpoch());
                bf.append(c);
                bf.append(date.getOffset());
                bf.append(c);
                for (int i = 0; i < 6; i++) {
                    bf.append(elements[i]);
                    bf.append(c);
                }
                bf.append(nl);

                br.write(bf.toString());
            }

        }
    }

    /**
     * Check equality
     * 
     * @param dvRef
     *        expected
     * @param mat1
     *        actual
     * @param tolerance2
     *        tolerances
     * @param comparisonType2
     *        comparison type
     * @throws IOException
     *         if fails
     */
    private void checkMatricesAreEqual(final double[][] dvRef, final double[][] mat1, final double[][] tolerance2,
                                       final ComparisonTypes[][] comparisonType2) throws IOException {

        // make sure the matrices lengths are the same
        this.checkLengths(dvRef, mat1, tolerance2, comparisonType2);

        double val;
        double tol;
        ComparisonTypes ctype;
        boolean status = true;

        final String[] labels = { "aa", "lm", "ex", "ey", "ix", "iy", "k1", "k2" };
        final String nl = "\n";
        String type;
        final String c = "; ";
        final String pathname = GENERIC_RESULTS_DIRECTORY + File.separator + this.testName.replace(" ", "_") + "_"
            + "DV.res";
        final File file = new File(pathname);
        if (!file.exists()) {
            file.createNewFile();
        }
        if (!file.canWrite()) {
            System.out.println(pathname);
            throw new RuntimeException();
        }
        final BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));

        br.append("# Partial derivatives for " + this.testName + "  -   Extrapolation time "
            + (this.jjfin - this.jjdeb) + " days"
            + nl);
        br.append("dE" + c + "dE0" + c + "val" + c + "ref" + c + "type" + c + "dev" + c + "tol" + c + "status" + nl);

        for (int ii = 0; ii < dvRef.length; ii++) {
            for (int jj = 0; jj < dvRef[0].length; jj++) {
                if (tolerance2 != null) {
                    tol = tolerance2[ii][jj];
                } else {
                    tol = this.toleranceSingle;
                }

                ctype = comparisonType2[ii][jj];

                switch (ctype) {
                    case ABSOLUTE:

                        val = MathLib.abs(dvRef[ii][jj] - mat1[ii][jj]);
                        status &= val < tol;
                        type = "ABS";

                        break;
                    case RELATIVE:

                        if (MathLib.abs(dvRef[ii][jj]) < Precision.DOUBLE_COMPARISON_EPSILON
                            || MathLib.abs(mat1[ii][jj]) < Precision.DOUBLE_COMPARISON_EPSILON) {

                            val = MathLib.abs(dvRef[ii][jj] - mat1[ii][jj]);
                            status &= val < tol;
                            type = "ABS";

                        } else {

                            val = MathLib.abs((dvRef[ii][jj] - mat1[ii][jj]) / dvRef[ii][jj]);
                            status &= val < tol;
                            type = "REL";
                        }
                        break;
                    default:
                        throw new RuntimeException();
                }

                br.append(labels[ii]);
                br.append(c);
                br.append(labels[jj]);
                br.append(c);
                br.append(Double.toString(mat1[ii][jj]));
                br.append(c);
                br.append(Double.toString(dvRef[ii][jj]));
                br.append(c);
                br.append(type);
                br.append(c);
                br.append(Double.toString(val));
                br.append(c);
                br.append(Double.toString(tol));
                br.append(c);
                br.append(val < tol ? "OK" : "NOK");
                br.append(c);
                br.append(nl);

            }
        }

        br.close();

        if (this.asserts) {
            Assert.assertTrue(status);
        }
    }

    /**
     * Check lengths
     * 
     * @param dvRef
     *        expected
     * @param mat1
     *        actual
     * @param tolerance2
     *        tolerances
     * @param comparisonType2
     *        comparison type
     */
    private void checkLengths(final double[][] dvRef, final double[][] mat1, final double[][] tolerance2,
                              final ComparisonTypes[][] comparisonType2) {
        final int rows = dvRef.length;
        final int cols = dvRef[0].length;

        if (this.asserts) {
            Assert.assertEquals(rows, mat1.length);
            Assert.assertEquals(cols, mat1[0].length);

            if (tolerance2 != null) {
                Assert.assertEquals(rows, tolerance2.length);
                Assert.assertEquals(cols, tolerance2[0].length);

                Assert.assertEquals(rows, comparisonType2.length);
                Assert.assertEquals(cols, comparisonType2[0].length);
            }
        }
    }

    /**
     * Get finite differences
     * 
     * @param opp
     *        orbital parameters with +delta
     * @param opm
     *        orbital parameters with -delta
     * @param d
     *        delta
     * @return derivatives computed with finite differences
     */
    private double[] computeDvs(final double[] opp, final double[] opm, final double d) {
        final double[] result = new double[6];
        for (int ii = 0; ii < 6; ii++) {
            result[ii] = (opp[ii] - opm[ii]) / d;
        }
        return result.clone();
    }

    /** Comparison types */
    public enum ComparisonTypes {
        /** Absolute */
        ABSOLUTE {
            @Override
            public String toString() {
                return "ABS";
            }
        },
        /** Relative */
        RELATIVE {
            @Override
            public String toString() {
                return "REL";
            }
        };

        @Override
        public abstract String toString();
    }

    /** Enum for derivatives computation */
    public enum FD {
        /** empty */
        ORIG {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemeris;
            }
        },
        /** empty */
        AP {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisAp;
            }
        },
        /** empty */
        AM {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisAm;
            }
        },
        /** empty */
        LMP {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisLp;
            }
        },
        /** empty */
        LMM {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisLm;
            }
        },
        /** empty */
        EXP {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisExp;
            }
        },
        /** empty */
        EXM {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisExm;
            }
        },
        /** empty */
        EYP {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisEyp;
            }
        },
        /** empty */
        EYM {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisEym;
            }
        },
        /** empty */
        IXP {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisIxp;
            }
        },
        /** empty */
        IXM {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisIxm;
            }
        },
        /** empty */
        IYP {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisIyp;
            }
        },
        /** empty */
        IYM {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisIym;
            }
        },
        /** empty */
        SMP {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisK1p;
            }
        },
        /** empty */
        SMM {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisK1m;
            }
        },
        /** empty */
        SRP {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisK2p;
            }
        },
        /** empty */
        SRM {
            @Override
            public TreeMap<AbsoluteDate, SpacecraftState> getEphemeris() {
                return ephemerisK2m;
            }
        };

        public abstract TreeMap<AbsoluteDate, SpacecraftState> getEphemeris();
    }

    /**
     * Propagate with assigned delta
     * 
     * @param fd
     *        parameter for which to add delta
     * @return orbital parameters in an array [a, lm, ex, ey, ix, iy]
     * @throws PatriusException
     *         if fails
     * @throws IOException
     *         if fails
     * @throws ParseException
     *         if fails
     */
    public double[] getParams(final AbsoluteDate date, final FD fd) throws PatriusException, IOException,
                                                                   ParseException {
        final SpacecraftState state = fd.getEphemeris().get(date);

        final double[] r = new double[6];
        r[0] = new StelaEquinoctialOrbit(state.getOrbit()).getA();
        r[1] = new StelaEquinoctialOrbit(state.getOrbit()).getLM();
        r[2] = new StelaEquinoctialOrbit(state.getOrbit()).getEquinoctialEx();
        r[3] = new StelaEquinoctialOrbit(state.getOrbit()).getEquinoctialEy();
        r[4] = new StelaEquinoctialOrbit(state.getOrbit()).getIx();
        r[5] = new StelaEquinoctialOrbit(state.getOrbit()).getIy();

        return r;
    }

    /**
     * Run propagation with given delta
     * 
     * @param fd
     *        parameter for which to add delta
     * @return the propagated bulletin
     * @throws PatriusException
     *         if fails
     * @throws IOException
     *         if fails
     * @throws ParseException
     *         if fails
     */
    public void configProp(final FD fd) throws PatriusException, IOException, ParseException {

        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003ConfigurationWOEOP(false));

        // Propagator

        this.orbit =
            new StelaEquinoctialOrbit(this.aS, this.exS, this.eyS, this.ixS, this.iyS, this.lmS, this.cirf,
                this.startDate, this.mu, this.isOsculating);

        // drag
        final CelestialBody sun = new MeeusSun(MODEL.STELA);
        this.solarActivity = new double[] { 140, 15, 15, 15, 15, 15, 15, 15, 15 };
        this.dragSetUp(sun, this.solarActivity);

        StelaAeroModel sp = new StelaAeroModel(this.mass, this.Cx, this.dragArea, this.atmosphere, 50);
        StelaAtmosphericDrag atmosphericDrag = new StelaAtmosphericDrag(sp, this.atmosphere, 33, 6378000, 2500000,
            2);

        StelaSRPSquaring srpStela = new StelaSRPSquaring(this.mass, this.refArea, this.refCoef, 11, sun);

        switch (fd) {
            case AP:
                this.orbit =
                    new StelaEquinoctialOrbit(this.aS + this.delta[0], this.exS, this.eyS, this.ixS, this.iyS,
                        this.lmS, this.cirf, this.startDate, this.mu, this.isOsculating);
                break;
            case AM:
                this.orbit =
                    new StelaEquinoctialOrbit(this.aS - this.delta[0], this.exS, this.eyS, this.ixS, this.iyS,
                        this.lmS, this.cirf, this.startDate, this.mu, this.isOsculating);
                break;
            case LMP:
                this.orbit =
                    new StelaEquinoctialOrbit(this.aS, this.exS, this.eyS, this.ixS, this.iyS,
                        this.lmS + this.delta[1], this.cirf, this.startDate, this.mu, this.isOsculating);
                break;
            case LMM:
                this.orbit =
                    new StelaEquinoctialOrbit(this.aS, this.exS, this.eyS, this.ixS, this.iyS,
                        this.lmS - this.delta[1], this.cirf, this.startDate, this.mu, this.isOsculating);
                break;
            case EXP:
                this.orbit =
                    new StelaEquinoctialOrbit(this.aS, this.exS + this.delta[2], this.eyS, this.ixS, this.iyS,
                        this.lmS, this.cirf, this.startDate, this.mu, this.isOsculating);
                break;
            case EXM:
                this.orbit =
                    new StelaEquinoctialOrbit(this.aS, this.exS - this.delta[2], this.eyS, this.ixS, this.iyS,
                        this.lmS, this.cirf, this.startDate, this.mu, this.isOsculating);
                break;
            case EYP:
                this.orbit =
                    new StelaEquinoctialOrbit(this.aS, this.exS, this.eyS + this.delta[3], this.ixS, this.iyS,
                        this.lmS, this.cirf, this.startDate, this.mu, this.isOsculating);
                break;
            case EYM:
                this.orbit =
                    new StelaEquinoctialOrbit(this.aS, this.exS, this.eyS - this.delta[3], this.ixS, this.iyS,
                        this.lmS, this.cirf, this.startDate, this.mu, this.isOsculating);
                break;
            case IXP:
                this.orbit =
                    new StelaEquinoctialOrbit(this.aS, this.exS, this.eyS, this.ixS + this.delta[4], this.iyS,
                        this.lmS, this.cirf, this.startDate, this.mu, this.isOsculating);
                break;
            case IXM:
                this.orbit =
                    new StelaEquinoctialOrbit(this.aS, this.exS, this.eyS, this.ixS - this.delta[4], this.iyS,
                        this.lmS, this.cirf, this.startDate, this.mu, this.isOsculating);
                break;
            case IYP:
                this.orbit =
                    new StelaEquinoctialOrbit(this.aS, this.exS, this.eyS, this.ixS, this.iyS + this.delta[5],
                        this.lmS, this.cirf, this.startDate, this.mu, this.isOsculating);
                break;
            case IYM:
                this.orbit =
                    new StelaEquinoctialOrbit(this.aS, this.exS, this.eyS, this.ixS, this.iyS - this.delta[5],
                        this.lmS, this.cirf, this.startDate, this.mu, this.isOsculating);
                break;
            case SMP:

                sp = new StelaAeroModel(this.mass, this.Cx, this.dragArea + this.delta[6], this.atmosphere, 50);
                atmosphericDrag = new StelaAtmosphericDrag(sp, this.atmosphere, 33, 6378000, 2500000,
                    2);
                break;
            case SMM:

                sp = new StelaAeroModel(this.mass, this.Cx, this.dragArea - this.delta[6], this.atmosphere, 50);
                atmosphericDrag = new StelaAtmosphericDrag(sp, this.atmosphere, 33, 6378000, 2500000,
                    2);
                break;
            case SRP:

                srpStela = new StelaSRPSquaring(this.mass, this.refArea + this.delta[7], this.refCoef, 11, sun);
                break;
            case SRM:

                srpStela = new StelaSRPSquaring(this.mass, this.refArea - this.delta[7], this.refCoef, 11, sun);
                break;
            case ORIG:
                break;
            default:
                throw new RuntimeException();

        }

        this.initialState = new SpacecraftState(this.orbit);

        final RungeKutta6Integrator rk6 = new RungeKutta6Integrator(this.dt);

        final MyGRGSFormatReader reader = new MyGRGSFormatReader("stelaCoefficients", true);
        GravityFieldFactory.addPotentialCoefficientsReader(reader);
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();

        this.propagator = new StelaGTOPropagator(rk6, 5, 1);

        // Force Model

        // zonal
        final StelaZonalAttraction zonaux = new StelaZonalAttraction(provider, 7, true, 2, 7, true);
        this.propagator.addForceModel(zonaux);

        // tesseral
        final StelaTesseralAttraction tesseraux = new StelaTesseralAttraction(provider, 7, 2, this.dt, 5);
        if (this.tesseralFlag) {
            this.propagator.addForceModel(tesseraux);
        }

        // 3rd bodies

        // SUN

        final StelaThirdBodyAttraction sunPerturbation = new StelaThirdBodyAttraction(sun, 4, 2, 4);
        this.propagator.addForceModel(sunPerturbation);

        // MOON

        final CelestialBody moon = new MeeusMoonStela(6378136.46);
        final StelaThirdBodyAttraction moonPerturbation = new StelaThirdBodyAttraction(moon, 4, 2, 4);
        this.propagator.addForceModel(moonPerturbation);

        if (this.dragFlag) {
            this.propagator.addForceModel(atmosphericDrag);
        }

        // SRP
        this.propagator.addForceModel(srpStela);

        // final double[] add1 = { 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0,
        // 0,
        // 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0 };
        // parDer.put(startDate, add1);

        final PatriusFixedStepHandler handler = new PatriusFixedStepHandler(){
            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                                                                                            throws PropagationException {
                // System.out.println("calling handleStep at   " + currentState.getDate());
                double[] addState;
                try {

                    addState = currentState.getAdditionalState("PARTIAL_DERIVATIVES");

                    // // matrix reshapping
                    // double[][] mat = new double[6][8];
                    // JavaMathAdapter.vectorToMatrix(addState.clone(), mat);
                    // double[][] newMat = new double[mat.length][mat[0].length];
                    // double[][] newMat2 = new double[mat.length][mat[0].length];
                    // for (int i = 0; i < mat.length; i++) {
                    // newMat[i][0] = mat[i][0];
                    // newMat[i][1] = mat[i][2];
                    // newMat[i][2] = mat[i][3];
                    // newMat[i][3] = mat[i][4];
                    // newMat[i][4] = mat[i][5];
                    // newMat[i][5] = mat[i][1];
                    //
                    // }
                    // for (int i = 0; i < newMat[0].length; i++) {
                    // newMat2[0][i] = newMat[0][i];
                    // newMat2[1][i] = newMat[2][i];
                    // newMat2[2][i] = newMat[3][i];
                    // newMat2[3][i] = newMat[4][i];
                    // newMat2[4][i] = newMat[5][i];
                    // newMat2[5][i] = newMat[1][i];
                    //
                    // }
                    // double[] addstate2 = new double[addState.clone().length];
                    // JavaMathAdapter.matrixToVector(newMat2, addstate2, 0);

                    fd.getEphemeris().put(currentState.getDate(), currentState);
                    switch (fd) {
                        case ORIG:
                            GenericTransitionMatrixTest.this.parDer.put(currentState.getDate(), addState);
                            break;
                        default:
                            break;
                    }

                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
                // propagator.resetInitialState(currentState);

            }
        };

        this.propagator.setMasterMode(this.dt, handler);

        // reentry altitude

        final double earthRadius = 6378136.46;
        final double maxCheck = this.dt;
        final double threshold = 0.5;
        final OrbitNatureConverter orbConv = new OrbitNatureConverter(this.propagator.getForceModels());
        final PerigeeAltitudeDetector perDet = new PerigeeAltitudeDetector(maxCheck, threshold, this.reentryAltitude,
            earthRadius,
            orbConv);
        this.propagator.addEventDetector(perDet);

        // configure jacobians
        final StelaPartialDerivativesEquations eq = new StelaPartialDerivativesEquations(
            this.propagator.getGaussForceModels(), this.propagator.getLagrangeForceModels(), 1, this.propagator);
        // add the additional equations to the propagator:
        this.propagator.addAdditionalEquations(eq);
        final SpacecraftState updatedState = eq.addInitialAdditionalState(this.initialState);
        // add the additional states to the initial spacecraftstate:
        this.propagator.setInitialState(updatedState, this.mass, this.isOsculating);

        this.propagator.propagate(this.endDate);

    }

    /**
     * Set up test for drag perturbation
     * 
     * @param sun
     *        the sun
     * @param solarActivityIn
     *        the solar activity
     * @throws PatriusException
     *         an Orekit exception
     */
    public final void dragSetUp(final CelestialBody sun, final double[] solarActivityIn) throws PatriusException {

        // UTC-TAI leap seconds:
        TimeScalesFactory.clearUTCTAILoaders();
        TimeScalesFactory.addUTCTAILoader(new UTCTAILoader(){

            @Override
            public boolean stillAcceptsData() {
                return false;
            }

            @Override
            public void loadData(final InputStream input, final String name) throws IOException, ParseException,
                                                                            PatriusException {
            }

            @Override
            public SortedMap<DateComponents, Integer> loadTimeSteps() {
                final SortedMap<DateComponents, Integer> map = new TreeMap<DateComponents, Integer>();
                for (int ii = 2000; ii < 2112; ii++) {
                    // constant value:
                    map.put(new DateComponents(ii, 11, 13), 35);
                }
                return map;
            }

            @Override
            public String getSupportedNames() {
                return "No name";
            }
        });

        // earth - stela values
        final double f = 0.29825765000000E+03;
        final double ae = 6378136.46;
        // earth - stela values

        // Constant solar activity:
        final double[] aap = new double[solarActivityIn.length - 1];
        System.arraycopy(solarActivityIn, 1, aap, 0, aap.length);
        final ConstantSolarActivity solar = new ConstantSolarActivity(solarActivityIn[0], 15.);

        // Atmosphere:
        this.atmosphere = new MSIS00Adapter(new ClassicalMSISE2000SolarData(solar), ae, 1 / f, sun);

        // trying exponential implementation, as used in test class
        // final Frame mod = FramesFactory.getMOD(false);
        // atmosphere = new SimpleExponentialAtmosphere(new OneAxisEllipsoid(Utils.ae, 1.0 / 298.257222101, mod),
        // 0.0004, 42000.0, 7500.0);
    }

}
