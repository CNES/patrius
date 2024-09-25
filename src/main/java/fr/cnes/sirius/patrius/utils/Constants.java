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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:346:23/04/2015:creation of a local time class
 * VERSION::DM:600:16/06/2016:add Cook (Cn, Ct) models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;

//CHECKSTYLE: stop MagicNumber check
//CHECKSTYLE: stop InterfaceIsType check
//Reason: constants

/**
 * Set of useful physical constants.
 * 
 * @author Luc Maisonobe
 */
public interface Constants {

    /** Conversion factor: kilometers to meters. */
    double KM_TO_M = 1E3;

    /** Speed of light: 299792458.0 m/s. */
    double SPEED_OF_LIGHT = 299792458.0;

    /** Duration of a mean solar day: 86400.0 s. */
    double JULIAN_DAY = 86400.0;

    /** Duration of a julian year: 365.25 {@link #JULIAN_DAY}. */
    double JULIAN_YEAR = 31557600.0;

    /** Number of julian days in a century. */
    double JULIAN_DAY_CENTURY = 36525.0;

    /** Duration of a julian century: 36525 {@link #JULIAN_DAY}. */
    double JULIAN_CENTURY = JULIAN_DAY_CENTURY * JULIAN_DAY;

    /** Conversion factor from arc seconds to radians: 2*PI/(360*60*60). */
    double ARC_SECONDS_TO_RADIANS = MathUtils.TWO_PI / 1296000;

    /** Conversion factor from radians to seconds: 86400 / 2*PI. */
    double RADIANS_TO_SEC = Constants.JULIAN_DAY / MathUtils.TWO_PI;

    /** Standard gravity constant, used in maneuvers definition: 9.80665 m/s<sup>2</sup>. */
    double G0_STANDARD_GRAVITY = 9.80665;

    /** Sun radius: 695500000 m. */
    double SUN_RADIUS = 6.955e8;

    /** Moon equatorial radius: 1737400 m. */
    double MOON_EQUATORIAL_RADIUS = 1737400.0;

    /** Earth equatorial radius from WGS84 model: 6378137.0 m. */
    double WGS84_EARTH_EQUATORIAL_RADIUS = 6378137.0;

    /** Earth flattening from WGS84 model: 1.0 / 298.257223563. */
    double WGS84_EARTH_FLATTENING = 1.0 / 298.257223563;

    /** Earth angular velocity from WGS84 model: 7.292115e-5 rad/s. */
    double WGS84_EARTH_ANGULAR_VELOCITY = 7.292115e-5;

    /** Earth gravitational constant from WGS84 model: 3.986004418 m<sup>3</sup>/s<sup>2</sup>. */
    double WGS84_EARTH_MU = 3.986004418e14;

    /** Earth un-normalized second zonal coefficient from WGS84 model: . */
    double WGS84_EARTH_C20 = -1.08262668355315e-3;

    /** Earth equatorial radius from GRS80 model: 6378137.0 m. */
    double GRS80_EARTH_EQUATORIAL_RADIUS = 6378137.0;

    /** Earth flattening from GRS80 model: 1.0 / 298.257222101. */
    double GRS80_EARTH_FLATTENING = 1.0 / 298.257222101;

    /** Earth angular velocity from GRS80 model: 7.292115e-5 rad/s. */
    double GRS80_EARTH_ANGULAR_VELOCITY = 7.292115e-5;

    /** Earth gravitational constant from GRS80 model: 3.986005e14 m<sup>3</sup>/s<sup>2</sup>. */
    double GRS80_EARTH_MU = 3.986005e14;

    /** Earth un-normalized second zonal coefficient from GRS80 model: -1.08263e-3. */
    double GRS80_EARTH_C20 = -1.08263e-3;

    /** Earth equatorial radius from EGM96 model: 6378136.3 m. */
    double EGM96_EARTH_EQUATORIAL_RADIUS = 6378136.3;

    /** Earth gravitational constant from EGM96 model: 3.986004415 m<sup>3</sup>/s<sup>2</sup>. */
    double EGM96_EARTH_MU = 3.986004415e14;

    /** Earth un-normalized second zonal coefficient from EGM96 model: -1.08262668355315e-3. */
    double EGM96_EARTH_C20 = -1.08262668355315e-3;

    /** Earth un-normalized third zonal coefficient from EGM96 model: 2.53265648533224e-6. */
    double EGM96_EARTH_C30 = 2.53265648533224e-6;

    /** Earth un-normalized fourth zonal coefficient from EGM96 model: 1.619621591367e-6. */
    double EGM96_EARTH_C40 = 1.619621591367e-6;

    /** Earth un-normalized fifth zonal coefficient from EGM96 model: 2.27296082868698e-7. */
    double EGM96_EARTH_C50 = 2.27296082868698e-7;

    /** Earth un-normalized sixth zonal coefficient from EGM96 model: -5.40681239107085e-7. */
    double EGM96_EARTH_C60 = -5.40681239107085e-7;

    /** Earth equatorial radius from GRIM5C1 model: 6378136.46 m. */
    double GRIM5C1_EARTH_EQUATORIAL_RADIUS = 6378136.46;

    /** Earth flattening from GRIM5C1 model: 1.0 / 298.25765. */
    double GRIM5C1_EARTH_FLATTENING = 1.0 / 298.25765;

    /** Earth angular velocity from GRIM5C1 model: 7.292115e-5 rad/s. */
    double GRIM5C1_EARTH_ANGULAR_VELOCITY = 7.292115e-5;

    /** Earth gravitational constant from GRIM5C1 model: 3.986004415 m<sup>3</sup>/s<sup>2</sup>. */
    double GRIM5C1_EARTH_MU = 3.986004415e14;

    /** Earth un-normalized second zonal coefficient from GRIM5C1 model: -1.082626110612609e-3. */
    double GRIM5C1_EARTH_C20 = -1.082626110612609e-3;

    /** Earth un-normalized third zonal coefficient from GRIM5C1 model: 2.536150841690056e-6. */
    double GRIM5C1_EARTH_C30 = 2.536150841690056e-6;

    /** Earth un-normalized fourth zonal coefficient from GRIM5C1 model: 1.61936352497151e-6. */
    double GRIM5C1_EARTH_C40 = 1.61936352497151e-6;

    /** Earth un-normalized fifth zonal coefficient from GRIM5C1 model: 2.231013736607540e-7. */
    double GRIM5C1_EARTH_C50 = 2.231013736607540e-7;

    /** Earth un-normalized sixth zonal coefficient from GRIM5C1 model: -5.402895357302363e-7. */
    double GRIM5C1_EARTH_C60 = -5.402895357302363e-7;

    /** Earth equatorial radius from EIGEN5C model: 6378136.46 m. */
    double EIGEN5C_EARTH_EQUATORIAL_RADIUS = 6378136.46;

    /** Earth gravitational constant from EIGEN5C model: 3.986004415 m<sup>3</sup>/s<sup>2</sup>. */
    double EIGEN5C_EARTH_MU = 3.986004415e14;

    /** Earth un-normalized second zonal coefficient from EIGEN5C model: -1.082626457231767e-3. */
    double EIGEN5C_EARTH_C20 = -1.082626457231767e-3;

    /** Earth un-normalized third zonal coefficient from EIGEN5C model: 2.532547231862799e-6. */
    double EIGEN5C_EARTH_C30 = 2.532547231862799e-6;

    /** Earth un-normalized fourth zonal coefficient from EIGEN5C model: 1.619964434136e-6. */
    double EIGEN5C_EARTH_C40 = 1.619964434136e-6;

    /** Earth un-normalized fifth zonal coefficient from EIGEN5C model: 2.277928487005437e-7. */
    double EIGEN5C_EARTH_C50 = 2.277928487005437e-7;

    /** Earth un-normalized sixth zonal coefficient from EIGEN5C model: -5.406653715879098e-7. */
    double EIGEN5C_EARTH_C60 = -5.406653715879098e-7;

    /** Gaussian gravitational constant: 0.01720209895 &radic;(AU<sup>3</sup>/d<sup>2</sup>). */
    double JPL_SSD_GAUSSIAN_GRAVITATIONAL_CONSTANT = 0.01720209895;

    /** Astronomical Unit: 149597870691 m. */
    double JPL_SSD_ASTRONOMICAL_UNIT = 149597870691.0;

    /** Sun attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    double JPL_SSD_SUN_GM = JPL_SSD_GAUSSIAN_GRAVITATIONAL_CONSTANT * JPL_SSD_GAUSSIAN_GRAVITATIONAL_CONSTANT *
        JPL_SSD_ASTRONOMICAL_UNIT * JPL_SSD_ASTRONOMICAL_UNIT * JPL_SSD_ASTRONOMICAL_UNIT /
        (JULIAN_DAY * JULIAN_DAY);

    /** Sun/Mercury mass ratio: 6023600. */
    double JPL_SSD_SUN_MERCURY_MASS_RATIO = 6023600;

    /** Sun/Mercury attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    double JPL_SSD_MERCURY_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_MERCURY_MASS_RATIO;

    /** Sun/Venus mass ratio: 408523.71. */
    double JPL_SSD_SUN_VENUS_MASS_RATIO = 408523.71;

    /** Sun/Venus attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    double JPL_SSD_VENUS_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_VENUS_MASS_RATIO;

    /** Sun/(Earth + Moon) mass ratio: 328900.56. */
    double JPL_SSD_SUN_EARTH_PLUS_MOON_MASS_RATIO = 328900.56;

    /** Sun/(Earth + Moon) attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    double JPL_SSD_EARTH_PLUS_MOON_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_EARTH_PLUS_MOON_MASS_RATIO;

    /** Earth/Moon mass ratio: 81.30059. */
    double JPL_SSD_EARTH_MOON_MASS_RATIO = 81.300596;

    /** Moon attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    double JPL_SSD_MOON_GM = JPL_SSD_EARTH_PLUS_MOON_GM / (1.0 + JPL_SSD_EARTH_MOON_MASS_RATIO);

    /** Earth attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    double JPL_SSD_EARTH_GM = JPL_SSD_MOON_GM * JPL_SSD_EARTH_MOON_MASS_RATIO;

    /** Sun/(Mars system) mass ratio: 3098708.0. */
    double JPL_SSD_SUN_MARS_SYSTEM_MASS_RATIO = 3098708.0;

    /** Sun/(Mars system) attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    double JPL_SSD_MARS_SYSTEM_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_MARS_SYSTEM_MASS_RATIO;

    /** Sun/(Jupiter system) mass ratio: 1047.3486. */
    double JPL_SSD_SUN_JUPITER_SYSTEM_MASS_RATIO = 1047.3486;

    /** Sun/(Jupiter system) ttraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    double JPL_SSD_JUPITER_SYSTEM_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_JUPITER_SYSTEM_MASS_RATIO;

    /** Sun/(Saturn system) mass ratio: 3497.898. */
    double JPL_SSD_SUN_SATURN_SYSTEM_MASS_RATIO = 3497.898;

    /** Sun/(Saturn system) attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    double JPL_SSD_SATURN_SYSTEM_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_SATURN_SYSTEM_MASS_RATIO;

    /** Sun/(Uranus system) mass ratio: 22902.98. */
    double JPL_SSD_SUN_URANUS_SYSTEM_MASS_RATIO = 22902.98;

    /** Sun/(Uranus system) attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    double JPL_SSD_URANUS_SYSTEM_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_URANUS_SYSTEM_MASS_RATIO;

    /** Sun/(Neptune system) mass ratio: 19412.24. */
    double JPL_SSD_SUN_NEPTUNE_SYSTEM_MASS_RATIO = 19412.24;

    /** Sun/(Neptune system) attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    double JPL_SSD_NEPTUNE_SYSTEM_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_NEPTUNE_SYSTEM_MASS_RATIO;

    /** Sun/(Pluto system) mass ratio: 1.35e8. */
    double JPL_SSD_SUN_PLUTO_SYSTEM_MASS_RATIO = 1.35e8;

    /** Sun/(Pluto system) ttraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    double JPL_SSD_PLUTO_SYSTEM_GM = JPL_SSD_SUN_GM / JPL_SSD_SUN_PLUTO_SYSTEM_MASS_RATIO;

    /** Perfect gas constant (Jmol<sup>-1</sup>K<sup>-1</sup>. */
    double PERFECT_GAS_CONSTANT = 8.3144598;

    /** Avogadro constant. */
    double AVOGADRO_CONSTANT = 6.022140857E23;

    /**
     * Solar constant (W/M**2).
     * see "obelixtype.f90 in OBELIX software"
     */
    double CONST_SOL_W_M2 = 0.1367203504709000E+04;
    /**
     * Solar constant (N/M**2).
     * see "obelixtype.f90 in OBELIX software"
     */
    double CONST_SOL_N_M2 = CONST_SOL_W_M2 / Constants.SPEED_OF_LIGHT;

    /** Solar Constant (N/M**2). */
    double CONST_SOL_STELA = 0.45605E-05;
    /**
     * Critical prograde inclination from 4 - 5 &times; sin<sup>2</sup>i = 0
     * see "Fundamentals of Astrodynamics and Applications", 3rd Edition, D. A. Vallado, p.646
     */
    double CRITICAL_PROGRADE_INCLINATION = MathLib.asin(2. / MathLib.sqrt(5.));

    /**
     * Critical retrograde inclination from 4 - 5 &times; sin<sup>2</sup>i = 0
     * see "Fundamentals of Astrodynamics and Applications", 3rd Edition, D. A. Vallado, p.646
     */
    double CRITICAL_RETROGRADE_INCLINATION = FastMath.PI - CRITICAL_PROGRADE_INCLINATION;

    /** Gravitational constant (CODATA): 6.67384 &times; 10<sup>-11</sup> m<sup>3</sup>kg<sup>-1</sup>s<sup>-2</sup>. */
    double GRAVITATIONAL_CONSTANT = 6.67384E-11;

    /** OBELIX gravitational constant. */
    double CGU = 6.672E-11;

    /** UA from the 1992 Astronomical Almanac by P. Kenneth Seidelmann. */
    double SEIDELMANN_UA = 149597870000.0;

    /** UA from the 1992 Astronomical Almanac by P. Kenneth Seidelmann. */
    double CNES_STELA_UA = 1.49598022291E+11;

    /** CNES Stela reference equatorial radius. */
    double CNES_STELA_AE = 6378136.46;

    /** CNES Stela reference mu. */
    double CNES_STELA_MU = 398600441449820.000;

    // IERS92 data from CNES COMPAS_Base data set ...

    /** IERS92 light velocity in vacuum (meters per second) from CNES COMPAS_Base data set. */
    double IERS92_LIGHT_VELOCITY = 299792458.;

    /** IERS92 UA (m) from CNES COMPAS_Base data set. */
    double IERS92_UA = 150000000000.;

    /** IERS92 Earth standard gravitational parameter (m<sup>3</sup>s<sup>2</sup>) from CNES COMPAS_Base data set. */
    double IERS92_EARTH_GRAVITATIONAL_PARAMETER = 398600443104792.;

    /** IERS92 Earth standard gravitational parameter (m) from CNES COMPAS_Base data set. */
    double IERS92_EARTH_EQUATORIAL_RADIUS = 6378140.;

    /** IERS92 Earth flattening from CNES COMPAS_Base data set. */
    double IERS92_EARTH_FLATTENING = 0.0033536;

    /** IERS92 Earth rotation rate (rad/s) from CNES COMPAS_Base data set. */
    double IERS92_EARTH_ROTATION_RATE = 7.29211537319376E-05;

    /** IERS92 Earth J2 parameter from CNES COMPAS_Base data set. */
    double IERS92_EARTH_J2 = 0.00108263;

    /** IERS92 Sun standard gravitational parameter from CNES COMPAS_Base data set. */
    double IERS92_SUN_GRAVITATIONAL_PARAMETER = 1.32712443255261E+20;

    /** IERS92 Sun equatorial radius from CNES COMPAS_Base data set. */
    double IERS92_SUN_EQUATORIAL_RADIUS = 650000000.;

    /** IERS92 Sun flattening from CNES COMPAS_Base data set. */
    double IERS92_SUN_FLATTENING = 0.;

    /** IERS92 Sun rotation rate from CNES COMPAS_Base data set. */
    double IERS92_SUN_ROTATION_RATE = -1.13855098302775E-05;

    /** IERS92 Moon standard gravitational parameter from CNES COMPAS_Base data set. */
    double IERS92_MOON_GRAVITATIONAL_PARAMETER = 4902798867501.21;

    /** IERS92 Moon equatorial radius from CNES COMPAS_Base data set. */
    double IERS92_MOON_EQUATORIAL_RADIUS = 1737500.;

    /** IERS92 Moon flattening from CNES COMPAS_Base data set. */
    double IERS92_MOON_FLATTENING = 0.;

    /** IERS92 Moon rotation rate from CNES COMPAS_Base data set. */
    double IERS92_MOON_ROTATION_RATE = 2.86532965763744E-06;

    // UAI1994 data from CNES COMPAS_Base data set ...

    /** UAI1994 light velocity in vacuum (meters per second) from CNES COMPAS_Base data set. */
    double UAI1994_LIGHT_VELOCITY = 299792458.;

    /** UAI1994 UA (m) from CNES COMPAS_Base data set. */
    double UAI1994_UA = 149597871475.;

    /** UAI1994 precession rate (arcseconds/century) from CNES COMPAS_Base data set. */
    double UAI1994_PRECESSION_RATE = 5028.83;

    /** UAI1994 obliquity of the ecliptic (arcseconds) from CNES COMPAS_Base data set. */
    double UAI1994_OBLIQUITY = 84381.412;

    /** UAI1994 gravitational constant (m<sup>3</sup>kg<sup>-1</sup>s<sup>-2</sup>) from CNES COMPAS_Base data set. */
    double UAI1994_GRAVITATIONAL_CONSTANT = 6.67259E-11;

    /** UAI1994 solar radiation pressure coefficient from CNES COMPAS_Base data set. */
    double UAI1994_SOLAR_RADIATION_PRESSURE = 100000000.;

    /** UAI1994 Earth standard gravitational parameter from CNES COMPAS_Base data set. */
    double UAI1994_EARTH_GRAVITATIONAL_PARAMETER = 398600441500000.;

    /** UAI1994 Earth standard gravitational parameter from CNES COMPAS_Base data set. */
    double UAI1994_EARTH_EQUATORIAL_RADIUS = 6378136.55;

    /** UAI1994 Earth flattening from CNES COMPAS_Base data set. */
    double UAI1994_EARTH_FLATTENING = 0.00335281318;

    /** UAI1994 Earth rotation rate from CNES COMPAS_Base data set. */
    double UAI1994_EARTH_ROTATION_RATE = 7.292115E-05;

    /** UAI1994 Earth J2 parameter from CNES COMPAS_Base data set. */
    double UAI1994_EARTH_J2 = 0.00108263;

    /** UAI1994 Sun standard gravitational parameter from CNES COMPAS_Base data set. */
    double UAI1994_SUN_GRAVITATIONAL_PARAMETER = 1.32712440018E+20;

    /** UAI1994 Sun equatorial radius from CNES COMPAS_Base data set. */
    double UAI1994_SUN_EQUATORIAL_RADIUS = 650000000.;

    /** UAI1994 Sun flattening from CNES COMPAS_Base data set. */
    double UAI1994_SUN_FLATTENING = 0.;

    /** UAI1994 Sun rotation rate from CNES COMPAS_Base data set. */
    double UAI1994_SUN_ROTATION_RATE = -1.13855098302775E-05;

    /** UAI1994 Moon standard gravitational parameter from CNES COMPAS_Base data set. */
    double UAI1994_MOON_GRAVITATIONAL_PARAMETER = 4902801000000.;

    /** UAI1994 Moon equatorial radius from CNES COMPAS_Base data set. */
    double UAI1994_MOON_EQUATORIAL_RADIUS = 1737500;

    /** UAI1994 Moon flattening from CNES COMPAS_Base data set. */
    double UAI1994_MOON_FLATTENING = 0.;

    /** UAI1994 Moon rotation rate from CNES COMPAS_Base data set. */
    double UAI1994_MOON_ROTATION_RATE = 2.86532965763744e-06;

    // CHECKSTYLE: resume MagicNumber check
    //CHECKSTYLE: resume InterfaceIsType check
}
