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
 * @history creation version 1.0
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.11:DM:DM-3232:22/05/2023:[PATRIUS] Detection d'extrema dans la classe ExtremaGenericDetector
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS 
 * VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.11:DM:DM-3259:22/05/2023:[PATRIUS] Creer une interface StarConvexBodyShape
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3211:03/11/2022:[PATRIUS] Ajout de fonctionnalites aux PyramidalField
 * VERSION:4.9:DM:DM-3151:10/05/2022:[PATRIUS] Evolution de la casse VisibilityFromStationDetector
 * VERSION:4.9:DM:DM-3130:10/05/2022:[PATRIUS] Robustifier le calcul des phenomenes des CodedEventsLogger, ...
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3127:10/05/2022:[PATRIUS] Ajout de deux methodes resize a l'interface GeometricBodyShape ...
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2958:15/11/2021:[PATRIUS] calcul d'intersection a altitude non nulle pour l'interface BodyShape 
 * VERSION:4.8:DM:DM-2994:15/11/2021:[PATRIUS] Polynômes de Chebyshev pour l'interpolation et l'approximation
 * de fonctions 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.8:FA:FA-2941:15/11/2021:[PATRIUS] Correction anomalies suite a DM 2767 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.7:DM:DM-2649:18/05/2021: ajout d un getter parametrable TimeScalesFactory
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.6:DM:DM-2450:27/01/2021:[PATRIUS] moyennage au sens du modele Analytical2D 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.5:DM:DM-2299:27/05/2020:Implementation du propagateur analytique de Liu 
 * VERSION:4.5:DM:DM-2300:27/05/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear 
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * VERSION:4.4:DM:DM-2148:04/10/2019:[PATRIUS] Creations de parties mobiles dans un Assembly
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.4:DM:DM-2231:04/10/2019:[PATRIUS] Creation d'un cache dans les profils de vitesse angulaire
 * VERSION:4.4:DM:DM-2112:04/10/2019:[PATRIUS] Manoeuvres impulsionnelles sur increments orbitaux
 * VERSION:4.4:FA:FA-2258:04/10/2019:[PATRIUS] ecriture TLE et angles negatif
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:268:30/04/2015:Aero drag and lift model
 * VERSION::DM:525:22/04/2016: add new functionalities existing in LibKernel
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:571:12/04/2017:Add density partial derivative contribution in partial derivatives
 * VERSION::DM:849:20/03/2017:Implementation of DragCoefficientProvider with file reader
 * VERSION::FA:1323:13/11/2017:change log message
 * VERSION::FA:1176:28/11/2017:add error message
 * VERSION::FA:1449:15/03/2018:part can have either a Tank or a Mass property, not both
 * VERSION::FA:1868:31/10/2018: handle proper end of integration
 * VERSION::DM:1936:23/10/2018: add new methods to intervals classes
 * VERSION::DM:1489:23/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1948:15/11/2018:improvement of legs sequence design
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.utils.exception;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import fr.cnes.sirius.patrius.math.exception.util.Localizable;

/**
 * Enumeration for localized messages formats used in exceptions messages.
 * <p>
 * This class is roughly the same as the one in OREKIT. It adds PATRIUS library messages in the same
 * manner as OREKIT does. Users should see OREKIT documentation about localized messages format.
 * </p>
 *
 * @useSample <code>
 * ...
 * <br />
 * PatriusMessages myMessage = PatriusMessages.PDB_MY_MESSAGE;
 * <br />
 * System.out.println(myMessage.getLocalizedString(Locale.FRENCH));
 * <br />
 * ...
 * </code>
 *
 * @concurrency immutable
 *
 * @author Sylvain VRESK
 *
 * @version $Id: PatriusMessages.java 17785 2017-08-31 14:01:10Z bignon $
 *
 * @since 1.0
 *
 */
public enum PatriusMessages implements Localizable {

    // Add message hereafter
    /** A foo message for testing purpose. */
    PDB_FOO_MESSAGE("foo message for testing purpose"),
    /** Frame not inertial */
    PDB_NOT_INERTIAL_FRAME_SPICE("One of the frames ( {0} or {1} ) is not inertial. "
                               + "Non-inertial frames are not supported by this adaptation of SPICE."
                               + "Their utilisation implies the reading of non-SPK files, which is not"
                               + " supported for the moment."),
    /** Impossible to get here */
    PDB_IMPOSSIBLE_TO_GET_HERE_SPICE("Something is not working well. It is impossible to get here without "
                                   + "reading a non SPK file"),
    /** File not loaded */
    PDB_SPICE_KERNEL_NOT_LOADED("The file {0} has not been loaded. Before trying to use any SPICE file, "
                              + "please load it by using SpiceKernelManager.loadSpiceKernel(String filepath)"),
    /** File not found*/
    PDB_FILE_NOT_FOUND("File {0} cannot be found"),
    /** Ephemeris object not recognised */
    PDB_EPHEMERIS_OBJ_NOT_RECON("{0} is not a recognized name for an ephemeris object."),
    /** Frame not recognised */
    PDB_FRAME_NOT_RECOGNISED("The frame '{0}' is not recognised. If it is blank or there is a non-printable character"
                        + "there is probably an initialisation problem. If not, the frame is probably not supported"),
    /** Insuficient ephemeris data to compute state*/
    PDB_INSUFFICIENT_DATA_FOR_STATE("Insufficient ephemeris data has been loaded to compute the"
                                  + " state of {0} relative to {1} at the ephemeris epoch {2}."),
    /** Unnable to connect frames */
    PDB_UNNABLE_TO_CONNECT_FRAMES("There was no connection found between the frames requested"),
    /** Non-inertial frame not supported */
    PDB_NON_INERTIAL_FRAME("Non-inertial frame not supported. The reading of files of type different to SPK "
                         + "would be needed"),
    /** Reference frame ID not recognized*/
    PDB_UNKNOWN_FRAME_ID("The number {0} is not a recognized id-code for a reference frame."),
    /** Intertial ID not recognized*/
    PDB_UNKNOWN_INERTIAL_ID("A request has been made to obtain the transformation from inertial reference frame {0}"
                          + " to inertial reference frame {1}. Unfortunately {2} is not the id-code of a known "
                          + "inertial frame."),
    /** Spk type not supported*/
    PDB_SPK_TYPE_NOT_SUPPORTED("Only type 2 and type 3 SPK files are supported. Type was: {0}"),
    /** Number of coefficients is negative*/
    PDB_NCOF_SPK_RECORD_NEGATIVE("The input record coefficient count NCOF should be positive but was {0}"),
    /** SPK interval radius negative */
    PDB_SPK_INTERVAL_RADIUS_NEGATIVE("Interval radius bust be positive but was {0}"),
    /** No file found in the table */
    PDB_ANY_FILE_LOADED("Any SPK has been loaded so far. Before beginning a search, there must be at least 1 loaded"),
    /** Different size arrays not allowed. */
    PDB_NOT_SAME_SIZE("Arrays must have the same size"),
    /** Empty string not allowed. */
    PDB_EMPTY_STRING("Empty string not allowed"),
    /** Comment buffer too short*/
    PDB_COMMENT_BUFFER_TOO_SHORT("The lineSize defined ({0}), is too short."),
    /** CounterArray overflowed */
    PDB_COUNTER_ARRAY_OVERFLOW("A subsystem state counter overflowed."),
    /** CounterArray type not recognised. */
    PDB_WRONG_COUNTER_ARRAY_TYPE("The type {0} is not recognized as a possible type for CounterArray. "
                                 + "Only USER and SUBSYSTEM are allowed"),
    /** Count char exceeds*/
    PDB_COUNT_CHARS_EXCEEDS("count of comment characters exceeds the number of comment characters in the DAF"),
    /** Too many files in the list*/
    PDB_TOO_MANY_FILES("Too many files in the list"),
    /** End of comments not found*/
    PDB_EOC_NOT_FOUND("End of comments not found"),
    /** DAF not opened**/
    PDB_DAF_NOT_OPENED("There is not DAF opened to the handle : {0}"),
    /** FTP check failure*/
    PDB_FTP_FAILED("FTP check failed. File probably corrupted"),
    /** File cant't be read*/
    PDB_FILE_CANT_BE_READ("File cannot be read"),
    /** List completion method failed */
    PDB_LIST_COMPLETION_FAILED("Lists contain different number of elements. This should never happen."),
    /** Access method not supported*/
    PDB_WRONG_ACCESS_METHOD("Access method differs from READ. Actual access method is : {0}"),
    /** Negative address*/
    PDB_ILLEGAL_ADDRESS("Address must be >= 0 : Address searched : {0}"),
    /** Negative record*/
    PDB_ILLEGAL_RECORD("Record must be > 0 : Record searched : {0}"),
    /** Binary file format not supported*/
    PDB_BFF_NOT_SUPPORTED("Binary file format {0} is not supported"),
    /** Next array is first */
    PDB_FIRST_IS_NEXT("No array is current; the ''next'' array is the first array of DAF"),
    /** Previous array is last */
    PDB_LAST_IS_PREV("No array is current; the ''previous'' array is the last array of DAF"),
    /** No DAF being searched*/
    PDB_NO_DAF_SEARCHED("No DAF is being searched"),
    /** Failed to read descriptor record in DAF file */
    PDB_DESC_RECORD_FAILED("Attempt to read descriptor record {0} of DAF ''{1}'' failed; record was not found. "
            + "This condition may indicate a corrupted DAF."),
    /** Too many agents to notify*/
    PDB_TOO_MANY_AGENTS("Too many agents to notify"),
    /**Not enought bytes to read*/
    PDB_NOT_ENOUGH_BYTES("Not enough bytes to read are left"),
    /**Unable to locate file associated to handle */
    PDB_UNABLE_TO_LOCATE_FILE("Unable to locate file associated with handle {0}."
            + " Most likely the file has been closed"),
    /** File type not supported*/
    PDB_WRONG_TYPE("File type differs from SPK. Actual file type is : {0}"),
    /** Architecture type not supported*/
    PDB_WRONG_ARCHITECTURE("File architecture differs from DAF. Actual file architecture is : {0}"),
    /** No room left for another SPICE kernel */
    PDB_ROOM_SPICE_KERNEL("No room left for another SPICE kernel"),
    /** Invalid angle interval message. */
    PDB_INVALID_ANGLE_INTERVAL("Interval built with wrong arguments"),
    /** Angle outside interval message. */
    PDB_ANGLE_OUTSIDE_INTERVAL("The angle is outside the interval"),
    /** The vector's norm can't be zero. */
    PDB_ZERO_NORM("The vector's norm can't be zero"),
    /** No main Part. */
    PDB_NO_MAIN_PART("The vehicle must have a main part"),
    /** Already a main Part. */
    PDB_ALREADY_A_MAIN_PART("The vehicle already has a main part"),
    /** A part with this name already exists. */
    PDB_PART_NAME_EXISTS("A part with this name already exists"),
    /** No part with this name. */
    PDB_PART_DONT_EXIST("No part with this name"),
    /** The main part's frame must have a parent. */
    PDB_MAIN_FRAME_HAS_NO_PARENT("The main part's frame must have a parent"),
    /** The main part's frame must be the same as the SpacecraftState's definition frame. */
    PDB_BAD_PARENT_FRAME("The main part's frame must be the same as the SpacecraftState's definition frame"),
    /** A property of this type already exists in this part. */
    PDB_PROPERTY_ALREADY_EXIST("A property of this type already exists in this part"),
    /** Bad input length. */
    PDB_BAD_LENGTH("Bad input length"),
    /** This input value should be positive. */
    PDB_VALUE_SHOULD_BE_POSITIVE("This input value should be positive"),
    /** Error message for radiative model: no required radiative properties. */
    PDB_NO_RADIATIVE_MASS_PROPERTIES("The assembly has no radiative and mass properties"),
    /** Error message for aero model: no required aero properties. */
    PDB_NO_AERO_MASS_PROPERTIES("The assembly has no aero and mass properties"),
    /** Error message for aero drag and lift model: no required aero properties. */
    PDB_NO_AERO_GLOBAL_MASS_PROPERTIES("The assembly has no aero global and mass properties"),
    /** Error message for masking computations : no required geometry property. */
    PDB_NO_GEOMETRY_PROPERTY("One of the given parts has non GEOMETRY property"),
    /** Error message for RF link budget model : no required RF property. */
    PDB_NO_RF_PROPERTY("The part must have a RF property"),
    /**
     * Error message for extrema sight axis detector using an assembly and a part name : no required
     * SENSOR property.
     */
    PDB_NO_SENSOR_PROPERTY("The part must have a SENSOR property"),
    /** Error message for radiative model: redundant radiative properties. */
    PDB_REDUNDANT_RADIATIVE_PROPERTIES("The part must have only one radiative property (SPHERE or FACET)"),
    /** Error message for aero model: redundant aero properties. */
    PDB_REDUNDANT_AERO_PROPERTIES("The part must have only one aerodynamic property (SPHERE or FACET)"),
    /** Error message for mass model: redundant mass/tank properties. */
    PDB_REDUNDANT_MASS_TANK_PROP("The part can have either a Tank property or a Mass property, not both"),
    /** Error message for radiative model: methods do not have sense. */
    PDB_UNSUPPORTED_OPERATION(
            "This method has no sense from a model point of view: radiative properties are already defined "
                    + "for the assembly's parts"),
    /** Too few directions to create a field : must be at least 3. */
    PDB_TOO_FEW_DIRECTIONS("Too few directions to create a field : must be at least 3"),
    /** Two consecutive directions are too close to create the zone. */
    PDB_CLOSE_CONSECUTIVE_DIRECTIONS("Two consecutive directions are too close to create the zone"),
    /** Two consecutive directions are too close to create the zone. */
    PDB_CROSSING_ARCS("Two arcs between consecutive points are crossing : "
            + "points number({0} - {1}) and ({2} - {3}). Unable to create the zone."),
    /** Invalid date specified. */
    PDB_INVALID_DATE("Invalid date"),
    /** The frame must be pseudo inertial. */
    PDB_NOT_INERTIAL_FRAME("The frame must be pseudo inertial"),
    /** The absolute temperature can't be zero or lower. */
    PDB_NULL_TEMPERATURE("The absolute temperature can't be zero or lower"),
    /** Invalid vectors for sector field of view creation. */
    PDB_INVALID_VECTORS_FOR_SECTOR_FIELD("the angle from the pole vector to V1 must be greater than the angle "
            + "from the pole vector to V2, none of them can aligned with or opposite to the pole vector."),
    /** unsupported parameter. */
    PDB_UNSUPPORTED_PARAMETER_1_2_3("unsupported parameter name {0}: supported names {1}, {2}, {3}"),
    /** unsupported parameter. */
    PDB_UNSUPPORTED_PARAMETER_5("unsupported parameter name {0}: supported names {1}, {2}, {3}, {4}, {5}"),
    /** unsupported parameter. */
    PDB_UNSUPPORTED_PARAMETER_1("unsupported parameter name {0}: supported names {1}"),
    /** unsupported parameter. */
    PDB_UNSUPPORTED_ARRAY_DIMENSION("unsupported two-points array for spline interpolation"),
    /** iono invalid date. */
    PDB_IONO_DATE_OUT_OF_FILE("The date is outside of the range covered by the file"),
    /** USK file error. */
    PDB_USK_FILE_ERROR("USK file error"),
    /** Osculating to mean conversion convergence error. */
    PDB_OSC_MEAN_CVG_ERROR("Osculating to mean conversion convergence error"),
    /** Unsupported degree for Stela force model perturbations computation. */
    PDB_UNSUPPORTED_DEGREE_FOR_PERTURBATIONS("Unsopported degree for Stela force model perturbations"),
    /** Unsupported degree for Stela force model partial derivatives computation. */
    PDB_UNSUPPORTED_DEGREE_FOR_PARTIAL_DERIVATIVES("Unsopported degree for Stela force model partial derivatives"),
    /** Unsupported degree for Stela force model short periods computation. */
    PDB_UNSUPPORTED_DEGREE_FOR_SHORT_PERIODS("Unsopported degree for Stela force model short periods"),
    /** The geodetic altitude and latitude computation failed. */
    PDB_GEODETIC_PARAMETERS_COMPUTATION_FAILED("Geodetic altitude and latitude computation failed"),
    /** The Simpson rule computation failed. */
    PDB_SIMPSON_RULE_FAILED("The Simpson rule computation failed"),
    /** The squaring computation failed. */
    PDB_SQUARING_FAILED("The squaring computation failed"),
    /** For Stela purposes, reentry did not occur after n steps. */
    PDB_PROPAGATION_NO_REENTRY(
            "Problem in propagation, Spacecraft did not reenter {0} steps after the first correction"),
    /** For multi propagation purposes, added states date is different from others states date. */
    PDB_MULTI_SAT_DATE_MISMATCH("The added state date {0} does not match previous states date {1}"),
    /** For multi propagation purposes, the input satId is not part of the initial states map. */
    PDB_UNDEFINED_STATE_ID("The input sat ID {0} does not correspond with an initial state"),
    /** For multi propagation purposes, the input sat Id is null or empty. */
    PDB_NULL_STATE_ID("The input sat ID is null"),
    /** For multi propagation purposes, the user try to add a state with a name already added. */
    PDB_SAT_ID_ALREADY_USED("The input sat ID is already used"),
    /** For multi propagation purposes, the added tolerance is not of dimension-6. */
    PDB_ORBIT_TOLERENCE_LENGTH("The length of the input orbit tolerance is different from 6"),
    /** northing value out of range. */
    PDB_NORTHING_OUT_OF_RANGE("northing value out of range : maximum value is {0} but actual value is {1}"),
    /** easting value out of range. */
    PDB_EASTING_OUT_OF_RANGE("easting value out of range : maximum value is {0} but actual value is {1}"),
    /** points too close, cannot define azimuth. */
    PDB_POINTS_TOO_CLOSE("geodetic points are too close, azimuth cannot be defined"),
    /** the latitude is close or over -/+ 90°. */
    PDB_LATITUDE_CLOSE_90("the latitude is close or over -/+ 90°"),
    /** the latitude is out of range. */
    PDB_LATITUDE_OUT_OF_RANGE("The latitude to convert ({0} degrees) is out of range (i.e : > {1} degrees)"),
    /**
     * Error message for the vehicle : no required drag and lift if main shape is not a sphere and
     * has solar panels.
     */
    PDB_NO_AERO_PROP("Inconsistent vehicle for aero properties : main shape is not a sphere and the"
            + "vehicule has solar panels"),
    /**
     * Error message for derivatives computation wrt position in the AeroModel : computation is not
     * possible if derivatives wrt velocity are not computed (there are involved in derivatives wrt
     * position).
     */
    PDB_AERO_DERIVATIVES_COMPUTATION_ERROR("Partial derivatives wrt position for AeroModel can't be computed"
            + "since derivatives wrt velocity are deactivated"),
    /** Wrong interpolated drag coefficients file format (wrong number of columns). */
    PDB_WRONG_COLUMNS_NUMBER("Provided interpolated drag coefficients file has wrong number of columns ({0} instead "
            + "of {1} + {2})"),
    /** Wrong interpolated drag coefficients file format (missing lines). */
    PDB_MISSING_LINES("Provided interpolated drag coefficients file has some lines missing"),
    /** Wrong interpolated drag coefficients file format (missing lines). */
    PDB_WRONG_LINES("Provided interpolated drag coefficients file line {0} has wrong format"),
    /** The line has illegal parameters. */
    ILLEGAL_LINE("the line has illegal parameters"),
    /** The vector's norm can't be zero. */
    NULL_VECTOR("the vector's norm can't be zero"),
    /** Message. */
    ACCELERATION_NOT_INITIALIZED("The acceleration is not initialized"),
    /** out of range latitude : must be between -PI/2 and PI/2. */
    OUT_OF_RANGE_LATITUDE("out of range latitude : must be between -PI/2 and PI/2"),
    /** The date is not a transition date of the attitude sequence. */
    NO_TRANSITION_DATE("the date is not a transition date of the attitude sequence"),
    /** The cosine of latitude elementary surface is equal to zero. */
    ZERO_COSLAT_SURFACE("The cosine of latitude elementary surface is equal to zero"),
    /** Unsupported transition points parameter name. */
    UNKNOWN_TRANSITION_PARAMETER("unsupported transition points parameter name: supported names {0}, {1}, {2}"),
    /** Not implemented yet. */
    NOT_IMPLEMENTED("Not implemented yet"),
    /** Main wave missing. */
    MAIN_WAVE_MISSING("One of the main waves is has not been found in the file : can't compute admittance"),
    /** unable to compute ground velocity direction. */
    UNABLE_TO_COMPUTE_GROUND_VELOCITY_DIRECTION("unable to compute ground velocity direction"),
    /** Attitude law already in the sequence. */
    ATTITUDE_LAW_ALREADY_IN_THE_SEQUENCE("The attitude law with the selected code is already in the sequence"),
    /** Message. */
    ATTITUDE_DYNAMICS_ELEMENTS_NOT_AVAILABLE("Unable to compute spin derivatives for attitude law {0}"),
    /** Message. */
    ATTITUDE_SPIN_DERIVATIVES_NOT_AVAILABLE("Unable to compute spin derivative for attitude law {0}"),
    /** Message. */
    INTERNAL_ERROR("internal error, contact maintenance at {0}"),
    /** Message. */
    ALTITUDE_BELOW_ALLOWED_THRESHOLD("altitude ({0} m) is below the {1} m allowed threshold"),
    /** Message. */
    TRAJECTORY_INSIDE_BRILLOUIN_SPHERE("trajectory inside the Brillouin sphere (r = {0})"),
    /** Message. */
    ALMOST_EQUATORIAL_ORBIT("almost equatorial orbit (i = {0} degrees)"),
    /** Message. */
    ALMOST_CRITICALLY_INCLINED_ORBIT("almost critically inclined orbit (i = {0} degrees)"),
    /** Message. */
    UNABLE_TO_COMPUTE_ECKSTEIN_HECHLER_MEAN_PARAMETERS(
            "unable to compute Eckstein-Hechler mean parameters after {0} iterations. Use method setThreshold() "
                    + "to change convergence threshold."),
    /** Message. */
    UNABLE_TO_COMPUTE_LYDDANE_MEAN_PARAMETERS(
            "unable to compute Lyddane mean parameters after {0} iterations. Use method setThreshold() to "
                    + "change convergence threshold."),
    /** Message. */
    NULL_PARENT_FOR_FRAME("null parent for frame {0}"),
    /** Message. */
    FRAME_ANCESTOR_OF_BOTH_FRAMES("frame {0} is an ancestor of both frames {1} and {2}"),
    /** Message. */
    FRAME_ANCESTOR_OF_NEITHER_FRAME("frame {0} is an ancestor of neither frame {1} nor {2}"),
    /** Message. */
    UNSUPPORTED_LOCAL_ORBITAL_FRAME("unsupported local orbital frame, supported types: {0} and {1}"),
    /** Message. */
    NON_PSEUDO_INERTIAL_FRAME_NOT_SUITABLE_FOR_DEFINING_ORBITS(
            "non pseudo-inertial frame \"{0}\" is not suitable for defining orbits"),
    /** Message. */
    DATA_ROOT_DIRECTORY_DOESN_NOT_EXISTS("data root directory {0} does not exist"),
    /** Message. */
    NOT_A_DIRECTORY("{0} is not a directory"),
    /** Message. */
    NEITHER_DIRECTORY_NOR_ZIP_OR_JAR("{0} is neither a directory nor a zip/jar archive file"),
    /** Message. */
    UNABLE_TO_FIND_RESOURCE("unable to find resource {0} in classpath"),
    /** Message. */
    NO_EARTH_ORIENTATION_PARAMETERS_LOADED("no Earth Orientation Parameters loaded"),
    /** Message. */
    MISSING_EARTH_ORIENTATION_PARAMETERS_BETWEEN_DATES("missing Earth Orientation Parameters between {0} and {1}"),
    /** Message. */
    NOT_A_SUPPORTED_IERS_DATA_FILE("file {0} is not a supported IERS data file"),
    /** Message. */
    INCONSISTENT_DATES_IN_IERS_FILE("inconsistent dates in IERS file {0}: {1}-{2}-{3} and MJD {4}"),
    /** Message. */
    UNEXPECTED_DATA_AFTER_LINE_IN_FILE("unexpected data after line {0} in file {1}: {2}"),
    /** Message. */
    NON_CHRONOLOGICAL_DATES_IN_FILE("non-chronological dates in file {0}, line {1}"),
    /** Message. */
    NO_IERS_UTC_TAI_HISTORY_DATA_LOADED("no IERS UTC-TAI history data loaded"),
    /** Message. */
    NO_ENTRIES_IN_IERS_UTC_TAI_HISTORY_FILE("no entries found in IERS UTC-TAI history file {0}"),
    /** Message. */
    MISSING_SERIE_J_IN_FILE("missing serie j = {0} in file {1} (line {2})"),
    /** Message. */
    UNEXPECTED_END_OF_FILE_AFTER_LINE("unexpected end of file {0} (after line {1})"),
    /** Message. */
    UNABLE_TO_PARSE_LINE_IN_FILE("unable to parse line {0} of file {1}:\n{2}"),
    /** Message. */
    UNABLE_TO_FIND_FILE("unable to find file {0}"),
    /** Message. */
    POSITIVE_FLOW_RATE("positive flow rate (q: {0})"),
    /** Message. */
    NO_GRAVITY_FIELD_DATA_LOADED("no gravity field data loaded"),
    /** Message. */
    POTENTIAL_ARRAYS_SIZES_MISMATCH("potential arrays sizes mismatch (C: {0}x{1}, S: {2}x{3})"),
    /** Message. */
    POLAR_TRAJECTORY("polar trajectory (distance to polar axis: {0})"),
    /** Message. */
    UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER("unexpected format error for file {0} with loader {1}"),
    /** Message. */
    TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD("too large degree (n = {0}, potential maximal degree is {1})"),
    /** Message. */
    TOO_LARGE_ORDER_FOR_GRAVITY_FIELD("too large order (m = {0}, potential maximal order is {1})"),
    /** Message. */
    TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD_PD(
            "too large degree for partial derivatives (n = {0}, potential maximal degree is {1})"),
    /** Message. */
    TOO_LARGE_ORDER_FOR_GRAVITY_FIELD_PD(
            "too large order for partial derivatives (m = {0}, potential maximal order is {1})"),
    /** Message. */
    NO_TLE_FOR_OBJECT("no TLE data available for object {0}"),
    /** Message. */
    NO_TLE_FOR_LAUNCH_YEAR_NUMBER_PIECE(
            "no TLE data available for launch year {0}, launch number {1}, launch piece {2}"),
    /** Message. */
    NOT_TLE_LINES("lines {0} and {1} are not TLE lines:\n{0}: \"{2}\"\n{1}: \"{3}\""),
    /** Message. */
    MISSING_SECOND_TLE_LINE("expected a second TLE line after line {0}:\n{0}: \"{1}\""),
    /** Message. */
    TLE_LINES_DO_NOT_REFER_TO_SAME_OBJECT("TLE lines do not refer to the same object:\n{0}\n{1}"),
    /** Message. */
    TLE_CHECKSUM_ERROR("wrong checksum of TLE line {0}, expected {1} but got {2} ({3})"),
    /** Message. */
    NO_TLE_DATA_AVAILABLE("no TLE data available"),
    /** Message. */
    NOT_POSITIVE_MASS("mass is not positive: {0} kg"),
    /** Message. */
    TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL("too large eccentricity for propagation model: e = {0}"),
    /** Message. */
    NO_SOLAR_ACTIVITY_AT_DATE("no solar activity available at {0}, data available only in range [{1}, {2}]"),
    /** Message. */
    NON_EXISTENT_MONTH("non-existent month {0}"),
    /** Message. */
    NON_EXISTENT_YEAR_MONTH_DAY("non-existent date {0}-{1}-{2}"),
    /** Message. */
    NON_EXISTENT_WEEK_DATE("non-existent week date {0}-W{1}-{2}"),
    /** Message. */
    NON_EXISTENT_DATE("non-existent date {0}"),
    /** Message. */
    NON_EXISTENT_DAY_NUMBER_IN_YEAR("no day number {0} in year {1}"),
    /** Message. */
    NON_EXISTENT_HMS_TIME("non-existent time {0}:{1}:{2}"),
    /** Message. */
    NON_EXISTENT_TIME("non-existent time {0}"),
    /** Message. */
    OUT_OF_RANGE_SECONDS_NUMBER("out of range seconds number: {0}"),
    /** Message. */
    NEGATIVE_PRECISION("negative precision: {0}"),
    /** Message. */
    ANGLE_TYPE_NOT_SUPPORTED("angle type not supported, supported angles: {0}, {1} and {2}"),
    /** Message. */
    SATELLITE_COLLIDED_WITH_TARGET("satellite collided with target"),
    /** Message. */
    ATTITUDE_POINTING_LAW_DOES_NOT_POINT_TO_GROUND("attitude pointing law misses ground"),
    /** Message. */
    ORBIT_AND_ATTITUDE_DATES_MISMATCH("orbit date ({0}) does not match attitude date ({1})"),
    /** Message. */
    FRAMES_MISMATCH("frame {0} does not match frame {1}"),
    /** Message. */
    ATTITUDES_MISMATCH("attitude attribute does not match attitude from additional states map"),
    /** Message. */
    ADD_STATES_MASS_MISMATCH("names in the part names list of mass model are not included in the additional "
            + "states map"),
    /** Message. */
    INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION("initial state not specified for orbit propagation"),
    /** Message. */
    ODE_INTEGRATOR_NOT_SET_FOR_ORBIT_PROPAGATION("ODE integrator not set for orbit propagation"),
    /** Message. */
    PROPAGATOR_NOT_IN_EPHEMERIS_GENERATION_MODE("propagator is not in ephemeris generation mode"),
    /** Message. */
    EVENT_DATE_TOO_CLOSE(
            "event date {0}, greater than {1} minus {3} seconds and smaller than {2} plus {3} seconds, cannot "
                    + "be added"),
    /** Message. */
    UNABLE_TO_READ_JPL_HEADER("unable to read header record from JPL ephemerides binary file {0}"),
    /** Message. */
    INCONSISTENT_ASTRONOMICAL_UNIT_IN_FILES(
            "inconsistent values of astronomical unit in JPL ephemerides files: ({0} and {1})"),
    /** Message. */
    INCONSISTENT_EARTH_MOON_RATIO_IN_FILES(
            "inconsistent values of Earth/Moon mass ratio in JPL ephemerides files: ({0} and {1})"),
    /** Message. */
    NO_DATA_LOADED_FOR_CELESTIAL_BODY("no data loaded for celestial body {0}"),
    /** Message. */
    NOT_A_JPL_EPHEMERIDES_BINARY_FILE("file {0} is not a JPL ephemerides binary file"),
    /** Message. */
    NOT_A_MARSHALL_SOLAR_ACTIVITY_FUTURE_ESTIMATION_FILE(
            "file {0} is not a Marshall Solar Activity Future Estimation (MSAFE) file"),
    /** Message. */
    NO_JPL_EPHEMERIDES_BINARY_FILES_FOUND("no JPL ephemerides binary files found"),
    /** Message. */
    OUT_OF_RANGE_BODY_EPHEMERIDES_DATE("out of range date for {0} ephemerides: {1}"),
    /** Message. */
    OUT_OF_RANGE_EPHEMERIDES_DATE("out of range date for ephemerides: {0}, [{1}, {2}]"),
    /** Message. */
    UNEXPECTED_TWO_ELEVATION_VALUES_FOR_ONE_AZIMUTH(
            "unexpected two elevation values: {0} and {1}, for one azimuth: {2}"),
    /** Message. */
    UNKNOWN_PARAMETER("unknown parameter {0}"),
    /** Message. */
    UNSUPPORTED_PARAMETER_1_2("unsupported parameter name {0}: supported names {1}, {2}"),
    /** Message. */
    UNKNOWN_ADDITIONAL_EQUATION("unknown additional equation \"{0}\""),
    /** Message. */
    UNKNOWN_ADDITIONAL_STATE("unknown additional state \"{0}\""),
    /** Message. */
    WRONG_CORRESPONDENCE_STATES_EQUATIONS("wrong correspondence between additional states and additional equations"),
    /** Message. */
    UNKNOWN_MONTH("unknown month \"{0}\""),
    /** Message. */
    STATE_JACOBIAN_NOT_INITIALIZED("state Jacobian has not been initialized yet"),
    /** Message. */
    STATE_JACOBIAN_SHOULD_BE_6X6("state Jacobian is a {0}x{1} matrix, it should be a 6x6 matrix"),
    /** Message. */
    STATE_AND_PARAMETERS_JACOBIANS_ROWS_MISMATCH("state Jacobian has {0} rows but parameters Jacobian has {1} rows"),
    /** Message. */
    INITIAL_MATRIX_AND_PARAMETERS_NUMBER_MISMATCH(
            "initial Jacobian matrix has {0} columns, but {1} parameters have been selected"),
    /** Message. */
    ORBIT_A_E_MISMATCH_WITH_CONIC_TYPE(
            "orbit should be either elliptic with a > 0 and e < 1 or hyperbolic with a < 0 and e > 1, a = {0}, "
                    + "e = {1}"),
    /** Message. */
    ORBIT_ANOMALY_OUT_OF_HYPERBOLIC_RANGE("true anomaly {0} out of hyperbolic range (e = {1}, {2} < v < {3})"),
    /** Message. */
    HYPERBOLIC_ORBIT_NOT_HANDLED_AS("hyperbolic orbits cannot be handled as {0} instances"),
    /** Message. */
    CCSDS_DATE_INVALID_PREAMBLE_FIELD("invalid preamble field in CCSDS date: {0}"),
    /** Message. */
    CCSDS_DATE_INVALID_LENGTH_TIME_FIELD("invalid time field length in CCSDS date: {0}, expected {1}"),
    /** Message. */
    CCSDS_DATE_MISSING_AGENCY_EPOCH("missing agency epoch in CCSDS date"),
    /** Message. */
    CCSDS_ORBIT_DATA_MESSAGE_UNEXPECTED_UNIT("unexpected unit \"{0}\" in CCSDS Orbit Data Message, expected \"{1}\""),
    /** Message. */
    CCSDS_ORBIT_DATA_MESSAGE_UNSUPPORTED_FRAME("unsupported frame \"{0}\" in CCSDS Orbit Data Message"),
    /** Message. */
    CCSDS_ORBIT_DATA_MESSAGE_UNEXPECTED_LOF_FRAME(
            "local orbital frame \"{0}\" cannot be used for orbital parameters in CCSDS Orbit Data Message"),
    /** Message. */
    CCSDS_ORBIT_DATA_MESSAGE_UNSUPPORTED_TIME_SYSTEM("unsupported time system \"{0}\" in CCSDS Orbit Data Message"),
    /** Message. */
    CCSDS_ORBIT_DATA_MESSAGE_MISSING_DATE("missing date in CCSDS Orbit Data Message"),
    /** Message. */
    CCSDS_ORBIT_DATA_MESSAGE_MISSING_FRAME("missing frame in CCSDS Orbit Data Message"),
    /** Message. */
    CCSDS_ORBIT_DATA_MESSAGE_UNKNOWN_FRAME("unknown frame {0} in CCSDS Orbit Data Message"),
    /** Message. */
    CCSDS_ORBIT_DATA_MESSAGE_MISSING_GM("missing gravitational coefficient in CCSDS Orbit Data Message"),
    /** Message. */
    CCSDS_ORBIT_DATA_MESSAGE_MISSING_POSITION("missing position in CCSDS Orbit Data Message"),
    /** Message. */
    CCSDS_ORBIT_DATA_MESSAGE_MISSING_VELOCITY("missing velocity in CCSDS Orbit Data Message"),
    /** Message. */
    CCSDS_ORBIT_DATA_MESSAGE_MISSING_ORBIT("missing orbit in CCSDS Orbit Data Message"),
    /** Message. */
    CCSDS_ORBIT_DATA_MESSAGE_MISSING_COVARIANCE("missing covariance in CCSDS Orbit Data Message"),
    /** Message. */
    ADDITIONAL_STATE_NAME_RESERVED("name \"{0}\" is reserved for attitude additional states"),
    /** Message. */
    ADDITIONAL_EQUATION_NAME_ALREADY_IN_USE("name \"{0}\" is already used for an additional equation"),
    /** Message. */
    ADDITIONAL_STATE_WRONG_TOLERANCES_SIZE("tolerances array size differs from additional states vector size"),
    /** Message. */
    NON_RESETABLE_STATE("reset state not allowed {0}"),
    /** Message. */
    JACOBIAN_UNDEFINED("the jacobian matrix cannot be computed"),
    /** Message. */
    AZIMUTH_UNDEFINED("satellite passage near to the zenith"),
    /** Message. */
    CARDAN_MOUNTING_UNDEFINED("low satellite passage, near to the X axis"),
    /** Message. */
    POSITION_PARALLEL_TO_VELOCITY(
            "the position is parallel to the velocity, impossible to compute the normal to the trajectory plane "
                    + "vector."),
    /** Message. */
    OUT_OF_RANGE_DATE_FOR_ATTITUDE_LAW("the date is outside the interval of validity of the attitude law"),
    /** Message. */
    AT_LEAST_TWO_ATTITUDES_NEEDED("at least two attitudes are needed to interpolate"),
    /** Message. */
    INTERVAL_MUST_BE_INCLUDED("the time interval must be included in the one of this law"),
    /** Message. */
    INTERVAL_MUST_INCLUDE("the provided validity interval must include the one of the timeline"),
    /** Message. */
    ATTITUDE_LAW_SEQUENCE_EMPTY("the sequence of attitude laws is empty"),
    /** Message. */
    ATTITUDE_LAW_NOT_IN_THE_SEQUENCE("the attitude law in input is not a element of the attitude laws sequence"),
    /** Message. */
    ATTITUDE_LAW_AT_EDGE_SEQUENCE("the attitude law in input is the starting (ending) element of the sequence, "
            + "therefore there is no previous " + "(next) element"),
    /** Message. */
    UNCOMPUTED_SLEW("the slew has not been computed."),
    /** Message. */
    UNSUPPORTED_SLOPE_SELECTION_TYPE("the event type can only be INCREASING, DECREASING or INCREASING_DECREASING"),
    /** Message. */
    NOT_POSITIVE_DISTANCE("the distance is not positive."),
    /** Message. */
    NOT_CONTIGUOUS_LEG("the new leg is not contiguous to the existing sequence."),
    /** Message. */
    INVALID_INTERVAL_OF_VALIDITY("the intervals of validity of the two logs to merge are different."),
    /** Message. */
    DSST_NEWCOMB_OPERATORS_COMPUTATION("Cannot compute Newcomb operators for sigma > rho ({0} > {1})"),
    /** Message. */
    DSST_VMSN_COEFFICIENT_ERROR_MS("Cannot compute the Vmsn coefficient with m > s ({0} > {1})"),
    /** Message. */
    DSST_SPR_SHADOW_INCONSISTENT("inconsistent shadow computation: entry = {0} whereas exit = {1}"),
    /** Message. */
    DSST_ECC_NO_NUMERICAL_AVERAGING_METHOD(
            "The current orbit has an eccentricity ({0} > 0.5). DSST needs an unimplemented time dependent numerical "
                    + "method to compute the averaged rates"),
    /** Message. */
    SP3_UNSUPPORTED_VERSION("unsupported sp3 file version {0}"),
    /** Message. */
    SP3_UNSUPPORTED_TIMESYSTEM("unsupported time system {0}"),
    /** Message. */
    SP3_UNEXPECTED_END_OF_FILE("unexpected end of sp3 file (after line {0})"),
    /** Message. */
    NON_EXISTENT_GEOMAGNETIC_MODEL("non-existent geomagnetic model {0} for year {1}"),
    /** Message. */
    UNSUPPORTED_TIME_TRANSFORM(
            "geomagnetic model {0} with epoch {1} does not support time transformation, no secular variation "
                    + "coefficients defined"),
    /** Message. */
    OUT_OF_RANGE_TIME_TRANSFORM(
            "time transformation of geomagnetic model {0} with epoch {1} is outside its validity range: {2} "
                    + "!= [{3}, {4}]"),
    /** Message. */
    NOT_ENOUGH_CACHED_NEIGHBORS("too small number of cached neighbors: {0} (must be at least {1})"),
    /** Message. */
    NO_CACHED_ENTRIES("no cached entries"),
    /** Message. */
    NON_CHRONOLOGICALLY_SORTED_ENTRIES("generated entries not sorted: {0} > {1}"),
    /** Message. */
    NO_DATA_GENERATED("no data generated generated around date: {0}"),
    /** Message. */
    UNABLE_TO_GENERATE_NEW_DATA_BEFORE("unable to generate new data before {0}"),
    /** Message. */
    UNABLE_TO_GENERATE_NEW_DATA_AFTER("unable to generate new data after {0}"),
    /** Message. */
    DUPLICATED_ABSCISSA("abscissa {0} is duplicated"),
    /** Message. */
    EMPTY_INTERPOLATION_SAMPLE("sample for interpolation is empty"),
    /** Message. */
    UNSUPPORTED_METHOD("unsupported method"),
    /** Message. */
    NO_OCEAN_TIDES_COEFFICIENTS_FILES_LOADED("no ocean tides data file loaded"),
    /** Message. */
    NO_SOLAR_ACTIVITY_FILE_LOADED("no solar activity data file loaded"),
    /** Message. */
    INCORRECT_LENGTH_FOR_GEOMAG_COEFFICIENTS_ARRAY("incorrect length for ap or kp coefficients"),
    /** Message. */
    ILLEGAL_VALUE_FOR_GEOMAG_COEFFICIENT("illegal value for geomag coefficient"),
    /** Message. */
    FRAMES_FACTORY_LOCKED("Frames configuration is locked, unlock to change"),
    /** Message. */
    INVALID_ARRAY_LENGTH("Invalid array length : expecting {0} got {1}"),
    /** Message. */
    UNSUPPORTED_ROTATION_ORDER("Not supported rotation order"),
    /** Message. */
    OUT_OF_RANGE_ORDER("Out of range order: supported orders {1}, {2}, {3}, {4}"),
    /** Message. */
    OUT_OF_RANGE_ORDER_FOR_DERIVATIVES("Out of range order for derivatives computation"),
    /** Message. */
    SPIN_DERIVATIVES_ARE_NOT_AVAILABLE("Spin derivatives are not available for this attitude"),
    /** Message. */
    NO_VARIABLE_MASS_MODEL_FOUND("No variable mass model found : {0}"),
    /** Message. */
    MANEUVER_AMPLITUDE_EXCEEDS_FIXED_MAXIMUM_VALUE("The rotation amplitude exceeds the limit value theta max"),
    /** Message. */
    INVALID_SAMPLING_STEP("The sampling step should be a multiple of the computation step"),
    /** Message. */
    ATTITUDE_PROVIDER_ALREADY_DEFINED("An attitude provider is already defined for this attitude"),
    /** Message. */
    ATTITUDE_ADD_EQ_ALREADY_DEFINED("An additional equation is already defined for this attitude"),
    /** Message. */
    NO_ATTITUDE_DEFINED("No attitude defined"),
    /** Message. */
    NO_ATTITUDE_EVENTS_DEFINED("No attitude for events computation defined"),
    /** Message. */
    SINGLE_ATTITUDE_TREATMENT_EXPECTED("The requested action does not correspond with a single attitude treatment"),
    /** Message. */
    TWO_ATTITUDES_TREATMENT_EXPECTED(
            "The requested action does not correspond with an attitude treatment with two attitudes"),
    /** Message. */
    NO_MASS_INFOS_DEFINED("No mass informations defined"),
    /** Message. */
    LISTS_OF_ADD_STATES_MISMATCH("The additional states lists do not contain the same elements"),
    /** Message. */
    INTERPOLATION_ATTITUDES_MISMATCH("The states for interpolation do not contain the same attitude elements"),
    /** Message. */
    OUT_OF_RANGE_POLYNOMIAL_ORDER("Out of range polynomial interpolation order: supported orders {0}, {1}, {2}"),
    /** Message. */
    DATE_OUTSIDE_INTERVAL("The requested date is not in the interpolation interval"),
    /** Message. */
    DATE_OUTSIDE_LEGS_SEQUENCE_INTERVAL("The requested date : {0} is not in the legs sequence interval : {1}"),
    /** Message. */
    DATE_OUTSIDE_ATTITUDE_SEQUENCE("The requested date : {0} is not in the attitude sequence"),
    /** Message. */
    NOT_ENOUGH_INTERPOLATION_POINTS("There is {0} points while there should be at least {1} entries"),
    /** Message. */
    ODD_INTERPOLATION_ORDER("The interpolation order should be even"),
    /** Message. */
    WRONG_INTERPOLATION_ORDER("The interpolation order should be superior or equal to 2"),
    /** Message. */
    TWICE_THE_SAME_KEY("The array contains twice the same key"),
    /** Message. */
    UNEXPECTED_TWO_STATES_FOR_ONE_DATE("Unexpected two spacecraft states for one date: {0}"),
    /** Message. */
    UNEXPECTED_ALTITUDE_RANGE("US76 altitude range is 0 to 1000 km"),
    /** Message. */
    WRONG_INVERSE_TRIGONOMETRIC_FUNCTION_ARGUMENT("The inverse trigonometric function is undefined for this argument"),
    /** Message. */
    WRONG_STANDARD("unsupported tide standard"),
    /** Message. */
    SPACECRAFTFRAME_NOT_SUPPORTED("The SpacecraftFrame is not supported"),
    /** Message. */
    STELA_INTEGRATION_FRAME_NOT_SUPPORTED("The integration frame is not supported"),
    /** Message. */
    STELA_REFERENCE_SYSTEM_NOT_SUPPORTED("The reference system is not supported"),
    /** Message. */
    EVEN_SQUARING_POINTS("The number of squaring points must be odd"),
    /** Message. */
    UNDEFINED_DIRECTION("Direction {0} is not defined in case of {1}"),
    /** Message. */
    UNDEFINED_RADIUS("Radius is undefined (negative area)"),
    /** Message. */
    UNEXPECTED_ATMOSPHERE_MODEL("Harris Priester atmosphere model does not support speed of sound computation"),
    /** Message. */
    MONO_MULTI_DETECTOR("The detector used does not correspond with the propagation type (mono or multi)"),
    /** Message. */
    ATTITUDE_FORCES_NULL(
            "The attitude for forces computation is null, an attitude for events computation could not be added"),
    /** Message. */
    ATTITUDE_PROVIDER_FORCES_NULL("The attitude provider for forces computation is null, "
            + "an attitude provider for events computation could not be added"),
    /** Message. */
    OUT_OF_RANGE_DERIVATION_ORDER("Derivation order {0} is out of range"),
    /** Message. */
    NOT_ENOUGH_DATA_FOR_INTERPOLATION("Not enough data for Hermite interpolation"),
    /** Message. */
    NOT_INERTIAL_FRAME("The propagation frame must be inertial or pseudo inertial"),
    /** Message. */
    DIHEDRAL_FOV_NOT_ORTHOGONAL_AXIS("vector {0} and vector center must be orthogonal"),
    /** Message. */
    STELA_INTEGRATION_FAILED(
            "STELA integration failed (probably during reentry). Try reducing allowed minimum stepsize"),
    /** Message. */
    GEOD_CONVERGENCE_FAILED(
            "Failed to compute geodetic coordinates. The algorithm was unable to converge. 1st threshold is {0} "
                    + "(reached {1}) and 2st threshold is {2} (reached {3}). Use setAngularThreshold() or "
                    + "set2ndAngularThreshold() to adjust threshold."),
    /** Message. */
    FRAME_NO_NTH_ANCESTOR("Frame {0} does not have a {1}th ancestor, its depth is {2}"),
    /** Message. */
    NO_COMMON_FRAME("No common frame exists between Frame {0} and Frame {1}"),
    /** Message. */
    NOT_GRADIENT_MODEL("Provided force model is not a gradient model. Cannot compute partial derivatives using finite "
            + "differences"),
    /** Message. */
    NEGATIVE_DEGREE_FOR_PARTIAL_DERIVATIVES_COMPUTATION(
            "Cannot compute partial derivatives for the force model : input degree for partial derivatives "
                    + "(n = {0}) is negative !"),
    /** Message. */
    NEGATIVE_ORDER_FOR_PARTIAL_DERIVATIVES_COMPUTATION(
            "Cannot compute partial derivatives for the force model : input order for partial derivatives "
                    + "(m = {0}) is negative !"),
    /** Message. */
    ISIS_SUN_FRAME_UNDEFINED("ISIS Sun Frame is undefined : Sun is orthogonal to the orbit plane"),
    /** Message. */
    CONVERGENCE_FAILED_AFTER_N_ITERATIONS("Failed to converge after {0} iterations"),
    /** Message. */
    LOCAL_SOLAR_TIME_OUT_OF_RANGE("{0} time is out of range [-Pi, Pi["),
    /** Message. */
    MISSING_GRAVITY_COEFFICIENT("Potential model coefficient {0}[{1}, {2}] is missing (more may be missing)"),
    /** Message. */
    ARGUMENT_OUTSIDE_DOMAIN("Argument {0} outside domain [{1} ; {2}]"),
    /** Message. */
    ARRAY_SIZE_EXCEEDS_MAX_VARIABLES("array size cannot be greater than {0}"),
    /** Message. */
    ARRAY_SIZES_SHOULD_HAVE_DIFFERENCE_1("array sizes should have difference 1 ({0} != {1} + 1)"),
    /** Message. */
    ARRAY_SUMS_TO_ZERO("array sums to zero"),
    /** Message. */
    ASSYMETRIC_EIGEN_NOT_SUPPORTED("eigen decomposition of assymetric matrices not supported yet"),
    /** Message. */
    AT_LEAST_ONE_COLUMN("matrix must have at least one column"),
    /** Message. */
    AT_LEAST_ONE_ROW("matrix must have at least one row"),
    /** Message. */
    BANDWIDTH("bandwidth ({0})"),
    /** Message. */
    BINOMIAL_INVALID_PARAMETERS_ORDER("must have n >= k for binomial coefficient (n, k), got k = {0}, n = {1}"),
    /** Message. */
    BINOMIAL_NEGATIVE_PARAMETER("must have n >= 0 for binomial coefficient (n, k), got n = {0}"),
    /** Message. */
    CANNOT_CLEAR_STATISTIC_CONSTRUCTED_FROM_EXTERNAL_MOMENTS(
            "statistics constructed from external moments cannot be cleared"),
    /** Message. */
    CANNOT_COMPUTE_0TH_ROOT_OF_UNITY("cannot compute 0-th root of unity, indefinite result"),
    /** Message. */
    CANNOT_COMPUTE_BETA_DENSITY_AT_0_FOR_SOME_ALPHA("cannot compute beta density at 0 when alpha = {0,number}"),
    /** Message. */
    CANNOT_COMPUTE_BETA_DENSITY_AT_1_FOR_SOME_BETA("cannot compute beta density at 1 when beta = %.3g"),
    /** Message. */
    CANNOT_COMPUTE_NTH_ROOT_FOR_NEGATIVE_N("cannot compute nth root for null or negative n: {0}"),
    /** Message. */
    CANNOT_DISCARD_NEGATIVE_NUMBER_OF_ELEMENTS("cannot discard a negative number of elements ({0})"),
    /** Message. */
    CANNOT_FORMAT_INSTANCE_AS_3D_VECTOR("cannot format a {0} instance as a 3D vector"),
    /** Message. */
    CANNOT_FORMAT_INSTANCE_AS_COMPLEX("cannot format a {0} instance as a complex number"),
    /** Message. */
    CANNOT_FORMAT_INSTANCE_AS_REAL_VECTOR("cannot format a {0} instance as a real vector"),
    /** Message. */
    CANNOT_FORMAT_OBJECT_TO_FRACTION("cannot format given object as a fraction number"),
    /** Message. */
    CANNOT_INCREMENT_STATISTIC_CONSTRUCTED_FROM_EXTERNAL_MOMENTS(
            "statistics constructed from external moments cannot be incremented"),
    /** Message. */
    CANNOT_NORMALIZE_A_ZERO_NORM_VECTOR("cannot normalize a zero norm vector"),
    /** Message. */
    CANNOT_RETRIEVE_AT_NEGATIVE_INDEX("elements cannot be retrieved from a negative array index {0}"),
    /** Message. */
    CANNOT_SET_AT_NEGATIVE_INDEX("cannot set an element at a negative index {0}"),
    /** Message. */
    CANNOT_SUBSTITUTE_ELEMENT_FROM_EMPTY_ARRAY("cannot substitute an element from an empty array"),
    /** Message. */
    CANNOT_TRANSFORM_TO_DOUBLE("Conversion Exception in Transformation: {0}"),
    /** Message. */
    CARDAN_ANGLES_SINGULARITY("Cardan angles singularity"),
    /** Message. */
    CLASS_DOESNT_IMPLEMENT_COMPARABLE("class ({0}) does not implement Comparable"),
    /** Message. */
    CLOSEST_ORTHOGONAL_MATRIX_HAS_NEGATIVE_DETERMINANT("the closest orthogonal matrix has a negative determinant {0}"),
    /** Message. */
    COLUMN_INDEX_OUT_OF_RANGE("column index {0} out of allowed range [{1}, {2}]"),
    /** Message. */
    COLUMN_INDEX("column index ({0})"),
    /** Message. */
    CONTINUED_FRACTION_INFINITY_DIVERGENCE("Continued fraction convergents diverged to +/- infinity for value {0}"),
    /** Message. */
    CONTINUED_FRACTION_NAN_DIVERGENCE("Continued fraction diverged to NaN for value {0}"),
    /** Message. */
    CONTRACTION_CRITERIA_SMALLER_THAN_EXPANSION_FACTOR(
            "contraction criteria ({0}) smaller than the expansion factor ({1}). "
                    + " This would lead to a never ending "
                    + "loop of expansion and contraction as a newly expanded internal storage "
                    + "array would immediately satisfy" + "the criteria for contraction."),
    /** Message. */
    CONTRACTION_CRITERIA_SMALLER_THAN_ONE("contraction criteria smaller than one ({0}).  "
            + "This would lead to a never ending loop of expansion and "
            + "contraction as an internal storage array length " + "equal to the number of elements would satisfy the"
            + "contraction criteria."),
    /** Message. */
    CONVERGENCE_FAILED("convergence failed"),
    /** Message. */
    CROSSING_BOUNDARY_LOOPS("some outline boundary loops cross each other"),
    /** Message. */
    CROSSOVER_RATE("crossover rate ({0})"),
    /** Message. */
    CUMULATIVE_PROBABILITY_RETURNED_NAN("Cumulative probability function returned NaN for argument {0} p = {1}"),
    /** Message. */
    DIFFERENT_ROWS_LENGTHS("some rows have length {0} while others have length {1}"),
    /** Message. */
    DIFFERENT_ORIG_AND_PERMUTED_DATA("original and permuted data must contain the same elements"),
    /** Message. */
    DIGEST_NOT_INITIALIZED("digest not initialized"),
    /** Message. */
    DIMENSIONS_MISMATCH_2x2("got {0}x{1} but expected {2}x{3}"),
    /** Message. */
    DIMENSIONS_MISMATCH_SIMPLE("{0} != {1}"),
    /** Message. */
    DIMENSIONS_MISMATCH("dimensions mismatch"),
    /** Message. */
    DISCRETE_CUMULATIVE_PROBABILITY_RETURNED_NAN(
            "Discrete cumulative probability function returned NaN for argument {0}"),
    /** Message. */
    DISTRIBUTION_NOT_LOADED("distribution not loaded"),
    /** Message. */
    DUPLICATED_ABSCISSA_DIVISION_BY_ZERO("duplicated abscissa {0} causes division by zero"),
    /** Message. */
    ELITISM_RATE("elitism rate ({0})"),
    /** Message. */
    EMPTY_CLUSTER_IN_K_MEANS("empty cluster in k-means"),
    /** Message. */
    EMPTY_POLYNOMIALS_COEFFICIENTS_ARRAY("empty polynomials coefficients array"),
    /** Message. */
    EMPTY_SELECTED_COLUMN_INDEX_ARRAY("empty selected column index array"),
    /** Message. */
    EMPTY_SELECTED_ROW_INDEX_ARRAY("empty selected row index array"),
    /** Message. */
    EMPTY_STRING_FOR_IMAGINARY_CHARACTER("empty string for imaginary character"),
    /** Message. */
    ENDPOINTS_NOT_AN_INTERVAL("endpoints do not specify an interval: [{0}, {1}]"),
    /** Message. */
    EQUAL_VERTICES_IN_SIMPLEX("equal vertices {0} and {1} in simplex configuration"),
    /** Message. */
    EULER_ANGLES_SINGULARITY("Euler angles singularity"),
    /** Message. */
    EVALUATION("evaluation"),
    /** Message. */
    EXPANSION_FACTOR_SMALLER_THAN_ONE("expansion factor smaller than one ({0})"),
    /** Message. */
    FACTORIAL_NEGATIVE_PARAMETER("must have n >= 0 for n!, got n = {0}"),
    /** Message. */
    FAILED_BRACKETING(
            "number of iterations={4}, maximum iterations={5}, initial={6}, lower bound={7}, upper bound={8}, "
                    + "final a value={0}, final b value={1}, f(a)={2}, f(b)={3}"),
    /** Message. */
    FAILED_FRACTION_CONVERSION("Unable to convert {0} to fraction after {1} iterations"),
    /** Message. */
    FIRST_COLUMNS_NOT_INITIALIZED_YET("first {0} columns are not initialized yet"),
    /** Message. */
    FIRST_ELEMENT_NOT_ZERO("first element is not 0: {0}"),
    /** Message. */
    FIRST_ROWS_NOT_INITIALIZED_YET("first {0} rows are not initialized yet"),
    /** Message. */
    FRACTION_CONVERSION_OVERFLOW("Overflow trying to convert {0} to fraction ({1}/{2})"),
    /** Message. */
    FUNCTION_NOT_DIFFERENTIABLE("function is not differentiable"),
    /** Message. */
    FUNCTION_NOT_POLYNOMIAL("function is not polynomial"),
    /** Message. */
    GCD_OVERFLOW_32_BITS("overflow: gcd({0}, {1}) is 2^31"),
    /** Message. */
    GCD_OVERFLOW_64_BITS("overflow: gcd({0}, {1}) is 2^63"),
    /** Message. */
    HOLE_BETWEEN_MODELS_TIME_RANGES("{0} wide hole between models time ranges"),
    /** Message. */
    ILL_CONDITIONED_OPERATOR("condition number {1} is too high "),
    /** Message. */
    INDEX_LARGER_THAN_MAX("the index specified: {0} is larger than the current maximal index {1}"),
    /** Message. */
    INDEX_NOT_POSITIVE("index ({0}) is not positive"),
    /** Message. */
    INDEX_OUT_OF_RANGE("index {0} out of allowed range [{1}, {2}]"),
    /** Message. */
    INDEX("index ({0})"),
    /** Message. */
    NOT_FINITE_NUMBER("{0} is not a finite number"),
    /** Message. */
    INFINITE_BOUND("interval bounds must be finite"),
    /** Message. */
    INTERVALS_OVERLAPPING_NOT_ALLOWED("The intervals shouldn't overlap with each others"),
    /** Message. */
    ARRAY_ELEMENT("value {0} at index {1}"),
    /** Message. */
    INFINITE_ARRAY_ELEMENT("Array contains an infinite element, {0} at index {1}"),
    /** Message. */
    INFINITE_VALUE_CONVERSION("cannot convert infinite value"),
    /** Message. */
    INITIAL_CAPACITY_NOT_POSITIVE("initial capacity ({0}) is not positive"),
    /** Message. */
    INITIAL_COLUMN_AFTER_FINAL_COLUMN("initial column {1} after final column {0}"),
    /** Message. */
    INITIAL_ROW_AFTER_FINAL_ROW("initial row {1} after final row {0}"),
    /** Message. */
    INSTANCES_NOT_COMPARABLE_TO_EXISTING_VALUES("instance of class {0} not comparable to existing values"),
    /** Message. */
    INSUFFICIENT_DATA_FOR_T_STATISTIC("insufficient data for t statistic, needs at least 2, got {0}"),
    /** Message. */
    INSUFFICIENT_DIMENSION("insufficient dimension {0}, must be at least {1}"),
    /** Message. */
    DIMENSION("dimension ({0})"),
    /** Message. */
    INSUFFICIENT_OBSERVED_POINTS_IN_SAMPLE("sample contains {0} observed points, at least {1} are required"),
    /** Message. */
    INSUFFICIENT_ROWS_AND_COLUMNS("insufficient data: only {0} rows and {1} columns."),
    /** Message. */
    INTEGRATION_METHOD_NEEDS_AT_LEAST_TWO_PREVIOUS_POINTS("multistep method needs at least {0} previous steps, "
            + "got {1}"),
    /** Message. */
    INVALID_BINARY_DIGIT("invalid binary digit: {0}"),
    /** Message. */
    INVALID_BINARY_CHROMOSOME("binary mutation works on BinaryChromosome only"),
    /** Message. */
    INVALID_BRACKETING_PARAMETERS("invalid bracketing parameters:  lower bound={0},  initial={1}, upper bound={2}"),
    /** Message. */
    INVALID_FIXED_LENGTH_CHROMOSOME("one-point crossover only works with fixed-length chromosomes"),
    /** Message. */
    INVALID_INTERVAL_INITIAL_VALUE_PARAMETERS(
            "invalid interval, initial value parameters:  lower={0}, initial={1}, upper={2}"),
    /** Message. */
    INVALID_ITERATIONS_LIMITS("invalid iteration limits: min={0}, max={1}"),
    /** Message. */
    INVALID_MAX_ITERATIONS("bad value for maximum iterations number: {0}"),
    /** Message. */
    NOT_ENOUGH_DATA_REGRESSION("the number of observations is not sufficient to conduct regression"),
    /** Message. */
    INVALID_REGRESSION_ARRAY(
            "input data array length = {0} does not match the number of observations = {1} and the number of "
                    + "regressors = {2}"),
    /** Message. */
    INVALID_REGRESSION_OBSERVATION(
            "length of regressor array = {0} does not match the number of variables = {1} in the model"),
    /** Message. */
    INVALID_ROUNDING_METHOD(
            "invalid rounding method {0}, valid methods: {1} ({2}), {3} ({4}), {5} ({6}), {7} ({8}), {9} ({10}), "
                    + "{11} ({12}), {13} ({14}), {15} ({16})"),
    /** Message. */
    ITERATOR_EXHAUSTED("iterator exhausted"),
    /** Message. */
    ITERATIONS("iterations"),
    /** Message. */
    LCM_OVERFLOW_32_BITS("overflow: lcm({0}, {1}) is 2^31"),
    /** Message. */
    LCM_OVERFLOW_64_BITS("overflow: lcm({0}, {1}) is 2^63"),
    /** Message. */
    LIST_OF_CHROMOSOMES_BIGGER_THAN_POPULATION_SIZE("list of chromosomes bigger than maxPopulationSize"),
    /** Message. */
    LOESS_EXPECTS_AT_LEAST_ONE_POINT("Loess expects at least 1 point"),
    /** Message. */
    LOWER_BOUND_NOT_BELOW_UPPER_BOUND("lower bound ({0}) must be strictly less than upper bound ({1})"),
    /** Message. */
    LOWER_ENDPOINT_ABOVE_UPPER_ENDPOINT("lower endpoint ({0}) must be less than or equal to upper endpoint ({1})"),
    /** Message. */
    MAP_MODIFIED_WHILE_ITERATING("map has been modified while iterating"),
    /** Message. */
    EVALUATIONS("evaluations"),
    /** Message. */
    MAX_COUNT_EXCEEDED("maximal count ({0}) exceeded"),
    /** Message. */
    MAX_ITERATIONS_EXCEEDED("maximal number of iterations ({0}) exceeded"),
    /** Message. */
    MINIMAL_STEPSIZE_REACHED_DURING_INTEGRATION(
            "minimal step size ({1,number,0.00E00}) reached, integration needs {0,number,0.00E00}"),
    /** Message. */
    MISMATCHED_LOESS_ABSCISSA_ORDINATE_ARRAYS(
            "Loess expects the abscissa and ordinate arrays to be of the same size, but got {0} abscissae and "
                    + "{1} ordinatae"),
    /** Message. */
    MUTATION_RATE("mutation rate ({0})"),
    /** Message. */
    NAN_ELEMENT_AT_INDEX("element {0} is NaN"),
    /** Message. */
    NAN_VALUE_CONVERSION("cannot convert NaN value"),
    /** Message. */
    NEGATIVE_BRIGHTNESS_EXPONENT("brightness exponent should be positive or null, but got {0}"),
    /** Message. */
    NEGATIVE_COMPLEX_MODULE("negative complex module {0}"),
    /** Message. */
    NEGATIVE_ELEMENT_AT_2D_INDEX("element ({0}, {1}) is negative: {2}"),
    /** Message. */
    NEGATIVE_ELEMENT_AT_INDEX("element {0} is negative: {1}"),
    /** Message. */
    NEGATIVE_NUMBER_OF_SUCCESSES("number of successes must be non-negative ({0})"),
    /** Message. */
    NUMBER_OF_SUCCESSES("number of successes ({0})"),
    /** Message. */
    NEGATIVE_NUMBER_OF_TRIALS("number of trials must be non-negative ({0})"),
    /** Message. */
    NUMBER_OF_INTERPOLATION_POINTS("number of interpolation points ({0})"),
    /** Message. */
    NUMBER_OF_TRIALS("number of trials ({0})"),
    /** Message. */
    ROBUSTNESS_ITERATIONS("number of robustness iterations ({0})"),
    /** Message. */
    START_POSITION("start position ({0})"),
    /** Message. */
    NON_CONVERGENT_CONTINUED_FRACTION(
            "Continued fraction convergents failed to converge (in less than {0} iterations) for value {1}"),
    /** Message. */
    NON_INVERTIBLE_TRANSFORM("non-invertible affine transform collapses some lines into single points"),
    /** Message. */
    NON_POSITIVE_MICROSPHERE_ELEMENTS("number of microsphere elements must be positive, but got {0}"),
    /** Message. */
    NON_POSITIVE_POLYNOMIAL_DEGREE("polynomial degree must be positive: degree={0}"),
    /** Message. */
    NON_REAL_FINITE_ABSCISSA("all abscissae must be finite real numbers, but {0}-th is {1}"),
    /** Message. */
    NON_REAL_FINITE_ORDINATE("all ordinatae must be finite real numbers, but {0}-th is {1}"),
    /** Message. */
    NON_REAL_FINITE_WEIGHT("all weights must be finite real numbers, but {0}-th is {1}"),
    /** Message. */
    NON_SQUARE_MATRIX("non square ({0}x{1}) matrix"),
    /** Message. */
    NORM("Norm ({0})"),
    /** Message. */
    NORMALIZE_INFINITE("Cannot normalize to an infinite value"),
    /** Message. */
    NORMALIZE_NAN("Cannot normalize to NaN"),
    /** Message. */
    NOT_ADDITION_COMPATIBLE_MATRICES("{0}x{1} and {2}x{3} matrices are not addition compatible"),
    /** Message. */
    NOT_DECREASING_NUMBER_OF_POINTS("points {0} and {1} are not decreasing ({2} < {3})"),
    /** Message. */
    NOT_DECREASING_SEQUENCE("points {3} and {2} are not decreasing ({1} < {0})"),
    /** Message. */
    NOT_ENOUGH_DATA_FOR_NUMBER_OF_PREDICTORS("not enough data ({0} rows) for this many predictors ({1} predictors)"),
    /** Message. */
    NOT_ENOUGH_POINTS_IN_SPLINE_PARTITION("spline partition must have at least {0} points, got {1}"),
    /** Message. */
    NOT_INCREASING_NUMBER_OF_POINTS("points {0} and {1} are not increasing ({2} > {3})"),
    /** Message. */
    NOT_INCREASING_SEQUENCE("points {3} and {2} are not increasing ({1} > {0})"),
    /** Message. */
    NOT_MULTIPLICATION_COMPATIBLE_MATRICES("{0}x{1} and {2}x{3} matrices are not multiplication compatible"),
    /** Message. */
    NOT_POSITIVE_DEFINITE_MATRIX("not positive definite matrix"),
    /** Message. */
    NON_POSITIVE_DEFINITE_MATRIX(
            "not positive definite matrix: diagonal element at ({1},{1}) is smaller than {2} ({0})"),
    /** Message. */
    NON_POSITIVE_DEFINITE_OPERATOR("non positive definite linear operator"),
    /** Message. */
    NON_SELF_ADJOINT_OPERATOR("non self-adjoint linear operator"),
    /** Message. */
    NON_SQUARE_OPERATOR("non square ({0}x{1}) linear operator"),
    /** Message. */
    DEGREES_OF_FREEDOM("degrees of freedom ({0})"),
    /** Message. */
    NOT_POSITIVE_DEGREES_OF_FREEDOM("degrees of freedom must be positive ({0})"),
    /** Message. */
    NOT_POSITIVE_ELEMENT_AT_INDEX("element {0} is not positive: {1}"),
    /** Message. */
    NOT_POSITIVE_EXPONENT("invalid exponent {0} (must be positive)"),
    /** Message. */
    NOT_POSITIVE_SCALAR("invalid scalar {0} (must be positive)"),
    /** Message. */
    NUMBER_OF_ELEMENTS_SHOULD_BE_POSITIVE("number of elements should be positive ({0})"),
    /** Message. */
    EXPONENT("exponent ({0})"),
    /** Message. */
    NOT_POSITIVE_LENGTH("length must be positive ({0})"),
    /** Message. */
    LENGTH("length ({0})"),
    /** Message. */
    NOT_POSITIVE_MEAN("mean must be positive ({0})"),
    /** Message. */
    MEAN("mean ({0})"),
    /** Message. */
    NOT_POSITIVE_NUMBER_OF_SAMPLES("number of sample is not positive: {0}"),
    /** Message. */
    NUMBER_OF_SAMPLES("number of samples ({0})"),
    /** Message. */
    NOT_POSITIVE_PERMUTATION("permutation k ({0}) must be positive"),
    /** Message. */
    PERMUTATION_SIZE("permutation size ({0}"),
    /** Message. */
    NOT_POSITIVE_POISSON_MEAN("the Poisson mean must be positive ({0})"),
    /** Message. */
    NOT_POSITIVE_POPULATION_SIZE("population size must be positive ({0})"),
    /** Message. */
    POPULATION_SIZE("population size ({0})"),
    /** Message. */
    NOT_POSITIVE_ROW_DIMENSION("invalid row dimension: {0} (must be positive)"),
    /** Message. */
    NOT_POSITIVE_COLUMN_DIMENSION("invalid column dimension: {0} (must be positive)"),
    /** Message. */
    NOT_POSITIVE_SAMPLE_SIZE("sample size must be positive ({0})"),
    /** Message. */
    NOT_POSITIVE_SCALE("scale must be positive ({0})"),
    /** Message. */
    SCALE("scale ({0})"),
    /** Message. */
    NOT_POSITIVE_SHAPE("shape must be positive ({0})"),
    /** Message. */
    SHAPE("shape ({0})"),
    /** Message. */
    NOT_POSITIVE_STANDARD_DEVIATION("standard deviation must be positive ({0})"),
    /** Message. */
    STANDARD_DEVIATION("standard deviation ({0})"),
    /** Message. */
    NOT_POSITIVE_UPPER_BOUND("upper bound must be positive ({0})"),
    /** Message. */
    NOT_POSITIVE_WINDOW_SIZE("window size must be positive ({0})"),
    /** Message. */
    NOT_POWER_OF_TWO("{0} is not a power of 2"),
    /** Message. */
    NOT_POWER_OF_TWO_CONSIDER_PADDING("{0} is not a power of 2, consider padding for fix"),
    /** Message. */
    NOT_POWER_OF_TWO_PLUS_ONE("{0} is not a power of 2 plus one"),
    /** Message. */
    NOT_STRICTLY_DECREASING_NUMBER_OF_POINTS("points {0} and {1} are not strictly decreasing ({2} <= {3})"),
    /** Message. */
    NOT_STRICTLY_DECREASING_SEQUENCE("points {3} and {2} are not strictly decreasing ({1} <= {0})"),
    /** Message. */
    NOT_STRICTLY_INCREASING_KNOT_VALUES("knot values must be strictly increasing"),
    /** Message. */
    NOT_STRICTLY_INCREASING_NUMBER_OF_POINTS("points {0} and {1} are not strictly increasing ({2} >= {3})"),
    /** Message. */
    NOT_STRICTLY_INCREASING_SEQUENCE("points {3} and {2} are not strictly increasing ({1} >= {0})"),
    /** Message. */
    NOT_SUBTRACTION_COMPATIBLE_MATRICES("{0}x{1} and {2}x{3} matrices are not subtraction compatible"),
    /** Message. */
    NOT_SUPPORTED_IN_DIMENSION_N("method not supported in dimension {0}"),
    /** Message. */
    NOT_SYMMETRIC_MATRIX("not symmetric matrix"),
    /** Message. */
    NON_SYMMETRIC_MATRIX(
            "non symmetric matrix: the difference between entries at ({0},{1}) and ({1},{0}) is larger than {2}"),
    /** Message. */
    NO_BIN_SELECTED("no bin selected"),
    /** Message. */
    NO_CONVERGENCE_WITH_ANY_START_POINT("none of the {0} start points lead to convergence"),
    /** Message. */
    NO_DATA("no data"),
    /** Message. */
    NO_DEGREES_OF_FREEDOM("no degrees of freedom ({0} measurements, {1} parameters)"),
    /** Message. */
    NO_DENSITY_FOR_THIS_DISTRIBUTION("This distribution does not have a density function implemented"),
    /** Message. */
    NO_FEASIBLE_SOLUTION("no feasible solution"),
    /** Message. */
    NO_OPTIMUM_COMPUTED_YET("no optimum computed yet"),
    /** Message. */
    NO_REGRESSORS("Regression model must include at least one regressor"),
    /** Message. */
    NO_RESULT_AVAILABLE("no result available"),
    /** Message. */
    NO_SUCH_MATRIX_ENTRY("no entry at indices ({0}, {1}) in a {2}x{3} matrix"),
    /** Message. */
    NAN_NOT_ALLOWED("NaN is not allowed"),
    /** Message. */
    NULL_NOT_ALLOWED("null is not allowed"),
    /** Message, send a description. */
    NULL_NOT_ALLOWED_DESCRIPTION("A non-null value is expected ({0})"),
    /** Message. */
    NULL_ARRAY_NOT_ALLOWED("the supplied array is null"),
    /** Message. */
    EMPTY_ARRAY_NOT_ALLOWED("the supplied array is empty"),
    /** Message, send collection description. */
    EMPTY_COLLECTION_NOT_ALLOWED("A non-empty collection is expected ({0})"),
    /** Message. */
    NULL_MATRIX_NOT_ALLOWED("the supplied matrix is null"),
    /** Message. */
    NULL_GRAVITY_MODEL_NOT_ALLOWED("The given gravity model is null, while it is not allowed to be so"),
    /** Message, send collection description. */
    NULL_COLLECTION_NOT_ALLOWED("A non-null collection is expected ({0})"),
    /** Message. */
    ARRAY_ZERO_LENGTH_OR_NULL_NOT_ALLOWED("a null or zero length array not allowed"),
    /** Message. */
    COVARIANCE_MATRIX("covariance matrix"),
    /** Message. */
    DENOMINATOR("denominator"),
    /** Message. */
    DENOMINATOR_FORMAT("denominator format"),
    /** Message. */
    FRACTION("fraction"),
    /** Message. */
    FUNCTION("function"),
    /** Message. */
    IMAGINARY_FORMAT("imaginary format"),
    /** Message. */
    INPUT_ARRAY("input array"),
    /** Message. */
    NUMERATOR("numerator"),
    /** Message. */
    NUMERATOR_FORMAT("numerator format"),
    /** Message. */
    OBJECT_TRANSFORMATION("conversion exception in transformation"),
    /** Message. */
    REAL_FORMAT("real format"),
    /** Message. */
    WHOLE_FORMAT("whole format"),
    /** Message. */
    NUMBER_TOO_LARGE("{0} is larger than the maximum ({1})"),
    /** Message. */
    NUMBER_TOO_SMALL("{0} is smaller than the minimum ({1})"),
    /** Message. */
    NUMBER_TOO_LARGE_BOUND_EXCLUDED("{0} is larger than, or equal to, the maximum ({1})"),
    /** Message. */
    NUMBER_TOO_SMALL_BOUND_EXCLUDED("{0} is smaller than, or equal to, the minimum ({1})"),
    /** Message. */
    NUMBER_OF_SUCCESS_LARGER_THAN_POPULATION_SIZE(
            "number of successes ({0}) must be less than or equal to population size ({1})"),
    /** Message. */
    NUMERATOR_OVERFLOW_AFTER_MULTIPLY("overflow, numerator too large after multiply: {0}"),
    /** Message. */
    N_POINTS_GAUSS_LEGENDRE_INTEGRATOR_NOT_SUPPORTED(
            "{0} points Legendre-Gauss integrator not supported, number of points must be in the {1}-{2} range"),
    /** Message. */
    OBSERVED_COUNTS_ALL_ZERO("observed counts are all 0 in observed array {0}"),
    /** Message. */
    OBSERVED_COUNTS_BOTTH_ZERO_FOR_ENTRY("observed counts are both zero for entry {0}"),
    /** Message. */
    BOBYQA_BOUND_DIFFERENCE_CONDITION(
            "the difference between the upper and lower bound must be larger than twice the initial trust region "
                    + "radius ({0})"),
    /** Message. */
    OUT_OF_BOUNDS_QUANTILE_VALUE("out of bounds quantile value: {0}, must be in (0, 100]"),
    /** Message. */
    OUT_OF_BOUND_SIGNIFICANCE_LEVEL("out of bounds significance level {0}, must be between {1} and {2}"),
    /** Message. */
    SIGNIFICANCE_LEVEL("significance level ({0})"),
    /** Message. */
    OUT_OF_ORDER_ABSCISSA_ARRAY("the abscissae array must be sorted in a strictly increasing order, but "
            + "the {0}-th element is {1} whereas " + "{2}-th is {3}"),
    /** Message. */
    OUT_OF_RANGE_ROOT_OF_UNITY_INDEX("out of range root of unity index {0} (must be in [{1};{2}])"),
    /** Message. */
    OUT_OF_RANGE("out of range"),
    /** Message. */
    OUT_OF_RANGE_SIMPLE("{0} out of [{1}, {2}] range"),
    /** Message. */
    OUT_OF_RANGE_LEFT("{0} out of ({1}, {2}] range"),
    /** Message. */
    OUT_OF_RANGE_RIGHT("{0} out of [{1}, {2}) range"),
    /** Message. */
    OUTLINE_BOUNDARY_LOOP_OPEN("an outline boundary loop is open"),
    /** Message. */
    OVERFLOW("overflow"),
    /** Message. */
    OVERFLOW_IN_FRACTION("overflow in fraction {0}/{1}, cannot negate"),
    /** Message. */
    OVERFLOW_IN_ADDITION("overflow in addition: {0} + {1}"),
    /** Message. */
    OVERFLOW_IN_SUBTRACTION("overflow in subtraction: {0} - {1}"),
    /** Message. */
    OVERFLOW_IN_MULTIPLICATION("overflow in multiplication: {0} * {1}"),
    /** Message. */
    PERCENTILE_IMPLEMENTATION_CANNOT_ACCESS_METHOD("cannot access {0} method in percentile implementation {1}"),
    /** Message. */
    PERCENTILE_IMPLEMENTATION_UNSUPPORTED_METHOD("percentile implementation {0} does not support {1}"),
    /** Message. */
    PERMUTATION_EXCEEDS_N("permutation size ({0}) exceeds permuation domain ({1})"),
    /** Message. */
    POLYNOMIAL("polynomial"),
    /** Message. */
    POLYNOMIAL_INTERPOLANTS_MISMATCH_SEGMENTS(
            "number of polynomial interpolants must match the number of segments ({0} != {1} - 1)"),
    /** Message. */
    POPULATION_LIMIT_NOT_POSITIVE("population limit has to be positive"),
    /** Message. */
    POWER_NEGATIVE_PARAMETERS("cannot raise an integral value to a negative power ({0}^{1})"),
    /** Message. */
    PROPAGATION_DIRECTION_MISMATCH("propagation direction mismatch"),
    /** Message. */
    RANDOMKEY_MUTATION_WRONG_CLASS("RandomKeyMutation works only with RandomKeys, not {0}"),
    /** Message. */
    ROOTS_OF_UNITY_NOT_COMPUTED_YET("roots of unity have not been computed yet"),
    /** Message. */
    ROTATION_MATRIX_DIMENSIONS("a {0}x{1} matrix cannot be a rotation matrix"),
    /** Message. */
    ROW_INDEX_OUT_OF_RANGE("row index {0} out of allowed range [{1}, {2}]"),
    /** Message. */
    ROW_INDEX("row index ({0})"),
    /** Message. */
    SAME_SIGN_AT_ENDPOINTS(
            "function values at endpoints do not have different signs, endpoints: [{0}, {1}], values: [{2}, {3}]"),
    /** Message. */
    SAMPLE_SIZE_EXCEEDS_COLLECTION_SIZE("sample size ({0}) exceeds collection size ({1})"),
    /** Message. */
    SAMPLE_SIZE_LARGER_THAN_POPULATION_SIZE("sample size ({0}) must be less than or equal to population size ({1})"),
    /** Message. */
    SIMPLEX_NEED_ONE_POINT("simplex must contain at least one point"),
    /** Message. */
    SIMPLE_MESSAGE("{0}"),
    /** Message. */
    SINGULAR_MATRIX("matrix is singular"),
    /** Message. */
    SINGULAR_OPERATOR("operator is singular"),
    /** Message. */
    SUBARRAY_ENDS_AFTER_ARRAY_END("subarray ends after array end"),
    /** Message. */
    TOO_LARGE_CUTOFF_SINGULAR_VALUE("cutoff singular value is {0}, should be at most {1}"),
    /** Message. */
    TOO_LARGE_TOURNAMENT_ARITY("tournament arity ({0}) cannot be bigger than population size ({1})"),
    /** Message. */
    TOO_MANY_ELEMENTS_TO_DISCARD_FROM_ARRAY("cannot discard {0} elements from a {1} elements array"),
    /** Message. */
    TOO_MANY_REGRESSORS("too many regressors ({0}) specified, only {1} in the model"),
    /** Message. */
    TOO_SMALL_COST_RELATIVE_TOLERANCE(
            "cost relative tolerance is too small ({0}), no further reduction in the sum of squares is possible"),
    /** Message. */
    TOO_SMALL_INTEGRATION_INTERVAL("too small integration interval: length = {0}"),
    /** Message. */
    TOO_SMALL_ORTHOGONALITY_TOLERANCE(
            "orthogonality tolerance is too small ({0}), solution is orthogonal to the jacobian"),
    /** Message. */
    TOO_SMALL_PARAMETERS_RELATIVE_TOLERANCE(
            "parameters relative tolerance is too small ({0}), no further improvement in the approximate solution "
                    + "is possible"),
    /** Message. */
    TRUST_REGION_STEP_FAILED("trust region step has failed to reduce Q"),
    /** Message. */
    TWO_OR_MORE_CATEGORIES_REQUIRED("two or more categories required, got {0}"),
    /** Message. */
    TWO_OR_MORE_VALUES_IN_CATEGORY_REQUIRED("two or more values required in each category, one has {0}"),
    /** Message. */
    UNABLE_TO_BRACKET_OPTIMUM_IN_LINE_SEARCH("unable to bracket optimum in line search"),
    /** Message. */
    UNABLE_TO_COMPUTE_COVARIANCE_SINGULAR_PROBLEM("unable to compute covariances: singular problem"),
    /** Message. */
    UNABLE_TO_FIRST_GUESS_HARMONIC_COEFFICIENTS("unable to first guess the harmonic coefficients"),
    /** Message. */
    UNABLE_TO_ORTHOGONOLIZE_MATRIX("unable to orthogonalize matrix in {0} iterations"),
    /** Message. */
    UNABLE_TO_PERFORM_QR_DECOMPOSITION_ON_JACOBIAN("unable to perform Q.R decomposition on the {0}x{1} "
            + "jacobian matrix"),
    /** Message. */
    UNABLE_TO_SOLVE_SINGULAR_PROBLEM("unable to solve: singular problem"),
    /** Message. */
    UNBOUNDED_SOLUTION("unbounded solution"),
    /** Message. */
    UNKNOWN_MODE(
            "unknown mode {0}, known modes: {1} ({2}), {3} ({4}), {5} ({6}), {7} ({8}), {9} ({10}) and {11} ({12})"),
    /** Message. */
    UNMATCHED_ODE_IN_EXPANDED_SET("ode does not match the main ode set in the extended set"),
    /** Message. */
    CANNOT_PARSE_AS_TYPE("string \"{0}\" unparseable (from position {1}) as an object of type {2}"),
    /** Message. */
    CANNOT_PARSE("string \"{0}\" unparseable (from position {1})"),
    /** Message. */
    UNPARSEABLE_3D_VECTOR("unparseable 3D vector: \"{0}\""),
    /** Message. */
    UNPARSEABLE_COMPLEX_NUMBER("unparseable complex number: \"{0}\""),
    /** Message. */
    UNPARSEABLE_REAL_VECTOR("unparseable real vector: \"{0}\""),
    /** Message. */
    UNSUPPORTED_EXPANSION_MODE("unsupported expansion mode {0}, supported modes are {1} ({2}) and {3} ({4})"),
    /** Message. */
    UNSUPPORTED_OPERATION("unsupported operation"),
    /** Message. */
    ARITHMETIC_EXCEPTION("arithmetic exception"),
    /** Message. */
    ILLEGAL_STATE("illegal state"),
    /** Message. */
    USER_EXCEPTION("exception generated in user code"),
    /** Message. */
    URL_CONTAINS_NO_DATA("URL {0} contains no data"),
    /** Message. */
    VALUES_ADDED_BEFORE_CONFIGURING_STATISTIC("{0} values have been added before statistic is configured"),
    /** Message. */
    VECTOR_LENGTH_MISMATCH("vector length mismatch: got {0} but expected {1}"),
    /** Message. */
    VECTOR_MUST_HAVE_AT_LEAST_ONE_ELEMENT("vector must have at least one element"),
    /** Message. */
    WEIGHT_AT_LEAST_ONE_NON_ZERO("weigth array must contain at least one non-zero value"),
    /** Message. */
    WRONG_BLOCK_LENGTH("wrong array shape (block length = {0}, expected {1})"),
    /** Message. */
    WRONG_NUMBER_OF_POINTS("{0} points are required, got only {1}"),
    /** Message. */
    NUMBER_OF_POINTS("number of points ({0})"),
    /** Message. */
    ZERO_DENOMINATOR("denominator must be different from 0"),
    /** Message. */
    ZERO_DENOMINATOR_IN_FRACTION("zero denominator in fraction {0}/{1}"),
    /** Message. */
    ZERO_FRACTION_TO_DIVIDE_BY("the fraction to divide by must not be zero: {0}/{1}"),
    /** Message. */
    ZERO_NORM("zero norm"),
    /** Message. */
    ZERO_NORM_FOR_ROTATION_AXIS("zero norm for rotation axis"),
    /** Message. */
    ZERO_NORM_FOR_ROTATION_DEFINING_VECTOR("zero norm for rotation defining vector"),
    /** Message. */
    ZERO_NOT_ALLOWED("zero not allowed here"),
    /** Message. */
    BAD_SIZE_MATRIX_CREATION("bad size of Matrix3D constructor inputs"),
    /** Message. */
    BAD_NUMBER_OF_POINTS("only {2} and {4} points are allowed"),
    /** Message. */
    NOT_A_COVARIANCE_MATRIX("Provided matrix is not a covariance matrix"),
    /** Message. */
    INVALID_COVARIANCE_MATRIX(
            "Invalid covariance matrix for {0} orbits: the matrix should be at least {1}x{1}, not {2}x{2}"),
    /** Message. */
    INVALID_ORBIT_DATE_COVARIANCE_MATRIX(
            "The provided orbit is not defined at the same date as the covariance ({0} != {1})"),
    /** Message. */
    INVALID_ORBITS_DATE_COVARIANCE_MATRIX(
            "Invalid orbit date ({0}): the orbits must all be defined at the same date ({1})"),
    /** Message. */
    NO_ORBITAL_COORDINATE_DESCRIPTOR("No orbital coordinate descriptor on row number {0}"),
    /** Message. */
    INVALID_ORBITAL_COORDINATE_DESCRIPTOR_WRONG_ORBIT_TYPE(
            "Invalid orbital coordinate descriptor: row number {0} is mapped to {1} (wrong orbit type: {2} != {3})"),
    /** Message. */
    INVALID_ORBITAL_COORDINATE_DESCRIPTOR_WRONG_STATE_VECTOR_INDEX(
            "Invalid orbital coordinate descriptor: row number {0} is mapped to {1} "
                    + "(wrong state vector index: {2} != {3})"),
    /** Message. */
    INVALID_ORBITAL_COORDINATE_DESCRIPTOR_ROW_ALREADY_MAPPED(
            "Invalid orbital coordinate descriptor: row number {0} is already mapped to {1}"),
    /** Message. */
    INVALID_PARAMETERS_COUNT_NOT_MATCH_ORBITS_NUMBER(
            "Invalid additional parameters count array: its length of does not match the number of orbits "
                    + "({0} != {1})"),
    /** Message. */
    NEGATIVE_PARAMETERS_COUNT("Negative additional parameters count: {0} was supplied for orbit number {1}"),
    /** Message. */
    INVALID_PARAMETERS_COUNT_NOT_MATCH_COVARIANCE_SIZE(
            "Invalid additional parameters count array: total count is inconsistent with the size of the "
                    + "covariance matrix ({0} != {1})"),
    /** Message. */
    INVALID_ORBIT_INDEX("Invalid orbit index: {0} is not between 0 and {1}"),
    /** Message. */
    INVALID_PARAM_DESCRIPTORS_NUMBER_COVARIANCE_SIZE(
            "The number of parameter descriptors does not match the size of the covariance matrix ({0} != {1})"),
    /** Message. */
    PARAM_DESCRIPTORS_COLLECTION_DUPLICATES("The collection of parameter descriptors contains duplicates"),
    /** Message. */
    NULL_PARAM_DESCRIPTOR("Invalid parameter descriptors: element number {0} is null"),
    /** Message. */
    EMPTY_PARAM_DESCRIPTOR("Invalid parameter descriptors: element number {0} is empty"),
    /** Message. */
    IMMUTABLE_PARAM_DESCRIPTOR("Operation not allowed: this parameter descriptor is currently immutable"),
    /** Message. */
    PARAM_DESCRIPTOR_NOT_ASSOCIATED_WITH_COVARIANCE(
            "The provided parameter descriptor ({0}) is not associated with this covariance matrix"),
    /** Message. */
    INVALID_POLYGON_CLASSFICATION(
            "the classification of created polygon ({0}) is invalid, it must be concave or convexe"),
    /** Message. */
    INVALID_POLYGON_SIZE("the size of the created polygon is infinite"),
    /** Message. */
    COL_LEBEDEV_GRID("Invalid Lebedev grid order : {0}"),
    /** Message. */
    INCORRECT_INTERVAL("Incorrect interval: the lower bound should be smaller than the upper bound"),

    /** Message. */
    NULL_VEHICLE_SURFACE_MODEL("Null vehicle surface model."),
    /** Message. */
    NULL_DRAG_COEFFICIENT("Null drag coefficient function"),
    /** Message. */
    NULL_LIFT_COEFFICIENT("Null lift coefficient function"),
    /** Message. */
    UNSUPORTED_COMBINATION_DRAG_LIFT_COEFFICIENTS("Unsupported combination of Drag and Lift coefficients functions."),
    /** Message. */
    NOT_CONSTANT_DRAG_COEF("Drag coefficient is not constant."),
    /** Message. */
    NOT_CONSTANT_LIFT_COEF("Lift coefficient is not constant."),
    /** Message. */
    NULL_RADIATIVE_PROPERTY("Null radiative properties"),
    /** Message. */
    SUM_RADIATIVE_COEFFICIENT_NOT_1("Sum of radiative coefficients for visible domain is different from 1"),
    /** Message. */
    SUM_RADIATIVEIR_COEFFICIENT_NOT_1("Sum of radiative coefficients for infrared domain is different from 1"),
    /** Message. */
    UNSUPPORTED_SHAPE("Un-supported main part shape type"),
    /** Message. */
    LEGS_SEQUENCE_EMPTY("The legs sequence is empty"),
    /** Message. */
    ERROR_PROPAGATION_END_DATE("Propagation end date has not been reached ; dt = {0}s"),
    /** Message. */
    NOT_POSITIVE_SCALING_FACTOR("The scaling factor {0} is not positive"),
    /** Message. */
    MOBILE_PART_FRAME_UPDATE("Mobile part frame update requires a SpacecraftState."),
    /** Message. */
    INVALID_ANG_VEL_ATT_PROFILE_REF_DATE("The reference date {0} is not equal or after "
            + "the lower bound of the covered interval {1}"),
    /** Message. */
    MANEUVER_DE_NO_FEASIBLE("The maneuver is not feasible (da = {0}m and de = {1})"),
    /** Message. */
    MANEUVER_DI_NO_FEASIBLE("The maneuver is not feasible (da = {0}m and di = {1}rad)"),
    /** Message. */
    NEGATIVE_INCLINATION("Inclination is negative: {0}rad"),
    /** Message. */
    METHOD_NOT_AVAILABLE_LIU("This method (propagateMeanOrbit) is not available for Liu propagator."),
    /** Message. */
    UNABLE_TO_COMPUTE_LIU_MEAN_PARAMETERS(
            "unable to compute Liu mean parameters after {0} iterations. Use method setThreshold() to change "
                    + "convergence threshold."),
    /** Message. */
    METHOD_NOT_AVAILABLE_TABULATED_SLEW("This method (compute) is not available for the class TabulatedSlew"),
    /** Message. */
    METHOD_NOT_AVAILABLE_ANGUAR_VELOCITIES_POLYNOMIAL_SLEW("This method (compute) is not available for the "
            + "class AngularVelocitiesPolynomialSlew"),
    /** Duplicated element. */
    DUPLICATED_ELEMENT("Element : {0} is duplicated"),
    /** Message. */
    NOT_POSITIVE_ABSOLUTE_THRESHOLD("Absolute threshold is not positive or null."),
    /** Message. */
    NOT_POSITIVE_RELATIVE_THRESHOLD("Relative threshold is not positive or null."),
    /** Message. */
    NAN_THRESHOLD("Input threshold is NaN."),
    /** Message. */
    NOT_PERFORMED_DECOMPOSITION("Matrix decomposition has not been performed. Decomposed matrices are null!"),
    /** Message. */
    UNABLE_TO_PERFORM_ANALYTICAL2D_OSC_MEAN_CONVERSION(
            "Unable to converge on Analytical 2D mean to osculating conversion after {0} iterations. "
                    + "Use method setThreshold() to change convergence threshold."),
    /** Message. */
    MNT_INCONSISTENT_NUMBER_OF_VERTICES("Failed to load mesh: inconsistent number of vertices."),
    /** Message. */
    MNT_INCONSISTENT_NUMBER_OF_TRIANGLES("Failed to load mesh: inconsistent number of triangles."),
    /** Message. */
    FAILED_TO_LOAD_MESH("Failed to load mesh from file {0}."),
    /** Message. */
    FAILED_TO_WRITE_MESH("Failed to write mesh in file {0}."),
    /** Message. */
    MNT_SURFACE_NOT_VISIBLE_FROM_SENSOR("Surface is not visible from sensor."),
    /** Message. */
    COWELL_REQUIRES_CARTESIAN_COORDINATES("Cowell integrator requires propagation is cartesian coordinates."),
    /** Message. */
    NOT_SECOND_ORDER_EQUATIONS("Cowell integrator requires a set of second order equations."),
    /** Message. */
    COWELL_ORDER("Cowell integrator order should be lower than 20."),
    /** Message. */
    INFEASIBLE_PROBLEM("Infeasible problem."),
    /** Message. */
    KKT_SOLUTION_FAILED("KKT solution failed."),
    /** Message. */
    INITIAL_POINT_NOT_FEASIBLE("The initial point is not feasible."),
    /** Message. */
    INITIAL_POINT_FAILED("Failed to find an initial feasible point."),
    /** Message. */
    UNBOUNDED_PROBLEM("Unbounded problem"),
    /** Message. */
    OPTIMIZATION_FAILED("Optimization failed: impossible to remain within the faesible region"),
    /** Message. */
    HESSIAN_NULL_LP("Hessians are null for linear problems"),
    /** Message. */
    GRADIENT_NULL_LP("GradFi are not used for linear problems"),
    /** Message. */
    UNSOLVABLE_PROBLEM("Unsolvable problem"),
    /** Message. */
    FAILED_PROBLEM("Failed to solve the problem"),
    /** Message. */
    FAILED_RANK("Equalities matrix A must be pxn with rank(A) = p < n"),
    /** Message. */
    FAILED_FULL_RANK("Equalities matrix A must have full rank: rankAT < p"),
    /** Message. */
    UNKNOWN_TIMESCALE("Unknown time scale: {0}."),
    /** Message. */
    FAILED_TO_LOAD_GRID_FILE("Failed to load grid attraction data from file {0}."),
    /** Message. */
    LEG_CANNOT_BE_NULL("Leg cannot be null."),
    /** Message. */
    SEQUENCE_MUST_BE_EMPTY("Sequence must be empty during the leg time interval."),
    /** Message. */
    LEG_ALREADY_IN_SEQUENCE("Leg already in the sequence."),
    /** Message. */
    LEG_NOT_IN_SEQUENCE("Leg not in sequence."),
    /** Message. */
    UNAVAILABLE_FACETBODYSHAPE_INTERSECTION_POINT_METHOD("Method getIntersectionPoint(altitude) is not available "
            + "for FacetBodyShape objects."),
    /** Message. */
    CHEBYCHEV_POLYNOMIALS_NOT_SAME_RANGE(
            "The first Chebyshev polynomial range [{0}, {1}] is not the same as the second Chebyshev polynomial "
                    + "range [{2}, {3}]."),
    /** Message. */
    CHEBYCHEV_POLYNOMIAL_OUT_OF_RANGE("The argument {0} is not in the Chebyshev polynomial range [{1}, {2}]."),
    /** Message. */
    PAST_INFINITY_DATE_NOT_ALLOWED("PAST_INFINITY date is not allowed."),
    /** Message. */
    FUTURE_INFINITY_DATE_NOT_ALLOWED("FUTURE_INFINITY date is not allowed."),
    /** Message. */
    INVALID_STATE_VECTOR_INDEX("Invalid state vector index ({0} is not between 0 and 5)."),
    /** Message. */
    NULL_LINK_TYPE("The link type is null."),
    /** Message. */
    UNDEFINED_FRAME("The {0} is undefined."),
    /** Message. */
    INVALID_MARGIN_VALUE("Invalid margin value {0}."),
    /** Message. */
    FACET_ORIENTATION_MISMATCH(
            "Facets orientation mismatch around edge joining points ({0}, {1}, {2}) and ({3}, {4}, {5})."),
    /** Message. */
    OUT_OF_PLANE("The point ({0}, {1}, {2}) is out of plane."),
    /** Message. */
    EDGE_CONNECTED_TO_ONE_FACET(
            "The edge joining points ({0}, {1}, {2}) and ({3}, {4}, {5}) is connected to one facet only."),
    /** Message. */
    FACET_WITH_SEVERAL_BOUNDARY_LOOPS("a facet has several boundary loops."),
    /** Message. */
    CLOSE_VERTICES("Too close vertices near point ({0}, {1}, {2})."),
    /** Message. */
    UNAVAILABLE_JACOBIAN_FOR_GRID_MODEL("Jacobian is not defined for grid attraction model."),
    /** Message. */
    UNAVAILABLE_JACOBIAN_FOR_DROZINER_MODEL("Jacobian is not defined for Droziner attraction model."),
    /** Message. */
    FOV_UNION_OR_INTERSECTION_TOO_COMPLEX("The union or intersection is too complex and connot be computed."),
    /** Message. */
    BENT_MODEL_NOT_ELLIPSOID_POINT(
            "Bent model should be built with a TopocentricFrame associated to a EllipsoidPoint only."),
    /** Message. */
    NOT_CELESTIALBODY_FRAME(
            "In this configuration, TopocentricFrame should be associated to a CelestialBodyFrame only."),
    /** Invalid faces message (intersecting faces). */
    INVALID_FACES("The faces created by the given directions intersect each other."),
    /** Invalid week beginning date message. */
    WEEK_BEGINNING_DATE_INVALID("The week beginning date is invalid with the epoch date of {0}: {1} TAI."),
    /** CNAV model cannot be used for a Galileo satellite */
    CNAV_FOR_GALILEO_ERROR("CNAV broadcast model cannot be used with Galileo satellites."),
    /** Dimension mismatch for linear regression */
    DIMENSION_MISMATCH_REGRESSION("Abscissa and ordinates do not have the same dimensions for a linear regression"),
    /** Wrong quadratic equation */
    WRONG_QUADRATIC_EQUATION("Wrong quadratic equation: quadratic and linear coefficients are both equal to zero"),
    /** Message */
    SPICE_ERROR_EXCEPTION("Spice SpiceErrorException: {0}"),
    /** Message */
    FAILURE_TO_LOAD_LIBRAIRY("Failed to load dynamic library {0}: {1}"),
    /** Message */
    IO_EXCEPTION("IOException: {0}"),
    /** Message */
    UNKNOWN_FRAME_ID("The frame NAIF ID is unknown: {0}"),
    /** Message. */
    UNKNOWN_BODY_ID("The body NAIF ID is unknown: {0}"),
    /** Message. */
    UNKNOWN_MAPPING_BODY_CODE_NAME("The given body code {0} does not "
            + "correspond to a body name (String), please load it using method addSpiceBodyMapping"),
    /** Message. */
    UNKNOWN_MAPPING_SPICE_ID_PAIR_PATRIUS_FRAME("Unknown NAIF IDs pair [{0}, {1}] in file "
            + "spiceIDsToPatriusFramesMapping.properties. It should be defined in the user mapping"),
    /** Message. */
    UNKNOWN_MAPPING_PATRIUS_FRAME_SPICE_ID("No NAIF ID is defined for Frame {0} "
            + "in file patriusFrameToSpiceID.properties. It should be defined in the user mapping"),
    /** Message. */
    UNKNOWN_SPK_DATA_TYPE("Unknown SPK data type: type {0}"),
    /** Message. */
    OUT_OF_RANGE_DATE_FOR_EPHEMERIS_DATA_MODEL(
            "The date {0} is outside the interval of validity of the ephemeris data model"),
    /** Message. */
    NO_BSP_EPHEMERIDES_BINARY_FILES_FOUND("No BSP ephemeris binary files found"),
    /** Body not available in the bsp file*/
    BODY_NOT_AVAILABLE_IN_BSP_FILE("Body {0} is not available in BSP ephemeris file"),
    /** Message. */
    WRONG_FILE_FORMAT("There is a problem with the file format: {0}"),
    /** Filter type not found in the FilterType enum */
    MISSING_FILTER_TYPE("Filter type {0} does not exist."),
    /** Message. */
    SPICE_INITIALIZE_ERROR("Error when loading Spice shared library."),
    /** Message. */
    SPICE_INITIALIZE_MAPPING_ERROR("Error when initializing the Spice mapping."),
    /** Wrong IAUPoleModelType indicated */
    INVALID_IAUPOLEMODELTYPE("The IAUPoleModelType given as input is not accepted."),
    /** The input mesh is not star convex */
    INVALID_MESH_STARCONVEX("The input mesh does not correspond to a star convex facet body shape."),
    /** The entered body point is not an EllipsoidPoint instance. */
    EXPECT_ELLIPSOIDPOINT_INSTANCE("The entered body point is not an EllipsoidPoint instance."),
    /** The entered body point is not a FacetPoint instance. */
    EXPECT_FACETPOINT_INSTANCE("The entered body point is not a FacetPoint instance."),
    /** Ellipsodetic coordinates only available on ellipsoids. */
    ELLIPSODETIC_ONLY_ON_ELLIPSOIDS("Ellipsodetic coordinates only available on ellipsoids."),
    /** The two points aren't associated to the same body shape. */
    NOT_ASSOCIATED_SAME_BODYSHAPE("The two points aren't associated to the same body shape."),
    /** Message. */
    UNKNOWN_BODY("Body name is unknown: {0}");

    /**
     * Bogus string constant.
     */
    private static final String DOUBLEX = "xx";

    /**
     * Source English format.
     */
    private final String source;

    /**
     * Constructor.<br>
     * Initialises source English format.
     *
     * @param sourceFormat source English format to use when no localized version is available
     *
     * @since 1.0
     */
    private PatriusMessages(final String sourceFormat) {
        this.source = sourceFormat;
    }

    /** {@inheritDoc} */
    @Override
    public String getSourceString() {
        return this.source;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalizedString(final Locale locale) {
        String result;
        result = this.source;
        try {
            final ResourceBundle bundle = ResourceBundle.getBundle("META-INF/localization/PatriusMessages", locale);
            if (bundle.getLocale().getLanguage().equals(locale.getLanguage())) {
                // the value of the resource is the translated format
                result = bundle.getString(this.toString());
            } else {
                // we throw an MRE in case the language does not exist
                // so we are sure to go through the catch block.
                throw new MissingResourceException(DOUBLEX, DOUBLEX, DOUBLEX);
            }
        } catch (final MissingResourceException mre) {
            // do not send this exception
            // if the resource is unknown don't translate and fall back to using the source format
            result = this.source;
        }
        return result;
    }
}
