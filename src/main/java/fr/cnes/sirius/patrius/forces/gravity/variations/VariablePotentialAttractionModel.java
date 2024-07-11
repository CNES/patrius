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
 */
/* 
 * HISTORY
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

import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.GradientModel;
import fr.cnes.sirius.patrius.forces.gravity.GravityToolbox;
import fr.cnes.sirius.patrius.forces.gravity.tides.PotentialTimeVariations;
import fr.cnes.sirius.patrius.forces.gravity.variations.coefficients.VariablePotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.gravity.variations.coefficients.VariablePotentialCoefficientsSet;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.polynomials.HelmholtzPolynomial;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.parameter.JacobiansParameterizable;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
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
public class VariablePotentialAttractionModel extends JacobiansParameterizable implements ForceModel,
    GradientModel, PotentialTimeVariations {

    /** Parameter name for equatorial radius. */
    public static final String RADIUS = "equatorial radius";

    /** Parameter name for central attraction coefficient. */
    private static final String MU = "central attraction coefficient";

    /**
     * Generated serial UID
     */
    private static final long serialVersionUID = 1548110713737633271L;

    /** Degree */
    private final int d;

    /** Order */
    private final int o;

    /** Degree for partial derivatives computation */
    private final int dGrad;

    /** Order for partial derivatives computation */
    private final int oGrad;

    /** Degree correction */
    private final int dO;

    /** Order correction */
    private final int oO;

    /** Degree correction for partial derivatives computation */
    private final int dOGrad;

    /** Order correction for partial derivatives computation */
    private final int oOGrad;

    /** C coefficients */
    private final double[][] cData;

    /** S coefficients */
    private final double[][] sData;

    /** C coefficients for acceleration partial derivatives with respect to state computation */
    private final double[][] cPDData;

    /** S coefficients for acceleration partial derivatives with respect to state computation */
    private final double[][] sPDData;

    /** C coefficients */
    private final double[][] cDataNormed;

    /** S coefficients */
    private final double[][] sDataNormed;

    /** C coefficients acceleration partial derivatives with respect to state computation */
    private final double[][] cPDDataNormed;

    /** S coefficients acceleration partial derivatives with respect to state computation */
    private final double[][] sPDDataNormed;

    /** Optional terms */
    private boolean optional;

    /** Compute optional terms once only flag */
    private boolean computeOnce;

    /** Body frame */
    private final Frame bodyFrame;

    /** Set array */
    private final VariablePotentialCoefficientsSet[][] setData;

    /** Set array for acceleration partial derivatives with respect to state computation */
    private final VariablePotentialCoefficientsSet[][] setPDData;

    /** Reference date for optional terms computation (from provider) */
    private final AbsoluteDate refDate;
    /** Helmholtz polynomials */
    private final HelmholtzPolynomial helm;

    /** Central attraction coefficient parameter. */
    private Parameter paramMu = null;

    /** Equatorial radius parameter. */
    private Parameter paramAe = null;

    /** Denormalized coefficients for acceleration partial derivatives with respect to state computation. */
    private double[][] denCPD;
    /** Denormalized coefficients for acceleration partial derivatives with respect to state computation. */
    private double[][] denSPD;

    /**
     * Private constructor, holds common code for both public constructors.
     * 
     * @param centralBodyFrame
     *        central rotating body frame
     * @param muIn
     *        gravitational constant
     * @param aeIn
     *        equatorial radius
     * @param date
     *        reference date
     * @param md
     *        max degree available
     * @param degree
     *        static part max degree to take into account
     * @param order
     *        static part max order to take into account
     * @param degreePD
     *        static part degree to take into account for acceleration
     *        partial derivatives with respect to state computation
     * @param orderPD
     *        static part order to take into account for acceleration
     *        partial derivatives with respect to state computation
     * @param degreeOptional
     *        variable part max degree to take into account for acceleration
     * @param orderOptional
     *        variable part max order to take into account for acceleration
     * @param degreeOptionalPD
     *        variable part max degree to take into account for partial derivatives
     * @param orderOptionalPD
     *        variable part max order to take into account for partial derivatives
     * 
     * @throws PatriusException
     *         if order/degree too large
     */
    private VariablePotentialAttractionModel(final Frame centralBodyFrame, final double muIn, final double aeIn,
        final AbsoluteDate date, final int md, final int degree, final int order, final int degreePD,
        final int orderPD, final int degreeOptional, final int orderOptional, final int degreeOptionalPD,
        final int orderOptionalPD) throws PatriusException {
        super();
        this.paramMu = new Parameter(MU, muIn);
        this.paramAe = new Parameter(RADIUS, aeIn);
        this.addAllParameters(this.paramMu, this.paramAe);
        this.enrichParameterDescriptors();
        if (degree > md) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD, degree);
        }
        if (order > degree) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD, order);
        }
        if (degreePD > md) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD, degreePD);
        }
        if (orderPD > degreePD) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD, orderPD);
        }
        if (orderOptional > order) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD, orderOptional);
        }
        if (degreeOptional > degree) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD, degreeOptional);
        }
        if (orderOptionalPD > orderPD) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD, orderOptionalPD);
        }
        if (degreeOptionalPD > degreePD) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD, degreeOptionalPD);
        }

        // central body frame
        this.bodyFrame = centralBodyFrame;

        // Constants
        this.refDate = date;

        // Degree and order
        this.d = degree;
        this.o = order;

        // Denormalized static coefficients arrays
        this.cDataNormed = new double[this.d + 1][];
        this.sDataNormed = new double[this.d + 1][];
        this.setData = new VariablePotentialCoefficientsSet[this.d + 1][];

        // C and S arrays
        this.cData = new double[this.d + 1][];
        this.sData = new double[this.d + 1][];

        // Helmholtz polynomials
        this.helm = new HelmholtzPolynomial(degree, order);

        // Degree and order for acceleration partial derivatives with respect to state computation
        this.dGrad = degreePD;
        this.oGrad = orderPD;

        // Denormalized static coefficients arrays for acceleration partial derivatives with respect
        // to state computation
        this.cPDDataNormed = new double[this.dGrad + 1][];
        this.sPDDataNormed = new double[this.dGrad + 1][];
        this.setPDData = new VariablePotentialCoefficientsSet[this.dGrad + 1][];

        // CPD and SPD arrays
        this.cPDData = new double[this.dGrad + 1][];
        this.sPDData = new double[this.dGrad + 1][];

        // Corrections
        this.dO = degreeOptional;
        this.oO = orderOptional;
        this.dOGrad = degreeOptionalPD;
        this.oOGrad = orderOptionalPD;
    }

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
    public VariablePotentialAttractionModel(final Frame centralBodyFrame,
        final VariablePotentialCoefficientsProvider provider, final int degree, final int order,
        final int degreePD,
        final int orderPD) throws PatriusException {
        this(centralBodyFrame, provider, degree, order, degreePD, orderPD, 0, 0, 0, 0, false);
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
    public VariablePotentialAttractionModel(final Frame centralBodyFrame,
        final VariablePotentialCoefficientsProvider provider, final int degree,
        final int order) throws PatriusException {
        this(centralBodyFrame, provider, degree, order, degree, order, 0, 0, 0, 0, false);
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
     * @param degreePD
     *        degree for partial derivatives computation
     * @param orderPD
     *        order for partial derivatives computation
     * @param degreeOptional
     *        max degree of optional terms to take into account for acceleration computation
     * @param orderOptional
     *        max order of optional terms to take into account for acceleration computation
     * @param degreeOptionalPD
     *        max degree of optional terms to take into account for partial derivatives computation
     * @param orderOptionalPD
     *        max order of optional terms to take into account for partial derivatives computation
     * @param computeOptionalOnce
     *        true to indicate that coefficients should be computed just once at instantiation. false if
     *        coefficients are to be computed every time
     * @throws PatriusException
     *         if degree too large
     */
    public VariablePotentialAttractionModel(final Frame centralBodyFrame,
        final VariablePotentialCoefficientsProvider provider, final int degree, final int order,
        final int degreePD, final int orderPD, final int degreeOptional, final int orderOptional,
        final int degreeOptionalPD, final int orderOptionalPD,
        final boolean computeOptionalOnce) throws PatriusException {

        this(centralBodyFrame, provider.getMu(), provider.getAe(), provider.getDate(), provider.getMaxDegree(), degree,
            order, degreePD, orderPD, degreeOptional, orderOptional, degreeOptionalPD, orderOptionalPD);

        // Initialize static data
        this.initStaticData(provider, this.setData, this.cDataNormed, this.sDataNormed, this.cData, this.sData, this.d,
            this.o);
        if (this.computeGradientPosition()) {
            this.initStaticData(provider, this.setPDData, this.cPDDataNormed, this.sPDDataNormed, this.cPDData,
                this.sPDData, this.dGrad, this.oGrad);
        }

        // Corrections
        this.optional = (degreeOptional != 0 || orderOptional != 0 || degreeOptionalPD != 0 || orderOptionalPD != 0);
        this.computeOnce = computeOptionalOnce;
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
     * @param degreeOptional
     *        max degree of optional terms to take into account
     * @param orderOptional
     *        max order of optional terms to take into account
     * @param computeOptionalOnce
     *        true to indicate that coefficients should be computed just once at instanciation. false if
     *        coefficients are to be computed every time.
     * @throws PatriusException
     *         if degree too large
     */
    public VariablePotentialAttractionModel(final Frame centralBodyFrame,
        final VariablePotentialCoefficientsProvider provider, final int degree, final int order,
        final int degreeOptional, final int orderOptional,
        final boolean computeOptionalOnce) throws PatriusException {
        this(centralBodyFrame, provider, degree, order, degree, order, degreeOptional, orderOptional, degreeOptional,
            orderOptional, computeOptionalOnce);
    }

    /**
     * Initial static part of potential
     * 
     * @param provider
     *        variable coefficients provider
     * @param setDataCopy
     *        set array
     * @param cDataNormedCopy
     *        normalized C coefficients
     * @param sDataNormedCopy
     *        normalizes S coefficients
     * @param cDataCopy
     *        normalized C coefficients
     * @param sDataCopy
     *        normalizes S coefficients
     * @param degree
     *        degree
     * @param order
     *        order
     */
    private void initStaticData(final VariablePotentialCoefficientsProvider provider,
                                final VariablePotentialCoefficientsSet[][] setDataCopy,
                                final double[][] cDataNormedCopy,
                                final double[][] sDataNormedCopy, final double[][] cDataCopy,
                                final double[][] sDataCopy, final int degree, final int order) {

        // potential data
        final Map<Integer, Map<Integer, VariablePotentialCoefficientsSet>> data = provider.getData();

        // data storage
        Map<Integer, VariablePotentialCoefficientsSet> current;
        VariablePotentialCoefficientsSet set;

        // mu not taken into account
        int i = 0;
        setDataCopy[i] = new VariablePotentialCoefficientsSet[i + 1];
        cDataNormedCopy[i] = new double[i + 1];
        sDataNormedCopy[i] = new double[i + 1];

        // degree 1 not taken into account either
        i = 1;
        setDataCopy[i] = new VariablePotentialCoefficientsSet[i + 1];
        cDataNormedCopy[i] = new double[i + 1];
        sDataNormedCopy[i] = new double[i + 1];

        for (i = 2; i < degree + 1; i++) {

            // map of orders for degree i
            current = data.get(i);

            // data holders
            setDataCopy[i] = new VariablePotentialCoefficientsSet[i + 1];
            cDataNormedCopy[i] = new double[i + 1];
            sDataNormedCopy[i] = new double[i + 1];

            for (int j = 0; j < MathLib.min(order + 1, i + 1); j++) {

                // set of order j and degree i
                set = current.get(j);
                setDataCopy[i][j] = set;

                // static coefficients
                cDataNormedCopy[i][j] = set.getC();
                sDataNormedCopy[i][j] = set.getS();
            }
        }

        // Store data in final array
        for (int j = 0; j < cDataCopy.length; j++) {
            cDataCopy[j] = cDataNormedCopy[j].clone();
            sDataCopy[j] = sDataNormedCopy[j].clone();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateCoefficientsCandS(final AbsoluteDate date) throws PatriusException {

        // elapsed time for corrections computation
        final double elapsed = date.offsetFrom(this.refDate, TimeScalesFactory.getUTC()) / Constants.JULIAN_YEAR;

        // containers
        VariablePotentialCoefficientsSet current;
        double[] cc;
        double[] sc;
        double dot;
        double c1a;
        double c2a;
        double s1a;
        double s2a;

        // Static part
        for (int i = 0; i < this.cData.length; i++) {
            this.cData[i] = this.cDataNormed[i].clone();
            this.sData[i] = this.sDataNormed[i].clone();
        }

        // Dynamic part

        // Temporary variable for time-saving
        final double[] sincos = MathLib.sinAndCos(2 * FastMath.PI * elapsed);
        final double sin2Pi = sincos[0];
        final double cos2Pi = sincos[1];
        final double sin4Pi = 2. * sin2Pi * cos2Pi;
        final double cos4Pi = cos2Pi * cos2Pi - sin2Pi * sin2Pi;

        for (int n = 2; n < this.dO + 1; n++) {
            for (int m = 0; m <= MathLib.min(this.oO, n); m++) {

                // current degree and order
                current = this.setData[n][m];

                // arrays
                cc = current.getCc();
                sc = current.getSc();

                // correction computation, see http://grgs.obs-mip.fr/grace/variable-models-grace-lageos/formats
                dot = cc[0];
                s1a = cc[1];
                c1a = cc[2];
                s2a = cc[3];
                c2a = cc[4];
                this.cData[n][m] += dot * elapsed + s1a * sin2Pi + c1a * cos2Pi + s2a * sin4Pi + c2a * cos4Pi;

                dot = sc[0];
                s1a = sc[1];
                c1a = sc[2];
                s2a = sc[3];
                c2a = sc[4];
                this.sData[n][m] += dot * elapsed + s1a * sin2Pi + c1a * cos2Pi + s2a * sin4Pi + c2a * cos4Pi;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateCoefficientsCandSPD(final AbsoluteDate date) throws PatriusException {

        // elapsed time for corrections computation
        final double elapsed = date.offsetFrom(this.refDate, TimeScalesFactory.getUTC()) / Constants.JULIAN_YEAR;

        // containers
        VariablePotentialCoefficientsSet current;
        double[] cc;
        double[] sc;
        double dot;
        double c1a;
        double c2a;
        double s1a;
        double s2a;

        // Static part
        for (int i = 0; i < this.cPDData.length; i++) {
            this.cPDData[i] = this.cPDDataNormed[i].clone();
            this.sPDData[i] = this.sPDDataNormed[i].clone();
        }

        // Dynamic part

        // Temporary variable for time-saving
        final double[] sincos = MathLib.sinAndCos(2 * FastMath.PI * elapsed);
        final double sin2Pi = sincos[0];
        final double cos2Pi = sincos[1];
        final double sin4Pi = 2. * sin2Pi * cos2Pi;
        final double cos4Pi = cos2Pi * cos2Pi - sin2Pi * sin2Pi;

        for (int n = 2; n < this.dOGrad + 1; n++) {
            for (int m = 0; m <= MathLib.min(this.oOGrad, n); m++) {

                // current degree and order
                current = this.setPDData[n][m];

                // arrays
                cc = current.getCc();
                sc = current.getSc();

                // correction computation, see http://grgs.obs-mip.fr/grace/variable-models-grace-lageos/formats
                dot = cc[0];
                s1a = cc[1];
                c1a = cc[2];
                s2a = cc[3];
                c2a = cc[4];
                this.cPDData[n][m] += dot * elapsed + s1a * sin2Pi + c1a * cos2Pi + s2a * sin4Pi + c2a * cos4Pi;

                dot = sc[0];
                s1a = sc[1];
                c1a = sc[2];
                s2a = sc[3];
                c2a = sc[4];
                this.sPDData[n][m] += dot * elapsed + s1a * sin2Pi + c1a * cos2Pi + s2a * sin4Pi + c2a * cos4Pi;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder) throws PatriusException {
        // compute acceleration in inertial frame
        final Vector3D accInInert = this.computeAcceleration(s);
        adder.addXYZAcceleration(accInInert.getX(), accInInert.getY(), accInInert.getZ());
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D computeAcceleration(final SpacecraftState s) throws PatriusException {

        // get the position in body frame
        final Transform bodyToInertial = this.bodyFrame.getTransformTo(s.getFrame(), s.getDate());
        final Vector3D posInBody = bodyToInertial.getInverse().transformVector(s.getPVCoordinates().getPosition());
        final PVCoordinates pv = new PVCoordinates(posInBody, Vector3D.ZERO);

        Vector3D gamma = this.computeAcceleration(s.getDate(), pv);

        // compute acceleration in inertial frame
        gamma = bodyToInertial.transformVector(gamma);

        return gamma;
    }

    /**
     * Compute acceleration in rotating frame
     * 
     * @param date
     *        date at which to compute acceleration
     * @param pv
     *        pv of spacecraft
     * @return the acceleration in the rotating frame
     * @throws PatriusException
     *         if acceleration cannot be computed
     */
    public Vector3D computeAcceleration(final AbsoluteDate date, final PVCoordinates pv) throws PatriusException {

        // update coeffs
        if (this.optional) {
            this.updateCoefficientsCandS(date);
            if (this.computeOnce) {
                this.optional = false;
            }
        }

        return GravityToolbox.computeBalminoAcceleration(pv, this.cData, this.sData, this.paramMu.getValue(),
            this.paramAe.getValue(), this.d, this.o, this.helm);
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[0];
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientPosition() {
        return this.dGrad != 0 || this.oGrad != 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientVelocity() {
        return false;
    }

    /**
     * @return the mu
     */
    public double getMu() {
        return this.paramMu.getValue();
    }

    /**
     * @return the ae
     */
    public double getAe() {
        return this.paramAe.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public void addDAccDState(final SpacecraftState s, final double[][] dAccdPos,
                              final double[][] dAccdVel) throws PatriusException {

        if (this.computeGradientPosition()) {
            // coefficients update and preparation for the partial derivatives
            // computation
            // update coeffs
            if (this.optional) {
                this.updateCoefficientsCandSPD(s.getDate());
                if (this.computeOnce) {
                    this.optional = false;
                }
            }

            final int degree = this.cPDData.length;
            // denormalize the CPD and SPD normalized coefficients:
            final double[][] tempC = GravityToolbox.deNormalize(this.cPDData);
            final double[][] tempS = GravityToolbox.deNormalize(this.sPDData);
            // invert the arrays (optimization for later "line per line"
            // seeking)
            this.denCPD = new double[this.cPDData[degree - 1].length][this.cPDData.length];
            this.denSPD = new double[this.sPDData[degree - 1].length][this.sPDData.length];
            for (int i = 0; i < degree; i++) {
                final double[] cT = tempC[i];
                final double[] sT = tempS[i];
                for (int j = 0; j < cT.length; j++) {
                    this.denCPD[j][i] = cT[j];
                    this.denSPD[j][i] = sT[j];
                }
            }

            // get the position in body frame
            final Transform fromBodyFrame = this.bodyFrame.getTransformTo(s.getFrame(), s.getDate());
            final Transform toBodyFrame = fromBodyFrame.getInverse();
            final PVCoordinates pvSat = toBodyFrame.transformPVCoordinates(s.getPVCoordinates());

            final double[][] dAdP = GravityToolbox.computeDAccDPos(pvSat, s.getDate(), this.paramAe.getValue(),
                this.paramMu.getValue(), this.denCPD, this.denSPD);

            Vector3D dx = new Vector3D(dAdP[0][0], dAdP[1][0], dAdP[2][0]);
            Vector3D dy = new Vector3D(dAdP[0][1], dAdP[1][1], dAdP[2][1]);
            Vector3D dz = new Vector3D(dAdP[0][2], dAdP[1][2], dAdP[2][2]);
            // compute acceleration in inertial frame
            dx = fromBodyFrame.transformVector(dx);
            dy = fromBodyFrame.transformVector(dy);
            dz = fromBodyFrame.transformVector(dz);

            double[][] derfinal = { { dx.getX(), dy.getX(), dz.getX() },
                { dx.getY(), dy.getY(), dz.getY() },
                { dx.getZ(), dy.getZ(), dz.getZ() } };

            // jacobian matrix to express dPos in GCRF instead of body frame
            final double[][] jac = new double[6][6];
            toBodyFrame.getJacobian(jac);

            // keep the useful part (3x3 for position)
            final double[][] useful = new double[3][3];
            for (int i = 0; i < useful.length; i++) {
                for (int j = 0; j < useful[i].length; j++) {
                    useful[i][j] = jac[i][j];
                }
            }

            // matrices of partial derivatives and jacobian
            final Array2DRowRealMatrix dAdPMatrix = new Array2DRowRealMatrix(derfinal, false);
            final Array2DRowRealMatrix jacMatrix = new Array2DRowRealMatrix(useful, false);

            // multiplication
            final Array2DRowRealMatrix transformedMatrix = dAdPMatrix.multiply(jacMatrix);
            derfinal = transformedMatrix.getData(false);

            // the only non-null contribution for this force is dAcc/dPos
            dAccdPos[0][0] += derfinal[0][0];
            dAccdPos[0][1] += derfinal[0][1];
            dAccdPos[0][2] += derfinal[0][2];
            dAccdPos[1][0] += derfinal[1][0];
            dAccdPos[1][1] += derfinal[1][1];
            dAccdPos[1][2] += derfinal[1][2];
            dAccdPos[2][0] += derfinal[2][0];
            dAccdPos[2][1] += derfinal[2][1];
            dAccdPos[2][2] += derfinal[2][2];
        }
    }

    /**
     * {@inheritDoc}.
     * No parameter is supported by this force model.
     */
    @Override
    public void addDAccDParam(final SpacecraftState s, final Parameter param,
                              final double[] dAccdParam) throws PatriusException {
        throw new PatriusException(PatriusMessages.UNKNOWN_PARAMETER, param);
    }
    
    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }
}
