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
 * @history creation 06/08/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes definies dans la façade ALGO DV SIRUS 
 * VERSION:4.11:DM:DM-3319:22/05/2023:[PATRIUS] Rendre la classe QuaternionPolynomialSegment plus generique et ajouter de la coherence dans le package polynomials
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.events.detectors.AngularMomentumExcessDetector;
import fr.cnes.sirius.patrius.events.detectors.CenteredAolPassageDetector;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.analysis.polynomials.DatePolynomialFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.analytical.twod.Analytical2DOrbitModel;
import fr.cnes.sirius.patrius.propagation.analytical.twod.Analytical2DParameterModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * Test class for the angular momentum excess detector.
 * </p>
 *
 * @see AngularMomentumExcessDetector
 *
 * @author Florian Teilhard
 *
 *
 * @since 4.11
 *
 */
public class CenteredAolPassageDetectorTest {

    /** tolerance for date comparisons */
    private static final double DATE_COMPARISON_EPSILON = 1e-6;

    /**
     * @testType UT
     *
     * @testedMethod {@link CenteredAolPassageDetector#g(SpacecraftState)}
     * @testedMethod {@link CenteredAolPassageDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     *
     * @description Here we test the dates at which the event "passage of the centered AOL" has
     *              occured
     *
     * @input a spacecraft, its orbit, its attitude law
     *
     * @output dates of the detected event
     *
     * @testPassCriteria the stop date is correct, depending on the action given by the detector
     *                   (Continue or Stop). The reference dates are generated for non regression
     *                   test.
     * @throws PatriusException if a frame problem occurs
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void detectorTest() throws PatriusException {

        final AbsoluteDate originDate = AbsoluteDate.J2000_EPOCH;
        final Frame cirfFrame = FramesFactory.getCIRF();
        final double mu = Utils.mu;
        final Orbit initialOrbit = new KeplerianOrbit(7000E3, 0, 0, 0, 0, 0, PositionAngle.TRUE, cirfFrame, originDate,
                mu);

        final double[][] trigonometricCoefs = { { 0.0000001, 0, 0.0000001, 0 } };
        // Zero trigo coefficients so the associated model is only centered
        final double[][] trigonometricZero = { { 0, 0, 0, 0 } };

        // create "centered part" for other parameters than alpha and sma, set to a constant 0 value
        final double[] coefZeroPoly = { 0. };
        final DatePolynomialFunction polynomialFunctionZero = new DatePolynomialFunction(originDate,
            new PolynomialFunction(coefZeroPoly));

        // creating models all set to a constant zero value
        final Analytical2DParameterModel exModel = new Analytical2DParameterModel(polynomialFunctionZero,
                trigonometricZero);
        final Analytical2DParameterModel iModel = new Analytical2DParameterModel(polynomialFunctionZero,
                trigonometricZero);
        final Analytical2DParameterModel lnaModel = new Analytical2DParameterModel(polynomialFunctionZero,
                trigonometricZero);
        final Analytical2DParameterModel eyModel = new Analytical2DParameterModel(polynomialFunctionZero,
                trigonometricZero);

        // Propagation target date
        final AbsoluteDate targetDate = originDate.shiftedBy(50000.);

        // SMA MODEL
        // create "centered part" for sma: sma=cste
        final double[] coefSmaPoly = { initialOrbit.getA() };
        final DatePolynomialFunction polynomialFunctionSma = new DatePolynomialFunction(originDate,
            new PolynomialFunction(coefSmaPoly));
        // model for sma
        final Analytical2DParameterModel smaModel = new Analytical2DParameterModel(polynomialFunctionSma,
                trigonometricCoefs);

        // PSO MODEL
        // create "centered part" for pso: alpha=t-t0
        final double[] coefAlpha = { 0., 2. * MathLib.PI / initialOrbit.getKeplerianPeriod() };
        final DatePolynomialFunction polynomialFunctionAlpha = new DatePolynomialFunction(originDate,
            new PolynomialFunction(coefAlpha));
        // model for alpha
        final Analytical2DParameterModel alphaModel = new Analytical2DParameterModel(polynomialFunctionAlpha,
                trigonometricZero);

        // create an orbit model out of the parameter model - use user given pso and lna tables
        final Analytical2DOrbitModel provider = new Analytical2DOrbitModel(smaModel, exModel, eyModel, iModel,
                lnaModel, alphaModel, mu);

        // create the propagators
        final Propagator propagator = new KeplerianPropagator(initialOrbit);

        // Creating the CenteredAolPassageDetector and adding it to the propagator. The target AOL
        // is given
        final double targetAol = 2.5;
        final CenteredAolPassageDetector detector = new CenteredAolPassageDetector(targetAol, PositionAngle.TRUE,
                provider, cirfFrame);
        propagator.addEventDetector(detector);

        // Propagation
        final SpacecraftState state = propagator.propagate(targetDate);
        // Check the end date of the propagation
        Assert.assertEquals(targetAol / (2. * MathLib.PI) * initialOrbit.getKeplerianPeriod(),
                MathLib.abs(state.getDate().durationFrom(originDate)), DATE_COMPARISON_EPSILON);

    }

    /**
     * @testType UT
     *
     * @testedMethod {@link CenteredAolPassageDetector#copy()}
     * @testedMethod {@link CenteredAolPassageDetector#eventOccurred(SpacecraftState, boolean, boolean)}
     *
     * @description Here we test the copy method
     *
     * @input an event detector with random data
     *
     * @output a copy of the event detector
     *
     * @testPassCriteria the slope selection and the actions are identical between the original
     *                   detector and its copy.
     * @throws PatriusException if a frame problem occurs
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testCopy() throws PatriusException {

        final AbsoluteDate originDate = AbsoluteDate.J2000_EPOCH;
        final Frame cirfFrame = FramesFactory.getCIRF();
        final double mu = Utils.mu;
        final Orbit initialOrbit = new KeplerianOrbit(7000E3, 0, 0, 0, 0, 0, PositionAngle.TRUE, cirfFrame, originDate,
                mu);

        // Arbitrary construction of a provider
        final double[][] trigonometricCoefs = { { 0.0000001, 2, 0.0000001, 8 } };

        // create "centered part" for other parameters than alpha and sma, set to a constant 0 value
        final double[] coefPoly = { 5.3, 2.4, 8.9, 0.041 };
        final DatePolynomialFunction polynomialFunction = new DatePolynomialFunction(originDate,
            new PolynomialFunction(coefPoly));

        // creating models all set to a constant zero value
        final Analytical2DParameterModel exModel = new Analytical2DParameterModel(polynomialFunction,
                trigonometricCoefs);
        final Analytical2DParameterModel iModel = new Analytical2DParameterModel(polynomialFunction,
                trigonometricCoefs);
        final Analytical2DParameterModel lnaModel = new Analytical2DParameterModel(polynomialFunction,
                trigonometricCoefs);
        final Analytical2DParameterModel eyModel = new Analytical2DParameterModel(polynomialFunction,
                trigonometricCoefs);

        // SMA MODEL
        // create "centered part" for sma: sma=cste
        final double[] coefSmaPoly = { initialOrbit.getA() };
        final DatePolynomialFunction polynomialFunctionSma = new DatePolynomialFunction(originDate,
            new PolynomialFunction(coefSmaPoly));
        // model for sma
        final Analytical2DParameterModel smaModel = new Analytical2DParameterModel(polynomialFunctionSma,
                trigonometricCoefs);

        // PSO MODEL
        // create "centered part" for pso: alpha=t-t0
        final double[] coefAlpha = { 0., 2. * MathLib.PI / initialOrbit.getKeplerianPeriod() };
        final DatePolynomialFunction polynomialFunctionAlpha = new DatePolynomialFunction(originDate,
            new PolynomialFunction(coefAlpha));
        // model for alpha
        final Analytical2DParameterModel alphaModel = new Analytical2DParameterModel(polynomialFunctionAlpha,
                trigonometricCoefs);

        // create an orbit model out of the parameter model - use user given pso and lna tables
        final Analytical2DOrbitModel provider = new Analytical2DOrbitModel(smaModel, exModel, eyModel, iModel,
                lnaModel, alphaModel, mu);

        // Creating the CenteredAolPassageDetector and adding it to the propagator. The target AOL
        // is given
        final double targetAol = 2.5;

        // detector creation
        final CenteredAolPassageDetector detector = new CenteredAolPassageDetector(targetAol, PositionAngle.TRUE,
                provider, cirfFrame);

        final EventDetector detectorCopy = detector.copy();

        // Test the copy of the slope selection
        Assert.assertEquals(detector.getSlopeSelection(), detectorCopy.getSlopeSelection());
        // Test the copy of the Actions via eventOccured method
        Assert.assertEquals(detector.eventOccurred(null, true, false), detectorCopy.eventOccurred(null, true, false));
        Assert.assertEquals(detector.eventOccurred(null, false, false), detectorCopy.eventOccurred(null, false, false));
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link CenteredAolPassageDetector#centeredToOsculating(fr.cnes.sirius.patrius.orbits.CircularOrbit)
     *               ()}
     *
     * @description Here we test the centeredToOsculating method via a simple oscillating sma orbit
     *
     * @input an event detector with a osculating orbit
     *
     * @output an osculating orbit
     *
     * @testPassCriteria the returned osculating orbit is as expected
     * @throws PatriusException if a frame problem occurs
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testCenteredToOsculating() throws PatriusException {

        final AbsoluteDate originDate = AbsoluteDate.J2000_EPOCH;
        final Frame cirfFrame = FramesFactory.getCIRF();
        final double mu = Utils.mu;
        final Orbit initialOrbit = new KeplerianOrbit(7000E3, 0, 0, 0, 0, 0, PositionAngle.TRUE, cirfFrame, originDate,
                mu);

        final CircularOrbit initialCircularOrbit = new CircularOrbit(700E3, 0, 0, 0, 0, 0, PositionAngle.TRUE,
                cirfFrame, originDate, mu);

        final double[][] trigonometricCoefs = { { 1, 0, 25, 0 } };
        // Zero trigo coefficients so the associated model is only centered
        final double[][] trigonometricZero = { { 0, 0, 0, 0 } };

        // create "centered part" for other parameters than alpha and sma, set to a constant 0 value
        final double[] coefZeroPoly = { 0. };
        final DatePolynomialFunction polynomialFunctionZero = new DatePolynomialFunction(originDate,
            new PolynomialFunction(coefZeroPoly));

        // creating models all set to a constant zero value
        final Analytical2DParameterModel exModel = new Analytical2DParameterModel(polynomialFunctionZero,
                trigonometricZero);
        final Analytical2DParameterModel iModel = new Analytical2DParameterModel(polynomialFunctionZero,
                trigonometricZero);
        final Analytical2DParameterModel lnaModel = new Analytical2DParameterModel(polynomialFunctionZero,
                trigonometricZero);
        final Analytical2DParameterModel eyModel = new Analytical2DParameterModel(polynomialFunctionZero,
                trigonometricZero);


        // SMA MODEL
        // sma = 7E6 + 25*cos(deltaT)
        final double[] coefSmaPoly = { initialOrbit.getA() };
        final DatePolynomialFunction polynomialFunctionSma = new DatePolynomialFunction(originDate,
            new PolynomialFunction(coefSmaPoly));
        // model for sma
        final Analytical2DParameterModel smaModel = new Analytical2DParameterModel(polynomialFunctionSma,
                trigonometricCoefs);

        // PSO MODEL
        // alpha = 0.
        final double[] coefAlpha = { 0. };
        final DatePolynomialFunction polynomialFunctionAlpha = new DatePolynomialFunction(originDate,
            new PolynomialFunction(coefAlpha));
        // model for alpha
        final Analytical2DParameterModel alphaModel = new Analytical2DParameterModel(polynomialFunctionAlpha,
                trigonometricZero);

        // create an orbit model out of the parameter model - use user given pso and lna tables
        final Analytical2DOrbitModel provider = new Analytical2DOrbitModel(smaModel, exModel, eyModel, iModel,
                lnaModel, alphaModel, mu);

        // Creating the CenteredAolPassageDetector and adding it to the propagator. The target AOL
        // is given
        final double targetAol = 2.5;
        final CenteredAolPassageDetector detector = new CenteredAolPassageDetector(targetAol, PositionAngle.TRUE,
                provider, cirfFrame);
        final Orbit oscOrbit = detector.centeredToOsculating(initialCircularOrbit);
        // lna and pso are constant and equal to zero. the propagation is simple for the sma 2D
        // model
        Assert.assertEquals(smaModel.getValue(originDate, 0., 0.), oscOrbit.getA(), Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("analytical2Dvalidation");
        FramesFactory.setConfiguration(Utils.getIERS2010Configuration());
    }
}
