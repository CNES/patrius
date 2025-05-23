/**
 * 
 * Copyright 2021-2021 CNES
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
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.8:FA:FA-2946:15/11/2021:[PATRIUS] Robustesse dans IAUPoleCoefficients1D 
 * VERSION:4.8:FA:FA-2945:15/11/2021:[PATRIUS] Utilisation des degres dans des fonctions mathematiques 
 * VERSION:4.7:DM:DM-2684:18/05/2021:Création d'une classe UserIAUPole, à l'image de la classe UserCelestialBody
 * VERSION:4.7:DM:DM-2888:18/05/2021:ajout des derivee des angles alpha,delta,w &amp;#224; la classe UserIAUPole
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.bodies.CelestialBodyOrientation.OrientationType;
import fr.cnes.sirius.patrius.bodies.IAUPoleFunction.IAUTimeDependency;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.differentiation.UnivariateDifferentiableFunction;
import fr.cnes.sirius.patrius.math.analysis.function.CosineFunction;
import fr.cnes.sirius.patrius.math.analysis.function.SineFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the class {@link UserIAUPole}.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.7
 */
public class UserIAUPoleTest {

    /** Deg to rad. */
    private static final double DEG_TO_RAD = MathLib.PI / 180.;

    /** Epsilon for double comparison. */
    private static final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle User-defined IAU pole
         * 
         * @featureDescription Test user-defined IAU pole.
         * 
         * @coveredRequirements
         */
        USER_IAU_POLE
    }

    /**
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     * @testType UT
     * 
     * @testedFeature {@link features#USER_IAU_POLE}
     * 
     * @description check that user-defined Mercury IAU pole (with values from IAU NT) returns the same results as built
     *              from IAUPoleFactory
     * 
     * @testPassCriteria IAU pole from IAUPoleFactory and defined by UserIAUPole for Mercury are the same (relative
     *                   threshold: 1E-4, due to different versions of IAU NT results origin).
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testUserIAUPoleNTMercury() throws PatriusException {

        // Reference
        final CelestialBodyIAUOrientation reference = IAUPoleFactory.getIAUPole(EphemerisType.MERCURY);
        // Build actual
        final List<UnivariateDifferentiableFunction> alpha0fDays = new ArrayList<>();
        alpha0fDays.add(new PolynomialFunction(new double[] { 281.0103 * DEG_TO_RAD }));
        final List<UnivariateDifferentiableFunction> alpha0fCenturies = new ArrayList<>();
        alpha0fCenturies.add(new PolynomialFunction(new double[] { 0, -0.0328 * DEG_TO_RAD }));
        final IAUPoleCoefficients1D alpha0Coeffs = buildCoefficientsList(alpha0fDays, alpha0fCenturies);
        final List<UnivariateDifferentiableFunction> delta0fDays = new ArrayList<>();
        delta0fDays.add(new PolynomialFunction(new double[] { 61.4155 * DEG_TO_RAD }));
        final List<UnivariateDifferentiableFunction> delta0fCenturies = new ArrayList<>();
        delta0fCenturies.add(new PolynomialFunction(new double[] { 0, -0.0049 * DEG_TO_RAD }));
        final IAUPoleCoefficients1D delta0Coeffs = buildCoefficientsList(delta0fDays, delta0fCenturies);
        final List<UnivariateDifferentiableFunction> wfDays = new ArrayList<>();
        wfDays.add(new PolynomialFunction(new double[] { 329.5988 * DEG_TO_RAD, 6.1385108 * DEG_TO_RAD }));
        wfDays.add(new SineFunction(0.01067257 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 174.7910857 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00112309 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 349.5821714 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00011040 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 164.3732571 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00002539 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 339.1643429 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00000571 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 153.9554286 * DEG_TO_RAD })));
        final List<UnivariateDifferentiableFunction> wfCenturies = new ArrayList<>();
        final IAUPoleCoefficients1D wCoeffs = buildCoefficientsList(wfDays, wfCenturies);
        final IAUPoleCoefficients coefficients = new IAUPoleCoefficients(alpha0Coeffs, delta0Coeffs, wCoeffs);
        final CelestialBodyIAUOrientation actual = new UserIAUPole(coefficients);

        // Check
        final AbsoluteDate date = new AbsoluteDate(2003, 02, 01, TimeScalesFactory.getTT());
        Assert.assertEquals(0., (reference.getPrimeMeridianAngle(date) - actual.getPrimeMeridianAngle(date))
                / reference.getPrimeMeridianAngle(date), 1E-5);
        Assert.assertEquals(0., reference.getPole(date).distance(actual.getPole(date)), 1E-4);
    }

    /**
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     * @testType UT
     * 
     * @testedFeature {@link features#USER_IAU_POLE}
     * 
     * @description check that user-defined Mercury IAU pole (with values from IAUPoleFactory) returns the exact same
     *              results as built from IAUPoleFactory
     * 
     * @testPassCriteria IAU pole from IAUPoleFactory and defined by UserIAUPole for Mercury are exactly the same.
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testUserIAUPoleFactoryMercury() throws PatriusException {

        // Reference
        final CelestialBodyIAUOrientation reference = IAUPoleFactory.getIAUPole(EphemerisType.MERCURY);
        // Build actual
        final List<UnivariateDifferentiableFunction> alpha0fDays = new ArrayList<>();
        alpha0fDays.add(new PolynomialFunction(new double[] { 281.0097 * DEG_TO_RAD }));
        final List<UnivariateDifferentiableFunction> alpha0fCenturies = new ArrayList<>();
        alpha0fCenturies.add(new PolynomialFunction(new double[] { 0, -0.0328 * DEG_TO_RAD }));
        final IAUPoleCoefficients1D alpha0Coeffs = buildCoefficientsList(alpha0fDays, alpha0fCenturies);
        final List<UnivariateDifferentiableFunction> delta0fDays = new ArrayList<>();
        delta0fDays.add(new PolynomialFunction(new double[] { 61.4143 * DEG_TO_RAD }));
        final List<UnivariateDifferentiableFunction> delta0fCenturies = new ArrayList<>();
        delta0fCenturies.add(new PolynomialFunction(new double[] { 0, -0.0049 * DEG_TO_RAD }));
        final IAUPoleCoefficients1D delta0Coeffs = buildCoefficientsList(delta0fDays, delta0fCenturies);
        final List<UnivariateDifferentiableFunction> wfDays = new ArrayList<>();
        wfDays.add(new PolynomialFunction(new double[] { 329.5469 * DEG_TO_RAD, 6.1385025 * DEG_TO_RAD }));
        wfDays.add(new SineFunction(0.00993822 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 174.791086 * DEG_TO_RAD, 4.092335 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00104581 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 349.582171 * DEG_TO_RAD, 8.184670 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00010280 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 164.373257 * DEG_TO_RAD, 12.277005 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00002364 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 339.164343 * DEG_TO_RAD, 16.369340 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00000532 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 153.955429 * DEG_TO_RAD, 20.461675 * DEG_TO_RAD })));
        final List<UnivariateDifferentiableFunction> wfCenturies = new ArrayList<>();
        final IAUPoleCoefficients1D wCoeffs = buildCoefficientsList(wfDays, wfCenturies);
        final IAUPoleCoefficients coefficients = new IAUPoleCoefficients(alpha0Coeffs, delta0Coeffs, wCoeffs);
        final CelestialBodyIAUOrientation actual = new UserIAUPole(coefficients);

        // Check
        final AbsoluteDate date = new AbsoluteDate(2003, 02, 01, TimeScalesFactory.getTT());

        Assert.assertEquals(0., (reference.getPrimeMeridianAngle(date) - actual.getPrimeMeridianAngle(date))
                / reference.getPrimeMeridianAngle(date), 0);
        Assert.assertEquals(0., reference.getPole(date).distance(actual.getPole(date)), 0);
    }

    /**
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     * @testType UT
     * 
     * @testedFeature {@link features#USER_IAU_POLE}
     * 
     * @description check that user-defined Jupiter IAU pole (with values from IAU NT) returns the same results as built
     *              from IAUPoleFactory
     * 
     * @testPassCriteria IAU pole from IAUPoleFactory and defined by UserIAUPole for Jupiter are the same (relative
     *                   threshold: 0).
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testUserIAUPoleNTJupiter() throws PatriusException {

        // Reference
        final CelestialBodyIAUOrientation reference = IAUPoleFactory.getIAUPole(EphemerisType.JUPITER);
        // Build actual
        // Alpha 0
        final List<UnivariateDifferentiableFunction> alpha0fDays = new ArrayList<>();
        alpha0fDays.add(new PolynomialFunction(new double[] { 268.056595 * DEG_TO_RAD }));
        final List<UnivariateDifferentiableFunction> alpha0fCenturies = new ArrayList<>();
        alpha0fCenturies.add(new PolynomialFunction(new double[] { 0, -0.006499 * DEG_TO_RAD }));
        alpha0fCenturies.add(new SineFunction(0.000117 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 99.360714 * DEG_TO_RAD, 4850.4046 * DEG_TO_RAD })));
        alpha0fCenturies.add(new SineFunction(0.000938 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 175.895369 * DEG_TO_RAD, 1191.9605 * DEG_TO_RAD })));
        alpha0fCenturies.add(new SineFunction(0.001432 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 300.323162 * DEG_TO_RAD, 262.5475 * DEG_TO_RAD })));
        alpha0fCenturies.add(new SineFunction(0.000030 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 114.012305 * DEG_TO_RAD, 6070.2476 * DEG_TO_RAD })));
        alpha0fCenturies.add(new SineFunction(0.00215 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 49.511251 * DEG_TO_RAD, 64.3 * DEG_TO_RAD })));
        final IAUPoleCoefficients1D alpha0Coeffs = buildCoefficientsList(alpha0fDays, alpha0fCenturies);
        // Delta 0
        final List<UnivariateDifferentiableFunction> delta0fDays = new ArrayList<>();
        delta0fDays.add(new PolynomialFunction(new double[] { 64.495303 * DEG_TO_RAD }));
        final List<UnivariateDifferentiableFunction> delta0fCenturies = new ArrayList<>();
        delta0fCenturies.add(new PolynomialFunction(new double[] { 0, 0.002413 * DEG_TO_RAD }));
        delta0fCenturies.add(new CosineFunction(0.00005 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 99.360714 * DEG_TO_RAD, 4850.4046 * DEG_TO_RAD })));
        delta0fCenturies.add(new CosineFunction(0.000404 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 175.895369 * DEG_TO_RAD, 1191.9605 * DEG_TO_RAD })));
        delta0fCenturies.add(new CosineFunction(0.000617 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 300.323162 * DEG_TO_RAD, 262.5475 * DEG_TO_RAD })));
        delta0fCenturies.add(new CosineFunction(-0.000013 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 114.012305 * DEG_TO_RAD, 6070.2476 * DEG_TO_RAD })));
        delta0fCenturies.add(new CosineFunction(0.000926 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 49.511251 * DEG_TO_RAD, 64.3 * DEG_TO_RAD })));
        final IAUPoleCoefficients1D delta0Coeffs = buildCoefficientsList(delta0fDays, delta0fCenturies);
        // W
        final List<UnivariateDifferentiableFunction> wfDays = new ArrayList<>();
        wfDays.add(new PolynomialFunction(new double[] { 284.95 * DEG_TO_RAD, 870.536 * DEG_TO_RAD }));
        final List<UnivariateDifferentiableFunction> wfCenturies = new ArrayList<>();
        final IAUPoleCoefficients1D wCoeffs = buildCoefficientsList(wfDays, wfCenturies);
        final IAUPoleCoefficients coefficients = new IAUPoleCoefficients(alpha0Coeffs, delta0Coeffs, wCoeffs);
        final CelestialBodyIAUOrientation actual = new UserIAUPole(coefficients);

        // Check
        final AbsoluteDate date = new AbsoluteDate(2003, 02, 01, TimeScalesFactory.getTT());
        Assert.assertEquals(0., (reference.getPrimeMeridianAngle(date) - actual.getPrimeMeridianAngle(date))
                / reference.getPrimeMeridianAngle(date), 0);
        Assert.assertEquals(0., reference.getPole(date).distance(actual.getPole(date)), 0);
    }

    /**
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     * @testType UT
     * 
     * @testedFeature {@link features#USER_IAU_POLE}
     * 
     * @description check that Jupiter IAU pole (with values from IAU NT) derivatives returns the same results as
     *              computed by finite differences
     * 
     * @testPassCriteria IAU pole derivatives from IAUPoleFactory and reference are the same (relative threshold: 1E-3,
     *                   due to finite differences approximation).
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testUserIAUPoleDerivativesJupiter() throws PatriusException {
        // Build Jupiter
        final CelestialBodyIAUOrientation jupiter = IAUPoleFactory.getIAUPole(EphemerisType.JUPITER);

        // Check derivatives by finite differences
        final AbsoluteDate date = new AbsoluteDate(2003, 02, 01, TimeScalesFactory.getTT());
        // Pole
        final Vector3D actualPole = jupiter.getPoleDerivative(date).normalize();
        final Vector3D referencePole = (jupiter.getPole(date.shiftedBy(5.)).subtract(jupiter.getPole(date
            .shiftedBy(-5.)))).scalarMultiply(1 / 10.).normalize();
        Assert.assertEquals(0, actualPole.distance(referencePole) / referencePole.getNorm(), 1E-3);

        // W
        final double actualW = jupiter.getPrimeMeridianAngleDerivative(date);
        final double referenceW = (jupiter.getPrimeMeridianAngle(date.shiftedBy(5.)) - jupiter
            .getPrimeMeridianAngle(date.shiftedBy(-5.))) / 10.;
        Assert.assertEquals(0, (referenceW - actualW) / referenceW, 2E-9);
    }

    /**
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     * @testType UT
     * 
     * @testedFeature {@link features#USER_IAU_POLE}
     * 
     * @description check that user-defined Phobos IAU pole (with values from IAU NT) can be built
     * 
     * @testPassCriteria no exception is returned, user-defined Phobos IAU pole can be built.
     * 
     * @referenceVersion 4.7
     * 
     * @nonRegressionVersion 4.7
     */
    @Test
    public void testUserIAUPoleNTPhobos() throws PatriusException {

        // Build actual
        final List<UnivariateDifferentiableFunction> alpha0fDays = new ArrayList<>();
        alpha0fDays.add(new PolynomialFunction(new double[] { 317.67071657 * DEG_TO_RAD }));
        final List<UnivariateDifferentiableFunction> alpha0fCenturies = new ArrayList<>();
        alpha0fCenturies.add(new PolynomialFunction(new double[] { 0, -0.10844326 * DEG_TO_RAD }));
        alpha0fCenturies.add(new SineFunction(-1.78428399 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 190.72645543 * DEG_TO_RAD, 15917.10818695 * DEG_TO_RAD })));
        alpha0fCenturies.add(new SineFunction(0.02212824 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 21.46892470 * DEG_TO_RAD, 31834.27934054 * DEG_TO_RAD })));
        alpha0fCenturies.add(new SineFunction(-0.01028251 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 332.86082793 * DEG_TO_RAD, 19139.89694742 * DEG_TO_RAD })));
        alpha0fCenturies.add(new SineFunction(-0.00475595 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 394.93256437 * DEG_TO_RAD, 39280.79631835 * DEG_TO_RAD })));
        final IAUPoleCoefficients1D alpha0Coeffs = buildCoefficientsList(alpha0fDays, alpha0fCenturies);

        final List<UnivariateDifferentiableFunction> delta0fDays = new ArrayList<>();
        delta0fDays.add(new PolynomialFunction(new double[] { 52.88627266 * DEG_TO_RAD }));
        final List<UnivariateDifferentiableFunction> delta0fCenturies = new ArrayList<>();
        delta0fCenturies.add(new PolynomialFunction(new double[] { 0, 0.00668626 * DEG_TO_RAD }));
        delta0fCenturies.add(new CosineFunction(-1.07516537 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 190.72645543 * DEG_TO_RAD, 15917.10818695 * DEG_TO_RAD })));
        delta0fCenturies.add(new CosineFunction(0.00668626 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 21.46892470 * DEG_TO_RAD, 31834.27934054 * DEG_TO_RAD })));
        delta0fCenturies.add(new CosineFunction(-0.00648740 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 332.86082793 * DEG_TO_RAD, 19139.89694742 * DEG_TO_RAD })));
        delta0fCenturies.add(new CosineFunction(0.00281576 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 394.93256437 * DEG_TO_RAD, 39280.79631835 * DEG_TO_RAD })));
        final IAUPoleCoefficients1D delta0Coeffs = buildCoefficientsList(delta0fDays, delta0fCenturies);

        final List<UnivariateDifferentiableFunction> wfDays = new ArrayList<>();
        wfDays.add(new PolynomialFunction(new double[] { 34.9964842535 * DEG_TO_RAD, 1128.84475928 * DEG_TO_RAD }));
        final List<UnivariateDifferentiableFunction> wfCenturies = new ArrayList<>();
        wfCenturies.add(new PolynomialFunction(new double[] { 0, 0, 12.72192797 * DEG_TO_RAD }));
        wfCenturies.add(new SineFunction(1.42421769 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 190.72645543 * DEG_TO_RAD, 15917.10818695 * DEG_TO_RAD })));
        wfCenturies.add(new SineFunction(-0.02273783 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 21.46892470 * DEG_TO_RAD, 31834.27934054 * DEG_TO_RAD })));
        wfCenturies.add(new SineFunction(0.00410711 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 332.86082793 * DEG_TO_RAD, 19139.89694742 * DEG_TO_RAD })));
        wfCenturies.add(new SineFunction(0.00631964 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 394.93256437 * DEG_TO_RAD, 39280.79631835 * DEG_TO_RAD })));
        wfCenturies.add(new SineFunction(1.143 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 189.6327156 * DEG_TO_RAD, 41215158.1842005 * DEG_TO_RAD, 12.71192322 * DEG_TO_RAD })));
        final IAUPoleCoefficients1D wCoeffs = buildCoefficientsList(wfDays, wfCenturies);
        final IAUPoleCoefficients coefficients = new IAUPoleCoefficients(alpha0Coeffs, delta0Coeffs, wCoeffs);
        final CelestialBodyIAUOrientation actual = new UserIAUPole(coefficients);

        // Check not null
        final AbsoluteDate date = new AbsoluteDate(2003, 02, 01, TimeScalesFactory.getTT());
        Assert.assertNotNull(actual.getPrimeMeridianAngle(date));
        Assert.assertNotNull(actual.getPole(date));
    }

    /**
     * @throws PatriusException
     *         if attitude cannot be computed for provided date
     * @testType UT
     * 
     * @testedFeature {@link features#USER_IAU_POLE}
     * 
     * @description check that user-defined IAU pole (with null values) can be built
     * 
     * @testPassCriteria no exception is returned, user-defined pole with null values can be built.
     * 
     * @referenceVersion 4.8
     * 
     * @nonRegressionVersion 4.8
     */
    @Test
    public void testUserIAUPoleNull() throws PatriusException {

        // Build actual with null lists
        final IAUPoleCoefficients1D alpha0Coeffs = buildCoefficientsList(null, null);

        final List<UnivariateDifferentiableFunction> delta0fCenturies = new ArrayList<>();
        delta0fCenturies.add(new PolynomialFunction(new double[] { 0, 0.00668626 }));
        delta0fCenturies.add(new CosineFunction(-1.07516537, new PolynomialFunction(
            new double[] { 190.72645543, 15917.10818695 })));
        delta0fCenturies.add(new CosineFunction(0.00668626, new PolynomialFunction(
            new double[] { 21.46892470, 31834.27934054 })));
        delta0fCenturies.add(new CosineFunction(-0.00648740, new PolynomialFunction(
            new double[] { 332.86082793, 19139.89694742 })));
        delta0fCenturies.add(new CosineFunction(0.00281576, new PolynomialFunction(
            new double[] { 394.93256437, 39280.79631835 })));
        final IAUPoleCoefficients1D delta0Coeffs = buildCoefficientsList(null, delta0fCenturies);

        final List<UnivariateDifferentiableFunction> wfDays = new ArrayList<>();
        wfDays.add(new PolynomialFunction(new double[] { 34.9964842535, 1128.84475928 }));
        final IAUPoleCoefficients1D wCoeffs = buildCoefficientsList(wfDays, null);
        final IAUPoleCoefficients coefficients = new IAUPoleCoefficients(alpha0Coeffs, delta0Coeffs, wCoeffs);
        final CelestialBodyIAUOrientation actual = new UserIAUPole(coefficients);

        // Check not null
        final AbsoluteDate date = new AbsoluteDate(2003, 02, 01, TimeScalesFactory.getTT());
        Assert.assertNotNull(actual.getPrimeMeridianAngle(date));
        Assert.assertNotNull(actual.getPole(date));
        Assert.assertNotNull(actual.getPrimeMeridianAngleDerivative(date));
        Assert.assertNotNull(actual.getPoleDerivative(date));
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @testType UT
     * 
     * @description Builds a new instance and tests the angular coordinates getters.
     * 
     * @testPassCriteria The angular coordinates getters return the expected data.
     * 
     * @referenceVersion 4.13
     * 
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testAngularCoordinates() throws PatriusException {

        final AbsoluteDate date0 = new AbsoluteDate(2000, 1, 1, 12, 0, 0, TimeScalesFactory.getTDB());
        final AbsoluteDate date1 = date0.shiftedBy(35.);
        final Frame icrf = FramesFactory.getICRF();

        // Build actual
        final List<UnivariateDifferentiableFunction> alpha0fDays = new ArrayList<>();
        alpha0fDays.add(new PolynomialFunction(new double[] { 281.0097 * DEG_TO_RAD }));
        final List<UnivariateDifferentiableFunction> alpha0fCenturies = new ArrayList<>();
        alpha0fCenturies.add(new PolynomialFunction(new double[] { 0, -0.0328 * DEG_TO_RAD }));
        final IAUPoleCoefficients1D alpha0Coeffs = buildCoefficientsList(alpha0fDays, alpha0fCenturies);
        final List<UnivariateDifferentiableFunction> delta0fDays = new ArrayList<>();
        delta0fDays.add(new PolynomialFunction(new double[] { 61.4143 * DEG_TO_RAD }));
        final List<UnivariateDifferentiableFunction> delta0fCenturies = new ArrayList<>();
        delta0fCenturies.add(new PolynomialFunction(new double[] { 0, -0.0049 * DEG_TO_RAD }));
        final IAUPoleCoefficients1D delta0Coeffs = buildCoefficientsList(delta0fDays, delta0fCenturies);
        final List<UnivariateDifferentiableFunction> wfDays = new ArrayList<>();
        wfDays.add(new PolynomialFunction(new double[] { 329.5469 * DEG_TO_RAD, 6.1385025 * DEG_TO_RAD }));
        wfDays.add(new SineFunction(0.00993822 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 174.791086 * DEG_TO_RAD, 4.092335 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00104581 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 349.582171 * DEG_TO_RAD, 8.184670 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00010280 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 164.373257 * DEG_TO_RAD, 12.277005 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00002364 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 339.164343 * DEG_TO_RAD, 16.369340 * DEG_TO_RAD })));
        wfDays.add(new SineFunction(-0.00000532 * DEG_TO_RAD, new PolynomialFunction(
            new double[] { 153.955429 * DEG_TO_RAD, 20.461675 * DEG_TO_RAD })));
        final List<UnivariateDifferentiableFunction> wfCenturies = new ArrayList<>();
        final IAUPoleCoefficients1D wCoeffs = buildCoefficientsList(wfDays, wfCenturies);
        final IAUPoleCoefficients coefficients = new IAUPoleCoefficients(alpha0Coeffs, delta0Coeffs, wCoeffs);
        final CelestialBodyIAUOrientation orientation = new UserIAUPole(coefficients);

        // Evaluate the getAngularCoordinates method
        final AbsoluteDate date0Bis = date0.shiftedBy(10.);

        // ICRF_TO_INERTIAL
        // Already validated through the original AbstractCelestialBody validation

        // INERTIAL_TO_ROTATING
        // date0Bis & no IAUPoleModelType specification (should consider TRUE by default)
        double w = orientation.getPrimeMeridianAngle(date0Bis);
        double wdot = orientation.getPrimeMeridianAngleDerivative(date0Bis);
        Assert.assertEquals(new AngularCoordinates(new Rotation(Vector3D.PLUS_K, w),
            new Vector3D(wdot, Vector3D.PLUS_K)),
            orientation.getAngularCoordinates(date0Bis, OrientationType.INERTIAL_TO_ROTATING));

        // date1 & specify IAUPoleModelType.MEAN
        w = orientation.getPrimeMeridianAngle(date1, IAUPoleModelType.MEAN);
        wdot = orientation.getPrimeMeridianAngleDerivative(date1, IAUPoleModelType.MEAN);
        Assert.assertEquals(new AngularCoordinates(new Rotation(Vector3D.PLUS_K, w),
            new Vector3D(wdot, Vector3D.PLUS_K)),
            orientation.getAngularCoordinates(date1, OrientationType.INERTIAL_TO_ROTATING, IAUPoleModelType.MEAN));

        // ICRF_TO_ROTATING
        // Temp1 & temp2 are already validated, we use them to build the frames composition
        // date0Bis & no IAUPoleModelType specification (should consider TRUE by default)
        AngularCoordinates temp1 = orientation.getAngularCoordinates(date0Bis, OrientationType.ICRF_TO_INERTIAL);
        AngularCoordinates temp2 = orientation.getAngularCoordinates(date0Bis, OrientationType.INERTIAL_TO_ROTATING);
        Frame f1 = new Frame(icrf, new Transform(date0Bis, temp1), "f1");
        Frame f2 = new Frame(f1, new Transform(date0Bis, temp2), "f2");
        Transform t = icrf.getTransformTo(f2, date0Bis);
        Assert.assertEquals(t.getAngular(),
            orientation.getAngularCoordinates(date0Bis, OrientationType.ICRF_TO_ROTATING));

        // date1 & specify IAUPoleModelType.MEAN
        temp1 = orientation.getAngularCoordinates(date1, OrientationType.ICRF_TO_INERTIAL, IAUPoleModelType.MEAN);
        temp2 = orientation.getAngularCoordinates(date1, OrientationType.INERTIAL_TO_ROTATING, IAUPoleModelType.MEAN);
        f1 = new Frame(icrf, new Transform(date1, temp1), "f1");
        f2 = new Frame(f1, new Transform(date1, temp2), "f2");
        t = icrf.getTransformTo(f2, date1);
        Assert.assertEquals(t.getAngular(),
            orientation.getAngularCoordinates(date1, OrientationType.ICRF_TO_ROTATING, IAUPoleModelType.MEAN));

        // getAngularCoordinates(AbsoluteDate, IAUPoleModelType) method should call the ICRF_TO_ROTATING mode
        Assert.assertEquals(orientation.getAngularCoordinates(date0Bis),
            orientation.getAngularCoordinates(date0Bis, OrientationType.ICRF_TO_ROTATING));
        Assert.assertEquals(orientation.getAngularCoordinates(date1, IAUPoleModelType.MEAN),
            orientation.getAngularCoordinates(date1, OrientationType.ICRF_TO_ROTATING, IAUPoleModelType.MEAN));
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#USER_IAU_POLE}
     * 
     * @description test toString method of {@link UserIAUPole}
     * 
     * @testPassCriteria String is as expected
     * 
     * @referenceVersion 4.9
     * 
     * @nonRegressionVersion 4.9
     */
    @Test
    public void testToString() {
        final CelestialBodyIAUOrientation iauPole = new UserIAUPole(null);
        Assert.assertEquals("User-defined coefficients", iauPole.toString());
    }

    /**
     * Convert of list of IAU functions into a map if IAU functions.
     * 
     * @param functionInDays list of functions in days
     * @param functionInCenturies list of functions in centuries
     * @return
     */
    private static IAUPoleCoefficients1D
        buildCoefficientsList(final List<UnivariateDifferentiableFunction> functionInDays,
                              final List<UnivariateDifferentiableFunction> functionInCenturies) {
        List<IAUPoleFunction> functions = new ArrayList<>();
        if (functionInDays != null) {
            for (final UnivariateDifferentiableFunction f : functionInDays) {
                functions.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, f, IAUTimeDependency.DAYS));
            }
        }
        if (functionInCenturies != null) {
            for (final UnivariateDifferentiableFunction f : functionInCenturies) {
                functions.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, f, IAUTimeDependency.CENTURIES));
            }
        }
        if (functionInDays == null && functionInCenturies == null) {
            functions = null;
        }
        return new IAUPoleCoefficients1D(functions);
    }
}
