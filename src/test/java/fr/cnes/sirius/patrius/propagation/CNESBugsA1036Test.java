/**
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
 */
/**
 * HISTORY
* VERSION:4.8:DM:DM-2898:15/11/2021:[PATRIUS] Hypothese geocentrique a supprimer pour la SRP 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:90:15/10/2013:Renamed Droziner to UnnormalizedDroziner
 * VERSION::DM:158:24/10/2013:Changed toString in AbsoluteDate
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 *                             (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:363:03/12/2014:Solved toString round date bug
 * VERSION::DM:454:24/11/2015:Unimplemented method shouldBeRemoved()
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.io.IOException;
import java.text.ParseException;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.SphericalSpacecraft;
import fr.cnes.sirius.patrius.forces.gravity.DrozinerAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressureCircular;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * @author cardosop
 */
public class CNESBugsA1036Test {

    // CI -DESSOUS : testALLPROBS, reprise de PbAccesFinEphemeride dans tests-patrius-snapshot
    // A supprimer quand les tests individuels auront été extraits.
    @Test
    @Ignore
    public void testALLPROBS() throws PatriusException, IOException, ParseException {

        // Orbite initiale, propagateur sat1
        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate initDate = new AbsoluteDate("1970-04-01",
            TimeScalesFactory.getUTC());
        final AbsoluteDate evtDate = initDate.shiftedBy(300);
        final AbsoluteDate endDate = initDate.shiftedBy(60 * 60.);
        System.out.println("StartDate : " + initDate);
        System.out.println("EventDate : " + evtDate);
        System.out.println("EndDate : " + endDate);
        final double a = 7000000.0;
        final double e = 0.01;
        final double inc = FastMath.PI / 2.5;
        final double pom = MathLib.toRadians(1.0);
        final double gom = FastMath.PI / 4.0;
        final double M = 0.0;
        final Orbit initialOrbit = new KeplerianOrbit(a, e, inc, pom, gom, M,
            PositionAngle.TRUE, gcrf, initDate, Constants.WGS84_EARTH_MU);

        new myDiscontinuousDateDetector(
            60, 1e-2, evtDate);

        final EventDetector continuousDetector = new myContinuousDateDetector(
            evtDate);

        System.out.println("    =    EPHEM PROPAGATION START    =");
        final NumericalPropagator numPropag = myNumericPropag(initialOrbit);
        numPropag.setEphemerisMode();
        numPropag.propagate(initDate, endDate);
        final BoundedPropagator ephemPropag = numPropag.getGeneratedEphemeris();

        // Ajout d'un événement discontinu
        ephemPropag.addEventDetector(continuousDetector);

        System.out
            .println("1ère tentative sur [T0  ; Tf-0.000001] => ça marche : ");
        System.out
            .println("mais regarder la derniere date: 2000-04-01T00:59:60.000  !!");
        ephemPropag.propagate(initDate, endDate.shiftedBy(-0.000001));

        System.out
            .println("2ème tentative IDENTIQUE sur [T0  ; Tf-0.000001] => ça marche en réinitialisant le détecteur : ");
        ephemPropag.clearEventsDetectors();
        ephemPropag.addEventDetector(new myDiscontinuousDateDetector(60, 1e-2,
            evtDate));
        ephemPropag.propagate(initDate, endDate.shiftedBy(-0.000001));

        System.out
            .println("3ème tentative IDENTIQUE sur [T0  ; Tf-0.000001]] => la detection d'evt ne marche pas sans réinitialiser le détecteur : ");
        // Problème n° 1 (enlever le // pour mettre en évidence)
        // ephemPropag.propagate(initDate, endDate.shiftedBy(-0.000001));

        System.out
            .println("4ème tentative sur [T0  ; Tf] (tolérances en 1e-3) : ça fonctionne... ");
        // Problème n° 2 (enlever le // pour mettre en évidence)
        ephemPropag.clearEventsDetectors();
        ephemPropag.addEventDetector(new myDiscontinuousDateDetector(60, 1e-3,
            evtDate));

        ephemPropag.addEventDetector(new myContinuousDateDetector(60, 1e-3,
            evtDate));
        ephemPropag.propagate(initDate, endDate);

        System.out
            .println("5ème tentative sur [T0  ; Tf] (avec tolérances en 1e-2) : plantage sur dépassement de la date finale");
        // Problème n° 2 (enlever le // pour mettre en évidence)
        ephemPropag.clearEventsDetectors();
        ephemPropag.addEventDetector(new myDiscontinuousDateDetector(60, 1e-2,
            evtDate));

        ephemPropag.addEventDetector(new myContinuousDateDetector(60, 1e-2,
            evtDate));
        ephemPropag.propagate(initDate, endDate);

        System.out.println("    =    EPHEM PROPAGATION END      =");

    }

    @Test
    public void testBugBoundedPropagator() throws PatriusException, IOException, ParseException {

        // Orbite initiale, propagateur sat1
        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate initDate = new AbsoluteDate("1970-04-01",
            TimeScalesFactory.getUTC());
        final AbsoluteDate evtDate = initDate.shiftedBy(300);
        final AbsoluteDate endDate = initDate.shiftedBy(40 * 60.);
        // System.out.println("StartDate : " + initDate);
        // System.out.println("EventDate : " + evtDate);
        // System.out.println("EndDate : " + endDate);
        final double a = 7000000.0;
        final double e = 0.01;
        final double inc = FastMath.PI / 2.5;
        final double pom = MathLib.toRadians(1.0);
        final double gom = FastMath.PI / 4.0;
        final double M = 0.0;
        final Orbit initialOrbit = new KeplerianOrbit(a, e, inc, pom, gom, M,
            PositionAngle.TRUE, gcrf, initDate, Constants.WGS84_EARTH_MU);

        new myDiscontinuousDateDetector(
            60, 1e-2, evtDate);

        new myContinuousDateDetector(
            evtDate);

        // System.out.println("    =    EPHEM PROPAGATION START    =");
        final NumericalPropagator numPropag = myNumericPropag(initialOrbit);
        numPropag.setEphemerisMode();
        numPropag.propagate(initDate, endDate);
        final BoundedPropagator ephemPropag = numPropag.getGeneratedEphemeris();

        ephemPropag.clearEventsDetectors();
        ephemPropag.addEventDetector(new myDiscontinuousDateDetector(60, 1e-2,
            evtDate));

        ephemPropag.addEventDetector(new myContinuousDateDetector(60, 1e-2,
            evtDate));
        try {
            ephemPropag.propagate(initDate, endDate);
        } catch (final PropagationException pe) {
            // Should not happen (boundary bug for t1 in EventState)
            Assert.fail(pe.toString());
        }

    }

    private static class myContinuousDateDetector extends DateDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = 3013292180138805313L;

        public myContinuousDateDetector(final AbsoluteDate target) {
            super(target, 60, 1e-3);
            // TODO Auto-generated constructor stub
        }

        public myContinuousDateDetector(final double maxCheck, final double threshold,
            final AbsoluteDate evtDate) {
            super(evtDate, maxCheck, threshold);

        }

        /** {@inheritDoc} */
        @Override
        public
                Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                       throws PatriusException {

            // System.out.println(s.getDate() + " EVENT OCCURRED ");

            return Action.CONTINUE;
        }

    }

    private static class myDiscontinuousDateDetector extends AbstractDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = 63392434712932988L;
        /** Target date */
        private final AbsoluteDate targetDate;

        public myDiscontinuousDateDetector(final double maxCheck,
            final double threshold, final AbsoluteDate target) {
            super(maxCheck, threshold);
            this.targetDate = target;
        }

        /** {@inheritDoc} */
        @Override
        public double g(final SpacecraftState s) throws PatriusException {
            final double dt = s.getDate().durationFrom(this.targetDate);

            // System.out.println(s.getDate() + " : g = " + ((dt > 0) ? 1 : -1));
            return (dt > 0) ? 1 : -1;
            // discontinuous

        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final SpacecraftState s,
                                    final boolean increasing, final boolean forward) throws PatriusException {
            // System.out.println(s.getDate() + " EVENT OCCURRED ");
            return Action.CONTINUE;
        }

        @Override
        public boolean shouldBeRemoved() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public EventDetector copy() {
            return null;
        }
    }

    public static NumericalPropagator myNumericPropag(final Orbit initialOrbit)
                                                                               throws PatriusException, IOException,
                                                                               ParseException {
        final String DEFAULT = "default";
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-12, 1e-12, 1e-12, 1e-12, 1e-12, 1e-12 };
        final FirstOrderIntegrator dop = new DormandPrince853Integrator(0.1, 100,
            absTOL, relTOL);

        // Reference frame = ITRF 2005
        final Frame itrf = FramesFactory.getITRF();

        final NumericalPropagator prop = new NumericalPropagator(dop);

        // Forces

        // Gravity field
        GravityFieldFactory
            .addPotentialCoefficientsReader(new GRGSFormatReader(
                "grim4s4_gr", true));
        // get the gravity field coefficients provider from the 'grim4s4_gr'
        // file
        final PotentialCoefficientsProvider provider = GravityFieldFactory
            .getPotentialProvider();
        // we get the data as extracted from the file
        final int n = 4; // degree
        final int m = 4; // order
        final double[][] C = provider.getC(n, m, false);
        final double[][] S = provider.getS(n, m, false);

        // return perturbing force (ITRF2008 central body frame)
        final ForceModel potentiel = new DrozinerAttractionModel(
            FramesFactory.getITRF(), provider.getAe(), provider.getMu(), C,
            S);

        // b) third body
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final CelestialBody moon = CelestialBodyFactory.getMoon();

        final ForceModel sunAttraction = new ThirdBodyAttraction(sun);
        final ForceModel moonAttraction = new ThirdBodyAttraction(moon);

        // c) SRP
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.46,
            1.0 / 298.25765, FramesFactory.getITRF());
        final RadiationSensitive vehicle = new SphericalSpacecraft(10., 2.2,
            0.5, 0.5, 0., DEFAULT);

        final ForceModel prs = new SolarRadiationPressureCircular(sun,
            earth.getEquatorialRadius(), vehicle);

        prop.addForceModel(potentiel);
        prop.addForceModel(sunAttraction);
        prop.addForceModel(moonAttraction);

        prop.addForceModel(prs);

        // d) No atmospheric drag

        // Attitude
        final AttitudeProvider earthPointingAtt = new BodyCenterPointing(itrf);
        prop.setAttitudeProvider(earthPointingAtt);

        // Init
        final MassProvider massModel = new SimpleMassModel(1000., DEFAULT);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit, massModel);
        prop.resetInitialState(initialState);
        prop.setMassProviderEquation(massModel);

        return prop;
    }

    @BeforeClass
    public static void setUpBeforeClass() throws PatriusException {
        Utils.setDataRoot("regular-data:potential");
        FramesFactory.setConfiguration(
            Utils.getIERS2003ConfigurationWOEOP(true)
            );
    }

}