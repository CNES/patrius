/**
 *
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
 * HISTORY
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11:DM:DM-14:22/05/2023:[PATRIUS] Nombre max d'iterations dans le calcul de la propagation du signal 
 * VERSION:4.11:DM:DM-3318:22/05/2023:[PATRIUS] Besoin de forcer la normalisation dans la classe QuaternionPolynomialSegment
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.signalpropagation.VacuumSignalPropagationModel.FixedDate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This test validate the {@link VacuumSignalPropagationModel} partial derivatives computation by finite difference.
 *
 * @author bonitt
 */
public class VacuumSignalPropagationDPTest {

    /** Reference date. */
    private static AbsoluteDate date;

    /** Non-regression validity threshold. */
    private static double validityThreshold;

    /**
     * This test validate the {@link VacuumSignalPropagationModel} partial derivatives computation by finite difference.
     *
     * <p>
     * The following partial derivatives are evaluated (for both EMISSION/RECEPTION cases):
     * <ul>
     * <li>The propagation time/duration derivatives wrt the emitter position (inertial and non inertial frame)</li>
     * <li>The propagation time/duration derivatives wrt the receiver position (inertial and non inertial frame)</li>
     * <li>The propagation time/duration derivatives wrt time</li>
     * <li>The propagation vector derivatives wrt time</li>
     * </ul>
     * </p>
     *
     * <p>
     * For each partial derivatives:
     * <ul>
     * <li>Display the results: absolute and relative differences</li>
     * <li>Non regression validation test wrt reference values</li>
     * </ul>
     * </p>
     */
    @Test
    public void derivativesTest() throws PatriusException {

        System.out.println("\n" + "---------------------------------------------" + "\n"
                + "SignalPropagationDPTest report:" + "\n");

        // Define an inertial frame different from the standard GCRF to observe any frame conversion eventual errors
        final Frame frame = FramesFactory.getCIRF();

        // "spacecraft" initialization
        final double mu = Constants.EGM96_EARTH_MU;
        final CircularOrbit pvSat = new CircularOrbit(7000.0e3, 0.0, 0.0, MathLib.toRadians(45),
            0.0, MathLib.toRadians(45), PositionAngle.TRUE, frame, VacuumSignalPropagationDPTest.date, mu);

        // "station" initialization
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.GRS80_EARTH_EQUATORIAL_RADIUS,
            Constants.GRS80_EARTH_FLATTENING, FramesFactory.getITRF(), "Earth");
        final EllipsoidPoint coordinates = new EllipsoidPoint(earth, earth.getLLHCoordinatesSystem(),
            MathLib.toRadians(0.), MathLib.toRadians(0.), 0., "");
        final TopocentricFrame pvSta = new TopocentricFrame(coordinates, "topo");

        // Shift steps initialization
        final double hPos = 1.;
        final double hT = 1.;

        // Spacecraft / station PV shift initialization
        final PVCoordinates hSatX = new PVCoordinates(new Vector3D(hPos, 0., 0.), Vector3D.ZERO);
        final PVCoordinates hSatY = new PVCoordinates(new Vector3D(0., hPos, 0.), Vector3D.ZERO);
        final PVCoordinates hSatZ = new PVCoordinates(new Vector3D(0., 0., hPos), Vector3D.ZERO);

        final PVCoordinates hStaX = new PVCoordinates(new Vector3D(hPos, 0., 0.), Vector3D.ZERO);
        final PVCoordinates hStaY = new PVCoordinates(new Vector3D(0., hPos, 0.), Vector3D.ZERO);
        final PVCoordinates hStaZ = new PVCoordinates(new Vector3D(0., 0., hPos), Vector3D.ZERO);

        // ---- Check the propagation position vector derivatives wrt the emitter position ----
        System.out.println("\n" + "Validating the dPropdPem method (inertial frame):");

        // Emitter = spacecraft
        System.out.println("\n" + "fixedDate: " + FixedDate.RECEPTION);
        dPropdPemValidation(pvSat, pvSta, hSatX, FixedDate.RECEPTION, frame,
            new double[] { -1.0000061458708251, 4.3461327878940215E-6, 4.3461327878940215E-6 },
            new double[] { -1.0000061457976699, 4.346482455730438E-6, 4.346249625086784E-6 });
        dPropdPemValidation(pvSat, pvSta, hSatY, FixedDate.RECEPTION, frame,
            new double[] { -1.5725711329838063E-5, -0.9999888793579352, 1.1120642064723264E-5 },
            new double[] { -1.5725381672382355E-5, -0.9999888790771365, 1.1120922863483429E-5 });
        dPropdPemValidation(pvSat, pvSta, hSatZ, FixedDate.RECEPTION, frame,
            new double[] { -5.6291303393676225E-6, 3.98071300730319E-6, -0.9999960192869927 },
            new double[] { -5.628913640975952E-6, 3.981404006481171E-6, -0.9999960190616548 });

        // Emitter = station
        System.out.println("\n" + "fixedDate: " + FixedDate.EMISSION);
        dPropdPemValidation(pvSta, pvSat, hStaX, FixedDate.EMISSION, frame,
            new double[] { -0.9999938542736637, -4.345338946070621E-6, -4.345338946070621E-6 },
            new double[] { -0.9999938546679914, -4.345551133155823E-6, -4.345318302512169E-6 });
        dPropdPemValidation(pvSta, pvSat, hStaY, FixedDate.EMISSION, frame,
            new double[] { 1.5727410864746675E-5, -1.0000111200738875, -1.112007388749634E-5 },
            new double[] { 1.5727709978818893E-5, -1.000011119991541, -1.1119991540908813E-5 });
        dPropdPemValidation(pvSta, pvSat, hStaZ, FixedDate.EMISSION, frame,
            new double[] { 5.630026350166057E-6, -3.980713007423954E-6, -1.0000039807130074 },
            new double[] { 5.62937930226326E-6, -3.980472683906555E-6, -1.000003980472684 });

        // ---- Check the propagation position vector derivatives wrt the receiver position ----
        System.out.println("\n\n" + "Validating the dPropdPrec method (inertial frame):");
        // Receiver = station
        System.out.println("\n" + "fixedDate: " + FixedDate.RECEPTION);
        dPropdPrecValidation(pvSat, pvSta, hStaX, FixedDate.RECEPTION, frame,
            new double[] { 1.0000061458708251, -4.3461327878940215E-6, -4.3461327878940215E-6 },
            new double[] { 1.0000061457976699, -4.346482455730438E-6, -4.346249625086784E-6 });
        dPropdPrecValidation(pvSat, pvSta, hStaY, FixedDate.RECEPTION, frame,
            new double[] { 1.5725711329838063E-5, 0.9999888793579352, -1.1120642064723264E-5 },
            new double[] { 1.5725381672382355E-5, 0.9999888790771365, -1.1120922863483429E-5 });
        dPropdPrecValidation(pvSat, pvSta, hStaZ, FixedDate.RECEPTION, frame,
            new double[] { 5.6291303393676225E-6, -3.98071300730319E-6, 0.9999960192869927 },
            new double[] { 5.628913640975952E-6, -3.981404006481171E-6, 0.9999960190616548 });

        // Receiver = spacecraft
        System.out.println("\n" + "fixedDate: " + FixedDate.EMISSION);
        dPropdPrecValidation(pvSta, pvSat, hSatX, FixedDate.EMISSION, frame,
            new double[] { 0.9999938542736637, 4.345338946070621E-6, 4.345338946070621E-6 },
            new double[] { 0.9999938546679914, 4.345551133155823E-6, 4.345318302512169E-6 });
        dPropdPrecValidation(pvSta, pvSat, hSatY, FixedDate.EMISSION, frame,
            new double[] { -1.5727410864746675E-5, 1.0000111200738875, 1.112007388749634E-5 },
            new double[] { -1.5727709978818893E-5, 1.000011119991541, 1.1119991540908813E-5 });
        dPropdPrecValidation(pvSta, pvSat, hSatZ, FixedDate.EMISSION, frame,
            new double[] { -5.630026350166057E-6, 3.980713007423954E-6, 1.0000039807130074 },
            new double[] { -5.62937930226326E-6, 3.980472683906555E-6, 1.000003980472684 });

        // ---- Check the propagation time/duration derivatives wrt the emitter position ----
        System.out.println("\n\n" + "Validating the dTpropdPem method:");
        // Emitter = spacecraft
        System.out.println("\n" + "fixedDate: " + FixedDate.RECEPTION);
        dTpropdPemValidation(pvSat, pvSta, hSatX, FixedDate.RECEPTION, frame,
            1.1518497684434553E-9, 1.1518497822216034E-9);
        dTpropdPemValidation(pvSat, pvSta, hSatY, FixedDate.RECEPTION, frame,
            2.9472889146125162E-9, 2.947288912924506E-9);
        dTpropdPemValidation(pvSat, pvSta, hSatZ, FixedDate.RECEPTION, frame,
            1.0550030520176175E-9, 1.0550030848932579E-9);

        // Emitter = station
        System.out.println("\n" + "fixedDate: " + FixedDate.EMISSION);
        dTpropdPemValidation(pvSta, pvSat, hStaX, FixedDate.EMISSION, frame,
            -1.151731029423604E-9, -1.1517310438691197E-9);
        dTpropdPemValidation(pvSta, pvSat, hStaY, FixedDate.EMISSION, frame,
            -2.9473728757785494E-9, -2.947372901296319E-9);
        dTpropdPemValidation(pvSta, pvSat, hStaZ, FixedDate.EMISSION, frame,
            -1.0550870131836683E-9, -1.0550870177539196E-9);

        // ---- Check the propagation time/duration derivatives wrt the receiver position ----
        System.out.println("\n\n" + "Validating the dTpropdPrec method:");
        // Receiver = station
        System.out.println("\n" + "fixedDate: " + FixedDate.RECEPTION);
        dTpropdPrecValidation(pvSat, pvSta, hStaX, FixedDate.RECEPTION, frame,
            -1.1518497684433155E-9, -1.1518497267104522E-9);
        dTpropdPrecValidation(pvSat, pvSta, hStaY, FixedDate.RECEPTION, frame,
            -2.947288914612528E-9, -2.947288912924506E-9);
        dTpropdPrecValidation(pvSat, pvSta, hStaZ, FixedDate.RECEPTION, frame,
            -1.0550030520177389E-9, -1.0550030848932579E-9);

        // Receiver = spacecraft
        System.out.println("\n" + "fixedDate: " + FixedDate.EMISSION);
        dTpropdPrecValidation(pvSta, pvSat, hSatX, FixedDate.EMISSION, frame,
            1.1517310294236166E-9, 1.1517310438691197E-9);
        dTpropdPrecValidation(pvSta, pvSat, hSatY, FixedDate.EMISSION, frame,
            2.947372875778584E-9, 2.947372901296319E-9);
        dTpropdPrecValidation(pvSta, pvSat, hSatZ, FixedDate.EMISSION, frame,
            1.0550870131835593E-9, 1.0550870177539196E-9);

        // ---- Check the propagation time/duration derivatives wrt time ----
        System.out.println("\n\n" + "Validating the dTpropdT method:");
        dTpropdTValidation(pvSat, pvSta, hT, FixedDate.RECEPTION, frame,
            8.185611713789217E-6, 8.185611108713076E-6);
        dTpropdTValidation(pvSat, pvSta, hT, FixedDate.EMISSION, frame,
            8.185446352397563E-6, 8.18544574737734E-6);

        // ---- Check the propagation vector derivatives wrt time ----
        System.out.println("\n\n" + "Validating the dPropdT method:");
        dPropdTValidation(pvSat, pvSta, hT, FixedDate.RECEPTION, frame,
            new double[] { 5793.36862456802, -3690.832018443726, -3773.1458834355053 },
            new double[] { 5793.367586450186, -3690.8312885025516, -3773.1451525241137 });
        dPropdTValidation(pvSat, pvSta, hT, FixedDate.EMISSION, frame,
            new double[] { 5793.628143610548, -3690.7108727573423, -3773.026643633918 },
            new double[] { 5793.62710540439, -3690.7101428359747, -3773.025912744226 });

        System.out.println("\n" + "---------------------------------------------" + "\n");
    }

    /**
     * Evaluate the propagation position vector derivatives wrt the emitter position computation:
     * finite difference method.
     * <p>
     * <ul>
     * <li>Display the results: absolute and relative differences</li>
     * <li>Non regression validation test wrt reference values</li>
     * </ul>
     * </p>
     *
     * @param pvEm
     *        Emitter PV
     * @param pvRec
     *        Receiver PV
     * @param hEm
     *        Emitter PV shift
     * @param fixedDate
     *        Fixed date of computation
     * @param analysisFrame
     *        Working frame for the analysis (must be inertial)
     * @param expectedVal
     *        Expected analytic (derivative) values (for non-regression evaluation)
     * @param expectedRefVal
     *        Expected numeric (finite difference) values (for non-regression evaluation)
     * @throws PatriusException
     *         if a problem occurs during PVCoodinates providers manipulations
     */
    private static void dPropdPemValidation(final PVCoordinatesProvider pvEm, final PVCoordinatesProvider pvRec,
                                            final PVCoordinates hEm, final FixedDate fixedDate,
                                            final Frame analysisFrame, final double[] expectedVal,
                                            final double[] expectedRefVal)
        throws PatriusException {

        final double threshold = 1e-13;
        final VacuumSignalPropagationModel model =
            new VacuumSignalPropagationModel(analysisFrame, threshold, VacuumSignalPropagationModel.DEFAULT_MAX_ITER);

        // Apply the PV shift on the emitter to generate pvEmPlusHEm & pvEmMinusHEm
        final PVCoordinatesProvider pvEmPlusHEm = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -5167574309132454052L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                throws PatriusException {
                final PVCoordinates pvEmRef = new PVCoordinates(1., pvEm.getPVCoordinates(date, analysisFrame), 1., hEm);
                final Transform t = analysisFrame.getTransformTo(frame, date);
                return t.transformPVCoordinates(pvEmRef);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        final PVCoordinatesProvider pvEmMinusHEm = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -1967987720521307564L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                throws PatriusException {
                final PVCoordinates pvEmRef = new PVCoordinates(1., pvEm.getPVCoordinates(date, analysisFrame), -1.,
                    hEm);
                final Transform t = analysisFrame.getTransformTo(frame, date);
                return t.transformPVCoordinates(pvEmRef);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        // Compute the signal and the shifted signals
        final VacuumSignalPropagation signal = model.computeSignalPropagation(pvEm, pvRec,
            VacuumSignalPropagationDPTest.date, fixedDate);
        final VacuumSignalPropagation signalPlusHEm = model.computeSignalPropagation(pvEmPlusHEm, pvRec,
            VacuumSignalPropagationDPTest.date, fixedDate);
        final VacuumSignalPropagation signalMinusHEm = model.computeSignalPropagation(pvEmMinusHEm,
            pvRec, VacuumSignalPropagationDPTest.date, fixedDate);

        // Extract the shifted signals propagation vector and compute the finite difference
        final Vector3D propPlusHEm = signalPlusHEm.getVector();
        final Vector3D propMinusHEm = signalMinusHEm.getVector();
        final Vector3D valTheo = propPlusHEm.subtract(propMinusHEm);

        // Extract the signal's propagation position vector derivatives wrt the emitter position
        final RealMatrix dPropdPem = signal.getdPropdPem();

        // Evaluate the results (reporting & validation) for each axis
        final Vector3D hEmPos = hEm.getPosition();
        if (hEmPos.getX() != 0.) {
            final double hEmPosX = hEmPos.getX();
            final double numValX = valTheo.getX() / (2 * hEmPosX);
            final double numValY = valTheo.getY() / (2 * hEmPosX);
            final double numValZ = valTheo.getZ() / (2 * hEmPosX);

            final double analValX = dPropdPem.getEntry(0, 0); // dPropXdPemX
            final double analValY = dPropdPem.getEntry(1, 0); // dPropYdPemX
            final double analValZ = dPropdPem.getEntry(2, 0); // dPropZdPemX

            System.out.println("dPropXdPemX: " + compUtils(analValX, numValX));
            System.out.println("dPropYdPemX: " + compUtils(analValY, numValY));
            System.out.println("dPropZdPemX: " + compUtils(analValZ, numValZ));

            Assert.assertEquals(expectedVal[0], analValX, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[0], numValX, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedVal[1], analValY, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[1], numValY, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedVal[2], analValZ, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[2], numValZ, VacuumSignalPropagationDPTest.validityThreshold);
        }
        if (hEmPos.getY() != 0.) {
            final double hEmPosY = hEmPos.getY();
            final double numValX = valTheo.getX() / (2 * hEmPosY);
            final double numValY = valTheo.getY() / (2 * hEmPosY);
            final double numValZ = valTheo.getZ() / (2 * hEmPosY);

            final double analValX = dPropdPem.getEntry(0, 1); // dPropXdPemY
            final double analValY = dPropdPem.getEntry(1, 1); // dPropYdPemY
            final double analValZ = dPropdPem.getEntry(2, 1); // dPropZdPemY

            System.out.println("dPropXdPemY: " + compUtils(analValX, numValX));
            System.out.println("dPropYdPemY: " + compUtils(analValY, numValY));
            System.out.println("dPropZdPemY: " + compUtils(analValZ, numValZ));

            Assert.assertEquals(expectedVal[0], analValX, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[0], numValX, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedVal[1], analValY, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[1], numValY, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedVal[2], analValZ, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[2], numValZ, VacuumSignalPropagationDPTest.validityThreshold);
        }
        if (hEmPos.getZ() != 0.) {
            final double hEmPosZ = hEmPos.getZ();
            final double numValX = valTheo.getX() / (2 * hEmPosZ);
            final double numValY = valTheo.getY() / (2 * hEmPosZ);
            final double numValZ = valTheo.getZ() / (2 * hEmPosZ);

            final double analValX = dPropdPem.getEntry(0, 2); // dPropXdPemZ
            final double analValY = dPropdPem.getEntry(1, 2); // dPropYdPemZ
            final double analValZ = dPropdPem.getEntry(2, 2); // dPropZdPemZ

            System.out.println("dPropXdPemZ: " + compUtils(analValX, numValX));
            System.out.println("dPropYdPemZ: " + compUtils(analValY, numValY));
            System.out.println("dPropZdPemZ: " + compUtils(analValZ, numValZ));

            Assert.assertEquals(expectedVal[0], analValX, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[0], numValX, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedVal[1], analValY, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[1], numValY, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedVal[2], analValZ, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[2], numValZ, VacuumSignalPropagationDPTest.validityThreshold);
        }
    }

    /**
     * Evaluate the propagation position vector derivatives wrt the receiver position computation:
     * finite difference method.
     * <p>
     * <ul>
     * <li>Display the results: absolute and relative differences</li>
     * <li>Non regression validation test wrt reference values</li>
     * </ul>
     * </p>
     *
     * @param pvEm
     *        Emitter PV
     * @param pvRec
     *        Receiver PV
     * @param hRec
     *        Receiver PV shift
     * @param fixedDate
     *        Fixed date of computation
     * @param analysisFrame
     *        Working frame for the analysis (must be inertial)
     * @param expectedVal
     *        Expected analytic (derivative) values (for non-regression evaluation)
     * @param expectedRefVal
     *        Expected numeric (finite difference) values (for non-regression evaluation)
     * @throws PatriusException
     *         if a problem occurs during PVCoodinates providers manipulations
     */
    private static void dPropdPrecValidation(final PVCoordinatesProvider pvEm,
                                             final PVCoordinatesProvider pvRec, final PVCoordinates hRec,
                                             final FixedDate fixedDate,
                                             final Frame analysisFrame, final double[] expectedVal,
                                             final double[] expectedRefVal) throws PatriusException {

        final double threshold = 1e-13;
        final VacuumSignalPropagationModel model =
            new VacuumSignalPropagationModel(analysisFrame, threshold, VacuumSignalPropagationModel.DEFAULT_MAX_ITER);

        // Apply the PV shift on the receiver to generate pvRecPlusHRec & pvRecMinusHRec
        final PVCoordinatesProvider pvRecPlusHRec = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -3797726685731001748L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                final PVCoordinates pvRecRef = new PVCoordinates(1., pvRec.getPVCoordinates(date, analysisFrame), 1.,
                    hRec);
                final Transform t = analysisFrame.getTransformTo(frame, date);
                return t.transformPVCoordinates(pvRecRef);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        final PVCoordinatesProvider pvRecMinusHRec = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 4890557515299898028L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                final PVCoordinates pvRecRef = new PVCoordinates(1., pvRec.getPVCoordinates(date, analysisFrame), -1.,
                    hRec);
                final Transform t = analysisFrame.getTransformTo(frame, date);
                return t.transformPVCoordinates(pvRecRef);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        // Compute the signal and the shifted signals
        final VacuumSignalPropagation signal = model.computeSignalPropagation(pvEm, pvRec,
            VacuumSignalPropagationDPTest.date, fixedDate);
        final VacuumSignalPropagation signalPlusHRec = model.computeSignalPropagation(pvEm,
            pvRecPlusHRec, VacuumSignalPropagationDPTest.date, fixedDate);
        final VacuumSignalPropagation signalMinusHRec = model.computeSignalPropagation(pvEm,
            pvRecMinusHRec, VacuumSignalPropagationDPTest.date, fixedDate);

        // Extract the shifted signals propagation vector and compute the finite difference
        final Vector3D propPlusHRec = signalPlusHRec.getVector();
        final Vector3D propMinusHRec = signalMinusHRec.getVector();
        final Vector3D valTheo = propPlusHRec.subtract(propMinusHRec);

        // Extract the propagation position vector derivatives wrt the receiver position
        final RealMatrix dPropdPrec = signal.getdPropdPrec();

        // Evaluate the results (reporting & validation) for each axis
        final Vector3D hRecPos = hRec.getPosition();
        if (hRecPos.getX() != 0.) {
            final double hRecPosX = hRecPos.getX();
            final double numValX = valTheo.getX() / (2 * hRecPosX);
            final double numValY = valTheo.getY() / (2 * hRecPosX);
            final double numValZ = valTheo.getZ() / (2 * hRecPosX);

            final double analValX = dPropdPrec.getEntry(0, 0); // dPropXdPrecX
            final double analValY = dPropdPrec.getEntry(1, 0); // dPropYdPrecX
            final double analValZ = dPropdPrec.getEntry(2, 0); // dPropZdPrecX

            System.out.println("dPropXdPrecX: " + compUtils(analValX, numValX));
            System.out.println("dPropYdPrecX: " + compUtils(analValY, numValY));
            System.out.println("dPropZdPrecX: " + compUtils(analValZ, numValZ));

            Assert.assertEquals(expectedVal[0], analValX, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[0], numValX, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedVal[1], analValY, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[1], numValY, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedVal[2], analValZ, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[2], numValZ, VacuumSignalPropagationDPTest.validityThreshold);
        }
        if (hRecPos.getY() != 0.) {
            final double hRecPosY = hRecPos.getY();
            final double numValX = valTheo.getX() / (2 * hRecPosY);
            final double numValY = valTheo.getY() / (2 * hRecPosY);
            final double numValZ = valTheo.getZ() / (2 * hRecPosY);

            final double analValX = dPropdPrec.getEntry(0, 1); // dPropXdPrecY
            final double analValY = dPropdPrec.getEntry(1, 1); // dPropYdPrecY
            final double analValZ = dPropdPrec.getEntry(2, 1); // dPropZdPrecY

            System.out.println("dPropXdPrecY: " + compUtils(analValX, numValX));
            System.out.println("dPropYdPrecY: " + compUtils(analValY, numValY));
            System.out.println("dPropZdPrecY: " + compUtils(analValZ, numValZ));

            Assert.assertEquals(expectedVal[0], analValX, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[0], numValX, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedVal[1], analValY, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[1], numValY, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedVal[2], analValZ, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[2], numValZ, VacuumSignalPropagationDPTest.validityThreshold);
        }
        if (hRecPos.getZ() != 0.) {
            final double hRecPosZ = hRecPos.getZ();
            final double numValX = valTheo.getX() / (2 * hRecPosZ);
            final double numValY = valTheo.getY() / (2 * hRecPosZ);
            final double numValZ = valTheo.getZ() / (2 * hRecPosZ);

            final double analValX = dPropdPrec.getEntry(0, 2); // dPropXdPrecZ
            final double analValY = dPropdPrec.getEntry(1, 2); // dPropYdPrecZ
            final double analValZ = dPropdPrec.getEntry(2, 2); // dPropZdPrecZ

            System.out.println("dPropXdPrecZ: " + compUtils(analValX, numValX));
            System.out.println("dPropYdPrecZ: " + compUtils(analValY, numValY));
            System.out.println("dPropZdPrecZ: " + compUtils(analValZ, numValZ));

            Assert.assertEquals(expectedVal[0], analValX, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[0], numValX, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedVal[1], analValY, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[1], numValY, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedVal[2], analValZ, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal[2], numValZ, VacuumSignalPropagationDPTest.validityThreshold);
        }
    }

    /**
     * Evaluate the propagation time/duration derivatives wrt the emitter position computation:
     * finite difference method.
     * <p>
     * <ul>
     * <li>Display the results: absolute and relative differences</li>
     * <li>Non regression validation test wrt reference values</li>
     * </ul>
     * </p>
     *
     * @param pvEm
     *        Emitter PV
     * @param pvRec
     *        Receiver PV
     * @param hEm
     *        Emitter PV shift
     * @param fixedDate
     *        Fixed date of computation
     * @param analysisFrame
     *        Working frame for the analysis (must be inertial)
     * @param expectedVal
     *        Expected analytic (derivative) value (for non-regression evaluation)
     * @param expectedRefVal
     *        Expected numeric (finite difference) value (for non-regression evaluation)
     * @throws PatriusException
     *         if a problem occurs during PVCoodinates providers manipulations
     */
    private static void dTpropdPemValidation(final PVCoordinatesProvider pvEm,
                                             final PVCoordinatesProvider pvRec, final PVCoordinates hEm,
                                             final FixedDate fixedDate,
                                             final Frame analysisFrame, final double expectedVal,
                                             final double expectedRefVal)
        throws PatriusException {

        final double threshold = 1e-13;
        final VacuumSignalPropagationModel model =
            new VacuumSignalPropagationModel(analysisFrame, threshold, VacuumSignalPropagationModel.DEFAULT_MAX_ITER);

        // Apply the PV shift on the emitter to generate pvEmPlusHEm & pvEmMinusHEm
        final PVCoordinatesProvider pvEmPlusHEm = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = -1618681345604951082L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                final PVCoordinates pvEmRef = new PVCoordinates(1., pvEm.getPVCoordinates(date, analysisFrame), 1., hEm);
                final Transform t = analysisFrame.getTransformTo(frame, date);
                return t.transformPVCoordinates(pvEmRef);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        final PVCoordinatesProvider pvEmMinusHEm = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 2151902112412385160L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                final PVCoordinates pvEmRef = new PVCoordinates(1., pvEm.getPVCoordinates(date, analysisFrame), -1.,
                    hEm);
                final Transform t = analysisFrame.getTransformTo(frame, date);
                return t.transformPVCoordinates(pvEmRef);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        // Compute the signal and the shifted signals
        final VacuumSignalPropagation signal = model.computeSignalPropagation(pvEm, pvRec,
            VacuumSignalPropagationDPTest.date, fixedDate);
        final VacuumSignalPropagation signalPlusHEm = model.computeSignalPropagation(pvEmPlusHEm, pvRec,
            VacuumSignalPropagationDPTest.date, fixedDate);
        final VacuumSignalPropagation signalMinusHEm = model.computeSignalPropagation(pvEmMinusHEm,
            pvRec, VacuumSignalPropagationDPTest.date, fixedDate);

        // Extract the shifted signals propagation time/duration and compute the finite difference
        final double propDurationPlusHEm = signalPlusHEm.getSignalPropagationDuration();
        final double propDurationMinusHEm = signalMinusHEm.getSignalPropagationDuration();
        final double valTheo = propDurationPlusHEm - propDurationMinusHEm;

        // Extract the signal's propagation time/duration derivatives wrt the emitter position
        final Vector3D dTpropdPem = signal.getdTpropdPem();

        // Evaluate the results (reporting & validation) for each axis
        final Vector3D hEmPos = hEm.getPosition();
        if (hEmPos.getX() != 0.) {
            final double numVal = valTheo / (2 * hEmPos.getX());
            final double analVal = dTpropdPem.getX();
            System.out.println("dTpropdPemdX: " + compUtils(analVal, numVal));

            Assert.assertEquals(expectedVal, analVal, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal, numVal, VacuumSignalPropagationDPTest.validityThreshold);
        }
        if (hEmPos.getY() != 0.) {
            final double numVal = valTheo / (2 * hEmPos.getY());
            final double analVal = dTpropdPem.getY();
            System.out.println("dTpropdPemdY: " + compUtils(analVal, numVal));

            Assert.assertEquals(expectedVal, analVal, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal, numVal, VacuumSignalPropagationDPTest.validityThreshold);
        }
        if (hEmPos.getZ() != 0.) {
            final double numVal = valTheo / (2 * hEmPos.getZ());
            final double analVal = dTpropdPem.getZ();
            System.out.println("dTpropdPemdZ: " + compUtils(analVal, numVal));

            Assert.assertEquals(expectedVal, analVal, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal, numVal, VacuumSignalPropagationDPTest.validityThreshold);
        }
    }

    /**
     * Evaluate the propagation time/duration derivatives wrt the receiver position computation:
     * finite difference method.
     * <p>
     * <ul>
     * <li>Display the results: absolute and relative differences</li>
     * <li>Non regression validation test wrt reference values</li>
     * </ul>
     * </p>
     *
     * @param pvEm
     *        Emitter PV
     * @param pvRec
     *        Receiver PV
     * @param hRec
     *        Receiver PV shift
     * @param fixedDate
     *        Fixed date of computation
     * @param analysisFrame
     *        Working frame for the analysis (must be inertial)
     * @param expectedVal
     *        Expected analytic (derivative) value (for non-regression evaluation)
     * @param expectedRefVal
     *        Expected numeric (finite difference) value (for non-regression evaluation)
     * @throws PatriusException
     *         if a problem occurs during PVCoodinates providers manipulations
     */
    private static void dTpropdPrecValidation(final PVCoordinatesProvider pvEm,
                                              final PVCoordinatesProvider pvRec, final PVCoordinates hRec,
                                              final FixedDate fixedDate, final Frame analysisFrame,
                                              final double expectedVal, final double expectedRefVal)
        throws PatriusException {

        final double threshold = 1e-13;
        final VacuumSignalPropagationModel model =
            new VacuumSignalPropagationModel(analysisFrame, threshold, VacuumSignalPropagationModel.DEFAULT_MAX_ITER);

        // Apply the PV shift on the receiver to generate pvRecPlusHRec & pvRecMinusHRec
        final PVCoordinatesProvider pvRecPlusHRec = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 3949297937228642023L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                final PVCoordinates pvRecRef = new PVCoordinates(1., pvRec.getPVCoordinates(date, analysisFrame), 1.,
                    hRec);
                final Transform t = analysisFrame.getTransformTo(frame, date);
                return t.transformPVCoordinates(pvRecRef);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        final PVCoordinatesProvider pvRecMinusHRec = new PVCoordinatesProvider(){
            /** Serializable UID. */
            private static final long serialVersionUID = 7812092265618574590L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                final PVCoordinates pvRecRef = new PVCoordinates(1., pvRec.getPVCoordinates(date, analysisFrame), -1.,
                    hRec);
                final Transform t = analysisFrame.getTransformTo(frame, date);
                return t.transformPVCoordinates(pvRecRef);
            }

            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                throw new PatriusException(PatriusMessages.INTERNAL_ERROR);
            }
        };

        // Compute the signal and the shifted signals
        final VacuumSignalPropagation signal = model.computeSignalPropagation(pvEm, pvRec,
            VacuumSignalPropagationDPTest.date, fixedDate);
        final VacuumSignalPropagation signalPlusHRec = model.computeSignalPropagation(pvEm,
            pvRecPlusHRec, VacuumSignalPropagationDPTest.date, fixedDate);
        final VacuumSignalPropagation signalMinusHRec = model.computeSignalPropagation(pvEm,
            pvRecMinusHRec, VacuumSignalPropagationDPTest.date, fixedDate);

        // Extract the shifted signals propagation time/duration and compute the finite difference
        final double propDurationPlusHRec = signalPlusHRec.getSignalPropagationDuration();
        final double propDurationMinusHRec = signalMinusHRec.getSignalPropagationDuration();
        final double valTheo = propDurationPlusHRec - propDurationMinusHRec;

        // Extract the signal's propagation time/duration derivatives wrt the receiver position
        final Vector3D dTpropdPrec = signal.getdTpropdPrec();

        // Evaluate the results (reporting & validation) for each axis
        final Vector3D hRecPos = hRec.getPosition();
        if (hRecPos.getX() != 0.) {
            final double numVal = valTheo / (2 * hRecPos.getX());
            final double analVal = dTpropdPrec.getX();
            System.out.println("dTpropdPrecdX: " + compUtils(analVal, numVal));

            Assert.assertEquals(expectedVal, analVal, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal, numVal, VacuumSignalPropagationDPTest.validityThreshold);
        }
        if (hRecPos.getY() != 0.) {
            final double numVal = valTheo / (2 * hRecPos.getY());
            final double analVal = dTpropdPrec.getY();
            System.out.println("dTpropdPrecdY: " + compUtils(analVal, numVal));

            Assert.assertEquals(expectedVal, analVal, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal, numVal, VacuumSignalPropagationDPTest.validityThreshold);
        }
        if (hRecPos.getZ() != 0.) {
            final double numVal = valTheo / (2 * hRecPos.getZ());
            final double analVal = dTpropdPrec.getZ();
            System.out.println("dTpropdPrecdZ: " + compUtils(analVal, numVal));

            Assert.assertEquals(expectedVal, analVal, VacuumSignalPropagationDPTest.validityThreshold);
            Assert.assertEquals(expectedRefVal, numVal, VacuumSignalPropagationDPTest.validityThreshold);
        }
    }

    /**
     * Evaluate the propagation time/duration derivatives wrt time computation: finite difference method.
     * <p>
     * <ul>
     * <li>Display the results: absolute and relative differences</li>
     * <li>Non regression validation test wrt reference values</li>
     * </ul>
     * </p>
     *
     * @param pvEm
     *        Emitter PV
     * @param pvRec
     *        Receiver PV
     * @param hT
     *        Time duration shift
     * @param fixedDate
     *        Fixed date of computation
     * @param analysisFrame
     *        Working frame for the analysis (must be inertial)
     * @param expectedVal
     *        Expected analytic (derivative) value (for non-regression evaluation)
     * @param expectedRefVal
     *        Expected numeric (finite difference) value (for non-regression evaluation)
     * @throws PatriusException
     *         if a problem occurs during PVCoodinates providers manipulations
     */
    private static void dTpropdTValidation(final PVCoordinatesProvider pvEm,
                                           final PVCoordinatesProvider pvRec, final double hT,
                                           final FixedDate fixedDate, final Frame analysisFrame,
                                           final double expectedVal, final double expectedRefVal)
        throws PatriusException {

        final double threshold = 1e-13;
        final VacuumSignalPropagationModel model =
            new VacuumSignalPropagationModel(analysisFrame, threshold, VacuumSignalPropagationModel.DEFAULT_MAX_ITER);

        // Compute the signal and the shifted signals
        final VacuumSignalPropagation signal = model.computeSignalPropagation(pvEm, pvRec,
            VacuumSignalPropagationDPTest.date, fixedDate);
        final VacuumSignalPropagation signalPlusHT = model.computeSignalPropagation(pvEm, pvRec,
            VacuumSignalPropagationDPTest.date.shiftedBy(hT), fixedDate);
        final VacuumSignalPropagation signalMinusHT = model.computeSignalPropagation(pvEm, pvRec,
            VacuumSignalPropagationDPTest.date.shiftedBy(-hT), fixedDate);

        // Extract the shifted signals propagation time/duration and compute the finite difference
        final double propDurationPlusHT = signalPlusHT.getSignalPropagationDuration();
        final double propDurationMinusHT = signalMinusHT.getSignalPropagationDuration();
        final double valTheo = propDurationPlusHT - propDurationMinusHT;
        final double numVal = valTheo / (2 * hT);

        // Extract the signal's propagation time/duration derivatives wrt time
        final double analVal = signal.getdTpropdT();

        // Evaluate the results (reporting & validation)
        System.out.println("\n" + "fixedDate: " + fixedDate);
        System.out.println("dTpropdT: " + compUtils(analVal, numVal));

        Assert.assertEquals(expectedVal, analVal, VacuumSignalPropagationDPTest.validityThreshold);
        Assert.assertEquals(expectedRefVal, numVal, VacuumSignalPropagationDPTest.validityThreshold);
    }

    /**
     * Evaluate the propagation vector derivatives wrt time computation: finite difference method.
     * <p>
     * <ul>
     * <li>Display the results: absolute and relative differences on each axis</li>
     * <li>Non regression validation test wrt reference values</li>
     * </ul>
     * </p>
     *
     * @param pvEm
     *        Emitter PV
     * @param pvRec
     *        Receiver PV
     * @param hT
     *        Time duration shift
     * @param fixedDate
     *        Fixed date of computation
     * @param analysisFrame
     *        Working frame for the analysis (must be inertial)
     * @param expectedVal
     *        Expected analytic (derivative) values (for non-regression evaluation)
     * @param expectedRefVal
     *        Expected numeric (finite difference) values (for non-regression evaluation)
     * @throws PatriusException
     *         if a problem occurs during PVCoodinates providers manipulations
     */
    private static void dPropdTValidation(final PVCoordinatesProvider pvEm, final PVCoordinatesProvider pvRec,
                                          final double hT, final FixedDate fixedDate, final Frame analysisFrame,
                                          final double[] expectedVal, final double[] expectedRefVal)
        throws PatriusException {

        final double threshold = 1e-13;
        final VacuumSignalPropagationModel model =
            new VacuumSignalPropagationModel(analysisFrame, threshold, VacuumSignalPropagationModel.DEFAULT_MAX_ITER);

        // Compute the signal and the shifted signals
        final VacuumSignalPropagation signal = model.computeSignalPropagation(pvEm, pvRec,
            VacuumSignalPropagationDPTest.date, fixedDate);
        final VacuumSignalPropagation signalPlusHT = model.computeSignalPropagation(pvEm, pvRec,
            VacuumSignalPropagationDPTest.date.shiftedBy(hT), fixedDate);
        final VacuumSignalPropagation signalMinusHT = model.computeSignalPropagation(pvEm, pvRec,
            VacuumSignalPropagationDPTest.date.shiftedBy(-hT), fixedDate);

        // Extract the shifted signals propagation vector and compute the finite difference
        final Vector3D propPlusHT = signalPlusHT.getVector();
        final Vector3D propMinusHT = signalMinusHT.getVector();
        final Vector3D numValVector = propPlusHT.add(-1, propMinusHT).scalarMultiply(1 / (2 * hT));

        // Extract the signal's propagation vector derivatives wrt time
        final Vector3D analValVector = signal.getdPropdT();

        // Evaluate the results (reporting & validation) for each axis
        System.out.println("\n" + "fixedDate: " + fixedDate);

        System.out.println("dPpropXdT: " + compUtils(analValVector.getX(), numValVector.getX()));
        System.out.println("dPpropYdT: " + compUtils(analValVector.getY(), numValVector.getY()));
        System.out.println("dPpropZdT: " + compUtils(analValVector.getZ(), numValVector.getZ()));

        Assert.assertEquals(expectedVal[0], analValVector.getX(), VacuumSignalPropagationDPTest.validityThreshold);
        Assert.assertEquals(expectedRefVal[0], numValVector.getX(), VacuumSignalPropagationDPTest.validityThreshold);

        Assert.assertEquals(expectedVal[1], analValVector.getY(), VacuumSignalPropagationDPTest.validityThreshold);
        Assert.assertEquals(expectedRefVal[1], numValVector.getY(), VacuumSignalPropagationDPTest.validityThreshold);

        Assert.assertEquals(expectedVal[2], analValVector.getZ(), VacuumSignalPropagationDPTest.validityThreshold);
        Assert.assertEquals(expectedRefVal[2], numValVector.getZ(), VacuumSignalPropagationDPTest.validityThreshold);
    }

    /** Before the tests. */
    @BeforeClass
    public static void setUpBeforeClass() {
        try {
            Utils.setDataRoot("regular-dataCNES-2003");
            FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }
        date = AbsoluteDate.J2000_EPOCH;
        validityThreshold = 1e-16;
    }

    /**
     * Return a String formated text with the "val", "refVal", "absolute difference" and "relative difference"
     * information.
     *
     * @param val
     *        evaluated value
     * @param refVal
     *        reference value
     * @return formated text
     */
    private static String compUtils(final double val, final double refVal) {
        // Keep to display non regression values
        // return (val + ", " + refVal);
        // Use to display formatted results
        return String.format("(val:%g)\t(valRef:%g)\t(diff:%g)\t(diffRel:%g)", val, refVal,
            (val - refVal), Math.abs(val - refVal) / refVal);
    }
}
