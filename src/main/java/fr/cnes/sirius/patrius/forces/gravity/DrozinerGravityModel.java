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
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::DM:90:03/10/2013:moved main computation method to GravityToolbox
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:280:08/10/2014:propagator modified in order to use the mu of gravitational forces
 * VERSION::FA:372:27/11/2014:Newtonian attraction bug
 * VERSION::FA:423:17/11/2015: improve computation times
 * VERSION::DM:534:10/02/2016:Parametrization of force models
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
 * The algorithm implemented in this class has been designed by Andrzej Droziner (Institute of Mathematical Machines,
 * Warsaw) in his 1976 paper: <em>An algorithm for recurrent calculation of gravitational
 * acceleration</em> (artificial satellites, Vol. 12, No 2, June 1977).
 * </p>
 * <p>
 * The implementation of this class enables the computation of partial derivatives by finite differences with respect to
 * the <b>central attraction coefficient</b>.
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
public class DrozinerGravityModel extends AbstractHarmonicGravityModel {

    /** Parameter name for equatorial radius. */
    public static final String RADIUS = "equatorial radius";

    /** Serializable UID. */
    private static final long serialVersionUID = -6897768625006106349L;

    /** Threshold. */
    private static final double THRESHOLD = 10e-2;

    /** First normalized potential tesseral coefficients array. */
    private final double[][] c;

    /** Second normalized potential tesseral coefficients array. */
    private final double[][] s;

    /** Equatorial radius parameter. */
    private final Parameter paramAe;

    /** Number of zonal coefficients. */
    private final int degree;

    /** Number of tesseral coefficients. */
    private final int order;

    /**
     * Creates a new instance.
     * 
     * @param centralBodyFrameIn rotating body frame
     * @param equatorialRadius reference equatorial radius of the potential
     * @param mu central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param cCoefs un-normalized coefficients array (cosine part)
     * @param sCoefs un-normalized coefficients array (sine part)
     * @exception IllegalArgumentException if coefficients array do not match
     */
    public DrozinerGravityModel(final Frame centralBodyFrameIn, final double equatorialRadius,
                                final double mu, final double[][] cCoefs, final double[][] sCoefs) {
        // storing Mu in the map of parameters
        this(centralBodyFrameIn, new Parameter(RADIUS, equatorialRadius), new Parameter(MU, mu),
                cCoefs, sCoefs);
    }

    /**
     * Creates a new instance using {@link Parameter}.
     * 
     * @param centralBodyFrameIn rotating body frame
     * @param equatorialRadius parameter representing reference equatorial radius of the potential
     * @param mu parameter representing central body attraction coefficient
     *        (m<sup>3</sup>/s<sup>2</sup>)
     * @param cCoefs un-normalized coefficients array (cosine part)
     * @param sCoefs un-normalized coefficients array (sine part)
     * @exception IllegalArgumentException if coefficients array do not match
     */
    public DrozinerGravityModel(final Frame centralBodyFrameIn,
                                final Parameter equatorialRadius, final Parameter mu, final double[][] cCoefs,
                                final double[][] sCoefs) {
        this(centralBodyFrameIn, equatorialRadius, mu, cCoefs, sCoefs, true);
    }

    /**
     * Creates a new instance using {@link Parameter}.
     * 
     * @param centralBodyFrameIn rotating body frame
     * @param equatorialRadius parameter representing reference equatorial radius of the potential
     * @param mu parameter representing central body attraction coefficient
     *        (m<sup>3</sup>/s<sup>2</sup>)
     * @param cCoefs un-normalized coefficients array (cosine part)
     * @param sCoefs un-normalized coefficients array (sine part)
     * @param centralTermContributionIn true if central term contribution should be considered (by default), false if
     *        not
     * @exception IllegalArgumentException if coefficients array do not match
     */
    public DrozinerGravityModel(final Frame centralBodyFrameIn,
                                final Parameter equatorialRadius, final Parameter mu, final double[][] cCoefs,
                                final double[][] sCoefs, final boolean centralTermContributionIn) {
        // storing Mu in the map of parameters
        super(centralBodyFrameIn, mu);
        this.paramAe = equatorialRadius;
        setCentralTermContribution(centralTermContributionIn);

        if (cCoefs.length < 1) {
            // C size is 0, the degree is zero:
            this.c = new double[1][1];
            this.s = new double[1][1];
            this.degree = 0;
            this.order = 0;
        } else {
            this.degree = cCoefs.length - 1;
            this.order = cCoefs[this.degree].length - 1;
            // check the C and S matrix dimension is the same, otherwise throw an exception:
            if ((cCoefs.length != sCoefs.length)
                    || (cCoefs[cCoefs.length - 1].length != sCoefs[sCoefs.length - 1].length)) {
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.POTENTIAL_ARRAYS_SIZES_MISMATCH, cCoefs.length,
                    cCoefs[this.degree].length, sCoefs.length, sCoefs[this.degree].length);
            }

            // invert the arrays (optimization for later "line per line" seeking)
            this.c = new double[cCoefs[this.degree].length][cCoefs.length];
            this.s = new double[sCoefs[this.degree].length][sCoefs.length];
            for (int i = 0; i <= this.degree; i++) {
                final double[] cT = cCoefs[i];
                final double[] sT = sCoefs[i];
                for (int j = 0; j < cT.length; j++) {
                    this.c[j][i] = cT[j];
                    this.s[j][i] = sT[j];
                }
            }
        }
    }

    /** {@inheritDoc}. */
    @Override
    public Vector3D computeNonCentralTermsAcceleration(final Vector3D positionInBodyFrame, final AbsoluteDate date)
        throws PatriusException {
        return GravityToolbox.computeDrozinerAcceleration(positionInBodyFrame, this.c, this.s,
            this.getMu(), this.paramAe.getValue(), THRESHOLD, this.degree, this.order);
    }

    /** {@inheritDoc}. */
    @Override
    public final double[][] computeNonCentralTermsDAccDPos(final Vector3D positionInBodyFrame,
                                                           final AbsoluteDate date)
        throws PatriusException {
        // Return the acceleration derivatives with respect to position
        return new double[3][3];
    }

    /** {@inheritDoc}. */
    @Override
    public double[][] computeDAccDPos(final Vector3D positionInBodyFrame, final AbsoluteDate date)
        throws PatriusException {
        // Unavailable jacobian for Droziner gravity model
        throw new PatriusException(PatriusMessages.UNAVAILABLE_JACOBIAN_FOR_DROZINER_MODEL);
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
