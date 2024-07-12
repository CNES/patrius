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
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.11:DM:DM-3235:22/05/2023:[PATRIUS][TEMPS_CALCUL] L'attitude des spacecraft state devrait etre initialisee de maniere lazy
 * VERSION:4.11:DM:DM-3319:22/05/2023:[PATRIUS] Rendre la classe QuaternionPolynomialSegment plus generique et ajouter de la coherence dans le package polynomials
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2450:27/01/2021:[PATRIUS] moyennage au sens du modele Analytical2D 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:94:30/09/2013:2D propagator update
 * VERSION::DM:144:08/11/2013:Removed UTC-TAI dependency => removed obsolete test (no data)
 * VERSION::DM:211:08/04/2014:Modified analytical 2D propagator
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:266:29/04/2015:add various centered analytical models
 * VERSION::FA:556:24/02/2016:change max orders vs dev orders
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1173:24/08/2017:add propulsive and engine properties
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
/*
 */
package fr.cnes.sirius.patrius.propagation.analytical.twod;

//CHECKSTYLE:OFF
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.analysis.polynomials.DatePolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.complex.Quaternion;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.ElevationDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.NodeDetector;
import fr.cnes.sirius.patrius.propagation.events.NthOccurrenceDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class Analytical2DPropagatorTest {

    private final double eps = 4 * Precision.DOUBLE_COMPARISON_EPSILON;

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

    private Analytical2DParameterModel aModel;
    private Analytical2DParameterModel exModel;
    private Analytical2DParameterModel eyModel;
    private Analytical2DParameterModel iModel;
    private Analytical2DParameterModel rModel;
    private Analytical2DParameterModel lModel;
    private Analytical2DOrbitModel model;

    private Analytical2DPropagator prop;

    private AbsoluteDate ref;

    private MassProvider DEFAULT_MASS;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the analytical 2D propagator
         *
         * @featureDescription Validate the propagator
         *
         * @coveredRequirements DV-PROPAG_10, DV-PROPAG_20
         */
        ANALYTICAL_2D_PROPAGATOR
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(Analytical2DPropagatorTest.class.getSimpleName(), "Analytical 2D propagator");
    }

    @Before
    public void setup() throws URISyntaxException, IOException, PatriusException {

        this.aReader.readData(this.loc + this.aF);
        this.iReader.readData(this.loc + this.iF);
        this.exReader.readData(this.loc + this.exF);
        this.eyReader.readData(this.loc + this.eyF);
        this.rReader.readData(this.loc + this.rF);
        this.lReader.readData(this.loc + this.lF);

        this.aModel = this.aReader.getModel();
        this.exModel = this.exReader.getModel();
        this.eyModel = this.eyReader.getModel();
        this.iModel = this.iReader.getModel();
        this.rModel = this.rReader.getModel();
        this.lModel = this.lReader.getModel();

        this.ref = new AbsoluteDate(this.aReader.getDate(), TimeScalesFactory.getTAI());

        Utils.setDataRoot("analytical2Dvalidation");
        FramesFactory.setConfiguration(Utils.getIERS2010Configuration());
        this.DEFAULT_MASS = new SimpleMassModel(1000, "default");
        this.model = new Analytical2DOrbitModel(this.aModel, this.exModel, this.eyModel, this.iModel, this.rModel,
                this.lModel, this.DEFAULT_MASS, Constants.EGM96_EARTH_MU);

        // the attitude of the AbstractPropagator is not tested here
        this.prop = new Analytical2DPropagator(this.model, this.ref);
        this.prop = new Analytical2DPropagator(new LofOffset(FramesFactory.getGCRF(), LOFType.TNW), this.model,
                this.ref);
    }

    /**
     * @throws URISyntaxException
     *         if fails
     * @throws IOException
     *         if fails
     * @throws PatriusException
     *         if fails
     * @testType UT
     *
     * @testedFeature {@link features#ANALYTICAL_2D_PROPAGATOR}
     *
     * @testedMethod {@link Analytical2DPropagator#propagate(AbsoluteDate)}
     * @testedMethod {@link Analytical2DPropagator#propagateOrbit(AbsoluteDate)}
     * @testedMethod {@link Analytical2DPropagator#getMass(AbsoluteDate)}
     *
     * @description test the propagator
     *
     * @input polynomial, trigonometric and common parameters coefficients
     *
     * @output an orbit
     *
     * @testPassCriteria the resulting orbit is the same as the expected one, to the threshold 2e-14
     *                   on a relative
     *                   scale.
     *
     * @referenceVersion 1.3
     *
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testPropagate() throws URISyntaxException, IOException, PatriusException {

        Report.printMethodHeader("testPropagate", "Propagation", "CNES", this.eps, ComparisonType.RELATIVE);

        Utils.setDataRoot("analytical2Dvalidation");

        SpacecraftState result = this.prop.propagate(this.ref.shiftedBy(1000.));
        CircularOrbit c = (CircularOrbit) result.getOrbit();

        Assert.assertTrue(c.getFrame().equals(FramesFactory.getCIRF()));

        final double[] exp = { 7173631.0539035560, -0.00087881920576288180, 0.00038996201807501934, 1.7214033229710324,
                -0.99625893456226920, 1.0400756526021195 };

        // a , 1000.0000000000000, 7173631.0539035560
        // ex, 1000.0000000000000, -0.00087881920576288180
        // ey, 1000.0000000000000, 0.00038996201807501934
        // i , 1000.0000000000000, 1.7214033229710324
        // ra, 1000.0000000000000, -0.99625893456226920
        // al, 1000.0000000000000, 1.0400756526021195

        Assert.assertEquals(0, (exp[0] - c.getA()) / exp[0], this.eps);
        Assert.assertEquals(0, (exp[1] - c.getCircularEx()) / exp[1], this.eps);
        Assert.assertEquals(0, (exp[2] - c.getCircularEy()) / exp[2], this.eps);
        Assert.assertEquals(0, (exp[3] - c.getI()) / exp[3], this.eps);
        Assert.assertEquals(0, (exp[4] - c.getRightAscensionOfAscendingNode()) / exp[4], this.eps);
        Assert.assertEquals(0, (exp[5] - c.getAlphaM()) / exp[5], this.eps);

        Report.printToReport("a", exp[0], c.getA());
        Report.printToReport("ex", exp[1], c.getCircularEx());
        Report.printToReport("ey", exp[2], c.getCircularEy());
        Report.printToReport("i", exp[3], c.getI());
        Report.printToReport("RAAN", exp[4], c.getRightAscensionOfAscendingNode());
        Report.printToReport("AlphaM", exp[5], c.getAlphaM());

        Assert.assertTrue(c.getFrame().equals(FramesFactory.getCIRF()));

        Assert.assertEquals(1000, result.getMass("default"), this.eps * 1000);

        final Analytical2DOrbitModel model = new Analytical2DOrbitModel(this.aModel, this.exModel, this.eyModel,
                this.iModel, this.rModel, this.lModel, this.DEFAULT_MASS, Constants.EGM96_EARTH_MU);
        this.prop = new Analytical2DPropagator(model, this.ref, model.getMaxOrders());

        result = this.prop.propagate(this.ref.shiftedBy(1000.));
        c = (CircularOrbit) result.getOrbit();

        Assert.assertTrue(c.getFrame().equals(FramesFactory.getCIRF()));

        // a , 1000.0000000000000, 7173631.0539035560
        // ex, 1000.0000000000000, -0.00087881920576288180
        // ey, 1000.0000000000000, 0.00038996201807501934
        // i , 1000.0000000000000, 1.7214033229710324
        // ra, 1000.0000000000000, -0.99625893456226920
        // al, 1000.0000000000000, 1.0400756526021195

        Assert.assertEquals(0, (exp[0] - c.getA()) / exp[0], this.eps);
        Assert.assertEquals(0, (exp[1] - c.getCircularEx()) / exp[1], this.eps);
        Assert.assertEquals(0, (exp[2] - c.getCircularEy()) / exp[2], this.eps);
        Assert.assertEquals(0, (exp[3] - c.getI()) / exp[3], this.eps);
        Assert.assertEquals(0, (exp[4] - c.getRightAscensionOfAscendingNode()) / exp[4], this.eps);
        Assert.assertEquals(0, (exp[5] - c.getAlphaM()) / exp[5], this.eps);

        Assert.assertTrue(c.getFrame().equals(FramesFactory.getCIRF()));

        Assert.assertEquals(1000, result.getMass("default"), this.eps * 1000);

    }

    /**
     * @throws URISyntaxException
     *         if fails
     * @throws IOException
     *         if fails
     * @throws PatriusException
     *         if fails
     * @testType UT
     *
     * @testedFeature {@link features#ANALYTICAL_2D_PROPAGATOR}
     *
     * @testedMethod {@link Analytical2DPropagator#propagate(AbsoluteDate)}
     *
     * @description test the propagator event detection
     *
     * @input propagator
     *
     * @output event occurrences
     *
     * @testPassCriteria some events are detected, no threshold
     *
     * @referenceVersion 1.3
     *
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testPropagateWithEvents() throws URISyntaxException, IOException, PatriusException {

        final AbsoluteDate end = this.ref.shiftedBy(150000);

        // Date detector
        final AbsoluteDate toDetect = this.ref.shiftedBy(30000);
        final MyDateDetector dd = new MyDateDetector(toDetect);
        this.prop.addEventDetector(dd);

        // Elevation detector
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.EGM96_EARTH_EQUATORIAL_RADIUS,
                Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getITRF());
        final TopocentricFrame labo = new TopocentricFrame(earth, new GeodeticPoint(MathLib.toRadians(43),
                MathLib.toRadians(1.5), 400), "toulouse");
        final MyElevationDetector det = new MyElevationDetector(MathLib.toRadians(15), labo);
        this.prop.addEventDetector(det);

        this.prop.propagate(end);

        // Check events have been detected
        Assert.assertTrue(dd.getCount() != 0);
        Assert.assertTrue(det.data.size() != 0);
    }

    /**
     * @throws URISyntaxException
     *         if fails
     * @throws IOException
     *         if fails
     * @throws PatriusException
     *         if fails
     * @testType UT
     *
     * @testedFeature {@link features#ANALYTICAL_2D_PROPAGATOR}
     *
     * @testedMethod {@link Analytical2DPropagator#propagate(AbsoluteDate)}
     *
     * @description test the propagator with maneuver and attitude
     *
     * @input propagator
     *
     * @output event occurrences
     *
     * @testPassCriteria maneuver is performed, attitude at maneuver date is good, no threshold
     *
     * @referenceVersion 3.0
     *
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testPropagateWithManeuverAndAttitude() throws URISyntaxException, IOException, PatriusException {

        // Attitude provider
        this.prop.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getCIRF(), new Rotation(false,
                new Quaternion(0, 1, 0, 0))));

        // Impulse maneuver
        final EventDetector nodeDetector = new NodeDetector(FramesFactory.getITRF(), 0, 100, 1E-6);
        final NthOccurrenceDetector trigger = new NthOccurrenceDetector(nodeDetector, 1, Action.STOP);
        final MyImpulseManeuver maneuver = new MyImpulseManeuver(trigger, new Vector3D(20, Vector3D.PLUS_J),
                FramesFactory.getGCRF(), 500, this.DEFAULT_MASS, "default");
        this.prop.addEventDetector(maneuver);

        this.prop.propagate(this.ref.shiftedBy(20000));

        // Check maneuver has been performed and attitude has been taken into account
        Assert.assertEquals(maneuver.getCount(), 1);
        Assert.assertTrue(this.DEFAULT_MASS.getTotalMass() != 1000);
        Assert.assertEquals(maneuver.getAttitudeFrame().getName(), "CIRF");
        Assert.assertEquals(maneuver.getAttitudeRotation().getQuaternion().getQ0(), 0, 0);
        Assert.assertEquals(maneuver.getAttitudeRotation().getQuaternion().getQ1(), 1, 0);
        Assert.assertEquals(maneuver.getAttitudeRotation().getQuaternion().getQ2(), 0, 0);
        Assert.assertEquals(maneuver.getAttitudeRotation().getQuaternion().getQ3(), 0, 0);
    }

    /**
     * @testType UT
     *
     * @testedFeature {@link features#ANALYTICAL_2D_PROPAGATOR}
     *
     * @testedMethod {@link Analytical2DPropagator#propagate(AbsoluteDate)}
     *
     * @description test the propagator with orders different from maximal orders
     *
     * @input propagator
     *
     * @output circular orbit
     *
     * @testPassCriteria developement orders have been taken into account, not maximum orders (orbit
     *                   is as expected)
     *
     * @referenceVersion 3.2
     *
     * @nonRegressionVersion 3.2
     */
    @Test
    public void testDifferentOrders() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final double[] coefa = { 7000000. };
        final double[] coef0 = { 0. };

        // Max orders: 2
        final double[][] trig = { { 0, 0, 0.1, 0. }, { 1, 0, 0.01, 0. }, { 2, 0, 0.001, 0. }, };

        // Model order: 1
        final int[] orders = { 2, 1, 1, 1, 1, 1 };

        final Analytical2DParameterModel a = new Analytical2DParameterModel(new DatePolynomialFunction(date,
            new PolynomialFunction(coefa)), trig);
        final Analytical2DParameterModel ex = new Analytical2DParameterModel(new DatePolynomialFunction(date,
            new PolynomialFunction(coef0)), trig);
        final Analytical2DParameterModel ey = new Analytical2DParameterModel(new DatePolynomialFunction(date,
            new PolynomialFunction(coef0)), trig);
        final Analytical2DParameterModel i = new Analytical2DParameterModel(new DatePolynomialFunction(date,
            new PolynomialFunction(coef0)), trig);
        final Analytical2DParameterModel lna = new Analytical2DParameterModel(new DatePolynomialFunction(date,
            new PolynomialFunction(coef0)), trig);
        final Analytical2DParameterModel alpha = new Analytical2DParameterModel(
            new DatePolynomialFunction(date, new PolynomialFunction(coef0)), trig);
        final Analytical2DOrbitModel model = new Analytical2DOrbitModel(a, ex, ey, i, lna, alpha, orders,
                Constants.EGM96_EARTH_MU);
        final Analytical2DPropagator propagator = new Analytical2DPropagator(model, date);

        // Propagation
        final CircularOrbit res = (CircularOrbit) propagator.propagate(date.shiftedBy(86400.)).getOrbit();

        // Check that elements are same as initial => order taken into accounts where only first
        // orders
        Assert.assertEquals(res.getA(), 7000000.11, 0);
        Assert.assertEquals(res.getCircularEx(), 0.1, 0);
        Assert.assertEquals(res.getCircularEy(), 0.1, 0);
        Assert.assertEquals(res.getI(), 0.1, 0);
        Assert.assertEquals(res.getAlpha(PositionAngle.MEAN), 0.1, 0);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#ANALYTICAL_2D_PROPAGATOR}
     * @testedMethod {@link Analytical2DPropagator#mean2osc(Orbit)}
     * @testedMethod {@link Analytical2DPropagator#osc2mean(Orbit)}
     * @testedMethod {@link Analytical2DPropagator#propagateMeanOrbit(AbsoluteDate)}
     * @description checks that the conversion mean - osc - mean returns the initial orbit. This
     *              method uses different
     *              orders
     *              from model orders to check they are properly taken into account
     * @testPassCriteria orbit is as expected (reference : identity, tolerance: 5E-15)
     * @referenceVersion 4.6
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testMeanOsculatingConversion() throws PatriusException {

        final double eps = 1E-14;

        // Initialize propagator with different orders from orbit model
        this.prop = new Analytical2DPropagator(this.model, this.ref, new int[] { 10, 11, 12, 13, 14, 15 });

        // Initialization with osculating values
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final CircularOrbit orbit = (CircularOrbit) this.prop.propagateOrbit(date);

        // Osculating to mean conversion
        final CircularOrbit mean = (CircularOrbit) this.prop.osc2mean(orbit);

        // Mean to osculating conversion
        final CircularOrbit osc = (CircularOrbit) this.prop.mean2osc(mean);

        // Mean parameters
        final CircularOrbit mean2 = (CircularOrbit) this.prop.propagateMeanOrbit(date);

        // Check that the osculating parameters are equal to the initial osculating parameters
        Assert.assertEquals(0., relDiff(orbit.getA(), osc.getA()), eps);
        Assert.assertEquals(0., relDiff(orbit.getCircularEx(), osc.getCircularEx()), eps);
        Assert.assertEquals(0., relDiff(orbit.getCircularEy(), osc.getCircularEy()), eps);
        Assert.assertEquals(0., relDiff(orbit.getI(), osc.getI()), eps);
        Assert.assertEquals(0.,
                relDiff(orbit.getRightAscensionOfAscendingNode(), osc.getRightAscensionOfAscendingNode()), eps);
        Assert.assertEquals(0., relDiff(orbit.getAlphaM() % (2. * FastMath.PI), osc.getAlphaM() % (2. * FastMath.PI)),
                eps);

        // Check the mean parameters from the propagateMeanOrbit method are equal to the mean
        // parameters from the osc -
        // mean conversion
        Assert.assertEquals(0., relDiff(mean2.getA(), mean.getA()), eps);
        Assert.assertEquals(0., relDiff(mean2.getCircularEx(), mean.getCircularEx()), 1E-12);
        Assert.assertEquals(0., relDiff(mean2.getCircularEy(), mean.getCircularEy()), eps);
        Assert.assertEquals(0., relDiff(mean2.getI(), mean.getI()), eps);
        Assert.assertEquals(0.,
                relDiff(mean2.getRightAscensionOfAscendingNode(), mean.getRightAscensionOfAscendingNode()), eps);
        Assert.assertEquals(0., relDiff(mean2.getAlphaM() % (2. * FastMath.PI), mean.getAlphaM() % (2. * FastMath.PI)),
                eps);
    }

    /**
     * @testType UT
     * @testedFeature {@link features#ANALYTICAL_2D_PROPAGATOR}
     * @testedMethod {@link Analytical2DPropagator#mean2osc(Orbit)}
     * @testedMethod {@link Analytical2DPropagator#osc2mean(Orbit)}
     * @description Checks that a mean - osculating (as well as the other way around) conversion
     *              returns an orbit in
     *              initial orbit type and frame
     * @testPassCriteria Propagated orbital parameter values does not change (tolerance: 1E-12)
     */
    @Test
    public void testMeanOsculatingConversionFramesTypes() throws PatriusException {

        // Declaring the tolerance
        final double eps = 1E-12;

        // Initialization with osculating values (Circular orbit in CIRF frame)
        final AbsoluteDate date = AbsoluteDate.FIFTIES_EPOCH_TAI.shiftedBy(17532 * 86400.);
        final CircularOrbit orbit = (CircularOrbit) this.prop.propagateOrbit(date);

        // Change orbit type and frame
        final Frame frame = FramesFactory.getGCRF();
        final KeplerianOrbit initialOrbit = new KeplerianOrbit(orbit.getPVCoordinates(frame), frame, orbit.getDate(),
                orbit.getMu());

        // Osculating to mean conversion
        final Orbit mean = this.prop.osc2mean(initialOrbit);

        // Mean to osculating conversion
        final KeplerianOrbit osc = (KeplerianOrbit) this.prop.mean2osc(mean);

        // Check output frame and type, as well as output values
        Assert.assertEquals(frame, mean.getFrame());
        Assert.assertEquals(OrbitType.KEPLERIAN, mean.getType());
        Assert.assertEquals(frame, osc.getFrame());
        Assert.assertEquals(OrbitType.KEPLERIAN, osc.getType());

        Assert.assertEquals(0., relDiff(osc.getA(), initialOrbit.getA()), eps);
        Assert.assertEquals(0., relDiff(osc.getE(), initialOrbit.getE()), eps);
        Assert.assertEquals(0., relDiff(osc.getI(), initialOrbit.getI()), eps);
        Assert.assertEquals(0., relDiff(osc.getPerigeeArgument(), initialOrbit.getPerigeeArgument()), eps);
        Assert.assertEquals(0.,
                relDiff(osc.getRightAscensionOfAscendingNode(), initialOrbit.getRightAscensionOfAscendingNode()), eps);
        Assert.assertEquals(0.,
                relDiff(osc.getMeanAnomaly() % (2. * FastMath.PI), initialOrbit.getMeanAnomaly() % (2. * FastMath.PI)),
                eps);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * @testedFeature {@link features#ANALYTICAL_2D_PROPAGATOR}
     * @testedMethod {@link Analytical2DPropagator#setThreshold(double)}
     * @testedMethod {@link Analytical2DPropagator#osc2mean(Orbit)}
     * @description Test that the setThreshold(double) method works.
     *              Test that osc2mean(orbit) method throws a PatriusException when called if
     *              convergence threshold is
     *              too low.
     *              Then the threshold is set to a larger value and the algorithm should converge
     * @testPassCriteria Exception is thrown if threshold is too low, convergence is reached is
     *                   threshold is set to a
     *                   higher value
     */
    @Test
    public void testMeanOsculatingThreshold() throws PatriusException {

        // Initialization with osculating values
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final CircularOrbit orbit = (CircularOrbit) this.prop.propagateOrbit(date);

        // Set threshold to a low (unreacheable) value
        this.prop.setThreshold(0);

        // Osculating to mean conversion
        try {
            this.prop.osc2mean(orbit);
            Assert.fail();
        } catch (final PatriusException exception) {
            // Assert whether the exception is thrown
            Assert.assertTrue(true);
        }

        // Set back threshold to default value
        this.prop.setThreshold(1E-14);
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
    private static double relDiff(final double expected, final double actual) {
        if (expected == 0) {
            return MathLib.abs(expected - actual);
        }
        return MathLib.abs((expected - actual) / expected);
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @description Evaluate the analytical 2D propagator serialization / deserialization process.
     *
     * @testPassCriteria The analytical 2D propagator can be serialized and deserialized.
     */
    @Test
    public void testSerialization() throws PatriusException {

        final Frame frame = FramesFactory.getGCRF();
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("thruster");
        final TankProperty p1 = new TankProperty(9000.);
        builder.addProperty(p1, "thruster");
        final MassModel massModel = new MassModel(builder.returnAssembly());
        final Analytical2DOrbitModel model = new Analytical2DOrbitModel(this.aModel, this.exModel, this.eyModel,
                this.iModel, this.rModel, this.lModel, massModel, Constants.EGM96_EARTH_MU);

        final Analytical2DPropagator propagator = new Analytical2DPropagator(model, this.ref, model.getMaxOrders());
        final Analytical2DPropagator deserializedPropagator = TestUtils.serializeAndRecover(propagator);

        for (int i = 0; i < 10; i++) {
            final AbsoluteDate currentDate = this.ref.shiftedBy(i);
            Assert.assertEquals(propagator.getPVCoordinates(currentDate, frame),
                    deserializedPropagator.getPVCoordinates(currentDate, frame));
        }
    }
}

class MyImpulseManeuver extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 1232622251108427111L;

    private int count = 0;

    private Frame attitudeFrame;

    private Rotation attitudeRotation;

    /** Triggering event. */
    private final EventDetector trigger;

    /** Velocity increment in the frame defined by the user. */
    private final Vector3D deltaVSat;
    /**
     * Frame of the velocity increment.
     * If null, the velocity increment is expressed in the satellite frame
     */
    private final Frame frame;

    /**
     * Local orbital frame type.
     */
    private final LOFType lofType;

    /** Engine exhaust velocity. */
    private final double vExhaust;
    /** Mass provider. */
    private final MassProvider mass;
    /** Part name. */
    private final String partName;
    /** If true, the integration variable (time) increases during integration. */
    private boolean forwardLocal;

    /**
     * Build a new instance.
     *
     * Note : The frame could be set to null to express the velocity increment in spacecraft frame.
     * WARNING : It is not recommended to use this constructor with a LocalOrbitalFrame built with a
     * PVCoordinatesProvider equal to the current propagator.
     *
     * @param trigger
     *        triggering event (it must generate a <b>STOP</b> event action to trigger the maneuver)
     * @param deltaVSat
     *        velocity increment in the frame defined by the user
     * @param frame
     *        the frame of the velocity increment.
     *        Null frame means spacecraft frame
     * @param isp
     *        engine specific impulse (s)
     * @param massModel
     *        mass model
     * @param part
     *        part of the mass model that provides the propellants
     */
    public MyImpulseManeuver(final EventDetector trigger, final Vector3D deltaVSat, final Frame frame,
            final double isp, final MassProvider massModel, final String part) {
        super(trigger.getSlopeSelection(), trigger.getMaxCheckInterval(), trigger.getThreshold());
        this.trigger = trigger;
        this.deltaVSat = deltaVSat;
        this.vExhaust = Constants.G0_STANDARD_GRAVITY * isp;
        this.frame = frame;
        this.lofType = null;
        this.mass = massModel;
        this.partName = part;
    }

    /** {@inheritDoc} */
    @Override
    public double getMaxCheckInterval() {
        return this.trigger.getMaxCheckInterval();
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxIterationCount() {
        return this.trigger.getMaxIterationCount();
    }

    /** {@inheritDoc} */
    @Override
    public double getThreshold() {
        return this.trigger.getThreshold();
    }

    /** {@inheritDoc}. */
    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
            throws PatriusException {
        this.forwardLocal = forward;
        // filter underlying event
        return (this.trigger.eventOccurred(s, increasing, forward) == Action.STOP) ? Action.RESET_STATE
                : Action.CONTINUE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldBeRemoved() {
        return this.trigger.shouldBeRemoved();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public double g(final SpacecraftState s) throws PatriusException {
        return this.trigger.g(s);
    }

    /**
     * {@inheritDoc}
     *
     * @throws PatriusException
     *         thrown if the mass becomes negative
     *         (PatriusMessages.SPACECRAFT_MASS_BECOMES_NEGATIVE)
     * @throws PatriusException
     *         thrown if no attitude informations is defined
     * @throws PatriusException
     *         thrown if error occurs during transformation
     **/
    @Override
    public SpacecraftState resetState(final SpacecraftState oldState) throws PatriusException {
        this.count++;
        this.attitudeFrame = oldState.getAttitude().getReferenceFrame();
        this.attitudeRotation = oldState.getAttitude().getRotation();

        // Code from ImpulseManeuver
        final AbsoluteDate date = oldState.getDate();

        // compute thrust direction
        final double direction = (this.forwardLocal == true) ? 1.0 : -1.0;

        // convert velocity increment in inertial frame
        final Vector3D deltaV;
        if (this.frame == null) {
            if (this.lofType == null) {
                final Attitude attitudeEvents = oldState.getAttitudeEvents();
                // Check if the attitude exists
                if (attitudeEvents != null) {
                    // velocity increment in satellite frame
                    deltaV = attitudeEvents.getRotation().applyTo(this.deltaVSat);
                } else {
                    throw new PatriusException(PatriusMessages.NO_ATTITUDE_EVENTS_DEFINED);
                }
            } else {
                // velocity increment in local orbital frame
                final Transform tranform = this.lofType.transformFromInertial(date, oldState.getPVCoordinates());
                deltaV = tranform.getInverse().transformVector(this.deltaVSat);
            }
        } else {
            // velocity increment in a frame defined by the user
            final Transform tranform = this.frame.getTransformTo(oldState.getFrame(), date);
            deltaV = tranform.transformVector(this.deltaVSat);
        }

        // apply increment to position/velocity
        final PVCoordinates oldPV = oldState.getPVCoordinates();
        final PVCoordinates newPV = new PVCoordinates(oldPV.getPosition(), oldPV.getVelocity().add(
                new Vector3D(direction, deltaV)));

        final CartesianOrbit newOrbitCartesian = new CartesianOrbit(newPV, oldState.getFrame(), date, oldState.getMu());

        // compute new mass and update mass model
        // deltaV applied onto satellite!
        final double ratio = MathLib.exp(-deltaV.getNorm() / this.vExhaust);
        final double oldPartMass = this.mass.getMass(this.partName);
        final double oldTotalMass = this.mass.getTotalMass();

        final double newPartMass;
        if (this.forwardLocal) {
            newPartMass = oldPartMass - oldTotalMass * (1 - ratio);
        } else {
            newPartMass = oldPartMass + oldTotalMass * (1 - ratio) / ratio;
        }
        if (newPartMass < 0.0) {
            throw new PropagationException(PatriusMessages.NOT_POSITIVE_MASS, newPartMass);
        }
        // NB : additional states map is updated from MassProvider in SpacecarftState constructor
        // update MassProvider
        this.mass.updateMass(this.partName, newPartMass);

        // pack everything in a new state
        final Orbit newOrbit = oldState.getOrbit().getType().convertType(newOrbitCartesian);
        return oldState.updateOrbit(newOrbit).addMassProvider(this.mass);
    }

    /** {@inheritDoc}. */
    @Override
    public int getSlopeSelection() {
        return 2;
    }

    public int getCount() {
        return this.count;
    }

    public Frame getAttitudeFrame() {
        return this.attitudeFrame;
    }

    public Rotation getAttitudeRotation() {
        return this.attitudeRotation;
    }

    @Override
    public EventDetector copy() {
        return null;
    }
}

class MyElevationDetector extends ElevationDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 961542276684375683L;

    TreeMap<AbsoluteDate, PVCoordinates> data = new TreeMap<>();

    public MyElevationDetector(final double elevation, final TopocentricFrame topo) {
        super(elevation, topo, 60);
    }

    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
            throws PatriusException {
        this.data.put(s.getDate(), s.getPVCoordinates(FramesFactory.getGCRF()));
        return Action.CONTINUE;
    }

    /**
     * @return the data
     */
    public TreeMap<AbsoluteDate, PVCoordinates> getData() {
        return this.data;
    }

}

class MyDateDetector extends DateDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = 4899096183207955884L;

    private int count = 0;

    public MyDateDetector(final AbsoluteDate target) {
        super(target);
    }

    @Override
    public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
            throws PatriusException {
        this.count++;
        return Action.CONTINUE;
    }

    public int getCount() {
        return this.count;
    }
}
