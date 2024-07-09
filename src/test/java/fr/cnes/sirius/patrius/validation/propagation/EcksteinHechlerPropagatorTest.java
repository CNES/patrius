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
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:489:13/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.validation.propagation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.ParametersType;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.tools.validationTool.Validate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.validation.orbits.OrbitTest;

/**
 * @description
 *              <p>
 *              - EcksteinHechler Propagator Test
 *              </p>
 *              <p>
 * 
 * @author clauded
 * 
 * @version $Id: EcksteinHechlerPropagatorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 * 
 */

public class EcksteinHechlerPropagatorTest {

    /** validation tool */
    static Validate validate;

    /** Non regression epsilon */
    private final double nonRegEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Epsilon used for distance comparison. */
    private final double epsilonDistance = Utils.epsilonTest;

    /** Epsilon used for distance comparison. */
    private final double epsilonAngle = Utils.epsilonAngle;

    /** Epsilon used for distance comparison. */
    private final double epsilonEcir = Utils.epsilonEcir;

    /**
     * mu
     */
    private double mu;
    /**
     * ae
     */
    private double ae;
    /**
     * c20
     */
    private double c20;
    /**
     * c30
     */
    private double c30;
    /**
     * c40
     */
    private double c40;
    /**
     * c50
     */
    private double c50;
    /**
     * c60
     */
    private double c60;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Circular orbit propagation with Eckstein-Hechler model.
         * 
         * @featureDescription the purpose of this feature is to validate the Eckstein-Hechler
         *                     propagation class compared with MSLIB results
         * 
         * @coveredRequirements DV-PROPAG_10, DV-PROPAG_20
         */
        CIRCULARORBIT
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#CIRCULARORBIT}
     * 
     * @testedMethod {@link EcksteinHechlerPropagator#propagate(AbsoluteDate)}
     * 
     * @description validate the Eckstein-Hechler propagation class compared with Celestlab results
     * 
     * @input CircularOrbit initialOrbit
     * @output a
     * @output ex
     * @output ey
     * @output i
     * @output RAAN
     * @output AoP + M
     * 
     * @testPassCriteria deviations from reference below thresholds. For distances comparison, the epsilon is set to
     *                   1e-12; for angles comparison, the epsilon is set to 1e-7; for eccentricity comparison the
     *                   epsilon is set to 1e-4 (circular orbit). These epsilons take into account the fact that the
     *                   measures are
     *                   physical and that their references come from another software.
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public final void propagatedCircular() throws PatriusException, IOException, ParseException {

        // Comparison with a given extrapolated orbit

        AbsoluteDate initDate;
        initDate = AbsoluteDate.FIFTIES_EPOCH_TT.shiftedBy(12584. * Constants.JULIAN_DAY);

        initDate = initDate.shiftedBy(100);
        double a = 7218419.263470929;
        double ex = 4.507986679304844E-4;
        double ey = 2.2019084841181662E-4;
        double i = 1.7227476282682561;
        double gom = 2.9095797726470324;
        double pso_M = 0.10430136226618855;

        final Orbit initialOrbit = new CircularOrbit(a, ex, ey, i, gom, pso_M,
            PositionAngle.MEAN, FramesFactory.getEME2000(), initDate, this.mu);

        // Extrapolator definition
        final EcksteinHechlerPropagator extrapolatorHechler =
            new EcksteinHechlerPropagator(initialOrbit, this.ae, this.mu, initialOrbit.getFrame(),
                this.c20, this.c30, this.c40, this.c50, this.c60, ParametersType.OSCULATING);

        // Extrapolation at a final date different from initial date
        final double deltat = (3. * Constants.JULIAN_DAY) - 100.;
        final AbsoluteDate extrapDate = initDate.shiftedBy(deltat);

        final SpacecraftState finalOrbit = extrapolatorHechler.propagate(extrapDate);

        // the final orbit (circular parameters)
        final double t = 0.0;
        a = 7218607.863234544;
        ex = -5.523676252101827E-4;
        ey = 1.248838099681473E-4;
        i = 1.7227456636941942;
        gom = 2.9609350905572454;
        pso_M = 6.0800784760254;

        final CircularOrbit ofinalOrbit = new CircularOrbit(finalOrbit.getOrbit());

        validate.assertEqualsWithRelativeTolerance(ofinalOrbit.getDate().durationFrom(extrapDate),
            0.0, Utils.epsilonTest, t, this.nonRegEpsilon, "EcksteinHechlerPropagatorTest Date");
        validate.assertEqualsWithRelativeTolerance(ofinalOrbit.getA(),
            7218607.863234544, this.nonRegEpsilon, a, this.epsilonDistance, "EcksteinHechlerPropagatorTest A");
        validate.assertEqualsWithRelativeTolerance(ofinalOrbit.getCircularEx(),
            -5.523676252101827E-4, this.nonRegEpsilon, ex, this.epsilonEcir, "EcksteinHechlerPropagatorTest Ex");
        validate.assertEqualsWithRelativeTolerance(ofinalOrbit.getCircularEy(),
            1.248838099681473E-4, this.nonRegEpsilon, ey, this.epsilonEcir, "EcksteinHechlerPropagatorTest Ey");
        this.assertAngleEqualsRelative(ofinalOrbit.getI(),
            1.7227456636941942, this.nonRegEpsilon, i, this.epsilonAngle, "EcksteinHechlerPropagatorTest I");
        this.assertAngleEqualsRelative(ofinalOrbit.getRightAscensionOfAscendingNode(),
            2.9609350905572454, this.nonRegEpsilon, gom, this.epsilonAngle, "EcksteinHechlerPropagatorTest RAAN");
        this.assertAngleEqualsRelative(ofinalOrbit.getLM(),
            6.0800784760254, this.nonRegEpsilon, pso_M, this.epsilonAngle, "EcksteinHechlerPropagatorTest LM");
    }

    private void assertAngleEqualsRelative(final double actual, final double nonRegExpected, final double nonRegEps,
                                           final double externalRefExpected, final double externalRefEps,
                                           final String deviationDesciption) {
        final double actual_angle = MathUtils.normalizeAngle(actual, FastMath.PI);
        final double nonRegExpected_angle = MathUtils.normalizeAngle(nonRegExpected, FastMath.PI);
        final double externalRefExpected_angle = MathUtils.normalizeAngle(externalRefExpected, FastMath.PI);
        validate.assertEqualsWithRelativeTolerance(actual_angle, nonRegExpected_angle, nonRegEps,
            externalRefExpected_angle, externalRefEps, deviationDesciption);
    }

    /**
     * setUp()
     * 
     * @since 1.0
     */
    @Before
    public final void setUp() {
        Utils.setDataRoot("regular-data");
        Utils.setDataRoot("regular-data:potential/shm-format");

        this.mu = 3.9860047e14;
        this.ae = 6.378137e6;
        this.c20 = -1.08263e-3;
        this.c30 = 2.54e-6;
        this.c40 = 1.62e-6;
        this.c50 = 2.3e-7;
        this.c60 = -5.5e-7;

        try {
            validate = new Validate(OrbitTest.class);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * tearDown()
     * 
     * @since 1.0
     */
    @After
    public final void tearDown() {
        this.mu = Double.NaN;
        this.ae = Double.NaN;
        this.c20 = Double.NaN;
        this.c30 = Double.NaN;
        this.c40 = Double.NaN;
        this.c50 = Double.NaN;
        this.c60 = Double.NaN;

        try {
            validate.produceLog();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
