/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
/*
 * HISTORY
* VERSION:4.11:DM:DM-3311:22/05/2023:[PATRIUS] Evolutions mineures sur CelestialBody, shape et reperes
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3147:10/05/2022:[PATRIUS] Ajout a l'interface ITargetDirection d'une methode getTargetPvProv ...
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:FA:FA-2945:15/11/2021:[PATRIUS] Utilisation des degres dans des fonctions mathematiques 
 * VERSION:4.8:DM:DM-2992:15/11/2021:[PATRIUS] Possibilite d'interpoler l'attitude en ignorant les rotations rate 
 * VERSION:4.7:DM:DM-2703:18/05/2021: Acces facilite aux donnees d'environnement (application interplanetaire) 
 * VERSION:4.7:DM:DM-2684:18/05/2021:Création d'une classe UserIAUPole, à l'image de la classe  UserCelestialBody
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.7:DM:DM-2888:18/05/2021:ajout des derivee des angles alpha,delta,w  la classe UserIAUPole
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:FA:FA-1978:15/05/2019:Anomalie calcul orientation corps celeste (UAI)
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:769:17/02/2017:add UserDefinedCelestialBody
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import fr.cnes.sirius.patrius.bodies.IAUPoleFunction.IAUTimeDependency;
import fr.cnes.sirius.patrius.math.analysis.FunctionUtils;
import fr.cnes.sirius.patrius.math.analysis.function.CosineFunction;
import fr.cnes.sirius.patrius.math.analysis.function.SineFunction;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

//CHECKSTYLE: stop MultipleStringLiteralsCheck check
//Reason: code clarity

/**
 * Factory class for IAU poles.
 * <p>
 * The pole models provided here come from the 2009 report
 * "http://astropedia.astrogeology.usgs.gov/alfresco/d/d/
 * workspace/SpacesStore/28fd9e81-1964-44d6-a58b-fbbf61e64e15/WGCCRE2009reprint.pdf" and the 2011
 * erratum "http://astropedia.astrogeology.usgs.gov/alfresco/d/d/workspace/SpacesStore/
 * 04d348b0-eb2b-46a2-abe9-6effacb37763/WGCCRE-Erratum-2011reprint.pdf" of the IAU/IAG Working Group
 * on Cartographic Coordinates and Rotational Elements of the Planets and Satellites (WGCCRE). Note
 * that these value differ from earliest reports (before 2005).
 * </p>
 * 
 * <p>
 * By default, IAU pole data for planetary bodies (including Sun and Moon) are available in PATRIUS
 * through use of method {@link #getIAUPole(EphemerisType)}. Data come from the IAU 2009 working
 * group Technical Note (see above).
 * </p>
 * <p>
 * IAUPoleFactory does not allow user-defined data.
 * </p>
 * @author Luc Maisonobe
 * @since 5.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
// Reason: code clarity and simplicity
public final class IAUPoleFactory {

    /** Resources text file. */
    private static final String IAUPOLE_DATA = "/META-INF/iauPole.properties";

    /** Sun. */
    private static final String SUN = "Sun";

    /** Mercury. */
    private static final String MERCURY = "Mercury";

    /** Venus. */
    private static final String VENUS = "Venus";

    /** Earth. */
    private static final String EARTH = "Earth";

    /** Moon. */
    private static final String MOON = "Moon";

    /** Mars. */
    private static final String MARS = "Mars";

    /** Jupiter. */
    private static final String JUPITER = "Jupiter";

    /** Saturn. */
    private static final String SATURN = "Saturn";

    /** Uranus. */
    private static final String URANUS = "Uranus";

    /** Neptune. */
    private static final String NEPTUNE = "Neptune";

    /** Pluto. */
    private static final String PLUTO = "Pluto";

    /** Origin of values. */
    private static final String ORIGIN = "2009 NT from IAU/IAG Working Group (IAUPoleFactory)";

    /** IAU pole data stored as properties file. */
    private static Properties properties;

    /** IAU pole data for all celestial bodies. */
    private static Map<EphemerisType, IAUPole> poleData;

    static {
        // Load IAU pole data
        properties = new Properties();
        final InputStream stream = IAUPoleFactory.class.getResourceAsStream(IAUPOLE_DATA);
        try {
            properties.load(stream);
            stream.close();
        } catch (final FileNotFoundException e) {
            throw new PatriusRuntimeException("Unable to find resource " + IAUPOLE_DATA + " in classpath.", e);
        } catch (final IOException e) {
            throw new PatriusRuntimeException("Failed to read file " + IAUPOLE_DATA + ".", e);
        }

        // Build IAU pole objects and add to map
        poleData = new HashMap<>();
        poleData.put(EphemerisType.SUN, buildGeneric(SUN));
        poleData.put(EphemerisType.MERCURY, buildMercury());
        poleData.put(EphemerisType.VENUS, buildGeneric(VENUS));
        poleData.put(EphemerisType.EARTH, buildGeneric(EARTH));
        poleData.put(EphemerisType.MOON, buildMoon());
        poleData.put(EphemerisType.MARS, buildGeneric(MARS));
        poleData.put(EphemerisType.JUPITER, buildJupiter());
        poleData.put(EphemerisType.SATURN, buildGeneric(SATURN));
        poleData.put(EphemerisType.URANUS, buildGeneric(URANUS));
        poleData.put(EphemerisType.NEPTUNE, buildNeptune());
        poleData.put(EphemerisType.PLUTO, buildGeneric(PLUTO));
    }

    /**
     * Private constructor.
     * <p>
     * This class is a utility class, it should neither have a public nor a default constructor.
     * This private constructor prevents the compiler from generating one automatically.
     * </p>
     */
    private IAUPoleFactory() {
    }

    /**
     * Build generic IAUPole.
     * @param body body name
     * @return generic IAUPole
     */
    private static IAUPole buildGeneric(final String body) {
        // Alpha 0
        final List<IAUPoleFunction> alpha0List = new ArrayList<>();
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.CONSTANT,
                createPolynomialFunction(new double[] { get(body + ".alpha0_deg") }), IAUTimeDependency.DAYS));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createPolynomialFunction(new double[] { 0,
                get(body + ".alpha1_degpercentury") }), IAUTimeDependency.CENTURIES));
        final IAUPoleCoefficients1D alpha0Coeffs = new IAUPoleCoefficients1D(alpha0List);

        // Delta 0
        final List<IAUPoleFunction> delta0List = new ArrayList<>();
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.CONSTANT,
                createPolynomialFunction(new double[] { get(body + ".delta0_deg") }), IAUTimeDependency.DAYS));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createPolynomialFunction(new double[] { 0,
                get(body + ".delta1_degpercentury") }), IAUTimeDependency.CENTURIES));
        final IAUPoleCoefficients1D delta0Coeffs = new IAUPoleCoefficients1D(delta0List);

        // W
        final List<IAUPoleFunction> w0List = new ArrayList<>();
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.CONSTANT, createPolynomialFunction(new double[] { get(body
                + ".w0_deg") }), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createPolynomialFunction(new double[] { 0,
                get(body + ".w1_degperday") }), IAUTimeDependency.DAYS));
        final IAUPoleCoefficients1D wCoeffs = new IAUPoleCoefficients1D(w0List);

        // Build UserIAUPole
        return new UserIAUPole(new IAUPoleCoefficients(alpha0Coeffs, delta0Coeffs, wCoeffs)) {
            /** Serializable UID. */
            private static final long serialVersionUID = -6262157219961711147L;

            /** {@inheritDoc} */
            @Override
            public String toString() {
                return ORIGIN;
            }
        };
    }

    /**
     * Build Mercury IAUPole.
     * @return Mercury IAUPole
     */
    private static IAUPole buildMercury() {
        // Alpha 0
        final List<IAUPoleFunction> alpha0List = new ArrayList<>();
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.CONSTANT,
                createPolynomialFunction(new double[] { get(MERCURY + ".alpha0_deg") }), IAUTimeDependency.DAYS));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createPolynomialFunction(new double[] { 0,
                get(MERCURY + ".alpha1_degpercentury") }), IAUTimeDependency.CENTURIES));
        final IAUPoleCoefficients1D alpha0Coeffs = new IAUPoleCoefficients1D(alpha0List);

        // Delta 0
        final List<IAUPoleFunction> delta0List = new ArrayList<>();
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.CONSTANT,
                createPolynomialFunction(new double[] { get(MERCURY + ".delta0_deg") }), IAUTimeDependency.DAYS));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createPolynomialFunction(new double[] { 0,
                get(MERCURY + ".delta1_degpercentury") }), IAUTimeDependency.CENTURIES));
        final IAUPoleCoefficients1D delta0Coeffs = new IAUPoleCoefficients1D(delta0List);

        // W
        final List<IAUPoleFunction> w0List = new ArrayList<>();
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.CONSTANT,
                createPolynomialFunction(new double[] { get(MERCURY + ".w0_deg") }), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createPolynomialFunction(new double[] { 0,
                get(MERCURY + ".w1_degperday") }), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MERCURY + ".m1_coef_deg",
                MERCURY + ".m1_0_deg", MERCURY + ".m1_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MERCURY + ".m2_coef_deg",
                MERCURY + ".m2_0_deg", MERCURY + ".m2_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MERCURY + ".m3_coef_deg",
                MERCURY + ".m3_0_deg", MERCURY + ".m3_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MERCURY + ".m4_coef_deg",
                MERCURY + ".m4_0_deg", MERCURY + ".m4_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MERCURY + ".m5_coef_deg",
                MERCURY + ".m5_0_deg", MERCURY + ".m5_1_degperday"), IAUTimeDependency.DAYS));
        final IAUPoleCoefficients1D wCoeffs = new IAUPoleCoefficients1D(w0List);

        // Build UserIAUPole
        return new UserIAUPole(new IAUPoleCoefficients(alpha0Coeffs, delta0Coeffs, wCoeffs)) {
            /** Serializable UID. */
            private static final long serialVersionUID = -6262157219961711147L;

            /** {@inheritDoc} */
            @Override
            public String toString() {
                return ORIGIN;
            }
        };
    }

    /**
     * Build Jupiter IAUPole.
     * @return Jupiter IAUPole
     */
    private static IAUPole buildJupiter() {
        // Alpha 0
        final List<IAUPoleFunction> alpha0List = new ArrayList<>();
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.CONSTANT,
                createPolynomialFunction(new double[] { get(JUPITER + ".alpha0_deg") }), IAUTimeDependency.DAYS));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createPolynomialFunction(new double[] { 0,
                get(JUPITER + ".alpha1_degpercentury") }), IAUTimeDependency.CENTURIES));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(JUPITER
                + ".alpha_ja_coef_deg", JUPITER + ".ja0_deg", JUPITER + ".ja1_degpercentury"),
                IAUTimeDependency.CENTURIES));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(JUPITER
                + ".alpha_jb_coef_deg", JUPITER + ".jb0_deg", JUPITER + ".jb1_degpercentury"),
                IAUTimeDependency.CENTURIES));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(JUPITER
                + ".alpha_jc_coef_deg", JUPITER + ".jc0_deg", JUPITER + ".jc1_degpercentury"),
                IAUTimeDependency.CENTURIES));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(JUPITER
                + ".alpha_jd_coef_deg", JUPITER + ".jd0_deg", JUPITER + ".jd1_degpercentury"),
                IAUTimeDependency.CENTURIES));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(JUPITER
                + ".alpha_je_coef_deg", JUPITER + ".je0_deg", JUPITER + ".je1_degpercentury"),
                IAUTimeDependency.CENTURIES));
        final IAUPoleCoefficients1D alpha0Coeffs = new IAUPoleCoefficients1D(alpha0List);

        // Delta 0
        final List<IAUPoleFunction> delta0List = new ArrayList<>();
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.CONSTANT,
                createPolynomialFunction(new double[] { get(JUPITER + ".delta0_deg") }), IAUTimeDependency.DAYS));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createPolynomialFunction(new double[] { 0,
                get(JUPITER + ".delta1_degpercentury") }), IAUTimeDependency.CENTURIES));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createCosineFunction(JUPITER
                + ".delta_ja_coef_deg", JUPITER + ".ja0_deg", JUPITER + ".ja1_degpercentury"),
                IAUTimeDependency.CENTURIES));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createCosineFunction(JUPITER
                + ".delta_jb_coef_deg", JUPITER + ".jb0_deg", JUPITER + ".jb1_degpercentury"),
                IAUTimeDependency.CENTURIES));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createCosineFunction(JUPITER
                + ".delta_jc_coef_deg", JUPITER + ".jc0_deg", JUPITER + ".jc1_degpercentury"),
                IAUTimeDependency.CENTURIES));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createCosineFunction(JUPITER
                + ".delta_jd_coef_deg", JUPITER + ".jd0_deg", JUPITER + ".jd1_degpercentury"),
                IAUTimeDependency.CENTURIES));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createCosineFunction(JUPITER
                + ".delta_je_coef_deg", JUPITER + ".je0_deg", JUPITER + ".je1_degpercentury"),
                IAUTimeDependency.CENTURIES));
        final IAUPoleCoefficients1D delta0Coeffs = new IAUPoleCoefficients1D(delta0List);

        // W
        final List<IAUPoleFunction> w0List = new ArrayList<>();
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.CONSTANT,
                createPolynomialFunction(new double[] { get(JUPITER + ".w0_deg") }), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createPolynomialFunction(new double[] { 0,
                get(JUPITER + ".w1_degperday") }), IAUTimeDependency.DAYS));
        final IAUPoleCoefficients1D wCoeffs = new IAUPoleCoefficients1D(w0List);

        // Build UserIAUPole
        return new UserIAUPole(new IAUPoleCoefficients(alpha0Coeffs, delta0Coeffs, wCoeffs)) {
            /** Serializable UID. */
            private static final long serialVersionUID = -6262157219961711147L;

            /** {@inheritDoc} */
            @Override
            public String toString() {
                return ORIGIN;
            }
        };
    }

    /**
     * Build Moon IAUPole.
     * @return Moon IAUPole
     */
    // CHECKSTYLE: stop MethodLength check
    // Reason: code consistency with other methods
    private static IAUPole buildMoon() {
        // CHECKSTYLE: resume MethodLength check

        // Alpha 0
        final List<IAUPoleFunction> alpha0List = new ArrayList<>();
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.CONSTANT,
                createPolynomialFunction(new double[] { get(MOON + ".alpha0_deg") }), IAUTimeDependency.DAYS));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createPolynomialFunction(new double[] { 0,
                get(MOON + ".alpha1_degpercentury") }), IAUTimeDependency.CENTURIES));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".alpha_e0_deg",
                MOON + ".e0_0_deg", MOON + ".e0_1_degperday"), IAUTimeDependency.DAYS));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".alpha_e1_deg",
                MOON + ".e1_0_deg", MOON + ".e1_1_degperday"), IAUTimeDependency.DAYS));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".alpha_e2_deg",
                MOON + ".e2_0_deg", MOON + ".e2_1_degperday"), IAUTimeDependency.DAYS));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".alpha_e3_deg",
                MOON + ".e3_0_deg", MOON + ".e3_1_degperday"), IAUTimeDependency.DAYS));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".alpha_e5_deg",
                MOON + ".e5_0_deg", MOON + ".e5_1_degperday"), IAUTimeDependency.DAYS));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".alpha_e9_deg",
                MOON + ".e9_0_deg", MOON + ".e9_1_degperday"), IAUTimeDependency.DAYS));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".alpha_e12_deg",
                MOON + ".e12_0_deg", MOON + ".e12_1_degperday"), IAUTimeDependency.DAYS));
        final IAUPoleCoefficients1D alpha0Coeffs = new IAUPoleCoefficients1D(alpha0List);

        // Delta 0
        final List<IAUPoleFunction> delta0List = new ArrayList<>();
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.CONSTANT,
                createPolynomialFunction(new double[] { get(MOON + ".delta0_deg") }), IAUTimeDependency.DAYS));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createPolynomialFunction(new double[] { 0,
                get(MOON + ".delta1_degpercentury") }), IAUTimeDependency.CENTURIES));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createCosineFunction(MOON + ".delta_e0_deg",
                MOON + ".e0_0_deg", MOON + ".e0_1_degperday"), IAUTimeDependency.DAYS));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createCosineFunction(MOON + ".delta_e1_deg",
                MOON + ".e1_0_deg", MOON + ".e1_1_degperday"), IAUTimeDependency.DAYS));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createCosineFunction(MOON + ".delta_e2_deg",
                MOON + ".e2_0_deg", MOON + ".e2_1_degperday"), IAUTimeDependency.DAYS));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createCosineFunction(MOON + ".delta_e3_deg",
                MOON + ".e3_0_deg", MOON + ".e3_1_degperday"), IAUTimeDependency.DAYS));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createCosineFunction(MOON + ".delta_e5_deg",
                MOON + ".e5_0_deg", MOON + ".e5_1_degperday"), IAUTimeDependency.DAYS));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createCosineFunction(MOON + ".delta_e6_deg",
                MOON + ".e6_0_deg", MOON + ".e6_1_degperday"), IAUTimeDependency.DAYS));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createCosineFunction(MOON + ".delta_e9_deg",
                MOON + ".e9_0_deg", MOON + ".e9_1_degperday"), IAUTimeDependency.DAYS));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createCosineFunction(MOON + ".delta_e12_deg",
                MOON + ".e12_0_deg", MOON + ".e12_1_degperday"), IAUTimeDependency.DAYS));
        final IAUPoleCoefficients1D delta0Coeffs = new IAUPoleCoefficients1D(delta0List);

        // W
        final List<IAUPoleFunction> w0List = new ArrayList<>();
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.CONSTANT, createPolynomialFunction(new double[] { get(MOON
                + ".w0_deg") }), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createPolynomialFunction(new double[] { 0,
                get(MOON + ".w1_degperday"), get(MOON + ".w_d2_degperday2") }), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".w_e0_deg", MOON
                + ".e0_0_deg", MOON + ".e0_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".w_e1_deg", MOON
                + ".e1_0_deg", MOON + ".e1_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".w_e2_deg", MOON
                + ".e2_0_deg", MOON + ".e2_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".w_e3_deg", MOON
                + ".e3_0_deg", MOON + ".e3_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".w_e4_deg", MOON
                + ".e4_0_deg", MOON + ".e4_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".w_e5_deg", MOON
                + ".e5_0_deg", MOON + ".e5_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".w_e6_deg", MOON
                + ".e6_0_deg", MOON + ".e6_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".w_e7_deg", MOON
                + ".e7_0_deg", MOON + ".e7_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".w_e8_deg", MOON
                + ".e8_0_deg", MOON + ".e8_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".w_e9_deg", MOON
                + ".e9_0_deg", MOON + ".e9_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".w_e10_deg", MOON
                + ".e10_0_deg", MOON + ".e10_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".w_e11_deg", MOON
                + ".e11_0_deg", MOON + ".e11_1_degperday"), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(MOON + ".w_e12_deg", MOON
                + ".e12_0_deg", MOON + ".e12_1_degperday"), IAUTimeDependency.DAYS));
        final IAUPoleCoefficients1D wCoeffs = new IAUPoleCoefficients1D(w0List);

        // Build UserIAUPole
        return new UserIAUPole(new IAUPoleCoefficients(alpha0Coeffs, delta0Coeffs, wCoeffs)) {
            /** Serializable UID. */
            private static final long serialVersionUID = -6262157219961711147L;

            /** {@inheritDoc} */
            @Override
            public String toString() {
                return ORIGIN;
            }
        };
    }

    /**
     * Build Neptune IAUPole.
     * @return Neptune IAUPole
     */
    private static IAUPole buildNeptune() {
        // Alpha 0
        final List<IAUPoleFunction> alpha0List = new ArrayList<>();
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.CONSTANT,
                createPolynomialFunction(new double[] { get(NEPTUNE + ".alpha0_deg") }), IAUTimeDependency.DAYS));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createPolynomialFunction(new double[] { 0,
                get(NEPTUNE + ".alpha1_degpercentury") }), IAUTimeDependency.CENTURIES));
        alpha0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createSineFunction(NEPTUNE
                + ".alpha_coef_deg", NEPTUNE + ".n0_deg", NEPTUNE + ".n1_degpercentury"), IAUTimeDependency.CENTURIES));
        final IAUPoleCoefficients1D alpha0Coeffs = new IAUPoleCoefficients1D(alpha0List);

        // Delta 0
        final List<IAUPoleFunction> delta0List = new ArrayList<>();
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.CONSTANT,
                createPolynomialFunction(new double[] { get(NEPTUNE + ".delta0_deg") }), IAUTimeDependency.DAYS));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createPolynomialFunction(new double[] { 0,
                get(NEPTUNE + ".delta1_degpercentury") }), IAUTimeDependency.CENTURIES));
        delta0List.add(new IAUPoleFunction(IAUPoleFunctionType.HARMONICS, createCosineFunction(NEPTUNE
                + ".delta_coef_deg", NEPTUNE + ".n0_deg", NEPTUNE + ".n1_degpercentury"), IAUTimeDependency.CENTURIES));
        final IAUPoleCoefficients1D delta0Coeffs = new IAUPoleCoefficients1D(delta0List);

        // W
        final List<IAUPoleFunction> w0List = new ArrayList<>();
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.CONSTANT,
                createPolynomialFunction(new double[] { get(NEPTUNE + ".w0_deg") }), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createPolynomialFunction(new double[] { 0,
                get(NEPTUNE + ".w1_degperday") }), IAUTimeDependency.DAYS));
        w0List.add(new IAUPoleFunction(IAUPoleFunctionType.SECULAR, createSineFunction(NEPTUNE + ".w_coef_deg", NEPTUNE
                + ".n0_deg", NEPTUNE + ".n1_degpercentury"), IAUTimeDependency.CENTURIES));
        final IAUPoleCoefficients1D wCoeffs = new IAUPoleCoefficients1D(w0List);

        // Build UserIAUPole
        return new UserIAUPole(new IAUPoleCoefficients(alpha0Coeffs, delta0Coeffs, wCoeffs)) {
            /** Serializable UID. */
            private static final long serialVersionUID = -6262157219961711147L;

            /** {@inheritDoc} */
            @Override
            public String toString() {
                return ORIGIN;
            }
        };
    }

    /**
     * Get an IAU pole.
     * 
     * @param body
     *        body for which the pole is requested
     * @return IAU pole for the body, or dummy EME2000 aligned pole
     *         for barycenters
     */
    public static IAUPole getIAUPole(final EphemerisType body) {
        final IAUPole iauPole = poleData.get(body);
        if (iauPole == null) {
            return new GCRFAligned();
        }
        return iauPole;
    }

    /**
     * Return value from key in .properties file.
     * 
     * @param key
     *        key
     * @return value from key in .properties file
     */
    private static double get(final String key) {
        return Double.parseDouble(properties.getProperty(key));
    }

    /**
     * Create polynomial function in radians from provided coefficients in degrees.
     * @param polynomialCoefsDeg polynomial coefficients
     * @return polynomial function in radians
     */
    private static PolynomialFunction createPolynomialFunction(final double[] polynomialCoefsDeg) {
        final double[] polynomialCoefsRad = new double[polynomialCoefsDeg.length];
        for (int i = 0; i < polynomialCoefsDeg.length; i++) {
            polynomialCoefsRad[i] = MathLib.toRadians(polynomialCoefsDeg[i]);
        }
        return new PolynomialFunction(polynomialCoefsRad);
    }

    /**
     * Create Sine function from provided coefficients.
     * @param multiplicativeCoefName multiplicative coefficient
     * @param polynomialCoefsNames polynomial coefficients
     * @return Sine function
     */
    private static SineFunction createSineFunction(final String multiplicativeCoefName,
            final String... polynomialCoefsNames) {
        final double[] polynomialCoefs = new double[polynomialCoefsNames.length];
        for (int i = 0; i < polynomialCoefsNames.length; i++) {
            polynomialCoefs[i] = get(polynomialCoefsNames[i]);
        }
        final PolynomialFunction fDeg2Rad = new PolynomialFunction(new double[] { MathLib.PI / 180. });
        return new SineFunction(MathLib.toRadians(get(multiplicativeCoefName)), FunctionUtils.multiply(
                new PolynomialFunction(polynomialCoefs), fDeg2Rad));
    }

    /**
     * Create Cosine function from provided coefficients.
     * @param multiplicativeCoefName multiplicative coefficient
     * @param polynomialCoefsNames polynomial coefficients
     * @return Cosine function
     */
    private static CosineFunction createCosineFunction(final String multiplicativeCoefName,
            final String... polynomialCoefsNames) {
        final double[] polynomialCoefs = new double[polynomialCoefsNames.length];
        for (int i = 0; i < polynomialCoefsNames.length; i++) {
            polynomialCoefs[i] = get(polynomialCoefsNames[i]);
        }
        final PolynomialFunction fDeg2Rad = new PolynomialFunction(new double[] { MathLib.PI / 180. });
        return new CosineFunction(MathLib.toRadians(get(multiplicativeCoefName)), FunctionUtils.multiply(
                new PolynomialFunction(polynomialCoefs), fDeg2Rad));
    }

    /**
     * Default IAUPole implementation for barycenters.
     * <p>
     * This implementation defines directions such that the inertially oriented and body oriented
     * frames are identical and aligned with GCRF. It is used for example to define the ICRF.
     * </p>
     */
    private static class GCRFAligned implements IAUPole {

        /** Serializable UID. */
        private static final long serialVersionUID = 4148478144525077641L;

        /** {@inheritDoc} */
        @Override
        public Vector3D getPole(final AbsoluteDate date) {
            return Vector3D.PLUS_K;
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D getPole(final AbsoluteDate date, final IAUPoleModelType iauPoleType) {
            return Vector3D.PLUS_K;
        }

        /** {@inheritDoc} */
        @Override
        public double getPrimeMeridianAngle(final AbsoluteDate date) {
            return 0;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "GCRF-aligned";
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D getPoleDerivative(final AbsoluteDate date) {
            return Vector3D.PLUS_K;
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D getPoleDerivative(final AbsoluteDate date, final IAUPoleModelType iauPoleType) {
            return Vector3D.PLUS_K;
        }

        /** {@inheritDoc} */
        @Override
        public double getPrimeMeridianAngleDerivative(final AbsoluteDate date) {
            return 0;
        }

        /** {@inheritDoc} */
        @Override
        public double getPrimeMeridianAngle(final AbsoluteDate date, final IAUPoleModelType iauPoleType) {
            return 0.;
        }

        /** {@inheritDoc} */
        @Override
        public double getPrimeMeridianAngleDerivative(final AbsoluteDate date, final IAUPoleModelType iauPoleType) {
            return 0.;
        }
    }

    // CHECKSTYLE: resume MultipleStringLiteralsCheck check
}
