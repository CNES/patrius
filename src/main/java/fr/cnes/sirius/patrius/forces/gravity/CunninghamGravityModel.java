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
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11.1:DM:DM-95:30/06/2023:[PATRIUS] Utilisation de types gen. dans les classes internes d'interp. de AbstractEOPHistory
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::DM:90:03/10/2013:moved main computation method to GravityToolbox
 * VERSION::FA:228:26/03/2014:Corrected partial derivatives computation
 * VERSION::FA:93:31/03/2014:changed api for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:280:08/10/2014:propagator modified in order to use the mu of gravitational forces
 * VERSION::FA:372:27/11/2014:Newtonian attraction bug
 * VERSION::FA:423:17/11/2015: improve computation times
 * VERSION::DM:505:19/08/2015:corrected addDAccDParam exception
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:1267:09/03/2018: Addition of getters for C and CS tables
 * VERSION::DM:1489:07/06/2018:add GENOPUS Custom classes
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represents the gravitational field of a celestial body.
 * <p>
 * The algorithm implemented in this class has been designed by Leland E. Cunningham (Lockheed Missiles and Space
 * Company, Sunnyvale and Astronomy Department University of California, Berkeley) in his 1969 paper:
 * <em>On the computation of the spherical harmonic
 * terms needed during the numerical integration of the orbital motion
 * of an artificial satellite</em> (Celestial Mechanics 2, 1970).
 * </p>
 * 
 * <p>
 * Warning: using a 0x0 Earth potential model is equivalent to a simple Newtonian attraction. However computation times
 * will be much slower since this case is not particularized and hence conversion from body frame (often ITRF) to
 * integration frame is necessary.
 * </p>
 * 
 * @author Fabien Maussion
 * @author Luc Maisonobe
 * @author V&eacute;ronique Pommier-Maurussane
 */
@SuppressWarnings("PMD.NullAssignment")
public class CunninghamGravityModel extends AbstractHarmonicGravityModel {

    /** Parameter name for equatorial radius. */
    public static final String RADIUS = "equatorial radius";

    /** Serializable UID. */
    private static final long serialVersionUID = 759122284106467933L;

    /** First normalized potential tesseral coefficients array. */
    private final double[][] c;

    /** Second normalized potential tesseral coefficients array. */
    private final double[][] s;

    /**
     * First normalized potential tesseral coefficients array for acceleration partial derivatives
     * with respect to state computation.
     */
    private final double[][] cPD;

    /**
     * Second normalized potential tesseral coefficients array for acceleration partial derivatives
     * with respect to state computation.
     */
    private final double[][] sPD;

    /** Equatorial radius parameter. */
    private final Parameter paramAe;

    /** Degree of potential. */
    private final int degree;

    /** Order of potential. */
    private final int order;

    /**
     * Creates a new instance.
     * 
     * @param centralBodyFrame rotating body frame
     * @param equatorialRadius reference equatorial radius of the potential
     * @param mu central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param cIn un-normalized coefficients array (cosine part)
     * @param sIn un-normalized coefficients array (sine part)
     * 
     * @exception IllegalArgumentException if coefficients array do not match
     */
    public CunninghamGravityModel(final Frame centralBodyFrame, final double equatorialRadius,
                                  final double mu, final double[][] cIn, final double[][] sIn) {
        this(centralBodyFrame, equatorialRadius, mu, cIn, sIn, cIn.length < 1 ? 0 : cIn.length - 1,
                cIn.length < 1 ? 0 : cIn[cIn.length - 1].length - 1);
    }

    /**
     * Creates a new instance.
     * 
     * @param centralBodyFrame rotating body frame
     * @param equatorialRadius reference equatorial radius of the potential
     * @param mu central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param cIn un-normalized coefficients array (cosine part)
     * @param sIn un-normalized coefficients array (sine part)
     * @param degreePD degree for partial derivatives
     * @param orderPD order for partial derivatives
     * @exception IllegalArgumentException if coefficients array do not match or degree and/or order
     *            for partial derivatives is higher than degree and/or order for acceleration
     */
    public CunninghamGravityModel(final Frame centralBodyFrame, final double equatorialRadius,
                                  final double mu, final double[][] cIn, final double[][] sIn, final int degreePD,
                                  final int orderPD) {
        this(centralBodyFrame, new Parameter(RADIUS, equatorialRadius), new Parameter(MU, mu), cIn,
                sIn, degreePD, orderPD);
    }

    /**
     * Creates a new instance using {@link Parameter}.
     * 
     * @param centralBodyFrame rotating body frame
     * @param equatorialRadius reference equatorial radius of the potential
     * @param mu parameter representing central body attraction coefficient
     *        (m<sup>3</sup>/s<sup>2</sup>)
     * @param cIn parameter representing un-normalized coefficients array (cosine part)
     * @param sIn un-normalized coefficients array (sine part)
     * @exception IllegalArgumentException if coefficients array do not match
     */
    public CunninghamGravityModel(final Frame centralBodyFrame,
                                  final Parameter equatorialRadius, final Parameter mu, final double[][] cIn,
                                  final double[][] sIn) {
        this(centralBodyFrame, equatorialRadius, mu, cIn, sIn, cIn.length < 1 ? 0 : cIn.length - 1,
                cIn.length < 1 ? 0 : cIn[cIn.length - 1].length - 1);
    }

    /**
     * Creates a new instance using {@link Parameter}.
     * 
     * @param centralBodyFrame rotating body frame
     * @param equatorialRadius reference equatorial radius of the potential
     * @param mu parameter representing central body attraction coefficient
     *        (m<sup>3</sup>/s<sup>2</sup>)
     * @param cIn parameter representing un-normalized coefficients array (cosine part)
     * @param sIn un-normalized coefficients array (sine part)
     * @param degreePD degree for partial derivatives
     * @param orderPD order for partial derivatives
     * @exception IllegalArgumentException if coefficients array do not match or degree and/or order
     *            for partial derivatives is higher than degree and/or order for acceleration
     */
    public CunninghamGravityModel(final Frame centralBodyFrame,
                                  final Parameter equatorialRadius, final Parameter mu, final double[][] cIn,
                                  final double[][] sIn, final int degreePD, final int orderPD) {
        this(centralBodyFrame, equatorialRadius, mu, cIn, sIn, degreePD, orderPD, true);
    }

    /**
     * Creates a new instance using {@link Parameter}.
     * 
     * @param centralBodyFrame rotating body frame
     * @param equatorialRadius reference equatorial radius of the potential
     * @param mu parameter representing central body attraction coefficient
     *        (m<sup>3</sup>/s<sup>2</sup>)
     * @param cIn parameter representing un-normalized coefficients array (cosine part)
     * @param sIn un-normalized coefficients array (sine part)
     * @param degreePD degree for partial derivatives
     * @param orderPD order for partial derivatives
     * @param centralTermContributionIn true if central term contribution should be considered (by default), false if
     *        not
     * @exception IllegalArgumentException if coefficients array do not match or degree and/or order
     *            for partial derivatives is higher than degree and/or order for acceleration
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    public CunninghamGravityModel(final Frame centralBodyFrame,
                                  final Parameter equatorialRadius, final Parameter mu, final double[][] cIn,
                                  final double[][] sIn, final int degreePD, final int orderPD,
                                  final boolean centralTermContributionIn) {
        // CHECKSTYLE: resume CyclomaticComplexity check
        super(centralBodyFrame, mu);
        this.paramAe = equatorialRadius;
        setCentralTermContribution(centralTermContributionIn);

        if (cIn.length < 1) {
            // C size is 0, the degree is zero:
            this.c = new double[1][1];
            this.s = new double[1][1];
            this.degree = 0;
            this.order = 0;
        } else {
            this.degree = cIn.length - 1;
            this.order = cIn[this.degree].length - 1;

            // check the C and S matrix dimension is the same, otherwise throw an exception:
            if ((cIn.length != sIn.length)
                    || (cIn[cIn.length - 1].length != sIn[sIn.length - 1].length)) {
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.POTENTIAL_ARRAYS_SIZES_MISMATCH, cIn.length,
                    cIn[this.degree].length, sIn.length, sIn[this.degree].length);
            }

            // invert the arrays (optimization for later "line per line" seeking)
            this.c = new double[cIn[this.degree].length][cIn.length];
            this.s = new double[sIn[this.degree].length][sIn.length];

            for (int i = 0; i <= this.degree; i++) {
                final double[] cT = cIn[i];
                final double[] sT = sIn[i];
                for (int j = 0; j < cT.length; j++) {
                    this.c[j][i] = cT[j];
                    this.s[j][i] = sT[j];
                }
            }
        }

        // C[0][0] = 0 in order not to compute keplerian evolution (managed by propagator):
        this.c[0][0] = 0.0;

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

            this.cPD = new double[orderPD + 1][degreePD + 1];
            this.sPD = new double[orderPD + 1][degreePD + 1];
            for (int i = 0; i <= orderPD; i++) {
                final double[] cT = this.c[i];
                final double[] sT = this.s[i];
                for (int j = 0; j <= degreePD; j++) {
                    this.cPD[i][j] = cT[j];
                    this.sPD[i][j] = sT[j];
                }
            }

        } else {
            this.cPD = null;
            this.sPD = null;
        }
    }

    /** {@inheritDoc}. */
    @Override
    public Vector3D computeNonCentralTermsAcceleration(final Vector3D positionInBodyFrame, final AbsoluteDate date)
        throws PatriusException {
        return GravityToolbox.computeCunninghamAcceleration(positionInBodyFrame,
            this.paramAe.getValue(), this.c, this.s, this.degree, this.order, getMu());
    }

    /** {@inheritDoc}. */
    @Override
    public final double[][] computeNonCentralTermsDAccDPos(final Vector3D positionInBodyFrame,
                                                           final AbsoluteDate date)
        throws PatriusException {
        if (this.cPD == null || this.sPD == null) {
            return new double[3][3];
        }

        // partial derivatives in body frame
        return GravityToolbox.computeDAccDPos(positionInBodyFrame, this.paramAe.getValue(),
            getMu(), this.cPD, this.sPD);
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
