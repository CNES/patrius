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
 * VERSION::FA:1976:04/12/2018:Anomaly on events detection
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.events.singlegpsvisis;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.AttitudesSequence;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.CelestialBodyPointed;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.events.CodedEvent;
import fr.cnes.sirius.patrius.events.CodedEventsLogger;
import fr.cnes.sirius.patrius.events.GenericCodingEventDetector;
import fr.cnes.sirius.patrius.events.postprocessing.Timeline;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Calcul de visibilités de satellites GPS - Détection d'événements lors d'une discontinuité d'attitude.
 * 
 * @author François DESCLAUX
 * 
 * @since 3.0
 */

public class Main2SingleGpsVisisTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Test computation of GPS visibility with attitude discontinuity
         * 
         * @featureDescription
         * 
         * @coveredRequirements
         */
        SINGLE_GPS_VISIS,
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SINGLE_GPS_VISIS}
     * 
     * @testedMethod {@link NumericalPropagator#addEventDetector(fr.cnes.sirius.patrius.propagation.events.EventDetector)}
     * @testedMethod {@link NumericalPropagator#propagate(AbsoluteDate)}
     * 
     * @description Propagation with eclipse detection. Particular case.
     * 
     * @input a numerical propagator with an input eclipse detector and an input attitude discontinuity
     * 
     * @output eclipse detection
     * 
     * @testPassCriteria 2 eclipse in + 2 eclipse out should be detected
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testEclipseDetection2() throws PatriusException, IOException, ParseException {

        // Initialization
        Utils.setDataRoot("regular-dataPBASE");

        // Orbite initiale, propagateur sat1
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();
        final AbsoluteDate initDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getUTC());
        final AbsoluteDate endDate = initDate.shiftedBy(3 * 60 * 60);
        final AbsoluteDateInterval datesInterval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED,
            initDate, endDate, IntervalEndpointType.CLOSED);
        final Orbit initialOrbit = new KeplerianOrbit(6700.e3, 0.01, FastMath.PI / 2.5, MathLib.toRadians(1.0),
            FastMath.PI / 4.0, 0., PositionAngle.TRUE,
            gcrf, initDate, Constants.WGS84_EARTH_MU);

        // Attitude en Pointage Terre
        final AttitudeLaw earthPointingAtt = new BodyCenterPointing(itrf);
        // Attitude en Pointage Soleil
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final AttitudeLaw sunPointingAtt = new CelestialBodyPointed(gcrf, sun, Vector3D.PLUS_K, Vector3D.MINUS_K,
            Vector3D.PLUS_I);

        // DEUX Detecteurs Eclipses indépendants
        final EventDetector eclipseIn = GPSvisEclDetectors.myEnterEclipseDetector();
        final EventDetector eclipseOut = GPSvisEclDetectors.myExitEclipseDetector();

        // Séquence d'attitude
        final AttitudesSequence attSequence = new AttitudesSequence();
        // Entrée en eclipse = passage en pointage Terre
        attSequence.addSwitchingCondition(sunPointingAtt, eclipseIn, false, true, earthPointingAtt);
        // Sortie d'éclipse = passage en pointage Soleil
        attSequence.addSwitchingCondition(earthPointingAtt, eclipseOut, true, false, sunPointingAtt);

        // Choix de l'attitude initiale
        // Position initiale Nuit
        attSequence.resetActiveProvider(earthPointingAtt);

        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, attSequence);

        // Prise en compte des "SwitchingCondition" pour la sequence d'attitude
        attSequence.registerSwitchEvents(propagator);

        // EVENTS LOGGER
        final CodedEventsLogger logger = new CodedEventsLogger();
        //
        // SATELLITES GPS
        // Orbites
        final GPS_orbits orbs = new GPS_orbits();
        // Propagateur et détecteurs de visi GPS
        int gpsNr = orbs.getSatNb();
        final ArrayList<GenericCodingEventDetector> cedl = new ArrayList<GenericCodingEventDetector>();
        // attention, on ne s'intéresse qu'au GPS n°1
        gpsNr = 1;
        for (int i = 0; i < gpsNr; i++) {
            final KeplerianPropagator GPSPropag = new KeplerianPropagator(orbs.getGpsOrbList().get(i));
            cedl.add(GPSvisEclDetectors.myGPSCodedEventDetector(GPSPropag, i));
            // Ajout des detecteurs au propagateur
            propagator.addEventDetector(logger.monitorDetector(cedl.get(i)));
        }

        // Ajout d'un détecteur et d'un step handler pour tracés.
        propagator.addEventDetector(GPSvisEclDetectors.myEclipseDetector());

        // Propagation
        propagator.propagate(initDate, endDate).getDate();

        // Check
        final Timeline timeLine = new Timeline(logger, datesInterval);
        final List<CodedEvent> events = timeLine.getCodedEventsList();

        Assert.assertEquals(events.size(), 4);
        Assert.assertEquals(events.get(0).getDate().toString(), "2008-01-01T00:29:26.709");
        Assert.assertEquals(events.get(1).getDate().toString(), "2008-01-01T01:26:11.288");
        Assert.assertEquals(events.get(2).getDate().toString(), "2008-01-01T02:00:25.179");
        Assert.assertEquals(events.get(3).getDate().toString(), "2008-01-01T02:57:10.161");
        Assert.assertEquals(events.get(0).getCode(), "GPS 0 IN");
        Assert.assertEquals(events.get(1).getCode(), "GPS 0 OUT");
        Assert.assertEquals(events.get(2).getCode(), "GPS 0 IN");
        Assert.assertEquals(events.get(3).getCode(), "GPS 0 OUT");
    }

    private class GPS_orbits {

        private final int satNb = 24;
        private final ArrayList<KeplerianOrbit> gpsOrbList = new ArrayList<KeplerianOrbit>();

        /**
         * @param args
         * @throws PatriusException
         * @throws IllegalArgumentException
         */
        public GPS_orbits() throws IllegalArgumentException, PatriusException {
            // Epoch of 01 July 1990, 0 hours, 0 minutes, 0 seconds.
            final AbsoluteDate date = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getGPS());
            final Frame GCRFrame = FramesFactory.getGCRF();
            final double sma = 26400000.;
            final double ecc = 1.e-2;
            final double inc = MathLib.toRadians(55.0);
            final double[] pa = new double[this.satNb];
            final double[] raan = new double[this.satNb];
            final double[] trueAnom = new double[this.satNb];
            final double[] raanDeg = { 317.0, 317.0, 317.0, 317.0, 17.0, 17.0,
                17.0, 17.0, 77.0, 77.0, 77.0, 77.0, 137.0, 137.0, 137.0, 137.0,
                197.0, 197.0, 197.0, 197.0, 257.0, 257.0, 257.0, 257.0 };
            final double[] aolDeg = { 280.7, 310.3, 60.0, 173.4, 339.7, 81.9,
                115.0, 213.9, 16.0, 138.7, 244.9, 273.5, 42.1, 70.7, 176.8,
                299.6, 101.7, 200.5, 233.7, 335.9, 142.2, 255.6, 5.3, 34.5 };
            for (int i = 0; i < this.satNb; i++) {
                pa[i] = 0.0;
                raan[i] = MathLib.toRadians(raanDeg[i]);
                trueAnom[i] = MathLib.toRadians(aolDeg[i]) - pa[i];
            }
            for (int i = 0; i < this.satNb; i++) {
                this.gpsOrbList.add(new KeplerianOrbit(sma, ecc, inc, pa[i], raan[i],
                    trueAnom[i], PositionAngle.TRUE, GCRFrame, date,
                    Constants.WGS84_EARTH_MU));
            }
        }

        public int getSatNb() {
            return this.satNb;
        }

        public ArrayList<KeplerianOrbit> getGpsOrbList() {
            return this.gpsOrbList;
        }
    }
}
