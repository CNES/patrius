/**
 *
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
 * @history Created 10/07/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:93:08/08/2013:completed Javadoc concerning the partial derivatives parameters
 * VERSION::DM:90:03/10/2013:moved part GravityToolbox to Orekit, created
 * TidesToolbox. Using normalized gravitational attraction.
 * VERSION::DM:241:01/10/2014:created AbstractTides class
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::FA:442:14/08/2015:Computation times optimization
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1489:07/06/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.ArithmeticUtils;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.DateTimeComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class implements the perturbating force due to ocean tides.
 * 
 * <p>
 * The implementation of this class enables the computation of partial derivatives by finite differences with respect to
 * the <b>central attraction coefficient</b>.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment not thread safe because of the method updateCoefficientsCandS().
 * 
 * @author Rami Houdroge, Thomas Trapier
 * 
 * @version $Id: OceanTides.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
@SuppressWarnings("PMD.UseConcurrentHashMap")
public class OceanTides extends AbstractTides {

    /** Parameter name for Density at surface. */
    public static final String RHO = "density at surface";
    /** Generated Serial UID */
    private static final long serialVersionUID = 7083827601800332989L;

    /** 0.04 */
    private static final double PZ4 = .04;
    /** 0.5 */
    private static final double P5 = 0.5;
    /** 20 */
    private static final double D20 = 20;
    /** 10.0 */
    private static final double C_10 = 10.;
    /** -2 */
    private static final int C_N2 = -2;
    /**
     * Steps of 12 hours for admittance coefficients computation
     */
    private static final double DEFAULT_STEP = D20;

    /** Degree for acceleration computation. */
    private final int l;

    /** Order for acceleration computation. */
    private final int m;

    /** Degree for partial derivatives computation. */
    private final int lgrad;

    /** Order for partial derivatives computation. */
    private final int mgrad;

    /** True if acceleration partial derivatives with respect to position have to be computed. */
    private final boolean computePartialDerivativesWrtPosition;

    /**
     * Heights map
     */
    private final Map<Double, Double> heightsP = new TreeMap<Double, Double>();
    /**
     * Heights map
     */
    private final Map<Double, Double> heightsS = new TreeMap<Double, Double>();
    /**
     * Names map
     */
    private final Map<String, Double> names = new TreeMap<String, Double>();
    /**
     * Ocean tides data
     */
    private final IOceanTidesDataProvider oceanTidesData;
    /**
     * Doodson numbers of waves that have height data
     */
    private final Set<Double> mainWaves = new TreeSet<Double>();
    /**
     * Doodson numbers of waves that have height data
     */
    private final Set<Double> fileWaves = new TreeSet<Double>();
    /**
     * Doodson numbers of secondary waves and their pivots
     */
    private final Map<Double, double[]> secWaves = new TreeMap<Double, double[]>();
    /**
     * Boolean to ignore or take into account secondary waves
     */
    private final boolean withAdmittanceIn;
    /**
     * Doodson-Warburg conventions
     */
    private final Map<Double, Double> phases = new TreeMap<Double, Double>();
    /**
     * Admittance coefficients : key is Doodson number, value is array {Kc, Ks}
     */
    @SuppressWarnings("PMD.LooseCoupling")
    private final TreeMap<AbsoluteDate, TreeMap<Double, double[]>> admittanceCoefficients =
        new TreeMap<AbsoluteDate, TreeMap<Double, double[]>>();

    /** Love numbers. */
    private final double[] loveNumbers;

    /** Love numbers for partial derivatives. */
    private final double[] loveNumbersPD;

    /** Density at surface parameter. */
    private Parameter paramRho = null;

    /** Interval variable stored at constructor to speed-up acceleration computation at runtime. */
    private final StaticData dataArray;

    /**
     * Interval variable stored at constructor to speed-up partial derivatives computation at
     * runtime.
     */
    private final StaticData dataArrayPD;

    /**
     * Constructor.
     * 
     * @param centralBodyFrame rotating central body frame
     * @param equatorialRadius equatorial radius
     * @param mu gravitational constant for central body
     * @param density density of water
     * @param degree degree
     * @param order order
     * @param withAdmittance if the admittance computation is requested
     * @param tidesData data for ocean tides
     */
    public OceanTides(final Frame centralBodyFrame, final double equatorialRadius, final double mu,
                      final double density, final int degree, final int order, final boolean withAdmittance,
                      final IOceanTidesDataProvider tidesData) {
        this(centralBodyFrame, equatorialRadius, mu, density, degree, order, degree, order,
                withAdmittance, tidesData);
    }

    /**
     * Constructor.
     * 
     * @param centralBodyFrame rotating central body frame
     * @param equatorialRadius equatorial radius
     * @param mu gravitational constant for central body
     * @param density density of water
     * @param degree degree for acceleration computation
     * @param order order for acceleration computation
     * @param degreePD degree for partial derivatives computation
     * @param orderPD order for partial derivatives computation
     * @param withAdmittance if the admittance computation is requested
     * @param tidesData data for ocean tides
     */
    public OceanTides(final Frame centralBodyFrame, final double equatorialRadius, final double mu,
                      final double density, final int degree, final int order, final int degreePD,
                      final int orderPD, final boolean withAdmittance, final IOceanTidesDataProvider tidesData) {
        super(centralBodyFrame, equatorialRadius, mu, degree + 1, order + 1, degreePD + 1,
                orderPD + 1);
        this.l = degree + 1;
        this.m = order + 1;
        this.lgrad = degreePD + 1;
        this.mgrad = orderPD + 1;
        // adding the density as parameter
        this.paramRho = new Parameter(RHO, density);
        this.addParameter(this.paramRho);
        this.enrichParameterDescriptors();
        this.oceanTidesData = tidesData;
        this.withAdmittanceIn = withAdmittance;
        this.loveNumbers = new double[this.l];
        this.loveNumbersPD = new double[this.lgrad];
        this.initializeFramework();
        this.dataArray = this.initData(this.loveNumbers, this.l, this.m);
        this.dataArrayPD = this.initData(this.loveNumbersPD, this.lgrad, this.mgrad);
        this.computePartialDerivativesWrtPosition = (degreePD != 0 || orderPD != 0);
    }

    /**
     * Constructor using {@link Parameter}.
     * 
     * @param centralBodyFrame rotating central body frame
     * @param equatorialRadius equatorial radius parameter
     * @param mu gravitational constant for central body parameter
     * @param density density of water parameter
     * @param degree degree
     * @param order order
     * @param withAdmittance if the admittance computation is requested
     * @param tidesData data for ocean tides
     */
    public OceanTides(final Frame centralBodyFrame, final Parameter equatorialRadius,
                      final Parameter mu, final Parameter density, final int degree, final int order,
                      final boolean withAdmittance, final IOceanTidesDataProvider tidesData) {
        this(centralBodyFrame, equatorialRadius, mu, density, degree, order, degree, order,
                withAdmittance, tidesData);
    }

    /**
     * Constructor using {@link Parameter}.
     * 
     * @param centralBodyFrame rotating central body frame
     * @param equatorialRadius equatorial radius parameter
     * @param mu gravitational constant for central body parameter
     * @param density density of water parameter parameter
     * @param degree degree for acceleration computation
     * @param order order for acceleration computation
     * @param degreePD degree for partial derivatives computation
     * @param orderPD order for partial derivatives computation
     * @param withAdmittance if the admittance computation is requested
     * @param tidesData data for ocean tides
     */
    public OceanTides(final Frame centralBodyFrame, final Parameter equatorialRadius,
                      final Parameter mu, final Parameter density, final int degree, final int order,
                      final int degreePD, final int orderPD, final boolean withAdmittance,
                      final IOceanTidesDataProvider tidesData) {
        super(centralBodyFrame, equatorialRadius, mu, degree + 1, order + 1, degreePD + 1,
                orderPD + 1);
        this.l = degree + 1;
        this.m = order + 1;
        this.lgrad = degreePD + 1;
        this.mgrad = orderPD + 1;
        // adding the density as parameter
        this.paramRho = density;
        this.addParameter(this.paramRho);
        this.enrichParameterDescriptors();
        this.oceanTidesData = tidesData;
        this.withAdmittanceIn = withAdmittance;
        this.loveNumbers = new double[this.l];
        this.loveNumbersPD = new double[this.lgrad];
        this.initializeFramework();
        this.dataArray = this.initData(this.loveNumbers, this.l, this.m);
        this.dataArrayPD = this.initData(this.loveNumbersPD, this.lgrad, this.mgrad);
        this.computePartialDerivativesWrtPosition = (degreePD != 0 || orderPD != 0);
    }

    /**
     * Initialization.
     */
    private void initializeFramework() {

        // Get available heights and names
        this.heightsP.putAll(OceanTidesWaves.getMainHeights());
        this.heightsS.putAll(OceanTidesWaves.getSecondaryHeights());

        final double[] doodsonNumber = this.oceanTidesData.getDoodsonNumbers();
        final Set<Double> doodsonFileNumbersSet = new TreeSet<Double>();

        // loop over waves in tide coefficients provider
        int mainWavesCount = 0;
        for (final double d : doodsonNumber) {
            // main waves
            if (this.heightsP.containsKey(d)) {
                // wave is added if height data found
                this.mainWaves.add(d);
                mainWavesCount++;
            }
            // all the waves from the file
            this.fileWaves.add(d);
        }

        // Doodson-Warburg conventions
        for (final double d : this.fileWaves) {
            // if the wave height is known, it is used to compute the phase
            if (this.heightsP.containsKey(d)) {
                this.phases.put(d, this.getPhaseConvention(d, this.heightsP.get(d)));
            } else if (this.heightsS.containsKey(d)) {
                this.phases.put(d, this.getPhaseConvention(d, this.heightsS.get(d)));

            } else {
                // if the wave height is unknown, the phase is 0.
                this.phases.put(d, 0.);
            }
            doodsonFileNumbersSet.add(d);
        }

        // secondary waves
        if (this.withAdmittanceIn) {
            this.initAdmittance(mainWavesCount, doodsonFileNumbersSet);
        }

        // Coefficients to convert from load factors to gravity coefficients
        this.initLoveNumbers();
    }

    /**
     * Initialisation if the computation is made with admittance.
     * 
     * @param mainWavesCount the number of main waves in the file
     * @param doodsonFileNumbersSet the Doodson numbers of the waves of the file
     */
    private void initAdmittance(final int mainWavesCount, final Set<Double> doodsonFileNumbersSet) {
        this.names.putAll(OceanTidesWaves.getMainNames());

        // check that all the main waves are available in the file
        if (mainWavesCount != this.heightsP.size()) {
            throw PatriusException
                .createIllegalArgumentException(PatriusMessages.MAIN_WAVE_MISSING);
        }

        double[] piv;
        // store secondary waves (waves from the file that also
        // belong to the secondary waves list (cf. OceanTidesWaves))

        for (final Entry<Double, Double> entry : this.heightsS.entrySet()) {
            final double d = entry.getKey();
            if (!doodsonFileNumbersSet.contains(d)) {
                piv = this.getPivots(d);
                if (piv[0] * piv[1] * piv[2] != 0) {
                    // wave added if height data is found and pivot waves exist
                    this.secWaves.put(d, piv);
                    // get phase
                    this.phases.put(d, this.getPhaseConvention(d, this.heightsS.get(d)));
                }
            }
        }
    }

    /**
     * Initialize Love numbers.
     */
    private void initLoveNumbers() {
        // initialize Love numbers
        final double[] constants = this.oceanTidesData.getLoveNumbers();
        for (int k = 0; k < MathLib.min(constants.length, this.l); k++) {
            this.loveNumbers[k] = constants[k];
        }
        for (int k = 0; k < MathLib.min(constants.length, this.lgrad); k++) {
            this.loveNumbersPD[k] = constants[k];
        }
    }

    /**
     * Precompute some date-independent coefficients to speed-up runtime computation.
     * 
     * @param loveNumbersIn love numbers
     * @param mIn m
     * @param lIn l
     * @return precomputed data
     */
    private StaticData initData(final double[] loveNumbersIn, final int lIn, final int mIn) {

        // Factor array
        // Load factor to gravity coefficients conversion constant (OBELIX Eq 6-6 p. 61)
        final double constant = this.paramRho.getValue() * PZ4 * FastMath.PI * this.paramAe.getValue()
                * this.paramAe.getValue() * MathLib.divide(Constants.CGU, this.paramMu.getValue());
        final double[] factorArray = new double[loveNumbersIn.length + 1];
        for (int i = 1; i < factorArray.length; i++) {
            factorArray[i] = constant * (1 + loveNumbersIn[i - 1]) / (2 * i + 1);
        }

        // Initialization
        final int[][] nDoodsonArray = new int[this.fileWaves.size()][6];
        final double[] phaseArray = new double[this.fileWaves.size()];
        final boolean[] mainWavesArray = new boolean[this.fileWaves.size()];
        final int[] maxOrderArray = new int[this.fileWaves.size()];
        final int[][] maxDegreeArray = new int[this.fileWaves.size()][];
        final int[][] minDegreeArray = new int[this.fileWaves.size()][];
        final double[][][] cppcmArray = new double[this.fileWaves.size()][][];
        final double[][][] cpmcmArray = new double[this.fileWaves.size()][][];
        final double[][][] sppsmArray = new double[this.fileWaves.size()][][];
        final double[][][] spmsmArray = new double[this.fileWaves.size()][][];
        int index = 0;

        // Loop on all waves
        for (final double s : this.fileWaves) {
            // nDoodson
            nDoodsonArray[index] = TidesToolbox.nDoodson(s);
            // Phase
            phaseArray[index] = this.phases.get(s);
            // Main waves
            mainWavesArray[index] = this.mainWaves.contains(s);
            // Max order
            maxOrderArray[index] = MathLib.min(mIn, this.oceanTidesData.getMaxOrder(s) + 1);

            // Min and max order
            maxDegreeArray[index] = new int[MathLib.min(mIn, maxOrderArray[index] + 1)];
            minDegreeArray[index] = new int[MathLib.min(mIn, maxOrderArray[index] + 1)];
            cppcmArray[index] = new double[MathLib.min(mIn, maxOrderArray[index] + 1)][];
            cpmcmArray[index] = new double[MathLib.min(mIn, maxOrderArray[index] + 1)][];
            sppsmArray[index] = new double[MathLib.min(mIn, maxOrderArray[index] + 1)][];
            spmsmArray[index] = new double[MathLib.min(mIn, maxOrderArray[index] + 1)][];
            for (int j = 0; j < maxOrderArray[index]; j++) {
                // ignore the degrees < 2 (cf. OBELIX)
                maxDegreeArray[index][j] = MathLib.min(lIn, this.oceanTidesData.getMaxDegree(s, j) + 1);
                minDegreeArray[index][j] = MathLib.max(this.oceanTidesData.getMinDegree(s, j), 2);

                // Sums C+ + C-, C+ - C-, S+ + S- et S+ - S- ( x factor)
                cppcmArray[index][j] = new double[maxDegreeArray[index][j]];
                cpmcmArray[index][j] = new double[maxDegreeArray[index][j]];
                sppsmArray[index][j] = new double[maxDegreeArray[index][j]];
                spmsmArray[index][j] = new double[maxDegreeArray[index][j]];
                for (int i = minDegreeArray[index][j]; i < maxDegreeArray[index][j]; i++) {
                    final double[] coefs = this.oceanTidesData.getCpmSpm(s, i, j);
                    cppcmArray[index][j][i] = (coefs[0] + coefs[1]) * factorArray[i];
                    cpmcmArray[index][j][i] = (coefs[0] - coefs[1]) * factorArray[i];
                    sppsmArray[index][j][i] = (coefs[2] + coefs[3]) * factorArray[i];
                    spmsmArray[index][j][i] = (coefs[2] - coefs[3]) * factorArray[i];
                }
            }

            index++;
        }

        // Build array
        return new StaticData(phaseArray, mainWavesArray, nDoodsonArray, maxOrderArray,
            maxDegreeArray, minDegreeArray, cppcmArray, cpmcmArray, sppsmArray, spmsmArray);
    }

    /**
     * Get secondary wave pivots
     * 
     * @param doodson Doodson number of secondary wave
     * @return double array containing the three pivots
     */
    private double[] getPivots(final double doodson) {

        /*
         * Pivots are given as per fmf_ccadmt.f90 l. 167
         */
        // initialize Doodson number decomposition as a sextuplet of integers
        final int[] ndood = TidesToolbox.nDoodson(doodson);
        // wave pivots
        Double k1 = 0.;
        Double k2 = 0.;
        Double k3 = 0.;

        // Names of primary waves
        final String mm = "Mm";
        final String mf = "Mf";
        final String mtm = "Mtm";
        final String msqm = "Msqm";
        final String m2 = "M2";
        final String n2 = "N2";
        final String n22 = "2N2";

        if (ndood[0] == 0) {

            // Large periods
            k1 = this.names.get("Ssa");
            k2 = this.names.get(mm);
            k3 = this.names.get(mf);

            if (ndood[1] == 2) {

                k1 = this.names.get(mm);
                k2 = this.names.get(mf);
                k3 = this.names.get(mtm);

            } else if (ndood[1] >= 3) {

                k1 = this.names.get(mf);
                k2 = this.names.get(mtm);
                k3 = this.names.get(msqm);

            }

        } else if (ndood[0] == 1) {

            // Diurnal
            k1 = this.names.get("Q1");
            k2 = this.names.get("O1");
            k3 = this.names.get("K1");

        } else if (ndood[0] == 2) {
            // half-diurnal

            k1 = this.names.get(n2);
            k2 = this.names.get(m2);
            k3 = this.names.get("K2");

            if (ndood[1] <= C_N2) {

                k1 = this.names.get(n22);
                k2 = this.names.get(n2);
                k3 = this.names.get(m2);
            }
        }
        // return pivots array
        return new double[] { k1, k2, k3 };
    }

    /**
     * Get normalized C coefficients table
     * 
     * @param date user date
     * @return Normalized C coefficients
     * @throws PatriusException if fails
     */
    public double[][] getNormalizedCCoefs(final AbsoluteDate date) throws PatriusException {
        this.updateCoefficientsCandS(date);
        return this.coefficientsC.clone();
    }

    /**
     * Get normalized S coefficients table
     * 
     * @param date user date
     * @return Normalized S coefficients
     * @throws PatriusException if fails
     */
    public double[][] getNormalizedSCoefs(final AbsoluteDate date) throws PatriusException {
        this.updateCoefficientsCandS(date);
        return this.coefficientsS.clone();
    }

    /**
     * Get denormalized C coefficients table
     * 
     * @param date user date
     * @return Denormalized C coefficients
     * @throws PatriusException if fails
     */
    public double[][] getDenormalizedCCoefs(final AbsoluteDate date) throws PatriusException {
        this.updateCoefficientsCandS(date);
        return this.deNormalize(this.coefficientsC);
    }

    /**
     * Get denormalized S coefficients table
     * 
     * @param date user date
     * @return Denormalized C coefficients
     * @throws PatriusException if fails
     */
    public double[][] getDenormalizedSCoefs(final AbsoluteDate date) throws PatriusException {
        this.updateCoefficientsCandS(date);
        return this.deNormalize(this.coefficientsS);
    }

    /** {@inheritDoc} */
    @Override
    public void updateCoefficientsCandS(final AbsoluteDate date) throws PatriusException {
        this.coefficientsC = new double[this.l][this.m];
        this.coefficientsS = new double[this.l][this.m];
        this.updateCoefficients(date, this.coefficientsC, this.coefficientsS, this.dataArray);
    }

    /** {@inheritDoc} */
    @Override
    public void updateCoefficientsCandSPD(final AbsoluteDate date) throws PatriusException {
        this.coefficientsCPD = new double[this.lgrad][this.mgrad];
        this.coefficientsSPD = new double[this.lgrad][this.mgrad];
        this.updateCoefficients(date, this.coefficientsCPD, this.coefficientsSPD, this.dataArrayPD);
    }

    /**
     * Update C and S arrays.
     * 
     * @param date date of computation
     * @param arrayC cosine array
     * @param arrayS sine array
     * @param data data
     * @throws PatriusException if update fails
     */
    private void updateCoefficients(final AbsoluteDate date, final double[][] arrayC,
                                    final double[][] arrayS, final StaticData data) throws PatriusException {

        // Admittance
        if (this.withAdmittanceIn) {
            // Admittance tables checks
            this.checkAdmittanceTablesAvailability(date);
        }

        // fundamental arguments
        final double[][] args = TidesToolbox.computeFundamentalArguments(date,
            this.oceanTidesData.getStandard());

        int index = 0;

        for (final double s : this.fileWaves) {
            final double thetaS = computeThetaS(data.nDoodsonArray[index], args, 0);

            // Doodson-Warburg phase convention (cf. obelixutil.f90 function doodwarb line 55)
            final double phase = data.phaseArray[index];

            double admC = 1.;
            double admS = 0.;
            if (this.withAdmittanceIn && data.mainWavesArray[index]) {
                final double[] interpolated = this.getInterpolatedAdmittanceCoefficients(date, s);
                admC = interpolated[0];
                admS = interpolated[1];
            }

            // Intermediate variables to speed-up computation
            final double[] sincos = MathLib.sinAndCos(thetaS + phase);
            final double sinThetaSPhase = sincos[0];
            final double cosThetaSPhase = sincos[1];
            final double cosThetaSPhaseAdmC = cosThetaSPhase * admC;
            final double sinThetaSPhaseAdmC = sinThetaSPhase * admC;
            final double cosThetaSPhaseAdmS = cosThetaSPhase * admS;
            final double sinThetaSPhaseAdmS = sinThetaSPhase * admS;

            final double[][] cppcmIndex = data.cppcmArray[index];
            final double[][] cpmcmIndex = data.cpmcmArray[index];
            final double[][] sppsmIndex = data.sppsmArray[index];
            final double[][] spmsmIndex = data.spmsmArray[index];

            final int[] maxdegIndex = data.maxDegreeArray[index];
            final int[] mindegIndex = data.minDegreeArray[index];

            // loops on the available degrees and orders
            final int maxord = data.maxOrderArray[index];
            for (int j = 0; j < maxord; j++) {

                // Intermediate variables to speed-up computation
                final double[] cppcmj = cppcmIndex[j];
                final double[] cpmcmj = cpmcmIndex[j];
                final double[] sppsmj = sppsmIndex[j];
                final double[] spmsmj = spmsmIndex[j];

                final int maxdeg = maxdegIndex[j];
                final int mindeg = mindegIndex[j];

                // check of the min and max degrees available in the file
                for (int i = mindeg; i < maxdeg; i++) {

                    // Sums C+ + C-, C+ - C-, S+ + S- et S+ - S- ( x factor)
                    final double cppcm = cppcmj[i];
                    final double cpmcm = cpmcmj[i];
                    final double sppsm = sppsmj[i];
                    final double spmsm = spmsmj[i];

                    // Cf equations 6-6 (p. 61) and 6-9 (p. 64) from OBELIX manual
                    arrayC[i][j] += cosThetaSPhaseAdmC * sppsm + cosThetaSPhaseAdmS * cppcm
                            + sinThetaSPhaseAdmC * cppcm - sinThetaSPhaseAdmS * sppsm;
                    arrayS[i][j] += cosThetaSPhaseAdmC * cpmcm - cosThetaSPhaseAdmS * spmsm
                            - sinThetaSPhaseAdmC * spmsm - sinThetaSPhaseAdmS * cpmcm;
                }
            }
            index++;
        }
    }

    /**
     * Interpolates admittance coefficients
     * 
     * @param date date of wanted admittance coefficients
     * @param doodson Doodson number of wave
     * @return double[] array { admC, admS }
     */
    private double[] getInterpolatedAdmittanceCoefficients(final AbsoluteDate date,
                                                           final double doodson) {

        // get before and after entries
        final Entry<AbsoluteDate, TreeMap<Double, double[]>> before = this.admittanceCoefficients
            .floorEntry(date);
        final Entry<AbsoluteDate, TreeMap<Double, double[]>> after = this.admittanceCoefficients
            .ceilingEntry(date);

        // retrieve admittance values
        final double[] admB = before.getValue().get(doodson);
        final double[] admA = after.getValue().get(doodson);

        // linear interpolation
        final double fact = 2 * date.durationFrom(before.getKey()) / Constants.JULIAN_DAY;

        // return interpolated admittance coefficients
        return new double[] { admB[0] * (1 - fact) + admA[0] * fact,
            admB[1] * (1 - fact) + admA[1] * fact };
    }

    /**
     * Checks sufficient admittance data is available. If not, computation of more.
     * 
     * @param date date of admittance coefficients
     * 
     * @throws PatriusException if fails
     */
    private void checkAdmittanceTablesAvailability(
                                                   final AbsoluteDate date) throws PatriusException {

        // test of the table emptiness
        final boolean emptyTable = this.admittanceCoefficients.isEmpty();

        // initialisations
        boolean tooFarTables = false;
        double durationToFirstKey = 0.;
        double durationToLastKey = 0.;

        // if the table is not empty, test of the available dates
        if (!emptyTable) {

            // durations between the current date and the min/max available dates of the table
            durationToFirstKey = this.admittanceCoefficients.firstKey().durationFrom(date);
            durationToLastKey = this.admittanceCoefficients.lastKey().durationFrom(date);

            // if the available dates are too far from the current date
            final double minDuration = MathLib.min(MathLib.abs(durationToFirstKey),
                MathLib.abs(durationToLastKey));
            tooFarTables = (minDuration > C_10 * Constants.JULIAN_DAY);
        }

        // if the table is empty or the available dates too far : total computation of the table
        // from the current date
        if (tooFarTables || emptyTable) {

            // the table is computed from the current day's 0h00
            final DateTimeComponents currentComp = date.getComponents(TimeScalesFactory.getTAI());
            final DateComponents currentDateComp = currentComp.getDate();
            final AbsoluteDate initDateOFTable = new AbsoluteDate(currentDateComp,
                new TimeComponents(0.), TimeScalesFactory.getTAI());

            this.computeNewAdmittanceTables(initDateOFTable, 1);
        } else if (durationToFirstKey > 0.) {
            // if there is no more available date before this, but some after
            // and not too far, computation of new dates backwards
            this.computeNewAdmittanceTables(
                this.admittanceCoefficients.firstKey().shiftedBy(Constants.JULIAN_DAY / 2), -1);
        } else if (durationToLastKey < 0.) {
            // if there is no more available date after this, but some before
            // and not too far, computation of new dates forwards
            this.computeNewAdmittanceTables(
                this.admittanceCoefficients.lastKey().shiftedBy(-Constants.JULIAN_DAY / 2), 1);
        }
        // if there are available dates before and after this, no more computation to do

    }

    /**
     * Computes the new admittance coefficients
     * 
     * @param date Date for computation of admittance coefficients
     * @param direction Direction in time (1 for forward and -1 for backwards)
     * @throws PatriusException if fails
     */
    //CHECKSTYLE: stop MethodLength check
    private void computeNewAdmittanceTables(final AbsoluteDate date, final int direction)
        throws PatriusException {
        //CHECKSTYLE: resume MethodLength check

        // fundamental arguments
        double[][] args;

        // waves
        double[] wavesAd;

        // decomposed Doodson numbers
        int[] d1;
        int[] d2;
        int[] d3;
        int[] dk;

        // frequencies
        double f1;
        double f2;
        double f3;
        double fk;

        // new coefficients
        double hh1;
        double hh2;
        double hh3;

        // new coefficients
        double arg;
        double arg1;
        double arg2;
        double arg3;

        // admittance coeffs initialisations
        double[] adm;
        final TreeMap<AbsoluteDate, TreeMap<Double, double[]>> admCoeffs =
            new TreeMap<AbsoluteDate, TreeMap<Double, double[]>>();

        // loop on the 12 hours time steps : initialisations of each step
        for (int i = 0; i <= DEFAULT_STEP; i++) {
            // twelve hours shifts
            final AbsoluteDate currentDate = date.shiftedBy(direction * Constants.JULIAN_DAY / 2
                    * i);
            admCoeffs.put(currentDate, this.initNewAdmittanceTable());
        }

        // Doodson Fundamental Arguments at the first date
        final double[][] argFirstDate = TidesToolbox.computeFundamentalArguments(date,
            this.oceanTidesData.getStandard());

        // loop on the secondary waves
        for (final Entry<Double, double[]> entry2 : this.secWaves.entrySet()) {
            final double currentWave = entry2.getKey();

            /*
             * For each secondary wave, the admittance coefficients are computed as per OBELIX
             * manual Eq 6-8 p. 64 and OBELIX source code fmf_ccadmt.f90 line 204.
             */

            wavesAd = this.secWaves.get(currentWave);

            // decompose Doodson numbers
            d1 = TidesToolbox.nDoodson(wavesAd[0]);
            d2 = TidesToolbox.nDoodson(wavesAd[1]);
            d3 = TidesToolbox.nDoodson(wavesAd[2]);
            dk = TidesToolbox.nDoodson(currentWave);

            // get frequencies
            f1 = computeThetaS(d1, argFirstDate, 1);
            f2 = computeThetaS(d2, argFirstDate, 1);
            f3 = computeThetaS(d3, argFirstDate, 1);
            fk = computeThetaS(dk, argFirstDate, 1);

            // interpolation par polynome de Lagrange a 3 points
            hh1 = (this.heightsS.get(currentWave) * (fk - f2) * (fk - f3))
                    / (this.heightsP.get(wavesAd[0]) * (f1 - f2) * (f1 - f3));
            hh2 = (this.heightsS.get(currentWave) * (fk - f1) * (fk - f3))
                    / (this.heightsP.get(wavesAd[1]) * (f2 - f1) * (f2 - f3));
            hh3 = (this.heightsS.get(currentWave) * (fk - f1) * (fk - f2))
                    / (this.heightsP.get(wavesAd[2]) * (f3 - f1) * (f3 - f2));

            // loop on the 12 hours time steps
            for (final Entry<AbsoluteDate, TreeMap<Double, double[]>> entry : admCoeffs.entrySet()) {
                // Entries
                final AbsoluteDate currentDate = entry.getKey();
                // temporary table for this wave and date
                final TreeMap<Double, double[]> tempTable = entry.getValue();

                // Doodson Fundamental Arguments
                args = TidesToolbox.computeFundamentalArguments(currentDate,
                    this.oceanTidesData.getStandard());

                // theta S for each wave
                arg = computeThetaS(dk, args, 0);
                arg1 = arg - computeThetaS(d1, args, 0);
                arg2 = arg - computeThetaS(d2, args, 0);
                arg3 = arg - computeThetaS(d3, args, 0);

                // new admittance for first pivot wave
                final double[] sincosarg1 = MathLib.sinAndCos(arg1);
                final double sinarg1 = sincosarg1[0];
                final double cosarg1 = sincosarg1[1];
                adm = tempTable.get(wavesAd[0]);
                adm[0] += hh1 * cosarg1;
                adm[1] += hh1 * sinarg1;
                tempTable.put(wavesAd[0], adm);

                // new admittance for second pivot wave
                final double[] sincosarg2 = MathLib.sinAndCos(arg2);
                final double sinarg2 = sincosarg2[0];
                final double cosarg2 = sincosarg2[1];
                adm = tempTable.get(wavesAd[1]);
                adm[0] += hh2 * cosarg2;
                adm[1] += hh2 * sinarg2;
                tempTable.put(wavesAd[1], adm);

                // new admittance for third pivot wave
                final double[] sincosarg3 = MathLib.sinAndCos(arg3);
                final double sinarg3 = sincosarg3[0];
                final double cosarg3 = sincosarg3[1];
                adm = tempTable.get(wavesAd[2]);
                adm[0] += hh3 * cosarg3;
                adm[1] += hh3 * sinarg3;
                tempTable.put(wavesAd[2], adm);

                admCoeffs.put(currentDate, tempTable);
            }
        }
        this.admittanceCoefficients.putAll(admCoeffs);
    }

    /**
     * Initializes a new admittance table with <br>
     * K<sup>c</sup> = 1 <br>
     * K<sup>s</sup> = 0 <br>
     * for each main wave
     * 
     * @return the new admittance table
     */
    @SuppressWarnings("PMD.LooseCoupling")
    private TreeMap<Double, double[]> initNewAdmittanceTable() {

        // All Cs and Ss in the initial tables are 1. and 0.
        final double[] def = new double[] { 1., 0. };

        final TreeMap<Double, double[]> newTable = new TreeMap<Double, double[]>();
        for (final double d : this.mainWaves) {
            newTable.put(d, def.clone());
        }

        return newTable;

    }

    /**
     * Compute the waves θ<sub>s</sub>
     * 
     * @param nDood decomposed Doodson number
     * @param args Doodson fundamental arguments
     * @param dv 1 if derivative wanted, 0 otherwise
     * @return θ<sub>s</sub> for given Doodson wave
     */
    private static double computeThetaS(final int[] nDood, final double[][] args, final int dv) {

        // init result
        double theta = 0.;

        // loop over all fundamental arguments
        for (int j = 0; j < nDood.length; j++) {
            theta += nDood[j] * args[j][dv];
        }

        return theta;
    }

    /**
     * Get the Doodson-Warburg phase convention
     * 
     * @param onde Doodson number of wave
     * @param h height of wave
     * @return the Doodson-Warburg phase convention
     */
    private double getPhaseConvention(final double onde, final double h) {

        // result container
        final double doodwarb;
        // evaluate the type from the onde
        final int type = (int) (0.01 * onde);

        // filter on height
        if (h > 0) {
            // filter on type
            switch (type) {
                case 0:
                    doodwarb = FastMath.PI;
                    break;
                case 1:
                    doodwarb = P5 * FastMath.PI;
                    break;
                default:
                    doodwarb = 0.;
                    break;
            }
        } else if (h < 0) {
            // filter on type
            switch (type) {
                case 1:
                    doodwarb = -P5 * FastMath.PI;
                    break;
                case 2:
                    doodwarb = FastMath.PI;
                    break;
                default:
                    doodwarb = 0.;
                    break;
            }
        } else {
            doodwarb = 0;
        }
        // return Doodson-Warburg phase
        return doodwarb;

    }

    /** {@inheritDoc} */
    @Override
    public boolean computeGradientPosition() {
        return this.computePartialDerivativesWrtPosition;
    }

    /**
     * Denormalize a coefficient.
     * 
     * @param normalized normalized coefficients array
     * @param n degree
     * @param mm order
     * @comment for our purpose, n + mm < 21 therefore the method
     *          ArithmeticUtils.factorialDouble(int) can be used
     * @return unnormalized coefficient
     */
    private double deNormalize(final double normalized, final int n, final int mm) {

        // (n + m)!
        final double facNplusM = ArithmeticUtils.factorialDouble(n + mm);

        // (n - m)! : in our case m < n
        final double facNminusM = ArithmeticUtils.factorialDouble(n - mm);

        // corrective coefficient: k = sqrt[(2-gamma_0m)(2n+1)(n-m)! / (n+m)!]
        double k = 0.;

        if (mm == 0) {
            k = MathLib.sqrt(MathLib.divide((2 * n + 1) * facNminusM, facNplusM));
        } else {
            k = MathLib.sqrt(MathLib.divide(2 * (2 * n + 1) * facNminusM, facNplusM));
        }
        // k=1;
        return k * normalized;
    }

    /**
     * Denormalize a coefficients table.
     * 
     * @param tab table
     * @comment for our purpose, n + mm < 21 therefore the method
     *          ArithmeticUtils.factorialDouble(int) can be used
     * @return unnormalized table
     */
    private double[][] deNormalize(final double[][] tab) {

        final double[][] newTab = new double[this.l][this.m];

        for (int i = 0; i < this.l; i++) {
            for (int j = 0; j < MathLib.min(this.m, i + 1); j++) {
                newTab[i][j] = this.deNormalize(tab[i][j], i, j);
            }
        }

        return newTab;
    }

    /**
     * @return the oceanTidesData
     */
    public IOceanTidesDataProvider getOceanTidesData() {
        return this.oceanTidesData;
    }
    
    /** {@inheritDoc} */
    @Override
    public void checkData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        // Nothing to do
    }

    /**
     * Class used to store once and for all data fixed at construction.
     * 
     * @author Emmanuel Bignon
     * @since 3.2
     */
    private static class StaticData implements Serializable {

        /** Serial UID. */
        private static final long serialVersionUID = 8309782370684286558L;

        /** Phase - Interval variable stored at constructor to speed-up computation at runtime. */
        private final double[] phaseArray;

        /** Main waves - Interval variable stored at constructor to speed-up computation at runtime. */
        private final boolean[] mainWavesArray;

        /** nDoodson - Interval variable stored at constructor to speed-up computation at runtime. */
        private final int[][] nDoodsonArray;

        /** Max order - Interval variable stored at constructor to speed-up computation at runtime. */
        private final int[] maxOrderArray;

        /** Max degree - Interval variable stored at constructor to speed-up computation at runtime. */
        private final int[][] maxDegreeArray;

        /** Min degree - Interval variable stored at constructor to speed-up computation at runtime. */
        private final int[][] minDegreeArray;

        /** CPPCM - Interval variable stored at constructor to speed-up computation at runtime. */
        private final double[][][] cppcmArray;

        /** CPMCM - Interval variable stored at constructor to speed-up computation at runtime. */
        private final double[][][] cpmcmArray;

        /** SPPSM - Interval variable stored at constructor to speed-up computation at runtime. */
        private final double[][][] sppsmArray;

        /** SPMSM - Interval variable stored at constructor to speed-up computation at runtime. */
        private final double[][][] spmsmArray;

        /**
         * Constructor.
         * 
         * @param phaseArrayIn phase array
         * @param mainWavesArrayIn main waves array
         * @param nDoodsonArrayIn n Doodson array
         * @param maxOrderArrayIn max order array
         * @param maxDegreeArrayIn max degree array
         * @param minDegreeArrayIn min degree array
         * @param cppcmArrayIn cppcm array
         * @param cpmcmArrayIn cpmcm array
         * @param sppsmArrayIn sppsm array
         * @param spmsmArrayIn spmsm array
         */
        public StaticData(final double[] phaseArrayIn, final boolean[] mainWavesArrayIn,
                          final int[][] nDoodsonArrayIn, final int[] maxOrderArrayIn,
                          final int[][] maxDegreeArrayIn, final int[][] minDegreeArrayIn,
                          final double[][][] cppcmArrayIn, final double[][][] cpmcmArrayIn,
                          final double[][][] sppsmArrayIn, final double[][][] spmsmArrayIn) {
            this.phaseArray = phaseArrayIn;
            this.mainWavesArray = mainWavesArrayIn;
            this.nDoodsonArray = nDoodsonArrayIn;
            this.maxOrderArray = maxOrderArrayIn;
            this.maxDegreeArray = maxDegreeArrayIn;
            this.minDegreeArray = minDegreeArrayIn;
            this.cppcmArray = cppcmArrayIn;
            this.cpmcmArray = cpmcmArrayIn;
            this.sppsmArray = sppsmArrayIn;
            this.spmsmArray = spmsmArrayIn;
        }
    }
}
