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
 * @history Created 14/11/2012
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le
 * propagateur
 * VERSION:4.11.1:DM:DM-95:30/06/2023:[PATRIUS] Utilisation de types gen. dans les classes internes d'interp. de
 * AbstractEOPHistory
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.11:FA:FA-3312:22/05/2023:[PATRIUS] TrueInertialFrame pas vraiment pseudo-inertiel
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.7:DM:DM-2766:18/05/2021:Evol. et corr. dans le package fr.cnes.sirius.patrius.math.linear (suite DM 2300) 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::DM:90:15/10/2013:Using normalized gravitational attraction.
 * VERSION::FA:93:01/04/2014:Changed partial derivatives API
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:464:24/06/2015:Analytical computation of the partial derivatives
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.variations;

import java.util.Map;

import fr.cnes.sirius.patrius.forces.gravity.AbstractHarmonicGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.GravityToolbox;
import fr.cnes.sirius.patrius.forces.gravity.tides.PotentialTimeVariations;
import fr.cnes.sirius.patrius.forces.gravity.variations.coefficients.VariablePotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.gravity.variations.coefficients.VariablePotentialCoefficientsSet;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.analysis.polynomials.HelmholtzPolynomial;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represents a variable gravity field. It computes a static potential and a time variable potential. The C
 * and S coefficients array are computed according to the algorithm given by
 * 
 * @concurrency not thread-safe
 * @concurrency.comment not thread-safe because of global arrays
 * 
 * @see <a href="http://grgs.obs-mip.fr/grace/variable-models-grace-lageos/formats">GRACE / LAGEOS variable model</a>
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: VariablePotentialAttractionModel.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.3
 * 
 */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class VariablePotentialGravityModel extends AbstractHarmonicGravityModel implements PotentialTimeVariations {

    /** Parameter name for equatorial radius. */
    public static final String RADIUS = "equatorial radius";

    /** Serializable UID. */
    private static final long serialVersionUID = 1548110713737633271L;

    /** Degree */
    private final int degree;

    /** Order */
    private final int order;

    /** Degree for partial derivatives computation */
    private final boolean computePD;

    /** Degree correction */
    private final int variableDegree;

    /** Order correction */
    private final int variableOrder;

    /** C coefficients */
    private final double[][] cData;

    /** S coefficients */
    private final double[][] sData;

    /** C sub array for partial derivatives */
    private final double[][] cSubDataForPD;

    /** S sub array for partial derivatives */
    private final double[][] sSubDataForPD;

    /** Denormalized C coefficients for partial derivatives (not the array is also transposed) */
    private final double[][] denormalizedCPD;

    /** Denormalized S coefficients for partial derivatives (not the array is also transposed) */
    private final double[][] denormalizedSPD;

    /** Compute optional terms once only flag */
    private final boolean computeVariableCoefficientsOnce;

    /** Set array */
    private final VariablePotentialCoefficientsSet[][] setData;

    /** Reference date for optional terms computation (from provider) */
    private final AbsoluteDate refDate;

    /** Helmholtz polynomials */
    private final HelmholtzPolynomial helm;

    /** Equatorial radius parameter. */
    private final Parameter paramAe;

    /** Cached date to decide if the variable coefficients should be updated */
    private AbsoluteDate cachedUpdateCoefficientDate = null;

    /** Internal boolean to decide if variable coefficient should be updated */
    private boolean needToUpdateCoefficients;

    /**
     * Variable gravity field force model constructor (static part only).
     *
     * @param centralBodyFrame
     *        central rotating body frame
     * @param provider
     *        normalized variable coefficients provider
     * @param degree
     *        degree for acceleration computation
     * @param order
     *        order for acceleration computation
     * @param degreePD
     *        degree for partial derivatives computation
     * @param orderPD
     *        order for partial derivatives computation
     * @throws PatriusException
     *         if degree too large
     */
    public VariablePotentialGravityModel(final Frame centralBodyFrame,
                                         final VariablePotentialCoefficientsProvider provider, final int degree,
                                         final int order, final int degreePD, final int orderPD)
        throws PatriusException {
        this(centralBodyFrame, provider, degree, order, 0, 0, degreePD, orderPD, false);
    }

    /**
     * Variable gravity field force model constructor (static part only).
     *
     * @param centralBodyFrame
     *        central rotating body frame
     * @param provider
     *        normalized variable coefficients provider
     * @param degree
     *        degree
     * @param order
     *        order
     * @throws PatriusException
     *         if degree too large
     */
    public VariablePotentialGravityModel(final Frame centralBodyFrame,
                                         final VariablePotentialCoefficientsProvider provider, final int degree,
                                         final int order) throws PatriusException {
        this(centralBodyFrame, provider, degree, order, 0, 0, degree, order, true);
    }

    /**
     * Variable gravity field force model constructor. Takes into account optional terms.
     *
     * @param centralBodyFrame
     *        central rotating body frame
     * @param provider
     *        normalized variable coefficients provider
     * @param degree
     *        degree
     * @param order
     *        order
     * @param variableDegree
     *        max degree of variable terms to take into account
     * @param variableOrder
     *        max order of variable terms to take into account
     * @param computeOptionalOnce
     *        true to indicate that coefficients should be computed just once at instanciation. false if
     *        coefficients are to be computed every time.
     * @throws PatriusException
     *         if degree too large
     */
    public VariablePotentialGravityModel(final Frame centralBodyFrame,
                                         final VariablePotentialCoefficientsProvider provider, final int degree,
                                         final int order, final int variableDegree, final int variableOrder,
                                         final boolean computeOptionalOnce) throws PatriusException {
        this(centralBodyFrame, provider, degree, order, variableDegree, variableOrder, degree,
                order, computeOptionalOnce);
    }

    /**
     * Variable gravity field force model constructor. Takes into account optional terms.
     *
     * @param centralBodyFrame
     *        central rotating body frame
     * @param provider
     *        normalized variable coefficients provider
     * @param degree
     *        degree for acceleration computation
     * @param order
     *        order for acceleration computation
     * @param variableDegree
     *        max degree of optional terms to take into account for acceleration computation
     * @param variableOrder
     *        max order of optional terms to take into account for acceleration computation
     * @param degreePD
     *        degree for partial derivatives computation
     * @param orderPD
     *        order for partial derivatives computation
     * @param computeOptionalOnce
     *        true to indicate that coefficients should be computed just once at instantiation. false if
     *        coefficients are to be computed every time
     * @throws PatriusException
     *         if degree too large
     */
    public VariablePotentialGravityModel(final Frame centralBodyFrame,
                                         final VariablePotentialCoefficientsProvider provider, final int degree,
                                         final int order, final int variableDegree, final int variableOrder,
                                         final int degreePD, final int orderPD, final boolean computeOptionalOnce)
        throws PatriusException {
        this(centralBodyFrame, provider, degree, order, variableDegree, variableOrder, degreePD, orderPD,
                computeOptionalOnce, true);
    }

    /**
     * Variable gravity field force model constructor. Takes into account optional terms.
     *
     * @param centralBodyFrame
     *        central rotating body frame
     * @param provider
     *        normalized variable coefficients provider
     * @param degree
     *        degree for acceleration computation
     * @param order
     *        order for acceleration computation
     * @param variableDegree
     *        max degree of variable terms to take into account for acceleration computation
     * @param variableOrder
     *        max order of variable terms to take into account for acceleration computation
     * @param degreePD
     *        degree for partial derivatives computation
     * @param orderPD
     *        order for partial derivatives computation
     * @param computeVariableCoefficientsOnce
     *        true to indicate that coefficients should be computed just once at instantiation. false if
     *        coefficients are to be computed every time
     * @param centralTermContributionIn true if central term contribution should be considered (by default), false if
     *        not
     * @throws PatriusException
     *         if degree too large
     */
    public VariablePotentialGravityModel(final Frame centralBodyFrame,
                                         final VariablePotentialCoefficientsProvider provider, final int degree,
                                         final int order, final int variableDegree, final int variableOrder,
                                         final int degreePD, final int orderPD,
                                         final boolean computeVariableCoefficientsOnce,
                                         final boolean centralTermContributionIn)
        throws PatriusException {

        super(centralBodyFrame, new Parameter(MU, provider.getMu()));
        setCentralTermContribution(centralTermContributionIn);
        this.paramAe = new Parameter(RADIUS, provider.getAe());

        // Check degrees and orders consistency
        checkDegreesOrders(provider.getMaxDegree(), degree, order, variableDegree, variableOrder, degreePD, orderPD);

        // Constants
        this.refDate = provider.getDate();

        // Degree and order
        this.degree = degree;
        this.order = order;

        this.computePD = degreePD > 0;

        // Degree and order for variable coefficients
        this.variableDegree = variableDegree;
        this.variableOrder = variableOrder;

        // Helmholtz polynomials
        this.helm = new HelmholtzPolynomial(degree, order);

        // Corrections
        this.computeVariableCoefficientsOnce = computeVariableCoefficientsOnce;
        this.needToUpdateCoefficients = true;

        // Arrays initializations
        final Map<Integer, Map<Integer, VariablePotentialCoefficientsSet>> variableSetData = provider.getData();
        this.setData = new VariablePotentialCoefficientsSet[this.degree + 1][];
        this.cData = new double[this.degree + 1][];
        this.sData = new double[this.degree + 1][];
        this.cSubDataForPD = new double[degreePD + 1][];
        this.sSubDataForPD = new double[degreePD + 1][];

        // Loop on degrees
        for (int i = 0; i < degree + 1; i++) {
            // map for degree i
            final Map<Integer, VariablePotentialCoefficientsSet> variableSetDataI = variableSetData.get(i);

            // Data holders
            cData[i] = new double[i + 1];
            sData[i] = new double[i + 1];
            if (i <= degreePD) {
                this.cSubDataForPD[i] = new double[MathLib.min(i, orderPD) + 1];
                this.sSubDataForPD[i] = new double[MathLib.min(i, orderPD) + 1];
            }

            // Variable set
            final VariablePotentialCoefficientsSet[] setDataI = new VariablePotentialCoefficientsSet[i + 1];
            for (int j = 0; j < MathLib.min(order + 1, i + 1); j++) {
                // set of order j and degree i
                setDataI[j] = variableSetDataI.get(j);
            }
            setData[i] = setDataI;
        }

        // Transposed data initialization
        this.denormalizedCPD = new double[this.cSubDataForPD[degreePD].length][degreePD + 1];
        this.denormalizedSPD = new double[this.sSubDataForPD[degreePD].length][degreePD + 1];

    }

    /**
     * Check degrees and orders consistency
     * 
     * @param providerMaxDegree max degree of the provider
     * @param degree degree
     * @param order order
     * @param variableDegree degree correction
     * @param variableOrder order correction
     * @param degreePD degree for partial derivatives
     * @param orderPD order for partial derivatives
     * @throws PatriusException in case there is a problem with the degree or the order
     */
    private static void checkDegreesOrders(final double providerMaxDegree, final double degree, final double order,
                                           final double variableDegree, final double variableOrder,
                                           final double degreePD, final double orderPD) throws PatriusException {
        // Check if the degree is higher than the max degree of the provider
        if (degree > providerMaxDegree) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD, degree);
        }
        // Check if the order is higher than the degree
        if (order > degree) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD, order);
        }
        // Check if the degree for partial derivatives is higher than the degree
        if (degreePD > degree) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD, degreePD);
        }
        // Check if the order for partial derivatives is higher than the degree for partial derivatives
        if (orderPD > degreePD) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD, orderPD);
        }
        // Check if the degree correction is higher than the degree
        if (variableDegree > degree) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD, variableDegree);
        }
        // Check if the order correction is higher than the order
        if (variableOrder > order) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD, variableOrder);
        }
    }

    /**
     * Update cData, sData, denCPD and denSPD if date is different than the one in cache
     * 
     * @param date the date for the update of the coefficients
     */
    private void updateCoefficients(final AbsoluteDate date) {
        // Check if the date provided is equal to the cached update coefficient date
        if (!date.equals(this.cachedUpdateCoefficientDate)) {

            // Update cData and sData
            // Compute the drift function
            final double driftFunction = VariablePotentialCoefficientsSet.computeDriftFunction(date, this.refDate);
            // Compute periodic functions
            final double[] periodicFunctions = VariablePotentialCoefficientsSet.computePeriodicFunctions(date);
            // Define the periodic functions
            final double sin2Pi = periodicFunctions[0];
            final double cos2Pi = periodicFunctions[1];
            final double sin4Pi = periodicFunctions[2];
            final double cos4Pi = periodicFunctions[3];

            // Loop on the degrees
            for (int n = 2; n <= this.degree; n++) {
                // Loop on the orders
                for (int m = 0; m <= MathLib.min(this.order, n); m++) {

                    // current degree and order
                    final VariablePotentialCoefficientsSet current = this.setData[n][m];
                    // Retrieve c coefficient
                    double cValue = current.getCoefC();
                    // Retrieve s coefficient
                    double sValue = current.getCoefS();

                    // Check if the current degree is lower than the degree correction and if the current order is lower
                    // than the order correction
                    if (n <= this.variableDegree && m <= this.variableOrder) {
                        cValue += current.computeCDriftComponent(driftFunction) +
                                current.computeCPeriodicComponent(sin2Pi, cos2Pi, sin4Pi, cos4Pi);
                        sValue += current.computeSDriftComponent(driftFunction) +
                                current.computeSPeriodicComponent(sin2Pi, cos2Pi, sin4Pi, cos4Pi);
                    }
                    // Update c
                    this.cData[n][m] = cValue;
                    // Update s
                    this.sData[n][m] = sValue;
                }
            }

            // Update denCPD and denSPD if partial derivatives need to be computed
            if (this.computePD) {
                // Loop on the C sub array for partial derivatives
                for (int i = 0; i < this.cSubDataForPD.length; i++) {
                    System.arraycopy(this.cData[i], 0, this.cSubDataForPD[i], 0, this.cSubDataForPD[i].length);
                    System.arraycopy(this.sData[i], 0, this.sSubDataForPD[i], 0, this.sSubDataForPD[i].length);
                }

                // Denormalize the C and S normalized coefficients
                final double[][] tempCPD = GravityToolbox.deNormalize(this.cSubDataForPD);
                final double[][] tempSPD = GravityToolbox.deNormalize(this.sSubDataForPD);

                // Invert the arrays (optimization for later "line per line" seeking)
                for (int i = 0; i < this.cSubDataForPD.length; i++) {
                    final double[] cT = tempCPD[i];
                    final double[] sT = tempSPD[i];
                    for (int j = 0; j < cT.length; j++) {
                        this.denormalizedCPD[j][i] = cT[j];
                        this.denormalizedSPD[j][i] = sT[j];
                    }
                }
            }
            // Update the cached update coefficient date to the given date
            this.cachedUpdateCoefficientDate = date;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateCoefficientsCandS(final AbsoluteDate date) {
        updateCoefficients(date);
    }

    /** {@inheritDoc} */
    @Override
    public void updateCoefficientsCandSPD(final AbsoluteDate date) {
        updateCoefficients(date);
    }

    /** {@inheritDoc}. */
    @Override
    public Vector3D computeNonCentralTermsAcceleration(final Vector3D positionInBodyFrame, final AbsoluteDate date)
        throws PatriusException {
        // Update coefficients
        if (this.needToUpdateCoefficients) {
            this.updateCoefficientsCandS(date);
            if (this.computeVariableCoefficientsOnce || this.variableDegree == 0) {
                // If variable degree is 0, then the coefficients are not variable and need to be computed only once
                this.needToUpdateCoefficients = false;
            }
        }
        // Compute the acceleration of the gravity model
        return GravityToolbox.computeBalminoAcceleration(positionInBodyFrame, this.cData, this.sData,
            this.getMu(), this.paramAe.getValue(), this.degree, this.order, this.helm);
    }

    /** {@inheritDoc}. */
    @Override
    public final double[][] computeNonCentralTermsDAccDPos(final Vector3D positionInBodyFrame,
                                                           final AbsoluteDate date)
        throws PatriusException {
        if (!this.computePD) {
            return new double[3][3];
        }
        // coefficients update and preparation for the partial derivatives computation update coeffs
        if (this.needToUpdateCoefficients) {
            this.updateCoefficientsCandSPD(date);

            if (this.computeVariableCoefficientsOnce) {
                this.needToUpdateCoefficients = false;
            }
        }

        return GravityToolbox.computeDAccDPos(positionInBodyFrame, this.paramAe.getValue(), getMu(),
            this.denormalizedCPD, this.denormalizedSPD);
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

}
