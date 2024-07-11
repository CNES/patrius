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
 * VERSION:4.9:FA:FA-3126:10/05/2022:[PATRIUS] Imports obsoletes suite a suppression de reflexion Java dans Patrius 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:64:30/05/2013:update with renamed classes
 * VERSION::DM:131:28/10/2013:Changed ConstanSolarActivity class
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:605:30/09/2016:gathered Meeus models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.forces.drag;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ClassicalMSISE2000SolarData;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.stela.PotentialCoefficientsProviderTest;
import fr.cnes.sirius.patrius.stela.bodies.MeeusMoonStela;
import fr.cnes.sirius.patrius.stela.forces.StelaForceModel;
import fr.cnes.sirius.patrius.stela.forces.atmospheres.MSIS00Adapter;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaThirdBodyAttraction;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaZonalAttraction;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCTAILoader;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Tests for {@link StelaAtmosphericDrag}
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaAtmosphericDragTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the Stela atmospheric drag model
         * 
         * @featureDescription Computation of atmospheric drag perturbations, its short periods and partial derivatives.
         * 
         * @coveredRequirements
         */
        STELA_ATMOSPHERIC_DRAG
    }

    /**
     * The date.
     */
    private static AbsoluteDate date;

    /**
     * The atmospheric model.
     */
    private static Atmosphere atmosphere;

    /**
     * The Sun.
     */
    private static CelestialBody sun;

    /**
     * The Moon.
     */
    private static CelestialBody moon;

    /**
     * The Stela Cd value.
     */
    private static StelaCd cd;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(StelaAtmosphericDragTest.class.getSimpleName(), "STELA atmospheric drag force");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG}
     * 
     * @testedMethod {@link StelaAtmosphericDrag#computePerturbation(StelaEquinoctialOrbit, OrbitNatureConverter)}
     * @testedMethod {@link StelaAtmosphericDrag#computePartialDerivatives(StelaEquinoctialOrbit)}
     * 
     * @description test drag acceleration perturbations for GTO, a part of the orbit is within the atmosphere. It tests
     *              partial derivatives computation as well.
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output atmospheric drag perturbations, atmospheric drag partial derivatives
     * 
     * @testPassCriteria results are the same as STELA v2.6 at 1E-11 (relative tolerance), the relative error for
     *                   partial derivatives is 1E-8
     *                   (STELA test has patched densities to stay close to Scilab reference which is not the case here)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testComputePerturbationsGTO1() throws PatriusException {

        date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(22370 * 86400 + 35);

        // Orbit initialization :
        final double a = 24350500.0;
        final double LambdaEq = 1.919862177193762509E+00;
        final double eX = 3.644381018870250788E-01;
        final double eY = 6.312253086822904313E-01;
        final double iX = 9.052430460833646442E-02;
        final double iY = 5.226423163382672848E-02;

        final StelaEquinoctialOrbit pv8 = new StelaEquinoctialOrbit(a, eX, eY, iX, iY, LambdaEq,
            FramesFactory.getCIRF(), date, Constants.CNES_STELA_MU);
        final StelaAeroModel sp = new StelaAeroModel(114.907, new StelaCd(2.2), 1.07, atmosphere, 50);
        final StelaAtmosphericDrag atmosphericDrag = new StelaAtmosphericDrag(sp, atmosphere, 33, 6378000, 2500000, 1);
        atmosphericDrag.setTransMatComputationFlag(true);
        final PotentialCoefficientsProviderTest provider = new PotentialCoefficientsProviderTest();
        final StelaZonalAttraction zonal = new StelaZonalAttraction(provider, 7, false, 7, 0, false);

        final StelaThirdBodyAttraction sunForce = new StelaThirdBodyAttraction(sun, 2, 2, 2);
        final StelaThirdBodyAttraction moonForce = new StelaThirdBodyAttraction(moon, 2, 2, 2);
        final List<StelaForceModel> forces = new ArrayList<>();
        forces.add(zonal);
        forces.add(sunForce);
        forces.add(moonForce);
        final OrbitNatureConverter converter = new OrbitNatureConverter(forces);
        final double[] result = atmosphericDrag.computePerturbation(pv8, converter);

        // Comparison with expected results
        final double[] expected = { -3.125811145397272561E-02, 2.544451035366259333E-14, -1.743570866285511706E-10,
            -3.008827061883685649E-10, -2.255865921759909135E-13, -3.825044236407246195E-13 };

        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals(0.0, (expected[i] - result[i]) / expected[i], 1E-11);
        }

        Report.printMethodHeader("testComputePerturbationsGTO1", "Drag computation (GTO 1)", "STELA 2.6", 1E-11,
            ComparisonType.RELATIVE);
        Report.printToReport("Perturbation", expected, result);

        // The actual partial derivatives:
        final double[][] resultPD = atmosphericDrag.computePartialDerivatives(pv8);

        // The expected partial derivatives (Stela reference):
        final double[][] expectedPD = {
            { 1.8983037515192916E-7, 0.0, -8.567760527805676, -14.81432999044688, 0.003984223031005645,
                -0.0037411342503020847 },
            { -1.1016169234905066E-19, 0.0, 5.307190067859292E-12, 8.851891871869989E-12, -6.89953921938275E-13,
                -1.2215828479610354E-13 },
            { 1.0658041954407233E-15, 0.0, -4.781630578338703E-8, -8.185313329697108E-8, 2.0590703474640228E-11,
                -1.993257625080873E-11 },
            { 1.8400334915351054E-15, 0.0, -8.172893355083644E-8, -1.4179161175829065E-7, 3.929953148094947E-11,
                -3.6556891902512703E-11 },
            { 1.361064669306401E-18, 0.0, -6.109682157211508E-11, -1.0425921339041284E-10, -1.2077978944053934E-12,
                -2.054766881034488E-12 },
            { 2.3182228354703883E-18, 0.0, -1.0305266560326096E-10, -1.7818636561748564E-10,
                -2.022866397420742E-12, -3.67099202491137E-12 }
        };

        for (int i = 0; i < expectedPD.length; i++) {
            // compare all the components of the matrix:
            for (int j = 0; j < expectedPD[i].length; j++) {

                if (MathLib.abs(expectedPD[i][j]) < 1E-25) {
                    Assert.assertEquals(resultPD[i][j], expectedPD[i][j], 2E-8);
                } else {
                    Assert.assertEquals(0, (resultPD[i][j] - expectedPD[i][j]) / expectedPD[i][j], 2E-8);
                }
            }
        }

        Report.printMethodHeader("testComputePerturbationsGTO1", "Drag computation (GTO 1) - Partial derivatives",
            "STELA 2.6", 2E-8, ComparisonType.RELATIVE);
        Report.printToReport("Partial derivatives", expectedPD, resultPD);

        Assert.assertEquals(1, atmosphericDrag.getDragRecomputeStep(), 0);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG}
     * 
     * @testedMethod {@link StelaAtmosphericDrag#computePerturbation(StelaEquinoctialOrbit, OrbitNatureConverter)}
     * 
     * @description Test Gdrag acceleration perturbations computation for GTO, the orbit is entirely within the
     *              atmosphere bounds
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output atmospheric drag perturbations
     * 
     * @testPassCriteria references from Stela (Satlight V2 (20/08/2012), same epsilon value
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testComputePerturbationsGTO2() throws PatriusException {

        Report.printMethodHeader("testComputePerturbationsGTO2", "Drag computation (GTO 1)", "STELA 2.6", 3E-12,
            ComparisonType.RELATIVE);

        // Orbit initialization :
        date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(22370 * 86400 + 35);
        final double a = 7300000;
        final double LambdaEq = 1.919862177193762509E+00;
        final double eX = 0;
        final double eY = 0;
        final double iX = 9.052430460833646442E-02;
        final double iY = 5.226423163382672848E-02;

        final StelaEquinoctialOrbit pv8 = new StelaEquinoctialOrbit(a, eX, eY, iX, iY, LambdaEq,
            FramesFactory.getCIRF(), date, Constants.CNES_STELA_MU);
        final StelaAeroModel sp = new StelaAeroModel(114.907, new StelaCd(2.2), 1.07, atmosphere, 50);
        final StelaAtmosphericDrag atmosphericDrag = new StelaAtmosphericDrag(sp, atmosphere, 33, 6378000, 2500000, 1);

        final PotentialCoefficientsProviderTest provider = new PotentialCoefficientsProviderTest();
        final StelaZonalAttraction zonal = new StelaZonalAttraction(provider, 7, false, 7, 0, false);

        final StelaThirdBodyAttraction sunForce = new StelaThirdBodyAttraction(sun, 2, 2, 2);
        final StelaThirdBodyAttraction moonForce = new StelaThirdBodyAttraction(moon, 2, 2, 2);
        final List<StelaForceModel> forces = new ArrayList<>();
        forces.add(zonal);
        forces.add(sunForce);
        forces.add(moonForce);
        final OrbitNatureConverter converter = new OrbitNatureConverter(forces);
        final double[] result = atmosphericDrag.computePerturbation(pv8, converter);

        // Comparison with expected results
        final double[] expected = { -7.444463180130804E-6, -2.7691697726168427E-17, -2.3032460199612546E-13,
            -1.6633380353100253E-13, -1.817050846650441E-15, -1.2015467198439325E-15 };

        for (int i = 0; i < result.length; i++) {
            Assert.assertEquals(0.0, (expected[i] - result[i]) / expected[i], 3E-12);
        }
        Report.printToReport("Perturbation", expected, result);

    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG}
     * 
     * @testedMethod {@link StelaAtmosphericDrag#computeShortPeriods(StelaEquinoctialOrbit)}
     * 
     * @description Test short periods generation
     * 
     * @input StelaEquinoctialOrbit
     * 
     * @output atmospheric drag short periods perturbations
     * 
     * @testPassCriteria references from Stela (Satlight V2 (20/08/2012), same epsilon value
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testComputeShortPeriods() throws PatriusException {

        // Epsilon for this test:
        final double EPS = 1E-12;
        final DateComponents datec = new DateComponents(2011, 4, 1);
        final TimeComponents timec = new TimeComponents(0, 0, 35.);
        date = new AbsoluteDate(datec, timec, TimeScalesFactory.getTAI());

        // Orbit initialization :
        final double a = 7062500;
        final double LambdaEq = 0;
        final double eX = 0.001;
        final double eY = 0.002;
        final double iX = 0.03;
        final double iY = 0.1;
        final double mu = 398600441449820.000;

        final StelaEquinoctialOrbit pv8 = new StelaEquinoctialOrbit(a, eX, eY, iX, iY, LambdaEq,
            FramesFactory.getMOD(false), date, mu);
        final StelaAeroModel sp = new StelaAeroModel(1000, cd, 10, atmosphere, 50);
        final StelaAtmosphericDrag atmosphericDrag = new StelaAtmosphericDrag(sp, atmosphere, 33, 6378000, 2500000, 1);

        final double[] SP = atmosphericDrag.computeShortPeriods(pv8);

        for (final double element : SP) {
            Assert.assertEquals(0, element, EPS);
        }

    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ATMOSPHERIC_DRAG}
     * 
     * @testedMethod {@link StelaAtmosphericDrag#StelaAtmosphericDrag(org.orekit.forces.drag.DragSensitive, Atmosphere, int, double, double, int)}
     * @testedMethod {@link StelaAtmosphericDrag#setTransMatComputationFlag(boolean)}
     * 
     * @description coverage test
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void converageTest()
                               throws PatriusException {

        final DateComponents datec = new DateComponents(1987, 11, 16);
        final TimeComponents timec = new TimeComponents(0, 0, 35.);
        date = new AbsoluteDate(datec, timec, TimeScalesFactory.getTAI());

        // Orbit initialization :
        final double a = 24447763.4504052214;
        final double LambdaEq = 3.44084361259434912;
        final double eX = 0.67328072170512310;
        final double eY = 0.27729700918224887;
        final double iX = 0.00074176167540498;
        final double iY = 0.00033579986381688;
        final double mu = 398600441449820.000;

        new StelaEquinoctialOrbit(a, eX, eY, iX, iY, LambdaEq,
            FramesFactory.getMOD(false), date, mu);
        final StelaAeroModel sp = new StelaAeroModel(1000, cd, 10, atmosphere, 50);
        final StelaAtmosphericDrag atmosphericDrag = new StelaAtmosphericDrag(sp, atmosphere, 33, 6378000, 2500000, 1);
        atmosphericDrag.setTransMatComputationFlag(true);

        // Check result
        final double[] res = atmosphericDrag.computeAnomalyBounds(17756000.00000295768, 0.5, 36000000);
        Assert.assertEquals(0, res[0], 0);
        Assert.assertEquals(0, res[1], 0);
    }

    /**
     * Set up method.
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {

        // Next line clears data set by other tests,
        // are overriden later
        Utils.setDataRoot("regular-dataPBASE");

        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        // Cd value:
        cd = new StelaCd(2.2);
        // UTC-TAI leap seconds:
        TimeScalesFactory.clearUTCTAILoaders();
        TimeScalesFactory.addUTCTAILoader(new UTCTAILoader(){

            @Override
            public boolean stillAcceptsData() {
                return false;
            }

            @Override
            public void loadData(final InputStream input, final String name)
                throws IOException, ParseException, PatriusException {
                // nothing to do
            }

            @Override
            public SortedMap<DateComponents, Integer> loadTimeSteps() {
                final SortedMap<DateComponents, Integer> map = new TreeMap<>();
                for (int i = 1969; i < 2010; i++) {
                    // constant value:
                    map.put(new DateComponents(i, 11, 13), 35);
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

        // Celestial bodies:
        sun = new MeeusSun(MODEL.STELA);
        moon = new MeeusMoonStela(6378136.46);

        // Constant solar activity:
        final ConstantSolarActivity solarActivity = new ConstantSolarActivity(140.00, 15.);

        // Atmosphere:
        atmosphere = new MSIS00Adapter(new ClassicalMSISE2000SolarData(solarActivity), ae, 1 / f, sun);
    }
}
