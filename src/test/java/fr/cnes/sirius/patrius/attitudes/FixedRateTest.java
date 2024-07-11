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
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
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

public class FixedRateTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(FixedRateTest.class.getSimpleName(), "Fixed rate attitude provider propagation");
    }

    @Test
    public void testZeroRate() throws PatriusException {
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 3, 2),
            new TimeComponents(13, 17, 7.865),
            TimeScalesFactory.getUTC());
        final Frame frame = FramesFactory.getEME2000();
        final FixedRate law = new FixedRate(new Attitude(date, frame,
            new Rotation(false, 0.48, 0.64, 0.36, 0.48),
            Vector3D.ZERO));
        final PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                new Vector3D(0, 0, 3680.853673522056));
        final Orbit orbit = new KeplerianOrbit(pv, frame, date, 3.986004415e14);
        final Rotation attitude0 = law.getAttitude(orbit, date, frame).getRotation();
        Assert.assertEquals(0, Rotation.distance(attitude0, law.getReferenceAttitude().getRotation()), 1.0e-10);
        final Rotation attitude1 = law.getAttitude(orbit.shiftedBy(10.0), date.shiftedBy(10.0), frame).getRotation();
        Assert.assertEquals(0, Rotation.distance(attitude1, law.getReferenceAttitude().getRotation()), 1.0e-10);
        final Rotation attitude2 = law.getAttitude(orbit.shiftedBy(20.0), date.shiftedBy(20.0), frame).getRotation();
        Assert.assertEquals(0, Rotation.distance(attitude2, law.getReferenceAttitude().getRotation()), 1.0e-10);

    }

    @Test
    public void testNonZeroRate() throws PatriusException {
        Report.printMethodHeader("testNonZeroRate", "Rotation computation", "Orekit v7 reference", 1E-10,
            ComparisonType.ABSOLUTE);
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 3, 2),
            new TimeComponents(13, 17, 7.865),
            TimeScalesFactory.getUTC());
        final double rate = 2 * FastMath.PI / (12 * 60);
        final Frame frame = FramesFactory.getEME2000();
        final FixedRate law = new FixedRate(new Attitude(date, frame,
            new Rotation(false, 0.48, 0.64, 0.36, 0.48),
            new Vector3D(rate, Vector3D.PLUS_K)));
        final PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                new Vector3D(0, 0, 3680.853673522056));
        final Orbit orbit = new KeplerianOrbit(pv, FramesFactory.getEME2000(), date, 3.986004415e14);
        final Rotation attitude0 = law.getAttitude(orbit, date, frame).getRotation();
        Assert.assertEquals(0, Rotation.distance(attitude0, law.getReferenceAttitude().getRotation()), 1.0e-10);
        final Rotation attitude1 = law.getAttitude(orbit.shiftedBy(10.0), date.shiftedBy(10.0), frame).getRotation();
        Assert.assertEquals(10 * rate, Rotation.distance(attitude1, law.getReferenceAttitude().getRotation()), 1.0e-10);
        final Rotation attitude2 = law.getAttitude(orbit.shiftedBy(-20.0), date.shiftedBy(-20.0), frame).getRotation();
        Assert.assertEquals(20 * rate, Rotation.distance(attitude2, law.getReferenceAttitude().getRotation()), 1.0e-10);
        Assert.assertEquals(30 * rate, Rotation.distance(attitude2, attitude1), 1.0e-10);
        final Rotation attitude3 = law.getAttitude(orbit.shiftedBy(0.0), date, frame).getRotation();
        Assert.assertEquals(0, Rotation.distance(attitude3, law.getReferenceAttitude().getRotation()), 1.0e-10);

        Report.printToReport("Rotation", law.getReferenceAttitude().getRotation(), attitude3);
    }

    @Test
    public void testSpin() throws PatriusException {

        Report
            .printMethodHeader("testSpin", "Spin computation", "Finite differences", 1.2E-14, ComparisonType.ABSOLUTE);

        final AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
            new TimeComponents(3, 25, 45.6789),
            TimeScalesFactory.getUTC());

        final double rate = 2 * FastMath.PI / (12 * 60);
        final AttitudeProvider law =
            new FixedRate(new Attitude(date, FramesFactory.getEME2000(),
                new Rotation(false, 0.48, 0.64, 0.36, 0.48),
                new Vector3D(rate, Vector3D.PLUS_K)));
        law.setSpinDerivativesComputation(true);

        final KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, MathLib.toRadians(50.),
                MathLib.toRadians(10.), MathLib.toRadians(20.),
                MathLib.toRadians(30.), PositionAngle.MEAN,
                FramesFactory.getEME2000(), date, 3.986004415e14);

        final Propagator propagator = new KeplerianPropagator(orbit, law);

        final double h = 0.01;
        final SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        final SpacecraftState s0 = propagator.propagate(date);
        final SpacecraftState sPlus = propagator.propagate(date.shiftedBy(h));

        // check spin is consistent with attitude evolution
        final double errorAngleMinus = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(),
            s0.getAttitude().getRotation());
        final double evolutionAngleMinus = Rotation.distance(sMinus.getAttitude().getRotation(),
            s0.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        final double errorAnglePlus = Rotation.distance(s0.getAttitude().getRotation(),
            sPlus.shiftedBy(-h).getAttitude().getRotation());
        final double evolutionAnglePlus = Rotation.distance(s0.getAttitude().getRotation(),
            sPlus.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

        final Vector3D spin0 = s0.getAttitude().getSpin();
        final Vector3D reference = AngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
            sPlus.getAttitude().getRotation(),
            2 * h);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 1.2e-14);

        Report.printToReport("Spin", reference, spin0);
    }

    @Test
    public void testRotationAcceleration() throws PatriusException {

        Report.printMethodHeader("testRotationAcceleration", "Rotation acceleration computation", "Finite differences",
            1E-14, ComparisonType.ABSOLUTE);

        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // Computation date
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 04, 07),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());

        // Fixed rate in EME2000 frame
        final double rate = 2 * FastMath.PI / (12 * 60);
        final FixedRate law =
            new FixedRate(new Attitude(date, FramesFactory.getEME2000(),
                new Rotation(false, 0.48, 0.64, 0.36, 0.48),
                new Vector3D(rate, Vector3D.PLUS_K)));

        // Acceleration is 0, so nothing have to be computed here
        // Nevertheless, we activate the computation of spin derivative
        // for coverage purpose (by the way, it will return 0)
        law.setSpinDerivativesComputation(true);

        // frame is EME2000 : rate is constant and acceleration should be 0
        final Frame frameToCompute = FramesFactory.getEME2000();
        for (int i = 0; i < 10000; i += 100) {
            final Vector3D acc = law.getAttitude(null, date.shiftedBy(i), frameToCompute).getRotationAcceleration();
            Assert.assertEquals(acc.distance(Vector3D.ZERO), 0.0, 0.);
        }

        // frame is different from EME2000 : rate is not constant anymore :
        final Frame frameToCompute2 = FramesFactory.getTEME();
        for (int i = 0; i < 10000; i += 100) {
            final Vector3D acc = law.getAttitude(null, date.shiftedBy(i), frameToCompute2).getRotationAcceleration();
            final Vector3D accDerivateSpin = this.getSpinFunction(law, null, frameToCompute2, date.shiftedBy(i))
                .nthDerivative(1).getVector3D(date.shiftedBy(i));
            Assert.assertEquals(acc.distance(accDerivateSpin), 0.0, 1e-14);
            if (i == 0) {
                Report.printToReport("Rotation acceleration", accDerivateSpin, acc);
            }
        }

        // Check rotation acceleration is null when spin derivative is deactivated
        law.setSpinDerivativesComputation(false);
        Assert.assertNull(law.getAttitude(null, date, frameToCompute2).getRotationAcceleration());
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
    public Vector3DFunction getSpinFunction(final FixedRate law, final PVCoordinatesProvider pvProv, final Frame frame,
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
        Utils.setDataRoot("regular-data");
    }

}
