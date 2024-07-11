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
 * @history creation 05/10/2011
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:837:03/03/2017: Suppression of class JPLEphemerisLoader in orekit-addons
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.bodies;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.JPLEphemeridesLoader;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.tools.validationTool.Validate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class aims at validating the ephemerides of Orekit (DE405 and DE406)
 * 
 * References : code coming from JPL (cf JPLEphemerisTest)
 * 
 * The accuracy of the ephemerides of the Sun and the moon is controlled on 19 years.
 * 
 */
public class JPLEphemerisValTest {

    /** validation tool */
    static Validate validate;

    /** EME2000 frame. */
    static Frame gcrf;

    /** ICRF frame. */
    static Frame icrf405;

    /** ICRF frame. */
    static Frame icrf406;

    /** Reader for JPL Moon reference ephemeris . */
    static BufferedReader readerMoon;

    /** Readers for JPL Sun reference ephemeris. */
    static BufferedReader readerSun1;
    static BufferedReader readerSun2;

    /** Folder to JPL reference ephemeris. */
    private static final String jplFolder = "jpl_ephem_data/";

    /** Moon and Sun ephemeris files. */
    // Moon
    private static final String ephemMoon = jplFolder + "planet_10_pos_vel.txt";

    // Sun
    private static final String ephemSun1 = jplFolder + "planet_3_pos_vel.txt";
    private static final String ephemSun2 = jplFolder + "planet_11_pos_vel.txt";

    /** Non regression epsilon. */
    private final double epsilon = 1E-12;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Sun ephemeris
         * 
         * @featureDescription DE406 ephemeris (JPL ephemeris)
         * 
         * @coveredRequirements DV-MOD_460, DV-MOD_430
         */
        BODIES_EPHEMERIS,
        /**
         * @featureTitle Moon ephemeris
         * 
         * @featureDescription DE405 ephemeris (JPL ephemeris)
         * 
         * @coveredRequirements DV-MOD_460
         */
        DE405_MOON_EPHEMERIS,

        /**
         * @featureTitle Sun ephemeris
         * 
         * @featureDescription DE405 ephemeris (JPL ephemeris)
         * 
         * @coveredRequirements DV-MOD_460
         */
        DE405_SUN_EPHEMERIS,
        /**
         * @featureTitle Moon ephemeris
         * 
         * @featureDescription DE406 ephemeris (JPL ephemeris)
         * 
         * @coveredRequirements DV-MOD_460
         */
        DE406_MOON_EPHEMERIS,

        /**
         * @featureTitle Sun ephemeris
         * 
         * @featureDescription DE406 ephemeris (JPL ephemeris)
         * 
         * @coveredRequirements DV-MOD_460
         */
        DE406_SUN_EPHEMERIS,

        /**
         * @featureTitle Gravitational constant
         * 
         * @featureDescription gravitational constant
         * 
         * @coveredRequirements DV-MOD_460
         */
        GM,
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#DE405_MOON_EPHEMERIS}
     * 
     * @testedMethod {@link CelestialBody#getPVCoordinates(AbsoluteDate, Frame)}
     * @testedMethod {@link JPLEphemeridesLoader#loadCelestialBody(String)}
     * 
     * @description we compare the ephemeris of the Moon given by Orekit with those given by the JPL. The comparison
     *              is done on 19 years starting from J2000 epoch, for each day, we perform 2 comparisons. We compare
     *              the norms of
     *              the position vectors and the norms of the velocity vectors.
     * 
     * @input Moon ephemeris given by the JPL
     * 
     * @output Moon ephemeris
     * 
     * @testPassCriteria the difference between the position norms should be lower than 2 m and the difference between
     *                   the velocity norms should be lower than 1E-5 m/s.
     * 
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     *         if the header constants cannot be read
     * @throws IOException
     */
    @Test
    public void validateMoonEphemerisWrtJPLReferenceFile405() throws PatriusException, IOException {

        // J2000 date with TT time scale
        AbsoluteDate date = new AbsoluteDate();

        // deviation on the position
        double deviation_position = 0;
        double maxDeviation_position = 0;
        final double mediumDeviation_position;
        double sumDeviations_position = 0;
        // deviation on the velocity
        double deviation_velocity = 0;
        double maxDeviation_velocity = 0;
        final double mediumDeviation_velocity;
        double sumDeviations_velocity = 0;

        // execution times
        long t0;
        long t1;
        long timeJPL = 0;
        long timeOREKIT = 0;

        // 2 points per day for 19 years
        final long steps = 19 * 365 * 2;

        // Initialisation of the date
        final double elapsedTimeJ2000_JULIAN_EPOCH = date.durationFrom(AbsoluteDate.JULIAN_EPOCH);
        double julianDays = elapsedTimeJ2000_JULIAN_EPOCH / 86400.0;

        // PV ephemeris
        final double[] ephemeris_r = new double[3];
        final double[] ephemeris_rprime = new double[3];

        /*
         * Initialisation of the OREKIT ephemeris
         */
        final JPLEphemeridesLoader loader = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.MOON);

        final CelestialBody moon = loader.loadCelestialBody(CelestialBodyFactory.MOON);

        // Sets of coordinates which will be compared
        PVCoordinates pvJPL;
        PVCoordinates pvOREKIT;
        Vector3D positionJPL;
        Vector3D positionOREKIT;
        Vector3D velocityJPL;
        Vector3D velocityOREKIT;

        /*
         * Loop on the date
         */
        String[] line;
        for (int j = 0; j < steps; j++) {

            // JPL ephemeris
            t0 = System.currentTimeMillis();
            line = readerMoon.readLine().split(" ");

            ephemeris_r[0] = Double.parseDouble(line[1]);
            ephemeris_r[1] = Double.parseDouble(line[2]);
            ephemeris_r[2] = Double.parseDouble(line[3]);

            ephemeris_rprime[0] = Double.parseDouble(line[4]);
            ephemeris_rprime[1] = Double.parseDouble(line[5]);
            ephemeris_rprime[2] = Double.parseDouble(line[6]);

            t1 = System.currentTimeMillis();

            timeJPL += t1 - t0;

            pvJPL = new PVCoordinates(new Vector3D(ephemeris_r[0] * Constants.JPL_SSD_ASTRONOMICAL_UNIT,
                ephemeris_r[1] * Constants.JPL_SSD_ASTRONOMICAL_UNIT, ephemeris_r[2]
                    * Constants.JPL_SSD_ASTRONOMICAL_UNIT),
                new Vector3D(ephemeris_rprime[0] * Constants.JPL_SSD_ASTRONOMICAL_UNIT / 86400,
                    ephemeris_rprime[1] * Constants.JPL_SSD_ASTRONOMICAL_UNIT / 86400,
                    ephemeris_rprime[2] * Constants.JPL_SSD_ASTRONOMICAL_UNIT / 86400));

            // OREKIT ephemeris
            t0 = System.currentTimeMillis();
            pvOREKIT = moon.getPVCoordinates(date, gcrf);
            t1 = System.currentTimeMillis();

            timeOREKIT += t1 - t0;

            // Comparison between JPL and OREKIT
            positionJPL = pvJPL.getPosition();
            positionOREKIT = pvOREKIT.getPosition();
            velocityJPL = pvJPL.getVelocity();
            velocityOREKIT = pvOREKIT.getVelocity();

            // deviation on the position
            deviation_position = positionJPL.subtract(positionOREKIT).getNorm();
            if (deviation_position > maxDeviation_position) {
                maxDeviation_position = deviation_position;
            }
            sumDeviations_position += deviation_position;

            // deviation on the velocity
            deviation_velocity = velocityJPL.subtract(velocityOREKIT).getNorm();
            if (deviation_velocity > maxDeviation_velocity) {
                maxDeviation_velocity = deviation_velocity;
            }
            sumDeviations_velocity += deviation_velocity;

            // half a day time-lag
            date = date.shiftedBy(43200.0);
            julianDays = julianDays + 0.5;
        }

        // average deviation on the position
        mediumDeviation_position = sumDeviations_position / steps;
        // average deviation on the velocity
        mediumDeviation_velocity = sumDeviations_velocity / steps;

        System.out.println("** MOON EPHEMERIS COMPARISON **");
        System.out.println();
        System.out.println("Max deviation on the position (m)  = " + maxDeviation_position);
        System.out.println("Medium deviation on the position  (m) = " + mediumDeviation_position);
        System.out.println("Max deviation on the velocity (m/s)  = " + maxDeviation_velocity);
        System.out.println("Medium deviation on the velocity (m/s) = " + mediumDeviation_velocity);
        System.out.println("JPL calculation took " + timeJPL + " ms");
        System.out.println("OREKIT calculation took " + timeOREKIT + " ms");
        System.out.println();

        Assert.assertTrue("The max deviation in position is above " + 2.0 + " m !!", maxDeviation_position < 2.0);
        Assert.assertTrue("The max deviation in velocity is above " + 1E-5 + " m/s !!", maxDeviation_velocity < 1E-5);

        // Close stream
        readerMoon.close();

    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#DE405_SUN_EPHEMERIS}
     * 
     * @testedMethod {@link CelestialBody#getPVCoordinates(AbsoluteDate, Frame)}
     * @testedMethod {@link JPLEphemeridesLoader#loadCelestialBody(String)}
     * 
     * @description We compare the ephemeris of the Sun given by Orekit with those given by the JPL. The comparison
     *              is done on 19 years starting from J2000 epoch, for each day, we perform 2 comparisons. We compare
     *              the norms
     *              of the position vectors and the norms of the velocity vectors in the EME2000 frame and in the ICRF.
     * 
     * @input Sun ephemeris given by the JPL
     * 
     * @output Sun ephemeris
     * 
     * @testPassCriteria In the EME2000, the difference between the position norms should be lower than 60 m and the
     *                   difference between the velocity norms should be lower than 1E-3 m/s. In the ICRF, the
     *                   difference between the
     *                   position norms should be lower than 10 cm and the difference between the velocity norms should
     *                   be lower than
     *                   1E-9 m/s.
     * 
     * @see JPLEphemerisValTest
     * 
     * @comments Given that the ephemeris of the Sun DE405 is given in the ICRF, we expect to get better result by
     *           doing the comparison in the ICRF than in the EME2000 frame.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     *         if the header constants cannot be read
     * @throws IOException
     */
    @Test
    public void validateSunEphemerisWrtJPLReferenceFile405() throws PatriusException, IOException {

        // J2000 date with TT time scale
        AbsoluteDate date = new AbsoluteDate();

        // deviation on the position (eme 2000)
        double deviation_position_eme2000 = 0;
        double maxDeviation_position_eme2000 = 0;
        final double mediumDeviation_position_eme2000;
        double sumDeviations_position_eme2000 = 0;
        // deviation on the velocity (eme 2000)
        double deviation_velocity_eme2000 = 0;
        double maxDeviation_velocity_eme2000 = 0;
        final double mediumDeviation_velocity_eme2000;
        double sumDeviations_velocity_eme2000 = 0;

        // deviation on the position (icrf)
        double deviation_position_icrf = 0;
        double maxDeviation_position_icrf = 0;
        final double mediumDeviation_position_icrf;
        double sumDeviations_position_icrf = 0;
        // deviation on the velocity (icrf)
        double deviation_velocity_icrf = 0;
        double maxDeviation_velocity_icrf = 0;
        final double mediumDeviation_velocity_icrf;
        double sumDeviations_velocity_icrf = 0;

        // execution times
        long t0;
        long t1;
        long timeJPL = 0;
        long timeOREKIT_eme2000 = 0;
        long timeOREKIT_icrf = 0;

        // 2 points per day
        final long steps = 19 * 365 * 2;

        // Initialisation of the date
        final double elapsedTimeJ2000_JULIAN_EPOCH = date.durationFrom(AbsoluteDate.JULIAN_EPOCH);
        double julianDays = elapsedTimeJ2000_JULIAN_EPOCH / 86400.0;

        // PV ephemeris
        final double[] ephemeris_r = new double[3];
        final double[] ephemeris_rprime = new double[3];

        /*
         * Initialisation of the OREKIT ephemeris
         */
        final JPLEphemeridesLoader loader = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SUN);

        // Sun body
        de405("unxp2000.405");
        final CelestialBody sun = loader.loadCelestialBody(CelestialBodyFactory.SUN);

        // Reader for Moon ephemeris (other reader because the first one have been used in other test)
        final URL url = JPLEphemerisValTest.class.getClassLoader().getResource(ephemMoon);
        final BufferedReader readerMoonClone = new BufferedReader(new FileReader(new File(url.getPath())));

        // Sets of coordinates which will be compared
        PVCoordinates pvSunJPL;
        PVCoordinates pvEMBJPL;
        PVCoordinates pvMoonJPL;
        // the ratio between Earth and Moon masses
        final double ratio = loader.getLoadedEarthMoonMassRatio();

        PVCoordinates pvOREKIT_icrf;
        PVCoordinates pvOREKIT_eme2000;

        Vector3D positionJPL_icrf;
        Vector3D positionOREKIT_icrf;
        Vector3D velocityJPL_icrf;
        Vector3D velocityOREKIT_icrf;

        Vector3D positionJPL_eme2000;
        Vector3D positionOREKIT_eme2000;
        Vector3D velocityJPL_eme2000;
        Vector3D velocityOREKIT_eme2000;

        /*
         * Loop on the date
         */
        String[] line;
        for (int j = 0; j < steps; j++) {

            // JPL ephemeris (Sun ephemeris in the ICRF)
            t0 = System.currentTimeMillis();

            line = readerSun2.readLine().split(" ");
            ephemeris_r[0] = Double.parseDouble(line[1]);
            ephemeris_r[1] = Double.parseDouble(line[2]);
            ephemeris_r[2] = Double.parseDouble(line[3]);

            ephemeris_rprime[0] = Double.parseDouble(line[4]);
            ephemeris_rprime[1] = Double.parseDouble(line[5]);
            ephemeris_rprime[2] = Double.parseDouble(line[6]);

            t1 = System.currentTimeMillis();
            timeJPL += t1 - t0;

            pvSunJPL = new PVCoordinates(new Vector3D(ephemeris_r[0] * Constants.JPL_SSD_ASTRONOMICAL_UNIT,
                ephemeris_r[1] * Constants.JPL_SSD_ASTRONOMICAL_UNIT,
                ephemeris_r[2] * Constants.JPL_SSD_ASTRONOMICAL_UNIT),
                new Vector3D(ephemeris_rprime[0] * Constants.JPL_SSD_ASTRONOMICAL_UNIT / 86400,
                    ephemeris_rprime[1] * Constants.JPL_SSD_ASTRONOMICAL_UNIT / 86400,
                    ephemeris_rprime[2] * Constants.JPL_SSD_ASTRONOMICAL_UNIT / 86400));

            // **************************
            // In the ICRF
            // **************************

            // OREKIT ephemeris
            t0 = System.currentTimeMillis();
            pvOREKIT_icrf = sun.getPVCoordinates(date, icrf405);
            t1 = System.currentTimeMillis();

            timeOREKIT_icrf += t1 - t0;

            // Comparison between JPL and OREKIT
            positionJPL_icrf = pvSunJPL.getPosition();
            positionOREKIT_icrf = pvOREKIT_icrf.getPosition();
            velocityJPL_icrf = pvSunJPL.getVelocity();
            velocityOREKIT_icrf = pvOREKIT_icrf.getVelocity();

            // deviation on the position
            deviation_position_icrf = positionJPL_icrf.subtract(positionOREKIT_icrf).getNorm();
            if (deviation_position_icrf > maxDeviation_position_icrf) {
                maxDeviation_position_icrf = deviation_position_icrf;
            }
            sumDeviations_position_icrf += deviation_position_icrf;

            // deviation on the velocity
            deviation_velocity_icrf = velocityJPL_icrf.subtract(velocityOREKIT_icrf).getNorm();
            if (deviation_velocity_icrf > maxDeviation_velocity_icrf) {
                maxDeviation_velocity_icrf = deviation_velocity_icrf;
            }
            sumDeviations_velocity_icrf += deviation_velocity_icrf;

            // *****************************
            // In the EME2000 frame
            // *****************************

            // Earth-Moon barycenter ephemeris (in the ICRF)

            line = readerSun1.readLine().split(" ");
            ephemeris_r[0] = Double.parseDouble(line[1]);
            ephemeris_r[1] = Double.parseDouble(line[2]);
            ephemeris_r[2] = Double.parseDouble(line[3]);

            ephemeris_rprime[0] = Double.parseDouble(line[4]);
            ephemeris_rprime[1] = Double.parseDouble(line[5]);
            ephemeris_rprime[2] = Double.parseDouble(line[6]);

            pvEMBJPL = new PVCoordinates(new Vector3D(ephemeris_r[0] * Constants.JPL_SSD_ASTRONOMICAL_UNIT,
                ephemeris_r[1] * Constants.JPL_SSD_ASTRONOMICAL_UNIT,
                ephemeris_r[2] * Constants.JPL_SSD_ASTRONOMICAL_UNIT),
                new Vector3D(ephemeris_rprime[0] * Constants.JPL_SSD_ASTRONOMICAL_UNIT / 86400,
                    ephemeris_rprime[1] * Constants.JPL_SSD_ASTRONOMICAL_UNIT / 86400,
                    ephemeris_rprime[2] * Constants.JPL_SSD_ASTRONOMICAL_UNIT / 86400));

            // Moon epehmeris (in the EME2000 frame)

            line = readerMoonClone.readLine().split(" ");
            ephemeris_r[0] = Double.parseDouble(line[1]);
            ephemeris_r[1] = Double.parseDouble(line[2]);
            ephemeris_r[2] = Double.parseDouble(line[3]);

            ephemeris_rprime[0] = Double.parseDouble(line[4]);
            ephemeris_rprime[1] = Double.parseDouble(line[5]);
            ephemeris_rprime[2] = Double.parseDouble(line[6]);

            pvMoonJPL = new PVCoordinates(new Vector3D(ephemeris_r[0] * Constants.JPL_SSD_ASTRONOMICAL_UNIT,
                ephemeris_r[1] * Constants.JPL_SSD_ASTRONOMICAL_UNIT,
                ephemeris_r[2] * Constants.JPL_SSD_ASTRONOMICAL_UNIT),
                new Vector3D(ephemeris_rprime[0] * Constants.JPL_SSD_ASTRONOMICAL_UNIT / 86400,
                    ephemeris_rprime[1] * Constants.JPL_SSD_ASTRONOMICAL_UNIT / 86400,
                    ephemeris_rprime[2] * Constants.JPL_SSD_ASTRONOMICAL_UNIT / 86400));

            // OREKIT ephemeris

            t0 = System.currentTimeMillis();
            pvOREKIT_eme2000 = sun.getPVCoordinates(date, gcrf);
            t1 = System.currentTimeMillis();

            timeOREKIT_eme2000 += t1 - t0;

            // Comparison between JPL and OREKIT

            // the position of the sun in the eme2000 frame is given by :
            // r_sun_eme2000 = r_sun_icrf - r_emb_icrf + 1/(1+ratio) r_moon_eme2000
            // see Satellite Orbits : Models, Methods and Applications, Montenbruck Olivier & Gill Eberhard
            positionJPL_eme2000 = pvSunJPL.getPosition()
                .add(pvEMBJPL.getPosition().negate())
                .add(pvMoonJPL.getPosition().scalarMultiply(1. / (1. + ratio)));
            positionOREKIT_eme2000 = pvOREKIT_eme2000.getPosition();
            velocityJPL_eme2000 = pvSunJPL.getVelocity()
                .add(pvEMBJPL.getVelocity().negate())
                .add(pvMoonJPL.getVelocity().scalarMultiply(1. / (1. + ratio)));
            velocityOREKIT_eme2000 = pvOREKIT_eme2000.getVelocity();

            // deviation on the position
            deviation_position_eme2000 = positionJPL_eme2000.subtract(positionOREKIT_eme2000).getNorm();
            if (deviation_position_eme2000 > maxDeviation_position_eme2000) {
                maxDeviation_position_eme2000 = deviation_position_eme2000;
            }
            sumDeviations_position_eme2000 += deviation_position_eme2000;

            // deviation on the velocity
            deviation_velocity_eme2000 = velocityJPL_eme2000.subtract(velocityOREKIT_eme2000).getNorm();
            if (deviation_velocity_eme2000 > maxDeviation_velocity_eme2000) {
                maxDeviation_velocity_eme2000 = deviation_velocity_eme2000;
            }
            sumDeviations_velocity_eme2000 += deviation_velocity_eme2000;

            // half a day time-lag
            date = date.shiftedBy(43200.0);
            julianDays = julianDays + 0.5;
        }

        // average deviation on position in the eme2000 frame
        mediumDeviation_position_eme2000 = sumDeviations_position_eme2000 / steps;
        // average deviation on velocity in the eme2000 frame
        mediumDeviation_velocity_eme2000 = sumDeviations_velocity_eme2000 / steps;

        // average deviation on position in the icrf
        mediumDeviation_position_icrf = sumDeviations_position_icrf / steps;
        // average deviation on velocity in the icrf
        mediumDeviation_velocity_icrf = sumDeviations_velocity_icrf / steps;

        System.out.println("** SUN EPHEMERIS COMPARISON **");
        System.out.println();
        System.out.println("In the frame EME2000");
        System.out.println("Max deviation on the position (m)    = " + maxDeviation_position_eme2000);
        System.out.println("Medium deviation on the position (m) = " + mediumDeviation_position_eme2000);
        System.out.println("Max deviation on the velocity (m/s)    = " + maxDeviation_velocity_eme2000);
        System.out.println("Medium deviation on the velocity (m/s) = " + mediumDeviation_velocity_eme2000);
        System.out.println("JPL calculation took " + timeJPL + " ms");
        System.out.println("OREKIT calculation took " + timeOREKIT_eme2000 + " ms");
        System.out.println();
        System.out.println("In the frame ICRF");
        System.out.println("Max deviation on the position (m)    = " + maxDeviation_position_icrf);
        System.out.println("Medium deviation on the position (m) = " + mediumDeviation_position_icrf);
        System.out.println("Max deviation on the velocity (m/s)    = " + maxDeviation_velocity_icrf);
        System.out.println("Medium deviation on the velocity (m/s) = " + mediumDeviation_velocity_icrf);
        System.out.println("JPL calculation took " + timeJPL + " ms");
        System.out.println("OREKIT calculation took " + timeOREKIT_icrf + " ms");
        System.out.println();

        // in the eme2000 frame
        Assert.assertTrue("In the Frame EME2000 : The max deviation in position is above " + 60 + "m !!",
            maxDeviation_position_eme2000 < 60);
        Assert.assertTrue("In the Frame EME2000 : The max deviation in velocity is above " + 0.001 + "m/s !!",
            maxDeviation_velocity_eme2000 < 0.001);
        // in the icrf frame
        Assert.assertTrue("In the Frame ICRF : The max deviation in position is above " + 3 + "cm !!",
            maxDeviation_position_icrf < 0.03);
        Assert.assertTrue("In the Frame ICRF : The max deviation in velocity is above " + 1E-9 + "m/s !!",
            maxDeviation_velocity_icrf < 1E-9);

        // Close streams
        readerSun1.close();
        readerSun2.close();
        readerMoonClone.close();
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#DE406_MOON_EPHEMERIS}
     * 
     * @testedMethod {@link CelestialBody#getPVCoordinates(AbsoluteDate, Frame)}
     * @testedMethod {@link JPLEphemeridesLoader#loadCelestialBody(String)}
     * 
     * @description We compare the ephemeris of the Moon given by Orekit based on DE406 file with those given Orekit
     *              based on DE405 file. The comparison is done on 19 years starting from J2000 epoch, for each day. We
     *              compare
     *              the norms of the position vectors and the norms of the velocity vectors in the EME2000 frame.
     * 
     * @input Moon ephemeris given by Orekit with the DE405 file
     * 
     * @output Moon ephemeris
     * 
     * @testPassCriteria The mean difference between the position norms should be lower than 1 m.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     *         if the header constants cannot be read
     */
    @Test
    public void validateMoonEphemerisDE406() throws PatriusException {

        // J2000 date with TT time scale
        AbsoluteDate date = new AbsoluteDate();

        // deviation on the position
        double deviation_position = 0;
        final double mediumDeviation_position;
        double sumDeviations_position = 0;

        // execution times
        long t0;
        long t1;
        long timeOREKITDE405 = 0;
        long timeOREKITDE406 = 0;

        // 2 points per day for 19 years
        final long steps = 19 * 365 * 2;

        // Initialisation of the date
        final double elapsedTimeJ2000_JULIAN_EPOCH = date.durationFrom(AbsoluteDate.JULIAN_EPOCH);
        double julianDays = elapsedTimeJ2000_JULIAN_EPOCH / 86400.0;

        /*
         * Initialisation of the OREKIT ephemeris
         */
        final JPLEphemeridesLoader loaderDE405 = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.MOON);

        final CelestialBody moonDE405 = loaderDE405.loadCelestialBody(CelestialBodyFactory.MOON);

        /*
         * Initialisation of the OREKIT ephemeris
         */
        final JPLEphemeridesLoader loaderDE406 = new JPLEphemeridesLoader("unxp1800.406",
            JPLEphemeridesLoader.EphemerisType.MOON);
        CelestialBodyFactory.clearCelestialBodyLoaders();
        final CelestialBody moonDE406 = loaderDE406.loadCelestialBody(CelestialBodyFactory.MOON);

        // Sets of coordinates which will be compared
        PVCoordinates pvOREKITDE406;
        PVCoordinates pvOREKITDE405;
        Vector3D positionOREKITDE406;
        Vector3D positionOREKITDE405;

        /*
         * Loop on the date
         */
        for (int j = 0; j < steps; j++) {

            // OREKIT ephemeris
            t0 = System.currentTimeMillis();
            pvOREKITDE405 = moonDE405.getPVCoordinates(date, gcrf);
            t1 = System.currentTimeMillis();

            timeOREKITDE405 += t1 - t0;

            t0 = System.currentTimeMillis();
            pvOREKITDE406 = moonDE406.getPVCoordinates(date, gcrf);
            t1 = System.currentTimeMillis();

            timeOREKITDE406 += t1 - t0;

            // Comparison between JPL and OREKIT
            positionOREKITDE406 = pvOREKITDE406.getPosition();
            positionOREKITDE405 = pvOREKITDE405.getPosition();

            // deviation on the position
            deviation_position = positionOREKITDE406.subtract(positionOREKITDE405).getNorm();
            sumDeviations_position += deviation_position;

            // half a day time-lag
            date = date.shiftedBy(43200.0);
            julianDays = julianDays + 0.5;
        }

        // average deviation on the position
        mediumDeviation_position = sumDeviations_position / steps;

        System.out.println("** MOON EPHEMERIS COMPARISON **");
        System.out.println();
        System.out.println("Medium deviation on the position  (m) = " + mediumDeviation_position);
        System.out.println("OREKIT405 calculation took " + timeOREKITDE405 + " ms");
        System.out.println("OREKIT406 calculation took " + timeOREKITDE406 + " ms");
        System.out.println();

        Assert.assertTrue("The max deviation in position is above " + 50 + " cm !!", mediumDeviation_position < 0.5);
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#DE406_SUN_EPHEMERIS}
     * 
     * @testedMethod {@link CelestialBody#getPVCoordinates(AbsoluteDate, Frame)}
     * @testedMethod {@link JPLEphemeridesLoader#loadCelestialBody(String)}
     * 
     * @description We compare the ephemeris of the Sun given by Orekit based on DE406 file with those given Orekit
     *              based on DE405 file. The comparison is done on 19 years starting from J2000 epoch, for each day. We
     *              compare
     *              the norms of the position vectors and the norms of the velocity vectors in the ICRF.
     * 
     * @input Sun ephemeris given by Orekit with the DE405 file
     * 
     * @output Sun ephemeris
     * 
     * @testPassCriteria The mean difference between the position norms should be lower than 3 m.
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     *         if the header constants cannot be read
     */
    @Test
    public void validateSunEphemerisDE406() throws PatriusException {

        // J2000 date with TT time scale
        AbsoluteDate date = new AbsoluteDate();

        // deviation on the position
        double deviation_position = 0;
        final double mediumDeviation_position;
        double sumDeviations_position = 0;

        // execution times
        long t0;
        long t1;
        long timeOREKITDE405 = 0;
        long timeOREKITDE406 = 0;

        // 2 points per day for 19 years
        final long steps = 19 * 365 * 2;

        // Initialisation of the date
        final double elapsedTimeJ2000_JULIAN_EPOCH = date.durationFrom(AbsoluteDate.JULIAN_EPOCH);
        double julianDays = elapsedTimeJ2000_JULIAN_EPOCH / 86400.0;

        /*
         * Initialisation of the OREKIT ephemeris
         */
        final JPLEphemeridesLoader loaderDE405 = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SUN);
        de405("unxp2000.405");
        final CelestialBody sunDE405 = loaderDE405.loadCelestialBody(CelestialBodyFactory.SUN);

        /*
         * Initialisation of the OREKIT ephemeris
         */
        final JPLEphemeridesLoader loaderDE406 = new JPLEphemeridesLoader("unxp1800.406",
            JPLEphemeridesLoader.EphemerisType.SUN);
        de406("unxp1800.406");
        final CelestialBody sunDE406 = loaderDE406.loadCelestialBody(CelestialBodyFactory.SUN);

        // Sets of coordinates which will be compared
        PVCoordinates pvOREKITDE406;
        PVCoordinates pvOREKITDE405;
        Vector3D positionOREKITDE406;
        Vector3D positionOREKITDE405;

        /*
         * Loop on the date
         */
        for (int j = 0; j < steps; j++) {

            // OREKIT ephemeris
            t0 = System.currentTimeMillis();
            pvOREKITDE405 = sunDE405.getPVCoordinates(date, icrf405);
            t1 = System.currentTimeMillis();

            timeOREKITDE405 += t1 - t0;

            t0 = System.currentTimeMillis();
            pvOREKITDE406 = sunDE406.getPVCoordinates(date, icrf406);
            t1 = System.currentTimeMillis();

            timeOREKITDE406 += t1 - t0;

            // Comparison between JPL and OREKIT
            positionOREKITDE406 = pvOREKITDE406.getPosition();
            positionOREKITDE405 = pvOREKITDE405.getPosition();

            // deviation on the position
            deviation_position = positionOREKITDE406.subtract(positionOREKITDE405).getNorm();
            sumDeviations_position += deviation_position;

            // half a day time-lag
            date = date.shiftedBy(43200.0);
            julianDays = julianDays + 0.5;
        }

        // average deviation on the position
        mediumDeviation_position = sumDeviations_position / steps;

        System.out.println("** SUN EPHEMERIS COMPARISON **");
        System.out.println();
        System.out.println("Medium deviation on the position  (m) = " + mediumDeviation_position);
        System.out.println("OREKIT405 calculation took " + timeOREKITDE405 + " ms");
        System.out.println("OREKIT406 calculation took " + timeOREKITDE406 + " ms");
        System.out.println();

        Assert.assertTrue("The max deviation in position is above " + 13.0 + " m !!", mediumDeviation_position < 13.0);
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#BODIES_EPHEMERIS}
     * 
     * @testedMethod {@link CelestialBody#getPVCoordinates(AbsoluteDate, Frame)}
     * @testedMethod {@link JPLEphemeridesLoader#loadCelestialBody(String)}
     * 
     * @description given a date, we compare one point of the ephemerides given by Orekit with the DE405 file with the
     *              corresponding point given by COMPAS
     * 
     * @input one point of the ephemerides of the Sun, the Moon, Venus, Mars, Jupiter
     * 
     * @output position velocity coordinates
     * 
     * @testPassCriteria the pv coordinates given by Orekit are the same as those given by COMPAS given a
     *                   relative tolerance
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     *         if the header constants cannot be read
     */
    @Test
    public void validateBodiesEphemeridesWrtCOMPASReferenceFile() throws PatriusException {

        // J2000 date with TT time scale
        final AbsoluteDate date = new AbsoluteDate();

        /*
         * OREKIT ephemeris of the Sun
         */
        JPLEphemeridesLoader loader = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SUN);
        de405("unxp2000.405");
        final CelestialBody sun = loader.loadCelestialBody(CelestialBodyFactory.SUN);

        PVCoordinates pvOREKIT = sun.getPVCoordinates(date, gcrf);

        // relative comparison
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getPosition().getX(), 2.6499034219411404E10, this.epsilon,
            26499034229., 1E-9, "x component deviation for the sun");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getPosition().getY(), -1.3275741766627747E11, this.epsilon,
            -132757417665., 1E-9, "y component deviation for the sun");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getPosition().getZ(), -5.755671744859666E10, this.epsilon,
            -57556717448., 1E-9, "z component deviation for the sun");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getVelocity().getX(), 29794.260048710246, this.epsilon,
            29794.260, 1E-6, "x component deviation for the sun");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getVelocity().getY(), 5018.052458653004, this.epsilon,
            5018.052, 1E-6, "y component deviation for the sun");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getVelocity().getZ(), 2175.3937278440676, this.epsilon,
            2175.394, 1E-6, "z component deviation for the sun");

        // absolute comparison
        validate.assertEquals(pvOREKIT.getPosition().getX(), 2.6499034219411404E10, 1E-4,
            26499034229., 10, "x component deviation for the sun");
        validate.assertEquals(pvOREKIT.getPosition().getY(), -1.3275741766627747E11, 1E-4,
            -132757417665., 10, "y component deviation for the sun");
        validate.assertEquals(pvOREKIT.getPosition().getZ(), -5.755671744859666E10, 1E-4,
            -57556717448., 10, "z component deviation for the sun");
        validate.assertEquals(pvOREKIT.getVelocity().getX(), 29794.260048710246, this.epsilon,
            29794.260, 1E-2, "x component deviation for the sun");
        validate.assertEquals(pvOREKIT.getVelocity().getY(), 5018.052458653004, this.epsilon,
            5018.052, 1E-2, "y component deviation for the sun");
        validate.assertEquals(pvOREKIT.getVelocity().getZ(), 2175.3937278440676, this.epsilon,
            2175.394, 1E-2, "z component deviation for the sun");

        loader = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.MOON);

        final CelestialBody moon = loader.loadCelestialBody(CelestialBodyFactory.MOON);

        pvOREKIT = moon.getPVCoordinates(date, gcrf);

        // relative comparison
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getPosition().getX(), -2.916083886613286E8, this.epsilon,
            -291608388., 1E-8, "x component deviation for the moon");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getPosition().getY(), -2.667168290261366E8, this.epsilon,
            -266716829., 1E-8, "y component deviation for the moon");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getPosition().getZ(), -7.610248122761913E7, this.epsilon,
            -76102481., 1E-8, "z component deviation for the moon");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getVelocity().getX(), 643.5313730299109, this.epsilon,
            643.531, 1E-6, "x component deviation for the moon");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getVelocity().getY(), -666.0876960856899, this.epsilon,
            -666.088, 1E-6, "y component deviation for the moon");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getVelocity().getZ(), -301.3257067549776, this.epsilon,
            -301.326, 1E-6, "z component deviation for the moon");

        // absolute comparison
        validate.assertEquals(pvOREKIT.getPosition().getX(), -2.916083886613286E8, this.epsilon,
            -291608388., 1, "x component deviation for the moon");
        validate.assertEquals(pvOREKIT.getPosition().getY(), -2.667168290261366E8, this.epsilon,
            -266716829., 1, "y component deviation for the moon");
        validate.assertEquals(pvOREKIT.getPosition().getZ(), -7.610248122761913E7, this.epsilon,
            -76102481., 1, "z component deviation for the moon");
        validate.assertEquals(pvOREKIT.getVelocity().getX(), 643.5313730299109, this.epsilon,
            643.531, 1E-3, "x component deviation for the moon");
        validate.assertEquals(pvOREKIT.getVelocity().getY(), -666.0876960856899, this.epsilon,
            -666.088, 1E-3, "y component deviation for the moon");
        validate.assertEquals(pvOREKIT.getVelocity().getZ(), -301.3257067549776, this.epsilon,
            -301.326, 1E-3, "z component deviation for the moon");

        loader = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.VENUS);
        de405("unxp2000.405");
        final CelestialBody venus = loader.loadCelestialBody(CelestialBodyFactory.VENUS);

        pvOREKIT = venus.getPVCoordinates(date, gcrf);

        // relative comparison
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getPosition().getX(), -8.095745976035785E10, this.epsilon,
            -80957459750., 1E-9, "x component deviation for venus");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getPosition().getY(), -1.3967994701465866E11, this.epsilon,
            -139679947023., 1E-9, "y component deviation for venus");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getPosition().getZ(), -5.387052975987144E10, this.epsilon,
            -53870529764., 1E-9, "z component deviation for venus");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getVelocity().getX(), 31176.16616264989, this.epsilon,
            31176.166, 1E-7, "x component deviation for venus");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getVelocity().getY(), -26999.766060020007, this.epsilon,
            -26999.766, 1E-7, "y component deviation for venus");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getVelocity().getZ(), -12316.44154611773, this.epsilon,
            -12316.442, 1E-7, "z component deviation for venus");

        // absolute comparison
        validate.assertEquals(pvOREKIT.getPosition().getX(), -8.095745976035785E10, 1E-4,
            -80957459750., 100, "x component deviation for venus");
        validate.assertEquals(pvOREKIT.getPosition().getY(), -1.3967994701465866E11, 1E-4,
            -139679947023., 100, "y component deviation for venus");
        validate.assertEquals(pvOREKIT.getPosition().getZ(), -5.387052975987144E10, 1E-4,
            -53870529764., 100, "z component deviation for venus");
        validate.assertEquals(pvOREKIT.getVelocity().getX(), 31176.16616264989, this.epsilon,
            31176.166, 1E-3, "x component deviation for venus");
        validate.assertEquals(pvOREKIT.getVelocity().getY(), -26999.766060020007, this.epsilon,
            -26999.766, 1E-3, "y component deviation for venus");
        validate.assertEquals(pvOREKIT.getVelocity().getZ(), -12316.44154611773, this.epsilon,
            -12316.442, 1E-3, "z component deviation for venus");

        loader = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.MARS);
        de405("unxp2000.405");
        final CelestialBody mars = loader.loadCelestialBody(CelestialBodyFactory.MARS);

        pvOREKIT = mars.getPVCoordinates(date, gcrf);

        // relative comparison
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getPosition().getX(), 2.3454717486124158E11, this.epsilon,
            234547174871., 1E-10, "x component deviation for mars");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getPosition().getY(), -1.3254779779212714E11, this.epsilon,
            -132547797783., 1E-10, "y component deviation for mars");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getPosition().getZ(), -6.308587985737891E10, this.epsilon,
            -63085879853., 1E-10, "z component deviation for mars");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getVelocity().getX(), 30956.932405412874, this.epsilon,
            30956.932, 1E-7, "x component deviation for mars");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getVelocity().getY(), 28936.462238564196, this.epsilon,
            28936.462, 1E-7, "y component deviation for mars");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getVelocity().getZ(), 13114.565454395733, this.epsilon,
            13114.565, 1E-7, "z component deviation for mars");

        // absolute comparison
        validate.assertEquals(pvOREKIT.getPosition().getX(), 2.3454717486124158E11, 1E-4,
            234547174871., 10, "x component deviation for mars");
        validate.assertEquals(pvOREKIT.getPosition().getY(), -1.3254779779212714E11, 1E-4,
            -132547797783., 10, "y component deviation for mars");
        validate.assertEquals(pvOREKIT.getPosition().getZ(), -6.308587985737891E10, 1E-4,
            -63085879853., 10, "z component deviation for mars");
        validate.assertEquals(pvOREKIT.getVelocity().getX(), 30956.932405412874, this.epsilon,
            30956.932, 1E-3, "x component deviation for mars");
        validate.assertEquals(pvOREKIT.getVelocity().getY(), 28936.462238564196, this.epsilon,
            28936.462, 1E-3, "y component deviation for mars");
        validate.assertEquals(pvOREKIT.getVelocity().getZ(), 13114.565454395733, this.epsilon,
            13114.565, 1E-3, "z component deviation for mars");

        loader = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.JUPITER);
        de405("unxp2000.405");
        final CelestialBody jupiter = loader.loadCelestialBody(CelestialBodyFactory.JUPITER);

        pvOREKIT = jupiter.getPVCoordinates(date, gcrf);

        // relative comparison
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getPosition().getX(), 6.250665759029778E11, this.epsilon,
            625066616968., 1E-6, "x component deviation for jupiter");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getPosition().getY(), 2.7662896308906433E11, this.epsilon,
            276628918962., 1E-6, "y component deviation for jupiter");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getPosition().getZ(), 1.033376442659299E11, this.epsilon,
            103337624023., 1E-6, "z component deviation for jupiter");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getVelocity().getX(), 21884.42157327971, this.epsilon,
            21884.399, 1E-5, "x component deviation for jupiter");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getVelocity().getY(), 15201.54985491874, this.epsilon,
            15201.626, 1E-5, "y component deviation for jupiter");
        validate.assertEqualsWithRelativeTolerance(pvOREKIT.getVelocity().getZ(), 6733.11288726119, this.epsilon,
            6733.149, 1E-5, "z component deviation for jupiter");

        // absolute comparison
        validate.assertEquals(pvOREKIT.getPosition().getX(), 6.250665759029778E11, 1E-4,
            625066616968., 50000, "x component deviation for jupiter");
        validate.assertEquals(pvOREKIT.getPosition().getY(), 2.7662896308906433E11, 1E-4,
            276628918962., 50000, "y component deviation for jupiter");
        validate.assertEquals(pvOREKIT.getPosition().getZ(), 1.033376442659299E11, 1E-4,
            103337624023., 50000, "z component deviation for jupiter");
        validate.assertEquals(pvOREKIT.getVelocity().getX(), 21884.42157327971, this.epsilon,
            21884.399, 0.1, "x component deviation for jupiter");
        validate.assertEquals(pvOREKIT.getVelocity().getY(), 15201.54985491874, this.epsilon,
            15201.626, 0.1, "y component deviation for jupiter");
        validate.assertEquals(pvOREKIT.getVelocity().getZ(), 6733.11288726119, this.epsilon,
            6733.149, 0.1, "z component deviation for jupiter");
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#GM}
     * 
     * @testedMethod {@link CelestialBody#getGM()}
     * 
     * @description compare the GM given by Orekit with those given by JPL
     * 
     * @input for the sun : 132712440018 km3/s2 ; for the moon : 4902.7779 km3/s2; for the earth :
     *        398600.4418 km3/s2
     * 
     * @output double GM
     * 
     * @testPassCriteria the outputs and the inputs are the same given an absolute tolerance
     * 
     * @referenceVersion 1.0
     * 
     * @nonRegressionVersion 1.0
     * 
     * @throws PatriusException
     *         if the header constants cannot be read
     */
    @Test
    public void testGM() throws PatriusException {

        /*
         * OREKIT ephemeris of the Sun
         */
        JPLEphemeridesLoader loader = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.SUN);
        de405("unxp2000.405");
        final CelestialBody sun = loader.loadCelestialBody(CelestialBodyFactory.SUN);

        validate.assertEqualsWithRelativeTolerance(sun.getGM(), 1.3271244001798696E20, this.epsilon,
            132712440018. * 1E9,
            1E-13, "GM sun deviation");

        loader = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.EARTH);

        final CelestialBody earth = loader.loadCelestialBody(CelestialBodyFactory.EARTH);

        validate.assertEqualsWithRelativeTolerance(earth.getGM(), 3.986004328969392E14, this.epsilon,
            398600.4418 * 1E9,
            1E-7, "GM earth deviation");

        loader = new JPLEphemeridesLoader("unxp2000.405",
            JPLEphemeridesLoader.EphemerisType.MOON);

        final CelestialBody moon = loader.loadCelestialBody(CelestialBodyFactory.MOON);

        validate.assertEqualsWithRelativeTolerance(moon.getGM(), 4.902800582147764E12, this.epsilon, 4902.7779 * 1E9,
            1E-5,
            "GM moon deviation");
    }

    /**
     * Creation of the earth moon barycenter and the solar system barycenter loaders with a specific
     * data file.
     * 
     * @param fileName
     *        name of the file that has to be loaded
     * @throws PatriusException
     *         if the header constants cannot be read
     */
    private static void de406(final String fileName) throws PatriusException {

        CelestialBodyFactory.clearCelestialBodyLoaders();

        final JPLEphemeridesLoader loaderEMB = new JPLEphemeridesLoader(fileName,
            JPLEphemeridesLoader.EphemerisType.EARTH_MOON);
        final JPLEphemeridesLoader loaderSSB = new JPLEphemeridesLoader(fileName,
            JPLEphemeridesLoader.EphemerisType.SOLAR_SYSTEM_BARYCENTER);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);
    }

    /**
     * Creation of the earth moon barycenter and the solar system barycenter loaders with a specific
     * data file.
     * 
     * @param fileName
     *        name of the file that has to be loaded
     * @throws PatriusException
     *         if the header constants cannot be read
     */
    private static void de405(final String fileName) throws PatriusException {

        CelestialBodyFactory.clearCelestialBodyLoaders();

        final JPLEphemeridesLoader loaderEMB = new JPLEphemeridesLoader(fileName,
            JPLEphemeridesLoader.EphemerisType.EARTH_MOON);
        final JPLEphemeridesLoader loaderSSB = new JPLEphemeridesLoader(fileName,
            JPLEphemeridesLoader.EphemerisType.SOLAR_SYSTEM_BARYCENTER);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);
    }

    /**
     * set up
     */
    @BeforeClass
    public static void setUp() {
        try {
            gcrf = FramesFactory.getGCRF();

            Utils.setDataRoot("regular-dataCNES-2003/de406-ephemerides");
            de406("unxp1800.406");
            icrf406 = FramesFactory.getICRF();

            Utils.setDataRoot("regular-dataCNES-2003/de405-ephemerides");
            de405("unxp2000.405");
            icrf405 = FramesFactory.getICRF();

            // Reader for Moon ephemeris
            final URL url1 = JPLEphemerisValTest.class.getClassLoader().getResource(ephemMoon);
            FileInputStream stream1 = null;
            try {
                stream1 = new FileInputStream(new File(url1.getFile()));
            } catch (final FileNotFoundException e) {
                e.printStackTrace();
            }
            readerMoon = new BufferedReader(new InputStreamReader(stream1));

            // Readers for Sun ephemeris
            final URL url2 = JPLEphemerisValTest.class.getClassLoader().getResource(ephemSun1);
            final URL url3 = JPLEphemerisValTest.class.getClassLoader().getResource(ephemSun2);
            FileInputStream stream2 = null;
            FileInputStream stream3 = null;
            try {
                stream2 = new FileInputStream(new File(url2.getFile()));
                stream3 = new FileInputStream(new File(url3.getFile()));
            } catch (final FileNotFoundException e) {
                e.printStackTrace();
            }
            readerSun1 = new BufferedReader(new InputStreamReader(stream2));
            readerSun2 = new BufferedReader(new InputStreamReader(stream3));

            Utils.setDataRoot("regular-dataCNES-2003");
            FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));
        } catch (final PatriusException e) {
            e.printStackTrace();
        }
        try {
            validate = new Validate(JPLEphemerisValTest.class);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * tearDown
     */
    @AfterClass
    public static void tearDown() {
        try {
            validate.produceLog();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
