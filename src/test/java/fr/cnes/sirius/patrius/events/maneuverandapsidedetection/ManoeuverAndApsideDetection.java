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
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:105:21/11/2013: class creation.
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:226:12/09/2014: problem with event detections.
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.maneuverandapsidedetection;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.AbstractIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SimpleMassModel;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.ApsideDetector;
import fr.cnes.sirius.patrius.propagation.events.DateDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Calculs tests avec manoeuvre impulsionnelle générant Evt2
 * 
 * @author François DESCLAUX
 * 
 * @since 2.1
 */

public class ManoeuverAndApsideDetection {

    private static int DOP = 0;
    private static int KEP = 1;

    private static boolean result = false;

    @Test
    public void main() throws IllegalArgumentException,
                      PatriusException, IOException, ParseException {

        System.out.println(" -- Test Discont Events -- ");

        final int mode = KEP;
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003ConfigurationWOEOP(true));
        final double mu = Constants.EGM96_EARTH_MU;
        // Orbite initiale, propagateur
        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate initDate = new AbsoluteDate("2010-01-23",
            TimeScalesFactory.getUTC());
        final double a = 20000.e3;
        final double e = 0.5;
        final double inc = MathLib.toRadians(0);
        final double pom = MathLib.toRadians(0);
        final double gom = MathLib.toRadians(0);
        final double M = MathLib.toRadians(90);
        final MassProvider mass = new SimpleMassModel(1000, "part");
        final Orbit initialOrbit = new KeplerianOrbit(a, e, inc, pom, gom, M,
            PositionAngle.TRUE, gcrf, initDate, Constants.WGS84_EARTH_MU);
        System.out.println("INIT : " + initialOrbit.getDate());
        System.out.println("Period (hrs): " + initialOrbit.getKeplerianPeriod()
            / 3600);
        Vector3D pos = initialOrbit.getPVCoordinates().getPosition();
        Vector3D vel = initialOrbit.getPVCoordinates().getVelocity();
        System.out.println("Vertical Speed : "
            + pos.normalize().dotProduct(vel));

        Propagator propagator = null;
        final AttitudeProvider earthPointingAtt = new BodyCenterPointing(FramesFactory.getITRF());

        if (mode == DOP) {

            // PROPAG NUMERIQUE
            final AbstractIntegrator absInteg = new DormandPrince853Integrator(0.001,
                200., 0.001, 1e-6);
            propagator = new NumericalPropagator(absInteg);
            propagator.resetInitialState(new SpacecraftState(initialOrbit, mass));

            propagator.setAttitudeProvider(earthPointingAtt);

            ((NumericalPropagator) propagator)
                .setOrbitType(OrbitType.CARTESIAN);

        } else {
            // PROPAGATEUR KEPLERIEN
            propagator = new KeplerianPropagator(initialOrbit, earthPointingAtt, mu, mass);
            ((KeplerianPropagator) propagator).addAdditionalStateProvider(mass);
        }

        // Detecteurs d'evenements
        final double maxCheck = 100.;
        final double threshold = 1.e-8;

        // Detecteur passage à rayon maximal (ApsideDetector.g = dot(X,V))
        final EventDetector apsideDet2 = new ApsideDetector(ApsideDetector.APOGEE,
            maxCheck, threshold){
            private static final long serialVersionUID = 498999883343982274L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                System.out.println("DET2 OCCURRED \nMax Radius at : " + s.getDate());
                final Vector3D pos = s.getPVCoordinates().getPosition();
                final Vector3D vel = s.getPVCoordinates().getVelocity();
                System.out.println("Vertical Speed : "
                    + pos.normalize().dotProduct(vel));
                System.out.println("g = :" + this.g(s));
                result = true;
                return Action.CONTINUE;
            }

            @Override
            public double g(final SpacecraftState state)
                throws PatriusException {
                final PVCoordinates pv = state.getPVCoordinates();
                final double gval = Vector3D.dotProduct(pv.getPosition(), pv.getVelocity());
                return gval;
            }
        };

        // Manoeuvre impulsionnelle
        final AbsoluteDate tman = initDate.shiftedBy(120.);
        final EventDetector det1 = new DateDetector(tman, maxCheck, threshold){
            private static final long serialVersionUID = -7935065157760087866L;

            @Override
            public
                    Action
                    eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                                                                                                           throws PatriusException {
                System.out.println("DET1 OCCURRED : " + s.getDate());
                return Action.STOP;
            }
        };
        final ImpulseManeuver impulse = new ImpulseManeuver(det1, new Vector3D(
            0, 0, 3000), 321., "part");

        // Ajout detecteurs au propagateur
        propagator.addEventDetector(impulse);
        propagator.addEventDetector(apsideDet2);

        if (mode == DOP) {
            System.out.println("    =   NUM PROPAGATION START    =");
            propagator.setEphemerisMode();
        } else {
            System.out.println("    =   KEPLERIAN PROPAGATION START    =");
        }

        final SpacecraftState s = propagator.propagate(initDate.shiftedBy(130.));
        System.out.println("Final Date : " + s.getDate());
        pos = s.getPVCoordinates().getPosition();
        vel = s.getPVCoordinates().getVelocity();
        System.out.println("Vertical Speed : "
            + pos.normalize().dotProduct(vel));
        System.out.println("    =   PROPAGATION END      =");

        Assert.assertTrue(result);
    }
}
