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
 * @history creation 23/04/2012
 */
/*
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.6:DM:DM-2624:27/01/2021:[PATRIUS] Correction tropospherique avec le modele d’Azoulay
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:88:18/11/2013: the tropoCorrectionTest has been modified
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.LinearTwoPointsPVProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.signalpropagation.SignalPropagationModel.FixedDate;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.AzoulayModel;
import fr.cnes.sirius.patrius.signalpropagation.troposphere.TroposphericCorrection;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description
 *              <p>
 *              Test class for the signal propagation computation
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class SignalPropagationTest {

    /** Features description. */
    enum features {
        /**
         * @featureTitle signal propagation
         * 
         * @featureDescription Computation of a signal propagation in space
         * 
         * @coveredRequirements DV-MES_FILT_200, DV-MES_FILT_210
         */
        SIGNAL_PROPAGATION,
        /**
         * @featureTitle tropospheric correction the signal
         * 
         * @featureDescription Computation of the tropospheric correction a signal
         * 
         * @coveredRequirements DV-MES_FILT_470
         */
        SIGNAL_TROPO_CORRECTION
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(SignalPropagationTest.class.getSimpleName(), "Signal propagation");
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#SIGNAL_PROPAGATION}
     * 
     * @testedMethod {@link SignalPropagationModel#computeSignalPropagation(PVCoordinatesProvider, PVCoordinatesProvider, AbsoluteDate, FixedDate)}
     * @testedMethod {@link SignalPropagation#getVector(Frame)}
     * @testedMethod {@link SignalPropagation#getStartDate()}
     * @testedMethod {@link SignalPropagation#getEndDate()}
     * @testedMethod {@link SignalPropagation#getFrame()}
     * @testedMethod {@link SignalPropagation#getdPropdPem(Frame)}
     * @testedMethod {@link SignalPropagation#getdPropdPrec(Frame)}
     * 
     * @description computation of a signal propagation between two moving points in the void
     * 
     * @input two PVCoordinatesProviders, a date
     * 
     * @output The associated {@link SignalPropagation} object
     * 
     * @testPassCriteria the output signal propagation contains the right information
     * @throws PatriusException
     *         if frames problems occur
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void propagationInVoidTest() throws PatriusException {

        final double threshold = 1.e-13;
        Report.printMethodHeader("propagationInVoidTest", "Signal propagation computation (in void)", "FDS", threshold,
            ComparisonType.ABSOLUTE);

        // Frame and dates
        final Frame refFrame = FramesFactory.getGCRF();
        final AbsoluteDate startDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate endDate = startDate.shiftedBy(10.);

        // "station"
        final PVCoordinates pvSta1 = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO);
        final PVCoordinates pvSta2 = new PVCoordinates(new Vector3D(1.e7, 0., 0.), Vector3D.ZERO);

        final PVCoordinatesProvider station = new LinearTwoPointsPVProvider(
            pvSta1, startDate, pvSta2, endDate, refFrame);

        // "spacecraft"
        final PVCoordinates pvSpace1 = new PVCoordinates(new Vector3D(1.0e7, 0., 0.), Vector3D.ZERO);
        final PVCoordinates pvSpace2 = new PVCoordinates(new Vector3D(0., 0., 0.), Vector3D.ZERO);

        final PVCoordinatesProvider spacecraft = new LinearTwoPointsPVProvider(
            pvSpace1, startDate, pvSpace2, endDate, refFrame);

        // Model
        final SignalPropagationModel model = new SignalPropagationModel(refFrame, threshold);

        // Station - spacecraft signal computation
        final SignalPropagation goSignal = model.computeSignalPropagation(station, spacecraft,
                startDate, FixedDate.EMISSION);

        // Values tests
        // The position comparison are made with a relative "threshold" value, so 1.0e-6
        final Vector3D vector = goSignal.getVector(refFrame);
        final double expectedDuration = 1.e7 / (Constants.SPEED_OF_LIGHT + 1.e7 / 10.);
        Assert.assertEquals(expectedDuration * Constants.SPEED_OF_LIGHT, vector.getX(), 1.e-6);
        Assert.assertEquals(0., vector.getY(), 1.e-6);
        Assert.assertEquals(0., vector.getZ(), 1.e-6);

        Assert.assertEquals(0., goSignal.getStartDate().durationFrom(startDate), threshold);
        Assert.assertEquals(expectedDuration, goSignal.getEndDate().durationFrom(startDate), threshold);
        Assert.assertEquals(goSignal.getFixedDateType(), FixedDate.EMISSION);
        Assert.assertTrue(goSignal.getFrame().equals(refFrame));

        Report.printToReport("Signal duration", expectedDuration, goSignal.getEndDate().durationFrom(startDate));

        // Same computation from the reception date
        final SignalPropagation goSignal2 = model.computeSignalPropagation(
            station, spacecraft, goSignal.getEndDate(), FixedDate.RECEPTION);

        // Values tests
        final Vector3D vector2 = goSignal2.getVector(refFrame);
        Assert.assertEquals(vector.getX(), vector2.getX(), 1.e-6);
        Assert.assertEquals(vector.getY(), vector2.getY(), 1.e-6);
        Assert.assertEquals(vector.getZ(), vector2.getZ(), 1.e-6);

        Assert.assertEquals(0., goSignal2.getStartDate().durationFrom(startDate), threshold);
        Assert.assertEquals(0., goSignal2.getEndDate().durationFrom(goSignal.getEndDate()),
                threshold);
        Assert.assertEquals(goSignal2.getFixedDateType(), FixedDate.RECEPTION);

        // Wrong model creation
        final Frame wrongFrame = new LocalOrbitalFrame(refFrame, LOFType.TNW, spacecraft, "wrong frame");
        try {
            new SignalPropagationModel(wrongFrame, threshold);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            new SignalPropagation(Vector3D.ZERO, startDate, endDate, wrongFrame, FixedDate.EMISSION);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // For coverage as the signal is already built: try to compute the propagation position
        // vector derivatives
        // wrt the emitter/receiver position with a non pseudo inertial frame (should failed)
        try {
            goSignal.getdPropdPem(FramesFactory.getITRF());
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
        try {
            goSignal.getdPropdPrec(FramesFactory.getITRF());
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#SIGNAL_TROPO_CORRECTION}
     * 
     * @testedMethod {@link SignalPropagationModel#getSignalTropoCorrection(TroposphericCorrection, SignalPropagation, TopocentricFrame)}
     * 
     * @description computation of the tropospheric correction of a signal arriving to a ground station
     *              knowing its topocentric frame.
     * 
     * @input a signal propagation object, a topocentric frame, a tropospheric correction model
     * 
     * @output The elevation and distance corrections
     * 
     * @testPassCriteria the corrections are the expected ones
     * @throws PatriusException
     *         if frames problems occur
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void tropoCorrectionTest() throws PatriusException {

        Report.printMethodHeader("tropoCorrectionTest", "Tropospheric correction computation", "FDS",
            this.comparisonEpsilon, ComparisonType.ABSOLUTE);

        // pressure [Pa]
        final double pressure = 102000.;

        // temperature [K]
        final double temperature = 20 + 273.16;

        // moisture [percent]
        final double moisture = 20.;

        // geodetic altitude [m]
        final double altitude = 150.;

        // Orbit initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame EME2000Frame = FramesFactory.getEME2000();

        // station frame creation
        final BodyShape earth = new OneAxisEllipsoid(6000000., 0., EME2000Frame);
        final GeodeticPoint point = new GeodeticPoint(30., 10., altitude);
        final TopocentricFrame topoFrame = new TopocentricFrame(earth, point, "Gstation");

        // creation of a propagation vector with coordinates in an inertial frame (EME2000), but building it
        // in the topo frame to know its true elevation
        final Vector3D propagationVectorInTopoFrame = new Vector3D(100., 0.,
                100. * MathLib.tan(8.6393797973719300E-01));
        final Transform toEME2000 = topoFrame.getTransformTo(EME2000Frame, date.shiftedBy(0.001));
        final Vector3D propagationVectorEME2000 = toEME2000.transformVector(propagationVectorInTopoFrame);

        final SignalPropagation signal = new SignalPropagation(propagationVectorEME2000, date,
            date.shiftedBy(0.001), EME2000Frame, FixedDate.RECEPTION);

        // creation of the SignalPropagationModel and the TroposphericCorrection
        final SignalPropagationModel model = new SignalPropagationModel(EME2000Frame, 0.001);
        final AzoulayModel correction = new AzoulayModel(pressure, temperature, moisture, altitude, false);

        // reference PATRIUS (since FDS has wrong coefficient C1) correction and computation
        final double correctionsRef = 3.004627681227842;
        final double correctionsRes = model.getSignalTropoCorrection(correction, signal, topoFrame);

        Assert.assertEquals(correctionsRef, correctionsRes, this.comparisonEpsilon);
        Report.printToReport("Correction", correctionsRef, correctionsRes);
    }

    /**
     * @throws PatriusException if the precession-nutation model data embedded in the library cannot
     *         be read.
     * @description Evaluate Try to initialize a {@link SignalPropagationModel} and compute the
     *              propagation position vector derivatives wrt
     *              the emitter/receiver position with a non pseudo inertial frame.
     * 
     * @testedMethod {@link SignalPropagationModel#SignalPropagationModel(Frame, double)}
     * 
     * @testPassCriteria The {@link IllegalArgumentException} exception is returned as expected.
     */
    @Test
    public void nonPseudoInertialFrameException() throws PatriusException {
        try {
            new SignalPropagationModel(FramesFactory.getITRF(), 1e-12);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }
}
