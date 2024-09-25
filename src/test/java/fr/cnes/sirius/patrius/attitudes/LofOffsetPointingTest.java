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
 */
/*
 *
 * HISTORY
* VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait 
 *          retourner un CelestialBodyFrame 
* VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:FA:FA-1978:15/05/2019:Anomalie calcul orientation corps celeste (UAI)
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:271:05/09/2014:Anomalies definitions LVLH et VVLH
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class LofOffsetPointingTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    // Reference frame = ITRF 2005C
    private CelestialBodyFrame frameItrf2005;

    // Earth shape
    OneAxisEllipsoid earthSpheric;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(LofOffsetPointingTest.class.getSimpleName(), "Lof offset pointing attitude provider");
    }

    /**
     * Test if both constructors are equivalent
     */
    @Test
    public void testLof() throws PatriusException {

        Report.printMethodHeader("testLof", "Rotation computation", "Orekit v7 reference", Utils.epsilonAngle,
            ComparisonType.ABSOLUTE);

        // Satellite position
        final CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(0.), MathLib.toRadians(270.),
                MathLib.toRadians(5.300), PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);

        // Create lof aligned law
        // ************************
        final LofOffset lofLaw = new LofOffset(circ.getFrame(), LOFType.LVLH);
        final LofOffsetPointing lofPointing =
            new LofOffsetPointing(this.earthSpheric, lofLaw, Vector3D.PLUS_K, Vector3D.PLUS_J, Vector3D.PLUS_K);
        final Rotation lofRot = lofPointing.getAttitude(circ, this.date, circ.getFrame()).getRotation();

        // Compare to body center pointing law
        // *************************************
        final BodyCenterPointing centerLaw = new BodyCenterPointing(this.earthSpheric.getBodyFrame());
        final Rotation centerRot = centerLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();
        final double angleBodyCenter = centerRot.applyInverseTo(lofRot).getAngle();
        Assert.assertEquals(0., angleBodyCenter, Utils.epsilonAngle);

        // Compare to nadir pointing law
        // *******************************
        final NadirPointing nadirLaw = new NadirPointing(this.earthSpheric);
        final Rotation nadirRot = nadirLaw.getAttitude(circ, this.date, circ.getFrame()).getRotation();
        final double angleNadir = nadirRot.applyInverseTo(lofRot).getAngle();
        Assert.assertEquals(0., angleNadir, Utils.epsilonAngle);

        Report.printToReport("Angle at date", 0, angleNadir);
    }

    @Test(expected = PatriusException.class)
    public void testMiss() throws PatriusException {
        final CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(0.), MathLib.toRadians(270.),
                MathLib.toRadians(5.300), PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);
        final LofOffset upsideDown = new LofOffset(circ.getFrame(), LOFType.LVLH, RotationOrder.XYX, FastMath.PI, 0, 0);
        final LofOffsetPointing pointing = new LofOffsetPointing(this.earthSpheric, upsideDown, Vector3D.PLUS_K);
        pointing.getTargetPosition(circ, this.date, circ.getFrame());
    }

    @Test(expected = PatriusException.class)
    public void testMiss2() throws PatriusException {
        final CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(0.), MathLib.toRadians(270.),
                MathLib.toRadians(5.300), PositionAngle.MEAN,
                FramesFactory.getEME2000(), this.date, this.mu);
        final LofOffset upsideDown = new LofOffset(circ.getFrame(), LOFType.LVLH, RotationOrder.XYX, FastMath.PI, 0, 0);
        final LofOffsetPointing pointing = new LofOffsetPointing(this.earthSpheric, upsideDown, Vector3D.PLUS_K);
        pointing.getTargetPV(circ, this.date, circ.getFrame());
    }

    @Test
    public void testSpin() throws PatriusException {

        Report
            .printMethodHeader("testSpin", "Spin computation", "Finite differences", 1.0e-10, ComparisonType.ABSOLUTE);

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
            new TimeComponents(3, 25, 45.6789),
            TimeScalesFactory.getUTC());
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, MathLib.toRadians(50.),
                MathLib.toRadians(10.), MathLib.toRadians(20.),
                MathLib.toRadians(30.), PositionAngle.MEAN,
                FramesFactory.getEME2000(), date, 3.986004415e14);

        final LofOffsetPointing law =
            new LofOffsetPointing(this.earthSpheric,
                new LofOffset(orbit.getFrame(), LOFType.LVLH, RotationOrder.XYX, 0.1, 0.2, 0.3),
                Vector3D.PLUS_K);

        final Propagator propagator = new KeplerianPropagator(orbit, law);

        final double h = 0.01;
        final SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        final SpacecraftState sPlus = propagator.propagate(date.shiftedBy(h));

        final Rotation rMinus = law.getAttitude(orbit, date.shiftedBy(-h), orbit.getFrame()).getRotation();
        final Rotation r0 = law.getAttitude(orbit, date, orbit.getFrame()).getRotation();
        final Rotation rPlus = law.getAttitude(orbit, date.shiftedBy(h), orbit.getFrame()).getRotation();

        // check spin is consistent with attitude evolution
        final double errorAngleMinus = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(), r0);
        final double evolutionAngleMinus = Rotation.distance(rMinus, r0);
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        final double errorAnglePlus = Rotation.distance(r0,
            sPlus.shiftedBy(-h).getAttitude().getRotation());
        final double evolutionAnglePlus = Rotation.distance(r0,
            rPlus);
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

        final Vector3D spin0 = law.getAttitude(orbit, date, orbit.getFrame()).getSpin();
        final Vector3D reference = AngularCoordinates.estimateRate(rMinus, rPlus, 2 * h);
        Assert.assertTrue(spin0.getNorm() > 1.0e-3);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 1.0e-10);

        Report.printToReport("Spin at date", reference, spin0);
    }

    @Test
    public void testRotationAcceleration() throws PatriusException {

        Report.printMethodHeader("testRotationAcceleration", "Rotation acceleration computation", "Finite differences",
            3E-10, ComparisonType.ABSOLUTE);

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
            new TimeComponents(3, 25, 45.6789),
            TimeScalesFactory.getUTC());
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, MathLib.toRadians(50.),
                MathLib.toRadians(10.), MathLib.toRadians(20.),
                MathLib.toRadians(30.), PositionAngle.MEAN,
                FramesFactory.getEME2000(), date, 3.986004415e14);

        final LofOffsetPointing law =
            new LofOffsetPointing(this.earthSpheric,
                new LofOffset(orbit.getFrame(), LOFType.LVLH, RotationOrder.XYX, 0.1, 0.2, 0.3),
                Vector3D.PLUS_K);
        law.setSpinDerivativesComputation(true);
        // Check that derivation of spin with finite difference method is closed to acceleration
        for (int i = 0; i < orbit.getKeplerianPeriod(); i += 1) {
            final Vector3D acc = law.getAttitude(orbit, date.shiftedBy(i), FramesFactory.getITRF())
                .getRotationAcceleration();
            final Vector3D accDerivateSpin =
                this.getSpinFunction(law, orbit, FramesFactory.getITRF(), date.shiftedBy(i))
                    .nthDerivative(1).getVector3D(date.shiftedBy(i));
            Assert.assertEquals(acc.distance(accDerivateSpin), 0.0, 3e-10);
            if (i == 0) {
                Report.printToReport("Rotation acceleration at date", accDerivateSpin, acc);
            }
        }

        // Check rotation acceleration is null when spin derivative is deactivated
        law.setSpinDerivativesComputation(false);
        Assert.assertNull(law.getAttitude(orbit).getRotationAcceleration());
    }

    /**
     * Local function to provide spin function.
     * 
     * @param pvProv
     *        local position-velocity provider around current date
     * @param frame
     *        reference frame from which spin function of date is computed
     * @param zeroAbscissa
     *        the date for which x=0 for spin function of date
     * @param law
     *        law
     * @return spin function of date relative
     */
    public Vector3DFunction getSpinFunction(final AttitudeLaw law, final PVCoordinatesProvider pvProv,
                                            final Frame frame,
                                            final AbsoluteDate zeroAbscissa) {
        return new AbstractVector3DFunction(zeroAbscissa){
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {
                return law.getAttitude(pvProv, date, frame).getSpin();
            }
        };
    }

    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");
            FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

            // Computation date
            this.date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                TimeComponents.H00,
                TimeScalesFactory.getUTC());

            // Body mu
            this.mu = 3.9860047e14;

            // Reference frame = ITRF 2005
            this.frameItrf2005 = FramesFactory.getITRF();

            // Elliptic earth shape
            this.earthSpheric =
                new OneAxisEllipsoid(6378136.460, 0., this.frameItrf2005);

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() throws PatriusException {
        this.date = null;
        this.frameItrf2005 = null;
        this.earthSpheric = null;
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
