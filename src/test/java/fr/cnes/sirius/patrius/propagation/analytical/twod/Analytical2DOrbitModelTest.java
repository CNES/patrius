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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2450:27/01/2021:[PATRIUS] moyennage au sens du modele Analytical2D 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:94:30/09/2013:2D propagator update
 * VERSION::DM:211:08/04/2014:Modified analytical 2D propagator
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:345:31/10/2014: coverage
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:266:29/04/2015:add various centered analytical models
 * VERSION::FA:556:24/02/2016:change max orders vs dev orders
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical.twod;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * 
 * @author houdroger
 * 
 * @version $Id: Analytical2DOrbitModelTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 */
public class Analytical2DOrbitModelTest {

    private final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

    private final String loc = "analytical2Dvalidation" + File.separator + "input" + File.separator;
    private final String exF = "test_2D_coef_ex.txt";
    private final String eyF = "test_2D_coef_ey.txt";
    private final String aF = "test_2D_coef_sma.txt";
    private final String iF = "test_2D_coef_inc.txt";
    private final String rF = "test_2D_coef_lna.txt";
    private final String lF = "test_2D_coef_psoM.txt";

    private final ParameterModelReader aReader = new ParameterModelReader();
    private final ParameterModelReader exReader = new ParameterModelReader();
    private final ParameterModelReader eyReader = new ParameterModelReader();
    private final ParameterModelReader iReader = new ParameterModelReader();
    private final ParameterModelReader rReader = new ParameterModelReader();
    private final ParameterModelReader lReader = new ParameterModelReader();

    private Analytical2DParameterModel smaModel;
    private Analytical2DParameterModel exModel;
    private Analytical2DParameterModel eyModel;
    private Analytical2DParameterModel incModel;
    private Analytical2DParameterModel lnaModel;
    private Analytical2DParameterModel aolModel;

    private Analytical2DOrbitModel model;

    private AbsoluteDate date;

    private AbsoluteDate ref;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the orbit models
         * 
         * @featureDescription Validate the orbit model
         * 
         * @coveredRequirements DV-PROPAG_10, DV-PROPAG_20
         */
        ANALYTICAL_2D_ORBIT_MODEL
    }

    @Before
    public void setup() throws URISyntaxException, IOException, PatriusException {
        Utils.setDataRoot("analytical2Dvalidation");
        FramesFactory.setConfiguration(Utils.getIERS2010Configuration());

        this.aReader.readData(this.loc + this.aF);
        this.iReader.readData(this.loc + this.iF);
        this.exReader.readData(this.loc + this.exF);
        this.eyReader.readData(this.loc + this.eyF);
        this.rReader.readData(this.loc + this.rF);
        this.lReader.readData(this.loc + this.lF);

        this.smaModel = this.aReader.getModel();
        this.exModel = this.exReader.getModel();
        this.eyModel = this.eyReader.getModel();
        this.incModel = this.iReader.getModel();
        this.lnaModel = this.rReader.getModel();
        this.aolModel = this.lReader.getModel();

        this.ref = new AbsoluteDate(this.aReader.getDate(), TimeScalesFactory.getTAI());
        this.model =
            new Analytical2DOrbitModel(this.smaModel, this.exModel, this.eyModel, this.incModel, this.lnaModel,
                this.aolModel,
                Constants.EGM96_EARTH_MU);

        this.date = this.ref.shiftedBy(1000.);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANALYTICAL_2D_ORBIT_MODEL}
     * 
     * @testedMethod {@link Analytical2DOrbitModel#getAolModel()}
     * @testedMethod {@link Analytical2DOrbitModel#getSmaModel()}
     * @testedMethod {@link Analytical2DOrbitModel#getIncModel()}
     * @testedMethod {@link Analytical2DOrbitModel#getLnaModel()}
     * @testedMethod {@link Analytical2DOrbitModel#getExModel()}
     * @testedMethod {@link Analytical2DOrbitModel#getEyModel()}
     * @testedMethod {@link Analytical2DOrbitModel#getParameterModels()}
     * @testedMethod {@link Analytical2DOrbitModel#getMu}
     * @testedMethod {@link Analytical2DOrbitModel#getMaxOrders()}
     * @testedMethod {@link Analytical2DOrbitModel#getDevelopmentOrders()}
     * 
     * @description test the getters
     * 
     * @input parameters
     * 
     * @output parameter models, mu
     * 
     * @testPassCriteria attributes are exactly as expected
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testAnalytical2DOrbitModelGetter() {

        // Check mu
        Assert.assertEquals(Constants.EGM96_EARTH_MU, this.model.getMu(), 0.);

        // Check maxm trigonometric orders
        this.checkArrays(this.model.getMaxOrders(), new int[] { 83, 153, 158, 65, 47, 141 });

        // Check development trigonometric orders
        this.checkArrays(this.model.getDevelopmentOrders(), new int[] { 83, 153, 158, 65, 47, 141 });

        // Check models
        this.compareModels(this.smaModel, this.model.getSmaModel(), this.eps);
        this.compareModels(this.exModel, this.model.getExModel(), this.eps);
        this.compareModels(this.eyModel, this.model.getEyModel(), this.eps);
        this.compareModels(this.incModel, this.model.getIncModel(), this.eps);
        this.compareModels(this.lnaModel, this.model.getLnaModel(), this.eps);
        this.compareModels(this.aolModel, this.model.getAolModel(), this.eps);

        // Check models array
        final Analytical2DParameterModel[] result = this.model.getParameterModels();
        this.compareModels(this.smaModel, result[0], this.eps);
        this.compareModels(this.exModel, result[1], this.eps);
        this.compareModels(this.eyModel, result[2], this.eps);
        this.compareModels(this.incModel, result[3], this.eps);
        this.compareModels(this.lnaModel, result[4], this.eps);
        this.compareModels(this.aolModel, result[5], this.eps);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ANALYTICAL_2D_ORBIT_MODEL}
     * 
     * @testedMethod {@link Analytical2DOrbitModel#Analytical2DOrbitModel(AbsoluteDate, Analytical2DParameterModel, Analytical2DParameterModel, Analytical2DParameterModel, Analytical2DParameterModel, Analytical2DParameterModel, Analytical2DParameterModel, boolean, double, double)}
     * @testedMethod {@link Analytical2DOrbitModel#Analytical2DOrbitModel(AbsoluteDate, Analytical2DParameterModel, Analytical2DParameterModel, Analytical2DParameterModel, Analytical2DParameterModel, Analytical2DParameterModel, Analytical2DParameterModel, boolean, int[], double, double)}
     * 
     * @description test the constructors
     * 
     * @input parameters
     * 
     * @output nothing
     * 
     * @testPassCriteria exceptions were expected
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testAnalytical2DOrbitModelConstructors() {

        new Analytical2DOrbitModel(this.smaModel, this.exModel, this.eyModel, this.incModel, this.lnaModel,
            this.aolModel, this.model.getMaxOrders(),
            Constants.EGM96_EARTH_MU);

        final MassProvider defaultMass = new SimpleMassModel(1000, "default");
        new Analytical2DOrbitModel(this.smaModel, this.exModel, this.eyModel, this.incModel, this.lnaModel,
            this.aolModel, this.model.getMaxOrders(),
            defaultMass, Constants.EGM96_EARTH_MU);

        // Inconsistent orders
        try {
            new Analytical2DOrbitModel(this.smaModel, this.exModel, this.eyModel, this.incModel, this.lnaModel,
                this.aolModel, new int[13],
                Constants.EGM96_EARTH_MU);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            new Analytical2DOrbitModel(this.smaModel, this.exModel, this.eyModel, this.incModel, this.lnaModel,
                this.aolModel, new int[] { 0, 0, 0,
                    0, -1, 0 }, Constants.EGM96_EARTH_MU);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#ANALYTICAL_2D_ORBIT_MODEL}
     * 
     * @testedMethod {@link Analytical2DOrbitModel#propagateModel(AbsoluteDate)}
     * 
     * @description test the propagate method
     * 
     * @input a date
     * 
     * @output 6 circular parameters
     * 
     * @testPassCriteria results are as expected, to 2e-14 on a relative scale
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testPropagateModel() throws PatriusException {

        this.model =
            new Analytical2DOrbitModel(this.smaModel, this.exModel, this.eyModel, this.incModel, this.lnaModel,
                this.aolModel,
                Constants.EGM96_EARTH_MU);

        final double[] act = this.model.propagateModel(this.date);
        final double[] exp = { 7173631.0539035560, -0.00087881920576288180, 0.00038996201807501934, 1.7214033229710324,
            -0.99625893456226920, 1.0400756526021195 };

        for (int i = 0; i < exp.length; i++) {
            Assert.assertEquals("element " + i, 0, (exp[i] - act[i]) / act[i], this.eps);
        }
    }

    /**
     * @testType UT
     * @testedFeature {@link features#ANALYTICAL_2D_ORBIT_MODEL}
     * @testedMethod {@link Analytical2DOrbitModel#mean2osc(Orbit)}
     * @testedMethod {@link Analytical2DOrbitModel#osc2mean(Orbit)}
     * @testedMethod {@link Analytical2DOrbitModel#propagateMeanOrbit(AbsoluteDate)}
     * @description checks that the conversion mean - osc - mean returns the initial orbit
     * @testPassCriteria orbit is as expected (reference : identity, tolerance: 5E-15)
     * @referenceVersion 4.6
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testMeanOsculatingConversion() throws PatriusException {
        
        final double eps = 1E-14;

        // Initialization with osculating values
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final double[] initValues = model.propagateModel(date);
        final CircularOrbit orbit = new CircularOrbit(initValues[0], initValues[1], initValues[2], initValues[3], initValues[4], initValues[5], PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442e+14);
        
        // Osculating to mean conversion
        final CircularOrbit mean = (CircularOrbit) model.osc2mean(orbit);
        
        // Mean to osculating conversion
        final CircularOrbit osc = (CircularOrbit) model.mean2osc(mean);
        
        // Mean parameters
        final CircularOrbit mean2 = (CircularOrbit) model.propagateMeanOrbit(date);
        
        // Check that the osculating parameters are equal to the initial osculating parameters
        Assert.assertEquals(0., this.relDiff(orbit.getA(), osc.getA()), eps);
        Assert.assertEquals(0., this.relDiff(orbit.getCircularEx(), osc.getCircularEx()), eps);
        Assert.assertEquals(0., this.relDiff(orbit.getCircularEy(), osc.getCircularEy()), eps);
        Assert.assertEquals(0., this.relDiff(orbit.getI(), osc.getI()), eps);
        Assert.assertEquals(0., this.relDiff(orbit.getRightAscensionOfAscendingNode(), osc.getRightAscensionOfAscendingNode()), eps);
        Assert.assertEquals(0., this.relDiff(orbit.getAlphaM() % (2. * FastMath.PI), osc.getAlphaM() % (2. * FastMath.PI)), eps);

        // Check the mean parameters from the propagateMeanOrbit method are equal to the mean parameters from the osc - mean conversion
        Assert.assertEquals(0., this.relDiff(mean2.getA(), mean.getA()), eps);
        Assert.assertEquals(0., this.relDiff(mean2.getCircularEx(), mean.getCircularEx()), 1E-12);
        Assert.assertEquals(0., this.relDiff(mean2.getCircularEy(), mean.getCircularEy()), eps);
        Assert.assertEquals(0., this.relDiff(mean2.getI(), mean.getI()), eps);
        Assert.assertEquals(0., this.relDiff(mean2.getRightAscensionOfAscendingNode(), mean.getRightAscensionOfAscendingNode()), eps);
        Assert.assertEquals(0., this.relDiff(mean2.getAlphaM() % (2. * FastMath.PI), mean.getAlphaM() % (2. * FastMath.PI)), eps);
    }
    
    /**
     * @testType UT
     * @testedFeature {@link features#ANALYTICAL_2D_ORBIT_MODEL}
     * @testedMethod {@link Analytical2DOrbitModel#mean2osc(Orbit)}
     * @testedMethod {@link Analytical2DOrbitModel#osc2mean(Orbit)}
     * @description Checks that a mean - osculating (as well as the other way around) conversion returns an orbit in initial orbit type and frame
     * @testPassCriteria Propagated orbital parameter values does not change (tolerance: 1E-12)
     */
    @Test
    public void testMeanOsculatingConversionFramesTypes() throws PatriusException {
        
        // Declaring the tolerance
        final double eps = 1E-12;

        // Initialization with osculating values (Circular orbit in CIRF frame)
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final double[] initValues = model.propagateModel(date);
        final CircularOrbit orbit = new CircularOrbit(initValues[0], initValues[1], initValues[2], initValues[3], initValues[4], initValues[5], PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442e+14);
        
        // Change orbit type and frame
        final Frame frame = FramesFactory.getGCRF();
        final KeplerianOrbit initialOrbit = new KeplerianOrbit(orbit.getPVCoordinates(frame), frame, orbit.getDate(), orbit.getMu());

        // Osculating to mean conversion
        final Orbit mean = model.osc2mean(initialOrbit);
        
        // Mean to osculating conversion
        final KeplerianOrbit osc = (KeplerianOrbit) model.mean2osc(mean);

        // Check output frame and type, as well as output values
        Assert.assertEquals(frame, mean.getFrame());
        Assert.assertEquals(OrbitType.KEPLERIAN, mean.getType());
        Assert.assertEquals(frame, osc.getFrame());
        Assert.assertEquals(OrbitType.KEPLERIAN, osc.getType());

        Assert.assertEquals(0., this.relDiff(osc.getA(), initialOrbit.getA()), eps);
        Assert.assertEquals(0., this.relDiff(osc.getE(), initialOrbit.getE()), eps);
        Assert.assertEquals(0., this.relDiff(osc.getI(), initialOrbit.getI()), eps);
        Assert.assertEquals(0., this.relDiff(osc.getPerigeeArgument(), initialOrbit.getPerigeeArgument()), eps);
        Assert.assertEquals(0., this.relDiff(osc.getRightAscensionOfAscendingNode(), initialOrbit.getRightAscensionOfAscendingNode()), eps);
        Assert.assertEquals(0., this.relDiff(osc.getMeanAnomaly() % (2. * FastMath.PI), initialOrbit.getMeanAnomaly() % (2. * FastMath.PI)), eps);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * @testedFeature {@link features#ANALYTICAL_2D_ORBIT_MODEL}
     * @testedMethod {@link Analytical2DOrbitModel#setThreshold(double)}
     * @testedMethod {@link Analytical2DOrbitModel#osc2mean(Orbit)}
     * @description Test that the setThreshold(double) method works.
     * Test that osc2mean(orbit) method throws a PatriusException when called if convergence threshold is too low.
     * Then the threshold is set to a larger value and the algorithm should converge
     * @testPassCriteria Exception is thrown if threshold is too low, convergence is reached is threshold is set to a higher value
     */
    @Test
    public void testMeanOsculatingThreshold() throws PatriusException {

        // Initialization with osculating values
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final double[] initValues = model.propagateModel(date);
        final CircularOrbit orbit = new CircularOrbit(initValues[0], initValues[1], initValues[2], initValues[3], initValues[4], initValues[5], PositionAngle.MEAN,
            FramesFactory.getCIRF(), date, 3.98600442e+14);
        
        // Set threshold to a low (unreacheable) value
        model.setThreshold(0);
        
        // Osculating to mean conversion
        try { 
            model.osc2mean(orbit);
            Assert.fail();
        } catch (final PatriusException exception) {
            // Assert whether the exception is thrown
            Assert.assertTrue(true);
        }
        
        // Set back threshold to default value
        model.setThreshold(1E-14);
    }

    /**
     * Compare 2 models.
     * 
     * @param exp
     *        expected model
     * @param act
     *        actual model
     * @param eps
     *        epsilon
     */
    private void compareModels(final Analytical2DParameterModel exp, final Analytical2DParameterModel act,
                               final double eps) {

        // trig part
        final double[][] dataExp = exp.getTrigonometricCoefficients();
        final double[][] dataAct = act.getTrigonometricCoefficients();

        Assert.assertEquals(dataExp.length, dataAct.length);
        for (int i = 0; i < dataExp.length; i++) {
            this.checkArrays(dataExp[i], dataAct[i], eps);
        }

        // poly part
        this.checkArrays(((DatePolynomialFunction) exp.getCenteredModel()).getCoefPoly(),
            ((DatePolynomialFunction) act.getCenteredModel()).getCoefPoly(), eps);
    }

    /**
     * Cmpare 2 int[] arrays.
     * 
     * @param a
     *        first array
     * @param b
     *        second array
     */
    private void checkArrays(final int[] a, final int[] b) {
        Assert.assertEquals(a.length, b.length);
        for (int i = 0; i < b.length; i++) {
            Assert.assertEquals(b[i], a[i]);
        }
    }

    /**
     * Compare 2 arrays.
     * 
     * @param array1
     *        first array
     * @param array2
     *        second array
     * @param eps
     *        epsilon
     */
    private void checkArrays(final double[] array1, final double[] array2, final double eps) {
        // Check lengths
        Assert.assertEquals(array1.length, array2.length);

        // Check values
        for (int i = 0; i < array1.length; i++) {
            Assert.assertEquals(array1[i], array2[i], eps);
        }
    }
    
    /**
     * Compute relative difference.
     * 
     * @param expected
     *        expected
     * @param actual
     *        actual
     * @return relative difference
     */
    private double relDiff(final double expected, final double actual) {
        if (expected == 0) {
            return MathLib.abs(expected - actual);
        } else {
            return MathLib.abs((expected - actual) / expected);
        }
    }
}
