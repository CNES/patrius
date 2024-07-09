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
 * @history Created 07/05/2015
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:415:09/03/2015: Attitude discontinuity on event issue
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events.singlegpsvisis;

import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.events.GenericCodingEventDetector;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.CircularFieldOfViewDetector;
import fr.cnes.sirius.patrius.propagation.events.EclipseDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class GPSvisEclDetectors {

    private static GPSvisEclDetectors staticInstance = new GPSvisEclDetectors();

    private GPSvisEclDetectors() {
    }

    // Detecteur éclipse
    public static EventDetector myEclipseDetector() throws PatriusException {
        final double maxCheck = 60.;
        final double threshold = 1.0e-3;
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final PVCoordinatesProvider earth = CelestialBodyFactory.getEarth();
        final EventDetector eclipseDet = new EclipseDetector(sun, Constants.SUN_RADIUS, earth,
            Constants.EGM96_EARTH_EQUATORIAL_RADIUS, 1, maxCheck, threshold){
            private static final long serialVersionUID = -2700953470991125672L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) throws PatriusException {
                // ATTENTION g positif hors éclipse, donc increasing en sortie
                // d'éclipse
                if (!increasing) {
                    System.out.println(s.getDate() + " ENTER Eclipse");
                } else {
                    System.out.println(s.getDate() + " EXIT Eclipse");
                }
                return Action.CONTINUE;
            }
        };
        return eclipseDet;
    }

    // Detecteur d'entrée en éclipse (en réalité, détecte les entrées et sorties)
    public static EventDetector myEnterEclipseDetector() throws PatriusException {
        final double maxCheck = 60.;
        final double threshold = 1.0e-3;
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final PVCoordinatesProvider earth = CelestialBodyFactory.getEarth();
        final EventDetector eclipseDet = new EclipseDetector(sun, Constants.SUN_RADIUS, earth,
            Constants.EGM96_EARTH_EQUATORIAL_RADIUS, 1, maxCheck, threshold){
            private static final long serialVersionUID = -2700953470991125672L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) throws PatriusException {
                // ATTENTION g positif hors éclipse, donc increasing en sortie
                // d'éclipse
                if (!increasing) {
                    System.out.println(s.getDate() + " SwitchEvt-EnterEclipse");
                }
                return Action.RESET_STATE;
            }
        };
        return eclipseDet;
    }

    // Detecteur de sortie d'éclipse (en réalité, détecte les entrées et sorties)
    public static EventDetector myExitEclipseDetector() throws PatriusException {
        final double maxCheck = 60.;
        final double threshold = 1.0e-3;
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final PVCoordinatesProvider earth = CelestialBodyFactory.getEarth();
        final EventDetector eclipseDet = new EclipseDetector(sun, Constants.SUN_RADIUS, earth,
            Constants.EGM96_EARTH_EQUATORIAL_RADIUS, 1, maxCheck, threshold){
            private static final long serialVersionUID = -2700953470991125672L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward)
                                                              throws PatriusException {
                // ATTENTION g positif hors éclipse, donc increasing en sortie
                // d'éclipse
                if (increasing) {
                    System.out.println(s.getDate() + " SwitchEvt-ExitEclipse");
                }
                return Action.RESET_STATE;
            }

        };
        return eclipseDet;
    }

    // Creation d'un GenericCodingEventDetector visi GPS
    // CircularFieldOfViewDetector : handles target entry/exit events
    // with respect to a satellite sensor circular field of view.
    // Utilisé ici pour l'antenne récepteur GPS
    //
    public static GenericCodingEventDetector myGPSCodedEventDetector(final PVCoordinatesProvider gpsSat,
                                                                     final int gpsNr) {
        final double maxCheck = 60.;
        // 1 ANTENNE Recepteur GPS sur le dos du satellite (-Z)
        final Vector3D fovCenter = Vector3D.MINUS_K;
        final double halfAp = MathLib.toRadians(90);
        final CircularFieldOfViewDetector detector = staticInstance.new CustomCircularFieldOfViewDetector(
            maxCheck, gpsSat, fovCenter, halfAp, gpsNr);
        final GenericCodingEventDetector gceDet = new GenericCodingEventDetector(detector, "GPS " + gpsNr
            + " IN", "GPS " + gpsNr + " OUT", true, "GPS " + gpsNr + " VISI");
        return gceDet;
    }

    private class CustomCircularFieldOfViewDetector extends CircularFieldOfViewDetector {

        private final int satNr;

        public CustomCircularFieldOfViewDetector(final double maxCheck, final PVCoordinatesProvider pvTarget,
            final Vector3D center, final double halfAperture, final int satNr) {
            super(pvTarget, center, halfAperture, maxCheck);
            this.satNr = satNr;
        }

        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                    final boolean forward)
                                                          throws PatriusException {
            eventMessage(s, increasing, "GPS " + this.satNr + " visibility ");
            return Action.CONTINUE;
        }

        /*
         * (non-Javadoc)
         * @see org.orekit.propagation.events.CircularFieldOfViewDetector#g(org.orekit
         * .propagation.SpacecraftState)
         */
        @Override
        public double g(final SpacecraftState s) throws PatriusException {
            final double val = super.g(s);
            return val;
        }

    }

    private static void eventMessage(final SpacecraftState s, final boolean increasing,
                                     final String evtName) throws PatriusException {
        final KeplerianOrbit kep = new KeplerianOrbit(s.getOrbit());
        double aol = MathLib.toDegrees(kep.getTrueAnomaly() + kep.getPerigeeArgument());
        if (aol > 360.) {
            aol -= 360.;
        }
        System.out.format(s.getDate() + " (AoL = %8.3f deg) "
            + (increasing ? " ENTER " + evtName : " EXIT  " + evtName) + "%n", aol);
    }
}
