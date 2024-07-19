/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 * VERSION:4.12.1:FA:FA-125:05/09/2023:[PATRIUS] Reliquat OPENFD-62 sur le code des body shapes
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3194:03/11/2022:[PATRIUS] Fusion des interfaces GeometricBodyShape et BodyShape 
 * VERSION:4.9.1:FA:FA-3193:01/06/2022:[PATRIUS] Revenir a la signature initiale de la methode getLocalRadius
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3181:10/05/2022:[PATRIUS] Passage a protected de la methode setPropagationDelayType
 * VERSION:4.9:DM:DM-3135:10/05/2022:[PATRIUS] Calcul d'intersection sur BodyShape  
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:DM:DM-3133:10/05/2022:[PATRIUS] Ajout de plusieurs fonctionnalites a la classe EclipseDetector 
 * VERSION:4.9:FA:FA-3170:10/05/2022:[PATRIUS] Incoherence de datation entre la methode getLocalRadius...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
 * VERSION:4.5:DM:DM-2245:27/05/2020:Ameliorations de EclipseDetector 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:227:02/10/2014:Merged eclipse detectors and added eclipse detector by lighting ratio
 * VERSION::FA:382:09/12/2014:Eclipse detector corrections
 * VERSION::DM:394:20/02/2015: Added possibility to define the action performed when event occurs
 * VERSION::FA:491:04/11/2015: Added test testSatUnderOcculingBodySurface
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:611:04/08/2016:New implementation using radii provider for visibility of main/inhibition targets
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.events;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.directions.ConstantVectorDirection;
import fr.cnes.sirius.patrius.attitudes.directions.EarthCenterDirection;
import fr.cnes.sirius.patrius.attitudes.directions.ITargetDirection;
import fr.cnes.sirius.patrius.bodies.BodyPoint;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

public class EclipseDetectorTest {

    private double mu;
    private AbsoluteDate iniDate;
    private SpacecraftState initialState;
    private NumericalPropagator propagator;

    private final double sunRadius = 696000000.;
    private final double earthRadius = 6400000.;

    @Test
    public void testEclipse() throws PatriusException {
        this.propagator.addEventDetector(new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius, 0, 60., 1.e-3){

            /** Serializable UID. */
            private static final long serialVersionUID = 4206045819676401256L;

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward) {
                return increasing ? Action.CONTINUE : Action.STOP;
            }
        });
        this.propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.propagator
            .getInitialState().getMu())));
        final SpacecraftState finalState = this.propagator.propagate(this.iniDate.shiftedBy(6000));
        Assert.assertEquals(2303.1835, finalState.getDate().durationFrom(this.iniDate), 1.0e-3);
    }

    // test for coverage
    @Test
    public void testConstructors() throws PatriusException {
        // fake BodyShape:
        final BodyShape body = new BodyShape(){

            /** Serializable UID. */
            private static final long serialVersionUID = -8121394927618726243L;

            @Override
            public Frame getBodyFrame() {
                return null;
            }

            @Override
            public EllipsoidPoint getIntersectionPoint(final Line line, final Vector3D close, final Frame frame,
                                                       final AbsoluteDate date) {
                return null;
            }

            @Override
            public EllipsoidPoint transform(final Vector3D point, final Frame frame, final AbsoluteDate date) {
                return null;
            }

            @Override
            public Vector3D transform(final EllipsoidPoint point) {
                return null;
            }

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
                return new PVCoordinates(Vector3D.PLUS_I, Vector3D.ZERO);
            }

            @Override
            public EllipsoidPoint[] getIntersectionPoints(final Line line, final Frame frame, final AbsoluteDate date) {
                return null;
            }

            @Override
            public double distanceTo(final Line line, final Frame frame, final AbsoluteDate date) {
                return 200.0;
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date, final Frame frame) {
                return null;
            }

            @Override
            public String getName() {
                return "Fake BodyShape";
            }

            @Override
            public double getApparentRadius(final PVCoordinatesProvider posObserver, final AbsoluteDate date,
                                            final PVCoordinatesProvider occultedBody,
                                            final PropagationDelayType propagationDelayType)
                throws PatriusException {
                if (date.durationFrom(EclipseDetectorTest.this.iniDate) == 0.0) {
                    throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
                }
                return 0;
            }

            @Override
            public EllipsoidPoint getIntersectionPoint(final Line line, final Vector3D close, final Frame frame,
                                                       final AbsoluteDate date, final double altitude) {
                return null;
            }

            @Override
            public BodyShape resize(final MarginType marginType, final double marginValue) {
                return null;
            }

            @Override
            public EllipsoidPoint[] closestPointTo(final Line line, final Frame frame, final AbsoluteDate date) {
                return null;
            }

            @Override
            public BodyPoint getIntersectionPoint(final Line line, final Vector3D close, final Frame frame,
                                                  final AbsoluteDate date, final String name) {
                return null;
            }

            @Override
            public BodyPoint[] closestPointTo(final Line line) {
                return null;
            }

            @Override
            public BodyPoint closestPointTo(final Vector3D point, final Frame frame, final AbsoluteDate date) {
                return null;
            }

            @Override
            public BodyPoint closestPointTo(final Vector3D point) {
                return null;
            }

            @Override
            public BodyPoint closestPointTo(final Vector3D point, final String name) {
                return null;
            }

            @Override
            public BodyPoint buildPoint(final LLHCoordinatesSystem coordSystem, final double latitude,
                                        final double longitude, final double height, final String name) {
                return null;
            }

            @Override
            public BodyPoint buildPoint(final Vector3D position, final String name) {
                return null;
            }

            @Override
            public BodyPoint buildPoint(final Vector3D position, final Frame frame, final AbsoluteDate date,
                                        final String name) {
                return null;
            }

            @Override
            public double getDistanceEpsilon() {
                return 0;
            }

            @Override
            public void setDistanceEpsilon(final double epsilon) {
                // nothing to do
            }

            @Override
            public LLHCoordinatesSystem getLLHCoordinatesSystem() {
                return null;
            }

            @Override
            public void setLLHCoordinatesSystem(final LLHCoordinatesSystem coordSystem) {
                // nothing to do
            }

            @Override
            public double getEncompassingSphereRadius() {
                return 0;
            }

            @Override
            public boolean isDefaultLLHCoordinatesSystem() {
                return true;
            }

        };
        final EclipseDetector eclipse1 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1, AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD);
        boolean isTestOk = false;
        try {
            eclipse1.g(this.initialState);
        } catch (final PatriusExceptionWrapper e) {
            isTestOk = true;
        }
        Assert.assertTrue(isTestOk);

        new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1, 100, 1E-6);
        final SpacecraftState state = this.initialState.shiftedBy(2000.0);
        // Cover the EclipseDetector(PVCoordinatesProvider, double, PVCoordinatesProvider, double, double, double,
        // double) constructor:
        EclipseDetector eclipse3 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius, 1e-12, 100, 1E-6);
        Assert.assertTrue(eclipse3.isTotalEclipse());
        eclipse3 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius, 1.0 - 1e-12, 100, 1E-6);
        Assert.assertFalse(eclipse3.isTotalEclipse());
        eclipse3 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius, 0.5, 100, 1E-6);
        Assert.assertFalse(eclipse3.isTotalEclipse());

        // Cover the EclipseDetector(PVCoordinatesProvider, double, BodyShape, double, double, double)
        // constructor:
        EclipseDetector eclipse4 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1e-12, 100, 1E-6);
        Assert.assertTrue(eclipse4.isTotalEclipse());
        eclipse4 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1.0 - 1e-12, 100, 1E-6);
        Assert.assertFalse(eclipse4.isTotalEclipse());
        eclipse4 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 0.5, 100, 1E-6);
        Assert.assertFalse(eclipse4.isTotalEclipse());

        // Cover the EclipseDetector with two actions
        final EclipseDetector eclipse5 =
            new EclipseDetector(null, CelestialBodyFactory.getSun(), this.sunRadius, 100, 1E-6,
                Action.RESET_STATE, Action.CONTINUE);
        final EclipseDetector detector2 = (EclipseDetector) eclipse5.copy();
        Assert.assertTrue(detector2.isTotalEclipse());
        Assert.assertEquals(Action.CONTINUE, detector2.eventOccurred(state, true, true));
        Assert.assertEquals(Action.RESET_STATE, detector2.eventOccurred(state, false, true));
        Assert.assertNull(detector2.getOccultedDirection());

        // Cover the EclipseDetector with two actions
        final EclipseDetector eclipse6 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius, 1e-12, 100, 1E-6, Action.RESET_STATE, Action.CONTINUE);
        Assert.assertTrue(eclipse6.isTotalEclipse());
        Assert.assertEquals(Action.CONTINUE, eclipse6.eventOccurred(state, true, true));
        Assert.assertEquals(Action.RESET_STATE, eclipse6.eventOccurred(state, false, true));

        // Cover the EclipseDetector with two actions
        final EclipseDetector eclipse7 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1e-12, 100, 1E-6, Action.RESET_STATE, Action.CONTINUE);
        Assert.assertTrue(eclipse7.isTotalEclipse());
        Assert.assertEquals(Action.CONTINUE, eclipse7.eventOccurred(state, true, true));
        Assert.assertEquals(Action.RESET_STATE, eclipse7.eventOccurred(state, false, true));

        Assert.assertEquals(Action.RESET_STATE, eclipse7.getActionAtEntry());
        Assert.assertEquals(Action.CONTINUE, eclipse7.getActionAtExit());
        Assert.assertEquals(false, eclipse7.isRemoveAtEntry());
        Assert.assertEquals(false, eclipse7.isRemoveAtExit());

        // Constructors for GENOPUS
        final EclipseDetector eclipse8 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1e-12, 0, EclipseDetector.EXIT, 100, 1E-6, Action.RESET_STATE, false);
        Assert.assertEquals(Action.RESET_STATE, eclipse8.eventOccurred(state, true, true));
        final EclipseDetector eclipse9 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1e-12, 1, EclipseDetector.ENTRY, 100, 1E-6, Action.RESET_DERIVATIVES, false);
        Assert.assertEquals(Action.RESET_DERIVATIVES, eclipse9.eventOccurred(state, false, true));
        final EclipseDetector eclipse10 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1e-12, 0.5, EclipseDetector.ENTRY, 100, 1E-6, Action.RESET_DERIVATIVES, false);
        Assert.assertEquals(Action.RESET_DERIVATIVES, eclipse10.eventOccurred(state, false, true));

        // Evaluate the EclipseDetector(PVCoordinatesProvider, double, BodyShape,
        // double, double, double, int) constructor
        final EclipseDetector eclipse11 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1e-12, 100, 1E-6, 0);
        Assert.assertTrue(eclipse11.isTotalEclipse());
        Assert.assertEquals(Action.STOP, eclipse11.eventOccurred(state, true, true));
        Assert.assertEquals(Action.STOP, eclipse11.eventOccurred(state, false, true));

        Assert.assertEquals(Action.CONTINUE, eclipse11.getActionAtEntry());
        Assert.assertEquals(Action.STOP, eclipse11.getActionAtExit());
        Assert.assertEquals(false, eclipse11.isRemoveAtEntry());
        Assert.assertEquals(false, eclipse11.isRemoveAtExit());

        // Evaluate the EclipseDetector(PVCoordinatesProvider, double, BodyShape, double, double, double,
        // Action, boolean, int) constructor
        final EclipseDetector eclipse12 = new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius,
            body, 1e-12, 100, 1E-6, Action.CONTINUE, false, 2);
        Assert.assertTrue(eclipse12.isTotalEclipse());
        Assert.assertEquals(Action.CONTINUE, eclipse12.eventOccurred(state, true, true));
        Assert.assertEquals(Action.CONTINUE, eclipse12.eventOccurred(state, false, true));

        Assert.assertEquals(Action.CONTINUE, eclipse12.getActionAtEntry());
        Assert.assertEquals(Action.CONTINUE, eclipse12.getActionAtExit());
        Assert.assertEquals(false, eclipse12.isRemoveAtEntry());
        Assert.assertEquals(false, eclipse12.isRemoveAtExit());

        // Evaluate the EclipseDetector(PVCoordinatesProvider, double, BodyShape, double, double, double,
        // Action, boolean, int) constructor with an invalid slope selection
        isTestOk = false;
        try {
            new EclipseDetector(CelestialBodyFactory.getSun(), this.sunRadius, body, 1e-12, 100, 1E-6, Action.CONTINUE,
                false, 3);
        } catch (final IllegalArgumentException e) {
            isTestOk = true;
        }
        Assert.assertTrue(isTestOk);

        // Evaluate the EclipseDetector(PVCoordinatesProvider, double, BodyShape, double, double, double,
        // Action, Action, boolean, boolean, int) constructor
        final EclipseDetector eclipse13 = (EclipseDetector) new EclipseDetector(CelestialBodyFactory.getSun(),
            this.sunRadius, body, 1e-12, 100, 1E-6, Action.RESET_STATE, Action.CONTINUE, false, true, 2).copy();
        Assert.assertTrue(eclipse13.isTotalEclipse());
        Assert.assertEquals(Action.CONTINUE, eclipse13.eventOccurred(state, true, true));
        Assert.assertEquals(Action.RESET_STATE, eclipse13.eventOccurred(state, false, true));

        Assert.assertEquals(Action.RESET_STATE, eclipse13.getActionAtEntry());
        Assert.assertEquals(Action.CONTINUE, eclipse13.getActionAtExit());
        Assert.assertEquals(false, eclipse13.isRemoveAtEntry());
        Assert.assertEquals(true, eclipse13.isRemoveAtExit());

        // Evaluate the EclipseDetector(IDirection, BodyShape, double, double, double, Action, Action, boolean,
        // boolean) constructor with 2 actions, an occulted direction which is not an instance of ITargetDirection and a
        // lightning ratio (1e-12) <= EPSILON (1e-10)
        final ConstantVectorDirection direction = new ConstantVectorDirection(new Vector3D(1, 0, 0),
            FramesFactory.getGCRF());
        final EclipseDetector eclipse14 = new EclipseDetector(direction, body, 1e-12, 100, 1E-6, Action.RESET_STATE,
            Action.CONTINUE, false, true);
        Assert.assertNotNull(eclipse14);
        final EclipseDetector detector3 = (EclipseDetector) eclipse14.copy();
        Assert.assertNotNull(detector3);
        Assert.assertNotNull(detector3.getOccultedDirection());
        Assert.assertFalse(detector3.getOccultedDirection() instanceof ITargetDirection);
        Assert.assertTrue(detector3.isTotalEclipse());
        Assert.assertEquals(Action.CONTINUE, detector3.eventOccurred(state, true, true));
        Assert.assertEquals(Action.RESET_STATE, detector3.eventOccurred(state, false, true));

        // Evaluate the EclipseDetector(IDirection, BodyShape, double, double, double, Action, Action, boolean,
        // boolean) constructor with 2 actions, an occulted direction which is not an instance of ITargetDirection and a
        // lightning ratio (1 - 1e-12) >= 1 - EPSILON (1 - 1e-10)
        final ConstantVectorDirection direction2 = new ConstantVectorDirection(new Vector3D(1, 0, 0),
            FramesFactory.getGCRF());
        final EclipseDetector eclipse15 = new EclipseDetector(direction2, body, 1 - 1e-12, 100, 1E-6,
            Action.RESET_STATE, Action.CONTINUE, false, true);
        Assert.assertNotNull(eclipse15);
        final EclipseDetector detector4 = (EclipseDetector) eclipse15.copy();
        Assert.assertNotNull(detector4);
        Assert.assertNotNull(detector4.getOccultedDirection());
        Assert.assertFalse(detector4.getOccultedDirection() instanceof ITargetDirection);
        Assert.assertFalse(detector4.isTotalEclipse());
        Assert.assertEquals(Action.CONTINUE, detector4.eventOccurred(state, true, true));
        Assert.assertEquals(Action.RESET_STATE, detector4.eventOccurred(state, false, true));

        // Evaluate the EclipseDetector(IDirection, BodyShape, double, double, double, Action, Action, boolean,
        // boolean) constructor with 2 actions, an occulted direction which is not an instance of ITargetDirection and a
        // lightning ratio such that lightning ratio (0.5) > EPSILON (1e-10) and lightning ratio (0.5) < 1 - EPSILON (1
        // - 1e-10)
        final ConstantVectorDirection direction3 = new ConstantVectorDirection(new Vector3D(1, 0, 0),
            FramesFactory.getGCRF());
        final EclipseDetector eclipse16 = new EclipseDetector(direction3, body, 0.5, 100, 1E-6, Action.RESET_STATE,
            Action.CONTINUE, false, true);
        Assert.assertNotNull(eclipse16);
        final EclipseDetector detector5 = (EclipseDetector) eclipse16.copy();
        Assert.assertNotNull(detector5);
        Assert.assertNotNull(detector5.getOccultedDirection());
        Assert.assertFalse(detector5.getOccultedDirection() instanceof ITargetDirection);
        Assert.assertFalse(detector5.isTotalEclipse());
        Assert.assertEquals(Action.CONTINUE, detector5.eventOccurred(state, true, true));
        Assert.assertEquals(Action.RESET_STATE, detector5.eventOccurred(state, false, true));

        // Evaluate the EclipseDetector(IDirection, BodyShape, double, double, double, Action, Action, boolean,
        // boolean) constructor with 2 actions, an occulted direction which is an instance of ITargetDirection and a
        // light speed propagation delay type for the detector
        final EarthCenterDirection direction4 = new EarthCenterDirection();
        final EclipseDetector eclipse17 = new EclipseDetector(direction4, body, 1e-12, 100, 1E-6, Action.RESET_STATE,
            Action.CONTINUE, false, true);
        Assert.assertNotNull(eclipse17);
        final EclipseDetector detector6 = (EclipseDetector) eclipse17.copy();
        Assert.assertNotNull(detector6);
        detector6.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getGCRF());
        Assert.assertNotNull(detector6.getOccultedDirection());
        Assert.assertTrue(detector6.getOccultedDirection() instanceof ITargetDirection);
        Assert.assertTrue(detector6.isTotalEclipse());
        Assert.assertEquals(Action.CONTINUE, detector6.eventOccurred(state, true, true));
        Assert.assertEquals(Action.RESET_STATE, detector6.eventOccurred(state, false, true));

        // Try to set a propagation delay type with a frame which is not pseudo-inertial
        isTestOk = false;
        try {
            detector6.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, new Frame(FramesFactory.getGCRF(),
                Transform.IDENTITY, "frame", false));
        } catch (final IllegalArgumentException e) {
            isTestOk = true;
        }
        Assert.assertTrue(isTestOk);
    }

    @Test
    public void testPenumbra() throws PatriusException {
        this.propagator.addEventDetector(new EclipseDetector(
            CelestialBodyFactory.getSun(), this.sunRadius,
            CelestialBodyFactory.getEarth(), this.earthRadius, 1, AbstractDetector.DEFAULT_MAXCHECK,
            AbstractDetector.DEFAULT_THRESHOLD));
        this.propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(this.propagator
            .getInitialState().getMu())));
        final SpacecraftState finalState = this.propagator.propagate(this.iniDate.shiftedBy(6000));
        Assert.assertEquals(4388.1558707427685, finalState.getDate().durationFrom(this.iniDate), 1.0e-6);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * @description test the EclipseDetector when the satellite is under the occulting body surface (possible margin is
     *              taken into account)
     *              with occulted occulting satellite angle OBTUSE
     *              This test was created following the FT-491
     *              The occulted body is a fictitious Sun and the occulting body is a fictitious Moon, whose coordinates
     *              are:
     *              Moon coordinates : [0, -(x/2 + delta), 0], radius Moon = 1/2x
     *              Sun coordinates : [0, 3x, 0], radius Sun = 1/2x
     *              x is the semi-major axis of the equatorial and circular orbit.
     * @input an equatorial orbit, a fictitious Sun and Moon
     * @output the final spacecraft state
     * @testPassCriteria exit the eclipse at 90° occulted - occulting - satellite angle. Spacecraft initially in the
     *                   shadow (and below the occulting body surface).
     * @referenceVersion 3.1
     * @nonregressionVersion 3.1
     */
    @Test
    public void testSatUnderOccultingBodySurfaceAngleObtuse() throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate("2013-03-20T11:00:00.000", TimeScalesFactory.getTAI());
        final double x = 7780e3;
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(0.0, -x, 0.0),
            new Vector3D(7157.792507, 0.0, 0.0)), FramesFactory.getGCRF(), date, this.mu);
        final double keplerianPeriod = orbit.getKeplerianPeriod();

        // delta (can represent the margin in the occulting body radius)
        final double delta = x / 100;

        // Fictitious Sun:
        final PVCoordinatesProvider testSun = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -8005683123153493999L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
                return new PVCoordinates(new Vector3D(0, 3 * x, 0), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date, final Frame frame) {
                return null;
            }
        };

        // Fictitious Moon:
        final PVCoordinatesProvider testMoon = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -1023677361475895353L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
                return new PVCoordinates(new Vector3D(0, -(x / 2 + delta), 0.0), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date, final Frame frame) {
                return null;
            }
        };

        final double radiusSun = 0.5 * x;
        final double radiusMoon = 0.5 * x;

        final SpacecraftState initState = new SpacecraftState(orbit);

        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initState);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initState.getMu())));

        // Eclipse detector, propagation will stop when exiting the eclipse
        final EclipseDetector eclipseDetector =
            new EclipseDetector(testSun, radiusSun, testMoon, radiusMoon, 0.0, 1000, 1E-6);

        // Check that the satellite is under the occulting body surface
        final double dSatOccculting = initState.getPVCoordinates().getPosition()
            .distance(testMoon.getPVCoordinates(this.iniDate, FramesFactory.getGCRF()).getPosition());
        Assert.assertTrue(dSatOccculting < radiusMoon);

        // Propagation with eclipse detector
        propagator.addEventDetector(eclipseDetector);
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(keplerianPeriod));

        // As the angle occulted occulting satellite angle is OBTU and the radius of the fictitious Sun and the
        // fictitious moon are equal, final position of spacecraft should be x/2 (make a drawing):
        Assert.assertEquals(finalState.getPVCoordinates().getPosition().getX(), 0.5 * x, 1.2e-4);

        // Check that g function is negative around initial date (below the occulting body surface but in the shadow)
        Assert.assertTrue(eclipseDetector.g(initState) < 0);
        Assert.assertTrue(eclipseDetector.g(initState.shiftedBy(100)) < 0);
        Assert.assertTrue(eclipseDetector.g(initState.shiftedBy(-100)) < 0);

        // Check that the isInEclipse method returns true (because there is eclipse, since the g function is
        // negative)
        Assert.assertTrue(eclipseDetector.isInEclipse(initState));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * @description test the EclipseDetector when the satellite is under the occulting body surface (possible margin is
     *              taken into account)
     *              with occulted occulting satellite angle ACUTE
     *              This test was created following the FT-491
     *              The occulted body is a fictitious Sun and the occulting body is a fictitious Moon, whose coordinates
     *              are:
     *              Moon coordinates : [0, -(x/2 + delta), 0], radius Moon = 1/2x
     *              Sun coordinates : [2x, -3x, 0], radius Sun = 1/2x
     *              x is the semi-major axis of the equatorial and circular orbit.
     * @input an equatorial orbit, a fictitious Sun and Moon
     * @output the final spacecraft state
     * @testPassCriteria exit the eclipse at -90° occulted - occulting - satellite angle. Spacecraft initially not in
     *                   the shadow (although below the occulting body surface).
     * @referenceVersion 3.1
     * @nonregressionVersion 3.1
     */
    @Test
    public void testSatUnderOccultingBodySurfaceAngleAcute() throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate("2013-03-20T11:00:00.000", TimeScalesFactory.getTAI());
        final double x = 7780e3;
        final CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(new Vector3D(0.0, -x, 0.0),
            new Vector3D(7157.792507, 0.0, 0.0)), FramesFactory.getGCRF(), date, this.mu);
        final double keplerianPeriod = orbit.getKeplerianPeriod();

        // delta (can represent the margin in the occulting body radius)
        final double delta = x / 100;

        // Fictitious Sun:
        final PVCoordinatesProvider testSun = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -5735871993666365113L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
                return new PVCoordinates(new Vector3D(0, -3 * x, 0), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date, final Frame frame) {
                return null;
            }
        };

        // Fictitious Moon:
        final PVCoordinatesProvider testMoon = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 7531786566461947925L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
                return new PVCoordinates(new Vector3D(0, -(x / 2 + delta), 0.0), Vector3D.ZERO);
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date, final Frame frame) {
                return null;
            }
        };

        final double radiusSun = 0.5 * x;
        final double radiusMoon = 0.5 * x;

        final SpacecraftState initState = new SpacecraftState(orbit);

        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initState);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(initState.getMu())));

        // Eclipse detector, propagation will stop when exiting the eclipse
        final EclipseDetector eclipseDetector =
            new EclipseDetector(testSun, radiusSun, testMoon, radiusMoon, 0.0, 1000, 1E-6);

        // Check that the satellite is under the occulting body surface
        final double dSatOccculting = initState.getPVCoordinates().getPosition()
            .distance(testMoon.getPVCoordinates(this.iniDate, FramesFactory.getGCRF()).getPosition());
        Assert.assertTrue(dSatOccculting < radiusMoon);

        // Propagation with eclipse detector
        propagator.addEventDetector(eclipseDetector);
        final SpacecraftState finalState = propagator.propagate(date.shiftedBy(keplerianPeriod));

        // As the angle occulted occulting satellite angle is AIGU and the radius of the fictitious Sun and the
        // fictitious moon are equal, final position of spacecraft should be - x/2 (make a drawing)
        Assert.assertEquals(finalState.getPVCoordinates().getPosition().getX(), -0.5 * x, 1e-4);

        // Check that g function is positive around initial date (below the occulting body surface but enlightened)
        Assert.assertTrue(eclipseDetector.g(initState) > 0);
        Assert.assertTrue(eclipseDetector.g(initState.shiftedBy(100)) > 0);
        Assert.assertTrue(eclipseDetector.g(initState.shiftedBy(-100)) > 0);

        // Check that the isInEclipse method returns false (because there is no eclipse, since the g function is
        // positive)
        Assert.assertFalse(eclipseDetector.isInEclipse(initState));
    }

    /**
     * Test needed to validate the setMaxCheckInterval(double) method of the {@link AbstractDetector} class.
     * 
     * @throws PatriusException if the celestial body cannot be built
     * @testType UT
     * @description test that the max check interval set by means of the setMaxCheckInterval(double) method of the
     *              {@link AbstractDetector} class is correct.
     * @input the desired max check interval
     * @output the set max check interval
     * @testPassCriteria the set max check interval shall be equal to the desired max check interval.
     * @referenceVersion 4.9
     * @nonregressionVersion 4.9
     */
    @Test
    public void testSetMaxCheckInterval() throws PatriusException {
        final double maxCheck = 100.;
        final EclipseDetector eclipse = new EclipseDetector(null, CelestialBodyFactory.getSun(), this.sunRadius,
            maxCheck, 1E-6, Action.RESET_STATE, Action.CONTINUE);
        Assert.assertEquals(maxCheck, eclipse.getMaxCheckInterval(), 0.);
        eclipse.setMaxCheckInterval(maxCheck * 2);
        Assert.assertEquals(maxCheck * 2, eclipse.getMaxCheckInterval(), 0.);
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        this.mu = 3.9860047e14;
        final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        this.iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
            FramesFactory.getEME2000(), this.iniDate, this.mu);
        this.initialState = new SpacecraftState(orbit);
        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        this.propagator = new NumericalPropagator(integrator);
        this.propagator.setInitialState(this.initialState);
    }

    @After
    public void tearDown() {
        this.iniDate = null;
        this.initialState = null;
        this.propagator = null;
    }
}
