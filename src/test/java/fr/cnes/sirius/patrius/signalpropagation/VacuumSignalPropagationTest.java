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
* VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
* VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
* VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
* VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
* VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait 
 *          retourner un CelestialBodyFrame 
* VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
* VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite 
 *          de convertir les sorties de VacuumSignalPropagation 
* VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
* VERSION:4.11.1:FA:FA-72:30/06/2023:[PATRIUS] Mauvaise prise en compte du MeteoConditionProvider dans les AbstractTropoFactory
 * VERSION:4.11:DM:DM-14:22/05/2023:[PATRIUS] Nombre max d'iterations dans le calcul de la propagation du signal 
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Ajout de conditions meteorologiques variables dans les modeles de troposphere
 * VERSION:4.11:DM:DM-3318:22/05/2023:[PATRIUS] Besoin de forcer la normalisation dans la classe QuaternionPolynomialSegment
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3228:03/11/2022:[PATRIUS] Integration des evolutions de la branche patrius-for-lotus 
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
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.LinearTwoPointsPVProvider;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.exception.ConvergenceException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagation.SignalPropagationRole;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel.ConvergenceAlgorithm;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel.FixedDate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description
 *              <p>
 *              Test class for the signal propagation computation in space
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class VacuumSignalPropagationTest {

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

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(VacuumSignalPropagationTest.class.getSimpleName(), "Signal propagation");
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#SIGNAL_PROPAGATION}
     * 
     * @testedMethod {@link VacuumSignalPropagationModel#computeSignalPropagation}
     * @testedMethod {@link VacuumSignalPropagation#getVector()}
     * @testedMethod {@link VacuumSignalPropagation#getEmissionDate()}
     * @testedMethod {@link VacuumSignalPropagation#getReceptionDate()}
     * @testedMethod {@link VacuumSignalPropagation#getEmitterPV()}
     * @testedMethod {@link VacuumSignalPropagation#getReceiverPV()}
     * @testedMethod {@link VacuumSignalPropagation#getFrame()}
     * 
     * @description computation of a signal propagation between two moving points in the void
     * 
     * @input two PVCoordinatesProviders, a date
     * 
     * @output The associated {@link VacuumSignalPropagation} object
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
        final VacuumSignalPropagationModel model = new VacuumSignalPropagationModel(refFrame, threshold,
            VacuumSignalPropagationModel.DEFAULT_MAX_ITER);

        // Station - spacecraft signal computation
        final VacuumSignalPropagation goSignal = model.computeSignalPropagation(station, spacecraft,
            startDate, FixedDate.EMISSION);

        // Values tests
        // The position comparison are made with a relative "threshold" value, so 1.0e-6
        final Vector3D vector = goSignal.getVector();
        final double expectedDuration = 1.e7 / (Constants.SPEED_OF_LIGHT + 1.e7 / 10.);
        Assert.assertEquals(expectedDuration * Constants.SPEED_OF_LIGHT, vector.getX(), 1.e-6);
        Assert.assertEquals(0., vector.getY(), 1.e-6);
        Assert.assertEquals(0., vector.getZ(), 1.e-6);

        Assert.assertEquals(0., goSignal.getEmissionDate().durationFrom(startDate), threshold);
        Assert.assertEquals(station.getPVCoordinates(startDate, refFrame), goSignal.getEmitterPV());
        Assert.assertEquals(expectedDuration, goSignal.getReceptionDate().durationFrom(startDate), threshold);
        Assert.assertEquals(spacecraft.getPVCoordinates(goSignal.getReceptionDate(), refFrame),
            goSignal.getReceiverPV());
        Assert.assertEquals(goSignal.getFixedDateType(), FixedDate.EMISSION);
        Assert.assertTrue(goSignal.getFrame().equals(refFrame));

        Report.printToReport("Signal duration", expectedDuration, goSignal.getReceptionDate().durationFrom(startDate));

        // Same computation from the reception date
        final AbsoluteDate recDate = goSignal.getReceptionDate();
        final VacuumSignalPropagation goSignal2 = model.computeSignalPropagation(
            station, spacecraft, recDate, FixedDate.RECEPTION);

        // Values tests
        // Deprecated method
        final Vector3D vector2 = goSignal2.getVector();
        Assert.assertEquals(vector.getX(), vector2.getX(), 1.e-6);
        Assert.assertEquals(vector.getY(), vector2.getY(), 1.e-6);
        Assert.assertEquals(vector.getZ(), vector2.getZ(), 1.e-6);

        Assert.assertEquals(0., goSignal2.getEmissionDate().durationFrom(startDate), threshold);
        Assert.assertEquals(station.getPVCoordinates(goSignal2.getEmissionDate(), refFrame), goSignal2.getEmitterPV());
        Assert.assertEquals(0., goSignal2.getReceptionDate().durationFrom(recDate), threshold);
        Assert.assertEquals(spacecraft.getPVCoordinates(recDate, refFrame), goSignal2.getReceiverPV());
        Assert.assertEquals(goSignal2.getFixedDateType(), FixedDate.RECEPTION);

        // Wrong model creation
        final Frame wrongFrame = new LocalOrbitalFrame(refFrame, LOFType.TNW, spacecraft, "wrong frame");
        try {
            new VacuumSignalPropagationModel(wrongFrame, threshold, VacuumSignalPropagationModel.DEFAULT_MAX_ITER);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * @throws PatriusException if frames problems occur
     * @testType UT
     * 
     * @testedFeature {@link features#SIGNAL_PROPAGATION}
     * 
     * @testedMethod {@link VacuumSignalPropagationModel#SignalPropagationModel(Frame, double, ConvergenceAlgorithm)}
     * @testedMethod {@link VacuumSignalPropagationModel#computeSignalPropagation(PVCoordinatesProvider, PVCoordinatesProvider, AbsoluteDate, FixedDate)}
     * @testedMethod {@link VacuumSignalPropagation#getSignalPropagationDuration()}
     * @testedMethod {@link VacuumSignalPropagation#getVector()}
     * 
     * @description Compare the computation of a signal propagation between two moving points in the void using both
     *              convergence algorithms
     *              (they should produce the same results)
     * 
     * @input two PVCoordinatesProviders, several dates
     * 
     * @output The associated {@link VacuumSignalPropagation} object
     * 
     * @testPassCriteria the output signal propagations computed by both convergence algorithms should contains the same
     *                   information (below
     *                   the threshold used to compute the signal)
     */
    @Test
    public void convergenceAlgoComparisonTest() throws PatriusException {

        // Thresholds
        final double thresholdDuration = 1e-12;
        final double thresholdDistance = thresholdDuration * 8e3; // 8e3 M/s is the upper bound of the spacecraft
                                                                  // velocity

        // Frame and dates
        final Frame refFrame = FramesFactory.getGCRF();
        final AbsoluteDate startDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate endDate = startDate.shiftedBy(1000.);
        final int iteration = 1000;
        final double step = endDate.durationFrom(startDate) / iteration;

        // "station"
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.GRS80_EARTH_EQUATORIAL_RADIUS,
            Constants.GRS80_EARTH_FLATTENING, FramesFactory.getITRF(), "Earth");
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
            MathLib.toRadians(67.8805741), MathLib.toRadians(21.0310484), 521.18, "");
        final PVCoordinatesProvider station = new TopocentricFrame(point, "Gstation");

        // "spacecraft"
        final Vector3D pos = new Vector3D(7179992.82, 2276.519, -14178.396);
        final Vector3D vel = new Vector3D(7.450848, -1181.198684, 7356.62864);
        final PVCoordinatesProvider spacecraft =
            new CartesianOrbit(new PVCoordinates(pos, vel), refFrame, startDate, Constants.EGM96_EARTH_MU);

        // Models
        final VacuumSignalPropagationModel modelFixePoint =
            new VacuumSignalPropagationModel(refFrame, ConvergenceAlgorithm.FIXE_POINT);
        final VacuumSignalPropagationModel modelNetwon = new VacuumSignalPropagationModel(refFrame);

        // Compute N signals (for robustness)
        for (int i = 0; i < iteration; i++) {
            final AbsoluteDate emDate = startDate.shiftedBy(i * step);

            // Station - spacecraft signal computation from the emission date
            final VacuumSignalPropagation signalEmFixePoint =
                modelFixePoint.computeSignalPropagation(station, spacecraft, emDate, FixedDate.EMISSION);
            final VacuumSignalPropagation signalEmNewton = modelNetwon.computeSignalPropagation(station, spacecraft,
                emDate, FixedDate.EMISSION);

            // Values tests
            Assert.assertEquals(signalEmNewton.getSignalPropagationDuration(),
                signalEmFixePoint.getSignalPropagationDuration(),
                thresholdDuration);

            Assert.assertEquals(signalEmNewton.getVector().getX(), signalEmFixePoint.getVector().getX(),
                thresholdDistance);
            Assert.assertEquals(signalEmNewton.getVector().getY(), signalEmFixePoint.getVector().getY(),
                thresholdDistance);
            Assert.assertEquals(signalEmNewton.getVector().getZ(), signalEmFixePoint.getVector().getZ(),
                thresholdDistance);

            // Same computation from the reception date
            final AbsoluteDate recDate = signalEmFixePoint.getReceptionDate(); // One date as reference
            final VacuumSignalPropagation signalRecFixePoint =
                modelFixePoint.computeSignalPropagation(station, spacecraft, recDate, FixedDate.RECEPTION);
            final VacuumSignalPropagation signalRecNewton =
                modelNetwon.computeSignalPropagation(station, spacecraft, recDate, FixedDate.RECEPTION);

            // Values tests
            Assert.assertEquals(signalRecNewton.getSignalPropagationDuration(),
                signalRecFixePoint.getSignalPropagationDuration(),
                thresholdDuration);

            Assert.assertEquals(signalRecNewton.getVector().getX(), signalRecFixePoint.getVector().getX(),
                thresholdDistance);
            Assert.assertEquals(signalRecNewton.getVector().getY(), signalRecFixePoint.getVector().getY(),
                thresholdDistance);
            Assert.assertEquals(signalRecNewton.getVector().getZ(), signalRecFixePoint.getVector().getZ(),
                thresholdDistance);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#SIGNAL_PROPAGATION}
     * 
     * @testedMethod {@link VacuumSignalPropagation#getShapiroTimeCorrection(CelestialPoint)}
     * @testedMethod {@link VacuumSignalPropagation#getShapiroTimeCorrection(double)}
     * 
     * @description evaluate the computation of the Shapiro time dilation due to the gravitational attraction of the
     *              body present at the center of the signal propagation frame.
     * 
     * @input a computed signal propagation
     * 
     * @output The resulting Shapiro time dilation
     * 
     * @testPassCriteria the Shapiro time dilations are computed as expected (non regression on the values)
     * @throws PatriusException
     *         if an error occurs
     * 
     * @referenceVersion 4.10
     * 
     * @nonRegressionVersion 4.10
     */
    @Test
    public void shapiroTimeCorrectionTest() throws PatriusException {

        final double threshold = 1e-16; // For non regression results evaluation

        // Frame and dates
        final Frame refFrame = FramesFactory.getGCRF();
        final AbsoluteDate startDate = AbsoluteDate.J2000_EPOCH;

        // "station"
        final CelestialPoint earth = CelestialBodyFactory.getEarth();
        final double mu = earth.getGM();
        // final BodyShape earthShape = earth.getShape();
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(Constants.GRS80_EARTH_EQUATORIAL_RADIUS,
            Constants.GRS80_EARTH_FLATTENING, FramesFactory.getITRF(), "Earth");
        final EllipsoidPoint point = new EllipsoidPoint(earthShape, earthShape.getLLHCoordinatesSystem(),
            MathLib.toRadians(67.8805741), MathLib.toRadians(21.0310484), 521.18, "");
        final PVCoordinatesProvider station = new TopocentricFrame(point, "station");

        // "spacecraft"
        final Vector3D pos = new Vector3D(7179992.82, 2276.519, -14178.396);
        final Vector3D vel = new Vector3D(7.450848, -1181.198684, 7356.62864);
        final PVCoordinatesProvider spacecraft =
            new CartesianOrbit(new PVCoordinates(pos, vel), refFrame, startDate, Constants.EGM96_EARTH_MU);

        // Model and station - spacecraft signal computation
        final VacuumSignalPropagationModel model = new VacuumSignalPropagationModel(refFrame, 1e-12,
            VacuumSignalPropagationModel.DEFAULT_MAX_ITER);
        final VacuumSignalPropagation goSignal = model.computeSignalPropagation(station, spacecraft,
            startDate, FixedDate.EMISSION);

        // Expected Shapiro time correction value (reference based on non regression)
        final double expectedShapiroTime = 4.45043593223174E-11;

        // Results evaluation (the two methods should produce the same result)
        Assert.assertEquals(expectedShapiroTime, goSignal.getShapiroTimeCorrection(earth), threshold);
        Assert.assertEquals(expectedShapiroTime, goSignal.getShapiroTimeCorrection(mu), threshold);
    }

    /**
     * @throws PatriusException
     *         if frames problems occur
     * @testType UT
     * 
     * @testedFeature {@link features#SIGNAL_PROPAGATION}
     * 
     * @testedMethod {@link SignalPropagationRole#TRANSMITTER}
     * @testedMethod {@link SignalPropagationRole#RECEIVER}
     * 
     * @description Evaluate the enumerate behavior
     * 
     * @testPassCriteria the enumerate return the expected information
     */
    @Test
    public void signalPropagationRoleTest() throws PatriusException {

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
        final VacuumSignalPropagationModel model = new VacuumSignalPropagationModel(refFrame, 1e-13,
            VacuumSignalPropagationModel.DEFAULT_MAX_ITER);

        // Station - spacecraft signal computation
        final VacuumSignalPropagation goSignal = model.computeSignalPropagation(station, spacecraft,
            startDate, FixedDate.EMISSION);

        final SignalPropagationRole transmitterRole = SignalPropagationRole.TRANSMITTER;
        final AbsoluteDate transDate = transmitterRole.getDate(goSignal);
        final Vector3D transDTPropDPos = transmitterRole.getdTPropDPos(goSignal);
        final RealMatrix transDPropDPos = transmitterRole.getdPropDPos(goSignal);

        Assert.assertEquals(goSignal.getEmissionDate(), transDate);
        Assert.assertEquals(goSignal.getdTpropdPem(), transDTPropDPos);
        Assert.assertEquals(goSignal.getdPropdPem(), transDPropDPos);

        final SignalPropagationRole receiverRole = SignalPropagationRole.RECEIVER;
        final AbsoluteDate receiverDate = receiverRole.getDate(goSignal);
        final Vector3D receiverDTPropDPos = receiverRole.getdTPropDPos(goSignal);
        final RealMatrix receiverDPropDPos = receiverRole.getdPropDPos(goSignal);

        Assert.assertEquals(goSignal.getReceptionDate(), receiverDate);
        Assert.assertEquals(goSignal.getdTpropdPrec(), receiverDTPropDPos);
        Assert.assertEquals(goSignal.getdPropdPrec(), receiverDPropDPos);
    }

    /**
     * @throws PatriusException
     *         if an error occurs
     * @description Evaluate the signal propagation serialization / deserialization process.
     *
     * @testPassCriteria The signal propagation can be serialized and deserialized.
     */
    @Test
    public void testSerialization() {

        // Orbit initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final CelestialBodyFrame eme2000 = FramesFactory.getEME2000();

        // Station frame creation
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6000000., 0., eme2000);
        final EllipsoidPoint point = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(), 30., 10., 150., "");
        final TopocentricFrame topoFrame = new TopocentricFrame(point, "Gstation");

        // Creation of the satellite PV built knowing its true elevation in the topo frame
        final PVCoordinates pvSat = new PVCoordinates(
            new Vector3D(100., 0., 100. * MathLib.tan(8.6393797973719300E-01)), Vector3D.ZERO);

        final VacuumSignalPropagation signal = new VacuumSignalPropagation(pvSat, PVCoordinates.ZERO, date,
            date.shiftedBy(0.001), topoFrame, FixedDate.RECEPTION);

        final VacuumSignalPropagation deserializedSignal = TestUtils.serializeAndRecover(signal);

        Assert.assertEquals(signal.getEmissionDate(), deserializedSignal.getEmissionDate());
        Assert.assertEquals(signal.getReceptionDate(), deserializedSignal.getReceptionDate());
        Assert.assertEquals(signal.getVector(), deserializedSignal.getVector());
        Assert.assertEquals(signal.getdPropdT(), deserializedSignal.getdPropdT());
        Assert.assertEquals(signal.getdTpropdPrec(), deserializedSignal.getdTpropdPrec());
        Assert.assertEquals(signal.getdTpropdPem(), deserializedSignal.getdTpropdPem());
    }

    /**
     * @throws PatriusException if the precession-nutation model data embedded in the library cannot be read
     * @description Try to initialize a {@link VacuumSignalPropagationModel} with a non pseudo inertial frame.
     * 
     * @testedMethod {@link VacuumSignalPropagationModel#SignalPropagationModel(Frame, double)}
     * 
     * @testPassCriteria The {@link IllegalArgumentException} exception is returned as expected.
     */
    @Test
    public void nonPseudoInertialFrameException() throws PatriusException {
        try {
            new VacuumSignalPropagationModel(FramesFactory.getITRF(), 1e-12,
                VacuumSignalPropagationModel.DEFAULT_MAX_ITER);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException if the precession-nutation model data embedded in the library cannot be read
     * @description Try to initialize a {@link VacuumSignalPropagation} the emitter and receiver PV defined at the same
     *              position.
     * 
     * @testedMethod {@link VacuumSignalPropagation#SignalPropagation(PVCoordinates, PVCoordinates, AbsoluteDate, AbsoluteDate, Frame, FixedDate)}
     * 
     * @testPassCriteria The {@link IllegalArgumentException} exception is returned as expected.
     */
    @Test
    public void samePosSignalPropagationException() {
        try {
            new VacuumSignalPropagation(PVCoordinates.ZERO, PVCoordinates.ZERO, AbsoluteDate.J2000_EPOCH,
                AbsoluteDate.J2000_EPOCH.shiftedBy(1.),
                FramesFactory.getGCRF(), FixedDate.EMISSION);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws PatriusException PatriusException
     * @description Check that a too small maxIterSignalPropagation in the getSignalEmissionDate method
     * 
     * @testedMethod {@link VacuumSignalPropagationModel#getSignalEmissionDate(PVCoordinatesProvider, PVCoordinatesProvider, AbsoluteDate, double, PropagationDelayType, Frame, double)}
     * 
     */
    @Test(expected = ConvergenceException.class)
    public void tooFewIterationsGetSignalEmissionDatePropagationException() throws PatriusException {
        // Initialization
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);

        // get the signal emission date with a too small maxIterSignalPropagation (1)
        VacuumSignalPropagationModel.getSignalEmissionDate(CelestialBodyFactory.getSun(), initialOrbit,
            AbsoluteDate.J2000_EPOCH.shiftedBy(84600.), 1E-12, PropagationDelayType.LIGHT_SPEED,
            FramesFactory.getICRF(), 1);
    }

    /**
     * @throws PatriusException PatriusException
     * @description Check that a too small maxIterSignalPropagation in the getSignalReceptionDate method
     * 
     * @testedMethod {@link VacuumSignalPropagationModel#getSignalReceptionDate(PVCoordinatesProvider, PVCoordinatesProvider, AbsoluteDate, double, PropagationDelayType, Frame, double)}
     * 
     */
    @Test(expected = ConvergenceException.class)
    public void tooFewIterationsGetSignalReceptionDatePropagationException() throws PatriusException {
        // Initialization
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);

        // get the signal emission date with a too small maxIterSignalPropagation (1)
        VacuumSignalPropagationModel.getSignalReceptionDate(CelestialBodyFactory.getSun(), initialOrbit,
            AbsoluteDate.J2000_EPOCH.shiftedBy(84600.), 1E-12, PropagationDelayType.LIGHT_SPEED,
            FramesFactory.getICRF(), 1);
    }
}
