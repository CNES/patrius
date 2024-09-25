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
 * @history creation 27/09/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-4:08/12/2023:[PATRIUS] Lien entre un repere predefini et un CelestialBody
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.7:FA:FA-2762:18/05/2021:Probleme lors des controles qualite via la PIC 
 * VERSION:4.3:DM:DM-2095:15/05/2019:[PATRIUS] preparation au deploiement sur les depots centraux maven
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: update due to the refactoring of the BentModel class
 * VERSION::FA:345:31/10/2014: coverage
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.ionosphere;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.mesh.FacetBodyShape;
import fr.cnes.sirius.patrius.bodies.mesh.FacetPoint;
import fr.cnes.sirius.patrius.bodies.mesh.ObjMeshLoader;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * @description
 *              <p>
 *              Test class for the Bent model of ionospheric correction.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class BentModelTest {

    /** USK_FILENAME. */
    private static final String USK_FILENAME = "NEWUSK";

    /** R12_FILENAME. */
    private static final String R12_FILENAME = "CCIR12";

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Ionospheric correction : Bent model
         * 
         * @featureDescription Computation of the electronic content with the Bent model for
         *                     the correction of the ionosphere effects.
         * 
         * @coveredRequirements DV-MES_FILT_450
         */
        IONO_BENT_MODEL
    }

    /** Epsilon for reference values comparison. */
    private static final double REF_EPSILON = 2.7e-8;

    /** The signal frequency. */
    private static double frequency;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(BentModelTest.class.getSimpleName(), "Bent ionospheric model");
    }

    /**
     * Orekit setup.
     * 
     * @throws PatriusException
     *         should not happen
     */
    @BeforeClass
    public static void setUp() throws PatriusException {
        Utils.setDataRoot("bent");
        FramesFactory.setConfiguration(Utils.getZOOMConfiguration());

        frequency = 0.1;
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IONO_BENT_MODEL}
     * 
     * @testedMethod {@link BentModel#computeElectronicCont(AbsoluteDate, Vector3D, Frame)}
     * @testedMethod {@link BentModel#BentModel(R12Provider, SolarActivityDataProvider, USKProvider, BodyShape, Vector3D, Frame, double)}
     * 
     * @description generation of one data sample with computeElectronicCont()
     * 
     * @input reference data for 1 station/satellite at 1 given date.
     * 
     * @output value of computeElectronicCont()
     * 
     * @testPassCriteria value as expected
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void bentSingleRefStation1Test() throws PatriusException {

        Report.printMethodHeader("bentSingleRefStation1Test", "Electronic content computation", "Unknown", REF_EPSILON,
            ComparisonType.RELATIVE);

        // Station data
        final Frame stationFrame = FramesFactory.getITRF();
        final double refStaPosX = -5769000.420;
        final double refStaPosY = -1874461.864;
        final double refStaPosZ = -1970925.840;
        final Vector3D refStaPos = new Vector3D(refStaPosX, refStaPosY, refStaPosZ);

        // Satellite data
        final Frame satelliteFrame = FramesFactory.getITRF();
        final int refJjCnes = 20153;
        final double refSecCnes = 2.6489995716769146E+04;
        final TimeScale tt = TimeScalesFactory.getTT();
        final AbsoluteDate refDate = (new AbsoluteDate(
            new DateComponents(DateComponents.FIFTIES_EPOCH,
                refJjCnes), tt)).shiftedBy(refSecCnes);
        final double refSatPosX = -5.5877446855148710E+06;
        final double refSatPosY = -1.8902671304754778E+06;
        final double refSatPosZ = -3.2389286654965272E+06;

        final Vector3D refSatPos = new Vector3D(refSatPosX, refSatPosY, refSatPosZ);

        // Reference TOTNA
        final double refTOTNA = 9.0547654874614576E+16;

        // solar activity
        final SolarActivityDataProvider cstSolarActivity = new ConstantSolarActivity(84., 15.);
        // R12
        final R12Provider r12Prov = new R12Loader(R12_FILENAME);
        // NEWUSK
        final USKProvider uskProv = new USKLoader(USK_FILENAME);

        // earth shape
        final CelestialBodyFrame earthFrame = FramesFactory.getITRF();
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(6378137, 1. / 298.257, earthFrame);

        // model creation
        final BentModel ionoBent = new BentModel(r12Prov, cstSolarActivity,
            uskProv, earth, refStaPos, stationFrame);
        // test the sample
        testOneDataSample(refDate, refSatPos, satelliteFrame, refTOTNA, ionoBent, true);

        // Below ; call with negative solar activity, code coverage purposes only.
        final SolarActivityDataProvider negaSolarActivity = new ConstantSolarActivity(-84., 15.);
        final BentModel ionoBent2 = new BentModel(r12Prov, negaSolarActivity,
            uskProv, earth, refStaPos, stationFrame);
        // Computation with positive and negative solar activity give different results
        final Double electronicContPos = ionoBent.computeElectronicCont(refDate, refSatPos, satelliteFrame);
        final Double electronicContNeg = ionoBent2.computeElectronicCont(refDate, refSatPos, satelliteFrame);
        Assert.assertFalse(electronicContPos == electronicContNeg);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IONO_BENT_MODEL}
     * 
     * @testedMethod {@link BentModel#computeElectronicCont(AbsoluteDate, Vector3D, Frame)}
     * @testedMethod {@link BentModel#BentModel(R12Provider, SolarActivityDataProvider, USKProvider, BodyShape, Vector3D, Frame, double)}
     * 
     * @description generation of one data sample with computeElectronicCont()
     * 
     * @input reference data for 1 station/satellite at 1 given date - on the equator.
     * 
     * @output value of computeElectronicCont()
     * 
     * @testPassCriteria value as expected
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void bentSingleRefStation10Test() throws PatriusException {

        // Station data
        final Frame stationFrame = FramesFactory.getITRF();
        final double refStaPosX = -6378500.;
        final double refStaPosY = 0.;
        final double refStaPosZ = 0.;
        final Vector3D refStaPos = new Vector3D(refStaPosX, refStaPosY, refStaPosZ);

        // Satellite data
        final Frame satelliteFrame = FramesFactory.getITRF();
        final int refJjCnes = 20153;
        final double refSecCnes = 7.3619998230346828e+04;
        final TimeScale tt = TimeScalesFactory.getTT();
        final AbsoluteDate refDate = (new AbsoluteDate(
            new DateComponents(DateComponents.FIFTIES_EPOCH,
                refJjCnes), tt)).shiftedBy(refSecCnes);
        final double refSatPosX = -6.7216397231360767E+06;
        final double refSatPosY = -3.9259700442822365E+05;
        final double refSatPosZ = 7.9461748385360246E+04;

        final Vector3D refSatPos = new Vector3D(refSatPosX, refSatPosY, refSatPosZ);

        // Reference TOTNA
        final double refTOTNA = 1.4240415345944491E+17;

        // solar activity
        // SolarActivityDataFactory.addSolarActivityDataReader(new ACSOLFormatReader("refbent_ACSOL.act"));
        // final SolarActivityDataProvider solarActivity = SolarActivityDataFactory.getSolarActivityDataProvider();
        final SolarActivityDataProvider solarActivity = new ConstantSolarActivity(84., 15.);
        // R12
        final R12Provider r12Prov = new R12Loader(R12_FILENAME);
        // NEWUSK
        final USKProvider uskProv = new USKLoader(USK_FILENAME);

        // earth shape
        final CelestialBodyFrame earthFrame = FramesFactory.getITRF();
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(6378137, 1. / 298.257, earthFrame);

        // model creation
        final BentModel ionoBent = new BentModel(r12Prov, solarActivity,
            uskProv, earth, refStaPos, stationFrame);

        testOneDataSample(refDate, refSatPos, satelliteFrame, refTOTNA, ionoBent, false);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#IONO_BENT_MODEL}
     * @testedMethod {@link BentModel#computeSignalDelay(AbsoluteDate, Vector3D, Frame)}
     * @description generation of one data sample with computeElectronicCont(). For coverage purpose
     * 
     * @input reference data for 1 station/satellite at 1 given date - on the equator.
     * @output value of computeSignalDelay()
     * @testPassCriteria value as expected
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testComputeSignalDelay() throws PatriusException {

        Report.printMethodHeader("testComputeSignalDelay", "Signal delay computation", "Unknown", 0.,
            ComparisonType.ABSOLUTE);

        // Station data
        final Frame stationFrame = FramesFactory.getITRF();
        final double refStaPosX = -6378500.;
        final double refStaPosY = 0.;
        final double refStaPosZ = 0.;
        final Vector3D refStaPos = new Vector3D(refStaPosX, refStaPosY, refStaPosZ);

        // Satellite data
        final Frame satelliteFrame = FramesFactory.getITRF();
        final int refJjCnes = 20153;
        final double refSecCnes = 7.3619998230346828e+04;
        final TimeScale tt = TimeScalesFactory.getTT();
        final AbsoluteDate refDate = (new AbsoluteDate(
            new DateComponents(DateComponents.FIFTIES_EPOCH,
                refJjCnes), tt)).shiftedBy(refSecCnes);
        final double refSatPosX = -6.7216397231360767E+06;
        final double refSatPosY = -3.9259700442822365E+05;
        final double refSatPosZ = 7.9461748385360246E+04;

        final Vector3D refSatPos = new Vector3D(refSatPosX, refSatPosY, refSatPosZ);

        // solar activity
        // SolarActivityDataFactory.addSolarActivityDataReader(new ACSOLFormatReader("refbent_ACSOL.act"));
        // final SolarActivityDataProvider solarActivity = SolarActivityDataFactory.getSolarActivityDataProvider();
        final SolarActivityDataProvider solarActivity = new ConstantSolarActivity(84., 15.);
        // R12
        final R12Provider r12Prov = new R12Loader(R12_FILENAME);
        // NEWUSK
        final USKProvider uskProv = new USKLoader(USK_FILENAME);

        // earth shape
        final CelestialBodyFrame earthFrame = FramesFactory.getITRF();
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(6378137, 1. / 298.257, earthFrame);

        // model creation
        final BentModel ionoBent = new BentModel(r12Prov, solarActivity,
            uskProv, earth, refStaPos, stationFrame);

        // Computed TOTNA
        final double tec = ionoBent.computeElectronicCont(refDate, refSatPos, satelliteFrame);
        final double CDELTAT = 40.3;

        final double result1 = CDELTAT / (Constants.SPEED_OF_LIGHT * frequency * frequency) * tec;
        Assert.assertEquals(result1, ionoBent.computeSignalDelay(frequency, refDate, refSatPos, satelliteFrame), 0.);
        Report.printToReport("Signal delay", result1,
            ionoBent.computeSignalDelay(frequency, refDate, refSatPos, satelliteFrame));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IONO_BENT_MODEL}
     * 
     * @testedMethod {@link BentModel#computeElectronicCont(AbsoluteDate, Vector3D, Frame)}
     * @testedMethod {@link BentModel#BentModel(R12Provider, SolarActivityDataProvider, USKProvider, BodyShape, Vector3D, Frame, double)}
     * 
     * @description generation of one data sample with computeElectronicCont()
     * 
     * @input reference data for 1 station/satellite at 1 given date.
     *        Special case for coverage purposes.
     * 
     * @output value of computeElectronicCont()
     * 
     * @testPassCriteria value as expected
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void bentSingleRefCoverage01Test() throws PatriusException {

        // Station data
        final Frame stationFrame = FramesFactory.getITRF();
        final double refStaPosX = -4175044.253;
        final double refStaPosY = 3033347.205;
        final double refStaPosZ = 3749423.345;
        final Vector3D refStaPos = new Vector3D(refStaPosX, refStaPosY, refStaPosZ);

        // Satellite data
        final Frame satelliteFrame = FramesFactory.getITRF();
        final AbsoluteDate refDate = new AbsoluteDate("2005-03-17T14:24:05.807",
            TimeScalesFactory.getUTC());

        final double refSatPosX = -5905867.181630313;
        final double refSatPosY = 4408738.546852538;
        final double refSatPosZ = 2214973.5379348355;

        final Vector3D refSatPos = new Vector3D(refSatPosX, refSatPosY, refSatPosZ);

        // Reference TOTNA
        final double refTOTNA = 1.22212512408211072E17;

        // solar activity
        final SolarActivityDataProvider cstSolarActivity = new ConstantSolarActivity(84., 15.);
        // R12
        final R12Provider r12Prov = new R12Loader(R12_FILENAME);
        // NEWUSK
        final USKProvider uskProv = new USKLoader(USK_FILENAME);

        // earth shape
        final CelestialBodyFrame earthFrame = FramesFactory.getITRF();
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(6378137, 1. / 298.257, earthFrame);

        // model creation
        final BentModel ionoBent = new BentModel(r12Prov, cstSolarActivity,
            uskProv, earth, refStaPos, stationFrame);
        // test the sample
        testOneDataSample(refDate, refSatPos, satelliteFrame, refTOTNA, ionoBent, false);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IONO_BENT_MODEL}
     * 
     * @testedMethod {@link BentModel#computeElectronicCont(AbsoluteDate, Vector3D, Frame)}
     * @testedMethod {@link BentModel#BentModel(R12Provider, SolarActivityDataProvider, USKProvider, BodyShape, Vector3D, Frame, double)}
     * 
     * @description generation of one data sample with computeElectronicCont()
     * 
     * @input reference data for 1 station/satellite at 1 given date.
     *        Special case for coverage purposes.
     * 
     * @output value of computeElectronicCont()
     * 
     * @testPassCriteria value as expected
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void bentSingleRefCoverage02Test() throws PatriusException {

        // Station data
        final Frame stationFrame = FramesFactory.getITRF();
        final double refStaPosX = -5769000.42;
        final double refStaPosY = -1874461.864;
        final double refStaPosZ = -1970925.840;
        final Vector3D refStaPos = new Vector3D(refStaPosX, refStaPosY, refStaPosZ);

        // Satellite data
        final Frame satelliteFrame = FramesFactory.getITRF();
        final AbsoluteDate refDate = new AbsoluteDate("2006-01-25T23:40:00.000",
            TimeScalesFactory.getUTC());

        final double refSatPosX = -5168545.654642752;
        final double refSatPosY = -3974355.015862368;
        final double refSatPosZ = -2492946.5481476765;

        final Vector3D refSatPos = new Vector3D(refSatPosX, refSatPosY, refSatPosZ);

        // Reference TOTNA
        final double refTOTNA = 1.72423396614846234E18;

        // solar activity
        final SolarActivityDataProvider cstSolarActivity = new ConstantSolarActivity(130., 15.);
        // R12
        final R12Provider r12Prov = new R12Loader(R12_FILENAME);
        // NEWUSK
        final USKProvider uskProv = new USKLoader(USK_FILENAME);

        // earth shape
        final CelestialBodyFrame earthFrame = FramesFactory.getITRF();
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(6378137, 1. / 298.257, earthFrame);

        // model creation
        final BentModel ionoBent = new BentModel(r12Prov, cstSolarActivity,
            uskProv, earth, refStaPos, stationFrame);
        // test the sample
        testOneDataSample(refDate, refSatPos, satelliteFrame, refTOTNA, ionoBent, false);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IONO_BENT_MODEL}
     * 
     * @testedMethod {@link BentModel#computeElectronicCont(AbsoluteDate, Vector3D, Frame)}
     * @testedMethod {@link BentModel#BentModel(R12Provider, SolarActivityDataProvider, USKProvider, BodyShape, Vector3D, Frame, double)}
     * 
     * @description generation of one data sample with computeElectronicCont()
     * 
     * @input reference data for 1 station/satellite at 1 given date.
     *        Special case for coverage purposes.
     * 
     * @output value of computeElectronicCont()
     * 
     * @testPassCriteria value as expected
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void bentSingleRefCoverage03Test() throws PatriusException {

        // Station data
        final Frame stationFrame = FramesFactory.getITRF();
        final double refStaPosX = -6378500.;
        final double refStaPosY = -1000000.;
        final double refStaPosZ = 0.;
        final Vector3D refStaPos = new Vector3D(refStaPosX, refStaPosY, refStaPosZ);

        // Satellite data
        final Frame satelliteFrame = FramesFactory.getITRF();
        final int refJjCnes = 20153;
        final double refSecCnes = 7.3619998230346828e+04;
        final TimeScale tt = TimeScalesFactory.getTT();
        final AbsoluteDate refDate = (new AbsoluteDate(
            new DateComponents(DateComponents.FIFTIES_EPOCH,
                refJjCnes), tt)).shiftedBy(refSecCnes);
        final double refSatPosX = -6.7216397231360767E+06;
        final double refSatPosY = -3.9259700442822365E+05;
        final double refSatPosZ = 7.9461748385360246E+04;

        final Vector3D refSatPos = new Vector3D(refSatPosX, refSatPosY, refSatPosZ);

        // Reference TOTNA
        final double refTOTNA = 5.9596873921953248E16;

        // solar activity
        // SolarActivityDataFactory.addSolarActivityDataReader(new ACSOLFormatReader("refbent_ACSOL.act"));
        // final SolarActivityDataProvider solarActivity = SolarActivityDataFactory.getSolarActivityDataProvider();
        final SolarActivityDataProvider solarActivity = new ConstantSolarActivity(84., 15.);
        // R12
        final R12Provider r12Prov = new R12Loader(R12_FILENAME);
        // NEWUSK
        final USKProvider uskProv = new USKLoader(USK_FILENAME);

        // earth shape
        final CelestialBodyFrame earthFrame = FramesFactory.getITRF();
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(6378137, 1. / 298.257, earthFrame);

        // model creation
        final BentModel ionoBent = new BentModel(r12Prov, solarActivity,
            uskProv, earth, refStaPos, stationFrame);

        testOneDataSample(refDate, refSatPos, satelliteFrame, refTOTNA, ionoBent, false);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IONO_BENT_MODEL}
     * 
     * @testedMethod {@link BentModel#computeElectronicCont(AbsoluteDate, Vector3D, Frame)}
     * @testedMethod {@link BentModel#BentModel(R12Provider, SolarActivityDataProvider, USKProvider, BodyShape, Vector3D, Frame, double)}
     * 
     * @description generation of one data sample with computeElectronicCont()
     * 
     * @input reference data for 1 station/satellite at 1 given date.
     *        Special case for coverage purposes.
     * 
     * @output value of computeElectronicCont()
     * 
     * @testPassCriteria value as expected
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void bentSingleRefCoverage04Test() throws PatriusException {
        // Station data
        final Frame stationFrame = FramesFactory.getITRF();
        final double refStaPosX = 1.;
        final double refStaPosY = 1.;
        final double refStaPosZ = -6378250.0;
        final Vector3D refStaPos = new Vector3D(refStaPosX, refStaPosY, refStaPosZ);

        // Satellite data
        final Frame satelliteFrame = FramesFactory.getITRF();
        final AbsoluteDate refDate = new AbsoluteDate("2005-03-06T17:45:55.814",
            TimeScalesFactory.getUTC());

        final double refSatPosX = 05.67147212024;
        final double refSatPosY = -233668.95414682373;
        final double refSatPosZ = -21126928.947411655;

        final Vector3D refSatPos = new Vector3D(refSatPosX, refSatPosY, refSatPosZ);

        // Reference TOTNA
        final double refTOTNA = 9.0632944296550224E16;

        // solar activity
        final SolarActivityDataProvider cstSolarActivity = new ConstantSolarActivity(130., 15.);
        // R12
        final R12Provider r12Prov = new R12Loader(R12_FILENAME);
        // NEWUSK
        final USKProvider uskProv = new USKLoader(USK_FILENAME);

        // earth shape
        final CelestialBodyFrame earthFrame = FramesFactory.getITRF();
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(6378137, 1. / 298.257, earthFrame);

        // model creation
        final BentModel ionoBent = new BentModel(r12Prov, cstSolarActivity,
            uskProv, earth, refStaPos, stationFrame);
        // test the sample
        testOneDataSample(refDate, refSatPos, satelliteFrame, refTOTNA, ionoBent, false);
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @description Evaluate the correction model serialization / deserialization process.
     *
     * @testPassCriteria The correction model can be serialized and deserialized.
     */
    @Test
    public void testSerialization() throws PatriusException {

        // Station data
        final Frame stationFrame = FramesFactory.getITRF();
        final double refStaPosX = -5769000.420;
        final double refStaPosY = -1874461.864;
        final double refStaPosZ = -1970925.840;
        final Vector3D refStaPos = new Vector3D(refStaPosX, refStaPosY, refStaPosZ);

        // Satellite data
        final int refJjCnes = 20153;
        final double refSecCnes = 2.6489995716769146E+04;
        final TimeScale tt = TimeScalesFactory.getTT();
        final AbsoluteDate refDate = (new AbsoluteDate(
            new DateComponents(DateComponents.FIFTIES_EPOCH,
                refJjCnes), tt)).shiftedBy(refSecCnes);
        final double refSatPosX = -5.5877446855148710E+06;
        final double refSatPosY = -1.8902671304754778E+06;
        final double refSatPosZ = -3.2389286654965272E+06;

        final Vector3D refSatPos = new Vector3D(refSatPosX, refSatPosY, refSatPosZ);

        // solar activity
        final SolarActivityDataProvider cstSolarActivity = new ConstantSolarActivity(84., 15.);
        // R12
        final R12Provider r12Prov = new R12Loader(R12_FILENAME);
        // NEWUSK
        final USKProvider uskProv = new USKLoader(USK_FILENAME);

        // earth shape
        final CelestialBodyFrame earthFrame = FramesFactory.getITRF();
        final EllipsoidBodyShape earth = new OneAxisEllipsoid(6378137, 1. / 298.257, earthFrame);

        // model creation
        final BentModel model = new BentModel(r12Prov, cstSolarActivity,
            uskProv, earth, refStaPos, stationFrame);
        final BentModel deserializedModel = TestUtils.serializeAndRecover(model);

        Assert.assertEquals(model.computeSignalDelay(frequency, refDate, refSatPos, stationFrame),
            deserializedModel.computeSignalDelay(frequency, refDate, refSatPos, stationFrame), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#IONO_BENT_MODEL}
     * 
     * @testedMethod {@link BentModel} constructor
     * 
     * @description check an exception is thrown if associated BodyPoint is not a GeodeticPoint
     * 
     * @testPassCriteria an exception is thrown if associated BodyPoint is not a GeodeticPoint
     */
    @Test
    public void bentException() throws PatriusException, URISyntaxException {
        // Try with FacetBodyShape
        try {
            final String modelFile1 = "mnt" + File.separator + "Phobos_Ernst_HD.obj";
            final String fullName1 = FacetBodyShape.class.getClassLoader().getResource(modelFile1).toURI().getPath();
            final FacetBodyShape body = new FacetBodyShape("", FramesFactory.getGCRF(),
                new ObjMeshLoader(fullName1));
            new BentModel(null, null, null, new TopocentricFrame(new FacetPoint(body, Vector3D.PLUS_I, ""), ""));
            Assert.fail();
        } catch (final PatriusRuntimeException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * Test for one data sample.
     * 
     * @param stationFrame
     *        .
     * @param refStaPos
     *        .
     * @param satelliteFrame
     *        .
     * @param refDate
     *        .
     * @param refSatPos
     *        .
     * @param refTOTNA
     *        .
     * @param bentCorr
     *        .
     * @throws PatriusException
     *         should not happen
     */
    private static void testOneDataSample(final AbsoluteDate refDate, final Vector3D refSatPos,
                                          final Frame satelliteFrame, final double refTOTNA, final BentModel bentCorr,
                                          final boolean writeInReport)
        throws PatriusException {

        // Computed TOTNA
        final double computedTOTNA = bentCorr.computeElectronicCont(refDate, refSatPos, satelliteFrame);

        MathLib.abs(refTOTNA - computedTOTNA);
        final double relDiff = MathLib.abs((refTOTNA - computedTOTNA) / refTOTNA);

        // Relative difference expected always below REF_EPSILON
        Assert.assertEquals(0., relDiff, REF_EPSILON);

        if (writeInReport) {
            Report.printToReport("Electronic content", refTOTNA, computedTOTNA);
        }
    }
}
