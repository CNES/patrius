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
 * HISTORY
* VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
* VERSION:4.6:DM:DM-2501:27/01/2021:[PATRIUS] Nouveau seuil dans OrbitNatureConverter 
 * VERSION:4.5:DM:DM-2416:27/05/2020:Nouveau seuil dans OrbitNatureConverter 
* VERSION:4.4:FA:FA-2297:04/10/2019:[PATRIUS] probleme de convergence osculateur -> moyen de Stela
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:605:30/09/2016:gathered Meeus models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.orbits;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.forces.gravity.GravityToolbox;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.ArrayRealVector;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.stela.PotentialCoefficientsProviderTest;
import fr.cnes.sirius.patrius.stela.bodies.MeeusMoonStela;
import fr.cnes.sirius.patrius.stela.forces.StelaForceModel;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaThirdBodyAttraction;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaZonalAttraction;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for {@link OrbitNatureConverter}
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */

public class OrbitNatureConverterTest {

    /** STELA epsilon. */
    private static final double STELAEPS = 1e-15;
    /** Relative comparison epsilon for relative nature convertor using Stela GUI. */
    private static final double IHMREL = 5e-11;

    /** Provider for Zonal values */
    PotentialCoefficientsProviderTest provider = new PotentialCoefficientsProviderTest();

    /** Features description. */
    public enum features {
        /**
         * @featureTitle STELA osculating to mean orbit converter
         * 
         * @featureDescription Osculating to mean orbit converter for StelaEquinoctialOrbit only
         * 
         * @coveredRequirements
         */
        STELA_ORBIT_NATURE_CONVERTER_TO_MEAN,
        /**
         * @featureTitle STELA mean to osculating orbit converter
         * 
         * @featureDescription Mean to osculating orbit converter for StelaEquinoctialOrbit only
         * 
         * @coveredRequirements
         */
        STELA_ORBIT_NATURE_CONVERTER_TO_OSC
    }

    /**
     * Class setup.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(OrbitNatureConverterTest.class.getSimpleName(), "STELA mean <=> osculating conversion");
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(FramesConfigurationFactory.getIERS2010Configuration());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ORBIT_NATURE_CONVERTER_TO_MEAN}
     * 
     * @testedMethod {@link OrbitNatureConverter#toMean(StelaEquinoctialOrbit)}
     * 
     * @description test the toMean method, without forces.
     * 
     * @input parameters for an osculating StelaEquinoctialOrbit
     * 
     * @output a mean StelaEquinoctialOrbit
     * 
     * @testPassCriteria the result StelaEquinoctialOrbit parameters are close enough to a Stela reference result
     *                   (relative tolerance = 1E-15).
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testToMean() throws PatriusException {
        // Test with NO forces (earth potential only)

        Report.printMethodHeader("testToMean", "Osculating to mean conversion (J2 only)", "STELA", STELAEPS,
            ComparisonType.ABSOLUTE);

        // forceModel
        final StelaZonalAttraction zonal = new StelaZonalAttraction(this.provider, 2, false, 2, 0, false);
        final ArrayList<StelaForceModel> forceModel = new ArrayList<StelaForceModel>();
        forceModel.add(zonal);

        // Converter instance
        final OrbitNatureConverter obc = new OrbitNatureConverter(forceModel);

        // Osculating orbit
        final AbsoluteDate oscDate = AbsoluteDate.CCSDS_EPOCH;
        final Frame modFrame = FramesFactory.getCIRF();

        final double oscuA = 2.43505E+7 + 30302.2448425885268;
        final double oscEx = 0.3644381018870251 + 0.00114325314172558;
        final double oscEy = 0.6312253086822904 - 0.00020463300459082;
        final double oscIx = 0.09052430460833645 - 0.00001000511520404;
        final double oscIy = 0.05226423163382672 + 0.00005904201900081;
        final double oscLM = 0.8922879297441284 - 0.00102477947610415;
        // STELA earth MU
        final double oscMu = 398600441449820.000;
        final StelaEquinoctialOrbit osculatingOrbit = new StelaEquinoctialOrbit
            (oscuA, oscEx, oscEy,
                oscIx, oscIy, oscLM,
                modFrame, oscDate, oscMu);
        // Reference mean orbit parameters
        final double refMeanA = 2.43505E+7;
        final double refMeanEx = 0.3644381018870251;
        final double refMeanEy = 0.6312253086822904;
        final double refMeanIx = 0.09052430460833645;
        final double refMeanIy = 0.05226423163382672;
        final double refMeanLM = 0.8922879297441284;

        // Convert the orbit
        final StelaEquinoctialOrbit meanOrbitRez = obc.toMean(osculatingOrbit);

        Assert.assertEquals(refMeanA, meanOrbitRez.getA(), STELAEPS);
        Assert.assertEquals(refMeanEx, meanOrbitRez.getEquinoctialEx(), STELAEPS);
        Assert.assertEquals(refMeanEy, meanOrbitRez.getEquinoctialEy(), STELAEPS);
        Assert.assertEquals(refMeanIx, meanOrbitRez.getIx(), STELAEPS);
        Assert.assertEquals(refMeanIy, meanOrbitRez.getIy(), STELAEPS);
        Assert.assertEquals(refMeanLM, meanOrbitRez.getLM(), STELAEPS);
        Assert.assertEquals(0., meanOrbitRez.getDate().durationFrom(oscDate), STELAEPS);
        Assert.assertEquals(modFrame, meanOrbitRez.getFrame());

        Report.printToReport("a", refMeanA, meanOrbitRez.getA());
        Report.printToReport("Ex", refMeanEx, meanOrbitRez.getEquinoctialEx());
        Report.printToReport("Ey", refMeanEy, meanOrbitRez.getEquinoctialEy());
        Report.printToReport("Ix", refMeanIx, meanOrbitRez.getIx());
        Report.printToReport("Iy", refMeanIy, meanOrbitRez.getIy());
        Report.printToReport("LM", refMeanLM, meanOrbitRez.getLM());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ORBIT_NATURE_CONVERTER_TO_OSC}
     * 
     * @testedMethod {@link OrbitNatureConverter#toOsculating(StelaEquinoctialOrbit)}
     * 
     * @description test the toOsculating method, without forces.
     * 
     * @input parameters for a mean StelaEquinoctialOrbit
     * 
     * @output an osculating StelaEquinoctialOrbit
     * 
     * @testPassCriteria the result StelaEquinoctialOrbit parameters are close enough to a Stela reference result
     *                   (relative tolerance = 1E-15).
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testToOsculating() throws PatriusException {
        // Test with NO forces (earth potential only)

        Report.printMethodHeader("testToOsculating", "Mean to osculating conversion (J2 only)", "STELA", STELAEPS,
            ComparisonType.ABSOLUTE);

        // forceModel
        final StelaZonalAttraction zonal = new StelaZonalAttraction(this.provider, 2, false, 2, 0, false);

        final ArrayList<StelaForceModel> forceModel = new ArrayList<StelaForceModel>();
        forceModel.add(zonal);

        // Converter instance
        final OrbitNatureConverter obc = new OrbitNatureConverter(forceModel);

        // Mean orbit
        final AbsoluteDate meanDate = AbsoluteDate.CCSDS_EPOCH;
        final Frame modFrame = FramesFactory.getCIRF();

        final double meanA = 2.43505E+7;
        final double meanEx = 0.3644381018870251;
        final double meanEy = 0.6312253086822904;
        final double meanIx = 0.09052430460833645;
        final double meanIy = 0.05226423163382672;
        final double meanLM = 0.8922879297441284;
        // STELA earth MU
        final double meanMu = 398600441449820.000;
        final StelaEquinoctialOrbit meanOrbit = new StelaEquinoctialOrbit
            (meanA, meanEx, meanEy,
                meanIx, meanIy, meanLM,
                modFrame, meanDate, meanMu);
        // Reference osculating orbit parameters
        final double refOscA = 2.43505E+7 + 30302.2448425885268;
        final double refOscEx = 0.3644381018870251 + 0.00114325314172558;
        final double refOscEy = 0.6312253086822904 - 0.00020463300459082;
        final double refOscIx = 0.09052430460833645 - 0.00001000511520404;
        final double refOscIy = 0.05226423163382672 + 0.00005904201900081;
        final double refOscLM = 0.8922879297441284 - 0.00102477947610415;

        // Convert the orbit
        final StelaEquinoctialOrbit osculatingOrbitRez = obc.toOsculating(meanOrbit);

        Assert.assertEquals(refOscA, osculatingOrbitRez.getA(), STELAEPS);
        Assert.assertEquals(refOscEx, osculatingOrbitRez.getEquinoctialEx(), STELAEPS);
        Assert.assertEquals(refOscEy, osculatingOrbitRez.getEquinoctialEy(), STELAEPS);
        Assert.assertEquals(refOscIx, osculatingOrbitRez.getIx(), STELAEPS);
        Assert.assertEquals(refOscIy, osculatingOrbitRez.getIy(), STELAEPS);
        Assert.assertEquals(refOscLM, osculatingOrbitRez.getLM(), STELAEPS);
        Assert.assertEquals(0., osculatingOrbitRez.getDate().durationFrom(meanDate), STELAEPS);
        Assert.assertEquals(modFrame, osculatingOrbitRez.getFrame());

        Report.printToReport("a", refOscA, osculatingOrbitRez.getA());
        Report.printToReport("Ex", refOscEx, osculatingOrbitRez.getEquinoctialEx());
        Report.printToReport("Ey", refOscEy, osculatingOrbitRez.getEquinoctialEy());
        Report.printToReport("Ix", refOscIx, osculatingOrbitRez.getIx());
        Report.printToReport("Iy", refOscIy, osculatingOrbitRez.getIy());
        Report.printToReport("LM", refOscLM, osculatingOrbitRez.getLM());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ORBIT_NATURE_CONVERTER_TO_OSC}
     * 
     * @testedMethod {@link OrbitNatureConverter#toOsculating(StelaEquinoctialOrbit)}
     * 
     * @description test the toOsculating method, with different forces.
     * 
     * @input parameters for a mean StelaEquinoctialOrbit
     * 
     * @output an osculating StelaEquinoctialOrbit
     * 
     * @testPassCriteria the result StelaEquinoctialOrbit parameters are close enough to a Stela reference result
     *                   (relative tolerance = 1E-12, since IHM is used to generate refernece results).
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testToOsculatingSeveralForces() throws PatriusException {

        Report.printMethodHeader("testToOsculatingSeveralForces", "Mean to osculating conversion (all forces)",
            "STELA", IHMREL, ComparisonType.RELATIVE);

        // forceModel
        // Zonaux
        final StelaZonalAttraction zonal = new StelaZonalAttraction(this.provider, 2, false, 2, 0, false);
        final ArrayList<StelaForceModel> forceModel = new ArrayList<StelaForceModel>();
        forceModel.add(zonal);
        // Sun
        final CelestialBody sun = new MeeusSun(MODEL.STELA);
        final StelaThirdBodyAttraction sunPerturbation = new StelaThirdBodyAttraction(sun, 4, 2, 2);
        forceModel.add(sunPerturbation);
        // Moon
        final CelestialBody moon = new MeeusMoonStela(6378136.46);
        final StelaThirdBodyAttraction moonPerturbation = new StelaThirdBodyAttraction(moon, 4, 2, 2);
        forceModel.add(moonPerturbation);

        // Converter instance
        final OrbitNatureConverter obc = new OrbitNatureConverter(forceModel);

        // Mean orbit
        final AbsoluteDate meanDate = new AbsoluteDate(new DateComponents(2005, 06, 27),
            new TimeComponents(15, 34, 28.164), TimeScalesFactory.getTAI()).shiftedBy(35);
        final Frame modFrame = FramesFactory.getCIRF();

        final double meanA = 2.43505E+7;
        final double meanEx = 0.3644381018870251;
        final double meanEy = 0.6312253086822904;
        final double meanIx = 0.09052430460833645;
        final double meanIy = 0.05226423163382672;
        final double meanLM = 0;

        // STELA earth MU
        final double meanMu = 398600441449820.000;
        final StelaEquinoctialOrbit meanOrbit = new StelaEquinoctialOrbit
            (meanA, meanEx, meanEy,
                meanIx, meanIy, meanLM,
                modFrame, meanDate, meanMu);
        // Reference osculating orbit parameters
        final double refOscA = 24346.3087947 * 1000;
        final double refOscEx = 0.36505119311;
        final double refOscEy = 0.63081227236;
        final double refOscIx = 0.09046807948;
        final double refOscIy = 0.05234464445;
        final double refOscLM = MathLib.toRadians(359.947406588);

        // Convert the orbit
        final StelaEquinoctialOrbit osculatingOrbitRez = obc.toOsculating(meanOrbit);

        Assert.assertEquals(0, (refOscA - osculatingOrbitRez.getA()) / refOscA, IHMREL);
        Assert.assertEquals(0, (refOscEx - osculatingOrbitRez.getEquinoctialEx()) / refOscEx, IHMREL);
        Assert.assertEquals(0, (refOscEy - osculatingOrbitRez.getEquinoctialEy()) / refOscEy, IHMREL);
        Assert.assertEquals(0, (refOscIx - osculatingOrbitRez.getIx()) / refOscIx, IHMREL);
        Assert.assertEquals(0, (refOscIy - osculatingOrbitRez.getIy()) / refOscIy, IHMREL);
        Assert.assertEquals(0, (refOscLM - osculatingOrbitRez.getLM()) / refOscLM, IHMREL);
        Assert.assertEquals(0., osculatingOrbitRez.getDate().durationFrom(meanDate), STELAEPS);
        Assert.assertEquals(modFrame, osculatingOrbitRez.getFrame());

        Report.printToReport("a", refOscA, osculatingOrbitRez.getA());
        Report.printToReport("Ex", refOscEx, osculatingOrbitRez.getEquinoctialEx());
        Report.printToReport("Ey", refOscEy, osculatingOrbitRez.getEquinoctialEy());
        Report.printToReport("Ix", refOscIx, osculatingOrbitRez.getIx());
        Report.printToReport("Iy", refOscIy, osculatingOrbitRez.getIy());
        Report.printToReport("LM", refOscLM, osculatingOrbitRez.getLM());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ORBIT_NATURE_CONVERTER_TO_OSC}
     * @testedFeature {@link features#STELA_ORBIT_NATURE_CONVERTER_TO_MEAN}
     * 
     * @testedMethod {@link OrbitNatureConverter#toOsculating(StelaEquinoctialOrbit)}
     * @testedMethod {@link OrbitNatureConverter#toMean(StelaEquinoctialOrbit)}
     * 
     * @description test the toOsculating and the toMean methods, with different forces and return conversions
     * 
     * @input parameters for a mean/osculating StelaEquinoctialOrbit
     * 
     * @output a mean or osculating StelaEquinoctialOrbit
     * 
     * @testPassCriteria Way and back conversion returns the identity (relative tolerance = 1E-15)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testReturnNatureConversions() throws PatriusException {

        // forceModel
        // Zonaux
        final StelaZonalAttraction zonal = new StelaZonalAttraction(this.provider, 2, false, 2, 0, false);
        final ArrayList<StelaForceModel> forceModel = new ArrayList<StelaForceModel>();
        forceModel.add(zonal);
        // Sun
        final CelestialBody sun = new MeeusSun(MODEL.STELA);
        final StelaThirdBodyAttraction sunPerturbation = new StelaThirdBodyAttraction(sun, 4, 2, 2);
        forceModel.add(sunPerturbation);
        // Moon
        final CelestialBody moon = new MeeusMoonStela(6378136.46);
        final StelaThirdBodyAttraction moonPerturbation = new StelaThirdBodyAttraction(moon, 4, 2, 2);
        forceModel.add(moonPerturbation);

        // Converter instance
        final OrbitNatureConverter obc = new OrbitNatureConverter(forceModel);

        // Mean orbit
        final AbsoluteDate meanDate = new AbsoluteDate(new DateComponents(2011, 04, 01),
            new TimeComponents(0, 0, 35.), TimeScalesFactory.getTAI());
        final Frame modFrame = FramesFactory.getCIRF();

        final double meanA = 2.43505E+7;
        final double meanEx = 0.3644381018870251;
        final double meanEy = 0.6312253086822904;
        final double meanIx = 0.09052430460833645;
        final double meanIy = 0.05226423163382672;
        final double meanLM = 0.8922879297441284;

        // STELA earth MU
        final double meanMu = 398600441449820.000;

        final StelaEquinoctialOrbit meanOrbit = new StelaEquinoctialOrbit
            (meanA, meanEx, meanEy,
                meanIx, meanIy, meanLM,
                modFrame, meanDate, meanMu);

        // Reference osculating orbit parameters
        final double refOscA = 24380865.269;
        final double refOscEx = 0.36558118805;
        final double refOscEy = 0.631019133342;
        final double refOscIx = 0.09051402292;
        final double refOscIy = 0.05232309101;
        final double refOscLM = 0.8912661978294608156;

        final StelaEquinoctialOrbit oscOrbit = new StelaEquinoctialOrbit
            (refOscA, refOscEx, refOscEy,
                refOscIx, refOscIy, refOscLM,
                modFrame, meanDate, meanMu);

        // WAY 1 : MEAN -> OSC -> MEAN

        // Step 1 : MEAN -> OSC
        final StelaEquinoctialOrbit osculatingOrbitRez = obc.toOsculating(meanOrbit);
        // Step 2 : OSC -> MEAN
        final StelaEquinoctialOrbit meanOrbitRez = obc.toMean(osculatingOrbitRez);

        Assert.assertEquals(0, (meanA - meanOrbitRez.getA()) / refOscA, IHMREL);
        Assert.assertEquals(0, (meanEx - meanOrbitRez.getEquinoctialEx()) / refOscEx, IHMREL);
        Assert.assertEquals(0, (meanEy - meanOrbitRez.getEquinoctialEy()) / refOscEy, IHMREL);
        Assert.assertEquals(0, (meanIx - meanOrbitRez.getIx()) / refOscIx, IHMREL);
        Assert.assertEquals(0, (meanIy - meanOrbitRez.getIy()) / refOscIy, IHMREL);
        Assert.assertEquals(0, (meanLM - meanOrbitRez.getLM()) / refOscLM, IHMREL);
        Assert.assertEquals(0., meanOrbitRez.getDate().durationFrom(meanDate), STELAEPS);
        Assert.assertEquals(modFrame, meanOrbitRez.getFrame());

        // WAY 2 : OSC -> MEAN -> OSC

        // Step 1 : OSC -> MEAN
        final StelaEquinoctialOrbit meanOrbitRez2 = obc.toMean(oscOrbit);
        // Step 2 : MEAN -> OSC
        final StelaEquinoctialOrbit osculatingOrbitRez2 = obc.toOsculating(meanOrbitRez2);

        Assert.assertEquals(0, (refOscA - osculatingOrbitRez2.getA()) / refOscA, IHMREL);
        Assert.assertEquals(0, (refOscEx - osculatingOrbitRez2.getEquinoctialEx()) / refOscEx, IHMREL);
        Assert.assertEquals(0, (refOscEy - osculatingOrbitRez2.getEquinoctialEy()) / refOscEy, IHMREL);
        Assert.assertEquals(0, (refOscIx - osculatingOrbitRez2.getIx()) / refOscIx, IHMREL);
        Assert.assertEquals(0, (refOscIy - osculatingOrbitRez2.getIy()) / refOscIy, IHMREL);
        Assert.assertEquals(0, (refOscLM - osculatingOrbitRez2.getLM()) / refOscLM, IHMREL);
        Assert.assertEquals(0., osculatingOrbitRez2.getDate().durationFrom(meanDate), STELAEPS);
        Assert.assertEquals(modFrame, osculatingOrbitRez2.getFrame());

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ORBIT_NATURE_CONVERTER_TO_MEAN}
     * 
     * @testedMethod {@link OrbitNatureConverter#toMean(StelaEquinoctialOrbit)}
     * 
     * @description same as testToMean, but different frame for the input orbit
     * 
     * @input parameters for an osculating StelaEquinoctialOrbit
     * 
     * @output a mean StelaEquinoctialOrbit
     * 
     * @testPassCriteria the result StelaEquinoctialOrbit parameters are close enough to a Stela reference result.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testToMeanOtherFrame() throws PatriusException {
        // Test with NO forces (earth potential only)

        Report.printMethodHeader("testToMeanOtherFrame", "Osculating to mean conversion (J2 only, different frame)",
            "STELA", 5E-7, ComparisonType.RELATIVE);

        // forceModel
        // Zonaux
        final StelaZonalAttraction zonal = new StelaZonalAttraction(this.provider, 2, false, 2, 0, false);
        final ArrayList<StelaForceModel> forceModel = new ArrayList<StelaForceModel>();
        forceModel.add(zonal);

        // Converter instance
        final OrbitNatureConverter obc = new OrbitNatureConverter(forceModel);

        // Osculating orbit
        final AbsoluteDate oscDate = new AbsoluteDate(new DateComponents(2005, 06, 27),
            new TimeComponents(15, 34, 28.164), TimeScalesFactory.getTAI()).shiftedBy(35);
        final Frame gcrfFrame = FramesFactory.getGCRF();

        final double oscA = 24350.5 * 1000;
        final double oscEx = 0.36446947909;
        final double oscEy = 0.63120719201;
        final double oscIx = 0.09050599987;
        final double oscIy = 0.05252229221;
        final double oscLM = MathLib.toRadians(359.997151878);
        // STELA earth MU
        final double oscMu = 398600441449820.000;
        final StelaEquinoctialOrbit osculatingOrbit = new StelaEquinoctialOrbit
            (oscA, oscEx, oscEy,
                oscIx, oscIy, oscLM,
                gcrfFrame, oscDate, oscMu);

        // Reference mean orbit parameters
        final double refMeanA = 24354.6802862 * 1000;
        final double refMeanEx = 0.36382483097;
        final double refMeanEy = 0.63163932908;
        final double refMeanIx = 0.09058078152;
        final double refMeanIy = 0.05218452547;
        final double refMeanLM = MathLib.toRadians(0.0527387511);

        // Convert the orbit
        final StelaEquinoctialOrbit meanOrbitRez = obc.toMean(osculatingOrbit);

        Assert.assertEquals(0., relDel(refMeanA, meanOrbitRez.getA()), 1E-8);
        Assert.assertEquals(0., relDel(refMeanEx, meanOrbitRez.getEquinoctialEx()), 1E-8);
        Assert.assertEquals(0., relDel(refMeanEy, meanOrbitRez.getEquinoctialEy()), 1E-8);
        Assert.assertEquals(0., relDel(refMeanIx, meanOrbitRez.getIx()), 1E-8);
        Assert.assertEquals(0., relDel(refMeanIy, meanOrbitRez.getIy()), 5E-7);
        Assert.assertEquals(0., relDel(refMeanLM, meanOrbitRez.getLM()), 5E-7);
        Assert.assertEquals(0., meanOrbitRez.getDate().durationFrom(oscDate), 0);
        Assert.assertEquals(FramesFactory.getCIRF(), meanOrbitRez.getFrame());

        Report.printToReport("a", refMeanA, meanOrbitRez.getA());
        Report.printToReport("Ex", refMeanEx, meanOrbitRez.getEquinoctialEx());
        Report.printToReport("Ey", refMeanEy, meanOrbitRez.getEquinoctialEy());
        Report.printToReport("Ix", refMeanIx, meanOrbitRez.getIx());
        Report.printToReport("Iy", refMeanIy, meanOrbitRez.getIy());
        Report.printToReport("LM", refMeanLM, meanOrbitRez.getLM());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ORBIT_NATURE_CONVERTER_TO_OSC}
     * 
     * @testedMethod {@link OrbitNatureConverter#toOsculating(StelaEquinoctialOrbit)}
     * 
     * @description same as testToOsculating, but with an other frame
     * 
     * @input parameters for a mean StelaEquinoctialOrbit
     * 
     * @output an osculating StelaEquinoctialOrbit
     * 
     * @testPassCriteria the result StelaEquinoctialOrbit parameters are close enough to a Stela reference result.
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testToOsculatingOtherFrame() throws PatriusException {

        Report.printMethodHeader("testToMeanOtherFrame", "Mean to osculating conversion (J2 only, different frame)",
            "STELA", 2E-7, ComparisonType.RELATIVE);

        // forceModel
        // Zonaux
        final StelaZonalAttraction zonal = new StelaZonalAttraction(this.provider, 2, false, 2, 0, false);
        final ArrayList<StelaForceModel> forceModel = new ArrayList<StelaForceModel>();
        forceModel.add(zonal);
        // Sun
        final CelestialBody sun = new MeeusSun(MODEL.STELA);
        final StelaThirdBodyAttraction sunPerturbation = new StelaThirdBodyAttraction(sun, 4, 2, 2);
        forceModel.add(sunPerturbation);
        // Moon
        final CelestialBody moon = new MeeusMoonStela(6378136.46);
        final StelaThirdBodyAttraction moonPerturbation = new StelaThirdBodyAttraction(moon, 4, 2, 2);
        forceModel.add(moonPerturbation);

        // Converter instance
        final OrbitNatureConverter obc = new OrbitNatureConverter(forceModel);

        // Mean orbit
        final AbsoluteDate meanDate = new AbsoluteDate(new DateComponents(2005, 06, 27),
            new TimeComponents(15, 34, 28.164), TimeScalesFactory.getTAI()).shiftedBy(35);
        final Frame gcrfFrame = FramesFactory.getGCRF();

        final double meanA = 24350.5 * 1000;
        final double meanEx = 0.36446947909;
        final double meanEy = 0.63120719201;
        final double meanIx = 0.09050599987;
        final double meanIy = 0.05252229221;
        final double meanLM = MathLib.toRadians(359.997151878);
        // STELA earth MU
        final double meanMu = Constants.CNES_STELA_MU;
        final StelaEquinoctialOrbit meanOrbit = new StelaEquinoctialOrbit
            (meanA, meanEx, meanEy,
                meanIx, meanIy, meanLM,
                gcrfFrame, meanDate, meanMu);

        // Reference osculating orbit parameters (CIRF)
        final double refOscA = 24346.3087947 * 1000;
        final double refOscEx = 0.36505119311;
        final double refOscEy = 0.63081227236;
        final double refOscIx = 0.09046807948;
        final double refOscIy = 0.05234464445;
        final double refOscLM = MathLib.toRadians(359.947406588);

        // Convert the orbit
        final StelaEquinoctialOrbit osculatingOrbitRez = obc.toOsculating(meanOrbit);

        Assert.assertEquals(0., relDel(refOscA, osculatingOrbitRez.getA()), 1E-8);
        Assert.assertEquals(0., relDel(refOscEx, osculatingOrbitRez.getEquinoctialEx()), 1E-8);
        Assert.assertEquals(0., relDel(refOscEy, osculatingOrbitRez.getEquinoctialEy()), 1E-8);
        Assert.assertEquals(0., relDel(refOscIx, osculatingOrbitRez.getIx()), 1E-8);
        Assert.assertEquals(0., relDel(refOscIy, osculatingOrbitRez.getIy()), 2E-7);
        Assert.assertEquals(0., relDel(refOscLM, osculatingOrbitRez.getLM()), 1E-10);
        Assert.assertEquals(0., osculatingOrbitRez.getDate().durationFrom(meanDate), 0);
        Assert.assertEquals(FramesFactory.getCIRF(), osculatingOrbitRez.getFrame());

        Report.printToReport("a", refOscA, osculatingOrbitRez.getA());
        Report.printToReport("Ex", refOscEx, osculatingOrbitRez.getEquinoctialEx());
        Report.printToReport("Ey", refOscEy, osculatingOrbitRez.getEquinoctialEy());
        Report.printToReport("Ix", refOscIx, osculatingOrbitRez.getIx());
        Report.printToReport("Iy", refOscIy, osculatingOrbitRez.getIy());
        Report.printToReport("LM", refOscLM, osculatingOrbitRez.getLM());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_ORBIT_NATURE_CONVERTER_TO_OSC}
     * 
     * @testedMethod {@link OrbitNatureConverter#toOsculating(StelaEquinoctialOrbit)}
     * 
     * @description check convergence threshold is properly modified if some convergence problem occur. Also check an
     *              exception can be thrown or not depending on user choice
     * 
     * @input parameters for a mean StelaEquinoctialOrbit
     * 
     * @output an osculating StelaEquinoctialOrbit
     * 
     * @testPassCriteria no exception is thrown (or not) when convergence threshold is modified
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void testConvergence() throws PatriusException, IOException, ParseException {

        // Initialization
        final PatriusPotentialCoefficientsProvider provider = new PatriusPotentialCoefficientsProvider();
        final List<StelaForceModel> forces = new ArrayList<StelaForceModel>();
        forces.add(new fr.cnes.sirius.patrius.stela.forces.gravity.StelaZonalAttraction(provider, 7, true, 2, 7, true));
        forces.add(new fr.cnes.sirius.patrius.stela.forces.gravity.StelaTesseralAttraction(provider, 7, 2, 3600.0, 5));
        final OrbitNatureConverter converter = new OrbitNatureConverter(forces);
        final double mu = 3.9860043770442E14;
        final AbsoluteDate date = new AbsoluteDate(2011, 8, 15, 23, 8, 3.616, TimeScalesFactory.getTAI());
        final PVCoordinates coord = new PVCoordinates(
                new Vector3D(5094628.645677539, 739117.075720549, -4279063.301845413),
                new Vector3D(4986.069235266889, -737.3507807170975, 5831.637431462053),
                new Vector3D(-6.769553243561339, -0.9821113193028379, 5.685860317002564));
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(coord, FramesFactory.getCIRF(), date, mu);

        // First test: no convergence is expected (threshold is 1E-14)
        try {
            final StelaEquinoctialOrbit meanOrbit = converter.toMean(orbit);
            // No convergence: exception expected
            Assert.fail();
        } catch (final PatriusException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Second test: no convergence is expected but no exception should be thrown because we set a larger degraded threshold
        try {
            OrbitNatureConverter.setThresholdDegraded(1E-10);
            final StelaEquinoctialOrbit meanOrbit = converter.toMean(orbit);
            // Expected because we decided not to thrown an exception
            Assert.assertTrue(true);
        } catch (final PatriusException e) {
            // No convergence: exception not expected
            Assert.fail();
        }
        // Set back threshold
        OrbitNatureConverter.setThresholdDegraded(1E-14);

        // Change threshold
        OrbitNatureConverter.setThreshold(1E-13);

        // Second test: convergence is expected (threshold is now 1E-13)
        try {
            final StelaEquinoctialOrbit meanOrbit = converter.toMean(orbit);
            // Expected
            Assert.assertTrue(true);
        } catch (final PatriusException e) {
            // No convergence: exception expected
            Assert.fail();
        }
    }

    public static class PatriusPotentialCoefficientsProvider implements PotentialCoefficientsProvider {

        /** Gravitation constant */
        private final double mu;

        /** Earth radius */
        private final double ae;

        /** Geopotential (normalized) coefficients */
        private final BlockRealMatrix normC;
        private final BlockRealMatrix normS;

        /** Geopotential (denormalized) coefficients */
        private final BlockRealMatrix denormC;
        private final BlockRealMatrix denormS;

        public static final String STELA_REF_DATA = "/stela" + File.separator + "ft2297" + File.separator + "stela_physical_parameters.txt";

        /**
         * Constructor for a constant gravity model
         * 
         * @throws PatriusException
         * @throws ParseException
         * @throws IOException
         */
        public PatriusPotentialCoefficientsProvider() throws IOException, ParseException, PatriusException {
            // Setup gravitation constant and Earth radius
            this.mu = 398600441449820.000;
            this.ae = 6378136.46;

            this.denormC = new BlockRealMatrix(16, 16);
            this.denormS = new BlockRealMatrix(16, 16);

            fillUpCoeffs();

            // Normalize coefficients
            this.normC = new BlockRealMatrix(GravityToolbox.normalize(this.denormC.getData(false)));
            this.normS = new BlockRealMatrix(GravityToolbox.normalize(this.denormS.getData(false)));
        }

        /**
         * @see fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider#getJ(boolean,
         *      int)
         */
        @Override
        public double[] getJ(final boolean normalized, final int n) throws PatriusException {
            final double[] dJ;
            if (normalized) {
                dJ = this.normC.getColumn(0);
            } else {
                dJ = this.denormC.getColumn(0);
            }
            // Respecting convention Jn0 = -Cn0
            return new ArrayRealVector(dJ).mapMultiply(-1.0).toArray();
        }

        /**
         * @see fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider#getC(int,
         *      int, boolean)
         */
        @Override
        public double[][] getC(final int n, final int m, final boolean normalized) throws PatriusException {
            final BlockRealMatrix submatrix;
            if (normalized) {
                submatrix = this.normC.getSubMatrix(0, n, 0, m);
            } else {
                submatrix = this.denormC.getSubMatrix(0, n, 0, m);
            }
            return submatrix.getData(false);
        }

        /**
         * @see fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider#getS(int,
         *      int, boolean)
         */
        @Override
        public double[][] getS(final int n, final int m, final boolean normalized) throws PatriusException {
            final BlockRealMatrix submatrix;
            if (normalized) {
                submatrix = this.normS.getSubMatrix(0, n, 0, m);
            } else {
                submatrix = this.denormS.getSubMatrix(0, n, 0, m);
            }
            return submatrix.getData();
        }

        /**
         * @see fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider#getMu()
         */
        @Override
        public double getMu() {
            return this.mu;
        }

        /**
         * @see fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider#getAe()
         */
        @Override
        public double getAe() {
            return this.ae;
        }

        /**
         * Fill up coefficients
         * 
         * @throws IOException
         */
        private void fillUpCoeffs() throws IOException {

            // Setup reference configuration file
            final URL refFile = this.getClass().getResource(STELA_REF_DATA);

            // Read the remaining coefficients from file
            BufferedReader br = null;
            try {
                // Adding default coefficients
                // degree = 0 order = 0
                this.denormC.setEntry(0, 0, 1.0);
                this.denormS.setEntry(0, 0, 0.0);

                // degree = 1 order = 0
                this.denormC.setEntry(1, 0, 0.0);
                this.denormS.setEntry(1, 0, 0.0);

                // degree = 1 order = 1
                this.denormC.setEntry(1, 1, 0.0);
                this.denormS.setEntry(1, 1, 0.0);

                // Get a reader for the file
                br = new BufferedReader(new InputStreamReader(refFile.openStream()));

                boolean isTesseral = false;
                boolean isZonal = false;

                // Read the file
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) {
                        continue;
                    } else if (line.contains("# Zonal gravitational harmonics")) {
                        isZonal = true;
                        isTesseral = false;
                    } else if (line.contains("# n m Cnm Snm (n: degree, m: order)")) {
                        isZonal = false;
                        isTesseral = true;
                    } else if (line.charAt(0) == '#') {
                        continue;
                    } else {
                        final String[] fields = line.trim().split("\\s+");

                        // Extracting zonal coefficients (if applies)
                        if (isZonal) {
                            // Inserting zonal coefficients
                            final int deg = Integer.parseInt(fields[0].replace("J", ""));
                            // Respecting convention Jn0 = -Cn0
                            this.denormC.setEntry(deg, 0, -Double.parseDouble(fields[2]));
                            this.denormS.setEntry(deg, 0, 0.0);
                        }

                        // Extracting tesseral coefficients if applies
                        if (isTesseral) {
                            // Inserting tesseral coefficients
                            final int deg = Integer.parseInt(fields[0]);
                            final int order = Integer.parseInt(fields[1]);
                            this.denormC.setEntry(deg, order, Double.parseDouble(fields[2]));
                            this.denormS.setEntry(deg, order, Double.parseDouble(fields[3]));
                        }
                    }
                }

            } finally {
                br.close();
            }
        }

    }

    /**
     * Relative difference.
     * 
     * @param expected
     *        expected
     * @param actual
     *        actual
     * @return relative difference
     */
    private static double relDel(final double expected, final double actual) {
        final double rez = (MathLib.abs(expected - actual) / expected);
        return rez;
    }
}
