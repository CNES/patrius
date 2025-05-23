/**
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
 *
 * @history Created 19/07/2012
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le
 * propagateur
 * VERSION:4.11.1:DM:DM-95:30/06/2023:[PATRIUS] Utilisation de types gen. dans les classes internes d'interp. de
 * AbstractEOPHistory
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300)
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:90:03/10/2013:Created normalized attraction model
 * VERSION::FA:183:14/03/2014:Improved javadoc
 * VERSION::FA:228:26/03/2014:Corrected partial derivatives computation
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:01/10/2014:New architecure for parameterizable Parameters
 * VERSION::FA:280:08/10/2014:propagator modified in order to use the mu of gravitational forces
 * VERSION::FA:295:24/10/2014:order null computation modified
 * VERSION::FA:372:27/11/2014:Newtonian attraction bug
 * VERSION::FA:423:17/11/2015: improve computation times
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:1267:09/03/2018: Addition of getters for C and CS tables
 * VERSION::DM:1489:07/06/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.analysis.polynomials.HelmholtzPolynomial;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Computation of central body attraction with normalized coefficients and Helmholtz Polynomials.
 * 
 * <p>
 * The algorithm implemented in this class has been designed by Balmino Georges (Observatoire Midi-Pyrénées/ Groupe de
 * Recherche de Géodésie Spatiale (GRGS) / Centre National d’Etudes Spatiales (CNES), France) in his 1990 paper:
 * <em>Non-singular formulation of the gravity vector and gravity gradient tensor in spherical harmonics.</em> (Manuscr.
 * Geod., Vol. 15, No. 1, p. 11 - 16, 02/1990). It uses normalized C and S coefficients for greater accuracy.
 * </p>
 * 
 * <p>
 * Warning: using a 0x0 Earth potential model is equivalent to a simple Newtonian attraction. However computation times
 * will be much slower since this case is not particularized and hence conversion from body frame (often ITRF) to
 * integration frame is necessary.
 * </p>
 * 
 * @concurrency not thread-safe
 * @concurrency uses internal mutable attributes
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $Id: BalminoAttractionModel.java 18094 2017-10-02 17:18:57Z bignon $
 */
@SuppressWarnings("PMD.NullAssignment")
public class BalminoGravityModel extends AbstractHarmonicGravityModel {

    /** Parameter name for equatorial radius. */
    public static final String RADIUS = "equatorial radius";

    /** Serializable UID. */
    private static final long serialVersionUID = 3308248172613083145L;

    /** Normalized C coefficients. */
    private final double[][] c;

    /** Normalized S coefficients. */
    private final double[][] s;

    /** Helmholtz polynomials. */
    private final HelmholtzPolynomial poly;

    /**
     * Denormalized coefficients for acceleration partial derivatives with respect to state
     * computation.
     */
    private double[][] denCPD;
    /**
     * Denormalized coefficients for acceleration partial derivatives with respect to state
     * computation.
     */
    private double[][] denSPD;

    /** Equatorial radius parameter. */
    private final Parameter paramAe;

    /** Max order. */
    private final int order;

    /** Max degree. */
    private final int degree;

    /**
     * Create an instance of a normalized gravity computation model using normalized coefficients.
     * 
     * @param frame central body frame
     * @param ae equatorial radius constant
     * @param mu standard gravitational constant
     * @param cCoefs Normalized c coefficients
     * @param sCoefs Normalized s coefficients
     */
    public BalminoGravityModel(final Frame frame, final double ae, final double mu,
                               final double[][] cCoefs, final double[][] sCoefs) {
        this(frame, ae, mu, cCoefs, sCoefs, cCoefs.length < 1 ? 0 : cCoefs.length - 1,
                cCoefs.length < 1 ? 0 : cCoefs[cCoefs.length - 1].length - 1);
    }

    /**
     * Create an instance of a normalized gravity computation model using normalized coefficients.
     * 
     * @param frame central body frame
     * @param ae equatorial radius constant
     * @param mu standard gravitational constant
     * @param cCoefs Normalized c coefficients for acceleration computation
     * @param sCoefs Normalized s coefficients for acceleration computation
     * @param degreePD degree for partial derivatives
     * @param orderPD order for partial derivatives
     */
    public BalminoGravityModel(final Frame frame, final double ae, final double mu,
                               final double[][] cCoefs, final double[][] sCoefs, final int degreePD,
                               final int orderPD) {
        this(frame, new Parameter(RADIUS, ae), new Parameter(MU, mu), cCoefs, sCoefs, degreePD,
                orderPD);
    }

    /**
     * Create an instance of a normalized gravity computation model using normalized coefficients.
     * 
     * @param frame central body frame
     * @param ae parameter storing equatorial radius
     * @param mu parameter storing standard gravitational
     * @param cCoefs Normalized c coefficients
     * @param sCoefs Normalized s coefficients
     */
    public BalminoGravityModel(final Frame frame, final Parameter ae, final Parameter mu,
                               final double[][] cCoefs, final double[][] sCoefs) {
        this(frame, ae, mu, cCoefs, sCoefs, cCoefs.length < 1 ? 0 : cCoefs.length - 1,
                cCoefs.length < 1 ? 0 : cCoefs[cCoefs.length - 1].length - 1);
    }

    /**
     * Create an instance of a normalized gravity computation model using normalized coefficients.
     * 
     * @param frame central body frame
     * @param ae parameter storing equatorial radius
     * @param mu parameter storing standard gravitational
     * @param cCoefs Normalized c coefficients for acceleration computation
     * @param sCoefs Normalized s coefficients for acceleration computation
     * @param degreePD degree for partial derivatives
     * @param orderPD order for partial derivatives
     */
    public BalminoGravityModel(final Frame frame, final Parameter ae, final Parameter mu,
                               final double[][] cCoefs, final double[][] sCoefs, final int degreePD,
                               final int orderPD) {
        this(frame, ae, mu, cCoefs, sCoefs, degreePD, orderPD, true);
    }

    /**
     * Create an instance of a normalized gravity computation model using normalized coefficients.
     * 
     * @param frame central body frame
     * @param ae parameter storing equatorial radius
     * @param mu parameter storing standard gravitational
     * @param cCoefs Normalized c coefficients for acceleration computation
     * @param sCoefs Normalized s coefficients for acceleration computation
     * @param degreePD degree for partial derivatives
     * @param orderPD order for partial derivatives
     * @param centralTermContributionIn true if central term contribution should be considered (by default), false if
     *        not
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    public BalminoGravityModel(final Frame frame, final Parameter ae, final Parameter mu,
                               final double[][] cCoefs, final double[][] sCoefs, final int degreePD,
                               final int orderPD, final boolean centralTermContributionIn) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        // No jacobian parameter is supported by this force model
        super(frame, mu);
        this.paramAe = ae;
        this.c = cCoefs;
        this.s = sCoefs;
        setCentralTermContribution(centralTermContributionIn);

        if (this.c.length < 1) {
            // C size is 0, the degree is zero:
            this.order = 0;
            this.degree = 0;
        } else {
            this.degree = cCoefs.length - 1;
            this.order = cCoefs[this.degree].length - 1;
        }

        // create the Helmholtz polynomial used to compute Balmino acceleration:
        if (this.order == 0 && this.degree != 0) {
            this.poly = new HelmholtzPolynomial(this.degree, this.order + 1);
        } else {
            this.poly = new HelmholtzPolynomial(this.degree, this.order);
        }

        // Build arrays for partial derivatives
        // Check that input values for degreePD and orderPD are positive
        if (degreePD < 0) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.NEGATIVE_DEGREE_FOR_PARTIAL_DERIVATIVES_COMPUTATION, degreePD);
        }

        if (orderPD < 0) {
            throw PatriusException.createIllegalArgumentException(
                PatriusMessages.NEGATIVE_ORDER_FOR_PARTIAL_DERIVATIVES_COMPUTATION, orderPD);
        }

        // Local variables for normalized C/S coefficients for acceleration partial
        // derivatives with respect to state computation
        final double[][] cPD = new double[degreePD + 1][];
        final double[][] sPD = new double[degreePD + 1][];
        if (degreePD > 0 || orderPD > 0) {

            // Check degree and order for partial derivatives are lower of equal to those for
            // acceleration
            if (degreePD > this.degree) {
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD_PD, this.degree, degreePD);
            }
            if (orderPD > this.order) {
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD_PD, this.order, orderPD);
            }

            // Truncate arrays C and S for partial derivatives

            for (int i = 0; i <= degreePD; i++) {
                final double[] ci = new double[MathLib.min(i, orderPD) + 1];
                final double[] si = new double[MathLib.min(i, orderPD) + 1];
                System.arraycopy(this.c[i], 0, ci, 0, ci.length);
                System.arraycopy(this.s[i], 0, si, 0, si.length);
                cPD[i] = ci;
                sPD[i] = si;
            }

            // Denormalize the C and S normalized coefficients
            final double[][] tempCPD = GravityToolbox.deNormalize(cPD);
            final double[][] tempSPD = GravityToolbox.deNormalize(sPD);

            // invert the arrays (optimization for later "line per line" seeking)
            this.denCPD = new double[cPD[cPD.length - 1].length][cPD.length];
            this.denSPD = new double[sPD[cPD.length - 1].length][sPD.length];

            for (int i = 0; i <= degreePD; i++) {
                final double[] cT = tempCPD[i];
                final double[] sT = tempSPD[i];
                for (int j = 0; j < cT.length; j++) {
                    this.denCPD[j][i] = cT[j];
                    this.denSPD[j][i] = sT[j];
                }
            }

            this.denCPD[0][0] = 0.0;
        }
    }

    /** {@inheritDoc}. */
    @Override
    public Vector3D computeNonCentralTermsAcceleration(final Vector3D positionInBodyFrame, final AbsoluteDate date)
        throws PatriusException {
        return GravityToolbox.computeBalminoAcceleration(positionInBodyFrame, this.c, this.s,
            this.getMu(), this.paramAe.getValue(), this.degree, this.order, this.poly);
    }

    /** {@inheritDoc}. */
    @Override
    public final double[][] computeNonCentralTermsDAccDPos(final Vector3D positionInBodyFrame,
                                                           final AbsoluteDate date)
        throws PatriusException {
        if (this.denCPD == null || this.denSPD == null) {
            return new double[3][3];
        }
        return GravityToolbox.computeDAccDPos(positionInBodyFrame, this.paramAe.getValue(),
            getMu(), this.denCPD, this.denSPD);
    }

    /**
     * Get the equatorial radius.
     * 
     * @return equatorial radius (m)
     */
    public double getAe() {
        return this.paramAe.getValue();
    }

    /**
     * Set the equatorial radius.
     * 
     * @param aeIn the equatorial radius.
     */
    public void setAe(final double aeIn) {
        this.paramAe.setValue(aeIn);
    }

    /**
     * @return the normalized C coefficients.
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[][] getC() {
        return this.c;
    }

    /**
     * @return the normalized S coefficients.
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public double[][] getS() {
        return this.s;
    }
}
