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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:64:30/05/2013:update with renamed classes
 * VERSION::DM:91:26/07/2013:test modification
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:605:30/09/2016:gathered Meeus models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.SimpleExponentialAtmosphere;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.RungeKutta6Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaAeroModel;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaAtmosphericDrag;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaCd;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaTesseralAttraction;
import fr.cnes.sirius.patrius.stela.forces.radiation.StelaSRPSquaring;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the class StelaDifferentialEquations.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaDifferentialEquationsTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Stela GTO differential equations system
         * 
         * @featureDescription test the Stela GTO differential equations system
         * 
         * @coveredRequirements
         */
        STELA_GTO_DIFFERENTIAL_EQUATIONS
    }

    /** A Stela GTO propagator. */
    private StelaGTOPropagator propagator;

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_DIFFERENTIAL_EQUATIONS}
     * 
     * @testedMethod {@link StelaDifferentialEquations#StelaDifferentialEquations(StelaGTOPropagator)}
     * @testedMethod {@link StelaDifferentialEquations#computeDerivatives(double, double[], double[])}
     * 
     * @description test the computation of the time derivative of the state vector when no forces are added
     *              to the propagator (only keplerian contribution).
     * 
     * @input a Stela GTO propagator
     * 
     * @output the time derivative of the state vector
     * 
     * @testPassCriteria the output derivatives are the expected one
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testDerivativesComputation() throws PatriusException {
        final StelaDifferentialEquations equations = new StelaDifferentialEquations(this.propagator);
        final KeplerianOrbit keplerian = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
            0.048363, PositionAngle.TRUE, FramesFactory.getMOD(false), new AbsoluteDate(new DateComponents(2008,
                01, 15), new TimeComponents(5, 30, 5.), TimeScalesFactory.getUTC()), Constants.EGM96_EARTH_MU);
        final StelaEquinoctialOrbit or = new StelaEquinoctialOrbit(keplerian);
        final double[] y = { or.getA(), or.getLM(), or.getEquinoctialEx(), or.getEquinoctialEy(), or.getIx(),
            or.getIy() };
        final double[] yDot = new double[6];
        equations.computeDerivatives(5000.56, y, yDot);
        // Only yDot[5] is not zero:
        Assert.assertEquals(0.0, yDot[0], 0.0);
        Assert.assertEquals(0.0, yDot[2], 0.0);
        Assert.assertEquals(0.0, yDot[3], 0.0);
        Assert.assertEquals(0.0, yDot[4], 0.0);
        Assert.assertEquals(0.0, yDot[5], 0.0);
        Assert.assertEquals(MathLib.sqrt(Constants.EGM96_EARTH_MU / 24464560.0) / 24464560.0, yDot[1], 0.0);

        // updateQuad
        // Check result
        try {
            equations.updateQuads(null, new StelaTesseralAttraction(null));
            Assert.assertTrue(false);
        } catch (final Exception e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_DIFFERENTIAL_EQUATIONS}
     * 
     * @testedMethod {@link StelaDifferentialEquations#StelaDifferentialEquations(StelaGTOPropagator)}
     * @testedMethod {@link StelaDifferentialEquations#getDimension()}
     * 
     * @description tests the gaussForcesYDot method
     * 
     * @input a Stela GTO propagator
     * 
     * @output the time derivative of the state vector
     * 
     * @testPassCriteria the output derivatives are the expected one
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     */
    @Test
    public void testGaussForcesYDot() throws PatriusException {

        final StelaEquinoctialOrbit equorbit = new StelaEquinoctialOrbit(
            new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.TRUE, FramesFactory.getMOD(false), new AbsoluteDate(),
                Constants.EGM96_EARTH_MU));

        final StelaGTOPropagator propDrag = new StelaGTOPropagator(new RungeKutta6Integrator(86400), 5, 100){

            @Override
            public double[] getdDragdt() {

                final double[] out = { 0, 0, 0, 0, 0, 0 };
                return out;
            }
        };
        final StelaGTOPropagator propDrag2 = new StelaGTOPropagator(new RungeKutta6Integrator(86400), 5, 100){
            @Override
            public boolean isRecomputeDrag() {

                return false;
            }

            @Override
            public double[] getdDragdt() {

                final double[] out = { 0, 0, 0, 0, 0, 0 };
                return out;
            }
        };
        propDrag.setInitialState(new SpacecraftState(equorbit), 1000, false);
        propDrag2.setInitialState(new SpacecraftState(equorbit), 1000, false);
        final Frame mod = FramesFactory.getMOD(false);
        final Atmosphere atmosphere = new SimpleExponentialAtmosphere(
            new OneAxisEllipsoid(Utils.ae, 1.0 / 298.257222101, mod),
            0.0004, 42000.0, 7500.0);
        final StelaAeroModel sp = new StelaAeroModel(1000, new StelaCd(2.2), 10);
        final StelaAtmosphericDrag atmo = new StelaAtmosphericDrag(sp, atmosphere, 33, 6378, 2500000, 2);
        propDrag.addForceModel(atmo);
        propDrag2.addForceModel(atmo);

        final StelaSRPSquaring srp = new StelaSRPSquaring(1000, 10, 1.5, 11, new MeeusSun(MODEL.STELA));
        propDrag.addForceModel(srp);

        final StelaDifferentialEquations equadiff = new StelaDifferentialEquations(propDrag);
        final StelaDifferentialEquations equadiff2 = new StelaDifferentialEquations(propDrag2);

        // Check result
        final double[] doublin1 = { 1, 2, 3, 4, 5, 6 };
        final double[] doublin2 = { 1, 2, 3, 4, 5, 6 };
        final double[] doublin = { 1, 2, 3, 4, 5, 6 };

        equadiff.gaussForcesYDot(doublin, equorbit);
        for (int j = 1; j < doublin.length; j++) {
            Assert.assertFalse(doublin[j] == doublin1[j]);
        }

        equadiff2.gaussForcesYDot(doublin2, equorbit);
        for (int j = 0; j < doublin.length; j++) {
            Assert.assertTrue(doublin2[j] == doublin1[j]);
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_DIFFERENTIAL_EQUATIONS}
     * 
     * @testedMethod {@link StelaDifferentialEquations#StelaDifferentialEquations(StelaGTOPropagator)}
     * @testedMethod {@link StelaDifferentialEquations#getDimension()}
     * 
     * @description tests misc
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testMisc() throws PatriusException {

        final StelaEquinoctialOrbit equorbit = new StelaEquinoctialOrbit(
            new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                0.048363, PositionAngle.TRUE, FramesFactory.getMOD(false), new AbsoluteDate(),
                Constants.EGM96_EARTH_MU));
        final StelaTesseralAttraction tess = new StelaTesseralAttraction(null);
        this.propagator.addForceModel(tess);
        final StelaDifferentialEquations equadiff = new StelaDifferentialEquations(this.propagator){
            @Override
            public int getDimension() {
                int ret;
                if (this.tesseralComputed) {
                    ret = 0;
                } else {
                    ret = 1;
                }
                return ret;
            }
        };

        Assert.assertEquals(1, equadiff.getDimension(), 0);

        equadiff.updateQuads(equorbit, tess);
        
        Assert.assertEquals(0, equadiff.getDimension(), 0);
    }

    /**
     * Setup method
     * 
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    @Before
    public void setUp() throws PatriusException, IOException, ParseException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 01, 15), new TimeComponents(5, 30, 5.),
            TimeScalesFactory.getUTC());

        final KeplerianOrbit keplerian = new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
            0.048363, PositionAngle.TRUE, FramesFactory.getMOD(false), date, Constants.EGM96_EARTH_MU);

        final SpacecraftState initialState = new SpacecraftState(new EquinoctialOrbit(keplerian));

        final ClassicalRungeKuttaIntegrator integrator = new ClassicalRungeKuttaIntegrator(0.1);
        this.propagator = new StelaGTOPropagator(integrator);
        this.propagator.setInitialState(initialState, 1000, false);
    }

    /*
     * NOTE
     * The GRGS reader, and abstract reader below extend the original classes because zonal and tesseral coefficients
     * test
     * values are unnormalized, whereas the original implementations require normalized coefficients. The intent of
     * theses
     * classes is to override the getC and getS method to return the coefficients as read, whitout any further
     * operations.
     */
}
