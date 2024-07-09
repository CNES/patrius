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
 * @history creation 15/10/2015
 *
 * HISTORY
* VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
* VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:457:15/10/2015:added Glint pointing direction
 * VERSION::FA:560:03/03/2015:validation of Glint direction
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::DM:1950:11/12/2018:move guidance package to attitudes.profiles
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.directions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.CNESUtils;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AbstractAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudesSequence;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.attitudes.directions.GlintApproximatePointingDirection;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.ExtendedOneAxisEllipsoid;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.analysis.solver.BisectionSolver;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.EphemerisPvHermite;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.PVCoordinatesPropagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AbstractDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector.Action;
import fr.cnes.sirius.patrius.propagation.events.NadirSolarIncidenceDetector;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Validation test for GlintApproximatePointingDirection.
 * <p>
 * Validation threshold is 4E-5 since Sun ephemeris model is different between PATRIUS (Meeus model) and PCGE reference
 * (Newcomb model).
 * </p>
 * 
 * @author rodriguest
 * 
 * @version $Id$
 * 
 * @since 3.1
 * 
 */

public class GlintApproximatePointingDirectionValTest {

    /** Earth name */
    private final String name = "earth";
    /** EME 2000 */
    private final Frame frame = FramesFactory.getEME2000();
    /** Equatorial radius constant */
    private final double equatorialRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
    /** Flatness constant */
    private final double flatness = Constants.WGS84_EARTH_FLATTENING;
    /** Gravitational constant */
    private final double mu = Constants.WGS84_EARTH_MU;
    /** flatten earth shape */
    private EllipsoidBodyShape earthShape;
    /** sun ephemeris */
    private CelestialBody sun;
    /** epsilon comparison */
    private final double epsilon = 4E-5;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Attitude Law Target Direction
         * 
         * @featureDescription Validation test of the X sat orientation relative to a reference ephemeris file
         * 
         */
        GLINT_ATTITUDE_LAW
    }

    /**
     * @throws PatriusException
     *         should not happen
     * @throws IOException
     *         should not happen
     * @testType VT
     * 
     * @testedMethod {@link GlintApproximatePointingDirection#getTargetPVCoordinates(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @testedFeature {@link features#GLINT_ATTITUDE_LAW}
     * 
     * @description Compute quaternion over an orbit ephemeris with an attitude law built using Lofoffset law and Glint
     *              direction.
     * 
     * @input PVCoordinates reference ephemeris used to build a PVCoordinatePropagator with an EphemerisPVHermite.
     * @input earth, and sun ephemeris. Instantiate in setup.
     * @input earthshape as a an extedendOneAxisEllispoid to compute eclipse.
     * 
     * @output Spacecraftstate ephemeris
     * 
     * @testPassCriteria output quaternion is the same as PCGE reference quaternion on the all ephemeris (threshold:
     *                   4E-5).
     *                   Validation threshold is 4E-5 since Sun ephemeris model is different between PATRIUS (Meeus
     *                   model) and PCGE reference (Newcomb model).
     * 
     * @referenceVersion 3.1
     */

    @Test
    public void glintPositionValTest() throws IOException, PatriusException {

        final String rootResource = "glint-data/";
        // Read reference ephemeris
        final URL url1 = GlintApproximatePointingDirectionValTest.class.getClassLoader().getResource(
            rootResource + "Spacecraft_position.txt");
        final URL url3 = GlintApproximatePointingDirectionValTest.class.getClassLoader().getResource(
            rootResource + "Attitude_val.txt");
        final MatrixFileReader mat1 = new MatrixFileReader(url1.getPath());
        final MatrixFileReader mat3 = new MatrixFileReader(url3.getPath());

        final double[][] dataPV = mat1.getData();
        final double[][] dataVal = mat3.getData();
        final int nbLines = mat1.getData().length;

        // Fill a table of spacecraft PVCoordinates, a table of dates, and a table of reference attitude
        final PVCoordinates[] spacecraftPvCoord = new PVCoordinates[nbLines];
        final AbsoluteDate[] dates = new AbsoluteDate[nbLines];
        final Rotation[] referenceRot = new Rotation[nbLines];

        for (int i = 0; i < nbLines; i++) {
            spacecraftPvCoord[i] = new PVCoordinates(
                new Vector3D(dataPV[i][2], dataPV[i][3], dataPV[i][4]).scalarMultiply(1E3),
                new Vector3D(dataPV[i][5], dataPV[i][6], dataPV[i][7]).scalarMultiply(1E3));
            final AbsoluteDate dateTT = AbsoluteDate.MODIFIED_JULIAN_EPOCH.shiftedBy(dataPV[i][0]
                * Constants.JULIAN_DAY + dataPV[i][1]);
            dates[i] = dateTT.shiftedBy(-33.01 - 32.184);
            referenceRot[i] = new Rotation(true, dataVal[i][2], dataVal[i][3], dataVal[i][4], dataVal[i][5]);
        }

        // build a EphemerisPVHermite from these tables
        final EphemerisPvHermite ephPvHermite =
            new EphemerisPvHermite(spacecraftPvCoord, null, this.frame, dates, null);
        final PVCoordinatesPropagator propagator = new PVCoordinatesPropagator(ephPvHermite, ephPvHermite.getMinDate(),
            this.mu, this.frame);

        // build a TNW align attitude law with X earth pointed
        final LofOffset tnwLaw = new LofOffset(LOFType.TNW, RotationOrder.XYZ, -FastMath.PI / 2, -FastMath.PI / 2, 0);

        final MyAttitudeLaw glintAttLaw = new MyAttitudeLaw();

        // use a nadir solar incidence to switch from glint pointing to body center pointing
        final NadirSolarIncidenceDetector nadirSolarDetect = new NadirSolarIncidenceDetector(MathLib.toRadians(90),
            this.earthShape,
            AbstractDetector.DEFAULT_MAXCHECK, AbstractDetector.DEFAULT_THRESHOLD, Action.RESET_STATE);

        // build an attitude law point glint out of eclipse and earth center during eclipse
        final AttitudesSequence attLaw = new AttitudesSequence();
        attLaw.addSwitchingCondition(glintAttLaw, nadirSolarDetect, true, false, tnwLaw);
        attLaw.addSwitchingCondition(tnwLaw, nadirSolarDetect, false, true, glintAttLaw);
        attLaw.resetActiveProvider(tnwLaw);
        propagator.setAttitudeProvider(attLaw);
        attLaw.registerSwitchEvents(propagator);

        // For each line of the input reference ephemeris file, compute the attitude and check the angular deviation
        for (int j = 2; j < nbLines - 2; j++) {
            // Propagation
            final SpacecraftState ss = propagator.propagate(dates[j - 1], dates[j]);

            // Get attitude (actual and expected)
            final Rotation actual = ss.getAttitude().getRotation();
            final Rotation expected = referenceRot[j];

            // Retrieve position (for quick check) and satellite pointing direction (toward Glint)
            final Vector3D posSatExpected = spacecraftPvCoord[j].getPosition();
            final Vector3D posSatActual = ss.getPVCoordinates().getPosition();

            // Compute the day / night limit of the nadir point to filter differences due to slew in CNES reference
            // spacecraft position in earth frame
            final Vector3D satPos = ss.getPVCoordinates(this.earthShape.getBodyFrame()).getPosition();

            // nadir point
            final GeodeticPoint satGeo =
                this.earthShape.transform(satPos, this.earthShape.getBodyFrame(), ss.getDate());
            final GeodeticPoint nadirGeo = new GeodeticPoint(satGeo.getLatitude(), satGeo.getLongitude(), 0.0);
            final Vector3D nadirPos = this.earthShape.transform(nadirGeo);

            // zenith vector from this nadir point
            final Vector3D zenithVect = satPos.subtract(nadirPos);

            // vector : nadir point to sun
            final Vector3D sunPos =
                this.sun.getPVCoordinates(ss.getDate(), this.earthShape.getBodyFrame()).getPosition();
            final Vector3D nadirToSun = sunPos.subtract(nadirPos);

            // incidence angle
            final double sunElev = Vector3D.angle(nadirToSun, zenithVect);
            final double nightSunLimit = MathLib.toRadians(85);

            // Check (performed only when satellite is pointing Glint)
            if (sunElev < nightSunLimit) {
                Assert.assertEquals(0., Vector3D.distance(posSatExpected, posSatActual),
                    Precision.DOUBLE_COMPARISON_EPSILON);
                Assert.assertEquals(0., Rotation.distance(actual, expected), this.epsilon);
            }
        }
    }

    /**
     * Attitude law built for validation purpose.
     * 
     * @author Emmanuel Bignon
     */
    private class MyAttitudeLaw extends AbstractAttitudeLaw {

        /** Glint direction. */
        private final GlintApproximatePointingDirection glint;

        /** Intermediate attitude law: offset from LOF. */
        private final LofOffset intermediateLaw;// = new LofOffset(LOFType.LVLH, RotationOrder.YZX, -FastMath.PI / 2, 0,
                                                // 0);

        /**
         * Constructor
         */
        public MyAttitudeLaw() {
            // Glint direction
            final BisectionSolver solver = new BisectionSolver(1.0E-15, 1.0E-15);
            this.glint =
                new GlintApproximatePointingDirection(GlintApproximatePointingDirectionValTest.this.earthShape,
                    GlintApproximatePointingDirectionValTest.this.sun, solver);

            // Build inermediate frame
            final Rotation rotation = new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_K, Vector3D.PLUS_K, Vector3D.MINUS_I);
            final double[] angles = rotation.getAngles(RotationOrder.YZX);
            this.intermediateLaw = new LofOffset(LOFType.LVLH, RotationOrder.YZX, angles[0], angles[1], angles[2]);
        }

        /** {@inheritDoc} */
        @Override
        public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                    final Frame frame) throws PatriusException {
            // Atittude law: composition of intermediate attitude and rotation from glint to xi

            // Intermediate law
            final Attitude intermediateAttitude = this.intermediateLaw.getAttitude(pvProv, date, frame);

            // Rotation from xi to glint
            final Vector3D glintDirection = this.glint.getVector(pvProv, date, frame);

            // final Vector3D xi = pvProv.getPVCoordinates(date, frame).getPosition().negate();
            final Vector3D xi = intermediateAttitude.getRotation().applyTo(Vector3D.PLUS_I);
            final Rotation rotation = new Rotation(xi, glintDirection);

            // Composition
            final Rotation globalRotation = rotation.applyTo(intermediateAttitude.getRotation());
            return new Attitude(date, frame, new AngularCoordinates(globalRotation, Vector3D.ZERO));
        }
    }

    /**
     * Set up
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        CNESUtils.clearNewFactoriesAndCallSetDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final JPLEphemeridesLoader loaderSun = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SUN);
        this.sun = loaderSun.loadCelestialBody(CelestialBodyFactory.SUN);
        this.sun = new MeeusSun();

        this.earthShape = new ExtendedOneAxisEllipsoid(this.equatorialRadius, this.flatness,
            FramesFactory.getITRF(), this.name);
    }

    /**
     * Utility class to store data from a txt file in matrices
     */
    private class MatrixFileReader {
        private final String filePath;

        private double[][] dataTransposed;
        private double[][] data;

        /**
         * Lecture d'un fichier texte contenant une matrice
         * (Commentaire possibles dans le fichier pour les lignes commence par #)
         * 
         * @param myFilePath
         *        path to the file
         * @throws IOException
         *         should not happens
         */
        public MatrixFileReader(final String myFilePath) throws IOException {
            super();
            this.filePath = myFilePath;
            this.parseFile();
        }

        /**
         * Renvoie la matrice contenue dans le fichier
         * data[0] contient la premiere ligne du fichier
         * data[1] contient la deuxieme ligne du fichier
         * etc...
         * 
         * @return data
         */
        public double[][] getData() {
            return this.data;
        }

        private void parseFile() throws IOException {
            final BufferedReader reader = new BufferedReader(new FileReader(this.filePath));
            String line = null;
            final List<String> items = new ArrayList<String>();
            StringTokenizer splitter;
            while ((line = reader.readLine()) != null) {
                // Les lignes qui démarrent avec # ou qui ne contiennent rien ou que des espaces
                // sont ignorées.
                if (!line.startsWith("#") && !line.trim().isEmpty()) {
                    items.add(line);
                }
            }

            final int nbLig = items.size();
            int nbCol = 0;

            if (nbLig == 0) {
                this.dataTransposed = new double[nbCol][nbLig];
            }
            else {

                final String firstLigne = items.get(0);
                splitter = new StringTokenizer(firstLigne, " ");

                nbCol = splitter.countTokens();

                this.dataTransposed = new double[nbCol][nbLig];
                int counter = 0;
                for (final String item : items) {
                    splitter = new StringTokenizer(item, " ");
                    for (int i = 0; i < nbCol; i++) {
                        // Attention: Double.parseDouble ne fonctionne pas si
                        // les nombres sont écrits avec une virgule comme séparateur !!
                        this.dataTransposed[i][counter] = Double.parseDouble((String) splitter.nextElement());
                    }
                    counter++;
                }
            }

            this.data = new double[nbLig][nbCol];
            for (int i = 0; i < nbLig; i++) {
                for (int j = 0; j < nbCol; j++) {
                    this.data[i][j] = this.dataTransposed[j][i];
                }
            }
            reader.close();
        }
    }
}
