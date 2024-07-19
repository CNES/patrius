/**
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.math.filter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import fr.cnes.sirius.patrius.math.analysis.UnivariateVectorFunction;
import fr.cnes.sirius.patrius.math.interval.AngleTools;
import fr.cnes.sirius.patrius.math.linear.BlockRealMatrix;
import fr.cnes.sirius.patrius.math.linear.RealMatrix;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class implements a digital FIR filter.
 *
 * @author GMV
 */
public final class FIRFilter {

    /** Enum for filter types */
    public enum FilterType {
        /** Causal and non-linear filter */
        CAUSAL,
        /** Linear, non-causal and centered filter */
        LINEAR;
    }

    /** Enum for data types */
    public enum DataType {
        /** Angular data type */
        ANGULAR,
        /** Other data type */
        OTHER;
    }

    /** Coefficients of the filter in the internal usable order */
    private final double[] coeffs;

    /** Filter sampling step */
    private final double samplingStep;

    /** Number of points to be used before the computation point */
    private final int nbPointsBefore;

    /** Number of points to be used after the computation point */
    private final int nbPointsAfter;

    /** Data types of the function values to be computed by this filter */
    private final DataType[] dataTypeArray;

    /**
     * Constructor.
     *
     * @param filterType
     *        FIR filter type
     * @param dataTypeArray
     *        data type to be filtered
     * @param coeffs
     *        coefficients of the filter (in case of LINEAR filter, coefficients are symmetric so
     *        only the half of
     *        the coefficients plus one must be defined)
     * @param samplingStep
     *        filter sampling step
     * @throws PatriusException
     *         if the specified type of FIR filter is not currently implemented
     */
    public FIRFilter(final FilterType filterType, final DataType[] dataTypeArray, final List<Double> coeffs,
            final double samplingStep) throws PatriusException {

        // Storing step
        this.samplingStep = samplingStep;

        // Storing the data type array corresponding to functions to filter
        this.dataTypeArray = Arrays.copyOf(dataTypeArray, dataTypeArray.length);

        // Getting the number of points to be used before and after the computation point, depending
        // on the type of filter
        switch (filterType) {
        // Causal and non-linear filter
            case CAUSAL:
                this.nbPointsBefore = coeffs.size() - 1;
                this.nbPointsAfter = 0;
                break;

            // Linear, non-causal and centered filter
            case LINEAR:
                this.nbPointsBefore = coeffs.size() - 1;
                this.nbPointsAfter = coeffs.size() - 1;
                break;

            default:
                throw new PatriusException(PatriusMessages.MISSING_FILTER_TYPE, filterType.name());
        }

        // Computing the expanded coefficients of the filter in the usable order
        this.coeffs = new double[getNbPointsBefore() + getNbPointsAfter() + 1];
        int j = 0;
        for (int i = this.coeffs.length - 1; i >= 0; i--) {
            if (j < coeffs.size()) {
                this.coeffs[i] = coeffs.get(j);
            } else {
                this.coeffs[i] = coeffs.get(i);
            }
            j++;
        }
    }

    /**
     * Getter for the number of points before the computation date to be used by the filter.
     *
     * @return the number of points before the computation date to be used by the filter
     */
    public int getNbPointsBefore() {
        return this.nbPointsBefore;
    }

    /**
     * Getter for the number of points after the computation date to be used by the filter.
     *
     * @return the number of points after the computation date to be used by the filter
     */
    public int getNbPointsAfter() {
        return this.nbPointsAfter;
    }

    /**
     * Getter for sampling step of the filter.
     *
     * @return the sampling step of the filter
     */
    public double getSamplingStep() {
        return this.samplingStep;
    }

    /**
     * Computes the filtered value of the given function at the given computation point. Angular
     * values are normalized to [-PI, PI) before being returned.
     *
     * @param f
     *        vectorial function to filter
     * @param x0
     *        computation point
     * @return the filtered value of the function at given computation point
     */
    public double[] compute(final UnivariateVectorFunction f, final double x0) {

        // Creating the matrix to contain the function values (rows) at each required point x
        // (columns)
        final RealMatrix yMatrix = new BlockRealMatrix(f.getSize(), this.nbPointsBefore + this.nbPointsAfter + 1);

        // Computing the column associated to the function value at each required point x
        for (int i = 0; i < this.nbPointsBefore + 1; i++) {
            yMatrix.setColumn(i, f.value(x0 - getSamplingStep() * (this.nbPointsBefore - i)));
        }
        for (int i = 1; i < this.nbPointsAfter + 1; i++) {
            yMatrix.setColumn(this.nbPointsBefore + i, f.value(x0 + getSamplingStep() * i));
        }

        // Making values continuous if they are of ANGULAR type
        final DataType[] valueType = dataTypeArray;
        for (int i = 0; i < f.getSize(); i++) {
            if (DataType.ANGULAR.equals(valueType[i])) {
                makeContinuousValues(yMatrix, i);
            }
        }

        // Post-multiply the y matrix by the column vector containing the coefficients of the filter
        // to obtain the filtered values
        final double[] filteredValues = yMatrix.operate(this.coeffs);

        // Normalize angles in [-PI, PI)
        for (int i = 0; i < f.getSize(); i++) {
            if (DataType.ANGULAR.equals(valueType[i])) {
                filteredValues[i] = MathUtils.normalizeAngle(filteredValues[i], 0.0);
            }
        }

        return filteredValues;
    }

    /**
     * Set row with continuous values.
     *
     * @param yMatrix
     *        the matrix
     * @param i
     *        the row
     */
    private void makeContinuousValues(final RealMatrix yMatrix, final int i) {
        // Passer par des double[]
        final List<Double> values = DoubleStream.of(yMatrix.getRowVector(i).toArray()).boxed()
                .collect(Collectors.toList());
        // getRow()
        AngleTools.unMod(values, 2 * FastMath.PI);
        final double[] continuousValues = new double[values.size()];
        for (int j = 0; j < values.size(); j++) {
            continuousValues[j] = values.get(j);
        }
        yMatrix.setRow(i, continuousValues);
    }
}
