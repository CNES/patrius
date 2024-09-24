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
* VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes definies dans la façade ALGO DV SIRUS 
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2799:18/05/2021:Suppression des pas de temps fixes codes en dur 
 * VERSION:4.6:DM:DM-2603:27/01/2021:[PATRIUS] Ajout de getters pour les 2 LOS de la classe AbstractGroundPointing 
 * VERSION:4.3:FA:FA-1978:15/05/2019:Anomalie calcul orientation corps celeste (UAI)
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:87:05/08/2013:deleted tests testing the obsolete methods of BodyCenterPointing
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::DM:344:15/04/2015:Construction of an attitude law from a Local Orbital Frame
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:489:15/10/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.BasicPVCoordinatesProvider;
import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.bodies.BasicBoardSun;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 *
 * Non-regression test class for IsisSunAndPseudoSpinPointing class
 * @author fteilhard
 *
 */
public class IsisSunAndPseudoSpinPointingTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(IsisSunAndPseudoSpinPointingTest.class.getSimpleName(),
                "Isis Sun and pseudo spin pointing attitude provider");
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    /**
     * @throws PatriusException
     *
     *
     * @testedMethod {@link IsisSunAndPseudoSpinPointing#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     *
     * @description Non regression test of the getAttitude method with a positive beta sign
     *
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testGetAttitudePositiveBeta() throws PatriusException {
        // The sun direction
        final IDirection sunDirection = new BasicBoardSun();
        final double pseudoSpinPhase = 0.2;
        final boolean positiveBetaSign = true;

        final IsisSunAndPseudoSpinPointing pointing = new IsisSunAndPseudoSpinPointing(sunDirection, pseudoSpinPhase,
                positiveBetaSign);

        // PV coordinates (random values used for non-regression)
        final PVCoordinatesProvider pvProv = new BasicPVCoordinatesProvider(new PVCoordinates(new Vector3D(8e7, 7e8,
                1e4), new Vector3D(4e3, 1.5e3, 4e2)), FramesFactory.getGCRF());

        // The date
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        // A random target frame
        final Frame targetFrame = FramesFactory.getICRF();

        final Attitude attitudeOutput = pointing.getAttitude(pvProv, date, targetFrame);

        // Reference quaternions
        final double q0 = -0.008702658309307127;
        final double q1 = -0.10233592713725569;
        final double q2 = 0.5421854119570007;
        final double q3 = 0.8339583927369082;
        final Attitude attitudeExpected = new Attitude(targetFrame, new TimeStampedAngularCoordinates(date,
                new Rotation(true, q0, q1, q2, q3), new Vector3D(0., 0., -5.42863645645151E-6), null));

        // Comparing the output attitude components to their reference values
        Assert.assertTrue((attitudeOutput.getReferenceFrame()).equals(attitudeExpected.getReferenceFrame()));

        Assert.assertTrue((attitudeOutput.getDate()).equals(attitudeExpected.getDate()));

        Assert.assertTrue((attitudeOutput.getRotation().getQuaternion()).equals(attitudeExpected.getRotation()
                .getQuaternion()));

        Assert.assertTrue((attitudeOutput.getOrientation().getRotation().getQuaternion()).equals(attitudeExpected
                .getOrientation().getRotation().getQuaternion()));

        Assert.assertNull(attitudeOutput.getOrientation().getRotationAcceleration());
    }

    /**
     * @throws PatriusException
     *
     *
     * @testedMethod {@link IsisSunAndPseudoSpinPointing#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     *
     * @description Non regression test of the getAttitude method with a negative beta sign
     *
     *
     * @referenceVersion 4.11
     *
     * @nonRegressionVersion 4.11
     */
    @Test
    public void testGetAttitudeNegativeBeta() throws PatriusException {
        final IDirection sunDirection = new BasicBoardSun();
        final double pseudoSpinPhase = 0.5;
        final boolean positiveBetaSign = false;

        final IsisSunAndPseudoSpinPointing pointing = new IsisSunAndPseudoSpinPointing(sunDirection, pseudoSpinPhase,
                positiveBetaSign);

        final PVCoordinatesProvider pvProv = new BasicPVCoordinatesProvider(new PVCoordinates(new Vector3D(-8e7, -7e8,
                1e4), new Vector3D(4e3, 1.5e3, 4e2)), FramesFactory.getGCRF());

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        final Frame targetFrame = FramesFactory.getICRF();

        final Attitude attitudeOutput = pointing.getAttitude(pvProv, date, targetFrame);

        final double q0 = 0.8232935059429012;
        final double q1 = -0.5513901325547855;
        final double q2 = -0.02016394413898541;
        final double q3 = 0.1332296519181156;
        final Attitude attitudeExpected = new Attitude(targetFrame, new TimeStampedAngularCoordinates(date,
                new Rotation(true, q0, q1, q2, q3), new Vector3D(0., 0., 5.428644649696134E-6), null));

        Assert.assertTrue((attitudeOutput.getReferenceFrame()).equals(attitudeExpected.getReferenceFrame()));

        Assert.assertTrue((attitudeOutput.getDate()).equals(attitudeExpected.getDate()));

        Assert.assertTrue((attitudeOutput.getRotation().getQuaternion()).equals(attitudeExpected.getRotation()
                .getQuaternion()));

        Assert.assertTrue((attitudeOutput.getOrientation().getRotation().getQuaternion()).equals(attitudeExpected
                .getOrientation().getRotation().getQuaternion()));

        Assert.assertNull(attitudeOutput.getOrientation().getRotationAcceleration());
    }

    /**
     * Check that an exception is raised if xSun is the null vector.
     * @throws PatriusException
     */
    @Test(expected = PatriusException.class)
    public void testGetAttitudeXSunZero() throws PatriusException {
        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        final IDirection sunDirection = new BasicBoardSun();
        final double pseudoSpinPhase = 0.5;
        final boolean positiveBetaSign = false;

        final IsisSunAndPseudoSpinPointing pointing = new IsisSunAndPseudoSpinPointing(sunDirection, pseudoSpinPhase,
                positiveBetaSign);

        // Earth to Sun vector
        final Vector3D sunVector = sunDirection.getVector(null, date, gcrf);
        // Create a vector normal to the sun vector
        final Vector3D vectorInNormalPlanToSunVector = sunVector.crossProduct(new Vector3D(1, 0, 0));
        // Create another vector normal to the sun vector
        final Vector3D secondVectorInNormalPlanToSunVector = sunVector.crossProduct(new Vector3D(1, -1, 0));

        // PV coordinates with both positions and velocities vector in the plan normal to the sun
        // direction and containing the Earth.
        final PVCoordinatesProvider pvProv = new BasicPVCoordinatesProvider(new PVCoordinates(
                vectorInNormalPlanToSunVector, secondVectorInNormalPlanToSunVector), FramesFactory.getGCRF());

        final Frame targetFrame = FramesFactory.getICRF();

        // The pvProv construction shall produce to a null Xsun vector, leading to an exception in
        // the attitude computation.
        pointing.getAttitude(pvProv, date, targetFrame);

    }
}
