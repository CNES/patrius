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
 * @history creation 22/10/2015
 *
 * HISTORY
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-108:08/12/2023:[PATRIUS] Modele d'obliquite et de precession de la Terre
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes definies dans la façade ALGO DV SIRUS 
 * VERSION:4.11:DM:DM-3300:22/05/2023:[PATRIUS] Nouvelle approche pour le calcul de la position relative de 2 corps celestes 
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:FA:FA-3201:03/11/2022:[PATRIUS] Prise en compte de l'aberration stellaire dans l'interface ITargetDirection
 * VERSION:4.9:DM:DM-3147:10/05/2022:[PATRIUS] Ajout a l'interface ITargetDirection d'une methode getTargetPvProv ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:419:22/10/2015: Creation direction to central body center
 * VERSION::DM:557:15/02/2016: class rename
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes.directions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.TabulatedAttitudeTest;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the North Normal To Ecliptic Direction
 *              </p>
 *
 * @author fteilhard
 *
 *
 * @since 3.1
 */
public class NorthNormalToEclipticDirectionTest {

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(TabulatedAttitudeTest.class.getSimpleName(), "North normal to ecliptic direction");
    }

    /**
     * @testType UT
     *
     * @testedMethod {@link NorthNormalToEclipticDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     *
     * @description Instantiation of a direction described by a central body
     *              (the central body's center is the target point),
     *              and getting of the vector associated to a given origin expressed in a frame, at
     *              a date.
     *
     * @output Vector3D
     *
     * @testPassCriteria the returned vector is the correct one from the origin to the central
     *                   body's center,
     *                   when expressed in the wanted frame. The 1.0e-14 epsilon is the simple
     *                   double comparison
     *                   epsilon, used because
     *                   the computations involve here no mechanics algorithms.
     *
     * @referenceVersion 3.1
     *
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testGetVector() {

        Report.printMethodHeader("testGetVector", "Get direction vector", "Math", this.comparisonEpsilon,
                ComparisonType.ABSOLUTE);

        try {
            // frames creation
            final Frame frame = FramesFactory.getGCRF();
            // Sun body
            final CelestialPoint sunBody = CelestialBodyFactory.getSun();
            // the date
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

            // reference direction creation
            final NorthNormalToEclipticDirection directionRef = new NorthNormalToEclipticDirection(sunBody);
            // get the direction vector in Earth frame as given with the getVector method.
            final Vector3D vectorRes = directionRef.getVector(null, date, frame);

            // get Sun PV as seen from Earth
            final PVCoordinates sunPV = sunBody.getPVCoordinates(date, frame);
            // get the normalized momentum vector: it is normal to the ecliptic plan
            final Vector3D vectorExp = sunPV.getMomentum().normalize();

            Assert.assertEquals(vectorRes.getX(), vectorExp.getX(), this.comparisonEpsilon);
            Assert.assertEquals(vectorRes.getY(), vectorExp.getY(), this.comparisonEpsilon);
            Assert.assertEquals(vectorRes.getZ(), vectorExp.getZ(), this.comparisonEpsilon);

            // A translated frame from GCRF. The normal to the ecliptic plan should be the same in a
            // translated frame
            final Vector3D translationVect = new Vector3D(1e8, -1e8, 1e5);
            final Transform outTransform = new Transform(date, translationVect);
            final Frame outputFrame = new Frame(frame, outTransform, "outFram");

            // Creation of another origin from the earth frame. The normal to the ecliptic plan
            // should be the same in for any origin
            final Vector3D originPos = new Vector3D(1.635732e4, -8.654534e5, 5.6721e8);
            final Vector3D originVel = new Vector3D(7.6874231e4, 654.687534e4, -17.721e6);
            final PVCoordinates originPV = new PVCoordinates(originPos, originVel);
            final BasicPVCoordinatesProvider inOrigin = new BasicPVCoordinatesProvider(originPV, frame);

            // get the direction vector in the output translated frame and from another origin
            final Vector3D vectorFromNewOriginInOutputFrame = directionRef.getVector(inOrigin, date, outputFrame);

            Assert.assertEquals(vectorFromNewOriginInOutputFrame.getX(), vectorRes.getX(), this.comparisonEpsilon);
            Assert.assertEquals(vectorFromNewOriginInOutputFrame.getY(), vectorRes.getY(), this.comparisonEpsilon);
            Assert.assertEquals(vectorFromNewOriginInOutputFrame.getZ(), vectorRes.getZ(), this.comparisonEpsilon);

        } catch (final PatriusException e) {
            Assert.fail();
        }
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     *
     * @testedMethod {@link NorthNormalToEclipticDirection#getLine(PVCoordinatesProvider, AbsoluteDate, Frame)}
     *
     * @description Instantiation of a direction described as the North normal to the ecliptic plan.
     *
     *
     * @output Line
     *
     * @testPassCriteria the returned Line contains the origin
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testGetLine() throws PatriusException {
        // frames creation
        final Frame gcrf = FramesFactory.getGCRF();
        // Sun body
        final CelestialPoint sunBody = CelestialBodyFactory.getSun();
        // the date
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // A translated frame from GCRF. The normal to the ecliptic plan should be the same in a
        // translated frame
        final Vector3D translationVect = new Vector3D(150, 1878, 695);
        final Transform outTransform = new Transform(date, translationVect);
        final Frame outputFrame = new Frame(gcrf, outTransform, "outFram");

        // Creation of another origin from the earth frame. The normal to the ecliptic plan
        // should be the same in for any origin. The origin shall be contained by the line.
        final Vector3D originPos = new Vector3D(1.635732e3, -8.654534e2, 5.6721e1);
        final Vector3D originVel = new Vector3D(7.6874231e4, 654.687534e4, -17.721e6);

        final PVCoordinates originPV = new PVCoordinates(originPos, originVel);
        final BasicPVCoordinatesProvider inOrigin = new BasicPVCoordinatesProvider(originPV, gcrf);

        // direction creation
        final NorthNormalToEclipticDirection direction = new NorthNormalToEclipticDirection(sunBody);

        // line creation
        final Line line = direction.getLine(inOrigin, date, outputFrame);
        // expected point
        final Vector3D expectedOrigin = outTransform.transformPosition(originPos);

        // test of the point being on the line
        Assert.assertTrue(line.contains(expectedOrigin));

        // line creation with a null origin: it is equivalent to (0,0,0) in gcrf
        final Line lineOriginNull = direction.getLine(null, date, outputFrame);
        // test of the point being on the line
        Assert.assertTrue(lineOriginNull.contains(outTransform.transformPosition(Vector3D.ZERO)));
    }

    /**
     * @throws PatriusException
     * @testType UT
     *
     *
     * @testedMethod {@link NorthNormalToEclipticDirection#getVector(PVCoordinatesProvider, AbsoluteDate, Frame)}
     *
     * @description Test the getVector method in the case of a Sun with zero velocity
     *
     *
     * @output A Vector3D colinear with the angular momentum vector
     *
     * @testPassCriteria the returned vector is the same as the non-regression reference value
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testSunPVZero() throws PatriusException {
        // frames creation
        final Frame gcrf = FramesFactory.getGCRF();
        // Sun body with no velocity
        final CelestialPoint sunBody = new MeeusSun();
        // the date
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        // Creation of another origin from the earth frame. The normal to the ecliptic plan
        // should be the same in for any origin. The origin shall be contained by the line.
        final Vector3D originPos = new Vector3D(1.635732e3, -8.654534e2, 5.6721e1);
        final Vector3D originVel = new Vector3D(7.6874231e4, 654.687534e4, -17.721e6);

        final PVCoordinates originPV = new PVCoordinates(originPos, originVel);
        final BasicPVCoordinatesProvider inOrigin = new BasicPVCoordinatesProvider(originPV, gcrf);

        // direction creation
        final NorthNormalToEclipticDirection direction = new NorthNormalToEclipticDirection(sunBody);

        final Vector3D vector = direction.getVector(inOrigin, date, gcrf);

        // non-regression value
        final Vector3D vectorRef = new Vector3D(-3.3242509297753987E-7, -0.3977772118105524, 0.9174820378427648);

        // Compare the reference value to the result
        Assert.assertEquals(vectorRef.getX(), vector.getX(), 0.);
        Assert.assertEquals(vectorRef.getY(), vector.getY(), 0.);
        Assert.assertEquals(vectorRef.getZ(), vector.getZ(), 0.);
    }

    /**
     * Set up
     *
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
    }
}
