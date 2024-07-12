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
 * @history Created 10/02/2016
 *
 * HISTORY
 * VERSION:4.11:DM:DM-3235:22/05/2023:[PATRIUS][TEMPS_CALCUL] L'attitude des spacecraft state devrait etre initialisee de maniere lazy
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:555:10/02/2016:new solar activity data provider
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres.solarActivity;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.AeroModel;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000InputParameters;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ContinuousMSISE2000SolarData;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.drag.DragSensitive;
import fr.cnes.sirius.patrius.forces.gravity.BalminoGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test the {@link ExtendedSolarActivityWrapper} class
 *
 * @author Emmanuel Bignon
 *
 * @version $Id: ExtendedSolarActivityWrapperTest.java 17911 2017-09-11 12:02:31Z bignon $
 *
 * @since 3.2
 *
 */
public class ExtendedSolarActivityWrapperTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle ExtendedSolarActivityWrapper unit tests
         *
         * @featureDescription test solar activity provider ExtendedSolarActivityWrapper
         *
         * @coveredRequirements
         */
        EXTENDED_SOLAR_ACTIVITY_WRAPPER
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#EXTENDED_SOLAR_ACTIVITY_WRAPPER}
     *
     * @testedMethod {@link ExtendedSolarActivityWrapper#getAp(AbsoluteDate)}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getKp(AbsoluteDate)}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getInstantFlux(AbsoluteDate)}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getMaxDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getMinDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getFluxMinDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getFluxMaxDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getApKpMinDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getApKpMaxDate()}.
     *
     * @description this test tests all cases from all methods of {@link ExtendedSolarActivityWrapper} with positive
     *              average duration
     *
     * @input date, own-made solar activity provider {@link LocalSolarActivityProvider}, 10 days average duration
     *
     * @output solar and geomagnetic activity data
     *
     * @testPassCriteria solar activity and geomagnetic activity is as expected. Exact results are expected.
     *
     * @referenceVersion 3.2
     *
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testNominal() throws PatriusException {
        // Initialization
        final SolarActivityDataProvider innerProvider = new LocalSolarActivityProvider();
        final SolarActivityDataProvider provider = new ExtendedSolarActivityWrapper(innerProvider, 10 * 86400.);

        // Mean reference values
        final double meanF107A1 = 5 * 86400;
        final double meanF107A2 = 95 * 86400.;
        final double meanAp1 = 6 - 1. / 16.;
        final double meanAp2 = 94. - 1. / 16.;
        final double meanKp1 = SolarActivityToolbox.apToKp(meanAp1);
        final double meanKp2 = SolarActivityToolbox.apToKp(meanAp2);

        // Test
        genericTest(provider, meanF107A1, meanF107A2, meanAp1, meanAp2, meanKp1, meanKp2);
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#EXTENDED_SOLAR_ACTIVITY_WRAPPER}
     *
     * @testedMethod {@link ExtendedSolarActivityWrapper#getAp(AbsoluteDate)}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getKp(AbsoluteDate)}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getInstantFlux(AbsoluteDate)}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getMaxDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getMinDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getFluxMinDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getFluxMaxDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getApKpMinDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getApKpMaxDate()}.
     *
     * @description this test tests all cases from all methods of {@link ExtendedSolarActivityWrapper} with
     *              null/negative average duration
     *
     * @input date, own-made solar activity provider {@link LocalSolarActivityProvider}, 0 days average duration /
     *        negative average duration
     *
     * @output solar and geomagnetic activity data
     *
     * @testPassCriteria solar activity and geomagnetic activity is as expected. Exact results are expected.
     *
     * @referenceVersion 3.2
     *
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testNegativeOrNullDuration() throws PatriusException {
        // Initialization
        final SolarActivityDataProvider innerProvider = new LocalSolarActivityProvider();
        final SolarActivityDataProvider provider1 = new ExtendedSolarActivityWrapper(innerProvider, 0 * 86400.);
        final SolarActivityDataProvider provider2 = new ExtendedSolarActivityWrapper(innerProvider, -86400.);

        // Mean reference values
        final double meanF107A1 = 0 * 86400;
        final double meanF107A2 = 100 * 86400.;
        final double meanAp1 = 1.;
        final double meanAp2 = 99.;
        final double meanKp1 = SolarActivityToolbox.apToKp(meanAp1);
        final double meanKp2 = SolarActivityToolbox.apToKp(meanAp2);

        // Test
        genericTest(provider1, meanF107A1, meanF107A2, meanAp1, meanAp2, meanKp1, meanKp2);
        genericTest(provider2, meanF107A1, meanF107A2, meanAp1, meanAp2, meanKp1, meanKp2);
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#EXTENDED_SOLAR_ACTIVITY_WRAPPER}
     *
     * @testedMethod {@link ExtendedSolarActivityWrapper#getAp(AbsoluteDate)}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getKp(AbsoluteDate)}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getInstantFlux(AbsoluteDate)}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getMaxDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getMinDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getFluxMinDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getFluxMaxDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getApKpMinDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getApKpMaxDate()}.
     *
     * @description this test tests all cases from all methods of {@link ExtendedSolarActivityWrapper} with average
     *              duration larger than available data
     *
     * @input date, own-made solar activity provider {@link LocalSolarActivityProvider}, 10 days average duration larger
     *        than available data
     *
     * @output solar and geomagnetic activity data
     *
     * @testPassCriteria solar activity and geomagnetic activity is as expected. Exact results are expected.
     *
     * @referenceVersion 3.2
     *
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testDurationLargerThanData() throws PatriusException {
        // Initialization
        final SolarActivityDataProvider innerProvider = new LocalSolarActivityProvider();
        final SolarActivityDataProvider provider = new ExtendedSolarActivityWrapper(innerProvider, 200 * 86400.);

        // Mean reference values
        final double meanF107A1 = 50 * 86400;
        final double meanF107A2 = 50 * 86400.;
        final double meanAp1 = 50. - 1. / 16.;
        final double meanAp2 = 50. - 1. / 16.;
        final double meanKp1 = SolarActivityToolbox.apToKp(meanAp1);
        final double meanKp2 = SolarActivityToolbox.apToKp(meanAp2);

        // Test
        genericTest(provider, meanF107A1, meanF107A2, meanAp1, meanAp2, meanKp1, meanKp2);
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     * @testedFeature {@link features#EXTENDED_SOLAR_ACTIVITY_WRAPPER}
     *
     * @testedMethod {@link ExtendedSolarActivityWrapper#getAp(AbsoluteDate)}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getKp(AbsoluteDate)}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getInstantFlux(AbsoluteDate)}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getMaxDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getMinDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getFluxMinDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getFluxMaxDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getApKpMinDate()}.
     * @testedMethod {@link ExtendedSolarActivityWrapper#getApKpMaxDate()}.
     *
     * @description this test tests all cases from all methods of {@link ExtendedSolarActivityWrapper} with positive
     *              average duration and {@link ConstantSolarActivity} solar activity
     *
     * @input date, own-made solar activity provider {@link ConstantSolarActivity}, 10 days average duration
     *
     * @output solar and geomagnetic activity data
     *
     * @testPassCriteria solar activity and geomagnetic activity is as expected. Exact results are expected.
     *
     * @referenceVersion 3.2
     *
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testConstantSolarActivity() throws PatriusException {

        // Mean reference values
        final double meanF107A = 100;
        final double meanAp = 1;
        final double meanKp = SolarActivityToolbox.apToKp(meanAp);

        // Initialization
        final SolarActivityDataProvider innerProvider = new ConstantSolarActivity(meanF107A, meanAp);
        final SolarActivityDataProvider provider = new ExtendedSolarActivityWrapper(innerProvider, 10 * 86400.);

        // Test

        // ===== Check single values (several values) =====

        // F10.7
        Assert.assertEquals(meanF107A, provider.getInstantFluxValue(AbsoluteDate.J2000_EPOCH.shiftedBy(-150000.)), 0.);
        Assert.assertEquals(meanF107A, provider.getInstantFluxValue(AbsoluteDate.J2000_EPOCH.shiftedBy(10. * 86400.)),
            0.);
        Assert.assertEquals(meanF107A,
            provider.getInstantFluxValue(AbsoluteDate.J2000_EPOCH.shiftedBy(100. * 86400. + 150000)), 0.);

        // Ap
        Assert.assertEquals(meanAp, provider.getAp(AbsoluteDate.J2000_EPOCH.shiftedBy(-150000.)), 0.);
        Assert.assertEquals(meanAp, provider.getAp(AbsoluteDate.J2000_EPOCH.shiftedBy(10. * 86400.)), 0.);
        Assert.assertEquals(meanAp, provider.getAp(AbsoluteDate.J2000_EPOCH.shiftedBy(100. * 86400. + 150000)), 0.);

        // Kp
        Assert.assertEquals(meanKp, provider.getKp(AbsoluteDate.J2000_EPOCH.shiftedBy(-150000.)), 0.);
        Assert.assertEquals(meanKp, provider.getKp(AbsoluteDate.J2000_EPOCH.shiftedBy(10. * 86400.)), 0.);
        Assert.assertEquals(meanKp, provider.getKp(AbsoluteDate.J2000_EPOCH.shiftedBy(100. * 86400. + 150000)), 0.);

        // ===== Check multiple values (several values) =====

        // F10.7
        final SortedMap<AbsoluteDate, Double> f107 = provider.getInstantFluxValues(AbsoluteDate.J2000_EPOCH,
            AbsoluteDate.J2000_EPOCH.shiftedBy(86400));
        Assert.assertEquals(2, f107.size());
        Assert.assertEquals(meanF107A, f107.get(AbsoluteDate.J2000_EPOCH), 0.);
        Assert.assertEquals(meanF107A, f107.get(AbsoluteDate.J2000_EPOCH.shiftedBy(86400)), 0.);

        // Ap/Kp
        final SortedMap<AbsoluteDate, Double[]> apkp = provider.getApKpValues(AbsoluteDate.J2000_EPOCH,
            AbsoluteDate.J2000_EPOCH.shiftedBy(86400));
        Assert.assertEquals(2, apkp.size());
        Assert.assertEquals(meanAp, apkp.get(AbsoluteDate.J2000_EPOCH)[0], 0.);
        Assert.assertEquals(meanAp, apkp.get(AbsoluteDate.J2000_EPOCH.shiftedBy(86400))[0], 0.);
        Assert.assertEquals(meanKp, apkp.get(AbsoluteDate.J2000_EPOCH)[1], 0.);
        Assert.assertEquals(meanKp, apkp.get(AbsoluteDate.J2000_EPOCH.shiftedBy(86400))[1], 0.);

        // ===== Check limit dates =====

        Assert.assertTrue(provider.getMinDate().equals(AbsoluteDate.PAST_INFINITY));
        Assert.assertTrue(provider.getMaxDate().equals(AbsoluteDate.FUTURE_INFINITY));
        Assert.assertTrue(provider.getFluxMinDate().equals(AbsoluteDate.PAST_INFINITY));
        Assert.assertTrue(provider.getFluxMaxDate().equals(AbsoluteDate.FUTURE_INFINITY));
        Assert.assertTrue(provider.getApKpMinDate().equals(AbsoluteDate.PAST_INFINITY));
        Assert.assertTrue(provider.getApKpMaxDate().equals(AbsoluteDate.FUTURE_INFINITY));
    }

    /**
     * Generic test with local solar activity provider
     *
     * @param provider
     *        solar activity provider
     * @param meanF107A1
     *        F10.7 value before underlying provider validity interval
     * @param meanF107A2
     *        F10.7 value after underlying provider validity interval
     * @param meanAp1
     *        Ap value before underlying provider validity interval
     * @param meanAp2
     *        Ap value after underlying provider validity interval
     * @param meanKp1
     *        Kp value before underlying provider validity interval
     * @param meanKp2
     *        Kp value after underlying provider validity interval
     * @throws PatriusException
     */
    private static void genericTest(final SolarActivityDataProvider provider, final double meanF107A1,
                                    final double meanF107A2, final double meanAp1, final double meanAp2,
                                    final double meanKp1, final double meanKp2) throws PatriusException {

        // ===== Check single values (before, on first bound of, within, on last bound of, after interval) =====

        // F10.7
        Assert.assertEquals(meanF107A1, provider.getInstantFluxValue(AbsoluteDate.J2000_EPOCH.shiftedBy(-150000.)), 0.);
        Assert.assertEquals(0., provider.getInstantFluxValue(AbsoluteDate.J2000_EPOCH.shiftedBy(0.)), 0.);
        Assert.assertEquals(10 * 86400.,
            provider.getInstantFluxValue(AbsoluteDate.J2000_EPOCH.shiftedBy(10. * 86400.)), 0.);
        Assert.assertEquals(100 * 86400.,
            provider.getInstantFluxValue(AbsoluteDate.J2000_EPOCH.shiftedBy(100. * 86400.)), 0.);
        Assert.assertEquals(meanF107A2,
            provider.getInstantFluxValue(AbsoluteDate.J2000_EPOCH.shiftedBy(100. * 86400. + 150000)), 0.);

        // Ap
        Assert.assertEquals(meanAp1, provider.getAp(AbsoluteDate.J2000_EPOCH.shiftedBy(-150000.)), 0.);
        Assert.assertEquals(1., provider.getAp(AbsoluteDate.J2000_EPOCH.shiftedBy(1. * 86400)), 0.);
        Assert.assertEquals(10., provider.getAp(AbsoluteDate.J2000_EPOCH.shiftedBy(10. * 86400.)), 0.);
        Assert.assertEquals(99., provider.getAp(AbsoluteDate.J2000_EPOCH.shiftedBy(99. * 86400.)), 0.);
        Assert.assertEquals(meanAp2, provider.getAp(AbsoluteDate.J2000_EPOCH.shiftedBy(100. * 86400. + 150000)), 0.);

        // Kp
        Assert.assertEquals(meanKp1, provider.getKp(AbsoluteDate.J2000_EPOCH.shiftedBy(-150000.)), 0.);
        Assert.assertEquals(SolarActivityToolbox.apToKp(1.),
            provider.getKp(AbsoluteDate.J2000_EPOCH.shiftedBy(1. * 86400)), 0.);
        Assert.assertEquals(SolarActivityToolbox.apToKp(10.),
            provider.getKp(AbsoluteDate.J2000_EPOCH.shiftedBy(10. * 86400.)), 0.);
        Assert.assertEquals(SolarActivityToolbox.apToKp(99.),
            provider.getKp(AbsoluteDate.J2000_EPOCH.shiftedBy(99. * 86400.)), 0.);
        Assert.assertEquals(meanKp2, provider.getKp(AbsoluteDate.J2000_EPOCH.shiftedBy(100. * 86400. + 150000)), 0.);

        // ===== Check multiple values (before, astride left of, within, astride right of, after interval) =====

        // F10.7
        final SortedMap<AbsoluteDate, Double> f107Before = provider.getInstantFluxValues(
            AbsoluteDate.J2000_EPOCH.shiftedBy(-250000), AbsoluteDate.J2000_EPOCH.shiftedBy(-150000));
        Assert.assertEquals(2, f107Before.size());
        Assert.assertEquals(meanF107A1, f107Before.get(AbsoluteDate.J2000_EPOCH.shiftedBy(-250000)), 0.);
        Assert.assertEquals(meanF107A1, f107Before.get(AbsoluteDate.J2000_EPOCH.shiftedBy(-150000)), 0.);

        final SortedMap<AbsoluteDate, Double> f107AstrideLeft = provider.getInstantFluxValues(
            AbsoluteDate.J2000_EPOCH.shiftedBy(-86400), AbsoluteDate.J2000_EPOCH.shiftedBy(86400));
        Assert.assertEquals(3, f107AstrideLeft.size());
        Assert.assertEquals(meanF107A1, f107AstrideLeft.get(AbsoluteDate.J2000_EPOCH.shiftedBy(-1. * 86400)), 0.);
        Assert.assertEquals(0. * 86400., f107AstrideLeft.get(AbsoluteDate.J2000_EPOCH.shiftedBy(0. * 86400)), 0.);
        Assert.assertEquals(1. * 86400., f107AstrideLeft.get(AbsoluteDate.J2000_EPOCH.shiftedBy(1. * 86400)), 0.);

        final SortedMap<AbsoluteDate, Double> f107Within = provider.getInstantFluxValues(
            AbsoluteDate.J2000_EPOCH.shiftedBy(86400), AbsoluteDate.J2000_EPOCH.shiftedBy(3. * 86400));
        Assert.assertEquals(3, f107Within.size());
        Assert.assertEquals(1. * 86400., f107Within.get(AbsoluteDate.J2000_EPOCH.shiftedBy(1. * 86400)), 0.);
        Assert.assertEquals(2. * 86400., f107Within.get(AbsoluteDate.J2000_EPOCH.shiftedBy(2. * 86400)), 0.);
        Assert.assertEquals(3. * 86400., f107Within.get(AbsoluteDate.J2000_EPOCH.shiftedBy(3. * 86400)), 0.);

        final SortedMap<AbsoluteDate, Double> f107AstrideRight = provider.getInstantFluxValues(
            AbsoluteDate.J2000_EPOCH.shiftedBy(99 * 86400), AbsoluteDate.J2000_EPOCH.shiftedBy(101 * 86400));
        Assert.assertEquals(3, f107AstrideRight.size());
        Assert.assertEquals(99. * 86400., f107AstrideRight.get(AbsoluteDate.J2000_EPOCH.shiftedBy(99. * 86400)), 0.);
        Assert.assertEquals(100. * 86400., f107AstrideRight.get(AbsoluteDate.J2000_EPOCH.shiftedBy(100. * 86400)), 0.);
        Assert.assertEquals(meanF107A2, f107AstrideRight.get(AbsoluteDate.J2000_EPOCH.shiftedBy(101. * 86400)), 0.);

        final SortedMap<AbsoluteDate, Double> f107After = provider.getInstantFluxValues(
            AbsoluteDate.J2000_EPOCH.shiftedBy(100 * 86400 + 150000),
            AbsoluteDate.J2000_EPOCH.shiftedBy(100 * 86400 + 250000));
        Assert.assertEquals(2, f107After.size());
        Assert.assertEquals(meanF107A2, f107After.get(AbsoluteDate.J2000_EPOCH.shiftedBy(100 * 86400 + 150000)), 0.);
        Assert.assertEquals(meanF107A2, f107After.get(AbsoluteDate.J2000_EPOCH.shiftedBy(100 * 86400 + 250000)), 0.);

        // Ap/Kp
        final SortedMap<AbsoluteDate, Double[]> apkpBefore = provider.getApKpValues(
            AbsoluteDate.J2000_EPOCH.shiftedBy(-250000), AbsoluteDate.J2000_EPOCH.shiftedBy(-150000));
        Assert.assertEquals(2, apkpBefore.size());
        Assert.assertEquals(meanAp1, apkpBefore.get(AbsoluteDate.J2000_EPOCH.shiftedBy(-250000))[0], 0.);
        Assert.assertEquals(meanAp1, apkpBefore.get(AbsoluteDate.J2000_EPOCH.shiftedBy(-150000))[0], 0.);
        Assert.assertEquals(meanKp1, apkpBefore.get(AbsoluteDate.J2000_EPOCH.shiftedBy(-250000))[1], 0.);
        Assert.assertEquals(meanKp1, apkpBefore.get(AbsoluteDate.J2000_EPOCH.shiftedBy(-150000))[1], 0.);

        final SortedMap<AbsoluteDate, Double[]> apkpAstrideLeft = provider.getApKpValues(
            AbsoluteDate.J2000_EPOCH.shiftedBy(0), AbsoluteDate.J2000_EPOCH.shiftedBy(2 * 86400));
        Assert.assertEquals(17, apkpAstrideLeft.size());
        for (int i = 0; i < 8; i++) {
            Assert.assertEquals(meanAp1,
                apkpAstrideLeft.get(AbsoluteDate.J2000_EPOCH.shiftedBy(0. * 86400 + 3 * 3600 * i))[0], 0.);
            Assert.assertEquals(meanKp1,
                apkpAstrideLeft.get(AbsoluteDate.J2000_EPOCH.shiftedBy(0. * 86400 + 3 * 3600 * i))[1], 0.);
        }
        for (int i = 0; i <= 8; i++) {
            Assert.assertEquals(1. + i / 8.,
                apkpAstrideLeft.get(AbsoluteDate.J2000_EPOCH.shiftedBy(1. * 86400 + 3 * 3600 * i))[0], 0.);
            Assert.assertEquals(SolarActivityToolbox.apToKp(1. + i / 8.),
                apkpAstrideLeft.get(AbsoluteDate.J2000_EPOCH.shiftedBy(1. * 86400 + 3 * 3600 * i))[1], 0.);
        }

        final SortedMap<AbsoluteDate, Double[]> apkpWithin = provider.getApKpValues(
            AbsoluteDate.J2000_EPOCH.shiftedBy(2 * 86400), AbsoluteDate.J2000_EPOCH.shiftedBy(3. * 86400));
        Assert.assertEquals(9, apkpWithin.size());
        for (int i = 0; i <= 8; i++) {
            Assert.assertEquals(2. + i / 8.,
                apkpWithin.get(AbsoluteDate.J2000_EPOCH.shiftedBy(2. * 86400 + 3 * 3600 * i))[0], 0.);
            Assert.assertEquals(SolarActivityToolbox.apToKp(2. + i / 8.),
                apkpWithin.get(AbsoluteDate.J2000_EPOCH.shiftedBy(2. * 86400 + 3 * 3600 * i))[1], 0.);
        }

        final SortedMap<AbsoluteDate, Double[]> apkpAstrideRight = provider.getApKpValues(
            AbsoluteDate.J2000_EPOCH.shiftedBy(98 * 86400), AbsoluteDate.J2000_EPOCH.shiftedBy(100 * 86400));
        Assert.assertEquals(17, apkpAstrideRight.size());
        for (int i = 0; i <= 8; i++) {
            Assert.assertEquals(98. + i / 8.,
                apkpAstrideRight.get(AbsoluteDate.J2000_EPOCH.shiftedBy(98. * 86400 + 3 * 3600 * i))[0], 0.);
            Assert.assertEquals(SolarActivityToolbox.apToKp(98. + i / 8.),
                apkpAstrideRight.get(AbsoluteDate.J2000_EPOCH.shiftedBy(98. * 86400 + 3 * 3600 * i))[1], 0.);
        }
        for (int i = 1; i <= 8; i++) {
            Assert.assertEquals(meanAp2,
                apkpAstrideRight.get(AbsoluteDate.J2000_EPOCH.shiftedBy(99. * 86400 + 3 * 3600 * i))[0], 0.);
            Assert.assertEquals(meanKp2,
                apkpAstrideRight.get(AbsoluteDate.J2000_EPOCH.shiftedBy(99. * 86400 + 3 * 3600 * i))[1], 0.);
        }

        final SortedMap<AbsoluteDate, Double[]> apkpAfter = provider.getApKpValues(
            AbsoluteDate.J2000_EPOCH.shiftedBy(100 * 86400 + 150000),
            AbsoluteDate.J2000_EPOCH.shiftedBy(100 * 86400 + 250000));
        Assert.assertEquals(2, apkpAfter.size());
        Assert.assertEquals(meanAp2, apkpAfter.get(AbsoluteDate.J2000_EPOCH.shiftedBy(100 * 86400 + 150000))[0], 0.);
        Assert.assertEquals(meanAp2, apkpAfter.get(AbsoluteDate.J2000_EPOCH.shiftedBy(100 * 86400 + 250000))[0], 0.);
        Assert.assertEquals(meanKp2, apkpAfter.get(AbsoluteDate.J2000_EPOCH.shiftedBy(100 * 86400 + 150000))[1], 0.);
        Assert.assertEquals(meanKp2, apkpAfter.get(AbsoluteDate.J2000_EPOCH.shiftedBy(100 * 86400 + 250000))[1], 0.);

        // ===== Check limit dates =====

        Assert.assertTrue(provider.getMinDate().equals(AbsoluteDate.PAST_INFINITY));
        Assert.assertTrue(provider.getMaxDate().equals(AbsoluteDate.FUTURE_INFINITY));
        Assert.assertTrue(provider.getFluxMinDate().equals(AbsoluteDate.PAST_INFINITY));
        Assert.assertTrue(provider.getFluxMaxDate().equals(AbsoluteDate.FUTURE_INFINITY));
        Assert.assertTrue(provider.getApKpMinDate().equals(AbsoluteDate.PAST_INFINITY));
        Assert.assertTrue(provider.getApKpMaxDate().equals(AbsoluteDate.FUTURE_INFINITY));
    }

    /**
     * Local solar activity provider.
     * <ul>
     * <li>F10.7 defined between AbsoluteDate.J2000_EPOCH and AbsoluteDate.J2000_EPOCH + 100 days</li>
     * <li>Ap/Kp defined between AbsoluteDate.J2000_EPOCH + 1 day and AbsoluteDate.J2000_EPOCH + 99 days</li>
     * <li>Flux sampled every day, (fake) flux being equals to duration from AbsoluteDate.J2000_EPOCH.</li>
     * <li>Ap/Kp sampled every 3h, (fake) Ap/Kp being equals to duration from AbsoluteDate.J2000_EPOCH in days.</li>
     * </ul>
     *
     * @author Emmanuel Bignon
     *
     */
    private class LocalSolarActivityProvider implements SolarActivityDataProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = 8562481659986215938L;
        private final AbsoluteDate minF107Date = AbsoluteDate.J2000_EPOCH;
        private final AbsoluteDate maxF107Date = AbsoluteDate.J2000_EPOCH.shiftedBy(86400. * 100);
        private final AbsoluteDate minApKpDate = AbsoluteDate.J2000_EPOCH.shiftedBy(86400. * 1);
        private final AbsoluteDate maxApKpDate = AbsoluteDate.J2000_EPOCH.shiftedBy(86400. * 99);
        private final SortedMap<AbsoluteDate, Double> mapF107;
        private final SortedMap<AbsoluteDate, Double[]> mapApKp;

        public LocalSolarActivityProvider() {
            this.mapF107 = new TreeMap<>();
            this.mapApKp = new TreeMap<>();
            AbsoluteDate date = this.minF107Date;
            while (date.durationFrom(this.maxF107Date) <= 0) {
                this.mapF107.put(date, date.durationFrom(AbsoluteDate.J2000_EPOCH));
                date = date.shiftedBy(86400.);
            }
            date = this.minApKpDate;
            while (date.durationFrom(this.maxApKpDate) <= 0) {
                final double ap = date.durationFrom(AbsoluteDate.J2000_EPOCH) / 86400.;
                this.mapApKp.put(date, new Double[] { ap, SolarActivityToolbox.apToKp(ap) });
                date = date.shiftedBy(3. * 3600.);
            }
        }

        @Override
        public AbsoluteDate getMinDate() {
            return this.minApKpDate;
        }

        @Override
        public AbsoluteDate getMaxDate() {
            return this.maxApKpDate;
        }

        @Override
        public AbsoluteDate getFluxMinDate() {
            return this.minF107Date;
        }

        @Override
        public AbsoluteDate getFluxMaxDate() {
            return this.maxF107Date;
        }

        @Override
        public AbsoluteDate getApKpMinDate() {
            return this.minApKpDate;
        }

        @Override
        public AbsoluteDate getApKpMaxDate() {
            return this.maxApKpDate;
        }

        @Override
        public SortedMap<AbsoluteDate, Double>
            getInstantFluxValues(final AbsoluteDate date1, final AbsoluteDate date2)
                throws PatriusException {
            if (date1.durationFrom(this.minF107Date) < 0 || date2.durationFrom(this.maxF107Date) > 0) {
                throw new PatriusException(new MathIllegalStateException());
            }
            return this.mapF107.subMap(date1.shiftedBy(-1E-6), date2.shiftedBy(1E-6));
        }

        @Override
        public double getInstantFluxValue(final AbsoluteDate date) throws PatriusException {
            if (date.durationFrom(this.minF107Date) >= 0 && date.durationFrom(this.maxF107Date) <= 0) {
                return date.durationFrom(AbsoluteDate.J2000_EPOCH);
            }
                throw PatriusException.createInternalError(null);
        }

        @Override
        public SortedMap<AbsoluteDate, Double[]>
            getApKpValues(final AbsoluteDate date1, final AbsoluteDate date2)
                throws PatriusException {
            if (date1.durationFrom(this.minApKpDate) < 0 || date2.durationFrom(this.maxApKpDate) > 0) {
                throw new PatriusException(new MathIllegalStateException());
            }
            return this.mapApKp.subMap(date1.shiftedBy(-1E-6), date2.shiftedBy(1E-6));
        }

        @Override
        public double getAp(final AbsoluteDate date) throws PatriusException {
            if (date.durationFrom(this.minApKpDate) >= 0 && date.durationFrom(this.maxApKpDate) <= 0) {
                return date.durationFrom(AbsoluteDate.J2000_EPOCH) / 86400.;
            }
                throw PatriusException.createInternalError(null);
        }

        @Override
        public double getKp(final AbsoluteDate date) throws PatriusException {
            return SolarActivityToolbox.apToKp(this.getAp(date));
        }

        @Override
        public double getStepApKp() throws PatriusException {
            return 3 * 3600.;
        }

        @Override
        public double getStepF107() throws PatriusException {
            return 86400.;
        }
    }

    /**
     * FA-3009: short acsol file. Extension of solar activity should not throw any exception.
     */
    @Test
    public void testPropagationShortACSOL() throws PatriusException, IOException, ParseException {
        Utils.setDataRoot("shortACSOL");

        // Create mass model
        final Assembly assembly = createAssembly();
        final MassModel massProv1 = new MassModel(assembly);

        // Initial state
        final Frame gcrf = FramesFactory.getGCRF();
        final CartesianOrbit initialOrbit = createInitOrbit(gcrf);

        // Attitude provider
        final AttitudeProvider attitudeProv = new ConstantAttitudeLaw(gcrf, Rotation.IDENTITY);
        final SpacecraftState initialState = new SpacecraftState(attitudeProv, attitudeProv, initialOrbit, massProv1);

        // Create propagator
        final NumericalPropagator prop = new NumericalPropagator(createDopri(), initialState.getFrame(),
            OrbitType.CARTESIAN, PositionAngle.TRUE);
        prop.setSlaveMode();

        prop.setMassProviderEquation(massProv1);

        prop.setAttitudeProvider(attitudeProv);

        prop.setInitialState(initialState);

        // Add the force models to the propagator
        final List<ForceModel> forceList = new ArrayList<>();
        forceList.add(getPotential(30, 30));
        forceList.add(getDrag(assembly));
        for (final ForceModel model : forceList) {
            prop.addForceModel(model);
        }

        // Propagate one day and get end SV
        final AbsoluteDate tf = initialOrbit.getDate().shiftedBy(86400.0);
        final PVCoordinates endSV = prop.getPVCoordinates(tf, gcrf);

        // Do checks
        Assert.assertNotNull(endSV);
    }

    private static CartesianOrbit createInitOrbit(final Frame gcrf) {

        /**
         * ATTENTION!!!
         */
        // The only value that matters in this test is the initialDate
        // The solar activity file contains data in the interval [2008-11-29T00:00:33.000, 2011-03-13T00:00:34.000]
        // We need a case where the value of "minDate" that is used in the call to SolarActivityToolBox.getMeanAp
        // is the same as the end date of the solar activity data interval.
        // This method is called several times from ContinuousMSISE2000SolarData.getApValues, the second one
        // using a "minDate" which is the input date (initial date for the first point) shifted backwards 1.5 hours
        // So let's use as initial date of our orbit solarActivityDataMaxDate + 1.5 hours
        final AbsoluteDate initDate = new AbsoluteDate(2011, 03, 13, 01, 30, 34.0, TimeScalesFactory.getTAI());
        final Vector3D initPos = new Vector3D(7.0E06, 0.0, 0.0);
        final Vector3D initVel = new Vector3D(0.0, 7.5E03, 1.0);
        final PVCoordinates pvCoord = new PVCoordinates(initPos, initVel);

        return new CartesianOrbit(pvCoord, gcrf, initDate, Constants.WGS84_EARTH_MU);
    }

    private static Assembly createAssembly() throws PatriusException {

        final AssemblyBuilder assemblyBuilder = new AssemblyBuilder();

        final String nameMainPart = "mci";
        assemblyBuilder.addMainPart(nameMainPart);
        final MassProperty property = new MassProperty(500.0);
        assemblyBuilder.addProperty(property, nameMainPart);
        assemblyBuilder.addProperty(new AeroSphereProperty(1.0, 2.2), nameMainPart);

        final String nameTank = "tank";
        assemblyBuilder.addPart(nameTank, nameMainPart, Transform.IDENTITY);
        final MassProperty property2 = new MassProperty(10.0);
        assemblyBuilder.addProperty(property2, nameTank);

        return assemblyBuilder.returnAssembly();
    }

    private static FirstOrderIntegrator createDopri() {

        final double posTol = 1.0E-05;
        final double velTol = 1.0E-08;
        final double relTol = 1.0E-07;

        final double[] absTolerance = { posTol, posTol, posTol, velTol, velTol, velTol };
        final double[] relTolerance = { relTol, relTol, relTol, relTol, relTol, relTol };

        return new DormandPrince853Integrator(1.0E-03, 120.0, absTolerance, relTolerance);
    }

    private static ForceModel getPotential(final int n, final int m)
        throws IOException, PatriusException, ParseException {

        // add a reader for gravity fields
        final GRGSFormatReader potReader = new GRGSFormatReader("", true);
        potReader.loadData(getGrim4s4_gr().openStream(), "grim4s4_gr");
        GravityFieldFactory.addPotentialCoefficientsReader(potReader);

        // get the gravity field coefficients provider from the 'grim4s4_gr' file
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();

        // we get the data as extracted from the file
        final double[][] C = provider.getC(n, m, true);
        final double[][] S = provider.getS(n, m, true);

        // return perturbing force (ITRF2008 central body frame)
        return new DirectBodyAttraction(new BalminoGravityModel(FramesFactory.getITRF(), provider.getAe(),
            provider.getMu(), C, S));
    }

    private static ForceModel getDrag(final Assembly assembly) throws PatriusException, IOException, ParseException {

        final ACSOLFormatReader solarDataProvider = new ACSOLFormatReader("ACSOL*.act");
        solarDataProvider.loadData(getSolarAct().openStream(), "PatriusSolarData");
        final ExtendedSolarActivityWrapper extendedSolarData = new ExtendedSolarActivityWrapper(solarDataProvider, 0.0);

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF());

        final MSISE2000InputParameters data = new ContinuousMSISE2000SolarData(extendedSolarData);
        final MSISE2000 atm = new MSISE2000(data, earth, new MeeusSun());

        final DragSensitive spacecraft = new AeroModel(assembly, atm, earth);
        return new DragForce(atm, spacecraft);
    }

    private static URL getGrim4s4_gr() {
        return ExtendedSolarActivityWrapperTest.class.getClassLoader().getResource("shortACSOL/grim4s4_gr");
    }

    private static URL getSolarAct() {
        return ExtendedSolarActivityWrapperTest.class.getClassLoader().getResource("shortACSOL/ACSOL_limited.act");
    }
}
