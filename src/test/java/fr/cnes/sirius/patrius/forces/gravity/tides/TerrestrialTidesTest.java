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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:90:15/10/2013:Using normalized attraction
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:241:08/12/2014:improved tides conception
 * VERSION::FA:464:24/06/2015:Analytical computation of the partial derivatives
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.gravity.tides.TidesStandards.TidesStandard;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.GraggBulirschStoerIntegrator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit testing for terrestrial tides.
 * 
 * @author Julie Anton, Gerald Mercadier
 * 
 * @version $Id: TerrestrialTidesTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class TerrestrialTidesTest {
    /** Features description. */
    public enum features {
        /**
         * @featureTitle terrestrial tides
         * 
         * @featureDescription computation of the different contributions
         * 
         * @coveredRequirements DV-MOD_200
         */
        TERRESTRIAL_TIDES_MAGNITUDE,

        /**
         * @featureTitle terrestrial tides data provider
         * 
         * @featureDescription test {@link TerrestrialTidesDataProvider}
         * 
         * @coveredRequirements DV-MOD_200
         */
        TERRESTRIAL_TIDES_DATA_PROVIDER
    }

    /**
     * FA 93 : added test to ensure the list of parameters is correct
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#TERRESTRIAL_TIDES_MAGNITUDE}
     * 
     * @testedMethod {@link TerrestrialTides#getParametersNames()}
     * @testedMethod {@link TerrestrialTides#getParameter(String)}
     * @testedMethod {@link TerrestrialTides#setParameter()}
     * 
     * @description Test for the parameters
     * 
     * @input a parameter
     * 
     * @output its value
     * 
     * @testPassCriteria the parameter value is as expected exactly (0 ulp difference)
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testParamList() throws PatriusException, IOException, ParseException {

        new AbsoluteDate(2005, 03, 05, 00, 45, 0.0, TimeScalesFactory.getTAI());

        // PV REF
        // -2.04682492904072348e+06 -6.41308742588674650e+06 -1.00704149423014715e+04
        // 6.86418712311315812e+03 -2.18303740441877153e+03 1.02300507209407936e+01
        final Vector3D pTest = new Vector3D(-2.04682492904072348e+06, -6.41308742588674650e+06,
            -1.00704149423014715e+04);
        final Vector3D vTest = new Vector3D(6.86418712311315812e+03, -2.18303740441877153e+03, 1.02300507209407936e+01);
        new PVCoordinates(pTest, vTest);

        // mu from grim4s4_gr GRGS file (Earth)
        final double muEarth = 3.9860043770442E+14;

        // Earth equatorial radius from grim4s4_gr GRGS file
        final double requa = 6378136.;

        final List<CelestialBody> bodies = new ArrayList<CelestialBody>();
        bodies.add(CelestialBodyFactory.getSun());
        bodies.add(CelestialBodyFactory.getMoon());

        // deformation due to lunisolar attraction up to degree 2

        final TerrestrialTides model = new TerrestrialTides(FramesFactory.getITRF(), requa, muEarth, bodies,
            false, false, false, new TerrestrialTidesDataProvider());

        double k = 5;
        Assert.assertEquals(2, model.getParameters().size());
        final ArrayList<Parameter> paramList = model.getParameters();
        for (int i = 0; i < paramList.size(); i++) {
            paramList.get(i).setValue(k);
            Assert.assertTrue(Precision.equals(k, paramList.get(i).getValue(), 0));
            k++;
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#TERRESTRIAL_TIDES_MAGNITUDE}
     * 
     * @testedMethod {@link TerrestrialTides#TerrestrialTides(fr.cnes.sirius.patrius.frames.Frame, double, double, List, boolean, boolean, boolean, IERSStandard)}
     * 
     * @description compare the magnitude of the different contributions
     * 
     * @input corrections related to terrestrial tides (third body attraction on an anelastic crust up to degree 2 and
     *        3, frequential correction of Love numbers, ellipticity correction) and a point on a low orbit.
     * 
     * @output each corrective component due to terrestrial tides
     * 
     * @testPassCriteria the different corrections must be the expected ones (references provided from OBELIX).
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public final void testForce() throws PatriusException {

        final double delta = 2e-14;
        final double delta_freq = 3e-11;

        // date 20153 2700
        final AbsoluteDate dateTest = new AbsoluteDate(2005, 03, 05, 00, 45, 0.0, TimeScalesFactory.getTAI());

        // PV REF
        // -2.04682492904072348e+06 -6.41308742588674650e+06 -1.00704149423014715e+04
        // 6.86418712311315812e+03 -2.18303740441877153e+03 1.02300507209407936e+01
        final Vector3D pTest = new Vector3D(-2.04682492904072348e+06, -6.41308742588674650e+06,
            -1.00704149423014715e+04);
        final Vector3D vTest = new Vector3D(6.86418712311315812e+03, -2.18303740441877153e+03, 1.02300507209407936e+01);
        final PVCoordinates pvTest = new PVCoordinates(pTest, vTest);

        // mu from grim4s4_gr GRGS file (Earth)
        final double muEarth = 3.9860043770442E+14;

        // Earth equatorial radius from grim4s4_gr GRGS file
        final double requa = 6378136.;

        final List<CelestialBody> bodies = new ArrayList<CelestialBody>();
        bodies.add(CelestialBodyFactory.getSun());
        bodies.add(CelestialBodyFactory.getMoon());

        // deformation due to lunisolar attraction up to degree 2

        final TerrestrialTides tide2Test = new TerrestrialTides(FramesFactory.getITRF(), requa, muEarth, bodies,
            false, false, false, new TerrestrialTidesDataProvider());
        final Vector3D accTide2 = tide2Test.computeAcceleration(pvTest, FramesFactory.getITRF(), dateTest);

        final double[] actualAccTideAtt2 = { accTide2.getX(), accTide2.getY(), accTide2.getZ() };

        // expected acceleration (OBELIX reference)
        // 1.3614908449285197E-07 7.7884162766524375E-08 1.6570552436597049E-07
        final double[] expectedAccTideAtt2 = { 1.3614908449285197E-07, 7.7884162766524375E-08, 1.6570552436597049E-07 };

        Assert.assertArrayEquals(expectedAccTideAtt2, actualAccTideAtt2, delta);

        // deformation due to lunisolar attraction up to degree 3
        final TerrestrialTides tide3Test = new TerrestrialTides(FramesFactory.getITRF(), requa, muEarth, bodies,
            true, false, false, new TerrestrialTidesDataProvider());

        final Vector3D accTide3 = tide3Test.computeAcceleration(pvTest, FramesFactory.getITRF(), dateTest);

        final double[] actualAccTideAtt3 = { accTide3.getX(), accTide3.getY(), accTide3.getZ() };

        // expected acceleration (OBELIX reference)
        // 1.3518826226853796E-07 7.7984444883687677E-08 1.6453128681485267E-07
        final double[] expectedAccTideAtt3 = { 1.3518826226853796E-07, 7.7984444883687677E-08, 1.6453128681485267E-07 };

        Assert.assertArrayEquals(expectedAccTideAtt3, actualAccTideAtt3, delta);

        // frequency correction
        final TerrestrialTides tideFreqTest = new TerrestrialTides(FramesFactory.getITRF(), requa, muEarth, bodies,
            false, true, false, new TerrestrialTidesDataProvider());

        final Vector3D accTideFreqTest = tideFreqTest
            .computeAcceleration(pvTest, FramesFactory.getITRF(), dateTest);

        final double[] actualAccTideFreqTest = new double[3];
        actualAccTideFreqTest[0] = accTideFreqTest.getX();
        actualAccTideFreqTest[1] = accTideFreqTest.getY();
        actualAccTideFreqTest[2] = accTideFreqTest.getZ();

        // expected acceleration (OBELIX reference)
        // 1.3608225435624012E-07 7.7754275116554434E-08 1.5356715805228039E-07
        final double[] expectedAccTideFreqTest = { 1.3608225435624012E-07, 7.7754275116554434E-08,
            1.5356715805228039E-07 };

        Assert.assertArrayEquals(expectedAccTideFreqTest, actualAccTideFreqTest, delta_freq);

        // ellipticity correction
        final TerrestrialTides tideEllTest = new TerrestrialTides(FramesFactory.getITRF(), requa, muEarth, bodies,
            false, false, true, new TerrestrialTidesDataProvider());

        final Vector3D accTideEllTest = tideEllTest.computeAcceleration(pvTest, FramesFactory.getITRF(), dateTest);

        final double[] actualAccTideEllTest = { accTideEllTest.getX(), accTideEllTest.getY(), accTideEllTest.getZ() };

        // expected acceleration (OBELIX reference)
        // 1.3643278659275806E-07 7.8261670829858954E-08 1.6644178803103256E-07
        final double[] expectedAccTideEllTest = { 1.3643278659275806E-07, 7.8261670829858954E-08,
            1.6644178803103256E-07 };

        Assert.assertArrayEquals(expectedAccTideEllTest, actualAccTideEllTest, delta);

        // all corrections (other PV in GCRF)
        final TerrestrialTides tides = new TerrestrialTides(FramesFactory.getGCRF(), requa, muEarth);

        // PV in GCRF
        // 2.70303160815657163e+06 6.15588486808402184e+06 -1.16119700511837618e+04
        // -7.06109645777311016e+03 3.08016738885103905e+03 1.36108059143140654e+01
        final PVCoordinates pv = new PVCoordinates(new Vector3D(2.70303160815657163e+06, 6.15588486808402184e+06,
            -1.16119700511837618e+04), new Vector3D(-7.06109645777311016e+03, 3.08016738885103905e+03,
            1.36108059143140654e+01));

        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getGCRF(), dateTest, muEarth);
        final SpacecraftState scr = new SpacecraftState(orbit);

        final Vector3D accTidesAllCorr = tides.computeAcceleration(scr);

        final double[] actualAccTideAllCorr =
        { accTidesAllCorr.getX(), accTidesAllCorr.getY(), accTidesAllCorr.getZ() };

        // expected results (PATRIUS 4.2 results)
        // -1.4352497580292397E-7 -6.425270953926116E-8 1.7753603936380424E-7
        final double[] expectedAccTideAllCorr =
        { -1.4352497580292397E-7, -6.425270953926116E-8, 1.7753602660843853E-7 };

        Assert.assertArrayEquals(expectedAccTideAllCorr, actualAccTideAllCorr, Precision.DOUBLE_COMPARISON_EPSILON);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#TERRESTRIAL_TIDES_MAGNITUDE}
     * 
     * @testedMethod {@link TerrestrialTides#TerrestrialTides(fr.cnes.sirius.patrius.frames.Frame, double, double)}
     * 
     * @description add force contribution to the numerical propagator
     * 
     * @input corrections related to terrestrial tides (third body attraction on an anelastic crust up to degree 2 and
     *        3, frequential correction of Love numbers, ellipticity correction).
     * 
     * @output each corrective component due to terrestrial tides
     * 
     * @testPassCriteria the tested methods run with no error.
     * 
     * @throws PatriusException
     *         when an Orekit error occurs
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public final void testContrib() throws PatriusException {

        // date 20153 1320
        final AbsoluteDate date = new AbsoluteDate(2005, 03, 06, 00, 22, 0.0, TimeScalesFactory.getTAI());
        final AbsoluteDate finalDate = date.shiftedBy(10.);

        // mu from grim4s4_gr GRGS file (Earth)
        final String paramName = "central attraction coefficient";
        final double muEarth = 3.9860043770442E+14;
        final Parameter mu = new Parameter(paramName, muEarth);
        // Earth equatorial radius from grim4s4_gr GRGS file
        final double requa = 6378136.;
        final Parameter r = new Parameter("requa", requa);

        // force model: tides with all corrections
        final TerrestrialTides tides = new TerrestrialTides(FramesFactory.getGCRF(), r, mu);

        // test addContribution method
        final PVCoordinates pv = new PVCoordinates(new Vector3D(2.70303160815657163e+06, 6.15588486808402184e+06,
            -1.16119700511837618e+04), new Vector3D(-7.06109645777311016e+03, 3.08016738885103905e+03,
            1.36108059143140654e+01));
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getGCRF(), date, muEarth);
        final SpacecraftState scr = new SpacecraftState(orbit);

        final NumericalPropagator calc = new NumericalPropagator(
            new GraggBulirschStoerIntegrator(10.0, 30.0, 0, 1.0e-5));
        calc.addForceModel(tides);

        calc.setInitialState(scr);
        calc.propagate(finalDate);
        final PVCoordinates finalPV = calc.getPVCoordinates(finalDate, FramesFactory.getGCRF());
        final double[] actualPV = { finalPV.getPosition().getX(), finalPV.getPosition().getY(),
            finalPV.getPosition().getZ() };

        // PATRIUS 4.2 results
        // 2632244.9161365354 6186282.151426412 -11475.10342016054
        final double[] expectedPV = { 2632244.916136536, 6186282.151426412, -11475.103420160543 };

        Assert.assertArrayEquals(expectedPV, actualPV, Precision.DOUBLE_COMPARISON_EPSILON);

        // test other methods

        tides.getEventsDetectors();
    }

    /**
     * @throws PatriusException
     *         thrown if standard is different from IERS 2003
     * @testType UT
     * 
     * @testedFeature {@link features#TERRESTRIAL_TIDES_DATA_PROVIDER}
     * 
     * @testedMethod {@link TerrestrialTidesDataProvider#TerrestrialTidesDataProvider(TidesStandard)}
     * 
     * @description Test TerrestrialTidesDataProvider constructor for coverage purpose
     * 
     * @input TerrestrialTidesDataProvider constructor
     * 
     * @output exception
     * 
     * @testPassCriteria throws OrekitException
     * 
     * @referenceVersion 2.3.1
     * 
     * @nonRegressionVersion 2.3.1
     */
    @Test(expected = PatriusException.class)
    public final void testTerrestrialTidesDataProviderConstructor() throws PatriusException {
        new TerrestrialTidesDataProvider(TidesStandard.IERS1996);
    }

    /**
     * 
     * Set up method before running the test.
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        fr.cnes.sirius.patrius.Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
    }

    /**
     * Additional partial derviatives tests to ensure the jacobian is correctly taken into account.
     * 
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void testPartialDerivatives() throws PatriusException, IOException, ParseException {

        final Frame gcrf = FramesFactory.getGCRF();

        // date 20153 1320
        final AbsoluteDate date = new AbsoluteDate(2005, 03, 06, 00, 22, 0.0, TimeScalesFactory.getTAI());
        date.shiftedBy(10.);

        // constants
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final double mu = Constants.GRIM5C1_EARTH_MU;
        final double a = ae + 400e3;
        final double e = .001;
        final double i = .93;

        // force model: tides with all corrections
        final TerrestrialTides tides = new TerrestrialTides(gcrf, ae, mu);

        // orbit
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, 0, 0, 0, PositionAngle.TRUE, gcrf, date, mu);
        final SpacecraftState state = new SpacecraftState(orbit);

        // partial derivatives
        final double[][] dAccdPos = new double[6][6];
        tides.addDAccDState(state, dAccdPos, new double[6][6]);

        /*
         * ======================================
         * finite diff _ DELTAS IN GCRF
         */
        final Vector3D pos = orbit.getPVCoordinates(gcrf).getPosition();
        final Vector3D vel = orbit.getPVCoordinates(gcrf).getVelocity();

        /* ===================================== */

        final double dh = .5;

        // positions
        final Vector3D ppx = pos.add(Vector3D.PLUS_I.scalarMultiply(dh));
        final Vector3D ppy = pos.add(Vector3D.PLUS_J.scalarMultiply(dh));
        final Vector3D ppz = pos.add(Vector3D.PLUS_K.scalarMultiply(dh));

        final Vector3D pmx = pos.add(Vector3D.PLUS_I.scalarMultiply(-dh));
        final Vector3D pmy = pos.add(Vector3D.PLUS_J.scalarMultiply(-dh));
        final Vector3D pmz = pos.add(Vector3D.PLUS_K.scalarMultiply(-dh));

        // pv coordinates
        final PVCoordinates pvpx = new PVCoordinates(ppx, vel);
        final PVCoordinates pvpy = new PVCoordinates(ppy, vel);
        final PVCoordinates pvpz = new PVCoordinates(ppz, vel);

        final PVCoordinates pvmx = new PVCoordinates(pmx, vel);
        final PVCoordinates pvmy = new PVCoordinates(pmy, vel);
        final PVCoordinates pvmz = new PVCoordinates(pmz, vel);

        // orbits
        final CartesianOrbit opx = new CartesianOrbit(pvpx, gcrf, date, mu);
        final CartesianOrbit opy = new CartesianOrbit(pvpy, gcrf, date, mu);
        final CartesianOrbit opz = new CartesianOrbit(pvpz, gcrf, date, mu);

        final CartesianOrbit omx = new CartesianOrbit(pvmx, gcrf, date, mu);
        final CartesianOrbit omy = new CartesianOrbit(pvmy, gcrf, date, mu);
        final CartesianOrbit omz = new CartesianOrbit(pvmz, gcrf, date, mu);

        // states
        final SpacecraftState sspx = new SpacecraftState(opx);
        final SpacecraftState sspy = new SpacecraftState(opy);
        final SpacecraftState sspz = new SpacecraftState(opz);

        final SpacecraftState ssmx = new SpacecraftState(omx);
        final SpacecraftState ssmy = new SpacecraftState(omy);
        final SpacecraftState ssmz = new SpacecraftState(omz);

        // acc
        final Vector3D apx = tides.computeAcceleration(sspx);
        final Vector3D apy = tides.computeAcceleration(sspy);
        final Vector3D apz = tides.computeAcceleration(sspz);

        final Vector3D amx = tides.computeAcceleration(ssmx);
        final Vector3D amy = tides.computeAcceleration(ssmy);
        final Vector3D amz = tides.computeAcceleration(ssmz);

        // pds
        final Vector3D pdx = apx.subtract(amx).scalarMultiply(1 / (2 * dh));
        final Vector3D pdy = apy.subtract(amy).scalarMultiply(1 / (2 * dh));
        final Vector3D pdz = apz.subtract(amz).scalarMultiply(1 / (2 * dh));

        final double[][] acc = { pdx.toArray(), pdy.toArray(), pdz.toArray() };
        final double[][] tacc = this.transpose(acc);

        final double[][] diff = new double[3][3];
        for (int ii = 0; ii < diff.length; ii++) {
            for (int j = 0; j < diff[ii].length; j++) {
                diff[ii][j] = (dAccdPos[ii][j] - tacc[ii][j]) / dAccdPos[ii][j];
                Assert.assertEquals(0, diff[ii][j], 5e-5);
            }
        }

        try {
            final double[] dAccdParam = new double[3];

            tides.addDAccDParam(state, new Parameter("toto", 1.), dAccdParam);
        } catch (final PatriusException exp) {
            // expected
        }
    }

    /**
     * 
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedMethod {@link TerrestrialTides#TerrestrialTides(Frame, double, double, List, boolean, boolean, boolean, ITerrestrialTidesDataProvider, boolean)}
     * @testedMethod {@link TerrestrialTides#TerrestrialTides(Frame, Parameter, Parameter, List, boolean, boolean, boolean, ITerrestrialTidesDataProvider, boolean)}
     * @testedMethod {@link TerrestrialTides#TerrestrialTides(Frame, double, double, boolean)}
     * @testedMethod {@link TerrestrialTides#TerrestrialTides(Frame, Parameter, Parameter, boolean)}
     * 
     * @description compute acceleration partial derivatives wrt position
     * 
     * @input instances of {@link TerrestrialTides}
     * 
     * @output partial derivatives
     * 
     * @testPassCriteria partial derivatives must be all null, since computation is deactivated at construction
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testNullPD() throws PatriusException {

        final Vector3D pTest = new Vector3D(-2.04682492904072348e+06, -6.41308742588674650e+06,
            -1.00704149423014715e+04);
        final Vector3D vTest = new Vector3D(6.86418712311315812e+03, -2.18303740441877153e+03, 1.02300507209407936e+01);
        new PVCoordinates(pTest, vTest);

        // mu from grim4s4_gr GRGS file (Earth)
        final double muEarth = 3.9860043770442E+14;

        // Earth equatorial radius from grim4s4_gr GRGS file
        final double requa = 6378136.;

        final List<CelestialBody> bodies = new ArrayList<CelestialBody>();
        bodies.add(CelestialBodyFactory.getSun());
        bodies.add(CelestialBodyFactory.getMoon());

        // instantiations
        final TerrestrialTides model = new TerrestrialTides(FramesFactory.getITRF(), requa, muEarth, bodies,
            false, false, false, new TerrestrialTidesDataProvider(), false);

        final TerrestrialTides model2 = new TerrestrialTides(FramesFactory.getITRF(), new Parameter("req", requa),
            new Parameter("mu", muEarth), bodies, false, false, false, new TerrestrialTidesDataProvider(), false);

        final TerrestrialTides model3 = new TerrestrialTides(FramesFactory.getGCRF(), requa, muEarth, false);
        final TerrestrialTides model4 = new TerrestrialTides(FramesFactory.getGCRF(), new Parameter("req", requa),
            new Parameter("mu", muEarth), false);

        // Check that derivatives computation is deactivated
        Assert.assertFalse(model.computeGradientPosition());
        // Derivatives wrt velocity are always null for tides
        Assert.assertFalse(model.computeGradientVelocity());

        // Spacecraft
        final PVCoordinates pv = new PVCoordinates(new Vector3D(2.70303160815657163e+06, 6.15588486808402184e+06,
            -1.16119700511837618e+04), new Vector3D(-7.06109645777311016e+03, 3.08016738885103905e+03,
            1.36108059143140654e+01));
        final AbsoluteDate date = new AbsoluteDate(2005, 03, 06, 00, 22, 0.0, TimeScalesFactory.getTAI());
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getGCRF(), date, muEarth);
        final SpacecraftState scr = new SpacecraftState(orbit);

        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        final double[][] dAccdPos2 = new double[3][3];
        final double[][] dAccdVel2 = new double[3][3];
        final double[][] dAccdPos3 = new double[3][3];
        final double[][] dAccdVel3 = new double[3][3];
        final double[][] dAccdPos4 = new double[3][3];
        final double[][] dAccdVel4 = new double[3][3];

        // Compute partial derivatives
        model.addDAccDState(scr, dAccdPos, dAccdVel);
        model2.addDAccDState(scr, dAccdPos2, dAccdVel2);
        model3.addDAccDState(scr, dAccdPos3, dAccdVel3);
        model4.addDAccDState(scr, dAccdPos4, dAccdVel4);

        // Check all derivatives are null
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(0, dAccdPos[i][j], 0);
                Assert.assertEquals(0, dAccdPos2[i][j], 0);
                Assert.assertEquals(0, dAccdPos3[i][j], 0);
                Assert.assertEquals(0, dAccdPos4[i][j], 0);
                Assert.assertEquals(0, dAccdVel[i][j], 0);
                Assert.assertEquals(0, dAccdVel2[i][j], 0);
                Assert.assertEquals(0, dAccdVel3[i][j], 0);
                Assert.assertEquals(0, dAccdVel4[i][j], 0);
            }
        }

    }

    /**
     * @throws PatriusException if a perturbing celestial body cannot be built
     * @testType UT
     * 
     * @testedMethod {@link ForceModel#enrichParameterDescriptors()}
     * 
     * @description check that the parameters of this force model are well enriched with the
     *              {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor.
     * 
     * @testPassCriteria the {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor is
     *                   well contained in each parameter of the force model
     */
    @Test
    public void testEnrichParameterDescriptors() throws PatriusException {
        // PV REF
        final Vector3D pTest = new Vector3D(-2.04682492904072348e+06, -6.41308742588674650e+06,
                -1.00704149423014715e+04);
        final Vector3D vTest = new Vector3D(6.86418712311315812e+03, -2.18303740441877153e+03,
                1.02300507209407936e+01);
        new PVCoordinates(pTest, vTest);

        // mu from grim4s4_gr GRGS file (Earth)
        final double muEarth = 3.9860043770442E+14;

        // Earth equatorial radius from grim4s4_gr GRGS file
        final double requa = 6378136.;

        final List<CelestialBody> bodies = new ArrayList<CelestialBody>();
        bodies.add(CelestialBodyFactory.getSun());
        bodies.add(CelestialBodyFactory.getMoon());

        final TerrestrialTides forceModel = new TerrestrialTides(FramesFactory.getITRF(), requa,
                muEarth, bodies, false, false, false, new TerrestrialTidesDataProvider());

        // Check that the force model has some parameters (otherwise this test isn't needed and the
        // enrichParameterDescriptors method shouldn't be called in the force model)
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL));
        }
    }

    double[][] transpose(final double[][] d) {

        final double[][] dt = new double[d[0].length][d.length];

        for (int i = 0; i < d.length; i++) {
            for (int j = 0; j < d[i].length; j++) {
                dt[j][i] = d[i][j];
            }
        }

        return dt;

    }

    void print(final double[][] d) {
        for (final double[] row : d) {
            for (final double e : row) {
                System.out.printf("%.16e\t", e);
            }
            System.out.println();
        }
    }
}
