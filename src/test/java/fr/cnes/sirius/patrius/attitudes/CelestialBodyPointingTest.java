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
* VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite 
 *          de convertir les sorties de VacuumSignalPropagation 
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
* VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:06/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
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
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

public class CelestialBodyPointingTest {

    /** Orbit. */
    private Orbit orbit;

    /** Sun. */
    private PVCoordinatesProvider sun;

    /** Sun pointing. */
    private CelestialBodyPointed sunPointing;

    /** Frame. */
    private Frame frame;

    /** Date. */
    private AbsoluteDate date;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(CelestialBodyPointingTest.class.getSimpleName(),
            "Celestial body pointing attitude provider");
    }

    @Test
    public void testSunPointing() throws PatriusException {

        Report.printMethodHeader("testSunPointing", "Spin computation", "Finite differences", 1.0e-10,
            ComparisonType.ABSOLUTE);

        final Attitude attitude = this.sunPointing.getAttitude(this.orbit, this.date, this.frame);
        final Vector3D xDirection = attitude.getRotation().applyTo(Vector3D.PLUS_I);
        final Vector3D zDirection = attitude.getRotation().applyTo(Vector3D.PLUS_K);
        Assert.assertEquals(0,
            Vector3D.dotProduct(zDirection, Vector3D.crossProduct(xDirection, Vector3D.PLUS_K)),
            1.0e-15);

        // the following statement checks we take parallax into account
        // Sun-Earth-Sat are in quadrature, with distance (Earth, Sat) == distance(Sun, Earth) / 5000
        Assert.assertEquals(MathLib.atan(1.0 / 5000.0),
            Vector3D.angle(xDirection,
                this.sun.getPVCoordinates(this.date, this.frame).getPosition()),
            1.0e-15);

        final double h = 0.1;
        final Attitude aMinus = this.sunPointing.getAttitude(this.orbit, this.date.shiftedBy(-h), this.frame);
        final Attitude aPlus = this.sunPointing.getAttitude(this.orbit, this.date.shiftedBy(h), this.frame);

        final Rotation rMinus =
            this.sunPointing.getAttitude(this.orbit, this.date.shiftedBy(-h), this.frame).getRotation();
        final Rotation r0 = this.sunPointing.getAttitude(this.orbit, this.date, this.frame).getRotation();
        final Rotation rPlus =
            this.sunPointing.getAttitude(this.orbit, this.date.shiftedBy(h), this.frame).getRotation();

        // check spin is consistent with attitude evolution
        final double errorAngleMinus = Rotation.distance(aMinus.shiftedBy(h).getRotation(),
            r0);
        final double evolutionAngleMinus = Rotation.distance(rMinus,
            r0);
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        final double errorAnglePlus = Rotation.distance(r0,
            aPlus.shiftedBy(-h).getRotation());
        final double evolutionAnglePlus = Rotation.distance(r0,
            rPlus);
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

        final Vector3D spin0 = this.sunPointing.getAttitude(this.orbit, this.date, this.orbit.getFrame()).getSpin();
        final Vector3D reference = AngularCoordinates.estimateRate(rMinus, rPlus, 2 * h);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 1.0e-10);

        Report.printToReport("Spin at date", reference, spin0);
    }

    @Test
    public void testRotationAcceleration() throws PatriusException {

        Report.printMethodHeader("testRotationAcceleration", "Rotation acceleration computation", "Finite differences",
            1E-11, ComparisonType.ABSOLUTE);

        this.sunPointing.setSpinDerivativesComputation(true);
        this.date = new AbsoluteDate(new DateComponents(2003, 01, 01),
            new TimeComponents(3, 25, 45.6789),
            TimeScalesFactory.getTAI());
        // Check that derivation of spin with finite difference method is closed to acceleration
        for (int i = 0; i < this.orbit.getKeplerianPeriod(); i++) {
            final Vector3D acc =
                this.sunPointing.getAttitude(this.orbit, this.date.shiftedBy(i), this.orbit.getFrame())
                    .getRotationAcceleration();
            final Vector3D spinDerivateAcc =
                this.getSpinFunction(this.sunPointing, this.orbit, this.orbit.getFrame(), this.date.shiftedBy(i))
                    .nthDerivative(1).getVector3D(this.date.shiftedBy(i));
            Assert.assertEquals(acc.distance(spinDerivateAcc), 0.0, 1e-11);
            if (i == 0) {
                Report.printToReport("Rotation acceleration at date", spinDerivateAcc, acc);
            }
        }

        // Check rotation acceleration is null when spin derivative is deactivated
        this.sunPointing.setSpinDerivativesComputation(false);
        Assert.assertNull(this.sunPointing.getAttitude(this.orbit).getRotationAcceleration());
        this.sunPointing.setSpinDerivativesComputation(true);
    }

    // Test getOrientation method
    @Test
    public void testGetOrientation() throws PatriusException {

        Report.printMethodHeader("testGetOrientation", "Rotation computation", "Math", 0, ComparisonType.ABSOLUTE);

        final PVCoordinatesProvider satPvProv = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 8087960709526607657L;

            private final Vector3D position = Vector3D.PLUS_I.scalarMultiply(8000E3);
            private final Vector3D velocity = Vector3D.ZERO;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(this.position, this.velocity);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };
        final PVCoordinatesProvider sunPvProv = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 7979139264383368765L;

            private final Vector3D position = Vector3D.PLUS_I.scalarMultiply(Constants.JPL_SSD_ASTRONOMICAL_UNIT);
            private final Vector3D velocity = Vector3D.ZERO;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                return new PVCoordinates(this.position, this.velocity);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };
        final CelestialBodyPointed attProv = new CelestialBodyPointed(FramesFactory.getEME2000(), sunPvProv,
            Vector3D.PLUS_K, Vector3D.PLUS_I, Vector3D.PLUS_K);
        final Rotation rot = attProv.getAttitude(satPvProv, AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000())
            .getRotation();
        Assert.assertTrue(rot.isEqualTo(Rotation.IDENTITY));

        Report.printToReport("Rotation at date", Rotation.IDENTITY, rot);
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
    public Vector3DFunction getSpinFunction(final CelestialBodyPointed sunpointing, final PVCoordinatesProvider pvProv,
                                            final Frame frame,
                                            final AbsoluteDate zeroAbscissa) {
        return new AbstractVector3DFunction(zeroAbscissa){
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {
                return sunpointing.getAttitude(pvProv, date, frame).getSpin();
            }
        };
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");

        this.sun = CelestialBodyFactory.getSun();

        this.frame = FramesFactory.getGCRF();
        this.date = new AbsoluteDate(new DateComponents(1970, 01, 01),
            new TimeComponents(3, 25, 45.6789),
            TimeScalesFactory.getTAI());
        this.sunPointing =
            new CelestialBodyPointed(this.frame, this.sun, Vector3D.PLUS_K,
                Vector3D.PLUS_I, Vector3D.PLUS_K);
        final PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32120171334, 5948437.45881852374, 0.0),
                new Vector3D(0, 0, 3680.853673522056));
        this.orbit = new KeplerianOrbit(pv, this.frame, this.date, 3.986004415e14);
    }

}
